# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 19/08/2026  
**Branch da correção:** `refactor/public-uuid`  
**Fase atual:** migração para UUID público concluída, validada e aprovada para merge na `main`.

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

Durante o desenvolvimento houve mismatch de checksum porque a V2 havia sido alterada após uma execução anterior em `dev`. O banco foi recriado em vez de usar `flyway repair`, garantindo que a versão final da migration fosse realmente executada do zero.

**Após o merge desta correção, V2 fica congelada. Toda mudança estrutural posterior deve usar V3 ou superior.**

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

## Estado por camada — CONCLUÍDO

### Entidades

As entidades públicas possuem `publicId` com geração automática via `@PrePersist`.

Durante os testes foi identificado que `MovimentacaoEstoque` e `HistoricoLaboratorio` possuíam `publicId`, porém estavam sem geração automática. Foi adicionado `@PrePersist` nas duas entidades para impedir INSERT com `public_id = NULL`.

### Repositories

Repositories públicos possuem:

```java
Optional<Entidade> findByPublicId(UUID publicId);
```

Buscas por PK/FK `Long` permanecem quando internas, inclusive locks pessimistas, FEFO/FIFO, consultas técnicas e concorrência.

### DTOs

IDs externos foram migrados de `Long` para `UUID` e mapeiam `entity.getPublicId()`.

Foi conferido:

```bash
grep -R "Long .*Id" src/main/java/com/sgl/dto
grep -R "getId()" src/main/java/com/sgl/dto
```

Resultado:

```text
nenhuma ocorrência pendente nos DTOs
```

`AprovarPedidoDTO.ItemAprovacaoDTO.itemId` também utiliza UUID e a validação de duplicidade usa `Set<UUID>`.

### ResourceNotFoundException

A exceção passou a aceitar identificador genérico:

```java
public ResourceNotFoundException(String recurso, Object id)
```

Isso permite UUID na fronteira pública e `Long` em operações internas.

### Services

Migrados:

```text
UnidadeService
LaboratorioService
ProdutoService
ProjetoService
UsuarioService
EstagiarioService
EstoqueCentralService
LoteService
HistoricoLaboratorioService
MovimentacaoEstoqueService
PedidoService
```

Regra aplicada:

```text
entrada pública → UUID
→ findByPublicId(UUID)
→ Long apenas depois de resolver a entidade
```

### Controllers

Migrados:

```text
UnidadeController
LaboratorioController
ProdutoController
ProjetoController
UsuarioController
EstagiarioController
EstoqueCentralController
LoteController
HistoricoLaboratorioController
PedidoController
MovimentacaoEstoqueController
```

`@PathVariable` e `@RequestParam` que identificam entidades públicas agora utilizam UUID.

### DataInitializer

Validado com a nova arquitetura:

```text
Long id → banco gera
UUID publicId → @PrePersist gera
```

A aplicação foi iniciada em banco vazio e o initializer executou normalmente após V1 + V2.

## Testes automatizados — VALIDADO

Foram migrados os testes afetados pela mudança de contrato externo:

```text
HistoricoLaboratorioServiceTest
MovimentacaoEstoqueServiceTest
PedidoServiceTest
PedidoConcorrenciaIntegrationTest
```

Padrão dos fixtures:

```text
Long id       → identidade interna
UUID publicId → identidade externa usada nas chamadas aos Services
```

O teste de concorrência continua preservando locks e validações de saldo.

Resultado final:

```text
mvn clean compile ✅
mvn test ✅
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

Foram executados os cinco testes principais:

```text
1. buscar entidade por UUID                                      ✅
2. criar pedido usando UUIDs relacionados                       ✅
3. aprovar pedido usando pedido UUID + item UUID + aprovador UUID ✅
4. consultar pedidos por laboratório + projeto + período via UUID ✅
5. cancelar pedido aprovado usando UUID                         ✅
```

A aprovação confirmou o fluxo completo da nova fronteira pública até a baixa de estoque e movimentação. O cancelamento confirmou a resolução do pedido por UUID e a restauração interna dos lotes/estoque. A consulta por laboratório/projeto/período confirmou que múltiplos UUIDs externos são resolvidos e convertidos para IDs internos apenas dentro do backend.

### Resultado final da correção

```text
Entidades             ✅
Migration V2          ✅
Repositories          ✅
DTOs                  ✅
Exceptions            ✅
Services              ✅
Controllers           ✅
DataInitializer       ✅
Compilação            ✅
Testes automatizados  ✅
Regressão Postman     ✅
```

**Correção `refactor/public-uuid` aprovada para Pull Request e merge na `main`.**

## Validações manuais críticas anteriores

Antes da migração UUID já estavam validados em PostgreSQL real:

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

A migração UUID não redesenhou essas regras; alterou apenas a fronteira pública dos identificadores.

## Autenticação e auditoria — DECISÃO ATUAL

Autenticação local simulada e revisão final de auditoria/autorização serão implementadas **após o frontend**.

Sequência aprovada:

```text
backend estrutural
→ correções estruturais restantes
→ OpenAPI/Swagger
→ frontend
→ autenticação + auditoria local
→ integração futura com autenticação corporativa
```

Enquanto isso, IDs temporários de usuário usados para testes locais podem continuar existindo, mas sempre como UUID público.

## OpenAPI / Swagger

Swagger continua planejado antes do frontend, após as correções estruturais restantes.

Pré-condições relacionadas ao UUID estão atendidas:

```text
compilação ✅
mvn test ✅
aplicação inicia ✅
V2 aplicada ✅
endpoints migrados para UUID ✅
regressão manual UUID ✅
```

## Frontend

O frontend vem após as correções estruturais restantes e OpenAPI/Swagger.

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
1. merge de refactor/public-uuid na main
2. criar branch própria para a próxima correção estrutural
3. analisar → revisar → validar → aprovar
4. registrar a nova correção neste arquivo
5. após as correções estruturais, seguir para OpenAPI/Swagger
6. frontend
7. autenticação + auditoria local pós-frontend
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
| 18/08/2026 | `@PrePersist` corrigido em `MovimentacaoEstoque` e `HistoricoLaboratorio` |
| 19/08/2026 | Regressão manual UUID concluída: busca, criação, aprovação, consulta relacional e cancelamento aprovados |
| 19/08/2026 | Correção `refactor/public-uuid` aprovada para merge na `main` |
