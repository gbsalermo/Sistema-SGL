# Auditoria da API antes do Swagger

**Data:** 13/08/2026

## Resultado

A API foi revisada antes da adoção de OpenAPI/Swagger. Os fluxos críticos de estoque e pedido continuam sendo a base validada da alpha.

## Correções aplicadas

- `UsuarioDTO` agora possui `unidadeId` e dados de unidade na resposta.
- `UsuarioService` vincula a unidade e valida que o laboratório pertence à mesma unidade.
- senha de usuário é somente de entrada no JSON; no POST é obrigatória e no PUT é opcional.
- criação de estagiário corrigida de `INSERT INTO estagiario` para a tabela real `estagiarios`.
- estagiário e laboratório precisam pertencer à mesma unidade.
- atualizações preservam `ativo` quando o campo não é enviado nos fluxos revisados.
- `DELETE` de estoque central passa a inativar o registro em vez de apagar o histórico.
- validações de entidades pai foram reforçadas nos fluxos revisados.

## Novo indicador de consumo

Endpoint:

```http
GET /api/v1/historico-laboratorio/laboratorio/{laboratorioId}/produto/{produtoId}/consumo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD
```

A fonte é `HistoricoLaboratorio`, portanto entram apenas materiais efetivamente entregues.

Retorno principal:

```text
quantidadePedidos
quantidadeTotalRecebida
mediaQuantidadePorPedido
mesesConsiderados
mediaConsumoMensal
quantidadeMinimaSugerida
```

`quantidadeMinimaSugerida` é a média mensal arredondada para cima. É apenas uma referência e não altera automaticamente o estoque.

## Pontos intencionalmente temporários da alpha

- `SecurityConfig` permanece liberado durante desenvolvimento.
- alguns endpoints ainda recebem `usuarioId`/`usuarioAprovadorId`.
- autenticação/autorização definitiva será ligada à API corporativa.

## Pendências não bloqueantes para Swagger

- alguns DTOs ainda misturam request e response;
- Unidade ainda possui exclusão física, protegida pelas FKs/409 quando houver dependências;
- listagens ainda não possuem paginação;
- CORS deve ser configurado antes de conectar frontend em outra origem.

## Erros HTTP

`RestExceptionHandler` já padroniza 400, 404, 409 e 500, oferecendo uma boa base para documentação OpenAPI.

## Próxima sequência

```text
mvn test
→ validar endpoint de consumo
→ adicionar Swagger/OpenAPI
→ configurar CORS
→ iniciar frontend alpha
```
