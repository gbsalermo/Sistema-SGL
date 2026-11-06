package com.sgl.dto.response;

import java.util.UUID;

/** Approved intern who still has no internship link; the lab may be null when there is no base lab. */
public record EstagiarioSemVinculoResponseDTO(UUID usuarioId, String nome, UUID laboratorioId, String laboratorioNome) {
}
