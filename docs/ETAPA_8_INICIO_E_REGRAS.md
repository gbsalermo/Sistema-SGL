# Etapa 8 — Checkpoint de início | Backend

**Data:** 20/10/2026
**Branch:** `collab/etapa-8-contextos-pedidos`
**Estado:** auditoria inicial documentada; implementação funcional **NÃO iniciada**.

## Objetivo vigente

Integrar o contexto operacional de participação do Estagiário aos Pedidos de Produtos e Soluções, utilizando o contrato existente em `docs/CONTRATO_CONTEXTOS_PEDIDOS_ETAPA_8.md`. A Etapa 7 está encerrada funcionalmente. Não reconstruir Estoque, Soluções nem o domínio de Projeto/SCI/Atividade.

## Base verificada na main

- `PedidoRequestDTO`: hoje exige `usuarioId` e `laboratorioId`, aceita `projetoId`; ainda não possui `participacaoId`.
- `Pedido`: possui referência ao Usuário, Laboratório e Projeto; ainda não persiste a participação de estágio nem snapshot próprio da origem.
- `PedidoService.criar()`: resolve Laboratório do request e valida laboratório-base do Usuário para perfis comuns, inclusive Estagiário; falta resolução por perfil/participação.
- `ElegibilidadeContextoOperacional.avaliar()`: política de elegibilidade já implementada; deve ser reutilizada na gravação, com data do servidor.
- `ContextoOperacionalConsultaService`: leitura por Estagiário, Atividade e Projeto já existente, com consultas de Unidade; não duplicar API.
- `VinculoEstagioAtividadeService.atualizar()`: permite mudar a Atividade de uma participação aberta, portanto um Pedido anterior precisa conservar o contexto de origem histórico.
- Migrations publicadas até `V39__estoque_minimo_apresentacao.sql`. Conferir a maior versão na main institucional/Flyway antes de numerar nova migration.

## Contrato de domínio para implementar (não alterar sem decisão)

1. Estagiário envia `participacaoId` público como identificação de contexto; a origem é Participação → Atividade → SCI → Projeto → Laboratório → Unidade.
2. Participação aberta não equivale a elegível. Reavaliar perfil, titularidade, Unidade/tenant, vínculo, datas e hierarquia na criação.
3. Sem participação elegível: bloquear criação; nunca cair para `Usuario.laboratorio`. Com uma: pré-seleção somente visual; com várias: seleção explícita pela UI.
4. Para TECNICO/ANALISTA/PESQUISADOR: Projeto informado determina Laboratório; Pedido de Produtos sem Projeto mantém laboratório-base quando permitido. Solução continua exigindo Projeto.
5. Gestor/Admin: respeitar laboratórios da Unidade e consistência com Projeto.
6. Rejeitar campos legados contraditórios; evitar exigi-los no novo fluxo Estagiário. Preservar compatibilidade com Pedidos legados e demais perfis.
7. Persistir referência à participação **e snapshot de origem** suficiente para que edição futura de participação ou hierarquia não reescreva o histórico.
8. Não revalidar elegibilidade histórica como condição para consultar ou concluir pedidos já criados. Avaliar as transições e permissões próprias separadamente, conforme contrato.
9. Não modificar o motor físico de estoque: FEFO/FIFO, fracionamento do lote, locks, baixa única na aprovação, devolução/cancelamento e rastreabilidade continuam no backend.
10. Soluções são `Pedido.tipo=SOLUCAO`, sem produto artificial; composição/aprovação integral e snapshots atuais não podem regredir.

## Ordem técnica

- **8.1:** definir resolução autoritativa por perfil e desenho da persistência histórica; revisar `PedidoRequestDTO`, `Pedido`, `PedidoResponseDTO`, `PedidoService`, repositórios, testes e migration.
- **8.2:** testes da criação 0/1/N, cross-tenant, solicitante falso (segurança definitiva exige identidade), participação futura/encerrada, vínculo inativo, alteração posterior de Atividade, legado e ciclo de aprovação.
- **8.3:** integrar formulários frontend ao contrato **depois** da validação do backend.
- **8.4:** teste operacional completo do cadastro à rastreabilidade, preservando estoque e Soluções.
- **8.5:** consolidar documentação/testes, conferir sincronização e aguardar MR manual do responsável.

## Portões e limites

- Fonte canônica: `GitLab/main`; GitHub é espelho da main e origem de colaboração em `collab/*`.
- Nunca escrever em main, fazer merge, force push ou sobrescrever alterações do supervisor.
- Alterações funcionais do **backend são realizadas manualmente pelo responsável**; IA analisa, propõe código de referência e revisa. Não afirmar testes verdes sem execução/evidência.
- `SecurityConfig` ainda usa `permitAll()`; o cabeçalho DEV não substitui identidade autenticada. O endpoint pessoal de contextos precisa de titularidade autenticada para uso real. Não declarar segurança de produção.
- Política institucional de visibilidade de Projetos por perfil permanece pendente de confirmação; não ampliar alcance por suposição.
- Respeitar `docs/SINCRONIZACAO_GITLAB_GITHUB.md` e `docs/PLANO_PRE_PRODUCAO.md`.
- Delete lógico/inativação geral não deve ser incluído por causa de numerações históricas de documentos; seguir roadmap vigente.
- **Este commit é somente documental, não é implementação funcional nem aceite de sincronização com o GitLab.**
