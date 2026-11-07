package com.sgl.dto.response;

import java.util.List;
import java.util.UUID;

/** Approved intern who still has no internship link; the list may be empty. */
public record EstagiarioSemVinculoResponseDTO(UUID usuarioId, String nome, List<LaboratorioResumoDTO> laboratorios) {
}
