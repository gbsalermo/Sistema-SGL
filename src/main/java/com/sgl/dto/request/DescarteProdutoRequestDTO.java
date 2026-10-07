package com.sgl.dto.request;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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
	@DecimalMin(value = "0.0", inclusive = false, message = "Quantidade deve ser maior que zero")
	private BigDecimal quantidade;

	@Schema(description = "Justificativa obrigatória para o descarte.", example = "Lotes vencidos identificados durante conferência mensal.", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Justificativa é obrigatória")
	private String justificativa;
}
