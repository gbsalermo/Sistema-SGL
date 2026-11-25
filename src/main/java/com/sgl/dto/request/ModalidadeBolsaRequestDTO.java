package com.sgl.dto.request;

import com.sgl.model.enums.TipoBolsa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModalidadeBolsaRequestDTO {

    @NotBlank(message = "Nome da modalidade é obrigatório")
    @Size(max = 150, message = "Nome deve possuir no máximo 150 caracteres")
    private String nome;

    @NotNull(message = "Tipo base de bolsa é obrigatório")
    private TipoBolsa tipoBase;

    private Boolean ativo;
}
