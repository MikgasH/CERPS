package org.example.analyticsservice.unit.exception;

import org.example.analyticsservice.exception.InsufficientDataException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InsufficientDataExceptionTest {

    @Test
    void constructor_WithMessage_ShouldCreateException() {
        String message = "Not enough data";

        InsufficientDataException ex = new InsufficientDataException(message);

        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
