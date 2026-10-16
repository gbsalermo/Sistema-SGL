package com.sgl.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.EstadoRecipienteEstoque;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "movimentacoes_recipiente", uniqueConstraints = {
		@UniqueConstraint(name = "uk_mov_rec_public_id", columnNames = "public_id"),
		@UniqueConstraint(name = "uk_mov_rec_movimentacao_recipiente", columnNames = { "movimentacao_estoque_id",
				"recipiente_estoque_id" }) })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoRecipiente implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "public_id", nullable = false, unique = true, updatable = false)
	private UUID publicId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "movimentacao_estoque_id", nullable = false, updatable = false)
	private MovimentacaoEstoque movimentacaoEstoque;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipiente_estoque_id", nullable = false, updatable = false)
	private RecipienteEstoque recipienteEstoque;

	@Column(name = "quantidade_anterior", nullable = false, updatable = false, precision = 19, scale = 6)
	private BigDecimal quantidadeAnterior;

	@Column(name = "quantidade_movimentada", nullable = false, updatable = false, precision = 19, scale = 6)
	private BigDecimal quantidadeMovimentada;

	@Column(name = "quantidade_atual", nullable = false, updatable = false, precision = 19, scale = 6)
	private BigDecimal quantidadeAtual;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_anterior", nullable = false, updatable = false, length = 30)
	private EstadoRecipienteEstoque estadoAnterior;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_atual", nullable = false, updatable = false, length = 30)
	private EstadoRecipienteEstoque estadoAtual;

	@Column(name = "abriu_recipiente", nullable = false, updatable = false)
	private Boolean abriuRecipiente;

	@Column(name = "esgotou_recipiente", nullable = false, updatable = false)
	private Boolean esgotouRecipiente;

	public void validarConsistencia() {

		if (movimentacaoEstoque == null) {
			throw new BusinessRuleException("A movimentação de estoque do detalhe é obrigatória.");
		}

		if (recipienteEstoque == null) {
			throw new BusinessRuleException("O recipiente da movimentação é obrigatório.");
		}

		if (quantidadeAnterior == null || quantidadeAnterior.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("A quantidade anterior do recipiente é inválida.");
		}

		if (quantidadeMovimentada == null || quantidadeMovimentada.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("A quantidade movimentada do recipiente deve ser maior que zero.");
		}

		if (quantidadeAtual == null || quantidadeAtual.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("A quantidade atual do recipiente é inválida.");
		}

		if (estadoAnterior == null || estadoAtual == null) {
			throw new BusinessRuleException("Os estados anterior e atual do recipiente são obrigatórios.");
		}

		if (abriuRecipiente == null || esgotouRecipiente == null) {
			throw new BusinessRuleException("Os indicadores físicos da movimentação são obrigatórios.");
		}
	}

	@PrePersist
	private void prepararPersistencia() {

		if (publicId == null) {
			publicId = UUID.randomUUID();
		}

		if (abriuRecipiente == null) {
			abriuRecipiente = false;
		}

		if (esgotouRecipiente == null) {
			esgotouRecipiente = false;
		}

		validarConsistencia();
	}
}