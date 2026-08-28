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
