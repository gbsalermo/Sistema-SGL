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

### 3.4 Fechar o conteúdo definitivo do rótulo de Resíduo

Antes de definir dimensões físicas ou integração Zebra, fechar quais informações realmente devem aparecer no rótulo.

O conteúdo deverá considerar, conforme disponibilidade e validação da Gestão:

- logo/marca SGL/Embrapa;
- Código SGL;
- Unidade e Laboratório;
- descrição/identificação do Resíduo;
- quantidade e unidade;
- composição;
- `processoOrigem` apresentado como procedência/uso;
- tratamento realizado e descrição, quando aplicável;
- classes informadas/confirmadas;
- riscos informados/confirmados;
- informações de segurança/EPI;
- recipiente;
- armazenamento temporário;
- destino previsto;
- Projeto, quando houver;
- nome de quem informou/gerou;
- nome do Gestor que recebeu inicialmente;
- datas operacionais realmente úteis.

Evitar informação redundante e preservar legibilidade.

Regra conceitual:

```text
antes da análise
→ dados declarados/informados quando ainda não houver confirmação

após análise/liberação
→ dados confirmados pela Gestão
```

### 3.5 Formatação física e impressão Zebra

Somente depois do conteúdo definitivo do rótulo estar fechado deve ser validado o ambiente real de impressão:

- modelo de impressora Zebra;
- dimensões físicas do rótulo;
- orientação;
- margens;
- driver/forma de envio;
- necessidade ou não de ZPL;
- comportamento de preview;
- critérios de habilitação do botão de impressão.

O layout físico deve ser simples, legível e compatível com a quantidade real de informação aprovada em 3.4.

### 3.6 Avaliar Ficha/Comprovante de Lote

Como verificação complementar de impressão operacional, avaliar uma **Ficha/Comprovante de Lote** imprimível para situações em que material de estoque precise ser repassado acompanhado de seus dados de rastreabilidade.

Esse documento não deve ser tratado ou nomeado como nota fiscal oficial; sua função é operacional e informativa.

A ficha deve partir dos dados já consolidados no lote e poderá incluir, conforme validação durante a etapa:

- código SGL do lote;
- produto;
- lote/referência do fornecedor;
- unidade recebida e apresentação;
- quantidade disponível ou quantidade repassada, conforme contexto;
- multiplicador;
- data de entrada;
- validade;
- condição de retirada unitária;
- observação;
- Unidade/laboratório;
- demais informações de rastreabilidade consideradas úteis.

Antes da implementação, decidir se essa ficha será página própria para impressão pelo navegador, PDF gerado pelo SGL ou ambos.

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

## Etapa 5 — Reestruturação de Projetos e vínculos de Estagiários

**Impacto:** alto  
**Origem:** itens 6, 7 e 8

Esta etapa deve ser tratada como uma única evolução de domínio.

### 5.1 Projeto com ciclo de vida próprio

Projeto passa a ter, entre os dados obrigatórios/relevantes:

- código/número próprio fornecido no cadastro;
- laboratório(s), conforme regra definitiva da modelagem;
- status/ciclo de vida;
- dados atuais do projeto;
- vínculos de pessoas.

Ciclo inicial proposto:

```text
INICIADO
→ EM_ANDAMENTO
→ ENCERRADO
```

Os nomes finais podem ser refinados durante a modelagem.

### 5.2 Vínculo Estagiário ↔ Projeto

O Estagiário deve estar vinculado a projeto ativo, além de seu laboratório.

O vínculo não deve ser apenas um `projetoId` no Estagiário. Deve preservar histórico e permitir:

- mais de um projeto;
- troca de projeto;
- encerramento;
- renovação;
- períodos distintos;
- atividade exercida no projeto;
- status do vínculo.

A modelagem deve prever uma entidade de vínculo com, no mínimo:

- Estagiário;
- Projeto;
- atividade exercida;
- início;
- fim previsto;
- fim efetivo;
- status;
- informações de renovação/encerramento quando necessárias.

### 5.3 Tela/Seção de Projetos

A interface de Projetos deve permitir:

- listar;
- cadastrar;
- mostrar o código/número obrigatório;
- editar;
- visualizar laboratório(s);
- visualizar status atual;
- visualizar ciclo de vida;
- listar vínculos de usuários comuns;
- listar vínculos de Estagiários.

### 5.4 Revisão da ação atual “Encerrar” Estagiário

Revisar a ação para representar adequadamente situações distintas, incluindo:

- inativação temporária;
- inativação por prazo indeterminado;
- encerramento definitivo.

Todas as opções devem exigir motivo detalhado e preservar histórico.

Antes da implementação backend, deve ser definida explicitamente a relação entre a situação institucional do Estagiário e seus vínculos ativos com Projetos.

---

## Etapa 6 — Relatórios de Projetos e Laboratórios

**Impacto:** médio após a estabilização da Etapa 5  
**Origem:** item 9

Criar visão de relatórios para Projetos utilizando o domínio definitivo criado na etapa anterior.

Preferência inicial: evitar aumentar excessivamente a lista de relatórios. Avaliar uma entrada consolidada como:

```text
Laboratórios e Projetos
```

A interface pode separar internamente as visões por abas ou filtros.

Possíveis dados:

- laboratório;
- projetos;
- código/número do projeto;
- status;
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

## Etapa 8 — Manual do Usuário e avaliação final de delete lógico

**Impacto:** variável  
**Origem:** itens 16 e 5

### 8.1 Manual do Usuário

Inicialmente disponível apenas para a interface comum/Solicitante.

A seção deve organizar documentos como:

- como usar o SGL;
- padrões de Soluções;
- regras da Embrapa;
- segurança em laboratório;
- manuseio de produtos;
- outros procedimentos institucionais.

A implementação definitiva de upload/armazenamento deve ser definida antes de criar contrato backend permanente. Evitar colocar binários grandes diretamente no PostgreSQL sem justificativa técnica.

### 8.2 Avaliação de delete lógico

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

## Etapa 9 — Testes automatizados do Frontend

**Impacto:** baixo sobre o domínio / alto valor de estabilização  
**Posição:** etapa final do bloco de pré-produção

Objetivo: automatizar a validação do frontend somente depois que as alterações funcionais e visuais das etapas anteriores estiverem estabilizadas.

### 9.1 Stack de testes escolhida

Para o SGL, a estratégia recomendada é:

```text
Vitest + Vue Test Utils
→ testes unitários e de componentes/lógica Vue

Cypress
→ testes End-to-End em navegador real
```

Entre Selenium e Cypress, o padrão escolhido para o frontend do SGL é **Cypress**, por ter integração direta com Vue 3 + Vite e oferecer uma experiência mais adequada ao stack atual.

Selenium não fica proibido tecnicamente, mas não será a ferramenta principal do projeto enquanto Cypress atender aos cenários necessários.

### 9.2 Escopo mínimo

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
- Manual do Usuário;
- tema claro/escuro;
- isolamento visual/funcional da Unidade conforme a sessão DEV.

### 9.3 Critério de fechamento

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
Etapa 8 — Manual + decisão de delete lógico
   ↓
Etapa 9 — testes automatizados do Frontend
```

Dependências críticas:

```text
Projeto–Estagiário
→ exige vínculo histórico próprio

Evolução estrutural de Pedidos
→ exige primeiro análise e síntese do padrão real utilizado pelo cliente

Soluções
→ exigem primeiro uma regra consistente de unidades de medida

Pedidos com Soluções
→ dependem de Soluções + unidades + validação atômica de estoque

Relatório de Projetos
→ depende do novo domínio de Projetos/Estagiários estabilizado

Rótulo Zebra de Resíduo
→ depende do fechamento dos novos dados, responsabilidades, classes e segurança antes da formatação física

Segurança herdada de Produto/ModeloResiduo
→ funciona como sugestão; o Resíduo real preserva snapshot próprio e validação da Gestão

Modelos de Resíduos
→ reutilizam as definições de classes/segurança da Etapa 3, sem alterar retroativamente ocorrências antigas

Testes automatizados finais do Frontend
→ dependem da estabilização das interfaces e fluxos das Etapas 1 a 8
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
  3.4 — conteúdo definitivo do rótulo            ⏳
  3.5 — formatação física / Zebra                ⏳
  3.6 — ficha/comprovante de lote                ⏳
Etapas 4 a 9                                     ⏳ aguardando sequência
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