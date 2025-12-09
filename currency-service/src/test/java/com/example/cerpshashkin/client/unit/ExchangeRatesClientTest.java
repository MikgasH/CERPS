package com.example.cerpshashkin.client.unit;

import com.example.cerpshashkin.client.impl.ExchangeRatesClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ExchangeRatesClientTest {

    @Test
    void getProviderName_ShouldReturnCorrectName() {
        ExchangeRatesClient client = new ExchangeRatesClient(null, null);

        assertThat(client.getProviderName()).isEqualTo("ExchangeRatesAPI");
    }
}
