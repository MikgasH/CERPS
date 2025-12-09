package com.example.cerpshashkin.client.unit;

import com.example.cerpshashkin.client.impl.FixerioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FixerioClientTest {

    @Test
    void getProviderName_ShouldReturnCorrectName() {
        FixerioClient client = new FixerioClient(null, null);

        assertThat(client.getProviderName()).isEqualTo("Fixer.io");
    }
}
