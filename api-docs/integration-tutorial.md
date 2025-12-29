# Integration Tutorial

This tutorial demonstrates a complete integration workflow with the CERPS API, from user registration through currency conversion and analytics.

## Scenario Overview

This tutorial covers the following workflow:

1. Register a new user account
2. Authenticate and obtain JWT token
3. Retrieve available currencies
4. Convert currency amounts
5. Analyze currency trends
6. Handle errors appropriately

## Prerequisites

- CERPS services running locally or on accessible server
- API client (curl, Postman, or HTTP library)
- Valid email address for registration

## Step 1: User Registration

Register a new user account with the system.

**Endpoint:** `POST /api/v1/auth/register`

**Request:**
```bash
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "developer@company.com",
    "password": "DevPass123!"
  }'
```

**Response:**
```
HTTP/1.1 201 Created

User registered successfully
```

**Validation Rules:**
- Email must be valid format and unique
- Password minimum 8 characters
- Password must contain: digit, uppercase, lowercase, special character

**Possible Errors:**
- `409 Conflict` - Email already registered
- `400 Bad Request` - Invalid email format or weak password

## Step 2: Authentication

Authenticate with credentials to receive JWT token.

**Endpoint:** `POST /api/v1/auth/login`

**Request:**
```bash
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "developer@company.com",
    "password": "DevPass123!"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkZXZlbG9wZXJAY29tcGFueS5jb20iLCJyb2xlcyI6WyJST0xFX1VTRVIiXSwiaWF0IjoxNzAzMDAwMDAwLCJleHAiOjE3MDMwODY0MDB9.signature",
  "type": "Bearer",
  "email": "developer@company.com",
  "roles": ["ROLE_USER"]
}
```

**Token Details:**
- Expires after 24 hours
- Must be included in Authorization header for protected endpoints
- Format: `Authorization: Bearer <token>`

**Store the token** for use in subsequent requests.

## Step 3: Verify Authentication

Confirm authentication status and user details.

**Endpoint:** `GET /api/v1/auth/me`

**Request:**
```bash
curl -X GET http://localhost:8000/api/v1/auth/me \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Response:**
```json
{
  "id": 15,
  "email": "developer@company.com",
  "roles": ["ROLE_USER"],
  "enabled": true,
  "createdAt": "2024-12-19T14:30:00Z"
}
```

## Step 4: Retrieve Supported Currencies

Get list of available currencies for conversion.

**Endpoint:** `GET /api/v1/currencies`

**Request:**
```bash
curl -X GET http://localhost:8000/api/v1/currencies \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Response:**
```json
[
  "AUD",
  "BYN",
  "CAD",
  "CHF",
  "CNY",
  "EUR",
  "GBP",
  "JPY",
  "NZD",
  "SEK",
  "USD"
]
```

**Note:** Currency list may vary based on system configuration.

## Step 5: Convert Currency

Convert amount from one currency to another.

**Endpoint:** `POST /api/v1/currencies/convert`

**Request:**
```bash
curl -X POST http://localhost:8000/api/v1/currencies/convert \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "amount": 1000,
    "from": "USD",
    "to": "EUR"
  }'
```

**Response:**
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

**Conversion Details:**
- Exchange rate is aggregated from multiple providers
- Median calculation ensures accuracy
- Rates updated hourly
- Conversion preserves 6 decimal places

**Edge Case - Same Currency:**
```bash
curl -X POST http://localhost:8000/api/v1/currencies/convert \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "amount": 1000,
    "from": "USD",
    "to": "USD"
  }'
```

**Response:**
```json
{
  "success": true,
  "originalAmount": 1000,
  "fromCurrency": "USD",
  "toCurrency": "USD",
  "convertedAmount": 1000,
  "exchangeRate": 1.0,
  "timestamp": "2024-12-19T14:36:00Z"
}
```

## Step 6: Analyze Currency Trends

Retrieve historical trend analysis for currency pair.

**Endpoint:** `GET /api/v1/analytics/trends`

**Request - 7 Day Trend:**
```bash
curl -X GET "http://localhost:8000/api/v1/analytics/trends?from=USD&to=EUR&period=7D" \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Response:**
```json
{
  "from": "USD",
  "to": "EUR",
  "period": "7D",
  "oldRate": 0.920000,
  "newRate": 0.924568,
  "changePercentage": 0.50,
  "startDate": "2024-12-12T14:35:00Z",
  "endDate": "2024-12-19T14:35:00Z",
  "dataPoints": 168
}
```

**Available Period Formats:**
- `7D` - 7 days
- `30D` - 30 days
- `1M` - 1 month (30 days)
- `3M` - 3 months (90 days)
- `6M` - 6 months (180 days)
- `1Y` - 1 year (365 days)

**Request - 30 Day Trend:**
```bash
curl -X GET "http://localhost:8000/api/v1/analytics/trends?from=GBP&to=USD&period=30D" \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Response:**
```json
{
  "from": "GBP",
  "to": "USD",
  "period": "30D",
  "oldRate": 1.265000,
  "newRate": 1.273400,
  "changePercentage": 0.66,
  "startDate": "2024-11-19T14:35:00Z",
  "endDate": "2024-12-19T14:35:00Z",
  "dataPoints": 720
}
```

**Trend Interpretation:**
- Positive `changePercentage`: Target currency strengthened
- Negative `changePercentage`: Target currency weakened
- `dataPoints`: Number of rate samples in period

**Access Restrictions:**
- Trends endpoint requires `ROLE_PREMIUM_USER` or `ROLE_ADMIN`
- Standard `ROLE_USER` receives `403 Forbidden`

## Step 7: Error Handling

Handle common error scenarios appropriately.

### Insufficient Data Error

When requesting trends for a period with insufficient historical data:

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/analytics/trends?from=USD&to=EUR&period=1Y" \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Response:**
```json
{
  "type": "about:blank",
  "title": "Insufficient Data",
  "status": 404,
  "detail": "Insufficient data for trend analysis. Found 100 data points, need at least 2"
}
```

### Invalid Currency Error

When using unsupported currency code:

**Request:**
```bash
curl -X POST http://localhost:8000/api/v1/currencies/convert \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "amount": 100,
    "from": "USD",
    "to": "XYZ"
  }'
```

**Response:**
```json
{
  "type": "about:blank",
  "title": "Invalid Request",
  "status": 400,
  "detail": "to: Invalid currency code. Must be a valid ISO 4217 code (e.g., USD, EUR)"
}
```

### Expired Token Error

When JWT token has expired:

**Request:**
```bash
curl -X GET http://localhost:8000/api/v1/currencies \
  -H "Authorization: Bearer <expired-token>"
```

**Response:**
```json
{
  "error": "Invalid JWT token",
  "status": 401
}
```

**Recovery:** Re-authenticate using login endpoint to obtain new token.

## Complete Integration Example

### Using curl

```bash
#!/bin/bash

BASE_URL="http://localhost:8000"
EMAIL="developer@company.com"
PASSWORD="DevPass123!"

# Register
curl -X POST "${BASE_URL}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}"

# Login and extract token
TOKEN=$(curl -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}" \
  | jq -r '.token')

# Get currencies
curl -X GET "${BASE_URL}/api/v1/currencies" \
  -H "Authorization: Bearer ${TOKEN}"

# Convert currency
curl -X POST "${BASE_URL}/api/v1/currencies/convert" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"amount":1000,"from":"USD","to":"EUR"}'

# Get trends
curl -X GET "${BASE_URL}/api/v1/analytics/trends?from=USD&to=EUR&period=7D" \
  -H "Authorization: Bearer ${TOKEN}"
```

### Using Python

```python
import requests

BASE_URL = "http://localhost:8000"

# Register
response = requests.post(
    f"{BASE_URL}/api/v1/auth/register",
    json={
        "email": "developer@company.com",
        "password": "DevPass123!"
    }
)

# Login
response = requests.post(
    f"{BASE_URL}/api/v1/auth/login",
    json={
        "email": "developer@company.com",
        "password": "DevPass123!"
    }
)
token = response.json()["token"]

# Set authorization header
headers = {"Authorization": f"Bearer {token}"}

# Get currencies
response = requests.get(
    f"{BASE_URL}/api/v1/currencies",
    headers=headers
)
currencies = response.json()

# Convert currency
response = requests.post(
    f"{BASE_URL}/api/v1/currencies/convert",
    headers=headers,
    json={
        "amount": 1000,
        "from": "USD",
        "to": "EUR"
    }
)
conversion = response.json()

# Get trends
response = requests.get(
    f"{BASE_URL}/api/v1/analytics/trends",
    headers=headers,
    params={
        "from": "USD",
        "to": "EUR",
        "period": "7D"
    }
)
trends = response.json()
```

### Using Java

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CerpsIntegration {
    
    private static final String BASE_URL = "http://localhost:8000";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) throws Exception {
        // Register
        String registerPayload = """
            {
                "email": "developer@company.com",
                "password": "DevPass123!"
            }
            """;
        
        HttpRequest registerRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/auth/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registerPayload))
            .build();
        
        client.send(registerRequest, HttpResponse.BodyHandlers.ofString());
        
        // Login
        String loginPayload = """
            {
                "email": "developer@company.com",
                "password": "DevPass123!"
            }
            """;
        
        HttpRequest loginRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginPayload))
            .build();
        
        HttpResponse<String> loginResponse = client.send(
            loginRequest, 
            HttpResponse.BodyHandlers.ofString()
        );
        
        String token = mapper.readTree(loginResponse.body())
            .get("token").asText();
        
        // Convert currency
        String convertPayload = """
            {
                "amount": 1000,
                "from": "USD",
                "to": "EUR"
            }
            """;
        
        HttpRequest convertRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/currencies/convert"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.ofString(convertPayload))
            .build();
        
        HttpResponse<String> convertResponse = client.send(
            convertRequest,
            HttpResponse.BodyHandlers.ofString()
        );
        
        System.out.println(convertResponse.body());
    }
}
```

## Best Practices

### Token Management

- Store tokens securely (not in source code or version control)
- Implement token refresh logic before expiration
- Clear tokens on logout
- Use environment variables for credentials

### Error Handling

- Always check HTTP status codes
- Parse error response bodies for details
- Implement retry logic for transient failures
- Log errors with correlation IDs when available

### Rate Limiting

- Implement exponential backoff for repeated requests
- Cache currency lists to reduce API calls
- Batch conversion requests when possible

### Security

- Use HTTPS in production environments
- Validate SSL certificates
- Never log or expose JWT tokens
- Implement request timeouts

## Next Steps

For detailed endpoint specifications, refer to:
- [User Service API Reference](api-reference-user.md)
- [Currency Service API Reference](api-reference-currency.md)
- [Analytics Service API Reference](api-reference-analytics.md)

For authentication implementation details, see:
- [Authentication Guide](authentication.md)

For error handling strategies, see:
- [Error Handling Guide](error-handling.md)
