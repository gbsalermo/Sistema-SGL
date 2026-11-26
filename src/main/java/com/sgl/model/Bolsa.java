package com.sgl.model;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bolsas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bolsa implements Serializable {

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

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @PrePersist
    private void defaults() {
        if (publicId == null) publicId = UUID.randomUUID();
        if (ativo == null) ativo = true;
    }
}
