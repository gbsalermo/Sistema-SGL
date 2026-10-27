package com.sgl.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sgl.model.enums.Perfil;

class PerfilPolicyTest {

	@ParameterizedTest
	@EnumSource(Perfil.class)
	void administradorGeral_concedeQualquerPerfil(Perfil alvo) {
		assertTrue(PerfilPolicy.podeConceder(Perfil.ADMINISTRADOR_GERAL, alvo));
	}

	@Test
	void administradorLocal_concedeAteGestor() {
		for (Perfil alvo : new Perfil[] { Perfil.GESTOR, Perfil.TECNICO, Perfil.ANALISTA, Perfil.PESQUISADOR,
				Perfil.ESTAGIARIO }) {
			assertTrue(PerfilPolicy.podeConceder(Perfil.ADMINISTRADOR, alvo), alvo.name());
		}
		assertFalse(PerfilPolicy.podeConceder(Perfil.ADMINISTRADOR, Perfil.ADMINISTRADOR));
		assertFalse(PerfilPolicy.podeConceder(Perfil.ADMINISTRADOR, Perfil.ADMINISTRADOR_GERAL));
	}

	@Test
	void gestor_concedeSoPerfisOperacionais() {
		for (Perfil alvo : new Perfil[] { Perfil.TECNICO, Perfil.ANALISTA, Perfil.PESQUISADOR, Perfil.ESTAGIARIO }) {
			assertTrue(PerfilPolicy.podeConceder(Perfil.GESTOR, alvo), alvo.name());
		}
		assertFalse(PerfilPolicy.podeConceder(Perfil.GESTOR, Perfil.GESTOR));
		assertFalse(PerfilPolicy.podeConceder(Perfil.GESTOR, Perfil.ADMINISTRADOR));
		assertFalse(PerfilPolicy.podeConceder(Perfil.GESTOR, Perfil.ADMINISTRADOR_GERAL));
	}

	@ParameterizedTest
	@EnumSource(value = Perfil.class, names = { "TECNICO", "ANALISTA", "PESQUISADOR", "ESTAGIARIO" })
	void perfisOperacionais_naoConcedemNada(Perfil concedente) {
		for (Perfil alvo : Perfil.values()) {
			assertFalse(PerfilPolicy.podeConceder(concedente, alvo), alvo.name());
		}
	}

	@Test
	void isGestao_incluiAdministradorGeral() {
		assertTrue(PerfilPolicy.isGestao(Perfil.GESTOR));
		assertTrue(PerfilPolicy.isGestao(Perfil.ADMINISTRADOR));
		assertTrue(PerfilPolicy.isGestao(Perfil.ADMINISTRADOR_GERAL));
		assertFalse(PerfilPolicy.isGestao(Perfil.TECNICO));
		assertFalse(PerfilPolicy.isGestao(Perfil.ESTAGIARIO));
		assertTrue(PerfilPolicy.isAdministradorGeral(Perfil.ADMINISTRADOR_GERAL));
		assertFalse(PerfilPolicy.isAdministradorGeral(Perfil.ADMINISTRADOR));
	}

	@Test
	void isAdministrador_incluiAdministradorGeral() {
		assertTrue(PerfilPolicy.isAdministrador(Perfil.ADMINISTRADOR));
		assertTrue(PerfilPolicy.isAdministrador(Perfil.ADMINISTRADOR_GERAL));
		assertFalse(PerfilPolicy.isAdministrador(Perfil.GESTOR));
		assertFalse(PerfilPolicy.isAdministrador(Perfil.TECNICO));
	}
}
