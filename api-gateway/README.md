# API Gateway

API Gateway for CERPS (Currency Exchange Rate Provider System) microservices architecture.

## Overview

This gateway serves as the single entry point for all client requests to the CERPS system. It handles:
- Request routing to appropriate microservices
- JWT token validation
- User authentication information propagation to downstream services

## Features

- **Centralized routing**: Routes requests to `user-service`, `currency-service`, and `analytics-service`
- **JWT validation**: Validates JWT tokens and extracts user information
- **Header propagation**: Forwards `X-User-Email` and `X-User-Roles` headers to downstream services
- **Public endpoints**: Allows unauthenticated access to login, registration, and swagger endpoints

## Routes

| Path Pattern | Target Service | Port |
|--------------|----------------|------|
| `/api/v1/auth/**` | user-service | 8081 |
| `/api/v1/currencies/**` | currency-service | 8080 |
| `/api/v1/analytics/**` | analytics-service | 8082 |

## Public Endpoints (No Authentication Required)

- `/api/v1/auth/register`
- `/api/v1/auth/login`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/actuator/**`

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_SECRET` | Base64-encoded secret key for JWT validation | Required |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `default` |

### Running Locally

```bash
mvn spring-boot:run -pl api-gateway
```

The gateway will be available at `http://localhost:8000`.

### Running with Docker

```bash
docker-compose up api-gateway
```

## Architecture

```
Client Request
      │
      ▼
┌─────────────────┐
│   API Gateway   │ (Port 8000)
│  JWT Validation │
└────────┬────────┘
         │
         ├─────────────────────────────────────┐
         │                                     │
         ▼                                     ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  user-service   │  │currency-service │  │analytics-service│
│   (Port 8081)   │  │   (Port 8080)   │  │   (Port 8082)   │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

## Headers Propagated to Downstream Services

When a valid JWT token is provided, the gateway extracts user information and forwards it:

- `X-User-Email`: User's email address from the JWT subject
- `X-User-Roles`: Comma-separated list of user roles from the JWT claims

## Security

The gateway validates JWT tokens using the same secret key as the `user-service`. Downstream services trust the headers provided by the gateway and do not perform additional JWT validation.

