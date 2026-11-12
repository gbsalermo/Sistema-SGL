# Dossiê técnico do SGL — Backend

**Revisão:** 12/11/2026  
**Repositório:** `gbsalermo/Sistema-SGL` — espelho GitHub do backend institucional `sgl-api`  
**Base:** árvore e arquivos selecionados da `release` espelhada; nenhum código funcional modificado  
**Objetivo:** localizar **controller → DTO → service → repository → model → migration → teste** e registrar riscos de alteração antes de qualquer implementação.

> **Precisão:** a existência de um arquivo, rota ou teste não significa execução/homologação de todos os cenários. Esta revisão confirmou pontos concretos de autenticação, tenant, pedidos, soluções, vínculos, migrations e exportações. Outros domínios têm **mapa estrutural verificado**, mas regras e métodos ainda exigem inspeção detalhada. O [dossiê legado completo](HISTORICO_DOSSIE_BACKEND_ATE_2026-11-12.md) foi preservado para consulta histórica. [Governança documental](GOVERNANCA_DOCUMENTAL.md).

## 1. Stack e arquitetura real

`pom.xml` declara **Java 17**, **Spring Boot 4.1.0**, Spring Web MVC, Spring Security, Spring Data JPA, Bean Validation, OAuth2 Client, JWT (JJWT), Springdoc OpenAPI, Flyway, PostgreSQL, H2, Apache POI, OpenPDF, JUnit/Spring Boot Test e ArchUnit. Versões transitivas e execução devem ser verificadas no Maven.

```text
src/main/java/com/sgl/
  controller/          endpoints REST e parâmetros
  dto/request/         payloads recebidos e validações
  dto/response/        respostas sem expor diretamente entidades
  service/             regras, transações e orquestração
  repository/          consultas e persistência JPA
  model/               entidades e enums de domínio
  security/            JWT, usuário atual, políticas de perfil/orientador
  tenant/              resolução de Unidade, TenantContext e filtro
  config/              security chain, OpenAPI, propriedades
  exception/           erros e tratamento de falhas
src/main/resources/
  db/migration/        scripts Flyway versionados
  application*.yml     configuração por ambiente, se presente
src/test/java/com/sgl/
  controller/          contratos e comportamento HTTP
  service/             casos de uso e regressões
  security/            autenticação e autorização
  architecture/        restrições de camadas
```

**Sequência de análise:** iniciar no controller e seu `@RequestMapping`; localizar DTO e método do service; inspecionar `CurrentUser`, `PerfilPolicy`, tenant, transação e repositories; conferir entidade/migration; procurar testes que exercitam casos positivos, negativos e multiunidade. Não inventar novos endpoints ou migrations sem confirmar contratos existentes.

## 2. Segurança, autenticação e Unidade — verificação direta

| Ponto | Arquivo | Evidência observada na `release` |
|---|---|---|
| Autenticação | `controller/AuthController.java` | `POST /api/v1/auth/login`, `GET /api/v1/auth/me`, `GET /api/v1/auth/config`; descrição do login corporativo via serviço |
| Login DEV | `controller/DevAuthController.java` | `POST /api/v1/auth/dev-login` somente com `@Profile({"dev","demo"})`; rota ausente fora desses perfis |
| Regra HTTP | `config/ApiSecurityConfig.java` | Sessão stateless, filtro JWT, autorização por authorities/perfis, CORS configurado, `/v3/api-docs/**` e Swagger liberados |
| JWT e identidade | `security/JwtAuthenticationFilter.java` | Bearer token validado; usuário recarregado do banco em cada chamada; contas inativas não autenticam; pendentes sem authority `SITUACAO_ATIVO` |
| Usuário atual | `security/CurrentUser.java` | `exigirProprioUsuario(UUID)` impede que body identifique outro usuário; `unidadeEmOperacao()` exige tenant |
| Unidade | `tenant/TenantRequestFilter.java` | Unidade derivada da sessão; `X-SGL-Unidade-Id` é seleção excepcional para `ADMINISTRADOR_GERAL`; divergência de outros perfis é recusada |
| Política de perfis | `security/PerfilPolicy.java` | Gestão: `GESTOR`, `ADMINISTRADOR`, `ADMINISTRADOR_GERAL`; admin: últimos dois; concessão de perfis centralizada |
| OpenAPI | `config/OpenApiConfig.java` | Metadata da API v1; contratos efetivos dependem das anotações e DTOs dos controllers |

**Correção importante do legado:** não tratar mais a autenticação como sessão DEV puramente controlada pelo frontend nem o header de Unidade como autoridade de tenant para todos. A implementação da `release` usa JWT e deriva a Unidade do usuário autenticado, com exceção controlada para administrador geral. Isso **não comprova** integração corporativa operacional em todos os ambientes, nem substitui testes de segurança.

**Pontos de atenção:** `AuthController` delega validação de credenciais ao `AuthService`; verificar integração, fallback e configuração de ambiente antes de afirmar comportamento de produção. A configuração da chain, políticas nos services e testes precisam ser analisadas em conjunto; anotação de rota sozinha não garante autorização por objeto. Não documentar secrets, tokens, senhas nem dados institucionais sensíveis.

## 3. Mapa de manutenção por domínio

**Prefixo dos arquivos Java:** `src/main/java/com/sgl/`. Os nomes abaixo foram verificados na árvore da `release`.

| Domínio | Controllers / entradas | Services / regras e dependências |
|---|---|---|
| Auth e sessão | `controller/AuthController.java`, `DevAuthController.java` | `service/AuthService.java`, `security/JwtService.java`, `config/ApiSecurityConfig.java`, `tenant/TenantRequestFilter.java` |
| Usuários, perfis, unidade | `controller/UsuarioController.java`, `UnidadeController.java`, `LaboratorioController.java` | `service/UsuarioService.java`, `UnidadeService.java`, `LaboratorioService.java`, `security/PerfilPolicy.java`, `service/LaboratoriosDaPessoa.java` |
| Pessoas corporativas e habilitação | `controller/PessoaCorporativaController.java`, `VinculoOperacionalUsuarioController.java`, `CargoSubstituicaoController.java` | `service/PessoaCorporativaService.java`, `VinculoOperacionalUsuarioService.java`, `CargoSubstituicaoService.java`, `DesativacaoCorporativaService.java`, `MudancaLotacaoService.java` |
| Projetos, SCI, atividades e SEG | `controller/ProjetoController.java`, `SciController.java`, `AtividadeController.java` | `service/ProjetoService.java`, `SciService.java`, `AtividadeService.java`, `CodigoSegValidator.java`, `CorrecaoCodigoSegService.java`, `ProrrogacaoService.java` |
| Estagiários e estágio | `controller/EstagiarioController.java`, `VinculoEstagioController.java`, `VinculoEstagioAtividadeController.java`, `ObservacaoVinculoEstagioController.java` | `service/EstagiarioService.java`, `VinculoEstagioService.java`, `VinculoEstagioAtividadeService.java`, `SincronizacaoVinculoEstagioService.java`, `ElegibilidadeContextoOperacional.java` |
| Contextos operacionais | `controller/ContextoOperacionalController.java` | `service/ContextoOperacionalConsultaService.java`, `ElegibilidadeContextoOperacional.java`, `LaboratoriosDaPessoa.java` |
| Pedidos | `controller/PedidoController.java` | `service/PedidoService.java`; `repository/PedidoRepository.java`, `ItemPedidoRepository.java`; verificar autorizações, estoque e histórico |
| Soluções | `controller/SolucoesController.java`, `ModeloSolucaoController.java` | `service/PedidoService.java` **compartilhado com Pedidos** para solicitações; `ModeloSolucaoService.java` para catálogo |
| Estoque e lotes | `controller/EstoqueCentralController.java`, `LoteController.java`, `MovimentacaoEstoqueController.java` | `service/EstoqueCentralService.java`, `LoteService.java`, `MovimentacaoEstoqueService.java`; `repository/RecipienteEstoqueRepository.java`, `MovimentacaoRecipienteRepository.java` |
| Produtos | `controller/ProdutoController.java` | `service/ProdutoService.java`, `repository/ProdutoRepository.java`; catálogo, risco, unidade e conversão |
| Resíduos | `controller/ResiduoController.java`, `ModeloResiduoController.java`, `ClasseResiduoController.java`, `LocalArmazenamentoResiduoController.java` | `service/ResiduoService.java`, `ModeloResiduoService.java`, `ClasseResiduoService.java`; `repository/HistoricoResiduoRepository.java` |
| Relatórios | `controller/RelatorioController.java`, `RelatorioExportacaoController.java`, `RelatorioProjetosController.java`, `RelatorioMovimentacoesController.java`, `RelatorioResumoOperacionalController.java` e demais `Relatorio*Controller.java` | `service/RelatorioService.java`, `RelatorioExportacaoService.java`, `RelatorioProjetosService.java`, `RelatorioMovimentacoesService.java`, `RelatorioResumoOperacionalService.java` e especializados |

**Evitar duplicação:** `PedidoService` é a orquestração real tanto para pedidos de materiais quanto para pedidos de soluções. Não criar um service paralelo de solução apenas porque a rota tem outro prefixo. A gestão de contexto e a política de perfis também têm implementações compartilhadas.

## 4. Endpoints e contratos observados

| Prefixo / ação | Controller | Confirmação |
|---|---|---|
| `/api/v1/auth/login`, `/me`, `/config` | `AuthController` | Endpoints anotados e delegação a `AuthService` |
| `/api/v1/auth/dev-login` | `DevAuthController` | Exclusivo `dev/demo` |
| `/api/v1/pedidos` | `PedidoController` | Criar, listar, consultar, filtrar e aprovar/rejeitar/entregar/cancelar |
| `/api/v1/solucoes/pedidos` | `SolucoesController` | Criar, listar, consultar, aprovar, entregar, cancelar, rejeitar; tipo `SOLUCAO` exigido na criação |
| `/api/v1/vinculos-estagio` | `VinculoEstagioController` | Criar/atualizar vínculos, referência de bolsa, prorrogação, nova bolsa, sincronização e vínculo institucional |
| `/api/v1/relatorios/*/exportar` | `RelatorioExportacaoController` | Rotas de estagiários, produtos, movimentações, resumo operacional, estoque/lotes, fiscalização e projetos |

**Observação:** esta tabela é um índice de rotas **inspecionadas**, não uma reprodução completa do OpenAPI. Para método, parâmetros, payload, retorno e códigos de erro, usar controller, DTO e Swagger da instância correta. `docs/ENDPOINTS_INTERNOS.md` é catálogo de apoio, não contrato exclusivo.

## 5. Regras críticas e dependências de negócio

### 5.1 Projeto → SCI → Atividade

O fluxo exige atenção à hierarquia, laboratório(s) vinculado(s), código SEG, vigência e prorrogações. Para alterações, começar por `ProjetoService`, `SciService`, `AtividadeService`, `CodigoSegValidator`, DTOs, repositórios e migrations V19–V24. **Não inferir** que um vínculo a Projeto equivale automaticamente a uma autorização irrestrita para todo laboratório.

### 5.2 Estagiários multilaboratório e vínculos

`V47__usuario_multi_laboratorio.sql` cria `usuario_laboratorios`, migra o antigo laboratório-base, incorpora laboratórios de projetos com participação ativa de estagiários e converte vínculos operacionais genéricos abertos em registros de laboratório da pessoa, preservando histórico do vínculo encerrado. `V48__laboratorio_fora_do_solicitante.sql` adiciona indicador em pedidos e resíduos para solicitações fora dos laboratórios da pessoa.

**Distinção relevante:** a migração V48 documenta liberdade de empregados para solicitar em laboratórios ativos de sua Unidade; não extrapolar isso para estagiários. Para estagiários, verificar `ElegibilidadeContextoOperacional`, vínculos, laboratórios permitidos, hierarquia e `PedidoService`. **A migração não demonstra sozinha que todos os cenários estão testados.**

### 5.3 Pedidos e soluções

`PedidoController` e `SolucoesController` usam `PedidoService`. `SolucoesController` exige `TipoPedido.SOLUCAO` ao criar. Conferir regras de aprovação, entrega, rejeição, cancelamento, disponibilidade e liberação de estoque no service, incluindo estados de preparação de soluções. Não supor “solicitar correção” como endpoint específico sem verificar contrato.

### 5.4 Estoque, recipientes e FEFO/FIFO

A implementação possui `LoteService`, `EstoqueCentralService`, repositórios de `RecipienteEstoque` e `MovimentacaoRecipiente`; migrations V33–V36 documentam normalização de quantidades, recipientes, movimentos e ajustes. **Preservar invariantes:** unidades compatíveis, seleção FEFO/FIFO, fracionamento, evitar abertura indiscriminada de recipientes, saldo não negativo e rastreabilidade por lote/recipiente. As condições exatas precisam ser verificadas no método antes de propor correção.

### 5.5 Resíduos e rótulos

`ResiduoService` é o núcleo; modelos, classes, locais, componentes e histórico têm controllers/services próprios. Para rótulos de produto/resíduo/solução e documento de entrada, validar campos e dados fornecidos pelo backend, além do ponto de acionamento no frontend. O backend não deve incorporar dependência de impressão Zebra do navegador.

### 5.6 Relatórios e exportação

`RelatorioExportacaoController` expõe exportação em formatos definidos por `FormatoExportacaoRelatorio`; `RelatorioExportacaoService` e serviços especializados geram conteúdo. O `pom.xml` inclui Apache POI e OpenPDF. Validar filtros, escopo de Unidade, resultados vazios, formatos e segurança dos arquivos; não usar o checkpoint de pré-merge de outubro como estado vigente.

## 6. Banco, Flyway e ambientes

A árvore da `release` contém **48 arquivos de migrations SQL versionadas**, incluindo `V47` e `V48`. **Contagem de arquivos não prova que todas foram aplicadas em uma instância**: verificar `flyway_schema_history`, perfil, datasource e logs do ambiente-alvo. Não editar migrations já aplicadas; criar nova versão para evolução compatível, com migração de dados, rollback operacional e testes de integridade.

O projeto declara PostgreSQL e H2. `dev/demo` possuem funcionalidades próprias (ex.: `DevAuthController`, inicializadores de massa em `src/main/java/com/sgl/test/`); não habilitar seed/demo ou login sem senha em produção. Credenciais e URLs institucionais sensíveis não devem entrar em docs.

## 7. Testes e critérios de aceite

A árvore contém testes de controllers, services, segurança, integração e arquitetura. Exemplos confirmados: `src/test/java/com/sgl/architecture/SglArchitectureTest.java`, `security/SecurityIntegrationTest.java`, `security/AutorizacaoGravacoesIntegrationTest.java`, `security/PerfilPolicyTest.java`, `security/JwtServiceTest.java`, `controller/AuthControllerIntegrationTest.java`, `controller/PedidoControllerTest.java`, `controller/LoteControllerTest.java`, `controller/RelatorioExportacaoControllerTest.java`.

```bash
./mvnw test
./mvnw verify
```

No Windows, usar `mvnw.cmd` quando aplicável. Comandos são **orientações de execução**, não evidência de que a suíte foi executada nesta auditoria.

**Aceite mínimo por alteração:** (1) compilar e executar testes do módulo; (2) testes de autorização por perfil e Unidade, incluindo negações; (3) migrações aplicadas em banco de teste; (4) regressões de estoque, vínculos, pedidos e histórico; (5) contrato frontend-backend; (6) documentação proprietária atualizada; (7) MR revisado. Carga, concorrência, E2E e segurança exigem evidências próprias; `ETAPA_12_PLANO_QUALIDADE_ARQUITETURA_CARGA_E2E.md` é plano, não aceite.

## 8. Política de implementação, colaboração e documentação

**Regra vigente de colaboração:** mudanças funcionais do backend são aplicadas **manualmente pelo mantenedor**, salvo autorização explícita para outro procedimento. O assistente pode inspecionar código real, indicar arquivo/método e propor alterações pequenas, testes e validação; **esta tarefa altera apenas Markdown de documentação**.

GitLab `release` é a branch canônica operacional; GitHub `release` é espelho; `main` é trilha distinta. Criar `collab/*` baseada na `release`, publicar, aguardar sincronização, abrir MR **manual** no GitLab para `release`. Não usar `--force` ou merge arbitrário entre `main` e `release`. Procedimento em `SINCRONIZACAO_GITLAB_GITHUB.md`.

**Autoridade dos documentos:** este dossiê é o **mapa de manutenção**; `GUIA_ESTRUTURAL.md` descreve arquitetura; `FLUXO_DO_SISTEMA.md` descreve jornadas; `CONTINUIDADE.md` (raiz) é checkpoint; `GOVERNANCA_DOCUMENTAL.md` resolve redundâncias; `docs/README.md` será o índice após o fechamento. Histórico integral em [`HISTORICO_DOSSIE_BACKEND_ATE_2026-11-12.md`](HISTORICO_DOSSIE_BACKEND_ATE_2026-11-12.md).

## 9. Pendências explícitas de homologação

- [x] Inventariar arquitetura, stack, módulos, migrations e testes existentes.
- [x] Verificar diretamente autenticação JWT, login DEV restrito, filtro de Unidade, perfis e rotas críticas.
- [x] Distinguir fluxo de Pedidos de Soluções sem duplicar `PedidoService`.
- [x] Registrar V47/V48 e seus efeitos declarados no SQL.
- [x] Preservar o dossiê antigo integralmente.
- [ ] Conferir cada DTO, regra de autorização por objeto e transação dos serviços críticos.
- [ ] Executar Maven test/verify e integração PostgreSQL/Flyway com evidências.
- [ ] Testar matrizes de perfil/Unidade, multi-laboratório, concorrência e FEFO/FIFO.
- [ ] Validar todos os endpoints/documentação OpenAPI e links internos.
- [ ] Validar comportamento real com o mantenedor e integrar via MR.

**Estado:** dossiê técnico do backend **consolidado a partir da `release`**, com limitações e pendências de homologação explícitas. Não confundir documentação técnica com aprovação operacional ou de segurança.
