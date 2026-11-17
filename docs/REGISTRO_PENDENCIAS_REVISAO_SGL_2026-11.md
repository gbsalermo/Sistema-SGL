# Registro único de pendências pós-revisão — SGL

**Aberto:** 17/11/2026 · **Situação:** backlog de investigação, correção e homologação **a executar depois do fechamento documental e da sincronização Git**.

**Cobertura:** `gbsalermo/Sistema-SGL` (backend) e `gbsalermo/SGL-FRONTEND` (frontend), ambos comparados à branch `release` espelhada no GitHub. Este é o **documento proprietário das pendências**; não duplicar a lista em dossiês, inventários ou READMEs. A fonte detalhada de evidências é [`MATRIZ_COERENCIA_FRONTEND_BACKEND_2026-11-12.md`](MATRIZ_COERENCIA_FRONTEND_BACKEND_2026-11-12.md), seções C1–C14. Inventários e dossiês continuam sendo, respectivamente, **auditoria de documentos** e **mapas de manutenção**.

> **IMPORTANTE:** “risco observado em análise estática” não é falha explorada nem incidente de produção. “Validação pendente” não é defeito confirmado. Nenhum teste de segurança, carga ou impressão foi executado por esta revisão documental. Não aplicar correções funcionais nesta etapa; o mantenedor aplica o backend manualmente, salvo autorização explícita. **Não modificar a Central de Ajuda, vídeos, posters, associações, scripts ou arquivos publicados** sem aprovação específica.

## Como manter este registro

- **Prioridade:** **P1** = investigar antes de homologação de segurança/produção por possível impacto de acesso a dados; **P2** = relevante para correção funcional/documental ou validação operacional; **P3** = otimização/limpeza controlada. Prioridade não indica incidente comprovado.
- **Natureza:** **RISCO** (inferido do fluxo estático, depende de teste), **DIVERGÊNCIA** (conteúdo/contrato antigo versus implementação), **DECISÃO** (requisito não confirmado) ou **VALIDAÇÃO** (falta evidência operacional).
- **Estado inicial:** `ABERTO`; ao trabalhar, marcar `EM ANÁLISE`, `DECIDIDO`, `CORRIGIDO EM COLLAB`, `VALIDADO EM TESTE`, `INTEGRADO EM RELEASE` ou `DESCARTADO COM JUSTIFICATIVA`. **Não encerrar por “existe teste” sem resultado.**
- **Para encerrar:** registrar decisão de negócio, commit/MR, teste (nome, ambiente, data e resultado) e confirmação do mantenedor. Para itens que exigem escolha de política, não propor alteração de regra sem ratificação.

## Painel priorizado

| ID | Prioridade | Tipo | Tema | Situação | Referência |
|---|---|---|---|---|---|
| SGL-REV-001 | P1 | RISCO | Leitura de pedidos de terceiros na mesma Unidade | ABERTO | C2 |
| SGL-REV-002 | P1 | RISCO | Leitura de participações/contextos de outros estagiários | ABERTO | C2 |
| SGL-REV-003 | P1 | RISCO | Consulta a vínculos operacionais de terceiros | ABERTO | C2 |
| SGL-REV-004 | P1 | RISCO | Consultas e históricos de resíduos de terceiros | ABERTO | C6 |
| SGL-REV-005 | P2 | DIVERGÊNCIA | `GET` de rótulo de resíduo pode gravar identificação legada | ABERTO | C6 |
| SGL-REV-006 | P2 | DIVERGÊNCIA | Ajuda geral atribui a estagiário escolha irrestrita de laboratório | ABERTO | C3 |
| SGL-REV-007 | P2 | DECISÃO | “Solicitar correção” de pedido citado em requisitos, sem fluxo próprio na release | ABERTO | Contrato de Pedido |
| SGL-REV-008 | P2 | DIVERGÊNCIA | Guias antigos de Postman/teste com IDs e contratos superados | ABERTO | Docs históricos |
| SGL-REV-009 | P1 | VALIDAÇÃO | Suíte atual e cobertura de segurança horizontal sem evidência executada | ABERTO | C5 |
| SGL-REV-010 | P2 | VALIDAÇÃO | FEFO/FIFO, recipientes e concorrência em PostgreSQL real | ABERTO | C7 |
| SGL-REV-011 | P2 | VALIDAÇÃO | Etiquetas A4/Zebra e documento de entrada na impressora | ABERTO | C8 |
| SGL-REV-012 | P2 | VALIDAÇÃO | Disponibilidade/entrega dos vídeos da Central de Ajuda no deploy | ABERTO | Preservação de mídia |
| SGL-REV-013 | P3 | RISCO | Fallback de consulta por produto potencialmente N+1 em pedidos | ABERTO | C4 |
| SGL-REV-014 | P2 | DIVERGÊNCIA | Comentários DEV e documentos com `main`/pré-merge como estado vigente | ABERTO | Governança |
| SGL-REV-015 | P2 | VALIDAÇÃO | Filtros e consistência da prévia versus PDF/XLSX | ABERTO | C1, Relatórios |
| SGL-REV-016 | P2 | VALIDAÇÃO | Login corporativo, perfis, Unidade e isolamento no ambiente publicado | ABERTO | Segurança |
| SGL-REV-017 | P2 | VALIDAÇÃO | E2E, desempenho, carga, falhas de dependência e implantação | ABERTO | Etapa 12 |

| SGL-REV-018 | P1 | RISCO | GET de Usuários expõe diretório completo da Unidade para usuário ativo | ABERTO | C9 |
| SGL-REV-019 | P1 | RISCO | GET de Estagiários retorna detalhes de vínculos sem Gestão explícita | ABERTO | C9 |
| SGL-REV-020 | P1 | RISCO | Fallback `findAll` de Usuários quando não há tenant | ABERTO | C9 |
| SGL-REV-021 | P2 | DIVERGÊNCIA | Contratos históricos de vínculos e Soluções incompatíveis com release | ABERTO | C10 |

| SGL-REV-022 | P2 | VALIDAÇÃO | Origem e execução real dos eventos de sincronização institucional do estágio | ABERTO | C11 |

| SGL-REV-023 | P2 | VALIDAÇÃO | Testes Zebra em Node fora da suíte padrão `npm run test` | ABERTO | C12 |

| SGL-REV-024 | P2 | VALIDAÇÃO | Perfil test usa H2 e não executa Flyway; falta homologação das migrations reais | ABERTO | C14 |

## Fichas de evidência e critérios de correção

### SGL-REV-001 — Visibilidade de Pedidos (P1 · RISCO)

**Código:** backend `config/ApiSecurityConfig.java` (GET em geral requer `SITUACAO_ATIVO`); `service/PedidoService.java`, métodos `listarTodos`, `buscarPorId`, `listarPorUsuario`, `listarPorStatus`, `listarPorUrgencia`. `listarPorUsuario(UUID)` filtra Unidade e ID solicitado, mas não verificou `CurrentUser.exigirProprioUsuario` no método. O frontend usa `pedidoService.ts` e mostra views por perfil, **não substituindo autorização server-side**.

**Investigar:** se um solicitante autenticado consegue consultar pedidos de outro usuário da mesma Unidade e quais campos poderia receber; a visão de Gestão/orientandos precisa continuar funcional.

**Critério de aceite:** matriz de leitura para solicitante, orientador, Gestão e administração ratificada; testes reais positivos/negativos com **dois usuários da mesma Unidade e pedidos distintos**, além de duas Unidades; correção pontual no backend **após** decisão de visibilidade.

### SGL-REV-002 — Contextos e participações de estágio (P1 · RISCO)

**Código:** `controller/ContextoOperacionalController.java`, `service/ContextoOperacionalConsultaService.java` (`listarDoEstagiario`, `listarDaAtividade`, `listarDoProjeto`). A implementação conferida restringe por Unidade, sem validação explícita de titularidade/gestor/orientador nesses métodos. DTO inclui nome de usuário, atividade, Projeto e vínculos.

**Critério de aceite:** regras de quem pode ver lista pessoal e participantes por Atividade/Projeto; testes de estagiário A consultando estagiário B da mesma Unidade, orientador elegível e Gestão; negativa quando acesso não autorizado.

### SGL-REV-003 — Vínculos operacionais de terceiros (P1 · RISCO)

**Código:** `service/VinculoOperacionalUsuarioService.java`, `resumo` e `listar(UUID)` filtram Unidade, mas não exigem perfil/titularidade nas leituras inspecionadas; escritas já exigem Gestão e impedem autoalteração.

**Critério de aceite:** restringir conforme política aprovada sem quebrar seleção legítima de vínculos nos formulários de Pedidos/Soluções, listagem administrativa e orientandos; testes de leitura por solicitante/gestor na mesma Unidade.

### SGL-REV-004 — Resíduos e históricos (P1 · RISCO)

**Código:** `service/ResiduoService.java`, `buscarPorId`, `listarTodos`, `listarPorStatus`, `listarPorGerador`, `buscarHistorico` e consultas de Unidade. Filtro tenant foi localizado; permissão por titular/perfil/vínculo não aparece nos métodos de leitura inspecionados.

**Critério de aceite:** decidir visibilidade por papel, gerador, Gestão, histórico e rótulos; testar leitura de resíduos de terceiros na mesma Unidade e acesso entre Unidades; preservar fluxo legítimo de coleta e liberação.

### SGL-REV-005 — GET com efeito colateral no rótulo de resíduo (P2 · DIVERGÊNCIA)

**Código:** `ResiduoController.java` → `ResiduoService.gerarDadosRotulo(UUID)` possui `assegurarIdentificacaoRotulo` seguido de `residuoRepository.save` condicional em registro antigo.

**Decisão:** manter backfill controlado ao consultar ou migrar a regularização para procedimento explícito? **Não remover** antes de medir impacto nos resíduos legados e código SGL.

**Aceite:** testes de compatibilidade, idempotência (duas consultas), concorrência, permissão e auditoria sem regeneração indevida.

### SGL-REV-006 — Ajuda de laboratórios e Projeto para estagiários (P2 · DIVERGÊNCIA)

**Evidência:** frontend `src/modules/ajuda/textos/pedido-em-qualquer-laboratorio.md` declara empregados **e estagiários** aptos a pedir a qualquer laboratório ativo da Unidade. `pedido-do-estagiario.md` e `PedidoService.resolverContextoPedido` exigem participação elegível, Projeto derivado e laboratório simultaneamente associado ao Projeto e à pessoa. `fazer-um-pedido-e-acompanhar.md` e `solicitar-uma-solucao.md` apresentam Projeto opcional sem ressalva de estagiário.

**Aceite:** validar texto e vídeo de cada tarefa, preparar proposta editorial por perfil e aprovar atualização textual **separadamente**, sem editar vídeos, player, posters, IDs, ordem ou rotas. Não alterar durante a revisão atual.

### SGL-REV-007 — Ação de solicitar correção de pedido (P2 · DECISÃO)

**Evidência:** o requisito apareceu em orientações operacionais anteriores. A `release` define `StatusPedido` como `PENDENTE`, `APROVADO`, `EM_PREPARACAO`, `REJEITADO`, `ENTREGUE`, `CANCELADO`; `PedidoController` expõe aprovar, rejeitar, entregar e cancelar, **sem endpoint dedicado de solicitação de correção**. `PedidosGestaoView.vue` não apresenta essa ação no fluxo identificado.

**Decisão do mantenedor:** implementar retorno para correção, reapresentação e histórico, ou considerar requisito substituído por outro mecanismo? **Não inventar status** nem ajustar tutorial sem decisão.

**Aceite, caso aprovado:** contrato/status e auditoria de eventos, permissões, testes, experiência em Meus Pedidos e gestão, comunicação e atualização dos tutoriais com preservação de vídeos.

### SGL-REV-008 — Roteiros Postman e teste legados (P2 · DIVERGÊNCIA)

**Evidência:** `docs/testes.md` fala de validação “antes da migração para PostgreSQL”, sugere `cd backend/sgl-backend` e exemplos de requisições sem JWT. `docs/REQUISICOES_POSTMAN_LOTES.md` utiliza IDs numéricos (`usuarioId: 2`, `laboratorioId: 1`, `produtoId: 3`) enquanto DTOs atuais usam **UUID público**, e a criação de pedido exige forma de retirada completa ou unidade física. Há exemplos com validade em agosto/2026 que não podem funcionar como lote válido em novembro.

**Aceite:** reescrever ou gerar coleção Postman atual **depois de conferir contratos e autorização**, usando placeholders UUID, Bearer token de teste, dados sintéticos e ambiente isolado; não executar scripts antigos contra produção.

### SGL-REV-009 — Execução de testes, cobertura horizontal e evidência (P1 · VALIDAÇÃO)

**Evidência existente, não executada nesta auditoria:** `security/SecurityIntegrationTest.java`, `AutorizacaoGravacoesIntegrationTest.java`, `service/PedidoContextoOperacionalServiceTest.java`, `PedidoEstoqueFisicoIntegrationTest.java`, `PedidoSolucaoServiceTest.java`. Foram observados testes de escritas e isolamento entre Unidades, mas **não foi comprovada** cobertura de negar leitura de terceiros **dentro** da mesma Unidade.

**Aceite:** rodar `./mvnw verify`, `npm run type-check`, `npm run test`, `npm run build` com hashes/ambiente/resultado; acrescentar cenário de autorização horizontal e testes que falhem antes da correção e passem depois. Sem execução, continuar `ABERTO`.

### SGL-REV-010 — FEFO/FIFO e concorrência (P2 · VALIDAÇÃO)

**Evidência:** `MovimentacaoEstoqueService.buscarLotesParaSaida` distingue FEFO/FIFO e `consumirRecipientes` prioriza abertos para frações. Há unitários e integração para combinações físicas, baixa única, restauração e conflitos; detalhes na matriz C7.

**Aceite:** executar testes com PostgreSQL/Flyway e massa sintética, incluindo concorrência de pedidos, locks por estoque/lote/recipiente, abertura mínima, quantitativos fracionados grandes e cancelamentos após movimentação subsequente. Não refatorar algoritmo apenas por aparência.

### SGL-REV-011 — Impressão física e rótulos (P2 · VALIDAÇÃO)

**Evidência:** acesso a documento de entrada em `EstoqueDetalheView.vue` após entrada e no detalhe; `DocumentoEntradaLoteView.vue` tem A4/Zebra. `RotuloSolucaoView.vue` libera impressão em `EM_PREPARACAO`/`ENTREGUE`; Resíduo bloqueia antes de `impressaoPermitida`; Produto possui impressão. **Gatilhos foram encontrados.**

**Aceite:** amostras reais A4/Zebra e fluxo completo, dimensões, margens, logos, código/QR, cortes, informações de Projeto/SCI/Atividade e estados; não modificar impressora/código durante auditoria.

### SGL-REV-012 — Vídeos da Ajuda no deploy (P2 · VALIDAÇÃO)

**Evidência:** 20 IDs de vídeos em `tasks.json`; `helpTasks.ts` monta `/ajuda/videos/<id>.mp4`; há posters JPG e PDF no Git, mas **não há MP4 na árvore consultada**. Publicação separada é possível e não foi verificada.

**Aceite:** confirmar no deploy todos os links de vídeo, MIME, status HTTP, tamanho, reprodução e fallback, registrando ambiente e origem do asset **sem substituir/remover vídeos ou alterar Central de Ajuda**. Ver [política de preservação do frontend](https://github.com/gbsalermo/SGL-FRONTEND/blob/collab/docs-release-estado-2026-11/docs/PRESERVACAO_CENTRAL_AJUDA_VIDEOS.md).

### SGL-REV-013 — Fallback de enriquecimento de pedidos (P3 · RISCO)

**Evidência:** `src/modules/pedidos/services/pedidoService.ts` chama `GET /v1/produtos/{id}` por produto ausente/incompleto, usando `Promise.allSettled`. O `ItemPedidoResponseDTO` atual já preenche atributos de produto. **Pode gerar requisições extras, mas não está provado que seja caminho frequente nem gargalo**.

**Aceite:** instrumentar volume de chamadas e duração em massa representativa; remover/otimizar fallback somente se houver redundância comprovada e teste de regressão para respostas legadas.

### SGL-REV-014 — Comentários e instruções históricas (P2 · DIVERGÊNCIA)

**Evidência:** `ContextoOperacionalController.java`, `ContextoOperacionalConsultaService.java` e partes antigas dos documentos ainda mencionam sessão DEV/header obrigatório para tenant; `TenantRequestFilter` hoje deriva Unidade da identidade autenticada. `EXPORTACAO_RELATORIOS.md` descreve Etapa 9 “pré-merge” e vários arquivos mencionam `main` canônica; produção documental atual usa `release`.

**Aceite:** preservar evidência histórica, marcar ou corrigir trechos normativos incompatíveis, atualizar procedimentos de testes e conferir links. Comentário de código só deve ser editado no fluxo autorizado para backend; não confundir correção textual com mudança de segurança.

### SGL-REV-015 — Relatórios: consistência e filtros (P2 · VALIDAÇÃO)

**Evidência:** frontend `relatorioService.ts` consulta prévias e exporta `PDF/XLSX`; controllers de exportação e specialized controllers foram localizados no backend.

**Aceite:** comparar dados e filtros de cada prévia versus PDF/XLSX, tipos, datas, unidades, campos de quantidade, vazios, links e nomes de arquivo; verificar desempenho com massa sintética e autorização por perfil, sem supor que `EXPORTACAO_RELATORIOS.md` datado já prova produção.

### SGL-REV-016 — Autenticação e implantação (P2 · VALIDAÇÃO)

**Evidência:** `AuthController` expõe login e `/auth/me`; `DevAuthController` é `@Profile(dev,demo)`; JWT e tenant têm configuração de segurança e testes. O frontend usa Bearer JWT e `X-SGL-Unidade-Id` somente para `ADMINISTRADOR_GERAL`.

**Aceite:** homologar configuração do ambiente correto, credenciais corporativas com massa autorizada, pendência, desativação, expiração, CORS e separação dev/demo/prod; não publicar segredos.

### SGL-REV-017 — Qualidade transversal (P2 · VALIDAÇÃO)

**Fonte:** `ETAPA_12_PLANO_QUALIDADE_ARQUITETURA_CARGA_E2E.md` explicitamente `PLANEJADO`.

**Aceite:** auditoria de consultas e índices, testes de carga/concorrência em ambiente isolado, E2E de fluxos, erros 4xx/5xx, recuperação, consistência de migração, observabilidade e validação do artefato publicado, com métricas e evidências. Evitar repetir problemas de outras aplicações sem medição própria no SGL.

### SGL-REV-018 — Direcionamento e minimização da consulta de Usuários (P1 · RISCO)

**Evidência:** `UsuarioController.java` tem `GET /api/v1/usuarios`, `/{id}` e `/por-laboratorio`; `UsuarioService.listarTodos`, `buscarPorId` e `listarPorLaboratorio` filtram por Unidade ou ID, mas **não exigem perfil de Gestão nem titularidade** nos trechos conferidos. `ApiSecurityConfig` permite leituras gerais para qualquer usuário com authority de ativo. `UsuarioResponseDTO` inclui e-mail, perfil, situação e laboratórios de terceiros. O frontend `estagiarioService.listarUsuarios` e `cadastrosAdminService.listarUsuarios` dependem da listagem para fluxos de Gestão/Admin.

**Critério de aceite:** decidir explicitamente quais campos podem constar de um diretório operacional e quem pode ver dados administrativos; testar GET autenticado de dois usuários ativos da mesma Unidade (perfis Estagiário, Técnico, Gestor, Admin) e de Unidades diferentes; garantir seletores/gestão após eventual API de resumo mínimo. **Não confundir tela restrita com rota protegida.**

### SGL-REV-019 — Listagem de Estagiários e seus vínculos (P1 · RISCO)

**Evidência:** `EstagiarioService.listarTodos`, `buscarPorId`, `listarAtivos` e `listarPorLaboratorio` verificam Unidade, mas não exigem Gestão. O DTO `EstagiarioResponseDTO` inclui `VinculoEstagioResponseDTO`: orientador, formação/curso, modalidade de bolsa, referência institucional e observações. Em contraste, `listarSemVinculo` e `listarOrientadoresElegiveis` **já exigem Gestão**. `EstagiariosGestaoView.vue` usa a listagem ampla, mas a UI não limita o endpoint.

**Critério de aceite:** acordar política de acesso ao histórico e campos de estágio, com visão própria, orientador e Gestão; testar pedidos diretos GET de usuários ativos distintos; avaliar DTO enxuto para seletores e DTO detalhado só para perfis autorizados; preservar funcionalidade atual de gestão.

### SGL-REV-020 — Fallback entre Unidades de Usuários sem tenant (P1 · RISCO CONDICIONAL)

**Evidência:** `UsuarioService.listarTodos` faz `TenantContext.unidadeAtual().map(usuarioRepository::findByUnidadePublicId).orElseGet(usuarioRepository::findAll)`. O comentário ainda atribui essa exceção a um antigo login DEV sem autenticação, mas o sistema atual usa JWT; a rota só requer usuário ativo. `TenantRequestFilter` usa a Unidade do JWT e não define tenant se o principal não tiver Unidade; `AuthenticatedUser.from` aceita nulo. O fluxo normal de login corporativo atribui Unidade habilitada, **porém a revisão não comprovou ausência de registros legados ATIVO sem Unidade**.

**Critério de aceite:** verificar invariantes e constraints do banco, autenticação de principal ATIVO sem Unidade, testes de negativa para `GET /api/v1/usuarios` nesse cenário; preferir comportamento fail-closed ou contrato explicitamente autorizado, com análise de compatibilidade do administrador geral. **Não afirmar vazamento reproduzido.**

### SGL-REV-021 — Contratos da Etapa 8 e estágio desatualizados (P2 · DIVERGÊNCIA)

**Evidência:** `docs/ETAPA_8_CONTRATO_VINCULOS_ACESSO.md` contém “Solução continua exigindo projeto” e restringe funcionários a laboratórios autorizados; a release e migrations V47/V48 evoluíram: empregado pode solicitar para laboratório ativo da Unidade com sinalização quando fora da lista, Projeto é opcional, Estagiário exige participação e autorização de laboratório do Projeto e da pessoa. `docs/CONTRATO_CONTEXTOS_PEDIDOS_ETAPA_8.md` ainda inicia com estado “não implementado”, apesar de adendo e código atual. `docs/DECISAO_CICLO_INSTITUCIONAL_ESTAGIO.md` é regra registrada de outubro e não certifica integração externa atual.

**Critério de aceite:** corrigir autoridade editorial sem perder decisões históricas, revisar referências cruzadas, confirmar regras de perfis em testes e código, e verificar tutoriais publicados separadamente **sem tocar em vídeos**. Marcar documento antigo como histórico e remeter ao dossiê e à matriz.

### SGL-REV-022 — Sincronização institucional efetiva dos vínculos (P2 · VALIDAÇÃO)

**Código verificado:** `VinculoEstagioController.java` expõe `POST /api/v1/vinculos-estagio/{vinculoId}/sincronizacoes-institucionais`; `SincronizacaoVinculoEstagioService.sincronizar` recebe evento, valida tenant/referências, trata idempotência e encerra participações quando o estágio é finalizado. Testes de service existem para prorrogação, finalização, replay, referência e tenant. **Isso demonstra mecanismo receptor e comportamento testável, não uma integração externa automática funcionando em produção.**

**Distinguir:** `AuthService.login` sincroniza identidade, Unidade/cargo e situação de acesso; `PessoaCorporativaService.importar` cria pessoa institucional por ação de Gestão; o evento de `VinculoEstagio` é um fluxo separado. Confundir esses três produz instruções operacionais enganosas.

**Verificação requerida:** identificar quem invoca o endpoint de sincronizações, se há job/webhook/integração ativa, origem/autorização do evento, tratamento de repetição, observabilidade e conformidade com políticas institucionais. Executar apenas em ambiente controlado com dados autorizados. Preservar fluxo local de prorrogação/nova bolsa. Atualizar `DECISAO_CICLO_INSTITUCIONAL_ESTAGIO.md` e manual somente após evidência de ambiente e aceite do mantenedor.

### SGL-REV-023 — Testes ZPL/BrowserPrint fora do npm run test (P2 · VALIDAÇÃO)

**Lacuna confirmada por configuração:** `gbsalermo/SGL-FRONTEND/package.json` executa `vitest run`, e `vitest.config.ts` inclui somente `src/**/__tests__/*.spec.ts`. Os arquivos `src/modules/rotulos/zebra/zpl.test.mjs` e `browserPrintLocal.test.mjs` usam `node:test` e estão fora desse padrão. Portanto, **`npm run test` não executa esses dois arquivos**, embora haja testes relevantes de conteúdo/segurança ZPL e transporte local.

**Procedimento de reprodução e aceite (após revisão):** executar os comandos indicados nos cabeçalhos dos arquivos, como `node --experimental-strip-types --test src/modules/rotulos/zebra/zpl.test.mjs` e o equivalente para `browserPrintLocal.test.mjs`, na versão Node compatível; registrar ambiente e resultado; adicionar esses comandos de modo explícito na rotina de testes/CI aprovada. **Isso não substitui teste com impressora real** (`SGL-REV-011`). O `npm run test` continua selecionando `helpTasks.spec.ts` e `helpAssets.spec.ts`, mas eles não verificam entrega dos vídeos no deploy (`SGL-REV-012`).

**Estado:** constatação de exclusão do padrão de testes, **não de falha dos casos Node**. Nenhum teste foi executado nesta auditoria e nenhum código/teste foi alterado.

### SGL-REV-024 — Migrations Flyway não verificadas pelo perfil test padrão (P2 · VALIDAÇÃO)

**Evidência técnica direta:** `src/main/resources/application-test.properties` configura H2 em memória e `spring.jpa.hibernate.ddl-auto=create-drop`, com `spring.flyway.enabled=false`. Os testes `PedidoConcorrenciaIntegrationTest` e `PedidoEstoqueFisicoIntegrationTest` usam `@ActiveProfiles("test")`. Há validações importantes de negócio e concorrência, mas **passar esses testes no H2 não demonstra a execução das migrations SQL ou equivalência com PostgreSQL de produção**. Não foi identificado nesta rodada resultado executado de testes sobre o schema real de PostgreSQL.

**Critério de aceite:** criar/executar um perfil isolado de integração com **PostgreSQL da versão suportada** (por exemplo, instância descartável/Testcontainers, se aprovado), aplicar Flyway **desde um banco vazio**, comparar schema/restrições/índices com JPA e executar cenários de concorrência, baixa física, autorização e migração de registros legados; adicionalmente verificar a evolução sobre cópia sanitizada representativa, sem dados institucionais reais. Registrar migrações aplicadas, tempos, exceções, logs, versão do banco e commit. Cobertura de concorrência sob PostgreSQL permanece vinculada a `SGL-REV-010` e critérios gerais de implantação a `SGL-REV-017`.

**Estado:** lacuna **confirmada na configuração do perfil test**, não falha ou corrupção de banco comprovada. Não alterar infraestrutura ou migrations durante esta auditoria documental.

## Resultados já confirmados — não reabrir como defeitos sem nova evidência

- **Rotas base de Pedidos, Soluções, Estagiários, Estoque, Resíduos e Relatórios** foram encontradas no frontend e nos controllers backend selecionados; correlação estática não prova autorização.
- **Pedidos de estagiário** exigem participação e laboratório permitido; **funcionários** podem solicitar a laboratório ativo da Unidade com sinalização de exceção. Isso é regra de implementação atual, não uma falha a “corrigir” indiscriminadamente.
- **Pontos de acesso para rótulos e documento de entrada estão implementados**; a pendência é teste físico e qualidade dos dados, não ausência de botão.
- **FEFO/FIFO, fracionamento e priorização de abertos** foram localizados no código, com testes nomeados para os casos principais; pendência é execução/homologação.
- **Migrações V47/V48 estão no repositório**, mas presença não comprova implantação em cada instância.

## Ordem proposta quando o mantenedor autorizar correções

1. **Definir política de visibilidade por perfil/titularidade** e tratar `001–004` com testes de autorização antes/depois; rodar `009`.
2. Resolver decisões funcionais/documentais `005–008`, com proteção dos vídeos e rotas publicadas.
3. Executar a validação integrada de estoque, impressão, relatórios, autenticação, carga e deployment `010–012`, `015–017`.
4. Medir o possível custo de `013` e corrigir somente se reproduzido; consolidar comentários e guias em `014`.
5. Registrar commits, MRs, aceites e dar baixa nos IDs. Em cada alteração, **uma fonte documental proprietária**; índices e dossiês somente apontam para este registro.

**Integração documental ainda pendente:** manter branches `collab/docs-release-estado-2026-11` isoladas até revisão final, conferir novamente diferenças contra GitLab `release`, sem force-push e sem alterações de código/media. READMEs finais ficam para o encerramento da auditoria.
