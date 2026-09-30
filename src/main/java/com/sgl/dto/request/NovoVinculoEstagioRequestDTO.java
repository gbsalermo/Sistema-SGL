package com.sgl.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.enums.TipoBolsa;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NovoVinculoEstagioRequestDTO {

    @NotNull(message = "Orientador é obrigatório")
    private UUID orientadorId;

    @NotNull(message = "Atividade inicial é obrigatória")
    private UUID atividadeId;

    @NotNull(message = "Data de início do vínculo é obrigatória")
    private LocalDate dataInicio;

    private LocalDate dataFimPrevista;

    @NotNull(message = "Tipo de vínculo é obrigatório")
    private TipoBolsa tipoBolsa;

    private String observacao;

    private String observacaoParticipacao;
}