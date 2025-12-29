# CERPS API Documentation

Currency Exchange Rate Provider System

## Overview

CERPS is a microservices-based system that provides real-time currency exchange rates, conversion services, and analytics. The system aggregates exchange rates from multiple external providers (Fixer.io, ExchangeRatesAPI, CurrencyAPI) and provides a unified API for currency operations.

### Key Features

- Currency conversion between supported currencies
- Trend analysis over configurable time periods
- JWT-based authentication and authorization
- High availability through fallback mechanisms and median calculation
- Real-time exchange rate aggregation

## System Architecture

CERPS consists of five microservices:

| Service | Port | Responsibility |
|---------|------|----------------|
| API Gateway | 8000 | Single entry point, JWT validation, request routing |
| User Service | 8081 | User authentication and authorization |
| Currency Service | 8080 | Exchange rate management and conversion |
| Analytics Service | 8082 | Trend analysis and historical data |
| Mock Services | 8083-8084 | Fallback providers for high availability |

### Technology Stack

- Java 21
- Spring Boot 3.5.6
- PostgreSQL (2 databases)
- Docker and Docker Compose
- JWT Authentication
- OpenAPI/Swagger

## Documentation Structure

### Getting Started
- [Quick Start Guide](getting-started.md) - Initial setup and first API calls
- [Integration Tutorial](integration-tutorial.md) - Step-by-step integration workflow

### Architecture
- [System Architecture](architecture.md) - System design and component interaction
- [Authentication](authentication.md) - JWT authentication implementation
- [Error Handling](error-handling.md) - Error codes and response formats

### API Reference
- [User Service API](api-reference-user.md) - User management endpoints
- [Currency Service API](api-reference-currency.md) - Exchange rate endpoints
- [Analytics Service API](api-reference-analytics.md) - Analytics endpoints
- [API Gateway Routes](api-gateway-routes.md) - Gateway routing configuration

### Additional Resources
- [Code Examples](examples.md) - Implementation examples
- [Best Practices](best-practices.md) - Recommended usage patterns
- [OpenAPI Specifications](schemas/) - Machine-readable API specifications

## Base URLs

```
API Gateway:     http://localhost:8000
User Service:    http://localhost:8081
Currency Service: http://localhost:8080
Analytics Service: http://localhost:8082
```

## Common Endpoints

```
POST   /api/v1/auth/register       - Register new user
POST   /api/v1/auth/login          - Authenticate and receive JWT token
POST   /api/v1/currencies/convert  - Convert currency amount
GET    /api/v1/analytics/trends    - Retrieve currency trends
```

## Authentication

All authenticated endpoints require a JWT token in the Authorization header:

```http
Authorization: Bearer <jwt-token>
```

Refer to the [Authentication Guide](authentication.md) for implementation details.

## Swagger UI Access

Interactive API documentation is available for each service:

- Gateway: http://localhost:8000/swagger-ui.html
- User Service: http://localhost:8081/swagger-ui.html
- Currency Service: http://localhost:8080/swagger-ui.html
- Analytics Service: http://localhost:8082/swagger-ui.html

## System Requirements

- Java 21 or higher
- Docker and Docker Compose
- PostgreSQL 15 or higher
- Maven 3.9 or higher

## Next Steps

For new users, begin with the [Quick Start Guide](getting-started.md) to set up the system and make your first API calls.

For integration scenarios, refer to the [Integration Tutorial](integration-tutorial.md) for a complete workflow example.

For detailed endpoint specifications, consult the relevant API Reference documentation.
