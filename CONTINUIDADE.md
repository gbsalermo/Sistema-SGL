# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 10/08/2026  
**Fase atual:** PostgreSQL conectado; Flyway inicializado; próxima etapa é criar a migration inicial

Este arquivo registra o estado atual do backend, decisões consolidadas e a ordem recomendada de continuidade.

## Estado atual

### Lotes e estoque

Concluído:

- entidade `Lote`;
- `LoteDTO`, `EntradaLoteDTO` e `AtualizarLoteDTO`;
- `LoteRepository`, `LoteService` e `LoteController`;
- entrada física por lote;
- FEFO para produtos perecíveis;
- FIFO para produtos não perecíveis;
- descarte de lotes vencidos;
- rastreabilidade por `MovimentacaoEstoque.lote`;
- restauração exata de lotes no cancelamento de pedido aprovado;
- remoção das operações físicas de `EstoqueCentralService`.

Regra de consistência:

```text
EstoqueCentral.quantidadeAtual
=
soma de Lote.quantidadeDisponivel
```

### Produto

`Produto` é catálogo e informa se o item é perecível.

A validade operacional pertence ao lote.

```text
Produto perecível
→ lote exige dataValidade
→ saída FEFO

Produto não perecível
→ lote sem dataValidade
→ saída FIFO
```

### MovimentacaoEstoque

`MovimentacaoEstoque` permanece como entidade de auditoria.

Cada lote afetado por uma operação gera sua própria movimentação, permitindo rastrear exatamente:

```text
produto
lote
quantidade
pedido, quando aplicável
laboratório, quando aplicável
usuário responsável
saldo anterior
saldo posterior
```

`MovimentacaoEstoqueService` centraliza entrada, saída, descarte e devolução/restauração física.

## Pedido

O fluxo atual é:

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Na aprovação:

```text
PedidoService
→ valida pedido e quantidades
→ localiza EstoqueCentral
→ delega ao MovimentacaoEstoqueService
→ FEFO/FIFO seleciona lotes
→ reduz lotes
→ reduz saldo agregado
→ registra SAIDA por lote
→ pedido fica APROVADO
```

Na entrega:

```text
pedido APROVADO
→ cria HistoricoLaboratorio
→ não baixa estoque novamente
→ pedido fica ENTREGUE
```

No cancelamento de pedido aprovado:

```text
consulta SAIDAS do pedido
→ identifica lotes usados
→ restaura exatamente esses lotes
→ restaura EstoqueCentral
→ pedido fica CANCELADO
```

O registro auditado `DEVOLUCAO` será completado quando o contexto autenticado local fornecer o usuário executor do cancelamento.

## Consultas por projeto e laboratório

Foi adicionada uma separação explícita entre **pedidos realizados** e **materiais efetivamente recebidos**.

### Pedidos realizados pelo projeto

```http
GET /api/v1/pedidos/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD
```

Essa consulta usa `Pedido.dataSolicitacao` e pode retornar pedidos em qualquer status.

### Materiais efetivamente recebidos pelo projeto

```http
GET /api/v1/historico-laboratorio/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD
```

Essa consulta usa `HistoricoLaboratorio.dataRecebimento` e representa somente materiais entregues.

As duas consultas validam:

```text
laboratório existe
projeto existe
projeto pertence ao laboratório informado
dataInicio <= dataFim
```

## Testes automatizados — VALIDADO

Em 10/08/2026, a suíte completa foi executada localmente após o ajuste do fixture de `MovimentacaoEstoqueServiceTest`.

Resultado confirmado:

```text
Tests run: 20
Failures: 0
Errors: 0
Skipped: 0
```

Os testes de service usam Mockito. O teste de contexto `SglApplicationTests` usa:

```java
@ActiveProfiles("test")
```

Assim, `mvn test` utiliza `application-test.properties` e continua usando H2 em memória, sem depender do PostgreSQL local.

## Configuração por ambiente

A configuração foi separada em:

```text
application.properties
→ configurações gerais

application-dev.properties
→ PostgreSQL + Flyway

application-test.properties
→ H2 para testes
```

### Profile `dev` como padrão local

Durante o desenvolvimento local, foi adotado temporariamente em `application.properties`:

```properties
spring.profiles.active=dev
```

Motivo: facilitar a execução pelo Eclipse sem precisar informar manualmente o profile a cada inicialização.

**Importante:** essa configuração é uma conveniência local de desenvolvimento. Quando houver ambiente de produção, o profile não deverá ficar fixo no arquivo principal. O ambiente de execução deverá informar explicitamente o profile apropriado (`dev`, `prod`, etc.).

As credenciais do PostgreSQL permanecem externas ao repositório, usando variáveis de ambiente:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/sgl}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWD:}
```

## PostgreSQL — CONEXÃO VALIDADA

Foi criado o banco local:

```text
sgl
```

A conexão do backend com PostgreSQL foi confirmada em 10/08/2026 com o profile `dev`.

O log confirmou:

```text
jdbc:postgresql://localhost:5432/sgl
PostgreSQL 18.4
schema public
```

O pool Hikari abriu conexão normalmente e o Flyway conseguiu acessar o mesmo banco.

## Flyway — INICIALIZAÇÃO VALIDADA

Flyway está integrado ao projeto e foi inicializado com sucesso.

Na primeira execução, como ainda não existiam migrations, ele:

```text
conectou no PostgreSQL
→ encontrou 0 migrations
→ criou flyway_schema_history
→ identificou o schema public como vazio
```

Depois disso, o Hibernate executou:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Como esperado, a aplicação interrompeu a inicialização por ausência das tabelas do domínio, começando por:

```text
Schema validation: missing table [estagiarios]
```

Esse erro é esperado neste ponto e confirma a separação de responsabilidades:

```text
Flyway
→ cria/evolui o schema

Hibernate
→ valida se o schema corresponde às entidades
```

O Hibernate não deve voltar a usar `update` no ambiente PostgreSQL.

## Próxima etapa — Migration V1

Criar:

```text
src/main/resources/db/migration/V1__create_initial_schema.sql
```

A migration será construída a partir das entidades atuais, em ordem compatível com as chaves estrangeiras.

Estratégia de aprendizado adotada:

```text
entidade Java
→ analisar @Entity/@Table/@Column/relacionamentos
→ traduzir para PostgreSQL
→ criar tabela na V1
→ avançar para a próxima entidade
```

A construção será feita entidade por entidade, começando pelas tabelas com menos dependências, como `unidades`, antes das tabelas com chaves estrangeiras.

A migration inicial deve refletir obrigatoriamente o modelo estabilizado:

- `Produto` sem `data_validade`;
- tabela `lote` com validade por lote;
- `movimentacao_estoque.lote_id` opcional;
- `estoque_central` único por `unidade_id + produto_id`;
- `lote` único por `estoque_central_id + numero_lote`;
- relacionamentos atuais de pedido, projeto, laboratório, usuário e histórico.

## DataInitializer

O `DataInitializer` ainda precisa ser ajustado antes da conclusão da migração.

Objetivo:

```text
desenvolvimento local
→ pode carregar dados de teste

produção
→ não deve executar dados artificiais automaticamente
```

A restrição por profile será feita após a migration inicial estar funcional.

## Autenticação

### Local simulada

Permanece planejada para depois da estabilização inicial no PostgreSQL.

Ela deverá fornecer o usuário responsável através de contexto autenticado, eliminando os `usuarioId` temporários de endpoints de movimentação.

### Definitiva externa

Será fornecida por API corporativa e permanece obrigatória para implantação definitiva.

## Próxima ordem de trabalho

1. **Criar `db/migration/V1__create_initial_schema.sql`.**
2. **Construir a migration entidade por entidade.**
3. Subir o backend com PostgreSQL e confirmar aplicação da V1.
4. Corrigir eventuais diferenças identificadas por `ddl-auto=validate`.
5. Ajustar `DataInitializer` para execução apenas em desenvolvimento.
6. Reexecutar os 20 testes.
7. Executar `docs/testes.md` com PostgreSQL.
8. Testar consistência EstoqueCentral × Lotes no PostgreSQL.
9. Testar consultas Projeto × Laboratório × período no PostgreSQL.
10. Executar testes de integração e concorrência.
11. Implementar autenticação local simulada.
12. Remover `usuarioId` temporário dos endpoints auditáveis.
13. Ativar `DEVOLUCAO` auditada com usuário executor real.
14. Adicionar OpenAPI/Swagger.
15. Iniciar frontend.

## Documentos de referência

- [`README.md`](README.md)
- [`docs/ENDPOINTS_INTERNOS.md`](docs/ENDPOINTS_INTERNOS.md)
- [`docs/testes.md`](docs/testes.md)
- [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md)
- [`docs/GUIA_ESTRUTURAL.md`](docs/GUIA_ESTRUTURAL.md)
- [`docs/CODIGOS_REFERENCIA_LOTE.md`](docs/CODIGOS_REFERENCIA_LOTE.md)
- [`docs/diagrama-uml-completo.puml`](docs/diagrama-uml-completo.puml)

## Histórico recente

| Data | Decisão |
|---|---|
| 07/08/2026 | `Lote` consolidado como composição rastreável do estoque |
| 07/08/2026 | FEFO definido para perecíveis e FIFO para não perecíveis |
| 07/08/2026 | Validade operacional transferida definitivamente para `Lote` |
| 07/08/2026 | `MovimentacaoEstoqueService` passou a centralizar operações físicas |
| 07/08/2026 | Aprovação de pedido passou a consumir lotes por FEFO/FIFO |
| 07/08/2026 | Cancelamento aprovado passou a restaurar exatamente os lotes consumidos |
| 07/08/2026 | Pedidos e histórico passaram a possuir filtros por Projeto + Laboratório + período |
| 10/08/2026 | Suíte completa validada: 20 testes, 0 falhas e 0 erros |
| 10/08/2026 | Dependências PostgreSQL/Flyway e profiles `dev`/`test` configurados |
| 10/08/2026 | Conexão PostgreSQL `sgl` confirmada pelo backend |
| 10/08/2026 | Flyway criou `flyway_schema_history` e confirmou schema vazio |
| 10/08/2026 | `dev` definido temporariamente como profile padrão local para facilitar execução pelo Eclipse |
| 10/08/2026 | Próxima etapa definida: construir `V1__create_initial_schema.sql` entidade por entidade |
