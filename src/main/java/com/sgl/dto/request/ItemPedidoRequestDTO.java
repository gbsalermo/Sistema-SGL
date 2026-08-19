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
public class ItemPedidoRequestDTO {

    @NotNull(message = "Id do produto é obrigatório")
    private UUID produtoId;

    @NotNull(message = "Quantidade solicitada é obrigatória")
    @Min(value = 1, message = "Quantidade solicitada deve ser no mínimo 1")
    private Integer quantidadeSolicitada;
}
