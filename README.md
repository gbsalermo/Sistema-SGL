# SGL — Sistema de Gestão de Laboratórios

Sistema backend para cadastro de unidades e laboratórios, catálogo de produtos, controle de estoque por unidade e atendimento de pedidos de materiais.

## Estado atual

O backend está funcional e em fase de consolidação. Os fluxos principais de cadastro, estoque e pedido já foram implementados. Autenticação por JWT, migração definitiva para PostgreSQL, testes automatizados e frontend permanecem como próximas etapas.

## Tecnologias

- Java e Spring Boot
- Spring Data JPA e Hibernate
- Bean Validation
- BCrypt para armazenamento seguro de senhas
- H2 no ambiente de desenvolvimento
- PostgreSQL planejado para o ambiente definitivo
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

## Fluxo principal do pedido

1. Um usuário cria um pedido para seu laboratório.
2. O sistema valida usuário, laboratório, unidade, projeto e produtos.
3. O pedido nasce como `PENDENTE`.
4. Um usuário aprovador informa as quantidades aprovadas.
5. Na aprovação, o saldo da unidade é reduzido e uma movimentação de saída é registrada.
6. Na entrega, o sistema cria o histórico de recebimento do laboratório.
7. Um pedido aprovado pode ser cancelado antes da entrega, devolvendo o saldo.

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Consulte o fluxo detalhado em [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md).

## Módulos implementados

| Módulo | Papel |
|---|---|
| Unidade | Representa a instituição ou setor proprietário dos laboratórios e estoques |
| Laboratório | Agrupa usuários, projetos, pedidos e históricos |
| Usuário | Identifica solicitantes, aprovadores e demais perfis operacionais |
| Estagiário | Especialização de usuário com dados do estágio |
| Produto | Catálogo e classificação de materiais |
| EstoqueCentral | Saldo de um produto dentro de uma unidade |
| MovimentacaoEstoque | Trilha de auditoria de entradas, saídas, descartes e ajustes |
| Projeto | Contexto opcional de um pedido |
| Pedido e ItemPedido | Solicitação e aprovação de materiais |
| HistoricoLaboratorio | Registro do material efetivamente entregue ao laboratório |

## Regras estruturais importantes

- Cada registro de estoque pertence a uma unidade e a um produto.
- A combinação `unidade_id + produto_id` deve ser única.
- O estoque nunca pode ficar negativo.
- A aprovação reduz apenas a quantidade aprovada.
- A entrega não reduz o estoque novamente.
- O cancelamento de um pedido aprovado devolve o saldo.
- A aprovação registra uma movimentação `SAIDA` com origem `PEDIDO`.
- Produto sem risco não mantém tipo ou descrição de risco.
- Produto não perecível não mantém tipo de perecível ou data de validade.
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

- padronizar exceções e respostas HTTP;
- criar testes automatizados dos fluxos críticos;
- registrar movimentação na devolução causada por cancelamento;
- implementar autenticação e autorização com Spring Security e JWT;
- preparar migrations e migrar para PostgreSQL;
- documentar os endpoints com OpenAPI;
- iniciar o frontend Vue.js.

## Documentação

- [`CONTINUIDADE.md`](CONTINUIDADE.md): estado técnico e ordem de continuidade.
- [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md): fluxo ponta a ponta.
- [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md): responsabilidades das classes e camadas.
- [`docs/diagrama-uml-completo.puml`](docs/diagrama-uml-completo.puml): modelo de domínio.
- [`docs/componentes.puml`](docs/componentes.puml): componentes da aplicação.
- [`docs/sequencia-pedido.puml`](docs/sequencia-pedido.puml): sequência do pedido.

## Responsável

Gabriel Salermo
