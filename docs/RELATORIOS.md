# Relatórios — SGL

**Atualizado em:** 03/09/2026  
**Estado:** consultas, prévias e exportações dos relatórios do primeiro protótipo estão integradas à `main`.

## Objetivo

Centralizar consultas operacionais, gerenciais, institucionais e de fiscalização com filtros específicos e exportação oficial PDF/XLSX.

## Regra arquitetural

```text
filtro do relatório
→ service de consulta
→ prévia JSON
→ mesma consulta para PDF/XLSX
```

O frontend não recria cálculos oficiais de relatório.

---

## Relatórios atuais

### 1. Estagiários ✅

```text
GET /api/v1/relatorios/estagiarios
GET /api/v1/relatorios/estagiarios/exportar?formato=PDF|XLSX
```

Cobertura:

- todos, ativos e inativos;
- laboratório;
- período de vínculo;
- situação e dados do estágio conforme contrato atual.

### 2. Produtos ✅

```text
GET /api/v1/relatorios/produtos
GET /api/v1/relatorios/produtos/exportar?formato=PDF|XLSX
```

Cobertura:

- ativos/inativos;
- perecibilidade;
- risco;
- fiscalização;
- órgão fiscalizador;
- visão cadastral do catálogo.

### 3. Movimentações ✅

```text
GET /api/v1/relatorios/movimentacoes
GET /api/v1/relatorios/movimentacoes/exportar?formato=PDF|XLSX
```

Cobertura inclui entradas, saídas, ajustes, devoluções e descartes, com filtros por período e contexto conforme Swagger.

#### Pedidos entregues

**Não existe relatório dedicado.** Usar Movimentações com recorte semelhante a:

```text
origem = PEDIDO
tipo = SAIDA
```

### 4. Resumo operacional ✅

```text
GET /api/v1/relatorios/resumo-operacional
GET /api/v1/relatorios/resumo-operacional/exportar?formato=PDF|XLSX
```

Consolida movimentações, entradas, saídas, descartes, produtos/lotes movimentados e rankings operacionais.

### 5. Estoque e lotes ✅

```text
GET /api/v1/relatorios/estoque-lotes
GET /api/v1/relatorios/estoque-lotes/exportar?formato=PDF|XLSX
```

Situações atuais:

```text
VALIDO
PROXIMO_VENCIMENTO
VENCIDO
SEM_VALIDADE
ESGOTADO
INATIVO
```

### 6. Fiscalização ✅

```text
GET /api/v1/relatorios/fiscalizacao
GET /api/v1/relatorios/fiscalizacao/exportar?formato=PDF|XLSX
```

É um recorte especializado de produtos explicitamente marcados como fiscalizados. Não inferir fiscalização por risco ou perecibilidade.

### 7. Resíduos ✅

```text
GET /api/v1/relatorios/residuos
GET /api/v1/relatorios/residuos/exportar?formato=PDF|XLSX
```

Filtros previstos pelo contrato atual:

```text
status
laboratorioId
nivelRisco
dataInicio
dataFim
```

Resumo inclui:

```text
total
informados
emAnalise
liberados
armazenados
despachados
altoRisco
```

Os itens preservam rastreabilidade do gerador, laboratório, status, riscos, quantidade e ciclo operacional.

### 8. Pessoas por laboratório ✅

```text
GET /api/v1/relatorios/pessoas-laboratorio?laboratorioId=...
GET /api/v1/relatorios/pessoas-laboratorio/exportar?formato=PDF|XLSX&laboratorioId=...
```

Objetivo: auditoria institucional de **todas as pessoas vinculadas ao laboratório**, não apenas estagiários.

Filtros adicionais:

```text
perfil
ativo
```

Retorno contempla:

```text
laboratório
unidade
responsável
nome/e-mail
perfil
ativo/inativo
marcação de responsável
estagiário: tipo de vínculo + período
totais por perfil
```

---

## Produtos x Fiscalização

```text
Produtos
→ catálogo e filtros gerais

Fiscalização
→ somente fiscalizados
→ saldo/lotes/vencimentos/movimentações/rastreabilidade
```

Campos de fiscalização pertencem ao Produto:

```text
fiscalizado
orgaosFiscalizadores
observacaoFiscalizacao
```

```text
fiscalizado=false → órgãos vazios e observação limpa
fiscalizado=true  → ao menos um órgão obrigatório
```

---

## Exportação

Formatos:

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

Cada arquivo representa um único tipo de relatório; um relatório composto pode possuir várias seções/abas internas.

Detalhes: `EXPORTACAO_RELATORIOS.md`.

---

## Estado de validação

O ciclo base de relatórios/exportações foi validado e integrado em 28/08/2026. Resíduos e Pessoas por laboratório foram implementados/integrados depois desse ciclo.

A homologação completa do primeiro protótipo deve repetir os fluxos integrados, incluindo:

```text
prévia de Resíduos
PDF/XLSX de Resíduos
Pessoas por laboratório
PDF/XLSX de Pessoas por laboratório
combinação de filtros
consistência entre prévia e arquivo
```

Não marcar essa bateria final como substituída apenas pela existência dos commits.

---

## Próxima evolução

Não há novo relatório obrigatório definido antes do congelamento do primeiro protótipo.

Qualquer novo relatório deve nascer de necessidade operacional real e contrato de dados claro, evitando duplicar telas ou criar relatórios apenas por conveniência visual.
