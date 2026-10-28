package com.sgl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

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
                auth.requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                    .anyRequest().hasAuthority(JwtAuthenticationFilter.ACTIVE_AUTHORITY);
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

    private static String accessDeniedCode() {
        return CurrentUser.find()
                .filter(AuthenticatedUser::isPendente)
                .map(user -> SecurityErrorWriter.USUARIO_PENDENTE)
                .orElse(SecurityErrorWriter.ACESSO_NEGADO);
    }
}
