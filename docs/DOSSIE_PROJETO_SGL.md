# Dossiê do Projeto SGL — Handoff

**Projeto:** SGL — Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Atualizado em:** 01/10/2026  
**Estado:** primeiro protótipo funcional aprovado; Etapas 1–5 concluídas e validadas; Etapa 6 em andamento, com 6.1–6.3 concluídos e validados; 6.4 em execução no modelo de sincronização institucional.  
**Objetivo:** permitir que outra pessoa ou IA retome o projeto pelo estado real atual sem reconstruir o histórico.

## Checkpoint atual

```text
Etapa 1 — padrão visual global              ✅ concluída
Etapa 2 — Dark Mode definitivo              ✅ concluída
Etapa 3 — refinamentos de Resíduos          ✅ concluída e validada
Etapa 4 — expansão operacional de Resíduos  ✅ concluída e validada
Etapa 5 — Projetos e Atividades             ✅ concluída e validada
Etapa 6 — Estagiários e vínculos             🔧 6.1–6.3 concluídos; 6.4 sincronização institucional
```

Handoff imediato:

`docs/VALIDACAO_ETAPA_6_3.md`

Decisão atual do ciclo institucional:

`docs/DECISAO_CICLO_INSTITUCIONAL_ESTAGIO.md`

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

As Etapas 4 e 5 foram integradas à `main`. A branch operacional atual é `collab/etapa-6-estagiarios-vinculos`; branches anteriores permanecem apenas como referência histórica.

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

As Etapas 1–5 estão fechadas. A branch operacional atual é `collab/etapa-6-estagiarios-vinculos`, criada sobre a `main` pós-Etapa 5.

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
V21 criação do domínio de Atividade
V22 históricos de prorrogação
V23 unicidade global do Código SEG
V24 histórico de correções SEG
V25 fundação de situação/orientador do estágio
V26 vínculos institucionais de estágio
V27 participação VinculoEstagio ↔ Atividade
V28 dados acadêmicos do vínculo + catálogo de Cursos
V29 catálogo de Culturas + associação com participação em Atividade
V30 referência institucional + histórico de sincronização do vínculo
V31 remoção da unicidade global não confirmada da referência institucional
```

A V29 e o 6.3 foram concluídos e validados em 01/10/2026. A V30 abriu o 6.4.1 com referência institucional do vínculo e histórico de sincronização. A V31 remove a unicidade global não confirmada da referência institucional sem reescrever a migration já publicada. `referenciaInstitucional` permanece indexada; idempotência de eventos usa origem + referência do evento quando disponível. Próxima migration esperada, se necessária: **V32+**.

Prorrogações serão eventos próprios com justificativa, autor, data final anterior/nova e histórico transacional. Não haverá propagação automática de prazo do pai para os filhos.

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

A numeração é institucional e cadastrada; o SGL valida formato, hierarquia e unicidade. A interface sugere o próximo sufixo provável para SCI/Atividade, mas permite edição antes do primeiro salvamento.


Etapa 5 estabiliza Projetos/Atividades e Código SEG.

Etapa 6 evolui Estagiários com:

- Orientador obrigatório;
- `Usuario → Estagiario → VinculoEstagio 1..N`;
- múltiplas participações históricas em Atividades;
- Projeto/SCI/Laboratório derivados da Atividade;
- vínculo sincronizado do ambiente pode existir antes da primeira Atividade; atividade aberta continua obrigatória para estado operacional ativo;
- estado operacional ativo dependente de Usuario ativo + vínculo não finalizado + participação aberta;
- Bolsa separada de Curso/Formação;
- Formação controlada por enum + opção `OUTRO`;
- Curso como catálogo administrável por Unidade;
- Cultura/área temática por participação;
- treinamento de segurança iniciado em `false` e concluído por ação específica;
- histórico de sincronizações/prorrogações institucionais;
- abandono da noção operacional de um único "Laboratório do Estagiário".

Estado atual da Etapa 6:

```text
6.1 contrato e fundação do vínculo                  ✅ 30/09/2026
6.2 vínculos Estagiário ↔ Atividade + histórico    ✅ 01/10/2026
6.3 dados acadêmicos/Curso/Cultura/segurança         ✅ concluído e validado
6.3.1 Formação + Curso + treinamento                ✅ concluído e validado
6.3.2 Cultura por participação                      ✅ concluído e validado
6.4 ciclo institucional/sincronização                🔧 atual
6.4.1 contrato + histórico institucional             ✅ implementado
6.4.2 sincronização do vínculo existente             🔧 atual
  6.4.2A DTOs/contrato                               ✅ implementado
  6.4.2B regra transacional                          🔧 atual
  6.4.2C endpoint + testes                           ⏳
6.4.3 nova bolsa / novo vínculo                      ⏳
6.4.4 testes/validação/documentação                  ⏳
6.5 frontend integrado                              ⏳
6.6 dados DEV/testes/validação/documentação         ⏳
```

O 6.2 teve suíte backend completa e compilação confirmadas sem erros/falhas em 01/10/2026. O 6.3 também foi validado em 01/10/2026 com compilação concluída e suíte completa verde. Checkpoint atual: `docs/VALIDACAO_ETAPA_6_3.md`.

Etapa 7 consome Etapas 5/6 em relatórios consolidados e inclui organização estrutural do módulo de Relatórios.

---

# 14. Unidades, Soluções e Pedidos

Etapa 8:

- separar unidade de medida de apresentação física;
- `1 L = 1000 mL`;
- `1 kg = 1000 g`;
- não converter massa/volume sem densidade;
- estabilizar domínio de Soluções;
- fechar o contrato de contexto operacional do solicitante;
- tratar `Usuario.laboratorio` e o laboratório da sessão DEV como compatibilidade, não como fonte de verdade para Estagiários;
- preparar Pedido para consumir a participação aberta do Estagiário.

Etapa 9 integra Soluções aos Pedidos com aprovação atômica dos componentes e aplica a evolução:

```text
Pedido
→ VinculoEstagioAtividade
→ Atividade
→ SCI
→ Projeto
→ Laboratorio
→ Unidade
```

Para Estagiários, a participação será escolhida entre as participações abertas e Projeto/Laboratório serão derivados pelo backend.

Para solicitantes não Estagiários:

- com Projeto: o Projeto é escolhido e o Laboratório é derivado pelo backend;
- sem Projeto: `Usuario.laboratorio` permanece como contexto institucional/base;
- a participação de estágio não é obrigatória.

A interface de Estagiários, já no 6.5, deve abandonar a legenda de um único laboratório e exibir os contextos por participação.

Decisão detalhada: `docs/DECISAO_CONTEXTO_OPERACIONAL_ESTAGIARIO_PEDIDOS.md`.

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
Etapa 5 ✅ concluída e validada
Etapa 6 🔧 6.1–6.3 concluídos; 6.4 atual
Etapa 7 ⏳
Etapa 8 ⏳ unidades/Soluções/contexto operacional
Etapa 9 ⏳ Pedidos/Soluções/participação
Etapa 10 ⏳
Etapa 11 ⏳
Etapa 12 ⏳
Etapa 13 ⏳
```

---

# 17. Regra final de retomada

**As Etapas 1–5 estão encerradas e validadas. Na Etapa 6, os blocos 6.1–6.3 estão concluídos e validados; o 6.4 está em andamento na branch `collab/etapa-6-estagiarios-vinculos`, iniciando pelo contrato/histórico de sincronização institucional. A identidade Usuario/Estagiario é estável; prorrogação mantém o mesmo vínculo e nova bolsa cria novo VinculoEstagio. Ler também `docs/DECISAO_CICLO_INSTITUCIONAL_ESTAGIO.md`.**
