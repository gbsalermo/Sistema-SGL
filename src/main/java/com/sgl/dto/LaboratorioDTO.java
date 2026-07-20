package com.sgl.dto;

import java.io.Serializable;

import com.sgl.model.Laboratorio;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
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
	private boolean ativo;
	
	public LaboratorioDTO(Laboratorio entity) {
		
		this.id = entity.getId();
		this.unidadeId = entity.getUnidade() != null ? entity.getUnidade().getId() : null;
		this.nome = entity.getNome();
		this.descricao = entity.getDescricao();
		this.responsavel = entity.getResponsavel() != null ? entity.getResponsavel().getId() : null;
		this.ativo = entity.getAtivo();
		
	}

}
