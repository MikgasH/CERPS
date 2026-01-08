# Getting Started

This guide provides instructions for setting up and making initial API calls to the CERPS system.

## Prerequisites

Before beginning, ensure the following tools are installed:

- Docker and Docker Compose
- API testing tool (curl, Postman, or similar)
- Git

## Installation

### Clone Repository

```bash
git clone <repository-url>
cd cerps-hashkin
```

### Configure Environment

Create environment file from template:

```bash
cp .env.example .env
```

Edit the `.env` file and configure required values:

```env
# PostgreSQL Configuration
POSTGRES_DB=exchange_rates_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password

# JWT Configuration
JWT_SECRET=your_base64_encoded_secret_key

# Encryption Master Key
ENCRYPTION_MASTER_KEY=your_base64_encoded_256bit_key
```

### Start Services

Launch all services using Docker Compose:

```bash
docker-compose up -d
```

Wait approximately 1-2 minutes for services to initialize. Verify service status:

```bash
docker-compose ps
```

All services should display `healthy` status.

## Verify Installation

Check health status of each service:

```bash
# API Gateway
curl http://localhost:8000/actuator/health

# User Service
curl http://localhost:8081/actuator/health

# Currency Service
curl http://localhost:8080/actuator/health

# Analytics Service
curl http://localhost:8082/actuator/health
```

Expected response for each service:

```json
{
  "status": "UP"
}
```

## Initial API Workflow

### Register User

Create a new user account:

```bash
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'
```

**Response:**
```
HTTP/1.1 201 Created
Content-Type: text/plain

User registered successfully
```

**Password Requirements:**
- Minimum 8 characters
- At least one digit
- At least one lowercase letter
- At least one uppercase letter
- At least one special character (@#$%^&+=!)

### Authenticate

Obtain JWT token:

```bash
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTYzOTU2MDAwMCwiZXhwIjoxNjM5NjQ2NDAwfQ.signature",
  "type": "Bearer",
  "email": "user@example.com",
  "roles": ["ROLE_USER"]
}
```

**Important:** Store the token value for subsequent authenticated requests. Tokens expire after 24 hours.

### Currency Conversion

Convert amount between currencies using obtained JWT token:

```bash
curl -X POST http://localhost:8000/api/v1/currencies/convert \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "amount": 100,
    "from": "USD",
    "to": "EUR"
  }'
```

Replace `<your-jwt-token>` with the token received from login.

**Response:**
```json
{
  "success": true,
  "originalAmount": 100,
  "fromCurrency": "USD",
  "toCurrency": "EUR",
  "convertedAmount": 92.456789,
  "exchangeRate": 0.924568,
  "timestamp": "2024-12-19T10:30:00Z"
}
```

### List Supported Currencies

Retrieve available currency codes:

```bash
curl -X GET http://localhost:8000/api/v1/currencies \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Response:**
```json
["AUD", "BYN", "CAD", "CHF", "CNY", "EUR", "GBP", "JPY", "NZD", "SEK", "USD"]
```

### Get User Information

Verify current user details:

```bash
curl -X GET http://localhost:8000/api/v1/auth/me \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Response:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "roles": ["ROLE_USER"],
  "enabled": true,
  "createdAt": "2024-12-19T10:25:00Z"
}
```

## Troubleshooting

### Connection Refused Error

**Cause:** Services are still initializing.

**Solution:** Wait 30-60 seconds and retry the request. Monitor service logs:

```bash
docker-compose logs -f
```

### 401 Unauthorized Error

**Cause:** JWT token is missing, invalid, or expired.

**Solution:**
1. Verify Authorization header format: `Authorization: Bearer <token>`
2. Obtain new token by logging in again
3. Check token expiration (24 hour lifetime)

### 400 Bad Request Error

**Cause:** Request payload does not match expected format.

**Solution:**
1. Verify JSON syntax
2. Check required fields are present
3. Validate data types match API specification
4. Refer to [API Reference](api-reference-user.md) for endpoint requirements

### Service Unhealthy Status

**Cause:** Service failed to start or lost database connection.

**Solution:**
1. Check service logs: `docker-compose logs <service-name>`
2. Verify PostgreSQL containers are running
3. Restart services: `docker-compose restart`

## Next Steps

For complete integration workflow, refer to the [Integration Tutorial](integration-tutorial.md).

For detailed endpoint specifications, consult the [API Reference](api-reference-user.md) documentation.

For authentication implementation details, see the [Authentication Guide](authentication.md).
