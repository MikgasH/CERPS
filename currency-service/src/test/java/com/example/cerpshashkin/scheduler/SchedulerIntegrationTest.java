package com.example.cerpshashkin.scheduler;

import com.example.cerpshashkin.BaseWireMockTest;
import com.example.cerpshashkin.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
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

    @Test
    void applicationStartup_WithAllProvidersFailed_ShouldUseMockProvider() {
        wireMockServer.resetAll();

        stubFor(get(urlPathMatching("/latest.*"))
                .willReturn(aResponse().withStatus(500)));

        setupMockService1Stub();
        setupMockService2Stub();

        exchangeRateService.refreshRates();

        Currency eur = Currency.getInstance("EUR");
        Currency usd = Currency.getInstance("USD");

        Optional<BigDecimal> rate = exchangeRateService.getExchangeRate(eur, usd);

        assertThat(rate).isPresent();
        assertThat(rate.get()).isPositive();
    }
}
