package com.sgl.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtualizarLoteRequestDTO {

    @NotBlank(message = "Número do lote é obrigatório")
    private String numeroLote;

    private LocalDate dataValidade;

    private Boolean ativo;
}
