package com.sgl.model;

import java.io.Serializable;
import java.time.LocalDate;

import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private String nome;
	
	private String descricao;
	
	@Column(unique = true)
	private String codigoReferencia;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UnidadeMedida unidadeMedida;
	
	private String localizacaoFisica;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NivelRisco risco = NivelRisco.NENHUM;
	
	@Enumerated(EnumType.STRING)
	private TipoRisco tipoRisco;
	
	private String descricaoRisco;
	
	@Column(nullable = false)
	private Boolean perecivel = false;
	
	//Validade do produto(unid)
	private LocalDate dataValidade;
	
	
	@Enumerated(EnumType.STRING)
	private TipoPerecivel tipoPerecivel;
	
	private String condicoesArmazenamento;
	
	@Column(nullable = false)
	private Boolean ativo = true;
	
	
	

}
