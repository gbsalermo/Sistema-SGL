package com.sgl.dto.response;

import java.util.List;
import java.util.UUID;

import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;

public record DevLoginUsuarioDTO(UUID id, String nome, String login, Perfil perfil, UUID unidadeId,
        String unidadeSigla, List<LaboratorioResumoDTO> laboratorios) {

    public static DevLoginUsuarioDTO de(Usuario usuario) {
        // Users without a Unidade (e.g. global admins) are valid: both unit fields stay null.
        return new DevLoginUsuarioDTO(usuario.getPublicId(), usuario.getNome(), usuario.getLogin(),
                usuario.getPerfil(),
                usuario.getUnidade() == null ? null : usuario.getUnidade().getPublicId(),
                usuario.getUnidade() == null ? null : usuario.getUnidade().getSigla(),
                LaboratorioResumoDTO.de(usuario));
    }
}
