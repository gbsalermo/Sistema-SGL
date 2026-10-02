package com.sgl.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProrrogarBolsaVinculoEstagioRequestDTO {

    @NotNull(message = "Nova data final prevista é obrigatória")
    private LocalDate novaDataFimPrevista;
}
