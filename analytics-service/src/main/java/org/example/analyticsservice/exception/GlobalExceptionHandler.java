package org.example.analyticsservice.exception;

import com.example.cerps.common.exception.BaseGlobalExceptionHandler;
import com.example.cerps.common.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(InsufficientDataException.class)
    public ProblemDetail handleInsufficientDataException(final InsufficientDataException ex) {
        log.error("Insufficient data: {}", ex.getMessage());

        final ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.NOT_FOUND,
                "Insufficient Data",
                ex.getMessage(),
                "insufficient-data"
        );
        problemDetail.setProperty("suggestion",
                "Try a wider date range or retry later once more rate history is available");

        return problemDetail;
    }

    @ExceptionHandler(CurrencyNotSupportedException.class)
    public ProblemDetail handleCurrencyNotSupportedException(final CurrencyNotSupportedException ex) {
        log.error("Currency not supported: {}", ex.getMessage());

        final ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Currency Not Supported",
                ex.getMessage(),
                "currency-not-supported"
        );
        problemDetail.setProperty("invalidCurrency", ex.getInvalidCurrency());

        return problemDetail;
    }

    @ExceptionHandler(MinimumPeriodNotSupportedException.class)
    public ProblemDetail handleMinimumPeriodNotSupportedException(final MinimumPeriodNotSupportedException ex) {
        log.error("Minimum period not supported: {}", ex.getMessage());

        final ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Minimum Period Not Supported",
                ex.getMessage(),
                "minimum-period-not-supported"
        );
        problemDetail.setProperty("minimumPeriod", ex.getMinimumPeriod());

        return problemDetail;
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ProblemDetail handleExternalServiceException(final ExternalServiceException ex) {
        log.error("External service call failed: {}", ex.getMessage(), ex);

        final ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                "Upstream currency-service is currently unavailable",
                "external-service-unavailable"
        );
        problemDetail.setProperty("upstream", "currency-service");
        problemDetail.setProperty("diagnostic", ex.getMessage());
        problemDetail.setProperty("suggestion", "Retry the request after a short delay");

        return problemDetail;
    }
}
