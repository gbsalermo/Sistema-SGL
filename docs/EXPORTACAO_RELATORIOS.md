# Exportação de Relatórios — SGL

> **Estado vigente (27/10/2026; pré-merge):** Etapa 9 implementada em `collab/etapa-9-relatorios-consolidados` e validada funcionalmente pelo mantenedor, ainda sem integração à `main`. A exportação de Projetos foi adicionada; Movimentações reúne Resumo/Detalhamento no frontend, mantendo endpoints de exportação separados. Quantidades físicas do Detalhamento são opcionais via `mostrarQuantidades`. Estoque/Lotes e Fiscalização incluem recipientes/apresentações e rastreabilidade. Não presumir homologação para produção.

**Atualizado em:** 27/10/2026  
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
Projetos                ✅
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
GET /api/v1/relatorios/projetos/exportar
```

Movimentações aceita também filtros opcionais por produto, embalagem física, contexto Projeto/SCI/Atividade e forma de consumo histórico. A apresentação/unidade na UI combina filtros; não representa prova da modalidade originalmente solicitada. A opção `mostrarQuantidades=false` omite quantidades e saldos no arquivo; `true` os inclui. O método antigo de serviço permanece disponível para compatibilidade.

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
