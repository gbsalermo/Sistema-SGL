# Relatórios — SGL

## Objetivo

Centralizar consultas operacionais, gerenciais e de fiscalização com exportação oficial em PDF e XLSX.

## Relatórios previstos

1. Estagiários
   - todos, ativos e inativos
   - por laboratório
   - por período de vínculo

2. Produtos
   - todos, ativos e inativos
   - perecíveis e não perecíveis
   - por nível de risco
   - fiscalizados e não fiscalizados
   - por órgão fiscalizador
   - visão cadastral geral do catálogo

3. Movimentações
   - entradas, saídas, ajustes, devoluções e descartes
   - filtros por período, produto, laboratório, lote e responsável
   - movimentações originadas por pedido permanecem como recorte/filtro deste relatório, e não como relatório separado

4. Resumo operacional
   - maiores entradas
   - maiores saídas
   - quantidade de movimentações
   - lotes mais movimentados

5. Estoque e lotes
   - posição atual
   - estoque baixo
   - lotes ativos
   - lotes próximos do vencimento
   - lotes vencidos
   - lotes esgotados
   - filtros por unidade, produto e situação

6. Resíduos
   - resíduos informados e situação atual
   - filtros por laboratório, status, período, projeto, gerador e gestor responsável
   - riscos informados e confirmados
   - recipiente e quantidade
   - armazenamento temporário
   - destino previsto e destino final confirmado
   - datas de recebimento, liberação, armazenamento e despacho
   - composição do resíduo
   - depende da integração do módulo atualmente desenvolvido em `feat/gestao-residuos`

7. Fiscalização
   - recorte especializado dos produtos explicitamente marcados como fiscalizados/controlados
   - filtros por produto, órgão fiscalizador, unidade, período e janela de vencimento
   - saldo atual consolidado
   - lotes ativos, vencidos e próximos do vencimento
   - entradas e saídas por período
   - rastreabilidade por lote
   - destino da saída: laboratório, projeto, solicitante e pedido
   - responsável pela movimentação
   - implementado em `GET /api/v1/relatorios/fiscalizacao`

## Produtos x Fiscalização

O relatório de Produtos é a visão geral do catálogo e deve permitir inclusive filtrar apenas produtos fiscalizados.

O relatório de Fiscalização não substitui Produtos. Ele é um relatório especializado de rastreabilidade e acrescenta informações operacionais exigidas em controle externo, como saldo, lotes, vencimentos, entradas, saídas e destino.

## Pedidos dentro de Movimentações

Pedidos entregues não possuem relatório dedicado. Quando a gestão precisar consultar movimentações relacionadas a pedidos, deve utilizar o relatório de Movimentações com o recorte de origem `PEDIDO` e, quando aplicável, tipo `SAIDA`.

O campo `Pedido.dataEntrega` permanece no domínio porque registra um evento real do pedido e pode ser utilizado em consultas futuras, auditoria e detalhamento, mesmo sem existir um relatório exclusivo de pedidos entregues.

## Regra de cadastro de Produto para fiscalização

A classificação de fiscalização pertence ao cadastro do Produto, e não ao módulo de Relatórios.

Na criação e edição de um produto, o responsável pelo cadastro deverá informar:

- `fiscalizado`: indica se o produto é controlado/fiscalizado externamente;
- `orgaosFiscalizadores`: um ou mais órgãos responsáveis pela fiscalização;
- `observacaoFiscalizacao`: informação complementar opcional.

Quando `fiscalizado = false`, os órgãos e a observação de fiscalização devem permanecer vazios.

Quando `fiscalizado = true`, deve ser informado pelo menos um órgão fiscalizador.

Órgãos inicialmente suportados:

- Polícia Federal;
- Vigilância Sanitária;
- ANVISA;
- Exército;
- Outro.

O futuro formulário `Administração → Cadastros → Produtos` deve apresentar uma seção própria de fiscalização. Essa classificação será a fonte oficial usada pelo relatório de fiscalização; risco químico, nível de risco e perecibilidade não devem ser usados para inferir automaticamente que um produto é fiscalizado.

## Regra de exportação

A prévia, o PDF e o XLSX devem usar a mesma consulta e os mesmos filtros. A geração oficial dos arquivos ficará no backend.

A etapa de exportação começa após a validação das consultas. A proposta técnica é manter um serviço de dados por relatório e adicionar formatadores separados para PDF e XLSX, evitando duplicar regras de filtro e cálculo.

## Dados de domínio adicionados nesta etapa

- `Pedido.dataEntrega` para registrar a data efetiva de entrega e suportar auditoria/consultas futuras.
- classificação explícita de fiscalização em Produto (`fiscalizado`, `orgaosFiscalizadores`, `observacaoFiscalizacao`).

## Estado atual

Consultas e prévias implementadas:

- Estagiários;
- Produtos;
- Movimentações;
- Resumo operacional;
- Estoque e lotes;
- Fiscalização.

Pendente por dependência de outra branch:

- Resíduos.

Próxima decisão técnica:

- formato e bibliotecas da exportação PDF/XLSX.
