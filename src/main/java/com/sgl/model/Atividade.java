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
@Table(name = "atividades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Atividade implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "public_id", nullable = false, unique = true, updatable = false)
	private UUID publicId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sci_id", nullable = false)
	@ToString.Exclude
	private Sci sci;

	@Column(name = "codigo_seg", nullable = false, length = 22)
	private String codigoSeg;

	@Column(nullable = false)
	private String nome;

	private String responsavel;

	@Column(name = "data_inicio", nullable = false)
	private LocalDate dataInicio;

	@Column(name = "data_fim")
	private LocalDate dataFim;

	@Default
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 64)
	private StatusProjeto status = StatusProjeto.ATIVO;

	@Default
	@Enumerated(EnumType.STRING)
	@Column(name = "situacao_execucao", nullable = false, length = 64)
	private SituacaoExecucaoProjeto situacaoExecucao = SituacaoExecucaoProjeto.NAO_INFORMADO;

	@Default
	@Column(nullable = false)
	private Boolean ativo = true;

	public void updateDates(LocalDate startDate, LocalDate endDate) {

		if (startDate == null) {
			throw new BusinessRuleException("A data de início da Atividade é obrigatória.");
		}

		if (endDate != null && startDate.isAfter(endDate)) {

			throw new BusinessRuleException("A data de início da Atividade não pode ser posterior à data de fim.");
		}

		this.dataInicio = startDate;
		this.dataFim = endDate;
	}

	public void validateActive() {

		if (!Boolean.TRUE.equals(ativo)) {
			throw new BusinessRuleException("A Atividade informada está inativa.");
		}
	}

	@PrePersist
	private void generatePublicId() {

		if (publicId == null) {
			publicId = UUID.randomUUID();
		}
	}
}