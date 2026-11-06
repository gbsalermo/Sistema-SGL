package com.sgl.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.sgl.model.enums.Perfil;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.security.AuthenticatedUser;
import com.sgl.security.CurrentUser;
import com.sgl.security.JwtAuthenticationFilter;
import com.sgl.security.JwtService;
import com.sgl.security.SecurityErrorWriter;
import com.sgl.tenant.TenantRequestFilter;

import jakarta.servlet.http.HttpServletResponse;

/**
 * The HTTP security chain. Kept apart from {@link SecurityConfig} because it needs the JWT service
 * and repositories, which {@code @WebMvcTest} slices (they import SecurityConfig) do not load.
 */
@Configuration
@EnableWebSecurity
public class ApiSecurityConfig {

    private static final List<HttpMethod> GRAVACOES =
            List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);

    private final Environment environment;

    public ApiSecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            UnidadeRepository unidadeRepository,
            CorsConfigurationSource corsConfigurationSource) throws Exception {

        // The H2 console is a local development aid only; everywhere else it stays behind a session.
        boolean devProfile = environment.acceptsProfiles(Profiles.of("dev"));

        http
            .csrf(csrf -> csrf.disable()) // Stateless Bearer tokens: no cookie for CSRF to ride on.
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> {
                headers.referrerPolicy(referrer ->
                        referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
                if (devProfile) {
                    headers.frameOptions(frame -> frame.disable()); // H2 console renders in an iframe.
                }
            })
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/dev-login").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/auth/config").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error").permitAll();
                if (devProfile) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                // A PENDENTE_VINCULACAO user may only ask "who am I" until an admin approves them.
                auth.requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated();
                autorizarGravacoes(auth);
                auth.anyRequest().hasAuthority(JwtAuthenticationFilter.ACTIVE_AUTHORITY);
            })
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, ex) ->
                        SecurityErrorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                                SecurityErrorWriter.NAO_AUTENTICADO))
                .accessDeniedHandler((request, response, ex) ->
                        SecurityErrorWriter.write(response, HttpServletResponse.SC_FORBIDDEN, accessDeniedCode())))
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, usuarioRepository),
                    UsernamePasswordAuthenticationFilter.class)
            // After authorization: the tenant is resolved only for requests that are already allowed.
            .addFilterAfter(new TenantRequestFilter(unidadeRepository), AuthorizationFilter.class)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    /**
     * Default-deny for writes: the web hides buttons per profile, but the API must not rely on that.
     * Every POST/PUT/PATCH/DELETE under /api/v1 needs a gestão profile unless listed here as stricter
     * (admin cadastros, Unidades) or as one of the few writes a solicitante screen performs. Service
     * checks stay in place as a second line. First match wins, so specific rules come before the
     * general one.
     */
    private static void autorizarGravacoes(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        AuthorizationManager<RequestAuthorizationContext> ativo =
                AuthorityAuthorizationManager.hasAuthority(JwtAuthenticationFilter.ACTIVE_AUTHORITY);
        AuthorizationManager<RequestAuthorizationContext> administradorGeral = AuthorizationManagers.allOf(
                ativo, AuthorityAuthorizationManager.hasRole(Perfil.ADMINISTRADOR_GERAL.name()));
        AuthorizationManager<RequestAuthorizationContext> administrador = AuthorizationManagers.allOf(
                ativo, AuthorityAuthorizationManager.hasAnyRole(
                        Perfil.ADMINISTRADOR.name(), Perfil.ADMINISTRADOR_GERAL.name()));
        AuthorizationManager<RequestAuthorizationContext> gestao = AuthorizationManagers.allOf(
                ativo, AuthorityAuthorizationManager.hasAnyRole(
                        Perfil.GESTOR.name(), Perfil.ADMINISTRADOR.name(), Perfil.ADMINISTRADOR_GERAL.name()));

        for (HttpMethod metodo : GRAVACOES) {
            auth.requestMatchers(metodo, "/api/v1/unidades", "/api/v1/unidades/**").access(administradorGeral)
                // Cadastros the web only offers on PERFIS_ADMIN routes (CadastrosAdminView and the
                // resíduo/solução catalogues); the resíduo correction is admin-only in ResiduoService too.
                .requestMatchers(metodo,
                        "/api/v1/laboratorios", "/api/v1/laboratorios/**",
                        "/api/v1/produtos", "/api/v1/produtos/**",
                        "/api/v1/classes-residuo", "/api/v1/classes-residuo/**",
                        "/api/v1/locais-armazenamento-residuo", "/api/v1/locais-armazenamento-residuo/**",
                        "/api/v1/modelos-residuo", "/api/v1/modelos-residuo/**",
                        "/api/v1/modelos-solucao", "/api/v1/modelos-solucao/**",
                        // Only the Projeto record itself: its prorrogações and código SEG
                        // corrections live in the gestão screen ProjetosGestaoView.
                        "/api/v1/projetos", "/api/v1/projetos/*",
                        "/api/v1/residuos/*/administrar").access(administrador);
        }
        // The only writes solicitante screens make: NovoPedidoView, NovaSolucaoView, InformarResiduoView.
        // Their services pin the acting user to the session user (CurrentUser.exigirProprioUsuario).
        auth.requestMatchers(HttpMethod.POST, "/api/v1/pedidos", "/api/v1/solucoes/pedidos", "/api/v1/residuos")
                .access(ativo);
        for (HttpMethod metodo : GRAVACOES) {
            auth.requestMatchers(metodo, "/api/v1/**").access(gestao);
        }
    }

    private static String accessDeniedCode() {
        return CurrentUser.find()
                .filter(AuthenticatedUser::isPendente)
                .map(user -> SecurityErrorWriter.USUARIO_PENDENTE)
                .orElse(SecurityErrorWriter.ACESSO_NEGADO);
    }
}
