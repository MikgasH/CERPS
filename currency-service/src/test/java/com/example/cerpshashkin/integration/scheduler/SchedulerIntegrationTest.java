package com.example.cerpshashkin.integration.scheduler;

import com.example.cerpshashkin.integration.BaseWireMockTest;
import com.example.cerpshashkin.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerIntegrationTest extends BaseWireMockTest {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Test
    void applicationStartup_ShouldLoadExchangeRatesIntoCache() {
        exchangeRateService.refreshRates();

        Currency eur = Currency.getInstance("EUR");
        Currency usd = Currency.getInstance("USD");

        Optional<BigDecimal> rate = exchangeRateService.getExchangeRate(eur, usd);

        assertThat(rate).isPresent();
        assertThat(rate.get()).isPositive();
    }
}
