# Etapa 7.6.0 — Auditoria antes de integrar Pedidos convencionais ao estoque físico

**Data:** 15/10/2026  
**Branch:** `collab/etapa-7-unidades-solucoes-contexto`  
**Natureza:** inventário técnico, matriz de lacunas e contrato de evolução. **Não é execução de testes nem implementação funcional.**

## 1. Escopo exato e limites

Completar o ciclo **Pedido convencional de Produtos → aprovação pela Gestão → saída de recipientes físicos → rastreabilidade/cancelamento/entrega**, usando o estoque estabilizado na 7.1/7.2. Não implementar aqui pedido de quantidade proporcional de Solução padrão, mistura Produto/Solução no mesmo Pedido nem associação `participacaoId` de Estagiário (Etapa 8).

Documentos confrontados: `docs/ETAPA_7_MODELO_ESTOQUE.md`, `docs/PLANO_PRE_PRODUCAO.md`, `docs/CONTINUIDADE_ETAPA_7_2026-10-06.md`, `CONTINUIDADE.md`, `docs/CONTRATO_CONTEXTOS_PEDIDOS_ETAPA_8.md`; fonte efetiva nos DTOs, `PedidoService`, `MovimentacaoEstoqueService`, `LoteRepository`, `RecipienteEstoque` e testes.

## 2. Base backend JÁ implementada — não duplicar

| Regra / estrutura | Código existente e decisão |
|---|---|
| Unidade canônica por Produto e conversão entre unidades compatíveis | `UnidadeMedida` e `ConversorUnidadeMedida`; nunca inventar conversão massa↔volume |
| Valores físicos decimais | `BigDecimal` nos saldos, Pedido/Itens, movimentações; V33 `NUMERIC(19,6)` |
| Apresentação distinta da quantidade física | `Lote.tipoEmbalagem`, `conteudoPorApresentacao`, `fracionavel`, `quantidadeApresentacoes` |
| Recipientes com identidade e saldo próprios | `RecipienteEstoque` e V34 (`FECHADO`/`ABERTO`/`ESGOTADO`) |
| Movimentação rastreável por recipiente | `MovimentacaoRecipiente` e V35; abertura/esgotamento e saldos anterior/atual |
| Lotes elegíveis e ordem de consumo | `buscarLotesParaSaida()`: perecíveis por FEFO (sem vencidos), demais por FIFO |
| Seleção física | `consumirRecipientes()`: lote não fracionável usa fechados inteiros; lote fracionável prioriza fechados para múltiplos inteiros da apresentação, abertos para frações e abre fechado quando necessário |
| Saldo e concorrência | locks `PESSIMISTIC_WRITE`, revalidação do saldo físico contra lote, transação, conflitos HTTP 409 |
| Cancelamento antes da entrega | `devolverSaidasDoPedido()` recompõe exatamente os recipientes consumidos se não houve movimentação concorrente posterior; do contrário falha sem inventar devolução |
| Soluções | composição decimal com unidade física e aprovação integral (sem aprovação parcial de componentes) — não modificar no 7.6.0 |

Testes existentes em `MovimentacaoEstoqueServiceTest`: FIFO, FEFO multi-lote, fechado inteiro, fração decimal, aberto antes de fechado, complemento fechado+aberto, abertura, saldo divergente, descarte, devoluções. `PedidoConcorrenciaIntegrationTest`: dois pedidos concorrendo pelo mesmo saldo. **Não repetir suíte equivalente no frontend**; testar apenas o contrato na borda e fluxos ponta a ponta.

## 3. Desalinhamentos efetivamente encontrados

| Código | Achado comprovado | Implicação |
|---|---|---|
| PF-01 | `ItemPedidoRequestDTO.quantidadeSolicitada` e `quantidadeAprovada` são decimais, mas Pedido comum requer `tipoEmbalagemSolicitada`, `quantidadeEmbalagensSolicitada: Integer` e `multiplicadorSolicitado: BigDecimal` com `quantidadeSolicitada = quantidadeEmbalagens × multiplicador`; `UNITARIO` exige multiplicador 1 | Não representa solicitação de 0,150 L ou 0,5 g com quantidade de apresentações inteira |
| PF-02 | `NovoPedidoView.vue` soma saldos por lote, cria opções `UNITARIO::1`, calcula outras formas por `Math.floor(saldoLote / conteudoPorApresentacao)`, e solicita `quantidadeForma` inteira com `min=1` | Não oferece seleção explícita de **quantidade física com unidade**; disponibilidade de embalagem fechada é apenas estimada |
| PF-03 | `PedidosGestaoView.vue` valida com `Number.isInteger`, `<input min=1>`, step 1 (UNITARIO) ou multiplicador | Bloqueia aprovações físicas decimais mesmo quando backend suporta `BigDecimal` |
| PF-04 | `PedidoService.validarFormaRetirada` e aprovação exigem múltiplos da apresentação para Pedido comum não-UNITARIO | A flexibilização exige **contrato backend mínimo**, não só retirada do `min=1` no Vue |
| PF-05 | `MovimentacaoEstoqueService.loteCompativelComFormaSolicitada` filtra lote por tipo/fator; para lote fracionável, `consumirRecipientes` pode completar quantidade de apresentação usando recipientes `ABERTO` se não houver fechados | Selecionar "1 GARRAFA/FRASCO lacrado" **não garante** lacre no atendimento por si só. Confirmar semântica e reforçar a seleção/revalidação se o pedido exige embalagem fechada |
| PF-06 | `LoteResponseDTO` traz `unidadeBase` e demais informações do lote; o tipo frontend `LotePedidoResponse` omite a unidade e apresenta enum de embalagem limitado (`UNITARIO`, `KIT`, `CAIXA`, `GARRAFA`, `GALAO`) frente ao domínio ampliado | UI não consegue rotular corretamente todas as unidades/apresentações do estoque; expandir type compatível antes dos controles |
| PF-07 | Disponibilidade em lotes é `quantidadeDisponivel` agregada; pode existir saldo apenas em recipientes ABERTOS | Contagem `floor(saldo/fator)` não é disponibilidade autoritativa de apresentações lacradas. Somente o backend, ao inspecionar recipientes, decide; UX deve distinguir estimativa do garantido |
| PF-08 | Código de Pedidos tem estilos legados globais com `!important`, fontes de 6,5–7,5 px e cores fixas | Revisar primeiro a origem da regra CSS, sem adicionar outra folha de overrides; seguir `SGL-FRONTEND/docs/REGRA_PADRONIZACAO_NOVOS_MODULOS.md` |

O uso de `BigDecimal` **não** implica que o contrato de retirada física já esteja pronto para todo tipo de Pedido.

## 4. Contrato-alvo, sem decidir um schema prematuramente

Manter duas escolhas funcionalmente distintas:

1. **Apresentação completa** — pessoa escolhe tipo de embalagem + número inteiro de apresentações; a aprovação deve garantir unidades físicas inteiras e, **quando a opção promete lacre**, recipientes efetivamente FECHADOS. Não reunir frações de recipientes ABERTOS para fingir embalagem lacrada.
2. **Quantidade fracionada** — pessoa escolhe quantidade física positiva e unidade compatível com o Produto (ex.: 150 mL, 0,5 g); somente lotes/recipientes que **permitem fracionamento** podem atendê-la; o backend converte para unidade canônica e valida precisão e saldo. Em medida discreta (unidades/reação), não presumir fração sem regra institucional.

**Ambas continuam submetidas à FEFO/FIFO de lotes.** Dentro do lote, priorizar ABERTO em retirada fracionária; para apresentação fechada, FECHADO. Ao atingir zero, estado ESGOTADO. Não escolher lote ou recipiente autoritativamente no navegador. Apresentação completa em vários lotes compatíveis requer preservação do compromisso físico; se não puder cumprir, resposta de negócio clara sem baixa parcial.

Os dados históricos/DTOs existentes devem permanecer legíveis. Definir no próximo bloco se a escrita utilizará discriminador de modo e campos opcionais novos ou outra estratégia compatível — a ideia **não** é fazer `quantidadeEmbalagensSolicitada` aceitar decimal ou transformar `UNITARIO` implicitamente em modo físico fracionário sem distinguir intenção.

## 5. Ordem para mudança segura

- **7.6.0-A — Contrato de Pedido:** estabelecer no backend os dois modos (apresentação inteira / quantidade fracionada) com validação inequívoca, unidade canônica, precisão e invariantes de recipiente. Alterar somente o necessário na borda de Pedido/Movimentação; **não reescrever FIFO/FEFO, locks, V33–V35 ou estoque físico**.
- **7.6.0-B — Testes do contrato:** documentar/incluir testes focados nos comportamentos novos: fração decimal com unidade, recusa em lote não fracionável, seleção ABERTO antes de FECHADO, lacrado indisponível apesar de saldo agregado, prioridade FEFO/FIFO, lotes mistos, quantidade inválida, concorrência e rollback. Reexecutar os testes já existentes.
- **7.6.0-C — Frontend Solicitante:** separar apresentação inteira de quantidade física fracionária, com unidades apropriadas, disponibilidade explicada, estados carregando/erro, normalização de dados, sem calcular FEFO/FIFO no browser.
- **7.6.0-D — Frontend Gestão:** aprovar quantidade decimal ou apresentações, com passos e limites correspondentes, exibir modo/unidade e baixa rastreada. Não alterar o contrato atômico de Soluções.
- **7.6.0-E — Regressão:** criar/aprovar/entregar/cancelar, verificar saldos Recipiente→Lote→Estoque, movimentos, sem dupla baixa e sem falsos lacres; validação Light/Dark/mobile/desktop; confirmar `mvn test`, boot e `npm run build`.

## 6. Critérios de não regressão

- Não consumir lote de outra Unidade nem lote vencido quando FEFO.
- Saída e baixa ocorrem **uma única vez na aprovação**, e entrega não repete.
- Aprovação mantém o Pedido PENDENTE se houver conflito físico; transações revertem baixa parcial.
- Sem registros duplicados de movimentação; cancelamento recompõe apenas quando legitimamente reversível.
- Sem alterar comportamento de Soluções (inclusive preparo/cancelamento) ou de Pedidos legados.
- Segurança: `X-SGL-Unidade-Id` em DEV **não** prova autorização real. Não aprovar produção antes da autenticação/autorização definitiva.
- Nenhum merge sem autorização.

**Estado da auditoria:** evidências conferidas por leitura estática de arquivos da branch; sem testes executados nesta rodada. A implementação da 7.6.0 **ainda não começou**.
