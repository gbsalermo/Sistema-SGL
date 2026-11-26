package com.sgl.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sgl.exception.BusinessRuleException;
import com.sgl.model.enums.CategoriaCargo;
import com.sgl.model.enums.PapelSistema;
import com.sgl.model.enums.Perfil;

class UsuarioPapeisTest {

    @Test
    void substituirPapeisRemoveDuplicados() {
        Usuario usuario = new Usuario();

        usuario.substituirPapeis(List.of(PapelSistema.GESTOR, PapelSistema.GESTOR, PapelSistema.ADMINISTRADOR_LOCAL));

        assertEquals(Set.of(PapelSistema.GESTOR, PapelSistema.ADMINISTRADOR_LOCAL), usuario.getPapeis());
        assertTrue(usuario.temPapel(PapelSistema.GESTOR));
        assertFalse(usuario.temPapel(PapelSistema.ADMINISTRADOR_GERAL));
    }

    @Test
    void substituirPorListaVaziaRemoveTodosOsPapeis() {
        Usuario usuario = new Usuario();
        usuario.substituirPapeis(List.of(PapelSistema.GESTOR));

        usuario.substituirPapeis(List.of());

        assertTrue(usuario.getPapeis().isEmpty());
    }

    @Test
    void papeisNaoPodemSerAlteradosPorFora() {
        Usuario usuario = new Usuario();

        assertThrows(UnsupportedOperationException.class, () -> usuario.getPapeis().add(PapelSistema.GESTOR));
    }

    @Test
    void semCategoriaNaoEhEstagiario() {
        Usuario usuario = new Usuario();

        assertFalse(usuario.isEstagiario());
        assertThrows(BusinessRuleException.class, usuario::validateInternProfile);
    }

    @Test
    void categoriaEstagiarioValidaCadastroDeEstagio() {
        Usuario usuario = new Usuario();
        usuario.setCategoriaCargo(CategoriaCargo.ESTAGIARIO);

        assertTrue(usuario.isEstagiario());
        assertDoesNotThrow(usuario::validateInternProfile);
    }

    @Test
    void perfilLegadoDeGestaoViraPapelUnico() {
        Usuario usuario = new Usuario();
        usuario.setCategoriaCargo(CategoriaCargo.PESQUISADOR);

        usuario.setPerfil(Perfil.ADMINISTRADOR);

        assertEquals(Set.of(PapelSistema.ADMINISTRADOR_LOCAL), usuario.getPapeis());
        assertNull(usuario.getCategoriaCargo());
    }

    @Test
    void perfilLegadoDeCargoViraCategoriaSemPapel() {
        Usuario usuario = new Usuario();
        usuario.substituirPapeis(List.of(PapelSistema.GESTOR));

        usuario.setPerfil(Perfil.ESTAGIARIO);

        assertTrue(usuario.getPapeis().isEmpty());
        assertEquals(CategoriaCargo.ESTAGIARIO, usuario.getCategoriaCargo());
    }
}
