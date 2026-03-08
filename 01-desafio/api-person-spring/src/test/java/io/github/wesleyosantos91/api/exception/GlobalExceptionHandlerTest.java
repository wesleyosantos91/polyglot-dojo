package io.github.wesleyosantos91.api.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.wesleyosantos91.domain.exception.ConflictException;
import io.github.wesleyosantos91.domain.exception.ResourceNotFoundException;
import io.github.wesleyosantos91.infrastructure.metrics.PersonMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.resilience.InvocationRejectedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler — Unit Tests")
class GlobalExceptionHandlerTest {

    @Mock
    ObjectProvider<io.micrometer.tracing.Tracer> tracerProvider;

    @Mock
    PersonMetrics personMetrics;

    @Mock
    HttpServletRequest request;

    @InjectMocks
    GlobalExceptionHandler handler;

    @BeforeEach
    void setup() {
        when(request.getRequestURI()).thenReturn("/api/test");
        when(tracerProvider.getIfAvailable()).thenReturn(null);
    }

    @Test
    @DisplayName("handleNotFound — returns 404 with RESOURCE_NOT_FOUND error code")
    void handleNotFound_returns404() {
        var ex = new ResourceNotFoundException("Person", UUID.randomUUID());
        var pd = handler.handleNotFound(ex, request);
        assertThat(pd.getStatus()).isEqualTo(404);
        assertThat(pd.getProperties().get("error_code")).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("handleBusiness — returns 422 with the exception's error code")
    void handleBusiness_returns422() {
        var ex = new ConflictException("PERSON_EMAIL_ALREADY_EXISTS", "email taken");
        var pd = handler.handleBusiness(ex, request);
        assertThat(pd.getStatus()).isEqualTo(422);
        assertThat(pd.getProperties().get("error_code")).isEqualTo("PERSON_EMAIL_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("handleDataIntegrity — returns 409 with DATA_INTEGRITY_VIOLATION code")
    void handleDataIntegrity_returns409() {
        var ex = new DataIntegrityViolationException("unique constraint violated");
        var pd = handler.handleDataIntegrity(ex, request);
        assertThat(pd.getStatus()).isEqualTo(409);
        assertThat(pd.getProperties().get("error_code")).isEqualTo("DATA_INTEGRITY_VIOLATION");
    }

    @Test
    @DisplayName("handleGeneric — returns 500 with INTERNAL_ERROR code")
    void handleGeneric_returns500() {
        var ex = new RuntimeException("unexpected failure");
        var pd = handler.handleGeneric(ex, request);
        assertThat(pd.getStatus()).isEqualTo(500);
        assertThat(pd.getProperties().get("error_code")).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    @DisplayName("handleIllegalArgument — returns 400 with the exception message")
    void handleIllegalArgument_returns400WithMessage() {
        var ex = new IllegalArgumentException("bad input value");
        var pd = handler.handleIllegalArgument(ex, request);
        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getProperties().get("error_code")).isEqualTo("INVALID_REQUEST");
        assertThat(pd.getDetail()).isEqualTo("bad input value");
    }

    @Test
    @DisplayName("handleIllegalArgument — uses default message when exception message is null")
    void handleIllegalArgument_usesDefaultWhenMessageNull() {
        var ex = new IllegalArgumentException((String) null);
        var pd = handler.handleIllegalArgument(ex, request);
        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getDetail()).isNotNull();
    }

    @Test
    @DisplayName("handleNotReadable — returns 400 with MALFORMED_JSON code")
    void handleNotReadable_returns400() {
        var ex = mock(HttpMessageNotReadableException.class);
        var pd = handler.handleNotReadable(ex, request);
        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getProperties().get("error_code")).isEqualTo("MALFORMED_JSON");
    }

    @Test
    @DisplayName("handleTypeMismatch — returns 400 with ARGUMENT_TYPE_MISMATCH code")
    void handleTypeMismatch_returns400() {
        var ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        var pd = handler.handleTypeMismatch(ex, request);
        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getProperties().get("error_code")).isEqualTo("ARGUMENT_TYPE_MISMATCH");
    }

    @Test
    @DisplayName("handleInvocationRejected — returns 503 with RESOURCE_BUSY code")
    void handleInvocationRejected_returns503() {
        var ex = mock(InvocationRejectedException.class);
        var pd = handler.handleInvocationRejected(ex, request);
        assertThat(pd.getStatus()).isEqualTo(503);
        assertThat(pd.getProperties().get("error_code")).isEqualTo("RESOURCE_BUSY");
    }

    @Test
    @DisplayName("handleConstraintViolation — returns 400 with CONSTRAINT_VIOLATION code")
    void handleConstraintViolation_returns400() {
        var ex = new ConstraintViolationException("constraint violated", Set.of());
        var pd = handler.handleConstraintViolation(ex, request);
        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getProperties().get("error_code")).isEqualTo("CONSTRAINT_VIOLATION");
    }

    @Test
    @DisplayName("handleValidation — includes rejected_value for non-sensitive fields")
    void handleValidation_includesRejectedValueForNonSensitiveField() {
        var bindingResult = mock(BindingResult.class);
        var fieldError = new FieldError("personRequest", "name", "x", false,
                new String[]{"Size"}, null, "too short");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        var pd = handler.handleValidation(ex, request);

        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getProperties().get("error_code")).isEqualTo("VALIDATION_ERROR");
        @SuppressWarnings("unchecked")
        var violations = (List<Map<String, Object>>) pd.getProperties().get("violations");
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0)).containsKey("rejected_value");
    }

    @Test
    @DisplayName("handleValidation — masks rejected_value for email fields")
    void handleValidation_masksRejectedValueForEmailField() {
        var bindingResult = mock(BindingResult.class);
        var fieldError = new FieldError("personRequest", "email", "bad@email", false,
                new String[]{"Email"}, null, "invalid email format");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        var pd = handler.handleValidation(ex, request);

        @SuppressWarnings("unchecked")
        var violations = (List<Map<String, Object>>) pd.getProperties().get("violations");
        assertThat(violations.get(0)).doesNotContainKey("rejected_value");
    }

    @Test
    @DisplayName("problem — includes correlation_id property when MDC has it set")
    void problem_includesCorrelationIdFromMdc() {
        MDC.put("correlation_id", "abc-123");
        try {
            var pd = handler.handleNotFound(
                    new ResourceNotFoundException("Person", "some-id"), request);
            assertThat(pd.getProperties().get("correlation_id")).isEqualTo("abc-123");
        } finally {
            MDC.remove("correlation_id");
        }
    }

    @Test
    @DisplayName("problem — includes trace_id and span_id when tracer has an active span")
    void problem_includesTraceAndSpanIds_whenTracerHasActiveSpan() {
        var tracer = mock(io.micrometer.tracing.Tracer.class);
        var span = mock(io.micrometer.tracing.Span.class);
        var context = mock(io.micrometer.tracing.TraceContext.class);

        when(tracerProvider.getIfAvailable()).thenReturn(tracer);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("trace-abc");
        when(context.spanId()).thenReturn("span-def");

        var pd = handler.handleNotFound(new ResourceNotFoundException("Person", "id"), request);

        assertThat(pd.getProperties().get("trace_id")).isEqualTo("trace-abc");
        assertThat(pd.getProperties().get("span_id")).isEqualTo("span-def");
    }

    @Test
    @DisplayName("problem — omits trace_id when tracer has no active span")
    void problem_omitsTraceIds_whenCurrentSpanIsNull() {
        var tracer = mock(io.micrometer.tracing.Tracer.class);
        when(tracerProvider.getIfAvailable()).thenReturn(tracer);
        when(tracer.currentSpan()).thenReturn(null);

        var pd = handler.handleNotFound(new ResourceNotFoundException("Person", "id"), request);

        assertThat(pd.getProperties()).doesNotContainKey("trace_id");
    }

    @Test
    @DisplayName("handleValidation — omits rejected_value when rejected value is null")
    void handleValidation_omitsRejectedValue_whenNull() {
        var bindingResult = mock(BindingResult.class);
        var fieldError = new FieldError("personRequest", "name", null, false,
                new String[]{"NotBlank"}, null, "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        var pd = handler.handleValidation(ex, request);

        @SuppressWarnings("unchecked")
        var violations = (List<Map<String, Object>>) pd.getProperties().get("violations");
        assertThat(violations.get(0)).doesNotContainKey("rejected_value");
    }

    @Test
    @DisplayName("handleValidation — masks rejected_value for token fields")
    void handleValidation_masksRejectedValueForTokenField() {
        var bindingResult = mock(BindingResult.class);
        var fieldError = new FieldError("req", "token", "secret-token", false,
                new String[]{"NotBlank"}, null, "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        var pd = handler.handleValidation(ex, request);

        @SuppressWarnings("unchecked")
        var violations = (List<Map<String, Object>>) pd.getProperties().get("violations");
        assertThat(violations.get(0)).doesNotContainKey("rejected_value");
    }

    @Test
    @DisplayName("handleValidation — masks rejected_value for password fields")
    void handleValidation_masksRejectedValueForPasswordField() {
        var bindingResult = mock(BindingResult.class);
        var fieldError = new FieldError("req", "password", "secret123", false,
                new String[]{"Size"}, null, "too short");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        var pd = handler.handleValidation(ex, request);

        @SuppressWarnings("unchecked")
        var violations = (List<Map<String, Object>>) pd.getProperties().get("violations");
        assertThat(violations.get(0)).doesNotContainKey("rejected_value");
    }

    @Test
    @DisplayName("handleConstraintViolation — maps each violation with field and message")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void handleConstraintViolation_mapsViolationDetails() {
        jakarta.validation.ConstraintViolation violation = mock(jakarta.validation.ConstraintViolation.class);
        var path = mock(jakarta.validation.Path.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be null");

        Set<jakarta.validation.ConstraintViolation<?>> violationSet = new java.util.HashSet<>();
        violationSet.add(violation);
        var ex = new ConstraintViolationException("violations", violationSet);
        var pd = handler.handleConstraintViolation(ex, request);

        assertThat(pd.getStatus()).isEqualTo(400);
        var result = (List<Map<String, Object>>) pd.getProperties().get("violations");
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsKey("field").containsKey("message");
    }

    @Test
    @DisplayName("problem — omits trace_id when tracer span context is null")
    void problem_omitsTraceIds_whenSpanContextIsNull() {
        var tracer = mock(io.micrometer.tracing.Tracer.class);
        var span = mock(io.micrometer.tracing.Span.class);

        when(tracerProvider.getIfAvailable()).thenReturn(tracer);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(null);

        var pd = handler.handleNotFound(new ResourceNotFoundException("Person", "id"), request);

        assertThat(pd.getProperties()).doesNotContainKey("trace_id");
    }
}
