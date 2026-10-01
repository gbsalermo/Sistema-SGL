package com.sgl.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CulturaRequestDTO {

	@NotNull(message = "Unidade é obrigatória")
	private UUID unidadeId;

	@NotBlank(message = "Nome da cultura é obrigatório")
	private String nome;

	private Boolean ativo;
}