# CERPS — Currency Exchange Rate Processing System

Backend microservices for [BudgetControl](#related-repositories).

## Architecture

Two Spring Boot services backed by a shared PostgreSQL instance:

| Service             | Port | Responsibility                                                |
|---------------------|------|---------------------------------------------------------------|
| Currency Service    | 8080 | Rate fetching, conversion, rate history, admin, migrations    |
| Analytics Service   | 8082 | Trend calculation, downsampling, percentage change            |

Analytics Service is database-free; it consumes Currency Service over REST.

## Tech Stack

Java 21 · Spring Boot 3 · PostgreSQL 16 · Liquibase · Docker / Docker Compose · Railway (deployment)

## Live Environment

| Resource                    | URL                                                                              |
|-----------------------------|----------------------------------------------------------------------------------|
| Swagger UI (Currency)       | https://supportive-vision-production.up.railway.app/swagger-ui.html              |
| Swagger UI (Analytics)      | https://sparkling-curiosity-production-9ffd.up.railway.app/swagger-ui.html       |

## Getting Started Locally

**Prerequisites:** Docker, Java 21

```bash
cp .env.example .env
# Fill at minimum: ENCRYPTION_MASTER_KEY, ADMIN_API_KEY, POSTGRES_PASSWORD
#   ENCRYPTION_MASTER_KEY=$(openssl rand -base64 32)
#   ADMIN_API_KEY=$(openssl rand -hex 32)

docker-compose up --build

curl http://localhost:8080/actuator/health
curl http://localhost:8082/actuator/health
```

Swagger UI (local): http://localhost:8080/swagger-ui.html · http://localhost:8082/swagger-ui.html

## Environment Variables

| Variable                | Required | Description                                                       |
|-------------------------|----------|-------------------------------------------------------------------|
| `POSTGRES_PASSWORD`     | Yes      | PostgreSQL password                                               |
| `POSTGRES_USER`         | No       | PostgreSQL user (default: `postgres`)                             |
| `POSTGRES_CURRENCY_DB`  | No       | Database name (default: `currency_db`)                            |
| `ADMIN_API_KEY`         | Yes      | Auth for `/api/v1/admin/**` (`openssl rand -hex 32`)              |
| `ENCRYPTION_MASTER_KEY` | Yes      | Base64 256-bit AES key for provider keys (`openssl rand -base64 32`) |
| `GEMINI_API_KEY`        | No       | Required for `/api/v1/ai/bank-commission`                         |
| `FIXER_API_KEY`         | No       | Optional; provider keys are normally set via Admin API            |
| `EXCHANGERATES_API_KEY` | No       | Optional; provider keys are normally set via Admin API            |
| `CURRENCYAPI_KEY`       | No       | Optional; provider keys are normally set via Admin API            |

Provider API keys are stored encrypted in the database and managed at runtime via
`POST /api/v1/admin/provider-keys` (see Swagger).

## Related Repositories

- **BudgetControl Android app:** https://github.com/MikgasH/BudgetControl
- **Diploma documentation:** https://github.com/MikgasH/BudgetControl-docs
