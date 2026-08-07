# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 07/08/2026  
**Fase atual:** modelo de lotes implementado; próxima etapa é revisão de testes antes do PostgreSQL

Este arquivo registra o estado real do backend, as decisões consolidadas e a ordem recomendada para continuar o desenvolvimento.

## Estado atual

### Estrutura de lotes concluída

Foram implementados:

- entidade `Lote`;
- `LoteDTO`, `EntradaLoteDTO` e `AtualizarLoteDTO`;
- `LoteRepository`;
- `LoteService` para consultas e manutenção cadastral;
- `LoteController`;
- vínculo entre `Lote` e `EstoqueCentral`;
- bloqueios pessimistas para seleção e alteração de lotes;
- unicidade lógica de número do lote dentro de cada estoque.

`Lote` representa uma entrada física rastreável e possui:

```text
numeroLote
quantidadeInicial
quantidadeDisponivel
dataEntrada
dataValidade
ativo
EstoqueCentral
```

A quantidade inicial não deve ser alterada depois da entrada. A quantidade disponível só pode mudar através de uma operação física de estoque.

## Responsabilidades atuais

### Produto

`Produto` voltou a ser exclusivamente catálogo.

A validade operacional foi removida de `Produto` e de `ProdutoDTO`.

```text
Produto perecível
→ informa que seus lotes exigem dataValidade

Produto não perecível
→ seus lotes não possuem dataValidade
```

Consultas de validade passam a utilizar `Lote`, não `Produto`.

### EstoqueCentral

`EstoqueCentral` representa o saldo agregado da combinação:

```text
Unidade + Produto
```

`EstoqueCentral.quantidadeAtual` continua persistido.

Regra de consistência:

```text
EstoqueCentral.quantidadeAtual
=
soma de Lote.quantidadeDisponivel
```

O `EstoqueCentralService` não possui mais operações físicas de entrada, saída ou descarte. Ele ficou responsável por cadastro, configuração e consultas do estoque agregado.

Um novo `EstoqueCentral` nasce com:

```text
quantidadeAtual = 0
```

O saldo é alimentado pelas movimentações de lote.

### MovimentacaoEstoque

`MovimentacaoEstoque` continua sendo a entidade de auditoria.

Foi adicionado vínculo opcional:

```text
MovimentacaoEstoque → Lote
```

Quando uma operação utiliza vários lotes, é criada uma `MovimentacaoEstoque` para cada lote afetado. Não foi criada entidade intermediária `MovimentacaoLote`.

Isso permite rastrear exatamente qual quantidade saiu, entrou, foi descartada ou devolvida em cada lote.

### MovimentacaoEstoqueService

`MovimentacaoEstoqueService` centraliza as operações físicas que alteram quantidades.

Atualmente contém lógica para:

```text
entrada por lote
saída FEFO/FIFO
descarte por vencimento
devolução/restauração de lotes de pedido
```

Toda operação altera lote, saldo agregado e movimentação dentro da mesma transação quando houver usuário responsável disponível para auditoria.

## Entrada física

A entrada direta de `EstoqueCentralService` foi removida.

Fluxo atual:

```text
receber EntradaLoteDTO
→ obter usuário responsável do contexto de autenticação
→ bloquear EstoqueCentral
→ validar produto e lote
→ criar Lote
→ aumentar EstoqueCentral.quantidadeAtual
→ registrar MovimentacaoEstoque ENTRADA vinculada ao lote
```

`EntradaLoteDTO` não recebe `usuarioId`. O usuário deverá vir do contexto de autenticação local e, futuramente, da autenticação corporativa.

## FEFO e FIFO

A política de saída foi fechada.

### Produto perecível

Usa **FEFO — First Expire, First Out**:

```text
primeiro a vencer → primeiro a sair
```

Somente lotes com:

```text
ativo = true
quantidadeDisponivel > 0
dataValidade >= hoje
```

participam da saída normal.

Lotes vencidos ficam fora do atendimento de pedidos e seguem para descarte.

### Produto não perecível

Usa **FIFO — First In, First Out** pela entrada:

```text
primeiro a entrar → primeiro a sair
```

Ordenação:

```text
dataEntrada ASC
id ASC
```

Produto não perecível não deve receber `dataValidade` no lote.

## Pedido

O `PedidoService` foi adaptado ao modelo por lotes.

### Aprovação

O `PedidoService` continua responsável por:

```text
validar pedido
validar quantidade aprovada
alterar status
```

Ele não altera mais diretamente o saldo.

Fluxo:

```text
PedidoService.aprovar
→ localiza EstoqueCentral
→ MovimentacaoEstoqueService.registrarSaida
→ seleciona lotes FEFO/FIFO
→ reduz lotes
→ reduz saldo agregado
→ registra uma SAIDA por lote
→ PedidoService marca APROVADO
```

A antiga autorização para utilizar produto vencido foi removida. Lote vencido não participa do fluxo normal de pedido.

### Cancelamento de pedido aprovado

As saídas do pedido são consultadas através de `MovimentacaoEstoque`.

Como cada saída contém o lote usado, o cancelamento consegue restaurar exatamente:

```text
Lote A + quantidade retirada do Lote A
Lote B + quantidade retirada do Lote B
...
EstoqueCentral + total devolvido
```

A restauração física por lote já está implementada.

O registro auditado de `DEVOLUCAO` continuará dependente do usuário executor do cancelamento. Enquanto o contexto de autenticação local ainda não estiver implementado, o cancelamento restaura fisicamente os lotes sem inventar um usuário responsável.

## Descarte por vencimento

O descarte antigo baseado em `Produto.dataValidade` foi removido de `EstoqueCentralService`.

O novo fluxo seleciona somente lotes:

```text
dataValidade < hoje
ativo = true
quantidadeDisponivel > 0
```

Se a quantidade de descarte ultrapassar um lote vencido, o processamento continua pelos próximos lotes vencidos em ordem de validade.

Cada lote descartado gera sua própria movimentação `DESCARTE_VENCIMENTO`.

## Dados iniciais

`DataInitializer` foi adaptado à nova modelagem.

Os produtos perecíveis não possuem mais validade global. Os estoques iniciais possuem lotes correspondentes, preservando desde o início:

```text
EstoqueCentral.quantidadeAtual
=
soma dos lotes
```

## Concorrência

A proteção pessimista continua sendo usada.

Fluxo conceitual:

```text
Pedido, quando aplicável
→ EstoqueCentral
→ Lotes em ordem determinística
```

As consultas FEFO/FIFO utilizam `PESSIMISTIC_WRITE`.

A validação real de concorrência e possíveis deadlocks será feita posteriormente no PostgreSQL com testes de integração.

## Testes

Os testes antigos ainda não foram migrados para a nova arquitetura.

Os 10 testes existentes foram escritos antes da remoção das operações físicas de `EstoqueCentralService`, portanto devem ser revisados antes de serem usados novamente como referência de regressão.

Próximos testes necessários:

- `LoteServiceTest`;
- entrada física por lote;
- entrada de perecível sem validade;
- rejeição de validade em não perecível;
- saída FEFO;
- saída FIFO;
- saída utilizando vários lotes;
- saldo utilizável insuficiente por existência de lotes vencidos;
- descarte de um e vários lotes vencidos;
- aprovação de pedido por lotes;
- cancelamento restaurando exatamente os lotes consumidos;
- consistência entre soma dos lotes e `EstoqueCentral.quantidadeAtual`.

## Autenticação

### Local simulada

Continua planejada para desenvolvimento e testes após a integração com PostgreSQL.

Ela deverá fornecer o usuário responsável pelo contexto autenticado para que DTOs de operações físicas não recebam `usuarioId` do cliente.

### Definitiva externa

A autenticação definitiva será fornecida por API externa da empresa e permanece obrigatória para a versão final.

Ela continua fora da sequência numerada porque depende da infraestrutura corporativa.

## Próxima ordem de trabalho

1. Revisar e migrar os testes para a arquitetura por lotes.
2. Atualizar UML e documentação estrutural restante.
3. Integrar PostgreSQL.
4. Configurar conexão por ambiente.
5. Adicionar Flyway e criar a migration inicial já com `Lote` e o vínculo `MovimentacaoEstoque.lote`.
6. Criar dados locais de desenvolvimento.
7. Executar testes de integração e concorrência.
8. Implementar autenticação local simulada.
9. Passar o usuário autenticado para entrada, descarte, cancelamento e demais operações auditáveis.
10. Ativar registro `DEVOLUCAO` no cancelamento com o usuário executor real.
11. Implementar relatórios, exportações e OpenAPI.
12. Executar estabilização completa.
13. Iniciar frontend.

A autenticação externa deverá ser integrada assim que a infraestrutura corporativa estiver disponível.

## Documentos de referência

- [`README.md`](README.md)
- [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md)
- [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md)
- [`docs/CODIGOS_REFERENCIA_TESTES.md`](docs/CODIGOS_REFERENCIA_TESTES.md)
- [`docs/CODIGOS_REFERENCIA_LOTE.md`](docs/CODIGOS_REFERENCIA_LOTE.md)
- [`docs/diagrama-uml-completo.puml`](docs/diagrama-uml-completo.puml)

## Histórico recente

| Data | Decisão |
|---|---|
| 06/08/2026 | Dez testes unitários executados com sucesso na arquitetura anterior |
| 06/08/2026 | Bloqueio pessimista adicionado a estoque e pedido |
| 06/08/2026 | PostgreSQL adiado até estabilização do modelo de lotes |
| 07/08/2026 | `EstoqueCentral.quantidadeAtual` mantido como saldo agregado persistido |
| 07/08/2026 | `Lote` implementado como composição rastreável do estoque |
| 07/08/2026 | FEFO definido para perecíveis e FIFO para não perecíveis |
| 07/08/2026 | Validade removida de `Produto` e transferida definitivamente para `Lote` |
| 07/08/2026 | Operações físicas removidas de `EstoqueCentralService` |
| 07/08/2026 | `MovimentacaoEstoqueService` passou a centralizar entrada, saída, descarte e devolução |
| 07/08/2026 | `MovimentacaoEstoque` passou a referenciar o lote afetado |
| 07/08/2026 | Aprovação de pedido passou a consumir lotes por FEFO/FIFO |
| 07/08/2026 | Cancelamento aprovado passou a restaurar exatamente os lotes consumidos |
