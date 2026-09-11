# Plano de Pré-Produção do SGL

**Projeto:** Sistema de Gestão de Laboratórios (SGL)  
**Data de consolidação:** 04/09/2026  
**Status:** execução em andamento — Etapa 3 atual  
**Fase:** pré-produção pós-aprovação funcional  
**Fonte:** observações e decisões levantadas durante a apresentação com o cliente

Este documento é a referência canônica do bloco atual de pré-produção. Ele organiza as melhorias do menor para o maior impacto, respeitando dependências entre domínio, backend e frontend.

O roadmap formal de produção continua posterior a este bloco:

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

## 1. Regra de execução

As etapas devem ser executadas uma por vez. Antes de iniciar a etapa seguinte, a etapa atual deve estar implementada, revisada e estabilizada.

Fluxo recomendado:

```text
analisar a main atual
→ confirmar escopo da etapa
→ definir alterações de domínio/contrato
→ implementar
→ validar
→ refinar
→ atualizar documentação afetada
→ avançar para a próxima etapa
```

### Regra especial para backend

Alterações de backend serão implementadas **manualmente pelo responsável do projeto**. O apoio de IA deve:

- analisar o backend existente;
- definir a modelagem e as regras;
- indicar migrations, entidades, DTOs, repositories, services, controllers e testes necessários;
- revisar posteriormente a implementação realizada;
- não aplicar diretamente código funcional de backend sem nova decisão explícita.

Alterações apenas de frontend/documentação podem ser implementadas diretamente quando solicitado.

---

# 2. Sequência oficial das etapas

## Etapa 1 — Padronização e refinamento visual global

**Status:** ✅ concluída e validada  
**Impacto:** baixo  
**Origem:** item 1

Objetivo: remover inconsistências visuais sem alterar regra de negócio.

Abrange:

- cards;
- ícones;
- botões;
- setas;
- selects;
- filtros e seus símbolos;
- alinhamentos;
- espaçamentos;
- centralização de textos;
- estados visuais;
- chips/status;
- cores desbotadas ou fora do padrão.

Resultado esperado: componentes e telas autenticadas passam a seguir um padrão visual único, servindo de base para o Dark Mode e para novas funcionalidades.

---

## Etapa 2 — Dark Mode definitivo

**Status:** ✅ concluída e validada em 11/09/2026  
**Impacto:** baixo/médio  
**Origem:** item 2

Sequência obrigatória:

```text
esboço
→ paleta de cores
→ regras de comportamento
→ tokens/componentes
→ aplicação nas telas autenticadas
→ revisão tela a tela
→ testes
```

Regras:

- o Dark Mode deve seguir uma paleta definida, e não ajustes isolados por tela;
- componentes equivalentes devem reagir ao tema da mesma forma;
- a tela de login permanece fora do tema das interfaces autenticadas, salvo decisão futura explícita.

Fechamento realizado:

- fonte única de tema em `themeService.ts`;
- persistência de `sgl.theme`;
- DOM/body e Vuetify sincronizados;
- paleta escura consolidada em tokens;
- remoção das quatro camadas provisórias/legadas de Dark Mode;
- Login, 404 e rótulos de impressão mantidos claros;
- Gestão e Solicitante validados em Light/Dark;
- ações primárias no Dark ajustadas para azul menos luminoso;
- Relatórios, Resíduos, Perfil e Cadastros corrigidos para eliminar superfícies claras residuais;
- semântica de movimentações preservada também nos Relatórios:
  - Entrada/Devolução → verde;
  - Saída → azul;
  - Ajuste → âmbar;
  - Descarte por vencimento → vermelho.

Frontend: PR #50, squash merge `a3fff4fa8edb6b8900c4a5b359dbfc0245afb87c`.

---

## Etapa 3 — Refinamentos do fluxo atual de Resíduos

**Impacto:** médio  
**Origem:** itens 3, 13 e 14 + novas solicitações do cliente consolidadas em 11/09/2026

### 3.1 Remover redundância de análise

Retirar a sequência visual de “pendências de análise” quando ela apenas repetir as ações já representadas por recebimento e análise.

A remoção deve considerar também links/filtros vindos do Dashboard para não deixar navegação quebrada.

### 3.2 Ampliar dados, classificação, segurança e responsabilidade do Resíduo

Esta subetapa incorpora as novas informações solicitadas pelo cliente ao fluxo atual.

#### Procedência / uso

O campo atual `processoOrigem` será mantido como fonte da informação de **procedência/uso do Resíduo**.

Não criar campo redundante apenas para “procedência”.

Na interface, o significado deve ficar explícito, por exemplo:

```text
Procedência / uso do Resíduo
→ informe de qual processo, atividade ou uso surgiu o Resíduo
```

O mesmo dado deve aparecer no rótulo com nomenclatura clara para o usuário.

#### Tratamento realizado

Ao informar um Resíduo, o Solicitante deve indicar se já foi realizado algum tratamento.

Regra:

```text
tratamento realizado = não
→ nenhuma descrição adicional obrigatória

tratamento realizado = sim
→ descrição do tratamento realizado obrigatória
```

A informação deve permanecer vinculada à ocorrência real do Resíduo e ficar disponível para o rótulo.

#### Registro de responsabilidade / assinatura operacional

O sistema deve preservar separadamente:

```text
quem informou/gerou o Resíduo
quem recebeu inicialmente o Resíduo pela Gestão
```

Esses dois nomes devem poder aparecer no rótulo.

Não tratar esse recurso como assinatura digital criptográfica ou certificada. A finalidade é registrar autoria e responsabilidade operacional.

Regra de fluxo desejada:

```text
Solicitante informa
→ Gestor A recebe
→ Gestor A analisa
→ Gestor A libera
→ armazenamento posterior pode ser feito por Gestor A ou outro Gestor autorizado
→ despacho final pode ser feito por Gestor A ou outro Gestor autorizado
```

Portanto, o Gestor que recebe inicialmente assume a conferência até a liberação. O responsável inicial não deve ser perdido quando outro Gestor executar armazenamento ou despacho posteriormente.

O histórico continua registrando o usuário responsável por cada transição.

#### Classes de Resíduo

As classes serão **pré-cadastradas** e apresentadas com código, nome/descrição e estado ativo.

Exemplos fornecidos pelo cliente:

```text
A — Solventes ou soluções de substâncias orgânicas que não contenham halogênios
B — Solventes ou soluções orgânicas que contenham halogênios
F — Resíduos sólidos de produtos químicos orgânicos
H — Outros
```

A seleção deve permitir múltiplas classes quando aplicável, usando checkbox ou controle equivalente.

Preservar a diferença entre declaração e validação:

```text
Solicitante
→ classes informadas

Gestão
→ confere / adiciona / remove
→ classes confirmadas
```

A descrição das classes não deve ficar rigidamente presa a um enum se o cliente precisar adicionar, alterar ou inativar classes no futuro.

#### Informações de segurança

O Resíduo deverá possuir informações de segurança operacional, como necessidade de:

- luvas;
- óculos de proteção;
- máscara/proteção respiratória;
- avental ou proteção equivalente;
- outras medidas estruturadas que venham a ser aprovadas.

Quando houver Produtos do catálogo associados aos componentes, o sistema poderá **sugerir/herdar** medidas de segurança cadastradas nesses Produtos.

O Solicitante poderá realizar edição limitada dessas sugestões e a Gestão deverá poder confirmar a informação durante a análise.

A ocorrência real do Resíduo deve preservar um **snapshot** das informações de segurança utilizadas naquele registro. Alterações futuras no Produto ou em um Modelo de Resíduo não podem modificar retroativamente a segurança de Resíduos históricos.

A modelagem definitiva de segurança de Produto deverá ser feita junto desta subetapa apenas no nível necessário para sustentar essa herança, sem antecipar a reestruturação de unidades prevista na Etapa 8.

#### Estado físico

O Resíduo deverá registrar seu estado físico para uso operacional e no rótulo.

A lista definitiva de valores deve ser fechada durante a modelagem da subetapa, evitando inferir categorias apenas a partir do layout de referência do cliente. O dado deve ser simples, estruturado e adequado aos estados realmente utilizados na operação.

### 3.3 Corrigir o ciclo de geração e disponibilidade do rótulo

Regra desejada:

```text
Resíduo é informado
→ Código SGL já existe
→ rótulo pode ser gerado/visualizado pela Gestão
→ Gestão recebe e analisa
→ Gestão libera
→ somente então a impressão operacional do rótulo é habilitada
```

Gerar/visualizar e permitir impressão são eventos distintos.

Antes da confirmação da Gestão, a visualização pode utilizar os dados informados pelo Solicitante. Depois da análise/liberação, deve priorizar os dados confirmados.

A definição visual definitiva, os templates adaptados e a infraestrutura física de impressão ficam para a **Etapa 10 — Rótulos e impressão operacional**. A Etapa 3 deve apenas garantir que o Resíduo possua os dados necessários e que a regra de disponibilidade/permissão de impressão esteja correta.


---

## Etapa 4 — Expansão operacional de Resíduos

**Impacto:** médio  
**Origem:** itens 4 e 12 + decisão complementar de modelos de Resíduos

### 4.1 Locais de armazenamento cadastráveis

Criar cadastro de locais de armazenamento que possam ser reutilizados no fluxo operacional.

Uso esperado:

```text
local cadastrado
+ complemento livre
```

Exemplo:

```text
Almoxarifado Químico
+ Prateleira B2
```

Também deve existir opção para informar o local manualmente quando necessário.

Regra: uma etapa que exige armazenamento não pode ser concluída sem um local válido, seja ele cadastrado ou informado manualmente.

### 4.2 Modelos de Resíduos pré-cadastrados pela Gestão

Liberar o cadastro de Resíduos padrão recorrentes do laboratório.

Esses registros são **modelos reutilizáveis**, não ocorrências operacionais.

Um modelo poderá concentrar informações padrão que façam sentido na modelagem, como:

- nome/descrição;
- processo de origem/procedência e uso padrão;
- composição padrão;
- produtos/componentes relacionados;
- classes de Resíduo;
- riscos conhecidos;
- informações de segurança/EPI;
- recipiente/acondicionamento;
- tratamento padrão, apenas quando fizer sentido como sugestão;
- demais informações reutilizáveis do Resíduo padrão.

Essas informações funcionam como base para a nova ocorrência. O Resíduo real deve preservar seu próprio snapshot e continuar sujeito à conferência da Gestão.

### 4.3 Uso pelo Solicitante

Ao informar um Resíduo, o Solicitante deverá poder escolher entre:

```text
Resíduo pré-cadastrado
ou
Resíduo informado manualmente
```

Ao selecionar um modelo, os dados padrão são carregados e o usuário informa os dados específicos daquela ocorrência.

Distinção obrigatória:

```text
ModeloResiduo
= definição reutilizável/padrão

Residuo
= ocorrência operacional real
```

Alterar um modelo no futuro não deve modificar retroativamente Resíduos já registrados a partir dele.

---

## Etapa 5 — Projetos e Atividades

**Impacto:** alto  
**Origem:** itens 6, 7 e 8 + novas regras de Projeto levantadas com o cliente em 11/09/2026

Esta etapa fecha o domínio estrutural que será consumido por Estagiários e Relatórios.

Princípio de integridade:

```text
Projeto/Atividade estabilizados
→ só então Estagiários podem criar vínculos
→ só então Relatórios podem consumir esses vínculos
```

### 5.0 Portão de confirmação de Projeto/Atividade

Antes de alterar backend, banco ou contratos desta etapa, confirmar com o cliente:

- regra exata do Código SEG para Projeto e, se existir, para Atividade;
- se Atividade é realmente uma entidade subordinada ao Projeto;
- se `SCI` é apenas um tipo de Projeto ou um domínio diferente;
- lista oficial de situações de execução do Projeto;
- demais regras institucionais que alterem o domínio-base.

Esses pontos permanecem planejados, mas não fechados. Não antecipar enum, migration ou contrato definitivo antes da confirmação.

### 5.1 Domínio-base de Projeto

A relação Laboratório–Projeto fica definida como:

```text
Laboratório 1
   ↑
   │
   N
Projeto
```

Regras:

- todo Projeto pertence obrigatoriamente a **um único Laboratório**;
- um Laboratório pode possuir vários Projetos;
- a modelagem atual `Projeto -> Laboratorio` deve ser preservada/evoluída, não substituída por N:N.

O Projeto deverá possuir, conforme regra final:

- nome;
- descrição;
- Laboratório obrigatório;
- líder/responsável;
- início;
- fim;
- duração derivada ou regra equivalente;
- financiador;
- ciclo de vida;
- situação de execução;
- tipo, caso `PROJETO | SCI` seja confirmado;
- Código SEG;
- Código SGL/rastreabilidade interna conforme padrão do sistema.

#### Líder / responsável

O responsável do Projeto deve preferencialmente referenciar uma pessoa/usuário real do sistema, conforme elegibilidade institucional.

```text
responsável do Projeto
pode ser o responsável do Laboratório
ou
pode ser outra pessoa elegível
```

#### Datas e duração

Manter início e fim.

A duração deve ser calculada a partir das datas quando representar apenas intervalo temporal. Só persistir duração separadamente se o cliente confirmar que existe uma duração planejada/contratual independente das datas reais.

#### Financiador

Inicialmente tratar como informação do Projeto.

Não criar entidade própria de Financiador sem necessidade confirmada, como catálogo institucional, múltiplos financiadores ou dados próprios de relacionamento.

#### Ciclo de vida previsto

```text
CRIADO
→ ATIVO
→ ENCERRADO_COM_AVALIACAO_PENDENTE
→ CONCLUIDO
```

Os nomes técnicos finais podem ser refinados, mas os quatro momentos devem permanecer semanticamente distintos.

#### Situação de execução

Não misturar ciclo de vida com situação operacional/resultado.

Exemplos informados:

```text
EXECUTADO
NAO_EXECUTADO
EM_EXECUCAO
...
```

A lista oficial permanece pendente de confirmação.

### 5.2 Código institucional SEG

O Projeto deverá possuir Código SEG obrigatório.

Formato informado até o momento:

```text
AAAA.MM.DD.XX.XXX

AAAA → ano
MM   → mês
DD   → dia
XX   → índice base
XXX  → índice de atividade
```

O Código SEG é institucional e **não substitui** os identificadores/códigos gerados pelo SGL.

```text
Código SGL
→ identificação/rastreabilidade interna

Código SEG
→ identificação institucional
```

A regra exata de composição entre Projeto e Atividade deve ser confirmada no portão 5.0.

### 5.3 Atividades do Projeto — condicional à confirmação

Se confirmada:

```text
Projeto 1
  ↓
  N
AtividadeProjeto
```

Atividade será subordinada ao Projeto e deverá respeitar a regra institucional de Código SEG aplicável.

Possíveis dados, a confirmar:

- Código SEG;
- nome/título;
- descrição;
- período;
- situação/status;
- demais dados institucionais.

Se o cliente concluir que Atividade não deve existir como entidade própria, esta subetapa deve ser eliminada antes de iniciar a Etapa 6.

### 5.4 Interface de Projetos e Atividades

A interface só deve ser fechada depois do domínio desta etapa estar estabilizado.

Deverá permitir, conforme modelo final:

- listar/cadastrar/editar Projeto;
- visualizar Laboratório;
- Código SEG;
- Código SGL;
- líder/responsável;
- financiador;
- início/fim/duração;
- ciclo de vida;
- situação de execução;
- tipo, se confirmado;
- Atividades e seus códigos, se confirmadas.

Ao concluir a Etapa 5, Projeto e Atividade deixam de ser dependências abertas para a Etapa 6.

---

## Etapa 6 — Estagiários e vínculos

**Impacto:** alto  
**Dependência principal:** Etapa 5 concluída

Esta etapa evolui Estagiários **sobre o domínio de Projeto/Atividade já estabilizado**.

### 6.0 Portão de confirmação de Estagiários

Antes de alterar contratos definitivos, confirmar:

- quem pode ser Orientador e se todo Orientador existe como `Usuario` do SGL;
- se Orientador precisa pertencer ao mesmo Laboratório/Unidade ou pode ser externo;
- se a informação chamada **Cultura** é de fato cultura/área temática;
- se um Estagiário possui uma ou várias Culturas;
- catálogo inicial de Curso/Formação;
- se um Estagiário pode possuir uma ou várias Atividades dentro do mesmo Projeto;
- regra exata da prorrogação;
- relação entre situação institucional do Estagiário e seus vínculos de Projeto/Atividade.

### 6.1 Domínio institucional do Estagiário

#### Orientador obrigatório

Todo Estagiário deve possuir Orientador.

```text
se Orientador sempre for usuário institucional do SGL
→ relação obrigatória com Usuario

se puder ser externo
→ preservar sua identidade sem criar Usuario artificial
```

Orientador é diferente do responsável do Laboratório e do líder do Projeto, embora a mesma pessoa possa exercer mais de uma função quando permitido.

#### Cultura / área temática

Planejar cadastro auxiliar reutilizável, com exemplos como:

- mandioca;
- citros;
- abacaxi;
- demais culturas/áreas adotadas pela Unidade.

Preferência inicial:

```text
Cultura
- id/publicId
- nome
- descrição opcional
- ativo

Gestão
→ cadastra/ativa/inativa
→ seleciona para o Estagiário
```

A cardinalidade final deve ser confirmada em 6.0.

#### Bolsa ≠ Curso/Formação

Não misturar financiamento/vínculo com formação acadêmica.

`TipoBolsa` representa conceitos como:

```text
BOLSA_CNPQ
BOLSA_CAPES
BOLSA_INSTITUCIONAL
VOLUNTARIO
CONTRATUAL
```

Curso/Formação/Nível acadêmico representa valores como:

```text
ENSINO_MEDIO
GRADUACAO
MESTRADO
DOUTORADO
...
```

Essas dimensões devem ser armazenadas separadamente, mesmo que apareçam juntas na interface.

#### Treinamento inicial de segurança

Todo Estagiário deve possuir indicação explícita:

```text
treinamentoInicialSegurancaConcluido = true | false
```

Não adicionar certificado, data ou documento sem nova necessidade confirmada.

### 6.2 Vínculo Estagiário ↔ Projeto/Atividade

Dependências:

```text
Projeto estabilizado
+
Atividade estabilizada ou explicitamente descartada
+
Estagiário institucional estabilizado
```

Regras:

- todo Estagiário deve estar relacionado ao Projeto do qual participa;
- se Atividade existir como entidade, o Estagiário deve estar relacionado à Atividade da qual faz parte;
- não reduzir essa relação a simples `projetoId`/`atividadeId` no Estagiário quando for necessário preservar histórico.

O vínculo deve permitir, conforme regra final:

- entrada em Projeto;
- troca de Projeto/Atividade;
- períodos distintos;
- função/atividade exercida;
- status do vínculo;
- encerramento;
- renovação;
- múltiplos vínculos quando permitido.

### 6.3 Ciclo de vida e prorrogações

Requisito informado:

```text
INÍCIO
→ ATIVO
→ FIM
→ PRORROGAÇÃO + JUSTIFICATIVA
```

Prorrogação não deve sobrescrever silenciosamente a data anterior.

Regra mínima:

- justificativa obrigatória;
- preservar data de fim anterior;
- preservar nova data de fim;
- registrar quando ocorreu;
- registrar responsável pela alteração;
- manter histórico para auditoria.

A modelagem final deverá decidir se prorrogação é evento/transição, mantendo ou retomando estado ativo, em vez de tratá-la necessariamente como status terminal.

Também revisar encerramento normal, inativações eventualmente confirmadas e encerramento definitivo.

### 6.4 Interface de Estagiários

A interface deverá permitir, conforme domínio final:

- Orientador;
- Laboratório;
- Projeto;
- Atividade, se confirmada;
- Bolsa/vínculo;
- Curso/Formação;
- Cultura/área temática;
- treinamento inicial de segurança;
- início;
- fim previsto/efetivo;
- situação atual;
- histórico de prorrogações e justificativas;
- histórico dos vínculos de Projeto/Atividade.

Ao concluir a Etapa 6, o domínio de pessoas/vínculos necessário para relatórios deve estar estabilizado.

---

## Etapa 7 — Relatórios de Projetos, Estagiários e Laboratórios

**Impacto:** médio  
**Dependência principal:** Etapas 5 e 6 concluídas

Esta etapa consome os domínios anteriores sem obrigá-los a mudar apenas para facilitar relatório.

Ordem interna:

```text
Projetos/Atividades estabilizados
+
Estagiários/vínculos estabilizados
→ 7.1 dimensões/filtros
→ 7.2 consultas/agregações
→ 7.3 prévia/telas
→ 7.4 PDF/XLSX
```

### 7.1 Dimensões e filtros

Permitir, quando aplicável:

- Laboratório;
- responsável do Laboratório;
- Projeto;
- Código SEG;
- líder/responsável do Projeto;
- Atividade, se confirmada;
- Orientador;
- Bolsa/vínculo;
- Curso/Formação;
- Cultura/área temática;
- situação do Estagiário;
- período.

### 7.2 Agregações

Deve ser possível obter, conforme filtros:

- Estagiários ativos por Laboratório;
- quantidade por Orientador;
- quantidade por responsável de Laboratório;
- quantidade por Bolsa/vínculo;
- quantidade por Curso/Formação;
- quantidade por Cultura/área temática;
- quantidade por Projeto;
- quantidade por Atividade, se confirmada.

Quando houver recorte histórico, os totais devem derivar do histórico real do vínculo, e não apenas do booleano atual.

### 7.3 Visões de relatório

Preferência inicial:

```text
Laboratórios, Projetos e Estagiários
```

com abas/filtros/agrupamentos, evitando multiplicar relatórios sem necessidade.

### 7.4 Exportações

PDF/XLSX devem usar a mesma consulta, filtros, período e agrupamentos da prévia.

Não criar lógica de cálculo diferente entre tela e exportação.

---

## Etapa 8 — Unidades e Soluções

**Impacto:** alto  
**Origem:** itens 15 e 10  
**Objetivo:** estabilizar a base quantitativa e o domínio de Soluções antes de qualquer integração com Pedidos

Ordem interna obrigatória:

```text
normalização de unidades/apresentações
→ domínio de Soluções
→ composição e regras próprias
→ interface/contrato de Soluções estabilizados
→ somente então Etapa 9
```

### 8.1 Normalização de unidades e apresentações

Separar conceitualmente:

```text
unidade de medida da quantidade
≠
apresentação física da embalagem
```

Exemplos:

- `1 L = 1000 mL`;
- `1 kg = 1000 g`;
- caixa, kit, garrafa e galão representam apresentação/forma de retirada;
- cálculos usam unidades compatíveis/normalizadas.

Não realizar conversão genérica entre dimensões incompatíveis, como massa e volume (`g ↔ mL`), sem informação físico-química apropriada.

### 8.2 Domínio/cadastro de Soluções

Solução representa composição/receita reutilizável de Produtos.

Exemplo:

```text
Solução X
- Produto A: 10 mL
- Produto B: 50 mL
```

Prever:

- Soluções padrão cadastradas;
- composição com Produto + quantidade + unidade;
- CRUD/ciclo apropriado;
- possibilidade de composição manual esporádica, conforme modelagem definitiva;
- preservação da composição utilizada quando a Solução for consumida por outro fluxo.

### 8.3 Fechamento do contrato de Soluções

Antes de seguir para Pedidos, estabilizar:

- DTOs/contratos;
- composição;
- unidades;
- validações;
- comportamento de edição/inativação;
- interface de cadastro/consulta;
- regra de histórico/snapshot necessária para usos futuros.

A Etapa 9 não deve redefinir a entidade Solução; apenas integrá-la ao fluxo de Pedido.

---

## Etapa 9 — Pedidos e integração com Soluções

**Impacto:** alto  
**Dependência principal:** Etapa 8 concluída

### 9.0 Portão de confirmação do escopo de Pedidos

Antes de alterar Pedido, confirmar com o cliente se:

- o fluxo atual do SGL será mantido e apenas receberá Soluções;
- haverá alterações de campos/etapas;
- existe um padrão atual do cliente que precisa ser incorporado;
- quais pontos são obrigatórios versus apenas sugestões.

Até essa confirmação, não antecipar refatoração ampla de Pedido.

### 9.1 Analisar e adaptar o padrão de Pedido, se necessário

Se houver mudança além da integração de Soluções, comparar o padrão real do cliente com o SGL.

Levantar:

- campos obrigatórios/opcionais;
- terminologia;
- itens e quantidades;
- etapas;
- responsáveis;
- aprovação;
- atendimento;
- entrega;
- cancelamento;
- exceções;
- informações úteis e redundantes.

Classificar cada ponto:

```text
manter padrão do SGL
adotar padrão do cliente
combinar/refinar
descartar
```

Resultado obrigatório, quando aplicável:

```text
padrão real documentado
+ comparação cliente × SGL
+ decisões fechadas
+ padrão-alvo aprovado
```

### 9.2 Soluções em Pedidos

Um Pedido deve poder conter, conforme escopo final:

```text
Produto
Solução
ou ambos
```

A utilização de uma Solução deve preservar a composição efetivamente solicitada naquele momento.

### 9.3 Estoque e aprovação atômica de Soluções

A aprovação de uma Solução deve ser atômica quanto aos componentes.

Exemplo:

```text
Solução precisa de 1000 mL de Acetona
Estoque utilizável possui 999 mL
→ não aprovar parcialmente
```

Antes da baixa:

- validar todos os componentes;
- validar lotes;
- aplicar FIFO/FEFO conforme regras vigentes;
- considerar concorrência/locks;
- preservar rastreabilidade dos lotes consumidos;
- definir cancelamento/devolução;
- impedir entrega parcial que descaracterize a receita.

A Etapa 9 deve integrar Solução ao Pedido sem reabrir a modelagem-base já fechada na Etapa 8.

---

## Etapa 10 — Rótulos e impressão operacional

**Impacto:** médio  
**Dependência principal:** Produto, Resíduo e Solução estabilizados até a Etapa 9  
**Origem:** refinamento de rótulos solicitado pelo cliente + necessidade de padronização transversal

Princípio:

```text
padrão-base de rotulagem SGL
        ↓
Produto  → rótulo adaptado
Resíduo  → rótulo adaptado
Solução  → rótulo adaptado
```

### 10.1 Padrão-base de rótulos SGL

Definir:

- marcas SGL + Embrapa;
- identidade visual;
- hierarquia;
- tipografia/margens;
- Código SGL/rastreabilidade;
- alertas;
- palavra de advertência quando aplicável;
- preview;
- regras compartilhadas de impressão.

A referência visual do cliente orienta acabamento, sem obrigar cópia integral.

### 10.2 Rótulo adaptado de Produto

Priorizar informações pertinentes ao domínio:

- identificação;
- Código SGL;
- lote/referência;
- validade;
- quantidade/unidade/apresentação;
- riscos;
- segurança;
- armazenamento;
- fiscalização quando pertinente.

### 10.3 Rótulo adaptado de Resíduo

Consumir os dados estabilizados nas Etapas 3 e 4:

- identificação/Código SGL;
- Unidade/Laboratório;
- quantidade/unidade;
- composição;
- procedência/uso;
- estado físico;
- tratamento;
- classes;
- riscos;
- segurança/EPI;
- palavra de advertência;
- recipiente;
- armazenamento/destino;
- Projeto quando houver;
- quem informou/gerou;
- Gestor que recebeu inicialmente;
- datas úteis.

A regra da Etapa 3 permanece:

```text
visualização pode existir antes da liberação
→ impressão operacional somente após liberação
```

### 10.4 Rótulo adaptado de Solução

Fechar o template com base no domínio estabilizado da Etapa 8, considerando conforme modelo definitivo:

- nome/identificação;
- Código SGL;
- composição;
- concentração;
- quantidade/volume;
- preparo;
- validade;
- responsável;
- riscos;
- segurança;
- armazenamento.

### 10.5 Documento de Auditoria de Entrada de Lote

Disponibilizar documento formal gerado pelo SGL para registrar e imprimir uma entrada de lote.

Identificação obrigatória:

```text
DOCUMENTO INTERNO DO SGL
SEM VALOR FISCAL
```

Conteúdo previsto:

- SGL/Embrapa;
- identificador do documento;
- Código SGL do lote;
- Produto;
- lote/referência do fornecedor;
- Unidade/Laboratório;
- data/hora da entrada;
- quantidade originalmente recebida;
- unidade;
- apresentação;
- multiplicador;
- validade;
- armazenamento;
- observações;
- responsável;
- demais dados de rastreabilidade.

O documento representa **a entrada histórica**, e não o saldo atual do lote.

Deve permitir visualização, PDF/formato imprimível equivalente e impressão em papel comum.

### 10.6 Formatação física e impressão Zebra

Depois dos templates e documento estarem definidos, validar:

- modelo de impressora Zebra;
- dimensões;
- orientação;
- margens;
- driver/forma de envio;
- necessidade de ZPL;
- preview;
- testes físicos;
- infraestrutura compartilhada entre templates.

---

## Etapa 11 — Manual do Usuário e avaliação final de delete lógico

**Impacto:** variável

### 11.1 Manual do Usuário

Inicialmente disponível para a interface comum/Solicitante.

Organizar documentos como:

- como usar o SGL;
- padrões de Soluções;
- regras da Embrapa;
- segurança em laboratório;
- manuseio de produtos;
- procedimentos institucionais.

Definir estratégia de upload/armazenamento antes de criar contrato backend permanente.

### 11.2 Avaliação de delete lógico

Permanece opcional e propositalmente no fim das alterações funcionais.

Revisar entidade por entidade, incluindo:

- Estagiários;
- Produtos;
- Lotes;
- Projetos;
- cadastros auxiliares;
- demais entidades operacionais.

Não aplicar `ativo=true/false` indiscriminadamente quando o domínio já possuir ciclo de vida próprio.

---

## Etapa 12 — Testes automatizados do Frontend

**Impacto:** baixo sobre o domínio / alto valor de estabilização  
**Posição:** etapa final do bloco de pré-produção

Objetivo: automatizar a validação somente depois que as Etapas 1 a 11 estiverem estabilizadas.

### 12.1 Stack escolhida

```text
Vitest + Vue Test Utils
→ testes unitários/componentes

Cypress
→ testes End-to-End
```

Cypress permanece como ferramenta E2E principal.

### 12.2 Escopo mínimo

Cobrir, quando aplicável:

- sessão/login DEV e expiração;
- roteamento/guardas;
- Dashboards;
- Pedidos;
- aprovação/entrega/cancelamento;
- Produtos/estoque/lotes;
- Resíduos;
- modelos de Resíduo/locais;
- Projetos/Atividades;
- Estagiários/vínculos/prorrogações;
- relatórios/filtros;
- Soluções;
- integração Solução × Pedido;
- rótulos e impressão;
- Manual do Usuário;
- tema claro/escuro;
- isolamento por Unidade.

### 12.3 Critério de fechamento

```text
suíte unitária/componentes
+ suíte E2E Cypress
+ scripts npm
+ execução headless reproduzível
+ registro dos cenários críticos
```

A integração em CI pode ocorrer nesta etapa quando a infraestrutura estiver definida.

Esta etapa não substitui a homologação integrada final posterior.

---

# 3. Dependências principais

```text
Etapa 1 — padrão visual
   ↓
Etapa 2 — Dark Mode
   ↓
Etapa 3 — refinamentos do Resíduo atual
   ↓
Etapa 4 — expansão operacional de Resíduos
   ↓
Etapa 5 — Projetos + Atividades
   ↓
Etapa 6 — Estagiários + vínculos
   ↓
Etapa 7 — Relatórios consolidados
   ↓
Etapa 8 — Unidades + Soluções
   ↓
Etapa 9 — Pedidos + integração com Soluções
   ↓
Etapa 10 — Rótulos + impressão operacional
   ↓
Etapa 11 — Manual + decisão de delete lógico
   ↓
Etapa 12 — testes automatizados do Frontend
```

Dependências críticas:

```text
Projeto base
→ deve ser estabilizado antes de Atividades

Código SEG
→ regra Projeto × Atividade deve ser confirmada na Etapa 5 antes de contrato/migration definitiva

Atividades
→ se existirem, devem ser estabilizadas antes da Etapa 6

Estagiário institucional
→ Orientador, Bolsa/Curso, Cultura e treinamento são fechados na Etapa 6

Vínculo Estagiário–Projeto/Atividade
→ depende integralmente da Etapa 5

Relatórios
→ dependem das Etapas 5 e 6 estabilizadas; não devem forçar alteração retroativa desses domínios

Soluções
→ dependem primeiro da normalização de unidades na Etapa 8

Pedidos com Soluções
→ dependem do domínio de Soluções já estabilizado e da confirmação do escopo de Pedido na Etapa 9

Rótulos adaptados
→ dependem dos dados de Resíduo das Etapas 3/4 e do domínio de Soluções da Etapa 8

Documento de auditoria de entrada de lote
→ depende de dados históricos confiáveis da entrada, não do saldo atual

Impressão Zebra
→ depende do fechamento dos templates adaptados

Testes automatizados finais
→ dependem da estabilização das interfaces e fluxos das Etapas 1 a 11
```

Regra estrutural deste roadmap:

```text
etapa-base
→ estabilizar domínio/contrato
→ somente depois iniciar etapa dependente
```

Se uma informação pendente puder alterar uma etapa-base, ela deve ser confirmada antes da implementação daquela etapa, e não corrigida posteriormente em uma etapa dependente.

---

# 4. Estado de execução

No momento da atualização deste documento:

```text
Limpeza/revisão documental anterior              ✅ concluída
Planejamento das etapas de pré-produção           ✅ consolidado
Etapa 1 — refinamento visual global               ✅ concluída
Etapa 2 — Dark Mode definitivo                    ✅ concluída
Etapa 3 — refinamentos do fluxo atual de Resíduos ⏭ PRÓXIMA IMPLEMENTAÇÃO
  3.1 — remover redundância de análise            ⏭ primeiro passo
  3.2 — dados/classes/segurança/responsabilidade  ⏳
  3.3 — ciclo geração/visualização/impressão      ⏳
Etapas 4 a 12                                     ⏳ aguardando sequência
```

A matriz de permissões não é a próxima etapa enquanto este plano de pré-produção estiver aberto.

---

# 5. Regra de continuidade

Ao encerrar cada etapa, atualizar este documento e os `CONTINUIDADE.md` afetados com:

```text
status da etapa
→ decisões fechadas
→ arquivos/componentes/domínios alterados
→ validações executadas
→ pendências remanescentes
→ próxima etapa
```

Se uma nova necessidade surgir durante a pré-produção, ela deve ser posicionada neste plano de acordo com dependências e impacto antes da implementação, em vez de ser executada fora da sequência sem registro.