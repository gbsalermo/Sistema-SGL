package com.sgl.security;

import java.util.Set;

import com.sgl.model.enums.PapelSistema;

/**
 * Single source of truth for role-based rules. Roles accumulate, so every rule asks
 * "does any of the person's roles allow this".
 */
public final class PapelPolicy {

	private PapelPolicy() {
	}

	public static boolean isGestao(Set<PapelSistema> papeis) {
		return !papeis.isEmpty();
	}

	/** Admin-only operations also accept the general administrator, who operates on every Unidade. */
	public static boolean isAdministrador(Set<PapelSistema> papeis) {
		return papeis.contains(PapelSistema.ADMINISTRADOR_LOCAL) || papeis.contains(PapelSistema.ADMINISTRADOR_GERAL);
	}

	public static boolean isAdministradorGeral(Set<PapelSistema> papeis) {
		return papeis.contains(PapelSistema.ADMINISTRADOR_GERAL);
	}

	/**
	 * Answers only "may this grantor assign this role". Self-change and Unidade scoping are
	 * enforced by the caller, not here.
	 */
	public static boolean podeConceder(Set<PapelSistema> concedente, PapelSistema papel) {
		if (papel == null) {
			return false;
		}
		if (concedente.contains(PapelSistema.ADMINISTRADOR_GERAL)) {
			return true;
		}
		return concedente.contains(PapelSistema.ADMINISTRADOR_LOCAL) && papel != PapelSistema.ADMINISTRADOR_GERAL;
	}

	/**
	 * A person may change someone else's access only when they could have granted every role the
	 * target already holds; a target with no role can be managed by any management role.
	 */
	public static boolean podeAlterar(Set<PapelSistema> ator, Set<PapelSistema> papeisDoAlvo) {
		return isGestao(ator) && papeisDoAlvo.stream().allMatch(papel -> podeConceder(ator, papel));
	}
}
