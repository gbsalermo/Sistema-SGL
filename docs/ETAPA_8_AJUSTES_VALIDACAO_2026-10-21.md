# Etapa 8 — Correções da validação de 21/10/2026

## Pedidos de materiais
- A lista de pedidos mostra quantidade de embalagens e respectivo tipo quando disponíveis, ou grandeza física com unidade explícita. Quantidade aprovada é exibida na unidade canônica para não confundir embalagem solicitada com quantidade aprovada.
- A ausência de atividade vinculada agora é visível, em vez de ocultada na seção de detalhes.

## Soluções
- A ausência de atividade vinculada também é exibida explicitamente.
- O contrato atual de Pedido já persiste participação, Atividade e SCI na criação; para registros novos, caso a tela continue sem a origem, verificar payload e versão efetivamente executada do backend. Não inventar atividade para pedidos antigos.

## Resíduos
- Estagiários selecionam participação elegível na interface. Com uma participação, pré-seleção; múltiplas, escolha explícita; nenhuma, envio impedido.
- O backend exige `participacaoId` para estagiário, resolve de modo tenant-scoped, reavalia elegibilidade e deriva Projeto/Laboratório. Rejeita campos divergentes. Outros perfis mantêm o fluxo legado.
- A origem e os nomes históricos são persistidos em `residuos` pela migration `V41__residuo_contexto_operacional.sql`. Registros legados não são alterados retroativamente.
- Testes unitários adicionados para ausência de participação, participação não pertencente ao titular/unidade e envio indevido por outro perfil.

## Pendências de execução / aceite
- Executar no backend suíte Maven + inicialização Flyway num banco de validação.
- Executar no frontend build TypeScript/Vue e testes arquiteturais e de fluxos.
- Testar com João Silva duas participações elegíveis, selecionar a do segundo laboratório e confirmar que o resíduo persiste sob o laboratório do Projeto da Atividade.
- Verificar regressão de resíduos para Gestor, Administrador, Pesquisador e Técnico.
- Reabrir as novas solicitações de Pedido/Solução e checar `atividadeOrigemNome` e `sciOrigemNome` na resposta real.
- Não realizar merge sem validação manual.
