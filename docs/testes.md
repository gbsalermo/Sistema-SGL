# Testes — SGL

Este arquivo reúne os testes automatizados e o roteiro manual no Postman para validar a arquitetura de estoque por lotes antes da migração para PostgreSQL.

## 1. Executar testes automatizados

A partir da pasta do backend:

```bash
cd backend/sgl-backend
mvn test
```

A suíte atual cobre principalmente `MovimentacaoEstoqueService` e `PedidoService`.

### MovimentacaoEstoqueServiceTest

Os cenários principais são:

- entrada física cria lote, aumenta `EstoqueCentral.quantidadeAtual` e registra `ENTRADA`;
- produto perecível exige `dataValidade` no lote;
- produto não perecível utiliza FIFO;
- produto perecível utiliza FEFO;
- uma saída pode consumir mais de um lote;
- saldo agregado não basta para aprovação se não houver quantidade suficiente em lotes válidos;
- descarte atua somente sobre lotes vencidos;
- cancelamento restaura exatamente os mesmos lotes consumidos anteriormente.

### PedidoServiceTest

Os cenários principais são:

- aprovação delega a baixa física para `MovimentacaoEstoqueService`;
- somente pedido `PENDENTE` pode ser aprovado;
- quantidade aprovada deve ser maior que zero e não pode ultrapassar a solicitada;
- falha de estoque utilizável impede aprovação;
- usuário aprovador é obrigatório enquanto o contexto autenticado local ainda não está implementado;
- cancelamento de pedido `APROVADO` solicita a restauração dos lotes consumidos.

## 2. Regras que precisam ser confirmadas

```text
Produto
→ representa o item do catálogo
→ informa se é perecível
→ não possui uma data de validade única

Lote
→ representa a entrada física
→ possui quantidade e validade próprias

EstoqueCentral
→ mantém o saldo agregado

MovimentacaoEstoque
→ registra cada lote efetivamente afetado
```

Para saída:

```text
Produto perecível     → FEFO
Produto não perecível → FIFO
```

Para produtos perecíveis, lote vencido não participa de saída normal nem de aprovação de pedido.

Uma saída de pedido continua sendo solicitada como `Produto + quantidade`. O usuário não escolhe o lote. Internamente, o sistema seleciona os lotes adequados e registra uma `MovimentacaoEstoque` para cada lote afetado.

## 3. Preparação do Postman

Base URL:

```text
http://localhost:8080
```

Antes de começar, execute a aplicação e consulte os dados iniciais:

```http
GET http://localhost:8080/api/v1/produtos
```

```http
GET http://localhost:8080/api/v1/estoque-central
```

```http
GET http://localhost:8080/api/v1/lotes
```

Use os IDs retornados pela sua execução. Não assuma que os IDs serão sempre iguais aos exemplos deste arquivo.

## 4. Conferir estoque inicial

### Listar estoques

```http
GET http://localhost:8080/api/v1/estoque-central
```

Confirme que cada `quantidadeAtual` possui lotes correspondentes.

### Listar lotes

```http
GET http://localhost:8080/api/v1/lotes
```

Ou por estoque:

```http
GET http://localhost:8080/api/v1/lotes/por-estoque?estoqueId={estoqueId}
```

Regra esperada:

```text
EstoqueCentral.quantidadeAtual
=
soma dos Lote.quantidadeDisponivel daquele estoque
```

## 5. Entrada de produto não perecível

Endpoint temporário enquanto a autenticação local não está implementada:

```http
POST http://localhost:8080/api/v1/movimentacoes/estoques/{estoqueId}/lotes?usuarioId={usuarioId}
Content-Type: application/json
```

Body:

```json
{
  "numeroLote": "POSTMAN-FIFO-001",
  "quantidade": 10,
  "dataValidade": null,
  "origem": "COMPRA",
  "observacao": "Entrada de produto não perecível pelo Postman"
}
```

Resultado esperado:

```text
novo Lote com quantidadeInicial = 10
quantidadeDisponivel = 10
dataValidade = null
EstoqueCentral.quantidadeAtual aumenta em 10
MovimentacaoEstoque ENTRADA vinculada ao lote
```

Confira:

```http
GET http://localhost:8080/api/v1/lotes/por-estoque?estoqueId={estoqueId}
```

```http
GET http://localhost:8080/api/v1/movimentacoes/produto?produtoId={produtoId}
```

## 6. Entrada de produto perecível

```http
POST http://localhost:8080/api/v1/movimentacoes/estoques/{estoqueId}/lotes?usuarioId={usuarioId}
Content-Type: application/json
```

Body de exemplo:

```json
{
  "numeroLote": "POSTMAN-FEFO-A",
  "quantidade": 10,
  "dataValidade": "2026-08-20",
  "origem": "COMPRA",
  "observacao": "Primeiro lote para teste FEFO"
}
```

Crie um segundo lote do mesmo produto com validade posterior:

```json
{
  "numeroLote": "POSTMAN-FEFO-B",
  "quantidade": 10,
  "dataValidade": "2026-12-20",
  "origem": "COMPRA",
  "observacao": "Segundo lote para teste FEFO"
}
```

O lote `POSTMAN-FEFO-A` deve ser consumido antes do `POSTMAN-FEFO-B`.

## 7. Validar erro de perecível sem validade

Tente cadastrar uma entrada perecível com:

```json
{
  "numeroLote": "SEM-VALIDADE",
  "quantidade": 5,
  "dataValidade": null,
  "origem": "COMPRA",
  "observacao": "Este cadastro deve falhar"
}
```

Resultado esperado:

```text
400 Bad Request
Data de validade é obrigatória para produto perecível.
```

## 8. Validar erro de não perecível com validade

Em um estoque cujo produto seja não perecível:

```json
{
  "numeroLote": "NAO-PERECIVEL-COM-VALIDADE",
  "quantidade": 5,
  "dataValidade": "2027-01-01",
  "origem": "COMPRA",
  "observacao": "Este cadastro deve falhar"
}
```

Resultado esperado:

```text
400 Bad Request
Produto não perecível não deve possuir data de validade no lote.
```

## 9. Criar pedido de produto perecível

Primeiro identifique:

```text
usuarioId
laboratorioId
produtoId perecível
```

Crie o pedido:

```http
POST http://localhost:8080/api/v1/pedidos
Content-Type: application/json
```

Body:

```json
{
  "usuarioId": 2,
  "laboratorioId": 1,
  "projetoId": null,
  "observacao": "Pedido para validar FEFO",
  "arquivoDocumento": null,
  "itens": [
    {
      "produtoId": 3,
      "quantidadeSolicitada": 6
    }
  ]
}
```

Substitua os IDs pelos IDs reais retornados pelo sistema.

Resultado esperado:

```text
status = PENDENTE
nenhum lote é alterado
EstoqueCentral não é alterado
nenhuma SAIDA é registrada
```

Guarde:

```text
pedidoId
itemId
```

## 10. Consultar pedido pendente

```http
GET http://localhost:8080/api/v1/pedidos/{pedidoId}
```

Ou:

```http
GET http://localhost:8080/api/v1/pedidos/por-status?status=PENDENTE
```

O gestor deve enxergar o produto solicitado e a quantidade, enquanto a disponibilidade real será decidida a partir dos lotes válidos durante a aprovação.

## 11. Aprovar pedido e validar FEFO

Antes da aprovação, consulte os lotes:

```http
GET http://localhost:8080/api/v1/lotes/por-estoque?estoqueId={estoqueId}
```

Aprove:

```http
PUT http://localhost:8080/api/v1/pedidos/{pedidoId}/aprovar
Content-Type: application/json
```

Body:

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

Substitua `usuarioAprovadorId` e `itemId` pelos IDs reais.

Resultado esperado:

```text
Pedido → APROVADO
ItemPedido.quantidadeAprovada = 6
lotes são reduzidos seguindo FEFO
EstoqueCentral reduz 6
MovimentacaoEstoque registra os lotes consumidos
```

Se um lote tiver somente 4 unidades e o pedido aprovado for de 6:

```text
Lote A → -4
Lote B → -2
```

Devem existir duas movimentações de saída.

Confira:

```http
GET http://localhost:8080/api/v1/movimentacoes/pedido?pedidoId={pedidoId}
```

Cada saída deve indicar:

```text
loteId
numeroLote
quantidadeMovimentada
pedidoId
produtoId
tipoMovimentacao = SAIDA
origem = PEDIDO
```

## 12. Validar que lote vencido não atende pedido

Para esse cenário, tenha um produto perecível cujo `EstoqueCentral.quantidadeAtual` inclua unidades em lote vencido e poucas unidades em lotes válidos.

Exemplo conceitual:

```text
EstoqueCentral = 20

Lote vencido = 12
Lote válido = 8
```

Tente aprovar:

```text
quantidadeAprovada = 10
```

Mesmo com `EstoqueCentral.quantidadeAtual = 20`, o resultado esperado é erro porque apenas 8 unidades são utilizáveis.

Mensagem esperada semelhante a:

```text
Estoque utilizável insuficiente. Disponível nos lotes válidos: 8, solicitado: 10
```

Nenhum lote deve ser parcialmente modificado quando a operação é rejeitada.

## 13. Validar FIFO para produto não perecível

Cadastre dois lotes não perecíveis em momentos diferentes:

```text
FIFO-A → entrada mais antiga
FIFO-B → entrada mais recente
```

Depois provoque uma saída através de um pedido desse produto.

Resultado esperado:

```text
FIFO-A é consumido primeiro
FIFO-B só é usado se FIFO-A não for suficiente
```

Para inspecionar:

```http
GET http://localhost:8080/api/v1/lotes/por-estoque?estoqueId={estoqueId}
```

```http
GET http://localhost:8080/api/v1/movimentacoes/pedido?pedidoId={pedidoId}
```

## 14. Descarte de produto vencido

Endpoint temporário enquanto o usuário ainda não vem do contexto autenticado:

```http
POST http://localhost:8080/api/v1/movimentacoes/estoques/{estoqueId}/descarte-vencimento?usuarioId={usuarioId}
Content-Type: application/json
```

Body:

```json
{
  "quantidade": 5,
  "justificativa": "Descarte de lote vencido"
}
```

Resultado esperado:

```text
somente lotes vencidos são consumidos
EstoqueCentral diminui na mesma quantidade
tipoMovimentacao = DESCARTE_VENCIMENTO
origem = DESCARTE
cada lote afetado gera sua própria MovimentacaoEstoque
```

Confira:

```http
GET http://localhost:8080/api/v1/lotes/vencidos
```

```http
GET http://localhost:8080/api/v1/movimentacoes/tipo?tipo=DESCARTE_VENCIMENTO
```

## 15. Cancelar pedido aprovado

Escolha um pedido aprovado e consulte primeiro as saídas:

```http
GET http://localhost:8080/api/v1/movimentacoes/pedido?pedidoId={pedidoId}
```

Anote os lotes e quantidades consumidos.

Cancele:

```http
PUT http://localhost:8080/api/v1/pedidos/{pedidoId}/cancelar?observacao=Cancelamento%20Postman
```

Resultado esperado:

```text
Pedido → CANCELADO
os mesmos lotes consumidos na aprovação são restaurados
EstoqueCentral volta a receber a quantidade correspondente
```

Confira novamente:

```http
GET http://localhost:8080/api/v1/lotes/por-estoque?estoqueId={estoqueId}
```

```http
GET http://localhost:8080/api/v1/estoque-central/{estoqueId}
```

Observação: o registro auditado `DEVOLUCAO` ficará completo quando o contexto de autenticação local fornecer o usuário responsável pelo cancelamento.

## 16. Entregar pedido

Para um pedido aprovado que não será usado no teste de cancelamento:

```http
PUT http://localhost:8080/api/v1/pedidos/{pedidoId}/entregar
```

Resultado esperado:

```text
Pedido → ENTREGUE
não ocorre nova baixa no estoque
HistoricoLaboratorio é criado com a quantidade já aprovada
```

## 17. Consultas úteis durante os testes

Todos os lotes:

```http
GET http://localhost:8080/api/v1/lotes
```

Lotes de um estoque:

```http
GET http://localhost:8080/api/v1/lotes/por-estoque?estoqueId={estoqueId}
```

Lotes vencidos:

```http
GET http://localhost:8080/api/v1/lotes/vencidos
```

Todas as movimentações:

```http
GET http://localhost:8080/api/v1/movimentacoes
```

Movimentações de um pedido:

```http
GET http://localhost:8080/api/v1/movimentacoes/pedido?pedidoId={pedidoId}
```

Movimentações por produto:

```http
GET http://localhost:8080/api/v1/movimentacoes/produto?produtoId={produtoId}
```

Movimentações por tipo:

```http
GET http://localhost:8080/api/v1/movimentacoes/tipo?tipo=SAIDA
```

Estoque:

```http
GET http://localhost:8080/api/v1/estoque-central/{estoqueId}
```

Pedidos pendentes:

```http
GET http://localhost:8080/api/v1/pedidos/por-status?status=PENDENTE
```

## 18. Checklist antes do PostgreSQL

Só iniciar oficialmente a migração quando estes pontos estiverem confirmados:

```text
[ ] mvn test executa sem falhas
[ ] entrada não perecível cria lote sem validade
[ ] entrada perecível exige validade
[ ] saldo do EstoqueCentral acompanha os lotes
[ ] FIFO funciona para não perecível
[ ] FEFO funciona para perecível
[ ] saída consegue consumir mais de um lote
[ ] lote vencido não atende pedido normal
[ ] aprovação de pedido reduz lotes e saldo agregado
[ ] movimentações indicam o lote de origem
[ ] descarte reduz somente lotes vencidos
[ ] cancelamento restaura exatamente os lotes usados
[ ] entrega não baixa o estoque novamente
```

Com esses pontos confirmados, o modelo estará suficientemente estabilizado para iniciar a migração para PostgreSQL e Flyway.
