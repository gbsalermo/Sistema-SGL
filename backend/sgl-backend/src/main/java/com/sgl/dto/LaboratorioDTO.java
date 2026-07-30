package com.sgl.dto;

import java.io.Serializable;

import com.sgl.model.Laboratorio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LaboratorioDTO implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private Long id;
	@NotNull(message = "unidadeId é obrigatório")
	private Long unidadeId;
	@NotBlank(message = "nome é obrigatório")
	private String nome;
	private String descricao;
	private Long responsavel;
	private Boolean ativo;
	
	public LaboratorioDTO(Laboratorio entity) {
		
		this.id = entity.getId();
		this.unidadeId = entity.getUnidade() != null ? entity.getUnidade().getId() : null;
		this.nome = entity.getNome();
		this.descricao = entity.getDescricao();
		this.responsavel = entity.getResponsavel() != null ? entity.getResponsavel().getId() : null;
		this.ativo = entity.getAtivo();
		
	}

}
