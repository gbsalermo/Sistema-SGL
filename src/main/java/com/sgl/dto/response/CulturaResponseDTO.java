package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.Cultura;

import lombok.Getter;

@Getter
public class CulturaResponseDTO {

	private final UUID id;

	private final UUID unidadeId;
	private final String unidadeNome;

	private final String nome;
	private final Boolean ativo;

	public CulturaResponseDTO(Cultura entity) {

		this.id = entity.getPublicId();

		this.unidadeId = entity.getUnidade().getPublicId();

		this.unidadeNome = entity.getUnidade().getNome();

		this.nome = entity.getNome();
		this.ativo = entity.getAtivo();
	}
}