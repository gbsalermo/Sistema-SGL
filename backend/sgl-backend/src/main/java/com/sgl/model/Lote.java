package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "lote",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_lote_estoque_numero",
                        columnNames = {"estoque_central_id", "numero_lote"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lote implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estoque_central_id", nullable = false)
    private EstoqueCentral estoqueCentral;

    @Column(name = "numero_lote", nullable = false, length = 100)
    private String numeroLote;

    @Column(nullable = false)
    private Integer quantidadeInicial;

    @Column(nullable = false)
    private Integer quantidadeDisponivel;

    @Column(nullable = false)
    private LocalDate dataEntrada;

    // Obrigatória para perecíveis; nula para produtos controlados por FIFO.
    private LocalDate dataValidade;

    @Column(nullable = false)
    private Boolean ativo = true;

    @PrePersist
    private void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
