# Relatórios — SGL

## Objetivo

Centralizar consultas operacionais, gerenciais e de fiscalização com exportação oficial em PDF e XLSX.

## Relatórios previstos

1. Estagiários
   - todos, ativos e inativos
   - por laboratório
   - por período de vínculo

2. Movimentações
   - entradas, saídas, ajustes, devoluções e descartes
   - filtros por período, produto, laboratório, lote e responsável

3. Resumo operacional
   - maiores entradas
   - maiores saídas
   - quantidade de movimentações
   - lotes recebidos, próximos do vencimento, vencidos e descartados

4. Pedidos entregues
   - total de pedidos entregues
   - filtros por período, laboratório, projeto, solicitante e produto
   - data efetiva de entrega

5. Estoque e lotes
   - posição atual
   - estoque baixo
   - lotes ativos
   - lotes próximos do vencimento
   - lotes vencidos

6. Fiscalização
   - produtos explicitamente marcados como fiscalizados/controlados
   - órgão(s) fiscalizador(es)
   - saldo atual
   - lotes e vencimentos
   - entradas e saídas
   - destino da saída (laboratório/projeto/solicitante/pedido)

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

## Lacunas de domínio identificadas

- Pedido precisa registrar `dataEntrega` para permitir relatórios por período real de entrega.
- Produto precisa registrar explicitamente se é fiscalizado/controlado e por quais órgãos; risco e perecibilidade não são suficientes para inferir fiscalização.

## Ordem de implementação

1. Completar dados de domínio (`dataEntrega` e fiscalização de produto).
2. Criar DTOs/serviços de relatório.
3. Criar endpoints de consulta.
4. Adicionar exportação PDF.
5. Adicionar exportação XLSX.
6. Integrar a central `/relatorios` no frontend.
