package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.OrgaoFiscalizador;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Column(unique = true)
    private String codigoReferencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnidadeMedida unidadeMedida;

    private String localizacaoFisica;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelRisco risco = NivelRisco.NENHUM;

    @Enumerated(EnumType.STRING)
    private TipoRisco tipoRisco;

    private String descricaoRisco;

    @Column(nullable = false)
    private Boolean perecivel = false;

    @Enumerated(EnumType.STRING)
    private TipoPerecivel tipoPerecivel;

    private String condicoesArmazenamento;

    private String unidadeArmazenamento;

    @Column(nullable = false)
    @Builder.Default
    private Boolean fiscalizado = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "produto_orgaos_fiscalizadores",
            joinColumns = @JoinColumn(name = "produto_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "orgao", nullable = false)
    @Builder.Default
    private Set<OrgaoFiscalizador> orgaosFiscalizadores = new HashSet<>();

    @Column(length = 500)
    private String observacaoFiscalizacao;

    @Column(nullable = false)
    private Boolean ativo = true;

    public void updateRisk(
            NivelRisco riskLevel,
            TipoRisco riskType,
            String riskDescription) {

        if (riskLevel == null) {
            throw new BusinessRuleException("O nível de risco é obrigatório.");
        }

        this.risco = riskLevel;

        if (riskLevel == NivelRisco.NENHUM) {
            this.tipoRisco = null;
            this.descricaoRisco = null;
            return;
        }

        if (riskType == null) {
            throw new BusinessRuleException(
                    "O tipo de risco é obrigatório para produtos com risco."
            );
        }

        this.tipoRisco = riskType;
        this.descricaoRisco = riskDescription;
    }

    public void updatePerishability(Boolean isPerishable, TipoPerecivel perishableType) {
        boolean perishable = Boolean.TRUE.equals(isPerishable);
        this.perecivel = perishable;

        if (!perishable) {
            this.tipoPerecivel = null;
            return;
        }

        if (perishableType == null) {
            throw new BusinessRuleException(
                    "O tipo de perecível é obrigatório para produtos perecíveis."
            );
        }

        this.tipoPerecivel = perishableType;
    }

    public void updateFiscalizacao(
            Boolean isFiscalizado,
            Set<OrgaoFiscalizador> orgaos,
            String observacao) {

        boolean controlado = Boolean.TRUE.equals(isFiscalizado);
        this.fiscalizado = controlado;

        if (!controlado) {
            this.orgaosFiscalizadores.clear();
            this.observacaoFiscalizacao = null;
            return;
        }

        if (orgaos == null || orgaos.isEmpty()) {
            throw new BusinessRuleException(
                    "Informe ao menos um órgão fiscalizador para produtos fiscalizados."
            );
        }

        this.orgaosFiscalizadores.clear();
        this.orgaosFiscalizadores.addAll(orgaos);
        this.observacaoFiscalizacao = observacao;
    }

    public void validateLotExpirationDate(LocalDate expirationDate) {
        if (Boolean.TRUE.equals(perecivel) && expirationDate == null) {
            throw new BusinessRuleException(
                    "Data de validade é obrigatória para produto perecível."
            );
        }

        if (!Boolean.TRUE.equals(perecivel) && expirationDate != null) {
            throw new BusinessRuleException(
                    "Produto não perecível não deve possuir data de validade no lote."
            );
        }
    }

    public void validateActive() {
        if (!Boolean.TRUE.equals(ativo)) {
            throw new BusinessRuleException("O produto está inativo.");
        }
    }

    @PrePersist
    private void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (fiscalizado == null) {
            fiscalizado = false;
        }
        if (orgaosFiscalizadores == null) {
            orgaosFiscalizadores = new HashSet<>();
        }
    }
}
