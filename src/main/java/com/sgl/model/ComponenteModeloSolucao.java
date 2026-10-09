package com.sgl.model;

import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "componentes_modelo_solucao")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ComponenteModeloSolucao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modelo_solucao_id", nullable = false)
    private ModeloSolucao modeloSolucao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false, precision = 19, scale = 6)
    private java.math.BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida", nullable = false, length = 30)
    private com.sgl.model.enums.UnidadeMedida unidadeMedida;

    @PrePersist
    private void gerarId() {
        if (publicId == null) publicId = UUID.randomUUID();
    }
}

