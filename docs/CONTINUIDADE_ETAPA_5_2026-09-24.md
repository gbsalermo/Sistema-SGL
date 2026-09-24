# Continuidade SGL — Etapa 5

**Criado em:** 24/09/2026  
**Etapa anterior:** Etapa 4 — Expansão operacional de Resíduos ✅ concluída e validada  
**Etapa atual:** Etapa 5 — Projetos e Atividades 🔧 iniciada  
**Bloco atual:** 5.1 — Projeto base 🔧 atual; 5.0 ✅ fechado  
**Branch de trabalho:** `collab/etapa-5-projetos-atividades`  
**Fonte canônica de `main`:** GitLab institucional  

---

## 1. Objetivo

A Etapa 5 estabiliza o domínio de Projetos e, se confirmado, introduz Atividades subordinadas.

Roadmap:

```text
5.0 Portão de confirmação
→ 5.1 Projeto base
→ 5.2 Código SEG
→ 5.3 Atividades — condicional
→ 5.4 Interface
```

Nenhuma alteração estrutural de domínio deve ser feita antes do fechamento do 5.0.

---

## 2. Estado atual do Projeto

O SGL já possui um cadastro funcional de `Projeto`.

Hoje:

```text
Projeto
├── UUID público
├── Laboratório obrigatório
├── nome
├── descrição
├── dataInicio
├── dataFim
├── responsavel (texto livre)
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

## 7. Próximo passo exato

O **5.0 está fechado**.

Próximo bloco:

```text
5.1 Projeto base
→ revisar migration atual de projetos
→ definir a próxima migration Flyway
→ evoluir Projeto existente
→ DTOs
→ Repository/Service
→ Controller
→ testes
```

Depois estabilizar Projeto antes de iniciar SCI.
