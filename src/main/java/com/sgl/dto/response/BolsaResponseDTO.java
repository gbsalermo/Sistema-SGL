package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.Bolsa;
import lombok.Getter;

@Getter
public class BolsaResponseDTO {
    private final UUID id;
    private final UUID unidadeId;
    private final String nome;
    private final Boolean ativo;

    public BolsaResponseDTO(Bolsa entity) {
        this.id = entity.getPublicId();
        this.unidadeId = entity.getUnidade().getPublicId();
        this.nome = entity.getNome();
        this.ativo = entity.getAtivo();
    }
}
