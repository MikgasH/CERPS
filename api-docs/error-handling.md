# Error Handling Guide

This document describes error responses, status codes, and error handling strategies for the CERPS API.

## Error Response Format

All error responses follow RFC 7807 Problem Details format:

```json
{
  "type": "about:blank",
  "title": "Error Title",
  "status": 400,
  "detail": "Detailed error message"
}
```

**Fields:**
- `type`: URI reference identifying the problem type
- `title`: Short, human-readable summary
- `status`: HTTP status code
- `detail`: Explanation specific to this occurrence

## HTTP Status Codes

### 2xx Success

**200 OK**
- Request succeeded
- Response body contains requested data

**201 Created**
- Resource successfully created
- Typically returned from registration endpoint

**204 No Content**
- Request succeeded
- No response body
- Typically returned from delete operations

### 4xx Client Errors

**400 Bad Request**
- Invalid request syntax
- Validation failure
- Missing required fields
- Invalid data format

**401 Unauthorized**
- Authentication required
- Invalid or missing JWT token
- Token expired

**403 Forbidden**
- Valid authentication
- Insufficient permissions
- User lacks required role

**404 Not Found**
- Resource does not exist
- Endpoint not found
- Insufficient data for requested operation

**409 Conflict**
- Request conflicts with current state
- Typically duplicate resource creation

### 5xx Server Errors

**500 Internal Server Error**
- Unexpected server condition
- Unhandled exception

**502 Bad Gateway**
- Invalid response from upstream server
- External API failure

**503 Service Unavailable**
- Service temporarily unavailable
- All providers failed
- Rate limit exceeded

## Error Categories

### Authentication Errors

**Missing Token**
```json
{
  "error": "Missing or invalid Authorization header",
  "status": 401
}
```

**Endpoint:** All authenticated endpoints

**Cause:** No Authorization header present

**Resolution:** Include JWT token in request header

---

**Invalid Token**
```json
{
  "error": "Invalid JWT token",
  "status": 401
}
```

**Cause:**
- Malformed token
- Invalid signature
- Token expired

**Resolution:**
- Verify token format
- Re-authenticate to obtain new token

---

**Bad Credentials**
```json
{
  "type": "about:blank",
  "title": "Authentication failed",
  "status": 401,
  "detail": "Invalid username or password"
}
```

**Endpoint:** POST /api/v1/auth/login

**Cause:**
- Incorrect email
- Incorrect password
- Non-existent user

**Resolution:** Verify credentials and retry

---

**Account Disabled**
```json
{
  "type": "about:blank",
  "title": "Account disabled",
  "status": 401,
  "detail": "User account is disabled"
}
```

**Endpoint:** POST /api/v1/auth/login

**Cause:** User account administratively disabled

**Resolution:** Contact system administrator

### Authorization Errors

**Insufficient Permissions**
```json
{
  "type": "about:blank",
  "title": "Access denied",
  "status": 403,
  "detail": "You don't have permission to access this resource"
}
```

**Cause:** User role insufficient for endpoint

**Common Scenarios:**
- USER role accessing analytics endpoint
- USER role attempting admin operations

**Resolution:**
- Verify endpoint access requirements
- Request role upgrade if needed

### Validation Errors

**Invalid Email Format**
```json
{
  "type": "about:blank",
  "title": "Validation error",
  "status": 400,
  "detail": "email: Invalid email format"
}
```

**Endpoint:** POST /api/v1/auth/register

**Cause:** Email does not match RFC 5322 format

**Resolution:** Provide valid email address

---

**Weak Password**
```json
{
  "type": "about:blank",
  "title": "Validation error",
  "status": 400,
  "detail": "password: Password must contain at least one digit, one lowercase, one uppercase letter and one special character"
}
```

**Endpoint:** POST /api/v1/auth/register

**Cause:** Password does not meet complexity requirements

**Resolution:** Provide password matching requirements

---

**Invalid Currency Code**
```json
{
  "type": "about:blank",
  "title": "Invalid Request",
  "status": 400,
  "detail": "to: Invalid currency code. Must be a valid ISO 4217 code (e.g., USD, EUR)"
}
```

**Endpoint:** POST /api/v1/currencies/convert

**Cause:**
- Currency code not 3 letters
- Currency code not ISO 4217 standard
- Unsupported currency

**Resolution:**
- Use 3-letter ISO 4217 codes
- Check supported currencies list
- Verify currency code spelling

---

**Invalid Amount**
```json
{
  "type": "about:blank",
  "title": "Validation error",
  "status": 400,
  "detail": "amount: Amount must be greater than 0"
}
```

**Endpoint:** POST /api/v1/currencies/convert

**Cause:**
- Negative amount
- Zero amount
- Missing amount field

**Resolution:** Provide positive decimal amount

---

**Missing Required Parameter**
```json
{
  "type": "about:blank",
  "title": "Missing required parameter",
  "status": 400,
  "detail": "Required parameter 'currency' is missing"
}
```

**Endpoint:** POST /api/v1/currencies

**Cause:** Required query or path parameter not provided

**Resolution:** Include all required parameters

---

**Invalid Period Format**
```json
{
  "type": "about:blank",
  "title": "Invalid Request",
  "status": 400,
  "detail": "period: Period must be in format: 1H, 7D, 1M, 1Y"
}
```

**Endpoint:** GET /api/v1/analytics/trends

**Cause:** Period parameter does not match expected format

**Valid Formats:**
- `7D` - 7 days
- `30D` - 30 days
- `1M` - 1 month
- `3M` - 3 months
- `6M` - 6 months
- `1Y` - 1 year

**Resolution:** Use valid period format

### Resource Errors

**User Already Exists**
```json
{
  "type": "about:blank",
  "title": "User already exists",
  "status": 409,
  "detail": "Email 'user@example.com' is already registered"
}
```

**Endpoint:** POST /api/v1/auth/register

**Cause:** Email address already registered

**Resolution:**
- Use different email
- Login with existing credentials
- Reset password if forgotten

---

**Currency Not Found**
```json
{
  "type": "about:blank",
  "title": "Currency not found",
  "status": 404,
  "detail": "Currency not found: XYZ"
}
```

**Endpoint:** Various currency endpoints

**Cause:** Referenced currency does not exist in system

**Resolution:** Verify currency code against supported currencies list

---

**Currency Not Supported**
```json
{
  "type": "about:blank",
  "title": "Currency Not Supported",
  "status": 400,
  "detail": "Currency 'BTC' is not supported. Available currencies: USD, EUR, GBP, JPY, CHF, CAD, AUD, CNY, SEK, NZD"
}
```

**Endpoint:** POST /api/v1/currencies/convert

**Cause:** Valid currency code but not configured in system

**Resolution:** Use supported currency from provided list

---

**Insufficient Data**
```json
{
  "type": "about:blank",
  "title": "Insufficient Data",
  "status": 404,
  "detail": "Insufficient data for trend analysis. Found 50 data points, need at least 2"
}
```

**Endpoint:** GET /api/v1/analytics/trends

**Cause:**
- Requested time period too far in past
- Currency pair lacks historical data
- System recently deployed

**Resolution:**
- Request shorter time period
- Wait for more data accumulation
- Try different currency pair

---

**Exchange Rate Not Available**
```json
{
  "type": "about:blank",
  "title": "Exchange rate unavailable",
  "status": 503,
  "detail": "Exchange rate not available for USD -> EUR"
}
```

**Endpoint:** POST /api/v1/currencies/convert

**Cause:**
- All external providers failed
- Mock services unavailable
- Network connectivity issues

**Resolution:**
- Retry request
- Check service status
- Wait for provider recovery

---

**Provider Key Not Found**
```json
{
  "type": "about:blank",
  "title": "Provider key not found",
  "status": 404,
  "detail": "Provider key not found with id: 5"
}
```

**Endpoint:** Admin endpoints

**Cause:** Referenced API key does not exist

**Resolution:** Verify provider key ID

### External Service Errors

**External API Error**
```json
{
  "type": "about:blank",
  "title": "External API error",
  "status": 502,
  "detail": "Failed to fetch latest exchange rates from Fixer.io: HTTP error: 401"
}
```

**Cause:**
- Invalid API key
- API provider rate limit exceeded
- API provider service outage
- Network timeout

**System Behavior:**
- Attempts failover to other providers
- Falls back to mock services if all providers fail
- Retries with exponential backoff

**Resolution:**
- System handles automatically
- Retry request if failure persists
- Check service health endpoints

---

**All Providers Failed**
```json
{
  "type": "about:blank",
  "title": "All providers failed",
  "status": 503,
  "detail": "All exchange rate providers failed: Fixer.io, ExchangeRatesAPI, CurrencyAPI, MockService1, MockService2"
}
```

**Cause:**
- All external APIs unavailable
- Mock services failed
- Network outage

**Resolution:**
- Wait and retry
- Check system status
- Contact support if persistent

### Server Errors

**Internal Server Error**
```json
{
  "type": "about:blank",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred"
}
```

**Cause:**
- Unhandled exception
- Database connection failure
- Configuration error

**Resolution:**
- Retry request
- Contact support with correlation ID
- Check service logs

## Error Handling Best Practices

### Client Implementation

**Retry Strategy:**

```java
public Response makeRequestWithRetry(Request request) {
    int maxRetries = 3;
    int retryDelay = 1000; // milliseconds
    
    for (int attempt = 0; attempt < maxRetries; attempt++) {
        try {
            Response response = executeRequest(request);
            
            if (response.getStatus() < 500) {
                // Client error or success, don't retry
                return response;
            }
            
            // Server error, retry
            if (attempt < maxRetries - 1) {
                Thread.sleep(retryDelay * (attempt + 1));
            }
        } catch (Exception e) {
            if (attempt == maxRetries - 1) {
                throw e;
            }
            Thread.sleep(retryDelay * (attempt + 1));
        }
    }
    
    throw new RuntimeException("Max retries exceeded");
}
```

**Error Categorization:**

```java
public enum ErrorCategory {
    AUTHENTICATION,  // Retry not helpful, need new credentials
    AUTHORIZATION,   // Retry not helpful, need different role
    VALIDATION,      // Retry not helpful, fix request
    NOT_FOUND,       // Retry not helpful, resource doesn't exist
    SERVER_ERROR,    // Retry may help
    SERVICE_UNAVAILABLE // Retry with backoff
}

public ErrorCategory categorizeError(int statusCode) {
    return switch (statusCode) {
        case 401 -> ErrorCategory.AUTHENTICATION;
        case 403 -> ErrorCategory.AUTHORIZATION;
        case 400, 422 -> ErrorCategory.VALIDATION;
        case 404 -> ErrorCategory.NOT_FOUND;
        case 500, 502 -> ErrorCategory.SERVER_ERROR;
        case 503 -> ErrorCategory.SERVICE_UNAVAILABLE;
        default -> ErrorCategory.SERVER_ERROR;
    };
}
```

**Exponential Backoff:**

```python
import time

def request_with_backoff(url, max_retries=5):
    base_delay = 1  # seconds
    
    for attempt in range(max_retries):
        try:
            response = requests.get(url)
            
            if response.status_code < 500:
                return response
            
            if attempt < max_retries - 1:
                delay = base_delay * (2 ** attempt)
                time.sleep(delay)
                
        except requests.RequestException as e:
            if attempt == max_retries - 1:
                raise
            delay = base_delay * (2 ** attempt)
            time.sleep(delay)
    
    raise Exception("Max retries exceeded")
```

**Graceful Degradation:**

```javascript
async function getCurrencyRate(from, to) {
  try {
    const response = await fetch(`/api/v1/currencies/convert`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({amount: 1, from, to})
    });
    
    if (response.ok) {
      const data = await response.json();
      return data.exchangeRate;
    }
    
    if (response.status === 503) {
      // Service unavailable, use cached rate if available
      return getCachedRate(from, to);
    }
    
    throw new Error(`HTTP ${response.status}`);
    
  } catch (error) {
    // Network error, use cached rate
    return getCachedRate(from, to);
  }
}
```

### Logging and Monitoring

**Correlation ID Usage:**

All responses include `X-Correlation-ID` header for request tracing:

```bash
curl -v http://localhost:8000/api/v1/currencies
```

Response headers:
```
X-Correlation-ID: 550e8400-e29b-41d4-a716-446655440000
```

**Use in support requests:**
```
Subject: API Error - Currency Conversion Failed
Correlation ID: 550e8400-e29b-41d4-a716-446655440000
Timestamp: 2024-12-19 14:30:00 UTC
Error: 503 Service Unavailable
```

**Client-Side Logging:**

```java
try {
    Response response = makeApiCall();
    
    if (!response.isSuccessful()) {
        logger.error(
            "API request failed. " +
            "Status: {}, " +
            "Correlation-ID: {}, " +
            "Message: {}",
            response.getStatus(),
            response.getHeader("X-Correlation-ID"),
            response.getBody()
        );
    }
} catch (Exception e) {
    logger.error("API request exception", e);
}
```

### Error Recovery Strategies

**Authentication Errors (401):**
1. Attempt token refresh if implemented
2. Re-authenticate with credentials
3. Prompt user for login
4. Clear invalid token from storage

**Authorization Errors (403):**
1. Verify endpoint access requirements
2. Check user role in token
3. Request role upgrade if legitimate use case
4. Show user-friendly access denied message

**Validation Errors (400):**
1. Parse error detail for specific field
2. Display field-level error messages
3. Allow user to correct and resubmit
4. Do not retry without modification

**Not Found Errors (404):**
1. Verify resource identifier
2. Check if resource was deleted
3. Provide navigation to valid resources
4. Do not retry

**Server Errors (500, 502, 503):**
1. Retry with exponential backoff
2. Use cached data if available
3. Show user-friendly error message
4. Log error with correlation ID
5. Contact support if persistent

## Testing Error Scenarios

### Simulating Errors

**Invalid Token:**
```bash
curl -X GET http://localhost:8000/api/v1/currencies \
  -H "Authorization: Bearer invalid-token"
```

**Missing Token:**
```bash
curl -X GET http://localhost:8000/api/v1/currencies
```

**Invalid Credentials:**
```bash
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"wrongpassword"}'
```

**Invalid Currency:**
```bash
curl -X POST http://localhost:8000/api/v1/currencies/convert \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"amount":100,"from":"USD","to":"INVALID"}'
```

**Insufficient Permissions:**
```bash
# USER role accessing premium endpoint
curl -X GET "http://localhost:8000/api/v1/analytics/trends?from=USD&to=EUR&period=7D" \
  -H "Authorization: Bearer ${USER_TOKEN}"
```

### Automated Testing

```java
@Test
public void testInvalidCredentials() {
    LoginRequest request = new LoginRequest(
        "user@example.com",
        "wrongpassword"
    );
    
    ResponseEntity<LoginResponse> response = 
        authController.login(request);
    
    assertEquals(401, response.getStatusCodeValue());
}

@Test
public void testMissingToken() {
    ResponseEntity<List<String>> response = 
        currencyController.getCurrencies();
    
    assertEquals(401, response.getStatusCodeValue());
}

@Test
public void testInsufficientPermissions() {
    // Authenticate as USER
    String userToken = authenticateAsUser();
    
    // Attempt premium endpoint access
    TrendsRequest request = new TrendsRequest("USD", "EUR", "7D");
    
    ResponseEntity<TrendsResponse> response = 
        analyticsController.getTrends(request);
    
    assertEquals(403, response.getStatusCodeValue());
}
```

## Troubleshooting Guide

| Error | Immediate Action | Long-term Solution |
|-------|------------------|-------------------|
| 401 Unauthorized | Re-authenticate | Implement token refresh |
| 403 Forbidden | Check role requirements | Request role upgrade |
| 400 Validation | Fix request data | Improve input validation |
| 404 Not Found | Verify resource exists | Update resource references |
| 409 Conflict | Use different identifier | Check for duplicates before creation |
| 500 Server Error | Retry with backoff | Contact support |
| 502 Bad Gateway | Wait and retry | Check external API status |
| 503 Service Unavailable | Use cached data | Monitor service health |

## Contact Support

When reporting errors, provide:
- Correlation ID from X-Correlation-ID header
- Timestamp of request
- Request endpoint and method
- HTTP status code received
- Error response body
- Steps to reproduce
