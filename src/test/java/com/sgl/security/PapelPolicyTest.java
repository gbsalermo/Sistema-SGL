package com.sgl.security;

import static com.sgl.model.enums.PapelSistema.ADMINISTRADOR_GERAL;
import static com.sgl.model.enums.PapelSistema.ADMINISTRADOR_LOCAL;
import static com.sgl.model.enums.PapelSistema.GESTOR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sgl.model.enums.PapelSistema;

class PapelPolicyTest {

	@Test
	void semPapelNaoEhGestao() {
		assertFalse(PapelPolicy.isGestao(Set.of()));
		assertFalse(PapelPolicy.isAdministrador(Set.of()));
	}

	@ParameterizedTest
	@EnumSource(PapelSistema.class)
	void qualquerPapelEhGestao(PapelSistema papel) {
		assertTrue(PapelPolicy.isGestao(Set.of(papel)));
	}

	@Test
	void gestorNaoEhAdministrador() {
		assertFalse(PapelPolicy.isAdministrador(Set.of(GESTOR)));
		assertTrue(PapelPolicy.isAdministrador(Set.of(GESTOR, ADMINISTRADOR_LOCAL)));
		assertFalse(PapelPolicy.isAdministradorGeral(Set.of(ADMINISTRADOR_LOCAL)));
		assertTrue(PapelPolicy.isAdministradorGeral(Set.of(ADMINISTRADOR_GERAL)));
	}

	@ParameterizedTest
	@EnumSource(PapelSistema.class)
	void administradorGeralConcedeQualquerPapel(PapelSistema papel) {
		assertTrue(PapelPolicy.podeConceder(Set.of(ADMINISTRADOR_GERAL), papel));
	}

	@Test
	void administradorLocalConcedeAdministradorLocalEGestor() {
		assertTrue(PapelPolicy.podeConceder(Set.of(ADMINISTRADOR_LOCAL), ADMINISTRADOR_LOCAL));
		assertTrue(PapelPolicy.podeConceder(Set.of(ADMINISTRADOR_LOCAL), GESTOR));
		assertFalse(PapelPolicy.podeConceder(Set.of(ADMINISTRADOR_LOCAL), ADMINISTRADOR_GERAL));
	}

	@Test
	void gestorNaoConcedePapel() {
		assertFalse(PapelPolicy.podeConceder(Set.of(GESTOR), GESTOR));
		assertFalse(PapelPolicy.podeConceder(Set.of(), GESTOR));
	}

	@Test
	void papeisAcumuladosSomamPermissoes() {
		assertTrue(PapelPolicy.podeConceder(Set.of(GESTOR, ADMINISTRADOR_LOCAL), GESTOR));
	}

	@Test
	void podeAlterarExigeConcederTodosOsPapeisDoAlvo() {
		assertFalse(PapelPolicy.podeAlterar(Set.of(ADMINISTRADOR_LOCAL), Set.of(ADMINISTRADOR_GERAL)));
		assertFalse(PapelPolicy.podeAlterar(Set.of(ADMINISTRADOR_LOCAL), Set.of(GESTOR, ADMINISTRADOR_GERAL)));
		assertTrue(PapelPolicy.podeAlterar(Set.of(ADMINISTRADOR_LOCAL), Set.of(GESTOR, ADMINISTRADOR_LOCAL)));
	}

	@Test
	void gestorAlteraQuemNaoTemPapel() {
		// Mirrors the former rule: a GESTOR manages solicitantes, never another management profile.
		assertTrue(PapelPolicy.podeAlterar(Set.of(GESTOR), Set.of()));
		assertFalse(PapelPolicy.podeAlterar(Set.of(GESTOR), Set.of(GESTOR)));
	}

	@Test
	void semPapelNaoAlteraNinguem() {
		assertFalse(PapelPolicy.podeAlterar(Set.of(), Set.of()));
	}
}
