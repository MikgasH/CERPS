/*
package com.example.cerpshashkin;

import com.example.cerpshashkin.entity.SupportedCurrencyEntity;
import com.example.cerpshashkin.repository.SupportedCurrencyRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseWireMockTest {

    protected static final WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.options().dynamicPort()
        );
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        System.out.println("WireMock started successfully on port: " + wireMockServer.port());
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String baseUrl = "http://localhost:" + wireMockServer.port();
        registry.add("api.fixer.url", () -> baseUrl);
        registry.add("api.exchangerates.url", () -> baseUrl);
        registry.add("api.currencyapi.url", () -> baseUrl);
        registry.add("api.mock1.url", () -> baseUrl);
        registry.add("api.mock2.url", () -> baseUrl);
    }

    @BeforeAll
    static void setupTestData(@Autowired SupportedCurrencyRepository supportedCurrencyRepository,
                              @Autowired RoleRepository roleRepository,
                              @Autowired UserRepository userRepository,
                              @Autowired PasswordEncoder passwordEncoder) {
        if (supportedCurrencyRepository.count() == 0) {
            setupCurrencies(supportedCurrencyRepository);
        }
        if (roleRepository.count() == 0) {
            setupRolesAndUsers(roleRepository, userRepository, passwordEncoder);
        }
    }

    private static void setupCurrencies(SupportedCurrencyRepository repository) {
        List.of("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY", "SEK", "NZD")
                .forEach(code -> repository.save(
                        SupportedCurrencyEntity.builder()
                                .currencyCode(code)
                                .build()
                ));
    }

    private static void setupRolesAndUsers(RoleRepository roleRepository,
                                           UserRepository userRepository,
                                           PasswordEncoder passwordEncoder) {
        RoleEntity roleUser = roleRepository.save(
                RoleEntity.builder().name("ROLE_USER").build()
        );
        RoleEntity rolePremium = roleRepository.save(
                RoleEntity.builder().name("ROLE_PREMIUM_USER").build()
        );
        RoleEntity roleAdmin = roleRepository.save(
                RoleEntity.builder().name("ROLE_ADMIN").build()
        );

        String password = passwordEncoder.encode("password");

        userRepository.save(UserEntity.builder()
                .email("user@example.com")
                .password(password)
                .enabled(true)
                .createdAt(Instant.now())
                .roles(Set.of(roleUser))
                .build());

        userRepository.save(UserEntity.builder()
                .email("premium@example.com")
                .password(password)
                .enabled(true)
                .createdAt(Instant.now())
                .roles(Set.of(roleUser, rolePremium))
                .build());

        userRepository.save(UserEntity.builder()
                .email("admin@example.com")
                .password(password)
                .enabled(true)
                .createdAt(Instant.now())
                .roles(Set.of(roleUser, rolePremium, roleAdmin))
                .build());
    }

    @BeforeEach
    void resetWireMock() {
        if (wireMockServer.isRunning()) {
            wireMockServer.resetAll();
            WireMock.configureFor("localhost", wireMockServer.port());
            setupDefaultWireMockStubs();
        } else {
            System.err.println("WireMock server is NOT running before test execution. Skipping reset.");
        }
    }

    protected void setupDefaultWireMockStubs() {
        setupFixerStub();
        setupExchangeRatesStub();
        setupCurrencyApiStub();
        setupMockService1Stub();
        setupMockService2Stub();
    }

    protected void setupFixerStub() {
        stubFor(get(urlPathMatching("/latest"))
                .withQueryParam("access_key", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("fixer-exchangerates-success-response.json"))));
    }

    protected void setupExchangeRatesStub() {
        stubFor(get(urlPathMatching("/latest"))
                .withQueryParam("access_key", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("fixer-exchangerates-success-response.json"))));
    }

    protected void setupCurrencyApiStub() {
        stubFor(get(urlPathMatching("/latest"))
                .withQueryParam("apikey", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("currencyapi-success-response.json"))));
    }

    protected void setupMockService1Stub() {
        stubFor(get(urlPathEqualTo("/api/rates/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("mock-service-success-response.json"))));
    }

    protected void setupMockService2Stub() {
        stubFor(get(urlPathEqualTo("/api/rates/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJsonFile("mock-service-success-response.json"))));
    }

    protected String readJsonFile(final String fileName) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            URL resource = classLoader.getResource("test-data/" + fileName);

            if (resource == null) {
                throw new IllegalArgumentException("File not found: test-data/" + fileName);
            }

            return Files.readString(Paths.get(resource.toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to read test data file: " + fileName, e);
        }
    }
}
*/
