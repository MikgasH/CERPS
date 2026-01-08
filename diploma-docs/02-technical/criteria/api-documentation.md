# Criterion 6: API Documentation

## Overview

| Aspect | Details |
|--------|---------|
| **Criterion** | Comprehensive API Documentation |
| **Format** | OpenAPI 3.0 + Markdown |
| **Interactive** | Swagger UI per service |
| **Documentation Files** | 12 Markdown files |
| **Code Examples** | Java, Python, JavaScript |

## Documentation Strategy

### Why OpenAPI + Markdown

| Factor | OpenAPI Only | Markdown Only | **Combined** |
|--------|--------------|---------------|--------------|
| Machine-readable | Yes | No | Yes |
| Human-readable | Technical | Yes | Yes |
| Interactive testing | Swagger UI | No | Yes |
| Tutorials/Guides | Limited | Yes | Yes |
| Code examples | Limited | Yes | Yes |
| Version control | Yes | Yes | Yes |

**Decision:** Combined approach provides both machine-readable specs and human-friendly documentation.

## Documentation Structure

```
docs/
├── README.md                    # Entry point, navigation
├── getting-started.md           # Quick start guide
├── integration-tutorial.md      # Step-by-step integration
├── architecture.md              # System design
├── authentication.md            # JWT implementation
├── error-handling.md            # Error codes catalog
├── api-reference-user.md        # User Service API
├── api-reference-currency.md    # Currency Service API
├── api-reference-analytics.md   # Analytics Service API
├── api-gateway-routes.md        # Gateway routing
├── examples.md                  # Code examples
├── best-practices.md            # Usage recommendations
└── schemas/
    ├── gateway-openapi.yaml     # Gateway OpenAPI spec
    └── user-service-openapi.yaml # User Service OpenAPI spec
```

## OpenAPI Specifications

### Auto-generated with SpringDoc

```java
// Dependency
// implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'

@Operation(
    summary = "Convert currency",
    description = "Convert amount from one currency to another"
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Conversion successful"),
    @ApiResponse(responseCode = "400", description = "Invalid currency code"),
    @ApiResponse(responseCode = "401", description = "Unauthorized")
})
@PostMapping("/convert")
public ResponseEntity<ConversionResponse> convert(
    @Valid @RequestBody ConversionRequest request) {
    // ...
}
```

### OpenAPI Spec Example

```yaml
openapi: 3.1.0
info:
  title: User Management Service API
  description: REST API for user authentication and authorization
  version: 1.0.0
servers:
  - url: http://localhost:8000
    description: API Gateway
security:
  - Bearer Authentication: []
paths:
  /api/v1/auth/register:
    post:
      tags:
        - Authentication
      summary: Register a new user
      requestBody:
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/RegisterRequest"
      responses:
        "201":
          description: User registered successfully
        "409":
          description: Email already exists
```

## Swagger UI Access

| Service | URL | Purpose |
|---------|-----|---------|
| API Gateway | http://localhost:8000/swagger-ui.html | Full API explorer |
| User Service | http://localhost:8081/swagger-ui.html | Auth endpoints |
| Currency Service | http://localhost:8080/swagger-ui.html | Currency endpoints |
| Analytics Service | http://localhost:8082/swagger-ui.html | Trends endpoints |

## API Reference Documentation

### Endpoint Documentation Format

Each endpoint documented with:
- HTTP method and path
- Authentication requirements
- Request body schema
- Query/Path parameters
- Response codes and bodies
- Example requests (curl)
- Error responses

### Example: Currency Conversion

```markdown
### Convert Currency

**Endpoint:** `POST /api/v1/currencies/convert`

**Authentication:** Required (Bearer token)

**Roles:** USER, PREMIUM_USER, ADMIN

**Request Body:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| amount | number | Yes | Positive decimal value |
| from | string | Yes | ISO 4217 currency code |
| to | string | Yes | ISO 4217 currency code |

**Request Example:**
```bash
curl -X POST http://localhost:8000/api/v1/currencies/convert \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"amount": 1000, "from": "USD", "to": "EUR"}'
```

**Success Response (200):**
```json
{
  "success": true,
  "originalAmount": 1000,
  "fromCurrency": "USD",
  "toCurrency": "EUR",
  "convertedAmount": 924.567890,
  "exchangeRate": 0.924568,
  "timestamp": "2024-12-19T14:35:00Z"
}
```
```

## Code Examples

### Multi-language Support

| Language | Use Case |
|----------|----------|
| Java | Enterprise integration |
| Python | Data science, scripts |
| JavaScript | Web applications |

### Python Example

```python
import requests

class CERPSClient:
    def __init__(self, base_url):
        self.base_url = base_url
        self.token = None
    
    def login(self, email, password):
        response = requests.post(
            f"{self.base_url}/api/v1/auth/login",
            json={"email": email, "password": password}
        )
        response.raise_for_status()
        self.token = response.json()["token"]
        return self.token
    
    def convert_currency(self, amount, from_curr, to_curr):
        response = requests.post(
            f"{self.base_url}/api/v1/currencies/convert",
            headers={"Authorization": f"Bearer {self.token}"},
            json={"amount": amount, "from": from_curr, "to": to_curr}
        )
        response.raise_for_status()
        return response.json()

# Usage
client = CERPSClient("http://localhost:8000")
client.login("user@example.com", "SecurePass123!")
result = client.convert_currency(1000, "USD", "EUR")
print(f"Converted: {result['convertedAmount']} EUR")
```

## Error Documentation

### Standard Error Format (RFC 7807)

```json
{
  "type": "about:blank",
  "title": "Validation error",
  "status": 400,
  "detail": "amount: Amount must be greater than 0"
}
```

### Error Catalog

| Status | Title | Common Causes |
|--------|-------|---------------|
| 400 | Bad Request | Validation failure, invalid format |
| 401 | Unauthorized | Missing/invalid token |
| 403 | Forbidden | Insufficient role |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource |
| 503 | Service Unavailable | All providers failed |

## Documentation Guidelines

### Writing Standards

- Clear, concise language
- Active voice
- Consistent formatting
- Code blocks with syntax highlighting
- Tables for structured data

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Files | lowercase-with-hyphens | api-reference-user.md |
| Endpoints | UPPERCASE method | POST /api/v1/auth/login |
| Parameters | camelCase | fromCurrency |

### Version Control

- Documentation in same repository as code
- Updated with code changes
- Reviewed in pull requests

## OpenAPI Specifications

OpenAPI 3.0 specifications are provided for all services:

| Service | File | Endpoints |
|---------|------|-----------|
| User Service | api-schemas/user-service-openapi.yaml | 5 |
| Currency Service | api-schemas/currency-service-openapi.yaml | 8 |
| Analytics Service | api-schemas/analytics-service-openapi.yaml | 1 |

## Completeness Checklist

| Category | Status |
|----------|--------|
| Quick start guide | Complete |
| Installation steps | Complete |
| First API call example | Complete |
| User Service endpoints | Complete |
| Currency Service endpoints | Complete |
| Analytics Service endpoints | Complete |
| Gateway routes | Complete |
| Authentication guide | Complete |
| Error handling guide | Complete |
| Best practices | Complete |
| OpenAPI specs | Complete |
| Architecture diagram | Complete |
| Java examples | Complete |
| Python examples | Complete |
| JavaScript examples | Complete |

## Metrics

| Metric | Value |
|--------|-------|
| Documentation files | 12 |
| OpenAPI specifications | 3 |
| Endpoints documented | 14 |
| Code examples | 20+ |
| Languages covered | 3 |
