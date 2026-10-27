package com.sgl.corporate;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

import com.sgl.model.enums.Perfil;

/** Suggests a Perfil from the corporate job title; the manager can always override it. */
public final class ProfileSuggestion {

    private ProfileSuggestion() {
    }

    public static Optional<Perfil> fromCargo(String cargo, TipoPessoaCorporativa tipo) {
        // Colaboradores are interns/fellows; the Estagiários tab implies the profile even without a title.
        String text = cargo == null ? "" : Normalizer.normalize(cargo, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        if (text.contains("estagiario") || text.contains("bolsista")) {
            return Optional.of(Perfil.ESTAGIARIO);
        }
        if (text.contains("pesquisador")) {
            return Optional.of(Perfil.PESQUISADOR);
        }
        if (text.contains("analista")) {
            return Optional.of(Perfil.ANALISTA);
        }
        if (text.contains("tecnico") || text.contains("assistente")) {
            return Optional.of(Perfil.TECNICO);
        }
        return Optional.empty();
    }
}
