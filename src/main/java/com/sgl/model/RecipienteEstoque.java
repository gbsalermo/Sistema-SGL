package com.sgl.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.EstadoRecipienteEstoque;
import com.sgl.model.enums.TipoEmbalagem;
import com.sgl.model.enums.UnidadeMedida;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recipientes_estoque", uniqueConstraints = {
		@UniqueConstraint(name = "uk_recipiente_public_id", columnNames = "public_id"),
		@UniqueConstraint(name = "uk_recipiente_codigo_interno", columnNames = "codigo_interno"),
		@UniqueConstraint(name = "uk_recipiente_lote_sequencial", columnNames = { "lote_id", "sequencial" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecipienteEstoque implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "public_id", nullable = false, unique = true, updatable = false)
	private UUID publicId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lote_id", nullable = false, updatable = false)
	private Lote lote;

	@Column(name = "sequencial", nullable = false, updatable = false)
	@Setter(lombok.AccessLevel.NONE)
	private Integer sequencial;

	@Column(name = "codigo_interno", nullable = false, unique = true, updatable = false, length = 180)
	@Setter(lombok.AccessLevel.NONE)
	private String codigoInterno;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_embalagem", nullable = false, updatable = false, length = 30)
	private TipoEmbalagem tipoEmbalagem;

	@Column(name = "capacidade_inicial", nullable = false, updatable = false, precision = 19, scale = 6)
	private BigDecimal capacidadeInicial;

	@Column(name = "quantidade_disponivel", nullable = false, precision = 19, scale = 6)
	private BigDecimal quantidadeDisponivel;

	@Enumerated(EnumType.STRING)
	@Column(name = "unidade_medida", nullable = false, updatable = false, length = 30)
	private UnidadeMedida unidadeMedida;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 30)
	private EstadoRecipienteEstoque estado = EstadoRecipienteEstoque.FECHADO;

	@Column(name = "data_abertura")
	private LocalDateTime dataAbertura;

	@Column(name = "data_esgotamento")
	private LocalDateTime dataEsgotamento;

	@Column(length = 500)
	private String observacao;

	public void definirIdentificacao(String codigoInterno, Integer sequencial) {

		if (this.codigoInterno != null || this.sequencial != null) {
			throw new BusinessRuleException("A identificação do recipiente é imutável.");
		}

		if (codigoInterno == null || codigoInterno.isBlank() || sequencial == null || sequencial <= 0) {

			throw new BusinessRuleException("Código interno e sequência do recipiente são obrigatórios.");
		}

		this.codigoInterno = codigoInterno.trim();
		this.sequencial = sequencial;
	}

	public void setQuantidadeDisponivel(BigDecimal quantidadeDisponivel) {

		if (quantidadeDisponivel == null) {
			this.quantidadeDisponivel = null;
			return;
		}

		if (quantidadeDisponivel.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("A quantidade disponível do recipiente não pode ser negativa.");
		}

		if (capacidadeInicial != null && quantidadeDisponivel.compareTo(capacidadeInicial) > 0) {

			throw new BusinessRuleException(
					"A quantidade disponível do recipiente não pode ultrapassar sua capacidade inicial.");
		}

		this.quantidadeDisponivel = quantidadeDisponivel;
	}

	public void validarConsistencia() {

		if (lote == null) {
			throw new BusinessRuleException("O lote do recipiente é obrigatório.");
		}

		if (tipoEmbalagem == null) {
			throw new BusinessRuleException("O tipo de embalagem do recipiente é obrigatório.");
		}

		if (unidadeMedida == null) {
			throw new BusinessRuleException("A unidade de medida do recipiente é obrigatória.");
		}

		if (capacidadeInicial == null || capacidadeInicial.compareTo(BigDecimal.ZERO) <= 0) {

			throw new BusinessRuleException("A capacidade inicial do recipiente deve ser maior que zero.");
		}

		if (quantidadeDisponivel == null || quantidadeDisponivel.compareTo(BigDecimal.ZERO) < 0
				|| quantidadeDisponivel.compareTo(capacidadeInicial) > 0) {

			throw new BusinessRuleException("A quantidade disponível do recipiente é inválida.");
		}

		if (estado == null) {
			throw new BusinessRuleException("O estado do recipiente é obrigatório.");
		}

		validarUnidadeCanonica();
		validarEstadoFisico();
	}

	private void validarUnidadeCanonica() {

		if (lote.getEstoqueCentral() == null || lote.getEstoqueCentral().getProduto() == null
				|| lote.getEstoqueCentral().getProduto().getUnidadeMedida() == null) {
			return;
		}

		UnidadeMedida unidadeProduto = lote.getEstoqueCentral().getProduto().getUnidadeMedida();

		if (unidadeMedida != unidadeProduto) {
			throw new BusinessRuleException("A unidade do recipiente deve ser igual à unidade canônica do produto.");
		}
	}

	private void validarEstadoFisico() {

		switch (estado) {

		case FECHADO -> {

			if (quantidadeDisponivel.compareTo(capacidadeInicial) != 0) {
				throw new BusinessRuleException("Recipiente FECHADO deve possuir a capacidade inicial completa.");
			}

			if (dataAbertura != null || dataEsgotamento != null) {
				throw new BusinessRuleException("Recipiente FECHADO não pode possuir data de abertura ou esgotamento.");
			}
		}

		case ABERTO -> {

			if (quantidadeDisponivel.compareTo(BigDecimal.ZERO) <= 0) {
				throw new BusinessRuleException("Recipiente ABERTO deve possuir saldo maior que zero.");
			}

			if (dataAbertura == null) {
				throw new BusinessRuleException("Recipiente ABERTO deve possuir data de abertura.");
			}

			if (dataEsgotamento != null) {
				throw new BusinessRuleException("Recipiente ABERTO não pode possuir data de esgotamento.");
			}
		}

		case ESGOTADO -> {

			if (quantidadeDisponivel.compareTo(BigDecimal.ZERO) != 0) {
				throw new BusinessRuleException("Recipiente ESGOTADO deve possuir saldo zero.");
			}

			if (dataEsgotamento == null) {
				throw new BusinessRuleException("Recipiente ESGOTADO deve possuir data de esgotamento.");
			}
		}
		}
	}

	@PrePersist
	private void prepararPersistencia() {

		if (publicId == null) {
			publicId = UUID.randomUUID();
		}

		if (estado == null) {
			estado = EstadoRecipienteEstoque.FECHADO;
		}

		validarConsistencia();
	}

	@PreUpdate
	private void validarAntesDeAtualizar() {
		validarConsistencia();
	}
}