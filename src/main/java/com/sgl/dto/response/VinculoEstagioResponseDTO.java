package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.VinculoEstagio;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoBolsa;

import lombok.Getter;

@Getter
public class VinculoEstagioResponseDTO {

    private final UUID id;

    private final UUID orientadorId;
    private final String orientadorNome;

    private final LocalDate dataInicio;
    private final LocalDate dataFimPrevista;
    private final LocalDate dataFimEfetiva;

    private final TipoBolsa tipoBolsa;
    private final SituacaoEstagio situacao;

    private final String observacao;

    public VinculoEstagioResponseDTO(VinculoEstagio entity) {

        this.id = entity.getPublicId();

        this.orientadorId = entity.getOrientador() != null
                ? entity.getOrientador().getPublicId()
                : null;

        this.orientadorNome = entity.getOrientador() != null
                ? entity.getOrientador().getNome()
                : null;

        this.dataInicio = entity.getDataInicio();
        this.dataFimPrevista = entity.getDataFimPrevista();
        this.dataFimEfetiva = entity.getDataFimEfetiva();

        this.tipoBolsa = entity.getTipoBolsa();
        this.situacao = entity.getSituacao();

        this.observacao = entity.getObservacao();
    }
}