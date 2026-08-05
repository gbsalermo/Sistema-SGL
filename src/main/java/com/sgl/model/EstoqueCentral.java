package com.sgl.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa o saldo de um produto dentro de uma unidade específica.
 *
 * <p>Produto é apenas catálogo; a quantidade disponível pertence a esta
 * entidade. A combinação unidade + produto é única para impedir saldos
 * duplicados dentro da mesma unidade.</p>
 */
@Entity
@Table(name = "estoque_central", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_estoque_unidade_produto",
                columnNames = {"unidade_id", "produto_id"}
        )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstoqueCentral implements Serializable {

    public static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unidade proprietária do saldo e fronteira usada para separar estoques. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    /** Produto do catálogo ao qual este saldo se refere. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    /** Saldo disponível para entradas, saídas e atendimento de pedidos. */
    @Column(nullable = false)
    private Integer quantidadeAtual = 0;

    /** Limite usado para identificar necessidade de reposição. */
    @Column(nullable = false)
    private Integer quantidadeMinima = 0;

    /** Permite bloquear operações sem apagar o histórico do registro. */
    @Column(nullable = false)
    private Boolean ativo = true;
}
