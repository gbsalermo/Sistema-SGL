package com.sgl.dto;

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
public class MovimentacaoEstoqueDTO {

	@NotNull(message = "quantidade é obrigatória")
	@Min(value = 1, message = "quantidade deve ser maior que zero")
	private Integer quantidade;
}
