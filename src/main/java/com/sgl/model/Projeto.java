package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "projetos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Projeto implements Serializable{
	
	private static final long  serialVersionUId = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "laboratorio_id", nullable = false)
	@ToString.Exclude
	private Laboratorio laboratorio;
	
	@Column(nullable = false)
	private String nome;
	
	private String descricao;
	
	private LocalDate dataInicio;
	
	private LocalDate dataFim;
	
	private String responsavel;
	
	@Column(nullable = false)
	private Boolean ativo = true;

}
