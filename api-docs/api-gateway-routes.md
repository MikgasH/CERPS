# API Gateway Routes

This document describes the routing configuration and behavior of the CERPS API Gateway.

## Overview

The API Gateway serves as the single entry point for all client requests, routing them to appropriate microservices.

**Gateway URL:** `http://localhost:8000`

## Routing Configuration

### User Service Routes

**Target Service:** User Service (http://localhost:8081)

| Gateway Path | Method | Description |
|--------------|--------|-------------|
| /api/v1/auth/register | POST | User registration |
| /api/v1/auth/login | POST | User authentication |
| /api/v1/auth/me | GET | Get current user |
| /api/v1/auth/change-password | POST | Change password |

### Currency Service Routes

**Target Service:** Currency Service (http://localhost:8080)

| Gateway Path | Method | Description |
|--------------|--------|-------------|
| /api/v1/currencies | GET | List supported currencies |
| /api/v1/currencies | POST | Add currency (Admin) |
| /api/v1/currencies/convert | POST | Convert currency |
| /api/v1/currencies/refresh | POST | Refresh rates (Admin) |
| /api/v1/admin/provider-keys | POST | Create provider key (Admin) |
| /api/v1/admin/provider-keys | GET | List provider keys (Admin) |
| /api/v1/admin/provider-keys/{id} | GET | Get provider key (Admin) |
| /api/v1/admin/provider-keys/{id} | PUT | Update provider key (Admin) |
| /api/v1/admin/provider-keys/{id} | DELETE | Deactivate provider key (Admin) |
| /api/v1/admin/provider-keys/{id}/rotate | POST | Rotate provider key (Admin) |

### Analytics Service Routes

**Target Service:** Analytics Service (http://localhost:8082)

| Gateway Path | Method | Description |
|--------------|--------|-------------|
| /api/v1/analytics/trends | GET | Get currency trends (Premium/Admin) |

## Routing Logic

1. Request arrives at Gateway
2. Path matching against configured routes
3. Request forwarded to target service
4. Response returned to client

## Error Handling

**404 Not Found** - No matching route  
**503 Service Unavailable** - Backend service down  
**504 Gateway Timeout** - Backend service timeout

## Health Checks

**Endpoint:** `GET /actuator/health`

**Response:**
```json
{
  "status": "UP"
}
```

## Related Documentation

- [Architecture](architecture.md) - System architecture
- [API Reference - User Service](api-reference-user.md)
- [API Reference - Currency Service](api-reference-currency.md)
- [API Reference - Analytics Service](api-reference-analytics.md)
