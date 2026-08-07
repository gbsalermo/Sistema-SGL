# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 07/08/2026  
**Fase atual:** arquitetura por lotes estabilizada; validação final antes do PostgreSQL

Este arquivo registra o estado atual do backend, decisões consolidadas e a ordem recomendada de continuidade.

## Estado atual

### Lotes e estoque

Concluído:

- entidade `Lote`;
- `LoteDTO`, `EntradaLoteDTO` e `AtualizarLoteDTO`;
- `LoteRepository`, `LoteService` e `LoteController`;
- entrada física por lote;
- FEFO para produtos perecíveis;
- FIFO para produtos não perecíveis;
- descarte de lotes vencidos;
- rastreabilidade por `MovimentacaoEstoque.lote`;
- restauração exata de lotes no cancelamento de pedido aprovado;
- remoção das operações físicas de `EstoqueCentralService`.

Regra de consistência:

```text
EstoqueCentral.quantidadeAtual
=
soma de Lote.quantidadeDisponivel
```

### Produto

`Produto` é catálogo e informa se o item é perecível.

A validade operacional pertence ao lote.

```text
Produto perecível
→ lote exige dataValidade
→ saída FEFO

Produto não perecível
→ lote sem dataValidade
→ saída FIFO
```

### MovimentacaoEstoque

`MovimentacaoEstoque` permanece como entidade de auditoria.

Cada lote afetado por uma operação gera sua própria movimentação, permitindo rastrear exatamente:

```text
produto
lote
quantidade
pedido, quando aplicável
laboratório, quando aplicável
usuário responsável
saldo anterior
saldo posterior
```

`MovimentacaoEstoqueService` centraliza entrada, saída, descarte e devolução/restauração física.

## Pedido

O fluxo atual é:

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Na aprovação:

```text
PedidoService
→ valida pedido e quantidades
→ localiza EstoqueCentral
→ delega ao MovimentacaoEstoqueService
→ FEFO/FIFO seleciona lotes
→ reduz lotes
→ reduz saldo agregado
→ registra SAIDA por lote
→ pedido fica APROVADO
```

Na entrega:

```text
pedido APROVADO
→ cria HistoricoLaboratorio
→ não baixa estoque novamente
→ pedido fica ENTREGUE
```

No cancelamento de pedido aprovado:

```text
consulta SAIDAS do pedido
→ identifica lotes usados
→ restaura exatamente esses lotes
→ restaura EstoqueCentral
→ pedido fica CANCELADO
```

O registro auditado `DEVOLUCAO` será completado quando o contexto autenticado local fornecer o usuário executor do cancelamento.

## Consultas por projeto e laboratório

Foi adicionada uma separação explícita entre **pedidos realizados** e **materiais efetivamente recebidos**.

### Pedidos realizados pelo projeto

```http
GET /api/v1/pedidos/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD
```

Essa consulta usa `Pedido.dataSolicitacao` e pode retornar pedidos em qualquer status.

### Materiais efetivamente recebidos pelo projeto

```http
GET /api/v1/historico-laboratorio/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD
```

Essa consulta usa `HistoricoLaboratorio.dataRecebimento` e representa somente materiais entregues.

As duas consultas validam:

```text
laboratório existe
projeto existe
projeto pertence ao laboratório informado
dataInicio <= dataFim
```

Com isso, o sistema já consegue responder perguntas como:

```text
quantos pedidos o Laboratório A recebeu em junho?
quantos desses pedidos pertencem ao Projeto 1?
quanto material o Projeto 1 efetivamente recebeu em junho?
```

## Testes automatizados

A suíte foi migrada para a arquitetura atual.

### `MovimentacaoEstoqueServiceTest`

Cobre:

- entrada por lote;
- validade obrigatória em perecível;
- FIFO;
- FEFO;
- consumo de múltiplos lotes;
- insuficiência de lotes válidos;
- descarte de vencidos;
- restauração exata de lotes.

### `PedidoServiceTest`

Cobre:

- aprovação delegada ao service de movimentação;
- regras de status;
- quantidade aprovada;
- falha de estoque utilizável;
- exigência de aprovador;
- cancelamento com restauração;
- pedidos por projeto/laboratório/período;
- projeto pertencente a outro laboratório;
- período invertido.

### `HistoricoLaboratorioServiceTest`

Cobre:

- materiais recebidos por projeto/laboratório/período;
- vínculo incorreto Projeto → Laboratório;
- período invertido.

A suíte deve ser executada localmente antes da migração:

```bash
cd backend/sgl-backend
mvn test
```

O roteiro manual completo está em [`docs/testes.md`](docs/testes.md).

## Catálogo de endpoints

Foi criado o documento:

```text
docs/ENDPOINTS_INTERNOS.md
```

Ele contém os endpoints atuais separados por entidade, método HTTP e função.

O documento é operacional/interno ao desenvolvimento, porém sua visibilidade acompanha a do repositório. Como o repositório está público, o arquivo também está público.

Até a adoção de OpenAPI/Swagger, esse arquivo deve ser mantido como inventário principal da API.

## Autenticação

### Local simulada

Permanece planejada para desenvolvimento após a integração inicial com PostgreSQL.

Ela deverá fornecer o usuário responsável através de contexto autenticado, eliminando os `usuarioId` temporários de endpoints de movimentação.

### Definitiva externa

Será fornecida por API corporativa e permanece obrigatória para implantação definitiva.

## PostgreSQL

A modelagem de domínio necessária para iniciar a migração está praticamente estabilizada.

Antes de iniciar oficialmente PostgreSQL/Flyway, executar:

```text
1. mvn test
2. roteiro Postman de Lote/FEFO/FIFO
3. aprovação e cancelamento
4. descarte
5. pedidos por projeto/período
6. histórico recebido por projeto/período
```

Se os fluxos estiverem corretos, iniciar a migração.

## Próxima ordem de trabalho

1. Executar a suíte automatizada e corrigir eventuais falhas.
2. Executar `docs/testes.md` no Postman.
3. Confirmar consistência EstoqueCentral × Lotes.
4. Confirmar filtros de Projeto × Laboratório × período.
5. Integrar PostgreSQL.
6. Configurar conexão por ambiente.
7. Adicionar Flyway.
8. Criar migration inicial com o modelo estabilizado.
9. Criar dados locais de desenvolvimento.
10. Executar testes de integração e concorrência.
11. Implementar autenticação local simulada.
12. Remover `usuarioId` temporário dos endpoints auditáveis.
13. Ativar `DEVOLUCAO` auditada com usuário executor real.
14. Adicionar OpenAPI/Swagger.
15. Iniciar frontend.

## Documentos de referência

- [`README.md`](README.md)
- [`docs/ENDPOINTS_INTERNOS.md`](docs/ENDPOINTS_INTERNOS.md)
- [`docs/testes.md`](docs/testes.md)
- [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md)
- [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md)
- [`docs/CODIGOS_REFERENCIA_LOTE.md`](docs/CODIGOS_REFERENCIA_LOTE.md)
- [`docs/diagrama-uml-completo.puml`](docs/diagrama-uml-completo.puml)

## Histórico recente

| Data | Decisão |
|---|---|
| 07/08/2026 | `Lote` consolidado como composição rastreável do estoque |
| 07/08/2026 | FEFO definido para perecíveis e FIFO para não perecíveis |
| 07/08/2026 | Validade operacional transferida definitivamente para `Lote` |
| 07/08/2026 | `MovimentacaoEstoqueService` passou a centralizar operações físicas |
| 07/08/2026 | Aprovação de pedido passou a consumir lotes por FEFO/FIFO |
| 07/08/2026 | Cancelamento aprovado passou a restaurar exatamente os lotes consumidos |
| 07/08/2026 | Pedidos passaram a poder ser consultados por Projeto + Laboratório + período |
| 07/08/2026 | Histórico de recebimentos passou a poder ser consultado por Projeto + Laboratório + período |
| 07/08/2026 | Suíte automatizada atualizada para os novos filtros |
| 07/08/2026 | Criado inventário de endpoints em `docs/ENDPOINTS_INTERNOS.md` |
