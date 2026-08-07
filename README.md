# SGL — Sistema de Gestão de Laboratórios

Sistema backend para cadastro de unidades e laboratórios, catálogo de produtos, controle de estoque por unidade e atendimento de pedidos de materiais.

## Estado atual

O backend está funcional e em fase de consolidação. Os fluxos principais de cadastro, estoque e pedido já foram implementados. Antes da migração para PostgreSQL, o domínio de estoque está sendo evoluído para controle por lotes, validade e consumo FEFO.

A autenticação definitiva dependerá de uma API externa da empresa. Enquanto a infraestrutura corporativa não estiver disponível, desenvolvimento e testes utilizarão autenticação local simulada.

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

Assim, o mesmo produto pode possuir saldos diferentes em unidades diferentes.

`Lote` passa a representar a entrada física e rastreável de um produto em determinado estoque, com quantidade e validade próprias. O saldo agregado continuará persistido em `EstoqueCentral.quantidadeAtual` e deverá permanecer consistente com a soma das quantidades disponíveis dos lotes.

## Controle por lote e estratégia FEFO

O SGL adotará controle de estoque por lotes. Cada entrada física deverá criar ou identificar um lote e alimentar o saldo agregado do `EstoqueCentral` dentro da mesma transação.

Para saídas de materiais com lotes controlados por validade, a estratégia adotada será **FEFO — First Expire, First Out**:

```text
Primeiro a vencer → primeiro a sair
```

Exemplo:

```text
Lote A — validade 10/09 — saldo 5
Lote B — validade 20/12 — saldo 10

Saída solicitada: 7

Consumo FEFO:
Lote A: 5 → 0
Lote B: 10 → 8
EstoqueCentral: 15 → 8
```

A opção por FEFO, e não apenas FIFO, ocorre porque a validade é relevante para o domínio. O objetivo é reduzir perdas por vencimento e garantir rastreabilidade da quantidade retirada de cada lote.

Consequências da modelagem:

- a validade operacional pertence ao `Lote`, e não ao catálogo global de `Produto`;
- `Produto` apenas informa características do material, como perecibilidade;
- entradas físicas serão controladas por lote;
- saídas poderão consumir um ou mais lotes;
- descarte por vencimento será feito sobre lotes vencidos;
- aprovação de pedido deverá consumir lotes segundo FEFO;
- cancelamentos e devoluções deverão preservar a rastreabilidade dos lotes envolvidos.

## Centralização das movimentações de estoque

A arquitetura será organizada em duas responsabilidades distintas:

```text
MovimentacaoEstoque
= entidade de registro e auditoria do que aconteceu

MovimentacaoEstoqueService
= centralização das regras que alteram fisicamente o estoque
```

O `MovimentacaoEstoqueService` deverá coordenar operações como entrada, saída, descarte e devolução, mantendo na mesma transação:

```text
Lote(s)
+ EstoqueCentral
+ MovimentacaoEstoque
```

O `EstoqueCentralService` ficará responsável principalmente pela configuração e consulta do saldo agregado, deixando de ser o proprietário direto das operações físicas de entrada e saída.

## Fluxo principal do pedido

1. Um usuário cria um pedido para seu laboratório.
2. O sistema valida usuário, laboratório, unidade, projeto e produtos.
3. O pedido nasce como `PENDENTE`.
4. Um usuário aprovador informa as quantidades aprovadas.
5. Na aprovação, o saldo é validado e a saída deverá consumir os lotes adequados por FEFO.
6. A movimentação de saída é registrada para auditoria.
7. Na entrega, o sistema cria o histórico de recebimento do laboratório.
8. Um pedido aprovado pode ser cancelado antes da entrega, devolvendo o saldo e preservando a rastreabilidade dos lotes.

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Consulte o fluxo detalhado em [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md).

## Módulos

| Módulo | Papel |
|---|---|
| Unidade | Representa a instituição ou setor proprietário dos laboratórios e estoques |
| Laboratório | Agrupa usuários, projetos, pedidos e históricos |
| Usuário | Identifica solicitantes, aprovadores e demais perfis operacionais |
| Estagiário | Especialização de usuário com dados do estágio |
| Produto | Catálogo e classificação de materiais |
| EstoqueCentral | Saldo agregado de um produto dentro de uma unidade |
| Lote | Entrada física rastreável, saldo individual e validade do material |
| MovimentacaoEstoque | Trilha de auditoria de entradas, saídas, descartes e devoluções |
| MovimentacaoEstoqueService | Coordena as operações físicas que alteram lotes e saldo agregado |
| Projeto | Contexto opcional de um pedido |
| Pedido e ItemPedido | Solicitação e aprovação de materiais |
| HistoricoLaboratorio | Registro do material efetivamente entregue ao laboratório |

## Regras estruturais importantes

- Cada registro de estoque pertence a uma unidade e a um produto.
- A combinação `unidade_id + produto_id` deve ser única.
- `EstoqueCentral.quantidadeAtual` permanecerá persistido como saldo agregado.
- Toda alteração de quantidade deverá manter o saldo agregado consistente com os lotes.
- Entrada física deve criar ou identificar um lote; não deve alterar apenas o saldo agregado.
- Saídas por lote utilizarão FEFO quando houver controle de validade.
- O estoque nunca pode ficar negativo.
- A aprovação reduz apenas a quantidade aprovada.
- A entrega não reduz o estoque novamente.
- O cancelamento de um pedido aprovado devolve o saldo.
- A aprovação registra uma movimentação `SAIDA` com origem `PEDIDO`.
- Produto sem risco não mantém tipo ou descrição de risco.
- A validade operacional passa a ser responsabilidade do lote.
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

- criar a entidade e estrutura inicial de `Lote`;
- separar as operações físicas em `MovimentacaoEstoqueService`;
- remover entrada e saída física de `EstoqueCentralService`;
- implementar consumo FEFO;
- adaptar pedido, descarte e devolução para lotes;
- atualizar os testes unitários;
- preparar migrations e migrar para PostgreSQL;
- implementar autenticação local simulada;
- documentar os endpoints com OpenAPI;
- iniciar o frontend.

A autenticação definitiva via API externa permanece obrigatória, porém fora da ordem sequencial por depender da infraestrutura corporativa.

## Documentação

- [`CONTINUIDADE.md`](CONTINUIDADE.md): estado técnico e ordem de continuidade.
- [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md): fluxo ponta a ponta.
- [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md): responsabilidades das classes e camadas.
- [`docs/CODIGOS_REFERENCIA_LOTE.md`](docs/CODIGOS_REFERENCIA_LOTE.md): códigos de referência para a implementação inicial de lotes.
- [`docs/diagrama-uml-completo.puml`](docs/diagrama-uml-completo.puml): modelo de domínio.
- [`docs/componentes.puml`](docs/componentes.puml): componentes da aplicação.
- [`docs/sequencia-pedido.puml`](docs/sequencia-pedido.puml): sequência do pedido.

## Responsável

Gabriel Salermo
