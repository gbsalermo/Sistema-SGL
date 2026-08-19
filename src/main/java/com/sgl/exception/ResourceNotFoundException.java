package com.sgl.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String recurso, Object id) {
        super(recurso + " não encontrado com id: " + id);
    }
}
