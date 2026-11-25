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

Modificar **somente** a consulta `pertenceAUnidade` para aceitar o vínculo de cadastro como alternativa ao estoque, mantendo seu nome, assinatura e parâmetros:

```java
@Query("""
  SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
  FROM Produto p
  LEFT JOIN p.unidadeCadastro uc
  WHERE p.publicId = :produtoPublicId
    AND (
      uc.publicId = :unidadePublicId
      OR EXISTS (
        SELECT ec.id FROM EstoqueCentral ec
        WHERE ec.produto = p AND ec.unidade.publicId = :unidadePublicId
      )
    )
  """)
boolean pertenceAUnidade(
    @Param("produtoPublicId") UUID produtoPublicId,
    @Param("unidadePublicId") UUID unidadePublicId);
```

**Atenção:** a nova semântica de `pertenceAUnidade` significa **vínculo ao catálogo OU ao estoque**, não exclusivamente estoque. Procurar chamadas que exijam apenas vínculo físico antes de concluir a homologação.

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

O método existente `buscarProdutoNoTenant` passará a encontrar produtos recém-cadastrados nessa Unidade graças à nova regra de `pertenceAUnidade`. Manter a validação de Unidade no método.

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

- [ ] Revisar impacto de `pertenceAUnidade` em todas as chamadas, principalmente leitura por ID e edição.
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
