# Endpoints Internos — SGL

**Atualizado em:** 18/09/2026  

> Documento de acompanhamento técnico do backend.
>
> **Visibilidade:** este arquivo acompanha a visibilidade do repositório. Como o repositório atual é público, este documento também é público.

Base local:

```text
http://localhost:8080
```

Prefixo da API:

```text
/api/v1
```

---

## Unidade

Base: `/api/v1/unidades`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/unidades` | Lista todas as unidades. |
| GET | `/api/v1/unidades/{id}` | Busca uma unidade pelo ID. |
| POST | `/api/v1/unidades` | Cria uma unidade. |
| PUT | `/api/v1/unidades/{id}` | Atualiza uma unidade. |
| DELETE | `/api/v1/unidades/{id}` | Remove a unidade; relacionamentos existentes podem gerar `409 Conflict`. |

---

## Laboratório

Base: `/api/v1/laboratorios`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/laboratorios` | Lista todos os laboratórios. |
| GET | `/api/v1/laboratorios/{id}` | Busca laboratório pelo ID. |
| GET | `/api/v1/laboratorios/por-unidade?unidadeId={id}` | Lista laboratórios pertencentes a uma unidade. |
| POST | `/api/v1/laboratorios` | Cria laboratório. |
| PUT | `/api/v1/laboratorios/{id}` | Atualiza laboratório. |
| DELETE | `/api/v1/laboratorios/{id}` | Inativa o laboratório. |

O responsável, quando informado, deve pertencer à mesma unidade do laboratório.

---

## Usuário

Base: `/api/v1/usuarios`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/usuarios` | Lista todos os usuários. |
| GET | `/api/v1/usuarios/{id}` | Busca usuário pelo ID. |
| GET | `/api/v1/usuarios/por-laboratorio?laboratorioId={id}` | Lista usuários de um laboratório. |
| POST | `/api/v1/usuarios` | Cria usuário. `unidadeId` e senha são obrigatórios. |
| PUT | `/api/v1/usuarios/{id}` | Atualiza usuário. A senha é opcional; se omitida, permanece inalterada. |
| DELETE | `/api/v1/usuarios/{id}` | Inativa o usuário. |

`laboratorioId`, quando informado, deve pertencer à mesma `unidadeId` do usuário. A senha é somente de entrada e não é devolvida nas respostas JSON.

> A identidade das operações auditáveis ainda utiliza parâmetros temporários em alguns endpoints. A autenticação definitiva será integrada à API corporativa.

---

## Estagiário

Base: `/api/v1/estagiarios`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/estagiarios` | Lista todos os estagiários. |
| GET | `/api/v1/estagiarios/{id}` | Busca estagiário pelo ID. |
| GET | `/api/v1/estagiarios/por-laboratorio?laboratorioId={id}` | Lista estagiários de um laboratório. |
| GET | `/api/v1/estagiarios/ativos` | Lista estágios/estagiários ativos. |
| POST | `/api/v1/estagiarios` | Cria a extensão de estagiário para um usuário com perfil `ESTAGIARIO`. |
| PUT | `/api/v1/estagiarios/{id}` | Atualiza estagiário. |
| PUT | `/api/v1/estagiarios/{id}/encerrar` | Encerra o estágio. |
| DELETE | `/api/v1/estagiarios/{id}` | Inativa/encerra o estágio. |

Usuário e laboratório do estagiário devem pertencer à mesma unidade.

---

## Produto

Base: `/api/v1/produtos`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/produtos` | Lista todos os produtos do catálogo. |
| GET | `/api/v1/produtos/{id}` | Busca produto pelo ID. |
| GET | `/api/v1/produtos/risco/{nivel}` | Lista produtos por nível de risco. |
| GET | `/api/v1/produtos/pereciveis` | Lista produtos marcados como perecíveis. |
| GET | `/api/v1/produtos/buscar?nome={nome}` | Pesquisa produtos por nome. |
| POST | `/api/v1/produtos` | Cria produto no catálogo. |
| PUT | `/api/v1/produtos/{id}` | Atualiza produto. |
| DELETE | `/api/v1/produtos/{id}` | Inativa produto. |

`Produto` informa apenas se o material é perecível. A data de validade operacional pertence ao `Lote`.

---

## Projeto

Base: `/api/v1/projetos`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/projetos` | Lista todos os projetos. |
| GET | `/api/v1/projetos/{id}` | Busca projeto pelo ID. |
| GET | `/api/v1/projetos/por-laboratorio?laboratorioId={id}` | Lista projetos vinculados ao laboratório. |
| GET | `/api/v1/projetos/ativos` | Lista projetos ativos. |
| POST | `/api/v1/projetos` | Cria projeto. |
| PUT | `/api/v1/projetos/{id}` | Atualiza projeto. |
| DELETE | `/api/v1/projetos/{id}` | Inativa projeto. |

---

## Local de Armazenamento de Resíduo

Base: `/api/v1/locais-armazenamento-residuo`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/locais-armazenamento-residuo` | Lista os locais visíveis no contexto da Unidade. |
| GET | `/api/v1/locais-armazenamento-residuo/ativos` | Lista apenas locais ativos, usados em novas seleções de Resíduo. |
| GET | `/api/v1/locais-armazenamento-residuo/{id}` | Busca local pelo UUID público. |
| POST | `/api/v1/locais-armazenamento-residuo` | Cria local para uma Unidade. |
| PUT | `/api/v1/locais-armazenamento-residuo/{id}` | Atualiza nome/estado do local sem transferi-lo para outra Unidade. |
| DELETE | `/api/v1/locais-armazenamento-residuo/{id}` | Inativa logicamente o local. |

Regras:

- nome obrigatório, até 150 caracteres;
- nome duplicado na mesma Unidade é rejeitado de forma case-insensitive;
- o mesmo nome pode existir em Unidades diferentes;
- local inativo não pode ser escolhido em novas operações;
- renomear/inativar o catálogo não altera o snapshot histórico já salvo no Resíduo.

---

## Modelos de Resíduo

Base: `/api/v1/modelos-residuo`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/modelos-residuo` | Lista modelos visíveis no contexto da Unidade, inclusive inativos. |
| GET | `/api/v1/modelos-residuo/ativos` | Lista modelos ativos para novas ocorrências. |
| GET | `/api/v1/modelos-residuo/{id}` | Busca modelo por UUID público. |
| POST | `/api/v1/modelos-residuo` | Cria modelo reutilizável na Unidade. |
| PUT | `/api/v1/modelos-residuo/{id}` | Atualiza dados reutilizáveis do modelo. |
| DELETE | `/api/v1/modelos-residuo/{id}` | Inativa logicamente o modelo. |

Modelos não movimentam estoque e não possuem vínculo histórico obrigatório com `Residuo`.

---

## Resíduo

Base: `/api/v1/residuos`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/residuos` | Lista resíduos visíveis no contexto atual. |
| GET | `/api/v1/residuos/{id}` | Busca Resíduo pelo UUID público. |
| GET | `/api/v1/residuos/por-status?status={status}` | Lista por status operacional. |
| GET | `/api/v1/residuos/por-laboratorio?laboratorioId={id}` | Lista por laboratório. |
| GET | `/api/v1/residuos/por-gerador?usuarioGeradorId={id}` | Lista resíduos do gerador. |
| POST | `/api/v1/residuos` | Informa nova ocorrência de Resíduo. |
| PUT | `/api/v1/residuos/{id}/receber` | Gestão recebe o Resíduo e inicia análise. |
| PUT | `/api/v1/residuos/{id}/analisar-liberar` | Confirma classificação, segurança, local planejado e libera para armazenamento. |
| PUT | `/api/v1/residuos/{id}/armazenar` | Confirma armazenamento físico; pode manter ou corrigir o local. |
| PUT | `/api/v1/residuos/{id}/despachar` | Confirma despacho/destinação. |
| PUT | `/api/v1/residuos/{id}/administrar` | Administrador cancela ou retorna exatamente uma etapa, com justificativa. |
| GET | `/api/v1/residuos/{id}/rotulo` | Retorna dados da prévia/rótulo. |
| GET | `/api/v1/residuos/{id}/historico` | Retorna histórico operacional do Resíduo. |

Na análise/liberação, o local pode ser informado de duas formas mutuamente exclusivas:

```text
localArmazenamentoResiduoId + complementoLocalArmazenamento opcional
OU
localArmazenamentoTemporario manual
```

Na confirmação física, omitir todos os campos de localização significa **manter o local planejado**. Se houver correção, o histórico registra o valor planejado e o confirmado.

---

## Estoque Central

Base: `/api/v1/estoque-central`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/estoque-central` | Lista todos os registros agregados de estoque. |
| GET | `/api/v1/estoque-central/{id}` | Busca um estoque pelo ID. |
| GET | `/api/v1/estoque-central/por-unidade?unidadeId={id}` | Lista estoques de uma unidade. |
| GET | `/api/v1/estoque-central/por-unidade-produto?unidadeId={id}&produtoId={id}` | Busca o estoque específico da combinação Unidade + Produto. |
| GET | `/api/v1/estoque-central/estoque-baixo?unidadeId={id}` | Lista produtos com saldo igual ou inferior ao mínimo na unidade. |
| POST | `/api/v1/estoque-central` | Cria registro Unidade + Produto com saldo inicial zero. |
| PUT | `/api/v1/estoque-central/{id}` | Atualiza quantidade mínima/ativo; o saldo não pode ser editado diretamente. |
| DELETE | `/api/v1/estoque-central/{id}` | Inativa o estoque central, preservando histórico e lotes. |

Regra estrutural:

```text
EstoqueCentral.quantidadeAtual
=
soma de Lote.quantidadeDisponivel
```

Entrada, saída e descarte físico pertencem ao `MovimentacaoEstoqueService`.

---

## Lote

Base: `/api/v1/lotes`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/lotes` | Lista todos os lotes. |
| GET | `/api/v1/lotes/{id}` | Busca lote pelo ID. |
| GET | `/api/v1/lotes/por-estoque?estoqueId={id}` | Lista os lotes que compõem um EstoqueCentral. |
| GET | `/api/v1/lotes/vencidos` | Lista lotes vencidos ativos. |
| PUT | `/api/v1/lotes/{id}` | Corrige/manutenção cadastral do lote sem edição livre de quantidade. |
| DELETE | `/api/v1/lotes/{id}` | Inativa lote sem saldo disponível. |

Política de saída:

```text
Produto perecível     → FEFO
Produto não perecível → FIFO
```

O pedido solicita `Produto + quantidade`; o lote é selecionado internamente pelo sistema.

---

## Movimentação de Estoque

Base: `/api/v1/movimentacoes`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/movimentacoes` | Lista todas as movimentações auditadas. |
| GET | `/api/v1/movimentacoes/{id}` | Busca movimentação pelo ID. |
| GET | `/api/v1/movimentacoes/produto?produtoId={id}` | Lista movimentações de um produto. |
| GET | `/api/v1/movimentacoes/laboratorio?laboratorioId={id}` | Lista movimentações associadas a um laboratório. |
| GET | `/api/v1/movimentacoes/usuario?usuarioId={id}` | Lista movimentações executadas por usuário. |
| GET | `/api/v1/movimentacoes/pedido?pedidoId={id}` | Lista movimentações originadas por um pedido. |
| GET | `/api/v1/movimentacoes/tipo?tipo={tipo}` | Lista movimentações por tipo. |
| POST | `/api/v1/movimentacoes/estoques/{estoqueId}/lotes?usuarioId={usuarioId}` | Registra entrada física, cria lote, aumenta saldo e registra `ENTRADA`. |
| POST | `/api/v1/movimentacoes/estoques/{estoqueId}/descarte-vencimento?usuarioId={usuarioId}` | Descarta lotes vencidos e registra auditoria por lote. |

Os `usuarioId` das operações físicas são temporários até a integração do contexto autenticado.

Se uma saída utilizar mais de um lote, existe uma movimentação para cada lote afetado.

---

## Pedido

Base: `/api/v1/pedidos`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/pedidos` | Lista todos os pedidos. |
| GET | `/api/v1/pedidos/{id}` | Busca pedido pelo ID. |
| GET | `/api/v1/pedidos/por-usuario?usuarioId={id}` | Lista pedidos criados por um usuário. |
| GET | `/api/v1/pedidos/por-status?status={status}` | Lista pedidos por status. |
| GET | `/api/v1/pedidos/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD` | Lista pedidos realizados por um projeto no laboratório e período. |
| POST | `/api/v1/pedidos` | Cria pedido com status inicial `PENDENTE`. |
| PUT | `/api/v1/pedidos/{id}/aprovar` | Aprova quantidades e executa saída FEFO/FIFO. |
| PUT | `/api/v1/pedidos/{id}/rejeitar?observacao={texto}` | Rejeita pedido pendente. |
| PUT | `/api/v1/pedidos/{id}/entregar` | Marca pedido aprovado como entregue e cria `HistoricoLaboratorio`, sem nova baixa. |
| PUT | `/api/v1/pedidos/{id}/cancelar?observacao={texto}` | Cancela pedido; quando aprovado, restaura os lotes consumidos. |

O payload de aprovação exige pelo menos um item e não aceita o mesmo `itemId` repetido.

A consulta por projeto usa `Pedido.dataSolicitacao` e representa solicitações realizadas.

---

## Histórico do Laboratório

Base: `/api/v1/historico-laboratorio`

| Método | Endpoint | Função |
|---|---|---|
| GET | `/api/v1/historico-laboratorio` | Lista todo o histórico de recebimentos. |
| GET | `/api/v1/historico-laboratorio/{id}` | Busca registro de histórico pelo ID. |
| GET | `/api/v1/historico-laboratorio/laboratorio/{laboratorioId}` | Lista materiais recebidos pelo laboratório. |
| GET | `/api/v1/historico-laboratorio/produto/{produtoId}` | Lista recebimentos relacionados a um produto. |
| GET | `/api/v1/historico-laboratorio/pedido/{pedidoId}` | Lista os registros de recebimento de um pedido. |
| GET | `/api/v1/historico-laboratorio/laboratorio/{laboratorioId}/periodo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD` | Lista materiais efetivamente recebidos pelo laboratório no período. |
| GET | `/api/v1/historico-laboratorio/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD` | Lista materiais recebidos por um projeto específico no período. |
| GET | `/api/v1/historico-laboratorio/laboratorio/{laboratorioId}/produto/{produtoId}/consumo?dataInicio=AAAA-MM-DD&dataFim=AAAA-MM-DD` | Calcula consumo histórico do produto pelo laboratório. |

### Indicador de consumo

O endpoint de consumo usa apenas registros efetivamente entregues e retorna:

```text
quantidadePedidos
quantidadeTotalRecebida
mediaQuantidadePorPedido
mesesConsiderados
mediaConsumoMensal
quantidadeMinimaSugerida
```

A média mensal considera todos os meses do intervalo, inclusive meses sem recebimento. `quantidadeMinimaSugerida` é a média mensal arredondada para cima e não altera automaticamente a configuração do estoque.

### Diferença entre Pedido e Histórico

```text
Pedido
→ solicitação realizada
→ usa dataSolicitacao

HistoricoLaboratorio
→ recebimento efetivo
→ nasce na entrega
→ usa dataRecebimento
```

---

## Status de Pedido

```text
PENDENTE
├── APROVADO
│   ├── ENTREGUE
│   └── CANCELADO
└── REJEITADO
```

---

## Regras de período

Nos endpoints que recebem período:

```text
dataInicio é obrigatória
dataFim é obrigatória
dataInicio <= dataFim
```

Para pedidos, `dataFim` inclui o dia completo. Consultas por projeto validam o vínculo Projeto × Laboratório.

---

## Estado pré-Swagger

A revisão detalhada está em [`API_AUDITORIA_PRE_SWAGGER.md`](API_AUDITORIA_PRE_SWAGGER.md).

Pontos intencionalmente temporários da alpha:

```text
autenticação corporativa ainda não integrada
usuarioId/usuarioAprovadorId ainda presentes em fluxos auditáveis
SecurityConfig liberado para desenvolvimento
```

---

## Manutenção deste documento

Sempre que um endpoint for criado, removido ou tiver sua responsabilidade alterada, atualizar também:

```text
README.md
CONTINUIDADE.md
docs/JSON_EXEMPLOS.md
docs/testes.md
```
