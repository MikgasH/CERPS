package com.example.cerpshashkin.service;

import com.example.cerpshashkin.BaseWireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeRateServiceCacheIntegrationTest extends BaseWireMockTest {

    @Autowired
    private ExchangeRateService exchangeRateService;

    private Currency eur;
    private Currency usd;
    private Currency gbp;
    private Currency jpy;

    @BeforeEach
    void setUp() {
        eur = Currency.getInstance("EUR");
        usd = Currency.getInstance("USD");
        gbp = Currency.getInstance("GBP");
        jpy = Currency.getInstance("JPY");

        exchangeRateService.refreshRates();
    }

    @Test
    void getExchangeRate_EurToUsd_ShouldReturnFromCache() {
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(eur, usd);

        assertThat(result).isPresent();
        assertThat(result.get()).isPositive();
    }

    @Test
    void getExchangeRate_EurToGbp_ShouldReturnFromCache() {
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(eur, gbp);

        assertThat(result).isPresent();
        assertThat(result.get()).isPositive();
    }

    @Test
    void getExchangeRate_EurToJpy_ShouldReturnFromCache() {
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(eur, jpy);

        assertThat(result).isPresent();
        assertThat(result.get()).isPositive();
    }

    @Test
    void getExchangeRate_UsdToEur_ShouldReturnInverseFromCache() {
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(usd, eur);

        assertThat(result).isPresent();
        assertThat(result.get()).isPositive();
    }

    @Test
    void getExchangeRate_GbpToEur_ShouldReturnInverseFromCache() {
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(gbp, eur);

        assertThat(result).isPresent();
        assertThat(result.get()).isPositive();
    }

    @Test
    void getExchangeRate_JpyToEur_ShouldReturnInverseFromCache() {
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(jpy, eur);

        assertThat(result).isPresent();
        assertThat(result.get()).isPositive();
    }

    @Test
    void getExchangeRate_UsdToGbp_ShouldReturnCrossRateFromCache() {
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(usd, gbp);

        assertThat(result).isPresent();
        assertThat(result.get()).isPositive();
    }

    @Test
    void getExchangeRate_GbpToUsd_ShouldReturnCrossRateFromCache() {
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(gbp, usd);

        assertThat(result).isPresent();
        assertThat(result.get()).isPositive();
    }

    @Test
    void getExchangeRate_UsdToJpy_ShouldReturnCrossRateFromCache() {
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(usd, jpy);

        assertThat(result).isPresent();
        assertThat(result.get()).isPositive();
    }

    @Test
    void getExchangeRate_GbpToJpy_ShouldReturnCrossRateFromCache() {
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(gbp, jpy);

        assertThat(result).isPresent();
        assertThat(result.get()).isPositive();
    }

    @Test
    void getExchangeRate_AllCombinations_ShouldWorkFromCache() {
        Currency[] currencies = {eur, usd, gbp, jpy};

        int successfulConversions = 0;

        for (Currency from : currencies) {
            for (Currency to : currencies) {
                if (from.equals(to)) {
                    continue;
                }

                Optional<BigDecimal> result = exchangeRateService.getExchangeRate(from, to);

                assertThat(result)
                        .as("Conversion from %s to %s should succeed", from, to)
                        .isPresent();

                assertThat(result.get())
                        .as("Rate for %s -> %s should be positive", from, to)
                        .isPositive();

                successfulConversions++;
            }
        }

        assertThat(successfulConversions).isEqualTo(12);
    }

    @Test
    void getExchangeRate_RoundTrip_ShouldBeConsistent() {
        Optional<BigDecimal> eurToUsd = exchangeRateService.getExchangeRate(eur, usd);
        Optional<BigDecimal> usdToEur = exchangeRateService.getExchangeRate(usd, eur);

        assertThat(eurToUsd).isPresent();
        assertThat(usdToEur).isPresent();

        BigDecimal roundTrip = eurToUsd.get().multiply(usdToEur.get());

        assertThat(roundTrip).isCloseTo(
                BigDecimal.ONE,
                org.assertj.core.data.Offset.offset(new BigDecimal("0.01"))
        );
    }

    @Test
    void getExchangeRate_TransitiveProperty_ShouldBeConsistent() {
        Optional<BigDecimal> direct = exchangeRateService.getExchangeRate(usd, gbp);

        Optional<BigDecimal> usdToEur = exchangeRateService.getExchangeRate(usd, eur);
        Optional<BigDecimal> eurToGbp = exchangeRateService.getExchangeRate(eur, gbp);

        assertThat(direct).isPresent();
        assertThat(usdToEur).isPresent();
        assertThat(eurToGbp).isPresent();

        BigDecimal viaChain = usdToEur.get().multiply(eurToGbp.get());

        assertThat(direct.get()).isCloseTo(
                viaChain,
                org.assertj.core.data.Offset.offset(new BigDecimal("0.000001"))
        );
    }
}
