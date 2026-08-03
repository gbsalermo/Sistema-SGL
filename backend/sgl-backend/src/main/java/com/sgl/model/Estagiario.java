package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "estagiarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estagiario implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", nullable = false, unique = true)
	@ToString.Exclude
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "laboratorio_id", nullable = false)
	@ToString.Exclude
	private Laboratorio laboratorio;

	@Column(nullable = false)
	private LocalDate dataInicioEstagio;

	private LocalDate dataFimEstagio;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoBolsa tipoBolsa;

	@Column(nullable = false)
	private String funcao;

	private String observacao;

	@Column(nullable = false)
	private Boolean ativo = true;
}
