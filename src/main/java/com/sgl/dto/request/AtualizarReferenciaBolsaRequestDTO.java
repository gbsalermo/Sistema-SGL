package com.sgl.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtualizarReferenciaBolsaRequestDTO {

    @NotBlank(message = "Referência da bolsa é obrigatória")
    @Size(max = 120, message = "Referência da bolsa deve possuir no máximo 120 caracteres")
    private String referencia;
}
