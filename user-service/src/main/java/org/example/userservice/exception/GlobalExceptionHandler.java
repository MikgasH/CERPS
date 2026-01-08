package org.example.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleUserAlreadyExistsException(final UserAlreadyExistsException ex) {
        return createProblemDetail(HttpStatus.CONFLICT, "User already exists", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleBadCredentialsException(final BadCredentialsException ex) {
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Authentication failed", "Invalid username or password");
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleDisabledException(final DisabledException ex) {
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Account disabled", "User account is disabled");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleUsernameNotFoundException(final UsernameNotFoundException ex) {
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Authentication failed", "Invalid username or password");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMethodArgumentNotValidException(final MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        return createProblemDetail(HttpStatus.BAD_REQUEST, "Validation error", message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleIllegalArgumentException(final IllegalArgumentException ex) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid argument", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGenericException(final Exception ex) {
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred: " + ex.getMessage());
    }

    private ProblemDetail createProblemDetail(final HttpStatus status, final String title, final String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status.value());
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        return problemDetail;
    }
}
