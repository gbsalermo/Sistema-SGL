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
| PF-05 | `loteCompativelComFormaSolicitada` filtra tipo/fator; `consumirRecipientes` prioriza FECHADOS para múltiplos completos, depois ABERTOS para a fração e só abre outros fechados para o restante | **Contrato já fechado nas seções 9.9/9.13 da continuidade 7**: preservar recipientes abertos quando uma apresentação inteira fechada compatível está disponível; NÃO inventar uma obrigação geral de lacre. Verificar no teste a priorização e o fallback físico real, sem prometer lacre indiscriminadamente |
| PF-06 | `LoteResponseDTO` traz `unidadeBase` e demais informações do lote; o tipo frontend `LotePedidoResponse` omite a unidade e apresenta enum de embalagem limitado (`UNITARIO`, `KIT`, `CAIXA`, `GARRAFA`, `GALAO`) frente ao domínio ampliado | UI não consegue rotular corretamente todas as unidades/apresentações do estoque; expandir type compatível antes dos controles |
| PF-07 | Disponibilidade em lotes é `quantidadeDisponivel` agregada; pode existir saldo apenas em recipientes ABERTOS | `floor(saldo/fator)` não mede o número de FECHADOS nem a retirada física exata. A UI não deve prometer recipiente fechado com base apenas no total agregado; o servidor decide a seleção conforme 9.9/9.13 |
| PF-08 | Código de Pedidos tem estilos legados globais com `!important`, fontes de 6,5–7,5 px e cores fixas | Revisar primeiro a origem da regra CSS, sem adicionar outra folha de overrides; seguir `SGL-FRONTEND/docs/REGRA_PADRONIZACAO_NOVOS_MODULOS.md` |

O uso de `BigDecimal` **não** implica que o contrato de retirada física já esteja pronto para todo tipo de Pedido.

## 4. Contrato funcional ORIGINAL — já decidido, não rediscutir

**Fonte autoritativa:** `docs/CONTINUIDADE_ETAPA_7_2026-10-06.md`, **§9.9 (FEFO acima de estado aberto/fechado)** e **§9.13 (retirada inteira preserva recipientes abertos)**, com exemplos aprovados no §9.3. A implementação vigente está em `MovimentacaoEstoqueService.buscarLotesParaSaida`, `calcularQuantidadeFisicamenteConsumivel`, `consumirRecipientes` e `aplicarConsumoRecipiente`.

**Precedência imutável:**

1. Entre lotes, vale **FEFO para perecíveis, FIFO para os demais**. Recipiente aberto de lote posterior não ultrapassa o lote mais prioritário.
2. Dentro do lote, a **parcela inteira da apresentação** prioriza recipientes **FECHADOS**, preservando os ABERTOS.
3. Para a **parcela fracionária restante**, prioriza recipientes **ABERTOS**, ordenados por data de abertura, menor saldo e sequencial para desempate.
4. Abrir outro FECHADO **somente quando os abertos disponíveis não forem suficientes** para o restante; consumir unidades fechadas completas primeiro reduz a abertura desnecessária em pedidos grandes. Isto é **uma estratégia de minimização de novas aberturas**, não um limite numérico arbitrário de recipientes abertos inventado no frontend.
5. Lote marcado **não fracionável** só admite apresentação inteira/recipiente inteiro; registrar estados e quantidades exatas por recipiente, revalidando após locks na aprovação.

**Exemplos expressamente aprovados** (lote prioritário com 1 aberto de 300 mL e 2 fechados de 500 mL):
- Pedido 500 mL → retirar um FECHADO de 500 mL; preservar aberto.
- Pedido 200 mL → retirar 200 mL do ABERTO; restam 100 mL.
- Pedido 700 mL → retirar FECHADO de 500 mL + 200 mL do ABERTO; não abrir outro.

**Não foi aprovada regra nova "toda apresentação completa é lacrada garantida mesmo sem fechado compatível".** O contrato fala em **prioridade por fechado** e em evitar aberturas desnecessárias; se a UI oferecer explicitamente `lacrado` como promessa futura, isso deverá ser sustentado pelo contrato físico próprio, e não pressuposto nesta etapa.

**Lacuna de integração, não lacuna de decisão:** Pedido comum continua exigindo contagem inteira de embalagens no `ItemPedidoRequestDTO`, e o frontend impede quantidade/aprovação decimal. Adaptar a **representação da quantidade física solicitada e sua unidade** para chegar ao motor existente, preservando compatibilidade com apresentações e histórico. Não converter `quantidadeEmbalagensSolicitada` para decimal (continua contagem) nem duplicar FEFO/FIFO no browser. O formato exato do request será determinado pela implementação mínima retrocompatível, e não por redefinição de regras de estoque.

## 5. Ordem para mudança segura

- **7.6.0-A — Borda de Pedido:** adaptar o request/validação para encaminhar quantidades físicas decimais e unidade compatível, mantendo contagem inteira para apresentações; **preservar as regras já fechadas §9.9/§9.13**. Não reescrever FIFO/FEFO, seleção física, locks, V33–V35 ou inventar restrição de lacre.
- **7.6.0-B — Testes do contrato:** usar os testes existentes de FEFO/FIFO e de 200/500/700 mL como referência. Adicionar somente testes de integração de Pedido→estoque para quantidade decimal, lote não fracionável, pedidos grandes sem abertura desnecessária, lote mais antigo prioritário, limites de saldo e rollback. Não duplicar testes do algoritmo puro.
- **7.6.0-C — Frontend Solicitante:** separar apresentação inteira de quantidade física fracionária, com unidades apropriadas, disponibilidade explicada, estados carregando/erro, normalização de dados, sem calcular FEFO/FIFO no browser.
- **7.6.0-D — Frontend Gestão:** aprovar quantidade decimal ou apresentações, com passos e limites correspondentes, exibir modo/unidade e baixa rastreada. Não alterar o contrato atômico de Soluções.
- **7.6.0-E — Regressão:** criar/aprovar/entregar/cancelar, verificar saldos Recipiente→Lote→Estoque, movimentos, sem dupla baixa e sem promessa de lacre não verificada; validar exemplos 200/500/700 mL, Light/Dark/mobile/desktop, `mvn test`, boot e `npm run build`.

## 6. Critérios de não regressão

- Não consumir lote de outra Unidade nem lote vencido quando FEFO.
- Saída e baixa ocorrem **uma única vez na aprovação**, e entrega não repete.
- Aprovação mantém o Pedido PENDENTE se houver conflito físico; transações revertem baixa parcial.
- Sem registros duplicados de movimentação; cancelamento recompõe apenas quando legitimamente reversível.
- Sem alterar comportamento de Soluções (inclusive preparo/cancelamento) ou de Pedidos legados.
- Segurança: `X-SGL-Unidade-Id` em DEV **não** prova autorização real. Não aprovar produção antes da autenticação/autorização definitiva.
- Nenhum merge sem autorização.

**Estado da auditoria:** evidências conferidas por leitura estática de arquivos da branch; sem testes executados nesta rodada. A implementação da 7.6.0 **ainda não começou**.


## 7. Checkpoint de implementação 7.6-A — 15/10/2026

A borda de Pedido convencional foi ajustada na branch de trabalho para aceitar **dois modos exclusivos**:

- **Apresentação:** conserva `tipoEmbalagemSolicitada`, `quantidadeEmbalagensSolicitada: Integer` e `multiplicadorSolicitado: BigDecimal`, bem como a validação do total solicitado.
- **Quantidade física:** informa `quantidadeSolicitada: BigDecimal` e `unidadeMedidaSolicitada`, **sem os três campos de apresentação**; a quantidade é convertida à unidade canônica do Produto e deve caber em seis casas decimais.

`PedidoRequestDTO.isFormaRetiradaValida` impede combinar os modos. `ItemPedido.@PrePersist` preserva os campos nulos de apresentação no modo físico (sem preenchê-los como `UNITARIO`); a aprovação de produto físico deixa de impor múltiplos da embalagem, mas continua validando quantidade positiva, teto solicitado e invocando `MovimentacaoEstoqueService.registrarSaida` de forma transacional. O motor físico não foi alterado.

Testes adicionados em `PedidoControllerTest` para aceitação decimal e rejeição de payload misto e em `PedidoServiceTest` para aprovação fracionada sem restrição de embalagem. **Testes não executados nesta sessão**: GitHub acessível pelo conector, mas o ambiente local de execução não consegue resolver `github.com` para clonar a branch. Não declarar a etapa verde antes de executar `mvn test` no ambiente do projeto.

**Pendente para encerrar 7.6-A/B:** regressão de criação e aprovação ponta a ponta, comprovação dos cenários 200/500/700 mL, misto de lotes e não fracionáveis, rollback/conflito e compatibilidade das apresentações legadas. Frontend e Soluções não modificados neste checkpoint. Sem merge.

## 8. Checkpoint 7.6-E — regressão curta e pendências (16/10/2026)

**Aceites informados pelo responsável:** 7.6-A backend (build/suíte/boot), 7.6-B seis testes JUnit integrados (captura verde), 7.6-C Novo Pedido e 7.6-D aprovação na Gestão após refinamento visual. A validação visual da D é aceite funcional, **não** homologação ponta a ponta de todo o SGL.

**Conferência estática nesta rodada:** formulário Solicitante mantém dois modos exclusivos; pedidos por unidade/REAÇÃO não mostram campo livre de fração; request por medida envia quantidade e unidade, deixando embalagem nula. Gestão distingue modo físico, permite aprovação decimal até 6 casas e preserva múltiplos para embalagens. Frontend e backend mantêm seleção FEFO/FIFO e recipientes **somente no servidor**. Dados DEV do Formaldeído ("frasco 500 mL", saldo 8 mL, multiplicador histórico 15) são incoerentes após migração e **não** demonstram falha do algoritmo; revisar na 7.6.1 sem converter históricos automaticamente.

### Checklist mínimo para aceite de regressão curta

1. Backend `mvn test` (suíte integral, não apenas classe nova) e inicialização DEV sem erro.
2. Frontend `npm run build` / type-check; Novo Pedido apresentação inteira e medida decimal.
3. Pedido por medida de material fracionável, unidade compatível, aprovação (total ou parcial), baixa uma vez, entrega sem segunda baixa.
4. Pedido por apresentação inteira e não fracionável; bloqueio de fracionamento indevido e conservação de status/saldos no conflito.
5. Lotes vencidos e priorização FEFO/FIFO, rastreio do lote/recipiente, cancelamento reversível quando aplicável (comparar com testes JUnit existentes).
6. Pedidos antigos e Soluções (aprovação/preparo/cancelamento) permanecem funcionais, sem mudança de receita.
7. Layout em Light/Dark, desktop 720p/1080p/1440p e mobile; foco, contraste, avisos, inputs e ausência de cortes no painel Gestão.
8. Conferir estoque coerente na 7.6.1 DEV/DEMO e registrar divergências em dados iniciais, sem ajustar o algoritmo para acomodar massa antiga.

**Fora do aceite atual por decisão do responsável:** roteiro manual exaustivo Cadastro de Produto → Entrada → Pedido → Aprovação → Entrega → Rastreabilidade, reservado à Etapa 8. Antes dele, 7.6.1 corrige massas DEV/DEMO e 7.7 realiza QA/consolidação.

**Estado:** auditoria estática e roteiro registrados. Não alegar execução local de build ou smoke nesta rodada; os resultados anteriores A–D foram comunicados pelo responsável. Sem merge.
