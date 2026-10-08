# Etapa 7 — Blueprint de migrations do estoque

**Branch:** \`collab/etapa-7-unidades-solucoes-contexto\`  
**Banco principal:** PostgreSQL  
**Maior migration consolidada anterior:** V32  
**Dados atuais:** DEV/DEMO/teste — descartáveis  
**Objetivo:** preparar o novo modelo de unidade, quantidade decimal, recipientes físicos e rastreabilidade por recipiente.

---

## 1. Decisão sobre unidade persistida

\`Produto.unidadeMedida\` será a **unidade canônica operacional daquele Produto**.

Exemplos:

\`\`\`text
Etanol   → ML
NaCl     → G
Agarose  → G
Ponteira → UNIDADE
Kit PCR  → REACAO
\`\`\`

As quantidades persistidas de Estoque, Lote, Recipiente, Pedido e Movimentação daquele Produto são normalizadas para essa unidade.

Os fatores de conversão de \`UnidadeMedida\` servem para converter entrada/solicitação para a unidade canônica do Produto.

Exemplo:

\`\`\`text
Produto Etanol → canônica ML

entrada: 1 L
persistência: 1000.000000 ML
\`\`\`

A unidade-base do grupo (mL para volume, mg para massa etc.) é referência matemática de conversão, não uma segunda unidade persistida do saldo.

---

## 2. Saldos agregados

Manter:

\`\`\`text
EstoqueCentral.quantidadeAtual
Lote.quantidadeInicial
Lote.quantidadeDisponivel
\`\`\`

mas convertidos para \`BigDecimal / NUMERIC(19,6)\`.

Papel:

- acelerar consultas e relatórios;
- preservar contratos consolidados durante a transição;
- oferecer saldo resumido.

Fonte física operacional após a implantação:

\`\`\`text
RecipienteEstoque
\`\`\`

Invariante obrigatória:

\`\`\`text
Lote.quantidadeDisponivel
=
SUM(RecipienteEstoque.quantidadeDisponivel)

EstoqueCentral.quantidadeAtual
=
SUM(Lote.quantidadeDisponivel)
\`\`\`

Os saldos agregados nunca são alterados isoladamente por fluxos normais.

---

## 3. V33 — normalize_stock_quantities

Arquivo planejado:

\`\`\`text
V33__normalize_stock_quantities.sql
\`\`\`

Converter para \`NUMERIC(19,6)\`:

### estoque_central

\`\`\`text
quantidade_atual
quantidade_minima
\`\`\`

### lote

\`\`\`text
quantidade_inicial
quantidade_disponivel
conteudo_por_apresentacao
\`\`\`

Permanece INTEGER:

\`\`\`text
quantidade_apresentacoes
\`\`\`

### itens_pedido

Converter:

\`\`\`text
quantidade_solicitada
quantidade_aprovada
multiplicador_solicitado
\`\`\`

Permanece INTEGER:

\`\`\`text
quantidade_embalagens_solicitada
\`\`\`

### movimentacao_estoque

Converter:

\`\`\`text
quantidade_movimentada
quantidade_anterior
quantidade_atual
\`\`\`

### historico_laboratorio

Converter:

\`\`\`text
quantidade
\`\`\`

Motivo: um laboratório pode receber quantidade fracionada aprovada.

### Constraints

Adicionar/verificar:

\`\`\`text
quantidades de saldo >= 0
quantidade solicitada > 0
quantidade aprovada > 0 quando não nula
multiplicador > 0
\`\`\`

Como a base é descartável, não será criado backfill físico.

---

## 4. V34 — create_stock_containers

Arquivo planejado:

\`\`\`text
V34__create_stock_containers.sql
\`\`\`

Tabela:

\`\`\`text
recipientes_estoque
\`\`\`

Colunas:

\`\`\`text
id                      BIGINT identity PK
public_id               UUID NOT NULL UNIQUE
lote_id                 BIGINT NOT NULL FK
sequencial              INTEGER NOT NULL
codigo_interno          VARCHAR(180) NOT NULL UNIQUE
tipo_embalagem          VARCHAR(30) NOT NULL
capacidade_inicial      NUMERIC(19,6) NOT NULL
quantidade_disponivel   NUMERIC(19,6) NOT NULL
unidade_medida          VARCHAR(30) NOT NULL
estado                  VARCHAR(30) NOT NULL
data_abertura           TIMESTAMP NULL
data_esgotamento        TIMESTAMP NULL
observacao              VARCHAR(500) NULL
\`\`\`

Não adicionar \`ativo\` nesta etapa: o ciclo físico é representado por \`estado\`. A política geral de delete lógico será analisada na Etapa 11.

Unique:

\`\`\`text
(lote_id, sequencial)
codigo_interno
public_id
\`\`\`

Checks:

\`\`\`text
capacidade_inicial > 0
quantidade_disponivel >= 0
quantidade_disponivel <= capacidade_inicial
sequencial > 0
\`\`\`

Consistência de estado:

\`\`\`text
FECHADO
→ quantidade_disponivel = capacidade_inicial
→ data_abertura IS NULL
→ data_esgotamento IS NULL

ABERTO
→ quantidade_disponivel > 0
→ data_abertura IS NOT NULL
→ data_esgotamento IS NULL

ESGOTADO
→ quantidade_disponivel = 0
→ data_esgotamento IS NOT NULL
\`\`\`

Observação: um recipiente ABERTO pode voltar a possuir quantidade igual à capacidade por AJUSTE, mas continua ABERTO; capacidade cheia não restaura lacre/integridade física.

Por isso o CHECK de ABERTO aceita:

\`\`\`text
0 < quantidade_disponivel <= capacidade_inicial
\`\`\`

---

## 5. V35 — create_container_movement_details

Arquivo planejado:

\`\`\`text
V35__create_container_movement_details.sql
\`\`\`

Tabela:

\`\`\`text
movimentacoes_recipiente
\`\`\`

Colunas:

\`\`\`text
id                       BIGINT identity PK
public_id                UUID NOT NULL UNIQUE
movimentacao_estoque_id  BIGINT NOT NULL FK
recipiente_estoque_id    BIGINT NOT NULL FK
quantidade_anterior      NUMERIC(19,6) NOT NULL
quantidade_movimentada   NUMERIC(19,6) NOT NULL
quantidade_atual         NUMERIC(19,6) NOT NULL
estado_anterior          VARCHAR(30) NOT NULL
estado_atual             VARCHAR(30) NOT NULL
abriu_recipiente         BOOLEAN NOT NULL
esgotou_recipiente       BOOLEAN NOT NULL
\`\`\`

Unique recomendado:

\`\`\`text
(movimentacao_estoque_id, recipiente_estoque_id)
\`\`\`

Checks:

\`\`\`text
quantidade_anterior >= 0
quantidade_movimentada > 0
quantidade_atual >= 0
\`\`\`

A direção da quantidade é dada pela movimentação principal; o detalhe registra magnitude positiva.

---

## 6. TipoMovimentacao

Para auditoria, substituir a ambiguidade do \`AJUSTE\` genérico por tipos explícitos no código:

\`\`\`text
ENTRADA
SAIDA
AJUSTE_ENTRADA
AJUSTE_SAIDA
DEVOLUCAO
DESCARTE_VENCIMENTO
\`\`\`

\`OrigemMovimentacao.AJUSTE\` permanece.

Como os dados são descartáveis, não há necessidade de compatibilidade com registros históricos reais do valor \`AJUSTE\`.

---

## 7. Enums definitivos deste bloco

### DimensaoMedida

\`\`\`text
VOLUME
MASSA
COMPRIMENTO
CONTAGEM
\`\`\`

### GrupoConversaoMedida

\`\`\`text
VOLUME
MASSA
COMPRIMENTO
UNIDADE
REACAO
\`\`\`

### UnidadeMedida

\`\`\`text
ML
L
MG
G
KG
METRO
UNIDADE
REACAO
\`\`\`

\`OUTRO\` não será unidade operacional conversível nesta primeira versão.

### TipoEmbalagem

\`\`\`text
UNITARIO
FRASCO
AMPOLA
GARRAFA
GALAO
CAIXA
KIT
PACOTE
SACO
TUBO
POTE
PAR
OUTRO
\`\`\`

### EstadoRecipienteEstoque

\`\`\`text
FECHADO
ABERTO
ESGOTADO
\`\`\`

### TipoAjusteEstoque

\`\`\`text
ENTRADA
SAIDA
\`\`\`

### DestinoAjusteEntrada

\`\`\`text
NOVO_RECIPIENTE
RECIPIENTE_EXISTENTE
\`\`\`

---

## 8. Entidades Java a alterar/criar

Alterar:

\`\`\`text
Produto
EstoqueCentral
Lote
ItemPedido
MovimentacaoEstoque
HistoricoLaboratorio
\`\`\`

Criar:

\`\`\`text
RecipienteEstoque
MovimentacaoRecipiente
\`\`\`

Criar enums:

\`\`\`text
DimensaoMedida
GrupoConversaoMedida
EstadoRecipienteEstoque
TipoAjusteEstoque
DestinoAjusteEntrada
\`\`\`

Revisar:

\`\`\`text
UnidadeMedida
TipoEmbalagem
TipoMovimentacao
\`\`\`

---

## 9. DTOs impactados

No mínimo:

\`\`\`text
EntradaLoteRequestDTO
AtualizarLoteRequestDTO
LoteResponseDTO
EstoqueCentralRequestDTO
EstoqueCentralResponseDTO
ItemPedidoRequestDTO
AprovarPedidoRequestDTO
PedidoResponseDTO
MovimentacaoEstoqueResponseDTO
Relatórios que expõem quantidade
\`\`\`

Criar:

\`\`\`text
AjusteEstoqueRequestDTO
RecipienteEstoqueResponseDTO
MovimentacaoRecipienteResponseDTO
\`\`\`

---

## 10. Ordem de implementação

\`\`\`text
7.1-A enums e conversor de unidade
7.1-B BigDecimal nas entidades/DTOs
7.1-C V33
7.1-D RecipienteEstoque + repository
7.1-E V34
7.1-F MovimentacaoRecipiente + repository
7.1-G V35
7.1-H entrada de lote materializando recipientes
7.1-I seleção FEFO + aberto/fechado
7.1-J concorrência e revalidação
7.1-K ajuste de estoque
7.1-L DEV/DEMO data reset
7.1-M testes
\`\`\`

Somente depois seguir para o domínio de Soluções.


---

## 11. Implementação 7.1-A

Implementado na branch da Etapa 7:

- `DimensaoMedida`;
- `GrupoConversaoMedida`;
- `UnidadeMedida` com metadados de dimensão/grupo/fator;
- remoção de apresentações do enum `UnidadeMedida`;
- expansão de `TipoEmbalagem`;
- `EstadoRecipienteEstoque`;
- `TipoAjusteEstoque`;
- `DestinoAjusteEntrada`;
- `ConversorUnidadeMedida`;
- testes unitários do conversor;
- atualização mínima dos initializers para não usar CAIXA/FRASCO como unidade.

A alteração de `TipoMovimentacao.AJUSTE` para entrada/saída explícitas fica para o bloco do fluxo de ajustes, evitando antecipar comportamento funcional antes do novo estoque estar implementado.

Próximo bloco:

```text
7.1-B — BigDecimal nas entidades e DTOs
```


---

## 12. Implementação 7.1-B e 7.1-C

Validação confirmada em 08/10/2026.

### 7.1-B — BigDecimal

Concluído:

- entidades migradas para `BigDecimal`;
- DTOs de entrada/saída atualizados;
- Services adaptados;
- relatórios adaptados;
- initializers adaptados;
- suíte de testes novamente estabilizada.

### 7.1-C — V33

Migration aplicada:

```text
V33__normalize_stock_quantities.sql
```

Commit funcional:

```text
7495c9a — Feat: V33
```

A aplicação foi validada rodando com o PostgreSQL já migrado para `NUMERIC(19,6)`.

Situação:

```text
7.1-A ✅
7.1-B ✅
7.1-C ✅
7.1-D ✅ RecipienteEstoque + repository
7.1-E ✅ V34 aplicada e validada
7.1-F 🔧 próximo — MovimentacaoRecipiente + repository
7.1-G ⏳ V35
```


---

## 13. Implementação 7.1-D e 7.1-E

Validação confirmada em 08/10/2026.

Concluído:

- entidade `RecipienteEstoque`;
- `RecipienteEstoqueRepository`;
- migration `V34__create_stock_containers.sql`;
- constraints de capacidade, saldo e estado físico;
- índices por Lote e estado;
- validação da aplicação com Flyway/Hibernate;
- suíte de testes integral verde.

Commit funcional:

```text
c10f438 — Feat: Criação do RecipienteEstoque + repository e V34
```

Situação atual:

```text
7.1-D ✅
7.1-E ✅
7.1-F 🔧 próximo — MovimentacaoRecipiente + repository
7.1-G ⏳ V35
```
