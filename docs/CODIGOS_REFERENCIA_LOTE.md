# Códigos de Referência — Lote

Este documento resume a implementação atual do controle de lotes no SGL.

## Modelo

```text
Produto
  └── EstoqueCentral (Unidade + Produto)
        ├── Lote A
        ├── Lote B
        └── Lote C
```

Regra de consistência:

```text
EstoqueCentral.quantidadeAtual
=
soma de Lote.quantidadeDisponivel
```

`Produto` não possui mais validade operacional. A validade pertence ao lote.

## Entidade `Lote`

Campos principais:

```java
private Long id;
private EstoqueCentral estoqueCentral;
private String numeroLote;
private Integer quantidadeInicial;
private Integer quantidadeDisponivel;
private LocalDate dataEntrada;
private LocalDate dataValidade;
private Boolean ativo;
```

A combinação abaixo deve ser única:

```text
estoque_central_id + numero_lote
```

`quantidadeInicial` representa o recebido originalmente e não deve diminuir.

`quantidadeDisponivel` só deve ser alterada por operações físicas de estoque.

## `EntradaLoteDTO`

A entrada não recebe `usuarioId` do cliente.

```java
public class EntradaLoteDTO {

    @NotBlank
    private String numeroLote;

    @NotNull
    @Min(1)
    private Integer quantidade;

    private LocalDate dataValidade;

    @NotNull
    private OrigemMovimentacao origem;

    private String observacao;
}
```

O usuário responsável deverá ser obtido do contexto de autenticação local e, futuramente, da autenticação corporativa.

## Regra de validade

```text
Produto perecível
→ dataValidade obrigatória no lote
→ saída FEFO

Produto não perecível
→ dataValidade deve ser nula
→ saída FIFO
```

Entrada de lote já vencido deve ser rejeitada.

## `LoteRepository`

### FEFO

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
        SELECT lote
        FROM Lote lote
        WHERE lote.estoqueCentral.id = :estoqueId
          AND lote.ativo = true
          AND lote.quantidadeDisponivel > 0
          AND lote.dataValidade IS NOT NULL
          AND lote.dataValidade >= :dataReferencia
        ORDER BY lote.dataValidade ASC, lote.dataEntrada ASC, lote.id ASC
        """)
List<Lote> buscarDisponiveisPorFefoComBloqueio(
        @Param("estoqueId") Long estoqueId,
        @Param("dataReferencia") LocalDate dataReferencia
);
```

### FIFO

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
        SELECT lote
        FROM Lote lote
        WHERE lote.estoqueCentral.id = :estoqueId
          AND lote.ativo = true
          AND lote.quantidadeDisponivel > 0
        ORDER BY lote.dataEntrada ASC, lote.id ASC
        """)
List<Lote> buscarDisponiveisPorEntradaComBloqueio(
        @Param("estoqueId") Long estoqueId
);
```

### Lotes vencidos

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
        SELECT lote
        FROM Lote lote
        WHERE lote.estoqueCentral.id = :estoqueId
          AND lote.ativo = true
          AND lote.quantidadeDisponivel > 0
          AND lote.dataValidade IS NOT NULL
          AND lote.dataValidade < :dataReferencia
        ORDER BY lote.dataValidade ASC, lote.dataEntrada ASC, lote.id ASC
        """)
List<Lote> buscarVencidosComBloqueio(
        @Param("estoqueId") Long estoqueId,
        @Param("dataReferencia") LocalDate dataReferencia
);
```

## `LoteService`

Responsabilidades:

```text
listar lotes
buscar lote
listar por estoque
listar vencidos
corrigir número/validade
inativar lote sem saldo
```

Não pertence ao `LoteService`:

```text
entrada
saída
alteração direta de quantidade
descarte
devolução
```

Essas operações pertencem ao `MovimentacaoEstoqueService`.

## `MovimentacaoEstoque`

A movimentação possui vínculo opcional com o lote afetado:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "lote_id")
private Lote lote;
```

Não existe entidade `MovimentacaoLote`.

Quando uma operação usa vários lotes, é criada uma movimentação para cada lote.

Exemplo:

```text
Saída total = 8

Lote A = 5
→ MovimentacaoEstoque SAIDA quantidade 5 lote A

Lote B = 3
→ MovimentacaoEstoque SAIDA quantidade 3 lote B
```

## Entrada física

Fluxo:

```text
bloquear EstoqueCentral
→ validar EntradaLoteDTO
→ criar Lote
→ aumentar quantidadeAtual
→ registrar ENTRADA vinculada ao lote
```

Método central:

```java
registrarEntradaLote(
        Long estoqueId,
        EntradaLoteDTO dto,
        Usuario usuario
)
```

## Saída

Método central:

```java
registrarSaida(
        Long estoqueId,
        Integer quantidade,
        Usuario usuario,
        OrigemMovimentacao origem,
        Pedido pedido,
        Laboratorio laboratorio,
        String observacao
)
```

Seleção:

```java
if (Boolean.TRUE.equals(estoque.getProduto().getPerecivel())) {
    // FEFO
} else {
    // FIFO
}
```

Uma saída pode consumir vários lotes. Lotes vencidos não participam de saída normal.

## Pedido

Na aprovação:

```text
PedidoService
→ valida pedido e quantidade
→ MovimentacaoEstoqueService.registrarSaida(...)
→ atualiza quantidadeAprovada
→ marca APROVADO
```

O `PedidoService` não reduz mais diretamente `EstoqueCentral.quantidadeAtual`.

## Descarte

O descarte por validade trabalha apenas com lotes vencidos.

```text
selecionar lotes vencidos
→ reduzir quantidadeDisponivel
→ reduzir quantidadeAtual do estoque
→ registrar DESCARTE_VENCIMENTO por lote
```

## Cancelamento e devolução

As movimentações `SAIDA` do pedido armazenam os lotes utilizados.

No cancelamento aprovado:

```text
buscar SAIDAS do pedido
→ localizar lote de cada saída
→ restaurar exatamente aquela quantidade no lote
→ restaurar EstoqueCentral
```

A movimentação auditada `DEVOLUCAO` será registrada assim que a autenticação local fornecer o usuário executor real do cancelamento.

## Regra arquitetural final

```text
Produto
= catálogo

EstoqueCentral
= saldo agregado

Lote
= composição e rastreabilidade física do saldo

MovimentacaoEstoque
= auditoria do que aconteceu com cada lote

MovimentacaoEstoqueService
= regras que alteram quantidades

PedidoService
= ciclo de vida do pedido
```

Os testes da arquitetura anterior ainda precisam ser migrados antes da integração com PostgreSQL.
