package com.sgl.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtualizarLoteDTO {

    @NotBlank(message = "Número do lote é obrigatório")
    private String numeroLote;

    private LocalDate dataValidade;

    private Boolean ativo;
}