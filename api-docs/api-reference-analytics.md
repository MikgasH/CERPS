# Analytics Service API Reference

This document provides detailed specifications for all Analytics Service endpoints.

## Base Information

**Base URL:** `http://localhost:8082`  
**Via Gateway:** `http://localhost:8000`

**Service Responsibilities:**
- Historical trend analysis for currency pairs
- Percentage change calculations over time periods
- Data aggregation and metrics

## Authentication

All endpoints require JWT authentication with specific role requirements:

```http
Authorization: Bearer <jwt-token>
```

**Required Roles:** PREMIUM_USER, ADMIN

**Note:** Standard USER role will receive 403 Forbidden response.

## Endpoints

### Get Currency Trends

Analyze currency pair trends over specified time period.

**Endpoint:** `GET /api/v1/analytics/trends`

**Authentication:** Required

**Roles:** PREMIUM_USER, ADMIN only

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| from | string | Yes | Source currency code (ISO 4217, 3 letters) |
| to | string | Yes | Target currency code (ISO 4217, 3 letters) |
| period | string | Yes | Time period format: {number}{unit} |

**Period Format:**

| Format | Description | Example |
|--------|-------------|---------|
| {n}H | Hours | 24H = 24 hours |
| {n}D | Days | 7D = 7 days |
| {n}M | Months | 1M = 30 days |
| {n}Y | Years | 1Y = 365 days |

**Common Period Values:**
- `7D` - 7 days (1 week)
- `30D` - 30 days
- `1M` - 1 month (30 days)
- `3M` - 3 months (90 days)
- `6M` - 6 months (180 days)
- `1Y` - 1 year (365 days)

**Request Example:**
```bash
curl -X GET "http://localhost:8000/api/v1/analytics/trends?from=USD&to=EUR&period=7D" \
  -H "Authorization: Bearer <premium-or-admin-token>"
```

**Success Response:**

**Status Code:** `200 OK`

**Body:**
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

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| from | string | Source currency code |
| to | string | Target currency code |
| period | string | Requested time period |
| oldRate | number | Exchange rate at period start (6 decimals) |
| newRate | number | Exchange rate at period end (6 decimals) |
| changePercentage | number | Percentage change (2 decimals) |
| startDate | string | Period start timestamp (ISO 8601) |
| endDate | string | Period end timestamp (ISO 8601) |
| dataPoints | integer | Number of rate samples in period |

**Percentage Change Calculation:**

```
changePercentage = ((newRate - oldRate) / oldRate) × 100
```

**Interpretation:**
- Positive value: Target currency strengthened
- Negative value: Target currency weakened
- Zero: No change

**Example Interpretations:**

```json
{
  "from": "USD",
  "to": "EUR",
  "changePercentage": 2.50
}
```
Interpretation: EUR strengthened by 2.50% against USD (EUR became more valuable)

```json
{
  "from": "EUR",
  "to": "USD",
  "changePercentage": -2.50
}
```
Interpretation: USD weakened by 2.50% against EUR (USD became less valuable)

**Error Responses:**

**400 Bad Request** - Invalid period format
```json
{
  "type": "about:blank",
  "title": "Invalid Request",
  "status": 400,
  "detail": "period: Period must be in format: 1H, 7D, 1M, 1Y"
}
```

**400 Bad Request** - Invalid currency code
```json
{
  "type": "about:blank",
  "title": "Invalid Request",
  "status": 400,
  "detail": "from: From currency must be a 3-letter code"
}
```

**403 Forbidden** - Insufficient role
```json
{
  "type": "about:blank",
  "title": "Access denied",
  "status": 403,
  "detail": "You don't have permission to access this resource"
}
```

**404 Not Found** - Insufficient data
```json
{
  "type": "about:blank",
  "title": "Insufficient Data",
  "status": 404,
  "detail": "Insufficient data for trend analysis. Found 1 data points, need at least 2"
}
```

**Causes of Insufficient Data:**
- Requested period too far in past
- Currency pair lacks historical data
- System recently deployed
- Data cleanup removed old records

---

## Trend Analysis Details

### Data Requirements

Minimum requirements for trend calculation:
- At least 2 data points in the specified period
- Both start and end rates must be available
- Rates must have valid timestamps

### Data Sources

Analytics service queries the same database as Currency Service but with read-only access:

**Database:** `currency_db`  
**User:** `analytics_readonly`  
**Tables:** `exchange_rates`

### Query Process

```
1. Calculate start date: current_date - period
2. Calculate end date: current_date
3. Query exchange_rates table:
   - Filter by base_currency = from
   - Filter by target_currency = to
   - Filter by timestamp BETWEEN start_date AND end_date
   - Order by timestamp ASC
4. Extract oldest rate (start of period)
5. Extract newest rate (end of period)
6. Calculate percentage change
7. Count data points
```

### Historical Data Retention

- Exchange rates stored for 395 days
- Older records automatically cleaned up
- Cannot query trends beyond retention period

---

## Use Cases

### Monitoring Currency Volatility

Track short-term volatility:
```bash
# 24-hour trend
curl -X GET "http://localhost:8000/api/v1/analytics/trends?from=GBP&to=USD&period=1D" \
  -H "Authorization: Bearer <token>"

# 7-day trend
curl -X GET "http://localhost:8000/api/v1/analytics/trends?from=GBP&to=USD&period=7D" \
  -H "Authorization: Bearer <token>"
```

### Investment Decision Support

Compare multiple periods:
```bash
# Short-term trend (1 month)
curl -X GET "http://localhost:8000/api/v1/analytics/trends?from=EUR&to=JPY&period=1M" \
  -H "Authorization: Bearer <token>"

# Long-term trend (1 year)
curl -X GET "http://localhost:8000/api/v1/analytics/trends?from=EUR&to=JPY&period=1Y" \
  -H "Authorization: Bearer <token>"
```

### Risk Assessment

Analyze historical performance:
```bash
# 6-month performance
curl -X GET "http://localhost:8000/api/v1/analytics/trends?from=USD&to=CHF&period=6M" \
  -H "Authorization: Bearer <token>"
```

---

## Data Models

### TrendsRequest

```json
{
  "from": "string (ISO 4217, 3 letters)",
  "to": "string (ISO 4217, 3 letters)",
  "period": "string (format: {number}{unit})"
}
```

### TrendsResponse

```json
{
  "from": "string",
  "to": "string",
  "period": "string",
  "oldRate": "number (6 decimals)",
  "newRate": "number (6 decimals)",
  "changePercentage": "number (2 decimals)",
  "startDate": "string (ISO 8601)",
  "endDate": "string (ISO 8601)",
  "dataPoints": "integer"
}
```

---

## Access Control

### Role Requirements

**PREMIUM_USER Role:**
- Access to all trend analysis features
- No additional restrictions

**ADMIN Role:**
- Access to all trend analysis features
- No additional restrictions

**USER Role:**
- No access to analytics endpoints
- Receives 403 Forbidden

### Upgrading User Role

Standard users cannot access analytics features. Role upgrade requires:

1. Administrative action (manual database update)
2. No self-service upgrade mechanism
3. Contact system administrator

**Current Role Check:**
```bash
curl -X GET http://localhost:8000/api/v1/auth/me \
  -H "Authorization: Bearer <token>"
```

Response includes user roles:
```json
{
  "id": 15,
  "email": "user@example.com",
  "roles": ["ROLE_USER"],
  "enabled": true,
  "createdAt": "2024-12-19T14:30:00Z"
}
```

---

## Performance Considerations

### Query Performance

**Indexed Queries:**
- Currency pair lookups use database indexes
- Time range queries optimized with timestamp index
- Typical response time: 50-200ms

**Database Indexes:**
```sql
idx_trends ON exchange_rates(
  base_currency,
  target_currency,
  timestamp
)
```

### Caching Strategy

Analytics service does not cache results because:
- Data changes hourly with new rates
- Historical queries need fresh data
- Query performance adequate without caching

### Rate Limiting

Recommended limits for production:
- 100 requests per minute per user
- 1000 requests per hour per user

---

## Code Examples

### Java - Trend Analysis

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class AnalyticsServiceClient {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String baseUrl = "http://localhost:8000";
    private final String token;
    
    public AnalyticsServiceClient(String token) {
        this.token = token;
    }
    
    public TrendsResponse getTrends(
            String from, String to, String period) throws Exception {
        
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
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                response.body(),
                TrendsResponse.class
            );
        } else if (response.statusCode() == 403) {
            throw new RuntimeException("Insufficient permissions. Premium role required.");
        } else {
            throw new RuntimeException("Request failed: " + response.body());
        }
    }
    
    public void printTrendAnalysis(
            String from, String to, String period) throws Exception {
        
        TrendsResponse trend = getTrends(from, to, period);
        
        System.out.println("=== Trend Analysis ===");
        System.out.println("Pair: " + from + "/" + to);
        System.out.println("Period: " + period);
        System.out.println("Start Rate: " + trend.getOldRate());
        System.out.println("End Rate: " + trend.getNewRate());
        System.out.println("Change: " + trend.getChangePercentage() + "%");
        System.out.println("Data Points: " + trend.getDataPoints());
    }
}
```

### Python - Trend Analysis

```python
import requests
from datetime import datetime

class AnalyticsServiceClient:
    def __init__(self, base_url, token):
        self.base_url = base_url
        self.token = token
    
    def get_trends(self, from_currency, to_currency, period):
        headers = {"Authorization": f"Bearer {self.token}"}
        params = {
            "from": from_currency,
            "to": to_currency,
            "period": period
        }
        
        response = requests.get(
            f"{self.base_url}/api/v1/analytics/trends",
            headers=headers,
            params=params
        )
        
        if response.status_code == 403:
            raise Exception("Insufficient permissions. Premium role required.")
        
        response.raise_for_status()
        return response.json()
    
    def print_trend_analysis(self, from_currency, to_currency, period):
        trend = self.get_trends(from_currency, to_currency, period)
        
        print("=== Trend Analysis ===")
        print(f"Pair: {from_currency}/{to_currency}")
        print(f"Period: {period}")
        print(f"Start Rate: {trend['oldRate']}")
        print(f"End Rate: {trend['newRate']}")
        print(f"Change: {trend['changePercentage']}%")
        print(f"Data Points: {trend['dataPoints']}")

# Usage
client = AnalyticsServiceClient("http://localhost:8000", token)
client.print_trend_analysis("USD", "EUR", "7D")
```

### JavaScript - Trend Analysis

```javascript
class AnalyticsServiceClient {
  constructor(baseUrl, token) {
    this.baseUrl = baseUrl;
    this.token = token;
  }
  
  async getTrends(from, to, period) {
    const params = new URLSearchParams({from, to, period});
    
    const response = await fetch(
      `${this.baseUrl}/api/v1/analytics/trends?${params}`,
      {
        headers: {
          'Authorization': `Bearer ${this.token}`
        }
      }
    );
    
    if (response.status === 403) {
      throw new Error('Insufficient permissions. Premium role required.');
    }
    
    if (!response.ok) {
      throw new Error(`Request failed: ${response.status}`);
    }
    
    return await response.json();
  }
  
  async printTrendAnalysis(from, to, period) {
    const trend = await this.getTrends(from, to, period);
    
    console.log('=== Trend Analysis ===');
    console.log(`Pair: ${from}/${to}`);
    console.log(`Period: ${period}`);
    console.log(`Start Rate: ${trend.oldRate}`);
    console.log(`End Rate: ${trend.newRate}`);
    console.log(`Change: ${trend.changePercentage}%`);
    console.log(`Data Points: ${trend.dataPoints}`);
  }
}

// Usage
const client = new AnalyticsServiceClient('http://localhost:8000', token);
await client.printTrendAnalysis('USD', 'EUR', '7D');
```

### Multi-Period Comparison

```javascript
async function compareMultiplePeriods(client, from, to) {
  const periods = ['7D', '30D', '3M', '6M', '1Y'];
  const results = [];
  
  for (const period of periods) {
    try {
      const trend = await client.getTrends(from, to, period);
      results.push({
        period,
        change: trend.changePercentage,
        dataPoints: trend.dataPoints
      });
    } catch (error) {
      console.error(`Failed to get ${period} trend:`, error.message);
    }
  }
  
  console.log(`\n=== Multi-Period Analysis: ${from}/${to} ===`);
  results.forEach(r => {
    console.log(`${r.period}: ${r.change}% (${r.dataPoints} data points)`);
  });
  
  return results;
}

// Usage
await compareMultiplePeriods(client, 'USD', 'EUR');
```

---

## Troubleshooting

### Issue: 403 Forbidden

**Cause:** User role is USER, not PREMIUM_USER or ADMIN

**Resolution:**
1. Verify current role using `/api/v1/auth/me`
2. Request role upgrade from administrator
3. Re-authenticate after role upgrade

### Issue: 404 Insufficient Data

**Cause:** Not enough historical data points in requested period

**Resolution:**
1. Try shorter time period (e.g., 7D instead of 1Y)
2. Try different currency pair
3. Wait for more data accumulation
4. Verify currency pair is actively tracked

### Issue: Invalid Period Format

**Cause:** Period parameter does not match required format

**Resolution:**
Use format `{number}{unit}` where unit is H, D, M, or Y

Valid examples:
- `24H`
- `7D`
- `1M`
- `1Y`

Invalid examples:
- `1 week` (use `7D`)
- `one month` (use `1M`)
- `1day` (use `1D`)

---

## Metrics and Monitoring

### Prometheus Metrics

Available at: `http://localhost:8082/actuator/prometheus`

**Key Metrics:**
- `analytics_trends_success_total` - Successful trend calculations
- `analytics_trends_failure_total` - Failed trend calculations
- `analytics_trends_time_seconds` - Trend calculation duration

### Health Check

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
- Swagger UI: http://localhost:8082/swagger-ui.html
- OpenAPI JSON: http://localhost:8082/v3/api-docs

---

## Database Access

Analytics Service has read-only access to currency database:

**Connection Details:**
- Database: `currency_db`
- User: `analytics_readonly`
- Permissions: SELECT only on exchange_rates table

**Security:**
- Cannot modify data
- Cannot create/drop tables
- Cannot insert/update/delete records

---

## Related Documentation

- [Integration Tutorial](integration-tutorial.md) - Complete workflow with analytics
- [Currency Service API](api-reference-currency.md) - Exchange rate data source
- [Authentication Guide](authentication.md) - Role-based access details
- [Error Handling](error-handling.md) - Error codes and recovery strategies
