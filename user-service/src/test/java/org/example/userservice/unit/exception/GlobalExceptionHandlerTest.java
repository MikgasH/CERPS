package org.example.userservice.unit.exception;

import org.example.userservice.exception.GlobalExceptionHandler;
import org.example.userservice.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should handle UserAlreadyExistsException with 409 CONFLICT")
    void handleUserAlreadyExistsException_ShouldReturn409() {
        UserAlreadyExistsException exception =
                new UserAlreadyExistsException("Email 'test@example.com' is already registered");

        ProblemDetail problemDetail = exceptionHandler.handleUserAlreadyExistsException(exception);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getTitle()).isEqualTo("User already exists");
        assertThat(problemDetail.getDetail()).contains("test@example.com");
    }

    @Test
    @DisplayName("Should handle BadCredentialsException with 401 UNAUTHORIZED")
    void handleBadCredentialsException_ShouldReturn401() {
        BadCredentialsException exception = new BadCredentialsException("Invalid credentials");

        ProblemDetail problemDetail = exceptionHandler.handleBadCredentialsException(exception);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Authentication failed");
        assertThat(problemDetail.getDetail()).isEqualTo("Invalid username or password");
    }

    @Test
    @DisplayName("Should handle DisabledException with 401 UNAUTHORIZED")
    void handleDisabledException_ShouldReturn401() {
        DisabledException exception = new DisabledException("Account is disabled");

        ProblemDetail problemDetail = exceptionHandler.handleDisabledException(exception);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Account disabled");
        assertThat(problemDetail.getDetail()).isEqualTo("User account is disabled");
    }

    @Test
    @DisplayName("Should handle UsernameNotFoundException with 401 UNAUTHORIZED")
    void handleUsernameNotFoundException_ShouldReturn401() {
        UsernameNotFoundException exception =
                new UsernameNotFoundException("User not found: test@example.com");

        ProblemDetail problemDetail = exceptionHandler.handleUsernameNotFoundException(exception);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Authentication failed");
        assertThat(problemDetail.getDetail()).isEqualTo("Invalid username or password");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with 400 BAD_REQUEST")
    void handleMethodArgumentNotValidException_ShouldReturn400() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("registerRequest", "email", "Invalid email format");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ProblemDetail problemDetail = exceptionHandler.handleMethodArgumentNotValidException(exception);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Validation error");
        assertThat(problemDetail.getDetail()).contains("email");
        assertThat(problemDetail.getDetail()).contains("Invalid email format");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with multiple errors")
    void handleMethodArgumentNotValidException_WithMultipleErrors_ShouldReturnFirstError() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("registerRequest", "email", "Invalid email format");
        FieldError error2 = new FieldError("registerRequest", "password", "Password too weak");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        ProblemDetail problemDetail = exceptionHandler.handleMethodArgumentNotValidException(exception);

        assertThat(problemDetail.getDetail()).contains("email");
        assertThat(problemDetail.getDetail()).doesNotContain("password");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with no field errors")
    void handleMethodArgumentNotValidException_WithNoFieldErrors_ShouldReturnGenericMessage() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ProblemDetail problemDetail = exceptionHandler.handleMethodArgumentNotValidException(exception);

        assertThat(problemDetail.getDetail()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with 400 BAD_REQUEST")
    void handleIllegalArgumentException_ShouldReturn400() {
        IllegalArgumentException exception =
                new IllegalArgumentException("New password must be different from current password");

        ProblemDetail problemDetail = exceptionHandler.handleIllegalArgumentException(exception);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Invalid argument");
        assertThat(problemDetail.getDetail()).contains("New password must be different");
    }

    @Test
    @DisplayName("Should handle generic Exception with 500 INTERNAL_SERVER_ERROR")
    void handleGenericException_ShouldReturn500() {
        Exception exception = new Exception("Unexpected database error");

        ProblemDetail problemDetail = exceptionHandler.handleGenericException(exception);

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getTitle()).isEqualTo("Internal server error");
        assertThat(problemDetail.getDetail()).contains("An unexpected error occurred");
        assertThat(problemDetail.getDetail()).contains("Unexpected database error");
    }

    @Test
    @DisplayName("Should create ProblemDetail with correct structure")
    void exceptionHandlers_ShouldReturnCorrectProblemDetailStructure() {
        BadCredentialsException exception = new BadCredentialsException("Test");

        ProblemDetail problemDetail = exceptionHandler.handleBadCredentialsException(exception);

        assertThat(problemDetail.getStatus()).isNotNull();
        assertThat(problemDetail.getTitle()).isNotNull();
        assertThat(problemDetail.getDetail()).isNotNull();
    }
}
