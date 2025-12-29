# 2. Technical Implementation

This section covers the technical architecture, technology stack, and implementation details for each diploma criterion.

## Contents

- [Technology Stack](tech-stack.md)
- [Deployment Guide](deployment.md)
- **Criteria Documentation:**
  - [1. Backend](criteria/backend.md)
  - [2. Cryptography](criteria/cryptography.md)
  - [3. Database](criteria/database.md)
  - [4. Microservices](criteria/microservices.md)
  - [5. Containerization](criteria/containerization.md)
  - [6. API Documentation](criteria/api-documentation.md)
  - [7. Automated Testing](criteria/testing.md)

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENTS                                  │
│                    (REST API Consumers)                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API GATEWAY :8000                           │
│              (JWT Validation, Routing, CORS)                     │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│ User Service  │    │Currency Service│    │Analytics Svc  │
│    :8081      │    │    :8080      │    │    :8082      │
└───────────────┘    └───────────────┘    └───────────────┘
        │                    │                     │
        ▼                    ▼                     │
┌───────────────┐    ┌───────────────┐            │
│  users_db     │    │ currency_db   │◄───────────┘
│  PostgreSQL   │    │  PostgreSQL   │     (read-only)
└───────────────┘    └───────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│   Fixer.io    │    │ExchangeRates  │    │ CurrencyAPI   │
│   (External)  │    │   (External)  │    │  (External)   │
└───────────────┘    └───────────────┘    └───────────────┘
                              │
        ┌─────────────────────┴─────────────────────┐
        ▼                                           ▼
┌───────────────┐                          ┌───────────────┐
│Mock Service 1 │                          │Mock Service 2 │
│    :8083      │                          │    :8084      │
└───────────────┘                          └───────────────┘
```

## Technology Summary

| Layer | Technology | Version |
|-------|------------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.5.6 |
| API Gateway | Spring Cloud Gateway | 4.x |
| Database | PostgreSQL | 15 |
| Migrations | Liquibase | 4.x |
| Security | Spring Security + JWT | 6.x |
| Encryption | AES-256-GCM | - |
| Containers | Docker + Compose | 24.x |
| Testing | JUnit 5, Mockito, WireMock | 5.x |
| Coverage | JaCoCo | 0.8.x |
| API Docs | SpringDoc OpenAPI | 2.x |

## Criteria Summary

| # | Criterion | Status | Key Implementation |
|---|-----------|--------|-------------------|
| 1 | Back-end | Completed | Spring Boot 3.5.6, layered architecture, SOLID |
| 2 | Cryptography | Completed | JWT (HS256), BCrypt, AES-256-GCM |
| 3 | Database | Completed | PostgreSQL 15, Liquibase, optimized indexes |
| 4 | Microservices | Completed | 4 services + 2 mock, API Gateway pattern |
| 5 | Containerization | Completed | Docker Compose, 9 containers, health checks |
| 6 | API Documentation | Completed | OpenAPI 3.0, Swagger UI, 12 doc files |
| 7 | Testing (70% min) | Completed | 87% coverage, 487 tests, JUnit 5 |
