# Exemplos JSON — API SGL

Documento de apoio para testes manuais no Postman e desenvolvimento do frontend.

Base local:

```text
http://localhost:8080/api/v1
```

> Os IDs abaixo são apenas exemplos. Sempre substitua pelos IDs reais do seu banco.
>
> Nos endpoints que usam `usuarioId` como query param, isso é temporário até a autenticação fornecer o usuário pelo contexto.

---

## 1. Unidade

### Criar unidade

```http
POST /api/v1/unidades
Content-Type: application/json
```

```json
{
  "nome": "Instituto de Biologia",
  "sigla": "IB"
}
```

### Atualizar unidade

```http
PUT /api/v1/unidades/{id}
Content-Type: application/json
```

```json
{
  "nome": "Instituto de Biologia Atualizado",
  "sigla": "IB"
}
```

`GET` e `DELETE` não possuem body JSON.

---

## 2. Laboratório

### Criar laboratório

```http
POST /api/v1/laboratorios
Content-Type: application/json
```

```json
{
  "unidadeId": 1,
  "nome": "Laboratório de Microbiologia",
  "descricao": "Laboratório de estudo de microrganismos",
  "responsavel": 2,
  "ativo": true
}
```

`responsavel` pode ser `null` caso ainda não exista responsável definido.

### Atualizar laboratório

```http
PUT /api/v1/laboratorios/{id}
Content-Type: application/json
```

```json
{
  "unidadeId": 1,
  "nome": "Laboratório de Microbiologia",
  "descricao": "Descrição atualizada",
  "responsavel": 2,
  "ativo": true
}
```

`GET` e `DELETE` não possuem body JSON.

---

## 3. Usuário

### Criar usuário

```http
POST /api/v1/usuarios
Content-Type: application/json
```

```json
{
  "nome": "João da Silva",
  "email": "joao@sgl.com",
  "senha": "123456",
  "perfil": "PESQUISADOR",
  "laboratorioId": 1,
  "ativo": true
}
```

A senha é enviada normalmente pela API, mas o backend aplica BCrypt antes de persistir no banco.

Perfis atuais:

```text
ADMINISTRADOR
ANALISTA
ESTAGIARIO
GESTOR
PESQUISADOR
TECNICO
```

### Atualizar usuário

```http
PUT /api/v1/usuarios/{id}
Content-Type: application/json
```

```json
{
  "nome": "João da Silva",
  "email": "joao@sgl.com",
  "senha": "novaSenha123",
  "perfil": "PESQUISADOR",
  "laboratorioId": 1,
  "ativo": true
}
```

Se a senha não for alterada, o service atual aceita senha ausente/nula na lógica de atualização; ao testar, confirme o comportamento da validação do DTO utilizado pelo controller.

`DELETE /api/v1/usuarios/{id}` inativa o usuário e não possui body JSON.

---

## 4. Estagiário

O estágio é associado a um usuário existente.

### Criar registro de estagiário

```http
POST /api/v1/estagiarios
Content-Type: application/json
```

```json
{
  "usuarioId": 5,
  "laboratorioId": 4,
  "dataInicioEstagio": "2026-08-01",
  "dataFimEstagio": null,
  "tipoBolsa": "BOLSA_INSTITUCIONAL",
  "observacao": "Estágio cadastrado pelo sistema",
  "ativo": true
}
```

Tipos de bolsa atuais:

```text
BOLSA_CAPES
BOLSA_CNPQ
BOLSA_INSTITUCIONAL
VOLUNTARIO
```

### Atualizar estagiário

```http
PUT /api/v1/estagiarios/{id}
Content-Type: application/json
```

```json
{
  "usuarioId": 5,
  "laboratorioId": 4,
  "dataInicioEstagio": "2026-08-01",
  "dataFimEstagio": null,
  "tipoBolsa": "BOLSA_INSTITUCIONAL",
  "observacao": "Observação atualizada",
  "ativo": true
}
```

### Encerrar estágio

```http
PUT /api/v1/estagiarios/{id}/encerrar
```

Sem body JSON.

---

## 5. Produto

### Criar produto não perecível

```http
POST /api/v1/produtos
Content-Type: application/json
```

```json
{
  "nome": "Microplacas 96 poços",
  "descricao": "Microplacas de poliestireno para ELISA",
  "codigoReferencia": "MIC-96-PO",
  "unidadeMedida": "UNIDADE",
  "localizacaoFisica": "Armário B3",
  "risco": "NENHUM",
  "tipoRisco": "NENHUM",
  "descricaoRisco": null,
  "perecivel": false,
  "tipoPerecivel": "NENHUM",
  "condicoesArmazenamento": "Local seco",
  "unidadeArmazenamento": "caixa com 50 unidades",
  "ativo": true
}
```

### Criar produto perecível

```json
{
  "nome": "Meio de Cultivo BHI",
  "descricao": "Brain Heart Infusion para cultivo de bactérias",
  "codigoReferencia": "MID-BHI-500",
  "unidadeMedida": "FRASCO",
  "localizacaoFisica": "Prateleira C2",
  "risco": "MEDIO",
  "tipoRisco": "BIOLOGICO",
  "descricaoRisco": "Material biológico - manusear com EPI",
  "perecivel": true,
  "tipoPerecivel": "MICROBIANO",
  "condicoesArmazenamento": "Armazenar em geladeira 2-8°C",
  "unidadeArmazenamento": "frasco de 500mL",
  "ativo": true
}
```

A validade não pertence ao produto. Ela é informada em cada lote.

Valores atuais de risco:

```text
NivelRisco: NENHUM, BAIXO, MEDIO, ALTO
TipoRisco: NENHUM, BIOLOGICO, CORROSIVO, INFLAMAVEL, RADIOATIVO, TOXICO
TipoPerecivel: NENHUM, ANIMAL, MICROBIANO, QUIMICO, VEGETAL
```

Unidades de medida atuais:

```text
AMPOLA
CAIXA
FRASCO
G
KG
L
METRO
MG
ML
OUTRO
PAR
UNIDADE
```

### Atualizar produto

```http
PUT /api/v1/produtos/{id}
```

Use o mesmo formato JSON de criação, com os novos valores.

---

## 6. Projeto

### Criar projeto

```http
POST /api/v1/projetos
Content-Type: application/json
```

```json
{
  "laboratorioId": 3,
  "nome": "Projeto de Óptica Avançada",
  "descricao": "Estudo de fenômenos ópticos",
  "dataInicio": "2026-08-01",
  "dataFim": null,
  "responsavel": "Dr. João Pereira",
  "ativo": true
}
```

### Atualizar projeto

```http
PUT /api/v1/projetos/{id}
```

Use o mesmo formato JSON da criação.

---

## 7. Estoque Central

`EstoqueCentral` representa a combinação Unidade + Produto. A quantidade atual é calculada pelas movimentações dos lotes e não deve ser enviada pelo cliente.

### Criar estoque

```http
POST /api/v1/estoque-central
Content-Type: application/json
```

```json
{
  "unidadeId": 1,
  "produtoId": 3,
  "quantidadeMinima": 5,
  "ativo": true
}
```

### Atualizar configuração do estoque

```http
PUT /api/v1/estoque-central/{id}
Content-Type: application/json
```

```json
{
  "unidadeId": 1,
  "produtoId": 3,
  "quantidadeMinima": 10,
  "ativo": true
}
```

Não envie `quantidadeAtual`: esse campo é somente leitura.

---

## 8. Entrada física / criação de lote

O lote é criado pela movimentação de entrada, não por um POST direto em `/lotes`.

### Entrada de produto não perecível

```http
POST /api/v1/movimentacoes/estoques/{estoqueId}/lotes?usuarioId={usuarioId}
Content-Type: application/json
```

```json
{
  "numeroLote": "LOTE-001",
  "quantidade": 10,
  "dataValidade": null,
  "origem": "COMPRA",
  "observacao": "Entrada de produto não perecível"
}
```

### Entrada de produto perecível

```json
{
  "numeroLote": "LOTE-PERECIVEL-001",
  "quantidade": 10,
  "dataValidade": "2027-01-31",
  "origem": "COMPRA",
  "observacao": "Entrada de produto perecível"
}
```

Origens atuais:

```text
AJUSTE
COMPRA
DESCARTE
DEVOLUCAO
INVENTARIO
PEDIDO
```

### Atualizar dados cadastrais do lote

```http
PUT /api/v1/lotes/{id}
Content-Type: application/json
```

```json
{
  "numeroLote": "LOTE-001-CORRIGIDO",
  "dataValidade": "2027-02-28",
  "ativo": true
}
```

A quantidade do lote não é editada livremente por esse endpoint.

---

## 9. Descarte de produto vencido

```http
POST /api/v1/movimentacoes/estoques/{estoqueId}/descarte-vencimento?usuarioId={usuarioId}
Content-Type: application/json
```

```json
{
  "quantidade": 5,
  "justificativa": "Descarte de lote vencido"
}
```

---

## 10. Pedido

### Criar pedido

```http
POST /api/v1/pedidos
Content-Type: application/json
```

```json
{
  "usuarioId": 2,
  "laboratorioId": 1,
  "projetoId": 3,
  "observacao": "Pedido para experimento",
  "arquivoDocumento": null,
  "itens": [
    {
      "produtoId": 3,
      "quantidadeSolicitada": 6
    }
  ]
}
```

O pedido é criado como `PENDENTE`. A criação não baixa nem reserva lote.

### Pedido com vários produtos

```json
{
  "usuarioId": 2,
  "laboratorioId": 1,
  "projetoId": 3,
  "observacao": "Pedido com múltiplos materiais",
  "arquivoDocumento": null,
  "itens": [
    {
      "produtoId": 3,
      "quantidadeSolicitada": 6
    },
    {
      "produtoId": 5,
      "quantidadeSolicitada": 2
    }
  ]
}
```

---

## 11. Aprovar pedido

```http
PUT /api/v1/pedidos/{pedidoId}/aprovar
Content-Type: application/json
```

```json
{
  "observacao": "Aprovado para atendimento",
  "usuarioAprovadorId": 2,
  "itens": [
    {
      "itemId": 10,
      "quantidadeAprovada": 6
    }
  ]
}
```

Use o `itemId` retornado na criação/consulta do pedido, e não o `produtoId`.

### Aprovar parcialmente um item

Se foram solicitadas 10 unidades e serão aprovadas 7:

```json
{
  "observacao": "Aprovação parcial",
  "usuarioAprovadorId": 2,
  "itens": [
    {
      "itemId": 10,
      "quantidadeAprovada": 7
    }
  ]
}
```

A aprovação executa a saída dos lotes usando:

```text
Produto perecível     -> FEFO
Produto não perecível -> FIFO
```

---

## 12. Rejeitar pedido

Não usa body JSON.

```http
PUT /api/v1/pedidos/{pedidoId}/rejeitar?observacao=Pedido%20rejeitado
```

---

## 13. Entregar pedido

Não usa body JSON.

```http
PUT /api/v1/pedidos/{pedidoId}/entregar
```

A entrega muda o pedido de `APROVADO` para `ENTREGUE` e cria o `HistoricoLaboratorio`. Não ocorre uma segunda baixa de estoque.

---

## 14. Cancelar pedido

Não usa body JSON.

```http
PUT /api/v1/pedidos/{pedidoId}/cancelar?observacao=Cancelamento%20do%20pedido
```

Se o pedido estava `APROVADO`, o sistema restaura exatamente os lotes que foram consumidos na aprovação.

---

## 15. Consultas de pedido

Sem body JSON.

```http
GET /api/v1/pedidos
GET /api/v1/pedidos/{id}
GET /api/v1/pedidos/por-usuario?usuarioId={id}
GET /api/v1/pedidos/por-status?status=PENDENTE
GET /api/v1/pedidos/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=2026-08-01&dataFim=2026-08-31
```

Status atuais:

```text
PENDENTE
APROVADO
ENTREGUE
REJEITADO
CANCELADO
```

---

## 16. Consultas de movimentação

Sem body JSON.

```http
GET /api/v1/movimentacoes
GET /api/v1/movimentacoes/{id}
GET /api/v1/movimentacoes/produto?produtoId={id}
GET /api/v1/movimentacoes/laboratorio?laboratorioId={id}
GET /api/v1/movimentacoes/usuario?usuarioId={id}
GET /api/v1/movimentacoes/pedido?pedidoId={id}
GET /api/v1/movimentacoes/tipo?tipo=SAIDA
```

---

## 17. Consultas de lote

Sem body JSON.

```http
GET /api/v1/lotes
GET /api/v1/lotes/{id}
GET /api/v1/lotes/por-estoque?estoqueId={id}
GET /api/v1/lotes/vencidos
```

---

## 18. Histórico do laboratório

O histórico é gerado pelo sistema na entrega do pedido. Não existe JSON manual de criação no fluxo atual.

Consultas:

```http
GET /api/v1/historico-laboratorio
GET /api/v1/historico-laboratorio/{id}
GET /api/v1/historico-laboratorio/laboratorio/{laboratorioId}
GET /api/v1/historico-laboratorio/produto/{produtoId}
GET /api/v1/historico-laboratorio/pedido/{pedidoId}
GET /api/v1/historico-laboratorio/laboratorio/{laboratorioId}/periodo?dataInicio=2026-08-01&dataFim=2026-08-31
GET /api/v1/historico-laboratorio/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo?dataInicio=2026-08-01&dataFim=2026-08-31
```

---

## 19. Consultas rápidas de cadastros

Sem body JSON.

```http
GET /api/v1/unidades
GET /api/v1/unidades/{id}

GET /api/v1/laboratorios
GET /api/v1/laboratorios/{id}
GET /api/v1/laboratorios/por-unidade?unidadeId={id}

GET /api/v1/usuarios
GET /api/v1/usuarios/{id}
GET /api/v1/usuarios/por-laboratorio?laboratorioId={id}

GET /api/v1/estagiarios
GET /api/v1/estagiarios/{id}
GET /api/v1/estagiarios/por-laboratorio?laboratorioId={id}
GET /api/v1/estagiarios/ativos

GET /api/v1/produtos
GET /api/v1/produtos/{id}
GET /api/v1/produtos/risco/{nivel}
GET /api/v1/produtos/pereciveis
GET /api/v1/produtos/buscar?nome={nome}

GET /api/v1/projetos
GET /api/v1/projetos/{id}
GET /api/v1/projetos/por-laboratorio?laboratorioId={id}
GET /api/v1/projetos/ativos

GET /api/v1/estoque-central
GET /api/v1/estoque-central/{id}
GET /api/v1/estoque-central/por-unidade?unidadeId={id}
GET /api/v1/estoque-central/por-unidade-produto?unidadeId={id}&produtoId={id}
GET /api/v1/estoque-central/estoque-baixo?unidadeId={id}
```

---

## 20. Local de armazenamento de Resíduo

### Criar local cadastrado

```http
POST /api/v1/locais-armazenamento-residuo
X-SGL-Unidade-Id: {unidadePublicId}
Content-Type: application/json
```

```json
{
  "unidadeId": "cb09ba48-d244-46bc-8324-037b8529b3a9",
  "nome": "Almoxarifado Químico",
  "ativo": true
}
```

### Atualizar local

```http
PUT /api/v1/locais-armazenamento-residuo/{localId}
X-SGL-Unidade-Id: {unidadePublicId}
Content-Type: application/json
```

Use o mesmo formato da criação. `DELETE` faz inativação lógica e não possui body.

Consultas úteis:

```http
GET /api/v1/locais-armazenamento-residuo
GET /api/v1/locais-armazenamento-residuo/ativos
GET /api/v1/locais-armazenamento-residuo/{localId}
```

---

## 21. Resíduos — análise e armazenamento com local estruturado

### Analisar/liberar usando local cadastrado

```http
PUT /api/v1/residuos/{residuoId}/analisar-liberar
X-SGL-Unidade-Id: {unidadePublicId}
Content-Type: application/json
```

```json
{
  "usuarioGestorId": "11111111-1111-1111-1111-111111111111",
  "nivelRiscoConfirmado": "MEDIO",
  "riscosConfirmados": ["INFLAMAVEL", "PERIGO_SAUDE"],
  "localArmazenamentoResiduoId": "22222222-2222-2222-2222-222222222222",
  "complementoLocalArmazenamento": "Prateleira B2",
  "localArmazenamentoTemporario": null,
  "destinoFinalPrevisto": "Empresa licenciada para tratamento",
  "dataPrevistaDespacho": "2026-09-30",
  "observacaoGestor": "Classificação conferida",
  "classesConfirmadasIds": [
    "33333333-3333-3333-3333-333333333333"
  ],
  "medidasSegurancaConfirmadas": [
    "LUVAS",
    "OCULOS_PROTECAO"
  ],
  "observacaoSegurancaConfirmada": "Manter recipiente fechado"
}
```

O snapshot histórico resultante fica no formato:

```text
Almoxarifado Químico - Prateleira B2
```

### Analisar/liberar usando local manual

Envie `localArmazenamentoResiduoId = null`, sem complemento, e preencha:

```json
{
  "localArmazenamentoTemporario": "Área provisória externa"
}
```

Os demais campos obrigatórios da análise continuam iguais ao exemplo anterior.

### Confirmar armazenamento sem corrigir o local

```http
PUT /api/v1/residuos/{residuoId}/armazenar
X-SGL-Unidade-Id: {unidadePublicId}
Content-Type: application/json
```

```json
{
  "usuarioGestorId": "11111111-1111-1111-1111-111111111111",
  "localArmazenamentoResiduoId": null,
  "complementoLocalArmazenamento": null,
  "localArmazenamentoTemporario": null
}
```

O local planejado é mantido.

### Corrigir para outro local cadastrado

```json
{
  "usuarioGestorId": "11111111-1111-1111-1111-111111111111",
  "localArmazenamentoResiduoId": "44444444-4444-4444-4444-444444444444",
  "complementoLocalArmazenamento": "Estante A1",
  "localArmazenamentoTemporario": null
}
```

### Corrigir manualmente

```json
{
  "usuarioGestorId": "11111111-1111-1111-1111-111111111111",
  "localArmazenamentoResiduoId": null,
  "complementoLocalArmazenamento": null,
  "localArmazenamentoTemporario": "Área provisória externa"
}
```

Não envie catálogo e texto manual ao mesmo tempo. Complemento só é válido com local cadastrado.

---

## 22. Ordem prática para testar um fluxo completo

```text
1. Criar/consultar Unidade
2. Criar/consultar Laboratório
3. Criar/consultar Usuário
4. Criar/consultar Produto
5. Criar EstoqueCentral para Unidade + Produto
6. Registrar entrada física -> cria Lote
7. Criar Projeto
8. Criar Pedido
9. Aprovar Pedido -> baixa FEFO/FIFO
10. Entregar Pedido -> cria HistóricoLaboratorio
11. Consultar movimentações, lotes e histórico
```

Para o inventário completo de endpoints, consulte também `docs/ENDPOINTS_INTERNOS.md`.
