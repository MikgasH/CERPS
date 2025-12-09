package com.example.cerpshashkin.client;

import com.example.cerpshashkin.dto.UserValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class UserServiceClient {

    private final RestClient restClient;
    private final String userServiceUrl;

    public UserServiceClient(
            final RestClient.Builder restClientBuilder,
            @Value("${services.user-service.url}") final String userServiceUrl
    ) {
        this.userServiceUrl = userServiceUrl;
        this.restClient = restClientBuilder.build();
    }

    public UserValidationResponse validateToken(final String token) {
        log.debug("Validating token with User Service");

        try {
            final UserValidationResponse response = restClient.post()
                    .uri(userServiceUrl + "/api/internal/auth/validate")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(UserValidationResponse.class);

            log.debug("Token validation response: valid={}", response != null && response.valid());
            return response;

        } catch (final Exception e) {
            log.error("Failed to validate token with User Service", e);
            return new UserValidationResponse(false, null, null);
        }
    }
}
