package com.sgl.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
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
    private DataIntegrityViolationException integridade(String estado, String mensagemTecnica) {
        return new DataIntegrityViolationException("Erro interno nao exibivel",
                new SQLException(mensagemTecnica, estado));
    }

    @Test
    void descricaoLongaIndicaLimiteSemExporDetalhesDoBanco() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/produtos");
        var response = handler.handleDataIntegrity(
                integridade("22001", "VALUE TOO LONG: tabela produtos coluna descricao"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().codigo()).isEqualTo("CAMPO_TEXTO_EXTENSO");
        assertThat(response.getBody().message()).contains("campo de texto");
        assertThat(response.getBody().message()).doesNotContain("tabela", "VALUE TOO LONG");
    }

    @Test
    void duplicidadeDeChaveIndicaConflito() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/produtos");
        var response = handler.handleDataIntegrity(
                integridade("23505", "duplicate key: segredo"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().codigo()).isEqualTo("VALOR_DUPLICADO");
        assertThat(response.getBody().message()).doesNotContain("segredo");
    }

    @Test
    void chaveEstrangeiraInvalidaIndicaReferencia() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/produtos");
        var response = handler.handleDataIntegrity(
                integridade("23503", "foreign key not found"), req);
        assertThat(response.getBody().codigo()).isEqualTo("REFERENCIA_INVALIDA");
    }

    @Test
    void erroNaoClassificadoNaoVazaSqlInterno() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/produtos");
        var response = handler.handleDataIntegrity(
                integridade("99999", "SELECT senha FROM secrets"), req);

        assertThat(response.getBody().codigo()).isEqualTo("INTEGRIDADE_DADOS");
        assertThat(response.getBody().message()).doesNotContain("senha", "SELECT");
    }

}
