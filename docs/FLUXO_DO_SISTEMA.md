# Fluxo do Sistema SGL

**Atualizado em:** 17/09/2026  
**Checkpoint:** Etapa 4 iniciada; 4.1 — locais de armazenamento em andamento.

Este documento descreve como os módulos principais se conectam no estado funcional aprovado e nas etapas de pré-produção já validadas. Detalhes de contrato devem ser confirmados no Swagger/OpenAPI e detalhes de implementação no código da branch integrada/validada.

---

## 1. Contexto institucional

```text
Unidade
  → Laboratório
      → Usuários
      → Projetos

Produto
  → EstoqueCentral da Unidade
```

Regras atuais:

1. Unidade é entidade institucional do domínio.
2. Laboratórios pertencem a uma Unidade.
3. Usuários pertencem a uma Unidade e, quando aplicável, a um Laboratório.
4. Projetos pertencem ao contexto do Laboratório/Unidade.
5. Produtos formam o catálogo.
6. Cada Unidade possui seu próprio contexto de estoque.
7. Catálogos operacionais adicionados ao domínio, como Classes de Resíduo e futuramente Locais de Armazenamento, também respeitam a Unidade.

No modo DEV, o frontend envia `X-SGL-Unidade-Id` e o backend usa `TenantContext` para restringir operações à Unidade corrente. Esse mecanismo ainda não substitui a futura identidade corporativa confiável.

---

## 2. Entrada de estoque

```text
contexto da Unidade + Produto
  → informar lote/apresentação/quantidade
  → Service valida o registro
  → atualiza saldo do EstoqueCentral
  → atualiza/cria Lote
  → grava MovimentacaoEstoque
```

---

## 3. Saída manual

```text
saldo/lotes disponíveis
  → validar quantidade
  → selecionar lote conforme regra aplicável
  → impedir saldo negativo
  → subtrair quantidade
  → registrar movimentação
```

A alteração de saldo e a movimentação pertencem à mesma operação transacional.

---

## 4. Criação do pedido

O usuário informa laboratório, projeto opcional e itens solicitados.

O saldo **não é reduzido na criação**. O pedido é salvo como `PENDENTE`.

---

## 5. Aprovação

```text
Aprovador + Pedido PENDENTE
  → valida itens/quantidades
  → localiza estoque e lotes da Unidade
  → exclui lote vencido
  → perecível: FEFO
  → não perecível: FIFO
  → reduz lotes utilizados
  → atualiza EstoqueCentral
  → grava MovimentacaoEstoque SAIDA/PEDIDO
  → altera Pedido para APROVADO
```

Todo o processamento é transacional. Urgência não altera FIFO/FEFO.

---

## 6. Rejeição e entrega

```text
PENDENTE → REJEITADO
```

Rejeição não altera estoque.

```text
APROVADO
→ registrar HistoricoLaboratorio
→ registrar data real de entrega
→ ENTREGUE
```

A entrega **não reduz o estoque novamente**.

---

## 7. Cancelamento de Pedido

- `PENDENTE`: cancela sem alterar estoque.
- `APROVADO`: restaura os lotes exatos utilizados e muda para `CANCELADO`.
- `ENTREGUE`: não pode ser cancelado pelo fluxo comum.
- `REJEITADO` ou `CANCELADO`: já está encerrado.

Esse cancelamento de Pedido não deve ser usado como modelo automático para Resíduos; cada domínio possui regras próprias.

---

## 8. Resíduos — fluxo validado na Etapa 3

```text
Produto != Resíduo
```

Um componente de Resíduo pode referenciar Produto para rastreabilidade e sugestão de segurança sem baixar ou repor estoque.

Fluxo operacional atual:

```text
Laboratório informa
      ↓
INFORMADO
      ↓ Gestão recebe
EM_ANALISE
      ↓ Gestão confirma risco/classes/segurança e libera
LIBERADO_PARA_ARMAZENAMENTO
      ↓ Gestor autorizado confirma armazenamento
ARMAZENADO_TEMPORARIAMENTE
      ↓ Gestor autorizado confirma destinação
DESPACHADO
```

Responsabilidade:

```text
usuarioGerador
→ preserva quem informou/gerou

gestorRecebedorInicial
→ preserva quem recebeu e conduziu a conferência inicial

HistoricoResiduo
→ preserva quem executou cada transição real
```

---

## 9. Dados de Resíduo

A ocorrência real preserva dados próprios, incluindo:

```text
procedência/uso (processoOrigem)
estado físico
tratamento realizado
composição
risco informado / confirmado
classes informadas / confirmadas
segurança informada / confirmada
recipiente / quantidade
observações
armazenamento / destino
```

Classes e segurança possuem separação entre declaração e confirmação.

Regra arquitetural:

```text
cadastro atual/editável
≠
snapshot histórico da ocorrência
```

Mudanças futuras em catálogos não podem modificar retroativamente os dados históricos preservados no Resíduo.

---

## 10. Identificação, prévia e impressão de Resíduo

Código:

```text
SGL-RES-AAAA-NNNNNN
```

A identificação existe desde a criação.

```text
identificação
≠
visualização do rótulo
≠
permissão de impressão
```

Fluxo:

```text
INFORMADO
→ código + QR técnico
→ prévia permitida
→ impressão bloqueada

EM_ANALISE
→ prévia permitida
→ impressão bloqueada

LIBERADO_PARA_ARMAZENAMENTO ou posterior
→ impressão permitida
```

O QR técnico pode existir no contrato sem ser renderizado pelo template físico atual. O padrão final, Zebra e infraestrutura física ficam para a Etapa 10.

---

## 11. Etapa 4.1 — expansão do armazenamento 🔧

A Etapa 4 já foi iniciada e a 4.1 está em andamento.

Modelagem aprovada:

```text
LocalArmazenamentoResiduo
= catálogo mutável por Unidade

Residuo.localArmazenamentoResiduo
= referência opcional ao catálogo

Residuo.complementoLocalArmazenamento
= complemento opcional da ocorrência

Residuo.localArmazenamentoTemporario
= snapshot textual histórico completo
```

Fluxo futuro após integração completa da 4.1:

```text
EM_ANALISE
→ Gestão escolhe:
   local cadastrado + complemento opcional
   OU texto manual
→ sistema forma/preserva snapshot textual
→ LIBERADO_PARA_ARMAZENAMENTO

LIBERADO_PARA_ARMAZENAMENTO
→ confirmação física
→ manter local planejado
   OU corrigir catálogo/complemento/manual
→ registrar mudança no histórico
→ ARMAZENADO_TEMPORARIAMENTE
```

Regras:

- local cadastrado pertence à Unidade;
- cadastro possui ativação/inativação;
- inativação não apaga nem invalida histórico;
- renomear cadastro não altera snapshot de ocorrências antigas;
- catálogo e texto manual são caminhos alternativos;
- lookup deve validar tenant/Unidade do Resíduo;
- rótulo e relatório continuam inicialmente usando `localArmazenamentoTemporario`.

Implementação planejada:

```text
4.1-A V16 + entidade + repository
4.1-B CRUD + tenant
4.1-C integração com análise/liberação
4.1-D confirmação física/correção
4.1-E revisão backend
4.1-F frontend Cadastros
4.1-G frontend Gestão
4.1-H regressão e fechamento
```

Próximo passo: **4.1-A**.

---

## 12. Etapas 4.2–4.4 — ainda não implementar

```text
4.2 modelos de Resíduos reutilizáveis
4.3 escolha modelo x preenchimento manual pelo Solicitante
4.4 correções administrativas do ciclo
```

A 4.4 avaliará cancelamento operacional e retorno para análise/liberação com justificativa e histórico. Isso não deve ser confundido com a decisão geral de delete lógico da Etapa 11.

---

## 13. Consulta e rastreabilidade

```text
EstoqueCentral
→ saldo consolidado operacional

Lote
→ saldo físico, validade, apresentação e rastreabilidade

MovimentacaoEstoque
→ explica operações físicas de estoque

HistoricoLaboratorio
→ registra o que o laboratório recebeu

Pedido
→ registra solicitação e decisões do fluxo

Residuo + HistoricoResiduo
→ registra ciclo e responsáveis do Resíduo
```

Esses conceitos não devem ser usados como saldos paralelos.

---

## Fluxo resumido

```text
Contexto institucional / Unidade
  ↓
Catálogo + Estoque por Unidade
  ↓
Lotes
  ↓
Pedido PENDENTE
  ├─ rejeição → REJEITADO
  └─ aprovação
       ├─ baixa FEFO/FIFO
       ├─ movimentação SAIDA/PEDIDO
       └─ APROVADO
            ├─ entrega → histórico → ENTREGUE
            └─ cancelamento → restaura lotes usados → CANCELADO

Laboratório
  ↓
Resíduo INFORMADO
  ↓ receber
EM_ANALISE
  ↓ analisar/liberar
LIBERADO_PARA_ARMAZENAMENTO
  ↓ armazenar
ARMAZENADO_TEMPORARIAMENTE
  ↓ despachar
DESPACHADO
```
