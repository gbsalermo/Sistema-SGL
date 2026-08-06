# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 06/08/2026  
**Fase atual:** preparação para integração com PostgreSQL

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

### Próxima etapa

- Integrar o backend com PostgreSQL.
- Preparar configuração de conexão por ambiente.
- Adicionar migrations versionadas.
- Criar dados de teste locais.
- Validar entidades, constraints e relacionamentos no banco real.
- Criar teste de integração concorrente para confirmar os bloqueios pessimistas.

### Pendente no planejamento sequencial

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
- A movimentação `DEVOLUCAO` será adicionada após a autenticação local simulada fornecer o usuário responsável.
- Pedido entregue não pode ser cancelado pelo fluxo comum.

### Concorrência de estoque e pedido

A proteção de concorrência foi implementada com `LockModeType.PESSIMISTIC_WRITE`.

Buscas bloqueadas de estoque são utilizadas em:

- entrada manual;
- saída manual;
- descarte por vencimento;
- aprovação de pedido;
- cancelamento de pedido aprovado;
- futura devolução auditada.

Buscas bloqueadas de pedido são utilizadas em:

- aprovação;
- rejeição;
- entrega;
- cancelamento.

Objetivos:

- impedir duas alterações simultâneas sobre o mesmo saldo;
- impedir o processamento simultâneo do mesmo status de pedido;
- evitar aprovação duplicada, dupla baixa e conflitos entre entrega e cancelamento;
- manter buscas comuns sem bloqueio em operações somente de leitura.

Os testes unitários confirmam que os Services usam os métodos bloqueados corretos. O comportamento real entre duas transações será validado no PostgreSQL por teste de integração.

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
- usuário inativo não pode realizar saída;
- operações de alteração utilizam busca bloqueada.

Cobertura atual da aprovação:

- aprovação parcial reduz somente a quantidade aprovada;
- movimentação `SAIDA` com origem `PEDIDO` é registrada;
- pedido fora de `PENDENTE` não pode ser aprovado;
- quantidade maior que a solicitada é rejeitada;
- estoque insuficiente impede aprovação;
- usuário aprovador é obrigatório na implementação atual;
- pedido e estoque utilizam buscas bloqueadas.

#### Etapa 2 — integração e estabilização

- migrations e schema PostgreSQL;
- testes de integração com banco real;
- teste concorrente de atualização de saldo;
- teste concorrente de transição de pedido;
- autenticação local simulada e autorização por perfil;
- testes de Controller com `MockMvc`;
- testes do `RestExceptionHandler`;
- ciclos completos de pedido e estoque;
- relatórios e exportações.

A integração com a autenticação externa será testada quando a infraestrutura corporativa estiver disponível, sem bloquear o restante da estabilização local.

### PostgreSQL

A próxima etapa deve seguir esta ordem:

1. adicionar ou confirmar o driver PostgreSQL no Maven;
2. definir configuração local por variáveis de ambiente;
3. evitar credenciais reais versionadas;
4. escolher e configurar migrations, preferencialmente Flyway;
5. criar a migration inicial do schema;
6. criar uma base local limpa;
7. validar a inicialização da aplicação;
8. inserir dados de teste de desenvolvimento;
9. executar a suíte unitária;
10. criar os primeiros testes de integração.

A aplicação não deve depender de `ddl-auto=create` como estratégia definitiva. A estrutura do banco deve ser versionada por migrations.

### Autenticação

Existirão duas origens de autenticação.

#### Autenticação local simulada

Será usada continuamente durante o desenvolvimento e os testes enquanto a infraestrutura corporativa não estiver disponível.

- utilizará usuários de teste armazenados no PostgreSQL local;
- permitirá validar perfis, autorizações e ações auditáveis;
- fornecerá o usuário responsável pelo contexto autenticado;
- não dependerá da hospedagem ou do DevOps da empresa;
- deverá ser isolada por configuração ou perfil de ambiente.

#### Autenticação definitiva externa

Será fornecida por uma API externa da empresa e é obrigatória para a versão definitiva.

- não faz parte da ordem sequencial por depender de uma liberação externa sem data confirmada;
- deverá ser integrada assim que a API, a hospedagem e o ambiente corporativo estiverem disponíveis;
- substituirá a origem local das credenciais e da identidade;
- não deverá exigir reescrita das regras de domínio;
- deverá alimentar o mesmo mecanismo interno de usuário autenticado usado pelos Services;
- deverá ser validada antes da implantação definitiva.

O cliente não deve informar manualmente o responsável por ações auditáveis. Tanto na simulação local quanto na integração externa, esse usuário deverá vir do contexto autenticado.

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

1. Integrar PostgreSQL ao projeto.
2. Configurar conexão local por ambiente.
3. Adicionar Flyway e criar a migration inicial.
4. Criar banco e usuário locais de desenvolvimento.
5. Validar schema, constraints e relacionamentos.
6. Adaptar o `DataInitializer` para perfil de desenvolvimento ou migrations de dados.
7. Executar os 10 testes unitários.
8. Criar teste de integração concorrente para estoque.
9. Criar teste de integração concorrente para pedido.
10. Implementar autenticação local simulada.
11. Obter o usuário responsável pelo contexto autenticado local.
12. Registrar `DEVOLUCAO` no cancelamento aprovado.
13. Implementar relatórios JSON.
14. Implementar exportações em PDF e Excel.
15. Adicionar OpenAPI.
16. Executar estabilização completa no ambiente local.
17. Iniciar frontend.

A integração com a autenticação externa não aparece numerada nessa sequência. Ela deve ser executada assim que a infraestrutura corporativa for liberada e permanece condição obrigatória para implantação definitiva.

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
| 06/08/2026 | Próxima etapa definida: integração com PostgreSQL e migrations |
| 06/08/2026 | Autenticação externa removida da ordem sequencial por depender da infraestrutura corporativa |
| 06/08/2026 | Autenticação local simulada mantida para desenvolvimento e testes até a liberação do ambiente da empresa |
