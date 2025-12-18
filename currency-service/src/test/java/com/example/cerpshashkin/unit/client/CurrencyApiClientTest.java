package com.example.cerpshashkin.unit.client;

import com.example.cerpshashkin.client.impl.CurrencyApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CurrencyApiClientTest {

    @Test
    void getProviderName_ShouldReturnCorrectName() {
        CurrencyApiClient client = new CurrencyApiClient(null, null, null);

        assertThat(client.getProviderName()).isEqualTo("CurrencyAPI");
    }
}
