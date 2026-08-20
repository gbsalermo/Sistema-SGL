package com.sgl.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Dados necessários para cadastrar ou atualizar um item do estoque central.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstoqueCentralRequestDTO {

    @Schema(description = "Identificador público UUID da unidade do estoque.", example = "550e8400-e29b-41d4-a716-446655440002", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Id da unidade é obrigatório")
    private UUID unidadeId;

    @Schema(description = "Identificador público UUID do produto armazenado.", example = "550e8400-e29b-41d4-a716-446655440004", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Id do produto é obrigatório")
    private UUID produtoId;

    @Schema(description = "Quantidade mínima configurada para alerta de estoque baixo.", example = "3", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantidade mínima é obrigatória")
    @Min(value = 0, message = "Quantidade mínima não pode ser negativa")
    private Integer quantidadeMinima;

    @Schema(description = "Indica se o registro de estoque está ativo.", example = "true")
    private Boolean ativo;
}
