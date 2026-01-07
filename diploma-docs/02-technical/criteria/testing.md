# Criterion 7: Automated Testing

## Overview

| Aspect | Details                       |
|--------|-------------------------------|
| **Criterion** | Automated Tests ≥70% Coverage |
| **Achieved Coverage** | **89%** (exceeds requirement) |
| **Total Tests** | 509                           |
| **Execution Time** | ~6 minutes                    |
| **Framework** | JUnit 5, Mockito, WireMock    |

## Coverage Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                    Test Coverage by Service                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Currency Service    ████████████████████░░░  90%  (316 tests)  │
│  API Gateway         ███████████████████████  94%  (24 tests)   │
│  User Service        ████████████████░░░░░░░  83%  (114 tests)  │
│  Analytics Service   ████████████████░░░░░░░  88%  (55 tests)   │
│                                                                  │
│  ─────────────────────────────────────────────────────────────  │
│  AVERAGE             █████████████████░░░░░░  89%  (509 tests)  │
│  REQUIRED            ██████████████░░░░░░░░░  70%               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Coverage by Service

| Service | Tests   | Line Coverage | Branch Coverage | Status |
|---------|---------|---------------|-----------------|--------|
| Currency Service | 316     | 90%           | 72%             | Exceeds |
| API Gateway | 24      | 94%           | 90%             | Exceeds |
| User Service | 114     | 83%           | 80%             | Exceeds |
| Analytics Service | 55      | 88%           | 72%             | Exceeds |
| **Total** | **509** | **89%**       | **79%**         | **Exceeds 70%** |

## Testing Strategy

### Test Pyramid

```
                    ┌───────────┐
                    │   E2E     │  (Manual/Future)
                    │  Tests    │
                   ┌┴───────────┴┐
                   │ Integration │  ~150 tests
                   │   Tests     │  (API, DB, Security)
                  ┌┴─────────────┴┐
                  │  Unit Tests   │  ~359 tests
                  │ (Fast, Isolated)│  (Business logic)
                  └───────────────┘
```

### Test Types

| Type | Purpose | Framework | Count |
|------|---------|-----------|-------|
| Unit | Business logic, calculations | JUnit 5, Mockito | ~359  |
| Integration | API endpoints, database | Spring Boot Test | ~150  |
| External API | Provider mocking | WireMock | ~50   |

## Technology Stack

### Core Testing Frameworks

```xml
<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- Spring Boot Test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- WireMock -->
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <scope>test</scope>
</dependency>

<!-- JaCoCo (Coverage) -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
</plugin>
```

## Test Examples

### Unit Test - Currency Conversion

```java
@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private ExchangeRateRepository repository;
    
    @InjectMocks
    private CurrencyService currencyService;

    @Test
    void convertCurrency_WithValidInput_ShouldReturnConvertedAmount() {
        // Given
        BigDecimal amount = new BigDecimal("1000");
        String from = "USD";
        String to = "EUR";
        BigDecimal rate = new BigDecimal("0.924568");
        
        when(repository.findLatestRate(from, to))
            .thenReturn(Optional.of(createRate(rate)));
        
        // When
        ConversionResponse result = currencyService.convert(amount, from, to);
        
        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.convertedAmount())
            .isEqualByComparingTo(new BigDecimal("924.568"));
        assertThat(result.exchangeRate()).isEqualByComparingTo(rate);
    }
    
    @Test
    void convertCurrency_WithSameCurrency_ShouldReturnSameAmount() {
        // Given
        BigDecimal amount = new BigDecimal("1000");
        
        // When
        ConversionResponse result = currencyService.convert(amount, "USD", "USD");
        
        // Then
        assertThat(result.convertedAmount()).isEqualByComparingTo(amount);
        assertThat(result.exchangeRate()).isEqualByComparingTo(BigDecimal.ONE);
    }
}
```

### Integration Test - Authentication Flow

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fullAuthenticationFlow_ShouldWorkEndToEnd() throws Exception {
        // Register
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "test@example.com",
                        "password": "SecurePass123!"
                    }
                    """))
            .andExpect(status().isCreated());

        // Login
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "test@example.com",
                        "password": "SecurePass123!"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andReturn();

        String token = JsonPath.read(
            loginResult.getResponse().getContentAsString(), 
            "$.token"
        );

        // Access protected endpoint
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
```

### WireMock Test - External API

```java
@SpringBootTest
@WireMockTest(httpPort = 8089)
class FixerClientTest {

    @Autowired
    private FixerClient fixerClient;

    @Test
    void getLatestRates_WithSuccessResponse_ShouldReturnRates() {
        // Given
        stubFor(get(urlEqualTo("/latest?access_key=test-key"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "success": true,
                        "base": "EUR",
                        "rates": {
                            "USD": 1.0845,
                            "GBP": 0.8567
                        }
                    }
                    """)));

        // When
        CurrencyExchangeResponse result = fixerClient.getLatestRates();

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.rates()).containsKey("USD");
        assertThat(result.rates().get("USD")).isEqualByComparingTo("1.0845");
    }

    @Test
    void getLatestRates_WithServerError_ShouldHandleGracefully() {
        // Given
        stubFor(get(urlEqualTo("/latest?access_key=test-key"))
            .willReturn(aResponse()
                .withStatus(500)));

        // When/Then
        assertThatThrownBy(() -> fixerClient.getLatestRates())
            .isInstanceOf(ExternalApiException.class);
    }
}
```

## Test Database Configuration

### H2 In-Memory Database

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
```

### Benefits

| Aspect | Benefit |
|--------|---------|
| Speed | No external DB startup |
| Isolation | Fresh state each test |
| PostgreSQL Mode | Compatible SQL syntax |
| Parallel Tests | No resource conflicts |

## Test Naming Convention

```java
// Pattern: methodName_WithCondition_ShouldExpectedResult

@Test
void register_WithValidData_ShouldReturn201() { }

@Test
void login_WithInvalidPassword_ShouldReturn401() { }

@Test
void convertCurrency_WithSameCurrencies_ShouldReturnSameAmount() { }

@Test
void getTrends_WithInsufficientData_ShouldReturn404() { }
```

## Test Structure (AAA Pattern)

```java
@Test
void methodUnderTest_Scenario_ExpectedBehavior() {
    // Arrange (Given)
    var input = createTestInput();
    when(mockDependency.method()).thenReturn(expectedValue);
    
    // Act (When)
    var result = serviceUnderTest.methodUnderTest(input);
    
    // Assert (Then)
    assertThat(result).isNotNull();
    assertThat(result.field()).isEqualTo(expected);
    verify(mockDependency).method();
}
```

## CI/CD Integration

### GitHub Actions Pipeline

```yaml
name: Test and Coverage

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run Tests with Coverage
        run: mvn clean verify
      
      - name: Upload Coverage Report
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: '**/target/site/jacoco/'
```

### Coverage Enforcement

```xml
<!-- pom.xml - JaCoCo configuration -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Running Tests

### Commands

```bash
# Run all tests
mvn test

# Run with coverage report
mvn verify

# Run specific service tests
mvn test -pl currency-service

# Run specific test class
mvn test -Dtest=CurrencyServiceTest

# View coverage report
open target/site/jacoco/index.html
```

### Coverage Report Location

```
<service>/target/site/jacoco/
├── index.html           # Main report
├── jacoco.csv           # CSV export
└── jacoco.xml           # XML for CI tools
```

## Key Test Scenarios

### Security Tests

| Scenario | Test |
|----------|------|
| Missing token | Returns 401 |
| Invalid token | Returns 401 |
| Expired token | Returns 401 |
| Insufficient role | Returns 403 |
| Valid authentication | Passes through |

### Business Logic Tests

| Scenario | Test |
|----------|------|
| Currency conversion | Correct calculation |
| Same currency | Returns 1:1 rate |
| Invalid currency | Returns 400 |
| Rate aggregation | Median calculation |
| Trend analysis | Percentage calculation |

### Error Handling Tests

| Scenario | Test |
|----------|------|
| External API failure | Fallback triggered |
| All providers fail | Returns 503 |
| Validation error | Returns 400 with details |
| Database error | Graceful handling |

## FIRST Principles

| Principle | Implementation |
|-----------|----------------|
| **F**ast | Unit tests < 100ms each |
| **I**ndependent | No test order dependency |
| **R**epeatable | Same result every run |
| **S**elf-validating | Clear pass/fail |
| **T**imely | Written with code |

## Summary

| Metric | Value              | Requirement | Status |
|--------|--------------------|-------------|--------|
| Line Coverage | 89%                | ≥70% | Exceeds |
| Branch Coverage | 79%                | - | Good |
| Total Tests | 509                | - | Comprehensive |
| Execution Time | ~6 min             | - | Acceptable |
| Test Types | Unit + Integration | Both required | Complete |
