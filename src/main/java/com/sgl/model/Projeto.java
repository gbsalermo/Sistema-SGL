package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "projetos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Projeto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "laboratorio_id", nullable = false)
    @ToString.Exclude
    private Laboratorio laboratorio;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    private LocalDate dataInicio;

    private LocalDate dataFim;

    private String responsavel;
    
    @Column(name = "codigo_seg", length = 18)
    private String codigoSeg;
    
    @Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private StatusProjeto status = StatusProjeto.ATIVO;
    
    @Default
    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_execucao", nullable = false, length = 64)
    private SituacaoExecucaoProjeto situacaoExecucao = SituacaoExecucaoProjeto.NAO_INFORMADO;
    
    @Default
    @Column(name = "possui_recurso_externo", nullable = false)
    private Boolean possuiRecursoExterno = false;
    
    @Column(name = "empresa_recurso_externo")
    private String empresaRecursoExterno;
    
    @Default
    @Column(nullable = false)
    private Boolean ativo = true;

    public void updateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate != null) {
            throw new BusinessRuleException(
                    "A data de início é obrigatória quando a data de fim for informada."
            );
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessRuleException(
                    "A data de início não pode ser posterior à data de fim."
            );
        }

        this.dataInicio = startDate;
        this.dataFim = endDate;
    }

    public void validateActive() {
        if (!Boolean.TRUE.equals(ativo)) {
            throw new BusinessRuleException("O projeto informado está inativo.");
        }
    }

    @PrePersist
    private void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
