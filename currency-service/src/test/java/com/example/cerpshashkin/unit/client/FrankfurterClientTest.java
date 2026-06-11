package com.example.cerpshashkin.unit.client;

import com.example.cerpshashkin.client.impl.FrankfurterClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FrankfurterClientTest {

    @Test
    void getProviderName_ShouldReturnCorrectName() {
        FrankfurterClient client = new FrankfurterClient(null, null, null);

        assertThat(client.getProviderName()).isEqualTo("Frankfurter");
    }

    @Test
    void isFallback_ShouldReturnTrue() {
        FrankfurterClient client = new FrankfurterClient(null, null, null);

        assertThat(client.isFallback()).isTrue();
    }

    @Test
    void isStale_ShouldReturnFalse_WhenResponseDateIsToday() {
        LocalDate today = LocalDate.of(2026, 6, 11);

        assertThat(FrankfurterClient.isStale(today, today)).isFalse();
    }

    @Test
    void isStale_ShouldReturnFalse_WhenResponseDateIsExactlyFourBusinessDaysOld() {
        // Fri 2026-06-05 -> Thu 2026-06-11: Mon, Tue, Wed, Thu = 4 business days
        LocalDate responseDate = LocalDate.of(2026, 6, 5);
        LocalDate today = LocalDate.of(2026, 6, 11);

        assertThat(FrankfurterClient.isStale(responseDate, today)).isFalse();
    }

    @Test
    void isStale_ShouldReturnTrue_WhenResponseDateIsFiveBusinessDaysOld() {
        // Thu 2026-06-04 -> Thu 2026-06-11: Fri, Mon, Tue, Wed, Thu = 5 business days
        LocalDate responseDate = LocalDate.of(2026, 6, 4);
        LocalDate today = LocalDate.of(2026, 6, 11);

        assertThat(FrankfurterClient.isStale(responseDate, today)).isTrue();
    }

    @Test
    void isStale_ShouldNotCountWeekends() {
        // Fri 2026-06-05 -> Mon 2026-06-08: only Mon counts = 1 business day
        LocalDate responseDate = LocalDate.of(2026, 6, 5);
        LocalDate today = LocalDate.of(2026, 6, 8);

        assertThat(FrankfurterClient.isStale(responseDate, today)).isFalse();
    }

    @Test
    void isStale_ShouldReturnTrue_WhenResponseDateIsWeeksOld() {
        LocalDate responseDate = LocalDate.of(2026, 5, 1);
        LocalDate today = LocalDate.of(2026, 6, 11);

        assertThat(FrankfurterClient.isStale(responseDate, today)).isTrue();
    }
}
