package com.sgl.corporate;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

import com.sgl.model.enums.CategoriaCargo;

/** Derives the job category from the corporate job title; management can always adjust it. */
public final class CategoriaCargoResolver {

    private CategoriaCargoResolver() {
    }

    public static Optional<CategoriaCargo> fromCargo(String cargo, TipoPessoaCorporativa tipo) {
        // Colaboradores are listed under the Estagiários tab, so the type alone implies the category.
        if (tipo == TipoPessoaCorporativa.COLABORADOR) {
            return Optional.of(CategoriaCargo.ESTAGIARIO);
        }
        String text = cargo == null ? "" : Normalizer.normalize(cargo, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        if (text.contains("estagiario") || text.contains("bolsista")) {
            return Optional.of(CategoriaCargo.ESTAGIARIO);
        }
        if (text.contains("pesquisador")) {
            return Optional.of(CategoriaCargo.PESQUISADOR);
        }
        if (text.contains("analista")) {
            return Optional.of(CategoriaCargo.ANALISTA);
        }
        if (text.contains("tecnico") || text.contains("assistente")) {
            return Optional.of(CategoriaCargo.TECNICO);
        }
        return Optional.empty();
    }
}
