# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 14/08/2026  
**Fase atual:** backend estabilizado em PostgreSQL real; validação manual crítica concluída; concorrência validada; etapa atual = correções estruturais antes do OpenAPI/Swagger

Este arquivo registra o estado atual do backend, decisões consolidadas e o ponto exato para continuidade do desenvolvimento. Deve ser tratado como fonte principal de contexto ao retomar o projeto.

## Estado atual

### Banco e migrations

PostgreSQL está configurado e validado no profile `dev`.

Banco local:

```text
sgl
```

A migration inicial está concluída:

```text
src/main/resources/db/migration/V1__create_initial_schema.sql
```

A V1 cria:

```text
unidades
produtos
laboratorios
usuarios
estagiarios
estoque_central
lote
projetos
pedidos
itens_pedido
movimentacao_estoque
historico_laboratorio
```

O Flyway foi validado em banco vazio:

```text
schema vazio
→ flyway_schema_history criado
→ V1 aplicada
→ schema em versão v1
→ Hibernate ddl-auto=validate validou o modelo
```

### Regra definitiva de migrations

```text
V1 está congelada e não deve mais ser alterada.
```

Toda mudança estrutural futura deverá gerar nova migration:

```text
V2__descricao.sql
V3__descricao.sql
...
```

## Profiles

Configuração atual:

```text
application.properties
→ configurações gerais
→ dev ativo temporariamente para facilitar execução local

application-dev.properties
→ PostgreSQL + Flyway

application-test.properties
→ H2 em memória
→ Flyway desabilitado
```

Credenciais PostgreSQL permanecem externas ao repositório:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/sgl}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWD:}
```

O profile `dev` fixado no arquivo principal é apenas conveniência local e deverá ser removido antes do ambiente de produção.

## DataInitializer

O `DataInitializer` foi utilizado com sucesso para popular PostgreSQL com dados de desenvolvimento.

Foram carregados:

```text
unidades
laboratórios
usuários
estagiário
produtos
estoques centrais
lotes
projetos
pedidos
itens de pedido
```

Com banco persistente, o initializer não pode inserir novamente os mesmos dados em toda inicialização por causa de constraints únicas como `unidades.sigla`.

O initializer deve permanecer restrito ao ambiente `dev`.

## Senhas com BCrypt — VALIDADO

O `SecurityConfig` possui `BCryptPasswordEncoder`.

O `UsuarioService` utiliza BCrypt na criação e na alteração de senha.

O `DataInitializer` também grava senhas codificadas.

Fluxo já validado:

```text
DROP DATABASE sgl
→ CREATE DATABASE sgl
→ Flyway executou V1
→ DataInitializer recriou os registros
→ usuários persistidos com hashes BCrypt
```

Nenhuma migration V2 foi necessária, pois `senha VARCHAR(255)` já comporta o hash.

## Testes automatizados

A suíte anterior estava validada em 12/08/2026:

```text
Tests run: 20
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Distribuição anterior:

```text
HistoricoLaboratorioServiceTest → 3
MovimentacaoEstoqueServiceTest → 7
PedidoServiceTest → 9
SglApplicationTests → 1
```

Em 13/08/2026 foi adicionado:

```text
PedidoConcorrenciaIntegrationTest
```

Arquivo:

```text
backend/sgl-backend/src/test/java/com/sgl/service/PedidoConcorrenciaIntegrationTest.java
```

O novo teste foi executado com sucesso isoladamente e valida dois pedidos concorrentes disputando o mesmo saldo.

O `SglApplicationTests` utiliza:

```java
@ActiveProfiles("test")
```

Logo:

```text
mvn test
→ profile test
→ H2 em memória
→ não depende do PostgreSQL de desenvolvimento
```

Ao retomar, é recomendável executar a suíte completa novamente para registrar formalmente o novo total de testes.

## Produto, Lote e EstoqueCentral

`Produto` continua sendo catálogo.

A validade operacional pertence ao `Lote`.

```text
Produto perecível
→ lote exige dataValidade
→ saída FEFO

Produto não perecível
→ lote sem validade
→ saída FIFO
```

`EstoqueCentral` mantém somente a referência ao produto e não duplica `produto.nome` na tabela.

Regra estrutural validada no PostgreSQL:

```text
EstoqueCentral.quantidadeAtual
=
soma de Lote.quantidadeDisponivel
```

Em 13/08/2026 foi executada uma conferência global de todos os estoques e todas as diferenças resultaram em `0`.

## MovimentacaoEstoque

Continua sendo a entidade de auditoria das operações físicas.

Cada lote afetado gera uma movimentação própria, permitindo rastrear:

```text
produto
lote
quantidade
pedido
laboratório
usuário responsável
saldo anterior
saldo posterior
```

`MovimentacaoEstoqueService` centraliza:

```text
entrada
saída
descarte
devolução/restauração
```

## Pedido

Fluxo atual:

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

### Criação

```text
valida usuário/laboratório/projeto/produto
→ cria itens
→ salva pedido como PENDENTE
→ não reduz estoque
→ não reserva lote
```

### Aprovação

```text
PedidoService
→ valida status e quantidades
→ localiza EstoqueCentral
→ chama MovimentacaoEstoqueService.registrarSaida()
→ FEFO/FIFO escolhe lotes
→ reduz lotes
→ reduz saldo agregado
→ registra SAIDA por lote
→ pedido fica APROVADO
```

Endpoint correto:

```http
PUT /api/v1/pedidos/{pedidoId}/aprovar
```

O body usa `itemId`, não `produtoId`.

### Entrega

```text
pedido APROVADO
→ cria HistoricoLaboratorio
→ não baixa estoque novamente
→ pedido fica ENTREGUE
```

### Cancelamento

Se o pedido estava aprovado:

```text
consulta SAIDAS do pedido
→ identifica exatamente os lotes usados
→ restaura os mesmos lotes
→ restaura EstoqueCentral
→ pedido fica CANCELADO
```

## Validação manual com PostgreSQL — CONCLUÍDA EM 13/08/2026

A bateria manual crítica foi concluída no PostgreSQL real.

### Entrada não perecível — VALIDADO

Validado:

```text
novo lote persistido
quantidadeInicial correta
quantidadeDisponivel correta
dataValidade = null
EstoqueCentral incrementado
MovimentacaoEstoque ENTRADA registrada
```

### FEFO — VALIDADO

Cenário:

```text
POSTMAN-FEFO-A = 4 unidades
POSTMAN-FEFO-B = 10 unidades
pedido aprovado = 6 unidades
```

Resultado confirmado:

```text
POSTMAN-FEFO-A: 4 → 0
POSTMAN-FEFO-B: 10 → 8
```

Movimentações confirmadas:

```text
SAIDA 4 → lote A
SAIDA 2 → lote B
```

`EstoqueCentral` também reduziu exatamente 6 unidades.

### FIFO — VALIDADO

O teste manual inicialmente utilizou um estoque que já possuía o lote inicial `INI-MIC-IB`.

O sistema consumiu corretamente esse lote antes dos lotes `POSTMAN-FIFO-A` e `POSTMAN-FIFO-B`, comprovando que a regra considera todos os lotes ativos e ordena por entrada/id.

Conclusão:

```text
produto não perecível
→ lote mais antigo disponível é consumido primeiro
```

Não foi alterada a regra para privilegiar lotes recém-criados, pois isso seria incorreto.

### Lote vencido não atende pedido — VALIDADO

Para montar o cenário foi criado lote válido e depois sua validade foi alterada diretamente no PostgreSQL, já que a API corretamente impede cadastrar um lote já vencido.

Cenário final:

```text
EstoqueCentral = 20
saldo utilizável em lotes válidos = 8
pedido = 9
```

Resultado confirmado:

```text
HTTP 400
Estoque utilizável insuficiente. Disponível nos lotes válidos: 8, solicitado: 9
```

O lote vencido foi ignorado na aprovação.

### Descarte de vencidos — VALIDADO

Foi descartada quantidade 5 do lote vencido.

Validado:

```text
somente lote vencido reduzido
EstoqueCentral -5
MovimentacaoEstoque = DESCARTE_VENCIMENTO
pedidoId = null
```

### Cancelamento de pedido aprovado — VALIDADO

Validado:

```text
pedido → CANCELADO
os mesmos lotes consumidos foram restaurados
EstoqueCentral recebeu a quantidade de volta
movimentações DEVOLUCAO vinculadas aos lotes corretos
```

### Entrega sem segunda baixa — VALIDADO

Validado:

```text
pedido APROVADO → ENTREGUE
lotes não sofrem nova redução
EstoqueCentral não sofre nova redução
não é criada segunda SAIDA
```

### HistoricoLaboratorio — VALIDADO

A entrega criou registro em `historico_laboratorio` com:

```text
laboratório
produto
quantidade aprovada
dataRecebimento
pedido
ativo = true
```

### Consultas Projeto × Laboratório × período — VALIDADO

Foram testadas as consultas de:

```text
pedidos realizados por projeto/período
histórico geral do laboratório por período
materiais efetivamente recebidos por projeto/período
período invertido
projeto pertencente a outro laboratório
```

Importante:

```text
Pedido.dataSolicitacao
→ representa solicitação

HistoricoLaboratorio.dataRecebimento
→ representa recebimento efetivo
```

Uma consulta de histórico por projeto pode retornar `[]` com `200 OK` quando não existe entrega vinculada àquele projeto no período. Isso é comportamento correto.

### Consistência final EstoqueCentral × lotes — VALIDADO

Consulta executada sobre todos os estoques:

```sql
SELECT
    ec.id AS estoque_id,
    ec.produto_id,
    ec.quantidade_atual AS saldo_estoque,
    COALESCE(SUM(l.quantidade_disponivel), 0) AS soma_lotes,
    ec.quantidade_atual - COALESCE(SUM(l.quantidade_disponivel), 0) AS diferenca
FROM estoque_central ec
LEFT JOIN lote l ON l.estoque_central_id = ec.id
GROUP BY ec.id, ec.produto_id, ec.quantidade_atual
ORDER BY ec.id;
```

Resultado:

```text
diferenca = 0 em todos os estoques
```

## Concorrência — VALIDADO

A criação do pedido não reserva saldo.

A saída acontece somente na aprovação.

`PedidoService.aprovar()` busca o pedido com bloqueio e `MovimentacaoEstoqueService` usa bloqueios pessimistas sobre estoque/lotes.

Foi criado o teste:

```text
PedidoConcorrenciaIntegrationTest
```

Cenário:

```text
Estoque/Lote = 10
Pedido A = 7
Pedido B = 7
→ duas threads iniciam a aprovação simultaneamente
```

O teste valida:

```text
exatamente 1 pedido APROVADO
exatamente 1 pedido PENDENTE
EstoqueCentral final = 3
Lote final = 3
saldo nunca negativo
total de SAIDA = 7
somente uma movimentação SAIDA no lote
```

O teste foi executado com sucesso em 13/08/2026.

Commit que adicionou o teste:

```text
e5fd297f4584fa305518199cdc7cb5a9fd3e35e5
```

## Consultas por projeto e laboratório

Pedidos realizados:

```http
GET /api/v1/pedidos/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD
```

Usa `Pedido.dataSolicitacao`.

Materiais efetivamente recebidos:

```http
GET /api/v1/historico-laboratorio/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD
```

Usa `HistoricoLaboratorio.dataRecebimento`.

Histórico geral do laboratório:

```http
GET /api/v1/historico-laboratorio/laboratorio/{laboratorioId}/periodo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD
```

## Documentação de JSON/Postman

Documentos operacionais principais:

```text
docs/JSON_EXEMPLOS.md
docs/ENDPOINTS_INTERNOS.md
docs/testes.md
```

`JSON_EXEMPLOS.md` reúne exemplos para unidade, laboratório, usuário, estagiário, produto, projeto, estoque, lote, descarte, pedido, aprovação, rejeição, entrega, cancelamento, movimentações, histórico e consultas.

## Autenticação — PRÓXIMA FASE

### Local simulada

Agora que os fluxos críticos do PostgreSQL foram estabilizados, a próxima fase planejada é implementar autenticação local simulada.

Objetivo:

```text
login local
→ identificar usuário autenticado
→ obter usuário pelo contexto de segurança
→ remover usuarioId temporário dos endpoints auditáveis
→ usar usuário real nas ENTRADAS / SAIDAS / DESCARTES / DEVOLUCOES
→ revisar autorização por Perfil
```

### Definitiva

Depois da autenticação local e da estabilização do sistema, a autenticação final deverá integrar com a API corporativa fornecida pela infraestrutura da empresa.

## Requisito futuro de reposição/compra

Requisito informado pelo cliente:

```text
Sai primeiro o produto com validade mais próxima.
Na compra existe prazo mínimo de validade por produto.
Compra só ocorre quando estoque estiver em nível crítico segundo histórico dos últimos 5 anos.
```

Interpretação atual:

```text
FEFO → implementado e validado
prazo mínimo de validade por produto → pós-protótipo / nova migration se necessário
nível crítico → baseado em saída histórica, não simplesmente quantidade de pedidos
```

Para cálculo histórico, a fonte correta será `MovimentacaoEstoque` com `tipoMovimentacao = SAIDA`.

## Frontend

Referências registradas:

- TikTok: https://vt.tiktok.com/ZS43bGhrK/
- Salvia Kit: https://github.com/salvia-kit/salvia-kit
- Materio Vuetify: https://github.com/themeselection/materio-vuetify-vuejs-admin-template-free
- Vue Notus: https://github.com/creativetimofficial/vue-notus
- Sneat Vuetify: https://github.com/themeselection/sneat-vuetify-vuejs-admin-template-free

Fluxo planejado:

```text
Templates / referências
        ↓
Figma
        ↓
Selecionar padrões úteis
        ↓
Adaptar ao fluxo do SGL
        ↓
Componentes reutilizáveis
        ↓
Design System
        ↓
Implementação frontend
```

Design System inicial:

```text
Foundations
→ cores
→ tipografia
→ espaçamento
→ bordas
→ estados visuais

Componentes
→ Button
→ Input / FormField
→ StatusBadge
→ DataTable
→ DashboardCard
→ Sidebar
→ Modal
```

## Pós-protótipo

Após a primeira versão funcional, novas necessidades deverão ser incorporadas incrementalmente.

Fluxo:

```text
necessidade real
→ registrar requisito
→ analisar impacto
→ classificar: regra / relatório / endpoint / UI / banco
→ implementar incrementalmente
→ V2/V3/... se houver mudança estrutural
→ testes
→ homologação
→ release
```

Possíveis itens futuros:

```text
relatórios e indicadores
média histórica de saída
estoque crítico baseado no histórico
apoio à reposição/compra
prazo mínimo de validade
novos filtros
dashboards
melhorias de UX
automações
integrações corporativas
```

## Próximos passos gerais

1. executar `mvn test` completo novamente e registrar o total atualizado com o teste de concorrência;
2. implementar autenticação local simulada;
3. remover `usuarioId` temporário dos endpoints auditáveis e usar contexto autenticado;
4. garantir auditoria de `DEVOLUCAO` com executor autenticado real;
5. revisar autorização por `Perfil`;
6. OpenAPI/Swagger;
7. frontend;
8. deploy da primeira versão;
9. integração futura com autenticação corporativa;
10. pós-protótipo.

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
| 07/08/2026 | Validade transferida para `Lote` |
| 07/08/2026 | `MovimentacaoEstoqueService` passou a centralizar operações físicas |
| 07/08/2026 | Cancelamento passou a restaurar os lotes exatos consumidos |
| 10/08/2026 | Suíte com 20 testes validada |
| 10/08/2026 | PostgreSQL/Flyway e profiles `dev`/`test` configurados |
| 11/08/2026 | V1 aplicada e Hibernate validou o schema |
| 11/08/2026 | Ajuste de `ativo` na herança `Estagiario` concluído |
| 11/08/2026 | DataInitializer executou integralmente em PostgreSQL |
| 11/08/2026 | V1 congelada |
| 12/08/2026 | BCrypt validado para persistência de senhas |
| 12/08/2026 | Banco recriado e fluxo Flyway + DataInitializer validado novamente |
| 12/08/2026 | `mvn test`: 20 testes, 0 falhas, BUILD SUCCESS |
| 12/08/2026 | Entrada não perecível validada no Postman e PostgreSQL |
| 12/08/2026 | Dois lotes perecíveis preparados para teste FEFO |
| 12/08/2026 | Pedido FEFO criado e aprovação concluída com `PUT /pedidos/{id}/aprovar` |
| 12/08/2026 | Criado `docs/JSON_EXEMPLOS.md` com exemplos operacionais da API |
| 13/08/2026 | FEFO validado fisicamente: 4+2 consumidos nos lotes corretos |
| 13/08/2026 | FIFO validado com consumo do lote mais antigo existente |
| 13/08/2026 | Lote vencido confirmado como indisponível para aprovação normal |
| 13/08/2026 | Descarte de vencimento validado |
| 13/08/2026 | Cancelamento validado restaurando exatamente os lotes consumidos |
| 13/08/2026 | Entrega validada sem segunda baixa e com criação de `HistoricoLaboratorio` |
| 13/08/2026 | Consultas por projeto/laboratório/período e histórico geral validadas |
| 13/08/2026 | Consistência global `EstoqueCentral = soma dos lotes` validada com diferença zero |
| 13/08/2026 | Criado e executado com sucesso `PedidoConcorrenciaIntegrationTest` |

### Próxima ação ao retomar

```text
1. executar mvn test completo
→ confirmar que toda a suíte, incluindo PedidoConcorrenciaIntegrationTest, passa em conjunto
→ registrar o novo total de testes

2. iniciar autenticação local simulada
→ definir fluxo de login
→ usar BCrypt existente
→ criar contexto de usuário autenticado
→ substituir usuarioId temporário nos endpoints auditáveis
→ preservar auditoria de ENTRADA, SAIDA, DESCARTE e DEVOLUCAO
→ revisar autorização por Perfil

Não voltar aos testes manuais FEFO/FIFO, salvo regressão específica.
A bateria manual crítica em PostgreSQL foi concluída em 13/08/2026.
```
