package com.sgl.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** A corporate-person search or import refused by the SGL; carries the machine-readable {@code codigo}. */
@Getter
public class ImportacaoRejectedException extends RuntimeException {

    public static final String JA_CADASTRADO = "JA_CADASTRADO";
    public static final String UNIDADE_SEM_CODIGO_CORPORATIVO = "UNIDADE_SEM_CODIGO_CORPORATIVO";

    private final HttpStatus status;
    private final String codigo;

    private ImportacaoRejectedException(HttpStatus status, String codigo, String message) {
        super(message);
        this.status = status;
        this.codigo = codigo;
    }

    public static ImportacaoRejectedException jaCadastrado() {
        return new ImportacaoRejectedException(HttpStatus.CONFLICT, JA_CADASTRADO,
                "Esta pessoa já está cadastrada no SGL.");
    }

    // 409 rather than 400: the request is fine, the Unidade's configuration is what blocks it.
    public static ImportacaoRejectedException unidadeSemCodigoCorporativo() {
        return new ImportacaoRejectedException(HttpStatus.CONFLICT, UNIDADE_SEM_CODIGO_CORPORATIVO,
                "A Unidade em operação não tem código corporativo configurado.");
    }
}
