package com.sgl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.model.enums.EventoObservacaoVinculoEstagio;
import com.sgl.model.enums.TipoObservacaoVinculoEstagio;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "observacoes_vinculo_estagio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservacaoVinculoEstagio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vinculo_estagio_id", nullable = false)
    @ToString.Exclude
    private VinculoEstagio vinculoEstagio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoObservacaoVinculoEstagio tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventoObservacaoVinculoEstagio evento;

    @Column(length = 1000)
    private String texto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @PrePersist
    private void gerarDefaults() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }

        if (dataHora == null) {
            dataHora = LocalDateTime.now();
        }
    }
}
