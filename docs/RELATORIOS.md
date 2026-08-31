# Relatórios — SGL

**Atualizado em:** 31/08/2026  
**Estado:** consultas, prévias e exportações dos relatórios atuais concluídas e integradas à `main`.

## Objetivo

Centralizar consultas operacionais, gerenciais e de fiscalização com filtros específicos por contexto e exportação oficial em PDF/XLSX.

## Regra arquitetural

O SGL utiliza endpoints/serviços específicos por relatório, em vez de um endpoint genérico com todas as combinações possíveis.

```text
filtro do relatório
→ service de consulta
→ prévia JSON
→ mesma consulta para PDF/XLSX
```

O frontend não deve recriar os cálculos ou regras de composição do relatório.

---

## Relatórios atuais

### 1. Estagiários ✅

Cobertura:

- todos, ativos e inativos;
- por laboratório;
- por período de vínculo.

Endpoint:

```text
GET /api/v1/relatorios/estagiarios
```

### 2. Produtos ✅

Cobertura:

- todos, ativos e inativos;
- perecíveis e não perecíveis;
- por nível/tipo de risco conforme contrato;
- fiscalizados e não fiscalizados;
- por órgão fiscalizador;
- visão geral do catálogo.

Endpoint:

```text
GET /api/v1/relatorios/produtos
```

### 3. Movimentações ✅

Cobertura:

- entradas;
- saídas;
- ajustes;
- devoluções;
- descartes;
- filtros por período, produto, laboratório, lote, responsável e origem conforme contrato atual.

Endpoint:

```text
GET /api/v1/relatorios/movimentacoes
```

#### Pedidos entregues

Pedidos entregues **não possuem relatório dedicado**.

Quando a gestão precisar analisar material movimentado por pedidos, usar Movimentações com recorte semelhante a:

```text
origem = PEDIDO
tipo = SAIDA
```

`Pedido.dataEntrega` permanece no domínio porque registra um evento real e pode ser usado em auditoria/consultas futuras.

### 4. Resumo operacional ✅

Cobertura:

- total de movimentações;
- entradas;
- saídas;
- descartes;
- produtos movimentados;
- lotes movimentados;
- principais entradas;
- principais saídas;
- lotes mais movimentados.

Endpoint:

```text
GET /api/v1/relatorios/resumo-operacional
```

### 5. Estoque e lotes ✅

Cobertura:

- posição atual;
- estoque baixo;
- lotes ativos;
- lotes próximos do vencimento;
- lotes vencidos;
- lotes esgotados;
- filtros por unidade, produto e situação.

Endpoint:

```text
GET /api/v1/relatorios/estoque-lotes
```

Classificações atuais:

```text
VALIDO
PROXIMO_VENCIMENTO
VENCIDO
SEM_VALIDADE
ESGOTADO
INATIVO
```

### 6. Fiscalização ✅

É um recorte especializado apenas dos produtos explicitamente classificados como fiscalizados/controlados.

Cobertura:

- produto;
- órgão fiscalizador;
- unidade;
- período;
- janela de vencimento;
- saldo atual;
- lotes ativos/vencidos/próximos do vencimento;
- entradas e saídas;
- rastreabilidade por lote;
- destino da saída: laboratório, projeto, solicitante e pedido;
- responsável pela movimentação.

Endpoint:

```text
GET /api/v1/relatorios/fiscalizacao
```

### 7. Resíduos 🟡

Planejado para apresentar:

- resíduos informados e situação atual;
- laboratório, projeto, gerador e gestor;
- riscos informados e confirmados;
- recipiente e quantidade;
- armazenamento temporário;
- destino previsto e final;
- datas operacionais;
- composição do resíduo.

Ainda não está ativo porque depende da reconciliação e integração do módulo de Resíduos existente em `feat/gestao-residuos`.

Essa branch está divergente da `main` e não deve ser mergeada diretamente sem portabilidade/migration adequada. Ver `../CONTINUIDADE.md` e `DOSSIE_PROJETO_SGL.md`.

---

## Produtos x Fiscalização

O relatório de Produtos é a visão cadastral geral.

O relatório de Fiscalização é uma visão especializada de rastreabilidade de produtos controlados.

```text
Produtos
→ catálogo + filtros gerais

Fiscalização
→ somente produtos marcados como fiscalizados
→ saldo + lotes + vencimentos + entradas/saídas + destino
```

Não deduzir `fiscalizado` a partir de risco ou perecibilidade.

---

## Regra de cadastro de Produto para fiscalização

A classificação pertence ao cadastro do Produto:

```text
fiscalizado
orgaosFiscalizadores
observacaoFiscalizacao
```

Quando `fiscalizado = false`:

```text
órgãos = vazio
observação = limpa
```

Quando `fiscalizado = true`:

```text
pelo menos um órgão obrigatório
```

Órgãos iniciais:

- Polícia Federal;
- Vigilância Sanitária;
- ANVISA;
- Exército;
- Outro.

A próxima tela `Administração → Cadastros → Produtos` deve expor essa configuração na criação e edição.

---

## Exportação — concluída ✅

Formatos oficiais:

```text
PDF  → OpenPDF 2.0.5
XLSX → Apache POI 5.5.1
```

Regra:

```text
prévia + PDF + XLSX
→ mesma consulta
→ mesmos filtros
```

Cada arquivo representa um relatório. Relatórios compostos podem possuir várias seções ou abas internas.

Endpoints:

```text
GET /api/v1/relatorios/estagiarios/exportar
GET /api/v1/relatorios/produtos/exportar
GET /api/v1/relatorios/movimentacoes/exportar
GET /api/v1/relatorios/resumo-operacional/exportar
GET /api/v1/relatorios/estoque-lotes/exportar
GET /api/v1/relatorios/fiscalizacao/exportar
```

Usar `formato=PDF` ou `formato=XLSX` e os mesmos filtros da prévia correspondente.

Detalhes: `EXPORTACAO_RELATORIOS.md`.

---

## Estado de validação

Em 28/08/2026 foram validados manualmente:

```text
Estagiários             ✅
Produtos                ✅
Movimentações           ✅
Resumo operacional      ✅
Estoque e lotes         ✅
Fiscalização            ✅
PDF                     ✅
XLSX                    ✅
```

O ciclo foi integrado à `main`; exportação não é mais uma “próxima decisão técnica”.

---

## Próxima evolução deste módulo

A próxima expansão específica de Relatórios será feita **após a integração do domínio de Resíduos**:

```text
Resíduos operacional
→ consulta/relatório de Resíduos
→ PDF/XLSX de Resíduos
```

Não criar novos relatórios apenas por conveniência visual; primeiro confirmar a necessidade operacional e o contrato de dados.
