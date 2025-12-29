# Criterion 1: Back-end

## Overview

| Aspect | Details |
|--------|---------|
| **Criterion** | Back-end Development |
| **Language** | Java 21 (LTS) |
| **Framework** | Spring Boot 3.5.6 |
| **Architecture** | Layered (Controller -> Service -> Repository) |
| **Principles** | SOLID, Clean Code, DRY |

## Technology Choice

### Why Java + Spring Boot

| Factor | Java + Spring | Node.js | Python + FastAPI |
|--------|---------------|---------|------------------|
| Type Safety | Strong static typing | Dynamic | Optional typing |
| Enterprise Adoption | Industry standard | Growing | Limited |
| Spring Ecosystem | Comprehensive | N/A | N/A |
| Performance | JIT optimization | Event loop | Async limited |
| EPAM Curriculum | Required | Optional | Optional |

**Decision:** Java 21 + Spring Boot 3.5.6 selected for enterprise-grade reliability, strong typing, and curriculum requirements.

## Layered Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Controller Layer                      │
│         (REST endpoints, request/response handling)      │
├─────────────────────────────────────────────────────────┤
│                     Service Layer                        │
│           (Business logic, transaction management)       │
├─────────────────────────────────────────────────────────┤
│                   Repository Layer                       │
│              (Data access, JPA operations)               │
├─────────────────────────────────────────────────────────┤
│                     Entity Layer                         │
│               (Domain models, JPA entities)              │
└─────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

| Layer | Responsibility | Spring Annotation |
|-------|----------------|-------------------|
| Controller | HTTP handling, validation, DTO mapping | `@RestController` |
| Service | Business logic, orchestration | `@Service` |
| Repository | Data persistence, queries | `@Repository` |
| Entity | Domain model, database mapping | `@Entity` |

## SOLID Principles Implementation

### Single Responsibility Principle (SRP)

Each class has one reason to change:

```java
// Good: Separate responsibilities
@Service
public class CurrencyConversionService {
    // Only handles conversion logic
}

@Service
public class ExchangeRateAggregationService {
    // Only handles rate aggregation
}

@Service
public class RateCacheService {
    // Only handles caching
}
```

### Open/Closed Principle (OCP)

Open for extension, closed for modification:

```java
// Interface for exchange rate providers
public interface ExchangeRateClient {
    CurrencyExchangeResponse getLatestRates();
    String getProviderName();
}

// Easy to add new providers without modifying existing code
@Component
public class FixerClient implements ExchangeRateClient { }

@Component
public class ExchangeRatesApiClient implements ExchangeRateClient { }

@Component
public class CurrencyApiClient implements ExchangeRateClient { }
```

### Liskov Substitution Principle (LSP)

Subtypes substitutable for base types:

```java
// Any ExchangeRateClient implementation can be used
@Service
public class ExchangeRateAggregationService {
    private final List<ExchangeRateClient> clients;

    public Map<String, BigDecimal> aggregateRates() {
        return clients.stream()
                .map(ExchangeRateClient::getLatestRates)
                .collect(/* median calculation */);
    }
}
```

### Interface Segregation Principle (ISP)

Clients depend only on interfaces they use:

```java
// Focused interfaces
public interface RateProvider {
    Map<String, BigDecimal> getCurrentRates();
}

public interface RateValidator {
    boolean isValidRate(BigDecimal rate);
}

public interface RateCache {
    Optional<BigDecimal> getCachedRate(String pair);
    void cacheRate(String pair, BigDecimal rate);
}
```

### Dependency Inversion Principle (DIP)

Depend on abstractions, not concretions:

```java
@Service
public class CurrencyService {
    // Depends on interface, not implementation
    private final ExchangeRateRepository repository;
    private final List<ExchangeRateClient> clients;

    // Constructor injection
    public CurrencyService(
            ExchangeRateRepository repository,
            List<ExchangeRateClient> clients) {
        this.repository = repository;
        this.clients = clients;
    }
}
```

## Package Structure

### Currency Service (Main Business Service)

```
com.example.cerpshashkin/
├── client/                         # External API clients
│   ├── ApiProvider.java            # Provider enum
│   ├── ExchangeRateClient.java     # Client interface
│   └── impl/
│       ├── FixerioClient.java
│       ├── ExchangeRatesClient.java
│       ├── CurrencyApiClient.java
│       ├── MockService1Client.java
│       └── MockService2Client.java
├── config/
│   ├── CacheConfig.java
│   ├── ExternalClientsConfig.java
│   ├── OpenApiConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── CurrencyController.java
│   └── ProviderKeyManagementController.java
├── converter/
│   ├── CurrencyApiConverter.java
│   └── ExternalApiConverter.java
├── dto/
│   ├── CreateProviderKeyRequest.java
│   ├── UpdateProviderKeyRequest.java
│   ├── ProviderKeyResponse.java
│   └── (API response DTOs)
├── entity/
│   ├── ExchangeRateEntity.java
│   ├── SupportedCurrencyEntity.java
│   └── ApiProviderKeyEntity.java
├── exception/
│   └── (12 custom exception classes)
├── filter/
│   └── GatewayHeaderAuthenticationFilter.java
├── mapper/
│   └── ProviderKeyMapper.java (MapStruct)
├── model/
│   ├── CachedRate.java
│   └── CurrencyExchangeResponse.java
├── repository/
│   ├── ExchangeRateRepository.java
│   ├── SupportedCurrencyRepository.java
│   └── ApiProviderKeyRepository.java
├── scheduler/
│   ├── ExchangeRateScheduler.java
│   └── DatabaseCleanupScheduler.java
└── service/
    ├── CurrencyService.java
    ├── CurrencyConversionService.java
    ├── ExchangeRateService.java
    ├── ExchangeRateProviderService.java
    ├── EncryptionService.java
    ├── ProviderKeyManagementService.java
    └── cache/
        └── CurrencyRateCache.java
```

### Common Library (cerps-common)

```
com.example.cerps.common/
├── config/
│   ├── BaseOpenApiConfig.java
│   └── CorrelationIdClientConfig.java
├── converter/
│   ├── CurrencyAttributeConverter.java
│   └── ResponseConverter.java
├── dto/
│   ├── ConversionRequest.java
│   ├── ConversionResponse.java
│   ├── TrendsRequest.java
│   ├── TrendsResponse.java
│   └── UserValidationResponse.java
├── exception/
│   └── (Base exception handlers)
├── filter/
│   └── CorrelationIdFilter.java
└── validation/
    ├── ValidCurrency.java
    ├── CurrencyCodeValidator.java
    ├── ValidPeriod.java
    └── PeriodValidator.java
```

### Custom Validators

**CurrencyCodeValidator** - validates ISO 4217 currency codes using `java.util.Currency.getInstance()`. Throws `IllegalArgumentException` for invalid codes.

```java
Currency.getInstance(value.trim().toUpperCase()); // USD, EUR, GBP...
```

**PeriodValidator** - validates trend analysis period format:

| Unit | Range | Examples |
|------|-------|----------|
| H (hours) | 12 - 8760 | 12H, 24H, 168H |
| D (days) | 1 - 365 | 1D, 7D, 30D |
| M (months) | 1 - 12 | 1M, 6M, 12M |
| Y (years) | 1 only | 1Y |

## Request Flow Example

```
POST /api/v1/currencies/convert
         │
         v
┌─────────────────────────────────────┐
│     CurrencyController              │
│  - Validates @RequestBody           │
│  - Calls service layer              │
└─────────────────────────────────────┘
         │
         v
┌─────────────────────────────────────┐
│     CurrencyService                 │
│  - Retrieves exchange rate          │
│  - Performs calculation             │
│  - Handles caching                  │
└─────────────────────────────────────┘
         │
         v
┌─────────────────────────────────────┐
│     ExchangeRateRepository          │
│  - Queries database                 │
│  - Returns entity                   │
└─────────────────────────────────────┘
         │
         v
┌─────────────────────────────────────┐
│     ConversionResponse (DTO)        │
│  - Formatted for client             │
└─────────────────────────────────────┘
```

## Code Quality Standards

### Validation

```java
// ConversionRequest.java - uses custom @ValidCurrency annotation
@Builder
public record ConversionRequest(
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    BigDecimal amount,

    @ValidCurrency(message = "Invalid source currency code")
    String from,

    @ValidCurrency(message = "Invalid target currency code")
    String to
) {}

// TrendsRequest.java - uses custom @ValidPeriod annotation
public record TrendsRequest(
    @NotBlank(message = "From currency is required")
    @ValidCurrency(message = "Invalid source currency code")
    String from,

    @NotBlank(message = "To currency is required")
    @ValidCurrency(message = "Invalid target currency code")
    String to,

    @NotBlank(message = "Period is required")
    @ValidPeriod
    String period
) {}
```

### Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CurrencyNotFoundException.class)
    public ProblemDetail handleCurrencyNotFound(CurrencyNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Currency not found");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
```

### Record Types for DTOs

```java
// Immutable, auto-generated equals/hashCode/toString
public record ConversionResponse(
    boolean success,
    BigDecimal originalAmount,
    String fromCurrency,
    String toCurrency,
    BigDecimal convertedAmount,
    BigDecimal exchangeRate,
    Instant timestamp
) {}
```

## Testing Strategy

| Type | Framework | Coverage Target |
|------|-----------|-----------------|
| Unit Tests | JUnit 5, Mockito | Service layer 90%+ |
| Integration Tests | Spring Boot Test | Controllers 80%+ |
| Repository Tests | @DataJpaTest | Repository layer 85%+ |

## Key Metrics

| Metric | Value |
|--------|-------|
| Total Services | 4 Spring Boot applications |
| Lines of Code | ~8,000 (excluding tests) |
| Test Coverage | 87% average |
| Dependencies | 15 main dependencies |
