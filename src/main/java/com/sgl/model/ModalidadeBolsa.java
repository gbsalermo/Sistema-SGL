package com.sgl.model;

import java.io.Serializable;
import java.util.UUID;

import com.sgl.model.enums.TipoBolsa;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "modalidades_bolsa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModalidadeBolsa implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id", nullable = false, updatable = false)
    private Unidade unidade;

    @Column(nullable = false, length = 150)
    private String nome;

    // Classificacao legada: preserva contratos e regras existentes de TipoBolsa.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_base", nullable = false, length = 40, updatable = false)
    private TipoBolsa tipoBase;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @PrePersist
    private void defaults() {
        if (publicId == null) publicId = UUID.randomUUID();
        if (ativo == null) ativo = true;
    }
}
