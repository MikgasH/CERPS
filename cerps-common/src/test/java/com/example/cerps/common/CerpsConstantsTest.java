package com.example.cerps.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CerpsConstantsTest {

    @Test
    void calculationScale_ShouldBe6() {
        assertThat(CerpsConstants.CALCULATION_SCALE).isEqualTo(6);
    }

    @Test
    void displayScale_ShouldBe2() {
        assertThat(CerpsConstants.DISPLAY_SCALE).isEqualTo(2);
    }

    @Test
    void correlationIdHeader_ShouldBeCorrect() {
        assertThat(CerpsConstants.CORRELATION_ID_HEADER).isEqualTo("X-Correlation-ID");
    }

    @Test
    void correlationIdMdc_ShouldBeCorrect() {
        assertThat(CerpsConstants.CORRELATION_ID_MDC).isEqualTo("correlationId");
    }

    @Test
    void serviceNameMdc_ShouldBeCorrect() {
        assertThat(CerpsConstants.SERVICE_NAME_MDC).isEqualTo("service");
    }

    @Test
    void errorUriPrefix_ShouldBeCorrect() {
        assertThat(CerpsConstants.ERROR_URI_PREFIX).isEqualTo("https://cerps.example.com/errors/");
    }

    @Test
    void exchangeRateSourceAggregated_ShouldBeAGGREGATED() {
        assertThat(CerpsConstants.EXCHANGE_RATE_SOURCE_AGGREGATED).isEqualTo("AGGREGATED");
    }

    @Test
    void cacheTtlSeconds_ShouldBe8Hours() {
        assertThat(CerpsConstants.CACHE_TTL_SECONDS).isEqualTo(28800L);
        assertThat(CerpsConstants.CACHE_TTL_SECONDS).isEqualTo(8 * 60 * 60);
    }

    @Test
    void maxRateValue_ShouldBeOneMillion() {
        assertThat(CerpsConstants.MAX_RATE_VALUE).isEqualTo(1_000_000L);
    }

    @Test
    void hundred_ShouldBe100() {
        assertThat(CerpsConstants.HUNDRED).isEqualTo(100);
    }

    @Test
    void downsamplingLimits_ShouldBeCorrect() {
        assertThat(CerpsConstants.MAX_POINTS_1D).isEqualTo(24);
        assertThat(CerpsConstants.MAX_POINTS_7D).isEqualTo(56);
        assertThat(CerpsConstants.MAX_POINTS_30D).isEqualTo(90);
        assertThat(CerpsConstants.MAX_POINTS_90D).isEqualTo(180);
        assertThat(CerpsConstants.MAX_POINTS_180D).isEqualTo(270);
        assertThat(CerpsConstants.MAX_POINTS_1Y).isEqualTo(365);
    }

    @Test
    void downsamplingLimits_ShouldBeOrdered() {
        assertThat(CerpsConstants.MAX_POINTS_1D)
                .isLessThan(CerpsConstants.MAX_POINTS_7D);
        assertThat(CerpsConstants.MAX_POINTS_7D)
                .isLessThan(CerpsConstants.MAX_POINTS_30D);
        assertThat(CerpsConstants.MAX_POINTS_30D)
                .isLessThan(CerpsConstants.MAX_POINTS_90D);
        assertThat(CerpsConstants.MAX_POINTS_90D)
                .isLessThan(CerpsConstants.MAX_POINTS_180D);
        assertThat(CerpsConstants.MAX_POINTS_180D)
                .isLessThan(CerpsConstants.MAX_POINTS_1Y);
    }
}
