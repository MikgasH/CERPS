# 3. User Guide

This section provides practical guidance for using the CERPS API.

## Contents

- [Features Overview](#features-overview)
- [Quick Start](#quick-start)
- [Common Workflows](#common-workflows)
- [Swagger UI Guide](#swagger-ui-guide)
- [FAQ](#faq)

## Features Overview

| Feature | Description | Access |
|---------|-------------|--------|
| Currency Conversion | Convert amounts between 11+ currencies | All users |
| Supported Currencies | View available currency codes | All users |
| Trend Analysis | Historical rate trends (7D to 1Y) | Premium users |
| User Profile | View account information | All users |
| Password Change | Update account password | All users |
| Add Currency | Extend supported currencies | Admin only |
| Refresh Rates | Manual rate update | Admin only |
| API Key Management | Manage provider credentials | Admin only |

## Quick Start

### 1. Start the System

```bash
# Clone repository
git clone https://github.com/MikgasH/exchange.git
cd cerps-hashkin

# Configure environment
cp .env.example .env
# Edit .env with your keys (generate with: openssl rand -base64 32)

# Start all services
docker-compose up -d

# Wait for services to be healthy (~2 minutes)
docker-compose ps
```

### 2. Register Account

```bash
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your@email.com",
    "password": "SecurePass123!"
  }'
```

**Password Requirements:**
- 8-20 characters
- At least one digit (0-9)
- At least one lowercase letter (a-z)
- At least one uppercase letter (A-Z)
- At least one special character (@#$%^&+=!)

### 3. Login

```bash
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your@email.com",
    "password": "SecurePass123!"
  }'
```

**Save the token** from the response for authenticated requests.

### 4. Convert Currency

```bash
curl -X POST http://localhost:8000/api/v1/currencies/convert \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "amount": 1000,
    "from": "USD",
    "to": "EUR"
  }'
```

## Common Workflows

### Currency Conversion

1. Get list of supported currencies
2. Convert desired amount
3. Use exchange rate for calculations

```bash
# Step 1: List currencies
curl -X GET http://localhost:8000/api/v1/currencies \
  -H "Authorization: Bearer $TOKEN"

# Response: ["AUD", "BYN", "CAD", "CHF", "CNY", "EUR", "GBP", "JPY", "NZD", "SEK", "USD"]

# Step 2: Convert
curl -X POST http://localhost:8000/api/v1/currencies/convert \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount": 500, "from": "GBP", "to": "USD"}'

# Response:
# {
#   "success": true,
#   "originalAmount": 500,
#   "fromCurrency": "GBP",
#   "toCurrency": "USD",
#   "convertedAmount": 635.50,
#   "exchangeRate": 1.271,
#   "timestamp": "2024-12-19T14:30:00Z"
# }
```

### Trend Analysis (Premium Users)

```bash
# 7-day trend
curl -X GET "http://localhost:8000/api/v1/analytics/trends?from=USD&to=EUR&period=7D" \
  -H "Authorization: Bearer $PREMIUM_TOKEN"

# 30-day trend
curl -X GET "http://localhost:8000/api/v1/analytics/trends?from=USD&to=EUR&period=30D" \
  -H "Authorization: Bearer $PREMIUM_TOKEN"

# Available periods: 7D, 30D, 1M, 3M, 6M, 1Y
```

### Password Change

```bash
curl -X POST http://localhost:8000/api/v1/auth/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "currentPassword": "SecurePass123!",
    "newPassword": "NewSecurePass456!"
  }'
```

## Swagger UI Guide

### Accessing Swagger UI

Open in browser:
- **API Gateway**: http://localhost:8000/swagger-ui.html
- **User Service**: http://localhost:8081/swagger-ui.html
- **Currency Service**: http://localhost:8080/swagger-ui.html
- **Analytics Service**: http://localhost:8082/swagger-ui.html

### Using Swagger UI

1. **Authorize**: Click "Authorize" button, enter `Bearer YOUR_TOKEN`
2. **Select Endpoint**: Expand endpoint section
3. **Try It Out**: Click "Try it out" button
4. **Fill Parameters**: Enter required values
5. **Execute**: Click "Execute" to send request
6. **View Response**: See response below

### Authentication in Swagger

1. First call `/api/v1/auth/login` endpoint
2. Copy `token` from response
3. Click "Authorize" button at top
4. Enter: `Bearer <your-token>`
5. Click "Authorize"
6. All subsequent requests will include token

## Supported Currencies

| Code | Currency | Symbol |
|------|----------|--------|
| AUD | Australian Dollar | A$ |
| BYN | Belarusian Ruble | Br |
| CAD | Canadian Dollar | C$ |
| CHF | Swiss Franc | Fr |
| CNY | Chinese Yuan | ¥ |
| EUR | Euro | € |
| GBP | British Pound | £ |
| JPY | Japanese Yen | ¥ |
| NZD | New Zealand Dollar | NZ$ |
| SEK | Swedish Krona | kr |
| USD | US Dollar | $ |

## User Roles

| Role | Capabilities |
|------|--------------|
| USER | View currencies, convert amounts, manage profile |
| PREMIUM_USER | All USER features + trend analysis |
| ADMIN | All features + add currencies, refresh rates, manage API keys |

## FAQ

### Authentication

**Q: How long is my token valid?**
A: JWT tokens expire after 24 hours. You need to login again to get a new token.

**Q: Why do I get 401 Unauthorized?**
A: Your token is missing, invalid, or expired. Login again to get a fresh token.

**Q: Can I change my email?**
A: Email changes are not supported. Register a new account if needed.

### Currency Conversion

**Q: Why is the rate different from Google?**
A: CERPS aggregates rates from multiple providers and calculates the median. Rates may differ from individual sources.

**Q: How often are rates updated?**
A: Exchange rates are updated hourly automatically.

**Q: Can I add new currencies?**
A: Only administrators can add new supported currencies.

### Analytics

**Q: Why do I get 403 Forbidden on trends?**
A: Trend analysis requires PREMIUM_USER or ADMIN role. Contact administrator for upgrade.

**Q: What does "Insufficient data" mean?**
A: The requested time period doesn't have enough historical data. Try a shorter period.

### Technical

**Q: Why are services unhealthy?**
A: Services need 1-2 minutes to start. Run `docker-compose ps` to check status.

**Q: How do I restart a service?**
A: Run `docker-compose restart <service-name>` (e.g., `docker-compose restart currency-service`)

**Q: Where are the logs?**
A: Run `docker-compose logs -f <service-name>` to view logs.

**Q: How do I reset everything?**
A: Run `docker-compose down -v` to remove all containers and data, then `docker-compose up -d` to start fresh.

## Error Codes

| Status | Meaning | Action |
|--------|---------|--------|
| 200 | Success | Request completed |
| 201 | Created | Resource created (registration) |
| 400 | Bad Request | Check request format/values |
| 401 | Unauthorized | Login again |
| 403 | Forbidden | Need higher role |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate (e.g., email exists) |
| 503 | Service Unavailable | Try again later |

## Getting Help

- **API Documentation**: See `/docs` folder in repository
- **Swagger UI**: Interactive testing at http://localhost:8000/swagger-ui.html
- **Source Code**: https://github.com/MikgasH/exchange
