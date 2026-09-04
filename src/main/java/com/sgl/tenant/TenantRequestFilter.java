package com.sgl.tenant;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TenantRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String valor = request.getHeader(TenantContext.HEADER_UNIDADE);

        try {
            if (valor != null && !valor.isBlank()) {
                try {
                    TenantContext.definir(UUID.fromString(valor.trim()));
                } catch (IllegalArgumentException ex) {
                    response.sendError(
                            HttpServletResponse.SC_BAD_REQUEST,
                            "Header " + TenantContext.HEADER_UNIDADE + " inválido."
                    );
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.limpar();
        }
    }
}
