# Authentication Guide

This document provides detailed information about authentication and authorization mechanisms in the CERPS API.

## Overview

CERPS uses JWT (JSON Web Token) based authentication with role-based access control (RBAC). All API requests, except registration and login, require a valid JWT token.

## Authentication Flow

### Registration

New users register through the registration endpoint:

**Endpoint:** `POST /api/v1/auth/register`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Password Requirements:**
- Minimum 8 characters
- Maximum 20 characters
- At least one digit (0-9)
- At least one lowercase letter (a-z)
- At least one uppercase letter (A-Z)
- At least one special character (@#$%^&+=!)

**Response:**
```
HTTP/1.1 201 Created

User registered successfully
```

**Validation Rules:**
- Email must be valid format (RFC 5322)
- Email must be unique (not already registered)
- Password must meet complexity requirements

**Process:**
1. System validates email format and uniqueness
2. Password complexity checked against requirements
3. Password hashed using BCrypt (strength 12)
4. User record created with default ROLE_USER
5. Account enabled by default

### Login

Authenticate with credentials to receive JWT token:

**Endpoint:** `POST /api/v1/auth/login`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTcwMzA3MDAwMCwiZXhwIjoxNzAzMTU2NDAwfQ.signature",
  "type": "Bearer",
  "email": "user@example.com",
  "roles": ["ROLE_USER"]
}
```

**Process:**
1. System queries user by email
2. Password verified against BCrypt hash
3. JWT token generated with claims:
    - Subject: user email
    - Roles: user's assigned roles
    - Issued At: current timestamp
    - Expiration: 24 hours from issue
4. Token and user information returned

## JWT Token Structure

### Token Format

JWT tokens consist of three Base64-encoded parts separated by dots:

```
header.payload.signature
```

Example:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTcwMzA3MDAwMCwiZXhwIjoxNzAzMTU2NDAwfQ.
K7vSLhZzA8xQM_YlHj9DzZl8RvZqPn5dW3bK2vF9E3s
```

### Header

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

- `alg`: Algorithm used (HMAC SHA-256)
- `typ`: Token type (JWT)

### Payload

```json
{
  "sub": "user@example.com",
  "roles": ["ROLE_USER"],
  "iat": 1703070000,
  "exp": 1703156400
}
```

**Claims:**
- `sub` (Subject): User's email address
- `roles`: Array of role identifiers
- `iat` (Issued At): Unix timestamp of token creation
- `exp` (Expiration): Unix timestamp when token expires

### Signature

```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

Token signed with 256-bit secret key using HMAC-SHA256 algorithm.

## Using JWT Tokens

### Request Format

Include JWT token in Authorization header:

```http
GET /api/v1/currencies HTTP/1.1
Host: localhost:8000
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Format Rules:**
- Header name: `Authorization`
- Value format: `Bearer <token>`
- Space required between "Bearer" and token
- Token should not be modified or truncated

### Token Validation

API Gateway validates tokens on each request:

1. Extract token from Authorization header
2. Verify token signature using secret key
3. Check token expiration
4. Extract user email from subject claim
5. Extract roles from roles claim
6. Propagate user context to downstream services

**Validation Failures:**
- Missing token: 401 Unauthorized
- Invalid signature: 401 Unauthorized
- Expired token: 401 Unauthorized
- Malformed token: 401 Unauthorized

### Token Lifespan

- Default expiration: 24 hours
- No automatic refresh mechanism
- Re-authentication required after expiration
- Token cannot be revoked before expiration

**Best Practice:** Request new token before expiration for seamless user experience.

## Role-Based Access Control

### Available Roles

**ROLE_USER:**
- Basic authenticated access
- Currency conversion
- Currency list retrieval
- Account management

**ROLE_PREMIUM_USER:**
- All ROLE_USER permissions
- Analytics and trends access
- Historical data queries

**ROLE_ADMIN:**
- All ROLE_PREMIUM_USER permissions
- Currency management
- Rate refresh control
- System configuration
- API key management

### Role Assignment

Roles assigned during user lifecycle:

**Registration:**
- Default role: ROLE_USER
- Automatic assignment on account creation

**Role Upgrade:**
- Manual upgrade by system administrator
- Database modification required
- No API endpoint for role changes

**Multiple Roles:**
- Users can have multiple roles
- Cumulative permissions
- Most permissive role applies

### Endpoint Authorization

Authorization enforced at service level:

**User Service:**
```java
@PreAuthorize("hasAnyRole('USER', 'PREMIUM_USER', 'ADMIN')")
public ResponseEntity<UserInfoResponse> getCurrentUser()

@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<String> adminOperation()
```

**Currency Service:**
```java
@PreAuthorize("hasAnyRole('USER', 'PREMIUM_USER', 'ADMIN')")
public ResponseEntity<ConversionResponse> convertCurrency()

@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<String> addCurrency()
```

**Analytics Service:**
```java
@PreAuthorize("hasAnyRole('PREMIUM_USER', 'ADMIN')")
public ResponseEntity<TrendsResponse> getTrends()
```

### Authorization Errors

**403 Forbidden Response:**
```json
{
  "type": "about:blank",
  "title": "Access denied",
  "status": 403,
  "detail": "You don't have permission to access this resource"
}
```

Occurs when:
- Valid token present
- User authenticated successfully
- Insufficient role permissions for endpoint

## Security Considerations

### Token Storage

**Client-Side Storage Options:**

**Recommended - Memory (Variable):**
```javascript
let jwtToken = null;

function setToken(token) {
  jwtToken = token;
}

function getToken() {
  return jwtToken;
}
```

**Not Recommended - LocalStorage:**
```javascript
// Vulnerable to XSS attacks
localStorage.setItem('jwt', token);
```

**Not Recommended - Cookies without httpOnly:**
```javascript
// Vulnerable to XSS attacks
document.cookie = `jwt=${token}`;
```

**Alternative - httpOnly Cookies:**
- Set by server in Set-Cookie header
- Not accessible to JavaScript
- Automatically sent with requests
- Requires server-side session handling

### Token Security

**Best Practices:**

1. **Never log tokens:**
```java
// Bad
log.info("User token: " + token);

// Good
log.info("User authenticated: " + userEmail);
```

2. **Never expose in URLs:**
```
// Bad
GET /api/v1/currencies?token=eyJhbGci...

// Good
GET /api/v1/currencies
Authorization: Bearer eyJhbGci...
```

3. **Validate on every request:**
- Don't cache validation results
- Always verify signature
- Check expiration on each use

4. **Use HTTPS in production:**
- Prevents token interception
- Protects against man-in-the-middle attacks
- Required for production deployment

5. **Implement token refresh:**
```java
if (isTokenExpiring(token)) {
  token = refreshToken();
}
```

### Password Security

**Hashing Algorithm:**
- BCrypt with cost factor 12
- Automatic salt generation
- Adaptive hashing (slower as hardware improves)

**Password Storage:**
```java
// Registration
String hashedPassword = passwordEncoder.encode(plainPassword);
user.setPassword(hashedPassword);

// Login verification
boolean matches = passwordEncoder.matches(
  providedPassword,
  storedHashedPassword
);
```

**Password Change Flow:**

1. Verify current password
2. Validate new password requirements
3. Ensure new password differs from current
4. Hash new password
5. Update database
6. Existing tokens remain valid until expiration

### API Security

**Request Validation:**
- Content-Type verification
- JSON schema validation
- Input sanitization
- SQL injection prevention via parameterized queries

**Rate Limiting:**
- Not currently implemented
- Recommended for production:
    - 100 requests per minute per IP
    - 1000 requests per hour per user

**CORS Configuration:**
- Currently allows all origins
- Production should restrict to known domains

## Integration Examples

### Java Client

```java
public class CerpsAuthClient {
    private final HttpClient client = HttpClient.newHttpClient();
    private String jwtToken;
    
    public void authenticate(String email, String password) throws Exception {
        String loginPayload = String.format(
            "{\"email\":\"%s\",\"password\":\"%s\"}",
            email, password
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8000/api/v1/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginPayload))
            .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        JsonObject json = JsonParser.parseString(response.body())
            .getAsJsonObject();
        this.jwtToken = json.get("token").getAsString();
    }
    
    public HttpRequest.Builder createAuthenticatedRequest(String url) {
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + jwtToken);
    }
}
```

### Python Client

```python
import requests
from datetime import datetime, timedelta

class CerpsAuthClient:
    def __init__(self, base_url):
        self.base_url = base_url
        self.token = None
        self.token_expiry = None
    
    def authenticate(self, email, password):
        response = requests.post(
            f"{self.base_url}/api/v1/auth/login",
            json={"email": email, "password": password}
        )
        response.raise_for_status()
        
        data = response.json()
        self.token = data["token"]
        # JWT expires in 24 hours
        self.token_expiry = datetime.now() + timedelta(hours=24)
    
    def is_token_valid(self):
        if not self.token:
            return False
        if datetime.now() >= self.token_expiry:
            return False
        return True
    
    def get_headers(self):
        if not self.is_token_valid():
            raise Exception("Token invalid or expired")
        return {"Authorization": f"Bearer {self.token}"}
    
    def make_request(self, method, endpoint, **kwargs):
        if not self.is_token_valid():
            raise Exception("Authentication required")
        
        url = f"{self.base_url}{endpoint}"
        headers = self.get_headers()
        
        return requests.request(
            method, url,
            headers=headers,
            **kwargs
        )
```

### JavaScript Client

```javascript
class CerpsAuthClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
    this.token = null;
    this.tokenExpiry = null;
  }
  
  async authenticate(email, password) {
    const response = await fetch(`${this.baseUrl}/api/v1/auth/login`, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({email, password})
    });
    
    if (!response.ok) {
      throw new Error('Authentication failed');
    }
    
    const data = await response.json();
    this.token = data.token;
    this.tokenExpiry = Date.now() + (24 * 60 * 60 * 1000); // 24 hours
  }
  
  isTokenValid() {
    return this.token && Date.now() < this.tokenExpiry;
  }
  
  getAuthHeaders() {
    if (!this.isTokenValid()) {
      throw new Error('Token invalid or expired');
    }
    return {'Authorization': `Bearer ${this.token}`};
  }
  
  async makeRequest(method, endpoint, options = {}) {
    if (!this.isTokenValid()) {
      throw new Error('Authentication required');
    }
    
    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      method,
      headers: {
        ...this.getAuthHeaders(),
        ...options.headers
      },
      ...options
    });
    
    return response;
  }
}
```

## Troubleshooting

### Issue: 401 Unauthorized

**Possible Causes:**
1. Token missing from request
2. Token expired (>24 hours old)
3. Token signature invalid
4. Token malformed

**Resolution:**
- Verify Authorization header present
- Check header format: `Bearer <token>`
- Re-authenticate if token expired
- Ensure token not modified

### Issue: 403 Forbidden

**Possible Causes:**
1. Insufficient role permissions
2. Endpoint requires higher privilege

**Resolution:**
- Check endpoint documentation for required roles
- Verify user roles in JWT payload
- Request role upgrade if necessary

### Issue: Password Validation Failed

**Possible Causes:**
1. Password too short (<8 characters)
2. Missing required character types
3. Password too long (>20 characters)

**Resolution:**
- Include at least one: digit, lowercase, uppercase, special character
- Meet minimum length requirement
- Stay within maximum length

### Issue: Email Already Registered

**Possible Causes:**
1. Email previously registered
2. Duplicate registration attempt

**Resolution:**
- Use different email address
- Login with existing credentials
- Reset password if forgotten

## Advanced Topics

### Custom JWT Claims

Current implementation includes:
- Standard claims: sub, iat, exp
- Custom claim: roles

Future enhancements may include:
- User ID claim
- Permissions array
- Organization ID
- Session ID for revocation

### Token Refresh Strategy

**Current:** No refresh mechanism

**Recommended Implementation:**
1. Short-lived access tokens (15 minutes)
2. Long-lived refresh tokens (7 days)
3. Refresh endpoint to exchange refresh token for new access token
4. Refresh token rotation on use

### Multi-Factor Authentication

**Current:** Not implemented

**Future Consideration:**
1. TOTP (Time-based One-Time Password)
2. SMS verification
3. Email verification
4. Backup codes

### OAuth 2.0 Integration

**Current:** Not implemented

**Potential Integration:**
- Google OAuth
- GitHub OAuth
- Microsoft OAuth
- Custom OAuth provider

## References

- [JWT Introduction](https://jwt.io/introduction)
- [RFC 7519 - JWT Specification](https://tools.ietf.org/html/rfc7519)
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [BCrypt Algorithm](https://en.wikipedia.org/wiki/Bcrypt)
