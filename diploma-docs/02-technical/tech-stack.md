# Technology Stack

## Core Technologies

### Java 21 (LTS)

**Why Java 21:**
- Long-term support until 2031
- Virtual threads for improved concurrency
- Pattern matching and record patterns
- Strong typing and compile-time safety
- Extensive enterprise ecosystem
- EPAM curriculum requirement

**Key Features Used:**
- Records for DTOs (immutable data carriers)
- Pattern matching in switch expressions
- Text blocks for SQL and JSON
- Optional for null safety

### Spring Boot 3.5.6

**Why Spring Boot:**
- Industry standard for Java microservices
- Auto-configuration reduces boilerplate
- Embedded server (no external Tomcat)
- Actuator for health checks and metrics
- Strong security framework integration

**Spring Modules Used:**

| Module | Purpose |
|--------|---------|
| spring-boot-starter-web | REST API controllers |
| spring-boot-starter-data-jpa | Database access |
| spring-boot-starter-security | Authentication/Authorization |
| spring-boot-starter-validation | Request validation |
| spring-boot-starter-actuator | Health checks, metrics |
| spring-cloud-starter-gateway | API Gateway (reactive) |

### PostgreSQL 15

**Why PostgreSQL:**
- ACID compliance for financial data
- Advanced indexing (B-tree, partial)
- JSON support for flexible data
- Excellent Spring Data JPA integration
- Free and open source
- Production-proven reliability

**Database Design:**
- 2 separate databases (users_db, currency_db)
- Database-per-service pattern
- Read-only user for Analytics Service
- Liquibase for version-controlled migrations

### Docker & Docker Compose

**Why Docker:**
- Consistent environment across machines
- Easy dependency management
- Simplified deployment
- Resource isolation
- Diploma requirement

**Container Configuration:**
- 9 containers total
- Health checks for all services
- Resource limits (CPU, memory)
- Named volumes for data persistence
- Bridge network for service communication

## Security Stack

### JWT (JSON Web Tokens)

**Implementation:**
- Algorithm: HS256 (HMAC-SHA256)
- Secret: 256-bit key
- Expiration: 24 hours
- Claims: subject (email), roles, iat, exp

**Library:** io.jsonwebtoken:jjwt (0.12.x)

### BCrypt Password Hashing

**Implementation:**
- Cost factor: 12 (2^12 iterations)
- Automatic salt generation
- Spring Security PasswordEncoder

### AES-256-GCM Encryption

**Purpose:** Encrypt external API keys at rest

**Implementation:**
- Algorithm: AES/GCM/NoPadding
- Key size: 256 bits
- IV size: 96 bits (12 bytes)
- Tag size: 128 bits

## Testing Stack

| Tool | Purpose | Version |
|------|---------|---------|
| JUnit 5 | Test framework | 5.10.x |
| Mockito | Mocking framework | 5.x |
| WireMock | External API mocking | 3.x |
| H2 | In-memory test database | 2.x |
| JaCoCo | Code coverage | 0.8.x |
| Spring Boot Test | Integration testing | 3.5.6 |

## API Documentation

| Tool | Purpose |
|------|---------|
| SpringDoc OpenAPI | Automatic spec generation |
| Swagger UI | Interactive API explorer |
| Markdown | Human-readable documentation |

## Build Tools

| Tool | Purpose |
|------|---------|
| Maven | Build automation, dependency management |
| Maven Wrapper | Consistent Maven version |
| JaCoCo Maven Plugin | Coverage reports |
| Spring Boot Maven Plugin | Executable JAR packaging |

## Development Tools

| Tool | Purpose |
|------|---------|
| IntelliJ IDEA | Primary IDE |
| Git | Version control |
| GitHub | Repository hosting |
| Docker Desktop | Local container runtime |
| Postman / curl | API testing |

## Version Compatibility Matrix

| Component | Version | Compatibility |
|-----------|---------|---------------|
| Java | 21 | Spring Boot 3.x requires Java 17+ |
| Spring Boot | 3.5.6 | Requires Java 17-21 |
| PostgreSQL | 15 | Compatible with Spring Data JPA 3.x |
| Docker | 24.x | Compose V2 syntax |
| JUnit | 5.10 | Spring Boot 3.x native support |
