package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;

import com.sgl.model.enums.TipoBolsa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estagiarios")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Estagiario extends Usuario implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(nullable = false)
	private LocalDate dataInicioEstagio;

	private LocalDate dataFimEstagio;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoBolsa tipoBolsa;

	private String observacao;

	@Column(nullable = false)
	private Boolean ativo = true;
}
