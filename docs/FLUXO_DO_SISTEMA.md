# Fluxo do Sistema SGL

**Atualizado em:** 25/09/2026

Este documento descreve como os módulos principais se conectam no estado funcional aprovado e nas etapas de pré-produção já validadas. Detalhes de contrato devem ser confirmados no Swagger/OpenAPI e detalhes de implementação no código da branch integrada à `main`.

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
4. Projetos mantêm um Laboratório responsável/contextual e pertencem à Unidade, mas são o eixo operacional para SCI e Atividades.
5. Produtos formam o catálogo.
6. Cada Unidade possui seu próprio contexto de estoque para os produtos utilizados.

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

A movimentação registra a operação física e o lote afetado quando aplicável.

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

Validações incluem:

- usuário, laboratório, projeto e produtos devem existir;
- usuário e laboratório devem pertencer ao contexto institucional permitido;
- projeto, quando informado, deve ser compatível com o laboratório;
- entidades envolvidas devem estar ativas;
- o mesmo produto não pode aparecer duas vezes;
- deve existir estoque ativo do produto no contexto da Unidade;
- forma de retirada deve ser compatível com a apresentação do produto/lote.

O saldo **não é reduzido na criação**. O pedido é salvo como `PENDENTE`.

---

## 5. Aprovação

```text
Aprovador + Pedido PENDENTE
  → valida itens/quantidades
  → localiza estoque e lotes da Unidade
  → exclui lote vencido da seleção
  → perecível: FEFO
  → não perecível: FIFO
  → reduz lotes utilizados
  → atualiza EstoqueCentral
  → grava quantidade aprovada
  → grava MovimentacaoEstoque SAIDA/PEDIDO
  → altera Pedido para APROVADO
```

Todo o processamento é transacional. Se um item falhar, nenhuma baixa parcial deve permanecer.

Urgência não altera FIFO/FEFO.

---

## 6. Rejeição

Somente pedido `PENDENTE` pode ser rejeitado pelo fluxo comum.

```text
PENDENTE
→ registrar motivo/observação
→ REJEITADO
```

Não altera estoque.

---

## 7. Entrega

Somente pedido `APROVADO` pode ser entregue.

```text
APROVADO
→ registrar HistoricoLaboratorio dos itens aprovados
→ registrar data real de entrega
→ ENTREGUE
```

A entrega **não reduz o estoque novamente**, porque a baixa física aconteceu na aprovação.

---

## 8. Cancelamento de Pedido

- `PENDENTE`: cancela sem alterar estoque.
- `APROVADO`: restaura as quantidades dos **lotes exatos utilizados na aprovação** e muda para `CANCELADO`.
- `ENTREGUE`: não pode ser cancelado pelo fluxo comum.
- `REJEITADO` ou `CANCELADO`: já está encerrado.

A restauração exata dos lotes é a garantia funcional atual.

Esse cancelamento de Pedido não deve ser usado como modelo automático para Resíduos; cada domínio possui regras próprias.

---

## 9. Resíduos — fluxo validado na Etapa 3

Resíduo é domínio próprio:

```text
Produto != Resíduo
```

Um componente de Resíduo pode referenciar Produto para rastreabilidade e sugestão de segurança. Essa referência não baixa nem repõe estoque automaticamente.

Fluxo operacional atual:

```text
Laboratório informa
      ↓
INFORMADO
      ↓ Gestão recebe
EM_ANALISE
      ↓ Gestão confirma risco/classes/segurança e libera
LIBERADO_PARA_ARMAZENAMENTO
      ↓ qualquer Gestor autorizado pode confirmar armazenamento
ARMAZENADO_TEMPORARIAMENTE
      ↓ qualquer Gestor autorizado pode confirmar destinação
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

Portanto, armazenamento ou despacho por outro Gestor não sobrescreve o responsável inicial.

---

## 10. Dados de Resíduo

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

Snapshots impedem que mudanças futuras em catálogos modifiquem Resíduos históricos.

---

## 11. Identificação, prévia e impressão de Resíduo

Código:

```text
SGL-RES-AAAA-NNNNNN
```

A identificação existe desde a criação.

A Etapa 3 separou:

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
→ código + QR
→ prévia permitida
→ impressão bloqueada

EM_ANALISE
→ prévia permitida
→ impressão bloqueada

LIBERADO_PARA_ARMAZENAMENTO ou posterior
→ impressão permitida
```

A prévia anterior à análise usa os dados informados. Depois da liberação, os dados confirmados pela Gestão têm precedência.

Template definitivo, Zebra e infraestrutura física ficam para a Etapa 10.

---

## 12. Expansão de Resíduos — Etapa 4 concluída

A Etapa 3 foi encerrada em 17/09/2026.

A Etapa 4 foi concluída nesta ordem:

```text
4.1 locais de armazenamento cadastráveis
→ 4.2 modelos de Resíduos reutilizáveis
→ 4.3 escolha modelo x preenchimento manual pelo Solicitante
→ 4.4 correções administrativas do ciclo, após definição das regras
```

A 4.4 implementou cancelamento operacional e retorno para análise/liberação com justificativa obrigatória e histórico auditável.

Isso não deve ser confundido com a decisão geral de delete lógico da Etapa 11.

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

## Hierarquia de Projetos — Etapa 5 (5.1 concluído; 5.2 SCI atual)

```text
Laboratório responsável/contextual
└── Projeto
    ├── Código SEG ...00
    └── SCI
        ├── Código SEG ...SS
        └── Atividade
            └── Código SEG ...SS.AAA
```

Projeto é o eixo funcional. Laboratório serve como contexto/filtro institucional; não é necessário entrar no Laboratório para navegar por Projeto.

Toda Atividade pertence a um SCI e, portanto, a um Projeto. Atividades podem encerrar antes do Projeto.



### Estado do Projeto após o 5.1

```text
Projeto
├── Laboratório responsável/contextual
├── Código SEG cadastrado
├── status de negócio
├── situação de execução
├── recurso externo/empresa
└── ativo técnico
```

O bloco 5.2 introduz SCI como entidade obrigatoriamente subordinada ao Projeto. Laboratório não deve ser duplicado em SCI quando puder ser derivado do Projeto sem perda de regra de negócio.
