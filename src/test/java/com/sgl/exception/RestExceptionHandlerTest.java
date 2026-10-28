package com.sgl.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void accessDenied_retorna403ComCodigoAcessoNegado() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/laboratorios");

        ResponseEntity<ApiError> response =
                handler.handleAccessDenied(new AccessDeniedException("Sessão não autenticada."), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().codigo()).isEqualTo("ACESSO_NEGADO");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/laboratorios");
    }

    @Test
    void outrosErros_naoTrazemCodigo() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/laboratorios");

        ResponseEntity<ApiError> response =
                handler.handleNotFound(new ResourceNotFoundException("x"), request);

        assertThat(response.getBody().codigo()).isNull();
    }
}
