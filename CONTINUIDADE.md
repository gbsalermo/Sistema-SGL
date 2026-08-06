# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 06/08/2026  
**Fase atual:** revisão de concorrência e proteção do saldo de estoque

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

### Em andamento

- Revisar concorrência de saldo.
- Definir bloqueio seguro para operações simultâneas de entrada, saída, aprovação, cancelamento e descarte.
- Preparar os pontos de acesso ao estoque para uso de bloqueio pessimista.

### Pendente

- Validar o bloqueio com testes de integração após a migração para PostgreSQL.
- Preparar migrations e PostgreSQL definitivo.
- Implementar autenticação local usando usuários de teste do PostgreSQL.
- Integrar a autenticação definitiva fornecida pela API externa.
- Obter o usuário responsável pelo contexto autenticado nas ações auditáveis.
- Registrar movimentação `DEVOLUCAO` ao cancelar pedido aprovado.
- Implementar consultas e endpoints JSON de relatórios.
- Adicionar exportação de relatórios em PDF e Excel.
- Criar documentação OpenAPI.
- Executar testes completos de integração, controllers e estabilização antes do frontend.
- Iniciar o frontend.

## Decisões oficiais

### Produto e estoque

`Produto` é um catálogo global e não possui saldo.

`EstoqueCentral` representa o saldo de um produto dentro de uma Unidade. Sua identidade lógica é:

```text
Unidade + Produto
```

A mesma Unidade não pode possuir dois registros de estoque para o mesmo Produto.

### Movimentação

Toda alteração relevante de saldo deve gerar `MovimentacaoEstoque`, contendo:

- produto e estoque afetado;
- usuário responsável;
- tipo e origem;
- quantidade movimentada;
- saldo anterior e saldo resultante;
- data e observação;
- pedido ou laboratório quando aplicável.

### Pedido

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

- A criação valida os vínculos, mas não reserva saldo.
- A aprovação valida saldo, reduz o estoque e registra movimentação `SAIDA`.
- A entrega cria `HistoricoLaboratorio` e não reduz o estoque novamente.
- O cancelamento de pedido aprovado devolve o saldo.
- A movimentação `DEVOLUCAO` será adicionada após a autenticação.
- Pedido entregue não pode ser cancelado pelo fluxo comum.

### Concorrência de estoque

Operações que consultam e alteram saldo devem tratar leitura e escrita como uma única operação protegida.

Problema que será evitado:

```text
Saldo inicial: 10

Operação A lê 10 e tenta retirar 7
Operação B lê 10 e tenta retirar 6

Sem bloqueio, as duas podem considerar o saldo suficiente.
```

Estratégia definida para os fluxos críticos:

- utilizar bloqueio pessimista de escrita ao buscar um estoque que será alterado;
- manter a busca bloqueada dentro de método `@Transactional`;
- impedir que duas transações alterem simultaneamente o mesmo registro;
- manter buscas comuns de consulta sem bloqueio;
- aplicar o bloqueio somente nos fluxos que modificam `quantidadeAtual`;
- validar o comportamento real posteriormente com PostgreSQL e testes de integração concorrentes.

Fluxos que precisam usar busca bloqueada:

- entrada manual;
- saída manual;
- descarte por vencimento;
- aprovação de pedido;
- cancelamento de pedido aprovado;
- futura devolução auditada.

### Exceções e respostas HTTP

Os Services utilizam:

```text
ResourceNotFoundException → HTTP 404
BusinessRuleException     → HTTP 400
```

O `RestExceptionHandler` padroniza erros de domínio, Bean Validation, JSON inválido, parâmetros, conflitos de integridade e erros internos.

### Estratégia de testes

#### Etapa 1 — proteção mínima concluída

Foram criados testes unitários com JUnit e Mockito, sem iniciar Spring ou banco.

Cobertura atual do estoque:

- entrada aumenta saldo e registra movimentação;
- saída reduz saldo e registra movimentação;
- estoque insuficiente bloqueia a saída;
- quantidade zero é rejeitada;
- usuário inativo não pode realizar saída.

Cobertura atual da aprovação:

- aprovação parcial reduz somente a quantidade aprovada;
- movimentação `SAIDA` com origem `PEDIDO` é registrada;
- pedido fora de `PENDENTE` não pode ser aprovado;
- quantidade maior que a solicitada é rejeitada;
- estoque insuficiente impede aprovação;
- usuário aprovador é obrigatório na implementação atual.

O rollback transacional real e a concorrência real serão testados na etapa de integração com banco.

#### Etapa 2 — estabilização antes do frontend

- testes de Controller com `MockMvc`;
- testes do `RestExceptionHandler`;
- testes de integração com PostgreSQL;
- testes concorrentes de atualização de saldo;
- testes de autenticação e autorização;
- ciclos completos de pedido e estoque;
- relatórios e exportações.

### Autenticação

A autenticação definitiva será fornecida por API externa.

Durante o desenvolvimento local, após a migração para PostgreSQL, serão usados usuários de teste armazenados no banco.

O cliente não deve informar manualmente o responsável por ações auditáveis. Esse usuário deverá vir do contexto autenticado.

### Relatórios

Após a migração definitiva para PostgreSQL, serão implementados endpoints JSON para:

- estoque baixo por Unidade;
- movimentações por período, produto, usuário e origem;
- pedidos por status, laboratório e período;
- produtos vencidos ou próximos do vencimento;
- materiais entregues por Laboratório;
- consumo por Unidade ou Laboratório.

Depois serão adicionadas exportações em PDF e Excel.

## Próxima ordem de trabalho

1. Adicionar busca de estoque com bloqueio pessimista no repository.
2. Substituir as buscas comuns pelas buscas bloqueadas nos fluxos que alteram saldo.
3. Executar novamente os 10 testes unitários para confirmar ausência de regressão.
4. Documentar quais operações estão protegidas.
5. Migrar para PostgreSQL com migrations e dados de teste.
6. Criar teste de integração concorrente para confirmar o bloqueio no banco real.
7. Implementar autenticação local.
8. Preparar integração com a API externa de autenticação.
9. Obter o usuário responsável pelo contexto autenticado.
10. Registrar `DEVOLUCAO` no cancelamento aprovado.
11. Implementar relatórios JSON.
12. Implementar exportações em PDF e Excel.
13. Adicionar OpenAPI.
14. Executar testes completos de integração e estabilização.
15. Iniciar frontend.

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
| 06/08/2026 | Iniciada revisão de concorrência com bloqueio pessimista para alteração de saldo |
