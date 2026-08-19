# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 19/08/2026  
**Branch da correção:** `divisao-dto`  
**Fase atual:** divisão de DTOs concluída e validada; branch pronta para merge na `main`.

Este arquivo registra o estado consolidado do backend, as decisões arquiteturais aprovadas e o ponto exato de continuidade.

## Regra de trabalho para correções estruturais

Toda correção estrutural deve seguir:

```text
branch própria da correção
→ análise
→ avaliação
→ revisão
→ aprovação
→ Pull Request
→ main
```

Toda correção aprovada deve ser registrada neste arquivo com o motivo da alteração.

## Estado geral do backend

O backend está estabilizado em PostgreSQL real com Flyway.

Regras principais consolidadas:

```text
Produto = catálogo
Lote = validade + quantidade disponível
Produto perecível → saída FEFO
Produto não perecível → saída FIFO
EstoqueCentral.quantidadeAtual = soma dos lotes
MovimentacaoEstoque = auditoria das operações físicas
Pedido só baixa estoque na aprovação
Entrega não baixa estoque novamente
Cancelamento de pedido aprovado restaura os lotes exatos consumidos
```

Fluxo de Pedido:

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

## Banco e migrations

PostgreSQL permanece como banco do profile `dev`.

A V1 está congelada:

```text
src/main/resources/db/migration/V1__create_initial_schema.sql
```

### V2 — UUID público

A V2 adiciona `public_id UUID`, preenche registros existentes com `gen_random_uuid()`, aplica `NOT NULL` e `UNIQUE` nas tabelas públicas.

Tabelas contempladas:

```text
usuarios
unidades
laboratorios
produtos
estoque_central
lote
projetos
pedidos
itens_pedido
movimentacao_estoque
historico_laboratorio
```

`Estagiario` utiliza o `publicId` herdado de `Usuario`.

Validação da V2:

```text
banco vazio
→ V1 aplicada
→ V2 aplicada
→ Hibernate ddl-auto=validate
→ DataInitializer executado
→ aplicação iniciada normalmente
```

A V2 está congelada. Toda mudança estrutural posterior deve usar V3 ou superior.

### V3 — defaults booleanos

Criada em `mini-ajustes`:

```text
src/main/resources/db/migration/V3__add_boolean_defaults.sql
```

Objetivo:

```text
campos booleanos obrigatórios também possuem valor default no banco
```

Defaults definidos:

```text
ativo      → DEFAULT TRUE
perecivel  → DEFAULT FALSE
```

Tabelas contempladas conforme os campos existentes:

```text
produtos
laboratorios
usuarios
estoque_central
lote
projetos
historico_laboratorio
```

A migration não altera V1/V2, preservando o histórico do Flyway.

## Correção estrutural — UUID público

### Motivo

A API utilizava diretamente IDs sequenciais `Long`, permitindo inferir registros vizinhos.

Arquitetura aprovada:

```text
Long id
→ chave primária interna
→ relacionamentos JPA
→ foreign keys
→ locks
→ queries técnicas internas
→ não atravessa a API

UUID publicId
→ identificador público
→ DTOs
→ endpoints
→ frontend
→ não sequencial
→ único
→ imutável
```

Fluxo padrão:

```text
Controller recebe UUID
→ Service usa findByPublicId(UUID)
→ entidade é localizada
→ backend usa Long id internamente quando necessário
```

## Estado por camada — UUID CONCLUÍDO

### Entidades

As entidades públicas possuem `publicId` com geração automática via `@PrePersist`.

`MovimentacaoEstoque` e `HistoricoLaboratorio` também possuem geração automática, evitando INSERT com `public_id = NULL`.

### Repositories

Repositories públicos possuem:

```java
Optional<Entidade> findByPublicId(UUID publicId);
```

Buscas por PK/FK `Long` permanecem quando internas, inclusive locks pessimistas, FEFO/FIFO, consultas técnicas e concorrência.

### ResourceNotFoundException

A exceção aceita identificador genérico:

```java
public ResourceNotFoundException(String recurso, Object id)
```

Isso permite UUID na fronteira pública e `Long` em operações internas.

### Services e Controllers

A fronteira pública utiliza UUID; o `Long` é usado apenas após a entidade ser resolvida internamente.

## Mini ajustes — CONCLUÍDO

Branch:

```text
mini-ajustes
```

### Padronização de comentários

Foi aplicada a regra:

```text
nome do método deve indicar seu papel
comentário apenas quando a implementação não é autoexplicativa
comentários técnicos em português
```

Comentários redundantes foram removidos. Permaneceram comentários curtos apenas em pontos como:

```text
locks pessimistas
FEFO/FIFO
regras de rastreabilidade
cálculos menos diretos
herança JPA
endpoints temporários de desenvolvimento
```

### Padronização de idioma técnico

Elementos técnicos novos/refatorados foram padronizados em inglês, por exemplo:

```text
gerarPublicId() → generatePublicId()
validateActive()
validateInternProfile()
updateRisk()
updateDates()
validateLotExpirationDate()
```

Não foi feita tradução global de nomes do domínio (`Usuario`, `Pedido`, `Laboratorio`, `Produto`, endpoints e colunas), pois isso alteraria contratos existentes e excederia o escopo de mini ajustes.

### Regras de domínio movidas para Models

Parte das validações que estavam concentradas nos Services foi aproximada das entidades responsáveis.

Exemplos:

```text
Usuario → valida perfil e estado ativo
Laboratorio → valida estado ativo
Projeto → valida datas e estado ativo
Produto → valida risco, perecibilidade e validade de lote
EstoqueCentral → valida estado ativo
```

Objetivo:

```text
Service = orquestração, transação e acesso a repositories
Model = regras diretamente ligadas ao próprio estado da entidade
```

### Relacionamentos N:N — decisão conceitual

Nenhuma nova tabela N:N foi criada nesta etapa.

Possibilidades futuras identificadas:

```text
Projeto x Usuario   → projeto_membro
Projeto x Produto   → projeto_material
Produto x Fornecedor → produto_fornecedor, caso Fornecedor seja criado
```

`Pedido x Produto` já é corretamente representado por `ItemPedido`, pois a relação possui atributos próprios como quantidade solicitada e aprovada.

## Divisão dos DTOs — CONCLUÍDA

Branch:

```text
divisao-dto
```

### Objetivo

Os DTOs antigos acumulavam responsabilidades de entrada e saída. A camada foi separada em contratos explícitos:

```text
dto/
├── request/
└── response/
```

Regras adotadas:

```text
RequestDTO
→ representa entrada da API
→ não carrega o id do próprio recurso em criação/atualização quando ele já vem pela URL
→ relacionamentos externos usam UUID público
→ concentra validações de entrada

ResponseDTO
→ representa saída da API
→ id público = entity.getPublicId()
→ pode trazer nomes e informações enriquecidas
→ não expõe Long interno
→ não expõe senha
```

DTOs de operação também foram movidos para `request`, incluindo aprovação de pedido, entrada/atualização de lote e descarte.

DTOs exclusivamente de consulta, como consumo por laboratório, foram movidos para `response`.

Os DTOs híbridos/legados da raiz de `com.sgl.dto` foram removidos após Services, Controllers e testes passarem a usar diretamente os novos contratos.

### IDs privados e públicos

A separação preserva a arquitetura:

```text
Model / banco
Long id       → interno
UUID publicId → externo

API
RequestDTO  → UUID para relacionamentos externos
ResponseDTO → UUID como id público
```

O `Long id` continua disponível para relacionamentos, locks e consultas técnicas internas, mas não atravessa a API.

### Validação da divisão

Resultado informado após a limpeza final:

```text
mvn clean test ✅
Tests run: 23
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

A suíte inclui o teste de concorrência de aprovação, que também passou após a migração dos DTOs.

O aviso do Surefire sobre `Corrupted channel by directly writing to native stream` não provocou falha da suíte; o build terminou com sucesso.

## Testes automatizados — VALIDADO

Resultado consolidado após as correções estruturais atuais:

```text
mvn clean compile ✅
mvn test ✅
mvn clean test ✅
23 testes ✅
0 falhas ✅
0 erros ✅
```

## Validação integrada da migração UUID — CONCLUÍDA

Validações estruturais:

```text
mvn clean compile ✅
V1 + V2 em banco vazio ✅
Hibernate validate ✅
DataInitializer ✅
aplicação inicia em dev ✅
mvn test ✅
```

### Regressão manual principal no Postman — CONCLUÍDA

```text
1. buscar entidade por UUID                                      ✅
2. criar pedido usando UUIDs relacionados                       ✅
3. aprovar pedido usando pedido UUID + item UUID + aprovador UUID ✅
4. consultar pedidos por laboratório + projeto + período via UUID ✅
5. cancelar pedido aprovado usando UUID                         ✅
```

## Validações manuais críticas anteriores

Já validados em PostgreSQL real:

```text
entrada de lote
FEFO
FIFO
lote vencido fora da aprovação
estoque utilizável insuficiente
descarte por vencimento
cancelamento restaurando os lotes exatos
entrega sem segunda baixa
HistoricoLaboratorio
consultas por projeto/laboratório/período
consistência EstoqueCentral = soma dos lotes
concorrência de aprovação
```

## Autenticação e auditoria — DECISÃO ATUAL

Autenticação local simulada e revisão final de auditoria/autorização serão implementadas após o frontend.

Sequência aprovada:

```text
backend estrutural
→ OpenAPI/Swagger
→ frontend
→ autenticação + auditoria local
→ integração futura com autenticação corporativa
```

Enquanto isso, IDs temporários de usuário usados para testes locais continuam como UUID público.

## OpenAPI / Swagger

Swagger é a próxima etapa após o merge da divisão dos DTOs.

Pré-condições principais:

```text
compilação ✅
mvn test ✅
aplicação inicia ✅
UUID público ✅
mini ajustes ✅
divisão Request/Response DTO ✅
```

## Frontend

O frontend vem após OpenAPI/Swagger.

Referências registradas:

```text
Salvia Kit
Materio Vuetify
Vue Notus
Sneat Vuetify
```

Fluxo planejado:

```text
referências/templates
→ Figma
→ selecionar padrões
→ adaptar ao fluxo do SGL
→ componentes reutilizáveis
→ Design System
→ implementação frontend
```

## Requisito futuro de reposição/compra

Estado:

```text
FEFO → implementado e validado
prazo mínimo de validade → pós-protótipo
estoque crítico histórico → pós-protótipo
```

## Próxima ação

```text
1. merge de divisao-dto na main
2. iniciar OpenAPI/Swagger
3. revisar contratos expostos pela API
4. seguir para frontend
5. autenticação + auditoria local pós-frontend
```

## Documentos de referência

- [`README.md`](README.md)
- [`docs/ENDPOINTS_INTERNOS.md`](docs/ENDPOINTS_INTERNOS.md)
- [`docs/JSON_EXEMPLOS.md`](docs/JSON_EXEMPLOS.md)
- [`docs/testes.md`](docs/testes.md)
- [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md)
- [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md)
- [`docs/CODIGOS_REFERENCIA_LOTE.md`](docs/CODIGOS_REFERENCIA_LOTE.md)
- [`docs/diagrama-uml-completo.puml`](docs/diagrama-uml-completo.puml)

## Histórico recente

| Data | Decisão / validação |
|---|---|
| 07/08/2026 | `Lote` consolidado como composição rastreável do estoque |
| 07/08/2026 | FEFO para perecíveis e FIFO para não perecíveis |
| 07/08/2026 | `MovimentacaoEstoqueService` passou a centralizar operações físicas |
| 11/08/2026 | PostgreSQL/Flyway validado e V1 congelada |
| 12/08/2026 | BCrypt validado |
| 13/08/2026 | Bateria manual crítica e concorrência validadas |
| 14/08/2026 | Arquitetura `Long interno + UUID público` aprovada |
| 18/08/2026 | Entidades, V2, repositories, DTOs, Services e Controllers migrados para UUID |
| 18/08/2026 | V1 + V2 validadas em banco recriado do zero |
| 18/08/2026 | DataInitializer e testes automatizados migrados/validados |
| 19/08/2026 | Regressão manual UUID concluída |
| 19/08/2026 | Correção UUID integrada à `main` |
| 19/08/2026 | Criada V3 com defaults booleanos no banco |
| 19/08/2026 | Comentários técnicos mantidos em português e nomenclatura técnica padronizada |
| 19/08/2026 | Regras diretamente ligadas às entidades movidas parcialmente dos Services para Models |
| 19/08/2026 | `mvn clean compile` e `mvn test` aprovados em `mini-ajustes` |
| 19/08/2026 | DTOs separados em `request` e `response`; DTOs legados removidos |
| 19/08/2026 | Services, Controllers e testes migrados para os novos contratos de DTO |
| 19/08/2026 | `mvn clean test`: 23 testes, 0 falhas, 0 erros |
