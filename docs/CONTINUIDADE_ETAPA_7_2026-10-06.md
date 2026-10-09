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
7.0 Auditoria + decisões de domínio          ✅ concluído
7.1 Unidade de medida x apresentação         🔧 atual
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

### 9.14 Auditoria do código atual — impacto real da mudança

A auditoria do código confirmou que o modelo atual já possui mecanismos úteis, porém todos ainda trabalham com saldo agregado inteiro por Lote.

#### Lote atual

O modelo `Lote` já possui:

- `tipoEmbalagem`;
- `apresentacao`;
- `quantidadeApresentacoes`;
- `conteudoPorApresentacao`;
- `fracionavel`;
- `quantidadeInicial`;
- `quantidadeDisponivel`.

Hoje `quantidadeInicial` e `quantidadeDisponivel` são `Integer`, e não existe identificação individual dos recipientes.

#### Movimentação atual

`MovimentacaoEstoqueService.registrarEntradaLote` calcula:

```text
quantidade total
=
quantidade de apresentações
× conteúdo por apresentação
```

e grava o total agregado no Lote e no EstoqueCentral.

A saída usa FIFO/FEFO no nível de Lote e reduz diretamente `Lote.quantidadeDisponivel`.

Portanto, a implementação atual **não consegue distinguir**:

```text
9 frascos fechados de 500 mL
+
1 frasco aberto de 450 mL
```

de um simples saldo agregado de `4.950 mL`.

#### Locks atuais

A implementação já possui bloqueio pessimista de EstoqueCentral/Lote em fluxos de movimentação. Isso é uma base positiva.

Na evolução da Etapa 7, a granularidade de lock deve chegar ao recipiente físico quando a retirada envolver fracionamento.

#### Devolução atual

A devolução hoje restaura saldo diretamente no Lote.

Com `RecipienteEstoque`, a devolução deverá saber quais recipientes foram afetados e não poderá simplesmente somar quantidade ao saldo agregado sem reconstruir corretamente o estado físico.

#### Conclusão da auditoria

O modelo atual pode ser evoluído, não refeito do zero:

```text
manter:
EstoqueCentral
Lote
TipoEmbalagem
MovimentacaoEstoque
FIFO/FEFO
locks

evoluir:
Integer → quantidade física decimal onde aplicável
Lote agregado → Lote + RecipienteEstoque
movimentação por Lote → movimentação com detalhe por recipiente
```

### 9.15 Decisão fechada — modelo de quantidade física

Decisão aprovada para a Etapa 7:

```text
quantidade física
→ BigDecimal

contagem de apresentações/recipientes
→ Integer

unidade canônica
→ definida no Produto

apresentação física
→ separada da unidade de medida

recipiente físico fracionável
→ rastreado individualmente
```

Aplicações previstas:

```text
EstoqueCentral.quantidadeAtual       → BigDecimal
EstoqueCentral.quantidadeMinima      → BigDecimal
Lote.quantidadeInicial               → BigDecimal
Lote.quantidadeDisponivel            → BigDecimal
MovimentacaoEstoque.quantidade*      → BigDecimal
RecipienteEstoque.capacidadeInicial  → BigDecimal
RecipienteEstoque.quantidadeDisponivel → BigDecimal
```

Campos de contagem física continuam inteiros:

```text
quantidadeApresentacoes
quantidade de recipientes
quantidade de caixas/frascos recebidos
```

Não usar `double`/ponto flutuante para saldo físico.

A migration deve preservar os valores inteiros existentes convertendo-os para a nova representação decimal sem perda.

---

## 10. 7.1 — desenho de dimensões e unidades

### 10.1 DimensaoMedida

Proposta fechada como base:

\`\`\`java
public enum DimensaoMedida {
    VOLUME,
    MASSA,
    COMPRIMENTO,
    CONTAGEM
}
\`\`\`

A dimensão classifica o tipo de quantidade, mas **não é suficiente sozinha para autorizar conversão automática**.

### 10.2 UnidadeMedida revisada

Proposta:

\`\`\`text
VOLUME
→ ML
→ L

MASSA
→ MG
→ G
→ KG

COMPRIMENTO
→ METRO

CONTAGEM
→ UNIDADE
→ REACAO
\`\`\`

Remover do conceito de unidade de medida:

\`\`\`text
CAIXA
FRASCO
AMPOLA
PAR
\`\`\`

Esses itens passam a pertencer ao conceito de apresentação/embalagem.

\`OUTRO\` também não deve ser usado como unidade canônica automática. Quando necessário, o sistema deverá exigir especificação explícita e bloquear conversões automáticas.

### 10.3 Compatibilidade de conversão

A regra de conversão não será apenas "mesma dimensão".

Exemplo:

\`\`\`text
mL ↔ L       ✅
mg ↔ g ↔ kg  ✅
metro        ✅ identidade
unidade      ✅ identidade
reação       ✅ identidade

unidade ↔ reação  ❌
massa ↔ volume    ❌
frasco ↔ mL       ❌ sem conteúdo declarado da apresentação
\`\`\`

Portanto, cada \`UnidadeMedida\` deve possuir um grupo/família de conversão explícito.

Proposta conceitual:

\`\`\`java
public enum GrupoConversaoMedida {
    VOLUME,
    MASSA,
    COMPRIMENTO,
    UNIDADE,
    REACAO
}
\`\`\`

Assim, \`UNIDADE\` e \`REACAO\` podem compartilhar a dimensão \`CONTAGEM\`, mas não são convertidas automaticamente entre si.

### 10.4 Unidade base interna por grupo

Bases recomendadas:

\`\`\`text
VOLUME       → mL
MASSA        → mg
COMPRIMENTO  → m
UNIDADE      → unidade
REACAO       → reação
\`\`\`

A unidade base interna permite comparar/somar saldos com segurança.

Exemplos:

\`\`\`text
1 L  → 1000 mL
1 kg → 1.000.000 mg
1 g  → 1000 mg
\`\`\`

A apresentação informada pelo usuário não precisa ser convertida permanentemente para a unidade visual; o backend pode persistir a quantidade canônica e manter metadados de apresentação para exibição/auditoria.

### 10.5 Produto

Cada Produto deverá possuir:

\`\`\`text
unidadeMedidaCanonica
dimensão derivada da unidade
grupo de conversão derivado da unidade
\`\`\`

Exemplos:

\`\`\`text
Etanol
→ unidade canônica: ML

NaCl
→ unidade canônica: MG ou G, conforme cadastro institucional escolhido

Ponteira
→ unidade canônica: UNIDADE

Kit PCR
→ unidade canônica: REACAO
\`\`\`

A escolha da unidade canônica deve ser estável depois que houver estoque/movimentação. Mudança posterior exige fluxo de migração/conversão auditável, não edição simples do cadastro.

### 10.6 Apresentação

\`TipoEmbalagem\` continuará separado da unidade e será ampliado/revisado no próximo subbloco.

Exemplos:

\`\`\`text
FRASCO 500 mL
AMPOLA 2 mL
CAIXA 100 unidades
KIT 50 reações
PAR
UNITARIO
\`\`\`

A relação é:

\`\`\`text
apresentação física
+ conteúdo por apresentação
+ unidade do conteúdo
\`\`\`

e não "embalagem como unidade de medida".

### 10.7 Precisão

Para quantidades físicas, usar \`BigDecimal\`.

A escala exata do banco será fechada na migration, com recomendação inicial de precisão suficiente para laboratório, por exemplo \`DECIMAL(19,6)\`, sem usar \`FLOAT\`/\`DOUBLE\`.

### 10.8 Compatibilidade legada

Mapeamento planejado do enum atual:

\`\`\`text
ML       → ML
L        → L
MG       → MG
G        → G
KG       → KG
METRO    → METRO
UNIDADE  → UNIDADE
REACAO   → REACAO

CAIXA    → apresentação CAIXA
FRASCO   → apresentação FRASCO
AMPOLA   → apresentação AMPOLA
PAR      → apresentação PAR
OUTRO    → exige revisão/mapeamento explícito
\`\`\`

Nenhum registro legado deve ser convertido silenciosamente de uma grandeza incompatível.

---

## 11. 7.1.3 — TipoEmbalagem e RecipienteEstoque

### 11.1 Auditoria do TipoEmbalagem atual

Enum atual:

\`\`\`java
UNITARIO,
KIT,
CAIXA,
GARRAFA,
GALAO
\`\`\`

Ele já cumpre parcialmente o papel correto: **apresentação física**, não unidade de medida.

Entretanto, precisa ser ampliado para cobrir apresentações já existentes no domínio legado e casos laboratoriais comuns.

Proposta:

\`\`\`java
public enum TipoEmbalagem {
    UNITARIO,
    FRASCO,
    AMPOLA,
    GARRAFA,
    GALAO,
    CAIXA,
    KIT,
    PACOTE,
    SACO,
    TUBO,
    POTE,
    PAR,
    OUTRO
}
\`\`\`

A enumeração representa somente o tipo físico da apresentação.

Exemplos:

\`\`\`text
FRASCO + 500 + ML
AMPOLA + 2 + ML
CAIXA + 100 + UNIDADE
KIT + 50 + REACAO
PACOTE + 1000 + UNIDADE
\`\`\`

\`OUTRO\` exige descrição textual em \`apresentacao\`.

### 11.2 Regra de fracionamento

O atributo \`fracionavel\` permanece útil, mas passa a significar:

> o conteúdo interno de uma apresentação física pode ser retirado parcialmente.

Exemplos:

\`\`\`text
Frasco 500 mL de Etanol
→ fracionável = true

Ampola descartável de 2 mL
→ normalmente fracionável = false

Caixa com 100 ponteiras
→ pode ser fracionável = true, se as unidades internas puderem sair separadamente

Kit PCR 50 reações
→ pode ser fracionável = true, desde que a regra institucional permita retirada por reação
\`\`\`

O tipo da embalagem sozinho nunca determina se ela é fracionável.

### 11.3 Nova entidade RecipienteEstoque

Responsabilidade:

Representar uma **unidade física individual de apresentação** pertencente a um Lote.

Estrutura proposta:

\`\`\`java
RecipienteEstoque
- id: Long
- publicId: UUID
- lote: Lote
- sequencial: Integer
- codigoInterno: String
- tipoEmbalagem: TipoEmbalagem
- capacidadeInicial: BigDecimal
- quantidadeDisponivel: BigDecimal
- unidadeMedida: UnidadeMedida
- estado: EstadoRecipienteEstoque
- dataAbertura: LocalDateTime?
- dataEsgotamento: LocalDateTime?
- ativo: Boolean
- observacao: String?
\`\`\`

### 11.4 EstadoRecipienteEstoque

Proposta:

\`\`\`java
public enum EstadoRecipienteEstoque {
    FECHADO,
    ABERTO,
    ESGOTADO
}
\`\`\`

Regras:

\`\`\`text
FECHADO
→ quantidadeDisponivel = capacidadeInicial
→ dataAbertura = null
→ dataEsgotamento = null

ABERTO
→ 0 < quantidadeDisponivel <= capacidadeInicial
→ dataAbertura != null
→ dataEsgotamento = null

ESGOTADO
→ quantidadeDisponivel = 0
→ dataEsgotamento != null
\`\`\`

Um recipiente pode passar:

\`\`\`text
FECHADO → ABERTO → ESGOTADO
\`\`\`

ou, em retirada integral:

\`\`\`text
FECHADO → ESGOTADO
\`\`\`

Nesse segundo caso, não é necessário simular uma abertura intermediária apenas para registrar a transição.

### 11.5 Código interno do recipiente

Cada recipiente precisa de identificação própria e estável para auditoria.

Proposta:

\`\`\`text
Lote:
LOT-ETANOL-001

Recipientes:
LOT-ETANOL-001-R001
LOT-ETANOL-001-R002
LOT-ETANOL-001-R003
...
\`\`\`

O código é imutável após a criação.

### 11.6 Materialização na entrada

Exemplo:

\`\`\`text
Entrada:
tipo = FRASCO
quantidadeApresentacoes = 10
conteudoPorApresentacao = 500
unidade = ML
fracionavel = true
\`\`\`

Resultado:

\`\`\`text
Lote
├── R001 — 500 mL — FECHADO
├── R002 — 500 mL — FECHADO
├── ...
└── R010 — 500 mL — FECHADO
\`\`\`

O saldo do Lote e do EstoqueCentral passa a ser derivável/sincronizado pela soma dos recipientes.

### 11.7 Quando não materializar recipiente individual

A materialização individual é obrigatória quando:

- a apresentação é fracionável;
- a rastreabilidade física individual é relevante;
- existem estados aberto/fechado;
- a quantidade disponível de uma apresentação pode divergir das demais.

Para produtos puramente unitários e não fracionáveis, deve ser avaliado se a materialização individual traz benefício suficiente.

Estratégia recomendada para simplificar invariantes:

> materializar todas as apresentações físicas recebidas como \`RecipienteEstoque\`, inclusive não fracionáveis.

Assim existe uma única fonte operacional para saída, auditoria e concorrência.

Exemplo:

\`\`\`text
100 caixas não fracionáveis
→ 100 RecipienteEstoque FECHADO
\`\`\`

A desvantagem é maior volume de registros, mas para a escala prevista do SGL isso é aceitável e simplifica fortemente o domínio.

### 11.8 Relação com Lote

O Lote continua guardando:

- número do fornecedor;
- código interno;
- validade;
- data de entrada;
- apresentação declarada;
- fracionável;
- quantidade de apresentações recebidas;
- rastreabilidade institucional.

O saldo agregado em \`Lote.quantidadeDisponivel\` pode ser mantido inicialmente por compatibilidade/performance, mas deverá ser considerado **saldo derivado e validado** contra os recipientes.

### 11.9 Relação com MovimentacaoEstoque

\`MovimentacaoEstoque\` continua sendo o evento agregado por Produto/Lote/Pedido.

Para preservar o detalhe físico, será necessário um detalhe de movimentação por recipiente.

Proposta futura:

\`\`\`text
MovimentacaoEstoque
└── MovimentacaoRecipiente
    ├── recipiente
    ├── quantidadeAnterior
    ├── quantidadeMovimentada
    ├── quantidadeAtual
    ├── estadoAnterior
    └── estadoAtual
\`\`\`

Isso evita duplicar uma movimentação principal para cada recipiente e permite registrar uma única saída de 700 mL composta por:

\`\`\`text
500 mL do R001
+
200 mL do R004
\`\`\`

### 11.10 Devolução

A devolução não pode simplesmente restaurar o valor no Lote.

Ela deve usar o detalhe original da movimentação.

Casos:

\`\`\`text
saída de recipiente fechado inteiro
→ devolução pode restaurar o mesmo recipiente, se fisicamente devolvido intacto

saída parcial de recipiente
→ devolução exige decisão operacional:
   a) retorno ao mesmo recipiente
   b) novo recipiente identificado
   c) não permitir retorno físico ao estoque
\`\`\`

Essa política ainda precisa ser fechada antes da implementação de devolução no novo modelo.

### 11.11 Próxima migration

A maior versão existente atualmente é V32.

A primeira migration disponível para esta etapa é:

\`\`\`text
V33
\`\`\`

Nenhuma V33 deve ser criada até o fechamento completo de:

- \`TipoEmbalagem\`;
- \`RecipienteEstoque\`;
- detalhes de movimentação por recipiente;
- política de devolução;
- estratégia de backfill dos lotes existentes.

### 11.12 Política fechada — devolução de material fracionado

Decisão confirmada:

Material fracionado que saiu fisicamente do estoque **não retorna pelo fluxo normal de DEVOLUCAO**.

Motivo:

- não é possível garantir esterilidade, pureza, estabilidade ou integridade do conteúdo devolvido;
- a maior parte do fracionamento será consumida em preparo de Soluções;
- devolver, por exemplo, 10 mL de acetona ao recipiente original pode contaminar ou inutilizar o produto;
- o SGL não deve reconstruir artificialmente um saldo físico cuja condição real não pode ser garantida.

Portanto:

\`\`\`text
retirada inteira de recipiente fechado
→ pode admitir DEVOLUCAO, desde que o recipiente retorne íntegro/lacrado

retirada fracionada
→ DEVOLUCAO normal proibida
\`\`\`

### 11.13 Ajuste de estoque como única forma de reentrada de material fracionado

Se material fracionado precisar voltar fisicamente ao controle de estoque, isso será tratado como **AJUSTE DE ENTRADA**, não como devolução.

O ajuste deverá exigir:

- usuário/Gestor responsável;
- justificativa obrigatória;
- Produto;
- Lote de origem, quando conhecido;
- quantidade;
- unidade;
- estado físico;
- destino do ajuste.

Destinos possíveis:

\`\`\`text
A) NOVO_RECIPIENTE
B) RECIPIENTE_EXISTENTE
\`\`\`

#### A) Novo recipiente

É o comportamento recomendado por padrão.

Exemplo:

\`\`\`text
retornaram 10 mL de acetona

→ criar novo RecipienteEstoque
→ saldo inicial = 10 mL
→ estado = ABERTO
→ origem = AJUSTE
→ observação/justificativa obrigatória
\`\`\`

O novo recipiente recebe identificação própria e não é confundido com o recipiente original.

#### B) Recipiente existente

Permitido apenas quando o Gestor especificar explicitamente o recipiente de destino.

O backend deverá validar:

- mesmo Produto;
- mesma unidade canônica;
- mesmo lote quando a política exigir;
- recipiente não esgotado/inativo;
- capacidade máxima não excedida;
- justificativa obrigatória.

Essa ação deve ser auditada com saldo antes/depois.

O sistema nunca deve escolher automaticamente um recipiente existente para receber material de ajuste.

### 11.14 Segurança do ajuste

A existência do ajuste não significa que qualquer material devolvido é tecnicamente reutilizável.

A decisão física de aceitar o material é responsabilidade do Gestor.

A interface deve deixar claro:

> Ajuste de entrada corrige/reconcilia o estoque. Não representa uma devolução automática nem garante a integridade do material.

O histórico deve permitir distinguir:

\`\`\`text
COMPRA
DEVOLUCAO de recipiente íntegro
AJUSTE de entrada
AJUSTE de saída
INVENTARIO
DESCARTE
\`\`\`

### 11.15 Impacto no modelo atual

O enum atual já possui:

\`\`\`text
TipoMovimentacao.AJUSTE
OrigemMovimentacao.AJUSTE
\`\`\`

mas o backend atual não possui endpoint operacional específico para ajuste de estoque.

Na Etapa 7 deverá ser criado um fluxo explícito de ajuste, sem reutilizar de forma ambígua a entrada normal de lote ou a devolução de Pedido.

Sugestão de separação:

\`\`\`text
AJUSTE_ENTRADA
AJUSTE_SAIDA
\`\`\`

como intenção operacional no contrato/DTO, mesmo que a persistência continue usando \`TipoMovimentacao.AJUSTE\` com quantidade/sentido bem definidos.

A decisão final do contrato será feita antes da V33.

---

## 12. 7.1.4 — MovimentacaoRecipiente e contrato de Ajuste

### 12.1 Auditoria do modelo atual de movimentação

Hoje \`MovimentacaoEstoque\` concentra:

- Produto;
- EstoqueCentral;
- Lote;
- Pedido;
- Laboratório;
- Usuário;
- tipo/origem;
- quantidade movimentada;
- saldo agregado anterior/atual.

Esse modelo continua útil como evento principal de estoque.

Entretanto, para retiradas que usam mais de um recipiente, ele não consegue responder:

- quais recipientes foram afetados;
- quanto saiu de cada recipiente;
- qual era o saldo individual antes/depois;
- se o recipiente foi aberto ou esgotado naquela operação.

### 12.2 Nova entidade MovimentacaoRecipiente

Proposta:

\`\`\`java
MovimentacaoRecipiente
- id: Long
- publicId: UUID
- movimentacaoEstoque: MovimentacaoEstoque
- recipienteEstoque: RecipienteEstoque
- quantidadeAnterior: BigDecimal
- quantidadeMovimentada: BigDecimal
- quantidadeAtual: BigDecimal
- estadoAnterior: EstadoRecipienteEstoque
- estadoAtual: EstadoRecipienteEstoque
- abriuRecipiente: Boolean
- esgotouRecipiente: Boolean
\`\`\`

A entidade é detalhe da movimentação principal.

Relação:

\`\`\`text
MovimentacaoEstoque 1 → N MovimentacaoRecipiente
\`\`\`

### 12.3 Exemplo de retirada de 700 mL

Estado:

\`\`\`text
R001 — 500 mL — FECHADO
R004 — 350 mL — ABERTO
\`\`\`

Pedido:

\`\`\`text
700 mL
\`\`\`

Movimentação principal:

\`\`\`text
SAIDA
Produto: Etanol
Quantidade: 700 mL
Lote: ETANOL-001
\`\`\`

Detalhes:

\`\`\`text
R001
500 → 0 mL
FECHADO → ESGOTADO

R004
350 → 150 mL
ABERTO → ABERTO
\`\`\`

### 12.4 Uma movimentação pode afetar mais de um Lote

Como a regra FEFO pode precisar atravessar Lotes para atender um Pedido, o desenho deve preservar uma movimentação principal por Lote afetado, mantendo compatibilidade com o modelo atual.

Exemplo:

\`\`\`text
Pedido: 900 mL

MovimentacaoEstoque #1
→ Lote A
→ 600 mL
→ detalhes dos recipientes de A

MovimentacaoEstoque #2
→ Lote B
→ 300 mL
→ detalhes dos recipientes de B
\`\`\`

Isso preserva a rastreabilidade já existente por Lote e evita tornar \`MovimentacaoEstoque.lote\` ambíguo.

### 12.5 DTO de ajuste

Proposta conceitual:

\`\`\`java
AjusteEstoqueRequestDTO
- tipoAjuste: ENTRADA | SAIDA
- produtoId: UUID
- loteId: UUID
- recipienteId: UUID? 
- destinoEntrada: NOVO_RECIPIENTE | RECIPIENTE_EXISTENTE?
- tipoEmbalagem: TipoEmbalagem?
- quantidade: BigDecimal
- unidadeMedida: UnidadeMedida
- justificativa: String
- observacao: String?
\`\`\`

Regras:

\`\`\`text
AJUSTE_ENTRADA + NOVO_RECIPIENTE
→ recipienteId = null
→ cria RecipienteEstoque próprio
→ tipoEmbalagem obrigatório
→ quantidade inicial = quantidade ajustada
→ estado inicial = ABERTO

AJUSTE_ENTRADA + RECIPIENTE_EXISTENTE
→ recipienteId obrigatório
→ não cria recipiente novo
→ adiciona quantidade ao recipiente informado
→ valida capacidade

AJUSTE_SAIDA
→ recipienteId obrigatório quando houver recipientes individualizados
→ reduz saldo físico explicitamente
\`\`\`

### 12.6 Enum de intenção de ajuste

Proposta:

\`\`\`java
public enum TipoAjusteEstoque {
    ENTRADA,
    SAIDA
}
\`\`\`

Persistência principal:

\`\`\`text
TipoMovimentacao = AJUSTE
OrigemMovimentacao = AJUSTE
\`\`\`

O enum novo representa a intenção operacional no contrato, sem quebrar a semântica histórica da movimentação.

### 12.7 Destino do ajuste de entrada

Proposta:

\`\`\`java
public enum DestinoAjusteEntrada {
    NOVO_RECIPIENTE,
    RECIPIENTE_EXISTENTE
}
\`\`\`

Não permitir valor implícito.

O usuário/Gestor precisa saber se está:

- criando uma nova unidade física;
- ou corrigindo o saldo de uma unidade física já existente.

### 12.8 Regras do ajuste de saída

Ajuste de saída serve para reconciliação física, perda, evaporação, divergência de inventário ou correção operacional.

Não deve ser usado como substituto de:

- Pedido;
- Descarte por vencimento;
- consumo por Solução;
- devolução.

Obrigatório:

- justificativa;
- usuário responsável;
- recipiente alvo;
- saldo suficiente;
- histórico antes/depois.

### 12.9 Capacidade no ajuste de entrada

Para \`RECIPIENTE_EXISTENTE\`:

\`\`\`text
quantidadeDisponivel + ajuste
<= capacidadeInicial
\`\`\`

Caso contrário:

\`\`\`text
bloquear
→ sugerir NOVO_RECIPIENTE
\`\`\`

O backend não aumenta automaticamente a capacidade original de um recipiente para fazer o ajuste caber.

### 12.10 Ajuste de recipiente esgotado

Decisão recomendada:

\`\`\`text
ESGOTADO
→ não recebe ajuste de entrada
\`\`\`

Se material reaparecer fisicamente, criar novo recipiente.

Motivo: um recipiente marcado como esgotado representa o encerramento físico/auditável daquela unidade.

### 12.11 Ajuste e Lote

Quando o Lote é conhecido:

- ajuste deve permanecer no mesmo Lote;
- novo recipiente é criado dentro daquele Lote.

Quando o material não possui Lote confiável:

- não reutilizar artificialmente um Lote antigo;
- criar fluxo de lote de ajuste/inventário com origem explicitamente auditável.

A definição exata desse caso será fechada no desenho da V33/V34.

### 12.12 Resposta da API

\`MovimentacaoEstoqueResponseDTO\` deverá evoluir para incluir detalhes dos recipientes.

Conceito:

\`\`\`text
movimentacao
├── quantidade total
├── lote
└── recipientes[]
    ├── código
    ├── antes
    ├── movimentado
    ├── depois
    ├── estado anterior
    └── estado atual
\`\`\`

A interface pode manter a linha resumida e expandir os detalhes sob demanda.

### 12.13 Atomicidade do ajuste

O ajuste deve ser uma única transação:

\`\`\`text
validar
→ bloquear Estoque/Lote/Recipiente
→ alterar recipiente
→ recalcular/validar Lote
→ recalcular/validar EstoqueCentral
→ registrar MovimentacaoEstoque
→ registrar MovimentacaoRecipiente
→ commit
\`\`\`

Qualquer falha:

\`\`\`text
rollback integral
\`\`\`

### 12.14 Situação após este subbloco

Decisões já fechadas:

- unidade x apresentação;
- BigDecimal;
- RecipienteEstoque;
- estados FECHADO/ABERTO/ESGOTADO;
- FEFO;
- prioridade fechado x aberto conforme retirada;
- concorrência/revalidação;
- devolução de fracionado proibida;
- ajuste de entrada/saída auditável;
- MovimentacaoRecipiente como detalhe físico.

Pendências antes da primeira migration:

1. fechar estratégia de backfill dos Lotes existentes;
2. definir exatamente quais colunas agregadas permanecem em Lote/EstoqueCentral;
3. decidir política para registros legados com \`UnidadeMedida\` atualmente igual a CAIXA/FRASCO/AMPOLA/PAR/OUTRO;
4. dividir as migrations da Etapa 7 em ordem segura.

---

## 13. 7.1.5 — estratégia de backfill dos Lotes legados

### 13.1 Achado da massa atual

A auditoria dos initializers confirmou que parte dos dados atuais **não contém informação física suficiente** para reconstruir recipientes reais com segurança.

Exemplos encontrados:

- Produtos cadastrados com \`UnidadeMedida.FRASCO\` e \`UnidadeMedida.CAIXA\`;
- apresentação textual como "frasco de 500 mL", mas Lote com \`conteudoPorApresentacao = 1\`;
- \`quantidadeApresentacoes\` preenchida com o mesmo valor da quantidade total;
- lotes antigos tratados como "Legado" e fracionáveis;
- dados em que o texto da apresentação sugere uma capacidade, mas essa capacidade não está estruturada.

Conclusão:

> A migration não pode inferir automaticamente a distribuição física real dos Lotes atuais a partir de texto livre.

### 13.2 Regra central do backfill

**Não inventar recipientes históricos.**

Para cada Lote legado com saldo, criar inicialmente um único registro técnico de compatibilidade:

\`\`\`text
RecipienteEstoque LEGADO
→ representa apenas o saldo agregado conhecido do Lote
→ não afirma quantos frascos/caixas reais existem
→ não afirma se estão abertos ou fechados
\`\`\`

Esse registro serve como ponte de migração, não como representação física definitiva.

### 13.3 Estado adicional de reconciliação

Adicionar um estado explícito para dados migrados sem distribuição física confiável.

Proposta:

\`\`\`java
public enum EstadoRecipienteEstoque {
    NAO_RECONCILIADO,
    FECHADO,
    ABERTO,
    ESGOTADO
}
\`\`\`

Regras:

\`\`\`text
NAO_RECONCILIADO
→ somente para backfill legado
→ quantidade disponível conhecida
→ distribuição física desconhecida
→ não assume aberto/fechado
\`\`\`

Novos recipientes criados após a Etapa 7 nunca devem nascer como \`NAO_RECONCILIADO\`.

### 13.4 Criação do recipiente legado

Para Lote existente:

\`\`\`text
capacidadeInicial
→ quantidadeInicial atual convertida para BigDecimal

quantidadeDisponivel
→ quantidadeDisponivel atual convertida para BigDecimal

unidadeMedida
→ unidade canônica mapeada do Produto

estado
→ NAO_RECONCILIADO quando houver saldo
→ ESGOTADO quando saldo = 0

codigo
→ <codigo-lote>-RLEGACY
\`\`\`

Campo recomendado:

\`\`\`text
origemLegada = true
\`\`\`

ou equivalente para impedir que esse registro seja confundido com recipiente físico cadastrado normalmente.

### 13.5 Reconciliação física obrigatória

Criar fluxo operacional de **Reconciliação de estoque legado**.

O Gestor abre um Lote não reconciliado e informa como o saldo físico realmente está distribuído.

Exemplo:

\`\`\`text
Saldo legado conhecido:
4.950 mL de Etanol

Gestor confere fisicamente:

9 frascos fechados × 500 mL
1 frasco aberto × 450 mL
\`\`\`

Após confirmar:

\`\`\`text
R001 ... R009 → 500 mL FECHADO
R010          → 450 mL ABERTO

RLEGACY
→ encerrado/substituído pela reconciliação
\`\`\`

A soma informada deve ser exatamente igual ao saldo legado antes da confirmação.

### 13.6 Reconciliação não altera o saldo

Reconciliação:

\`\`\`text
não é ENTRADA
não é SAIDA
não é DEVOLUCAO
\`\`\`

Ela apenas transforma:

\`\`\`text
saldo agregado conhecido
→ distribuição física conhecida
\`\`\`

Por isso deve possuir evento próprio de auditoria ou origem \`INVENTARIO\`, sem alterar o total do EstoqueCentral.

### 13.7 Operações antes da reconciliação

Regra recomendada:

- consulta e relatórios continuam funcionando;
- saldo agregado continua visível;
- novos Lotes usam imediatamente o modelo novo;
- Lote legado com \`NAO_RECONCILIADO\` não pode executar retirada parcial automatizada;
- para qualquer operação que dependa de escolher recipiente físico, exigir reconciliação primeiro.

Isso evita que o sistema continue aprofundando uma incerteza histórica.

A interface deve apresentar:

\`\`\`text
"Este lote foi migrado do modelo anterior e ainda não possui
distribuição física dos recipientes confirmada.
Reconcilie o lote antes de realizar retirada fracionada."
\`\`\`

### 13.8 Produtos com unidade legada inválida

Mapeamento automático seguro:

\`\`\`text
ML, L, MG, G, KG, METRO, UNIDADE, REACAO
→ mantêm significado
\`\`\`

Valores antigos que são apresentações:

\`\`\`text
CAIXA
FRASCO
AMPOLA
PAR
OUTRO
\`\`\`

não devem ser convertidos silenciosamente para uma unidade física arbitrária.

Esses Produtos recebem estado de pendência cadastral, conceitualmente:

\`\`\`text
unidadeCanonicaPendente = true
\`\`\`

e precisam ser revisados pelo Gestor/Administrador.

A apresentação antiga deve ser preservada como pista de auditoria.

Exemplos:

\`\`\`text
Produto atual:
Ponteiras
unidade = CAIXA
apresentação = "caixa com 1000 unidades"

Revisão:
unidade canônica = UNIDADE
tipo de embalagem = CAIXA
conteúdo por apresentação = 1000 UNIDADE
\`\`\`

\`\`\`text
Produto atual:
BHI
unidade = FRASCO
apresentação = "frasco de 500 mL"

Revisão:
unidade canônica = ML
tipo de embalagem = FRASCO
conteúdo por apresentação = 500 ML
\`\`\`

Essa conversão pode ser sugerida pela interface, mas exige confirmação humana.

### 13.9 Produtos sem pendência

Produtos já cadastrados com unidade física válida podem ser migrados automaticamente:

\`\`\`text
L → L ou ML canônico conforme política escolhida
ML → ML
G → G ou MG canônico conforme política escolhida
MG → MG
KG → KG ou MG canônico conforme política escolhida
UNIDADE → UNIDADE
REACAO → REACAO
METRO → METRO
\`\`\`

A migration não deve mudar numericamente o saldo sem aplicar o fator de conversão correspondente.

### 13.10 Estratégia de rollout

Sequência segura proposta:

\`\`\`text
1. adicionar novas estruturas sem remover colunas antigas
2. converter colunas de saldo para DECIMAL
3. criar RecipienteEstoque
4. gerar RLEGACY para Lotes existentes
5. marcar Produtos de unidade ambígua como pendentes
6. manter leitura compatível
7. disponibilizar reconciliação no frontend
8. somente depois tornar recipiente a fonte operacional obrigatória
9. remover/deprecar campos antigos apenas em etapa posterior
\`\`\`

Essa estratégia permite rollback e reduz risco de indisponibilidade.

### 13.11 Backfill das movimentações históricas

Não criar \`MovimentacaoRecipiente\` retroativamente para movimentações antigas.

Motivo:

> não sabemos quais recipientes físicos participaram das saídas históricas.

Movimentações anteriores à migration permanecem válidas no nível de Lote.

Somente novas movimentações, depois da ativação do modelo de recipientes, geram detalhes por recipiente.

A interface deve aceitar histórico misto:

\`\`\`text
movimentação antiga
→ detalhe por Lote

movimentação nova
→ detalhe por Lote + Recipiente
\`\`\`

### 13.12 Resultado

A migration preserva 100% do saldo conhecido sem fabricar informação física inexistente.

O modelo passa a distinguir claramente:

\`\`\`text
saldo legado conhecido
≠
distribuição física confirmada
\`\`\`

Essa distinção é obrigatória para manter a auditoria confiável.

---

## 14. Decisão de simplificação — sem backfill legado de estoque

Decisão confirmada em 06/10/2026:

> Todos os dados atualmente existentes no SGL são fictícios/de desenvolvimento/teste e podem ser descartados.

Portanto, a estratégia de backfill descrita no item 13 fica **supersedida para a implementação atual**.

Não será necessário:

- criar \`RLEGACY\`;
- adicionar \`NAO_RECONCILIADO\` apenas para preservar dados atuais;
- reconstruir recipientes a partir de lotes antigos;
- manter compatibilidade física com saldos fictícios;
- migrar movimentações históricas de teste;
- criar fluxo de reconciliação apenas para dados descartáveis.

### 14.1 Estratégia adotada

A Etapa 7 poderá assumir banco limpo para o novo modelo de estoque.

Fluxo recomendado:

\`\`\`text
1. implementar novo schema/modelo
2. atualizar migrations Flyway da Etapa 7
3. atualizar initializers DEV/DEMO
4. apagar/recriar bancos locais e ambientes de homologação que só contenham massa fictícia
5. executar Flyway desde o início
6. popular novamente dados fictícios já no novo formato
7. validar estoque, recipientes, pedidos e movimentações sobre a nova modelagem
\`\`\`

### 14.2 Consequência para EstadoRecipienteEstoque

Como não há necessidade de preservar lote legado real, o estado volta a ser somente:

\`\`\`java
public enum EstadoRecipienteEstoque {
    FECHADO,
    ABERTO,
    ESGOTADO
}
\`\`\`

\`NAO_RECONCILIADO\` não será criado nesta versão.

### 14.3 Consequência para UnidadeMedida antiga

Valores antigos como:

\`\`\`text
CAIXA
FRASCO
AMPOLA
PAR
OUTRO
\`\`\`

não precisam ser migrados em registros existentes.

Em vez disso, os initializers serão corrigidos para usar:

\`\`\`text
unidade canônica real
+
TipoEmbalagem
+
conteúdo por apresentação
\`\`\`

Exemplos:

\`\`\`text
Ponteiras
→ UNIDADE
→ CAIXA
→ 1000 UNIDADE

BHI
→ ML
→ FRASCO
→ 500 ML
\`\`\`

### 14.4 Flyway

A decisão de descartar massa fictícia **não significa editar migrations históricas já consolidadas sem necessidade**.

Preferência:

- manter V1–V32 como histórico do projeto;
- criar as migrations da Etapa 7 a partir de V33;
- os ambientes DEV/DEMO podem ser recriados do zero e receber V1→V33+;
- se alguma migration histórica incompatível impedir a reconstrução limpa, avaliar separadamente uma correção controlada.

### 14.5 Resultado

A modelagem da Etapa 7 pode ser feita para o estado correto futuro, sem carregar complexidade exclusiva para preservar dados fictícios atuais.



---

## 15. Fechamento do 7.0 e entrada no 7.1

O bloco 7.0 está concluído.

Foram fechados:

- unidade de medida x apresentação;
- quantidade física em BigDecimal;
- estoque por recipiente físico;
- FEFO;
- preferência entre recipiente aberto/fechado;
- concorrência;
- política de devolução;
- ajuste de estoque;
- rastreabilidade por recipiente;
- descarte da massa fictícia atual.

O desenho detalhado das migrations e entidades está em:

\`\`\`text
docs/ETAPA_7_MODELO_ESTOQUE.md
\`\`\`

Situação:

\`\`\`text
7.0 ✅
7.1 🔧 atual
\`\`\`


### 15.1 Implementação 7.1-A

Enums de medida/apresentação e conversor implementados.

```text
7.1-A ✅ implementado
7.1-B ✅ BigDecimal nas entidades/DTOs e Services
7.1-C ✅ V33 aplicada e aplicação validada
7.1-D ✅ RecipienteEstoque + repository
7.1-E ✅ V34 aplicada e suíte verde
7.1-F ✅ MovimentacaoRecipiente + repository
7.1-G ✅ V35 aplicada e suíte verde
7.1-H ✅ entrada de lote materializando recipientes
7.1-I ✅ seleção FEFO + aberto/fechado
7.1-J ✅ concorrência + revalidação
7.1-K 🔧 próximo — ajuste de estoque
```

A suíte ainda precisa ser executada no ambiente local/CI antes de considerar validação concluída.


### 15.2 Fechamento do 7.1-B/C

Em 08/10/2026:

- conversão `Integer → BigDecimal` concluída;
- testes estabilizados após a migração dos contratos;
- `V33__normalize_stock_quantities.sql` criada e aplicada;
- aplicação validada executando com o schema PostgreSQL atualizado.

Commit da V33:

```text
7495c9a — Feat: V33
```

Próximo bloco:

```text
7.1-D — RecipienteEstoque + Repository
7.1-E — V34__create_stock_containers.sql
```


### 15.3 Fechamento do 7.1-D/E

Em 08/10/2026:

- `RecipienteEstoque` criado;
- repository criado;
- `V34__create_stock_containers.sql` aplicada;
- aplicação iniciou normalmente;
- suíte de testes permaneceu integralmente verde.

Commit:

```text
c10f438 — Feat: Criação do RecipienteEstoque + repository e V34
```

Próximo bloco:

```text
7.1-F — MovimentacaoRecipiente + Repository
7.1-G — V35__create_container_movement_details.sql
```


### 15.4 Fechamento do 7.1-F/G

Em 08/10/2026:

- `MovimentacaoRecipiente` criada;
- repository criado;
- `V35__create_container_movement_details.sql` aplicada;
- aplicação iniciou normalmente;
- suíte de testes permaneceu integralmente verde.

Commit:

```text
0628b1d — Feat: MovimentacaoRecipiente + repository e V35
```

Próximo bloco:

```text
7.1-H — entrada de lote materializando recipientes
```


### 15.5 Fechamento do 7.1-H

Em 09/10/2026:

- `registrarEntradaLote` passou a criar recipientes físicos automaticamente;
- cada apresentação recebida gera um `RecipienteEstoque`;
- todos os recipientes novos iniciam `FECHADO`;
- códigos `R001`, `R002`, ... são gerados dentro do Lote;
- capacidade/saldo usam o conteúdo por apresentação na unidade canônica do Produto;
- teste específico de materialização foi adicionado;
- suíte de testes permaneceu integralmente verde.

Commits:

```text
fc5e300 — Feat: Adição de nova modelagem para as apresentações físicas dos lotes e produtos
6581864 — Test: validar materializacao de recipientes na entrada de lote
```

Próximo bloco:

```text
7.1-I — seleção FEFO + preferência entre recipientes abertos/fechados
```


### 15.6 Fechamento do 7.1-I

Em 09/10/2026:

- a saída passou a consumir recipientes físicos;
- FEFO/FIFO continua definindo o Lote prioritário;
- dentro do Lote, retiradas inteiras preferem recipientes fechados e frações preferem recipientes abertos;
- recipientes podem transitar de `FECHADO` para `ABERTO` ou `ESGOTADO`, e de `ABERTO` para `ESGOTADO`;
- cada alteração física gera detalhe em `MovimentacaoRecipiente`;
- testes específicos de seleção física e transições foram adicionados;
- `PedidoConcorrenciaIntegrationTest` foi adaptado ao novo estoque por recipientes;
- suíte completa permaneceu verde.

Commits:

```text
db408ec — Feat: adicao da nova modelagem de saida de produtos
7c22cf7 — Test: cobrir selecao fisica de recipientes na saida
41c3cfd — Test: adaptar concorrencia ao estoque por recipientes
```

Próximo bloco:

```text
7.1-J — concorrência + revalidação no nível dos recipientes
```


### 15.7 Fechamento do 7.1-J

Em 09/10/2026:

- concorrência passou a incluir lock pessimista no nível de `RecipienteEstoque`;
- os recipientes do Lote são bloqueados em ordem determinística;
- o saldo físico é revalidado depois dos locks;
- divergência entre saldo agregado e saldo físico interrompe a operação;
- conflitos concorrentes usam `StockConflictException` e HTTP 409;
- o Pedido concorrente perdedor permanece `PENDENTE`;
- o teste integrado confirma que 7 recipientes são consumidos e 3 permanecem disponíveis no cenário de estoque 10 / pedidos 7 + 7;
- suíte completa permaneceu verde.

Commits:

```text
e8b4a54 — Feat: novo tratamento de exceções
4484efe — Test: validar locks e revalidacao fisica do estoque
```

Próximo bloco:

```text
7.1-K — ajuste de estoque
```


### 15.8 Fechamento do 7.1-K1

Em 09/10/2026:

- ajustes de estoque passaram a possuir fluxo explícito de entrada e saída;
- novo recipiente e recipiente existente são tratados de forma distinta;
- capacidade, estado físico e unidade canônica são validados;
- ajuste de saída atua diretamente sobre o recipiente informado;
- `MovimentacaoRecipiente` registra antes/depois do ajuste;
- relatório mantém visão consolidada de ajustes;
- `V36__allow_positive_stock_adjustments.sql` aplicada para permitir saldo atual do Lote acima da quantidade inicial após ajuste positivo auditado;
- suíte completa permaneceu verde.

Commits:

```text
93ba0b6 — Feat: Modelagem de AJUSTE_ENTRADA/AJUSTE_SAIDA
4abcfeb — Test: cobrir ajustes fisicos de estoque
cbe8557 — feat: adcao V36
```

Próximo subbloco:

```text
7.1-K2 — descarte e devolução no nível dos recipientes físicos
```
