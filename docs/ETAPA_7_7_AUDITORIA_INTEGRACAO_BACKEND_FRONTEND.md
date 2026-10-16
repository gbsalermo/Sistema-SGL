# Etapa 7.7 — Auditoria inicial de integração backend/frontend

Data: 16/10/2026. Branch: `collab/etapa-7-unidades-solucoes-contexto`.
Escopo: Estoque, Pedidos e Soluções; conferência de contratos, documentos e pendências. **Leitura estática** dos repositórios, sem execução de build/JUnit nesta rodada e sem merge.

## Integrações conferidas

- `PedidoController` expõe `/api/v1/pedidos`, `/{id}/aprovar`, `/{id}/rejeitar`, `/{id}/entregar`, `/{id}/cancelar`, `/por-usuario`, `/por-status` e `/por-urgencia`. O `pedidoService.ts` usa caminhos correspondentes.
- `ItemPedidoRequestDTO` permite quantidade decimal física com `unidadeMedidaSolicitada` ou apresentação com os campos de embalagem, coerente com os tipos do frontend; Soluções continua possuindo serviço e endpoints próprios.
- `EstoqueCentralResponseDTO` contém `produtoUnidadeMedida`, `quantidadeAtual`, `quantidadeMinima` e resumo `apresentacoesFisicas`, conforme o contrato usado pela tabela principal.
- `RecipienteEstoqueRepository` disponibiliza consulta por estoque para a composição do resumo, sem estimar estado no frontend.
- Confirmações já fornecidas pelo responsável em turnos anteriores: backend/JUnit/API verdes, build frontend verde, solicitação/aprovação/entrega curta validada, apresentação de embalagens/saldo/estados na tabela validada.

## Pendências reais identificadas — priorização

### P1 — Consistência do DTO de estoque entre endpoints

`EstoqueCentralService.listarPorUnidade()` enriquece o `EstoqueCentralResponseDTO` com `apresentacoesFisicas`; `buscarPorId()`, `listarTodos()` e `buscarPorUnidadeEProduto()` continuam usando o construtor simples e retornam a propriedade vazia. Esse descompasso pode confundir consumidores que reutilizem o DTO. Unificar mapeamento ou oferecer DTO especializado para listagem. **Não é falha comprovada no painel já validado**, que usa `listarPorUnidade()`.

### P2 — Performance de listagem e agregação física

`comResumoFisico()` consulta recipientes separadamente para cada item do estoque (potencial N+1) e soma os estados em memória. Para a massa atual o painel funcionou; antes da produção avaliar consulta agregada por unidade, tipo e capacidade, com isolamento de tenant, principalmente em instalações maiores.

### P2 — Descrições legadas sobre unidades

`LoteResponseDTO` ainda descreve `quantidadeInicial` / `quantidadeDisponivel` como "unidades individuais", enquanto o modelo usa unidade canônica. A tela de detalhes de estoque e fluxos de entrada também preservam termos "unit." e regras de multiplicador inteiro para todo tipo de produto; revisar de forma consistente antes da homologação ampla do ciclo de cadastro/entrada (Etapa 8). Não mudar aritmeticamente os saldos.

### P2 — Massa DEMO com cenários legados

`DemoDataInitializer` usa capacidade física explícita para produtos conhecidos e fallback para outros. Casos `fracionavel=false` ainda são materializados como recipientes de capacidade 1 (ex.: caixas com várias unidades); verificar a representação real de caixas/kits, validade e consistência para utilização operacional. Apenas massa de teste, não regra do motor.

### P3 — Mensagem de erro obsoleta

O frontend `estoqueService.ts` orientava "main" e migrations V7/V8 ao encontrar contrato de lote inválido. Mensagem corrigida na branch, sem mudança funcional de API.

## Portões de fechamento

1. Reconciliar DTO e avaliar custo da agregação de recipientes.
2. Corrigir textos/contratos de entrada e qualificar massa DEMO não fracionável.
3. Validar `mvn test`, boot DEV, `npm run build` e smoke Light/Dark após correções.
4. Preservar o teste manual *exaustivo* Cadastro → Entrada → Pedido → Aprovação → Entrega → Rastreabilidade para Etapa 8, como decidido pelo responsável.
5. Não declarar produção pronta nem efetuar merge sem autorização.

## Fontes verificadas

- Backend: `EstoqueCentralController`, `EstoqueCentralService`, `EstoqueCentralResponseDTO`, `RecipienteEstoqueRepository`, `PedidoController`, `ItemPedidoRequestDTO`, `LoteResponseDTO`, `DemoDataInitializer`.
- Frontend: `EstoqueGestaoView.vue`, `EstoqueDetalheView.vue`, `estoqueService.ts`, `pedidoService.ts`, `solucaoService.ts`, `types/pedido.ts`, `types/estoque.ts`.

## 7.7 — Remediação automatizada (16/10/2026)

- **P1 tratado:** `EstoqueCentralService` utiliza `comResumoFisico` em criar, atualizar, buscar por ID, buscar por unidade/produto e estoque baixo; listagens usam `comResumosFisicos`. A resposta `apresentacoesFisicas` passa a ter o mesmo significado em todos esses endpoints.
- **P2 desempenho tratado para listagem:** `RecipienteEstoqueRepository.buscarPorUnidadeParaResumo` consulta os recipientes da unidade uma vez, com `JOIN FETCH` do lote, e o serviço distribui o resultado por ID do estoque. Consultas individuais ainda buscam recipientes do registro solicitado, sem necessidade de uma busca da unidade inteira.
- **P2 terminologia parcialmente tratado:** descrições de `LoteResponseDTO` usam unidade canônica; formulário de entrada da tela Estoque deixa de chamar indistintamente o multiplicador de "unidades".
- **P2 DEMO permanece sob ressalva:** itens não fracionáveis como caixas de ponteiras/luvas têm `UNIDADE` canônica e valores originais que não estabelecem uma correspondência inequívoca entre o conteúdo comercial descrito e cada unidade de saldo. Reinterpretar essas quantidades automaticamente criaria saldos históricos fictícios. Exige revisão explícita dos cenários da massa descartável e seus pedidos de demonstração antes do aceite total, sem alterar o motor do estoque.

**Limite de validação:** mudanças publicadas na branch; não foi executado build/teste backend ou frontend por esta sessão. Não encerrar a 7.7 sem `mvn test`, boot e `npm run build`. A validação operacional exaustiva permanece na Etapa 8.
