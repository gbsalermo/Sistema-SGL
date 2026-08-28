# Exportação de Relatórios — SGL

## Objetivo

Permitir que o gestor exporte e imprima um relatório por vez, sempre usando a mesma consulta e os mesmos filtros da prévia exibida na central de Relatórios.

## Branch da etapa

Backend: `feat/relatorios-exportacao`

Frontend: `feat/relatorios-exportacao-interface`

As branches partem, respectivamente, de `feat/relatorios` e `feat/relatorios-interface`.

## Formatos

- PDF: OpenPDF 2.0.5, compatível com Java 17.
- XLSX: Apache POI 5.5.1.

## Relatórios exportáveis

- Estagiários;
- Produtos;
- Movimentações;
- Resumo operacional;
- Estoque e lotes;
- Fiscalização.

Resíduos será adicionado depois da integração do módulo de resíduos à base utilizada por Relatórios.

## Regra principal

A exportação nunca combina relatórios diferentes em um único arquivo.

Um arquivo representa exatamente um tipo de relatório e reutiliza os filtros da consulta correspondente.

Relatórios compostos podem possuir mais de uma seção no PDF ou mais de uma aba no XLSX sem deixar de representar um único relatório. Exemplos:

- Estoque e lotes: abas `Posição de estoque` e `Lotes`;
- Fiscalização: abas `Produtos controlados` e `Rastreabilidade`;
- Resumo operacional: entradas, saídas e lotes mais movimentados.

## Layout e impressão

### PDF

- logo oficial do SGL no canto superior esquerdo;
- título e data de geração;
- filtros utilizados;
- indicadores-resumo;
- tabelas com cabeçalho repetido em novas páginas;
- orientação paisagem quando a quantidade de colunas exigir;
- conteúdo das células com quebra automática;
- numeração de páginas no rodapé;
- margens reduzidas para aproveitar melhor a folha A4.

### XLSX

- logo oficial do SGL no canto superior esquerdo;
- título, data de geração e filtros;
- indicadores-resumo;
- uma aba por seção lógica do relatório;
- cabeçalho congelado;
- autofiltro;
- quebra de texto nas células;
- largura de coluna automática com limite máximo;
- configuração de impressão A4;
- ajuste para uma página de largura;
- orientação paisagem para tabelas largas;
- cabeçalho da tabela repetido durante a impressão.

## Endpoints

Todos utilizam `formato=PDF` ou `formato=XLSX` e aceitam os mesmos filtros do endpoint de prévia correspondente.

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

## Logo

A logo usada nos arquivos está empacotada em:

```text
backend/sgl-backend/src/main/resources/relatorios/logo-sgl.png
```

Ela reutiliza o mesmo arquivo já mantido na documentação do SGL.
