package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.ResiduoClasse;

import lombok.Getter;

@Getter
public class ClasseResiduoSnapshotResponseDTO {

    private final UUID classeId;
    private final String codigo;
    private final String descricao;

    public ClasseResiduoSnapshotResponseDTO(
            ResiduoClasse entity) {

        this.classeId =
                entity.getClasseResiduo()
                        .getPublicId();

        this.codigo =
                entity.getCodigoSnapshot();

        this.descricao =
                entity.getDescricaoSnapshot();
    }
}