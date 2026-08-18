# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 18/08/2026  
**Branch atual da correção:** `refactor/public-uuid`  
**Fase atual:** correção estrutural de identificadores públicos antes do OpenAPI/Swagger

Este arquivo registra o estado consolidado do backend, as decisões arquiteturais já aprovadas e o ponto exato de continuidade.

## Regra de trabalho para correções estruturais

Toda correção estrutural deve seguir o fluxo:

```text
branch própria da correção
→ análise
→ avaliação
→ revisão
→ aprovação
→ Pull Request
→ main
```

Mudanças aprovadas devem ser registradas neste arquivo com o motivo da alteração.

## Estado geral do backend

O backend está estabilizado em PostgreSQL real com Flyway e os fluxos críticos já foram validados manualmente.

Principais regras já consolidadas:

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

Fluxo atual de Pedido:

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

Regra definitiva:

```text
V1 não deve mais ser alterada.
Toda mudança estrutural futura deve gerar V2, V3, V4...
```

A correção de UUID utiliza uma V2 responsável por adicionar `public_id UUID`, preencher registros existentes, aplicar `NOT NULL` e `UNIQUE`.

Entidades/tabelas contempladas:

```text
Usuario / usuarios
Unidade / unidades
Laboratorio / laboratorios
Produto / produtos
EstoqueCentral / estoque_central
Lote / lote
Projeto / projetos
Pedido / pedidos
ItemPedido / itens_pedido
MovimentacaoEstoque / movimentacao_estoque
HistoricoLaboratorio / historico_laboratorio
```

`Estagiario` utiliza o `publicId` herdado de `Usuario`.

## Correção estrutural — UUID público

### Problema que motivou a correção

A API utilizava diretamente IDs sequenciais `Long`. Isso permitia inferir a existência de outros registros a partir de um identificador conhecido.

Exemplo do problema:

```text
id = 27
→ torna previsível a existência de IDs próximos como 22, 23, 24, 25...
```

### Arquitetura aprovada

O `Long id` NÃO foi removido das entidades.

Padrão definitivo:

```text
Long id
→ chave primária interna
→ relacionamentos JPA
→ foreign keys
→ locks
→ consultas técnicas internas
→ não deve atravessar a API

UUID publicId
→ identificador público
→ DTOs
→ endpoints
→ frontend
→ não sequencial
→ único
→ imutável
```

Fluxo esperado:

```text
Controller recebe UUID
→ Service usa findByPublicId(UUID)
→ entidade é localizada
→ backend passa a usar Long id internamente quando necessário
```

Exemplo:

```text
UUID do laboratório
→ laboratorioRepository.findByPublicId(uuid)
→ Laboratorio encontrado
→ laboratorio.getId()
→ query interna por FK Long
```

### Entidades — CONCLUÍDO

As 11 entidades públicas possuem `publicId` com geração automática por UUID e persistência imutável.

`Estagiario` herda o identificador de `Usuario`.

### Migration V2 — PREPARADA

A V2 foi preparada com o padrão:

```text
ADD COLUMN public_id UUID
→ UPDATE com gen_random_uuid() para registros existentes
→ SET NOT NULL
→ UNIQUE
```

Inclui também:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

### Repositories — CONCLUÍDO

Os repositories das entidades públicas possuem busca por:

```java
Optional<Entidade> findByPublicId(UUID publicId);
```

Consultas internas baseadas em PK/FK continuam usando `Long` quando apropriado.

Isso inclui métodos técnicos de concorrência e bloqueio pessimista, como buscas internas de estoque, lote e pedido.

### DTOs — CONCLUÍDO

Identificadores externos dos DTOs foram migrados de `Long` para `UUID`.

Mapeamentos externos usam:

```java
entity.getPublicId()
```

em vez de:

```java
entity.getId()
```

Foi executada a conferência:

```bash
grep -R "Long .*Id" src/main/java/com/sgl/dto
grep -R "getId()" src/main/java/com/sgl/dto
```

Resultado após os ajustes:

```text
nenhuma ocorrência pendente nos DTOs
```

`AprovarPedidoDTO.ItemAprovacaoDTO.itemId` também passou para UUID, incluindo a validação de duplicidade com `Set<UUID>`.

### ResourceNotFoundException — AJUSTADO

A exceção deixou de depender exclusivamente de `Long` e aceita identificador genérico:

```java
public ResourceNotFoundException(String recurso, Object id)
```

Motivo:

```text
operações públicas → UUID
operações internas → Long
```

A camada de exceções não deve ficar acoplada a apenas um tipo de identificador.

### Services — MIGRADOS

Os services foram migrados para UUID nas operações que representam a fronteira externa da aplicação.

Services revisados/migrados:

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

Regra preservada:

```text
entrada pública → UUID
findByPublicId(UUID)
→ Long somente depois que a entidade já foi resolvida
```

Usos internos de `Long` foram mantidos intencionalmente em:

```text
FKs
getId()
existsBy...Id...
queries internas
bloqueios pessimistas
FEFO/FIFO
ordenação técnica
restauração de lotes
concorrência
```

No `PedidoService`, o `itemId` recebido na aprovação é comparado com `ItemPedido.publicId`.

No `MovimentacaoEstoqueService`, operações chamadas diretamente pela API recebem UUID; operações internas chamadas pelo fluxo de pedido podem continuar recebendo PK `Long`.

### Controllers — MIGRADOS

Todos os controllers foram migrados para receber UUID nos identificadores públicos:

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

`@PathVariable` e `@RequestParam` que identificam entidades públicas agora utilizam `UUID`.

Nos endpoints temporários de movimentação, `usuarioId` também passou para UUID e é resolvido por `findByPublicId()`.

## Estado atual da correção UUID

Concluído estruturalmente:

```text
Entidades        ✅
Migration V2     ✅ preparada
Repositories     ✅
DTOs             ✅
Exceptions       ✅
Services         ✅
Controllers      ✅
```

Ainda não considerar a correção aprovada/encerrada.

Falta validar o conjunto integrado.

## Próxima etapa imediata — VALIDAÇÃO DA MIGRAÇÃO UUID

Executar agora:

```text
1. atualizar branch local com refactor/public-uuid
2. compilar o projeto
3. corrigir referências restantes fora de DTO/Service/Controller
4. revisar DataInitializer
5. atualizar testes afetados
6. executar mvn test
7. iniciar aplicação com PostgreSQL/Flyway
8. testar endpoints principais no Postman usando exclusivamente UUID público
9. verificar que nenhum Long interno aparece nos responses ou é exigido nos endpoints
```

Buscas úteis:

```bash
grep -R "@PathVariable Long" src/main/java/com/sgl
grep -R "@RequestParam Long .*Id" src/main/java/com/sgl
grep -R "findById(dto.get" src/main/java/com/sgl
grep -R "getId()" src/main/java/com/sgl/dto
grep -R "Long .*Id" src/main/java/com/sgl/dto
```

As ocorrências restantes de `Long` devem ser avaliadas caso a caso. `Long` interno não é erro por si só.

## Testes já existentes antes da migração UUID

A suíte anterior estava validada com:

```text
HistoricoLaboratorioServiceTest
MovimentacaoEstoqueServiceTest
PedidoServiceTest
SglApplicationTests
PedidoConcorrenciaIntegrationTest
```

O teste de concorrência valida dois pedidos concorrentes disputando o mesmo saldo:

```text
Estoque/Lote = 10
Pedido A = 7
Pedido B = 7
→ somente um deve aprovar
→ saldo final = 3
→ nenhuma quantidade negativa
```

Como assinaturas de Services e DTOs mudaram para UUID, os testes podem precisar ser atualizados antes de voltarem a compilar.

## Validações manuais críticas já concluídas

Em PostgreSQL real foram validados:

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

Esses fluxos não precisam ser redesenhados por causa do UUID. Após a migração, devem apenas ser retestados como regressão usando identificadores públicos.

## Autenticação e auditoria — DECISÃO ATUAL

A autenticação local simulada e a revisão final de auditoria/autorização **não serão implementadas agora**.

Decisão consolidada:

```text
backend estrutural
→ Swagger/OpenAPI
→ frontend
→ autenticação + auditoria local
→ integração futura com autenticação corporativa
```

Enquanto isso, identificadores temporários de usuário necessários para testes locais podem continuar existindo nos endpoints de desenvolvimento, porém devem utilizar UUID público.

A autenticação definitiva futuramente deverá integrar com a API corporativa fornecida pela infraestrutura da empresa.

## OpenAPI / Swagger

Swagger continua sendo a próxima grande etapa funcional após a correção UUID estar compilando e validada.

Antes de iniciar Swagger, garantir:

```text
mvn test passando
aplicação iniciando em dev
V2 aplicada corretamente
endpoints aceitando UUID
responses sem Long interno exposto
fluxos críticos funcionando com UUID
```

## Frontend

O frontend vem após a documentação OpenAPI/Swagger.

Referências já registradas:

- Salvia Kit
- Materio Vuetify
- Vue Notus
- Sneat Vuetify

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

## Requisitos futuros de reposição/compra

Requisito informado pelo cliente:

```text
Sai primeiro o produto com validade mais próxima.
Na compra existe prazo mínimo de validade por produto.
Compra só ocorre quando estoque estiver em nível crítico segundo histórico dos últimos 5 anos.
```

Estado:

```text
FEFO → implementado e validado
prazo mínimo de validade → pós-protótipo
estoque crítico histórico → pós-protótipo
```

## Próximos passos gerais

```text
1. validar integralmente a migração UUID
2. corrigir DataInitializer/testes afetados
3. executar suíte completa
4. regressão principal no Postman com UUID
5. aprovar correção refactor/public-uuid
6. Pull Request para main
7. OpenAPI/Swagger
8. frontend
9. autenticação + auditoria local pós-frontend
10. integração futura com autenticação corporativa
11. deploy e evolução pós-protótipo
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
| 10/08/2026 | Suíte automatizada validada |
| 11/08/2026 | PostgreSQL/Flyway validado e V1 congelada |
| 12/08/2026 | BCrypt validado |
| 13/08/2026 | Bateria manual crítica e concorrência validadas |
| 14/08/2026 | Arquitetura `Long interno + UUID público` aprovada |
| 14/08/2026 | Entidades, V2, repositories e DTOs iniciaram migração para UUID |
| 18/08/2026 | Services migrados para UUID na fronteira externa |
| 18/08/2026 | Controllers migrados para UUID público |
| 18/08/2026 | Próxima etapa definida: compilação, testes e regressão completa da migração UUID |

### Próxima ação ao retomar

```text
VALIDAR A MIGRAÇÃO UUID.

→ puxar refactor/public-uuid
→ compilar
→ corrigir referências restantes
→ revisar DataInitializer e testes
→ mvn test
→ iniciar PostgreSQL/Flyway
→ Postman usando UUID
→ confirmar ausência de Long exposto

Somente após essa validação:
→ aprovar correção
→ Pull Request para main
→ iniciar OpenAPI/Swagger
```
