# Currency Service API Reference

This document provides detailed specifications for all Currency Service endpoints.

## Base Information

**Base URL:** `http://localhost:8080`  
**Via Gateway:** `http://localhost:8000`

**Service Responsibilities:**
- Exchange rate aggregation from multiple providers
- Currency conversion calculations
- Supported currency management
- API provider key management (admin only)

## Authentication

All endpoints require JWT authentication except where noted:

```http
Authorization: Bearer <jwt-token>
```

## Endpoints

### Get Supported Currencies

Retrieve list of all supported currency codes.

**Endpoint:** `GET /api/v1/currencies`

**Authentication:** Required

**Roles:** USER, PREMIUM_USER, ADMIN

**Request Example:**
```bash
curl -X GET http://localhost:8000/api/v1/currencies \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Success Response:**

**Status Code:** `200 OK`

**Body:**
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

**Response Format:** Array of ISO 4217 currency codes

**Error Responses:**

**401 Unauthorized** - Missing or invalid token
```json
{
  "error": "Invalid JWT token",
  "status": 401
}
```

---

### Add Currency (Admin)

Add a new supported currency to the system.

**Endpoint:** `POST /api/v1/currencies`

**Authentication:** Required

**Roles:** ADMIN only

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| currency | string | Yes | ISO 4217 currency code (3 letters) |

**Request Example:**
```bash
curl -X POST "http://localhost:8000/api/v1/currencies?currency=NOK" \
  -H "Authorization: Bearer <admin-jwt-token>"
```

**Success Response:**

**Status Code:** `200 OK`

**Body:**
```
Currency NOK added successfully
```

**Error Responses:**

**400 Bad Request** - Invalid currency code
```json
{
  "type": "about:blank",
  "title": "Invalid currency code",
  "status": 400,
  "detail": "Invalid currency code: XYZ"
}
```

**400 Bad Request** - Missing parameter
```json
{
  "type": "about:blank",
  "title": "Missing required parameter",
  "status": 400,
  "detail": "Required parameter 'currency' is missing"
}
```

**403 Forbidden** - Insufficient permissions
```json
{
  "type": "about:blank",
  "title": "Access denied",
  "status": 403,
  "detail": "You don't have permission to access this resource"
}
```

---

### Convert Currency

Convert amount from one currency to another.

**Endpoint:** `POST /api/v1/currencies/convert`

**Authentication:** Required

**Roles:** USER, PREMIUM_USER, ADMIN

**Request Body:**
```json
{
  "amount": "number",
  "from": "string",
  "to": "string"
}
```

**Request Example:**
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

**Validation Rules:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| amount | number | Yes | Positive decimal value |
| from | string | Yes | Valid ISO 4217 code, must be supported |
| to | string | Yes | Valid ISO 4217 code, must be supported |

**Success Response:**

**Status Code:** `200 OK`

**Body:**
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

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| success | boolean | Operation success indicator |
| originalAmount | number | Input amount |
| fromCurrency | string | Source currency code |
| toCurrency | string | Target currency code |
| convertedAmount | number | Calculated result (6 decimal places) |
| exchangeRate | number | Applied exchange rate |
| timestamp | string | Conversion timestamp (ISO 8601) |

**Exchange Rate Calculation:**
- Aggregated from multiple providers (Fixer.io, ExchangeRatesAPI, CurrencyAPI)
- Median calculation for accuracy
- Fallback to mock services if providers unavailable
- Cached for 1 hour

**Special Case - Same Currency:**
```json
{
  "amount": 1000,
  "from": "USD",
  "to": "USD"
}
```

Response:
```json
{
  "success": true,
  "originalAmount": 1000,
  "fromCurrency": "USD",
  "toCurrency": "USD",
  "convertedAmount": 1000,
  "exchangeRate": 1.0,
  "timestamp": "2024-12-19T14:35:00Z"
}
```

**Error Responses:**

**400 Bad Request** - Invalid amount
```json
{
  "type": "about:blank",
  "title": "Validation error",
  "status": 400,
  "detail": "amount: Amount must be greater than 0"
}
```

**400 Bad Request** - Invalid currency
```json
{
  "type": "about:blank",
  "title": "Invalid Request",
  "status": 400,
  "detail": "to: Invalid currency code. Must be a valid ISO 4217 code (e.g., USD, EUR)"
}
```

**400 Bad Request** - Unsupported currency
```json
{
  "type": "about:blank",
  "title": "Currency Not Supported",
  "status": 400,
  "detail": "Currency 'BTC' is not supported. Available currencies: USD, EUR, GBP, JPY, CHF, CAD, AUD, CNY, SEK, NZD"
}
```

**503 Service Unavailable** - Rate not available
```json
{
  "type": "about:blank",
  "title": "Exchange rate unavailable",
  "status": 503,
  "detail": "Exchange rate not available for USD -> EUR"
}
```

---

### Refresh Exchange Rates (Admin)

Manually trigger exchange rate update from external providers.

**Endpoint:** `POST /api/v1/currencies/refresh`

**Authentication:** Required

**Roles:** ADMIN only

**Request Example:**
```bash
curl -X POST http://localhost:8000/api/v1/currencies/refresh \
  -H "Authorization: Bearer <admin-jwt-token>"
```

**Success Response:**

**Status Code:** `200 OK`

**Body:**
```
Exchange rates updated successfully
```

**Process:**
1. Fetch rates from all configured external providers
2. Calculate median rates across providers
3. Store aggregated rates in database
4. Update in-memory cache
5. Clear old rates (>395 days)

**Error Responses:**

**403 Forbidden** - Insufficient permissions
```json
{
  "type": "about:blank",
  "title": "Access denied",
  "status": 403,
  "detail": "You don't have permission to access this resource"
}
```

**503 Service Unavailable** - All providers failed
```json
{
  "type": "about:blank",
  "title": "All providers failed",
  "status": 503,
  "detail": "All exchange rate providers failed: Fixer.io, ExchangeRatesAPI, CurrencyAPI, MockService1, MockService2"
}
```

---

## Admin Endpoints - Provider Key Management

### Create Provider Key

Create and encrypt API key for external provider.

**Endpoint:** `POST /api/v1/admin/provider-keys`

**Authentication:** Required

**Roles:** ADMIN only

**Request Body:**
```json
{
  "providerName": "string",
  "apiKey": "string"
}
```

**Request Example:**
```bash
curl -X POST http://localhost:8000/api/v1/admin/provider-keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-jwt-token>" \
  -d '{
    "providerName": "Fixer.io",
    "apiKey": "your-fixer-api-key-here"
  }'
```

**Validation Rules:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| providerName | string | Yes | 1-50 characters |
| apiKey | string | Yes | Non-empty |

**Success Response:**

**Status Code:** `201 Created`

**Body:**
```json
{
  "id": 1,
  "providerName": "Fixer.io",
  "active": true,
  "createdAt": "2024-12-19T14:40:00Z",
  "updatedAt": "2024-12-19T14:40:00Z"
}
```

**Security Note:** API key encrypted with AES-256-GCM before storage.

---

### Get All Provider Keys

Retrieve all active provider API keys metadata.

**Endpoint:** `GET /api/v1/admin/provider-keys`

**Authentication:** Required

**Roles:** ADMIN only

**Request Example:**
```bash
curl -X GET http://localhost:8000/api/v1/admin/provider-keys \
  -H "Authorization: Bearer <admin-jwt-token>"
```

**Success Response:**

**Status Code:** `200 OK`

**Body:**
```json
[
  {
    "id": 1,
    "providerName": "Fixer.io",
    "active": true,
    "createdAt": "2024-12-19T14:40:00Z",
    "updatedAt": "2024-12-19T14:40:00Z"
  },
  {
    "id": 2,
    "providerName": "CurrencyAPI",
    "active": true,
    "createdAt": "2024-12-19T15:00:00Z",
    "updatedAt": "2024-12-19T15:00:00Z"
  }
]
```

**Note:** Actual API keys are not returned, only metadata.

---

### Get Provider Key by ID

Retrieve specific provider key metadata.

**Endpoint:** `GET /api/v1/admin/provider-keys/{id}`

**Authentication:** Required

**Roles:** ADMIN only

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Provider key ID |

**Request Example:**
```bash
curl -X GET http://localhost:8000/api/v1/admin/provider-keys/1 \
  -H "Authorization: Bearer <admin-jwt-token>"
```

**Success Response:**

**Status Code:** `200 OK`

**Body:**
```json
{
  "id": 1,
  "providerName": "Fixer.io",
  "active": true,
  "createdAt": "2024-12-19T14:40:00Z",
  "updatedAt": "2024-12-19T14:40:00Z"
}
```

**Error Response:**

**404 Not Found** - Key not found
```json
{
  "type": "about:blank",
  "title": "Provider key not found",
  "status": 404,
  "detail": "Provider key not found with id: 999"
}
```

---

### Update Provider Key

Update API key for existing provider.

**Endpoint:** `PUT /api/v1/admin/provider-keys/{id}`

**Authentication:** Required

**Roles:** ADMIN only

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Provider key ID |

**Request Body:**
```json
{
  "apiKey": "string"
}
```

**Request Example:**
```bash
curl -X PUT http://localhost:8000/api/v1/admin/provider-keys/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-jwt-token>" \
  -d '{
    "apiKey": "new-api-key-value"
  }'
```

**Success Response:**

**Status Code:** `200 OK`

**Body:**
```json
{
  "id": 1,
  "providerName": "Fixer.io",
  "active": true,
  "createdAt": "2024-12-19T14:40:00Z",
  "updatedAt": "2024-12-19T15:30:00Z"
}
```

---

### Deactivate Provider Key

Soft delete provider key by setting active status to false.

**Endpoint:** `DELETE /api/v1/admin/provider-keys/{id}`

**Authentication:** Required

**Roles:** ADMIN only

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Provider key ID |

**Request Example:**
```bash
curl -X DELETE http://localhost:8000/api/v1/admin/provider-keys/1 \
  -H "Authorization: Bearer <admin-jwt-token>"
```

**Success Response:**

**Status Code:** `204 No Content`

**Note:** Key remains in database but marked as inactive.

---

### Rotate Provider Key

Replace existing API key with new one.

**Endpoint:** `POST /api/v1/admin/provider-keys/{id}/rotate`

**Authentication:** Required

**Roles:** ADMIN only

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | integer | Provider key ID |

**Request Body:**
```json
{
  "apiKey": "string"
}
```

**Request Example:**
```bash
curl -X POST http://localhost:8000/api/v1/admin/provider-keys/1/rotate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-jwt-token>" \
  -d '{
    "apiKey": "rotated-api-key-value"
  }'
```

**Success Response:**

**Status Code:** `200 OK`

**Body:**
```json
{
  "id": 1,
  "providerName": "Fixer.io",
  "active": true,
  "createdAt": "2024-12-19T14:40:00Z",
  "updatedAt": "2024-12-19T16:00:00Z"
}
```

---

## Data Models

### ConversionRequest

```json
{
  "amount": "number (positive)",
  "from": "string (ISO 4217, 3 letters)",
  "to": "string (ISO 4217, 3 letters)"
}
```

### ConversionResponse

```json
{
  "success": "boolean",
  "originalAmount": "number",
  "fromCurrency": "string",
  "toCurrency": "string",
  "convertedAmount": "number (6 decimals)",
  "exchangeRate": "number (6 decimals)",
  "timestamp": "string (ISO 8601)"
}
```

### ProviderKeyResponse

```json
{
  "id": "integer",
  "providerName": "string",
  "active": "boolean",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

---

## Exchange Rate Mechanism

### Provider Aggregation

The system aggregates rates from multiple providers:

1. **Fixer.io** - Primary provider
2. **ExchangeRatesAPI** - Secondary provider
3. **CurrencyAPI** - Tertiary provider
4. **Mock Service 1** - Fallback provider
5. **Mock Service 2** - Fallback provider

### Rate Calculation Process

```
1. Fetch rates from all available providers
2. Filter out failed responses
3. Normalize all rates to EUR base currency
4. Calculate median rate for each currency pair
5. Store aggregated rates in database
6. Update in-memory cache
```

### Median Calculation Example

```
Provider 1: 1.10 USD/EUR
Provider 2: 1.12 USD/EUR
Provider 3: 1.11 USD/EUR

Sorted: [1.10, 1.11, 1.12]
Median: 1.11 USD/EUR (middle value)
```

Benefits:
- Eliminates outlier rates
- More accurate than single provider
- Reduces impact of provider errors

### Caching Strategy

**In-Memory Cache:**
- TTL: 1 hour
- Key format: `{fromCurrency}_{toCurrency}`
- Automatic eviction on expiration

**Database Storage:**
- All rates stored with timestamps
- Historical data preserved
- Automatic cleanup (>395 days)

### Fallback Mechanism

```
1. Try real providers (Fixer, ExchangeRates, CurrencyAPI)
2. If all fail, use mock services
3. If mock services fail, return 503 error
```

---

## Currency Codes

### Supported ISO 4217 Codes

| Code | Currency | Symbol |
|------|----------|--------|
| AUD | Australian Dollar | $ |
| BYN | Belarusian Ruble | Br |
| CAD | Canadian Dollar | $ |
| CHF | Swiss Franc | Fr |
| CNY | Chinese Yuan | ¥ |
| EUR | Euro | € |
| GBP | British Pound | £ |
| JPY | Japanese Yen | ¥ |
| NZD | New Zealand Dollar | $ |
| SEK | Swedish Krona | kr |
| USD | US Dollar | $ |

**Note:** Currency list can be extended by administrators using the Add Currency endpoint.

---

## Rate Limits

Currently not implemented. Recommended limits for production:

- Currency list: 1000 requests per minute per user
- Currency conversion: 100 requests per minute per user
- Rate refresh: 1 request per minute (admin only)
- Provider key operations: 10 requests per minute (admin only)

---

## Code Examples

### Java - Currency Conversion

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class CurrencyServiceClient {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String baseUrl = "http://localhost:8000";
    private final String token;
    
    public CurrencyServiceClient(String token) {
        this.token = token;
    }
    
    public ConversionResponse convertCurrency(
            double amount, String from, String to) throws Exception {
        
        String payload = String.format(
            "{\"amount\":%f,\"from\":\"%s\",\"to\":\"%s\"}",
            amount, from, to
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/currencies/convert"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        if (response.statusCode() == 200) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                response.body(),
                ConversionResponse.class
            );
        } else {
            throw new RuntimeException(
                "Conversion failed: " + response.body()
            );
        }
    }
}
```

### Python - Currency Conversion

```python
import requests

class CurrencyServiceClient:
    def __init__(self, base_url, token):
        self.base_url = base_url
        self.token = token
    
    def convert_currency(self, amount, from_currency, to_currency):
        headers = {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json"
        }
        
        payload = {
            "amount": amount,
            "from": from_currency,
            "to": to_currency
        }
        
        response = requests.post(
            f"{self.base_url}/api/v1/currencies/convert",
            headers=headers,
            json=payload
        )
        response.raise_for_status()
        return response.json()

# Usage
client = CurrencyServiceClient("http://localhost:8000", token)
result = client.convert_currency(1000, "USD", "EUR")
print(f"Converted: {result['convertedAmount']} EUR")
```

### JavaScript - Currency Conversion

```javascript
class CurrencyServiceClient {
  constructor(baseUrl, token) {
    this.baseUrl = baseUrl;
    this.token = token;
  }
  
  async convertCurrency(amount, from, to) {
    const response = await fetch(
      `${this.baseUrl}/api/v1/currencies/convert`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({amount, from, to})
      }
    );
    
    if (!response.ok) {
      throw new Error(`Conversion failed: ${response.status}`);
    }
    
    return await response.json();
  }
}

// Usage
const client = new CurrencyServiceClient('http://localhost:8000', token);
const result = await client.convertCurrency(1000, 'USD', 'EUR');
console.log(`Converted: ${result.convertedAmount} EUR`);
```

---

## Health Check

**Endpoint:** `GET /actuator/health`

**Authentication:** Not required

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

---

## OpenAPI Specification

Interactive API documentation available at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

---

## Related Documentation

- [Integration Tutorial](integration-tutorial.md) - Complete workflow examples
- [Architecture](architecture.md) - System design and rate aggregation
- [Error Handling](error-handling.md) - Error codes and recovery strategies
