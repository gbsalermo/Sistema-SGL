package com.sgl.dto.request;

import java.time.LocalDate;

import com.sgl.model.enums.TipoBolsa;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NovaBolsaVinculoEstagioRequestDTO {

    @NotNull(message = "Tipo de bolsa é obrigatório")
    private TipoBolsa tipoBolsa;

    @NotNull(message = "Data de início da nova bolsa é obrigatória")
    private LocalDate dataInicio;

    @NotNull(message = "Data final prevista da nova bolsa é obrigatória")
    private LocalDate dataFimPrevista;
}
