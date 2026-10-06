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

---

## 9. Decisão adicional — estoque fracionável por recipiente físico

Foi confirmado que, para itens fracionáveis, o SGL não pode representar apenas um saldo agregado do lote.

Exemplo aprovado:

```text
Entrada:
10 frascos de Etanol
cada frasco = 500 mL

Saldo físico inicial:
10 × 500 mL
= 5.000 mL
```

Após retirada de 50 mL:

```text
INCORRETO:
10 frascos
ou
9 frascos + 450 mL sem identificar o recipiente

CORRETO:
9 frascos fechados de 500 mL
1 frasco aberto com 450 mL
```

O sistema precisa preservar o estado de cada recipiente/apresentação fracionável.

### 9.1 Modelo conceitual recomendado

```text
EstoqueCentral
→ Produto na Unidade
→ saldo consolidado canônico

Lote
→ validade / fornecedor / rastreabilidade

RecipienteEstoque
→ unidade física real dentro do lote
→ apresentação
→ capacidade inicial
→ quantidade disponível
→ aberto/fechado
→ data de abertura
→ ativo/esgotado
```

Nome definitivo da entidade ainda será fechado antes da migration. Nomes candidatos:

- `RecipienteEstoque`;
- `UnidadeFisicaEstoque`;
- `ApresentacaoEstoque`.

A preferência conceitual é por **RecipienteEstoque**, porque o registro representa uma unidade física individual rastreável.

### 9.2 Exemplo

```text
Lote ETANOL-001
├── frasco #1 → 500 mL → FECHADO
├── frasco #2 → 500 mL → FECHADO
├── ...
├── frasco #9 → 500 mL → FECHADO
└── frasco #10 → 450 mL → ABERTO
```

O saldo agregado continua podendo ser mostrado como:

```text
4.950 mL
```

mas nunca substitui a informação física dos recipientes.

### 9.3 Regra preliminar de retirada

A retirada precisa respeitar simultaneamente:

1. FIFO/FEFO do lote;
2. estado físico dos recipientes;
3. preferência por recipientes já abertos quando houver retirada parcial;
4. evitar abrir recipientes novos desnecessariamente;
5. registrar exatamente quais recipientes foram consumidos e quanto saiu de cada um.

Exemplos aprovados:

```text
Saldo:
9 × 500 mL fechados
1 × 450 mL aberto

Pedido 200 mL
→ retirar 200 mL do frasco aberto
→ frasco aberto passa a 250 mL

Pedido 600 mL
→ liberar 1 frasco fechado de 500 mL
→ retirar 100 mL do frasco aberto de 450 mL
→ frasco aberto passa a 350 mL
```

A regra exata para casos onde FIFO/FEFO conflita com "recipiente aberto primeiro" será fechada no 7.0 antes da implementação.

### 9.4 Auditoria obrigatória

Cada saída fracionada deve registrar:

- lote;
- recipiente físico;
- quantidade anterior no recipiente;
- quantidade retirada;
- quantidade restante;
- usuário responsável;
- pedido/origem;
- data/hora;
- unidade canônica;
- se o recipiente foi aberto naquela operação;
- se o recipiente foi esgotado naquela operação.

Não basta registrar apenas a quantidade total retirada do Lote.

### 9.5 Consistências obrigatórias

O backend deve garantir invariantes:

```text
saldo EstoqueCentral
=
soma dos saldos dos recipientes ativos

saldo do Lote
=
soma dos saldos dos recipientes do Lote

0 <= saldo do recipiente <= capacidade inicial

recipiente FECHADO
→ saldo = capacidade inicial

recipiente ABERTO
→ saldo pode ser menor que capacidade inicial

recipiente ESGOTADO
→ saldo = 0
```

Alteração manual que quebre essas igualdades deve ser bloqueada ou passar por ajuste auditável.

### 9.6 Concorrência

Saídas precisam bloquear os recipientes/lotes selecionados durante a transação.

Objetivo:

```text
dois Gestores aprovando ao mesmo tempo
≠
os dois consumirem os mesmos 100 mL restantes
```

A estratégia de lock atual dos Pedidos/Lotes deve ser revisada para chegar ao nível do recipiente físico quando houver fracionamento.

### 9.7 Entrada de estoque

Para apresentação fracionável:

```text
quantidade de apresentações = 10
conteúdo por apresentação = 500 mL
```

deve materializar dez recipientes físicos com 500 mL cada.

Para itens não fracionáveis, avaliar se é necessário materializar uma linha por unidade física ou se o saldo agregado continua suficiente.

### 9.8 Impacto na Etapa 7

O 7.0 passa a incluir auditoria específica de:

- `Lote.quantidadeDisponivel`;
- `Lote.quantidadeInicial`;
- `Lote.quantidadeApresentacoes`;
- `Lote.conteudoPorApresentacao`;
- `Lote.fracionavel`;
- `MovimentacaoEstoque`;
- seleção FIFO/FEFO;
- locks pessimistas;
- devolução/cancelamento;
- descarte;
- entrada/ajuste de lote.

Nenhuma migration deve ser criada antes de fechar esta modelagem.

### 9.9 Prioridade definitiva para fracionamento: FEFO acima de aberto/fechado

Decisão confirmada:

```text
1. validade/lote mais próximo do vencimento (FEFO)
2. dentro do lote prioritário:
   → recipiente já aberto para a fração
   → recipiente fechado somente quando necessário
3. entre múltiplos recipientes abertos:
   → continua prevalecendo a validade do lote
   → em empate, usar critério determinístico para reduzir sobras abertas
```

Portanto, um recipiente aberto de lote mais novo **não ultrapassa** um lote mais antigo apenas por já estar aberto.

Exemplo:

```text
Lote A — vence primeiro — recipientes fechados
Lote B — vence depois   — 1 recipiente aberto

retirada parcial
→ o sistema deve indicar primeiro o Lote A
→ se a operação parcial exigir abertura, abre o recipiente do Lote A
→ o recipiente aberto do Lote B continua preservado para quando chegar sua prioridade FEFO
```

Isso significa que o estoque pode possuir **múltiplos recipientes abertos simultaneamente**, e o modelo deve suportar esse estado sem tentar forçar artificialmente apenas um recipiente aberto por Produto.

Quando houver vários recipientes abertos, a seleção continua ordenada por validade/lote. Para empate dentro do mesmo lote, o critério de desempate deve ser estável e auditável; a implementação poderá priorizar o recipiente aberto há mais tempo e, em novo empate, o de menor saldo restante, reduzindo recipientes parcialmente consumidos.

A interface deve deixar visível a recomendação, por exemplo:

```text
Retirada recomendada:
Lote ETANOL-001 — vence em 12/11/2026
Frasco #04 — aberto — 220 mL disponíveis
```

e não exigir que o usuário descubra manualmente qual recipiente deve usar.

### 9.10 Concorrência — aprovação serializada e revalidação obrigatória

Decisão de integridade:

Dois Gestores podem abrir/preparar Pedidos ao mesmo tempo, mas a alteração física do estoque deve ser serializada.

Cenário:

```text
Gestor A abre Pedido A
Gestor B abre Pedido B

ambos enxergam o mesmo saldo

A aprova primeiro
→ locks são adquiridos
→ recipientes/lotes são revalidados
→ saída é registrada
→ transação de A é concluída

B tenta aprovar depois
→ lê/revalida o estado após A
→ não pode consumir o saldo antigo
```

Se a alocação que B havia visto foi alterada por A, a aprovação de B deve falhar atomicamente com conflito de estoque.

Comportamento recomendado:

```text
HTTP 409 / conflito de estoque

"O estoque foi alterado por outra operação.
Revise a disponibilidade antes de aprovar este pedido."
```

O Pedido B **não deve ser marcado automaticamente como ENTREGUE, APROVADO ou CANCELADO**.

Preferência:

```text
Pedido B permanece PENDENTE
→ interface atualiza a disponibilidade
→ Gestor revisa/reaprova
```

Se ainda houver saldo em outros lotes/recipientes, o sistema pode apresentar uma nova sugestão de alocação, mas não deve silenciosamente aprovar usando uma distribuição diferente daquela que foi revalidada durante a tentativa concorrente.

Se não houver mais saldo suficiente:

```text
"Saldo insuficiente após movimentação concorrente."
```

e o Pedido continua preservado para ajuste/rejeição explícita.

### 9.11 Estratégia técnica de concorrência

A aprovação deve combinar:

- transação única;
- lock pessimista nos registros físicos selecionados (`Lote`/`RecipienteEstoque`) ou estratégia equivalente;
- ordem determinística de aquisição dos locks para reduzir deadlock;
- revalidação de quantidade e estado depois de adquirir o lock;
- nenhuma movimentação parcial se qualquer item falhar;
- rollback integral do Pedido/Solução quando necessário;
- histórico de conflito opcional para auditoria.

Para aprovação envolvendo vários Produtos/Soluções:

```text
ou todos os componentes são reservados/baixados
ou nenhum é alterado
```

Essa regra preserva a atomicidade já desejada para Soluções.

### 9.12 Consequência para o desenho do estoque

O saldo exibido continua agregado, mas a fonte operacional passa a ser a soma das unidades físicas:

```text
EstoqueCentral
→ visão consolidada

Lote
→ agrupamento/rastreabilidade/validade

RecipienteEstoque
→ fonte física de disponibilidade fracionável

MovimentacaoEstoque
→ trilha de cada alteração
```

A implementação do 7.1 deve preservar essa hierarquia.

### 9.13 Regra adicional — retirada inteira deve preservar recipientes abertos

Decisão confirmada:

Quando a quantidade solicitada corresponde exatamente a uma ou mais apresentações completas, o sistema deve evitar consumir recipientes já abertos sem necessidade.

Prioridade:

```text
1. FEFO define o lote prioritário
2. dentro do lote prioritário:
   a) pedido de apresentação inteira
      → usar recipiente FECHADO compatível
   b) pedido fracionado
      → usar recipiente ABERTO compatível
      → abrir novo recipiente somente se necessário
```

Exemplo:

```text
Lote A — vence primeiro
├── frasco aberto: 300 mL
├── frasco fechado: 500 mL
└── frasco fechado: 500 mL
```

Pedido de 500 mL:

```text
→ entregar 1 frasco fechado de 500 mL
→ manter o frasco aberto com 300 mL
```

Pedido de 200 mL:

```text
→ retirar 200 mL do frasco aberto
→ restam 100 mL
```

Pedido de 700 mL:

```text
→ 1 frasco fechado de 500 mL
→ + 200 mL do recipiente aberto
```

Objetivo:

- evitar abrir recipientes desnecessariamente;
- reduzir proliferação de recipientes parcialmente consumidos;
- preservar rastreabilidade física;
- manter FEFO como regra superior entre lotes.

Essa regra deve ser aplicada pela recomendação automática de retirada e validada novamente no momento da aprovação.

