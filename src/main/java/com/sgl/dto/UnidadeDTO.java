package com.sgl.dto;

import java.io.Serializable;
import java.util.UUID;

import com.sgl.model.Unidade;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnidadeDTO implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	
	private UUID id;
	@NotBlank(message = "Nome é Obrigatório")
	private String nome;
	@NotBlank(message = "Sigla é obrigatória")
	private String sigla;
	
	public UnidadeDTO(Unidade entity) {
		this.id = entity.getPublicId();
		this.nome = entity.getNome();
		this.sigla = entity.getSigla();
	}

}
