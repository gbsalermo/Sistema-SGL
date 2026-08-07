# SGL — Sistema de Gestão de Laboratórios

Sistema backend para cadastro de unidades e laboratórios, catálogo de produtos, controle de estoque por unidade, rastreabilidade por lotes e atendimento de pedidos de materiais.

## Estado atual

O backend está funcional e em fase de consolidação. O domínio de estoque já foi refatorado para controle por lotes, validade por lote, consumo FEFO/FIFO e rastreabilidade das movimentações.

Também foram adicionadas consultas para diferenciar:

```text
pedidos realizados por um projeto
versus
materiais efetivamente recebidos por esse projeto
```

A próxima etapa técnica, após a validação dos testes automatizados e do roteiro Postman, é iniciar a migração para PostgreSQL e Flyway.

A autenticação definitiva dependerá de uma API externa da empresa. Enquanto a infraestrutura corporativa não estiver disponível, será utilizada autenticação local simulada para desenvolvimento e testes.

## Tecnologias

- Java e Spring Boot
- Spring Data JPA e Hibernate
- Bean Validation
- BCrypt
- H2 no ambiente atual
- PostgreSQL planejado como banco definitivo
- Flyway planejado para migrations
- JUnit e Mockito
- Lombok

## Arquitetura

```text
Requisição HTTP
    ↓
Controller
    ↓ DTO
Service
    ↓ Entity
Repository
    ↓
Banco de dados
```

Responsabilidades:

- `controller`: endpoints REST;
- `dto`: contratos de entrada e saída;
- `service`: regras de negócio, consistência e transações;
- `repository`: persistência e consultas;
- `model`: entidades e relacionamentos;
- `exception`: tratamento padronizado de erros;
- `config`: configurações da aplicação.

## Modelo do domínio

```text
Unidade
├── Laboratórios
├── Usuários
└── Estoque central
    └── Produto
        └── Lotes

Laboratório
├── Usuários
├── Projetos
├── Pedidos
└── Histórico de materiais recebidos
```

`Produto` é catálogo global. O saldo pertence ao `EstoqueCentral`, identificado por:

```text
Unidade + Produto
```

`Lote` representa uma entrada física rastreável, com quantidade própria e validade quando o produto for perecível.

Regra central:

```text
EstoqueCentral.quantidadeAtual
=
soma de Lote.quantidadeDisponivel
```

## FEFO e FIFO

A validade operacional pertence ao `Lote`.

Produtos perecíveis utilizam **FEFO — First Expire, First Out**:

```text
primeiro a vencer → primeiro a sair
```

Lotes vencidos não atendem saídas normais e seguem para descarte.

Produtos não perecíveis utilizam **FIFO — First In, First Out**:

```text
primeiro a entrar → primeiro a sair
```

Uma saída pode consumir vários lotes. Cada lote afetado gera uma `MovimentacaoEstoque` própria.

## Movimentações de estoque

```text
MovimentacaoEstoque
= auditoria do que aconteceu

MovimentacaoEstoqueService
= coordenação das operações físicas
```

O service centraliza:

```text
entrada por lote
saída FEFO/FIFO
descarte por vencimento
devolução/restauração por lote
```

`EstoqueCentralService` ficou restrito a cadastro, configuração e consulta do saldo agregado.

## Fluxo de pedido

1. Usuário cria pedido para seu laboratório.
2. O pedido nasce `PENDENTE`.
3. O gestor informa as quantidades aprovadas.
4. `PedidoService` delega a baixa ao `MovimentacaoEstoqueService`.
5. Perecíveis usam FEFO; não perecíveis usam FIFO.
6. Uma `SAIDA` é registrada para cada lote consumido.
7. Na entrega, o sistema cria `HistoricoLaboratorio` sem baixar estoque novamente.
8. Cancelamento de pedido aprovado restaura exatamente os lotes consumidos.

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

## Consultas por projeto

O sistema separa solicitações de recebimentos reais.

### Pedidos realizados

```http
GET /api/v1/pedidos/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD
```

Responde quais pedidos foram criados para aquele projeto no laboratório e período informados, independentemente do status.

### Materiais efetivamente recebidos

```http
GET /api/v1/historico-laboratorio/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD
```

Responde somente o que foi efetivamente entregue ao laboratório por pedidos vinculados ao projeto.

Isso permite comparar, por exemplo:

```text
Projeto 1 em junho
→ quantidade de pedidos realizados
→ quantidade de materiais efetivamente recebidos
```

As consultas validam que o projeto pertence ao laboratório e que `dataInicio <= dataFim`.

## Módulos

| Módulo | Papel |
|---|---|
| Unidade | Instituição/setor proprietário dos laboratórios e estoques |
| Laboratório | Agrupa usuários, projetos, pedidos e históricos |
| Usuário | Solicitantes, gestores e demais perfis |
| Estagiário | Especialização de usuário |
| Produto | Catálogo e classificação dos materiais |
| EstoqueCentral | Saldo agregado por Unidade + Produto |
| Lote | Entrada física, saldo individual e validade |
| MovimentacaoEstoque | Auditoria da quantidade e lote afetados |
| MovimentacaoEstoqueService | Coordena operações físicas de estoque |
| Projeto | Contexto opcional do pedido e eixo de consulta/relatório |
| Pedido / ItemPedido | Solicitação e aprovação de materiais |
| HistoricoLaboratorio | Material efetivamente entregue ao laboratório |

## Regras importantes

- `unidade_id + produto_id` deve ser único no estoque;
- estoque novo nasce com quantidade zero;
- toda entrada física cria lote;
- perecível exige validade no lote e usa FEFO;
- não perecível não possui validade no lote e usa FIFO;
- lote vencido não atende pedido;
- estoque nunca pode ficar negativo;
- aprovação reduz somente a quantidade aprovada;
- entrega não reduz estoque novamente;
- cancelamento aprovado restaura os lotes usados;
- histórico do laboratório representa recebimento efetivo, não apenas solicitação;
- consultas por projeto sempre validam o vínculo Projeto → Laboratório.

## Executar o backend

```bash
cd backend/sgl-backend
mvn clean install
mvn spring-boot:run
```

API local:

```text
http://localhost:8080
```

## Próximas etapas

- executar e corrigir eventuais falhas da suíte automatizada;
- executar o roteiro completo do Postman;
- validar consultas por projeto/período junto com lote/FEFO/FIFO;
- migrar para PostgreSQL;
- adicionar Flyway e migrations;
- criar testes de integração e concorrência;
- implementar autenticação local simulada;
- registrar `DEVOLUCAO` com usuário executor real;
- documentar automaticamente a API com OpenAPI/Swagger;
- iniciar frontend.

## Documentação

- [`CONTINUIDADE.md`](CONTINUIDADE.md): estado técnico e próximos passos.
- [`docs/ENDPOINTS_INTERNOS.md`](docs/ENDPOINTS_INTERNOS.md): inventário de endpoints por entidade e função.
- [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md): fluxo ponta a ponta.
- [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md): responsabilidades das classes.
- [`docs/diagrama-uml-completo.puml`](docs/diagrama-uml-completo.puml): modelo de domínio.

## Responsável

Gabriel Salermo
