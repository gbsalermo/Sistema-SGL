# Matriz de coerência entre frontend e backend — 12/11/2026

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
