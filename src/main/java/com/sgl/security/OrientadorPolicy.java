package com.sgl.security;

import java.util.Optional;

import com.sgl.corporate.CategoriaCargoResolver;
import com.sgl.corporate.TipoPessoaCorporativa;
import com.sgl.model.Usuario;
import com.sgl.model.enums.CategoriaCargo;

/**
 * Single source of truth for who may supervise an intern. Admins and managers
 * are often analysts or researchers at Embrapa, so the corporate job title
 * counts as well as the SGL profile. The unit check stays with the callers.
 */
public final class OrientadorPolicy {

	public static final String MENSAGEM_INELEGIVEL =
			"Orientador deve ser analista ou pesquisador (categoria no SGL ou cargo na Embrapa).";

	private OrientadorPolicy() {
	}

	public static boolean podeOrientar(Usuario usuario) {
		if (usuario == null || !Boolean.TRUE.equals(usuario.getAtivo())) {
			return false;
		}
		if (isAnalistaOuPesquisador(usuario.getCategoriaCargo())) {
			return true;
		}
		// Collaborators have no matricula and their cargo holds the link type
		// (e.g. "PESQUISADOR VISITANTE"), which must never grant supervision.
		String matricula = usuario.getMatricula();
		if (matricula == null || matricula.isBlank()) {
			return false;
		}
		Optional<CategoriaCargo> categoriaPeloCargo =
				CategoriaCargoResolver.fromCargo(usuario.getCargo(), TipoPessoaCorporativa.EMPREGADO);
		return categoriaPeloCargo.map(OrientadorPolicy::isAnalistaOuPesquisador).orElse(false);
	}

	private static boolean isAnalistaOuPesquisador(CategoriaCargo categoria) {
		return categoria == CategoriaCargo.ANALISTA || categoria == CategoriaCargo.PESQUISADOR;
	}
}
