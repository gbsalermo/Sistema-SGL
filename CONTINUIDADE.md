# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 06/08/2026  
**Fase atual:** revisão da modelagem de lote, validade e entrada de estoque

Este arquivo registra o estado real do backend, as decisões consolidadas e a ordem recomendada para continuar o desenvolvimento.

## Estado atual

### Concluído

- CRUDs de Unidade, Laboratório, Usuário, Estagiário, Produto e Projeto.
- Estoque central separado por combinação `Unidade + Produto`.
- Entrada e saída manual com alteração de saldo e histórico de movimentação.
- Descarte de produto vencido.
- Pedido com criação, aprovação, rejeição, entrega e cancelamento.
- Aprovação com baixa transacional do estoque.
- Registro de movimentação `SAIDA` durante a aprovação do pedido.
- Histórico de materiais entregues ao laboratório.
- Validações de consistência entre usuário, laboratório, unidade e projeto.
- Validações de risco, perecibilidade e código de referência do produto.
- Senhas armazenadas com BCrypt.
- Exclusão lógica de usuário por inativação.
- Exceções de domínio e respostas HTTP padronizadas.
- Documentação estrutural, fluxo e fontes UML atualizados.
- Primeira etapa de testes unitários concluída.
- `EstoqueCentralServiceTest`: 5 testes executados com sucesso.
- `PedidoServiceTest`: 5 testes executados com sucesso.
- Total atual: 10 testes, 0 falhas e 0 erros.
- Bloqueio pessimista implementado nas operações que alteram saldo.
- Bloqueio pessimista implementado nas transições de status de pedido.
- Testes unitários executados novamente após os bloqueios, sem regressões.

### Próxima etapa imediata

Antes da integração com PostgreSQL, será revisada a modelagem de lote e validade.

A primeira análise obrigatória será o fluxo de entrada do estoque:

- a entrada não deve mais registrar apenas uma quantidade diretamente em `EstoqueCentral`;
- cada entrada deve criar ou alimentar um `Lote` vinculado ao produto e ao estoque da unidade;
- o lote será responsável por número do lote, quantidade recebida, quantidade disponível, data de entrada e validade;
- o saldo de `EstoqueCentral` continuará representando o total agregado do produto na unidade;
- a criação ou atualização de um lote deverá alimentar diretamente esse saldo agregado dentro da mesma transação;
- movimentação de estoque e lote devem permanecer consistentes entre si.

### Decisão ainda não fechada

Antes de implementar, deve ser decidido se `EstoqueCentral.quantidadeAtual` continuará persistido ou se será calculado pela soma das quantidades disponíveis dos lotes.

Opções:

1. **Saldo agregado persistido**
   - `EstoqueCentral.quantidadeAtual` continua existindo;
   - entradas, saídas, descartes e devoluções atualizam lote e estoque na mesma transação;
   - consultas de saldo são simples e rápidas;
   - exige cuidado para impedir divergência entre lote e estoque.

2. **Saldo calculado pelos lotes**
   - o saldo é obtido pela soma de `Lote.quantidadeDisponivel`;
   - reduz duplicidade de informação;
   - consultas e bloqueios ficam mais complexos;
   - o estoque central passa a funcionar principalmente como agrupador `Unidade + Produto`.

Nenhuma alteração estrutural será implementada antes dessa decisão.

### PostgreSQL — etapa temporariamente adiada

A integração com PostgreSQL continua sendo a etapa seguinte, mas somente será iniciada após estabilizar a modelagem de lote e validade, para evitar criar migrations sobre um modelo que será alterado imediatamente.

Depois da revisão de lotes:

- integrar o backend com PostgreSQL;
- preparar configuração de conexão por ambiente;
- adicionar migrations versionadas;
- criar dados de teste locais;
- validar entidades, constraints e relacionamentos no banco real;
- criar testes de integração concorrentes.

### Pendente no planejamento sequencial

- Modelar a entidade `Lote`.
- Revisar o método de entrada de estoque.
- Definir como entradas alimentam lote e saldo agregado.
- Definir como saídas consomem lotes.
- Definir prioridade de consumo, preferencialmente FEFO: primeiro lote a vencer, primeiro a sair.
- Revisar descarte de produto vencido para operar por lote.
- Revisar aprovação de pedido para baixar quantidades de um ou mais lotes.
- Revisar cancelamento e devolução para restaurar a rastreabilidade por lote.
- Adaptar movimentações para registrar o lote quando aplicável.
- Atualizar testes unitários afetados.
- Integrar PostgreSQL e Flyway.
- Implementar autenticação local simulada usando usuários de teste do PostgreSQL.
- Obter o usuário responsável pelo contexto autenticado local nas ações auditáveis.
- Registrar movimentação `DEVOLUCAO` ao cancelar pedido aprovado.
- Implementar consultas e endpoints JSON de relatórios.
- Adicionar exportação de relatórios em PDF e Excel.
- Criar documentação OpenAPI.
- Executar testes completos de integração, controllers e estabilização antes do frontend.
- Iniciar o frontend.

### Dependência externa obrigatória, fora da sequência

- Integrar a autenticação definitiva fornecida pela API externa da empresa.
- Adaptar a aplicação ao ambiente de hospedagem e DevOps corporativo quando ele for liberado.
- Substituir a origem local simulada da identidade pelo contexto autenticado corporativo sem alterar as regras de negócio.
- Validar autenticação, autorização, perfis e auditoria no ambiente disponibilizado pela empresa.

A integração externa é indispensável para a versão definitiva do SGL, mas não possui posição fixa no planejamento sequencial porque depende da liberação da hospedagem e da infraestrutura de DevOps da empresa.

Enquanto essa dependência não estiver disponível, desenvolvimento, testes e execuções locais continuarão usando autenticação simulada com usuários armazenados no PostgreSQL local.

## Decisões oficiais

### Produto, estoque e lote

`Produto` é um catálogo global e não deve armazenar saldo nem validade de uma entrada específica.

`EstoqueCentral` representa o agrupamento e o saldo total de um produto dentro de uma Unidade. Sua identidade lógica permanece:

```text
Unidade + Produto
```

`Lote` representará uma entrada rastreável desse produto no estoque.

Modelo conceitual inicial:

```text
Produto
  └── EstoqueCentral por Unidade
        ├── Lote A — quantidade e validade próprias
        ├── Lote B — quantidade e validade próprias
        └── Lote C — quantidade e validade próprias
```

Campos candidatos de `Lote`:

- `id`;
- `numeroLote`;
- `estoqueCentral`;
- `produto`, caso seja necessário acesso direto;
- `quantidadeInicial`;
- `quantidadeDisponivel`;
- `dataEntrada`;
- `dataFabricacao`, quando aplicável;
- `dataValidade`;
- `fornecedor` ou referência de origem, se exigido;
- `ativo`;
- observação.

A lista definitiva de campos dependerá da validação dos requisitos com o supervisor.

### Entrada de estoque

O método atual de entrada deverá ser revisado.

Fluxo esperado:

```text
Receber dados da entrada
  → validar produto e unidade
  → localizar e bloquear EstoqueCentral
  → criar o Lote
  → aumentar o saldo agregado
  → registrar MovimentacaoEstoque
  → confirmar tudo na mesma transação
```

A entidade `Lote` será a origem rastreável da entrada e da validade. `EstoqueCentral` continuará representando a disponibilidade total do produto na unidade, caso seja mantido o saldo agregado persistido.

Não deve existir entrada que aumente apenas `EstoqueCentral.quantidadeAtual` sem criar ou identificar o lote correspondente, exceto em uma eventual migração controlada de dados antigos.

### Saída e consumo por lote

A saída não poderá reduzir somente o saldo agregado. Ela também deverá reduzir a quantidade disponível de um ou mais lotes.

A estratégia preferencial a validar é FEFO:

```text
First Expire, First Out
Primeiro a vencer, primeiro a sair
```

Isso evita consumir um lote mais novo enquanto outro está próximo do vencimento.

Uma única saída poderá consumir mais de um lote. Nesse caso, será necessário registrar quais lotes e quantidades participaram da movimentação, possivelmente por uma entidade intermediária entre movimentação e lote.

### Validade e descarte

A validade deve pertencer ao lote, não ao catálogo global de `Produto`.

Consequências esperadas:

- remover ou descontinuar `Produto.dataValidade` como informação operacional de estoque;
- produto perecível apenas indica que seus lotes exigem validade;
- descarte por vencimento deve selecionar um lote vencido;
- relatórios de vencimento devem consultar lotes;
- produtos iguais podem coexistir com datas de validade diferentes.

### Movimentação

Toda alteração relevante de saldo deve gerar `MovimentacaoEstoque`, contendo:

- produto e estoque afetado;
- usuário responsável;
- tipo e origem;
- quantidade movimentada;
- saldo anterior e saldo resultante;
- data e observação;
- pedido ou laboratório quando aplicável;
- lote ou detalhamento de lotes quando aplicável.

### Pedido

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

- A criação valida os vínculos, mas não reserva saldo.
- A aprovação valida saldo e deverá consumir os lotes adequados.
- A aprovação registra movimentação `SAIDA`.
- A entrega cria `HistoricoLaboratorio` e não reduz o estoque novamente.
- O cancelamento de pedido aprovado deverá restaurar saldo e rastreabilidade dos lotes consumidos.
- A movimentação `DEVOLUCAO` será adicionada após a autenticação local simulada fornecer o usuário responsável.
- Pedido entregue não pode ser cancelado pelo fluxo comum.

### Concorrência de estoque, lote e pedido

A proteção atual utiliza `LockModeType.PESSIMISTIC_WRITE`.

Com a inclusão de lotes, será necessário revisar a ordem dos bloqueios para evitar inconsistências e reduzir risco de deadlock.

Ordem candidata:

```text
Pedido, quando aplicável
  → EstoqueCentral
  → Lotes selecionados em ordem determinística
```

Os testes unitários confirmam atualmente o uso dos métodos bloqueados em estoque e pedido. O bloqueio dos lotes será definido durante a implementação e validado posteriormente no PostgreSQL.

### Autenticação

Existirão duas origens de autenticação.

#### Autenticação local simulada

Será usada durante desenvolvimento e testes enquanto a infraestrutura corporativa não estiver disponível.

- utilizará usuários de teste armazenados no PostgreSQL local;
- permitirá validar perfis, autorizações e ações auditáveis;
- fornecerá o usuário responsável pelo contexto autenticado;
- deverá ser isolada por perfil de ambiente.

#### Autenticação definitiva externa

Será fornecida por uma API externa da empresa e é obrigatória para a versão definitiva.

- permanece fora da ordem sequencial por depender de liberação externa;
- deverá ser integrada assim que a infraestrutura corporativa estiver disponível;
- substituirá a origem local da identidade sem exigir reescrita das regras de domínio;
- deverá alimentar o mesmo mecanismo interno de usuário autenticado usado pelos Services.

## Próxima ordem de trabalho

1. Revisar o método atual de entrada de `EstoqueCentralService`.
2. Definir se o saldo agregado será persistido ou calculado pelos lotes.
3. Validar com o supervisor os requisitos de número do lote, validade, fabricação, fornecedor e rastreabilidade.
4. Modelar `Lote` e seus relacionamentos.
5. Definir o DTO de entrada por lote.
6. Refatorar a entrada para criar lote, atualizar saldo e registrar movimentação na mesma transação.
7. Definir e implementar consumo FEFO nas saídas.
8. Adaptar aprovação, descarte, cancelamento e devolução para lotes.
9. Revisar movimentações e histórico para rastreabilidade por lote.
10. Atualizar e ampliar os testes unitários.
11. Atualizar documentação e UML.
12. Integrar PostgreSQL ao projeto.
13. Configurar conexão local por ambiente.
14. Adicionar Flyway e criar a migration inicial já com o modelo de lotes.
15. Criar banco e dados de desenvolvimento.
16. Executar testes de integração e concorrência.
17. Implementar autenticação local simulada.
18. Registrar `DEVOLUCAO` auditada.
19. Implementar relatórios, exportações e OpenAPI.
20. Executar estabilização completa.
21. Iniciar frontend.

A integração com a autenticação externa não aparece numerada nessa sequência. Ela será executada assim que a infraestrutura corporativa for liberada e permanece condição obrigatória para implantação definitiva.

## Documentos de referência

- [`README.md`](README.md): apresentação e execução.
- [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md): fluxo operacional.
- [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md): classes e camadas.
- [`docs/diagrama-uml-completo.puml`](docs/diagrama-uml-completo.puml): entidades e relacionamentos.
- [`docs/CODIGOS_REFERENCIA_TESTES.md`](docs/CODIGOS_REFERENCIA_TESTES.md): referência dos testes unitários.

## Histórico recente

| Data | Decisão |
|---|---|
| 04/08/2026 | Estoque central consolidado por Unidade + Produto |
| 04/08/2026 | Baixa definida no momento da aprovação |
| 05/08/2026 | Movimentação registrada durante aprovação |
| 05/08/2026 | Validações de Produto revisadas |
| 05/08/2026 | BCrypt mantido para proteger senhas |
| 05/08/2026 | Usuário passou a ser inativado em vez de excluído |
| 05/08/2026 | Documentação estrutural e UML revisados |
| 05/08/2026 | Relatórios planejados para depois da migração ao PostgreSQL |
| 06/08/2026 | Movimentação de devolução movida para depois da autenticação |
| 06/08/2026 | Autenticação local definida antes da integração externa |
| 06/08/2026 | Respostas HTTP e exceções de domínio padronizadas |
| 06/08/2026 | Estratégia de testes dividida em proteção mínima e estabilização final |
| 06/08/2026 | Dez testes unitários executados com sucesso |
| 06/08/2026 | Bloqueio pessimista adicionado aos fluxos de alteração de estoque |
| 06/08/2026 | Bloqueio pessimista adicionado às transições de status de pedido |
| 06/08/2026 | Testes unitários executados novamente sem falhas após as mudanças de concorrência |
| 06/08/2026 | Autenticação externa removida da ordem sequencial por depender da infraestrutura corporativa |
| 06/08/2026 | Autenticação local simulada mantida para desenvolvimento e testes |
| 06/08/2026 | Integração PostgreSQL adiada até estabilizar o modelo de lotes e validade |
| 06/08/2026 | Primeira análise definida: entrada de estoque deverá criar lote e alimentar o saldo agregado |
