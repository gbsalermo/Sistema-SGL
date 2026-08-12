# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 12/08/2026  
**Fase atual:** validação manual dos fluxos do backend em PostgreSQL real

Este arquivo registra o estado atual do backend, decisões consolidadas e o ponto exato para continuidade do desenvolvimento.

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

Foi identificado que, com banco persistente, o initializer não pode inserir novamente os mesmos dados em toda inicialização, pois existem constraints únicas como `unidades.sigla`.

O fluxo de desenvolvimento deve evitar duplicação dos dados iniciais e manter o initializer restrito ao ambiente `dev`.

## Senhas com BCrypt — VALIDADO

O `SecurityConfig` já possui `BCryptPasswordEncoder`.

O `UsuarioService` utiliza BCrypt na criação e na alteração de senha.

O `DataInitializer` também foi ajustado para inserir senhas codificadas em vez de texto puro.

O banco `sgl` foi recriado para validar o fluxo completo:

```text
DROP DATABASE sgl
→ CREATE DATABASE sgl
→ Flyway executou V1
→ DataInitializer recriou os registros
→ usuários persistidos com hashes BCrypt
```

Consulta utilizada para validação:

```sql
SELECT id, nome, email, senha
FROM usuarios
ORDER BY id;
```

Resultado esperado e confirmado: senhas armazenadas como hash BCrypt, não como `123456` em texto puro.

Nenhuma migration V2 foi necessária, pois `senha VARCHAR(255)` já comporta o hash.

## Testes automatizados — VALIDADO

A suíte completa foi executada novamente em 12/08/2026 após os ajustes recentes.

Resultado:

```text
Tests run: 20
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Distribuição atual:

```text
HistoricoLaboratorioServiceTest → 3
MovimentacaoEstoqueServiceTest → 7
PedidoServiceTest → 9
SglApplicationTests → 1
```

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

Para exibição, DTOs/consultas podem retornar `produtoNome` através do relacionamento.

Regra estrutural:

```text
EstoqueCentral.quantidadeAtual
=
soma de Lote.quantidadeDisponivel
```

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

Na criação do pedido:

```text
valida usuário/laboratório/projeto/produto
→ cria itens
→ salva pedido como PENDENTE
→ não reduz estoque
→ não reserva lote
```

### Aprovação

Na aprovação:

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

O endpoint correto é:

```http
PUT /api/v1/pedidos/{pedidoId}/aprovar
```

Não usar `POST` para aprovação.

Exemplo:

```json
{
  "observacao": "Aprovado para validar FEFO",
  "usuarioAprovadorId": 2,
  "itens": [
    {
      "itemId": 4,
      "quantidadeAprovada": 6
    }
  ]
}
```

O campo usado é `itemId`, não `produtoId`.

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

## Validação manual com PostgreSQL — EM ANDAMENTO

Os GETs básicos já haviam sido testados anteriormente. Nesta etapa, eles são usados apenas para conferir estado antes/depois das operações.

O objetivo atual é validar as regras críticas contra PostgreSQL real.

### Teste 1 — entrada não perecível — VALIDADO

Foi registrada uma entrada física via:

```http
POST /api/v1/movimentacoes/estoques/{estoqueId}/lotes?usuarioId={usuarioId}
```

Exemplo utilizado:

```json
{
  "numeroLote": "POSTMAN-FIFO-001",
  "quantidade": 10,
  "dataValidade": null,
  "origem": "COMPRA",
  "observacao": "Entrada FIFO via Postman"
}
```

Validado no Postman e diretamente no PostgreSQL:

```text
novo lote persistido
quantidadeInicial = 10
quantidadeDisponivel = 10
dataValidade = null
EstoqueCentral +10
MovimentacaoEstoque ENTRADA registrada
```

### Teste 2 — preparação FEFO — VALIDADO

Foram criados dois lotes para o mesmo produto perecível:

```text
POSTMAN-FEFO-A
quantidade = 4
validade = 20/08/2026

POSTMAN-FEFO-B
quantidade = 10
validade = 20/12/2026
```

O estoque agregado aumentou em 14 e os dois lotes foram persistidos corretamente.

### Teste 3 — pedido FEFO — APROVAÇÃO REALIZADA

Foi criado um pedido solicitando 6 unidades do produto perecível.

O pedido foi criado como `PENDENTE`, sem baixa na criação.

A aprovação inicialmente foi chamada acidentalmente com `POST`, resultando em erro. O método correto foi identificado como `PUT`.

Após corrigir para:

```http
PUT /api/v1/pedidos/3/aprovar
```

a aprovação funcionou.

### PONTO EXATO PARA RETOMAR AMANHÃ

Amanhã começar conferindo o resultado físico da aprovação FEFO.

Expectativa:

```text
POSTMAN-FEFO-A
4 → 0

POSTMAN-FEFO-B
10 → 8
```

Consultar no PostgreSQL:

```sql
SELECT
    id,
    numero_lote,
    quantidade_disponivel,
    data_validade
FROM lote
WHERE numero_lote IN ('POSTMAN-FEFO-A', 'POSTMAN-FEFO-B')
ORDER BY data_validade;
```

Depois conferir as movimentações do pedido aprovado:

```sql
SELECT
    id,
    tipo_movimentacao,
    quantidade_movimentada,
    lote_id,
    pedido_id
FROM movimentacao_estoque
WHERE pedido_id = 3
ORDER BY id;
```

Esperado:

```text
SAIDA 4 unidades → POSTMAN-FEFO-A
SAIDA 2 unidades → POSTMAN-FEFO-B
```

Também conferir `EstoqueCentral.quantidadeAtual` após a baixa.

Se tudo bater, marcar **FEFO em PostgreSQL como validado**.

## Próximos testes depois do FEFO

Ordem recomendada:

1. validar resultado físico do FEFO atual;
2. testar FIFO de produto não perecível com dois lotes;
3. testar lote vencido não atendendo pedido normal;
4. testar descarte de vencidos;
5. testar cancelamento de pedido aprovado restaurando os lotes exatos;
6. testar entrega sem segunda baixa de estoque;
7. validar criação de `HistoricoLaboratorio`;
8. testar consultas Projeto × Laboratório × período;
9. validar consistência final `EstoqueCentral` × soma dos lotes;
10. depois criar testes de integração/concorrência.

## Concorrência

A criação do pedido não reserva saldo.

A saída acontece somente na aprovação.

`PedidoService.aprovar()` busca o pedido com bloqueio antes de processá-lo.

Ainda deve ser criado um teste específico de concorrência para validar dois pedidos diferentes tentando consumir simultaneamente o mesmo estoque/lote.

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

## Documentação de JSON/Postman

Foi criado em 12/08/2026:

```text
docs/JSON_EXEMPLOS.md
```

O documento reúne exemplos de chamadas e bodies para:

```text
unidade
laboratório
usuário
estagiário
produto
projeto
estoque central
entrada de lote
atualização de lote
descarte
pedido
aprovação parcial/total
rejeição
entrega
cancelamento
movimentações
histórico
consultas principais
```

Ele deve ser mantido junto com:

```text
docs/ENDPOINTS_INTERNOS.md
docs/testes.md
```

## Autenticação

### Local simulada

Permanece planejada para depois da estabilização dos fluxos em PostgreSQL.

Ela substituirá os `usuarioId` temporários dos endpoints auditáveis por usuário obtido do contexto autenticado.

### Definitiva

A autenticação final deverá integrar com a API corporativa fornecida pela infraestrutura da empresa.

## Requisito futuro de reposição/compra

Requisito informado pelo cliente:

```text
Sai primeiro o produto com validade mais próxima.
Na compra existe prazo mínimo de validade por produto.
Compra só ocorre quando estoque estiver em nível crítico segundo histórico dos últimos 5 anos.
```

Interpretação atual:

```text
FEFO → já implementado
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

Após a primeira versão funcional, novas necessidades deverão ser incorporadas de forma incremental sem desestruturar a arquitetura base.

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

1. terminar roteiro manual crítico no PostgreSQL;
2. validar FEFO e FIFO reais;
3. validar descarte e cancelamento;
4. validar entrega/histórico;
5. validar filtros por projeto e período;
6. testes de integração PostgreSQL;
7. testes de concorrência;
8. autenticação local simulada;
9. remover `usuarioId` temporário;
10. auditoria de DEVOLUCAO com executor real;
11. OpenAPI/Swagger;
12. frontend;
13. deploy da primeira versão;
14. pós-protótipo.

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

### Próxima ação ao retomar

```text
Consultar POSTMAN-FEFO-A e POSTMAN-FEFO-B no PostgreSQL
→ confirmar 4→0 e 10→8
→ conferir duas movimentações SAIDA do pedido 3
→ confirmar EstoqueCentral -6
→ marcar FEFO PostgreSQL como VALIDADO
→ seguir para FIFO
```
