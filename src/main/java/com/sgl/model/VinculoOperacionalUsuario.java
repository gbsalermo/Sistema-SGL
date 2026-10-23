package com.sgl.model;

import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vinculos_operacionais_usuario")
@Getter @Setter
public class VinculoOperacionalUsuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "laboratorio_id", nullable = false)
    private Laboratorio laboratorio;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "projeto_id")
    private Projeto projeto;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sci_id")
    private Sci sci;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "atividade_id")
    private Atividade atividade;
    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;
    @Column(name = "data_fim")
    private LocalDate dataFim;
    @Column(name = "encerrado", nullable = false)
    private Boolean encerrado = false;
    @Column(length = 500)
    private String observacao;
    @PrePersist void criarId() {
        if (publicId == null) publicId = UUID.randomUUID();
    }
    public boolean vigente(LocalDate hoje) {
        return !Boolean.TRUE.equals(encerrado) && !hoje.isBefore(dataInicio) && (dataFim == null || !hoje.isAfter(dataFim));
    }
    public boolean pertenceAoProjeto(Projeto alvo) {
        return projeto != null && projeto.getId().equals(alvo.getId())
            || sci != null && sci.getProjeto().getId().equals(alvo.getId())
            || atividade != null && atividade.getSci().getProjeto().getId().equals(alvo.getId());
    }
}
