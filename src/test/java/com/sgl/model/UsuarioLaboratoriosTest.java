package com.sgl.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class UsuarioLaboratoriosTest {

    private static Laboratorio laboratorio(Long id, String nome) {
        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setId(id);
        laboratorio.setNome(nome);
        laboratorio.setAtivo(true);
        return laboratorio;
    }

    @Test
    void atuaNoLaboratorioComparaPorId() {
        Usuario usuario = new Usuario();
        usuario.substituirLaboratorios(List.of(laboratorio(1L, "Solos")));

        // A different instance with the same id stands in for a Hibernate proxy or detached copy.
        assertTrue(usuario.atuaNoLaboratorio(laboratorio(1L, "Outro nome")));
        assertFalse(usuario.atuaNoLaboratorio(laboratorio(2L, "Solos")));
        assertFalse(usuario.atuaNoLaboratorio(null));
        assertFalse(usuario.atuaNoLaboratorio(laboratorio(null, "Sem id")));
    }

    @Test
    void laboratoriosOrdenadosPorNome() {
        Usuario usuario = new Usuario();
        usuario.substituirLaboratorios(List.of(
                laboratorio(1L, "Virologia"), laboratorio(2L, "bioquímica"), laboratorio(3L, "Entomologia")));

        assertEquals(List.of("bioquímica", "Entomologia", "Virologia"),
                usuario.laboratoriosOrdenados().stream().map(Laboratorio::getNome).toList());
    }

    @Test
    void substituirTrocaAListaInteira() {
        Usuario usuario = new Usuario();
        usuario.substituirLaboratorios(List.of(laboratorio(1L, "A"), laboratorio(2L, "B")));

        usuario.substituirLaboratorios(List.of(laboratorio(3L, "C")));

        assertEquals(1, usuario.getLaboratorios().size());
        assertTrue(usuario.atuaNoLaboratorio(laboratorio(3L, "C")));
        assertFalse(usuario.atuaNoLaboratorio(laboratorio(1L, "A")));
    }

    @Test
    void limparEsvazia() {
        Usuario usuario = new Usuario();
        usuario.substituirLaboratorios(List.of(laboratorio(1L, "A"), laboratorio(2L, "B")));

        usuario.limparLaboratorios();

        assertTrue(usuario.getLaboratorios().isEmpty());
        assertTrue(usuario.laboratoriosOrdenados().isEmpty());
    }
}
