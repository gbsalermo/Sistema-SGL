package com.sgl.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** A Unidade enablement refused by the SGL; carries the machine-readable {@code codigo}. */
@Getter
public class UnidadeRejectedException extends RuntimeException {

    public static final String UNIDADE_JA_HABILITADA = "UNIDADE_JA_HABILITADA";
    public static final String SIGLA_EM_USO = "SIGLA_EM_USO";

    private final HttpStatus status;
    private final String codigo;

    private UnidadeRejectedException(HttpStatus status, String codigo, String message) {
        super(message);
        this.status = status;
        this.codigo = codigo;
    }

    public static UnidadeRejectedException jaHabilitada() {
        return new UnidadeRejectedException(HttpStatus.CONFLICT, UNIDADE_JA_HABILITADA,
                "Esta Unidade já está habilitada no SGL.");
    }

    // A legacy Unidade without corporate code holds the sigla; linking them is a manual decision.
    public static UnidadeRejectedException siglaEmUso(String sigla) {
        return new UnidadeRejectedException(HttpStatus.CONFLICT, SIGLA_EM_USO,
                "Já existe no SGL uma Unidade com a sigla " + sigla + ", sem código corporativo.");
    }
}
