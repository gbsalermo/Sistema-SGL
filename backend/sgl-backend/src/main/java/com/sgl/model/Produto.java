package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @PrePersist
    private void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
