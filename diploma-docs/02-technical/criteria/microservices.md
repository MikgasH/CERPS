# Criterion 4: Microservices Architecture

## Overview

| Aspect | Details |
|--------|---------|
| **Criterion** | Microservices Architecture |
| **Total Services** | 6 (4 main + 2 mock) |
| **Pattern** | API Gateway + Backend Services |
| **Communication** | Synchronous REST |
| **Service Discovery** | Docker DNS |

## Architecture Decision

### Why Microservices (vs Monolith)

| Factor | Microservices | Monolith |
|--------|---------------|----------|
| Independent Deployment | Each service separate | All or nothing |
| Technology Flexibility | Different stacks possible | Single stack |
| Scaling | Granular per service | Entire application |
| Team Independence | Clear boundaries | Shared codebase |
| Complexity | Higher operational | Simpler |
| Diploma Requirement | Required criterion | Not applicable |

**Decision:** Microservices architecture chosen for diploma requirement and to demonstrate enterprise patterns.

## Service Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENTS                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              v
┌─────────────────────────────────────────────────────────────────┐
│                    API GATEWAY :8000                             │
│            (Spring Cloud Gateway, JWT Filter)                    │
└─────────────────────────────────────────────────────────────────┘
        │                     │                     │
        v                     v                     v
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│ User Service  │    │Currency Service│   │Analytics Svc  │
│    :8081      │    │    :8080      │    │    :8082      │
│  (Auth/Users) │    │ (Rates/Conv)  │    │   (Trends)    │
└───────────────┘    └───────────────┘    └───────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        v                     v                     v
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│   Fixer.io    │    │Mock Service 1 │    │Mock Service 2 │
│   (External)  │    │    :8083      │    │    :8084      │
└───────────────┘    └───────────────┘    └───────────────┘
```

## Service Details

### 1. API Gateway (Port 8000)

**Technology:** Spring Cloud Gateway (Reactive)

**Responsibilities:**
- Single entry point for all clients
- JWT token validation
- Request routing to backend services
- User context propagation (X-User-Email, X-User-Roles headers)
- Correlation ID generation
- CORS handling

**Key Configuration:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://user-service:8081
          predicates:
            - Path=/api/v1/auth/**
        - id: currency-service
          uri: http://currency-service:8080
          predicates:
            - Path=/api/v1/currencies/**
        - id: analytics-service
          uri: http://analytics-service:8082
          predicates:
            - Path=/api/v1/analytics/**
```

### 2. User Service (Port 8081)

**Responsibility:** Authentication and User Management

**Endpoints:**
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/auth/register | User registration |
| POST | /api/v1/auth/login | Authentication |
| GET | /api/v1/auth/me | Current user info |
| POST | /api/v1/auth/change-password | Password change |

**Database:** users_db (PostgreSQL)

**Dependencies:** None (core service)

### 3. Currency Service (Port 8080)

**Responsibility:** Exchange Rate Management

**Endpoints:**
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/currencies | List currencies |
| POST | /api/v1/currencies | Add currency (Admin) |
| POST | /api/v1/currencies/convert | Convert amount |
| POST | /api/v1/currencies/refresh | Refresh rates (Admin) |
| CRUD | /api/v1/admin/provider-keys/** | API key management |

**Database:** currency_db (PostgreSQL)

**Dependencies:** 
- Mock Service 1 (fallback provider)
- Mock Service 2 (fallback provider)
- External APIs (Fixer, ExchangeRates, CurrencyAPI)

### 4. Analytics Service (Port 8082)

**Responsibility:** Trend Analysis

**Endpoints:**
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/analytics/trends | Currency trends |

**Database:** currency_db (read-only access)

**Dependencies:** None (reads from shared database)

### 5-6. Mock Services (Ports 8083, 8084)

**Responsibility:** Fallback Rate Providers

**Purpose:**
- High availability when external APIs fail
- Testing without external dependencies
- Development environment support

**Behavior:**
- Return realistic exchange rates
- Simulate provider response format
- Random rate variation (±10-15%)

## Communication Patterns

### Synchronous REST

All inter-service communication is synchronous HTTP/REST:

```
Client -> Gateway -> Service -> Response -> Gateway -> Client
```

**Why REST (vs Message Queue):**
- Request-response pattern fits use case
- Simpler implementation for diploma scope
- Real-time responses required for conversion

### Service-to-Service Authentication

```
┌─────────────────┐     Headers:                  ┌─────────────────┐
│   API Gateway   │────> X-User-Email: user@...   │ Backend Service │
│                 │────> X-User-Roles: ROLE_USER  │                 │
└─────────────────┘     X-Correlation-ID: uuid    └─────────────────┘
```

Backend services trust headers from Gateway (internal network).

## Data Management

### Database per Service

| Service | Database | Access |
|---------|----------|--------|
| User Service | users_db | Full (read/write) |
| Currency Service | currency_db | Full (read/write) |
| Analytics Service | currency_db | Read-only |

### Data Isolation

- No direct database access between services
- Analytics uses read-only user for currency_db
- User data never exposed to other services

## Resilience Patterns

### Fallback Chain

```
Currency Service Rate Fetch:
    │
    ├──> Fixer.io ──────> Success ──> Return rates
    │         │
    │         └──> Fail
    │               │
    ├──> ExchangeRatesAPI ──> Success ──> Return rates
    │         │
    │         └──> Fail
    │               │
    ├──> CurrencyAPI ──> Success ──> Return rates
    │         │
    │         └──> Fail
    │               │
    ├──> Mock Service 1 ──> Success ──> Return rates
    │         │
    │         └──> Fail
    │               │
    └──> Mock Service 2 ──> Success ──> Return rates
              │
              └──> Fail ──> Return error
```

### Health Checks

All services expose actuator health endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: always
```

Docker Compose waits for healthy status before starting dependent services.

## Service Configuration

### Environment Variables

```yaml
# Common to all services
JWT_SECRET: ${JWT_SECRET}
SPRING_PROFILES_ACTIVE: docker

# Currency Service specific
MOCK_SERVICE_1_URL: http://mock-service-1:8083
MOCK_SERVICE_2_URL: http://mock-service-2:8084
ENCRYPTION_MASTER_KEY: ${ENCRYPTION_MASTER_KEY}
```

### Service Discovery

Docker Compose DNS-based discovery:
- `user-service` resolves to User Service container
- `currency-service` resolves to Currency Service container
- No external service registry needed

## Deployment Dependencies

```
┌─────────────────────────────────────────────────────────────────┐
│                      Startup Order                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. PostgreSQL (users_db, currency_db)                          │
│          │                                                       │
│          v                                                       │
│  2. Mock Services (independent)                                  │
│          │                                                       │
│          v                                                       │
│  3. User Service (depends on users_db)                          │
│          │                                                       │
│          v                                                       │
│  4. Currency Service (depends on currency_db, mocks, user-svc)  │
│          │                                                       │
│          v                                                       │
│  5. Analytics Service (depends on currency_db, user-svc)        │
│          │                                                       │
│          v                                                       │
│  6. API Gateway (depends on all services)                       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Metrics

| Metric | Value |
|--------|-------|
| Total Services | 6 |
| Main Business Services | 4 |
| Mock/Fallback Services | 2 |
| Databases | 2 |
| Total Containers | 9 |
