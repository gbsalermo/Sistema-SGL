package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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

    /*
     * The Unidade is the project's own tenant boundary: with several laboratories
     * it can no longer be derived from "the" laboratory.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id")
    @ToString.Exclude
    private Unidade unidade;

    @Default
    @ManyToMany
    @JoinTable(
            name = "projeto_laboratorios",
            joinColumns = @JoinColumn(name = "projeto_id"),
            inverseJoinColumns = @JoinColumn(name = "laboratorio_id")
    )
    @ToString.Exclude
    private Set<Laboratorio> laboratorios = new LinkedHashSet<>();

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

    /*
     * Compares by id because the set may hold Hibernate proxies or detached
     * instances that are not the same object as the argument.
     */
    public boolean pertenceAoLaboratorio(Laboratorio laboratorio) {
        if (laboratorio == null || laboratorio.getId() == null) {
            return false;
        }
        return laboratorios.stream()
                .anyMatch(item -> Objects.equals(item.getId(), laboratorio.getId()));
    }

    /*
     * Determines the laboratory a pedido/resíduo operates in. Only active
     * laboratories are candidates: an inactive one may stay linked to the project
     * for history, but no new operation can happen there.
     */
    public Laboratorio resolverLaboratorioOperacional(UUID laboratorioPublicIdInformado) {
        if (laboratorioPublicIdInformado != null) {
            Laboratorio informado = laboratorios.stream()
                    .filter(item -> laboratorioPublicIdInformado.equals(item.getPublicId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException(
                            "O laboratório informado não pertence ao projeto."
                    ));
            if (!Boolean.TRUE.equals(informado.getAtivo())) {
                throw new BusinessRuleException("O laboratório informado está inativo.");
            }
            return informado;
        }

        List<Laboratorio> ativos = laboratorios.stream()
                .filter(item -> Boolean.TRUE.equals(item.getAtivo()))
                .toList();
        if (ativos.isEmpty()) {
            throw new BusinessRuleException("O projeto não possui laboratório ativo.");
        }
        if (ativos.size() > 1) {
            throw new BusinessRuleException(
                    "Informe o laboratório: o projeto está em mais de um laboratório."
            );
        }
        return ativos.get(0);
    }

    /*
     * A project stays operable while at least one of its laboratories is active;
     * which one is used is decided per pedido/resíduo by resolverLaboratorioOperacional.
     */
    public boolean possuiLaboratorioAtivo() {
        return laboratorios.stream().anyMatch(item -> Boolean.TRUE.equals(item.getAtivo()));
    }

    public List<Laboratorio> laboratoriosOrdenados() {
        return laboratorios.stream()
                .sorted(Comparator.comparing(Laboratorio::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    @PrePersist
    private void generatePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}
