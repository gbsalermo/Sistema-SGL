package com.sgl.security;

import java.util.UUID;

import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;

/**
 * Snapshot of the session user, rebuilt from the database on every request so that blocking,
 * approval and profile changes take effect immediately instead of when the token expires.
 */
public record AuthenticatedUser(UUID publicId, Perfil perfil, SituacaoUsuario situacao, UUID unidadePublicId) {

    public static AuthenticatedUser from(Usuario usuario) {
        UUID unidadePublicId = usuario.getUnidade() != null ? usuario.getUnidade().getPublicId() : null;
        return new AuthenticatedUser(usuario.getPublicId(), usuario.getPerfil(), usuario.getSituacao(), unidadePublicId);
    }

    public boolean isPendente() {
        return situacao == SituacaoUsuario.PENDENTE_VINCULACAO;
    }
}
