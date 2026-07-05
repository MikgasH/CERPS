package com.example.cerps.common.exception;

import com.example.cerps.common.CerpsConstants;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseGlobalExceptionHandlerTest {

    private final BaseGlobalExceptionHandler handler = new BaseGlobalExceptionHandler() { };

    @Test
    void handleMethodArgumentNotValidException_ShouldReturnBadRequestWithErrorsMap() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "must not be null");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ProblemDetail response = handler.handleMethodArgumentNotValidException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Validation Error");
        assertThat(response.getDetail()).isEqualTo("Validation failed for request parameters");
        assertThat(response.getProperties().get("errors"))
                .isEqualTo(Map.of("field", "must not be null"));
        assertRfc7807Shape(response, "validation");
    }

    @Test
    void handleConstraintViolationException_ShouldReturnBadRequestWithFirstViolation() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation.getMessage()).thenReturn("must not be null");
        when(violation.getPropertyPath().toString()).thenReturn("field");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ProblemDetail response = handler.handleConstraintViolationException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Validation Error");
        assertThat(response.getDetail()).contains("must not be null");
        assertRfc7807Shape(response, "validation");
    }

    @Test
    void handleMissingServletRequestParameterException_ShouldReturnBadRequest() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("amount", "BigDecimal");

        ProblemDetail response = handler.handleMissingServletRequestParameterException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Missing required parameter");
        assertThat(response.getDetail()).contains("amount");
        assertRfc7807Shape(response, "missing-parameter");
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input");

        ProblemDetail response = handler.handleIllegalArgumentException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getTitle()).isEqualTo("Invalid Request");
        assertThat(response.getDetail()).isEqualTo("Invalid input");
        assertRfc7807Shape(response, "invalid-request");
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        Exception ex = new RuntimeException("Server error");

        ProblemDetail response = handler.handleGenericException(ex);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getTitle()).isEqualTo("Internal Server Error");
        assertThat(response.getDetail()).isEqualTo("An unexpected error occurred");
        assertRfc7807Shape(response, "internal");
    }

    @Test
    void createProblemDetail_ShouldSetTitleTypeAndTimestamp() {
        ProblemDetail response = handler.createProblemDetail(
                HttpStatus.NOT_FOUND, "Some Title", "some detail", "some-slug");

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getTitle()).isEqualTo("Some Title");
        assertThat(response.getDetail()).isEqualTo("some detail");
        assertRfc7807Shape(response, "some-slug");
    }

    private void assertRfc7807Shape(final ProblemDetail response, final String errorType) {
        assertThat(response.getType())
                .hasToString(CerpsConstants.ERROR_URI_PREFIX + errorType);
        assertThat(response.getProperties()).containsKey("timestamp");
    }
}
