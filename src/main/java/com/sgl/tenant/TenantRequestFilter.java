package com.sgl.tenant;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sgl.repository.UnidadeRepository;
import com.sgl.security.AuthenticatedUser;
import com.sgl.security.CurrentUser;
import com.sgl.security.PerfilPolicy;
import com.sgl.security.SecurityErrorWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Resolves the Unidade in operation from the session, not from the client. The
 * {@link TenantContext#HEADER_UNIDADE} header is only a request to switch Unidade, honoured for
 * ADMINISTRADOR_GERAL alone; anyone else sending a different Unidade is refused.
 *
 * Runs inside the Spring Security chain after authorization, so it only sees requests that are
 * already allowed. Not a @Component, to avoid a second registration as a plain servlet filter.
 */
public class TenantRequestFilter extends OncePerRequestFilter {

    /**
     * Session endpoints (login, config, me) never operate on a Unidade. A stale header there would
     * otherwise lock a user out of relogin or the waiting screen, so they get no tenant at all.
     */
    private static final RequestMatcher AUTH_ROUTES =
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/auth/**");

    private final UnidadeRepository unidadeRepository;

    public TenantRequestFilter(UnidadeRepository unidadeRepository) {
        this.unidadeRepository = unidadeRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return AUTH_ROUTES.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            Optional<AuthenticatedUser> user = CurrentUser.find();
            // Public routes carry no session, hence no tenant; the header is ignored on purpose.
            if (user.isPresent()) {
                Resolution resolution = resolve(user.get(), request.getHeader(TenantContext.HEADER_UNIDADE));
                if (!resolution.allowed()) {
                    SecurityErrorWriter.write(response, HttpServletResponse.SC_FORBIDDEN,
                            SecurityErrorWriter.UNIDADE_NAO_PERMITIDA);
                    return;
                }
                // A user without Unidade gets no tenant, and tenant-scoped services fail closed.
                if (resolution.unidade() != null) {
                    TenantContext.definir(resolution.unidade());
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.limpar();
        }
    }

    private Resolution resolve(AuthenticatedUser user, String header) {
        UUID propria = user.unidadePublicId();
        if (header == null || header.isBlank()) {
            return Resolution.allow(propria);
        }

        UUID solicitada;
        try {
            solicitada = UUID.fromString(header.trim());
        } catch (IllegalArgumentException ex) {
            return Resolution.DENY;
        }

        if (solicitada.equals(propria)) {
            return Resolution.allow(propria);
        }
        // Only an existing Unidade may be operated, so a typo cannot open an empty phantom tenant.
        if (PerfilPolicy.isAdministradorGeral(user.perfil()) && unidadeRepository.findByPublicId(solicitada).isPresent()) {
            return Resolution.allow(solicitada);
        }
        return Resolution.DENY;
    }

    private record Resolution(boolean allowed, UUID unidade) {

        static final Resolution DENY = new Resolution(false, null);

        static Resolution allow(UUID unidade) {
            return new Resolution(true, unidade);
        }
    }
}
