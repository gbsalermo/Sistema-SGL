package com.sgl.exception;

/**
 * Indica que um recurso solicitado não foi encontrado.
 *
 * Essa exceção é convertida em HTTP 404 pelo RestExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String recurso, Long id) {
        super(recurso + " não encontrado com id: " + id);
    }
}
