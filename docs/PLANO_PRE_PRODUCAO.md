# Plano de Pré-Produção do SGL

**Projeto:** Sistema de Gestão de Laboratórios (SGL)  
**Data de consolidação:** 04/09/2026  
**Última atualização:** 25/09/2026  
**Status:** Etapas 1, 2, 3 e 4 concluídas e validadas; Etapa 5 em andamento — 5.1 Projeto base ✅, 5.2 SCI ✅, bloco atual 5.3 Atividades  
**Fase:** pré-produção pós-aprovação funcional

Este documento é a referência canônica do bloco de pré-produção. As etapas devem ser executadas em sequência, respeitando dependências de domínio, backend e frontend.

> **Checkpoint de infraestrutura — 24/09/2026:** GitLab continua sendo a fonte canônica de `main` nos dois repositórios; GitHub é espelho de `main` e ponto de colaboração para `collab/*`. A Etapa 4 foi integrada. A branch atual é `collab/etapa-5-projetos-atividades`, criada a partir da `main` já contendo a Etapa 4.

Roadmap formal posterior:

```text
pré-produção atual
→ matriz de permissões
→ congelamento funcional
→ homologação integrada final
→ autenticação/autorização/auditoria definitiva
→ integração corporativa
→ produção
```

---

# 1. Regra de execução

Fluxo recomendado:

```text
analisar main atual
→ confirmar escopo
→ fechar domínio/contratos
→ implementar
→ validar
→ refinar
→ documentar
→ integrar
→ iniciar próxima etapa
```

## Regra especial para backend

Alterações funcionais de backend são implementadas **manualmente pelo responsável do projeto**.

A IA deve:

- analisar código existente;
- propor modelagem/regras;
- indicar migrations, entidades, DTOs, repositories, services e controllers;
- fornecer código de referência quando solicitado;
- revisar a implementação feita pelo usuário;
- não aplicar backend funcional sem autorização explícita.

Frontend/documentação podem ser alterados diretamente quando autorizado.

---

# 2. Sequência oficial

## Etapa 1 — Padronização e refinamento visual global ✅

**Status:** concluída e validada.

Objetivo: eliminar inconsistências de cards, botões, selects, filtros, ícones, espaçamentos, estados visuais e semântica de cores.

---

## Etapa 2 — Dark Mode definitivo ✅

**Status:** concluída e validada em 11/09/2026.

Consolidado:

- `themeService.ts` como fonte única;
- persistência de tema;
- tokens Dark;
- Vuetify/DOM sincronizados;
- telas autenticadas cobertas;
- Login, 404 e rótulos de impressão preservados claros;
- CSS legado de tema removido.

Frontend: PR #50, squash merge `a3fff4fa8edb6b8900c4a5b359dbfc0245afb87c`.

---

## Etapa 3 — Refinamentos do fluxo atual de Resíduos ✅

**Status:** concluída e validada em 17/09/2026.

### 3.1 Redundância de análise ✅

- pseudo-filtro `PENDENTES_ANALISE` removido;
- navegação duplicada removida;
- status reais preservados.

### 3.2 Dados, classificação, segurança e responsabilidade ✅

#### 3.2.1 Estado físico, tratamento e responsabilidade

- `EstadoFisicoResiduo`;
- tratamento realizado + descrição condicional;
- `gestorRecebedorInicial`;
- armazenamento/despacho podem ser feitos por outro Gestor sem apagar responsabilidade inicial;
- migration V13.

#### 3.2.2 Classes de Resíduo

- catálogo editável por Unidade;
- códigos/descrições/ativo;
- classes A, B, F e H iniciais;
- classes informadas x confirmadas;
- snapshot histórico em `ResiduoClasse`;
- migration V14.

#### 3.2.3 Segurança/EPI

Medidas estruturadas:

```text
LUVAS
OCULOS_PROTECAO
PROTECAO_RESPIRATORIA
JALECO_AVENTAL
OUTRO
```

- recomendações no Produto;
- segurança informada x confirmada;
- snapshots no Resíduo;
- `OUTRO` exige descrição;
- migration V15.

#### 3.2.4 Frontend

- Procedência/uso usando `processoOrigem`;
- estado físico;
- tratamento;
- Classes de Resíduo;
- EPI;
- sugestões vindas dos Produtos;
- confirmação da Gestão;
- comparativo em dois cards: informado x aprovado;
- exibição do Gestor que liberou e data/hora pelo histórico;
- escala do formulário revisada para uso em 100% de zoom.

### 3.3 Identificação, prévia e impressão ✅

Regra final:

```text
INFORMADO / EM_ANALISE
→ código SGL + QR existem
→ prévia disponível
→ impressão bloqueada

LIBERADO_PARA_ARMAZENAMENTO ou posterior
→ impressão permitida
```

Gerar/visualizar e permitir impressão são eventos distintos.

Template definitivo, Zebra e infraestrutura física continuam na Etapa 10.

Validação confirmou criação, análise, armazenamento, despacho, uso de Gestores diferentes, histórico, prévia, bloqueio/liberação de impressão e legibilidade.

Detalhes: `docs/CONTINUIDADE_ETAPA_3_2026-09-11.md`.

---

## Etapa 4 — Expansão operacional de Resíduos ✅ CONCLUÍDA E VALIDADA

**Impacto:** médio.

Handoff: `docs/CONTINUIDADE_ETAPA_4_2026-09-17.md`.

### 4.1 Locais de armazenamento cadastráveis

Criar locais reutilizáveis por Unidade, preservando flexibilidade.

Uso desejado:

```text
local cadastrado
+ complemento livre
```

Exemplo:

```text
Almoxarifado Químico
+ Prateleira B2
```

Também permitir texto manual quando necessário.

Antes de implementar, fechar:

- entidade/catálogo por Unidade;
- ativação/inativação;
- comportamento quando local for renomeado;
- necessidade de snapshot no Resíduo;
- combinação local + complemento.

### 4.2 Modelos de Resíduos pré-cadastrados

Criar definição reutilizável para resíduos recorrentes.

```text
ModeloResiduo = definição reutilizável
Residuo       = ocorrência operacional real
```

Modelo poderá sugerir/preencher:

- nome/descrição;
- procedência/uso;
- composição;
- Produtos/componentes;
- Classes;
- riscos;
- segurança/EPI;
- recipiente;
- tratamento padrão quando fizer sentido;
- demais dados reutilizáveis aprovados.

Regra central: alterar um modelo futuramente não modifica Resíduos históricos.

### 4.3 Uso pelo Solicitante

Ao informar:

```text
usar modelo pré-cadastrado
ou
preencher manualmente
```

O modelo preenche sugestões; o Resíduo real continua sendo uma ocorrência independente e sujeita à conferência da Gestão.

### 4.4 Correções administrativas do ciclo de vida

Necessidade levantada ao validar a Etapa 3.

Avaliar para Administrador:

```text
Cancelar Resíduo
→ motivo obrigatório
→ preservar registro/histórico

Retornar para análise/liberação
→ motivo obrigatório
→ preservar eventos anteriores
→ exigir nova validação
→ bloquear impressão novamente quando aplicável
```

Antes de implementar, definir:

- de quais status pode retornar;
- se `DESPACHADO` é irreversível;
- se haverá status `CANCELADO`;
- o que ocorre com dados já confirmados;
- se nova liberação cria novo evento preservando o anterior;
- efeito sobre rótulo/impressão;
- permissões exatas.

Isso **não é delete lógico**. A decisão geral de delete lógico permanece na Etapa 11.

---

## Etapa 5 — Projetos e Atividades 🔧 ATUAL

**Impacto:** alto.

### 5.0 Portão de confirmação ✅ FECHADO

Confirmado em 24/09/2026:

- Código SEG cadastrado e hierárquico: Projeto `XX.XX.XX.XXX.XX.00`, SCI `XX.XX.XX.XXX.XX.SS`, Atividade `XX.XX.XX.XXX.XX.SS.AAA`;
- `Projeto 1 → N SCI 1 → N Atividades`;
- SCI é entidade própria subordinada ao Projeto;
- Atividade é entidade própria subordinada ao SCI e possui status independente;
- Projeto: `ATIVO → ENCERRADO_COM_AVALIACAO_PENDENTE → CONCLUIDO`;
- coluna ambígua da planilha indica existência de recurso externo;
- Estagiário executa Atividade e possui Orientador PESQUISADOR ou ANALISTA.

Decisões adicionais fechadas:

- Projeto mantém vínculo com um Laboratório responsável/contextual, mas é o eixo operacional principal;
- Laboratório funciona principalmente como contexto/filtro para pessoas, Projetos e Atividades;
- Projeto/SCI/Atividade possuem Código SEG, título, líder/responsável, início, fim, duração opcional/derivável, status e situação de execução;
- "Figura = Projeto/SCI/Atividade" é atributo da planilha e não será persistido como campo genérico;
- recurso externo pertence ao Projeto e exige empresa quando marcado;
- dados pessoais de Estagiário devem vir preferencialmente de Usuario/autenticação institucional.

### 5.1 Projeto base ✅ CONCLUÍDO E VALIDADO

**Fechamento:** 25/09/2026.


Relação de domínio confirmada:

```text
Projeto 1 → N SCI
SCI 1 → N Atividades
```

Projeto mantém um Laboratório responsável/contextual (`Laboratório 1 → N Projetos`), mas Projeto é o eixo funcional para SCI, Atividades e vínculos. A interface não deve obrigar navegação pelo Laboratório para trabalhar com Projeto.

Planejado:

- título/descrição;
- Laboratório responsável/contextual;
- líder/responsável;
- início/fim;
- duração opcional/derivável;
- status principal;
- situação de execução;
- Código SEG;
- Código SGL/rastreabilidade interna;
- possui recurso externo;
- empresa do recurso externo quando aplicável.

Ciclo confirmado pelo cliente:

```text
ATIVO
→ ENCERRADO_COM_AVALIACAO_PENDENTE
→ CONCLUIDO
```

Fechamento técnico do 5.1:

- migration V19 aplicada sobre a tabela existente;
- enums `StatusProjeto` e `SituacaoExecucaoProjeto`;
- Entity, DTOs, Service e OpenAPI atualizados;
- `codigoSeg` cadastrado, sem validação hierárquica antecipada;
- recurso externo + empresa com regra condicional;
- compatibilidade com contratos antigos preservada;
- dados DEV/Demo atualizados;
- testes de Service, Controller e Repository ampliados;
- suíte completa JUnit verde;
- validação funcional do CRUD e tenant concluída;
- próxima migration livre: V20.

### 5.2 SCI ✅ CONCLUÍDO E VALIDADO

**Fechamento:** 25/09/2026.

```text
Projeto 1 → N SCI
```

Implementado e validado:

- V20 criou `scis` com FK obrigatória para Projeto;
- Laboratório/Unidade são derivados do Projeto;
- Código SEG próprio é armazenado no SCI;
- início/fim próprios ficam contidos no período do Projeto;
- status e situação de execução são persistidos independentemente;
- vínculo com Projeto não é trocado pelo update comum;
- recurso externo não é duplicado;
- duração permanece derivável;
- tenant permanece fail-closed;
- CRUD e consultas por Projeto estão documentados em OpenAPI;
- dados DEV/Demo e testes automatizados foram adicionados;
- suíte completa verde e bateria funcional concluída com sucesso;
- validação completa do Código SEG continua no 5.4.

Migrations V21, V22, V23 e V24 já estão ocupadas pela Etapa 5. Próxima migration livre: **V25**.

### 5.3 Atividades ✅ CONCLUÍDA

```text
SCI 1 → N Atividades
```

Atividade é entidade própria, obrigatoriamente subordinada ao SCI e, por consequência, ao Projeto. Possui ciclo operacional próprio e pode ser encerrada antes do Projeto.

Contrato de Atividade fechado e implementado na V21. O histórico de prorrogações foi concluído na V22; a validação SEG completa segue no 5.4.

Regras temporais/prorrogação já fechadas:

- Atividade fica temporalmente contida no SCI; SCI permanece contido no Projeto;
- prorrogações exigem justificativa e histórico;
- Projeto/SCI/Atividade encerrados não recebem prorrogação comum;
- para prorrogar SCI, Projeto precisa continuar aberto;
- para prorrogar Atividade, SCI e Projeto precisam continuar abertos;
- prorrogar um pai não prorroga automaticamente os filhos;
- a prorrogação do pai apenas amplia o limite permitido para eventual prorrogação posterior do filho;
- reduzir datas de um pai não pode invalidar filhos existentes;
- mudanças que quebrariam a hierarquia devem ser rejeitadas, não corrigidas por cascata automática.

Base de Atividade/V21 validada funcionalmente em 25/09/2026. V22 de prorrogações também concluída e validada por suíte automatizada.

Persistência aprovada para prorrogações:

- históricos separados para Projeto, SCI e Atividade, cada um com FK real;
- preservar data final anterior, nova data final, justificativa, usuário e data/hora;
- aumento de uma data final já existente só ocorre pelo fluxo explícito de prorrogação;
- data + histórico são gravados atomicamente;
- endpoints próprios de prorrogação serão expostos para Projeto, SCI e Atividade;
- V21 fica dedicada à tabela de Atividades;
- V22 fica reservada aos históricos de prorrogação.

Autoria nesta fase de pré-autenticação:

- manter FK para `Usuario` nos históricos;
- enquanto não houver principal autenticado no backend, aceitar provisoriamente o UUID do operador no fluxo, validando tenant, usuário ativo e perfil;
- não tratar esse mecanismo como autenticação definitiva;
- substituir o UUID fornecido pelo cliente pelo usuário obtido do contexto autenticado quando a autenticação real for implementada.

V22 foi considerada concluída por validação automatizada em 25/09/2026; a bateria manual via Postman foi deliberadamente dispensada nesta rodada.

### 5.4 Código SEG — validação hierárquica 🧪 VALIDAÇÃO FINAL

Formato institucional confirmado:

```text
Projeto   XX.XX.XX.XXX.XX.00
SCI       XX.XX.XX.XXX.XX.SS
Atividade XX.XX.XX.XXX.XX.SS.AAA
```

O Código SEG será cadastrado, não gerado automaticamente nesta versão. O SGL validará formato, coerência com o pai e **unicidade global/institucional**, independentemente da Unidade e inclusive para registros inativos.

Subblocos do 5.4:

- **5.4.1 Formato e coerência hierárquica ✅** — validação de Projeto, SCI e Atividade conforme o padrão institucional;
- **5.4.2 Unicidade global ✅** — V23 + validação de Service, sem reutilização de Código SEG mesmo após inativação;
- **5.4.3 Imutabilidade no CRUD comum ✅** — após definido, o Código SEG não pode ser trocado por edição comum; Projeto legado sem código pode receber a primeira definição;
- **5.4.4 Correção administrativa de Código SEG ✅** — V24 + fluxo auditável e transacional para corrigir erros humanos sem liberar alteração comum perigosa;
- **5.4.5 Testes e fechamento 🧪** — testes automatizados implementados; resta confirmar a suíte completa verde.

A correção administrativa foi implementada pela `V24__create_seg_correction_history.sql` e por endpoints próprios em Projeto, SCI e Atividade. Exige justificativa e autoria, registra valor anterior/novo e data/hora, valida formato/hierarquia/unicidade e opera de forma transacional. Correção de Projeto propaga apenas o prefixo coerente aos SCI/Atividades descendentes, preservando seus sufixos; correção de SCI faz o mesmo com suas Atividades. Colisões bloqueiam toda a operação antes de qualquer mutação.

A correção pode ser aplicada a registros encerrados ou inativos, porque corrige o identificador institucional sem reabrir o ciclo de vida. Alteração direta no banco não é fluxo funcional de produção. Pode existir apenas como manutenção excepcional em DEV/pré-produção; em produção, a alternativa oficial para erro de digitação será o fluxo administrativo auditável.

### 5.5 Interface e integração

Só fechar depois de Projeto → SCI → Atividade estabilizados no backend.

A navegação deve dar protagonismo ao Projeto. Laboratório continua vínculo/contexto e filtro, mas não deve ser uma etapa obrigatória de navegação para trabalhar com Projeto.

---

## Etapa 6 — Estagiários e vínculos

**Dependência:** Etapa 5.

Confirmado para a Etapa 6:

- Orientador obrigatório e com perfil PESQUISADOR ou ANALISTA;
- Atividade obrigatória; Projeto é derivado de Atividade → SCI → Projeto;
- Estagiário pode migrar entre Atividades/Projetos, exigindo histórico de vínculos;
- Cultura representa a cultura da pesquisa e deve ser tratada como catálogo administrável por Unidade;
- Bolsa/vínculo separado de Curso/Formação/nível;
- início/fim do estágio;
- prorrogações com histórico;
- situação: EM_ANDAMENTO, FINALIZADO ou PRORROGADO;
- treinamento de segurança booleano;
- dados pessoais devem vir preferencialmente de Usuario/autenticação institucional;
- código interno SGL do vínculo pode ser adotado sem substituir identidade institucional.

---

## Etapa 7 — Relatórios consolidados

**Dependência:** Etapas 5 e 6.

### 7.1 Filtros/dimensões

Laboratório, responsável, Projeto, Código SEG, líder, Atividade, Orientador, Bolsa, Curso, Cultura, situação e período.

### 7.2 Consultas/agregações

Contagens por Laboratório, Orientador, responsável, Bolsa, Curso, Cultura, Projeto e Atividade.

### 7.3 Prévia/telas

Visões consolidadas de Laboratórios, Projetos e Estagiários.

### 7.4 PDF/XLSX

Mesma consulta/filtros da tela.

### 7.5 Organização estrutural

Mover controllers/services/DTOs de Relatórios para packages específicos, sem alterar contratos ou comportamento.

---

## Etapa 8 — Unidades e Soluções

Ordem:

```text
normalização de unidades/apresentações
→ domínio de Soluções
→ composição/regras
→ interface/contrato estabilizados
```

### 8.1 Unidades

Separar:

```text
unidade de medida
≠
apresentação física
```

Conversões válidas:

```text
1 L = 1000 mL
1 kg = 1000 g
```

Não converter `g ↔ mL` sem densidade apropriada.

### 8.2 Soluções

Solução = receita/composição reutilizável de Produtos.

### 8.3 Fechamento do contrato

Estabilizar DTOs, composição, unidades, validações, edição/inativação, interface e histórico/snapshot antes da Etapa 9.

---

## Etapa 9 — Pedidos + Soluções

**Dependência:** Etapa 8.

Antes de alterar Pedido, confirmar se o fluxo atual será mantido ou se há mudanças adicionais solicitadas pelo cliente.

Pedido poderá conter Produto, Solução ou ambos, conforme escopo final.

Aprovação de Solução deve ser atômica: se qualquer componente for insuficiente, não aprovar parcialmente.

Preservar FIFO/FEFO, locks, lotes consumidos e regra de cancelamento/devolução.

---

## Etapa 10 — Rótulos e impressão operacional

Definir padrão-base SGL e templates adaptados de:

- Produto;
- Resíduo;
- Solução.

Também incluir Documento de Auditoria de Entrada de Lote, identificado como:

```text
DOCUMENTO INTERNO DO SGL
SEM VALOR FISCAL
```

Depois validar Zebra, dimensões, orientação, margens, driver/envio, ZPL e testes físicos.

---

## Etapa 11 — Manual do Usuário + avaliação de delete lógico

### 11.1 Manual

Disponibilizar materiais institucionais e de uso do SGL.

Definir contrato e armazenamento antes de criar upload/download permanente.

### 11.2 Delete lógico

Avaliar entidade por entidade no fim das alterações funcionais.

Não usar `ativo` indiscriminadamente em entidades que já possuem ciclo de vida próprio.

---

## Etapa 12 — Testes automatizados do Frontend

Stack:

```text
Vitest + Vue Test Utils
Cypress
```

Cobrir fluxos críticos finais, execução headless e scripts reproduzíveis.

---

## Etapa 13 — Revisão estrutural e legibilidade

Última etapa técnica.

Revisar especialmente:

- `Residuo`;
- Services grandes;
- DTOs extensos;
- Controllers concentrados;
- métodos longos;
- organização de packages;
- documentação de snapshots/invariantes.

Critério:

```text
sem mudança funcional intencional
+ contratos preservados
+ migrations preservadas
+ testes da Etapa 12 reexecutados
```

---

# 3. Dependências principais

```text
Etapa 1 → Etapa 2 → Etapa 3 → Etapa 4

Etapa 5 Projeto/Atividade
→ Etapa 6 Estagiários/vínculos
→ Etapa 7 Relatórios

Etapa 8 Unidades/Soluções
→ Etapa 9 Pedidos/Soluções

Produto + Resíduo + Solução estabilizados
→ Etapa 10 Rótulos

Etapas 1–11 estabilizadas
→ Etapa 12 testes
→ Etapa 13 refactor final
```

---

# 4. Estado de execução

```text
Limpeza/revisão documental                         ✅
Planejamento de pré-produção                        ✅
Etapa 1 — refinamento visual global                 ✅
Etapa 2 — Dark Mode                                 ✅
Etapa 3 — refinamentos de Resíduos                  ✅ concluída e validada
Etapa 4 — expansão operacional de Resíduos          ✅ concluída e validada
Etapa 5 — Projetos e Atividades                     🔧 atual — 5.3 Atividades; 5.1 ✅; 5.2 ✅
Etapas 6–13                                         ⏳
```

A matriz de permissões não é a próxima tarefa enquanto este bloco estiver aberto.

---

# 5. Regra de continuidade

Ao encerrar cada etapa, registrar:

```text
status
→ decisões
→ alterações
→ validações
→ pendências
→ próxima etapa
```

Nova necessidade deve ser posicionada neste roadmap antes da implementação.

Handoff atual: `docs/CONTINUIDADE_ETAPA_5_2026-09-24.md`.
