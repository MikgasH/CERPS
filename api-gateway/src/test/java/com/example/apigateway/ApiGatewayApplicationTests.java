package com.example.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=dGVzdC1zZWNyZXQta2V5LW1pbi0yNTYtYml0cy10ZXN0LXNlY3JldC1rZXktbWluLTI1Ni1iaXRz",
        "spring.cloud.compatibility-verifier.enabled=false"
})
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
        // Verify that the application context loads successfully
    }
}

