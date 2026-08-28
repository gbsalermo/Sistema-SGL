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

4. Resumo operacional
   - maiores entradas
   - maiores saídas
   - quantidade de movimentações
   - lotes mais movimentados

5. Pedidos entregues
   - total de pedidos entregues
   - filtros por período, laboratório, projeto, solicitante e produto
   - data efetiva de entrega

6. Estoque e lotes
   - posição atual
   - estoque baixo
   - lotes ativos
   - lotes próximos do vencimento
   - lotes vencidos

7. Resíduos
   - resíduos informados e situação atual
   - filtros por laboratório, status, período, projeto, gerador e gestor responsável
   - riscos informados e confirmados
   - recipiente e quantidade
   - armazenamento temporário
   - destino previsto e destino final confirmado
   - datas de recebimento, liberação, armazenamento e despacho
   - composição do resíduo
   - depende da integração do módulo atualmente desenvolvido em `feat/gestao-residuos`

8. Fiscalização
   - recorte especializado dos produtos explicitamente marcados como fiscalizados/controlados
   - órgão(s) fiscalizador(es)
   - saldo atual
   - lotes e vencimentos
   - entradas e saídas
   - destino da saída (laboratório/projeto/solicitante/pedido)

## Produtos x Fiscalização

O relatório de Produtos é a visão geral do catálogo e deve permitir inclusive filtrar apenas produtos fiscalizados.

O relatório de Fiscalização não substitui Produtos. Ele é um relatório especializado de rastreabilidade e deve acrescentar informações operacionais exigidas em controle externo, como saldo, lotes, vencimentos, entradas, saídas e destino.

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

## Dados de domínio adicionados nesta etapa

- `Pedido.dataEntrega` para permitir relatórios por período real de entrega.
- classificação explícita de fiscalização em Produto (`fiscalizado`, `orgaosFiscalizadores`, `observacaoFiscalizacao`).

## Ordem de implementação

1. Completar dados de domínio necessários aos relatórios.
2. Criar DTOs/serviços de relatório.
3. Criar endpoints de consulta.
4. Integrar as prévias na central `/relatorios`.
5. Adicionar exportação PDF.
6. Adicionar exportação XLSX.
