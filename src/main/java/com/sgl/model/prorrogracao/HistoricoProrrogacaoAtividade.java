package com.sgl.model.prorrogracao;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.model.Atividade;
import com.sgl.model.Usuario;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "historico_prorrogacao_atividade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoProrrogacaoAtividade implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "public_id", nullable = false, unique = true, updatable = false)
	private UUID publicId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "atividade_id", nullable = false)
	@ToString.Exclude
	private Atividade atividade;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", nullable = false)
	@ToString.Exclude
	private Usuario usuario;

	@Column(name = "data_fim_anterior", nullable = false)
	private LocalDate dataFimAnterior;

	@Column(name = "data_fim_nova", nullable = false)
	private LocalDate dataFimNova;

	@Column(nullable = false, length = 1000)
	private String justificativa;

	@Column(name = "data_hora", nullable = false)
	private LocalDateTime dataHora;

	@PrePersist
	private void generateDefaults() {

		if (publicId == null) {
			publicId = UUID.randomUUID();
		}

		if (dataHora == null) {
			dataHora = LocalDateTime.now();
		}
	}
}