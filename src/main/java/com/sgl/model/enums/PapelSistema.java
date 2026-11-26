package com.sgl.model.enums;

/**
 * System roles a person may accumulate. Job titles (técnico, pesquisador...) are not roles:
 * they come from the corporate directory as {@link CategoriaCargo}.
 */
public enum PapelSistema {
    ADMINISTRADOR_GERAL,
    ADMINISTRADOR_LOCAL,
    GESTOR
}
