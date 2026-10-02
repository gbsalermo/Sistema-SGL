package com.sgl.dto.request;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VinculoEstagioAtividadeCulturasRequestDTO {

	@NotNull(message = "A lista de culturas é obrigatória")
	private Set<UUID> culturaIds = new LinkedHashSet<>();
}