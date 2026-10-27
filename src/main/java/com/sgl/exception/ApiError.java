package com.sgl.exception;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta padrão de erro da API.")
public record ApiError(
        @Schema(description = "Data e hora em que o erro ocorreu.", example = "2026-08-20T15:30:00")
        LocalDateTime timestamp,

        @Schema(description = "Código HTTP retornado.", example = "400")
        int status,

        @Schema(description = "Categoria resumida do erro.", example = "Erro de validação")
        String error,

        @Schema(description = "Mensagem detalhada do erro.", example = "Um ou mais campos estão inválidos.")
        String message,

        @Schema(description = "Caminho da requisição que gerou o erro.", example = "/api/v1/pedidos")
        String path,

        @Schema(description = "Erros específicos de campos quando houver falha de validação.")
        List<FieldValidationError> fieldErrors,

        // Machine-readable code the frontend routes on; omitted for errors that do not need one.
        @Schema(description = "Código do erro para tratamento pelo cliente, quando houver.", example = "ACESSO_NEGADO")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String codigo
) {

    public ApiError(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            List<FieldValidationError> fieldErrors) {

        this(timestamp, status, error, message, path, fieldErrors, null);
    }

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
