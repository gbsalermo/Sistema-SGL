# Continuidade SGL — Etapa 5

**Criado em:** 24/09/2026  
**Etapa anterior:** Etapa 4 — Expansão operacional de Resíduos ✅ concluída e validada  
**Etapa atual:** Etapa 5 — Projetos e Atividades 🔧 iniciada  
**Bloco atual:** 5.0 — Portão de confirmação — regras centrais confirmadas; 2 pontos pendentes  
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

**Pendente:** recuperar/confirmar a lista completa de campos obrigatórios da Atividade. Ela não está preservada de forma suficiente na documentação atual.

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

### 3.7 Ponto estrutural ainda aberto — Projeto x Laboratório ⚠️

Em 11/09 havia sido definido:

```text
Projeto obrigatoriamente ligado a 1 Laboratório
Laboratório 1 → N Projetos
```

Na retomada da Etapa 5 surgiu a percepção de que Projeto deve ter maior independência estrutural que Laboratório.

Não remover nem alterar a FK atual até decidir explicitamente entre:

```text
A) Projeto continua com 1 Laboratório responsável
ou
B) Projeto passa a ser independente/multilaboratorial
```

A hierarquia Projeto → SCI → Atividade não depende dessa decisão, mas a migration do Projeto sim.

---

## 4. Projeto base planejado

Após o portão:

- nome/descrição;
- Laboratório obrigatório;
- líder/responsável;
- início/fim;
- financiador;
- ciclo de vida;
- situação de execução;
- tipo, se confirmado;
- Código SEG;
- Código SGL/rastreabilidade interna.

Ciclo inicialmente proposto no roadmap:

```text
CRIADO
→ ATIVO
→ ENCERRADO_COM_AVALIACAO_PENDENTE
→ CONCLUIDO
```

Esse ciclo ainda deve ser tratado como proposta até o fechamento do 5.0.

---

## 5. Regras de implementação

- partir da `main` já contendo a Etapa 4;
- preservar isolamento por Unidade;
- manter `Laboratório 1 → N Projetos`;
- evoluir `Projeto` existente, não duplicar entidade;
- preservar compatibilidade com Resíduos e Estagiários que já referenciam Projeto;
- migrations aplicadas são imutáveis;
- backend primeiro, frontend somente após estabilização do contrato;
- mudanças funcionais devem ser testadas antes de avançar de bloco.

---

## 6. Próximo passo exato

Fechar as quatro decisões do **5.0**.

Somente depois:

```text
analisar schema atual
→ definir migration seguinte
→ evoluir Projeto
→ DTOs
→ Repository/Service
→ Controller
→ testes
→ frontend
```
