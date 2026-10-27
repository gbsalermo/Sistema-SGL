package com.sgl.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * A login attempt refused by the SGL itself. Carries the HTTP status and the machine-readable
 * {@code codigo} the frontend routes on; messages never include CPF or other personal data.
 */
@Getter
public class LoginRejectedException extends RuntimeException {

    public static final String CREDENCIAIS_INVALIDAS = "CREDENCIAIS_INVALIDAS";
    public static final String CONTA_DESATIVADA = "CONTA_DESATIVADA";
    public static final String UNIDADE_NAO_HABILITADA = "UNIDADE_NAO_HABILITADA";
    public static final String MUITAS_TENTATIVAS = "MUITAS_TENTATIVAS";

    private final HttpStatus status;
    private final String codigo;

    private LoginRejectedException(HttpStatus status, String codigo, String message) {
        super(message);
        this.status = status;
        this.codigo = codigo;
    }

    public static LoginRejectedException credenciaisInvalidas() {
        return new LoginRejectedException(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS,
                "Usuário ou senha inválidos.");
    }

    public static LoginRejectedException contaDesativada() {
        return new LoginRejectedException(HttpStatus.FORBIDDEN, CONTA_DESATIVADA,
                "Sua conta está desativada e não pode acessar o SGL.");
    }

    public static LoginRejectedException unidadeNaoHabilitada() {
        return new LoginRejectedException(HttpStatus.FORBIDDEN, UNIDADE_NAO_HABILITADA,
                "Sua Unidade ainda não está habilitada no SGL.");
    }

    public static LoginRejectedException muitasTentativas() {
        return new LoginRejectedException(HttpStatus.TOO_MANY_REQUESTS, MUITAS_TENTATIVAS,
                "Muitas tentativas de login sem sucesso. Aguarde alguns minutos e tente novamente.");
    }
}
