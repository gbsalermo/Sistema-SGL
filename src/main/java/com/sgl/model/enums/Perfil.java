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
