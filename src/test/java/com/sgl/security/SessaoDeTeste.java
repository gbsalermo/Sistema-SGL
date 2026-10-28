package com.sgl.security;

import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;

/** Puts a session user in the security context for unit tests that run without the filter chain. */
public final class SessaoDeTeste {

    private SessaoDeTeste() {
    }

    public static void entrar(UUID publicId, Perfil perfil) {
        AuthenticatedUser user = new AuthenticatedUser(publicId, perfil, SituacaoUsuario.ATIVO, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    public static void sair() {
        SecurityContextHolder.clearContext();
    }
}
