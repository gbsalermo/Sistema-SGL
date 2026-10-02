# Continuidade do Projeto SGL — Backend

**Projeto:** Sistema de Gestão de Laboratórios  
**Backend:** `gbsalermo/Sistema-SGL`  
**Frontend:** `gbsalermo/SGL-FRONTEND`  
**Última atualização:** 02/10/2026  
**Branch estável:** `main` do GitLab institucional  
**Branch histórica da Etapa 4:** `feat/etapa-4-residuos` — referência; não mergear integralmente  
**Branch atual de trabalho:** `collab/etapa-6-estagiarios-vinculos`  
**Fase atual:** pré-produção pós-aprovação funcional  
**Etapa concluída:** Etapa 5 — Projetos e Atividades ✅  
**Etapa atual:** Etapa 6 — Estagiários e vínculos 🔧  
**Etapa 4:** 4.1–4.4 reconciliados, testados e validados ponta a ponta ✅
**Plano oficial:** `docs/PLANO_PRE_PRODUCAO.md`  
**Checkpoint validado atual:** `docs/VALIDACAO_ETAPA_6_4.md`  
**Decisão do bloco 6.4:** `docs/DECISAO_CICLO_INSTITUCIONAL_ESTAGIO.md`

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
collab/etapa-6-estagiarios-vinculos
```

A Etapa 6 foi criada a partir da `main` pós-merge da Etapa 5. As branches das etapas anteriores permanecem somente como referência histórica.

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
V21 — criação do domínio de Atividade
V22 — históricos de prorrogação de Projeto/SCI/Atividade
V23 — unicidade global dos Códigos SEG
V24 — histórico auditável de correções de Código SEG
V25 — fundação do ciclo de estágio: situação + orientador no legado
V26 — criação de vínculos institucionais de estágio + migração do legado
V27 — associação histórica VinculoEstagio ↔ Atividade
V28 — dados acadêmicos do vínculo + catálogo de Cursos por Unidade
V29 — catálogo de Culturas + associação Cultura ↔ participação em Atividade
V30 — referência institucional do vínculo + histórico de sincronização institucional
V31 — remoção da unicidade global não confirmada da referência institucional
```

Regra obrigatória:

```text
migration aplicada = imutável
nova alteração de schema = próxima versão livre após V29
```

A V29 foi concluída e validada no 6.3.2. A V30 iniciou o 6.4.1 com `referencia_institucional` em `vinculos_estagio` e `historico_sincronizacao_vinculo_estagio`. Como a V30 já foi publicada, a V31 remove de forma evolutiva a unicidade global inicialmente assumida para `referencia_institucional`, preservando o índice de consulta. O SGL não assume unicidade global enquanto o contrato corporativo não confirmar esse requisito. A idempotência de evento é separada por origem + referência do evento quando disponível. Fundação implementada e ciclo institucional 6.4 concluído e validado em 02/10/2026. Próxima migration esperada, se necessária, é **V32**.

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

## Etapa 5 — Projetos + Atividades ✅ CONCLUÍDA E MERGEADA

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

Bloco 5.5: **Interface e integração ✅ concluída e validada**.

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

Fechamento do 5.5 em 29/09/2026:

- massa DEV idempotente validada em banco existente;
- frontend possui hub operacional direto de Projetos;
- Projeto → SCI → Atividade é exibido e gerenciado na mesma experiência;
- SCI e Atividade possuem criação/edição no hub;
- cadastro administrativo de Projeto está alinhado a Código SEG/status/situação/recurso externo;
- prorrogações, correções SEG e históricos estão integrados;
- filtro por Laboratório e hierarquia visual SCI/Atividade aprovados;
- sugestão editável de Código SEG para novo SCI/Atividade aprovada;
- dark mode e tipografia canônica validados;
- Etapa 5 oficialmente concluída e pronta para merge.

## Etapa 6 — Estagiários e vínculos 🔧 ATUAL

Branch:

```text
collab/etapa-6-estagiarios-vinculos
```

A Etapa 6 deve **evoluir o domínio atual de `Estagiario`**, sem criar um segundo módulo paralelo. O Estagiário continua sendo uma extensão institucional de `Usuario`.

### Identidade institucional x vínculo de estágio

`Usuario` é a identidade institucional única, originada futuramente do ambiente corporativo/DevOps. O vínculo de Estagiário possui ciclo de vida próprio.

Regras fechadas:

- `Usuario.ativo` e situação do estágio são conceitos distintos;
- um vínculo de estágio **nunca pode estar ativo se o Usuario estiver inativo**;
- finalizar um estágio **não desativa o Usuario**; a identidade institucional permanece independente do ciclo do estágio;
- Usuario inativado no ambiente institucional não pode manter estágio ativo;
- um Usuario ativo, mas sem vínculo de estágio/atividade válido, não deve manter acesso operacional de Estagiário à interface de pedidos;
- o acesso operacional do Estagiário depende de Usuario ativo + vínculo não finalizado + pelo menos uma participação de Atividade aberta;
- não reutilizar `Usuario.ativo` como único indicador de situação do estágio.

Situações previstas para o vínculo:

```text
EM_ANDAMENTO
PRORROGADO
FINALIZADO
```

### Orientador

Todo Estagiário deve possuir Orientador obrigatório.

O Orientador:

- é um `Usuario`;
- deve ter perfil `PESQUISADOR` ou `ANALISTA`;
- deve pertencer à mesma Unidade do Estagiário;
- deve estar ligado ao contexto do Projeto/Laboratório aplicável;
- não exige entidade paralela `Orientador`.

### Atividades, SCI e Projetos

O Estagiário deve estar relacionado a **pelo menos uma Atividade** e pode possuir **uma ou mais Atividades simultâneas**.

Hierarquia reaproveitada da Etapa 5:

```text
Estagiário
→ Atividade(s)
→ SCI
→ Projeto
→ Laboratório
→ Unidade
```

Regras fechadas:

- não criar FK redundante direta de Projeto no Estagiário;
- Projeto e SCI são derivados da Atividade;
- todas as Atividades vinculadas ao Estagiário devem pertencer à mesma Unidade do Usuario;
- adicionar outra Atividade não representa necessariamente uma "migração";
- encerramento de participação em uma Atividade deve encerrar apenas aquele vínculo;
- histórico deve preservar vínculos encerrados;
- não sobrescrever silenciosamente um vínculo anterior;
- um Estagiário pode continuar ativo em outra Atividade após encerrar uma participação específica.

### Contexto operacional do Estagiário

A participação em Atividade é a fonte de verdade operacional do Estagiário:

```text
Usuario
→ Estagiario
→ VinculoEstagio
→ VinculoEstagioAtividade
→ Atividade
→ SCI
→ Projeto
→ Laboratorio
→ Unidade
```

Consequências:

- não existe necessariamente um único "Laboratório do Estagiário";
- o mesmo vínculo pode possuir Atividades em Projetos/Laboratórios diferentes dentro da mesma Unidade;
- `Usuario.laboratorio` permanece temporariamente como campo legado/contexto de compatibilidade, mas **não é fonte operacional para Estagiários**;
- a interface de Estagiários deve mostrar Atividade/Projeto/Laboratório por participação, em vez de um laboratório único;
- Pedidos de Estagiários serão futuramente contextualizados por `VinculoEstagioAtividade`, com Projeto/Laboratório derivados pelo backend.

Decisão detalhada: `docs/DECISAO_CONTEXTO_OPERACIONAL_ESTAGIARIO_PEDIDOS.md`.

### Bolsa / tipo de vínculo, Formação e Curso

Os três conceitos permanecem separados e têm finalidade predominantemente informativa e de filtro/relatório.

`TipoBolsa` continua como enum, aproveitando o domínio atual e podendo ser evoluído com novos valores institucionais quando necessário.

Formação e Curso seguem estratégias diferentes:

- **Formação** é enum controlado no vínculo, com valores institucionais conhecidos e opção `OUTRO`;
- quando `formacao = OUTRO`, `formacaoOutro` é obrigatório;
- **Curso** é catálogo administrável por Unidade, porque novos cursos podem surgir sem exigir alteração de código/migration;
- Curso inativo permanece visível em vínculos históricos, mas não pode ser escolhido em novo vínculo.

Formações atuais:

```text
ENSINO_MEDIO
GRADUACAO
MESTRADO
DOUTORADO
POS_DOUTORADO
APOIO_ADMINISTRATIVO
APOIO_TECNICO
OUTRO
```

Curso não é enum e não deve ser duplicado como String livre no vínculo.

### Cultura / área temática

Cultura é uma característica ligada à participação do Estagiário em suas Atividades, não uma String única global no Estagiário.

Um Estagiário pode possuir múltiplas Culturas por estar vinculado a múltiplas Atividades.

A modelagem deve seguir o padrão de associações reutilizáveis já utilizado em Produto/Resíduo para riscos/classes:

- catálogo reutilizável por Unidade;
- relacionamento N:N quando aplicável;
- sem String solta repetida;
- respeitar tenant/Unidade;
- permitir filtro futuro em relatórios.

A cardinalidade foi implementada como N:N entre `VinculoEstagioAtividade` e `Cultura`, evitando atribuir uma única Cultura global ao Usuario.

### Período, encerramento e prorrogação

O vínculo de estágio deve distinguir claramente:

```text
dataInicioEstagio
dataFimPrevista
dataFimEfetiva
```

O ciclo institucional do estágio é **autoritativo no ambiente corporativo**, não no workflow cotidiano do SGL.

Regras:

- `Usuario` representa a identidade institucional estável; matrícula/CPF identifica a pessoa e troca de senha não cria nova identidade;
- `Estagiario` representa o papel persistente;
- `VinculoEstagio` representa cada ocorrência institucional de bolsa/estágio;
- prorrogação da mesma bolsa mantém o mesmo `VinculoEstagio` e amplia `dataFimPrevista`;
- nova bolsa após encerramento cria novo `VinculoEstagio` para o mesmo `Usuario/Estagiario`;
- o SGL não oferece "Finalizar estágio" como fonte normal de verdade;
- finalização/prorrogação recebidas do ambiente são sincronizadas e auditadas;
- finalizar vínculo não desativa o `Usuario`;
- `Usuario.ativo = false` remove acesso operacional, mas não implica sozinho `VinculoEstagio = FINALIZADO`;
- ao receber vínculo FINALIZADO, o SGL encerra participações ainda abertas e preserva histórico;
- inconsistência temporal recebida da fonte institucional deve ser sinalizada para reconciliação, nunca corrigida silenciosamente.

Prorrogação institucional deve preservar ao menos data final anterior, nova data, origem e data/hora. Justificativa de Gestor não é exigida quando a alteração vem da fonte institucional.

Decisão detalhada: `docs/DECISAO_CICLO_INSTITUCIONAL_ESTAGIO.md`.

### Treinamento de segurança

É informação do `VinculoEstagio` e nasce obrigatoriamente como `false`.

```text
novo vínculo
→ treinamentoSegurancaConcluido = false

treinamento realizado
→ ação específica de conclusão
→ true
```

Não permitir que o cliente crie um vínculo já marcado como treinado. A reversão de `true → false`, se algum dia necessária, deve ser tratada como correção administrativa/auditável e não como edição comum.

### Código interno / matrícula

Não criar Código SGL adicional para Estagiário.

A identidade funcional deverá utilizar a matrícula institucional do Usuario quando essa informação vier do ambiente corporativo/DevOps. O Código SEG permanece reservado ao domínio Projeto/SCI/Atividade.

### Fechamento do 6.1 — contrato e fundação do vínculo ✅

Concluído e validado em **30/09/2026**.

A fundação foi refinada para separar identidade, papel e ocorrência institucional:

```text
Usuario
└── Estagiario
    └── VinculoEstagio 1..N
```

Decisões e implementação consolidadas:

- `Usuario` continua sendo a identidade institucional única;
- `Estagiario` representa o papel persistente do usuário no domínio do SGL;
- `VinculoEstagio` representa cada ocorrência institucional do estágio ao longo do tempo;
- o mesmo Estagiário pode possuir múltiplos vínculos históricos sem sobrescrever períodos anteriores;
- V25 introduziu `SituacaoEstagio` e orientador na estrutura legada como etapa de transição;
- V26 criou `vinculos_estagio` com UUID público, orientador, período previsto/efetivo, tipo de vínculo, situação e observação;
- os dados legados são migrados para o primeiro `VinculoEstagio`;
- `SituacaoEstagio` possui `EM_ANDAMENTO`, `PRORROGADO` e `FINALIZADO`;
- Orientador é `Usuario` ativo com perfil `PESQUISADOR` ou `ANALISTA`, na mesma Unidade do Estagiário;
- criação de novo Estagiário gera o primeiro `VinculoEstagio`;
- respostas da API passam a carregar o histórico de vínculos;
- o estado operacional `ativo` passa a considerar `Usuario.ativo` + existência de vínculo não finalizado;
- atualização, exclusão e encerramento legados foram bloqueados para não sobrescrever histórico nem desativar indevidamente o Usuario;
- encerramento definitivo permanece reservado ao fluxo de vínculo da subetapa 6.4;
- campos legados em `estagiarios` permanecem temporariamente para compatibilidade e serão removidos somente após a migração completa do fluxo;
- suíte completa do backend foi executada após os ajustes e ficou verde;
- testes de Service, Controller e Repository foram atualizados para o novo domínio;
- Repository passou a cobrir explicitamente usuário ativo + vínculo ativo, vínculo finalizado e usuário inativo.

Importante: a exigência de primeira Atividade permanece para o fluxo local/DEV criado no 6.2, mas **não é uma regra absoluta do vínculo institucional**. Quando a integração corporativa estiver ativa, um novo `VinculoEstagio` poderá ser sincronizado antes de possuir participação em Atividade; nesse estado ele existe institucionalmente, mas ainda não é operacionalmente ativo no SGL.

### Fechamento do 6.2 — vínculos múltiplos Estagiário ↔ Atividade + histórico ✅

Concluído e validado em **01/10/2026**.

Implementação consolidada:

- V27 criou `vinculo_estagio_atividade` para preservar histórico de participação em Atividades;
- um `VinculoEstagio` pode possuir múltiplas participações em Atividades;
- Projeto, SCI, Laboratório e Unidade continuam derivados por `Atividade → SCI → Projeto → Laboratório → Unidade`;
- não foi criada FK redundante direta de Projeto no Estagiário/Vínculo;
- é permitido histórico de reentrada na mesma Atividade; apenas duas participações simultaneamente abertas para o mesmo vínculo/Atividade são bloqueadas;
- adicionar participação exige vínculo não finalizado, Usuario ativo, Atividade/SCI/Projeto/Laboratório operacionais e mesma Unidade;
- encerrar uma participação preserva histórico e não encerra o estágio;
- enquanto o vínculo estiver em andamento, a última participação aberta não pode ser encerrada isoladamente;
- o encerramento definitivo da última participação será coordenado pelo fluxo de encerramento do próprio vínculo no 6.4;
- no fluxo local/DEV, novo `VinculoEstagio` para Estagiário já existente nasce com a primeira Atividade; na integração institucional futura, o vínculo poderá chegar primeiro e receber a Atividade depois;
- um Estagiário não pode possuir dois vínculos não finalizados simultaneamente;
- o primeiro cadastro via `POST /estagiarios` também exige Atividade inicial e cria a primeira participação;
- respostas de vínculo passam a carregar `participacoesAtividade` com dados derivados de Atividade/SCI/Projeto/Laboratório;
- estado operacional `ativo` e listagem `/estagiarios/ativos` passam a exigir Usuario ativo + vínculo não finalizado + participação de Atividade aberta;
- desativação técnica de Projeto/SCI/Atividade não encerra silenciosamente um estágio já registrado; o histórico institucional permanece explícito.

Endpoints consolidados no 6.2:

```text
POST /api/v1/vinculos-estagio/{vinculoId}/atividades
GET  /api/v1/vinculos-estagio/{vinculoId}/atividades
GET  /api/v1/vinculos-estagio/{vinculoId}/atividades/ativas
PUT  /api/v1/vinculos-estagio/participacoes/{participacaoId}/encerrar
POST /api/v1/vinculos-estagio/estagiarios/{estagiarioId}
```

Validação:

- testes unitários de `EstagiarioService`, `VinculoEstagioService` e `VinculoEstagioAtividadeService`;
- testes de Controller para os fluxos HTTP novos;
- testes de Repository/H2 real para os caminhos de tenant e associação;
- cobertura da regra da última participação ativa;
- cobertura de primeiro vínculo com Atividade e novo vínculo histórico;
- cobertura da regra operacional de Estagiário ativo;
- **suíte completa do backend executada sem erros ou falhas em 01/10/2026**;
- **compilação completa do sistema concluída sem erros em 01/10/2026**.

Checkpoint detalhado: `docs/VALIDACAO_ETAPA_6_2.md`.

### Fechamento do 6.3 — dados acadêmicos, Curso, Cultura e segurança ✅

Concluído e validado em **01/10/2026**.

Implementação consolidada:

- V29 criou `culturas` e `vinculo_estagio_atividade_cultura`;
- Cultura é catálogo por Unidade, com inativação lógica;
- uma participação pode possuir múltiplas Culturas e a mesma Cultura pode aparecer em várias participações;
- atualização de Culturas usa operação idempotente por conjunto;
- Cultura inativa pode permanecer em associação histórica existente, mas não pode entrar como nova associação;
- criação de nova participação aceita `culturaIds`;
- primeiro vínculo de um Estagiário existente aceita Culturas na participação inicial;
- primeiro cadastro de Estagiário aceita Culturas na participação inicial;
- respostas diretas e aninhadas de participação retornam Culturas;
- `EstagiarioResponseDTO → VinculoEstagioResponseDTO → participacoesAtividade → culturas` permanece consistente;
- testes existentes foram ajustados às novas dependências e campos obrigatórios.

O 6.3 foi validado em 01/10/2026 com compilação backend concluída e suíte completa verde. Checkpoint: `docs/VALIDACAO_ETAPA_6_3.md`.

### Divisão de execução

```text
6.1 — contrato e fundação do vínculo                     ✅ concluído e validado
6.2 — vínculos múltiplos Estagiário ↔ Atividade + histórico ✅ concluído e validado
6.3 — dados acadêmicos, Curso, Cultura e segurança         ✅ concluído e validado
6.3.1 — Formação + Curso + treinamento                    ✅ concluído e validado
6.3.2 — Cultura por Unidade + participação                ✅ concluído e validado
6.4 — ciclo institucional e sincronização                 ✅ concluído e validado em 02/10/2026
6.4.1 — contrato + histórico da sincronização             ✅
6.4.2 — sincronização do vínculo existente                ✅
  6.4.2A — DTOs/contrato de sincronização                 ✅
  6.4.2B — regra transacional                             ✅
  6.4.2C — endpoint + testes                              ✅ suíte local verde
6.4.3 — nova bolsa / novo VinculoEstagio                  ✅ suíte local verde
6.4.4 — testes, validação e documentação                  ✅ concluído
6.5 — frontend integrado                                  ⏳
6.6 — dados DEV, testes, validação e documentação          ⏳
```

Não antecipar Etapa 7 durante esta implementação.

## Etapa 7 — Relatórios consolidados

Depende das Etapas 5 e 6 estabilizadas. Inclui filtros/agregações, telas, PDF/XLSX e organização estrutural do módulo de relatórios.

Decisões já fechadas para a Etapa 7:

- adicionar **Projetos** como relatório consolidado próprio;
- incluir Código SEG, Laboratório, responsável/líder, período, status, situação de execução, recurso externo e agregações de SCI/Atividades;
- incorporar dimensões de vínculos/Estagiários depois da Etapa 6;
- sintetizar **Movimentações** e **Resumo operacional** em uma única opção da Central;
- dentro de Movimentações, manter modos/visões **Resumo** e **Detalhamento**;
- preservar inicialmente os endpoints atuais de Movimentações e Resumo operacional para compatibilidade, tratando a unificação primeiro como decisão de interface;
- usar o espaço liberado no seletor da Central para o novo relatório de Projetos.

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

### Contexto operacional para Pedidos

Antes da Etapa 9, fechar o contrato de contexto operacional:

- Unidade/tenant continua vindo da identidade/sessão institucional;
- para Estagiários, Laboratório/Projeto não vêm de `Usuario.laboratorio`;
- o contexto operacional do Estagiário vem da participação aberta `VinculoEstagioAtividade`;
- se houver uma única participação aberta, a interface pode selecioná-la automaticamente;
- se houver múltiplas, o Estagiário escolhe qual Atividade está originando o Pedido;
- para TECNICO/ANALISTA/PESQUISADOR com Projeto, o frontend seleciona o Projeto e o backend deriva o Laboratório;
- para TECNICO/ANALISTA/PESQUISADOR sem Projeto, `Usuario.laboratorio` continua sendo o contexto institucional/base;
- `laboratorioId/laboratorioNome` existentes na sessão DEV são compatibilidade e não autoridade operacional universal.

## Etapa 9

Integrar Soluções aos Pedidos e aplicar a refatoração estrutural de contexto do Estagiário.

Para Pedidos de Estagiários:

```text
Pedido
→ VinculoEstagioAtividade
→ Atividade
→ SCI
→ Projeto
→ Laboratorio
→ Unidade
```

O frontend envia a participação escolhida; o backend deriva e valida Projeto/Laboratório/Unidade. Os FKs diretos existentes de Projeto/Laboratório podem ser preservados por compatibilidade/rastreabilidade, mas não devem ser tratados como valores livres fornecidos pelo cliente.

Para perfis não Estagiários, a participação não é obrigatória e o contrato específico será fechado nessa etapa.

Aprovação de Soluções deve validar atomicamente todos os componentes, preservando FIFO/FEFO, locks, lotes e regras de cancelamento/devolução.

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
Etapa 5 — Projetos + Atividades                       ✅ concluída, validada e mergeada
Etapa 6 — Estagiários + vínculos                      🔧 6.1–6.4 concluídos; 6.5 frontend implementado, validação pendente

**Massa DEV IQ de apoio ao 6.5:** `DataInitializer` passou a garantir cenários variados para `admin@sgl.com`/Unidade IQ: Estagiário operacional com múltiplas Atividades/Projetos/Laboratórios, vínculo prorrogado próximo do término, vínculo institucional sem Atividade e vínculo encerrado com histórico. A carga é idempotente e serve apenas à validação visual/funcional do 6.5; o fechamento formal de dados/testes continua no 6.6.
Etapa 7 — relatórios consolidados                     ⏳
Etapa 8 — unidades + Soluções + contexto operacional  ⏳
Etapa 9 — Pedidos + Soluções + participação           ⏳
Etapa 10 — Rótulos + impressão operacional            ⏳
Etapa 11 — Manual + decisão delete lógico             ⏳
Etapa 12 — testes automatizados frontend              ⏳
Etapa 13 — revisão estrutural e legibilidade           ⏳
```

Matriz de permissões, congelamento funcional e autenticação definitiva continuam posteriores ao bloco atual.

---

# 14. Regra final de retomada

**As Etapas 1–5 estão encerradas, validadas e integradas à main. Na Etapa 6, os blocos 6.1–6.4 estão concluídos e validados; o 6.5 — frontend integrado foi implementado e aguarda validação visual/local. Preservar a identidade estável Usuario → Estagiario, distinguir prorrogação da mesma bolsa de nova bolsa/novo VinculoEstagio e manter participação em Atividade como requisito operacional, não como identificador da ocorrência institucional. GitLab/main permanece a fonte canônica e GitHub/main seu espelho.**

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


### Refinamentos de apoio ao 6.5 — 02/10/2026

- `VinculoEstagioResponseDTO` passa a expor `dataFimPrevistaOriginal` quando há histórico de `PRORROGACAO`, derivado da primeira data prevista anterior registrada; isso permite ao frontend mostrar período original e `Prorrogado até ...` sem inventar dado.
- A massa DEV IQ ganhou um Estagiário com vínculo ainda `EM_ANDAMENTO`, mas apenas participações encerradas, para validar o estado visual `Sem atividade ativa` sem tratá-lo como `Não iniciado`.
- A massa DEV da Ana Costa passou a registrar histórico de prorrogação para exercitar a visualização do período original + nova data.


## Revisão híbrida de dados do vínculo — 02/10/2026

A decisão do 6.4 foi refinada para precedência **por campo**:

- identidade e dados pessoais institucionais continuam prioritariamente externos;
- Formação, Curso, Bolsa/modalidade, Orientador e período podem ser mantidos pelo SGL;
- quando a sincronização institucional enviar um desses campos, o valor institucional prevalece;
- ausência do campo na sincronização não apaga o valor local;
- Projeto/SCI/Atividade/Laboratório e participações continuam sendo domínio operacional do SGL;
- vínculos finalizados não podem ser editados pelo fluxo local comum.

Contrato local novo:

`PUT /api/v1/vinculos-estagio/{vinculoId}`

Permite editar Formação, Curso, Bolsa/modalidade, Orientador, data de início, data final prevista e observação. Aumento da data final prevista registra prorrogação local no histórico com origem `DEV`.

Regra obrigatória: todo novo vínculo deve possuir `dataInicio` e `dataFimPrevista`; não criar estágio sem término previsto.


## Pendência pós-Etapa 6 — Projetos ↔ Estagiários

Depois de concluir e validar a Etapa 6, revisar o hub de Projetos para que cada Atividade possa exibir os Estagiários relacionados por `VinculoEstagioAtividade`, ao lado/abaixo do responsável já mostrado atualmente. Não criar relação direta Projeto ↔ Estagiário; a associação deve continuar derivada da Atividade. A definição visual de participações atuais x históricas fica para essa rodada posterior.
