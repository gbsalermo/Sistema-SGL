<a id="readme-top"></a>

<div align="center">
  <img src="docs/LOGO.png" alt="SGL Logo" width="400" height="auto">

# SGL — Sistema de Gestão de Laboratórios

**Backend corporativo para pedidos, estoque por lotes, rastreabilidade, resíduos, fiscalização, vínculos institucionais, cadastros e relatórios laboratoriais.**

`Java 17` · `Spring Boot 4.1` · `PostgreSQL` · `Flyway` · `Swagger/OpenAPI` · `PDF/XLSX`

</div>

## O que é o SGL?

O **SGL (Sistema de Gestão de Laboratórios)** é uma plataforma de apoio à organização, execução e acompanhamento das atividades laboratoriais. Seu propósito é reunir em um mesmo ambiente a estrutura dos laboratórios, os projetos e suas atividades, o controle de materiais, as solicitações, os resíduos e as informações necessárias à gestão.

O sistema foi concebido para dar **rastreabilidade às operações**, reduzir controles dispersos e oferecer uma visão consistente dos recursos utilizados por diferentes laboratórios de uma instituição. Cada Unidade possui seu contexto operacional, enquanto Projetos, SCI, Atividades e vínculos permitem relacionar pessoas e operações às suas origens.

No backend, isso se traduz em serviços e regras de negócio para:

- **Materiais e estoque:** catálogo de produtos, entrada de lotes, recipientes, apresentações, fracionamento, critérios FEFO/FIFO e movimentações auditáveis.
- **Solicitações e soluções:** pedidos de materiais e de soluções, validação dos contextos operacionais, aprovação, preparo, entrega e tratamento das quantidades utilizadas.
- **Projetos e pessoas:** organização de Projetos, SCI e Atividades, vínculos de usuários, participações de Estagiários e histórico operacional.
- **Resíduos e segurança:** registro, classificação, acompanhamento, tratamento e destinação de resíduos, além de informações de riscos e fiscalização.
- **Gestão e relatórios:** consultas, indicadores, rastreabilidade e exportação de dados em PDF/XLSX.

A API concentra validações, transações e regras de consistência; a interface web utiliza esses contratos para permitir a operação cotidiana. O SGL está em **evolução de pré-produção**: a integração de identidade institucional e a autorização definitiva são previstas para fases próprias, não devendo ser confundidas com a sessão de desenvolvimento.

> **Acompanhamento da evolução:** o histórico de entregas, o estado de cada etapa e os próximos passos estão em [`CONTINUIDADE.md`](CONTINUIDADE.md) e [`docs/PLANO_PRE_PRODUCAO.md`](docs/PLANO_PRE_PRODUCAO.md). O histórico da Etapa 9 está documentado em [`docs/ETAPA_9_INICIO_RELATORIOS_CONSOLIDADOS_2026-10-26.md`](docs/ETAPA_9_INICIO_RELATORIOS_CONSOLIDADOS_2026-10-26.md).

---

## Objetivo

O SGL representa o ciclo operacional dos laboratórios de forma rastreável:

```text
estrutura institucional
→ catálogo de produtos
→ estoque central por Unidade
→ entrada de lotes
→ solicitação de material
→ aprovação
→ baixa física FIFO/FEFO
→ entrega
→ movimentações auditáveis
→ geração e destinação de resíduos
→ relatórios/fiscalização
→ administração de dados-base
```

As regras de estoque (FEFO/FIFO), as transições operacionais e os cálculos oficiais são executados pelo backend. O frontend apresenta e conduz as operações usando os contratos da API.

---

## Arquitetura

```text
HTTP / REST
     ↓
Controller
     ↓ RequestDTO / ResponseDTO
Service
     ↓
Repository
     ↓
PostgreSQL + Flyway
```

Responsabilidades:

```text
Controller  → contrato HTTP e validação de entrada
Service     → regra de negócio, transação e orquestração
Repository  → persistência e consultas
Model       → estado do domínio
DTO         → contrato público da API
```

### Identificadores

```text
Long id
→ interno: banco, JPA, FKs e locks

UUID publicId
→ público: DTOs, endpoints e frontend
```

Os contratos públicos da API utilizam UUID como identificador das entidades; os IDs numéricos internos permanecem na persistência.

---

## Multitenancy por Unidade

O SGL organiza os registros operacionais por Unidade, com delimitação aplicada nas consultas e nos serviços.

Fluxo atual:

```text
frontend lê unidadeId da sessão DEV
→ envia X-SGL-Unidade-Id
→ TenantRequestFilter valida o UUID
→ TenantContext mantém a Unidade durante a requisição
→ services/repositories restringem os dados da Unidade
```

No ambiente de desenvolvimento, o contexto da Unidade é informado pelo cabeçalho `X-SGL-Unidade-Id`, associado à sessão DEV. Esse fluxo viabiliza a validação funcional por Unidade; a identidade e a autorização corporativas não fazem parte dessa sessão.

**Na integração institucional**, a Unidade (tenant) será resolvida a partir da identidade autenticada na sessão/token institucional e aplicada pelo backend.

---

## Domínio principal

```text
Unidade
├── Laboratórios
│   ├── Usuários
│   ├── Estagiários
│   ├── Projetos
│   ├── Pedidos
│   └── Resíduos
└── Estoque Central
    └── Produto
        └── Lotes
            └── Movimentações
```

Conceitos centrais:

- **Produto:** catálogo e unidade-base de controle;
- **EstoqueCentral:** saldo consolidado por produto/Unidade;
- **Lote:** quantidade física, validade, embalagem, multiplicador e rastreabilidade;
- **MovimentacaoEstoque:** trilha das operações físicas;
- **Pedido:** solicitação e ciclo de aprovação/entrega;
- **Resíduo:** material gerado no laboratório e encaminhado à Gestão;
- **Estagiário:** identidade institucional ligada a vínculos históricos; contexto operacional vem das participações em Atividades, que derivam Projeto/Laboratório dentro da Unidade;
- **Fiscalização:** classificação explícita de produtos controlados.

---

## Estoque, FIFO e FEFO

```text
produto perecível     → FEFO
produto não perecível → FIFO
```

Invariantes:

```text
EstoqueCentral.quantidadeAtual acompanha os lotes
aprovação baixa estoque
entrega não baixa novamente
cancelamento aprovado restaura os lotes exatos
lote vencido não é elegível para aprovação
movimentação identifica o lote efetivamente afetado
```

Formas de retirada atuais:

```text
UNITARIO
KIT
CAIXA
GARRAFA
GALAO
```

Fracionamento:

```text
false → true  permitido
true  → false não permitido
```

---

## Pedidos

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

A aprovação realiza a baixa física. A entrega apenas conclui o fluxo. O cancelamento de pedido aprovado restaura as quantidades dos lotes utilizados. Urgência não altera FIFO/FEFO.

---

## Resíduos

Decisão de domínio:

```text
Produto != Resíduo
```

Uma composição de Resíduo pode referenciar Produto para rastreabilidade, sem movimentar estoque automaticamente.

Fluxo:

```text
INFORMADO
→ EM_ANALISE
→ LIBERADO_PARA_ARMAZENAMENTO
→ ARMAZENADO_TEMPORARIAMENTE
→ DESPACHADO
```

Código SGL:

```text
SGL-RES-AAAA-NNNNNN
```

Detalhes: [`docs/MODULO_RESIDUOS.md`](docs/MODULO_RESIDUOS.md).

---

## Relatórios

Relatórios implementados e validados funcionalmente na branch da Etapa 9:

```text
Estagiários
Produtos
Projetos — consolidação de SCI, Atividades e participações
Movimentações — Resumo / Detalhamento
Estoque e lotes
Fiscalização
Resíduos
Pessoas por laboratório
```

A Central apresenta **Movimentações** com modos Resumo (contagens de ocorrências, sem rankings) e Detalhamento (filtros por tipo, origem, contexto Projeto/SCI/Atividade, produto, apresentação/unidade e período). As quantidades físicas têm unidades identificadas e exibição opcional no Detalhamento e na exportação PDF/XLSX. **Estoque e lotes** informam apresentação, conteúdo e recipientes fechados, abertos ou esgotados. **Fiscalização** apresenta a trilha de movimentações com recipientes vinculados.

A classificação histórica de consumo como apresentação completa ou por medida é derivada das parcelas registradas, sem afirmar a intenção original da solicitação. Os endpoints anteriores de Movimentações e Resumo Operacional permanecem disponíveis.

Os relatórios utilizam consultas e filtros consistentes entre a prévia e as exportações PDF/XLSX.

Pedidos entregues continuam sendo um recorte de Movimentações, não um relatório separado.

---

## Identidade e controle de acesso

```text
Spring Security como base técnica                   ✅
sessão DEV no frontend                              ✅ temporária
isolamento operacional por Unidade                  ✅ desenvolvimento
guardas de rota por perfil                          ✅ UX
autenticação definitiva                             ⏳
autorização global no servidor                      ⏳
auditoria derivada da identidade autenticada        ⏳
integração corporativa/SSO                          ⏳
```

O ambiente DEV utiliza sessão e contexto de Unidade de desenvolvimento para validar fluxos e perfis. A implantação institucional incorporará autenticação, autorização no servidor e auditoria vinculada à identidade autenticada. Esses recursos são uma etapa distinta da homologação funcional do MVP.

---

## Banco e migrations

Ambiente de desenvolvimento padrão:

```text
PostgreSQL
Hibernate ddl-auto=validate
Flyway habilitado
```

O Flyway versiona e aplica as alterações de esquema do banco. Cada evolução é registrada em uma nova migration, preservando o histórico das versões já aplicadas.

Migrations existentes nesta revisão: **V1 a V44**, em `src/main/resources/db/migration/`. A aplicação utiliza validação do esquema via Hibernate, enquanto o Flyway mantém o histórico de alterações.

---

## Execução local

```bash
cd backend/sgl-backend
mvn spring-boot:run
```

Variáveis usuais:

```text
DB_URL=jdbc:postgresql://localhost:5432/sgl
DB_USER=postgres
DB_PASSWD=<senha>
```

Acessos locais:

```text
API        http://localhost:8080
Swagger    http://localhost:8080/swagger-ui/index.html
OpenAPI    http://localhost:8080/v3/api-docs
```

Testes:

```bash
mvn test
```

---

## Evolução do projeto

O desenvolvimento de pré-produção evolui de forma incremental, preservando os fluxos homologados. A Etapa 9 está validada funcionalmente na branch colaborativa, mas ainda não integrada à `main`:

| Etapa | Escopo | Situação |
|---|---|---|
| 1–4 | Padrão visual, experiência inicial, Resíduos e expansão operacional | Concluídas |
| 5 | Projetos, SCI e Atividades | Concluída |
| 6 | Estagiários, vínculos, participações e formação | Concluída |
| 7 | Estoque físico, apresentações, fracionamento, FEFO/FIFO e Soluções | Concluída |
| 8 | Contextos operacionais, Pedidos, Soluções, Resíduos e vínculos adicionais | Concluída e integrada |
| 9 | Relatórios consolidados: Projetos, Movimentações (Resumo/Detalhamento), Estoque/Lotes e Fiscalização | Implementada e validada funcionalmente; aguardando merge |
| 10 | Rótulos e impressão operacional | Planejada |
| 11 | Manual do usuário e revisão de inativação/delete lógico | Planejada |
| 12 | Testes transversais de arquitetura, carga, E2E e regressão | Planejada |
| 13 | Revisão estrutural e auditoria final integral | Planejada |

**Preparação para produção:** após a estabilização funcional, o plano contempla matriz de permissões, congelamento, homologação integrada, correções, identidade e autorização definitivas, integração institucional e liberação controlada.

O detalhamento das decisões, entregas e critérios de validação fica em [`CONTINUIDADE.md`](CONTINUIDADE.md) e no [plano de pré-produção](docs/PLANO_PRE_PRODUCAO.md). Este README apresenta a arquitetura e as capacidades do sistema; não substitui os checkpoints técnicos.

---

## Documentação

| Documento | Uso |
|---|---|
| [`CONTINUIDADE.md`](CONTINUIDADE.md) | checkpoint atual e regra de retomada |
| [`docs/DOSSIE_PROJETO_SGL.md`](docs/DOSSIE_PROJETO_SGL.md) | visão consolidada do sistema |
| [`docs/README.md`](docs/README.md) | índice e classificação documental |
| [`docs/MODULO_RESIDUOS.md`](docs/MODULO_RESIDUOS.md) | domínio de resíduos |
| [`docs/RELATORIOS.md`](docs/RELATORIOS.md) | relatórios atuais |
| [`docs/EXPORTACAO_RELATORIOS.md`](docs/EXPORTACAO_RELATORIOS.md) | exportações PDF/XLSX |
| [`docs/PENDENCIAS_POS_PROTOTIPO.md`](docs/PENDENCIAS_POS_PROTOTIPO.md) | pendências/refactors posteriores |

> Exemplos e documentos históricos são auxiliares. Em caso de conflito, prevalecem `main`, Swagger/OpenAPI e os documentos atuais indicados acima.
