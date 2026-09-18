package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.LocalArmazenamentoResiduo;

import lombok.Getter;

@Getter
public class LocalArmazenamentoResiduoResponseDTO {
	
	private final UUID id;
	
	private final UUID unidadeId;
	private final String unidadeNome;
	
	private final String nome;
	private final Boolean ativo;
	
	public LocalArmazenamentoResiduoResponseDTO(LocalArmazenamentoResiduo entity) {
	
		this.id = entity.getPublicId();
		this.unidadeId = entity.getUnidade().getPublicId();
		this.unidadeNome = entity.getUnidade().getNome();
		this.nome = entity.getNome();
		this.ativo = entity.getAtivo();
	}

}
