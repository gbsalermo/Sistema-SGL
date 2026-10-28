package com.sgl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Unidade corporativa a habilitar no SGL.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HabilitarUnidadeRequestDTO {

    @Schema(description = "unidadeId da Unidadeapi.", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Código corporativo é obrigatório")
    private Integer codigoCorporativo;
}
