# Dossiê do Projeto SGL — Handoff

**Projeto:** SGL — Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Atualizado em:** 25/09/2026  
**Estado:** primeiro protótipo funcional aprovado; Etapas 1, 2, 3 e 4 concluídas e validadas; Etapa 5 — Projetos e Atividades em andamento no bloco 5.3 — Atividades, com 5.1 — Projeto base e 5.2 — SCI concluídos e validados.  
**Objetivo:** permitir que outra pessoa ou IA retome o projeto pelo estado real atual sem reconstruir o histórico.

## Checkpoint atual

```text
Etapa 1 — padrão visual global              ✅ concluída
Etapa 2 — Dark Mode definitivo              ✅ concluída
Etapa 3 — refinamentos de Resíduos          ✅ concluída e validada
Etapa 4 — expansão operacional de Resíduos  ✅ concluída e validada
Etapa 5.1 — Projeto base                    ✅ concluído e validado
Etapa 5.2 — SCI                             ✅ concluído e validado
Etapa 5.3 — Atividades                      🔧 atual
```

Handoff imediato:

`docs/CONTINUIDADE_ETAPA_5_2026-09-24.md`

Plano canônico:

`docs/PLANO_PRE_PRODUCAO.md`

---

# 0. Infraestrutura Git e sincronização — obrigatório

A arquitetura de colaboração foi reorganizada em 22/09/2026 para impedir que o GitHub sobrescreva trabalho do supervisor no GitLab.

Documento detalhado:

`docs/SINCRONIZACAO_GITLAB_GITHUB.md`

Resumo:

```text
GitLab/main = fonte canônica

GitLab/main
    ↓ GitHub Actions a cada 15 min / manual
GitHub/main

GitHub/collab/*
    ↓ GitHub Actions em cada push
GitLab/collab/*
    ↓ Merge Request
GitLab/main
```

Regras obrigatórias:

- o supervisor pode avançar a `main` diretamente pelo fluxo institucional no GitLab;
- `GitHub/main` é espelho, não origem de trabalho;
- usuário e IA colaboram em `GitHub/collab/*`;
- a mesma `collab/*` é replicada automaticamente para o GitLab;
- integração final acontece por MR no GitLab;
- nenhum workflow usa `--force`;
- divergência faz o workflow falhar em vez de sobrescrever histórico;
- não editar a mesma branch independentemente nos dois remotes;
- os remotes locais esperados se chamam `github` e `gitlab`; a antiga configuração de `origin` com múltiplos push URLs foi removida.

Credencial dos workflows:

```text
GitHub Actions Secret: GITLAB_PUSH_TOKEN
```

O valor nunca deve ser documentado. A solução usa Git-over-HTTPS; SSH foi descartado após timeout da porta 22.

Workflows backend:

```text
.github/workflows/check-gitlab-connectivity.yml
→ nome exibido: Sync collab branches to GitLab
→ GitHub/collab/* → GitLab/collab/*

.github/workflows/sync-gitlab-main-to-github.yml
→ GitLab/main → GitHub/main
```

O primeiro arquivo manteve nome legado; não confundir o nome do arquivo com sua função atual.

O fluxo foi validado nos dois sentidos e os `main` chegaram a `0 0` pelo comando:

```bash
git rev-list --left-right --count gitlab/main...github/main
```

---

# 1. Ordem de precedência

Quando houver conflito:

```text
1. código da main
2. Swagger/OpenAPI
3. CONTINUIDADE.md do repositório em trabalho
4. docs/SINCRONIZACAO_GITLAB_GITHUB.md para Git/remotes
5. docs/PLANO_PRE_PRODUCAO.md
6. handoff da etapa atual
7. este DOSSIE_PROJETO_SGL.md
8. documento específico do módulo
9. documentos históricos
```

A Etapa 4 foi integrada e a Etapa 5 nasceu da `main` atualizada. A branch operacional atual é `collab/etapa-5-projetos-atividades`; a antiga `feat/etapa-4-residuos` permanece apenas como referência histórica.

---

# 2. Regra de trabalho

O responsável do projeto usa o SGL também como processo de aprendizado.

Portanto:

```text
backend funcional
→ IA analisa/modela/explica
→ fornece passos e código de referência
→ usuário implementa manualmente
→ IA revisa
```

Não aplicar backend funcional diretamente sem autorização explícita.

Frontend/documentação podem ser alterados diretamente quando autorizado.

Trabalhar sempre em branch própria e em etapas pequenas.

A Etapa 4 está fechada. A branch operacional atual é `collab/etapa-5-projetos-atividades`, criada sobre a `main` já contendo as Etapas 1–4. Branches antigas permanecem apenas como referência histórica.

---

# 3. Objetivo do SGL

O SGL cobre:

- pedidos de materiais;
- estoque central por Unidade;
- lotes e validade;
- FIFO/FEFO;
- movimentações;
- fiscalização;
- resíduos laboratoriais;
- estagiários e vínculos;
- administração de dados-base;
- relatórios e exportações;
- dashboards, alertas e busca;
- futura autenticação/autorização/auditoria corporativa.

Repos:

```text
gbsalermo/Sistema-SGL
→ Spring API, domínio, persistência, Flyway, Swagger, relatórios/exportações

gbsalermo/SGL-FRONTEND
→ Vue SPA, UX de Solicitante, Gestão e Administração
```

---

# 4. Stack

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

# 5. Estado executivo

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

# 6. Arquitetura backend

```text
Controller
→ contrato HTTP

Service
→ regras, transações e orquestração

Repository
→ persistência/consultas

Model
→ estado de domínio

RequestDTO / ResponseDTO
→ contratos públicos
```

Identificadores:

```text
Long id
→ interno

UUID publicId
→ público
```

---

# 7. PostgreSQL e Flyway

```text
PostgreSQL
Hibernate ddl-auto=validate
Flyway habilitado
```

Migrations aplicadas são imutáveis.

Migrations relevantes:

```text
V11 módulo base de Resíduos
V12 backfill Código SGL
V13 estado físico/tratamento/responsabilidade
V14 Classes de Resíduo
V15 segurança/EPI
V16 snapshot de Unidade do Resíduo
V17 locais de armazenamento de Resíduos
V18 modelos reutilizáveis de Resíduos
V19 expansão do domínio de Projeto
V20 criação do domínio de SCI
```

Próxima alteração de schema: V21+.

---

# 8. Multitenancy por Unidade

Backend:

```text
X-SGL-Unidade-Id
→ TenantRequestFilter
→ TenantContext
→ services/repositories
```

Frontend:

```text
sessão DEV com unidadeId
→ interceptor Axios
→ header X-SGL-Unidade-Id
```

Isso representa isolamento funcional em desenvolvimento, não autorização definitiva de produção.

---

# 9. Estoque e Pedidos

Domínio:

```text
Produto
→ catálogo

EstoqueCentral
→ saldo agregado por Unidade

Lote
→ saldo físico, validade, apresentação

MovimentacaoEstoque
→ rastreabilidade
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

# 10. Resíduos — estado atual

Decisão:

```text
Produto != Resíduo
```

Produto é catálogo/estoque. Resíduo é ocorrência real.

Componente pode referenciar Produto para rastreabilidade e sugestão de segurança sem movimentar estoque.

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

## Dados consolidados na Etapa 3

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

Classes são catálogo editável por Unidade, não enum rígido.

Segurança estruturada:

```text
LUVAS
OCULOS_PROTECAO
PROTECAO_RESPIRATORIA
JALECO_AVENTAL
OUTRO
```

## Snapshot

Regra arquitetural importante:

```text
cadastro atual/editável
≠
dado histórico da ocorrência
```

Alterar Produto, Classe ou futuramente ModeloResiduo não modifica Resíduos antigos.

## Rótulo

```text
INFORMADO / EM_ANALISE
→ prévia ✅
→ impressão ❌

LIBERADO_PARA_ARMAZENAMENTO ou posterior
→ impressão ✅
```

Código e QR técnico existem desde a criação.

Template definitivo/Zebra ficam na Etapa 10.

## Gestão

A tela compara:

```text
Informado pelo laboratório
vs
Aprovado pela Gestão
```

O Gestor que liberou é identificado no histórico por `RISCO_CONFERIDO_E_RESIDUO_LIBERADO`.

Armazenamento e despacho podem ser feitos por outro Gestor, preservando rastreabilidade.

Detalhes: `docs/MODULO_RESIDUOS.md`.

---

# 11. Etapa 3 — fechada

Validada em 17/09/2026.

```text
3.1 redundância visual                       ✅
3.2 dados/classes/segurança/responsabilidade ✅
3.3 prévia x impressão                       ✅
```

A validação incluiu fluxo ponta a ponta, Gestores diferentes, histórico, classes, EPI, comparação informado/aprovado, prévia, bloqueio/liberação da impressão e escala visual da tela.

---

# 12. Etapa 4 — concluída

Handoff:

`docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`

Ordem:

```text
4.1 locais de armazenamento
→ 4.2 modelos de Resíduo
→ 4.3 uso modelo x manual
→ 4.4 correções administrativas do ciclo
```

## 4.1

Catálogo reutilizável por Unidade + complemento livre + opção manual.

Fechar antes de implementar como preservar histórico se o local mudar.

## 4.2

```text
ModeloResiduo = padrão reutilizável
Residuo = ocorrência real
```

Modelo não movimenta estoque e não deve ser referência viva para histórico.

## 4.3

Solicitante escolhe modelo ou preenchimento manual.

## 4.4

Avaliar:

```text
ADMINISTRADOR
→ cancelar com motivo
→ retornar para análise/liberação com motivo
→ preservar histórico
```

Definir status permitidos e efeitos sobre rótulo/impressão antes de codar.

Não confundir com delete lógico, que continua na Etapa 11.

---

# 13. Projetos/SCI/Atividades atuais; Estagiários e Relatórios seguintes

Etapa 5 estabiliza a hierarquia:

```text
Laboratório responsável/contextual
└── Projeto
    └── SCI
        └── Atividade
```

**Projeto é o eixo operacional principal**. Laboratório continua útil como contexto institucional, filtro e vínculo responsável, sem obrigar navegação por Laboratório para acessar Projeto.

Código SEG:

```text
Projeto   XX.XX.XX.XXX.XX.00
SCI       XX.XX.XX.XXX.XX.SS
Atividade XX.XX.XX.XXX.XX.SS.AAA
```

A numeração é cadastrada nesta primeira versão; o SGL valida a hierarquia.


Etapa 5 estabiliza Projetos/Atividades e Código SEG.

Etapa 6 evolui Estagiários com:

- Orientador obrigatório;
- Projeto/Atividade;
- Bolsa separada de Curso/Formação;
- Cultura/área temática;
- treinamento de segurança;
- histórico de prorrogações.

Etapa 7 consome Etapas 5/6 em relatórios consolidados e inclui organização estrutural do módulo de Relatórios.

---

# 14. Unidades, Soluções e Pedidos

Etapa 8:

- separar unidade de medida de apresentação física;
- `1 L = 1000 mL`;
- `1 kg = 1000 g`;
- não converter massa/volume sem densidade;
- estabilizar domínio de Soluções.

Etapa 9 integra Soluções aos Pedidos com aprovação atômica dos componentes.

---

# 15. Rótulos, Manual, testes e refactor

Etapa 10:

- padrão-base de rótulos;
- Produto/Resíduo/Solução;
- documento interno de lote;
- Zebra/ZPL/testes físicos.

Etapa 11:

- Manual do Usuário;
- avaliação geral de delete lógico.

Etapa 12:

```text
Vitest + Vue Test Utils
Cypress
```

Etapa 13:

- revisão estrutural/legibilidade;
- atenção especial a `Residuo` e Services grandes;
- sem alterar comportamento;
- reexecutar testes depois do refactor.

---

# 16. Situação da pré-produção

```text
Etapa 1 ✅
Etapa 2 ✅
Etapa 3 ✅
Etapa 4 ✅ concluída
Etapa 5 🔧 atual — 5.3 Atividades; 5.1 ✅; 5.2 ✅
Etapa 6 ⏳
Etapa 7 ⏳
Etapa 8 ⏳
Etapa 9 ⏳
Etapa 10 ⏳
Etapa 11 ⏳
Etapa 12 ⏳
Etapa 13 ⏳
```

---

# 17. Regra final de retomada

**Retomar pela Etapa 5. Ler `CONTINUIDADE.md`, `docs/PLANO_PRE_PRODUCAO.md` e `docs/CONTINUIDADE_ETAPA_5_2026-09-24.md`. A branch atual é `collab/etapa-5-projetos-atividades`. Os blocos 5.0, 5.1 e 5.2 estão fechados; continuar pelo 5.3 — Atividades. Manter o usuário como autor das mudanças funcionais de backend, salvo autorização explícita.**
