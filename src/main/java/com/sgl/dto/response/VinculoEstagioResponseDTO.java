package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

import com.sgl.model.VinculoEstagio;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.model.VinculoEstagioAtividade;

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
    private final List<VinculoEstagioAtividadeResponseDTO>
    participacoesAtividade;

    private final String observacao;

    public VinculoEstagioResponseDTO(
            VinculoEstagio entity) {

        this(entity, List.of());
    }
    
    public VinculoEstagioResponseDTO(
            VinculoEstagio entity,
            List<VinculoEstagioAtividade> participacoes) {

        this.id = entity.getPublicId();

        this.orientadorId =
                entity.getOrientador() != null
                        ? entity.getOrientador().getPublicId()
                        : null;

        this.orientadorNome =
                entity.getOrientador() != null
                        ? entity.getOrientador().getNome()
                        : null;

        this.dataInicio = entity.getDataInicio();
        this.dataFimPrevista = entity.getDataFimPrevista();
        this.dataFimEfetiva = entity.getDataFimEfetiva();

        this.tipoBolsa = entity.getTipoBolsa();
        this.situacao = entity.getSituacao();
        this.observacao = entity.getObservacao();

        this.participacoesAtividade =
                participacoes.stream()
                        .map(VinculoEstagioAtividadeResponseDTO::new)
                        .toList();
    }
}