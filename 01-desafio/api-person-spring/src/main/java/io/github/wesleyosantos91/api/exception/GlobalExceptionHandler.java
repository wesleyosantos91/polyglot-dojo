package io.github.wesleyosantos91.api.exception;

import io.github.wesleyosantos91.domain.exception.BusinessException;
import io.github.wesleyosantos91.domain.exception.ResourceNotFoundException;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.resilience.InvocationRejectedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectProvider<Tracer> tracerProvider;

    public GlobalExceptionHandler(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @ExceptionHandler(InvocationRejectedException.class)
    public ProblemDetail handleInvocationRejected(InvocationRejectedException ex, HttpServletRequest request) {
        log.atWarn()
                .setMessage("invocation_rejected")
                .setCause(ex)
                .addKeyValue("error_code", "RESOURCE_BUSY")
                .addKeyValue("path", request.getRequestURI())
                .log();

        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Resource temporarily unavailable",
                "Banco temporariamente sobrecarregado. Tente novamente.",
                "urn:problem-type:resource-busy",
                "RESOURCE_BUSY",
                request
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        // INFO sem stack trace — 404 é erro esperado de cliente, não requer investigação
        log.atInfo()
                .setMessage("resource_not_found")
                .addKeyValue("error_code", "RESOURCE_NOT_FOUND")
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("detail", ex.getMessage())
                .log();

        return problem(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                ex.getMessage(),
                "urn:problem-type:resource-not-found",
                "RESOURCE_NOT_FOUND",
                request
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        // WARN sem stack trace — violação de regra de negócio é esperada, não um bug
        log.atWarn()
                .setMessage("business_error")
                .addKeyValue("error_code", ex.getErrorCode())
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("detail", ex.getMessage())
                .log();

        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Business rule violation",
                ex.getMessage(),
                "urn:problem-type:business-rule-violation",
                ex.getErrorCode(),
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.atWarn()
                .setMessage("data_integrity_violation")
                .setCause(ex)
                .addKeyValue("error_code", "DATA_INTEGRITY_VIOLATION")
                .addKeyValue("path", request.getRequestURI())
                .log();

        return problem(
                HttpStatus.CONFLICT,
                "Data conflict",
                "The request conflicts with current data state.",
                "urn:problem-type:data-conflict",
                "DATA_INTEGRITY_VIOLATION",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.atWarn()
                .setMessage("request_validation_failed")
                .setCause(ex)
                .addKeyValue("error_code", "VALIDATION_ERROR")
                .addKeyValue("path", request.getRequestURI())
                .log();

        ProblemDetail pd = problem(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "A requisição contém campos inválidos.",
                "urn:problem-type:validation-error",
                "VALIDATION_ERROR",
                request
        );

        List<Object> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toViolation)
                .collect(Collectors.toList());

        pd.setProperty("violations", violations);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        log.atWarn()
                .setMessage("constraint_violation")
                .setCause(ex)
                .addKeyValue("error_code", "CONSTRAINT_VIOLATION")
                .addKeyValue("path", request.getRequestURI())
                .log();

        ProblemDetail pd = problem(
                HttpStatus.BAD_REQUEST,
                "Constraint violation",
                "One or more request constraints were violated.",
                "urn:problem-type:constraint-violation",
                "CONSTRAINT_VIOLATION",
                request
        );

        List<Object> violations = ex.getConstraintViolations().stream()
                .map(v -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("field", String.valueOf(v.getPropertyPath()));
                    m.put("message", v.getMessage());
                    return m;
                })
                .collect(Collectors.toList());

        pd.setProperty("violations", violations);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.atWarn()
                .setMessage("malformed_json_request")
                .setCause(ex)
                .addKeyValue("error_code", "MALFORMED_JSON")
                .addKeyValue("path", request.getRequestURI())
                .log();

        return problem(
                HttpStatus.BAD_REQUEST,
                "Malformed request body",
                "Request body is invalid or malformed JSON.",
                "urn:problem-type:malformed-json",
                "MALFORMED_JSON",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.atWarn()
                .setMessage("argument_type_mismatch")
                .setCause(ex)
                .addKeyValue("error_code", "ARGUMENT_TYPE_MISMATCH")
                .addKeyValue("parameter", ex.getName())
                .addKeyValue("path", request.getRequestURI())
                .log();

        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid parameter type",
                "Parameter '" + ex.getName() + "' has an invalid value.",
                "urn:problem-type:argument-type-mismatch",
                "ARGUMENT_TYPE_MISMATCH",
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.atWarn()
                .setMessage("invalid_request_argument")
                .setCause(ex)
                .addKeyValue("error_code", "INVALID_REQUEST")
                .addKeyValue("path", request.getRequestURI())
                .log();

        return problem(
                HttpStatus.BAD_REQUEST,
                "Invalid request",
                ex.getMessage() != null ? ex.getMessage() : "Requisição inválida.",
                "urn:problem-type:invalid-request",
                "INVALID_REQUEST",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        log.atError()
                .setMessage("unhandled_exception")
                .setCause(ex)
                .addKeyValue("error_code", "INTERNAL_ERROR")
                .addKeyValue("path", request.getRequestURI())
                .log();

        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Ocorreu um erro inesperado.",
                "urn:problem-type:internal-error",
                "INTERNAL_ERROR",
                request
        );
    }

    private ProblemDetail problem(
            HttpStatus status,
            String title,
            String detail,
            String typeUri,
            String errorCode,
            HttpServletRequest request
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create(typeUri));

        pd.setInstance(URI.create(request.getRequestURI()));

        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        pd.setProperty("error_code", errorCode);

        String correlationId = MDC.get("correlation_id");
        if (correlationId != null) {
            pd.setProperty("correlation_id", correlationId);
        }

        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer != null) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null && currentSpan.context() != null) {
                pd.setProperty("trace_id", currentSpan.context().traceId());
                pd.setProperty("span_id", currentSpan.context().spanId());
            }
        }

        return pd;
    }

    private Object toViolation(FieldError fe) {
        var map = new LinkedHashMap<String, Object>();
        map.put("field", fe.getField());
        map.put("code", fe.getCode());
        map.put("message", fe.getDefaultMessage());

        Object rejected = fe.getRejectedValue();
        String field = fe.getField() == null ? "" : fe.getField().toLowerCase();
        if (rejected != null && !field.contains("email") && !field.contains("token") && !field.contains("password")) {
            map.put("rejected_value", rejected);
        }

        return map;
    }
}
