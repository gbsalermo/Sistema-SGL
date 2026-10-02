package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.Curso;

import lombok.Getter;

@Getter
public class CursoResponseDTO {

	private final UUID id;

	private final UUID unidadeId;
	private final String unidadeNome;

	private final String nome;
	private final Boolean ativo;

	public CursoResponseDTO(Curso entity) {

		this.id = entity.getPublicId();

		this.unidadeId = entity.getUnidade().getPublicId();

		this.unidadeNome = entity.getUnidade().getNome();

		this.nome = entity.getNome();
		this.ativo = entity.getAtivo();
	}
}