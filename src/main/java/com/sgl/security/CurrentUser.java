package com.sgl.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sgl.tenant.TenantContext;

/** Static access to the session user, mirroring how {@link TenantContext} exposes the tenant. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthenticatedUser> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static AuthenticatedUser get() {
        return find().orElseThrow(() -> new AccessDeniedException("Sessão não autenticada."));
    }

    /** The Unidade resolved by the tenant filter: the user's own, or the one an ADMINISTRADOR_GERAL selected. */
    public static UUID unidadeEmOperacao() {
        return TenantContext.unidadeAtual()
                .orElseThrow(() -> new AccessDeniedException("Nenhuma Unidade em operação para esta sessão."));
    }
}
