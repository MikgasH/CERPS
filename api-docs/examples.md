# Code Examples

This document provides comprehensive code examples for integrating with CERPS API in multiple programming languages.

## Table of Contents

- [Complete Workflows](#complete-workflows)
- [Java Examples](#java-examples)
- [Python Examples](#python-examples)
- [JavaScript Examples](#javascript-examples)
- [Error Handling Examples](#error-handling-examples)
- [Advanced Scenarios](#advanced-scenarios)

---

## Complete Workflows

### Basic User Journey

Complete flow from registration to currency conversion:

**Bash:**
```bash
#!/bin/bash

BASE_URL="http://localhost:8000"

# 1. Register user
echo "Registering user..."
curl -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "SecurePass123!"
  }'

# 2. Login
echo -e "\n\nLogging in..."
TOKEN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "SecurePass123!"
  }' | jq -r '.token')

echo "Token obtained: ${TOKEN:0:20}..."

# 3. Get supported currencies
echo -e "\n\nGetting supported currencies..."
curl -X GET "$BASE_URL/api/v1/currencies" \
  -H "Authorization: Bearer $TOKEN"

# 4. Convert currency
echo -e "\n\nConverting 1000 USD to EUR..."
curl -X POST "$BASE_URL/api/v1/currencies/convert" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "amount": 1000,
    "from": "USD",
    "to": "EUR"
  }'

# 5. Get user info
echo -e "\n\nGetting user info..."
curl -X GET "$BASE_URL/api/v1/auth/me" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Java Examples

### Complete Client Implementation

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class CERPSClient {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private String token;
    
    public CERPSClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.mapper = new ObjectMapper();
    }
    
    // Authentication
    
    public void register(String email, String password) throws Exception {
        String payload = String.format(
            "{\"email\":\"%s\",\"password\":\"%s\"}",
            email, password
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/auth/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        if (response.statusCode() != 201) {
            throw new RuntimeException("Registration failed: " + response.body());
        }
        
        System.out.println("User registered successfully");
    }
    
    public void login(String email, String password) throws Exception {
        String payload = String.format(
            "{\"email\":\"%s\",\"password\":\"%s\"}",
            email, password
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        if (response.statusCode() == 200) {
            JsonNode json = mapper.readTree(response.body());
            this.token = json.get("token").asText();
            System.out.println("Login successful");
        } else {
            throw new RuntimeException("Login failed: " + response.body());
        }
    }
    
    public JsonNode getCurrentUser() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/auth/me"))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        if (response.statusCode() == 200) {
            return mapper.readTree(response.body());
        } else {
            throw new RuntimeException("Failed to get user: " + response.body());
        }
    }
    
    // Currency Operations
    
    public String[] getSupportedCurrencies() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/currencies"))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        if (response.statusCode() == 200) {
            return mapper.readValue(response.body(), String[].class);
        } else {
            throw new RuntimeException("Failed to get currencies: " + response.body());
        }
    }
    
    public JsonNode convertCurrency(double amount, String from, String to) throws Exception {
        String payload = String.format(
            "{\"amount\":%.2f,\"from\":\"%s\",\"to\":\"%s\"}",
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
            return mapper.readTree(response.body());
        } else {
            throw new RuntimeException("Conversion failed: " + response.body());
        }
    }
    
    // Analytics (Premium only)
    
    public JsonNode getTrends(String from, String to, String period) throws Exception {
        String url = String.format(
            "%s/api/v1/analytics/trends?from=%s&to=%s&period=%s",
            baseUrl, from, to, period
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        if (response.statusCode() == 200) {
            return mapper.readTree(response.body());
        } else if (response.statusCode() == 403) {
            throw new RuntimeException("Premium role required for analytics");
        } else {
            throw new RuntimeException("Failed to get trends: " + response.body());
        }
    }
    
    // Example usage
    
    public static void main(String[] args) {
        try {
            CERPSClient client = new CERPSClient("http://localhost:8000");
            
            // Register and login
            client.register("demo@example.com", "SecurePass123!");
            client.login("demo@example.com", "SecurePass123!");
            
            // Get user info
            JsonNode user = client.getCurrentUser();
            System.out.println("User: " + user.get("email").asText());
            System.out.println("Roles: " + user.get("roles"));
            
            // List currencies
            String[] currencies = client.getSupportedCurrencies();
            System.out.println("Supported currencies: " + String.join(", ", currencies));
            
            // Convert currency
            JsonNode result = client.convertCurrency(1000, "USD", "EUR");
            System.out.println("Converted amount: " + result.get("convertedAmount"));
            System.out.println("Exchange rate: " + result.get("exchangeRate"));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Spring Boot RestTemplate Example

```java
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.util.Map;

public class CERPSSpringClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private String token;
    
    public CERPSSpringClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    public void login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> body = Map.of(
            "email", email,
            "password", password
        );
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/login",
            request,
            Map.class
        );
        
        this.token = (String) response.getBody().get("token");
    }
    
    public Map<String, Object> convertCurrency(double amount, String from, String to) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        
        Map<String, Object> body = Map.of(
            "amount", amount,
            "from", from,
            "to", to
        );
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/currencies/convert",
            request,
            Map.class
        );
        
        return response.getBody();
    }
}
```

---

## Python Examples

### Complete Client Implementation

```python
import requests
from typing import Optional, Dict, List

class CERPSClient:
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({'Content-Type': 'application/json'})
        self.token: Optional[str] = None
    
    # Authentication
    
    def register(self, email: str, password: str) -> str:
        """Register a new user"""
        response = self.session.post(
            f"{self.base_url}/api/v1/auth/register",
            json={"email": email, "password": password}
        )
        response.raise_for_status()
        return response.text
    
    def login(self, email: str, password: str) -> str:
        """Login and store token"""
        response = self.session.post(
            f"{self.base_url}/api/v1/auth/login",
            json={"email": email, "password": password}
        )
        response.raise_for_status()
        
        data = response.json()
        self.token = data['token']
        self.session.headers.update({'Authorization': f"Bearer {self.token}"})
        
        return self.token
    
    def get_current_user(self) -> Dict:
        """Get current user information"""
        response = self.session.get(f"{self.base_url}/api/v1/auth/me")
        response.raise_for_status()
        return response.json()
    
    def change_password(self, current_password: str, new_password: str) -> str:
        """Change user password"""
        response = self.session.post(
            f"{self.base_url}/api/v1/auth/change-password",
            json={
                "currentPassword": current_password,
                "newPassword": new_password
            }
        )
        response.raise_for_status()
        return response.text
    
    # Currency Operations
    
    def get_supported_currencies(self) -> List[str]:
        """Get list of supported currencies"""
        response = self.session.get(f"{self.base_url}/api/v1/currencies")
        response.raise_for_status()
        return response.json()
    
    def convert_currency(self, amount: float, from_currency: str, 
                        to_currency: str) -> Dict:
        """Convert currency"""
        response = self.session.post(
            f"{self.base_url}/api/v1/currencies/convert",
            json={
                "amount": amount,
                "from": from_currency,
                "to": to_currency
            }
        )
        response.raise_for_status()
        return response.json()
    
    # Analytics (Premium only)
    
    def get_trends(self, from_currency: str, to_currency: str, 
                   period: str) -> Dict:
        """Get currency trends (Premium only)"""
        response = self.session.get(
            f"{self.base_url}/api/v1/analytics/trends",
            params={
                "from": from_currency,
                "to": to_currency,
                "period": period
            }
        )
        
        if response.status_code == 403:
            raise PermissionError("Premium role required for analytics")
        
        response.raise_for_status()
        return response.json()
    
    # Helper methods
    
    def print_conversion(self, result: Dict):
        """Pretty print conversion result"""
        print(f"\n{'='*50}")
        print(f"Currency Conversion Result")
        print(f"{'='*50}")
        print(f"Amount: {result['originalAmount']} {result['fromCurrency']}")
        print(f"Converted: {result['convertedAmount']:.2f} {result['toCurrency']}")
        print(f"Exchange Rate: {result['exchangeRate']:.6f}")
        print(f"Timestamp: {result['timestamp']}")
        print(f"{'='*50}\n")
    
    def print_trends(self, trends: Dict):
        """Pretty print trends result"""
        print(f"\n{'='*50}")
        print(f"Trend Analysis: {trends['from']}/{trends['to']}")
        print(f"{'='*50}")
        print(f"Period: {trends['period']}")
        print(f"Start Rate: {trends['oldRate']:.6f}")
        print(f"End Rate: {trends['newRate']:.6f}")
        print(f"Change: {trends['changePercentage']:+.2f}%")
        print(f"Data Points: {trends['dataPoints']}")
        print(f"Start Date: {trends['startDate']}")
        print(f"End Date: {trends['endDate']}")
        print(f"{'='*50}\n")


# Example usage
def main():
    client = CERPSClient("http://localhost:8000")
    
    try:
        # Register and login
        print("Registering user...")
        client.register("demo@example.com", "SecurePass123!")
        
        print("Logging in...")
        client.login("demo@example.com", "SecurePass123!")
        
        # Get user info
        user = client.get_current_user()
        print(f"Logged in as: {user['email']}")
        print(f"Roles: {', '.join(user['roles'])}")
        
        # List currencies
        currencies = client.get_supported_currencies()
        print(f"\nSupported currencies: {', '.join(currencies)}")
        
        # Convert currency
        print("\nConverting 1000 USD to EUR...")
        result = client.convert_currency(1000, "USD", "EUR")
        client.print_conversion(result)
        
        # Try analytics (may fail without Premium role)
        try:
            print("Getting 7-day trends...")
            trends = client.get_trends("USD", "EUR", "7D")
            client.print_trends(trends)
        except PermissionError as e:
            print(f"Analytics unavailable: {e}")
        
    except requests.exceptions.HTTPError as e:
        print(f"HTTP Error: {e}")
        print(f"Response: {e.response.text}")
    except Exception as e:
        print(f"Error: {e}")


if __name__ == "__main__":
    main()
```

### Async Python Example (asyncio + aiohttp)

```python
import aiohttp
import asyncio
from typing import Dict, List

class AsyncCERPSClient:
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.token = None
    
    async def __aenter__(self):
        self.session = aiohttp.ClientSession()
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        await self.session.close()
    
    async def login(self, email: str, password: str):
        async with self.session.post(
            f"{self.base_url}/api/v1/auth/login",
            json={"email": email, "password": password}
        ) as response:
            data = await response.json()
            self.token = data['token']
    
    async def convert_currency(self, amount: float, from_currency: str, 
                              to_currency: str) -> Dict:
        headers = {"Authorization": f"Bearer {self.token}"}
        async with self.session.post(
            f"{self.base_url}/api/v1/currencies/convert",
            json={"amount": amount, "from": from_currency, "to": to_currency},
            headers=headers
        ) as response:
            return await response.json()
    
    async def get_multiple_conversions(self, conversions: List[tuple]):
        """Convert multiple currencies concurrently"""
        tasks = [
            self.convert_currency(amount, from_curr, to_curr)
            for amount, from_curr, to_curr in conversions
        ]
        return await asyncio.gather(*tasks)


# Usage
async def main():
    async with AsyncCERPSClient("http://localhost:8000") as client:
        await client.login("demo@example.com", "SecurePass123!")
        
        # Convert multiple currencies concurrently
        conversions = [
            (1000, "USD", "EUR"),
            (500, "GBP", "USD"),
            (2000, "EUR", "JPY")
        ]
        
        results = await client.get_multiple_conversions(conversions)
        
        for result in results:
            print(f"{result['originalAmount']} {result['fromCurrency']} = "
                  f"{result['convertedAmount']:.2f} {result['toCurrency']}")

asyncio.run(main())
```

---

## JavaScript Examples

### Node.js Fetch API

```javascript
class CERPSClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
    this.token = null;
  }
  
  // Authentication
  
  async register(email, password) {
    const response = await fetch(`${this.baseUrl}/api/v1/auth/register`, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({email, password})
    });
    
    if (!response.ok) {
      throw new Error(`Registration failed: ${response.status}`);
    }
    
    return await response.text();
  }
  
  async login(email, password) {
    const response = await fetch(`${this.baseUrl}/api/v1/auth/login`, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({email, password})
    });
    
    if (!response.ok) {
      throw new Error(`Login failed: ${response.status}`);
    }
    
    const data = await response.json();
    this.token = data.token;
    return this.token;
  }
  
  async getCurrentUser() {
    const response = await fetch(`${this.baseUrl}/api/v1/auth/me`, {
      headers: {'Authorization': `Bearer ${this.token}`}
    });
    
    if (!response.ok) {
      throw new Error(`Failed to get user: ${response.status}`);
    }
    
    return await response.json();
  }
  
  // Currency Operations
  
  async getSupportedCurrencies() {
    const response = await fetch(`${this.baseUrl}/api/v1/currencies`, {
      headers: {'Authorization': `Bearer ${this.token}`}
    });
    
    if (!response.ok) {
      throw new Error(`Failed to get currencies: ${response.status}`);
    }
    
    return await response.json();
  }
  
  async convertCurrency(amount, from, to) {
    const response = await fetch(`${this.baseUrl}/api/v1/currencies/convert`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.token}`
      },
      body: JSON.stringify({amount, from, to})
    });
    
    if (!response.ok) {
      throw new Error(`Conversion failed: ${response.status}`);
    }
    
    return await response.json();
  }
  
  // Analytics (Premium only)
  
  async getTrends(from, to, period) {
    const params = new URLSearchParams({from, to, period});
    const response = await fetch(
      `${this.baseUrl}/api/v1/analytics/trends?${params}`,
      {
        headers: {'Authorization': `Bearer ${this.token}`}
      }
    );
    
    if (response.status === 403) {
      throw new Error('Premium role required for analytics');
    }
    
    if (!response.ok) {
      throw new Error(`Failed to get trends: ${response.status}`);
    }
    
    return await response.json();
  }
  
  // Helper methods
  
  printConversion(result) {
    console.log('\n' + '='.repeat(50));
    console.log('Currency Conversion Result');
    console.log('='.repeat(50));
    console.log(`Amount: ${result.originalAmount} ${result.fromCurrency}`);
    console.log(`Converted: ${result.convertedAmount.toFixed(2)} ${result.toCurrency}`);
    console.log(`Exchange Rate: ${result.exchangeRate.toFixed(6)}`);
    console.log(`Timestamp: ${result.timestamp}`);
    console.log('='.repeat(50) + '\n');
  }
  
  printTrends(trends) {
    console.log('\n' + '='.repeat(50));
    console.log(`Trend Analysis: ${trends.from}/${trends.to}`);
    console.log('='.repeat(50));
    console.log(`Period: ${trends.period}`);
    console.log(`Start Rate: ${trends.oldRate.toFixed(6)}`);
    console.log(`End Rate: ${trends.newRate.toFixed(6)}`);
    console.log(`Change: ${trends.changePercentage > 0 ? '+' : ''}${trends.changePercentage.toFixed(2)}%`);
    console.log(`Data Points: ${trends.dataPoints}`);
    console.log('='.repeat(50) + '\n');
  }
}

// Example usage
async function main() {
  const client = new CERPSClient('http://localhost:8000');
  
  try {
    // Register and login
    console.log('Registering user...');
    await client.register('demo@example.com', 'SecurePass123!');
    
    console.log('Logging in...');
    await client.login('demo@example.com', 'SecurePass123!');
    
    // Get user info
    const user = await client.getCurrentUser();
    console.log(`Logged in as: ${user.email}`);
    console.log(`Roles: ${user.roles.join(', ')}`);
    
    // List currencies
    const currencies = await client.getSupportedCurrencies();
    console.log(`\nSupported currencies: ${currencies.join(', ')}`);
    
    // Convert currency
    console.log('\nConverting 1000 USD to EUR...');
    const result = await client.convertCurrency(1000, 'USD', 'EUR');
    client.printConversion(result);
    
    // Try analytics
    try {
      console.log('Getting 7-day trends...');
      const trends = await client.getTrends('USD', 'EUR', '7D');
      client.printTrends(trends);
    } catch (error) {
      console.log(`Analytics unavailable: ${error.message}`);
    }
    
  } catch (error) {
    console.error('Error:', error.message);
  }
}

main();
```

---

## Error Handling Examples

### Java - Comprehensive Error Handling

```java
public class RobustCERPSClient {
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    
    public JsonNode convertWithRetry(double amount, String from, String to) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < MAX_RETRIES) {
            try {
                return convertCurrency(amount, from, to);
            } catch (Exception e) {
                lastException = e;
                attempts++;
                
                if (attempts < MAX_RETRIES) {
                    System.err.println(
                        "Attempt " + attempts + " failed, retrying in " + 
                        RETRY_DELAY_MS + "ms..."
                    );
                    Thread.sleep(RETRY_DELAY_MS * attempts);
                }
            }
        }
        
        throw new RuntimeException(
            "Failed after " + MAX_RETRIES + " attempts",
            lastException
        );
    }
}
```

### Python - Error Handling with Retry Logic

```python
import time
from requests.exceptions import RequestException

class RobustCERPSClient(CERPSClient):
    MAX_RETRIES = 3
    RETRY_DELAY = 1.0
    
    def convert_with_retry(self, amount, from_currency, to_currency):
        """Convert currency with automatic retry"""
        for attempt in range(self.MAX_RETRIES):
            try:
                return self.convert_currency(amount, from_currency, to_currency)
            except RequestException as e:
                if attempt == self.MAX_RETRIES - 1:
                    raise
                
                delay = self.RETRY_DELAY * (attempt + 1)
                print(f"Attempt {attempt + 1} failed, retrying in {delay}s...")
                time.sleep(delay)
```

---

## Advanced Scenarios

### Batch Currency Conversion

```python
def batch_convert(client, conversions):
    """Process multiple conversions efficiently"""
    results = []
    errors = []
    
    for i, (amount, from_curr, to_curr) in enumerate(conversions):
        try:
            result = client.convert_currency(amount, from_curr, to_curr)
            results.append(result)
        except Exception as e:
            errors.append({
                'index': i,
                'conversion': (amount, from_curr, to_curr),
                'error': str(e)
            })
    
    return results, errors

# Usage
conversions = [
    (1000, 'USD', 'EUR'),
    (500, 'GBP', 'USD'),
    (2000, 'EUR', 'JPY'),
    (750, 'CHF', 'GBP')
]

results, errors = batch_convert(client, conversions)
print(f"Successful: {len(results)}, Failed: {len(errors)}")
```

### Multi-Period Trend Analysis

```javascript
async function analyzeMultiplePeriods(client, from, to) {
  const periods = ['7D', '30D', '3M', '6M', '1Y'];
  const results = [];
  
  for (const period of periods) {
    try {
      const trend = await client.getTrends(from, to, period);
      results.push({
        period,
        change: trend.changePercentage,
        oldRate: trend.oldRate,
        newRate: trend.newRate
      });
    } catch (error) {
      console.error(`Failed for period ${period}:`, error.message);
    }
  }
  
  // Print summary
  console.log(`\nTrend Summary: ${from}/${to}`);
  console.log('='.repeat(50));
  results.forEach(r => {
    const arrow = r.change > 0 ? '↑' : r.change < 0 ? '↓' : '→';
    console.log(`${r.period.padEnd(5)} ${arrow} ${r.change > 0 ? '+' : ''}${r.change.toFixed(2)}%`);
  });
  
  return results;
}
```

---

## Related Documentation

- [Integration Tutorial](integration-tutorial.md) - Step-by-step integration guide
- [API Reference - User Service](api-reference-user.md) - User endpoints
- [API Reference - Currency Service](api-reference-currency.md) - Currency endpoints
- [API Reference - Analytics Service](api-reference-analytics.md) - Analytics endpoints
- [Error Handling](error-handling.md) - Error codes and recovery
