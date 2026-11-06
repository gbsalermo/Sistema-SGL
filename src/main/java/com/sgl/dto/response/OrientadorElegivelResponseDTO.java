package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.enums.Perfil;

public record OrientadorElegivelResponseDTO(UUID usuarioId, String nome, Perfil perfil, String cargo) {
}
