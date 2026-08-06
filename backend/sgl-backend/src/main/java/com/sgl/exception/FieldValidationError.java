package com.sgl.exception;

/**
 * Representa um campo rejeitado pelo Bean Validation.
 */
public record FieldValidationError(
        String field,
        String message
) {
}