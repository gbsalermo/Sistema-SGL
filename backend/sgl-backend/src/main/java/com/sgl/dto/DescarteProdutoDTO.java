package com.sgl.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DescarteProdutoDTO {

    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser no mínimo 1")
    private Integer quantidade;

    @NotBlank(message = "Justificativa é obrigatória")
    private String justificativa;

    @NotNull(message = "Id do usuário responsável é obrigatório")
    private Long usuarioId;

    public DescarteProdutoDTO() {
    }

    public DescarteProdutoDTO(Integer quantidade, String justificativa, Long usuarioId) {
        this.quantidade = quantidade;
        this.justificativa = justificativa;
        this.usuarioId = usuarioId;
    }

}
