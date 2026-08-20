package com.sgl.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Erro de validação associado a um campo específico da requisição.")
public record FieldValidationError(
        @Schema(description = "Nome do campo inválido.", example = "laboratorioId")
        String field,

        @Schema(description = "Mensagem de validação associada ao campo.", example = "Id do laboratório é obrigatório")
        String message
) {
}
