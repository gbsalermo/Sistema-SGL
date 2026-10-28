package com.sgl.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sgl.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates {@code Authorization: Bearer} requests. The token only proves identity; the user is
 * reloaded on every request so a deactivated user loses access on the next call. Any failure leaves
 * the request anonymous and the authorization rules decide (401 on protected routes, pass on public
 * ones), so a stale token never blocks the login page.
 *
 * Deliberately not a @Component: Spring Boot would otherwise also register it as a plain servlet
 * filter outside the security chain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Granted only to ATIVO users; PENDENTE_VINCULACAO sessions lack it and are refused by the rules. */
    public static final String ACTIVE_AUTHORITY = "SITUACAO_ATIVO";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            jwtService.parse(header.substring(BEARER_PREFIX.length()).trim())
                    .flatMap(usuarioRepository::findWithUnidadeByPublicId)
                    .filter(usuario -> Boolean.TRUE.equals(usuario.getAtivo()))
                    .map(AuthenticatedUser::from)
                    .ifPresent(JwtAuthenticationFilter::authenticate);
        }

        filterChain.doFilter(request, response);
    }

    private static void authenticate(AuthenticatedUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.perfil().name()));
        if (!user.isPendente()) {
            authorities.add(new SimpleGrantedAuthority(ACTIVE_AUTHORITY));
        }

        SecurityContext context = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, authorities));
        SecurityContextHolder.getContextHolderStrategy().setContext(context);
    }
}
