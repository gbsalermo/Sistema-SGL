package com.sgl.security;

import com.sgl.model.enums.Perfil;

/**
 * Single source of truth for profile-based rules, so that adding a profile
 * does not require hunting for scattered GESTOR/ADMINISTRADOR comparisons.
 */
public final class PerfilPolicy {

	private PerfilPolicy() {
	}

	public static boolean isGestao(Perfil perfil) {
		return perfil == Perfil.GESTOR || perfil == Perfil.ADMINISTRADOR || perfil == Perfil.ADMINISTRADOR_GERAL;
	}

	/** Admin-only operations also accept the general administrator, who operates on every Unidade. */
	public static boolean isAdministrador(Perfil perfil) {
		return perfil == Perfil.ADMINISTRADOR || perfil == Perfil.ADMINISTRADOR_GERAL;
	}

	public static boolean isAdministradorGeral(Perfil perfil) {
		return perfil == Perfil.ADMINISTRADOR_GERAL;
	}

	/**
	 * Answers only "may this grantor assign this profile". Self-change and unit
	 * scoping are enforced by the caller, not here.
	 */
	public static boolean podeConceder(Perfil concedente, Perfil alvo) {
		if (concedente == null || alvo == null) {
			return false;
		}
		return switch (concedente) {
			case ADMINISTRADOR_GERAL -> true;
			case ADMINISTRADOR -> alvo != Perfil.ADMINISTRADOR && alvo != Perfil.ADMINISTRADOR_GERAL;
			case GESTOR -> !isGestao(alvo);
			default -> false;
		};
	}
}
