package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.ClasseResiduo;

import lombok.Getter;

@Getter
public class ClasseResiduoResponseDTO {

    private final UUID id;

    private final UUID unidadeId;
    private final String unidadeNome;

    private final String codigo;
    private final String descricao;
    private final Boolean ativo;

    public ClasseResiduoResponseDTO(
            ClasseResiduo entity) {

        this.id = entity.getPublicId();

        this.unidadeId =
                entity.getUnidade().getPublicId();

        this.unidadeNome =
                entity.getUnidade().getNome();

        this.codigo = entity.getCodigo();
        this.descricao = entity.getDescricao();
        this.ativo = entity.getAtivo();
    }
}