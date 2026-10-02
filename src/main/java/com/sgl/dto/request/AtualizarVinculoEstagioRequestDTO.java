package com.sgl.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.enums.FormacaoEstagiario;
import com.sgl.model.enums.TipoBolsa;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtualizarVinculoEstagioRequestDTO {

    @NotNull(message = "Orientador é obrigatório")
    private UUID orientadorId;

    @NotNull(message = "Data de início do vínculo é obrigatória")
    private LocalDate dataInicio;

    @NotNull(message = "Data final prevista é obrigatória")
    private LocalDate dataFimPrevista;

    @NotNull(message = "Tipo de vínculo é obrigatório")
    private TipoBolsa tipoBolsa;

    @NotNull(message = "Formação é obrigatória")
    private FormacaoEstagiario formacao;

    private String formacaoOutro;

    private UUID cursoId;

    private String observacao;
}
