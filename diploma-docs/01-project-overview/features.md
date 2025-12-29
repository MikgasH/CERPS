# Features

## Feature Overview

| Feature | Description | User Roles | Priority |
|---------|-------------|------------|----------|
| User Authentication | Register, login, JWT tokens | All | Must Have |
| Currency Conversion | Convert amounts between currencies | USER+ | Must Have |
| Currency List | View supported currencies | USER+ | Must Have |
| Trend Analysis | Historical rate trends | PREMIUM+ | Should Have |
| Currency Management | Add new currencies | ADMIN | Should Have |
| Rate Refresh | Manual rate update trigger | ADMIN | Should Have |
| API Key Management | Encrypted provider key storage | ADMIN | Should Have |

## User Stories

### Authentication

**US-01: User Registration**
> As a new user, I want to register with my email and password, so that I can access the currency conversion service.

Acceptance Criteria:
- [x] Email must be valid format and unique
- [x] Password must meet complexity requirements (8+ chars, digit, upper, lower, special)
- [x] User receives ROLE_USER by default
- [x] Duplicate email returns 409 Conflict

**US-02: User Login**
> As a registered user, I want to login with my credentials, so that I can receive a JWT token for API access.

Acceptance Criteria:
- [x] Valid credentials return JWT token
- [x] Token contains user email and roles
- [x] Token expires after 24 hours
- [x] Invalid credentials return 401 Unauthorized

**US-03: View Profile**
> As an authenticated user, I want to view my profile information, so that I can verify my account details.

Acceptance Criteria:
- [x] Returns user ID, email, roles, enabled status, creation date
- [x] Requires valid JWT token
- [x] Invalid token returns 401 Unauthorized

### Currency Operations

**US-04: List Currencies**
> As a user, I want to see all supported currencies, so that I know which conversions are available.

Acceptance Criteria:
- [x] Returns array of ISO 4217 currency codes
- [x] Requires authentication
- [x] List reflects admin-added currencies

**US-05: Convert Currency**
> As a user, I want to convert an amount from one currency to another, so that I can know the equivalent value.

Acceptance Criteria:
- [x] Accepts amount, source currency, target currency
- [x] Returns converted amount with 6 decimal precision
- [x] Returns applied exchange rate and timestamp
- [x] Same currency conversion returns 1:1 rate
- [x] Unsupported currency returns 400 Bad Request

**US-06: View Trends**
> As a premium user, I want to see currency pair trends over time, so that I can analyze rate movements.

Acceptance Criteria:
- [x] Supports periods: 7D, 30D, 1M, 3M, 6M, 1Y
- [x] Returns start rate, end rate, percentage change
- [x] Returns data point count for period
- [x] Requires PREMIUM_USER or ADMIN role
- [x] USER role returns 403 Forbidden

### Administration

**US-07: Add Currency**
> As an admin, I want to add new supported currencies, so that users can convert additional currency pairs.

Acceptance Criteria:
- [x] Accepts valid ISO 4217 currency code
- [x] Invalid code returns 400 Bad Request
- [x] Duplicate currency handled gracefully
- [x] Requires ADMIN role

**US-08: Refresh Rates**
> As an admin, I want to manually trigger rate refresh, so that I can ensure rates are current.

Acceptance Criteria:
- [x] Fetches rates from all configured providers
- [x] Calculates median rates
- [x] Updates database and cache
- [x] Requires ADMIN role

**US-09: Manage API Keys**
> As an admin, I want to securely store and manage external API keys, so that provider credentials are protected.

Acceptance Criteria:
- [x] Create, read, update, delete provider keys
- [x] Keys encrypted with AES-256-GCM before storage
- [x] Key rotation support
- [x] Actual keys never returned in responses

## MoSCoW Prioritization

### Must Have (Implemented)
- User registration and authentication
- JWT token generation and validation
- Currency conversion with multi-provider aggregation
- Median rate calculation
- Fallback to mock services
- Role-based access control
- 70%+ test coverage
- Docker containerization
- API documentation

### Should Have (Implemented)
- Trend analysis for premium users
- Admin currency management
- Encrypted API key storage
- Manual rate refresh
- Password change

### Could Have (Partially Implemented)
- Rate caching (1-hour TTL) Completed
- Correlation ID tracing Completed
- Health check endpoints Completed

### Won't Have (This Release)
- Web/Mobile UI
- Real-time WebSocket updates
- Cryptocurrency support
- Rate predictions
- Multi-language support
