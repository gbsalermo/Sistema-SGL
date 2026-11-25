# Integração para homologação — Produtos + Bolsas independentes

**Estado:** alterações reunidas nas branches colaborativas `collab/integracao-produtos-bolsas-2026-11` de **Sistema-SGL (API)** e **SGL-FRONTEND (Web)**. **Não integradas à `release`, não implantadas em produção.**

## Escopo consolidado

### 1. Hotfix de Produtos — funcionalmente validado pelo responsável

Mantidas as alterações de `collab/hotfix-produtos-descricao-catalogo-2026-11`:
- Descrição de produtos com texto longo: coluna PostgreSQL `TEXT` e entidade Java.
- Novo catálogo administrativo por Unidade, inclusive produtos sem `EstoqueCentral`.
- Rota `GET /api/v1/produtos/catalogo`, protegida para administradores.
- Registro de Unidade no cadastro e recuperação de produtos históricos sem estoque quando só há uma Unidade institucional.
- Mensagens de integridade e validação de campos, sem expor SQL.
- **Não alterar disponibilidade física para pedidos, FEFO/FIFO, movimentações, lotes e recipientes.**

### 2. Bolsas independentes por Unidade — aguardando homologação do responsável

Revisada a modelagem anterior que vinculava bolsas personalizadas obrigatoriamente ao enum `TipoBolsa`. **A modelagem antiga NÃO deve ser publicada.**

- Entidade `Bolsa` com `id`, `nome`, `ativo` e Unidade, sem `tipoBase`.
- API `GET/POST/PUT/DELETE /api/v1/bolsas`, com listagem `/ativas`, validação da Unidade e restrição de escrita ao perfil de gestão.
- Contratos de vínculo aceitam **exatamente uma origem**: `tipoBolsa` (opção legada) **OU** `bolsaId` (catálogo independente).
- `vinculos_estagio.tipo_bolsa` e `estagiarios.tipo_bolsa` passam a permitir NULL; vínculos legados preservam o enum original. CHECK no PostgreSQL exige um e somente um identificador para vínculos.
- `VinculoEstagio.bolsa` referencia catálogo da Unidade; `bolsaNome` guarda snapshot para preservar histórico após renomeação/inativação.
- Frontend tem escolha entre tipo legado e bolsa cadastrada; nova bolsa é cadastrada informando apenas o nome, sem herdar de CNPq/CAPES/Institucional/etc.
- A prorrogação mantém o vínculo, a nova bolsa cria outra ocorrência; o sincronizador institucional mantém precedência se receber explicitamente um novo tipo legado.
- **DEMO restaurada:** `DemoDataInitializer` semeia 6 estagiários ativos, cada um com vínculo e participação aberta, e 2 encerrados. Exclusivo de `@Profile("demo")`, sem dados fictícios em produção.
- Testes adicionados: `BolsaServiceTest`, `VinculoEstagioBolsaResponseTest`, `BolsaPicker.spec.ts`, cobertura de nova bolsa sem enum em serviços de vínculo/cadastro e `DemoDataInitializerIntegrationTest`.
- Não integra a futura proposta de currículo acadêmico nem altera o hotfix validado de produtos.

## Versionamento Flyway

A `release` atual termina em V48. Nesta integração existem exatamente:
- **V49** — `V49__descricao_produto_texto_e_unidade_cadastro.sql` — produtos.
- **V50** — `V50__bolsas_independentes_por_unidade.sql` — bolsas independentes, ajuste de nulidade legado e regra de exclusividade.

**Não executar/mesclar a V49 de bolsas da branch antiga.** Em um banco persistente que já tenha executado `V49__modalidades_bolsa_por_unidade.sql` **ou a V50 anterior** (`V50__modalidades_bolsa_por_unidade.sql`), não iniciar esta integração diretamente: é necessário um plano de migração/reconciliação e uma versão nova; versões Flyway executadas não podem ser simplesmente renomeadas sem plano de migração. A validação na DEMO/H2 não testa essas migrations, porque Flyway está desabilitado no perfil.

## Sequência para homologação local

1. Confirmar `git status` limpo em ambos os repositórios.
2. Executar `git fetch github --prune` e mudar para `collab/integracao-produtos-bolsas-2026-11` **nos dois projetos**.
3. Backend: `./mvnw clean verify` (no Windows: `mvnw.cmd clean verify`).
4. Frontend: `npm ci`, `npm run build`, `npm run test`.
5. Revalidar rapidamente o aceite de Produtos: descrição BDA longa e exibição de produto ativo sem estoque.
6. Cadastrar bolsa FAPESB somente com nome: **nenhum tipo legado obrigatório**; conferir reutilização, edição, inativação/reativação e prevenção de nomes duplicados.
7. Registrar nova ocorrência usando bolsa independente (`tipoBolsa = null`, `bolsaId` preenchido) e conferir data de corte, atividades, orientador, curso e treinamento; preservar nome histórico ao renomear/inativar catálogo.
8. Confirmar recusa de bolsa inativa ou de outra Unidade, e recusa de requisição sem origem ou com ambas; validar também opção legada sem `bolsaId`.
9. Testar prorrogação, edição do vínculo e sincronização institucional; nenhum fluxo deve exigir enum quando vínculo usa bolsa independente.
10. Reiniciar a DEMO/H2 e confirmar seis estagiários ativos com vínculo/participação e dois encerrados. Conferir também responsividade das telas do estagiário, inclusive no mobile.
11. Em DEV PostgreSQL com backup, confirmar aplicação de V49 + V50 em sequência e `spring.jpa.hibernate.ddl-auto=validate`.
12. Conferir que dados existentes não foram apagados e que catálogo, vínculos anteriores, pedidos e relatórios continuam íntegros.

**Limite da validação disponível nesta preparação:** os arquivos foram revisados e sincronizados no GitHub; **não foi executado Maven/Vitest nem aplicado Flyway em PostgreSQL** por este assistente. O aceite funcional de produtos foi comunicado pelo responsável. Não afirmar homologação de bolsas até receber os testes.

## Liberação posterior

Depois da homologação do responsável, abrir/usar MRs da integração para a `release` canônica do GitLab, **um por repositório**, e publicar tags coordenadas do backend e frontend. Fazer backup real de produção antes das migrations. Os PRs antigos de produtos (#37 backend e #59 frontend, no GitHub) não devem ser mesclados separadamente caso a integração consolidada seja aprovada.

## Proposta separada

Histórico acadêmico por pessoa, ainda apenas documental:
`collab/docs-modelagem-formacoes-academicas-2026-11` — `docs/PROPOSTA_HISTORICO_FORMACOES_ACADEMICAS.md`.
