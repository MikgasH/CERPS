# Copilot Instructions for CERPS-Hashkin

## Project Overview
CERPS-Hashkin is a Currency Exchange Rate Processing System built with Spring Boot 3.5.6 and Java 21. It integrates multiple external currency APIs (Fixer, ExchangeRates, CurrencyAPI) and mock services.

## Technology Stack
- **Java 21** with Spring Boot 3.5.6
- **Maven** for build management
- **PostgreSQL** as the primary database
- **Liquibase** for database migrations
- **Spring Security** with JWT (jjwt) for authentication
- **Spring Data JPA / Hibernate** for data access
- **Lombok** for reducing boilerplate code
- **SpringDoc OpenAPI** for API documentation
- **Testcontainers** and **WireMock** for integration testing

## Project Structure
```
src/main/java/com/example/cerpshashkin/
├── client/        # External API clients
├── config/        # Configuration classes
├── controller/    # REST controllers
├── converter/     # Data converters/mappers
├── dto/           # Data Transfer Objects
├── entity/        # JPA entities
├── exception/     # Custom exceptions and handlers
├── model/         # Domain models
├── repository/    # Spring Data repositories
├── scheduler/     # Scheduled tasks
├── service/       # Business logic services
└── validation/    # Custom validators
```

## Code Style Guidelines
- Follow **Checkstyle** rules configured in the project:
  - No unused or redundant imports
  - Use `final` parameters in methods
  - Include Javadoc for methods and types
  - Respect line length limits
  - Avoid magic numbers (use constants)
  - Proper whitespace usage
  - Mark classes as `final` when appropriate
- Use **constructor injection** for dependencies (avoid field injection with `@Autowired`)
- Leverage **Lombok** annotations: `@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`
- Prefer `Optional` over null checks
- Use meaningful variable and method names following Java conventions

## API Design
- Follow RESTful conventions for endpoints
- Use appropriate HTTP methods (GET, POST, PUT, DELETE)
- Return proper HTTP status codes
- Use DTOs for request/response objects (never expose entities directly)
- Document APIs with OpenAPI/Swagger annotations

## Database Guidelines
- Use **Liquibase** for all schema changes (never use `ddl-auto` for schema generation)
- Entity classes should be in the `entity` package
- Use Spring Data JPA repositories in the `repository` package
- Follow JPA best practices (avoid lazy loading issues, use appropriate fetch strategies)

## Security
- **Never hardcode** API keys, secrets, or passwords in code
- Use environment variables for sensitive configuration (see `application.yml` patterns)
- JWT tokens for authentication
- Follow Spring Security best practices

## Testing
- Write unit tests using **JUnit 5** and **Mockito**
- Use **Testcontainers** for PostgreSQL integration tests
- Use **WireMock** for mocking external API calls
- Use **H2** database for lightweight tests when appropriate
- Test data files are located in `src/test/resources/test-data/`
- Follow naming convention: `*Test.java` for unit tests, `*IT.java` for integration tests

## Configuration
- Use `application.yml` for configuration
- Use profiles for environment-specific settings (`application-test.yml` for tests)
- External API configurations are under `api.*` prefix
- Scheduling configuration under `scheduling.*` prefix
- Cache settings under `cache.*` prefix

## Multi-Module Structure
This is a multi-module Maven project with:
- **Main application** (root)
- **currency-service** - Currency-related functionality
- **user-service** - User management
- **cerps-common** - Shared utilities
- **mock-service-1/2** - Mock services for testing
- **analytics-service** - Analytics functionality

## Best Practices
- Keep services stateless
- Use `@Transactional` appropriately
- Handle exceptions properly with custom exception handlers
- Log important operations using SLF4J (`@Slf4j`)
- Write clean, maintainable code with proper separation of concerns

## Aplication notes for implementers
- No comments in code
