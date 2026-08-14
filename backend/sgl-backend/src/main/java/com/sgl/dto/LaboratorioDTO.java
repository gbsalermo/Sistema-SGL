package com.sgl.dto;

import java.io.Serializable;
import java.util.UUID;

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
	
	private UUID id;
	@NotNull(message = "unidadeId é obrigatório")
	private UUID unidadeId;
	@NotBlank(message = "nome é obrigatório")
	private String nome;
	private String descricao;
	private UUID responsavel;
	private Boolean ativo;
	
	public LaboratorioDTO(Laboratorio entity) {
		
		this.id = entity.getPublicId();
		this.unidadeId = entity.getUnidade() != null ? entity.getUnidade().getPublicId() : null;
		this.nome = entity.getNome();
		this.descricao = entity.getDescricao();
		this.responsavel = entity.getResponsavel() != null ? entity.getResponsavel().getPublicId() : null;
		this.ativo = entity.getAtivo();
		
	}

}
