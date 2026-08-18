# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 18/08/2026  
**Branch atual da correção:** `refactor/public-uuid`  
**Fase atual:** migração para UUID público implementada e validada estruturalmente; regressão manual parcial pendente antes da aprovação final da correção.

Este arquivo registra o estado consolidado do backend, as decisões arquiteturais aprovadas e o ponto exato para continuidade.

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

Regra definitiva:

```text
V1 não deve mais ser alterada.
Toda mudança estrutural posterior deve gerar nova migration.
```

### V2 — UUID público

A correção de UUID utiliza uma V2 que:

```text
ADD COLUMN public_id UUID
→ preenche registros existentes com gen_random_uuid()
→ SET NOT NULL
→ UNIQUE
```

Inclui:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

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

A V2 foi validada em banco recriado do zero:

```text
banco vazio
→ V1 aplicada
→ V2 aplicada
→ Hibernate ddl-auto=validate
→ DataInitializer executado
→ aplicação iniciada normalmente
```

Durante a validação houve mismatch de checksum porque a V2 já havia sido aplicada em versão anterior. Como o ambiente era `dev`, o banco foi recriado em vez de usar `flyway repair`, garantindo que a V2 final fosse realmente executada do zero.

Depois que a correção entrar na `main`, a V2 também deve ser considerada congelada; qualquer mudança estrutural posterior deve usar V3.

## Correção estrutural — UUID público

### Motivo

A API utilizava diretamente IDs sequenciais `Long`, permitindo inferir registros vizinhos.

Exemplo:

```text
id = 27
→ torna previsíveis IDs próximos como 22, 23, 24, 25...
```

### Arquitetura aprovada

O `Long id` NÃO foi removido.

```text
Long id
→ chave primária interna
→ relacionamentos JPA
→ foreign keys
→ locks
→ queries técnicas internas
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

Fluxo padrão:

```text
Controller recebe UUID
→ Service usa findByPublicId(UUID)
→ entidade é localizada
→ backend usa Long id internamente quando necessário
```

Exemplo:

```text
UUID do laboratório
→ laboratorioRepository.findByPublicId(uuid)
→ Laboratorio encontrado
→ laboratorio.getId()
→ query interna por FK Long
```

## Estado por camada

### Entidades — CONCLUÍDO

As entidades públicas possuem `publicId`.

Padrão:

```java
@Column(name = "public_id", nullable = false, unique = true, updatable = false)
private UUID publicId;

@PrePersist
private void gerarPublicId() {
    if (publicId == null) {
        publicId = UUID.randomUUID();
    }
}
```

Durante os testes foi identificado que `MovimentacaoEstoque` e `HistoricoLaboratorio` possuíam `publicId`, porém estavam sem geração automática. Foi adicionado `@PrePersist` nas duas entidades.

Motivo da correção:

```text
public_id é NOT NULL no banco
→ toda nova entidade precisa gerar UUID antes do INSERT
```

### Repositories — CONCLUÍDO

Repositories públicos possuem:

```java
Optional<Entidade> findByPublicId(UUID publicId);
```

Buscas por PK/FK `Long` permanecem quando internas.

Isso inclui locks pessimistas, FEFO/FIFO, consultas por relacionamentos e operações técnicas de concorrência.

### DTOs — CONCLUÍDO

IDs externos foram migrados de `Long` para `UUID`.

DTOs mapeiam:

```java
entity.getPublicId()
```

em vez de:

```java
entity.getId()
```

Foi conferido:

```bash
grep -R "Long .*Id" src/main/java/com/sgl/dto
grep -R "getId()" src/main/java/com/sgl/dto
```

Resultado:

```text
nenhuma ocorrência pendente nos DTOs
```

`AprovarPedidoDTO.ItemAprovacaoDTO.itemId` também usa UUID e a validação de duplicidade passou a utilizar `Set<UUID>`.

### ResourceNotFoundException — AJUSTADO

A exceção passou a aceitar identificador genérico:

```java
public ResourceNotFoundException(String recurso, Object id)
```

Motivo:

```text
fronteira externa → UUID
operações internas → Long
```

### Services — CONCLUÍDO

Services migrados:

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

`Long` foi mantido propositalmente em:

```text
FKs
getId()
queries internas
bloqueios pessimistas
FEFO/FIFO
restauração de lotes
concorrência
ordenação técnica
```

No `PedidoService`, o `itemId` recebido na aprovação é comparado com `ItemPedido.publicId`.

### Controllers — CONCLUÍDO

Controllers migrados:

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

`@PathVariable` e `@RequestParam` que identificam entidades públicas agora recebem UUID.

IDs temporários de usuário usados em endpoints de desenvolvimento também utilizam UUID.

## DataInitializer — VALIDADO

O `DataInitializer` está compatível com a arquitetura.

Construtores que possuem `id` e `publicId` deixam ambos `null`, permitindo:

```text
Long id → banco gera
UUID publicId → @PrePersist gera
```

Entidades criadas via builder também recebem UUID automaticamente no `@PrePersist`.

A aplicação foi iniciada com banco vazio e o DataInitializer executou normalmente após V1 + V2.

## Testes automatizados — VALIDADO

Após a migração, o primeiro `mvn test` falhou em `testCompile` porque testes antigos ainda chamavam Services com `Long`.

Foram migrados:

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

Mocks externos passaram de `findById()` para `findByPublicId()` onde necessário.

O teste de concorrência continuou preservando as verificações de lock e saldo interno.

Durante essa etapa, o teste de concorrência revelou ausência de geração de `publicId` em `MovimentacaoEstoque`; a mesma lacuna foi encontrada preventivamente em `HistoricoLaboratorio` e ambas foram corrigidas com `@PrePersist`.

Resultado final:

```text
mvn test ✅
```

A suíte passou após a migração dos testes e correções de geração dos UUIDs.

## Validação estrutural da migração UUID — CONCLUÍDA

Já validado:

```text
mvn clean compile ✅
V1 + V2 em banco vazio ✅
Hibernate validate ✅
DataInitializer ✅
aplicação inicia em dev ✅
mvn test ✅
```

## Regressão manual da API com UUID — EM ANDAMENTO / PAUSADA

Foi definida uma bateria curta de cinco testes principais:

```text
1. buscar entidade por UUID
2. criar pedido usando UUIDs relacionados
3. aprovar pedido usando pedido UUID + item UUID + aprovador UUID
4. consulta laboratório/projeto/período usando UUID
5. cancelar pedido aprovado usando UUID
```

### Resultado até agora

```text
1. busca por UUID ✅
2. criação de pedido com UUID ✅
3. aprovação → PENDENTE DE RETESTE
4. consulta por relacionamento → PENDENTE
5. cancelamento → PENDENTE
```

Na criação de pedido ocorreu inicialmente um `400` por um UUID de laboratório digitado com tamanho inválido. Após corrigir o UUID enviado, o pedido foi criado normalmente. Isso confirmou a desserialização e resolução de `usuarioId`, `laboratorioId` e `produtoId` por UUID.

### Observação sobre o teste de aprovação

Ao tentar aprovar o pedido, o PostgreSQL registrou:

```text
public_id NULL em movimentacao_estoque
```

Essa falha ocorreu porque a aplicação local ainda estava executando uma JVM iniciada antes da correção que adicionou `@PrePersist` a `MovimentacaoEstoque`.

Na branch remota, a entidade já está corrigida.

Antes de retestar a aprovação:

```bash
git pull origin refactor/public-uuid
mvn clean spring-boot:run
```

Depois repetir os testes 3, 4 e 5.

Não considerar a regressão manual concluída até esses três casos passarem.

## Validações manuais críticas anteriores

Antes da migração UUID já foram validados em PostgreSQL real:

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

Essas regras não foram redesenhadas pela migração UUID; a regressão atual serve para confirmar apenas a nova fronteira pública.

## Estado atual da correção `refactor/public-uuid`

```text
Entidades             ✅
Migration V2          ✅ validada do zero
Repositories          ✅
DTOs                  ✅
Exceptions            ✅
Services              ✅
Controllers           ✅
DataInitializer       ✅
Compilação            ✅
Testes automatizados  ✅
Regressão Postman     ⏸️ parcial
```

A correção ainda NÃO deve ser considerada encerrada ou pronta para PR enquanto os três testes manuais restantes não forem retomados e aprovados.

## Autenticação e auditoria — DECISÃO ATUAL

Autenticação local simulada e revisão final de auditoria/autorização não serão implementadas antes do frontend.

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

Swagger continua planejado antes do frontend.

Pré-condições já atendidas em grande parte:

```text
compilação ✅
mvn test ✅
aplicação inicia ✅
V2 aplicada ✅
endpoints migrados para UUID ✅
```

Ainda falta concluir a regressão manual UUID antes de considerar essa correção pronta para merge.

## Frontend

O frontend vem após as correções estruturais restantes e a etapa OpenAPI/Swagger.

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

## Próxima ação ao retomar a correção UUID

```text
1. atualizar branch local
2. reiniciar a aplicação com código novo
3. retestar aprovação de pedido por UUID
4. testar consulta laboratório/projeto/período por UUID
5. testar cancelamento por UUID
6. confirmar que nenhum Long interno aparece nos contratos públicos
7. aprovar correção refactor/public-uuid
8. Pull Request para main
```

## Próximo trabalho imediato

A regressão manual UUID ficará pausada por enquanto.

Próximo passo de desenvolvimento:

```text
→ iniciar a próxima correção/resolução estrutural em branch própria
→ analisar
→ revisar
→ validar
→ registrar neste CONTINUIDADE.md
```

Depois, retomar a sequência planejada rumo ao frontend.

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
| 18/08/2026 | V1 + V2 validadas em banco recriado do zero |
| 18/08/2026 | DataInitializer validado com geração automática de UUID |
| 18/08/2026 | Testes automatizados migrados para UUID e `mvn test` aprovado |
| 18/08/2026 | `@PrePersist` adicionado a `MovimentacaoEstoque` e `HistoricoLaboratorio` |
| 18/08/2026 | Busca por UUID e criação de pedido por UUID validadas no Postman |
| 18/08/2026 | Regressão manual UUID pausada com aprovação, consulta por relacionamento e cancelamento pendentes |
