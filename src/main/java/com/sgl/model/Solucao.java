package com.sgl.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.UnidadeMedida;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solucoes", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_solucoes_unidade_nome",
                columnNames = {"unidade_id", "nome"}
        )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Solucao implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Column(name = "rendimento_quantidade", nullable = false, precision = 19, scale = 6)
    private BigDecimal rendimentoQuantidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "rendimento_unidade", nullable = false, length = 30)
    private UnidadeMedida rendimentoUnidade;

    @Column(length = 160)
    private String concentracao;

    @Column(name = "instrucoes_preparo", length = 4000)
    private String instrucoesPreparo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @OneToMany(
            mappedBy = "solucao",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("ordem ASC")
    @Builder.Default
    private List<SolucaoComponente> componentes = new ArrayList<>();

    public void adicionarComponente(SolucaoComponente componente) {
        componente.setSolucao(this);
        componentes.add(componente);
    }

    public void limparComponentes() {
        componentes.clear();
    }

    public void validateActive() {
        if (!Boolean.TRUE.equals(ativo)) {
            throw new BusinessRuleException("A solução informada está inativa.");
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
        if (componentes == null) {
            componentes = new ArrayList<>();
        }
    }
}
