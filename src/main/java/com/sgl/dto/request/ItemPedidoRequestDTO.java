package com.sgl.dto.request;

import java.util.UUID;

import com.sgl.model.enums.TipoEmbalagem;

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

    @Schema(description = "Quantidade total solicitada em unidades individuais.", example = "50", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantidade solicitada é obrigatória")
    @Min(value = 1, message = "Quantidade solicitada deve ser no mínimo 1")
    private Integer quantidadeSolicitada;

    @Schema(description = "Forma física escolhida para retirada.", example = "KIT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Tipo de unidade é obrigatório")
    private TipoEmbalagem tipoEmbalagemSolicitada;

    @Schema(description = "Quantidade de embalagens/unidades solicitadas pelo usuário.", example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantidade da forma de retirada é obrigatória")
    @Min(value = 1, message = "Quantidade da forma de retirada deve ser no mínimo 1")
    private Integer quantidadeEmbalagensSolicitada;

    @Schema(description = "Quantidade de unidades individuais representada por cada embalagem escolhida.", example = "50", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Multiplicador da forma de retirada é obrigatório")
    @Min(value = 1, message = "Multiplicador deve ser no mínimo 1")
    private Integer multiplicadorSolicitado;
}
