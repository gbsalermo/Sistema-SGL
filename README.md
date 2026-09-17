<a id="readme-top"></a>

<div align="center">
  <img src="docs/LOGO.png" alt="SGL Logo" width="400" height="auto">

# SGL — Sistema de Gestão de Laboratórios

**Backend corporativo para pedidos, estoque por lotes, rastreabilidade, resíduos, fiscalização, vínculos institucionais, cadastros e relatórios laboratoriais.**

`Java 17` · `Spring Boot 4.1` · `PostgreSQL` · `Flyway` · `Swagger/OpenAPI` · `PDF/XLSX`

</div>

---

## Estado atual — 17/09/2026

O primeiro protótipo do SGL foi funcionalmente aprovado. As Etapas 1, 2 e 3 da pré-produção foram concluídas e validadas. A **Etapa 4 — Expansão operacional de Resíduos** foi iniciada na branch `feat/etapa-4-residuos`.

Subetapa atual:

```text
4.1 Locais de armazenamento cadastráveis      🔧 em andamento
4.1-A Fundação do catálogo no backend          ⏭ próxima implementação
```

A modelagem da 4.1 já foi aprovada antes da implementação:

```text
LocalArmazenamentoResiduo
→ catálogo mutável por Unidade

Residuo.localArmazenamentoResiduo
→ referência opcional ao catálogo

Residuo.complementoLocalArmazenamento
→ complemento opcional da ocorrência

Residuo.localArmazenamentoTemporario
→ snapshot textual histórico completo
```

Estado consolidado:

```text
Pedidos / urgência                                ✅
Estoque / lotes                                   ✅
FIFO / FEFO                                       ✅
Embalagem / multiplicador / fracionamento         ✅
Movimentações / rastreabilidade                   ✅
Swagger / OpenAPI                                 ✅
Fiscalização de produtos                          ✅
Relatórios operacionais                           ✅
Exportação PDF/XLSX                               ✅
Resíduos — fluxo atual refinado                   ✅ Etapa 3
Classes de Resíduo + snapshots                    ✅
Segurança/EPI + snapshots                         ✅
Código SGL + QR técnico de Resíduo                ✅
Prévia antecipada do rótulo                       ✅
Impressão condicionada à liberação                ✅
Estagiários — vínculo/edição/encerramento         ✅ base atual
Pessoas por laboratório                           ✅
Administração / Cadastros                         ✅
Isolamento operacional por Unidade                ✅
Autenticação/autorização definitiva               ⏳ etapa formal posterior
Integração corporativa/SSO                        ⏳ etapa formal posterior
```

> Para retomar o projeto, começar por [`CONTINUIDADE.md`](CONTINUIDADE.md), [`docs/PLANO_PRE_PRODUCAO.md`](docs/PLANO_PRE_PRODUCAO.md), [`docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`](docs/CONTINUIDADE_ETAPA_4_2026-09-17.md), [`docs/MODULO_RESIDUOS.md`](docs/MODULO_RESIDUOS.md) e [`docs/DOSSIE_PROJETO_SGL.md`](docs/DOSSIE_PROJETO_SGL.md). Para contratos HTTP, o Swagger/OpenAPI em execução continua sendo a fonte viva.

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

Regras críticas permanecem no backend; o frontend não deve recriar FIFO/FEFO, transições de domínio, validações oficiais ou cálculos de relatórios.

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

Novos contratos públicos devem continuar usando UUID.

---

## Multitenancy por Unidade

Fluxo atual:

```text
frontend lê unidadeId da sessão DEV
→ envia X-SGL-Unidade-Id
→ TenantRequestFilter valida o UUID
→ TenantContext mantém a Unidade durante a requisição
→ services/repositories restringem os dados da Unidade
```

Esse mecanismo suporta o isolamento funcional em desenvolvimento, mas não substitui a segurança definitiva. A autenticação corporativa deverá derivar tenant/Unidade da identidade autenticada confiável.

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
- **Resíduo:** ocorrência operacional real gerada no laboratório;
- **ClasseResiduo:** catálogo atual/editável por Unidade;
- **LocalArmazenamentoResiduo:** catálogo de locais da Unidade planejado na Etapa 4.1;
- **Estagiário:** vínculo institucional com Unidade/Laboratório e período;
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

Uma composição de Resíduo pode referenciar Produto para rastreabilidade e sugestão de segurança, sem movimentar estoque automaticamente.

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

O código e o QR técnico existem desde a criação. No template físico atual do frontend, o QR técnico pode existir no contrato sem necessariamente ser renderizado; a definição final de template/Zebra permanece na Etapa 10.

Detalhes: [`docs/MODULO_RESIDUOS.md`](docs/MODULO_RESIDUOS.md).

---

## Banco e migrations

Ambiente de desenvolvimento padrão:

```text
PostgreSQL
Hibernate ddl-auto=validate
Flyway habilitado
```

Migrations aplicadas são imutáveis. No domínio de Resíduos:

```text
V11 — módulo base de Resíduos
V12 — backfill Código SGL
V13 — estado físico, tratamento e responsabilidade inicial
V14 — Classes de Resíduo
V15 — segurança/EPI
V16 — próxima migration planejada para locais de armazenamento
```

A V16 ainda não foi implementada neste checkpoint.

---

## Sequência de trabalho

### Pré-produção pós-aprovação

```text
Etapa 1 — padrão visual global                 ✅
Etapa 2 — Dark Mode definitivo                 ✅
Etapa 3 — refinamentos do fluxo de Resíduos    ✅
Etapa 4 — expansão operacional de Resíduos     🔧 atual
Etapa 5 — Projetos + Atividades                ⏳
Etapa 6 — Estagiários + vínculos               ⏳
Etapa 7 — relatórios consolidados              ⏳
Etapa 8 — unidades + Soluções                  ⏳
Etapa 9 — Pedidos + Soluções                   ⏳
Etapa 10 — rótulos + impressão operacional     ⏳
Etapa 11 — Manual + delete lógico              ⏳
Etapa 12 — testes automatizados frontend       ⏳
Etapa 13 — revisão estrutural/legibilidade     ⏳
```

Etapa 4:

```text
4.1 Locais de armazenamento cadastráveis       🔧 atual
4.2 Modelos de Resíduos                        ⏳
4.3 modelo x preenchimento manual              ⏳
4.4 correções administrativas do ciclo         ⏳
```

Não antecipar 4.2–4.4 durante a implementação da 4.1.

---

## Regra de trabalho do backend

Alterações funcionais de backend são implementadas manualmente pelo responsável do projeto.

Fluxo:

```text
IA analisa/modela/explica
→ fornece passos e código de referência
→ responsável implementa manualmente
→ IA revisa
→ validar
→ commit lógico
```

Frontend e documentação podem ser alterados diretamente quando autorizado.

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

## Documentação

| Documento | Uso |
|---|---|
| [`CONTINUIDADE.md`](CONTINUIDADE.md) | checkpoint atual e regra de retomada |
| [`docs/PLANO_PRE_PRODUCAO.md`](docs/PLANO_PRE_PRODUCAO.md) | roadmap canônico |
| [`docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`](docs/CONTINUIDADE_ETAPA_4_2026-09-17.md) | handoff e plano operacional da Etapa 4 |
| [`docs/DOSSIE_PROJETO_SGL.md`](docs/DOSSIE_PROJETO_SGL.md) | visão consolidada do sistema |
| [`docs/README.md`](docs/README.md) | índice e classificação documental |
| [`docs/MODULO_RESIDUOS.md`](docs/MODULO_RESIDUOS.md) | domínio de Resíduos |
| [`docs/FLUXO_DO_SISTEMA.md`](docs/FLUXO_DO_SISTEMA.md) | fluxo operacional consolidado |

Documentos históricos permanecem para rastreabilidade e não devem ser interpretados como checkpoint atual.

---

<div align="center">
  <strong>SGL — Sistema de Gestão de Laboratórios</strong><br/>
  Etapa 4 de pré-produção iniciada — 4.1 em andamento.
</div>
