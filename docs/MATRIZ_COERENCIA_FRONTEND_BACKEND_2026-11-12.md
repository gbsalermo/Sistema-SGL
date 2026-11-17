# Matriz de coerência entre frontend e backend — 12/11/2026

> **Fila de correções (17/11/2026):** os achados e verificações C1–C21 são rastreados por IDs em [`REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md`](REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md). **Esta matriz é a evidência, não o backlog.**

**Referências:** `gbsalermo/SGL-FRONTEND` e `gbsalermo/Sistema-SGL`, ambos na `release` espelhada do GitLab. Esta matriz é um **roteiro de verificação**, não um certificado de testes E2E. Priorizar o código executável sobre instruções históricas.

| Domínio | Frontend (arquivos reais) | Backend (arquivos reais) | Risco de divergência documental |
|---|---|---|---|
| Identidade e sessão | `src/stores/session.ts`, `src/services/http.ts`, `src/router/index.ts` | `AuthController.java`, `AuthService.java`, `ApiSecurityConfig.java` | Não equiparar guard de rota à autorização do servidor; validar sessão institucional |
| Estagiários e vínculos | `EstagiariosGestaoView.vue`, `estagiarioService.ts` | `EstagiarioController.java`, `VinculoEstagioController.java`, `VinculoEstagioService.java` | Multi-laboratório, orientador e elegibilidade |
| Projetos/SCI/atividades | `ProjetosGestaoView.vue`, `projetosService.ts` | `ProjetoController.java`, `SciController.java`, `AtividadeController.java` | Contexto herdado, SEG, associação de laboratórios |
| Pedidos | `NovoPedidoView.vue`, `pedidoService.ts` | `PedidoController.java`, `PedidoService.java` | Ajuda geral sugere pedido para qualquer laboratório; texto de estagiário impõe restrições por atividade |
| Soluções | `NovaSolucaoView.vue`, `SolucoesGestaoView.vue`, `solucaoService.ts` | `SolucoesController.java` → `PedidoService.java` | Projeto opcional por perfil; transições e momento da impressão |
| Estoque | `EstoqueGestaoView.vue`, `estoqueService.ts` | `EstoqueCentralController.java`, `LoteController.java`, `LoteService.java` | FEFO/FIFO, conversão, fracionamento, baixa e recipientes |
| Resíduos | `InformarResiduoView.vue`, `residuoService.ts` | `ResiduoController.java`, `ResiduoService.java` | Componente livre/catálogo, liberação, snapshots e rótulo |
| Relatórios | `RelatoriosExportacaoView.vue`, `relatorioService.ts` | `Relatorio*Controller.java`, `Relatorio*Service.java` | Filtros, escopo e exportações |
| Rótulos | `RotuloProdutoView.vue`, `RotuloResiduoAjustadoView.vue`, `RotuloSolucaoView.vue`, `DocumentoEntradaLoteView.vue`, `rotulos/zebra/*` | DTOs/consultas dos módulos correspondentes | Rotas existem; validar gatilhos de impressão, Zebra e dados reais |

**Atenção aos textos públicos:** `src/modules/ajuda/textos/pedido-em-qualquer-laboratorio.md` diz que empregados e estagiários podem pedir a qualquer laboratório ativo; `pedido-do-estagiario.md` diz que pedidos de estagiário vinculados à atividade são bloqueados se não houver laboratório autorizado. Não eliminar a condição específica sem revisar o serviço. `fazer-um-pedido-e-acompanhar.md` e `solicitar-uma-solucao.md` descrevem projeto opcional genericamente; a elegibilidade pode ser mais restritiva.

**Verificações de aceite antes do merge:** confrontar DTOs e regras de acesso, rodar testes de frontend e backend, verificar migrações e consistência de rotas, testar os fluxos de estagiários e pedidos em perfis distintos, conferir impressão real e manter histórico de decisões. Não publicar afirmações de homologação não executada.

---

## Revisão cruzada complementar — 17/11/2026

**Base:** ambos os repositórios em `release`; comparação visual de services/types/views Vue com controllers, DTOs, regras de segurança e trechos de services Java. **Estado:** análise estática de código; sem executar aplicação, Maven, Vitest ou requests HTTP reais. A classificação abaixo não é homologação.

### C1 — Contratos HTTP conferidos por leitura

| Fluxo | Chamadas no frontend | Endpoint/controller Java conferido | Resultado |
|---|---|---|---|
| Pedido de Produto: criar, consultar, aprovar, rejeitar, entregar, cancelar | `pedidoService.ts` → `/v1/pedidos`, `/{id}/aprovar`, `/{id}/rejeitar`, `/{id}/entregar`, `/{id}/cancelar`, `/por-usuario` | `PedidoController.java` usa `/api/v1/pedidos` e delega ao `PedidoService` | **Rotas e métodos correspondem**. DTOs `PedidoRequestDTO` e `AprovarPedidoRequestDTO` expõem os campos utilizados. |
| Pedido de Solução: solicitar, aprovar, rejeitar, entregar, cancelar | `solucaoService.ts` → `/v1/solucoes/pedidos` e subrotas | `SolucoesController.java`, com `PedidoService` compartilhado | **Correspondência de rota e verbos**. Cancelamento envia `preparada` e `justificativa`, compatíveis com `CancelarSolucaoRequestDTO`. |
| Estágio, bolsas e participações | `estagiarioService.ts` → `/v1/estagiarios/*`, `/v1/vinculos-estagio/*`, `/participacoes/*` | `EstagiarioController`, `VinculoEstagioController`, `VinculoEstagioAtividadeController`, `ObservacaoVinculoEstagioController` | **Rotas inspecionadas encontradas**. Não equivale à revisão de todos os payloads/status. |
| Contexto de estágio | `contextoOperacionalService.ts` → `/v1/contextos-operacionais/usuarios/{id}` e consultas por atividade/projeto | `ContextoOperacionalController.java` | **Rotas correspondem**. **Permissões de leitura exigem revisão prioritária**, ver C2. |
| Estoque e lote | `estoqueService.ts` → `/v1/estoque-central/por-unidade`, `/v1/lotes/por-estoque`, `/v1/movimentacoes/estoques/{id}/lotes`, `/descarte-vencimento`, `/movimentacoes/lote` | `EstoqueCentralController`, `LoteController`, `MovimentacaoEstoqueController` | **Rotas encontradas**; nenhuma garantia ainda sobre cálculo, concorrência, FEFO/FIFO ou fracionamento. |
| Resíduo | `residuoService.ts` → criar, listar, gerador, status, transições e `/{id}/rotulo` | `ResiduoController.java` | **Correspondência dos caminhos inspecionados**. Gestão/autorizações e dados de rótulo pendentes de teste. |
| Relatórios | `relatorioService.ts` → `/v1/relatorios/{tipo}` e `/{tipo}/exportar` | `Relatorio*Controller.java`, `RelatorioExportacaoController.java`, controllers de Resíduos/Pessoas-Laboratório | **Padrão de rotas encontrado**, inclusive exportação especializada. Filtros/formatos não homologados. |

O cliente HTTP `src/services/http.ts` usa `VITE_API_BASE_URL`; a correspondência de caminhos acima **pressupõe base URL com `/api`**, coerente com o fallback DEV do frontend (`http://localhost:8080/api`) e os prefixos `/api/v1` no backend. Verificar valor de ambiente antes de diagnóstico de 404.

### C2 — Alerta de autorização horizontal em leituras (prioridade alta)

**Evidência estática:**
- `ApiSecurityConfig.autorizarGravacoes` diferencia **escritas** por perfil; leituras restantes passam em `anyRequest().hasAuthority(SITUACAO_ATIVO)`. Isso autentica usuário ativo, mas **não restringe automaticamente visibilidade por perfil, vínculo ou titularidade**.
- `PedidoService.listarTodos`, `listarPorStatus`, `listarPorUrgencia` e `buscarPorId` filtram por **Unidade**, mas nos trechos conferidos não exigem Gestão nem vínculo do usuário ao pedido. `listarPorUsuario(UUID usuarioId)` consulta um ID arbitrário da mesma Unidade sem `CurrentUser.exigirProprioUsuario` ou teste de papel gestor. `PedidoResponseDTO` contém nome do solicitante, Projeto, laboratório, histórico operacional e itens.
- `ContextoOperacionalConsultaService.listarDoEstagiario(UUID usuarioId)`, `listarDaAtividade` e `listarDoProjeto` têm filtro de Unidade e elegibilidade, mas **não há checagem de titular/orientador/Gestão** nos métodos examinados. O DTO contém identificação/nome do estagiário, hierarquia e participações.
- `VinculoOperacionalUsuarioService.resumo` e `listar(UUID)` possuem filtro de Unidade, porém nenhuma autorização de leitura por gestão/titular observada; escritas são protegidas.

**Conclusão estritamente limitada:** há **risco plausível de leitura de dados de terceiros dentro da mesma Unidade por usuários ativos**, dependendo do endpoint e da política pretendida. Isolamento por Unidade não equivale a autorização por objeto. **Não foi executado teste de exploração**; não declarar vulnerabilidade explorada nem liberar mudança automática no backend.

**Ação exigida antes da homologação:** definir matriz de visibilidade (solicitante só seus registros? orientador os orientandos? Gestão todos da Unidade? administrador geral unidade selecionada?), criar testes de acesso positivos/negativos para duas pessoas da mesma Unidade e IDs de terceiros; só então aplicar correções no backend manualmente, com revisão de impacto nos dashboards, relatórios, Gestão e Central de Ajuda. Não resolver escondendo botões no frontend.

### C3 — Regras de laboratório e contexto conciliadas

- **Funcionário operacional:** `PedidoService.resolverContextoPedido` admite escolha de laboratório ativo na Unidade, com Projeto opcional; se não houver laboratório explícito e a pessoa tiver exatamente um, pode inferi-lo. V48 marca `laboratorioForaDoSolicitante` em Pedidos/Resíduos quando aplicável.
- **Estagiário:** `PedidoService.resolverContextoPedido` exige `participacaoId`, verifica a elegibilidade de `ElegibilidadeContextoOperacional` e fixa Projeto → SCI → Atividade; laboratório deve integrar o Projeto e os laboratórios em que o estagiário atua. A UI de Pedido e Solução filtra seletores conforme participação e laboratórios da pessoa.
- **Vínculos operacionais:** `VinculoOperacionalUsuarioService.criar` rejeita estagiários para esse cadastro de vínculos adicionais: eles usam vínculos de estágio/participações; a lista de laboratórios da pessoa é gerida por fluxo próprio.
- **Conclusão editorial:** as orientações `pedido-em-qualquer-laboratorio.md` e `pedido-do-estagiario.md` exigem interpretação **por perfil e contexto**, não devem ser arbitrariamente unificadas. **Vídeos e textos da Ajuda permanecem intocados.**

### C4 — Inconsistências de comentários/risco de desempenho, sem alterar código

- O comentário de `ContextoOperacionalController` e o Javadoc de `ContextoOperacionalConsultaService` ainda descrevem autorização como futura, e `ContextoOperacionalConsultaService.exigirTenant` menciona cabeçalho obrigatório. Hoje `TenantRequestFilter` deriva a Unidade da sessão; esses comentários são **históricos/desatualizados**, mas a verificação de autorização específica em C2 permanece necessária.
- `pedidoService.ts` tenta enriquecer itens com consultas adicionais a `/v1/produtos/{id}` apenas quando campos operacionais vêm ausentes/indefinidos. `ItemPedidoResponseDTO` já declara/preenche esses campos. Portanto, **há fallback com requisições potencialmente N+1**, mas não existe evidência de que ele seja acionado rotineiramente nem de gargalo atual. Instrumentar antes de otimizar.
- `solucaoService.ts` monta opções de produtos combinando catálogo e saldos com duas requisições paralelas. Não é evidência, por si, de erro de carga.

### C5 — Evidências de testes existentes e lacunas

**Arquivos conferidos na árvore:** `SecurityIntegrationTest.java` cobre JWT, pendentes, isolamento de Unidade/header e administrador geral; `AutorizacaoGravacoesIntegrationTest.java` cobre escritas por perfil e inclui uma leitura liberada para estagiário; `PedidoControllerTest.java` cobre respostas HTTP/DTO/status com mocks. **Não foi identificada, nos testes examinados, prova de negativa para consultas a pedidos/participações de outro usuário ativo da mesma Unidade.** Isso **não prova ausência de testes em toda a suíte**.

**Prioridade:** (1) definir e testar autorização horizontal em GET, (2) contrato de visibilidade de estagiários/orientandos, (3) FEFO/FIFO/conversão/concorrência, (4) ciclo de Solução e impressão no preparo, (5) filtros, PDF/XLSX e carga. Rodar testes em ambiente controlado e registrar resultado real, não inferir aceite de nomes de arquivos.

**Estado da revisão 17/11:** correspondência de rotas-chave verificada estaticamente; **risco de acesso a dados na mesma Unidade sinalizado** para decisão e testes; funcionalidade e segurança de produção ainda não homologadas.

### C6 — Ampliação do alerta: Resíduos e semântica de GET

Em `ResiduoService.java`, `listarTodos`, `listarPorStatus`, `listarPorLaboratorio`, `listarPorGerador`, `buscarPorId`, `buscarHistorico`, `buscarHistoricoPorGerador` e `buscarHistoricoDaUnidade` foram inspecionados. Há **isolamento por Unidade**, mas não foi observada nos métodos de consulta uma exigência de titularidade, perfil Gestor ou vínculo ao registro — ampliar os testes de autorização horizontal da seção C2 também para resíduos e históricos. A intenção funcional de acesso a informações comuns da Unidade precisa ser definida pelo mantenedor antes de restringir consultas.

**Exceção arquitetural a registrar:** `GET /api/v1/residuos/{id}/rotulo` chama `ResiduoService.gerarDadosRotulo`, que executa um `save` condicional para completar identificação de registros legados. Portanto, a leitura de rótulo **pode produzir alteração persistida** em resíduos antigos. Trata-se de compatibilidade histórica intencional no comentário do serviço, mas precisa de teste e decisão explícita sobre segurança, auditoria e idempotência. Não remover a compatibilidade automaticamente.

**Conclusão:** as regras de **escrita** foram implementadas com maior especificidade na security chain; a **matriz de leitura** entre perfis, titulares e mesma Unidade é um critério de aceite ainda aberto.

### C7 — Estoque físico: código e testes existentes identificados

**Implementação inspecionada em `MovimentacaoEstoqueService.java`:**

- `buscarLotesParaSaida` escolhe `loteRepository.buscarDisponiveisPorFefoComBloqueio` para produtos perecíveis; para não perecíveis usa `buscarDisponiveisPorEntradaComBloqueio` (FIFO). Os nomes da query e o lock estão presentes; confirmar a ordenação SQL/JPA real e concorrência nos testes.
- `calcularQuantidadeFisicamenteConsumivel` limita consumo ao saldo dos recipientes; lote não fracionável restringe a apresentações inteiras e recipientes fechados completos.
- `consumirRecipientes` separa apresentações inteiras (prioriza fechados) e parcelas fracionárias (prioriza recipientes já abertos por data de abertura, saldo e sequencial), abrindo recipiente fechado apenas se ainda faltar quantidade. Isto **corresponde estruturalmente à regra de evitar abertura indiscriminada**.
- `devolverSaidasDoPedido` registra reversões associadas a recipientes originais e possui verificações contra alterações posteriores. `LoteService` impede desligar fracionamento quando incompatível com estado atual.

**Evidência da existência de testes específicos, sem afirmar execução nesta auditoria:**

- `MovimentacaoEstoqueServiceTest.java`: FEFO, FIFO, retiradas decimais, fechado versus aberto, mistura para 700, ajustes, conflitos, vencimento, cancelamento e preservação de lacres.
- `PedidoEstoqueFisicoIntegrationTest.java`: teste anotado com `@SpringBootTest`, perfil `test`; cenários de retiradas 200/500/700, pedido grande, FEFO, não fracionável e entrega sem segunda baixa.
- `PedidoContextoOperacionalServiceTest.java`: estagiário com/sem participação, diferentes laboratórios do Projeto, lista pessoal, exclusões, snapshots e funcionário sem Projeto.
- `PedidoSolucaoServiceTest.java`: conversão, receita imutável na aprovação, baixa única, cancelamento preparado/não preparado e entrega sem segunda baixa.

**Interpretação:** há implementação e cobertura prevista por testes automatizados para as regras principais. **Ainda falta executar e registrar os resultados atuais** de `mvnw verify`, conferir locks efetivos em PostgreSQL e realizar teste de concorrência/carga; não declarar “tudo validado” apenas com base no código ou nomes dos testes.

### C8 — Pontos de impressão: implementação frontend conferida

- **Solução:** `SolucoesGestaoView.vue` oferece acesso a `/solucoes/{id}/rotulo` nos estados `PENDENTE`, `EM_PREPARACAO` ou `ENTREGUE`. Em `RotuloSolucaoView.vue`, `PENDENTE` é **somente prévia**, enquanto `window.print()` e `ZebraExportPanel` ficam habilitados apenas em `EM_PREPARACAO` ou `ENTREGUE`. Portanto, o gatilho de impressão **está implementado** a partir da aprovação/preparação; resta teste operacional da impressora e conteúdo.
- **Documento de entrada:** `EstoqueDetalheView.vue` contém acesso ao documento **logo após registrar o lote** (botão “Imprimir documento de entrada” condicionado ao último lote criado) e **no detalhe de um lote existente**. `DocumentoEntradaLoteView.vue` implementa `window.print()` e `ZebraExportPanel`. A pendência histórica sobre “em qual tela imprimir” está **resolvida no código atual**, mas fluxo real e corte físico ainda precisam de validação.
- **Resíduo:** `RotuloResiduoAjustadoView.vue` usa `dados.impressaoPermitida` para bloquear tanto impressão A4 quanto Zebra antes da liberação técnica. A gestão oferece prévia em estados anteriores.
- **Produto:** `RotuloProdutoView.vue` oferece impressão A4 e painel Zebra quando há dados carregados.

**Distinção de aceite:** botão, rota, flag de habilitação e exportador no frontend foram verificados por leitura; **não foi validada impressão real, layout na Zebra, logos, QR ou integridade de cortes**. A Central de Ajuda e seus vídeos permaneceram sem alteração.

### C9 — Usuários e estagiários: leitura por perfil e minimização de dados (17/11)

**Código real examinado na `release`:**

| Rota | Serviço/DTO | Verificação estática | Uso frontend |
|---|---|---|---|
| `GET /api/v1/usuarios` | `UsuarioController.listarTodos` → `UsuarioService.listarTodos` → `UsuarioResponseDTO` | Em geral `ApiSecurityConfig` permite leituras a usuário ativo. O service filtra Unidade quando há tenant, mas **não faz checagem de perfil** nesse método. DTO contém e-mail, nome, perfil, situação e laboratórios de pessoas da Unidade. | `estagiarioService.listarUsuarios` e `cadastrosAdminService.listarUsuarios`; usos observados em telas de Gestão/Admin |
| `GET /api/v1/usuarios/{id}`, `/por-laboratorio` | `UsuarioService.buscarPorId`, `listarPorLaboratorio` | Tenant validado, porém sem checagem de titularidade nem finalidade da consulta nos métodos examinados. | Contrato comum de usuários |
| `GET /api/v1/estagiarios`, `/{id}`, `/ativos`, `/por-laboratorio` | `EstagiarioService` → `EstagiarioResponseDTO`, que inclui histórico `VinculoEstagioResponseDTO` | Tenant presente, mas **sem `exigirGestao` nas leituras citadas**; contrastar `listarSemVinculo` e `listarOrientadoresElegiveis`, que **exigem Gestão**. DTO de vínculo inclui orientador, formação/curso, tipo de bolsa, referências e observações. | `EstagiariosGestaoView.vue` consulta `estagiarioService.listarTodos`; UI de Gestão não é controle server-side |

**Risco:** um usuário ativo da mesma Unidade pode, em princípio, alcançar rotas cujos payloads expõem dados pessoais e de estágio além do necessário para sua função. **Ainda não foi executado teste autenticado com perfis distintos**, e a política desejada para diretórios/consulta de pessoas precisa ser formalizada antes de restringir. Não classificar como incidente comprovado. A segurança JWT e a filtragem de Unidade não implementam automaticamente autorização por registro.

**Caso especial de ausência de tenant:** `UsuarioService.listarTodos` ainda conserva `.orElseGet(usuarioRepository::findAll)` quando `TenantContext.unidadeAtual()` está vazio, herdado de uma era anterior ao login JWT. Hoje `JwtAuthenticationFilter` pode dar authority de ativo a um usuário sem unidade se tal estado existir no banco, e `TenantRequestFilter` nesse caso não define tenant. **Caminho potencial e condicional de leitura entre Unidades**, que exige verificar invariantes de dados, perfis e teste de usuário ATIVO sem Unidade. Usuários provisionados via `AuthService` recebem Unidade habilitada, o que reduz a expectativa de ocorrência normal, **mas não prova impossibilidade de registros legados**. `UsuarioResponseDTO` não expõe senha, mas expõe identificação e e-mail.

**Testes inspecionados:** `UsuarioControllerTest` e `EstagiarioControllerTest` usam mocks de services nos métodos de leitura; `SecurityIntegrationTest` enfatiza JWT/tenant entre Unidades; `AutorizacaoGravacoesIntegrationTest` cobre principalmente escritas; `EstagiarioServiceTest` cobre listagem e regras de estágio, mas **não foi demonstrada na amostra** negativa de leitura de outro usuário na mesma Unidade. É necessário um teste HTTP autenticado em ambiente isolado, sem dados reais.

**Próximos IDs na fila de correção:** `SGL-REV-018` (Usuários), `019` (Estagiários) e `020` (sem tenant). O frontend precisa de DTOs mínimos adequados para seletores, se a API passar a restringir listagens completas.

### C10 — Contratos históricos de Estágio e Etapa 8 não equivalem à implementação (17/11)

`docs/ETAPA_8_CONTRATO_VINCULOS_ACESSO.md` afirma “Solução continua exigindo projeto” e que técnicos só podem operar em laboratório autorizado. A `release` posterior aplica **Projeto opcional a empregado**, mantém `participacaoId` obrigatório para Estagiário e permite solicitação a laboratório ativo da Unidade com a flag `laboratorioForaDoSolicitante`. `PedidoSolucaoServiceTest` inclui o caso `permiteSolucaoSemProjetoQuandoLaboratorioEstaAutorizado`, enquanto o service aplica a política atual de Unidade/laboratório. **Contrato de outubro é histórico**, não motivo para regredir a API atual.

`docs/CONTRATO_CONTEXTOS_PEDIDOS_ETAPA_8.md` abre com “NÃO implementado no fluxo de criação”, mas possui adendo de fechamento da etapa; esse cabeçalho é histórico. O código `PedidoService.resolverContextoPedido` implementa a participação e permite ao estagiário escolher o laboratório entre os vinculados ao Projeto **e** à pessoa. Conferir versão vigente no código e preservar especificações históricas como registro de decisão.

`docs/DECISAO_CICLO_INSTITUCIONAL_ESTAGIO.md` registra precedência campo a campo da integração; **não é prova** de sincronização externa atual em produção. Validar com `AuthService` e `SincronizacaoVinculoEstagioService` na etapa integrada.

**Encaminhamento:** sinalizar vigência nos três documentos especializados e registrar `SGL-REV-021` para correção editorial definitiva após a decisão de contrato. Nenhuma alteração funcional nesta auditoria.


### C11 — Integração corporativa, primeiro acesso e sincronização institucional (17/11/2026)

**Base inspecionada:** `AuthService.java`, `PessoaCorporativaService.java`, `SincronizacaoVinculoEstagioService.java`, `VinculoEstagioController.java`, `PessoaCorporativaController.java`, `TenantRequestFilter.java`, serviços do frontend e as suites `AuthServiceIntegrationTest`, `PessoaCorporativaIntegrationTest` e `SincronizacaoVinculoEstagioServiceTest` da `release`.

| Fluxo distinto | Contrato verificado no código | Implicação documental |
|---|---|---|
| **Login institucional** | `AuthService.login` chama `CorporateApiClient.login`, valida situação da identidade, resolve Unidade corporativa habilitada e cria/atualiza `Usuario`. Ao sincronizar, preserva perfil e laboratórios locais; mudança de lotação passa por `MudancaLotacaoService`. | Não chamar esse processo de “importação manual” nem afirmar que vínculo de estágio é criado automaticamente pelo login. |
| **Empregado novo** | `AuthService.saveNewcomer` atribui perfil inicial sugerido e `SituacaoUsuario.ATIVO`; empregado antes pendente pode ser liberado no login posterior. | Não descrever todo primeiro acesso como obrigatoriamente pendente de Gestão. |
| **Colaborador/estagiário novo** | `saveNewcomer` registra `PENDENTE_VINCULACAO`; a security chain impede uso operacional até aprovação. | Distinguir cadastro da identidade de criação/regularização de estágio e seus vínculos. |
| **Importação pela Gestão** | `GET /api/v1/pessoas-corporativas` e `POST /api/v1/pessoas-corporativas/importar` usam `PessoaCorporativaService`; `requireGestao`, controle de perfil concedido, Unidade ativa e `LaboratoriosDaPessoa`. Importação cria pessoa já ativa com perfil e laboratórios. | Não confundir esta ação administrativa com login ou sincronização de bolsa. Frontend: `pessoasCorporativasService.ts` e `ImportarPessoasView.vue`. |
| **Sincronização de ocorrência de vínculo** | `POST /api/v1/vinculos-estagio/{vinculoId}/sincronizacoes-institucionais` chama `SincronizacaoVinculoEstagioService.sincronizar`: valida vínculo e tenant, trata evento repetido/idempotência, aplica alterações e encerra participações abertas no fim do estágio. | O **endpoint receptor existe**, mas esta análise **não demonstrou quem, quando ou se envia automaticamente eventos em produção**. Distinguir mecanismo implementado de integração operacional. |
| **Atualização local do vínculo/bolsa** | `VinculoEstagioController` também oferece atualização, referência, prorrogação e nova bolsa como fluxos locais. | Não tratar fallback local como retorno garantido da API institucional. |

**Evidências de testes presentes (sem execução nesta auditoria):** `AuthServiceIntegrationTest` contempla empregado ativo, colaborador pendente, preservação de pessoa importada, mudança de Unidade, desativação, conta DEV e bootstrap do primeiro administrador; `PessoaCorporativaIntegrationTest` cobre importação multilaboratório, restrições de perfil, Unidade e cache; `SincronizacaoVinculoEstagioServiceTest` contempla prorrogação, encerramento com participações, duplicação de evento, replay divergente, referência institucional e tenant. O nome `IntegrationTest` não certifica conexão à infraestrutura institucional real: há stubs/gateway de teste no repositório.

**Decisão documental:** três fluxos *não equivalentes* (login, importação, sincronização de ocorrência) devem permanecer separados nos guias e dossiês. O requisito de precedência por campo documentado em `DECISAO_CICLO_INSTITUCIONAL_ESTAGIO.md` não confirma automação em produção; a análise estática não encontrou evidência suficiente de agendamento/origem de eventos. Registrar como `SGL-REV-022` (integração de sincronização a homologar) e manter `SGL-REV-016` como homologação geral do login/implantação.

**Próximos testes de aceite:** confirmar no ambiente institucional autorizado: origem do evento, autenticação do emissor, cadence/acionamento, reenvio de mesmo evento, fim de estágio, mudança de lotação, perfil administrativo, consistência das participações e trilha de auditoria. Nunca publicar credenciais/identificadores institucionais em documentação.


### C12 — Escopo real da suíte de testes do frontend e Zebra (17/11/2026)

**Inspeção direta na `release` do frontend:**
- `package.json` define `npm run test` como `vitest run`, `npm run type-check` como `vue-tsc --noEmit` e `npm run build` como `vue-tsc --noEmit && vite build`.
- `vitest.config.ts` usa `test.include: ['src/**/__tests__/*.spec.ts']`. Por isso, `src/modules/rotulos/zebra/zpl.test.mjs` e `browserPrintLocal.test.mjs`, que estão fora de `__tests__` e terminam em `.test.mjs`, **não são selecionados por `npm run test`**.
- Esses dois arquivos importam `test` de `node:test` e contêm orientação explícita para execução com `node --experimental-strip-types --test src/modules/rotulos/zebra/<arquivo>.test.mjs`. Sua existência e expectativa de comando são verificadas; **nenhum teste foi executado nesta revisão**.
- `src/modules/ajuda/__tests__/helpTasks.spec.ts` e `helpAssets.spec.ts` **estão cobertos pelo padrão Vitest**. Verificam IDs/ordem de tutoriais, URLs, existência e tamanho dos posters e PDF; **não verificam disponibilidade real dos MP4**. Isso confirma a separação entre teste de assets versionados e verificação do deploy, sem alterar vídeo ou player.
- A árvore `.github/workflows/` dos espelhos GitHub consultados mostra workflows de sincronização, não evidência de etapa de teste/build. Isso **não autoriza concluir ausência de CI no GitLab institucional ou de testes locais do mantenedor**.

**Conclusão:** há uma **lacuna verificável de orquestração** para executar testes Zebra em conjunto com a suíte padrão do frontend. Registrar `SGL-REV-023`, vinculando aos aceites `SGL-REV-009` (evidência de execução), `011` (impressão física) e `012` (vídeos no deploy). Uma eventual inclusão no pipeline deve ser feita depois da revisão, sem mudar a implementação dos rótulos nem a Central de Ajuda.


### C13 — Auditoria da cobertura estrutural e das permissões (17/11/2026)

**Inventário verificável da árvore `release` (contagem de ARQUIVOS, não de casos de teste, nem de sucessos):**
- Backend: **128 arquivos Java com nome `*Test.java`** em `src/test/java/`; entre eles `SglArchitectureTest`, `SecurityIntegrationTest`, `AutorizacaoGravacoesIntegrationTest`, `PedidoAutorizacaoAcoesTest` e `PedidoConcorrenciaIntegrationTest`.
- Frontend: **31 arquivos `*.spec.ts`** no padrão `src/**/__tests__/*.spec.ts` da configuração Vitest, além dos dois `.test.mjs` de Zebra com `node:test` (C12).

**Arquitetura (backend):** `SglArchitectureTest.java` usa ArchUnit com **cinco regras negativas**: `model`, `repository` e `dto` não dependem de `controller`; `dto` não depende de `repository`; `service` não depende de `controller`. O próprio Javadoc as classifica como barreiras mínimas do MVP. **Não abrangem**, pela existência das regras, ciclos gerais, complexidade, qualidade das consultas, limites de transação, autorização por objeto, contratos de endpoints ou desempenho. Esses itens exigem revisão e testes específicos (`SGL-REV-017`).

**Segurança (backend):**
- `SecurityIntegrationTest` verifica JWT ausente/inválido/inativo, usuário pendente, endpoint de login, isolamento via header de Unidade e seleção de Unidade pelo administrador geral.
- `AutorizacaoGravacoesIntegrationTest` testa matriz de **gravações** por perfis e comprova pelo menos uma leitura autorizada para estagiário (`GET /api/v1/produtos`).
- `PedidoAutorizacaoAcoesTest` recusa aprovar, rejeitar, entregar e cancelar por perfis solicitantes, além de impedir aprovação em nome de terceiro por gestor.
- `ContextoOperacionalConsultaServiceTest` contempla tenant ausente, perfil não estagiário, Projeto/Atividade/Usuário de **outra Unidade** e mudança de tenant; o método do service não valida titularidade entre **dois estagiários ativos da mesma Unidade**.

**Lacuna de aceitação (não afirmar ausência de qualquer teste na suíte inteira):** nos arquivos examinados não foi localizada uma matriz de testes HTTP autenticados com acesso **negado a registros de outra pessoa da mesma Unidade** nos domínios Pedidos, Contextos, Vínculos, Resíduos, Usuários e Estagiários. Permanecem `SGL-REV-001–004`, `018–020` e `009`. O teste deve confirmar simultaneamente que o papel de Gestão e o orientador autorizado ainda conseguem consultar dados necessários.

**Segurança (frontend):** `src/router/__tests__/guards.spec.ts` confere navegação por perfil e pendência; `src/services/__tests__/http.spec.ts` usa `fakeAdapter` para Authorization, header de administrador e respostas 401/403; `src/stores/__tests__/session.spec.ts` usa adapter falso e armazenamento simulado; os testes de `NovoPedidoView` e `NovaSolucaoView` usam mocks para validação de laboratórios por estagiário/empregado. São regressões de UI e contratos de chamadas, **não testes de permissões reais do backend**.

### C14 — Banco de testes, Flyway, concorrência e E2E (17/11/2026)

**Perfil `test` backend confrontado com a implementação:**
- `src/main/resources/application-test.properties` define `spring.datasource.url=jdbc:h2:mem:sgldb-${random.uuid}`, `spring.jpa.hibernate.ddl-auto=create-drop` e **`spring.flyway.enabled=false`**.
- `PedidoConcorrenciaIntegrationTest` usa `@SpringBootTest` e `@ActiveProfiles("test")`; seu teste `deveImpedirDoisPedidosConcorrentesDeConsumiremOMesmoSaldo` executa dois fluxos simultâneos com `CountDownLatch`, verifica um pedido aprovado e outro pendente, e o saldo físico final.
- `PedidoEstoqueFisicoIntegrationTest` também usa perfil `test`, cobrindo fluxo de baixa e recipientes. Essas verificações são relevantes, **mas executadas sob H2 quando esse perfil é usado**, não uma confirmação dos locks transacionais, SQL, índices e constraints em PostgreSQL.
- **Ponto específico ainda descoberto:** o perfil de teste padrão desativa Flyway e recria estrutura via Hibernate. Consequentemente, **passar `mvnw test` com esse perfil não comprova que V1–V48 e futuras migrations sejam aplicadas corretamente em PostgreSQL**, nem que a modelagem real concorde com a gerada pelo Hibernate.
- Testes com `CorporateGatewayTestConfig` usam gateway corporativo simulado. Isso preserva autonomia da suíte, mas não substitui homologação com sistema institucional autorizado.

**Frontend:** `vitest.config.ts` opera em `jsdom`; os testes inspecionados de pedidos, soluções, sessão, router e serviço HTTP usam mocks/fakeAdapter. `package.json` não define script de E2E, e na árvore da `release` consultada não foi identificado projeto local de Cypress/Playwright. **Não concluir que não exista pipeline de E2E externo ou CI institucional no GitLab**. A etapa 12 continua plano, não resultado.

**Encaminhamento:** usar `SGL-REV-010` para concorrência/FEFO em PostgreSQL, `SGL-REV-017` para E2E/carga/arquitetura, `SGL-REV-009` para execução e evidências e abrir `SGL-REV-024` especificamente para **teste de aplicação de migrations Flyway em banco compatível com produção**, evitando esconder esta lacuna sob “build verde”. A equipe deve executar as verificações em ambiente isolado com dados sintéticos e registrar hash da release, profile e logs, sem alterar produção.


### C15 — Testes por domínio: pedidos, soluções, estoque, resíduos e relatórios (17/11/2026)

**Método:** leitura de suites e identificação dos cenários `@Test` da branch `release`. Os itens abaixo descrevem **cobertura codificada**, não resultado de execução.

| Domínio | Suites inspecionadas | Cobertura identificada | Limite de aceite ainda aberto |
|---|---|---|---|
| Pedidos | `PedidoServiceTest`, `PedidoAutorizacaoAcoesTest`, `PedidoConcorrenciaIntegrationTest` | Titularidade na **criação**, perfis nas **ações**, aprovação, quantidades, estados e dois pedidos sobre o mesmo saldo | Não substitui autorização por objeto nas **consultas GET** nem concorrência no PostgreSQL |
| Soluções | `PedidoSolucaoServiceTest` | Projeto opcional de funcionário, laboratório no Projeto, conversão, receita imutável, baixa única, confirmação “preparada?” e cancelamento | Validar jornada UI → API → banco e impressão física no preparo |
| Estoque | `MovimentacaoEstoqueServiceTest`, `LoteServiceTest`, `PedidoEstoqueFisicoIntegrationTest` | FEFO/FIFO, entrada decimal, recipientes, seleção de abertos/fechados, retirada 700, saldo, estornos, lote vencido, restrição de fracionamento | Validação de constraints, migrations, ordem e lock em PostgreSQL; dados e concorrência |
| Resíduos | `ResiduoServiceTest`, `ResiduoAdministracaoServiceTest` | Contexto de estagiário, laboratório, usuário, projeto, componentes livres/catálogo, recebimento/liberação, armazenagem, cancelamento, histórico | Falta comprovação de matriz de leitura por titular/mesma Unidade; revisar efeito colateral de GET rótulo |
| Relatórios | `RelatorioExportacaoServiceTest`, `RelatorioMovimentacoesServiceTest`, `RelatorioResumoOperacionalServiceTest`, `RelatorioResiduosExportacaoServiceTest`, `RelatorioPessoasLaboratorioExportacaoServiceTest`, `RelatorioExportacaoControllerTest` | Saídas PDF/XLSX, filtros por tipo/período/produto/embalagem, quantidades, unidade, falhas de exportação e respostas de controller | Não prova equivalência exaustiva de prévia vs. exportação nos filtros combinados, carga histórica, autorização GET ou executabilidade de todos os testes |

**Critério comum:** sempre registrar qual suite foi executada, commit, banco, perfil e resultados. Tests com `MockitoExtension` exercitam services simulados; tests `@SpringBootTest @ActiveProfiles("test")` usam H2, não PostgreSQL. A revisão dos controladores de Relatórios inclui mocks, o que valida resposta/roteamento mas não necessariamente integra todo o conteúdo da exportação.

### C16 — Relatórios: filtro em memória e ausência condicional de tenant (17/11/2026)

**Persistência de problema previamente apontado:** `docs/ETAPA_9_1_AUDITORIA_CONTRATOS_RELATORIOS_2026-10-26.md` já descrevia a consulta ampla. Confirmado **na `release` atual**:
- `RelatorioMovimentacoesService.gerar` invoca `movimentacaoRepository.findAll().stream()`, depois filtra em memória por Projeto, SCI, Atividade, tipo, origem, produto, laboratório, usuário, lote e período, antes de montar DTOs. Dados de detalhes por recipientes são obtidos em lote para os IDs filtrados, o que **não prova N+1 nesse trecho**.
- `RelatorioResumoOperacionalService.gerar` também chama `movimentacaoRepository.findAll().stream()`, filtrando produto/período e calculando totais/rankings no Java.
- `MovimentacaoEstoqueRepository.findAll` aplica JPQL de **Unidade quando tenant presente**, ordena por data, mas **não empurra os filtros dos relatórios para o SQL**. Carregar todo histórico da Unidade para filtrar depois pode aumentar memória, transporte BD→API e duração. **Nenhuma latência, cardinalidade ou gargalo foi medida**, logo se trata de risco de escalabilidade, não incidente comprovado. Vincular a `SGL-REV-025`.

**Questão independente de segurança, condicional:** a consulta JPQL do repository contém `(:#{@tenantProvider.unidadeId} IS NULL OR m.estoqueCentral.unidade.publicId = :#{@tenantProvider.unidadeId})`. Assim, **chamadas a `findAll` sem tenant fazem busca sem recorte por Unidade**. Nos dois métodos `gerar` não foi encontrada exigência incondicional de `TenantContext.unidadeAtual().orElseThrow()` (Movimentações exige Unidade apenas para o filtro por embalagem). Os controllers expõem `GET /api/v1/relatorios/movimentacoes` e `/resumo-operacional`; a security chain geralmente libera GET a usuário ativo, e `TenantRequestFilter` não define Unidade se o usuário autenticado não a possui.

**Risco a confirmar:** um usuário `ATIVO` com JWT válido **e sem Unidade** pode, dependendo das invariantes do banco, receber dados agregados de mais de uma Unidade. O login corporativo normal mapeia Unidade habilitada, portanto a auditoria **não demonstra que esse perfil residual exista nem que a via HTTP seja explorável**. Registrar `SGL-REV-026`, relacionado ao fallback sem tenant em Usuários (`SGL-REV-020`). Além disso, decidir se relatórios completos devem ser visíveis a qualquer usuário ativo ou somente Gestão (`SGL-REV-001–004`, `018–019`).

**Aceite proposto:** testes HTTP autenticados com principal ativo sem Unidade, dois usuários e dois tenants; teste de falha fechada sem contexto; medir consultas, custo e filtros em massa representativa, sem refatorar ou modificar o repository durante a revisão. Conferir `EXPLAIN` e índices em PostgreSQL isolado, e validar igualdade prévia/exportação após qualquer otimização. Nunca rodar benchmark com carga real em produção sem planejamento.


### C17 — Contrato de quantidades do Resumo Operacional soma unidades incompatíveis (17/11/2026)

**Divergência demonstrada por leitura de código e pelo próprio teste:**
- `RelatorioResumoOperacionalService.gerar` acumula `quantidadeEntradas`, `quantidadeSaidas` e `quantidadeDescartes` com `BigDecimal.add` **entre movimentações de produtos distintos**, sem agrupar por unidade de medida.
- `RelatorioResumoOperacionalResponseDTO` expõe esses totais globais `BigDecimal` sem campo correspondente de unidade.
- Em `RelatorioResumoOperacionalServiceTest.deveGerarResumoOperacionalComSucesso`, `produtoA` usa `UnidadeMedida.KG` e `produtoB` usa `UnidadeMedida.L`. O teste configura entradas de **10 kg** e **6 L** e afirma `resultado.getQuantidadeEntradas() == 16`. Esse total físico **não tem unidade homogênea**. Ranking por produto e lote preserva unidade em DTO especializado; contagens de movimentos são semanticamente válidas.
- Na `RelatoriosGestaoView.vue` analisada, o **modo Resumo** mostra `totalMovimentacoes`, `totalEntradas`, `totalSaidas`, `totalDevolucoes`, `totalAjustes`, `totalDescartes` — contagens, não os três campos de quantidades incompatíveis. Logo **não há evidência de exibição desses totais físicos globais nessa tela atualmente**, mas o contrato API os retorna e pode ser consumido em outras integrações.

**Natureza:** inconsistência **semântica de dados da API**, demonstrada no algoritmo e teste; não necessariamente bug visual de produção. Antes de alterar a API, decidir se esses campos serão descontinuados, serão retornados `null` quando múltiplas unidades, ou serão substituídos por totais agrupados por unidade/produto; preservar compatibilidade de clientes e de exportações.

**Aceite futuro:** testes com kg e L que não aceitem agregação física sem unidade, quantidades por grupo homogêneo, ausência de regressão nas contagens e no relatório detalhado, testes prévia versus PDF/XLSX. Registrar como `SGL-REV-027`, sem correção de código na auditoria.


### C18 — Estoque: correções históricas confirmadas e carga do Dashboard Gestão (17/11/2026)

**Pendências da Etapa 7.7 que NÃO devem ser reabertas:**
- `docs/ETAPA_7_7_AUDITORIA_INTEGRACAO_BACKEND_FRONTEND.md` descreve, no checkpoint inicial, que somente `listarPorUnidade` enriquecia `EstoqueCentralResponseDTO` com `apresentacoesFisicas`. Na `release` atual, `EstoqueCentralService.listarTodos` usa `comResumosFisicos` e `buscarPorId`/`buscarPorUnidadeEProduto` usam `comResumoFisico`. **A diferença inicial de DTO foi corrigida.**
- O N+1 inicial no resumo da **listagem geral** também foi tratado: `comResumosFisicos(List<EstoqueCentral>, UUID)` executa `recipienteEstoqueRepository.buscarPorUnidadeParaResumo(unidadeId)` uma vez e distribui recipientes por estoque em memória. **Não generalizar** que toda a agregação atual usa consulta individual; as listagens principais foram otimizadas.

**Risco residual em código atual:**
- `EstoqueCentralService.listarEstoqueBaixoPorUnidade` faz `.map(this::comResumoFisico)`, e cada `comResumoFisico` chama `findByLoteEstoqueCentralIdOrderByLoteIdAscSequencialAsc(estoque.getId())`. Assim, uma listagem de N estoques baixos **pode executar N consultas de recipientes**; ainda não medimos N SQL reais/latência.
- O frontend `DashboardGestaoView.vue` (`carregarDados`) inicia **8 fontes em `Promise.allSettled`**, incluindo `estoqueService.listarPorUnidade` e `listarEstoqueBaixo`. Depois executa `estoques.map(estoque => estoqueService.listarLotesPorEstoque(estoque.id))`, disparando **uma requisição HTTP por estoque ativo**, sem paginação nesse trecho.
- A lista `resultadosLotes` usa apenas os `fulfilled` para montar `lotes.value`; os rejeitados **não são incluídos** no cálculo `falhas`, que contabiliza apenas as oito fontes iniciais. Portanto, falha parcial na obtenção de lotes pode deixar indicadores incompletos **sem aviso específico** de que houve falha de lotes.
- A árvore `release` conferida contém testes para `EstoqueCentralService`/Lotes e telas do módulo Estoque, mas **não foi localizado** arquivo dedicado `DashboardGestaoView.spec.ts` entre os 31 specs selecionados por Vitest. Isso não significa inexistência de testes externos ou outros testes indiretos.

**Natureza e acompanhamento:** chamada por estoque e exclusão de falhas de lotes no contador são **fatos estruturais**; efeitos em tempo real/usuário e severidade dependem de medição e reproduções. Acompanhamento único `SGL-REV-028` (prioridade P2; desempenho + observabilidade do Dashboard). Não propor reduzir queries juntando dados indiscriminadamente sem preservar tenant, FEFO/FIFO, indicadores e estados vazios.

**Aceite futuro:** testar 0/1/N estoques ativos e baixos, N lotes por estoque, falha seletiva de uma requisição de lote, erro visível sem apagar indicadores válidos, carga com massa representativa, número de requests, tempo total e consultas no backend, mais alternativa de lote/resumo em lote autorizada. Ver também `SGL-REV-017` (E2E/carga) e `SGL-REV-025` (desempenho de Relatórios, que é outro fluxo).


### C19 — Revisão operacional especializada e amostragem de links (17/11/2026)

**Conciliação histórica concluída nesta rodada (somente notas de vigência, mantendo conteúdo original):**

| Documento | Situação confirmada contra a `release` | Tratamento editorial |
|---|---|---|
| Backend `ETAPA_7_5_AUDITORIA_E_CONTRATO_CONTEXTO_OPERACIONAL.md` | Endpoints de contexto e `participacaoId` efetivos, ao contrário das lacunas listadas antes da implementação | Aviso de que o plano inicial é histórico; autorização GET segue pendente em C2/C9 |
| Backend `ETAPA_7_6_0_AUDITORIA_CONTRATO_PEDIDOS_ESTOQUE_FISICO.md` | Pedido com saída física, regras de fracionamento/estorno e testes de service já implementados | Não reabrir a implementação; verificar PostgreSQL/Flyway e execução dos testes |
| Backend `ETAPA_7_7_AUDITORIA_INTEGRACAO_BACKEND_FRONTEND.md` | DTO de estoque e N+1 da listagem geral corrigidos; risco residual de consulta por estoque baixo e requisições de lotes no Dashboard | Marcar correções antigas como resolvidas; pendência atual única `SGL-REV-028` |
| Backend `PLANO_PRE_PRODUCAO.md` | “Marco vigente” de outubro não é a base operacional de novembro | Preservar como histórico; direção atual em `release`, dossiê e `CONTINUIDADE.md` |
| Backend `ETAPA_10_PREPARACAO_ROTULOS_IMPRESSAO_2026-10-27.md`, `ETAPA_10_CENARIO_GESTAO_ROTULOS_2026-10-28.md` | Rótulos e Browser Print existem; roteiro demo usa checkout de branch antiga e massa fictícia | Registrar estado real sem executar demonstração antiga em produção |
| Frontend `ETAPA_7_4_TESTE_FLUXO_SOLUCOES.md`, `ETAPA_7_5_MAPA_IMPLEMENTACAO_FRONTEND.md`, `ETAPA_8_INICIO_E_REGRAS.md` | Contratos evoluíram para JWT, header de Unidade exclusivo do admin geral e contexto obrigatório do estagiário | Marcar sessões/branches antigas como históricas, preservar aceite original |
| Frontend `ETAPA_9_INTEGRACAO_FRONTEND_2026-10-26.md` | Relatórios já estão na `release` | O “sem merge” refere-se ao checkpoint de outubro |
| Frontend `ETAPA_10_ZEBRA_PILOTO_ZPL_2026-10-29.md` e `ETAPA_10_PREPARACAO_ROTULOS_IMPRESSAO_2026-10-27.md` | Impressão direta Zebra já implementada, apesar de notas iniciais de piloto sem Browser Print | Não confundir piloto antigo com suporte atual; impressão física ainda não homologada |
| Frontend `PLANO_TESTES_PRIMEIRO_PROTOTIPO.md` | Plano antecede `ADMINISTRADOR_GERAL`, JWT e multilaboratório | Inserida nota para ampliar matriz de perfis e não tratar plano como execução |

**Referências locais:** a amostra anterior da auditoria conferiu **25 links Markdown relativos** em documentos centrais dos dois repositórios. Nesta rodada, foram verificados **15 links em oito guias especializados do backend** e **2 links em oito guias do frontend**, todos resolvidos para caminhos existentes na árvore Git das branches documentais: **42 referências verificadas ao todo, nenhuma inexistente na amostra**. Essa conferência verifica existência do **arquivo-alvo**, não âncoras internas, URLs externas nem todos os documentos. Permanecem verificações pendentes da auditoria de links completa, posteriormente à organização dos arquivos e antes dos READMEs finais.

**Central de Ajuda:** somente referência documental cruzada; `src/modules/ajuda`, `public/ajuda`, vídeos e posters não foram editados. Não foi executado teste de aplicação, download MP4 nem impressão real.

**Princípio de manutenção:** a presença de um item na seção histórica “Pendências” de uma Etapa não cria automaticamente um novo defeito. Verificar antes o código da `release`, adendos de fechamento e IDs do backlog.

### C20 — Auditoria somente-leitura da Central de Ajuda: 20 tutoriais, FAQ e mobile (17/11/2026)

**Fonte analisada no frontend `release`:** `src/modules/ajuda/tasks.json` (20 tarefas), 20 arquivos Markdown correspondentes em `src/modules/ajuda/textos/`, `perguntas-frequentes.md`, `helpTasks.ts`, `HelpTaskContent.vue`, `HelpCenterView.vue`, `helpTexts.spec.ts` e `HelpCenterView.spec.ts`. **Nenhum arquivo da Ajuda foi alterado**.

**Achados editoriais concretos:**
1. `pedido-em-qualquer-laboratorio.md` apresenta **Empregados e estagiários** como aptos a solicitar em qualquer laboratório ativo. `perguntas-frequentes.md` repete que “você pode pedir a qualquer laboratório ativo”. Mas `pedido-do-estagiario.md` e `PedidoService.resolverContextoPedido` exigem participação elegível e laboratório associado ao **Projeto e à pessoa** para Estagiário; a liberdade para outro laboratório ativo da Unidade corresponde ao **empregado**. `fazer-um-pedido-e-acompanhar.md` e `solicitar-uma-solucao.md` também chamam Projeto de opcional sem ressalva por perfil. **Ampliar `SGL-REV-006`** para incluir o FAQ e tutoriais genéricos, sem editar vídeos ou conteúdo agora.
2. `perguntas-frequentes.md` afirma que a mensagem “Aguardando aprovação” acompanha “seu primeiro acesso” genericamente. `AuthService.saveNewcomer` ativa **empregados novos** com perfil sugerido pelo cargo, mas cadastra **colaboradores/estagiários novos** como `PENDENTE_VINCULACAO`. A frase do FAQ carece de diferenciação por vínculo/identidade. **Registrar `SGL-REV-031`**, com decisão editorial posterior e conferência dos vídeos.
3. `central-de-solucoes.md` descreve impressão do rótulo na preparação, correspondente à liberação condicional verificada em `RotuloSolucaoView.vue`. `estoque-e-lotes.md` menciona impressão do documento de entrada, acesso localizado no frontend. `informar-um-residuo.md` explica corretamente composição **catálogo ou componente livre**. Esses exemplos **não devem ser reabertos como erros**.

**Mobile e testes da Ajuda:**
- `HelpCenterView.vue` usa layout adaptativo com `@media(max-width:959px)`: menu móvel em drawer, overlay e botão de alternância. `HelpTaskContent.vue` limita largura do vídeo a 100%, usa `preload=metadata` e mantém texto acessível quando o vídeo falha.
- `HelpCenterView.spec.ts` testa abrir/fechar o menu, Escape, links e fallback textual; `helpTexts.spec.ts` verifica existência/estrutura de textos e órfãos. **Não verifica** Safari/Android real, teclado virtual, velocidades de rede nem conteúdo das orientações contra permissões da API.
- Vídeos MP4 não estão no Git examinado; disponibilidade do deploy permanece `SGL-REV-012`, sem inferir ausência no ambiente publicado.

**Encaminhamento:** preservar os 20 IDs, menu, posters, FAQ, PDFs e links. Registrar diferenças **fora** da Ajuda; futura revisão textual exige aceite específico e alinhamento com a fala do vídeo, sem alterar material audiovisual automaticamente.

### C21 — Homologação cloud/staging e uso mobile por estagiários (17/11/2026)

**Decisão de planejamento:** acrescentar ensaio efetivo de endpoints e navegação **com navegador/API reais** num staging efêmero isolado, PostgreSQL + Flyway, JWT/gateway corporativo de teste autorizado e dados sintéticos; sem expor `dev-login`, sem tocar produção. Documento de execução e evidências único: [`PLANO_HOMOLOGACAO_NUVEM_MOBILE_2026-11-17.md`](PLANO_HOMOLOGACAO_NUVEM_MOBILE_2026-11-17.md).

**Inspeção de responsividade atual:**
- `NovoPedidoView.vue`: itens em grade com media queries a 900/720 px; `NovaSolucaoView.vue`: adaptação a 900/720 px; `InformarResiduoView.vue`: adaptação de campos a 900/620 px. **Existem estilos mobile**, mas isso não comprova usabilidade em tela real.
- `EstagiariosGestaoView.vue` é tela de **Gestão**, não solicitante; sua tabela possui `min-width:1160px` dentro de wrapper `overflow-x:auto`. Verificar rolagem horizontal **local** e drawers quando gestor acessar por telefone, sem confundir com fluxo de Estagiário solicitante.
- `HelpCenterView.vue` tem menu móvel a 959 px; os testes Vitest usam ambiente simulado, não touchscreen/teclado virtual.
- Sem evidência nesta auditoria de testes reais Android Chrome e iOS Safari, nem simulação de rede móvel com envio único/retry e jornada E2E estagiário multilaboratório.

**Portões recomendados:** CL-01–11 (auth, tenant, estágios, Pedidos, Soluções, Resíduos, Estoque, Relatórios, Ajuda/MP4) e MB-01–12 (telefone e jornadas do Estagiário). Larguras 320/360/390/430/768/1024 px e **dispositivos físicos** Android/iOS, teclado virtual, zoom, toque, conexão intermitente, mensagens de erro e prevenção de envios repetidos. Registrar perfis/fixtures, código, HTTP status, resultados, screenshots sintéticos e tempos medidos. Não inventar latência nem sucesso.

**Status:** **NÃO EXECUTADO**. Ambiente de nuvem não foi provisionado, nenhum endpoint real chamado e nenhuma visualização móvel real testada. Rastrear execução de mobile em `SGL-REV-029` e staging cloud em `SGL-REV-030`; manter `SGL-REV-009`, `017` e `024` como requisitos transversais. Não substituir autorização do servidor por um layout responsivo.
