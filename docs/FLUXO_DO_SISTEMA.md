# Fluxo do Sistema SGL

Este documento descreve como os módulos se conectam do cadastro inicial até a entrega de materiais.

## 1. Preparação da estrutura

```text
Unidade
  → Laboratório
      → Usuários
      → Projetos

Produto
  → EstoqueCentral da Unidade
```

1. Uma Unidade é cadastrada.
2. Um Laboratório é vinculado à Unidade.
3. Usuários são associados à Unidade e, quando aplicável, ao Laboratório.
4. Produtos são cadastrados no catálogo global.
5. Cada Unidade cria seus registros de estoque para os Produtos utilizados.

## 2. Entrada de estoque

```text
Usuário responsável
  → seleciona Unidade + Produto
  → informa quantidade
  → Service valida o registro
  → soma ao saldo
  → grava MovimentacaoEstoque
```

A movimentação registra o saldo anterior e o saldo resultante. O tipo é `ENTRADA`; a origem informa o contexto da entrada, como compra ou ajuste.

## 3. Saída manual

```text
Saldo atual
  → valida quantidade solicitada
  → impede saldo negativo
  → subtrai quantidade
  → registra movimentação de saída
```

A operação deve ocorrer na mesma transação que grava o histórico.

## 4. Criação do pedido

O usuário informa laboratório, projeto opcional e itens solicitados.

Validações:

- usuário, laboratório, projeto e produtos devem existir;
- usuário deve pertencer ao laboratório informado;
- usuário e laboratório devem pertencer à mesma unidade;
- projeto, quando informado, deve pertencer ao laboratório;
- entidades envolvidas devem estar ativas;
- o mesmo produto não pode aparecer duas vezes;
- deve existir estoque ativo do produto na unidade.

O saldo não é reduzido nessa etapa. O pedido é salvo como `PENDENTE`.

## 5. Aprovação

```text
Aprovador + Pedido PENDENTE
  → valida cada item
  → localiza estoque por Unidade + Produto
  → valida vencimento e saldo
  → calcula saldo anterior e atual
  → reduz EstoqueCentral
  → grava quantidade aprovada
  → grava MovimentacaoEstoque
  → altera Pedido para APROVADO
```

A movimentação usa:

```text
tipo = SAIDA
origem = PEDIDO
usuario = aprovador
pedido = pedido aprovado
```

Todo o processamento é transacional. Se um item falhar, nenhuma baixa parcial deve permanecer.

## 6. Rejeição

Somente pedido `PENDENTE` pode ser rejeitado. A rejeição muda o status para `REJEITADO`, registra a observação e não altera o estoque.

## 7. Entrega

Somente pedido `APROVADO` pode ser entregue.

Para cada item aprovado, o sistema cria um `HistoricoLaboratorio` com laboratório, produto, quantidade, data e pedido. A entrega não reduz o saldo novamente, pois a baixa ocorreu na aprovação.

## 8. Cancelamento

- Pedido `PENDENTE`: cancela sem alterar estoque.
- Pedido `APROVADO`: devolve ao estoque cada quantidade aprovada e muda para `CANCELADO`.
- Pedido `ENTREGUE`: não pode ser cancelado pelo fluxo comum.
- Pedido `REJEITADO` ou `CANCELADO`: já está encerrado.

Pendência técnica: a devolução de pedido aprovado ainda deve gerar uma movimentação `DEVOLUCAO` para completar a auditoria.

## 9. Consulta e auditoria

- `EstoqueCentral` responde pelo saldo disponível.
- `MovimentacaoEstoque` explica por que o saldo mudou.
- `HistoricoLaboratorio` registra o que o laboratório recebeu.
- `Pedido` registra a solicitação e as decisões do fluxo.

Esses conceitos não devem ser confundidos ou usados como saldos paralelos.

## Fluxo resumido

```text
Cadastros
  ↓
Estoque por Unidade + Produto
  ↓
Pedido PENDENTE
  ├─ rejeição → REJEITADO
  └─ aprovação
       ├─ baixa de estoque
       ├─ movimentação SAIDA/PEDIDO
       └─ APROVADO
            ├─ entrega → histórico → ENTREGUE
            └─ cancelamento → devolução → CANCELADO
```
