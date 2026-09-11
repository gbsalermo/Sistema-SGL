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

A modelagem definitiva de segurança de Produto deverá ser feita junto desta subetapa apenas no nível necessário para sustentar essa herança, sem antecipar a reestruturação de unidades prevista na Etapa 7.

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

A definição visual definitiva, os templates adaptados e a infraestrutura física de impressão ficam para a **Etapa 8 — Rótulos, documento de lote e impressão operacional**. A Etapa 3 deve apenas garantir que o Resíduo possua os dados necessários e que a regra de disponibilidade/permissão de impressão esteja correta.


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

## Etapa 5 — Projetos, Atividades e vínculos de Estagiários

**Impacto:** alto  
**Origem:** itens 6, 7 e 8 + novas regras de Projeto levantadas com o cliente em 11/09/2026

Esta etapa deve ser tratada de forma **hierárquica e dependente**. Nenhuma subetapa que consuma Projeto/Atividade deve ser iniciada antes de a subetapa estrutural anterior estar fechada.

Ordem obrigatória:

```text
5.0 confirmar regras institucionais ainda pendentes
→ 5.1 consolidar domínio-base de Projeto
→ 5.2 consolidar identificação/código SEG
→ 5.3 modelar Atividades, se confirmadas
→ 5.4 modelar vínculos de Estagiários sobre Projeto/Atividade
→ 5.5 revisar ciclo institucional do Estagiário
→ 5.6 implementar interface consolidada de Projetos
```

### 5.0 Portão de confirmação antes da implementação

Antes de alterar backend, banco ou contratos da Etapa 5, confirmar com o cliente:

- regra exata do código SEG para Projeto e, se existir, para Atividade;
- se Atividade é realmente uma entidade subordinada ao Projeto;
- se um Estagiário pode possuir uma ou várias Atividades dentro do mesmo Projeto;
- se `SCI` é apenas um tipo de Projeto ou um domínio diferente;
- lista oficial de situações de execução do Projeto;
- demais regras institucionais que afetem vínculo, encerramento ou avaliação.

Esses pontos permanecem **planejados, porém não fechados**. Não antecipar enum, migration ou contrato definitivo para eles antes da confirmação.

### 5.1 Consolidar o domínio-base de Projeto

A relação Laboratório–Projeto fica definida como:

```text
Laboratório 1
   ↑
   │
   N
Projeto
```

Regras:

- todo Projeto deve pertencer obrigatoriamente a **um único Laboratório**;
- um Laboratório pode possuir vários Projetos;
- a modelagem atual `Projeto -> Laboratorio` já segue essa direção e deve ser preservada/evoluída, não substituída por N:N.

O Projeto deverá possuir, conforme regra final da etapa:

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

O responsável do Projeto deve ser uma referência real a pessoa/usuário do sistema, e não apenas texto livre, sempre que a modelagem institucional permitir.

Regra:

```text
responsável do Projeto
pode ser o responsável do Laboratório
ou
pode ser outra pessoa elegível
```

A elegibilidade definitiva do responsável deve ser fechada na implementação da etapa.

#### Datas e duração

Manter início e fim.

A duração deve ser **calculada a partir das datas** quando representar apenas intervalo temporal. Só deve ser persistida separadamente se o cliente confirmar que existe uma duração planejada/contratual independente das datas reais.

#### Financiador

Inicialmente tratar como informação do Projeto.

Não criar entidade própria de Financiador sem necessidade confirmada, como catálogo institucional, múltiplos financiadores ou dados próprios de relacionamento.

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

O Código SEG é **institucional** e não substitui os identificadores/códigos gerados pelo SGL.

Princípio:

```text
Código SGL
→ identificação/rastreabilidade interna do sistema

Código SEG
→ identificação institucional do Projeto/atividade
```

A regra exata de composição entre Projeto e Atividade deve ser confirmada no portão 5.0 antes de criar validação definitiva.

### 5.3 Atividades do Projeto — condicional à confirmação

Se a relação for confirmada, a estrutura deverá seguir:

```text
Projeto 1
  ↓
  N
AtividadeProjeto
```

Atividade será subordinada ao Projeto e deverá respeitar a regra institucional de Código SEG aplicável.

Possíveis dados, a confirmar:

- código SEG;
- nome/título;
- descrição;
- período;
- situação/status;
- demais dados institucionais.

A Atividade deverá existir antes de qualquer vínculo de Estagiário que dependa dela.

Se o cliente concluir que Atividade não deve existir como entidade própria, esta subetapa deve ser eliminada e o vínculo do Estagiário será modelado diretamente com Projeto conforme a regra confirmada.

### 5.4 Vínculo de Estagiários com Projeto/Atividade

Esta subetapa depende de:

```text
5.1 Projeto estabilizado
+
5.3 Atividade definida ou explicitamente descartada
```

Não adicionar simplesmente um `projetoId` ou `atividadeId` ao Estagiário.

O vínculo deve preservar histórico e permitir, conforme as regras que serão fechadas na área de Estagiários:

- entrada em Projeto;
- troca;
- encerramento;
- renovação;
- períodos distintos;
- atividade/função exercida;
- status do vínculo;
- múltiplos vínculos quando permitido.

A modelagem definitiva desta subetapa será complementada pelas decisões específicas da área de Estagiários antes do início da Etapa 5.

### 5.5 Revisão do ciclo institucional do Estagiário

Depois de Projeto/Atividade/vínculo estabilizados, revisar a ação atual de encerramento do Estagiário para representar corretamente estados como:

- inativação temporária;
- inativação por prazo indeterminado;
- encerramento definitivo;
- demais situações confirmadas pelo cliente.

Todas as transições relevantes devem preservar motivo, período e histórico.

Antes de implementar, definir explicitamente como a situação institucional do Estagiário afeta vínculos ativos de Projeto/Atividade.

### 5.6 Interface consolidada de Projetos

A interface só deve ser fechada depois da estabilização das subetapas anteriores.

Deverá permitir, conforme o domínio final:

- listar;
- cadastrar;
- editar;
- visualizar Laboratório;
- visualizar Código SEG;
- visualizar Código SGL;
- visualizar líder/responsável;
- visualizar financiador;
- visualizar início/fim/duração;
- visualizar ciclo de vida;
- visualizar situação de execução;
- visualizar tipo, se confirmado;
- visualizar Atividades, se confirmadas;
- visualizar vínculos de pessoas/Estagiários.

#### Ciclo de vida previsto

O ciclo administrativo solicitado pelo cliente é:

```text
CRIADO
→ ATIVO
→ ENCERRADO_COM_AVALIACAO_PENDENTE
→ CONCLUIDO
```

Os nomes técnicos finais podem ser refinados na Etapa 5, mas os quatro momentos devem permanecer semanticamente distintos.

#### Situação de execução

Não misturar ciclo de vida com situação operacional/resultado.

Exemplos mencionados pelo cliente:

```text
EXECUTADO
NAO_EXECUTADO
EM_EXECUCAO
...
```

A lista oficial permanece pendente de confirmação.

---

## Etapa 6 — Relatórios de Projetos e Laboratórios

**Impacto:** médio após a estabilização da Etapa 5  
**Origem:** item 9

Criar visão de relatórios para Projetos utilizando **exclusivamente o domínio definitivo estabilizado na Etapa 5**.

Não antecipar no relatório campos, Atividades, tipos ou situações ainda pendentes de confirmação.

Preferência inicial: evitar aumentar excessivamente a lista de relatórios. Avaliar uma entrada consolidada como:

```text
Laboratórios e Projetos
```

A interface pode separar internamente as visões por abas ou filtros.

Possíveis dados, conforme o domínio final:

- Laboratório;
- Projeto;
- Código SEG;
- Código SGL;
- líder/responsável;
- financiador;
- início/fim/duração;
- ciclo de vida;
- situação de execução;
- tipo, se confirmado;
- Atividades e respectivos códigos SEG, se confirmadas;
- participantes;
- Estagiários;
- situação dos vínculos;
- períodos.

PDF/XLSX devem seguir a mesma consulta/filtros da prévia, mantendo o padrão atual do SGL.

---

## Etapa 7 — Unidades, Soluções e integração com Pedidos

**Impacto:** muito alto  
**Origem:** itens 15, 10 e 11 + decisão complementar sobre o padrão real de Pedidos do cliente

Ordem interna obrigatória:

```text
análise do padrão atual de Pedidos do cliente
→ síntese com o padrão atual do SGL
→ normalização de unidades
→ entidade/cadastro de Soluções
→ Soluções dentro de Pedidos
```

### 7.1 Analisar e adaptar o padrão de Pedidos atual do cliente

Antes de alterar o domínio, contrato ou interface de Pedidos, deve ser analisado o **padrão de pedido utilizado atualmente pelo cliente**.

O objetivo não é copiar integralmente o processo atual do cliente nem preservar o SGL sem questionamento. A subetapa deve comparar os dois modelos e produzir uma **síntese operacional melhor**, aproveitando os pontos positivos de cada um e removendo redundâncias, ambiguidades ou limitações.

A análise deve levantar, quando aplicável:

- campos e informações presentes no pedido atual do cliente;
- dados obrigatórios e opcionais;
- terminologia utilizada pela equipe;
- forma de solicitar produtos, quantidades e apresentações;
- organização dos itens do pedido;
- etapas do fluxo;
- responsáveis por cada etapa;
- aprovação, atendimento, entrega e cancelamento;
- observações, justificativas e situações excepcionais;
- informações que hoje ajudam a operação;
- informações redundantes, pouco claras ou que geram retrabalho.

Depois, cada ponto deve ser comparado com o Pedido atual do SGL e classificado de forma objetiva, por exemplo:

```text
manter padrão do SGL
adotar padrão do cliente
combinar/refinar os dois modelos
descartar por não agregar valor
```

A decisão deve considerar principalmente:

- clareza para o Solicitante;
- facilidade operacional para a Gestão;
- rastreabilidade;
- redução de retrabalho;
- consistência com Estoque e Movimentações;
- compatibilidade com a arquitetura e regras já consolidadas no SGL;
- preparação para a inclusão de Soluções e unidades normalizadas nas subetapas seguintes.

Resultado obrigatório desta subetapa:

```text
padrão real do cliente documentado
+ comparação cliente × SGL
+ pontos positivos e negativos identificados
+ decisões de manter/adotar/refinar/descartar
+ padrão-alvo de Pedido aprovado
```

Somente após esse padrão-alvo estar fechado devem começar as alterações estruturais de Pedidos previstas nesta Etapa 7.

### 7.2 Normalização de unidades e apresentações

Separar conceitualmente:

```text
unidade de medida da quantidade
≠
apresentação física da embalagem
```

Exemplos:

- `1 L` pode equivaler a `1000 mL`;
- `1 kg` pode equivaler a `1000 g`;
- caixa, kit, garrafa e galão continuam representando apresentação/forma de retirada;
- cálculos precisam utilizar unidades compatíveis/normalizadas.

Não realizar conversão genérica entre dimensões incompatíveis, como massa e volume (`g ↔ mL`), sem informação físico-química que permita essa conversão.

### 7.3 Entidade/cadastro de Soluções

Solução representa uma composição/receita reutilizável de produtos.

Exemplo conceitual:

```text
Solução X
- Produto A: 10 mL
- Produto B: 50 mL
```

Devem existir:

- Soluções padrão cadastradas;
- composição com Produto + quantidade + unidade;
- CRUD/ciclo apropriado;
- possibilidade de composição manual esporádica pelo Solicitante, conforme modelagem definitiva.

Uma Solução padrão é uma definição reutilizável; sua utilização em Pedido deve preservar a composição efetivamente solicitada naquele momento.

### 7.4 Soluções em Pedidos

Um Pedido deve poder conter:

```text
Produto
Solução
ou ambos simultaneamente
```

A aprovação de uma Solução precisa ser **atômica** quanto aos componentes necessários.

Exemplo:

```text
Solução precisa de 1000 mL de Acetona
Estoque utilizável possui 999 mL
→ Solução não pode ser aprovada parcialmente
```

Antes da baixa, o backend deverá validar todos os componentes e lotes necessários. Somente se a composição inteira puder ser atendida a transação deve realizar as baixas seguindo FIFO/FEFO e as regras de estoque.

Também devem ser considerados:

- concorrência;
- locks;
- rastreabilidade dos lotes consumidos;
- cancelamento/devolução;
- impossibilidade de “entrega parcial” de uma receita que deixaria de representar a Solução solicitada.

---

## Etapa 8 — Rótulos e impressão operacional

**Impacto:** médio  
**Dependência principal:** domínios de Produto, Resíduo e Solução estabilizados até a Etapa 7  
**Origem:** refinamento de rótulos solicitado pelo cliente + necessidade de padronização transversal

Esta etapa consolida uma arquitetura única de rotulagem para o SGL sem transformar todos os rótulos em cópias do mesmo conteúdo.

Princípio:

```text
padrão-base de rotulagem SGL
        ↓
Produto  → rótulo adaptado
Resíduo  → rótulo adaptado
Solução  → rótulo adaptado
```

Cada domínio utiliza apenas as informações pertinentes à sua operação.

### 8.1 Padrão-base de rótulos SGL

Definir os elementos compartilhados entre os tipos de rótulo:

- marcas SGL + Embrapa;
- identidade visual e hierarquia da informação;
- tipografia, margens e organização;
- Código SGL e informações essenciais de rastreabilidade;
- posição e destaque de alertas;
- palavra de advertência quando aplicável;
- comportamento de preview;
- regras compartilhadas de impressão.

A referência visual fornecida pelo cliente deve orientar acabamento e seleção de informações úteis, sem obrigar o SGL a reproduzir integralmente aquele modelo.

Frases extensas de perigo/precaução ou outros blocos regulatórios adicionais não entram automaticamente apenas porque existem no rótulo de referência; exigem decisão funcional própria.

### 8.2 Rótulo adaptado de Produto

Revisar o rótulo atual de Produto sobre a base comum, priorizando apenas informações pertinentes ao domínio, como:

- identificação do Produto;
- Código SGL/rastreabilidade;
- lote/referência;
- validade, quando aplicável;
- quantidade/unidade/apresentação;
- riscos;
- informações de segurança;
- armazenamento;
- fiscalização, quando pertinente.

Os campos definitivos devem ser fechados a partir do domínio estabilizado até a Etapa 7.

### 8.3 Rótulo adaptado de Resíduo

Aplicar o padrão-base aos dados definidos nas Etapas 3 e 4.

O rótulo poderá utilizar, conforme disponibilidade e confirmação pela Gestão:

- identificação e Código SGL;
- Unidade/Laboratório;
- quantidade e unidade;
- composição;
- procedência/uso por meio de `processoOrigem`;
- estado físico;
- tratamento realizado;
- classes de Resíduo;
- riscos;
- informações de segurança/EPI;
- palavra de advertência quando aplicável;
- recipiente;
- armazenamento/destino;
- Projeto, quando houver;
- quem informou/gerou;
- Gestor que recebeu inicialmente;
- datas operacionais úteis.

A regra de disponibilidade definida na Etapa 3 permanece válida:

```text
visualização pode existir antes da liberação
→ impressão operacional somente após a liberação prevista pelo fluxo
```

### 8.4 Rótulo adaptado de Solução

O template de Solução só deve ser fechado depois que o domínio de Soluções estiver estabilizado na Etapa 7.

Deve considerar, conforme a modelagem definitiva:

- nome/identificação da Solução;
- Código SGL;
- composição;
- concentração;
- quantidade/volume;
- data de preparo;
- validade, quando aplicável;
- responsável;
- riscos;
- segurança;
- armazenamento;
- demais informações realmente necessárias.

Não antecipar campos definitivos antes da modelagem da Solução.

### 8.5 Documento de Auditoria de Entrada de Lote

Disponibilizar um documento gerado pelo SGL para registrar e imprimir os dados de uma **entrada de lote**.

Objetivo:

```text
entrada de lote registrada no SGL
→ usuário pode visualizar o documento de auditoria
→ gerar/imprimir quando necessário
→ documento acompanha conferência, arquivo físico ou auditoria
```

O documento deve possuir aparência formal e organizada, podendo lembrar visualmente um documento fiscal pela distribuição das informações, mas deve ser identificado de forma explícita como:

```text
DOCUMENTO INTERNO DO SGL
SEM VALOR FISCAL
```

Não deve ser chamado de nota fiscal nem tentar substituir documento fiscal oficial.

Conteúdo previsto, conforme disponibilidade no domínio:

- identificação SGL/Embrapa;
- número/identificador do documento;
- Código SGL do lote;
- Produto;
- lote/referência do fornecedor;
- Unidade/Laboratório;
- data/hora da entrada;
- quantidade originalmente recebida;
- unidade de medida;
- apresentação/embalagem;
- multiplicador, quando aplicável;
- validade;
- condições relevantes de armazenamento;
- observações da entrada;
- responsável pelo registro/recebimento, quando disponível;
- demais dados necessários à rastreabilidade.

Para fins de auditoria, o documento deve representar **a entrada realizada**, e não simplesmente o saldo atual do lote. Baixas, retiradas ou movimentações posteriores não devem alterar retroativamente o conteúdo histórico da entrada.

A implementação deve preferir dados históricos confiáveis da entrada/movimentação ou snapshot equivalente, evitando montar o documento apenas a partir de campos mutáveis do estado atual do lote.

Formato desejado:

- visualização no SGL;
- geração de PDF ou formato imprimível equivalente;
- impressão em papel comum;
- layout adequado para arquivo e conferência administrativa.

Esse documento é diferente dos pequenos rótulos físicos de identificação e não depende de impressora Zebra.

### 8.6 Formatação física e impressão Zebra

Depois dos templates adaptados e do documento de auditoria estarem definidos, validar o ambiente real de impressão dos rótulos físicos:

- modelo de impressora Zebra;
- dimensões físicas dos rótulos;
- orientação;
- margens;
- driver/forma de envio;
- necessidade ou não de ZPL;
- comportamento de preview;
- testes físicos;
- possibilidade de compartilhar infraestrutura de impressão entre Produto, Resíduo e Solução.

O objetivo é possuir uma base de impressão compartilhada com templates específicos por domínio, evitando três soluções técnicas independentes.


---

## Etapa 9 — Manual do Usuário e avaliação final de delete lógico

**Impacto:** variável  
**Origem:** itens 16 e 5

### 9.1 Manual do Usuário

Inicialmente disponível apenas para a interface comum/Solicitante.

A seção deve organizar documentos como:

- como usar o SGL;
- padrões de Soluções;
- regras da Embrapa;
- segurança em laboratório;
- manuseio de produtos;
- outros procedimentos institucionais.

A implementação definitiva de upload/armazenamento deve ser definida antes de criar contrato backend permanente. Evitar colocar binários grandes diretamente no PostgreSQL sem justificativa técnica.

### 9.2 Avaliação de delete lógico

Esta parte permanece **opcional e propositalmente no fim das alterações funcionais**.

Antes de implementar, revisar entidade por entidade, incluindo exemplos como:

- Estagiários;
- Produtos;
- Lotes;
- Projetos;
- cadastros auxiliares;
- demais entidades operacionais.

Não aplicar um simples `ativo=true/false` indiscriminadamente. Algumas entidades já possuem ciclo de vida próprio e podem exigir estados como `INATIVO`, `ENCERRADO`, `DESCARTADO` ou equivalentes.

A implementação só deverá ocorrer após confirmar que o delete lógico agrega valor ao domínio sem conflitar com histórico, rastreabilidade ou estados já existentes.

---

## Etapa 10 — Testes automatizados do Frontend

**Impacto:** baixo sobre o domínio / alto valor de estabilização  
**Posição:** etapa final do bloco de pré-produção

Objetivo: automatizar a validação do frontend somente depois que as alterações funcionais e visuais das Etapas 1 a 9 estiverem estabilizadas.

### 10.1 Stack de testes escolhida

Para o SGL, a estratégia recomendada é:

```text
Vitest + Vue Test Utils
→ testes unitários e de componentes/lógica Vue

Cypress
→ testes End-to-End em navegador real
```

Entre Selenium e Cypress, o padrão escolhido para o frontend do SGL é **Cypress**, por ter integração direta com Vue 3 + Vite e oferecer uma experiência mais adequada ao stack atual.

Selenium não fica proibido tecnicamente, mas não será a ferramenta principal do projeto enquanto Cypress atender aos cenários necessários.

### 10.2 Escopo mínimo

A suíte final deve cobrir, de forma automatizada, os fluxos críticos que existirem ao término das etapas anteriores, incluindo quando aplicável:

- sessão/login DEV e expiração;
- roteamento e guardas por perfil;
- Dashboard Solicitante e Gestão;
- criação e acompanhamento de Pedidos;
- aprovação/entrega/cancelamento refletidos na interface;
- Produtos, estoque e lotes;
- Resíduos e seu ciclo operacional;
- modelos de Resíduos e locais de armazenamento;
- Projetos e vínculos de Estagiários;
- relatórios e filtros;
- Soluções dentro de Pedidos;
- rótulos adaptados de Produto, Resíduo e Solução + fluxos de preview/impressão;
- Manual do Usuário;
- tema claro/escuro;
- isolamento visual/funcional da Unidade conforme a sessão DEV.

### 10.3 Critério de fechamento

A etapa deve produzir:

```text
suíte unitária/componentes
+ suíte E2E Cypress
+ scripts npm padronizados
+ execução headless reproduzível
+ registro dos cenários críticos cobertos
```

A integração em CI pode ser realizada nesta etapa quando a infraestrutura do repositório estiver definida.

Esta etapa não substitui a homologação integrada final do roadmap formal; ela cria uma rede automatizada de regressão antes do congelamento funcional e da homologação.

---

# 3. Dependências principais

```text
Etapa 1 — padrão visual
   ↓
Etapa 2 — Dark Mode
   ↓
Etapa 3 — refinamentos do Resíduo atual
   ↓
Etapa 4 — locais + modelos de Resíduos
   ↓
Etapa 5 — Projetos + Estagiários
   ↓
Etapa 6 — relatórios de Projetos/Laboratórios
   ↓
Etapa 7 — unidades + Soluções + Pedidos
   ↓
Etapa 8 — Rótulos e impressão operacional
   ↓
Etapa 9 — Manual + decisão de delete lógico
   ↓
Etapa 10 — testes automatizados do Frontend
```

Dependências críticas:

```text
Projeto base
→ deve ser estabilizado antes de Atividades, vínculos de Estagiários e relatórios

Código SEG
→ regra Projeto × Atividade deve ser confirmada no início da Etapa 5 antes de contrato/migration definitiva

Atividades
→ são condicionais à confirmação do cliente e, se existirem, devem ser estabilizadas antes do vínculo Estagiário–Atividade

Projeto–Estagiário
→ exige vínculo histórico próprio e depende da definição final de Projeto/Atividade

Evolução estrutural de Pedidos
→ exige primeiro análise e síntese do padrão real utilizado pelo cliente

Soluções
→ exigem primeiro uma regra consistente de unidades de medida

Pedidos com Soluções
→ dependem de Soluções + unidades + validação atômica de estoque

Relatório de Projetos
→ depende de Projeto + Atividades (se confirmadas) + vínculos de Estagiários totalmente estabilizados

Rótulos adaptados
→ dependem dos dados de Resíduo estabilizados nas Etapas 3/4 e do domínio de Soluções estabilizado na Etapa 7

Documento de auditoria de entrada de lote
→ depende de dados históricos confiáveis da entrada e deve representar a quantidade/condição recebida, não o saldo atual

Impressão Zebra
→ depende do fechamento dos templates adaptados de Produto, Resíduo e Solução

Segurança herdada de Produto/ModeloResiduo
→ funciona como sugestão; o Resíduo real preserva snapshot próprio e validação da Gestão

Modelos de Resíduos
→ reutilizam as definições de classes/segurança da Etapa 3, sem alterar retroativamente ocorrências antigas

Testes automatizados finais do Frontend
→ dependem da estabilização das interfaces e fluxos das Etapas 1 a 9
```

---

# 4. Estado de execução

No momento da atualização deste documento:

```text
Limpeza/revisão documental anterior             ✅ concluída
Planejamento das etapas de pré-produção          ✅ consolidado neste documento
Etapa 1 — refinamento visual global              ✅ concluída
Etapa 2 — Dark Mode definitivo                   ✅ concluída
Etapa 3 — refinamentos do fluxo atual de Resíduos 🔧 ETAPA ATUAL
  3.1 — remover redundância de análise           ⏭ próximo passo
  3.2 — dados/classes/segurança/responsabilidade ⏳
  3.3 — ciclo geração/visualização/impressão     ⏳
Etapas 4 a 10                                    ⏳ aguardando sequência
```

A matriz de permissões **não é a próxima etapa** enquanto este plano de pré-produção estiver aberto.

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