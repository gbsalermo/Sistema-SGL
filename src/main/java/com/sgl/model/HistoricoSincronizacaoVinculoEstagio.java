package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.model.enums.OrigemSincronizacaoVinculoEstagio;
import com.sgl.model.enums.TipoEventoSincronizacaoVinculoEstagio;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "historico_sincronizacao_vinculo_estagio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoSincronizacaoVinculoEstagio implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "public_id", nullable = false, unique = true, updatable = false)
	private UUID publicId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vinculo_estagio_id", nullable = false)
	@ToString.Exclude
	private VinculoEstagio vinculoEstagio;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_evento", nullable = false, length = 30)
	private TipoEventoSincronizacaoVinculoEstagio tipoEvento;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private OrigemSincronizacaoVinculoEstagio origem;

	@Column(name = "referencia_evento", length = 120)
	private String referenciaEvento;

	@Enumerated(EnumType.STRING)
	@Column(name = "situacao_anterior", length = 30)
	private com.sgl.model.enums.SituacaoEstagio situacaoAnterior;

	@Enumerated(EnumType.STRING)
	@Column(name = "situacao_nova", length = 30)
	private com.sgl.model.enums.SituacaoEstagio situacaoNova;

	@Column(name = "data_fim_prevista_anterior")
	private LocalDate dataFimPrevistaAnterior;

	@Column(name = "data_fim_prevista_nova")
	private LocalDate dataFimPrevistaNova;

	@Column(name = "data_fim_efetiva_anterior")
	private LocalDate dataFimEfetivaAnterior;

	@Column(name = "data_fim_efetiva_nova")
	private LocalDate dataFimEfetivaNova;

	@Column(name = "data_hora_origem")
	private LocalDateTime dataHoraOrigem;

	@Column(name = "data_hora_sincronizacao", nullable = false)
	private LocalDateTime dataHoraSincronizacao;

	@PrePersist
	private void generateDefaults() {

		if (publicId == null) {
			publicId = UUID.randomUUID();
		}

		if (dataHoraSincronizacao == null) {
			dataHoraSincronizacao = LocalDateTime.now();
		}
	}
}