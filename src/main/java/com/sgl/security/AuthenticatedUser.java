package com.sgl.security;

import java.util.Set;
import java.util.UUID;

import com.sgl.model.Usuario;
import com.sgl.model.enums.CategoriaCargo;
import com.sgl.model.enums.PapelSistema;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;

/**
 * Snapshot of the session user, rebuilt from the database on every request so that blocking,
 * approval and role changes take effect immediately instead of when the token expires.
 * {@code laboratoriosResponsavel} holds the public ids of the active laboratories whose
 * responsável is this user: being responsável is an association, not a stored role.
 */
public record AuthenticatedUser(UUID publicId, Set<PapelSistema> papeis, CategoriaCargo categoriaCargo,
        Set<UUID> laboratoriosResponsavel, SituacaoUsuario situacao, UUID unidadePublicId) {

    public AuthenticatedUser {
        papeis = Set.copyOf(papeis);
        laboratoriosResponsavel = Set.copyOf(laboratoriosResponsavel);
    }

    /**
     * Describes a session in the former single-profile vocabulary, for tests written before roles.
     *
     * @deprecated build with roles and category.
     */
    @Deprecated
    public AuthenticatedUser(UUID publicId, Perfil perfil, SituacaoUsuario situacao, UUID unidadePublicId) {
        this(publicId, perfil.papel() == null ? Set.of() : Set.of(perfil.papel()), perfil.categoria(), Set.of(),
                situacao, unidadePublicId);
    }

    public static AuthenticatedUser from(Usuario usuario, Set<UUID> laboratoriosResponsavel) {
        UUID unidadePublicId = usuario.getUnidade() != null ? usuario.getUnidade().getPublicId() : null;
        return new AuthenticatedUser(usuario.getPublicId(), usuario.getPapeis(), usuario.getCategoriaCargo(),
                laboratoriosResponsavel, usuario.getSituacao(), unidadePublicId);
    }

    public boolean temPapel(PapelSistema papel) {
        return papeis.contains(papel);
    }

    public boolean isResponsavelPor(UUID laboratorioPublicId) {
        return laboratoriosResponsavel.contains(laboratorioPublicId);
    }

    public boolean isPendente() {
        return situacao == SituacaoUsuario.PENDENTE_VINCULACAO;
    }
}
