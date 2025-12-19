# Testing Strategy - CERPS Project

## Overview
This document describes the automated testing strategy for the Currency Exchange Rate Provider System (CERPS).

## Test Architecture

### 1. Test Layers

#### Unit Tests
- **Location**: `src/test/java/**/unit/`
- **Coverage**: Service layer, utilities, validators
- **Tools**: JUnit 5, Mockito, AssertJ
- **Characteristics**: Fast (< 100ms), isolated, no external dependencies

#### Integration Tests
- **Location**: `src/test/java/**/integration/`
- **Coverage**: Controllers, repositories, security, external API clients
- **Tools**: Spring Boot Test, WireMock, TestContainers (where applicable), H2
- **Characteristics**: Test component interactions, in-memory database

### 2. Test Coverage by Service

#### Currency Service
- **Integration Tests**: 38 test classes
    - External API clients (Fixer, ExchangeRates, CurrencyAPI, MockServices)
    - Repository layer (ExchangeRateRepository, SupportedCurrencyRepository, ApiProviderKeyRepository)
    - REST Controllers with security
    - Cache mechanisms
    - Scheduler integration
- **Unit Tests**: Service layer business logic
- **Coverage**: 83%

#### User Service
- **Integration Tests**: 7 test classes
    - Authentication flow (register, login, token validation)
    - JWT filter
    - Security configuration
    - Repository layer
- **Unit Tests**: JWT utilities, password validators
- **Coverage**: 78%

#### Analytics Service
- **Integration Tests**: 6 test classes
    - Trends calculation
    - Security filters
    - Repository operations
- **Unit Tests**: Trends calculation logic, exception handlers
- **Coverage**: 75%

#### API Gateway
- **Unit Tests**: Filter logic (CorrelationId, JWT)
- **Integration Tests**: Route configuration, authentication flow
- **Coverage**: 72%

### 3. External Dependencies Handling

#### WireMock for External APIs
```java
// Mock external currency provider
stubFor(get(urlEqualTo("/latest?access_key=test-key"))
    .willReturn(aResponse()
        .withStatus(200)
        .withBody(readJsonFile("success-response.json"))));
```

**Covered scenarios:**
- Successful responses
- HTTP errors (401, 404, 500, 503)
- Empty/invalid JSON
- Network timeouts

#### In-Memory Database (H2)
- PostgreSQL compatibility mode
- Test data initialization via SQL scripts
- Transaction rollback between tests

### 4. Test Execution

#### Local Execution
```bash
# Run all tests
mvn clean verify

# Run specific service tests
cd currency-service && mvn test

# Generate coverage report
mvn jacoco:report
```

#### CI/CD Pipeline
- **Platform**: GitHub Actions
- **Trigger**: Push to main, Pull Requests
- **Steps**:
    1. Checkout code
    2. Set up JDK 21
    3. Build with Maven
    4. Run tests (`mvn clean verify`)
    5. Generate coverage reports
    6. Upload artifacts

### 5. Test Quality Assurance

#### Coverage Thresholds
- **Minimum**: 70% (enforced by JaCoCo)
- **Target**: 80%+
- **Exclusions**: DTOs, configuration classes, main methods

#### Best Practices Followed
- **FIRST Principles**:
    - ✅ Fast: Unit tests < 100ms
    - ✅ Independent: No test dependencies
    - ✅ Repeatable: Same results every run
    - ✅ Self-Validating: Clear pass/fail
    - ✅ Timely: Written alongside code

- **Given-When-Then** structure
- **Descriptive test names**: `should_ReturnRate_When_ValidCurrenciesProvided()`
- **Arrange-Act-Assert** pattern

### 6. Known Gaps and Future Improvements

#### Current Gaps
- No mutation testing
- No load/performance tests
- Contract testing could be expanded

#### Planned Improvements
1. Add Testcontainers for PostgreSQL integration tests
2. Implement mutation testing with PITest
3. Add performance tests for high-load scenarios
4. Expand contract testing between services

### 7. Test Data Management

#### Test Data Location
- JSON fixtures: `src/test/resources/test-data/`
- SQL scripts: `src/test/resources/test-data.sql`
- Configuration: `src/test/resources/application-test.yml`

#### Data Strategy
- Minimal test data sets
- Isolated per test class
- Cleanup after each test (`@AfterEach`, `@Transactional`)

### 8. Debugging Failed Tests

#### Common Issues
1. **H2 Compatibility**: Use PostgreSQL mode
2. **Sequence Management**: Let H2 auto-manage
3. **Async Operations**: Use `@Transactional` carefully
4. **WireMock Port Conflicts**: Dynamic ports

#### Debug Commands
```bash
# Run with debug output
mvn test -X -Dtest=SpecificTest

# Check coverage report
open target/site/jacoco/index.html
```

## Metrics

| Service | Tests | Coverage | Execution Time |
|---------|-------|----------|----------------|
| Currency | 114 | 83% | ~2min |
| User | 47 | 78% | ~10s |
| Analytics | 23 | 75% | ~8s |
| API Gateway | 18 | 72% | ~7s |
| **Total** | **202** | **79%** | **~2.5min** |

## Conclusion

The CERPS project maintains comprehensive test coverage across all layers, with particular strength in integration testing of external APIs and security mechanisms. The testing strategy ensures reliable deployments while maintaining fast feedback loops for developers.
