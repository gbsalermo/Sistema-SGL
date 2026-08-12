# Continuidade do Projeto SGL

**Projeto:** Sistema de Gestão de Laboratórios  
**Última atualização:** 12/08/2026  
**Fase atual:** PostgreSQL + Flyway estabilizados com migration V1 aplicada com sucesso

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

A configuração está separada em:

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

## PostgreSQL — VALIDADO

Banco local atual:

```text
sgl
```

A conexão do backend com PostgreSQL está confirmada no profile `dev`.

Execução validada em 11/08/2026:

```text
jdbc:postgresql://localhost:5432/sgl
PostgreSQL 18.4
schema public
```

O pool Hikari abriu conexão normalmente e o backend iniciou com PostgreSQL real.

## Flyway — MIGRATION V1 CONCLUÍDA

A migration inicial foi construída manualmente, entidade por entidade, a partir do mapeamento JPA atual.

Arquivo definitivo:

```text
src/main/resources/db/migration/V1__create_initial_schema.sql
```

A V1 cria o schema inicial com as tabelas principais:

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

Também cria as FKs e constraints relevantes, incluindo:

```text
estoque_central único por unidade_id + produto_id
lote único por estoque_central_id + numero_lote
responsável de laboratório vinculado a usuário
herança JOINED entre usuarios e estagiarios
```

Em 11/08/2026, foi executado o teste completo em banco vazio.

Resultado confirmado:

```text
schema public vazio
→ Flyway criou flyway_schema_history
→ V1 validada
→ V1 aplicada com sucesso
→ schema passou a versão v1
```

O Hibernate foi mantido com:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Após a aplicação da V1, o Hibernate validou o schema sem acusar tabelas ou colunas ausentes.

A aplicação iniciou normalmente na porta 8080.

### Regra definitiva para migrations

A partir deste ponto:

```text
V1 não deve mais ser alterada.
```

Toda mudança estrutural futura no banco deverá gerar nova migration:

```text
V2__descricao_da_mudanca.sql
V3__descricao_da_mudanca.sql
...
```

Isso evita `checksum mismatch` e preserva o histórico real de evolução do schema.

## Ajuste identificado durante a V1 — Estagiario

Durante a primeira execução real em PostgreSQL, foi encontrado um conflito entre herança `JOINED` e o campo `ativo` duplicado em `Estagiario`.

Antes:

```text
Usuario
→ ativo

Estagiario
→ ativo duplicado
```

O Hibernate persistia `ativo` apenas na tabela `usuarios`, mas a migration exigia também `ativo NOT NULL` em `estagiarios`.

Decisão aplicada:

```text
ativo permanece somente em Usuario
Estagiario herda getAtivo()/setAtivo()
EstagiarioDTO mantém ativo porque o contrato da API ainda precisa expor esse estado
```

A coluna `ativo` foi removida da tabela `estagiarios` na V1 antes do fechamento definitivo da migration.

## DataInitializer — EXECUÇÃO VALIDADA EM POSTGRESQL

Após a correção da V1 e do modelo de `Estagiario`, o `DataInitializer` executou integralmente no PostgreSQL.

Foram inseridos com sucesso:

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

O log final confirmou:

```text
=== Dados de teste injetados com sucesso! ===
=== Estoques iniciais criados com lotes correspondentes ===
```

Próximo ajuste necessário:

```text
DataInitializer deve executar apenas no ambiente de desenvolvimento.
```

Em produção, dados artificiais não devem ser carregados automaticamente.

## Autenticação

### Local simulada

Permanece planejada para depois da estabilização inicial no PostgreSQL.

Ela deverá fornecer o usuário responsável através de contexto autenticado, eliminando os `usuarioId` temporários de endpoints de movimentação.

### Definitiva externa

Será fornecida por API corporativa e permanece obrigatória para implantação definitiva.

Essa integração permanece fora da sequência imediata de implementação porque depende da infraestrutura corporativa, mas continua indispensável para o projeto final.

## Ideias e inspirações para o frontend

Referências visuais e técnicas separadas para a futura etapa de frontend do SGL. O objetivo é usar esses materiais como inspiração de layout, navegação, dashboards, tabelas, cards, formulários e organização de telas administrativas, sem assumir cópia direta de identidade visual ou estrutura.

- TikTok — referência visual/ideia de interface: https://vt.tiktok.com/ZS43bGhrK/
- Salvia Kit: https://github.com/salvia-kit/salvia-kit
- Materio Vuetify Vue.js Admin Template Free: https://github.com/themeselection/materio-vuetify-vuejs-admin-template-free
- Vue Notus: https://github.com/creativetimofficial/vue-notus
- Sneat Vuetify Vue.js Admin Template Free: https://github.com/themeselection/sneat-vuetify-vuejs-admin-template-free

### Uso do Figma

O **Figma** será utilizado como ferramenta de apoio antes e durante a implementação do frontend.

A ideia é usar os modelos e referências salvos acima como ponto de partida visual, adaptando-os ao contexto real do SGL antes de transformar as telas em código.

### Etapas do frontend

O frontend seguirá este fluxo como processo base de design e implementação:

```text
Templates / referências
        ↓
      Figma
        ↓
Selecionar o que funciona
        ↓
Adaptar ao fluxo do SGL
        ↓
Criar componentes reutilizáveis
        ↓
Definir Design System
        ↓
Implementar no frontend
```

O objetivo é evitar que o frontend nasça diretamente de um template pronto. As referências servirão como matéria-prima; o Figma será a etapa de seleção, adaptação e validação visual; e somente depois os padrões aprovados serão transformados em componentes reais da aplicação.

O **Design System** deve começar pequeno e crescer junto com o sistema, evitando excesso de engenharia antes das primeiras telas funcionais.

Primeira base sugerida:

```text
Foundations
→ cores
→ tipografia
→ espaçamento
→ bordas
→ estados visuais

Componentes básicos
→ Button
→ Input / FormField
→ StatusBadge
→ DataTable
→ DashboardCard
→ Sidebar
→ Modal
```

Alguns estados importantes do domínio devem ser representados de forma padronizada no design:

```text
Pedido
→ PENDENTE
→ APROVADO
→ ENTREGUE
→ REJEITADO
→ CANCELADO

Estoque
→ NORMAL
→ CRÍTICO
→ ZERADO

Lote
→ VÁLIDO
→ PRÓXIMO DO VENCIMENTO
→ VENCIDO
```

Quando possível, haverá correspondência clara entre componente visual e componente implementado:

```text
FIGMA                         FRONTEND

Button                →       Button.vue
Status Badge          →       StatusBadge.vue
Data Table            →       DataTable.vue
Sidebar               →       Sidebar.vue
Dashboard Card        →       DashboardCard.vue
Modal                 →       Modal.vue
Input / FormField     →       FormField.vue
```

Fluxo prático por tela:

```text
selecionar referências úteis
→ adaptar no Figma
→ validar fluxo e hierarquia visual
→ identificar componentes reaproveitáveis
→ adicionar/ajustar componentes no Design System
→ implementar a tela
→ revisar comportamento responsivo
→ repetir para a próxima tela
```

O Figma deverá ajudar principalmente na adaptação de:

```text
sidebar e navegação
cards e indicadores
tabelas administrativas
formulários
fluxos de pedido e aprovação
telas de estoque e lotes
relatórios e dashboards
responsividade e hierarquia visual
```

A intenção não é copiar integralmente os templates de referência, mas aproveitar padrões de interface já maduros e reorganizá-los em um protótipo visual coerente com o domínio do SGL antes da implementação.

Essas referências deverão ser revisitadas quando a etapa de frontend começar oficialmente, principalmente para definir:

```text
layout geral do painel
sidebar e navegação
cards de indicadores
tabelas de estoque, lotes e pedidos
telas de cadastro e manutenção
relatórios e gráficos
design responsivo
```

## Pós-protótipo

Após a primeira versão funcional do SGL estar implantada e sendo utilizada em produção, o projeto entra em uma etapa contínua de **Pós-protótipo**.

O objetivo dessa fase é permitir que novas necessidades reais, sugestões dos usuários e melhorias identificadas durante o uso do sistema sejam incorporadas gradualmente **sem desestruturar a arquitetura base já validada**.

Princípios dessa etapa:

```text
arquitetura base permanece estável
→ novas ideias são avaliadas antes de alterar o domínio
→ mudanças estruturais usam novas migrations Flyway
→ regras já consolidadas não são reescritas sem necessidade
→ novas funcionalidades devem aproveitar services, entidades e fluxos existentes quando possível
→ alterações devem possuir testes antes de chegar à produção
```

Exemplos de itens que podem entrar no Pós-protótipo:

```text
novos relatórios e indicadores
média histórica de saída de produtos
cálculo de nível crítico baseado no histórico
apoio à decisão de reposição/compra
prazo mínimo de validade por produto no recebimento
novos filtros e consultas solicitados pelos usuários
dashboards adicionais
melhorias de UX no frontend
novas automações administrativas
integrações futuras com sistemas corporativos
```

Fluxo recomendado para novas ideias após a implantação:

```text
necessidade observada em produção
→ registrar requisito
→ avaliar impacto na arquitetura existente
→ definir se é regra de negócio, relatório, endpoint, interface ou alteração de banco
→ implementar de forma incremental
→ criar migration V2/V3/... quando houver mudança estrutural
→ adicionar testes
→ validar em ambiente de desenvolvimento/homologação
→ publicar nova versão
```

Essa etapa não possui uma lista fechada de funcionalidades. Ela funciona como uma área controlada de evolução do SGL após a entrega do protótipo, preservando as decisões arquiteturais centrais construídas durante o desenvolvimento inicial.

## Próximos passos

1. **Ajustar `DataInitializer` para executar somente no profile `dev`.**
2. **Reexecutar os 20 testes automatizados após o fechamento da V1.**
3. **Executar o roteiro `docs/testes.md` usando PostgreSQL.**
4. **Validar os endpoints principais manualmente via Postman com banco PostgreSQL.**
5. **Testar consistência `EstoqueCentral.quantidadeAtual` × soma dos lotes após entradas, saídas, descartes e cancelamentos.**
6. **Testar FEFO/FIFO em PostgreSQL real.**
7. **Testar consultas Projeto × Laboratório × período em PostgreSQL.**
8. **Criar testes de integração contra PostgreSQL quando o fluxo manual estiver estável.**
9. **Adicionar testes de concorrência para aprovação/saída de estoque.**
10. **Implementar autenticação local simulada.**
11. **Remover `usuarioId` temporário dos endpoints auditáveis.**
12. **Ativar `DEVOLUCAO` auditada com usuário executor real.**
13. **Adicionar OpenAPI/Swagger.**
14. **Iniciar frontend seguindo o fluxo Referências → Figma → componentes → Design System → implementação.**
15. **Implantar e validar a primeira versão funcional.**
16. **Entrar na etapa Pós-protótipo para evolução incremental sem alterar a arquitetura base de forma descontrolada.**

### Próxima etapa imediata recomendada

```text
DataInitializer por profile
→ mvn test
→ roteiro Postman no PostgreSQL
→ validar regras críticas de estoque
```

Somente depois dessa validação funcional completa o backend deve avançar para novas mudanças estruturais de banco.

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
| 11/08/2026 | Migration `V1__create_initial_schema.sql` concluída e aplicada com sucesso |
| 11/08/2026 | Hibernate `ddl-auto=validate` validou o schema criado pelo Flyway |
| 11/08/2026 | Duplicidade de `ativo` em `Estagiario` removida; estado permanece herdado de `Usuario` |
| 11/08/2026 | `DataInitializer` executou integralmente sobre PostgreSQL |
| 11/08/2026 | V1 congelada; futuras mudanças de schema deverão usar V2, V3 e seguintes |
| 11/08/2026 | Referências de frontend registradas para futura definição visual do SGL |
| 12/08/2026 | Criada etapa Pós-protótipo para evolução incremental do sistema em produção sem desestruturar a arquitetura base |
| 12/08/2026 | Figma definido como ferramenta de apoio para adaptar as referências visuais ao frontend do SGL antes da implementação |
| 12/08/2026 | Fluxo de frontend consolidado: Referências → Figma → adaptação ao SGL → componentes reutilizáveis → Design System → implementação |
