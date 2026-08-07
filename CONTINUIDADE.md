# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 07/08/2026  
**Fase atual:** modelagem de lotes e refatoração das movimentações de estoque

Este arquivo registra o estado real do backend, as decisões consolidadas e a ordem recomendada para continuar o desenvolvimento.

## Estado atual

### Concluído

- CRUDs de Unidade, Laboratório, Usuário, Estagiário, Produto e Projeto.
- Estoque central separado por combinação `Unidade + Produto`.
- Entrada e saída manual com alteração de saldo e histórico de movimentação na arquitetura atual.
- Descarte de produto vencido na arquitetura atual.
- Pedido com criação, aprovação, rejeição, entrega e cancelamento.
- Aprovação com baixa transacional do estoque.
- Registro de movimentação `SAIDA` durante a aprovação do pedido.
- Histórico de materiais entregues ao laboratório.
- Validações de consistência entre usuário, laboratório, unidade e projeto.
- Senhas armazenadas com BCrypt.
- Exclusão lógica de usuário por inativação.
- Exceções de domínio e respostas HTTP padronizadas.
- Bloqueio pessimista nas operações críticas de estoque e nas transições de pedido.
- `EstoqueCentralServiceTest`: 5 testes executados com sucesso.
- `PedidoServiceTest`: 5 testes executados com sucesso.
- Total atual: 10 testes, 0 falhas e 0 erros.

## Três mudanças estruturais aprovadas

### 1. Criação do módulo `Lote`

`Lote` passa a representar a entrada física e rastreável de um produto em determinada unidade.

Responsabilidades principais:

- número do lote;
- vínculo com `EstoqueCentral`;
- quantidade inicial;
- quantidade disponível;
- data de entrada;
- data de validade;
- rastreabilidade da entrada;
- participação nas saídas, descartes e devoluções.

A validade operacional deixa de pertencer ao catálogo global de `Produto` e passa a pertencer ao lote.

Para produtos perecíveis, a saída deverá seguir **FEFO — First Expire, First Out**:

```text
Primeiro a vencer → primeiro a sair
```

Uma única saída poderá consumir mais de um lote.

### 2. Separação entre auditoria e lógica de movimentação

A entidade existente `MovimentacaoEstoque` continuará sendo o registro histórico/auditoria do que aconteceu.

Será criado um `MovimentacaoEstoqueService` para centralizar as operações físicas que alteram quantidades.

```text
MovimentacaoEstoque
= entidade de auditoria

MovimentacaoEstoqueService
= coordenação das regras de entrada, saída, descarte e devolução
```

O novo Service deverá coordenar, dentro da mesma transação:

```text
bloqueio do EstoqueCentral
→ seleção/criação/alteração de Lote(s)
→ alteração do saldo agregado
→ criação de MovimentacaoEstoque
```

Ele não deverá assumir regras de ciclo de vida de Pedido, Laboratório ou Usuário que pertençam aos respectivos Services.

### 3. Redução das responsabilidades de `EstoqueCentralService`

`EstoqueCentralService` deixará de ser o proprietário direto das operações físicas de entrada e saída.

Responsabilidades que permanecem nele:

- criar/configurar o registro `Unidade + Produto`;
- consultar estoque;
- listar por unidade;
- consultar estoque baixo;
- alterar quantidade mínima;
- ativar/inativar o registro agregado.

Operações que deverão sair dele:

- entrada física;
- saída física;
- descarte por vencimento.

O `EstoqueCentral` continuará sofrendo a consequência das movimentações através da atualização de `quantidadeAtual`, mas essa atualização será coordenada por `MovimentacaoEstoqueService`.

## Decisão sobre saldo agregado

A decisão foi fechada: **`EstoqueCentral.quantidadeAtual` continuará persistido**.

O lote também possuirá seu próprio saldo disponível.

Regra de consistência:

```text
EstoqueCentral.quantidadeAtual
=
soma de Lote.quantidadeDisponivel dos lotes válidos daquele estoque
```

Toda operação que alterar lote e saldo agregado deverá ocorrer na mesma transação.

Não deverá existir alteração direta de `quantidadeAtual` sem uma operação de negócio correspondente sobre os lotes, exceto eventual migração controlada de dados antigos.

## Modelo conceitual

```text
Produto
  └── EstoqueCentral (Unidade + Produto)
        ├── Lote A — quantidade e validade próprias
        ├── Lote B — quantidade e validade próprias
        └── Lote C — quantidade e validade próprias
```

`Produto` permanece como catálogo global.

`EstoqueCentral` representa o saldo agregado do produto dentro de uma Unidade.

`Lote` representa a composição rastreável desse saldo.

## Entrada física

O método atual `EstoqueCentralService.entrada(...)` deverá ser removido após a migração da lógica.

Novo fluxo pretendido:

```text
Receber dados da entrada
→ localizar e bloquear EstoqueCentral
→ validar lote e quantidade
→ criar Lote
→ aumentar EstoqueCentral.quantidadeAtual
→ criar MovimentacaoEstoque ENTRADA
→ confirmar tudo na mesma transação
```

A entrada física será uma operação de movimentação baseada em lote. Não deverá existir entrada que apenas aumente o saldo agregado.

## Saída e FEFO

A saída deverá reduzir simultaneamente os lotes envolvidos e o saldo agregado.

Exemplo:

```text
EstoqueCentral = 15

Lote A — validade mais próxima — 5
Lote B — validade posterior    — 10

Saída = 7

Lote A: 5 → 0
Lote B: 10 → 8
EstoqueCentral: 15 → 8
```

Para produtos com controle de validade, a seleção será FEFO.

A implementação deverá prever uma saída utilizando vários lotes quando um único lote não possuir quantidade suficiente.

## Pedido

O `PedidoService` continua responsável pelo ciclo do pedido:

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Na aprovação, o `PedidoService` deverá delegar a baixa física ao `MovimentacaoEstoqueService`.

Conceitualmente:

```text
PedidoService
→ valida pedido e quantidade aprovada
→ solicita saída ao MovimentacaoEstoqueService
→ MovimentacaoEstoqueService aplica FEFO e atualiza estoque
→ PedidoService finaliza o status
```

O cancelamento de pedido aprovado deverá futuramente restaurar o saldo e a rastreabilidade dos lotes consumidos.

## Concorrência

A estratégia atual de bloqueio pessimista permanece válida.

Com lotes, a ordem de bloqueio deverá ser consistente para reduzir risco de deadlock:

```text
Pedido, quando aplicável
→ EstoqueCentral
→ Lotes selecionados em ordem determinística
```

A concorrência real será validada posteriormente em PostgreSQL com testes de integração.

## PostgreSQL

A migração para PostgreSQL foi adiada até a estabilização do modelo de lotes para evitar criar migrations sobre um modelo que seria alterado imediatamente.

Depois da refatoração de lote e movimentação:

- integrar PostgreSQL;
- configurar ambiente local;
- adicionar Flyway;
- criar migration inicial já com lotes;
- criar dados de desenvolvimento;
- executar testes de integração e concorrência.

## Autenticação

### Local simulada

Será usada durante desenvolvimento e testes enquanto a infraestrutura corporativa não estiver disponível.

- usuários de teste armazenados no PostgreSQL local;
- perfis e autorizações locais;
- contexto autenticado para ações auditáveis;
- configuração isolada por ambiente.

### Definitiva externa

A autenticação definitiva será fornecida por API externa da empresa e permanece obrigatória para a versão final.

Ela está fora do planejamento sequencial por depender da liberação da hospedagem e da infraestrutura de DevOps corporativa.

Quando liberada, deverá substituir a origem local da identidade sem exigir reescrita das regras de domínio.

## Próxima ordem de trabalho

1. Modelar a entidade `Lote` e seus relacionamentos.
2. Criar `LoteDTO`, `LoteRepository`, `LoteService` e `LoteController` para cadastro controlado e consultas.
3. Criar `MovimentacaoEstoqueService`.
4. Migrar primeiro a entrada física para `MovimentacaoEstoqueService`.
5. Remover a responsabilidade de entrada de `EstoqueCentralService`.
6. Ajustar e executar os testes afetados pela entrada.
7. Implementar saída por lote usando FEFO.
8. Remover a responsabilidade de saída de `EstoqueCentralService`.
9. Adaptar aprovação de pedido para delegar a baixa física.
10. Adaptar descarte por vencimento para lote.
11. Adaptar cancelamento e futura devolução para rastreabilidade por lote.
12. Revisar a modelagem de auditoria para registrar os lotes participantes de cada movimentação.
13. Atualizar e ampliar testes unitários.
14. Atualizar UML e documentação estrutural.
15. Integrar PostgreSQL.
16. Configurar Flyway e criar migrations.
17. Executar testes de integração e concorrência.
18. Implementar autenticação local simulada.
19. Registrar `DEVOLUCAO` auditada.
20. Implementar relatórios, exportações e OpenAPI.
21. Executar estabilização completa.
22. Iniciar frontend.

A autenticação externa não aparece numerada nessa sequência. Ela deverá ser integrada assim que a infraestrutura corporativa estiver disponível e permanece condição obrigatória para implantação definitiva.

## Documentos de referência

- [`README.md`](README.md): visão geral do projeto e decisões principais.
- [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md): fluxo operacional.
- [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md): classes e camadas.
- [`docs/CODIGOS_REFERENCIA_TESTES.md`](docs/CODIGOS_REFERENCIA_TESTES.md): referência dos testes existentes.
- [`docs/CODIGOS_REFERENCIA_LOTE.md`](docs/CODIGOS_REFERENCIA_LOTE.md): referência para implementação inicial do módulo de lotes.
- [`docs/diagrama-uml-completo.puml`](docs/diagrama-uml-completo.puml): entidades e relacionamentos.

## Histórico recente

| Data | Decisão |
|---|---|
| 06/08/2026 | Dez testes unitários executados com sucesso |
| 06/08/2026 | Bloqueio pessimista adicionado a estoque e transições de pedido |
| 06/08/2026 | Autenticação externa retirada da ordem sequencial por depender da infraestrutura corporativa |
| 06/08/2026 | PostgreSQL adiado até estabilização do modelo de lotes |
| 07/08/2026 | `EstoqueCentral.quantidadeAtual` definido como saldo agregado persistido |
| 07/08/2026 | Controle de validade transferido conceitualmente de Produto para Lote |
| 07/08/2026 | FEFO definido como estratégia de consumo dos lotes com validade |
| 07/08/2026 | Entrada física deixa de ser responsabilidade de `EstoqueCentralService` |
| 07/08/2026 | `MovimentacaoEstoqueService` definido como centralizador das alterações físicas de estoque |
| 07/08/2026 | `MovimentacaoEstoque` mantido como entidade de auditoria/histórico |
