package com.sgl.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes the security layer's error bodies. They are produced before the DispatcherServlet, so
 * RestExceptionHandler cannot shape them; the frontend routes on {@code codigo} alone.
 */
public final class SecurityErrorWriter {

    public static final String NAO_AUTENTICADO = "NAO_AUTENTICADO";
    public static final String USUARIO_PENDENTE = "USUARIO_PENDENTE";
    public static final String UNIDADE_NAO_PERMITIDA = "UNIDADE_NAO_PERMITIDA";
    public static final String ACESSO_NEGADO = "ACESSO_NEGADO";

    private SecurityErrorWriter() {
    }

    public static void write(HttpServletResponse response, int status, String codigo) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // codigo is always one of the constants above, so no JSON escaping is needed.
        response.getWriter().write("{\"codigo\":\"" + codigo + "\"}");
    }
}
