package com.sgl.dto.response;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.Estagiario;
import com.sgl.model.enums.TipoBolsa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Representação de um estagiário retornado pela API.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstagiarioResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Identificador público UUID do estagiário.", example = "550e8400-e29b-41d4-a716-446655440011")
    private UUID id;
    @Schema(description = "Identificador público UUID do usuário associado.", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID usuarioId;
    @Schema(description = "Nome do usuário associado ao estágio.", example = "Maria Oliveira")
    private String usuarioNome;
    @Schema(description = "Identificador público UUID do laboratório.", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID laboratorioId;
    @Schema(description = "Nome do laboratório do estágio.", example = "Laboratório de Química Orgânica")
    private String laboratorioNome;
    @Schema(description = "Data de início do estágio.", example = "2026-08-01")
    private LocalDate dataInicioEstagio;
    @Schema(description = "Data de encerramento do estágio, quando definida.", example = "2027-01-31")
    private LocalDate dataFimEstagio;
    @Schema(description = "Tipo de bolsa do estágio.", example = "ESTAGIO")
    private TipoBolsa tipoBolsa;
    @Schema(description = "Observação registrada no vínculo de estágio.", example = "Estágio vinculado ao projeto de síntese.")
    private String observacao;
    @Schema(description = "Indica se o estágio está ativo.", example = "true")
    private Boolean ativo;

    public EstagiarioResponseDTO(Estagiario entity) {
        this.id = entity.getPublicId();
        this.usuarioId = entity.getPublicId();
        this.usuarioNome = entity.getNome();
        this.laboratorioId = entity.getLaboratorio() != null ? entity.getLaboratorio().getPublicId() : null;
        this.laboratorioNome = entity.getLaboratorio() != null ? entity.getLaboratorio().getNome() : null;
        this.dataInicioEstagio = entity.getDataInicioEstagio();
        this.dataFimEstagio = entity.getDataFimEstagio();
        this.tipoBolsa = entity.getTipoBolsa();
        this.observacao = entity.getObservacao();
        this.ativo = entity.getAtivo();
    }
}
