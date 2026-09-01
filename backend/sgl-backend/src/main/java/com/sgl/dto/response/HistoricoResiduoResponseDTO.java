package com.sgl.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.model.HistoricoResiduo;
import com.sgl.model.enums.StatusResiduo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "Evento registrado no histórico de rastreabilidade do resíduo.")
@Getter
public class HistoricoResiduoResponseDTO {

    private final UUID id;
    private final UUID usuarioId;
    private final String usuarioNome;
    private final StatusResiduo status;
    private final String acao;
    private final String observacao;
    private final LocalDateTime dataHora;

    public HistoricoResiduoResponseDTO(HistoricoResiduo entity) {
        this.id = entity.getPublicId();
        this.usuarioId = entity.getUsuario().getPublicId();
        this.usuarioNome = entity.getUsuario().getNome();
        this.status = entity.getStatus();
        this.acao = entity.getAcao();
        this.observacao = entity.getObservacao();
        this.dataHora = entity.getDataHora();
    }
}
