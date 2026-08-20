package com.sgl.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Item solicitado em um pedido de materiais.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoRequestDTO {

    @Schema(description = "Identificador público UUID do produto solicitado.", example = "550e8400-e29b-41d4-a716-446655440004", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Id do produto é obrigatório")
    private UUID produtoId;

    @Schema(description = "Quantidade solicitada do produto.", example = "10", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantidade solicitada é obrigatória")
    @Min(value = 1, message = "Quantidade solicitada deve ser no mínimo 1")
    private Integer quantidadeSolicitada;
}
