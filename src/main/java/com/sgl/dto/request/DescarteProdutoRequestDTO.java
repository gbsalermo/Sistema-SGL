package com.sgl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Dados necessários para registrar descarte de produto por vencimento.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DescarteProdutoRequestDTO {

    @Schema(description = "Quantidade a ser descartada.", example = "5", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser no mínimo 1")
    private Integer quantidade;

    @Schema(description = "Justificativa obrigatória para o descarte.", example = "Lotes vencidos identificados durante conferência mensal.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Justificativa é obrigatória")
    private String justificativa;
}
