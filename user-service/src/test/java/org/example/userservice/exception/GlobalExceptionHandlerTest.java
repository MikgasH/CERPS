package org.example.userservice.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindingResult bindingResult;

    @Test
    void should_ReturnConflict_When_UserAlreadyExists() {
        UserAlreadyExistsException exception = new UserAlreadyExistsException("Email 'test@example.com' is already registered");

        ProblemDetail result = globalExceptionHandler.handleUserAlreadyExistsException(exception);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(result.getTitle()).isEqualTo("User already exists");
        assertThat(result.getDetail()).isEqualTo("Email 'test@example.com' is already registered");
    }

    @Test
    void should_ReturnUnauthorized_When_BadCredentials() {
        BadCredentialsException exception = new BadCredentialsException("Invalid credentials");

        ProblemDetail result = globalExceptionHandler.handleBadCredentialsException(exception);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(result.getTitle()).isEqualTo("Authentication failed");
        assertThat(result.getDetail()).isEqualTo("Invalid username or password");
    }

    @Test
    void should_ReturnUnauthorized_When_UserDisabled() {
        DisabledException exception = new DisabledException("User account is disabled");

        ProblemDetail result = globalExceptionHandler.handleDisabledException(exception);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(result.getTitle()).isEqualTo("Account disabled");
        assertThat(result.getDetail()).isEqualTo("User account is disabled");
    }

    @Test
    void should_ReturnUnauthorized_When_UsernameNotFound() {
        UsernameNotFoundException exception = new UsernameNotFoundException("User not found");

        ProblemDetail result = globalExceptionHandler.handleUsernameNotFoundException(exception);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(result.getTitle()).isEqualTo("Authentication failed");
        assertThat(result.getDetail()).isEqualTo("Invalid username or password");
    }

    @Test
    void should_ReturnBadRequest_When_MethodArgumentNotValid() {
        FieldError fieldError = new FieldError("registerRequest", "email", "must be a valid email");

        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ProblemDetail result = globalExceptionHandler.handleMethodArgumentNotValidException(methodArgumentNotValidException);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Validation error");
        assertThat(result.getDetail()).isEqualTo("email: must be a valid email");
    }

    @Test
    void should_ReturnDefaultMessage_When_NoFieldErrors() {
        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ProblemDetail result = globalExceptionHandler.handleMethodArgumentNotValidException(methodArgumentNotValidException);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Validation error");
        assertThat(result.getDetail()).isEqualTo("Validation failed");
    }

    @Test
    void should_ReturnBadRequest_When_IllegalArgument() {
        IllegalArgumentException exception = new IllegalArgumentException("New password must be different");

        ProblemDetail result = globalExceptionHandler.handleIllegalArgumentException(exception);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Invalid argument");
        assertThat(result.getDetail()).isEqualTo("New password must be different");
    }

    @Test
    void should_ReturnInternalServerError_When_GenericException() {
        Exception exception = new Exception("Unexpected error occurred");

        ProblemDetail result = globalExceptionHandler.handleGenericException(exception);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo("Internal server error");
        assertThat(result.getDetail()).isEqualTo("An unexpected error occurred: Unexpected error occurred");
    }

    @Test
    void should_ReturnBadRequest_When_MultipleFieldErrors() {
        FieldError fieldError1 = new FieldError("registerRequest", "email", "must be a valid email");
        FieldError fieldError2 = new FieldError("registerRequest", "password", "must not be blank");

        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        ProblemDetail result = globalExceptionHandler.handleMethodArgumentNotValidException(methodArgumentNotValidException);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getDetail()).isEqualTo("email: must be a valid email");
    }

    @Test
    void should_ReturnInternalServerError_When_NullPointerException() {
        Exception exception = new NullPointerException("Null value encountered");

        ProblemDetail result = globalExceptionHandler.handleGenericException(exception);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo("Internal server error");
        assertThat(result.getDetail()).contains("Null value encountered");
    }
}
