# Correção do cadastro de produtos — implementação na branch

**Estado (25/11/2026):** ajustes de backend e frontend publicados exclusivamente na branch
`collab/correcao-cadastro-produtos-2026-11`, ainda **sem homologação Maven/PostgreSQL e sem merge à release**.
A aplicação em um ambiente local exige atualizar ambos os projetos, executar a migration Flyway V50
e verificar a listagem com uma sessão da Unidade correta.

## Problemas identificados na release

1. **Descrição longa:** a coluna `produtos.descricao` começou como `VARCHAR(255)` em V1;
   o texto técnico de BDA informado possui 360 caracteres. Violações de integridade retornavam
   HTTP 409 com uma mensagem genérica, sem explicar o tamanho excedido.
2. **Produto criado, mas não listado:** o catálogo administrativo consumia `GET /api/v1/produtos`,
   que na Unidade retorna apenas produtos associados a `estoque_central`.
   Um produto recém-cadastrado sem estoque ficava invisível, embora seu código de referência
   já existisse no banco.

## Implementação aplicada

### Banco e domínio

- **V50** (`src/main/resources/db/migration/V50__descricao_produto_texto_e_unidade_cadastro.sql`)
  altera a coluna `produtos.descricao` para `TEXT`, acrescenta
  `unidade_cadastro_id` (FK opcional) e um índice.
- `Produto.java` usa `@Column(columnDefinition = "text")` e guarda
  `unidadeCadastro` com `@ManyToOne(fetch = LAZY)`.
- O campo de Unidade permanece nulo para produtos históricos sem origem confiável.
  A migration atribui a Unidade a órfãos sem estoque **somente quando existe exatamente
  uma Unidade** no banco. Em base multi-Unidade, casos históricos sem estoque
  exigem conciliação administrativa, sem atribuição arbitrária e sem apagar registros.

### Catálogo x estoque

- `ProdutoService.criar()` exige Unidade da sessão e grava a origem.
- `ProdutoRepository.findCatalogoAdministrativoDaUnidade()` traz produtos
  cadastrados na Unidade **ou** associados a `EstoqueCentral` da Unidade,
  inclusive produtos ativos sem saldo.
- `GET /api/v1/produtos/catalogo` é a nova rota de listagem administrativa, protegida
  para `ADMINISTRADOR` e `ADMINISTRADOR_GERAL`.
- A tela administrativa do frontend usa essa rota, com fallback temporário para
  o endpoint antigo enquanto a API estiver desatualizada.
- `ProdutoService.buscarProdutoNoTenant()` permite consultar/editar um produto
  cadastrado na Unidade sem exigir estoque; continua proibindo acesso a produtos
  de outras Unidades sem relação autorizada.
- **Não foram alterados** `GET /api/v1/produtos`,
  `ProdutoRepository.pertenceAUnidade()`, `findDisponiveisNaUnidade()` nem
  as regras de pedidos, soluções e retiradas de estoque. Cadastro
  administrativo **não cria saldo físico e não libera produto em pedidos**.

### Erros explicativos

- `RestExceptionHandler.handleDataIntegrity()` interpreta SQLState
  sem devolver mensagens SQL/Hibernate aos usuários:
  - `22001`: campo de texto extenso — HTTP 400 — `CAMPO_TEXTO_EXTENSO`.
  - `23505`: valor único duplicado — HTTP 409 — `VALOR_DUPLICADO`.
  - `23502`: campo obrigatório — HTTP 400 — `CAMPO_OBRIGATORIO`.
  - `23503`: referência inválida — HTTP 409 — `REFERENCIA_INVALIDA`.
  - `23514`: regra de banco violada — HTTP 400 — `VALOR_INVALIDO`.
  - Outros: `INTEGRIDADE_DADOS`, mensagem legível mas sem revelar SQL.
- `ProdutoRequestDTO` valida previamente os limites dos campos de texto
  que ainda têm tamanho definido. **A descrição geral não possui limite de 255.**
- `RestExceptionHandler.handleValidation()` inclui na mensagem as primeiras causas
  de campo, além do array `fieldErrors`. A tela de cadastros já exibe `message`.
- `ApiError` e as demais rotas continuam com seu contrato anterior.

## Testes incluídos

- `ProdutoServiceTest`: Unidade na criação; catálogo com produto sem estoque;
  acesso por Unidade; regressão.
- `ProdutoRepositoryTest`: descrição acima de 255 caracteres, catálogo multi-Unidade
  e produto legado com vínculo de estoque.
- `RestExceptionHandlerTest`: mensagens SQLState sem vazamento de SQL ou detalhes técnicos.
- Frontend `cadastroProdutosListagem.spec.ts`: consulta da rota nova,
  fallback para API antiga e propagação de erro real.

**Execução ainda pendente:** não foi possível executar a suíte no ambiente GitHub disponível;
o workflow de sincronização com GitLab não compila nem roda Maven/Vitest.
Verificação de código e presença de arquivos **não equivale a testes aprovados**.

## Homologação antes de qualquer merge

- [ ] Backend: `./mvnw clean verify`
- [ ] Frontend: `npm run build && npm run test`
- [ ] PostgreSQL DEV: atualizar com Flyway, confirmar `flyway_schema_history` com V50,
      inicialização sob `ddl-auto=validate`.
- [ ] Criar produto com descrição BDA de 360 caracteres; confirmar persistência intacta,
      HTTP 201 e exibição na área de Cadastros.
- [ ] Confirmar que produto sem `EstoqueCentral` **não aparece em pedidos operacionais**.
- [ ] Atualizar/inativar o produto sem estoque da Unidade de origem.
- [ ] Validar isolamento de Unidade e autorização da rota administrativa.
- [ ] Tentar duplicar código e conferir mensagem clara.
- [ ] Simular um campo limitado muito grande e conferir `@Size` / HTTP 400
      com o nome e tamanho permitido.
- [ ] Confirmar comportamento no DEMO H2 (Flyway desabilitado, esquema Hibernate) e
      em DEV PostgreSQL com backup.
- [ ] Verificar se existem produtos históricos sem estoque em banco multi-Unidade,
      conciliando a Unidade de origem sem adivinhação.

### Consulta diagnóstica somente leitura

```sql
SELECT p.id, p.nome, p.codigo_referencia, p.ativo, p.unidade_cadastro_id,
       EXISTS (
           SELECT 1 FROM estoque_central ec WHERE ec.produto_id = p.id
       ) AS possui_vinculo_estoque
FROM produtos p
WHERE p.nome ILIKE '%BDA%' OR p.nome ILIKE '%Batata Dextrose%';
```

Para produtos históricos sem Unidade em bancos multi-Unidade, a conciliação
deve ser feita com evidências da operação; **não remover registros nem duplicá-los**.
