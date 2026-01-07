# Project Scope

## In Scope

### Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | User registration with email/password | Must Have | Done |
| FR-02 | JWT-based authentication | Must Have | Done |
| FR-03 | Role-based access control (USER, PREMIUM_USER, ADMIN) | Must Have | Done |
| FR-04 | Currency conversion between supported pairs | Must Have | Done |
| FR-05 | List supported currencies | Must Have | Done |
| FR-06 | Multi-provider rate aggregation | Must Have | Done |
| FR-07 | Median rate calculation | Must Have | Done |
| FR-08 | Automatic fallback to mock services | Must Have | Done |
| FR-09 | Trend analysis (7D, 30D, 1Y periods) | Should Have | Done |
| FR-10 | Admin: Add new currencies | Should Have | Done |
| FR-11 | Admin: Manual rate refresh | Should Have | Done |
| FR-12 | Admin: Encrypted API key management | Should Have | Done |
| FR-13 | Password change functionality | Could Have | Done |

### Non-Functional Requirements

| ID | Requirement | Target | Status |
|----|-------------|--------|--------|
| NFR-01 | Response time < 500ms (95th percentile) | 500ms | Met |
| NFR-02 | System availability | 99.9% | Architecture supports |
| NFR-03 | Test coverage ≥ 70% | 70% | 87% achieved |
| NFR-04 | Containerized deployment | Docker | Done |
| NFR-05 | API documentation (OpenAPI 3.0) | Complete | Done |
| NFR-06 | Secure credential storage | Encrypted | AES-256-GCM |

## Out of Scope

| Item | Reason |
|------|--------|
| Web/Mobile UI | API-only project, UI is separate concern |
| Actual currency transactions | This is rate provider, not exchange platform |
| Cryptocurrency rates | Different data sources, future enhancement |
| Payment processing | Outside project domain |
| Cloud deployment | Local Docker deployment for diploma |
| Rate predictions (ML) | Future enhancement |
| Multi-language support | English only for diploma |
| Real-time WebSocket updates | REST API sufficient for requirements |

## Supported Currencies

Default currencies configured via Liquibase migration:

| Code | Currency | Region |
|------|----------|--------|
| USD | US Dollar | North America |
| EUR | Euro | Europe |
| GBP | British Pound | United Kingdom |
| JPY | Japanese Yen | Japan |
| CHF | Swiss Franc | Switzerland |
| CAD | Canadian Dollar | Canada |
| AUD | Australian Dollar | Australia |
| CNY | Chinese Yuan | China |
| SEK | Swedish Krona | Sweden |
| NZD | New Zealand Dollar | New Zealand |

*Additional currencies can be added by administrators via API.*

### External Dependencies (Third-Party APIs)

| Dependency | Type | Owner |
|------------|------|-------|
| Fixer.io API | External | Fixer.io |
| ExchangeRatesAPI | External | ExchangeRatesAPI.io |
| CurrencyAPI | External | CurrencyAPI.com |

### Internal Components

| Component | Type | Purpose |
|-----------|------|---------|
| Mock Provider 1 | Internal | Simulates Fixer.io responses for development/testing |
| Mock Provider 2 | Internal | Simulates ExchangeRatesAPI responses for development/testing |

> **Note:** Mock providers are internal microservices that simulate external API responses. They are used during development and testing when real API access is unavailable or rate-limited. They are NOT external dependencies.

## Constraints

| Constraint | Description |
|------------|-------------|
| Budget | Free-tier external APIs only |
| Timeline | February 2026 diploma deadline |
| Technology | Java 21, Spring Boot 3.x (EPAM curriculum) |
| Deployment | Docker Compose (no Kubernetes) |
| Database | PostgreSQL (relational required) |
