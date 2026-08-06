package com.sgl.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Converte exceções lançadas pela aplicação em respostas HTTP padronizadas.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    /**
     * Trata recursos que não existem.
     */
    @ExceptionHandler({
            ResourceNotFoundException.class,
            EntityNotFoundException.class
    })
    public ResponseEntity<ApiError> handleNotFound(
            RuntimeException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Recurso não encontrado",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * Trata violações conhecidas das regras de negócio.
     */
    @ExceptionHandler({
            BusinessRuleException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiError> handleBusinessRule(
            RuntimeException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Regra de negócio violada",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * Trata falhas das anotações de validação dos DTOs.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<FieldValidationError> fieldErrors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(this::convertFieldError)
                        .toList();

        ApiError error = new ApiError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação",
                "Um ou mais campos estão inválidos.",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    /**
     * Trata JSON inválido, enum inexistente ou valor incompatível
     * com o tipo esperado pelo DTO.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido",
                "O JSON enviado está malformado ou contém valores inválidos.",
                request.getRequestURI()
        );
    }

    /**
     * Trata parâmetros obrigatórios que não foram enviados.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        String message =
                "O parâmetro '" + ex.getParameterName()
                        + "' é obrigatório.";

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Parâmetro obrigatório ausente",
                message,
                request.getRequestURI()
        );
    }

    /**
     * Trata parâmetros enviados com tipo ou enum inválido.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String message =
                "O valor '" + ex.getValue()
                        + "' é inválido para o parâmetro '"
                        + ex.getName() + "'.";

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Parâmetro inválido",
                message,
                request.getRequestURI()
        );
    }

    /**
     * Trata violações de restrições do banco, como campos únicos
     * ou relacionamentos obrigatórios.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "Conflito de dados",
                "A operação viola uma restrição de integridade dos dados.",
                request.getRequestURI()
        );
    }

    /**
     * Impede que detalhes internos e stack traces sejam enviados
     * ao cliente em erros inesperados.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor",
                "Ocorreu um erro inesperado ao processar a solicitação.",
                request.getRequestURI()
        );
    }

    private FieldValidationError convertFieldError(
            FieldError fieldError) {

        return new FieldValidationError(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String error,
            String message,
            String path) {

        ApiError apiError = new ApiError(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                path
        );

        return ResponseEntity
                .status(status)
                .body(apiError);
    }
}