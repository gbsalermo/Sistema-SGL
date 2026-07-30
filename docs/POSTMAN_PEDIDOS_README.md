SGL — Testes de Endpoints de Pedido (Postman)

Objetivo
--------
Guia rápido para importar a coleção Postman e testar os principais endpoints de Pedido (criar, listar, aprovar, rejeitar, entregar, cancelar, deletar).

Arquivos
--------
- docs/Pedido.postman_collection.json  (coleção)
- docs/SGLEndpoints.postman_environment.json  (ambiente com variáveis)

Passo a passo
-------------
1. No Postman/Insomnia, importe a coleção e o ambiente listados acima.
2. Ajuste as variáveis do ambiente:
   - baseUrl (ex.: http://localhost:8080)
   - token (se houver autenticação)
3. Executar em ordem:
   a. "Criar Pedido" — payload de exemplo abaixo. Copiar o id retornado e colar em {{pedidoId}} no ambiente.
   b. "Listar Todos" / "Buscar Pedido por ID" — confirmar criação.
   c. "Aprovar Pedido" — use os itemId corretos e confirme quantidades aprovadas.
   d. "Entregar Pedido" — verificar registro em EstoqueLaboratorio.
   e. "Cancelar Pedido" (após aprovação) — confirmar que EstoqueCentral foi restituído.
   f. "Deletar Pedido" — só para PENDENTE ou REJEITADO.
4. Observe os campos status e quantidades em cada resposta para validar regras de negócio.

Exemplo: Criar Pedido
---------------------
POST /api/v1/pedidos
{
  "usuarioId": 4,
  "laboratorioId": 3,
  "projetoId": 1,
  "observacao": "Materiais para experimento",
  "itens": [
    { "produtoId": 1, "quantidadeSolicitada": 5 },
    { "produtoId": 5, "quantidadeSolicitada": 2 }
  ]
}

Exemplo: Aprovar Pedido
-----------------------
PUT /api/v1/pedidos/{{pedidoId}}/aprovar
{
  "observacao": "Aprovado pelo gestor",
  "itens": [
    { "itemId": 1, "quantidadeAprovada": 5 },
    { "itemId": 2, "quantidadeAprovada": 2 }
  ]
}

Dicas rápidas
-------------
- Garanta que os produtos solicitados tenham EstoqueCentral registrado.
- Ao aprovar, quantidadeAprovada não pode exceder quantidadeSolicitada.
- Use respostas (JSON) para pegar ids de itens/pedidos e atualizar as variáveis do ambiente.

Fim
---
Arquivo gerado automaticamente por Copilot CLI.  
