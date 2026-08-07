package com.sgl.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntradaLoteDTO {

	@NotBlank(message = "Número do lote é obrigatório")
	private String numeroLote;
	
	@NotNull(message = "Quantidade da entrada é obrigatória")
	@Min(value = 1, message = "Quantidade da entrada deve ser maior que zero")
	private Integer quantidade;
	
	private LocalDate dataValidade;
	
	private String observacao;
}
