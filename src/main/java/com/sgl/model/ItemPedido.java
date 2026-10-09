package com.sgl.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.model.enums.TipoPedido;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "itens_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedido implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    @ToString.Exclude
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    @ToString.Exclude
    private Produto produto;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantidadeSolicitada;

    @Column(precision = 19, scale = 6)
    private BigDecimal quantidadeAprovada;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida_solicitada", length = 30)
    private UnidadeMedida unidadeMedidaSolicitada;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_embalagem_solicitada")
    private TipoEmbalagem tipoEmbalagemSolicitada;

    @Column(name = "quantidade_embalagens_solicitada")
    private Integer quantidadeEmbalagensSolicitada;

    @Column(name = "multiplicador_solicitado", precision = 19, scale = 6)
    private BigDecimal multiplicadorSolicitado;

    @PrePersist
    private void prepareForPersistence() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }

        // Solucoes usam quantidades fisicas; embalagens sao tratadas no estoque.
        if (pedido != null && pedido.getTipo() == TipoPedido.SOLUCAO) return;

        if (tipoEmbalagemSolicitada == null) {
            tipoEmbalagemSolicitada = TipoEmbalagem.UNITARIO;
        }

        if (multiplicadorSolicitado == null
                || multiplicadorSolicitado.compareTo(BigDecimal.ZERO) <= 0) {
            multiplicadorSolicitado = BigDecimal.ONE;
        }
    }
}
