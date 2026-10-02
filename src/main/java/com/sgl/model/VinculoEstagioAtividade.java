package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "vinculo_estagio_atividade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VinculoEstagioAtividade implements Serializable {

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
        name = "vinculo_estagio_id",
        nullable = false
    )
    @ToString.Exclude
    private VinculoEstagio vinculoEstagio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "atividade_id",
        nullable = false
    )
    @ToString.Exclude
    private Atividade atividade;

    @Column(
        name = "data_inicio_participacao",
        nullable = false
    )
    private LocalDate dataInicioParticipacao;

    @Column(name = "data_fim_participacao")
    private LocalDate dataFimParticipacao;

    @Column(length = 500)
    private String observacao;

    @PrePersist
    private void generatePublicId() {

        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }
}