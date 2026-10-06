# Continuidade — Etapa 7 — Unidades + Soluções + contexto operacional

**Data:** 06/10/2026  
**Projeto:** SGL — Sistema de Gestão de Laboratórios  
**Branch backend:** `collab/etapa-7-unidades-solucoes-contexto`  
**Branch frontend:** `collab/etapa-7-unidades-solucoes-contexto`  
**Dependências:** Etapas 1–6 concluídas e validadas  
**Estado:** Etapa 7 iniciada — bloco 7.0 em auditoria

---

## 1. Motivo da reordenação do roadmap

A consolidação de Relatórios foi movida para depois de Unidades/Soluções e Pedidos.

Nova ordem:

```text
Etapa 7 — Unidades + Soluções + contexto operacional
Etapa 8 — Pedidos + Soluções + participação
Etapa 9 — Relatórios consolidados
```

Motivo: Etapas 7 e 8 alteram diretamente os dados e relações que alimentarão os relatórios. Consolidar Relatórios antes disso geraria retrabalho em DTOs, filtros, agregações, PDF/XLSX e telas.

---

## 2. Objetivo da Etapa 7

Estabilizar três fundamentos:

1. separar unidade física de medida da apresentação/embalagem;
2. criar o domínio de Soluções sobre Produtos e quantidades compatíveis;
3. fechar o contexto operacional que será usado por Pedidos na Etapa 8.

---

## 3. Auditoria inicial — estado atual

### 3.1 UnidadeMedida mistura conceitos

Estado atual:

```java
ML, L, MG, G, KG,
UNIDADE, REACAO, CAIXA, FRASCO, AMPOLA, PAR, METRO, OUTRO
```

Problema:

- `mL/L` representam volume;
- `mg/g/kg` representam massa;
- `metro` representa comprimento;
- `unidade` representa contagem;
- `caixa/frasco/ampola/par` são apresentações físicas;
- `reação` pode representar capacidade/rendimento, não uma unidade física universal.

Conclusão: o enum atual deve ser desmembrado conceitualmente antes de Soluções.

### 3.2 Produto

`Produto` possui hoje:

- `unidadeMedida`;
- `unidadeArmazenamento` como texto livre;
- risco, perecibilidade, fiscalização e segurança.

A Etapa 7 deve evitar duplicar os conceitos atuais. A decisão deve indicar qual é a unidade canônica de estoque/consumo do Produto e como a apresentação física é descrita.

### 3.3 EstoqueCentral

Estado atual:

```java
Integer quantidadeAtual
Integer quantidadeMinima
```

Isso funciona para contagem inteira, mas é insuficiente se o estoque canônico precisar representar, por exemplo:

```text
125,5 mL
0,75 g
```

Antes de migrar para decimal, é obrigatório avaliar Lote, Movimentação, Pedido, relatórios e dados DEV.

### 3.4 Pedido

`ItemPedido` usa:

```java
Integer quantidadeSolicitada
Integer quantidadeAprovada
TipoEmbalagem tipoEmbalagemSolicitada
Integer quantidadeEmbalagensSolicitada
Integer multiplicadorSolicitado
```

A estrutura já separa parcialmente a forma de retirada da quantidade total, o que deve ser aproveitado.

O problema é que `quantidadeSolicitada` ainda representa "unidades individuais", não uma quantidade física tipada por dimensão.

### 3.5 Soluções

Não existe atualmente entidade/domínio `Solucao` no backend.

A Etapa 7 deverá modelá-la sem transformar uma Solução em Produto artificialmente.

Regra preliminar:

```text
Produto != Solução
```

Uma Solução pode consumir Produtos como componentes, mas deve possuir identidade, composição e contexto próprios.

---

## 4. Decisões a fechar antes da primeira migration

### 7.0.1 — Dimensões físicas

Proposta de ponto de partida:

```text
VOLUME
→ mL
→ L

MASSA
→ mg
→ g
→ kg

COMPRIMENTO
→ m

CONTAGEM
→ unidade
```

Não fazer conversão massa ↔ volume sem densidade explícita.

### 7.0.2 — Apresentação física

Separar apresentação de unidade:

```text
FRASCO
AMPOLA
CAIXA
KIT
PAR
UNITARIO
OUTRO
```

`TipoEmbalagem` atual deve ser auditado antes de criar enum novo.

### 7.0.3 — Quantidade decimal

Decidir se:

```text
Integer
→ BigDecimal
```

deve ocorrer no estoque canônico.

Se sim, a migration deve ser compatível com dados inteiros existentes.

### 7.0.4 — Unidade canônica

Cada Produto deve ter uma unidade canônica de estoque/consumo.

Exemplos:

```text
Etanol → mL
NaCl → g
Ponteira → unidade
```

Apresentação:

```text
Frasco 500 mL
Pacote 100 unidades
Caixa 10 frascos
```

não altera a unidade canônica; apenas define como a quantidade é apresentada/retirada.

### 7.0.5 — Solução

Antes da implementação, definir:

- solução pertence a uma Unidade;
- nome/descrição;
- ativa/inativa;
- composição com Produtos;
- quantidade + unidade por componente;
- rendimento final;
- unidade do rendimento;
- instruções/preparo;
- necessidade de concentração;
- snapshots da composição em Pedido;
- se uma Solução pode conter outra Solução — recomendação inicial: **não na primeira versão**.

---

## 5. Contexto operacional herdado da Etapa 6

Para Estagiários:

```text
VinculoEstagioAtividade
→ Atividade
→ SCI
→ Projeto
→ Laboratório
→ Unidade
```

Esse é o contexto operacional a ser preservado para Pedidos.

Para outros perfis:

- Projeto selecionado determina Laboratório;
- sem Projeto, `Usuario.laboratorio` pode permanecer como contexto-base;
- frontend não deve enviar combinações incompatíveis de Projeto/Laboratório.

A Etapa 7 deve fechar o contrato; a aplicação efetiva em Pedido será Etapa 8.

---

## 6. Roadmap interno da Etapa 7

```text
7.0 Auditoria + decisões de domínio          🔧 atual
7.1 Unidade de medida x apresentação         ⏳
7.2 Modelo decimal/compatibilidade estoque   ⏳
7.3 Domínio de Soluções                      ⏳
7.4 Contexto operacional                     ⏳
7.5 Frontend integrado                       ⏳
7.6 Dados DEV + testes                       ⏳
7.7 Validação + documentação                 ⏳
```

---

## 7. Regra para a próxima ação

Não criar migration ainda.

Primeiro concluir 7.0 respondendo:

1. quais dimensões/unidades serão suportadas;
2. qual é a unidade canônica de cada Produto;
3. se quantidades físicas passam a `BigDecimal`;
4. como `TipoEmbalagem` atual será reutilizado;
5. qual é o contrato mínimo de Solução.

Somente depois iniciar 7.1.

---

## 8. Relatórios

O planejamento anterior de Relatórios foi preservado, mas movido para a Etapa 9.

Nada deve ser perdido:

- relatório de Projetos;
- dimensões de Estagiários/vínculos;
- Movimentações com `Resumo` e `Detalhamento`;
- preview/PDF/XLSX usando a mesma consulta/filtros.

A implementação aguarda a estabilização das Etapas 7 e 8.
