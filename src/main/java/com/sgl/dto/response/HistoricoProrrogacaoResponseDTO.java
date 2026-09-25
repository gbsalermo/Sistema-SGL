package com.sgl.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.model.prorrogacao.HistoricoProrrogacaoAtividade;
import com.sgl.model.prorrogacao.HistoricoProrrogacaoProjeto;
import com.sgl.model.prorrogacao.HistoricoProrrogacaoSci;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "Registro histórico de uma prorrogação.")
@Getter
public class HistoricoProrrogacaoResponseDTO {

    @Schema(description = "UUID público do registro de histórico.")
    private final UUID id;

    @Schema(description = "UUID do usuário responsável pela prorrogação.")
    private final UUID usuarioId;

    @Schema(description = "Nome do usuário responsável pela prorrogação.")
    private final String usuarioNome;

    @Schema(description = "Data final existente antes da prorrogação.")
    private final LocalDate dataFimAnterior;

    @Schema(description = "Nova data final definida pela prorrogação.")
    private final LocalDate dataFimNova;

    @Schema(description = "Justificativa da prorrogação.")
    private final String justificativa;

    @Schema(description = "Data e hora em que a prorrogação foi registrada.")
    private final LocalDateTime dataHora;

    public HistoricoProrrogacaoResponseDTO(
            HistoricoProrrogacaoProjeto entity) {

        this.id = entity.getPublicId();

        this.usuarioId =
                entity.getUsuario() != null
                        ? entity.getUsuario().getPublicId()
                        : null;

        this.usuarioNome =
                entity.getUsuario() != null
                        ? entity.getUsuario().getNome()
                        : null;

        this.dataFimAnterior = entity.getDataFimAnterior();
        this.dataFimNova = entity.getDataFimNova();
        this.justificativa = entity.getJustificativa();
        this.dataHora = entity.getDataHora();
    }

    public HistoricoProrrogacaoResponseDTO(
            HistoricoProrrogacaoSci entity) {

        this.id = entity.getPublicId();

        this.usuarioId =
                entity.getUsuario() != null
                        ? entity.getUsuario().getPublicId()
                        : null;

        this.usuarioNome =
                entity.getUsuario() != null
                        ? entity.getUsuario().getNome()
                        : null;

        this.dataFimAnterior = entity.getDataFimAnterior();
        this.dataFimNova = entity.getDataFimNova();
        this.justificativa = entity.getJustificativa();
        this.dataHora = entity.getDataHora();
    }

    public HistoricoProrrogacaoResponseDTO(
            HistoricoProrrogacaoAtividade entity) {

        this.id = entity.getPublicId();

        this.usuarioId =
                entity.getUsuario() != null
                        ? entity.getUsuario().getPublicId()
                        : null;

        this.usuarioNome =
                entity.getUsuario() != null
                        ? entity.getUsuario().getNome()
                        : null;

        this.dataFimAnterior = entity.getDataFimAnterior();
        this.dataFimNova = entity.getDataFimNova();
        this.justificativa = entity.getJustificativa();
        this.dataHora = entity.getDataHora();
    }
}