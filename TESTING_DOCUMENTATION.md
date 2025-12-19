# Automated Testing Documentation
## Currency Exchange Rate Provider System (CERPS)

---

## 1. Overview

Microservices-based currency exchange system with comprehensive automated testing.

**Test Coverage Results:**
- Currency Service: 89%
- User Service: 83%
- Analytics Service: 81%
- API Gateway: 94%
- Average: 87% (Required: 70%)

**Total Tests:** 487  
**Execution Time:** ~6 minutes

---

## 2. Testing Strategy

### Test Types
1. Unit Tests - Business logic, calculations, validators
2. Integration Tests - API endpoints, database, security, external services

### Technologies
- Framework: JUnit 5, Mockito, Spring Boot Test
- Mocking: WireMock (external APIs)
- Database: H2 in-memory (PostgreSQL mode)
- Coverage: JaCoCo
- CI/CD: GitHub Actions

---

## 3. Coverage by Service

### Currency Service (89%)
Tests: 315

Key Areas:
- External API clients (Fixer.io, ExchangeRates API, CurrencyAPI, Mock services)
- Repository layer (ExchangeRate, SupportedCurrency, ApiProviderKey)
- REST controllers with JWT security
- Cache mechanisms and scheduler
- Fallback logic for provider failures

Sample Test:
```java
@Test
void getLatestRates_ShouldReturnSuccessfulResponse() {
    stubFor(get(urlEqualTo("/latest?access_key=test-key"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(readJsonFile("success-response.json"))));

    CurrencyExchangeResponse result = fixerioClient.getLatestRates();

    assertThat(result.success()).isTrue();
    assertThat(result.rates()).isNotEmpty();
}
```

### User Service (83%)
Tests: 114

Key Areas:
- Full authentication flow (register → login → JWT validation)
- Password change functionality
- JWT token validation and extraction
- Spring Security configuration
- Role-based access control
- Repository layer (User, Role)

Sample Test:
```java
@Test
void fullAuthenticationFlow_ShouldWorkEndToEnd() {
    mockMvc.perform(post("/api/v1/auth/register")
        .content(registerRequest))
        .andExpect(status().isCreated());

    String token = loginAndGetToken();

    mockMvc.perform(get("/api/v1/auth/me")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
}
```

### Analytics Service (81%)
Tests: 34

Key Areas:
- Trends calculation (7D, 30D periods)
- Historical data queries
- Role-based access (PREMIUM_USER, ADMIN only)
- Gateway header authentication
- Repository time-range queries

Sample Test:
```java
@Test
void calculateTrends_WithSufficientData_ShouldReturnTrend() {
    List<ExchangeRateEntity> rates = List.of(
        createRate(sevenDaysAgo, "1.10"),
        createRate(now, "1.18")
    );

    TrendsResponse response = service.calculateTrends("USD", "EUR", "7D");

    assertThat(response.changePercentage()).isEqualByComparingTo("7.27");
}
```

### API Gateway (94%)
Tests: 24

Key Areas:
- JWT authentication filter
- Correlation ID generation and propagation
- Route configuration
- Public vs protected endpoints
- Multi-role token handling

Sample Test:
```java
@Test
void filter_WithValidToken_ShouldPassAuthentication() {
    String token = generateValidToken("user@example.com", "ROLE_USER");

    webTestClient.get()
        .uri("/api/v1/currencies")
        .header("Authorization", "Bearer " + token)
        .exchange()
        .expectStatus().isOk();
}
```

---

## 4. Testing Tools

### WireMock for External APIs
```java
@SpringBootTest
public abstract class BaseWireMockTest {
    protected static final WireMockServer wireMockServer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("api.fixer.url", () -> wireMockServer.baseUrl());
    }
}
```

Scenarios Tested:
- Successful responses (200)
- HTTP errors (401, 404, 500, 503)
- Empty/null data
- Invalid JSON
- Network timeouts

### H2 Database
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL
  jpa:
    hibernate:
      ddl-auto: create-drop
```

Benefits:
- Fast execution
- PostgreSQL compatibility
- Clean state per test
- No external dependencies

---

## 5. CI/CD Pipeline

Platform: GitHub Actions
```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Setup JDK 21
        uses: actions/setup-java@v4
      
      - name: Build and Test
        run: mvn clean verify
      
      - name: Upload Coverage
        uses: actions/upload-artifact@v4
        with:
          path: '**/target/site/jacoco/'
```

---

## 6. Best Practices

### FIRST Principles
- Fast: Unit tests under 100ms
- Independent: No test dependencies
- Repeatable: Deterministic results
- Self-Validating: Clear pass/fail
- Timely: Written with code

### Test Naming Convention
```
methodName_WithCondition_ShouldExpectedResult()

Examples:
- register_WithValidData_ShouldReturn201()
- login_WithInvalidPassword_ShouldReturn401()
- convertCurrency_WithSameCurrencies_ShouldReturnSameAmount()
```

### Test Structure (Given-When-Then)
```java
@Test
void testName() {
    // Given - setup
    var input = createInput();
    
    // When - action
    var result = service.process(input);
    
    // Then - assertion
    assertThat(result).isNotNull();
}
```

---

## 7. Test Results Summary

| Service | Tests | Coverage | Execution Time |
|---------|-------|----------|----------------|
| Currency Service | 315 | 89% | ~3 min |
| User Service | 114 | 83% | ~46 sec |
| Analytics Service | 34 | 81% | ~1 min |
| API Gateway | 24 | 94% | ~1 min |
| TOTAL | 487 | 87% | ~6 min |

All tests pass locally with zero failures.

---

## 8. Running Tests

### Local Execution
```bash
# Currency Service
mvn verify -pl currency-service

# User Service
mvn verify -pl user-service

# Analytics Service
mvn verify -pl analytics-service

# API Gateway
mvn verify -pl api-gateway

# Generate coverage report
mvn jacoco:report -pl <service-name>
```

### View Coverage Report
```bash
# After running tests
open <service-name>/target/site/jacoco/index.html
```

---

## 9. Requirements Met

- 70%+ coverage - Achieved 87%
- Structured test setup - Unit + Integration layers
- Unit tests - Business logic validation
- Integration tests - API, DB, Security, External services
- CI execution - GitHub Actions configured
- Documentation - This document + inline code comments
- Test naming - Clear convention followed

---

## 10. Conclusion

The CERPS project achieves 87% test coverage across all microservices, exceeding the required 70% threshold. The test suite includes:

- 487 automated tests (Unit + Integration)
- Comprehensive external API mocking with WireMock
- Full authentication flow testing
- Security and authorization validation
- Database integration tests
- Fast execution (approximately 6 minutes)

All critical business logic, API endpoints, and security mechanisms are thoroughly tested.

Repository: https://github.com/MikgasH/exchange/tree/feature/microservices-architecture
Coverage Reports: Available in target/site/jacoco/ after running mvn verify
```
