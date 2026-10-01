package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.enums.FormacaoEstagiario;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoBolsa;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "vinculos_estagio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VinculoEstagio implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "public_id", nullable = false, unique = true, updatable = false)
	private UUID publicId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "estagiario_id", nullable = false)
	@ToString.Exclude
	private Estagiario estagiario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "orientador_id")
	@ToString.Exclude
	private Usuario orientador;

	@Column(name = "data_inicio", nullable = false)
	private LocalDate dataInicio;

	@Column(name = "data_fim_prevista")
	private LocalDate dataFimPrevista;

	@Column(name = "data_fim_efetiva")
	private LocalDate dataFimEfetiva;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_bolsa", nullable = false)
	private TipoBolsa tipoBolsa;

	@Enumerated(EnumType.STRING)
	@Column(name = "formacao", length = 40)
	private FormacaoEstagiario formacao;

	@Column(name = "formacao_outro", length = 150)
	private String formacaoOutro;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "curso_id")
	@ToString.Exclude
	private Curso curso;

	@Column(name = "treinamento_seguranca_concluido", nullable = false)
	private Boolean treinamentoSegurancaConcluido = false;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SituacaoEstagio situacao;

	private String observacao;

	@PrePersist
	private void generateDefaults() {

		if (publicId == null) {
			publicId = UUID.randomUUID();
		}

		if (treinamentoSegurancaConcluido == null) {
			treinamentoSegurancaConcluido = false;
		}
	}
}