package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de auditoria de toda alteração relevante no saldo central.
 *
 * <p>A movimentação não calcula o saldo por conta própria: ela registra o
 * resultado produzido pelo caso de uso de entrada, saída, pedido, descarte ou
 * devolução.</p>
 */
@Entity
@Table(name = "movimentacao_estoque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoEstoque implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    /** Laboratório envolvido, quando a movimentação possuir esse contexto. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratorio_id")
    private Laboratorio laboratorio;

    /** Usuário responsável por autorizar ou executar a operação. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Pedido relacionado, preenchido nas movimentações originadas do fluxo de pedido. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    /** Efeito da operação no estoque: entrada, saída, ajuste, devolução ou descarte. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentacao tipoMovimentacao;

    /** Contexto de negócio que provocou a movimentação. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemMovimentacao origem;

    @Column(nullable = false)
    private Integer quantidadeMovimentada;

    /** Saldo imediatamente antes da operação. */
    @Column(nullable = false)
    private Integer quantidadeAnterior;

    /** Saldo imediatamente depois da operação. */
    @Column(nullable = false)
    private Integer quantidadeAtual;

    @Column(nullable = false)
    private LocalDateTime dataMovimentacao;

    private String observacao;

    /** Registro de estoque efetivamente alterado pela operação. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estoque_central_id", nullable = false)
    private EstoqueCentral estoqueCentral;
}
