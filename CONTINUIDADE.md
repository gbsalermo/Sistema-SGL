# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 06/08/2026  
**Fase atual:** proteção dos fluxos críticos e preparação da infraestrutura

Este arquivo registra o estado real do backend, as decisões já consolidadas e a ordem recomendada para continuar o desenvolvimento.

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
- Revisão das mensagens de erro mais evidentes.
- Exceções de domínio criadas para recurso inexistente e violação de regra de negócio.
- Migração das exceções genéricas dos Services para exceções personalizadas.
- Respostas HTTP de erro padronizadas por `RestExceptionHandler`.
- Tratamento de validação de DTO, JSON inválido, parâmetros ausentes, conflito de dados e erro interno.
- Documentação estrutural, fluxo e fontes UML atualizados.

### Pendente

- Criar uma primeira camada de testes unitários para os fluxos críticos de estoque e pedido.
- Revisar concorrência de estoque e bloqueio de atualizações simultâneas.
- Preparar migrations e PostgreSQL definitivo.
- Implementar autenticação local para desenvolvimento usando usuários de teste do PostgreSQL.
- Integrar a autenticação definitiva fornecida pela API externa.
- Registrar movimentação `DEVOLUCAO` ao cancelar pedido aprovado, usando o usuário autenticado como responsável.
- Implementar consultas e endpoints de relatórios após a migração para PostgreSQL.
- Adicionar exportação de relatórios em PDF e Excel.
- Criar documentação OpenAPI.
- Executar uma etapa completa de testes de integração, controllers e estabilização antes do frontend.
- Iniciar o frontend.

## Decisões oficiais

### Produto e estoque

`Produto` é um catálogo global. Ele descreve o material, risco, perecibilidade e forma de armazenamento, mas não possui saldo.

`EstoqueCentral` representa o saldo de um produto em uma unidade. Sua identidade lógica é:

```text
Unidade + Produto
```

A mesma unidade não pode possuir dois registros de estoque para o mesmo produto.

### Movimentação

Toda alteração de saldo relevante deve gerar `MovimentacaoEstoque`, contendo:

- produto e registro de estoque afetado;
- usuário responsável;
- tipo e origem da movimentação;
- quantidade movimentada;
- saldo anterior e saldo resultante;
- data e observação;
- pedido ou laboratório quando aplicável.

### Pedido

O pedido nasce como `PENDENTE`.

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

- A criação valida os vínculos, mas não reserva saldo.
- A aprovação valida saldo e reduz `quantidadeAprovada`.
- A aprovação registra movimentação `SAIDA` com origem `PEDIDO`.
- A entrega cria `HistoricoLaboratorio` e não reduz o estoque novamente.
- O cancelamento de pedido aprovado devolve o saldo.
- A movimentação de devolução será adicionada após a autenticação, para que o responsável venha do contexto autenticado e não de um ID enviado pelo cliente.
- Pedido entregue não pode ser cancelado pelo fluxo comum.

### Exceções e respostas HTTP

Os Services devem utilizar exceções de domínio em vez de exceções genéricas:

```text
ResourceNotFoundException
  → recurso solicitado não existe
  → HTTP 404

BusinessRuleException
  → regra de negócio foi violada
  → HTTP 400
```

O `RestExceptionHandler` é responsável por converter as exceções em um corpo HTTP padronizado com:

- data e hora;
- status HTTP;
- categoria do erro;
- mensagem;
- caminho da requisição;
- erros de campos, quando houver falha de Bean Validation.

Também são tratados:

- DTO inválido;
- JSON malformado ou valor de enum inválido;
- parâmetro obrigatório ausente;
- parâmetro com tipo incompatível;
- violação de integridade do banco;
- erros inesperados sem exposição de detalhes internos.

As exceções `EntityNotFoundException` e `IllegalArgumentException` permanecem temporariamente no handler apenas como compatibilidade, mas não devem ser usadas em novas regras dos Services.

### Estratégia de testes

Os testes automatizados serão implementados em duas etapas.

#### Etapa 1 — proteção mínima durante o desenvolvimento

Será criada agora uma camada pequena de testes unitários com JUnit e Mockito para proteger as regras mais críticas já existentes.

Prioridades:

- entrada aumenta o saldo;
- saída reduz o saldo;
- saída maior que o saldo é bloqueada;
- quantidade zero ou negativa é rejeitada;
- aprovação de pedido pendente reduz o estoque;
- pedido em status inválido não pode ser aprovado;
- falha em um item da aprovação não deve deixar alterações parciais.

Esses testes funcionam como proteção contra regressões durante as próximas alterações, especialmente concorrência, PostgreSQL, autenticação e devolução auditada.

#### Etapa 2 — estabilização completa antes do frontend

Após PostgreSQL, autenticação, devolução, relatórios e OpenAPI, será executada uma bateria mais ampla de testes para validar o backend final antes da integração com o frontend.

Essa etapa incluirá:

- testes de Controller com `MockMvc`;
- testes do `RestExceptionHandler` e dos corpos de erro;
- testes de integração com PostgreSQL;
- testes dos endpoints protegidos;
- testes de autorização por perfil;
- testes completos dos ciclos de pedido e estoque;
- casos extremos e conflitos de dados;
- validação dos endpoints de relatórios;
- execução da suíte completa antes de liberar o backend para o frontend.

### Autenticação

A autenticação definitiva do SGL será fornecida por uma API externa.

Durante o desenvolvimento local, após a migração para PostgreSQL, o sistema utilizará usuários de teste armazenados no próprio banco para simular a autenticação e permitir a validação dos fluxos protegidos.

Regras adotadas:

- o cliente não deve informar manualmente o ID do usuário responsável por ações auditáveis;
- o usuário responsável deve ser obtido do contexto autenticado;
- a autenticação local deve existir apenas como suporte ao desenvolvimento e aos testes;
- a integração com a API externa deve substituir a origem das credenciais sem alterar as regras de negócio dos Services;
- aprovação, cancelamento, entradas, saídas e demais ações auditáveis devem registrar o usuário autenticado.

### Usuários

- O solicitante é `pedido.getUsuario()`.
- O aprovador é informado no DTO de aprovação enquanto a autenticação não está pronta.
- Após a autenticação, o aprovador e os demais responsáveis devem vir do contexto autenticado.
- A movimentação de aprovação registra o aprovador como usuário responsável.
- Usuário é inativado, não removido fisicamente.
- A senha permanece protegida por BCrypt durante os testes locais.

### Produto

- O código de referência é único.
- Na atualização, a verificação de duplicidade ignora o próprio produto.
- `NivelRisco.NENHUM` limpa tipo e descrição de risco.
- Produto com risco exige tipo de risco.
- Produto não perecível limpa data de validade e tipo de perecível.
- Produto perecível exige data de validade e tipo de perecível.

### Relatórios

A etapa de relatórios será iniciada após a migração definitiva para PostgreSQL, quando o modelo de dados e as consultas estiverem estabilizados.

A primeira versão deve disponibilizar endpoints JSON para:

- estoque baixo por Unidade;
- movimentações por período, produto, usuário e origem;
- pedidos por status, laboratório e período;
- produtos vencidos ou próximos do vencimento;
- materiais entregues por Laboratório;
- consumo de produtos por Unidade ou Laboratório.

Depois da validação das consultas, o backend poderá oferecer exportação em PDF e Excel. O frontend será responsável por selecionar filtros, exibir os resultados e iniciar os downloads.

## Fluxo técnico

```text
Cliente
  → Controller
  → DTO validado
  → Service transacional
  → Repository
  → Banco
  → DTO de resposta
```

As regras que dependem do estado do banco pertencem ao Service. Controller não deve implementar regra de negócio. Repository não deve trabalhar com DTO.

## Próxima ordem de trabalho

1. Criar testes unitários mínimos dos fluxos críticos de estoque e pedido.
2. Revisar concorrência de saldo.
3. Migrar para PostgreSQL com migrations e dados de teste.
4. Implementar autenticação local usando os usuários de teste do PostgreSQL.
5. Preparar a integração com a API externa de autenticação.
6. Obter o usuário responsável pelo contexto autenticado nas ações auditáveis.
7. Registrar `DEVOLUCAO` durante o cancelamento de pedido aprovado.
8. Implementar consultas e endpoints JSON de relatórios.
9. Implementar exportação de relatórios em PDF e Excel.
10. Adicionar OpenAPI.
11. Criar testes de Controller e do `RestExceptionHandler`.
12. Criar testes de integração com PostgreSQL, autenticação e fluxos completos.
13. Executar a etapa final de estabilização do backend.
14. Iniciar frontend e integrar visualização e download dos relatórios.

## Cenários prioritários de teste

### Etapa 1 — testes mínimos

- entrada aumenta o saldo e registra movimentação;
- saída reduz o saldo e registra movimentação;
- saída maior que o saldo é bloqueada;
- quantidade zero ou negativa é rejeitada;
- aprovação parcial reduz somente a quantidade aprovada;
- estoque insuficiente impede a aprovação;
- pedido fora de `PENDENTE` não pode ser aprovado;
- falha durante a aprovação não deve persistir alterações parciais.

### Etapa 2 — estabilização final

- resposta `404` com corpo padrão para recurso inexistente;
- resposta `400` com corpo padrão para regra de negócio;
- lista de erros de campos quando o DTO for inválido;
- resposta para JSON malformado ou enum inválido;
- conflito de integridade convertido em `409`;
- integração real com PostgreSQL e migrations;
- autenticação local e integração com a API externa;
- acesso permitido ou negado de acordo com o perfil;
- produto vencido sem autorização;
- pedido aprovado cancelado e saldo devolvido;
- movimentação `DEVOLUCAO` associada ao usuário autenticado;
- tentativa de cancelar pedido entregue;
- movimentação com saldo anterior e atual corretos;
- duplicidade de `Unidade + Produto`;
- atualização de produto sem falso conflito de código;
- atualização de usuário sem troca de senha;
- inativação repetida de usuário;
- filtros e exportações dos relatórios.

## Documentos de referência

- [`README.md`](README.md): apresentação e execução.
- [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md): fluxo operacional completo.
- [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md): papel das classes e camadas.
- [`docs/diagrama-uml-completo.puml`](docs/diagrama-uml-completo.puml): entidades e relacionamentos.

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
| 06/08/2026 | Autenticação local definida com usuários de teste do PostgreSQL antes da integração externa |
| 06/08/2026 | Respostas HTTP de erro padronizadas com `RestExceptionHandler` |
| 06/08/2026 | Exceções genéricas dos Services migradas para exceções de domínio |
| 06/08/2026 | Estratégia de testes dividida em proteção mínima e estabilização final |
