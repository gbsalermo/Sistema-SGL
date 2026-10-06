# Plano de Pré-Produção do SGL

**Projeto:** Sistema de Gestão de Laboratórios (SGL)  
**Data de consolidação:** 04/09/2026  
**Última atualização:** 01/10/2026  
**Status:** Etapas 1–6 concluídas e validadas; Etapa 7 — Unidades + Soluções + contexto operacional em andamento
**Fase:** pré-produção pós-aprovação funcional

Este documento é a referência canônica do bloco de pré-produção. As etapas devem ser executadas em sequência, respeitando dependências de domínio, backend e frontend.

> **Checkpoint de infraestrutura — 01/10/2026:** GitLab continua sendo a fonte canônica de `main`; GitHub é espelho de `main` e ponto de colaboração para `collab/*`. A branch atual do backend é `collab/etapa-7-unidades-solucoes-contexto`, criada sobre a `main` pós-Etapa 5.

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

## Etapa 5 — Projetos e Atividades ✅ CONCLUÍDA E VALIDADA

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

### 5.4 Código SEG — validação hierárquica ✅ CONCLUÍDA

Formato institucional confirmado:

```text
Projeto   XX.XX.XX.XXX.XX.00
SCI       XX.XX.XX.XXX.XX.SS
Atividade XX.XX.XX.XXX.XX.SS.AAA
```

O Código SEG continua sendo um dado institucional cadastrado. O SGL não impõe sequência automática como regra: valida formato, coerência com o pai e **unicidade global/institucional**. Para facilitar o cadastro, a interface sugere o próximo sufixo provável de SCI/Atividade, mas o gestor pode substituí-lo por outro código institucional válido antes de salvar.

Subblocos do 5.4:

- **5.4.1 Formato e coerência hierárquica ✅** — validação de Projeto, SCI e Atividade conforme o padrão institucional;
- **5.4.2 Unicidade global ✅** — V23 + validação de Service, sem reutilização de Código SEG mesmo após inativação;
- **5.4.3 Imutabilidade no CRUD comum ✅** — após definido, o Código SEG não pode ser trocado por edição comum; Projeto legado sem código pode receber a primeira definição;
- **5.4.4 Correção administrativa de Código SEG ✅** — V24 + fluxo auditável e transacional para corrigir erros humanos sem liberar alteração comum perigosa;
- **5.4.5 Testes e fechamento ✅** — suíte automatizada completa confirmada verde em 25/09/2026.

A correção administrativa foi implementada pela `V24__create_seg_correction_history.sql` e por endpoints próprios em Projeto, SCI e Atividade. Exige justificativa e autoria, registra valor anterior/novo e data/hora, valida formato/hierarquia/unicidade e opera de forma transacional. Correção de Projeto propaga apenas o prefixo coerente aos SCI/Atividades descendentes, preservando seus sufixos; correção de SCI faz o mesmo com suas Atividades. Colisões bloqueiam toda a operação antes de qualquer mutação.

A correção pode ser aplicada a registros encerrados ou inativos, porque corrige o identificador institucional sem reabrir o ciclo de vida. Alteração direta no banco não é fluxo funcional de produção. Pode existir apenas como manutenção excepcional em DEV/pré-produção; em produção, a alternativa oficial para erro de digitação será o fluxo administrativo auditável.

Fechamento do 5.4:

- V23 protege unicidade global/institucional do Código SEG;
- V24 registra correções administrativas auditáveis;
- Código SEG é imutável no CRUD comum após definido;
- Projeto legado sem Código SEG pode receber a primeira definição;
- correções de Projeto e SCI propagam prefixos aos descendentes preservando sufixos;
- colisões impedem toda a transação;
- correção exige justificativa, tenant e operador com perfil permitido;
- registros encerrados/inativos podem ser corrigidos sem reabrir ciclo de vida;
- suíte JUnit completa confirmada verde em 25/09/2026;
- validação manual adicional em Postman não foi exigida para o fechamento deste bloco.

### 5.5 Interface e integração ✅ CONCLUÍDA E VALIDADA

Projeto → SCI → Atividade já está estabilizado no backend e a implementação principal do frontend foi concluída.

Implementado:

- rota operacional `/projetos` para Gestão/Administração;
- navegação direta por Projeto sem passagem obrigatória por Laboratório;
- listagem, busca, filtro e resumo operacional;
- visualização hierárquica Projeto → SCI → Atividade;
- cadastro/edição de SCI e Atividade;
- formulário administrativo de Projeto alinhado ao domínio atual;
- prorrogação explícita nos três níveis;
- correção administrativa de Código SEG nos três níveis;
- histórico visual de prorrogações e correções;
- massa DEV idempotente para validação da interface.

Sugestão assistida no 5.5:

- Novo SCI recebe sugestão `maior sufixo já utilizado + 1` a partir da raiz do Projeto;
- Nova Atividade recebe sugestão equivalente a partir do Código SEG do SCI;
- a sugestão considera registros ativos e inativos;
- o campo permanece editável antes da criação;
- códigos avulsos continuam permitidos desde que atendam formato, hierarquia e unicidade.

Fechamento confirmado em 29/09/2026 após validação funcional/visual da interface integrada e confirmação da suíte JUnit do backend verde.

Validado:

- hub `/projetos` carregando dados reais do backend DEV;
- filtros, incluindo Laboratório;
- hierarquia Projeto → SCI → Atividade;
- criação/edição de SCI e Atividade;
- prorrogação, correção SEG e histórico;
- primeira definição de SEG de Projeto via Cadastros;
- sugestão editável de SEG para SCI/Atividade;
- dark mode, tipografia e hierarquia visual SCI/Atividade;
- massa DEV idempotente;
- refinamentos finais de usabilidade.

**Etapa 5 encerrada. Etapa 6 liberada.**

---

## Etapa 6 — Estagiários e vínculos

**Dependência:** Etapa 5.

Confirmado para a Etapa 6:

- Orientador obrigatório e com perfil PESQUISADOR ou ANALISTA;
- Atividade obrigatória; Projeto é derivado de Atividade → SCI → Projeto;
- Estagiário pode participar de múltiplas Atividades no mesmo vínculo, com histórico preservado;
- o mesmo Estagiário pode receber novo vínculo institucional após finalizar o anterior;
- não podem existir dois vínculos de estágio não finalizados simultaneamente para o mesmo Estagiário;
- no fluxo local/DEV, o vínculo nasce com Atividade inicial; vínculo sincronizado do ambiente pode existir sem Atividade até receber contexto operacional;
- Estagiário operacionalmente ativo exige Usuario ativo + vínculo não finalizado + participação de Atividade aberta;
- finalizar estágio não desativa o Usuario;
- Cultura representa a cultura da pesquisa e deve ser tratada como catálogo administrável por Unidade;
- Bolsa/vínculo separado de Curso/Formação/nível;
- Formação é enum controlado com opção `OUTRO` + descrição específica;
- Curso é catálogo administrável por Unidade, não enum rígido;
- treinamento de segurança nasce `false` e só é concluído por ação específica;
- `Usuario.laboratorio` não é fonte operacional de laboratório para Estagiários;
- o contexto operacional do Estagiário é sua participação em Atividade, da qual Projeto/Laboratório são derivados;
- início/fim do estágio;
- prorrogações com histórico;
- situação: EM_ANDAMENTO, FINALIZADO ou PRORROGADO;
- treinamento de segurança booleano;
- dados pessoais devem vir preferencialmente de Usuario/autenticação institucional;
- não criar Código SGL adicional para Estagiário; identidade/matrícula virá da fonte institucional.

### 6.1 — contrato e fundação do vínculo ✅

Concluído e validado em 30/09/2026. Consolidou `Usuario → Estagiario → VinculoEstagio 1..N`, orientador, situação própria e V25/V26.

### 6.2 — vínculos múltiplos Estagiário ↔ Atividade + histórico ✅

Concluído e validado em 01/10/2026.

- V27 criou `vinculo_estagio_atividade`;
- múltiplas participações simultâneas são permitidas no mesmo vínculo;
- histórico de participações encerradas é preservado;
- a última participação aberta não pode ser encerrada isoladamente enquanto o vínculo estiver em andamento;
- no fluxo local/DEV, novo vínculo exige Atividade inicial; na integração institucional futura, o vínculo pode chegar antes da Atividade e só se torna operacionalmente ativo após uma participação aberta;
- respostas retornam as participações com Atividade/SCI/Projeto/Laboratório derivados;
- listagem/estado operacional ativo incorporam participação aberta;
- testes de Service, Controller e Repository/H2 foram adicionados;
- suíte backend completa e compilação foram executadas sem erros/falhas em 01/10/2026.

### 6.3 — dados acadêmicos, Curso, Cultura e treinamento de segurança ✅

Concluído e validado em 01/10/2026.

- V28 implementou Formação, Curso e treinamento de segurança;
- `FormacaoEstagiario` usa valores controlados + `OUTRO`;
- `Curso` é catálogo por Unidade com inativação lógica;
- `VinculoEstagio` recebe Formação, Curso e `treinamentoSegurancaConcluido`;
- treinamento nasce `false` e possui ação explícita de conclusão;
- V29 implementou catálogo/API de Cultura e associação explícita com participação;
- atualização de Culturas é idempotente por conjunto;
- Cultura inativa permanece em histórico existente, mas não entra em nova associação;
- criação de participação, novo vínculo e primeiro cadastro de Estagiário aceitam Culturas;
- respostas diretas e aninhadas retornam Culturas de forma consistente;
- compilação backend concluída e suíte completa verde em 01/10/2026.

Checkpoint: `docs/VALIDACAO_ETAPA_6_3.md`.

A Etapa 6 também fechou uma decisão estrutural para etapas futuras: **não existe necessariamente um único Laboratório do Estagiário**. A participação `VinculoEstagioAtividade` passa a ser o contexto operacional que futuramente alimentará Pedidos.

```text
Estagiario
→ VinculoEstagio
→ VinculoEstagioAtividade
→ Atividade
→ SCI
→ Projeto
→ Laboratorio
```

No bloco 6.5, a interface deve remover a apresentação de um único "Laboratório do Estagiário" como atributo definitivo e exibir Atividade/Projeto/Laboratório por participação.

Decisão detalhada: `docs/DECISAO_CONTEXTO_OPERACIONAL_ESTAGIARIO_PEDIDOS.md`.

Próximo bloco: **6.4 — período, situação, prorrogações e finalização**.

### 6.4 — ciclo institucional e sincronização ✅ concluído e validado em 02/10/2026

A fonte de verdade do ciclo de bolsa/estágio é o ambiente institucional. O SGL espelha o vínculo e executa consequências operacionais.

Divisão operacional:

```text
6.4.1 contrato + histórico da sincronização institucional ✅
→ 6.4.2 sincronização do vínculo existente ✅ concluído e validado em 02/10/2026
   → 6.4.2A DTOs/contrato ✅
   → 6.4.2B regra transacional ✅
   → 6.4.2C endpoint + testes ✅ suíte local verde
   → prorrogação/finalização derivadas do estado recebido
→ 6.4.3 nova bolsa / novo VinculoEstagio ✅ suíte local verde
→ 6.4.4 testes, validação e documentação ✅ concluído
```

Regras já fechadas:

- matrícula/CPF identifica a pessoa; senha é somente credencial;
- `Usuario` e `Estagiario` permanecem os mesmos ao longo de diferentes bolsas;
- `VinculoEstagio` representa uma ocorrência institucional;
- prorrogação da mesma bolsa mantém o mesmo vínculo e amplia `dataFimPrevista`;
- nova bolsa após o término cria novo vínculo para o mesmo Estagiário;
- `dataInicio` do vínculo sincronizado não deve ser reescrita silenciosamente;
- `dataFimEfetiva` é preenchida quando a fonte institucional informa encerramento;
- o SGL não disponibiliza encerramento/prorrogação manual como fluxo cotidiano;
- alterações institucionais devem ser auditadas com valores anteriores/novos, origem e data/hora;
- finalização recebida do ambiente encerra participações abertas, mas não desativa o Usuario;
- Usuario institucionalmente inativo perde acesso operacional sem que isso, isoladamente, finalize o vínculo;
- novo vínculo vindo do ambiente pode existir inicialmente sem Atividade; só fica operacionalmente ativo após participação aberta;
- CPF/matrícula não identifica uma bolsa específica; quando o ambiente fornecer identificador próprio do vínculo, ele deve ser usado como referência de sincronização;
- enquanto o contrato externo não confirmar seu escopo, `referenciaInstitucional` permanece indexada, porém sem unicidade global;
- idempotência de eventos é independente da referência do vínculo e usa `origem + referenciaEvento` quando a fonte disponibilizar identificador próprio do evento;
- V30 cria a fundação desse histórico institucional;
- V31 remove a unicidade global inicialmente assumida para `referenciaInstitucional`, preservando compatibilidade com bancos onde a V30 já tenha sido aplicada;
- bloco 6.4 concluído e validado em 02/10/2026; checkpoint: `docs/VALIDACAO_ETAPA_6_4.md`.

Decisão detalhada: `docs/DECISAO_CICLO_INSTITUCIONAL_ESTAGIO.md`.

---

### 6.5 — refinamento final do drawer 🔧

Na validação visual/funcional do 6.5, o drawer foi reorganizado para priorizar operação e legibilidade:

- Ações operacionais movidas para o topo;
- `BOLSA / VÍNCULO` concentra bolsa vigente/último vínculo e histórico de vínculos;
- seção isolada de Segurança removida;
- treinamento de segurança aparece nas Ações operacionais, exige confirmação no frontend e aceita reversão auditável;
- observações do treinamento entram no bloco de Observações;
- Gestor/Admin pode adicionar observações operacionais sem sobrescrever registros anteriores;
- Participações em Atividades receberam escala tipográfica maior;
- Gerenciar Culturas permite criar nova Cultura ativa da Unidade e selecioná-la para posterior associação;
- V32 sustenta observações e auditoria de treinamento.

Validação manual e automática desta rodada permanece para o 6.6.

---

## Refinamento pós-Etapa 6 — Integração visual Projetos ↔ Estagiários

Após o fechamento da Etapa 6, revisar a interface de Projetos para aproveitar o vínculo operacional já existente:

```text
Projeto
→ SCI
→ Atividade
→ VinculoEstagioAtividade
→ Estagiário
```

Objetivo visual:

- manter o responsável da Atividade, como já ocorre hoje;
- exibir também os Estagiários associados àquela Atividade;
- não criar FK direta Projeto ↔ Estagiário;
- derivar a relação pelas participações em Atividade já existentes;
- decidir no momento da implementação como diferenciar participações atuais e históricas sem poluir a hierarquia Projeto → SCI → Atividade.

Esse refinamento não faz parte do fechamento da Etapa 6 e não deve atrasar sua validação atual.

---

## Etapa 7 — Unidades + Soluções + contexto operacional 🔧 ATUAL

**Dependência:** Etapas 5 e 6 estabilizadas.

### 7.0 — auditoria do modelo atual

Antes de migration ou contrato novo, revisar:

- `UnidadeMedida`;
- `TipoEmbalagem`;
- Produto;
- EstoqueCentral;
- Lote;
- ItemPedido;
- Pedido;
- movimentações;
- telas e DTOs que exibem/recebem quantidade.

Achados iniciais já confirmados:

- `UnidadeMedida` mistura unidade física e apresentação;
- estoque e pedido usam quantidades inteiras;
- não existe domínio `Solucao` no backend atual.

### 7.1 — normalização de unidade x apresentação

Regra:

```text
unidade de medida
≠
apresentação física
```

Exemplos de dimensão/unidade:

```text
VOLUME: mL, L
MASSA: mg, g, kg
COMPRIMENTO: m
CONTAGEM: unidade
```

Apresentação física permanece separada, por exemplo:

```text
frasco
ampola
caixa
kit
par
```

Conversão automática somente dentro da mesma dimensão.

### 7.2 — modelo de quantidade e compatibilidade

Definir antes de alterar schema:

- unidade canônica por Produto;
- precisão decimal necessária;
- estratégia para dados legados;
- impacto em estoque, lotes, pedidos e movimentações;
- como multiplicadores de embalagem convivem com unidade física.

### 7.3 — domínio de Soluções

Modelar Solução como composição reutilizável, distinta de Produto.

A modelagem deve definir:

- identidade e status;
- Unidade/tenant;
- nome/descrição;
- componentes Produtos;
- quantidade e unidade de cada componente;
- rendimento/volume ou massa final quando aplicável;
- preparo/instruções quando necessário;
- histórico/snapshot suficiente para Pedidos.

### 7.4 — contexto operacional

Fechar o contrato que será consumido pela Etapa 8:

Para Estagiários:

```text
Pedido
→ VinculoEstagioAtividade
→ Atividade
→ SCI
→ Projeto
→ Laboratório
→ Unidade
```

Para TECNICO/ANALISTA/PESQUISADOR:

- com Projeto: Projeto determina Laboratório;
- sem Projeto: contexto-base pode continuar vindo de `Usuario.laboratorio`;
- o frontend não deve oferecer Projeto e Laboratório como escolhas independentes quando Projeto já determina Laboratório.

### 7.5 — frontend e validação

Após estabilizar contratos:

- atualizar cadastros/estoque onde necessário;
- preparar UI de Soluções;
- validar dados legados;
- ampliar testes backend;
- preservar relatórios atuais sem consolidá-los ainda.

---


## Etapa 8 — Unidades, Soluções e contexto operacional

Ordem:

```text
normalização de unidades/apresentações
→ domínio de Soluções
→ composição/regras
→ contrato de contexto operacional do solicitante
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

### 8.4 Contexto operacional dos solicitantes para Pedidos

Fechar antes da evolução de Pedido:

- Unidade/tenant permanece derivada da identidade/sessão institucional;
- `Usuario.laboratorio` deixa de ser considerado fonte operacional para Estagiários;
- um vínculo pode possuir participações em Atividades de Projetos/Laboratórios diferentes dentro da mesma Unidade;
- para Estagiários, o frontend trabalha com as participações abertas; uma única pode ser pré-selecionada e múltiplas exigem escolha explícita;
- para Estagiários, Projeto e Laboratório são derivados da participação;
- para TECNICO/ANALISTA/PESQUISADOR com Projeto, o usuário escolhe o Projeto e o backend deriva o Laboratório;
- para TECNICO/ANALISTA/PESQUISADOR sem Projeto, `Usuario.laboratorio` permanece como contexto institucional/base;
- o frontend não deve permitir Projeto e Laboratório independentes quando o Projeto já determina seu Laboratório;
- `laboratorioId/laboratorioNome` da sessão DEV permanecem apenas por compatibilidade até a migração do fluxo.

Essa decisão prepara a Etapa 9 sem antecipar a alteração funcional de Pedido.

---

## Etapa 9 — Pedidos + Soluções + contexto de participação

**Dependência:** Etapa 8.

A evolução de Pedido deve incorporar duas frentes: Soluções e contexto operacional do Estagiário.

Para Estagiários, a referência operacional do Pedido passa a ser `VinculoEstagioAtividade`:

```text
Pedido
→ participação escolhida
→ Atividade
→ SCI
→ Projeto
→ Laboratorio
→ Unidade
```

Regras:

- o Estagiário escolhe entre suas participações abertas;
- o frontend não combina livremente Atividade, Projeto e Laboratório;
- o backend deriva e valida Projeto/Laboratório/Unidade;
- a associação com a participação preserva o contexto histórico do Pedido;
- os FKs atuais de Projeto/Laboratório podem permanecer por compatibilidade, consulta e rastreabilidade, mas para Estagiários devem ser preenchidos/validados a partir da participação;
- Solicitante e Gestão leem o mesmo Pedido; não existem cópias independentes a sincronizar;
- para perfis não Estagiários, `VinculoEstagioAtividade` não é obrigatório e o contrato específico será fechado na etapa.

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
→ Etapa 7 Unidades/Soluções/contexto
→ Etapa 8 Pedidos/Soluções
→ Etapa 9 Relatórios

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
Etapa 5 — Projetos e Atividades                     ✅ concluída e validada
Etapa 6 — Estagiários e vínculos                    ✅ concluída e validada
Etapa 7 — Unidades + Soluções + contexto            🔧 atual
Etapas 8–13                                         ⏳
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

Fechamento da Etapa 5: `docs/VALIDACAO_ETAPA_5.md`. Checkpoint validado atual: `docs/VALIDACAO_ETAPA_6_4.md`. Bloco atual: 7.0 — auditoria do modelo de unidades, apresentações, quantidades e Soluções.


### Revisão híbrida 02/10/2026 — Etapa 6

- ambiente institucional permanece prioritário para identidade/estado institucional;
- dados de vínculo podem ser complementados/editados no SGL;
- precedência é por campo: valor institucional substitui o local somente quando o campo é recebido;
- Formação, Curso, Bolsa/modalidade, Orientador e período são editáveis localmente;
- Projeto/SCI/Atividade/Laboratório permanecem operacionais do SGL;
- `dataFimPrevista` passa a ser obrigatória em todos os novos vínculos;
- edição local de período não finaliza automaticamente o estágio;
- aumento de prazo local é auditado como prorrogação.

#### Ajuste de interface do 6.5 — bolsa e prazo de Atividade — 02/10/2026

No drawer de Estagiários, Bolsa deixa de ser um campo livre do `Editar vínculo`. A troca manual é um fallback específico que encerra a ocorrência atual e cria um novo `VinculoEstagio`, mantendo o histórico; o ambiente institucional continua autoritativo. Para participações, a data final visual/operacional usa `Atividade.dataFim` e, quando ela não existir, `Projeto.dataFim` como fallback. O drawer também oferece encerramento explícito de atividade/participação.
