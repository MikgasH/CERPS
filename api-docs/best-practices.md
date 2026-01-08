# Best Practices

This document provides recommendations for effective and secure integration with the CERPS API.

## Authentication and Security

### Token Management

**Store Tokens Securely:**
```javascript
// Good: Environment variable or secure storage
const token = process.env.CERPS_TOKEN;

// Bad: Hardcoded in source code
const token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
```

**Token Expiration Handling:**
```python
def get_valid_token(self):
    if self.token_expired():
        self.refresh_token()
    return self.token
```

**Never Log Tokens:**
```java
// Bad
logger.info("Token: " + token);

// Good
logger.info("User authenticated successfully");
```

### Password Security

**Client-Side Validation:**
```javascript
function validatePassword(password) {
  const minLength = 8;
  const maxLength = 20;
  const hasDigit = /\d/;
  const hasLower = /[a-z]/;
  const hasUpper = /[A-Z]/;
  const hasSpecial = /[@#$%^&+=!]/;
  
  return password.length >= minLength &&
         password.length <= maxLength &&
         hasDigit.test(password) &&
         hasLower.test(password) &&
         hasUpper.test(password) &&
         hasSpecial.test(password);
}
```

**Never Store Passwords:**
- Store tokens only, never passwords
- Clear password from memory after authentication
- Use secure input fields (type="password")

### HTTPS in Production

Always use HTTPS in production:
```python
# Production
base_url = "https://api.cerps.com"

# Development only
base_url = "http://localhost:8000"
```

---

## Error Handling

### Implement Retry Logic

```python
import time
from requests.exceptions import RequestException

def convert_with_retry(client, amount, from_currency, to_currency):
    max_retries = 3
    retry_delay = 1.0
    
    for attempt in range(max_retries):
        try:
            return client.convert_currency(amount, from_currency, to_currency)
        except RequestException as e:
            if attempt == max_retries - 1:
                raise
            time.sleep(retry_delay * (attempt + 1))
```

### Handle Specific Errors

```java
try {
    result = client.convertCurrency(amount, from, to);
} catch (ValidationException e) {
    // Handle validation error (400)
    logger.error("Invalid input: " + e.getMessage());
    showUserError("Please check your input");
} catch (AuthenticationException e) {
    // Handle auth error (401)
    logger.error("Authentication failed");
    redirectToLogin();
} catch (ServiceUnavailableException e) {
    // Handle service error (503)
    logger.error("Service unavailable");
    showUserError("Service temporarily unavailable. Please try again.");
}
```

### Graceful Degradation

```javascript
async function getCurrencyData(from, to) {
  try {
    // Try primary API
    return await client.convertCurrency(1000, from, to);
  } catch (error) {
    // Fall back to cached data
    return getCachedRate(from, to);
  }
}
```

---

## Performance Optimization

### Connection Reuse

```python
import requests

# Good: Reuse session
session = requests.Session()
client = CERPSClient(base_url, session=session)

# Bad: New connection each time
for i in range(100):
    requests.post(url, ...)  # Creates new connection
```

### Batch Operations

```python
# Good: Batch processing
conversions = [
    (1000, 'USD', 'EUR'),
    (500, 'GBP', 'USD'),
    (2000, 'EUR', 'JPY')
]

results = []
for amount, from_curr, to_curr in conversions:
    try:
        result = client.convert_currency(amount, from_curr, to_curr)
        results.append(result)
    except Exception as e:
        logger.error(f"Failed: {amount} {from_curr} -> {to_curr}")

# Bad: One-by-one with full error handling overhead
for conversion in conversions:
    result = convert_with_full_validation(conversion)
```

### Caching

```javascript
class CachedCERPSClient extends CERPSClient {
  constructor(baseUrl, cacheTTL = 3600000) {
    super(baseUrl);
    this.cache = new Map();
    this.cacheTTL = cacheTTL;
  }
  
  async getSupportedCurrencies() {
    const cacheKey = 'currencies';
    const cached = this.cache.get(cacheKey);
    
    if (cached && Date.now() - cached.timestamp < this.cacheTTL) {
      return cached.data;
    }
    
    const data = await super.getSupportedCurrencies();
    this.cache.set(cacheKey, {
      data,
      timestamp: Date.now()
    });
    
    return data;
  }
}
```

---

## API Usage Patterns

### Request Validation

```python
def convert_currency(amount, from_currency, to_currency):
    # Validate inputs before API call
    if amount <= 0:
        raise ValueError("Amount must be positive")
    
    if len(from_currency) != 3 or len(to_currency) != 3:
        raise ValueError("Currency codes must be 3 letters")
    
    if from_currency == to_currency:
        return {"convertedAmount": amount, "exchangeRate": 1.0}
    
    # Make API call only after validation
    return client.convert_currency(amount, from_currency, to_currency)
```

### Pagination for Large Data Sets

For future endpoints with pagination:
```python
def get_all_records(client, endpoint):
    all_records = []
    page = 1
    
    while True:
        response = client.get(endpoint, params={'page': page, 'size': 100})
        all_records.extend(response['data'])
        
        if not response['hasMore']:
            break
        
        page += 1
    
    return all_records
```

### Timeouts

```java
HttpClient client = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(5))
    .build();

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(url))
    .timeout(Duration.ofSeconds(10))
    .build();
```

---

## Data Handling

### Decimal Precision

```python
from decimal import Decimal

# Good: Use Decimal for currency
amount = Decimal('1000.00')
converted = Decimal(str(result['convertedAmount']))

# Bad: Float precision issues
amount = 1000.00  # May have rounding errors
```

### Currency Formatting

```javascript
function formatCurrency(amount, currencyCode) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currencyCode,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount);
}

// Usage
formatCurrency(1234.56, 'USD');  // "$1,234.56"
formatCurrency(1234.56, 'EUR');  // "€1,234.56"
```

### Input Sanitization

```python
def sanitize_currency_code(code):
    # Remove whitespace and convert to uppercase
    code = code.strip().upper()
    
    # Validate format
    if not code.isalpha() or len(code) != 3:
        raise ValueError(f"Invalid currency code: {code}")
    
    return code
```

---

## Testing

### Unit Testing

```python
import unittest
from unittest.mock import Mock, patch

class TestCERPSClient(unittest.TestCase):
    def setUp(self):
        self.client = CERPSClient("http://localhost:8000")
    
    @patch('requests.Session.post')
    def test_login_success(self, mock_post):
        mock_post.return_value.status_code = 200
        mock_post.return_value.json.return_value = {
            'token': 'test-token',
            'email': 'test@example.com'
        }
        
        token = self.client.login('test@example.com', 'password')
        
        self.assertEqual(token, 'test-token')
        self.assertEqual(self.client.token, 'test-token')
```

### Integration Testing

```java
@Test
public void testCurrencyConversionFlow() throws Exception {
    CERPSClient client = new CERPSClient("http://localhost:8000");
    
    // Login
    client.login("test@example.com", "password");
    assertNotNull(client.getToken());
    
    // Convert currency
    JsonNode result = client.convertCurrency(1000, "USD", "EUR");
    assertTrue(result.get("success").asBoolean());
    assertTrue(result.get("convertedAmount").asDouble() > 0);
}
```

### Error Testing

```javascript
describe('Error Handling', () => {
  it('should handle 401 unauthorized', async () => {
    client.token = 'invalid-token';
    
    await expect(
      client.convertCurrency(1000, 'USD', 'EUR')
    ).rejects.toThrow('Authentication failed');
  });
  
  it('should handle validation errors', async () => {
    await expect(
      client.convertCurrency(-1000, 'USD', 'EUR')
    ).rejects.toThrow('Validation error');
  });
});
```

---

## Monitoring and Logging

### Structured Logging

```python
import logging
import json

logger = logging.getLogger(__name__)

def log_api_call(method, endpoint, status_code, duration):
    log_data = {
        'method': method,
        'endpoint': endpoint,
        'status_code': status_code,
        'duration_ms': duration,
        'timestamp': datetime.now().isoformat()
    }
    
    if status_code >= 400:
        logger.error(json.dumps(log_data))
    else:
        logger.info(json.dumps(log_data))
```

### Correlation IDs

```java
public class CorrelationIdFilter {
    public void addCorrelationId(HttpRequest.Builder requestBuilder) {
        String correlationId = UUID.randomUUID().toString();
        requestBuilder.header("X-Correlation-ID", correlationId);
        
        // Log for tracing
        logger.info("Request correlation ID: " + correlationId);
    }
}
```

### Performance Monitoring

```javascript
async function monitoredApiCall(fn, operationName) {
  const start = Date.now();
  
  try {
    const result = await fn();
    const duration = Date.now() - start;
    
    metrics.recordSuccess(operationName, duration);
    
    return result;
  } catch (error) {
    const duration = Date.now() - start;
    
    metrics.recordFailure(operationName, duration, error);
    
    throw error;
  }
}

// Usage
const result = await monitoredApiCall(
  () => client.convertCurrency(1000, 'USD', 'EUR'),
  'currency_conversion'
);
```

---

## Rate Limiting

### Client-Side Rate Limiting

```python
import time
from collections import deque

class RateLimitedClient(CERPSClient):
    def __init__(self, base_url, max_requests=100, window_seconds=60):
        super().__init__(base_url)
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self.requests = deque()
    
    def _wait_if_needed(self):
        now = time.time()
        
        # Remove old requests outside window
        while self.requests and self.requests[0] < now - self.window_seconds:
            self.requests.popleft()
        
        # Check if limit exceeded
        if len(self.requests) >= self.max_requests:
            sleep_time = self.requests[0] + self.window_seconds - now
            if sleep_time > 0:
                time.sleep(sleep_time)
        
        self.requests.append(now)
    
    def convert_currency(self, amount, from_currency, to_currency):
        self._wait_if_needed()
        return super().convert_currency(amount, from_currency, to_currency)
```

### Exponential Backoff

```javascript
async function callWithBackoff(fn, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      if (error.status !== 429 || i === maxRetries - 1) {
        throw error;
      }
      
      // Exponential backoff: 1s, 2s, 4s
      const delay = Math.pow(2, i) * 1000;
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
}
```

---

## Production Considerations

### Configuration Management

```python
from dataclasses import dataclass
from os import getenv

@dataclass
class CERPSConfig:
    base_url: str
    timeout: int
    max_retries: int
    enable_caching: bool
    
    @classmethod
    def from_env(cls):
        return cls(
            base_url=getenv('CERPS_BASE_URL', 'http://localhost:8000'),
            timeout=int(getenv('CERPS_TIMEOUT', '30')),
            max_retries=int(getenv('CERPS_MAX_RETRIES', '3')),
            enable_caching=getenv('CERPS_ENABLE_CACHE', 'true').lower() == 'true'
        )
```

### Health Checks

```javascript
class HealthMonitor {
  constructor(client, checkInterval = 60000) {
    this.client = client;
    this.checkInterval = checkInterval;
    this.healthy = true;
  }
  
  async start() {
    setInterval(async () => {
      try {
        await fetch(`${this.client.baseUrl}/actuator/health`);
        if (!this.healthy) {
          console.log('Service recovered');
          this.healthy = true;
        }
      } catch (error) {
        if (this.healthy) {
          console.error('Service unhealthy');
          this.healthy = false;
        }
      }
    }, this.checkInterval);
  }
  
  isHealthy() {
    return this.healthy;
  }
}
```

### Circuit Breaker Pattern

```java
public class CircuitBreaker {
    private int failureCount = 0;
    private final int failureThreshold = 5;
    private boolean open = false;
    private long lastFailureTime = 0;
    private final long resetTimeout = 60000; // 1 minute
    
    public <T> T execute(Supplier<T> operation) throws Exception {
        if (open && System.currentTimeMillis() - lastFailureTime < resetTimeout) {
            throw new CircuitBreakerOpenException();
        }
        
        try {
            T result = operation.get();
            reset();
            return result;
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }
    
    private void recordFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
        
        if (failureCount >= failureThreshold) {
            open = true;
        }
    }
    
    private void reset() {
        failureCount = 0;
        open = false;
    }
}
```

---

## Common Pitfalls

### Avoid Blocking Operations

```python
# Bad: Blocking main thread
result = client.convert_currency(1000, 'USD', 'EUR')  # Blocks

# Good: Async operation
async def convert():
    result = await client.convert_currency(1000, 'USD', 'EUR')
    return result
```

### Don't Ignore Errors

```javascript
// Bad
try {
  await client.convertCurrency(amount, from, to);
} catch (error) {
  // Silent failure
}

// Good
try {
  await client.convertCurrency(amount, from, to);
} catch (error) {
  logger.error('Conversion failed', {error, amount, from, to});
  throw error;
}
```

### Validate User Input

```java
// Bad: Trust user input
double amount = Double.parseDouble(userInput);

// Good: Validate first
try {
    double amount = Double.parseDouble(userInput);
    if (amount <= 0 || amount > 1000000) {
        throw new IllegalArgumentException("Amount out of range");
    }
} catch (NumberFormatException e) {
    throw new IllegalArgumentException("Invalid amount format");
}
```

---

## Security Checklist

- [ ] Use HTTPS in production
- [ ] Store tokens securely
- [ ] Never log sensitive data
- [ ] Validate all user inputs
- [ ] Implement request timeouts
- [ ] Use strong password requirements
- [ ] Handle errors gracefully
- [ ] Implement rate limiting
- [ ] Monitor for unusual activity
- [ ] Keep dependencies updated

---

## Related Documentation

- [Integration Tutorial](integration-tutorial.md) - Complete integration guide
- [Error Handling](error-handling.md) - Error codes and recovery
- [Authentication](authentication.md) - Security implementation
- [Examples](examples.md) - Code examples
