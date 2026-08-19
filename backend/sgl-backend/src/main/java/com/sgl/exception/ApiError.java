package com.sgl.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldValidationError> fieldErrors
) {

    public ApiError(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path) {

        this(
                timestamp,
                status,
                error,
                message,
                path,
                null
        );
    }
}
