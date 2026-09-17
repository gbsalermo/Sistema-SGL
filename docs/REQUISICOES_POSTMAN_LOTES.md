# Requisições Postman — validação de Lotes e Movimentações

Base URL:

```text
http://localhost:8080/api/v1
```

> Os IDs abaixo consideram uma inicialização limpa com o `DataInitializer` atual. Antes dos testes, confirme os IDs com os endpoints de consulta, pois reinícios ou dados já existentes podem alterar a numeração.

## 1. Conferir dados iniciais

### Produtos

```http
GET /api/v1/produtos
```

Localize principalmente:

```text
Alcool Etílico 70%      -> não perecível
Midio de Cultivo BHI    -> perecível
```

### Estoques

```http
GET /api/v1/estoque-central
```

No banco limpo, o estoque do BHI no Instituto de Biologia tende a ser o estoque `4`, mas confirme no retorno.

### Usuários

```http
GET /api/v1/usuarios
```

Escolha um usuário ativo para executar entradas/descartes durante os testes locais.

### Lotes

```http
GET /api/v1/lotes
```

ou:

```http
GET /api/v1/lotes/por-estoque?estoqueId=4
```

O BHI deve possuir inicialmente um lote válido com saldo correspondente ao estoque agregado.

---

# Cenário A — entrada de produto não perecível

Use o estoque de um produto não perecível, por exemplo Álcool 70%.

```http
POST /api/v1/movimentacoes/estoques/{estoqueId}/lotes?usuarioId={usuarioId}
Content-Type: application/json
```

Body:

```json
{
  "numeroLote": "POSTMAN-FIFO-001",
  "quantidade": 10,
  "dataValidade": null,
  "origem": "COMPRA",
  "observacao": "Entrada Postman - produto não perecível"
}
```

Esperado:

```text
201 Created
novo Lote criado
quantidadeInicial = 10
quantidadeDisponivel = 10
dataValidade = null
EstoqueCentral.quantidadeAtual aumenta em 10
MovimentacaoEstoque ENTRADA criada com loteId preenchido
```

Verifique:

```http
GET /api/v1/lotes/por-estoque?estoqueId={estoqueId}
```

```http
GET /api/v1/estoque-central/{estoqueId}
```

```http
GET /api/v1/movimentacoes/tipo?tipo=ENTRADA
```

## Regra negativa: não perecível com validade

```http
POST /api/v1/movimentacoes/estoques/{estoqueId}/lotes?usuarioId={usuarioId}
Content-Type: application/json
```

```json
{
  "numeroLote": "POSTMAN-FIFO-INVALIDO",
  "quantidade": 5,
  "dataValidade": "2027-12-31",
  "origem": "COMPRA",
  "observacao": "Deve falhar"
}
```

Esperado: erro de regra de negócio informando que produto não perecível não deve possuir validade no lote.

---

# Cenário B — entrada de produto perecível

Use o estoque do `Midio de Cultivo BHI`.

```http
POST /api/v1/movimentacoes/estoques/{estoqueBhiId}/lotes?usuarioId={usuarioId}
Content-Type: application/json
```

Body:

```json
{
  "numeroLote": "BHI-POSTMAN-2026-A",
  "quantidade": 10,
  "dataValidade": "2026-08-20",
  "origem": "COMPRA",
  "observacao": "Lote BHI com vencimento próximo para validar FEFO"
}
```

> Se estiver executando este roteiro depois de 20/08/2026, troque a data por uma data futura próxima.

Esperado:

```text
201 Created
lote criado com validade
saldo agregado aumentado em 10
movimentação ENTRADA vinculada ao lote
```

## Regra negativa: perecível sem validade

```json
{
  "numeroLote": "BHI-SEM-VALIDADE",
  "quantidade": 5,
  "dataValidade": null,
  "origem": "COMPRA",
  "observacao": "Deve falhar"
}
```

Esperado: erro `Data de validade é obrigatória para produto perecível.`

---

# Cenário C — validar FEFO através de pedido

A ideia é manter pelo menos dois lotes válidos do BHI com vencimentos diferentes.

Consulte:

```http
GET /api/v1/lotes/por-estoque?estoqueId={estoqueBhiId}
```

Anote o lote com validade mais próxima e seu saldo.

## Criar um pedido de BHI

O usuário precisa pertencer ao laboratório informado e ambos precisam pertencer à mesma unidade do estoque do BHI.

Exemplo com usuário/laboratório do Instituto de Biologia:

```http
POST /api/v1/pedidos
Content-Type: application/json
```

```json
{
  "usuarioId": 2,
  "laboratorioId": 1,
  "projetoId": null,
  "observacao": "Pedido Postman para validar FEFO",
  "arquivoDocumento": null,
  "itens": [
    {
      "produtoId": 3,
      "quantidadeSolicitada": 6
    }
  ]
}
```

Confirme os IDs antes de executar.

Esperado:

```text
pedido criado como PENDENTE
nenhum lote alterado nesta etapa
nenhum saldo reservado
```

Anote:

```text
pedidoId
itemId
```

## Aprovar o pedido

```http
PUT /api/v1/pedidos/{pedidoId}/aprovar
Content-Type: application/json
```

```json
{
  "observacao": "Aprovado no teste FEFO",
  "usuarioAprovadorId": 2,
  "itens": [
    {
      "itemId": 123,
      "quantidadeAprovada": 6
    }
  ]
}
```

Troque `123` pelo `itemId` retornado na criação do pedido.

Esperado:

```text
pedido -> APROVADO
quantidadeAprovada -> 6
lote válido com vencimento mais próximo é consumido primeiro
se ele não possuir 6 unidades, a diferença sai do próximo lote FEFO
EstoqueCentral reduz exatamente 6
uma MovimentacaoEstoque SAIDA é criada para cada lote realmente consumido
```

Verifique:

```http
GET /api/v1/lotes/por-estoque?estoqueId={estoqueBhiId}
```

```http
GET /api/v1/estoque-central/{estoqueBhiId}
```

```http
GET /api/v1/movimentacoes/pedido?pedidoId={pedidoId}
```

Nas movimentações, confira principalmente:

```text
loteId
numeroLote
quantidadeMovimentada
quantidadeAnterior
quantidadeAtual
tipoMovimentacao = SAIDA
origem = PEDIDO
```

---

# Cenário D — lote vencido não atende pedido

Escolha um lote de BHI e altere sua validade para uma data anterior a hoje.

```http
PUT /api/v1/lotes/{loteId}
Content-Type: application/json
```

Exemplo:

```json
{
  "numeroLote": "BHI-VENCIDO-POSTMAN",
  "dataValidade": "2026-08-01",
  "ativo": true
}
```

> Use uma data realmente anterior ao dia do teste.

Depois confira:

```http
GET /api/v1/lotes/vencidos
```

O lote deve aparecer como vencido.

Crie um novo pedido cuja quantidade seja maior que a soma dos lotes válidos, mas menor ou igual ao `EstoqueCentral.quantidadeAtual` total.

Na aprovação, o esperado é falhar com algo semelhante a:

```text
Estoque utilizável insuficiente. Disponível nos lotes válidos: X, solicitado: Y
```

Isso comprova que o sistema não usa apenas o saldo agregado e não libera unidades vencidas para pedido.

---

# Cenário E — descarte do lote vencido

```http
POST /api/v1/movimentacoes/estoques/{estoqueBhiId}/descarte-vencimento?usuarioId={usuarioId}
Content-Type: application/json
```

Body:

```json
{
  "quantidade": 5,
  "justificativa": "Descarte de lote vencido durante validação Postman"
}
```

Esperado:

```text
somente lotes vencidos são consumidos
lotes válidos permanecem intactos
EstoqueCentral diminui na mesma quantidade
MovimentacaoEstoque DESCARTE_VENCIMENTO é criada por lote afetado
origem = DESCARTE
```

Verifique:

```http
GET /api/v1/lotes/vencidos
```

```http
GET /api/v1/estoque-central/{estoqueBhiId}
```

```http
GET /api/v1/movimentacoes/tipo?tipo=DESCARTE_VENCIMENTO
```

---

# Cenário F — cancelamento restaura os lotes exatos

Use um pedido `APROVADO` do cenário FEFO e consulte antes:

```http
GET /api/v1/movimentacoes/pedido?pedidoId={pedidoId}
```

Anote os lotes e quantidades das movimentações `SAIDA`.

Cancelar:

```http
PUT /api/v1/pedidos/{pedidoId}/cancelar?observacao=Cancelamento%20Postman
```

Esperado:

```text
pedido -> CANCELADO
cada lote usado na aprovação recebe exatamente a quantidade que havia fornecido
EstoqueCentral volta ao saldo anterior à aprovação
```

Confirme novamente:

```http
GET /api/v1/lotes/por-estoque?estoqueId={estoqueBhiId}
```

```http
GET /api/v1/estoque-central/{estoqueBhiId}
```

Neste estágio, a restauração física ocorre, mas a movimentação `DEVOLUCAO` ainda depende da integração do usuário responsável pelo cancelamento ao contexto de autenticação local.

---

# Cenário G — FIFO para não perecível através de pedido

Para validar FIFO de ponta a ponta, crie dois lotes novos de um produto não perecível em momentos diferentes. Como `dataEntrada` é criada automaticamente no dia corrente, quando os dois lotes forem inseridos no mesmo dia o desempate será pelo `id` do lote.

Depois faça um pedido que atravesse o saldo do primeiro lote.

Esperado:

```text
primeiro lote recebido/id menor é consumido primeiro
segundo lote fornece apenas a quantidade restante
uma SAIDA é registrada para cada lote utilizado
```

---

# Checklist antes do PostgreSQL

Considere a etapa validada quando os seguintes comportamentos forem confirmados:

```text
[ ] entrada não perecível cria lote sem validade
[ ] entrada perecível exige validade
[ ] saldo agregado cresce junto com o lote
[ ] pedido nasce PENDENTE sem reservar estoque
[ ] aprovação usa somente lotes válidos
[ ] perecível usa FEFO
[ ] não perecível usa FIFO
[ ] uma saída pode consumir vários lotes
[ ] movimentação identifica o lote de origem
[ ] lote vencido não atende pedido
[ ] descarte consome apenas lotes vencidos
[ ] cancelamento restaura os mesmos lotes
[ ] EstoqueCentral permanece igual à soma dos saldos dos lotes
```

Depois desse checklist e dos testes automatizados, a próxima etapa é iniciar a integração com PostgreSQL e preparar as migrations.
