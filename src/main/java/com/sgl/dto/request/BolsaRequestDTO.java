package com.sgl.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BolsaRequestDTO {

    @NotBlank(message = "Nome da modalidade é obrigatório")
    @Size(max = 150, message = "Nome deve possuir no máximo 150 caracteres")
    private String nome;

    private Boolean ativo;
}
