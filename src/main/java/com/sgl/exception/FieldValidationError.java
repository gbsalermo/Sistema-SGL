package com.sgl.exception;

public record FieldValidationError(
        String field,
        String message
) {
}
