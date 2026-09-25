# Continuidade SGL — Etapa 5

**Criado em:** 24/09/2026  
**Etapa anterior:** Etapa 4 — Expansão operacional de Resíduos ✅ concluída e validada  
**Etapa atual:** Etapa 5 — Projetos e Atividades 🔧 iniciada  
**Bloco atual:** 5.2 — SCI 🔧 atual; 5.0 ✅ e 5.1 ✅ fechados  
**Branch de trabalho:** `collab/etapa-5-projetos-atividades`  
**Fonte canônica de `main`:** GitLab institucional  

---

## 1. Objetivo

A Etapa 5 estabiliza o domínio de Projetos e introduz a hierarquia confirmada Projeto → SCI → Atividade.

Roadmap canônico:

```text
5.0 Portão de confirmação                         ✅ fechado
→ 5.1 Projeto base                                ✅ concluído e validado
→ 5.2 SCI                                         🔧 atual
→ 5.3 Atividades
→ 5.4 Código SEG — validação hierárquica
→ 5.5 Interface e integração
```

O backend deve estabilizar Projeto → SCI → Atividade antes do fechamento da interface. O Código SEG é cadastrado pelo usuário/gestão nesta etapa; o SGL valida formato e coerência hierárquica, sem gerar a numeração automaticamente.

---

## 2. Estado atual do Projeto

O SGL já possui um cadastro funcional de `Projeto`.

Estado após o fechamento do 5.1:

```text
Projeto
├── UUID público
├── Laboratório obrigatório
├── nome
├── descrição
├── dataInicio
├── dataFim
├── responsavel (texto livre)
├── codigoSeg
├── status
├── situacaoExecucao
├── possuiRecursoExterno
├── empresaRecursoExterno
└── ativo
```

Relação vigente:

```text
Laboratório 1 → N Projetos
```

O CRUD atual já é tenant-safe e a Administração possui interface de cadastro/edição.

A Etapa 5 deve evoluir essa base; não criar um segundo domínio de Projeto paralelo.

---

## 3. Portão 5.0 — estado consolidado

### 3.1 Código SEG ✅ regra hierárquica confirmada

O Código SEG é institucional e será tratado inicialmente como **dado cadastrado**, não como código gerado pelo SGL.

Estrutura confirmada pelo cliente:

```text
PROJETO   = XX.XX.XX.XXX.XX.00
SCI       = XX.XX.XX.XXX.XX.SS
ATIVIDADE = XX.XX.XX.XXX.XX.SS.AAA
```

Onde:

- os 13 dígitos do Projeto terminam em `00`;
- o SCI mantém a raiz do Projeto e usa sufixo sequencial `01`, `02`, `03`...;
- a Atividade herda integralmente o código do SCI e acrescenta três dígitos sequenciais;
- exemplo fornecido: `10.25.00.085.00.01.001`;
- o padrão é único; varia apenas a numeração.

Hierarquia:

```text
Projeto 1
└── N SCI
    └── N Atividades
```

### 3.2 SCI ✅ entidade própria subordinada ao Projeto

Definição do cliente:

> SCI é uma solução/contribuição para inovação ligada à gestão dos recursos do Projeto.

Regra:

- SCI nunca existe sem Projeto;
- um Projeto pode possuir vários SCI;
- SCI possui código SEG derivado do Projeto;
- não tratar SCI como mero "tipo de Projeto".

### 3.3 Atividade ✅ entidade própria subordinada ao SCI

Regra confirmada:

- Atividade pertence a um SCI;
- consequentemente, toda Atividade pertence a um Projeto;
- não existem Atividades soltas apenas no Laboratório;
- Código da Atividade = Código do SCI + 3 dígitos;
- Atividade possui ciclo/status independente do Projeto e pode ser encerrada antes dele;
- Estagiário executa uma Atividade; a própria Atividade representa sua responsabilidade no Projeto.

Dados cadastrais informados para Projeto/SCI/Atividade:

```text
Código SEG
Título
Líder/responsável
Início
Fim
Duração — opcional/derivável quando aplicável
Status principal
Situação de execução
```

A coluna "Figura = Projeto/SCI/Atividade" da planilha não vira um campo de domínio: o tipo da própria entidade já representa essa informação.

Recurso externo pertence ao contexto do Projeto e não deve ser duplicado em SCI/Atividade.

### 3.4 Ciclo de vida ✅ confirmado para Projeto

Cliente confirmou:

```text
ATIVO
→ ENCERRADO_COM_AVALIACAO_PENDENTE
→ CONCLUIDO
```

Não criar estado `CRIADO` como ciclo oficial sem necessidade adicional. Cadastro e ativação podem ser tratados separadamente se o fluxo exigir.

Atividades possuem estado independente. A lista exata de estados de Atividade ainda deve ser fechada sem inventar enum além do que foi confirmado.

### 3.5 Recurso externo ✅ significado confirmado

A coluna anteriormente chamada "Projeto" na planilha do cliente indica se o Projeto possui **recurso externo de outra empresa**.

Na modelagem isso deve virar um dado explícito, não manter o nome ambíguo da planilha.

### 3.6 Regras já confirmadas para Estagiários

- todo Estagiário possui Orientador;
- Orientador pode ser **PESQUISADOR** ou **ANALISTA**;
- graduando/mestrando/doutorando descreve o nível/formação do Estagiário, não do Orientador;
- Estagiário está vinculado a Projeto/Atividade;
- a Atividade já representa a responsabilidade do Estagiário;
- Cultura representa a cultura da pesquisa, com exemplos: mandioca, maracujá, abacaxi, citros, banana, mamão e outras;
- Bolsa/vínculo e Curso/Formação permanecem conceitos separados.

### 3.7 Projeto x Laboratório ✅ interpretação fechada

A relação física atual será preservada:

```text
Laboratório 1 → N Projetos
Projeto → 1 Laboratório responsável/contextual
```

Porém, **Projeto é o eixo operacional principal**, não um simples cadastro subordinado ao Laboratório.

Uso esperado:

```text
Projeto
→ SCI
→ Atividades
→ Estagiários vinculados

Laboratório
→ serve como contexto institucional/filtro
→ permite localizar funcionários
→ permite localizar Projetos e respectivas Atividades
```

Ou seja: manter a FK atual evita quebra de compatibilidade, mas a UI e os fluxos da Etapa 5 devem dar maior protagonismo ao Projeto. Não criar hierarquia visual que faça o usuário precisar "entrar no Laboratório" para trabalhar com Projeto.

---

## 4. Projeto base — contrato para 5.1

O `Projeto` existente será evoluído, sem criação de entidade paralela.

Campos de domínio previstos:

```text
Projeto
├── publicId / Código SGL interno
├── codigoSeg
├── titulo
├── descricao
├── laboratorioResponsavel
├── liderResponsavel
├── dataInicio
├── dataFim
├── duracao (preferencialmente derivada; não duplicar sem necessidade)
├── status
├── situacaoExecucao
├── possuiRecursoExterno
├── empresaRecursoExterno
└── ativo técnico/cadastral, somente se ainda necessário
```

### Status principal do Projeto

Confirmado:

```text
ATIVO
→ ENCERRADO_COM_AVALIACAO_PENDENTE
→ CONCLUIDO
```

### Situação de execução

Segundo indicador existente na fonte do cliente:

```text
NAO_INFORMADO
EM_ANDAMENTO_NO_PRAZO
EM_ANDAMENTO_ATRASADO
EXECUCAO_CANCELADA
```

Esse indicador é distinto do ciclo principal.

Como ele aparenta depender de início/fim/duração, **não automatizar nem derivar definitivamente nesta primeira migration sem validar a semântica completa**. Inicialmente o contrato deve preservar os valores oficiais; uma regra automática pode ser adicionada depois sem mudar o significado do dado.

### Recurso externo

```text
possuiRecursoExterno = false
→ empresaRecursoExterno = null

possuiRecursoExterno = true
→ empresaRecursoExterno obrigatória
```

A antiga coluna "Projeto = sim/não" da planilha não será reproduzida com esse nome ambíguo.

---

## 5. Preparação para Estagiários — Etapa 6

A Etapa 5 deve criar Projeto/SCI/Atividade já pensando nos vínculos da Etapa 6, mas **não duplicar dados pessoais**.

Já pertencem ao domínio atual de `Usuario`:

```text
nome
email
Unidade
Laboratório
perfil
ativo
```

Já pertencem ao `Estagiario` atual:

```text
dataInicioEstagio
dataFimEstagio
tipoBolsa (campo legado)
observacao
```

A Etapa 6 deverá acrescentar/evoluir:

- Código SGL interno do vínculo, se necessário;
- Orientador obrigatório, limitado a PESQUISADOR ou ANALISTA;
- Atividade obrigatória; Projeto é obtido por Atividade → SCI → Projeto;
- histórico de mudança/migração entre Atividades em vez de apenas sobrescrever a FK;
- Cultura como catálogo administrável por Unidade, semelhante ao conceito de catálogo reutilizável;
- prorrogações com histórico, não apenas alteração silenciosa da data final;
- situação do estágio: EM_ANDAMENTO, FINALIZADO, PRORROGADO;
- treinamento de segurança booleano;
- telefone/celular apenas se não vier da identidade institucional;
- separar **formação/nível** de **bolsa/vínculo**.

Exemplos de Cultura informados: mandioca, maracujá, abacaxi, citros, banana, mamão e outras.

A autenticação institucional será fonte preferencial para dados pessoais. O SGL deve guardar apenas os dados necessários ao vínculo acadêmico/operacional.

---

## 6. Regras de implementação

- partir da `main` já contendo a Etapa 4;
- preservar isolamento por Unidade;
- manter `Laboratório 1 → N Projetos`;
- evoluir `Projeto` existente, não duplicar entidade;
- preservar compatibilidade com Resíduos e Estagiários que já referenciam Projeto;
- migrations aplicadas são imutáveis;
- backend primeiro, frontend somente após estabilização do contrato;
- mudanças funcionais devem ser testadas antes de avançar de bloco.

---

## 7. Plano de execução canônico

### 5.1 — Projeto base ✅ CONCLUÍDO E VALIDADO

A tabela `projetos` nasceu na V1 e foi expandida pela **V19** sem recriação nem perda de compatibilidade. A próxima migration disponível é **V20**.

Ordem obrigatória:

```text
5.1.1 auditar tabela/entidade/DTOs/testes atuais
→ 5.1.2 desenhar V19 sem destruir dados existentes
→ 5.1.3 criar enums do domínio do Projeto
→ 5.1.4 evoluir Projeto.java
→ 5.1.5 evoluir Request/Response DTOs
→ 5.1.6 evoluir Repository/Service mantendo tenant fail-closed
→ 5.1.7 ajustar Controller/OpenAPI
→ 5.1.8 atualizar DemoDataInitializer quando necessário
→ 5.1.9 adicionar/ajustar testes
→ validar backend
```

Decisões para o 5.1:

- manter `laboratorio_id` obrigatório como laboratório responsável/contextual;
- tratar o campo atual `nome` como **Título**; evitar rename destrutivo só por nomenclatura;
- manter `responsavel` textual inicialmente como líder/responsável, sem criar FK prematura com Usuario;
- `codigoSeg` é dado institucional cadastrado;
- não gerar Código SEG automaticamente;
- duração deve ser **derivada de início/fim** sempre que isso representar apenas intervalo de datas; não persistir informação duplicada sem necessidade;
- criar status principal: `ATIVO`, `ENCERRADO_COM_AVALIACAO_PENDENTE`, `CONCLUIDO`;
- criar situação de execução separada: `NAO_INFORMADO`, `EM_ANDAMENTO_NO_PRAZO`, `EM_ANDAMENTO_ATRASADO`, `EXECUCAO_CANCELADA`;
- nesta primeira evolução, não inferir automaticamente a situação de execução apenas pelas datas;
- adicionar `possuiRecursoExterno`;
- quando `possuiRecursoExterno=true`, exigir empresa do recurso externo;
- preservar `ativo` enquanto compatibilidade/delete lógico não forem tratados na Etapa 11; ele não substitui o status de negócio;
- preservar todos os filtros por tenant já existentes;
- preservar compatibilidade com Pedido, Resíduo e demais referências atuais a Projeto.

Fechamento validado em 25/09/2026:

- V19 aplicada;
- backend compilando e aplicação DEV iniciando normalmente;
- suíte completa JUnit verde;
- CRUD atual sem regressão;
- tenant fail-closed validado;
- payload legado preservado;
- domínio expandido validado funcionalmente via API;
- documentação atualizada;
- formato/coerência/duplicidade do Código SEG permanecem para o 5.4.

### 5.2 — SCI 🔧 ATUAL

Projeto está estabilizado. Iniciar agora:

```text
Projeto 1 → N SCI
```

SCI será entidade própria. Dados previstos: Código SEG, título, líder/responsável, início/fim, status/situação conforme a fonte institucional e vínculo obrigatório ao Projeto. Não duplicar Laboratório se ele puder ser obtido do Projeto sem perda de regra de negócio.

### 5.3 — Atividades

Somente após SCI estabilizado:

```text
SCI 1 → N Atividades
```

Atividade será entidade própria, vinculada obrigatoriamente ao SCI. Ela possui ciclo independente e pode encerrar antes do Projeto. A própria Atividade representa a responsabilidade executada pelo Estagiário.

### 5.4 — Código SEG e validações hierárquicas

Formato canônico:

```text
Projeto   XX.XX.XX.XXX.XX.00
SCI       XX.XX.XX.XXX.XX.SS
Atividade XX.XX.XX.XXX.XX.SS.AAA
```

Nesta etapa:

- validar formato;
- validar Projeto terminando em `00`;
- validar SCI com a mesma raiz do Projeto e sufixo próprio;
- validar Atividade com o código completo do SCI + três dígitos;
- validar duplicidade conforme o escopo institucional definido no domínio;
- não gerar sequências automaticamente nesta primeira versão.

### 5.5 — Interface e integração

A interface deve refletir a importância funcional de Projeto:

```text
Projetos
→ SCI do Projeto
→ Atividades do SCI
→ participantes/vínculos quando aplicável
```

Laboratório permanece filtro/contexto, mas o usuário não deve precisar navegar por Laboratório para acessar Projeto.

O CRUD atual de Projeto em Administração permanece compatível durante a evolução. Só substituir/reorganizar a experiência após contratos de Projeto/SCI/Atividade estabilizados.

### Critério de avanço

Critério de avanço do 5.1 para 5.2:

```text
migration aplicada            ✅
+ backend compilando          ✅
+ testes verdes               ✅
+ CRUD atual sem regressão    ✅
+ tenant validado             ✅
+ documentação atualizada     ✅
```

**5.2 liberado em 25/09/2026.**

