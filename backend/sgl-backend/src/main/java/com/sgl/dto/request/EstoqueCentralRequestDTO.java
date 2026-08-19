package com.sgl.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstoqueCentralRequestDTO {

    @NotNull(message = "Id da unidade é obrigatório")
    private UUID unidadeId;

    @NotNull(message = "Id do produto é obrigatório")
    private UUID produtoId;

    @NotNull(message = "Quantidade mínima é obrigatória")
    @Min(value = 0, message = "Quantidade mínima não pode ser negativa")
    private Integer quantidadeMinima;

    private Boolean ativo;
}
