# Exportação de Relatórios — SGL

**Atualizado em:** 03/09/2026  
**Estado:** ✅ implementada e integrada; a homologação final deve revalidar o conjunto completo.

## Objetivo

Permitir que a Gestão exporte **um relatório por vez**, usando os mesmos filtros e a mesma consulta da prévia exibida no frontend.

---

## Formatos oficiais

```text
PDF  → OpenPDF 2.0.5
XLSX → Apache POI 5.5.1
```

---

## Relatórios exportáveis

```text
Estagiários             ✅
Produtos                ✅
Movimentações           ✅
Resumo operacional      ✅
Estoque e lotes         ✅
Fiscalização            ✅
Resíduos                ✅
Pessoas por laboratório ✅
```

---

## Regra principal

```text
prévia JSON
PDF
XLSX
→ mesma consulta
→ mesmos filtros
```

```text
1 relatório selecionado
→ 1 arquivo
```

Um relatório composto pode possuir seções ou abas internas.

---

## Endpoints

```text
GET /api/v1/relatorios/estagiarios/exportar
GET /api/v1/relatorios/produtos/exportar
GET /api/v1/relatorios/movimentacoes/exportar
GET /api/v1/relatorios/resumo-operacional/exportar
GET /api/v1/relatorios/estoque-lotes/exportar
GET /api/v1/relatorios/fiscalizacao/exportar
GET /api/v1/relatorios/residuos/exportar
GET /api/v1/relatorios/pessoas-laboratorio/exportar
```

Todos usam `formato=PDF` ou `formato=XLSX` e aceitam os filtros do relatório correspondente. Confirmar nomes e parâmetros atuais no Swagger/OpenAPI.

Exemplos:

```text
GET /api/v1/relatorios/residuos/exportar?formato=PDF&status=EM_ANALISE
GET /api/v1/relatorios/pessoas-laboratorio/exportar?formato=XLSX&laboratorioId=<UUID>
```

---

## Comportamento do frontend

A exportação deve representar a **última prévia válida**.

```text
consulta com filtro A
→ prévia A
→ exportar A
```

Alterar filtros/contexto sem gerar nova prévia deve invalidar a exportação anterior quando a tela usar esse padrão.

---

## PDF

Diretrizes do primeiro protótipo:

- identidade SGL;
- título e data de geração;
- filtros aplicados;
- indicadores/resumo quando houver;
- tabelas legíveis;
- A4;
- orientação paisagem para relatórios largos;
- paginação e cabeçalhos adequados para impressão.

O relatório de Resíduos também inclui resumo operacional e rastreabilidade do ciclo.

---

## XLSX

Diretrizes:

- título, data e filtros;
- resumo quando aplicável;
- dados completos;
- autofiltro;
- cabeçalho congelado;
- largura de colunas ajustada;
- abas adicionais quando o relatório exigir seções lógicas.

---

## Logo

Recurso empacotado no backend:

```text
backend/sgl-backend/src/main/resources/relatorios/logo-sgl.png
```

---

## Histórico

O ciclo inicial de exportação foi integrado em 28/08/2026:

```text
Backend  → PR #9
Frontend → PR #14
```

Depois disso foram adicionadas as exportações de **Resíduos** e **Pessoas por laboratório**.

Branches antigas de implementação representam histórico, não trabalho pendente.

---

## Homologação final

A bateria final do protótipo deve conferir, para todos os relatórios:

```text
filtros
prévia
PDF
XLSX
consistência prévia ↔ arquivo
nomes/tipos de colunas
casos vazios
impressão/legibilidade
```

Resíduos e Pessoas por laboratório merecem atenção especial porque foram adicionados após a validação manual do ciclo inicial de 28/08.
