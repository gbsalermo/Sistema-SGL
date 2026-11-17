# Matriz de coerência entre frontend e backend — 12/11/2026

> **Fila de correções (17/11/2026):** os achados e verificações C1–C8 são rastreados por IDs em [`REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md`](REGISTRO_PENDENCIAS_REVISAO_SGL_2026-11.md). **Esta matriz é a evidência, não o backlog.**

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
