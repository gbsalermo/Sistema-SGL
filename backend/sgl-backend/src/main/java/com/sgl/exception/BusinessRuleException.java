package com.sgl.exception;

/**
 * Representa uma violação de regra de negócio do sistema.
 *
 * Exemplos:
 * - estoque insuficiente;
 * - pedido em status inválido;
 * - usuário inativo;
 * - produto duplicado no pedido.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}