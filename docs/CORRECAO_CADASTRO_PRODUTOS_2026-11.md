# Correção do cadastro de produtos — implementação manual no backend

**Estado:** diagnóstico confirmado no código da `release`; correção backend **não aplicada** (regra do projeto: alterações funcionais no backend são manuais). A branch colaborativa contém apenas este roteiro. O frontend correspondente já tenta consultar `GET /api/v1/produtos/catalogo` e, enquanto a rota não existe, usa `GET /api/v1/produtos` como fallback.

## 1. Problemas comprovados

**409 em descrição longa.** `V1__create_initial_schema.sql` declara `produtos.descricao VARCHAR(255)`, e `Produto.java` tem `private String descricao;` sem comprimento explícito. O texto de cadastro de BDA informado contém **360 caracteres**. O `RestExceptionHandler` converte `DataIntegrityViolationException` em HTTP 409 genérico. Confirmar pela resposta da API/log qual restrição disparou: a causa de comprimento é altamente provável, enquanto código duplicado no `ProdutoService` gera HTTP 400 (`BusinessRuleException`).

**Produto ativo fora da lista.** `ProdutoService.listarTodos()` chama `produtosVisiveis()`, que usa `ProdutoRepository.findDisponiveisNaUnidade(unidadeId)` quando há tenant. Essa consulta retorna somente produto que já tem linha em `estoque_central` da Unidade. Entretanto, `ProdutoService.criar()` só insere em `produtos`. A tentativa posterior com o mesmo código encontra o registro global (`existsByCodigoReferencia`). `CadastrosAdminView.vue` não filtra incorretamente: a API já omite o produto.

## 2. Decisão arquitetural e compatibilidade

**Separar catálogo administrativo de produtos operacionalmente vinculados ao estoque.** Não mudar o comportamento de `GET /api/v1/produtos`, que atende seletores operacionais. Não criar estoque fictício de quantidade zero no cadastro, pois isso colidiria com `EstoqueCentralService.criar` e com a separação entre catálogo e entrada física.

O modelo atual de `Produto` é global (código de referência único no sistema), mas não registra a Unidade em que foi cadastrado. Para garantir visibilidade e permissão administrativa sem revelar produtos órfãos de outras Unidades, acrescentar uma **Unidade de cadastro (nullable para legados)**. A consulta administrativa mostrará (a) produtos cadastrados nessa Unidade **OU** (b) produtos com um `EstoqueCentral` associado à Unidade. Não altera o contrato de códigos únicos nem as consultas operacionais.

## 3. Aplicação por arquivo

### 3.1 Flyway V50 — arquivo novo

`src/main/resources/db/migration/V50__descricao_produto_texto_e_unidade_cadastro.sql`

```sql
ALTER TABLE produtos ALTER COLUMN descricao TYPE TEXT;

ALTER TABLE produtos ADD COLUMN unidade_cadastro_id BIGINT;

ALTER TABLE produtos
  ADD CONSTRAINT fk_produtos_unidade_cadastro
  FOREIGN KEY (unidade_cadastro_id) REFERENCES unidades(id);

CREATE INDEX idx_produtos_unidade_cadastro
  ON produtos(unidade_cadastro_id);

-- Só é seguro inferir a origem de produtos antigos sem estoque quando existe
-- exatamente uma Unidade no banco. Não atribuir origem arbitrária em multi-Unidade.
UPDATE produtos p
SET unidade_cadastro_id = (SELECT MIN(u.id) FROM unidades u)
WHERE p.unidade_cadastro_id IS NULL
  AND (SELECT COUNT(*) FROM unidades) = 1
  AND NOT EXISTS (
      SELECT 1 FROM estoque_central ec WHERE ec.produto_id = p.id
  );
```

A V49 já está na branch base (modalidades de bolsa). Em PostgreSQL o Flyway aplicará a V50 depois da V49; no perfil DEMO, o Hibernate gera o esquema a partir da entidade.

### 3.2 `Produto.java`

Adicionar imports `jakarta.persistence.ManyToOne` e `jakarta.persistence.FetchType` (caso ainda ausentes). Substituir o campo de descrição e acrescentar a Unidade de cadastro:

```java
@Column(columnDefinition = "text")
private String descricao;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "unidade_cadastro_id")
private Unidade unidadeCadastro;
```

`JoinColumn` já é utilizado no projeto, mas conferir import. Campo opcional preserva produtos legados.

### 3.3 `ProdutoRepository.java`

**Não alterar** `findAll` nem `findDisponiveisNaUnidade`. Adicionar método administrativo com `LEFT JOIN` (necessário porque a Unidade de cadastro é nullable):

```java
@Query("""
  SELECT DISTINCT p FROM Produto p
  LEFT JOIN p.unidadeCadastro uc
  WHERE uc.publicId = :unidadeId
     OR EXISTS (
       SELECT ec.id FROM EstoqueCentral ec
       WHERE ec.produto = p AND ec.unidade.publicId = :unidadeId
     )
  ORDER BY p.nome
  """)
List<Produto> findCatalogoAdministrativoDaUnidade(
    @Param("unidadeId") UUID unidadeId);
```

**Não alterar `pertenceAUnidade`.** O método possui semântica operacional de associação física ao estoque e é chamado por `PedidoService`, `ModeloSolucaoService`, `ModeloResiduoService` e `ResiduoService`. Tratar a mera criação de produto como vínculo físico poderia permitir seleções fora da regra atual, afetando pedidos e composições. A distinção entre catálogo e estoque deve permanecer.

### 3.4 `ProdutoService.java`

Adicionar colaboradores `UnidadeRepository` e import `Unidade`. Em `criar`, antes de salvar, exigir a Unidade resolvida da sessão e gravar a origem:

```java
UUID unidadeId = TenantContext.unidadeAtual()
    .orElseThrow(() -> new BusinessRuleException(
        "Unidade em operação é obrigatória."));
Unidade unidade = unidadeRepository.findByPublicId(unidadeId)
    .orElseThrow(() -> new ResourceNotFoundException("Unidade", unidadeId));

Produto produto = new Produto();
preencherProduto(produto, dto);
produto.setUnidadeCadastro(unidade);
return new ProdutoResponseDTO(produtoRepository.save(produto));
```

Adicionar consulta própria, sem tocar em `listarTodos()`:

```java
@Transactional(readOnly = true)
public List<ProdutoResponseDTO> listarCatalogoAdministrativo() {
    UUID unidadeId = TenantContext.unidadeAtual()
        .orElseThrow(() -> new BusinessRuleException(
            "Unidade em operação é obrigatória."));
    return produtoRepository.findCatalogoAdministrativoDaUnidade(unidadeId)
        .stream().map(ProdutoResponseDTO::new).toList();
}
```

**Alterar apenas a validação local de `buscarProdutoNoTenant`**, mantendo a consulta `pertenceAUnidade` intacta. Após buscar o produto por UUID, substituir o predicado de autorização interno por:

```java
if (TenantContext.unidadeAtual().map(unidadeId -> {
    boolean estaNoEstoqueDaUnidade =
        produtoRepository.pertenceAUnidade(id, unidadeId);
    boolean foiCadastradoNaUnidade =
        produto.getUnidadeCadastro() != null
        && unidadeId.equals(produto.getUnidadeCadastro().getPublicId());
    return !estaNoEstoqueDaUnidade && !foiCadastradoNaUnidade;
}).orElse(false)) {
    throw new ResourceNotFoundException("Produto", id);
}
```

Assim, a **edição e consulta administrativa por ID** funcionam para produtos recém-cadastrados, mas **pedidos, soluções e resíduos** continuam usando o vínculo físico de estoque conforme as regras atuais. Não alterar `ProdutoService.listarTodos()`.

### 3.5 `ProdutoController.java` e `ApiSecurityConfig.java`

Acrescentar à controller, ao lado de `listarTodos`:

```java
@GetMapping("/catalogo")
public ResponseEntity<List<ProdutoResponseDTO>> listarCatalogoAdministrativo() {
    return ResponseEntity.ok(produtoService.listarCatalogoAdministrativo());
}
```

Na cadeia `authorizeHttpRequests` de `ApiSecurityConfig`, adicionar **antes de `anyRequest`** e após a definição de rotas de autenticação:

```java
auth.requestMatchers(
    HttpMethod.GET, "/api/v1/produtos/catalogo"
).access(AuthorizationManagers.allOf(
    AuthorityAuthorizationManager.hasAuthority(
        JwtAuthenticationFilter.ACTIVE_AUTHORITY),
    AuthorityAuthorizationManager.hasAnyRole(
        Perfil.ADMINISTRADOR.name(), Perfil.ADMINISTRADOR_GERAL.name())
));
```

O cadastro de produtos já é restrito a administradores pelas regras de gravação da mesma classe. Não liberar a rota administrativa para qualquer usuário autenticado.

### 3.6 Frontend

Já preparado na branch `collab/correcao-cadastro-produtos-2026-11`: `cadastrosAdminService.listarProdutos()` tenta `/v1/produtos/catalogo`. Se o servidor ainda responder 404, mantém `/v1/produtos` como fallback temporário. A tela `CadastrosAdminView.vue` não precisa de filtro especial ou inserção local fictícia.

## 4. Vínculos anteriores ao ajuste

A V50 recupera automaticamente os produtos órfãos **apenas se o banco tiver uma única Unidade**. Em banco com várias Unidades, produtos legados sem linha em `estoque_central` **não possuem origem verificável** no modelo atual. Não atribuir a qualquer Unidade por suposição. Reconciliar registros específicos pelo histórico administrativo disponível; quando houver certeza, atualizar `unidade_cadastro_id` de forma dirigida. Não duplicar nem apagar produtos para contornar a listagem.

Consulta apenas diagnóstica:

```sql
SELECT p.id, p.nome, p.codigo_referencia, p.ativo,
       p.unidade_cadastro_id
FROM produtos p
WHERE NOT EXISTS (
  SELECT 1 FROM estoque_central ec WHERE ec.produto_id = p.id
)
ORDER BY p.nome;
```

## 5. Homologação obrigatória

- [ ] Confirmar que `pertenceAUnidade` permanece inalterado para pedidos, soluções e resíduos e que apenas `ProdutoService.buscarProdutoNoTenant` aceita a Unidade de cadastro.
- [ ] Ajustar `ProdutoServiceTest` para o novo `UnidadeRepository` e tenant na criação; criar teste de listagem administrativa e separação por Unidade.
- [ ] Garantir que `ProdutoRepositoryTest` cobre produtos com Unidade própria e sem estoque, e produtos legados vinculados apenas por estoque.
- [ ] `./mvnw clean verify`; `npm run build` e `npm run test`.
- [ ] Confirmar Flyway V50 e `ddl-auto=validate` em PostgreSQL de desenvolvimento com backup.
- [ ] Cadastrar produto BDA com descrição de 360 caracteres; confirmar `201`, persistência e exibição.
- [ ] Confirmar que produto sem estoque aparece em Cadastros, mas **não aparece em consulta operacional de disponibilidade**.
- [ ] Confirmar que é possível editar/inativar o produto sem estoque pelo mesmo tenant.
- [ ] Confirmar isolamento: outra Unidade sem vínculo de estoque não vê/edita o produto.
- [ ] Confirmar código duplicado mantém rejeição, sem novo registro.
- [ ] Testar DEMO H2 e uma base com múltiplas Unidades sem vazamento de catálogo.


## 6. Mensagens de erro úteis para o usuário (backend manual)

### 6.1 RestExceptionHandler.java

Adicionar ao início dos imports:

```java
import java.sql.SQLException;
```

Substituir **apenas** o método `handleDataIntegrity` pelo trecho abaixo. O `ApiError` mantém o mesmo contrato JSON (`message`, `codigo`, `status`, `path`). **Nunca devolver texto bruto de SQL/Hibernate** (pode expor consultas, nomes internos, valores de parâmetros ou outros dados sensíveis).

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ApiError> handleDataIntegrity(
        DataIntegrityViolationException ex,
        HttpServletRequest request) {

    String estado = identificarSqlState(ex);

    // SQLState padronizado (PostgreSQL e H2); não interpretar SQL pela mensagem.
    HttpStatus status;
    String mensagem;
    String codigo;

    if ("22001".equals(estado)) {
        status = HttpStatus.BAD_REQUEST;
        mensagem = "Um dos campos de texto ultrapassou o limite permitido. "
                + "Revise a descrição, localização e observações.";
        codigo = "CAMPO_TEXTO_EXTENSO";
    } else if ("23505".equals(estado)) {
        status = HttpStatus.CONFLICT;
        mensagem = "Já existe um cadastro com um valor que precisa ser único. "
                + "Verifique o código de referência ou outro identificador.";
        codigo = "VALOR_DUPLICADO";
    } else if ("23502".equals(estado)) {
        status = HttpStatus.BAD_REQUEST;
        mensagem = "Um campo obrigatório está sem valor. "
                + "Revise os dados preenchidos.";
        codigo = "CAMPO_OBRIGATORIO";
    } else if ("23503".equals(estado)) {
        status = HttpStatus.CONFLICT;
        mensagem = "Um registro relacionado não existe ou está sendo utilizado "
                + "por outros cadastros.";
        codigo = "REFERENCIA_INVALIDA";
    } else if ("23514".equals(estado)) {
        status = HttpStatus.BAD_REQUEST;
        mensagem = "Um dos valores informados não atende às regras de cadastro.";
        codigo = "VALOR_INVALIDO";
    } else {
        status = HttpStatus.CONFLICT;
        mensagem = "Não foi possível salvar devido a uma incompatibilidade nos dados. "
                + "Revise o cadastro e, se persistir, comunique ao administrador.";
        codigo = "INTEGRIDADE_DADOS";
    }

    log.warn("Integridade de dados recusada: sqlState={}, codigo={}, rota={}",
            estado == null ? "desconhecido" : estado,
            codigo,
            request.getRequestURI());

    return buildResponse(status,
            "Dados inválidos ou conflitantes",
            mensagem,
            request.getRequestURI(),
            codigo);
}

private String identificarSqlState(Throwable erro) {
    // Mensagem técnica e SQL completo não são retornados nem registrados.
    for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
        if (causa instanceof SQLException sql) {
            for (SQLException atual = sql; atual != null;
                    atual = atual.getNextException()) {
                if (atual.getSQLState() != null) {
                    return atual.getSQLState();
                }
            }
        }
        if (causa == causa.getCause()) break;
    }
    return null;
}
```

### 6.2 RestExceptionHandlerTest.java

Adicionar imports `java.sql.SQLException` e `org.springframework.dao.DataIntegrityViolationException`. Na classe existente, incluir testes cobrindo o estado e a resposta:

```java
@Test
void textoLongoInformaProblemaAoUsuario() {
    MockHttpServletRequest req =
        new MockHttpServletRequest("POST", "/api/v1/produtos");
    var ex = new DataIntegrityViolationException(
        "falha de integridade",
        new SQLException("value too long", "22001")
    );

    var resposta = handler.handleDataIntegrity(ex, req);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(resposta.getBody().codigo()).isEqualTo("CAMPO_TEXTO_EXTENSO");
    assertThat(resposta.getBody().message()).contains("campos de texto");
    assertThat(resposta.getBody().message()).doesNotContain("value too long");
}

@Test
void codigoDuplicadoInformaConflito() {
    MockHttpServletRequest req =
        new MockHttpServletRequest("POST", "/api/v1/produtos");
    var ex = new DataIntegrityViolationException(
        "falha de integridade",
        new SQLException("duplicate key", "23505")
    );

    var resposta = handler.handleDataIntegrity(ex, req);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(resposta.getBody().codigo()).isEqualTo("VALOR_DUPLICADO");
    assertThat(resposta.getBody().message()).contains("único");
}

@Test
void integridadeNaoClassificadaNaoExibeSqlInterno() {
    MockHttpServletRequest req =
        new MockHttpServletRequest("POST", "/api/v1/produtos");
    var ex = new DataIntegrityViolationException(
        "SQL SELECT senha FROM ...",
        new SQLException("detalhes tecnicos", "99999")
    );

    var resposta = handler.handleDataIntegrity(ex, req);

    assertThat(resposta.getBody().codigo()).isEqualTo("INTEGRIDADE_DADOS");
    assertThat(resposta.getBody().message()).doesNotContain("senha", "SELECT");
}
```

### 6.3 Interação com a tela

A função `mensagemErro()` em `CadastrosAdminView.vue` **já exibe** `error.response?.data?.message`. Não alterar para apresentar mensagens fixas que escondam a explicação fornecida pelo backend.

O cadastro do BDA terá dois resultados esperados após aplicar a V50: uma descrição de 360 caracteres deverá ser aceita (sem violação de comprimento) e um campo **diferente** que ultrapasse seu limite deverá produzir o erro `CAMPO_TEXTO_EXTENSO` com mensagem compreensível.

Para identificar o campo **exato**, usar validação por DTO com `@Size` em cada campo limitado, quando fizer sentido; o SQLState `22001` sozinho **não indica qual coluna** ultrapassou o limite. Não fazer análise de texto de exceção do SGBD no frontend.

- [ ] Aplicar V50 e `@Column(columnDefinition = "text")` antes de repetir o cadastro longo.
- [ ] Aplicar novo tratamento de erro e testes unitários.
- [ ] Testar violação SQLState 22001, 23505, 23502 e erro não classificado.
- [ ] Garantir que detalhes de SQL, parâmetros e segredos não são expostos nas respostas.
