# Testes — SGL

Este arquivo reúne os testes automatizados e o roteiro manual no Postman para validar a arquitetura atual antes da migração para PostgreSQL.

## 1. Executar testes automatizados

```bash
cd backend/sgl-backend
mvn test
```

A suíte atual cobre três áreas principais.

### `MovimentacaoEstoqueServiceTest`

- entrada física cria lote, aumenta `EstoqueCentral.quantidadeAtual` e registra `ENTRADA`;
- perecível exige validade no lote;
- não perecível usa FIFO;
- perecível usa FEFO;
- saída pode consumir mais de um lote;
- saldo agregado não basta quando os lotes válidos são insuficientes;
- descarte atua sobre lotes vencidos;
- cancelamento restaura os mesmos lotes consumidos.

### `PedidoServiceTest`

- aprovação delega a baixa física para `MovimentacaoEstoqueService`;
- somente `PENDENTE` pode ser aprovado;
- quantidade aprovada não pode ultrapassar a solicitada;
- falha de estoque utilizável impede aprovação;
- aprovador é obrigatório enquanto a autenticação local não fornece o contexto;
- cancelamento de `APROVADO` restaura lotes;
- consulta pedidos de um projeto em determinado laboratório e período;
- rejeita consulta quando projeto não pertence ao laboratório;
- rejeita período invertido.

### `HistoricoLaboratorioServiceTest`

- consulta materiais efetivamente recebidos por um projeto em determinado laboratório e período;
- rejeita projeto pertencente a outro laboratório;
- rejeita período invertido.

## 2. Regras que os testes precisam preservar

```text
Produto
→ catálogo
→ informa se é perecível
→ não possui validade única

Lote
→ entrada física
→ quantidade e validade próprias

EstoqueCentral
→ saldo agregado

MovimentacaoEstoque
→ auditoria por lote afetado
```

Saída:

```text
Produto perecível     → FEFO
Produto não perecível → FIFO
```

Pedidos continuam usando `Produto + quantidade`. O usuário não escolhe lote.

Para relatórios:

```text
Pedido
→ solicitação realizada
→ usa dataSolicitacao

HistoricoLaboratorio
→ material efetivamente recebido
→ nasce na entrega
→ usa dataRecebimento
```

## 3. Preparação do Postman

Base URL:

```text
http://localhost:8080
```

Consulte primeiro os dados reais da execução:

```http
GET http://localhost:8080/api/v1/unidades
GET http://localhost:8080/api/v1/laboratorios
GET http://localhost:8080/api/v1/usuarios
GET http://localhost:8080/api/v1/projetos
GET http://localhost:8080/api/v1/produtos
GET http://localhost:8080/api/v1/estoque-central
GET http://localhost:8080/api/v1/lotes
```

Não assuma IDs fixos. Use os IDs retornados localmente.

---

# Testes de Lote e Estoque

## 4. Conferir consistência inicial

```http
GET http://localhost:8080/api/v1/estoque-central
GET http://localhost:8080/api/v1/lotes
```

Para cada estoque:

```text
EstoqueCentral.quantidadeAtual
=
soma de Lote.quantidadeDisponivel
```

## 5. Entrada de produto não perecível

```http
POST http://localhost:8080/api/v1/movimentacoes/estoques/{estoqueId}/lotes?usuarioId={usuarioId}
Content-Type: application/json
```

```json
{
  "numeroLote": "POSTMAN-FIFO-001",
  "quantidade": 10,
  "dataValidade": null,
  "origem": "COMPRA",
  "observacao": "Entrada FIFO via Postman"
}
```

Esperado:

```text
lote criado
quantidadeInicial = 10
quantidadeDisponivel = 10
dataValidade = null
EstoqueCentral +10
MovimentacaoEstoque ENTRADA vinculada ao lote
```

Confira:

```http
GET http://localhost:8080/api/v1/lotes/por-estoque?estoqueId={estoqueId}
GET http://localhost:8080/api/v1/movimentacoes/produto?produtoId={produtoId}
```

## 6. Entrada de produto perecível

Crie dois lotes no mesmo estoque perecível:

```http
POST http://localhost:8080/api/v1/movimentacoes/estoques/{estoqueId}/lotes?usuarioId={usuarioId}
```

Primeiro:

```json
{
  "numeroLote": "POSTMAN-FEFO-A",
  "quantidade": 4,
  "dataValidade": "2026-08-20",
  "origem": "COMPRA",
  "observacao": "Primeiro lote FEFO"
}
```

Segundo:

```json
{
  "numeroLote": "POSTMAN-FEFO-B",
  "quantidade": 10,
  "dataValidade": "2026-12-20",
  "origem": "COMPRA",
  "observacao": "Segundo lote FEFO"
}
```

`POSTMAN-FEFO-A` deve ser consumido antes de `POSTMAN-FEFO-B`.

## 7. Perecível sem validade deve falhar

```json
{
  "numeroLote": "SEM-VALIDADE",
  "quantidade": 5,
  "dataValidade": null,
  "origem": "COMPRA",
  "observacao": "Deve falhar"
}
```

Esperado:

```text
400 Bad Request
Data de validade é obrigatória para produto perecível.
```

## 8. Não perecível com validade deve falhar

```json
{
  "numeroLote": "NAO-PERECIVEL-COM-VALIDADE",
  "quantidade": 5,
  "dataValidade": "2027-01-01",
  "origem": "COMPRA",
  "observacao": "Deve falhar"
}
```

Esperado:

```text
400 Bad Request
Produto não perecível não deve possuir data de validade no lote.
```

---

# Testes de Pedido + FEFO/FIFO

## 9. Criar pedido vinculado a projeto

Escolha um usuário, laboratório, projeto pertencente ao mesmo laboratório e um produto existente no estoque da unidade.

```http
POST http://localhost:8080/api/v1/pedidos
Content-Type: application/json
```

```json
{
  "usuarioId": 2,
  "laboratorioId": 1,
  "projetoId": 3,
  "observacao": "Pedido de teste vinculado ao projeto",
  "arquivoDocumento": null,
  "itens": [
    {
      "produtoId": 3,
      "quantidadeSolicitada": 6
    }
  ]
}
```

Use IDs reais.

Esperado:

```text
status = PENDENTE
lotes não mudam
EstoqueCentral não muda
nenhuma SAIDA é criada
```

Guarde `pedidoId` e `itemId`.

## 10. Aprovar e validar FEFO/FIFO

```http
PUT http://localhost:8080/api/v1/pedidos/{pedidoId}/aprovar
Content-Type: application/json
```

```json
{
  "observacao": "Aprovado no teste",
  "usuarioAprovadorId": 2,
  "itens": [
    {
      "itemId": 123,
      "quantidadeAprovada": 6
    }
  ]
}
```

Para perecível, se o primeiro lote tiver 4 e a aprovação for 6:

```text
Lote A → -4
Lote B → -2
```

Confira:

```http
GET http://localhost:8080/api/v1/movimentacoes/pedido?pedidoId={pedidoId}
GET http://localhost:8080/api/v1/lotes/por-estoque?estoqueId={estoqueId}
GET http://localhost:8080/api/v1/estoque-central/{estoqueId}
```

Cada saída deve mostrar o lote realmente consumido.

## 11. Lote vencido não atende pedido

Cenário:

```text
EstoqueCentral = 20
Lote vencido = 12
Lote válido = 8
```

Tente aprovar 10.

Esperado:

```text
Estoque utilizável insuficiente. Disponível nos lotes válidos: 8, solicitado: 10
```

Mesmo que o saldo agregado seja 20, apenas 8 podem atender o pedido.

## 12. FIFO para não perecível

Tenha dois lotes não perecíveis:

```text
FIFO-A → entrou primeiro
FIFO-B → entrou depois
```

Aprove um pedido que obrigue o consumo.

Esperado:

```text
FIFO-A é consumido antes de FIFO-B
```

---

# Testes de Descarte e Cancelamento

## 13. Descarte de vencidos

```http
POST http://localhost:8080/api/v1/movimentacoes/estoques/{estoqueId}/descarte-vencimento?usuarioId={usuarioId}
Content-Type: application/json
```

```json
{
  "quantidade": 5,
  "justificativa": "Descarte de lote vencido"
}
```

Esperado:

```text
somente lotes vencidos são reduzidos
EstoqueCentral reduz 5
uma movimentação DESCARTE_VENCIMENTO é criada por lote afetado
```

Confira:

```http
GET http://localhost:8080/api/v1/lotes/vencidos
GET http://localhost:8080/api/v1/movimentacoes/tipo?tipo=DESCARTE_VENCIMENTO
```

## 14. Cancelar pedido aprovado

Antes:

```http
GET http://localhost:8080/api/v1/movimentacoes/pedido?pedidoId={pedidoId}
```

Anote lotes e quantidades.

Depois:

```http
PUT http://localhost:8080/api/v1/pedidos/{pedidoId}/cancelar?observacao=Cancelamento%20Postman
```

Esperado:

```text
Pedido → CANCELADO
os mesmos lotes consumidos são restaurados
EstoqueCentral recebe a quantidade de volta
```

Confira novamente estoque e lotes.

---

# Testes de Histórico por Projeto

## 15. Consultar pedidos feitos pelo projeto no período

Esse endpoint consulta **solicitações**, usando `Pedido.dataSolicitacao`.

```http
GET http://localhost:8080/api/v1/pedidos/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=2026-06-01&dataFim=2026-06-30
```

Esperado:

```text
retorna apenas pedidos do projeto informado
retorna apenas pedidos do laboratório informado
considera solicitações entre 01/06 e 30/06 inclusive
```

Um pedido pode aparecer aqui mesmo que esteja `PENDENTE`, `REJEITADO`, `CANCELADO`, `APROVADO` ou `ENTREGUE`.

## 16. Projeto de outro laboratório deve falhar

Use um `laboratorioId` e um `projetoId` que não tenham vínculo.

```http
GET http://localhost:8080/api/v1/pedidos/laboratorio/{laboratorioA}/projeto/{projetoDoLaboratorioB}/periodo?dataInicio=2026-06-01&dataFim=2026-06-30
```

Esperado:

```text
400 Bad Request
O projeto informado não pertence ao laboratório informado.
```

## 17. Período invertido deve falhar

```http
GET http://localhost:8080/api/v1/pedidos/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=2026-06-30&dataFim=2026-06-01
```

Esperado:

```text
400 Bad Request
A data inicial não pode ser posterior à data final.
```

## 18. Entregar pedido do projeto

Use um pedido aprovado que não será cancelado:

```http
PUT http://localhost:8080/api/v1/pedidos/{pedidoId}/entregar
```

Esperado:

```text
Pedido → ENTREGUE
não ocorre uma segunda baixa no estoque
HistoricoLaboratorio é criado
```

## 19. Consultar materiais recebidos pelo projeto no período

Esse endpoint consulta **recebimentos efetivos**, usando `HistoricoLaboratorio.dataRecebimento`.

```http
GET http://localhost:8080/api/v1/historico-laboratorio/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=2026-06-01&dataFim=2026-06-30
```

Esperado:

```text
somente registros entregues aparecem
somente o projeto informado aparece
somente o laboratório informado aparece
```

Compare com o endpoint de pedidos. É normal existirem mais solicitações do que recebimentos.

## 20. Consultar histórico geral do laboratório no período

```http
GET http://localhost:8080/api/v1/historico-laboratorio/laboratorio/{laboratorioId}/periodo?dataInicio=2026-06-01&dataFim=2026-06-30
```

Isso permite comparar:

```text
Laboratório A em junho
→ todos os materiais recebidos

Projeto 1 do Laboratório A em junho
→ apenas os materiais recebidos pelo Projeto 1
```

---

# Consultas rápidas

```http
GET /api/v1/lotes
GET /api/v1/lotes/vencidos
GET /api/v1/movimentacoes
GET /api/v1/movimentacoes/pedido?pedidoId={pedidoId}
GET /api/v1/movimentacoes/produto?produtoId={produtoId}
GET /api/v1/movimentacoes/tipo?tipo=SAIDA
GET /api/v1/estoque-central/{estoqueId}
GET /api/v1/pedidos/por-status?status=PENDENTE
GET /api/v1/projetos/por-laboratorio?laboratorioId={laboratorioId}
GET /api/v1/historico-laboratorio/laboratorio/{laboratorioId}
```

O inventário completo da API está em [`ENDPOINTS_INTERNOS.md`](ENDPOINTS_INTERNOS.md).

---

# Checklist antes do PostgreSQL

```text
[ ] mvn test executa sem falhas
[ ] entrada não perecível cria lote sem validade
[ ] entrada perecível exige validade
[ ] saldo do EstoqueCentral acompanha os lotes
[ ] FIFO funciona para não perecível
[ ] FEFO funciona para perecível
[ ] saída consome múltiplos lotes quando necessário
[ ] lote vencido não atende pedido normal
[ ] aprovação reduz lotes e saldo agregado
[ ] movimentações registram o lote de origem
[ ] descarte reduz somente lotes vencidos
[ ] cancelamento restaura exatamente os lotes usados
[ ] entrega não reduz estoque novamente
[ ] consulta de pedidos por projeto/período retorna somente o projeto correto
[ ] projeto de outro laboratório é rejeitado
[ ] período invertido é rejeitado
[ ] histórico por projeto/período mostra somente materiais efetivamente entregues
[ ] histórico geral do laboratório e histórico específico do projeto apresentam resultados coerentes
```

Com todos os itens confirmados, o modelo estará suficientemente estabilizado para iniciar PostgreSQL e Flyway.
