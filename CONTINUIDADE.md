# Continuidade do Projeto SGL — Backend

**Projeto:** Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Última atualização:** 25/09/2026  
**Branch estável:** `main` do GitLab institucional  
**Branch histórica da Etapa 4:** `feat/etapa-4-residuos` — referência; não mergear integralmente  
**Branch atual de trabalho:** `collab/etapa-5-projetos-atividades`  
**Fase atual:** pré-produção pós-aprovação funcional  
**Etapa concluída:** Etapa 4 — expansão operacional de Resíduos ✅  
**Etapa atual:** Etapa 5 — Projetos e Atividades 🔧 5.3 Atividades; 5.1 Projeto base ✅; 5.2 SCI ✅  
**Etapa 4:** 4.1–4.4 reconciliados, testados e validados ponta a ponta ✅
**Plano oficial:** `docs/PLANO_PRE_PRODUCAO.md`  
**Handoff da etapa atual:** `docs/CONTINUIDADE_ETAPA_5_2026-09-24.md`

Este arquivo é o checkpoint principal de retomada. Para detalhes do módulo de Resíduos, usar `docs/MODULO_RESIDUOS.md`. Para contratos HTTP, confirmar sempre no Swagger/OpenAPI em execução.

> **Infraestrutura Git obrigatória:** antes de qualquer alteração, ler `docs/SINCRONIZACAO_GITLAB_GITHUB.md`. Desde 22/09/2026 o GitLab é a fonte canônica de `main`; o GitHub espelha `main` automaticamente e é o ponto de colaboração para `collab/*`. Não usar `--force` e não editar `GitHub/main` diretamente.

---

# 0. Regra de trabalho

```text
branch própria
→ implementação/revisão
→ validação
→ refinamento
→ Pull Request
→ main
→ atualizar documentação
```

Regra especial do projeto:

- alterações funcionais de backend são implementadas **manualmente pelo responsável do projeto**;
- a IA deve analisar, modelar, explicar, fornecer código de referência e revisar;
- não aplicar diretamente código funcional de backend sem autorização explícita;
- frontend/documentação podem ser alterados diretamente quando autorizado;
- não antecipar etapas futuras.

As Etapas 1–4 já foram integradas e validadas.

Branch atual:

```text
collab/etapa-5-projetos-atividades
```

A Etapa 5 foi criada a partir da `main` contendo o fechamento da Etapa 4. Branches antigas de Etapa 4 permanecem somente como referência histórica.

---

# 1. Estado consolidado

## Backend

```text
Spring Boot / PostgreSQL / Flyway                     ✅
Long interno + UUID público                           ✅
DTOs request/response                                 ✅
Tratamento global de erros                            ✅
Concorrência de aprovação                             ✅
FIFO / FEFO                                           ✅
Lotes / rastreabilidade                               ✅
Embalagens / multiplicador / fracionamento            ✅
Pedidos e urgência                                    ✅
Swagger / OpenAPI                                     ✅
Movimentações                                         ✅
Relatórios consolidados                               ✅ base atual
Produtos fiscalizados                                 ✅
PDF / XLSX                                            ✅
Resíduos — fluxo atual refinado                       ✅ Etapa 3 concluída
Estagiários — base atual                              ✅ evolução na Etapa 6
Pessoas por laboratório                               ✅
Administração / Cadastros                             ✅
Isolamento operacional por Unidade                    ✅
Autenticação/autorização/auditoria definitiva         ⏳ roadmap formal
Integração corporativa                                ⏳ roadmap formal
```

## Frontend integrado

```text
Login visual / sessão DEV                             ✅
Expiração automática da sessão DEV em 5h              ✅
Pedidos do solicitante                                ✅
Pedidos da gestão                                     ✅
Estoque e lotes                                       ✅
Movimentações                                         ✅
Resíduos — solicitante e gestão                       ✅ Etapa 3 concluída
Classes de Resíduo em Cadastros                       ✅
Segurança/EPI de Produto/Resíduo                      ✅
Prévia antecipada do rótulo                           ✅
Impressão bloqueada até liberação                     ✅
Estagiários                                           ✅ base atual
Relatórios + PDF/XLSX                                 ✅
Pessoas por laboratório                               ✅
Administração / Cadastros                             ✅
Dashboard Gestão                                      ✅
Dashboard Solicitante                                 ✅
Alertas operacionais                                  ✅
Busca global                                          ✅
Dark Mode definitivo                                  ✅
Página 404                                            ✅
Contexto de Unidade enviado à API                     ✅
Testes automatizados frontend                         ⏳ Etapa 12
Autenticação/autorização definitiva                   ⏳ roadmap formal
```

---

# 2. Ordem de precedência

Quando houver conflito entre documentos:

```text
1. código da main
2. Swagger/OpenAPI
3. CONTINUIDADE.md do repositório em trabalho
4. docs/SINCRONIZACAO_GITLAB_GITHUB.md para fluxo Git/remotes
5. docs/PLANO_PRE_PRODUCAO.md
6. handoff da etapa atual
7. docs/DOSSIE_PROJETO_SGL.md
8. documentos específicos de módulo
9. documentos históricos
```

Documentos históricos podem permanecer para rastreabilidade, mas não devem comandar a tarefa atual quando houver checkpoint mais recente.

---

# 3. Arquitetura e identificadores

```text
Controller = contrato HTTP
Service = regra/transação/orquestração
Repository = persistência
Model = estado de domínio
RequestDTO = entrada
ResponseDTO = saída
```

Identificadores:

```text
Long id
→ banco, JPA, FKs e locks

UUID publicId
→ endpoints, DTOs e frontend
```

Não introduzir ID numérico em contratos públicos sem necessidade explícita.

---

# 4. PostgreSQL e Flyway

Ambiente de desenvolvimento padrão:

```text
PostgreSQL
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

O Hibernate valida; o Flyway evolui o schema.

Migrations relevantes já aplicadas:

```text
V11 — módulo de Resíduos
V12 — backfill Código SGL
V13 — estado físico, tratamento e responsabilidade inicial
V14 — Classes de Resíduo
V15 — segurança/EPI
V16 — snapshot de Unidade do Resíduo, incorporado pelo supervisor
V17 — locais de armazenamento de Resíduos
V18 — modelos reutilizáveis de Resíduos
V19 — expansão do domínio de Projeto
V20 — criação do domínio de SCI
```

Regra obrigatória:

```text
migration aplicada = imutável
nova alteração de schema = próxima versão livre após V20
```

---

# 5. Multitenancy por Unidade

Backend:

```text
X-SGL-Unidade-Id
→ TenantRequestFilter
→ TenantContext
→ services/repositories restringem dados
→ contexto limpo ao final
```

Frontend:

```text
sessão DEV contém unidadeId
→ interceptor envia X-SGL-Unidade-Id
```

Interpretação correta:

```text
isolamento funcional por Unidade              ✅
segurança definitiva por identidade            ❌ ainda não
```

A autenticação futura deve derivar Unidade/tenant da identidade autenticada confiável.

---

# 6. Estoque, lotes e Pedidos

```text
Produto = catálogo
EstoqueCentral = saldo consolidado por produto/Unidade
Lote = validade + saldo + apresentação + rastreabilidade
MovimentacaoEstoque = trilha de operações físicas
```

Seleção:

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

Regras:

- criação não baixa estoque;
- aprovação executa a baixa;
- entrega não baixa novamente;
- cancelamento aprovado restaura os lotes efetivamente usados;
- urgência não altera FIFO/FEFO.

Unidades/apresentações serão refinadas na Etapa 8 antes de Soluções.

---

# 7. Resíduos — estado após a Etapa 3

Decisão central:

```text
Produto != Resíduo
```

Componente de Resíduo pode referenciar Produto para rastreabilidade e sugestão de segurança sem movimentar estoque.

Fluxo validado:

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

O código e o QR técnico existem desde o registro inicial.

## Dados incorporados na Etapa 3

- Procedência/uso preservada em `processoOrigem`;
- estado físico;
- tratamento realizado + descrição;
- `gestorRecebedorInicial`;
- Classes de Resíduo informadas/confirmadas;
- snapshots de classes;
- Segurança/EPI informada/confirmada;
- recomendações de segurança em Produto;
- snapshots de segurança;
- comparação visual informado x aprovado;
- identificação do Gestor que liberou pelo histórico.

## Rótulo

```text
INFORMADO / EM_ANALISE
→ prévia disponível
→ impressão bloqueada

LIBERADO_PARA_ARMAZENAMENTO ou posterior
→ impressão liberada
```

Visualização e impressão são eventos distintos.

Template definitivo/Zebra continuam na Etapa 10.

## Responsabilidade

```text
usuarioGerador
→ quem informou

gestorRecebedorInicial
→ quem recebeu inicialmente e conduziu a conferência

HistoricoResiduo
→ ator real de cada transição
```

Armazenamento e despacho podem ser executados por outro Gestor sem perder o histórico anterior. Isso foi validado manualmente.

Detalhes: `docs/MODULO_RESIDUOS.md`.

---

# 8. Etapa 3 — fechamento

Fechada e validada em **17/09/2026**.

```text
3.1 redundância de análise                            ✅
3.2.1 estado físico/tratamento/responsabilidade       ✅
3.2.2 Classes de Resíduo                              ✅
3.2.3 Segurança/EPI + snapshot                        ✅
3.2.4 integração frontend                             ✅
3.3 identificação/prévia/permissão de impressão       ✅
```

A validação final cobriu criação, análise, armazenamento, despacho, Gestores diferentes, histórico, prévia, bloqueio/liberação de impressão, comparação informado/aprovado e legibilidade da tela.

Checkpoint: `docs/CONTINUIDADE_ETAPA_3_2026-09-11.md`.

---

# 9. Etapa 4 — concluída e validada

Referência de fechamento:

`docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`

Estado consolidado:

```text
4.1 Locais de armazenamento cadastráveis              ✅
4.2 Modelos de Resíduos pré-cadastrados               ✅
4.3 Uso de modelo ou preenchimento manual             ✅
4.4 Correções administrativas do ciclo de vida        ✅
```

A implementação histórica de `feat/etapa-4-residuos` foi portada seletivamente para a base corrigida pelo supervisor, preservando multitenancy fail-closed e a V16 canônica. Na base reconciliada:

- V17 cria o catálogo de locais de armazenamento;
- V18 cria os Modelos de Resíduo;
- ModeloResiduo é template; Residuo continua ocorrência independente;
- cancelamento e retorno administrativo exigem justificativa e preservam histórico;
- `DESPACHADO` precisa retornar de etapa antes de eventual cancelamento;
- reanálise reaproveita vínculos de classes confirmadas sem duplicação;
- dashboards distinguem visão pessoal do Solicitante e visão operacional da Gestão;
- relatórios/exportações reconhecem `CANCELADO`;
- validações funcionais da Etapa 4 foram concluídas.

A etapa canônica atual é a **Etapa 5 — Projetos e Atividades**, no bloco **5.5 — Interface e integração**. Os blocos **5.1 — Projeto base**, **5.2 — SCI**, **5.3 — Atividades** e **5.4 — Código SEG** foram concluídos e validados em 25/09/2026.


# 10. Projetos, Estagiários e Relatórios — evolução atual e próximas etapas

## Etapa 5 — Projetos + Atividades 🔧 ATUAL

Portão 5.0 fechado em 24/09/2026:

- Projeto 1 → N SCI 1 → N Atividades;
- Código SEG hierárquico confirmado;
- SCI é entidade própria subordinada ao Projeto;
- Atividade é entidade própria subordinada ao SCI;
- Projeto mantém Laboratório responsável/contextual, mas é o eixo funcional principal;
- status do Projeto: ATIVO → ENCERRADO_COM_AVALIACAO_PENDENTE → CONCLUIDO;
- situação de execução separada;
- recurso externo + empresa quando aplicável.

### Fechamento do 5.1 — Projeto base ✅

Validado em 25/09/2026:

- V19 expandiu a tabela `projetos` sem recriá-la;
- `codigoSeg`, status de negócio, situação de execução e recurso externo foram incorporados;
- `ativo` foi preservado como indicador técnico, separado do status de negócio;
- compatibilidade com payloads antigos foi mantida na criação e atualização;
- recurso externo exige empresa quando habilitado e limpa a empresa ao ser desligado;
- isolamento por tenant permaneceu fail-closed;
- dados DEV/Demo e testes automatizados foram atualizados;
- suíte completa de testes ficou verde;
- validação funcional confirmou listagem, defaults, criação completa, regra de empresa, atualização compatível e desligamento de recurso externo;
- validação hierárquica/formato/duplicidade do Código SEG permanece deliberadamente no 5.4.

### Fechamento do 5.2 — SCI ✅

Validado em 25/09/2026:

- V20 criou a tabela `scis` com vínculo obrigatório a Projeto;
- SCI deriva Laboratório/Unidade por `SCI → Projeto → Laboratório → Unidade`;
- entidade, DTOs, Repository, Service e Controller/OpenAPI foram implementados;
- status e situação de execução são persistidos independentemente do Projeto;
- vínculo com Projeto é preservado no update comum;
- período do SCI é validado dentro do período do Projeto;
- Projeto sem data de início não recebe SCI;
- recurso externo permanece somente no Projeto;
- consultas públicas permanecem fail-closed por tenant;
- dados DEV/Demo e testes automatizados foram adicionados;
- suíte completa JUnit ficou verde;
- bateria funcional da API foi validada integralmente;
- formato, raiz e duplicidade do Código SEG permanecem deliberadamente no 5.4.

Bloco atual: **5.5 Interface e integração**.

Contrato aprovado para o bloco:

- Atividade será subordinada obrigatoriamente ao SCI;
- Atividade reutilizará os valores de `StatusProjeto` e `SituacaoExecucaoProjeto`, persistidos independentemente;
- V21 cria Atividades;
- prorrogações de Projeto/SCI/Atividade são eventos explícitos, justificados e históricos;
- V22 fica reservada a três históricos de prorrogação com FKs reais;
- prorrogação de pai não altera automaticamente filhos;
- redução de período de pai que invalidaria filhos deve ser rejeitada.

A base V21 de Atividades foi validada em 25/09/2026. A V22 de prorrogações também foi concluída em 25/09/2026 com suíte automatizada verde; a bateria manual via Postman foi deliberadamente dispensada nesta rodada.

No 5.4, formato/coerência hierárquica, unicidade global, imutabilidade no CRUD comum e correção administrativa auditável do Código SEG foram concluídos. V23 aplica restrições `UNIQUE`; V24 cria o histórico de correções. Projeto legado sem SEG ainda pode receber a primeira definição, mas depois disso Projeto/SCI/Atividade só podem trocar o identificador pelo fluxo administrativo. A correção exige justificativa, operador ativo do tenant com perfil GESTOR/ADMINISTRADOR, valida novamente formato/hierarquia/unicidade e atualiza transacionalmente os descendentes preservando seus sufixos. Registros encerrados/inativos também podem ter erro de identificação corrigido sem reabrir seu ciclo de vida. O código foi publicado e a suíte JUnit completa foi confirmada verde em 25/09/2026; o 5.4 está oficialmente concluído e validado.

Observação de segurança: o backend ainda não possui principal autenticado; autoria de prorrogações e correções de Código SEG é provisoriamente identificada por UUID de usuário validado contra tenant/perfil, seguindo o padrão de pré-autenticação existente, até a autenticação definitiva fornecer o ator pelo contexto autenticado.

## Etapa 6 — Estagiários

Planejado:

- Orientador obrigatório;
- Projeto/Atividade;
- Bolsa/vínculo separado de Curso/Formação;
- Cultura/área temática;
- treinamento inicial de segurança;
- prorrogações justificadas e históricas.

## Etapa 7 — Relatórios consolidados

Depende das Etapas 5 e 6 estabilizadas. Inclui filtros/agregações, telas, PDF/XLSX e organização estrutural do módulo de relatórios.

---

# 11. Unidades, Soluções e Pedidos

## Etapa 8

Normalizar:

```text
unidade de medida
≠
apresentação física
```

Conversões compatíveis:

```text
1 L = 1000 mL
1 kg = 1000 g
```

Não converter massa ↔ volume genericamente sem densidade.

Depois estabilizar domínio de Soluções.

## Etapa 9

Integrar Soluções aos Pedidos sem redefinir a entidade Solução. Aprovação deve validar atomicamente todos os componentes.

---

# 12. Rótulos, Manual, Testes e Refactor

## Etapa 10 — Rótulos e impressão

- padrão-base SGL;
- rótulos adaptados de Produto/Resíduo/Solução;
- documento interno de auditoria de entrada de lote;
- Zebra/ZPL/testes físicos.

## Etapa 11 — Manual + delete lógico

- Manual do Usuário;
- avaliação de delete lógico entidade por entidade;
- não substituir ciclos de vida por `ativo` indiscriminadamente.

## Etapa 12 — testes frontend

```text
Vitest + Vue Test Utils
Cypress
```

## Etapa 13 — revisão estrutural e legibilidade

Revisar classes grandes, com atenção especial a `Residuo`, Services, DTOs e Controllers.

Não fazer refactor grande agora apenas para reduzir linhas. O refactor final deve ocorrer depois da suíte da Etapa 12 e reexecutar os testes.

---

# 13. Situação da pré-produção

```text
Etapa 1 — refinamento visual global                   ✅
Etapa 2 — Dark Mode definitivo                        ✅
Etapa 3 — refinamentos do fluxo atual de Resíduos     ✅ concluída e validada
Etapa 4 — expansão operacional de Resíduos            ✅ concluída e validada
Etapa 5 — Projetos + Atividades                       🔧 atual — 5.5 Interface e integração; 5.1–5.4 ✅
Etapa 6 — Estagiários + vínculos                      ⏳
Etapa 7 — relatórios consolidados                     ⏳
Etapa 8 — unidades + Soluções                         ⏳
Etapa 9 — Pedidos + Soluções                          ⏳
Etapa 10 — Rótulos + impressão operacional            ⏳
Etapa 11 — Manual + decisão delete lógico             ⏳
Etapa 12 — testes automatizados frontend              ⏳
Etapa 13 — revisão estrutural e legibilidade           ⏳
```

Matriz de permissões, congelamento funcional e autenticação definitiva continuam posteriores ao bloco atual.

---

# 14. Regra final de retomada

**As Etapas 1–4 estão encerradas e validadas. A Etapa 5 está em andamento na branch `collab/etapa-5-projetos-atividades`, com 5.0, 5.1, 5.2, 5.3 e 5.4 fechados e validados; retomar pelo bloco 5.5 — Interface e integração. Ler `docs/CONTINUIDADE_ETAPA_5_2026-09-24.md` antes de alterar código. GitLab/main permanece a fonte canônica e GitHub/main seu espelho.**

### Estado do 4.4

Implementado na branch de reconciliação:
- status `CANCELADO`;
- ações administrativas `CANCELAR` e `RETORNAR_ETAPA`;
- justificativa obrigatória e registro em histórico;
- retorno de exatamente uma etapa por ação;
- `DESPACHADO` não pode ser cancelado diretamente, mas pode retornar para `ARMAZENADO_TEMPORARIAMENTE`;
- operação restrita no service ao perfil `ADMINISTRADOR` e ao tenant atual;
- cancelados incluídos em relatórios e exportações;
- testes unitários/service/controller adicionados.

A Etapa 4 só deve ser marcada como concluída após a validação funcional do frontend e do fluxo integrado.

### Ajustes após validação do 4.4

- histórico administrativo passou a usar nomes legíveis de status;
- corrigida a reanálise de Resíduo retornado: classes confirmadas existentes são reaproveitadas em vez de removidas/reinseridas, evitando conflito com `uk_residuo_classe_etapa`;
- criado histórico agregado por gerador para alimentar as atualizações do dashboard sem inferir eventos pelo status atual;
- testes adicionados para reconfirmação da mesma classe, histórico por gerador e texto humanizado do retorno.

Os ajustes acima foram validados e fazem parte do fechamento definitivo da Etapa 4.
