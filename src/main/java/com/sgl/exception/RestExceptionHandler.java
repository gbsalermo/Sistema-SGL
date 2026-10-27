package com.sgl.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.sgl.corporate.CorporateApiUnavailableException;
import com.sgl.corporate.CorporatePersonNotFoundException;
import com.sgl.corporate.InvalidCorporateCredentialsException;
import com.sgl.security.SecurityErrorWriter;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class RestExceptionHandler {

    public static final String PESSOA_NAO_ENCONTRADA = "PESSOA_NAO_ENCONTRADA";
    public static final String AUTENTICACAO_INDISPONIVEL = "AUTENTICACAO_INDISPONIVEL";

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

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

    // Unknown routes (e.g. /auth/dev-login outside dev/demo, where its controller does not exist)
    // would otherwise fall into the generic handler and surface as a 500.
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiError> handleNoRoute(
            Exception ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Recurso não encontrado",
                "O endereço solicitado não existe.",
                request.getRequestURI()
        );
    }

    // Without this, an AccessDeniedException raised past the filters (e.g. by CurrentUser) would fall
    // into the generic handler below and surface as a 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        ApiError apiError = new ApiError(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Acesso negado",
                ex.getMessage(),
                request.getRequestURI(),
                null,
                SecurityErrorWriter.ACESSO_NEGADO
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(apiError);
    }

    @ExceptionHandler(LoginRejectedException.class)
    public ResponseEntity<ApiError> handleLoginRejected(
            LoginRejectedException ex,
            HttpServletRequest request) {

        return buildResponse(ex.getStatus(), "Login recusado", ex.getMessage(), request.getRequestURI(), ex.getCodigo());
    }

    // The corporate exceptions carry technical messages; the user gets a fixed, friendly one instead.
    @ExceptionHandler(InvalidCorporateCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCorporateCredentials(
            InvalidCorporateCredentialsException ex,
            HttpServletRequest request) {

        LoginRejectedException rejected = LoginRejectedException.credenciaisInvalidas();
        return handleLoginRejected(rejected, request);
    }

    @ExceptionHandler(CorporatePersonNotFoundException.class)
    public ResponseEntity<ApiError> handleCorporatePersonNotFound(
            CorporatePersonNotFoundException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.FORBIDDEN, "Login recusado",
                "Seu cadastro não foi encontrado nas bases corporativas da Embrapa.",
                request.getRequestURI(), PESSOA_NAO_ENCONTRADA);
    }

    @ExceptionHandler(CorporateApiUnavailableException.class)
    public ResponseEntity<ApiError> handleCorporateApiUnavailable(
            CorporateApiUnavailableException ex,
            HttpServletRequest request) {

        // Only the cause type and HTTP status: the message and URL may carry a CPF path segment,
        // and the request body may hold a password.
        Throwable cause = ex.getCause();
        String status = cause instanceof RestClientResponseException response
                ? String.valueOf(response.getStatusCode().value())
                : "n/a";
        log.warn("Corporate gateway unavailable: cause={} status={}",
                cause == null ? "none" : cause.getClass().getSimpleName(), status);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Serviço indisponível",
                "Autenticação corporativa indisponível. Tente novamente em instantes.",
                request.getRequestURI(), AUTENTICACAO_INDISPONIVEL);
    }

    @ExceptionHandler(StockConflictException.class)
    public ResponseEntity<ApiError> handleStockConflict(
            StockConflictException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.CONFLICT,
                "Conflito de estoque",
                ex.getMessage(),
                request.getRequestURI()
        );
    }
    
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

    private FieldValidationError convertFieldError(FieldError fieldError) {
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

        return buildResponse(status, error, message, path, null);
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String error,
            String message,
            String path,
            String codigo) {

        ApiError apiError = new ApiError(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                path,
                null,
                codigo
        );

        return ResponseEntity
                .status(status)
                .body(apiError);
    }
}
