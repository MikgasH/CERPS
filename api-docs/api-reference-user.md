# User Service API Reference

This document provides detailed specifications for all User Service endpoints.

## Base Information

**Base URL:** `http://localhost:8081`  
**Via Gateway:** `http://localhost:8000`

**Service Responsibilities:**
- User registration and authentication
- JWT token generation and validation
- User account management
- Password management

## Authentication

All endpoints except registration and login require JWT authentication:

```http
Authorization: Bearer <jwt-token>
```

## Endpoints

### Register User

Create a new user account.

**Endpoint:** `POST /api/v1/auth/register`

**Authentication:** Not required (public endpoint)

**Request Body:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Request Example:**
```bash
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "SecurePass123!"
  }'
```

**Validation Rules:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| email | string | Yes | Valid email format, max 255 chars, unique |
| password | string | Yes | 8-20 chars, must contain: digit, lowercase, uppercase, special char |

**Success Response:**

**Status Code:** `201 Created`

**Body:**
```
User registered successfully
```

**Error Responses:**

**400 Bad Request** - Validation failure
```json
{
  "type": "about:blank",
  "title": "Validation error",
  "status": 400,
  "detail": "password: Password must contain at least one digit, one lowercase, one uppercase letter and one special character"
}
```

**409 Conflict** - Email already exists
```json
{
  "type": "about:blank",
  "title": "User already exists",
  "status": 409,
  "detail": "Email 'newuser@example.com' is already registered"
}
```

---

### Login

Authenticate user and receive JWT token.

**Endpoint:** `POST /api/v1/auth/login`

**Authentication:** Not required (public endpoint)

**Request Body:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Request Example:**
```bash
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "SecurePass123!"
  }'
```

**Validation Rules:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| email | string | Yes | Valid email format |
| password | string | Yes | Non-empty |

**Success Response:**

**Status Code:** `200 OK`

**Body:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJuZXd1c2VyQGV4YW1wbGUuY29tIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTcwMzA3MDAwMCwiZXhwIjoxNzAzMTU2NDAwfQ.signature",
  "type": "Bearer",
  "email": "newuser@example.com",
  "roles": ["ROLE_USER"]
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| token | string | JWT access token (expires in 24 hours) |
| type | string | Token type, always "Bearer" |
| email | string | User's email address |
| roles | array | User's assigned roles |

**Error Responses:**

**401 Unauthorized** - Invalid credentials
```json
{
  "type": "about:blank",
  "title": "Authentication failed",
  "status": 401,
  "detail": "Invalid username or password"
}
```

**401 Unauthorized** - Account disabled
```json
{
  "type": "about:blank",
  "title": "Account disabled",
  "status": 401,
  "detail": "User account is disabled"
}
```

---

### Get Current User

Retrieve information about the authenticated user.

**Endpoint:** `GET /api/v1/auth/me`

**Authentication:** Required

**Roles:** USER, PREMIUM_USER, ADMIN

**Request Example:**
```bash
curl -X GET http://localhost:8000/api/v1/auth/me \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Success Response:**

**Status Code:** `200 OK`

**Body:**
```json
{
  "id": 15,
  "email": "newuser@example.com",
  "roles": ["ROLE_USER"],
  "enabled": true,
  "createdAt": "2024-12-19T14:30:00Z"
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| id | integer | User's unique identifier |
| email | string | User's email address |
| roles | array | User's assigned roles |
| enabled | boolean | Account enabled status |
| createdAt | string | Account creation timestamp (ISO 8601) |

**Error Responses:**

**401 Unauthorized** - Missing or invalid token
```json
{
  "error": "Invalid JWT token",
  "status": 401
}
```

---

### Change Password

Change the password for the authenticated user.

**Endpoint:** `POST /api/v1/auth/change-password`

**Authentication:** Required

**Roles:** USER, PREMIUM_USER, ADMIN

**Request Body:**
```json
{
  "currentPassword": "string",
  "newPassword": "string"
}
```

**Request Example:**
```bash
curl -X POST http://localhost:8000/api/v1/auth/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "currentPassword": "SecurePass123!",
    "newPassword": "NewSecurePass456!"
  }'
```

**Validation Rules:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| currentPassword | string | Yes | Non-empty |
| newPassword | string | Yes | 8-20 chars, must contain: digit, lowercase, uppercase, special char |

**Success Response:**

**Status Code:** `200 OK`

**Body:**
```
Password changed successfully
```

**Error Responses:**

**400 Bad Request** - Validation failure
```json
{
  "type": "about:blank",
  "title": "Validation error",
  "status": 400,
  "detail": "newPassword: Password must contain at least one digit, one lowercase, one uppercase letter and one special character"
}
```

**401 Unauthorized** - Invalid current password
```json
{
  "type": "about:blank",
  "title": "Authentication failed",
  "status": 401,
  "detail": "Current password is incorrect"
}
```

**400 Bad Request** - Same password
```json
{
  "type": "about:blank",
  "title": "Invalid argument",
  "status": 400,
  "detail": "New password must be different from the current password"
}
```

---

### Validate Token (Internal)

Internal endpoint for inter-service token validation.

**Endpoint:** `POST /api/internal/auth/validate`

**Authentication:** Not required (internal use only)

**Note:** This endpoint is used by other microservices to validate JWT tokens. It should not be exposed publicly.

**Request Headers:**
```
Authorization: Bearer <jwt-token>
```

**Request Example:**
```bash
curl -X POST http://localhost:8081/api/internal/auth/validate \
  -H "Authorization: Bearer <jwt-token>"
```

**Success Response:**

**Status Code:** `200 OK`

**Body:**
```json
{
  "valid": true,
  "username": "user@example.com",
  "roles": ["ROLE_USER"]
}
```

**Invalid Token Response:**

**Status Code:** `200 OK`

**Body:**
```json
{
  "valid": false,
  "username": null,
  "roles": []
}
```

---

## Data Models

### User

```json
{
  "id": "integer",
  "email": "string",
  "password": "string (hashed)",
  "enabled": "boolean",
  "createdAt": "string (ISO 8601)",
  "roles": ["string"]
}
```

**Notes:**
- Password is stored as BCrypt hash
- Password never returned in API responses
- Roles stored in separate table with many-to-many relationship

### Role

```json
{
  "id": "integer",
  "name": "string",
  "createdAt": "string (ISO 8601)"
}
```

**Available Roles:**
- `ROLE_USER` - Standard user access
- `ROLE_PREMIUM_USER` - Premium features access
- `ROLE_ADMIN` - Administrative access

---

## Password Requirements

All passwords must meet the following requirements:

**Length:**
- Minimum: 8 characters
- Maximum: 20 characters

**Complexity:**
- At least one digit (0-9)
- At least one lowercase letter (a-z)
- At least one uppercase letter (A-Z)
- At least one special character (@#$%^&+=!)

**Examples:**

Valid passwords:
- `SecurePass123!`
- `MyP@ssw0rd`
- `Test1234#`

Invalid passwords:
- `short1!` (too short)
- `nocapitals123!` (no uppercase)
- `NOLOWERCASE123!` (no lowercase)
- `NoDigitsHere!` (no digits)
- `NoSpecial123` (no special character)

**Validation Pattern:**
```regex
^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$
```

---

## Common Workflows

### User Registration Flow

```
1. POST /api/v1/auth/register
   └─> 201 Created: "User registered successfully"

2. POST /api/v1/auth/login
   └─> 200 OK: {token, email, roles}

3. GET /api/v1/auth/me
   └─> 200 OK: {id, email, roles, enabled, createdAt}
```

### Password Change Flow

```
1. POST /api/v1/auth/login
   └─> 200 OK: {token, email, roles}

2. POST /api/v1/auth/change-password
   └─> 200 OK: "Password changed successfully"

3. POST /api/v1/auth/login (with new password)
   └─> 200 OK: {token, email, roles}
```

Note: Existing JWT tokens remain valid after password change until expiration.

---

## Rate Limiting

Currently not implemented. Recommended limits for production:

- Registration: 10 requests per hour per IP
- Login: 20 requests per hour per IP
- Password change: 5 requests per hour per user
- Get user info: 100 requests per minute per user

---

## Security Considerations

**Password Storage:**
- Passwords hashed using BCrypt with cost factor 12
- Original passwords never stored or logged
- Hashes cannot be reversed to plaintext

**JWT Tokens:**
- Signed with HS256 algorithm
- 256-bit secret key
- 24-hour expiration
- No refresh mechanism (re-authentication required)

**Best Practices:**
- Always use HTTPS in production
- Store tokens securely on client side
- Never log tokens or passwords
- Implement rate limiting
- Monitor for brute force attempts

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
- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs

---

## Code Examples

### Java - Complete Registration and Login

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserServiceClient {
    private static final String BASE_URL = "http://localhost:8000";
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    
    public void registerUser(String email, String password) throws Exception {
        String payload = String.format(
            "{\"email\":\"%s\",\"password\":\"%s\"}",
            email, password
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/auth/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        if (response.statusCode() == 201) {
            System.out.println("User registered successfully");
        } else {
            throw new RuntimeException("Registration failed: " + response.body());
        }
    }
    
    public String login(String email, String password) throws Exception {
        String payload = String.format(
            "{\"email\":\"%s\",\"password\":\"%s\"}",
            email, password
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        if (response.statusCode() == 200) {
            JsonNode json = mapper.readTree(response.body());
            return json.get("token").asText();
        } else {
            throw new RuntimeException("Login failed: " + response.body());
        }
    }
}
```

### Python - Registration and Login

```python
import requests

class UserServiceClient:
    def __init__(self, base_url):
        self.base_url = base_url
    
    def register(self, email, password):
        response = requests.post(
            f"{self.base_url}/api/v1/auth/register",
            json={"email": email, "password": password}
        )
        response.raise_for_status()
        return response.text
    
    def login(self, email, password):
        response = requests.post(
            f"{self.base_url}/api/v1/auth/login",
            json={"email": email, "password": password}
        )
        response.raise_for_status()
        return response.json()["token"]

# Usage
client = UserServiceClient("http://localhost:8000")
client.register("user@example.com", "SecurePass123!")
token = client.login("user@example.com", "SecurePass123!")
```

### JavaScript - Registration and Login

```javascript
class UserServiceClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
  }
  
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
    return data.token;
  }
}

// Usage
const client = new UserServiceClient('http://localhost:8000');
await client.register('user@example.com', 'SecurePass123!');
const token = await client.login('user@example.com', 'SecurePass123!');
```

---

## Related Documentation

- [Authentication Guide](authentication.md) - Detailed JWT implementation
- [Integration Tutorial](integration-tutorial.md) - Complete workflow examples
- [Error Handling](error-handling.md) - Error codes and recovery strategies
