package com.sgl.model.enums;

/**
 * Former single-valued profile, kept only so seeders and test fixtures can keep describing a
 * person in the old vocabulary. {@code Usuario.setPerfil} translates it into roles and job
 * category; production code never reads it back.
 *
 * @deprecated use {@link PapelSistema} and {@link CategoriaCargo}.
 */
@Deprecated
public enum Perfil {

	ADMINISTRADOR_GERAL,
	ADMINISTRADOR,
	GESTOR,
	TECNICO,
	ANALISTA,
	PESQUISADOR,
	ESTAGIARIO;

	public PapelSistema papel() {
		return switch (this) {
			case ADMINISTRADOR_GERAL -> PapelSistema.ADMINISTRADOR_GERAL;
			case ADMINISTRADOR -> PapelSistema.ADMINISTRADOR_LOCAL;
			case GESTOR -> PapelSistema.GESTOR;
			default -> null;
		};
	}

	/**
	 * Whether a person with these roles and category would have held this profile before roles
	 * accumulated: a management profile matches anyone holding that role; a job profile matches
	 * people with that category and no role. Keeps the legacy {@code ?perfil=} filters working.
	 */
	public boolean descreve(java.util.Set<PapelSistema> papeis, CategoriaCargo categoriaCargo) {
		PapelSistema papel = papel();
		if (papel != null) {
			return papeis.contains(papel);
		}
		return papeis.isEmpty() && categoria() == categoriaCargo;
	}

	/**
	 * The single profile a person would have shown before roles accumulated, for summaries and
	 * exports that still group by it: the highest role wins, otherwise the job category. A person
	 * with neither falls back to TECNICO, the same default a first login gets.
	 */
	public static Perfil legadoDe(java.util.Set<PapelSistema> papeis, CategoriaCargo categoriaCargo) {
		if (papeis.contains(PapelSistema.ADMINISTRADOR_GERAL)) {
			return ADMINISTRADOR_GERAL;
		}
		if (papeis.contains(PapelSistema.ADMINISTRADOR_LOCAL)) {
			return ADMINISTRADOR;
		}
		if (papeis.contains(PapelSistema.GESTOR)) {
			return GESTOR;
		}
		return categoriaCargo == null ? TECNICO : valueOf(categoriaCargo.name());
	}

	public CategoriaCargo categoria() {
		return switch (this) {
			case TECNICO -> CategoriaCargo.TECNICO;
			case ANALISTA -> CategoriaCargo.ANALISTA;
			case PESQUISADOR -> CategoriaCargo.PESQUISADOR;
			case ESTAGIARIO -> CategoriaCargo.ESTAGIARIO;
			default -> null;
		};
	}
}
