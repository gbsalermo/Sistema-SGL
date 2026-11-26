package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.enums.CategoriaCargo;

public record OrientadorElegivelResponseDTO(UUID usuarioId, String nome, CategoriaCargo categoriaCargo, String cargo) {
}
