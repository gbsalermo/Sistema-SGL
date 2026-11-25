package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.ModalidadeBolsa;
import com.sgl.model.enums.TipoBolsa;
import lombok.Getter;

@Getter
public class ModalidadeBolsaResponseDTO {
    private final UUID id;
    private final UUID unidadeId;
    private final String nome;
    private final TipoBolsa tipoBase;
    private final Boolean ativo;

    public ModalidadeBolsaResponseDTO(ModalidadeBolsa entity) {
        this.id = entity.getPublicId();
        this.unidadeId = entity.getUnidade().getPublicId();
        this.nome = entity.getNome();
        this.tipoBase = entity.getTipoBase();
        this.ativo = entity.getAtivo();
    }
}
