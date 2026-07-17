package com.sgl.dto;

import java.io.Serializable;

import com.sgl.model.Unidade;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnidadeDTO implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Long id;
	private String nome;
	private String sigla;
	
	public UnidadeDTO(Unidade entity) {
		this.id = entity.getId();
		this.nome = entity.getNome();
		this.sigla = entity.getSigla();
	}

}
