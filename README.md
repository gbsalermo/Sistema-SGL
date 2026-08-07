# SGL — Sistema de Gestão de Laboratórios

Sistema backend para cadastro de unidades e laboratórios, catálogo de produtos, controle de estoque por unidade e atendimento de pedidos de materiais.

## Estado atual

O backend está funcional e em fase de consolidação. O domínio de estoque já foi refatorado para controle por lotes, validade por lote, consumo FEFO/FIFO e rastreabilidade das movimentações. A próxima etapa técnica é revisar os testes afetados antes da migração para PostgreSQL.

A autenticação definitiva dependerá de uma API externa da empresa. Enquanto a infraestrutura corporativa não estiver disponível, será utilizada autenticação local simulada para desenvolvimento e testes.

## Tecnologias

- Java e Spring Boot
- Spring Data JPA e Hibernate
- Bean Validation
- BCrypt para armazenamento seguro de senhas
- H2 no ambiente atual de desenvolvimento
- PostgreSQL planejado para a próxima etapa de persistência definitiva
- Flyway planejado para migrations
- JUnit e Mockito para testes unitários
- Lombok

## Arquitetura

```text
Requisição HTTP
    ↓
Controller
    ↓ DTO
Service — validações e regras de negócio
    ↓ Entity
Repository — persistência JPA
    ↓
Banco de dados
```

Responsabilidades:

- `controller`: expõe os endpoints REST e delega o processamento.
- `dto`: define os dados recebidos e devolvidos pela API.
- `service`: concentra regras de negócio, consistência e transações.
- `repository`: executa consultas e persistência.
- `model`: representa as entidades e relacionamentos do domínio.
- `exception`: padroniza o tratamento das falhas da API.
- `config`: reúne configurações e dados iniciais do ambiente.

## Modelo do domínio

```text
Unidade
├── Laboratórios
├── Usuários
└── Estoque central
    └── registros por Produto
        └── Lotes

Laboratório
├── Usuários
├── Projetos
├── Pedidos
└── Histórico de materiais recebidos
```

`Produto` funciona como catálogo global. O saldo não fica no produto, mas em `EstoqueCentral`, identificado pela combinação única:

```text
Unidade + Produto
```

`Lote` representa a entrada física e rastreável de um produto naquele estoque, com quantidade inicial, quantidade disponível, data de entrada e, quando o produto for perecível, validade própria.

O saldo agregado permanece persistido em `EstoqueCentral.quantidadeAtual` e deve corresponder à soma das quantidades disponíveis dos lotes.

## Controle por lote, FEFO e FIFO

A validade operacional pertence exclusivamente ao `Lote`, não ao catálogo global de `Produto`.

Para produtos perecíveis, o SGL utiliza **FEFO — First Expire, First Out**:

```text
Primeiro a vencer → primeiro a sair
```

Lotes vencidos não participam da saída normal e seguem para o fluxo de descarte.

Para produtos não perecíveis, que não possuem `dataValidade`, o SGL utiliza **FIFO — First In, First Out**:

```text
Primeiro a entrar → primeiro a sair
```

A ordenação FIFO usa `dataEntrada` e, em caso de empate, o `id` do lote.

Uma saída pode consumir vários lotes. Nesse caso, cada lote afetado gera uma `MovimentacaoEstoque` própria, mantendo a rastreabilidade sem necessidade de uma entidade intermediária.

## Centralização das movimentações de estoque

A arquitetura separa auditoria de execução da regra:

```text
MovimentacaoEstoque
= entidade de registro e auditoria

MovimentacaoEstoqueService
= centralização das regras que alteram fisicamente o estoque
```

`MovimentacaoEstoque` possui vínculo opcional com o `Lote` efetivamente afetado.

`MovimentacaoEstoqueService` coordena:

```text
entrada por lote
saída FEFO/FIFO
descarte por vencimento
devolução/restauração por lote
```

O `EstoqueCentralService` ficou responsável apenas pelo cadastro, configuração e consulta do saldo agregado. Entrada, saída e descarte não pertencem mais a ele.

## Fluxo principal do pedido

1. Um usuário cria um pedido para seu laboratório.
2. O sistema valida usuário, laboratório, unidade, projeto e produtos.
3. O pedido nasce como `PENDENTE`.
4. Um usuário aprovador informa as quantidades aprovadas.
5. `PedidoService` delega a baixa ao `MovimentacaoEstoqueService`.
6. Produtos perecíveis consomem lotes por FEFO; não perecíveis usam FIFO.
7. Uma movimentação `SAIDA` é registrada para cada lote utilizado.
8. Na entrega, o sistema cria o histórico de recebimento do laboratório sem reduzir o estoque novamente.
9. Um pedido aprovado cancelado restaura exatamente os lotes consumidos na aprovação.

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

A movimentação auditada `DEVOLUCAO` será ativada quando o contexto de autenticação local fornecer o usuário executor real do cancelamento.

## Módulos

| Módulo | Papel |
|---|---|
| Unidade | Representa a instituição ou setor proprietário dos laboratórios e estoques |
| Laboratório | Agrupa usuários, projetos, pedidos e históricos |
| Usuário | Identifica solicitantes, aprovadores e demais perfis operacionais |
| Estagiário | Especialização de usuário com dados do estágio |
| Produto | Catálogo e classificação dos materiais |
| EstoqueCentral | Saldo agregado de um produto dentro de uma unidade |
| Lote | Entrada física rastreável, saldo individual e validade quando aplicável |
| MovimentacaoEstoque | Auditoria da quantidade movimentada e do lote afetado |
| MovimentacaoEstoqueService | Coordena entrada, saída, descarte e devolução física |
| Projeto | Contexto opcional de um pedido |
| Pedido e ItemPedido | Solicitação e aprovação de materiais |
| HistoricoLaboratorio | Registro do material efetivamente entregue ao laboratório |

## Regras estruturais importantes

- Cada estoque pertence a uma unidade e a um produto.
- A combinação `unidade_id + produto_id` deve ser única.
- `EstoqueCentral.quantidadeAtual` é saldo agregado persistido.
- O saldo agregado deve permanecer consistente com os lotes.
- Um novo estoque nasce com quantidade zero.
- Toda entrada física cria um lote.
- Produto perecível exige validade no lote e usa FEFO.
- Produto não perecível não possui validade no lote e usa FIFO.
- Lotes vencidos não atendem saídas normais.
- O estoque nunca pode ficar negativo.
- A aprovação reduz apenas a quantidade aprovada.
- A entrega não reduz o estoque novamente.
- O cancelamento aprovado restaura os lotes consumidos.
- Produto sem risco não mantém tipo ou descrição de risco.
- Usuários e demais registros históricos devem ser inativados quando a exclusão física comprometer a rastreabilidade.

## Estrutura do repositório

```text
Sistema-SGL/
├── backend/sgl-backend/
│   ├── src/main/java/com/sgl/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── exception/
│   │   ├── model/
│   │   ├── repository/
│   │   └── service/
│   └── pom.xml
├── docs/
│   ├── FLUXO_DO_SISTEMA.md
│   ├── GUIA_ESTRUTURAL.md
│   ├── CODIGOS_REFERENCIA_LOTE.md
│   ├── diagrama-uml-completo.puml
│   ├── componentes.puml
│   └── sequencia-pedido.puml
├── CONTINUIDADE.md
└── README.md
```

## Executar o backend

```bash
cd backend/sgl-backend
mvn clean install
mvn spring-boot:run
```

A API é iniciada, por padrão, em `http://localhost:8080`.

## Próximas etapas

- revisar e migrar os testes para a arquitetura por lotes;
- atualizar UML e documentação estrutural restante;
- integrar PostgreSQL;
- adicionar Flyway e migrations;
- implementar autenticação local simulada;
- registrar `DEVOLUCAO` com o usuário executor real;
- executar testes de integração e concorrência;
- documentar endpoints com OpenAPI;
- iniciar o frontend.

A autenticação definitiva via API externa permanece obrigatória, porém fora da ordem sequencial por depender da infraestrutura corporativa.

## Documentação

- [`CONTINUIDADE.md`](CONTINUIDADE.md): estado técnico e ordem de continuidade.
- [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md): fluxo ponta a ponta.
- [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md): responsabilidades das classes e camadas.
- [`docs/CODIGOS_REFERENCIA_LOTE.md`](docs/CODIGOS_REFERENCIA_LOTE.md): códigos de referência do módulo de lotes.
- [`docs/diagrama-uml-completo.puml`](docs/diagrama-uml-completo.puml): modelo de domínio.
- [`docs/componentes.puml`](docs/componentes.puml): componentes da aplicação.
- [`docs/sequencia-pedido.puml`](docs/sequencia-pedido.puml): sequência do pedido.

## Responsável

Gabriel Salermo
