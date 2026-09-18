# Dossiê do Projeto SGL — Handoff

**Projeto:** SGL — Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Atualizado em:** 17/09/2026  
**Estado:** primeiro protótipo funcional aprovado; Etapas 1, 2 e 3 concluídas; Etapa 4 em andamento; 4.1 concluída; 4.2 atual.  
**Branch atual:** `feat/etapa-4-residuos`  
**Próxima implementação:** 4.2 — ModeloResiduo.

Este documento resume o estado real atual do SGL para retomada humana ou por IA.

## Checkpoint atual

```text
Etapa 1 — padrão visual global              ✅ concluída
Etapa 2 — Dark Mode definitivo              ✅ concluída
Etapa 3 — refinamentos de Resíduos          ✅ concluída e validada
Etapa 4 — expansão operacional de Resíduos  🔧 em andamento
  4.1 — locais de armazenamento             ✅ concluída
  4.2 — Modelos de Resíduo                  🔧 atual
```

Handoff imediato:

`docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`

Plano canônico:

`docs/PLANO_PRE_PRODUCAO.md`

---

# 1. Ordem de precedência

```text
1. código da main / branch atual validada
2. Swagger/OpenAPI
3. CONTINUIDADE.md
4. docs/PLANO_PRE_PRODUCAO.md
5. handoff da etapa atual
6. este DOSSIE_PROJETO_SGL.md
7. documento específico do módulo
8. documentos históricos
```

---

# 2. Regra de trabalho

```text
backend funcional
→ IA analisa/modela/explica
→ fornece passos e código de referência
→ usuário implementa manualmente
→ IA revisa
```

Não aplicar backend funcional diretamente sem autorização explícita.

Frontend/documentação podem ser alterados diretamente quando autorizado. Trabalhar em branch própria, passos pequenos e commits lógicos.

---

# 3. Stack

## Backend

```text
Java 17
Spring Boot 4.1.0
Spring Data JPA / Hibernate
PostgreSQL
Flyway
Spring Validation
Spring Security / OAuth2 Client como base
SpringDoc OpenAPI / Swagger
JUnit / H2
OpenPDF
Apache POI
Maven
```

## Frontend

```text
Vue 3.5
Vite 8
TypeScript 5.9
Vue Router
Pinia
Axios
Vuetify 3
Node >= 20.19
```

---

# 4. Estado executivo

## Backend

```text
Fundação Spring/PostgreSQL/Flyway                 ✅
UUID público + Long interno                       ✅
Pedidos / urgência                                ✅
Estoque / lotes                                   ✅
FIFO / FEFO                                       ✅
Embalagem / multiplicador / fracionamento         ✅
Movimentações / rastreabilidade                   ✅
Swagger/OpenAPI                                   ✅
Fiscalização                                      ✅
Relatórios + PDF/XLSX                             ✅
Resíduos — Etapa 3 refinada                       ✅
Classes de Resíduo + snapshots                    ✅
Segurança/EPI + snapshots                         ✅
Estagiários — base atual                          ✅
Pessoas por laboratório                           ✅
Administração/Cadastros                           ✅
Isolamento operacional por Unidade                ✅
Autenticação/autorização definitiva               ⏳
Integração corporativa                            ⏳
```

## Frontend

```text
Sessão DEV + expiração                            ✅
Pedidos Solicitante/Gestão                        ✅
Estoque / lotes                                   ✅
Movimentações                                     ✅
Relatórios                                        ✅
Resíduos Solicitante/Gestão                       ✅ Etapa 3 concluída
Classes de Resíduo                                ✅
Segurança/EPI                                     ✅
Prévia de rótulo                                  ✅
Impressão condicionada à liberação                ✅
Estagiários                                       ✅ base atual
Pessoas por laboratório                           ✅
Administração/Cadastros                           ✅
Dashboard Gestão                                  ✅
Dashboard Solicitante                             ✅
Alertas                                           ✅
Busca                                             ✅
Dark Mode                                         ✅
404                                               ✅
Contexto de Unidade enviado à API                 ✅
Autenticação/autorização definitiva               ⏳
```

---

# 5. Arquitetura backend

```text
Controller → contrato HTTP
Service → regras, transações e orquestração
Repository → persistência/consultas
Model → estado de domínio
RequestDTO / ResponseDTO → contratos públicos
```

Identificadores:

```text
Long id → interno
UUID publicId → público
```

---

# 6. PostgreSQL e Flyway

```text
PostgreSQL
Hibernate ddl-auto=validate
Flyway habilitado
```

Migrations aplicadas são imutáveis.

No domínio de Resíduos:

```text
V11 módulo base
V12 backfill Código SGL
V13 estado físico/tratamento/responsabilidade
V14 Classes de Resíduo
V15 segurança/EPI
V16 locais de armazenamento aplicada e imutável
```

A V16 ainda não foi implementada neste checkpoint.

---

# 7. Multitenancy por Unidade

```text
frontend
→ X-SGL-Unidade-Id
→ TenantRequestFilter
→ TenantContext
→ services/repositories por Unidade
```

Isso representa isolamento funcional em desenvolvimento, não autorização definitiva de produção.

---

# 8. Estoque e Pedidos

```text
Produto → catálogo
EstoqueCentral → saldo agregado por Unidade
Lote → saldo físico, validade, apresentação
MovimentacaoEstoque → rastreabilidade
```

FIFO/FEFO:

```text
perecível     → FEFO
não perecível → FIFO
```

Pedidos:

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

Criação não baixa estoque; aprovação baixa; entrega não baixa novamente; cancelamento aprovado restaura lotes utilizados.

---

# 9. Resíduos — estado atual

```text
Produto != Resíduo
```

Produto é catálogo/estoque. Resíduo é ocorrência real.

Fluxo:

```text
INFORMADO
→ EM_ANALISE
→ LIBERADO_PARA_ARMAZENAMENTO
→ ARMAZENADO_TEMPORARIAMENTE
→ DESPACHADO
```

Código:

```text
SGL-RES-AAAA-NNNNNN
```

Dados consolidados na Etapa 3:

- procedência/uso em `processoOrigem`;
- estado físico;
- tratamento realizado;
- usuário gerador;
- Gestor recebedor inicial;
- risco informado/confirmado;
- classes informadas/confirmadas;
- snapshots de classes;
- segurança informada/confirmada;
- recomendações de segurança no Produto;
- snapshots de segurança;
- composição;
- armazenamento/destino;
- histórico por ator.

Regra arquitetural:

```text
cadastro atual/editável
≠
dado histórico da ocorrência
```

Alterar Produto, Classe, Local ou futuramente ModeloResiduo não pode modificar retroativamente o que já foi registrado como snapshot da ocorrência.

Rótulo:

```text
INFORMADO / EM_ANALISE
→ prévia ✅
→ impressão ❌

LIBERADO_PARA_ARMAZENAMENTO ou posterior
→ impressão ✅
```

Código e QR técnico existem desde a criação. O template físico atual pode não renderizar o QR; padrão final/Zebra ficam na Etapa 10.

---

# 10. Etapa 4.1 — decisão fechada

```text
LocalArmazenamentoResiduo
= catálogo mutável por Unidade

Residuo.localArmazenamentoResiduo
= referência opcional ao catálogo

Residuo.complementoLocalArmazenamento
= complemento opcional da ocorrência

Residuo.localArmazenamentoTemporario
= snapshot textual histórico completo
```

Regras:

- catálogo por Unidade;
- ativação/inativação;
- local inativo não entra em novas seleções;
- histórico antigo permanece válido;
- alteração de nome do cadastro não altera snapshot antigo;
- catálogo + complemento e caminho manual coexistem;
- modos catálogo/manual são mutuamente exclusivos no payload;
- validação de tenant pelo contexto da Unidade do Resíduo;
- análise define local planejado;
- confirmação física pode manter ou corrigir;
- correção deve permanecer rastreável;
- rótulo/relatório continuam consumindo `localArmazenamentoTemporario`.

Plano:

```text
4.1-A V16 + entidade + repository ✅
4.1-B CRUD + tenant ✅
4.1-C análise/liberação ✅
4.1-D confirmação física/correção ✅
4.1-E revisão backend ✅
4.1-F frontend Cadastros ✅
4.1-G frontend Gestão ✅
4.1-H regressão e fechamento ✅
```

Próximo passo: **4.2 — ModeloResiduo**.

Criar somente:

```text
V16__create_residue_storage_locations.sql
LocalArmazenamentoResiduo.java
LocalArmazenamentoResiduoRepository.java
```

Não criar service/controller/DTO nem alterar `Residuo.java` nessa subetapa.

---

# 11. Etapas futuras

```text
4.2 Modelos de Resíduo
4.3 modelo x preenchimento manual
4.4 correções administrativas do ciclo
5 Projetos + Atividades
6 Estagiários + vínculos
7 Relatórios consolidados
8 unidades + Soluções
9 Pedidos + Soluções
10 Rótulos + impressão operacional
11 Manual + delete lógico
12 testes automatizados frontend
13 revisão estrutural/legibilidade
```

A 4.1 foi concluída. A sequência atual inicia pela 4.2, sem antecipar 4.3/4.4.

---

# 12. Regra final de retomada

**A Etapa 4 está em andamento. A 4.1 foi concluída e validada. O próximo passo é a 4.2 — ModeloResiduo, preservando tenant e snapshot histórico.**
