package com.sgl.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;

class OrientadorPolicyTest {

	private static Usuario usuario(Perfil perfil, String matricula, String cargo) {
		Usuario usuario = new Usuario();
		usuario.setPerfil(perfil);
		usuario.setMatricula(matricula);
		usuario.setCargo(cargo);
		usuario.setAtivo(true);
		return usuario;
	}

	@Test
	void perfilAnalistaOrienta() {
		assertTrue(OrientadorPolicy.podeOrientar(usuario(Perfil.ANALISTA, null, null)));
	}

	@Test
	void perfilPesquisadorOrienta() {
		assertTrue(OrientadorPolicy.podeOrientar(usuario(Perfil.PESQUISADOR, null, null)));
	}

	@Test
	void administradorComCargoAnalistaAOrienta() {
		assertTrue(OrientadorPolicy.podeOrientar(usuario(Perfil.ADMINISTRADOR, "362535", "ANALISTA A")));
	}

	@Test
	void gestorComCargoPesquisadorOrienta() {
		assertTrue(OrientadorPolicy.podeOrientar(usuario(Perfil.GESTOR, "1", "PESQUISADOR B")));
	}

	@Test
	void tecnicoNaoOrienta() {
		assertFalse(OrientadorPolicy.podeOrientar(usuario(Perfil.TECNICO, "1", "TECNICO A")));
	}

	@Test
	void administradorComCargoTecnicoNaoOrienta() {
		assertFalse(OrientadorPolicy.podeOrientar(usuario(Perfil.ADMINISTRADOR, "1", "TECNICO A")));
	}

	@Test
	void cargoNuloNaoOrienta() {
		assertFalse(OrientadorPolicy.podeOrientar(usuario(Perfil.ADMINISTRADOR, "1", null)));
	}

	@Test
	void colaboradorSemMatriculaNaoOrientaPeloCargo() {
		assertFalse(OrientadorPolicy.podeOrientar(usuario(Perfil.ESTAGIARIO, null, "PESQUISADOR VISITANTE")));
	}

	@Test
	void inativoNaoOrienta() {
		Usuario inativo = usuario(Perfil.ANALISTA, null, null);
		inativo.setAtivo(false);
		assertFalse(OrientadorPolicy.podeOrientar(inativo));
	}

	@Test
	void usuarioNuloNaoOrienta() {
		assertFalse(OrientadorPolicy.podeOrientar(null));
	}
}
