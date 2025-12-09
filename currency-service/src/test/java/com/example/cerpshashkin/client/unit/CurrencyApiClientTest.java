package com.example.cerpshashkin.client.unit;

import com.example.cerpshashkin.client.impl.CurrencyApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CurrencyApiClientTest {

    @Test
    void getProviderName_ShouldReturnCorrectName() {
        CurrencyApiClient client = new CurrencyApiClient(null, null);

        assertThat(client.getProviderName()).isEqualTo("CurrencyAPI");
    }
}
