package com.sgl.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.model.ObservacaoVinculoEstagio;
import com.sgl.model.enums.EventoObservacaoVinculoEstagio;
import com.sgl.model.enums.TipoObservacaoVinculoEstagio;

import lombok.Getter;

@Getter
public class ObservacaoVinculoEstagioResponseDTO {

    private final UUID id;
    private final TipoObservacaoVinculoEstagio tipo;
    private final EventoObservacaoVinculoEstagio evento;
    private final String texto;
    private final UUID usuarioId;
    private final String usuarioNome;
    private final LocalDateTime dataHora;

    public ObservacaoVinculoEstagioResponseDTO(ObservacaoVinculoEstagio entity) {
        this.id = entity.getPublicId();
        this.tipo = entity.getTipo();
        this.evento = entity.getEvento();
        this.texto = entity.getTexto();
        this.usuarioId = entity.getUsuario().getPublicId();
        this.usuarioNome = entity.getUsuario().getNome();
        this.dataHora = entity.getDataHora();
    }
}
