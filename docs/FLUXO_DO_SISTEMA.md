# Fluxo do Sistema SGL

**Atualizado em:** 04/09/2026

Este documento descreve como os módulos principais se conectam no estado funcional aprovado. Detalhes de contrato devem ser confirmados no Swagger/OpenAPI e detalhes de implementação na `main`.

---

## 1. Contexto institucional

```text
Unidade
  → Laboratório
      → Usuários
      → Projetos

Produto
  → EstoqueCentral da Unidade
```

Regras atuais:

1. Unidade é entidade institucional do domínio.
2. Laboratórios pertencem a uma Unidade.
3. Usuários pertencem a uma Unidade e, quando aplicável, a um Laboratório.
4. Projetos pertencem ao contexto do Laboratório/Unidade.
5. Produtos formam o catálogo.
6. Cada Unidade possui seu próprio contexto de estoque para os produtos utilizados.

No modo DEV, o frontend envia `X-SGL-Unidade-Id` e o backend usa `TenantContext` para restringir operações à Unidade corrente. Esse mecanismo ainda não substitui a futura identidade corporativa confiável.

---

## 2. Entrada de estoque

```text
contexto da Unidade + Produto
  → informar lote/apresentação/quantidade
  → Service valida o registro
  → atualiza saldo do EstoqueCentral
  → atualiza/cria Lote
  → grava MovimentacaoEstoque
```

A movimentação registra a operação física e o lote afetado quando aplicável.

---

## 3. Saída manual

```text
saldo/lotes disponíveis
  → validar quantidade
  → selecionar lote conforme regra aplicável
  → impedir saldo negativo
  → subtrair quantidade
  → registrar movimentação
```

A alteração de saldo e a movimentação pertencem à mesma operação transacional.

---

## 4. Criação do pedido

O usuário informa laboratório, projeto opcional e itens solicitados.

Validações incluem:

- usuário, laboratório, projeto e produtos devem existir;
- usuário e laboratório devem pertencer ao contexto institucional permitido;
- projeto, quando informado, deve ser compatível com o laboratório;
- entidades envolvidas devem estar ativas;
- o mesmo produto não pode aparecer duas vezes;
- deve existir estoque ativo do produto no contexto da Unidade;
- forma de retirada deve ser compatível com a apresentação do produto/lote.

O saldo **não é reduzido na criação**. O pedido é salvo como `PENDENTE`.

---

## 5. Aprovação

```text
Aprovador + Pedido PENDENTE
  → valida itens/quantidades
  → localiza estoque e lotes da Unidade
  → exclui lote vencido da seleção
  → perecível: FEFO
  → não perecível: FIFO
  → reduz lotes utilizados
  → atualiza EstoqueCentral
  → grava quantidade aprovada
  → grava MovimentacaoEstoque SAIDA/PEDIDO
  → altera Pedido para APROVADO
```

Todo o processamento é transacional. Se um item falhar, nenhuma baixa parcial deve permanecer.

Urgência não altera FIFO/FEFO.

---

## 6. Rejeição

Somente pedido `PENDENTE` pode ser rejeitado pelo fluxo comum.

```text
PENDENTE
→ registrar motivo/observação
→ REJEITADO
```

Não altera estoque.

---

## 7. Entrega

Somente pedido `APROVADO` pode ser entregue.

```text
APROVADO
→ registrar HistoricoLaboratorio dos itens aprovados
→ registrar data real de entrega
→ ENTREGUE
```

A entrega **não reduz o estoque novamente**, porque a baixa física aconteceu na aprovação.

---

## 8. Cancelamento

- `PENDENTE`: cancela sem alterar estoque.
- `APROVADO`: restaura as quantidades dos **lotes exatos utilizados na aprovação** e muda para `CANCELADO`.
- `ENTREGUE`: não pode ser cancelado pelo fluxo comum.
- `REJEITADO` ou `CANCELADO`: já está encerrado.

A restauração exata dos lotes é a garantia funcional atual. Não documentar como obrigatória uma movimentação `DEVOLUCAO` específica enquanto o fluxo corrente não garantir seu registro em todos os caminhos de cancelamento.

---

## 9. Resíduos

Resíduo é domínio próprio:

```text
Produto != Resíduo
```

Um componente de Resíduo pode referenciar Produto apenas para rastreabilidade. Essa referência não baixa nem repõe estoque automaticamente.

Fluxo:

```text
INFORMADO
→ EM_ANALISE
→ LIBERADO_PARA_ARMAZENAMENTO
→ ARMAZENADO_TEMPORARIAMENTE
→ DESPACHADO
```

---

## 10. Consulta e rastreabilidade

```text
EstoqueCentral
→ saldo consolidado operacional

Lote
→ saldo físico, validade, apresentação e rastreabilidade

MovimentacaoEstoque
→ explica operações físicas de estoque

HistoricoLaboratorio
→ registra o que o laboratório recebeu

Pedido
→ registra solicitação e decisões do fluxo

Residuo + HistoricoResiduo
→ registra ciclo do resíduo
```

Esses conceitos não devem ser usados como saldos paralelos.

---

## Fluxo resumido

```text
Contexto institucional / Unidade
  ↓
Catálogo + Estoque por Unidade
  ↓
Lotes
  ↓
Pedido PENDENTE
  ├─ rejeição → REJEITADO
  └─ aprovação
       ├─ baixa FEFO/FIFO
       ├─ movimentação SAIDA/PEDIDO
       └─ APROVADO
            ├─ entrega → histórico → ENTREGUE
            └─ cancelamento → restaura lotes usados → CANCELADO

Laboratório
  ↓
Resíduo INFORMADO
  → análise
  → armazenamento
  → despacho
```
