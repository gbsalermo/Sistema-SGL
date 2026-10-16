package com.sgl.model;

import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "modelos_solucao", uniqueConstraints = @UniqueConstraint(name = "uk_modelo_solucao_unidade_nome", columnNames = {"unidade_id", "nome"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ModeloSolucao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Column(name = "instrucoes_preparo", length = 2000)
    private String instrucoesPreparo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @OneToMany(mappedBy = "modeloSolucao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    @Builder.Default
    private java.util.List<ComponenteModeloSolucao> componentes = new java.util.ArrayList<>();

    public void adicionarComponente(ComponenteModeloSolucao componente) {
        componente.setModeloSolucao(this);
        componentes.add(componente);
    }

    @PrePersist
    private void gerarId() {
        if (publicId == null) publicId = UUID.randomUUID();
    }
}

