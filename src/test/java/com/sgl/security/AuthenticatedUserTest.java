package com.sgl.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sgl.model.Usuario;
import com.sgl.model.enums.CategoriaCargo;
import com.sgl.model.enums.PapelSistema;

class AuthenticatedUserTest {

    @Test
    void carregaPapeisCategoriaELaboratoriosSobResponsabilidade() {
        Usuario usuario = new Usuario();
        usuario.setPublicId(UUID.randomUUID());
        usuario.substituirPapeis(List.of(PapelSistema.GESTOR));
        usuario.setCategoriaCargo(CategoriaCargo.PESQUISADOR);
        UUID laboratorio = UUID.randomUUID();

        AuthenticatedUser user = AuthenticatedUser.from(usuario, Set.of(laboratorio));

        assertEquals(Set.of(PapelSistema.GESTOR), user.papeis());
        assertEquals(CategoriaCargo.PESQUISADOR, user.categoriaCargo());
        assertTrue(user.temPapel(PapelSistema.GESTOR));
        assertFalse(user.temPapel(PapelSistema.ADMINISTRADOR_LOCAL));
        assertTrue(user.isResponsavelPor(laboratorio));
        assertFalse(user.isResponsavelPor(UUID.randomUUID()));
    }

    @Test
    void papeisDaSessaoNaoMudamSeOUsuarioMudarDepois() {
        Usuario usuario = new Usuario();
        usuario.substituirPapeis(List.of(PapelSistema.GESTOR));

        AuthenticatedUser user = AuthenticatedUser.from(usuario, Set.of());
        usuario.substituirPapeis(List.of());

        assertEquals(Set.of(PapelSistema.GESTOR), user.papeis());
    }
}
