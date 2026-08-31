# Exportação de Relatórios — SGL

**Atualizado em:** 31/08/2026  
**Estado:** ✅ concluída, validada e integrada à `main`.

## Objetivo

Permitir que o gestor exporte e imprima **um relatório por vez**, sempre usando a mesma consulta e os mesmos filtros da prévia exibida na Central de Relatórios.

---

## Histórico da implementação

Branches usadas no ciclo:

```text
Backend  → feat/relatorios-exportacao
Frontend → feat/relatorios-exportacao-interface
```

Essas branches representam o histórico de desenvolvimento, não trabalho pendente.

Integração concluída em 28/08/2026:

```text
Backend  → PR #9
Frontend → PR #14
```

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
Resíduos                ⏳ após integração do módulo
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

A exportação nunca combina tipos diferentes de relatório no mesmo arquivo.

```text
1 relatório selecionado
→ 1 arquivo
```

Um relatório composto pode possuir mais de uma seção/aba sem violar essa regra.

Exemplos:

```text
Estoque e Lotes.xlsx
├── Posição de estoque
└── Lotes

Fiscalização.xlsx
├── Produtos controlados
└── Rastreabilidade
```

---

## Comportamento do frontend

A interface exporta a **última prévia concluída**.

Isso evita o cenário:

```text
usuário consulta com filtro A
→ altera visualmente para filtro B
→ baixa arquivo sem consultar novamente
```

Trocar de relatório ou limpar/alterar o contexto da consulta invalida a exportação anterior até uma nova prévia válida ser gerada.

---

## Layout do PDF

- logo oficial do SGL no canto superior esquerdo;
- título e data de geração;
- filtros utilizados;
- indicadores-resumo;
- tabelas com cabeçalho repetido em novas páginas;
- orientação paisagem quando a quantidade de colunas exigir;
- conteúdo com quebra automática;
- numeração de páginas;
- margens compactas;
- formato A4.

---

## Layout do XLSX

- logo oficial do SGL no canto superior esquerdo;
- título, data e filtros;
- indicadores-resumo;
- uma aba por seção lógica quando necessário;
- cabeçalho congelado;
- autofiltro;
- quebra de texto;
- largura de coluna limitada;
- configuração de impressão A4;
- ajuste para uma página de largura;
- orientação paisagem para tabelas largas;
- repetição do cabeçalho durante impressão.

---

## Endpoints

Todos utilizam `formato=PDF` ou `formato=XLSX` e aceitam os filtros da prévia correspondente.

```text
GET /api/v1/relatorios/estagiarios/exportar
GET /api/v1/relatorios/produtos/exportar
GET /api/v1/relatorios/movimentacoes/exportar
GET /api/v1/relatorios/resumo-operacional/exportar
GET /api/v1/relatorios/estoque-lotes/exportar
GET /api/v1/relatorios/fiscalizacao/exportar
```

Exemplo:

```text
GET /api/v1/relatorios/fiscalizacao/exportar?formato=PDF&orgaoFiscalizador=POLICIA_FEDERAL
```

Para confirmar parâmetros e UUIDs, usar o Swagger/OpenAPI atual.

---

## Logo

Arquivo empacotado no backend:

```text
backend/sgl-backend/src/main/resources/relatorios/logo-sgl.png
```

---

## Validação

Validação manual registrada em 28/08/2026:

```text
prévia + filtros            ✅
PDF                         ✅
XLSX                        ✅
logo                        ✅
layout para impressão       ✅
fluxo frontend de exportar  ✅
```

---

## Próxima expansão

Não há pendência funcional nos seis relatórios atuais.

Quando Resíduos for reconciliado e integrado:

```text
consulta Resíduos
→ prévia
→ PDF
→ XLSX
```

A implementação deve reutilizar a mesma arquitetura e não criar uma segunda lógica de filtros apenas para exportação.
