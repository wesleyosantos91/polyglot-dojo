package io.github.wesleyosantos91.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Exception classes — Unit Tests")
class ExceptionClassesTest {

    @Test
    @DisplayName("BusinessException single-arg constructor uses default error code BUSINESS_ERROR")
    void businessException_singleArg_usesDefaultCode() {
        var ex = new BusinessException("something went wrong");
        assertThat(ex.getErrorCode()).isEqualTo("BUSINESS_ERROR");
        assertThat(ex.getMessage()).isEqualTo("something went wrong");
    }

    @Test
    @DisplayName("BusinessException two-arg constructor uses provided error code")
    void businessException_twoArg_usesProvidedCode() {
        var ex = new BusinessException("MY_CODE", "something went wrong");
        assertThat(ex.getErrorCode()).isEqualTo("MY_CODE");
        assertThat(ex.getMessage()).isEqualTo("something went wrong");
    }

    @Test
    @DisplayName("ConflictException single-arg constructor defaults to CONFLICT error code")
    void conflictException_singleArg_usesConflictCode() {
        var ex = new ConflictException("email already exists");
        assertThat(ex.getErrorCode()).isEqualTo("CONFLICT");
        assertThat(ex.getMessage()).isEqualTo("email already exists");
    }

    @Test
    @DisplayName("ConflictException two-arg constructor uses provided error code")
    void conflictException_twoArg_usesProvidedCode() {
        var ex = new ConflictException("PERSON_EMAIL_ALREADY_EXISTS", "email taken");
        assertThat(ex.getErrorCode()).isEqualTo("PERSON_EMAIL_ALREADY_EXISTS");
        assertThat(ex.getMessage()).isEqualTo("email taken");
    }

    @Test
    @DisplayName("ResourceNotFoundException single-arg constructor uses RESOURCE_NOT_FOUND code")
    void resourceNotFoundException_singleArg() {
        var ex = new ResourceNotFoundException("custom not found message");
        assertThat(ex.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(ex.getMessage()).isEqualTo("custom not found message");
    }

    @Test
    @DisplayName("ResourceNotFoundException two-arg constructor formats message with resource name and identifier")
    void resourceNotFoundException_twoArg_formatsMessage() {
        var ex = new ResourceNotFoundException("Person", "abc-123");
        assertThat(ex.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(ex.getMessage()).contains("Person").contains("abc-123");
    }
}
