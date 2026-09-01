package com.sgl.model;

import java.io.Serializable;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "componentes_residuo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponenteResiduo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "residuo_id", nullable = false)
    @ToString.Exclude
    private Residuo residuo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    @ToString.Exclude
    private Produto produto;

    @Column(name = "nome_componente", nullable = false)
    private String nomeComponente;

    @Column(nullable = false)
    @Builder.Default
    private Boolean principal = false;

    @Column(name = "concentracao_ou_quantidade", length = 100)
    private String concentracaoOuQuantidade;

    @Column(length = 500)
    private String observacao;

    @PrePersist
    private void generateDefaults() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (principal == null) {
            principal = false;
        }
    }
}
