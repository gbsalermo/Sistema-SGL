# Etapa 9.1 — Auditoria inicial dos contratos de Relatórios

**Data:** 26/10/2026  
**Estado:** auditoria estática inicial; nenhuma funcionalidade nova implementada; testes desta etapa ainda não executados.  
**Branches:** `collab/etapa-9-relatorios-consolidados` (backend e frontend).

## Fontes inspecionadas

Backend: `RelatorioMovimentacoesController`, `RelatorioResumoOperacionalController`, `RelatorioExportacaoController`, `RelatorioMovimentacoesService`, `RelatorioResumoOperacionalService`, `MovimentacaoEstoqueRepository`, `ProjetoController`, documentação `RELATORIOS.md` e contrato da Etapa 9.

Frontend: `RelatoriosGestaoView.vue`, `relatorioService.ts`, inventário e contrato de início da Etapa 9.

## Constatações do código

1. **Compatibilidade de prévia/exportação:** Movimentações possui GET `/api/v1/relatorios/movimentacoes` e `/exportar`, ambos chamando `RelatorioMovimentacoesService.gerar(...)` com os mesmos parâmetros de tipo, origem, produto, laboratório, usuário, lote e período. Resumo Operacional possui GET `/api/v1/relatorios/resumo-operacional` e `/exportar`, ambos chamando `RelatorioResumoOperacionalService.gerar(...)` com produto, período e limite. Assim, é possível consolidar os modos na UI **sem remover endpoints nem reproduzir agregações no cliente**.
2. **Carregamento excessivo confirmado no código:** os dois serviços invocam `movimentacaoRepository.findAll().stream()` e filtram em memória. `MovimentacaoEstoqueRepository.findAll()` já possui JPQL com filtro por tenant quando o tenant existe, mas ainda retorna todo o histórico da Unidade, sem recorte de filtros/período no banco. Existe risco de custo crescente e carregamento de associações para DTOs; não foi medido N+1 nem latência real.
3. **Limite de tenant a preservar:** a JPQL existente de `findAll()` usa `(:#{@tenantProvider.unidadeId} IS NULL OR ...)`, portanto o isolamento no nível dessa consulta depende do contexto carregado. Isso não é evidência de vazamento em requisição normal, mas a futura consulta de relatório precisa definir Unidade obrigatória no caso de uso e não aceitar filtragem ampla inadvertida. Autenticação corporativa permanece escopo separado.
4. **Relatório de Projetos ainda não entregue:** `ProjetoController` oferece o CRUD e as consultas operacionais, não um endpoint de relatório consolidado com exportação PDF/XLSX; requer contrato de consulta agregada. O hub de Projetos não deve ser confundido com relatório.
5. **Central atual:** `RelatoriosGestaoView.vue` define opções `movimentacoes` e `resumo-operacional` separadas. Os seus filtros e serviços já separam Resumo e Detalhamento, portanto a Etapa 9.3 pode reaproveitar a lógica existente sem duplicar consultas oficiais.
6. **Lacuna adicional de UI a conferir:** na função `visualizarRelatorio()`, quando selecionado `residuos`, aparece mensagem antiga afirmando que o módulo ainda depende de integração futura; isso não condiz com o estado consolidado da Etapa 8. Antes de modificar, confirmar a navegação efetiva da Central/rota dedicada e a exportação para não quebrar o módulo.
7. **Carga inicial:** `onMounted` da Central carrega paralelamente Laboratórios, Unidades, Produtos, Usuários e Lotes, independentemente do relatório selecionado. É um ponto de auditoria de carga/rede a tratar se houver necessidade, sem inventar métricas.

## Caminho de implementação acordado

**9.1-A:** completar contrato de filtros e agregações para Projetos, revisando `ProjetoRepository`, `SciRepository`, `AtividadeRepository`, DTOs e formato dos relatórios existentes; escolher índice/consulta que preserve Unidade.  
**9.1-B:** propor primeiro ajuste **incremental de backend para execução manual do responsável**: consulta filtrada por Unidade e parâmetros no `MovimentacaoEstoqueRepository`, usada pelos dois services, preservando assinatura HTTP, formato de saída, ordenação, valores e exportações. Antes, adicionar testes para filtros, período e tenant.  
**9.2:** Relatório de Projetos com prévia/exportação.  
**9.3:** unificar opções na UI como Movimentações > Resumo/Detalhamento.  
**9.4:** build, regressão e documentação.

Sem modificações no motor FEFO/FIFO, pedidos, Estoque ou autenticação. Não há autorização de merge.
