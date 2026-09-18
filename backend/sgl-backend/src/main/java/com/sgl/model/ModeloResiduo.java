package com.sgl.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.EstadoFisicoResiduo;
import com.sgl.model.enums.MedidaSeguranca;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "modelos_residuo",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_modelos_residuo_unidade_nome",
                        columnNames = {
                                "unidade_id",
                                "nome"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeloResiduo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "public_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "unidade_id",
            nullable = false
    )
    private Unidade unidade;

    @Column(
            nullable = false,
            length = 150
    )
    private String nome;

    @Column(
            nullable = false,
            length = 1000
    )
    private String descricao;

    @Column(
            name = "processo_origem",
            nullable = false,
            length = 1000
    )
    private String processoOrigem;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "estado_fisico",
            nullable = false
    )
    private EstadoFisicoResiduo estadoFisico;

    @Column(
            name = "tratamento_realizado",
            nullable = false
    )
    private Boolean tratamentoRealizado;

    @Column(
            name = "descricao_tratamento",
            length = 1000
    )
    private String descricaoTratamento;

    @Column(
            nullable = false,
            length = 255
    )
    private String recipiente;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "unidade_medida",
            nullable = false
    )
    private UnidadeMedida unidadeMedida;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "nivel_risco",
            nullable = false
    )
    private NivelRisco nivelRisco;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "modelo_residuo_riscos",
            joinColumns = @JoinColumn(
                    name = "modelo_residuo_id"
            )
    )
    @Enumerated(EnumType.STRING)
    @Column(
            name = "risco",
            nullable = false
    )
    @Builder.Default
    private Set<TipoRisco> riscos =
            new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "modelo_residuo_classes",
            joinColumns = @JoinColumn(
                    name = "modelo_residuo_id"
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "classe_residuo_id"
            )
    )
    @Builder.Default
    private Set<ClasseResiduo> classes =
            new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "modelo_residuo_medidas_seguranca",
            joinColumns = @JoinColumn(
                    name = "modelo_residuo_id"
            )
    )
    @Enumerated(EnumType.STRING)
    @Column(
            name = "medida",
            nullable = false
    )
    @Builder.Default
    private Set<MedidaSeguranca> medidasSeguranca =
            new LinkedHashSet<>();

    @Column(
            name = "observacao_seguranca",
            length = 1000
    )
    private String observacaoSeguranca;

    @OneToMany(
            mappedBy = "modeloResiduo",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ComponenteModeloResiduo> componentes =
            new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    public void addComponente(
            ComponenteModeloResiduo componente) {

        componente.setModeloResiduo(this);
        componentes.add(componente);
    }

    public void validateActive() {

        if (!Boolean.TRUE.equals(ativo)) {
            throw new BusinessRuleException(
                    "O modelo de resíduo informado está inativo."
            );
        }
    }

    @PrePersist
    private void generateDefaults() {

        if (publicId == null) {
            publicId = UUID.randomUUID();
        }

        if (ativo == null) {
            ativo = true;
        }

        if (riscos == null) {
            riscos = new LinkedHashSet<>();
        }

        if (classes == null) {
            classes = new LinkedHashSet<>();
        }

        if (medidasSeguranca == null) {
            medidasSeguranca =
                    new LinkedHashSet<>();
        }

        if (componentes == null) {
            componentes = new ArrayList<>();
        }
    }
}