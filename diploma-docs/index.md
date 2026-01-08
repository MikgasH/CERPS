# Currency Exchange Rate Provider Service (CERPS)

## Diploma Project Documentation

## Project Information

| Field | Value |
|-------|-------|
| Student | Mikita Hashkin |
| Group | 4th Year, Informatics |
| Supervisor | Jan Bartnitsky (UAB Gurtam, Software Engineer) |
| Institution | European Humanities University, School of Digital Competencies |
| Date | February 2026 |

## Project Links

| Resource | URL |
|----------|-----|
| Repository | https://github.com/MikgasH/exchange |
| API Gateway | http://localhost:8000 |
| Swagger UI | http://localhost:8000/swagger-ui.html |
| API Documentation | /docs/api-reference |

## Project Summary

CERPS is a microservices-based platform that provides reliable currency exchange rates by aggregating data from multiple external providers (Fixer.io, ExchangeRatesAPI, CurrencyAPI). The system calculates median rates to ensure accuracy, implements automatic fallback to mock services for high availability, and provides trend analysis for premium users. Built with Java 21 and Spring Boot 3.5.6, the platform demonstrates enterprise-grade architecture with JWT authentication, AES-256 encryption, and 87% test coverage.

## Evaluation Criteria

| # | Criterion | Status | Documentation |
|---|-----------|--------|---------------|
| 1 | Back-end | Completed | [backend.md](02-technical/criteria/backend.md) |
| 2 | Cryptography | Completed | [cryptography.md](02-technical/criteria/cryptography.md) |
| 3 | Database | Completed | [database.md](02-technical/criteria/database.md) |
| 4 | Microservices | Completed | [microservices.md](02-technical/criteria/microservices.md) |
| 5 | Containerization | Completed | [containerization.md](02-technical/criteria/containerization.md) |
| 6 | API Documentation | Completed | [api-documentation.md](02-technical/criteria/api-documentation.md) |
| 7 | Automated Tests (70% min) | Completed (89%) | [testing.md](02-technical/criteria/testing.md) |

## Documentation Structure

- [Project Overview](01-project-overview/index.md) - Business context, goals, and requirements
- [Technical Implementation](02-technical/index.md) - Architecture, tech stack, and criteria details
- [User Guide](03-user-guide/index.md) - Application usage instructions
- [Retrospective](04-retrospective/index.md) - Lessons learned and future improvements

## Quick Start

```bash
# Clone repository
git clone https://github.com/MikgasH/exchange.git
cd cerps-hashkin

# Configure environment
cp .env.example .env
# Edit .env with your values (generate keys with: openssl rand -base64 32)

# Start all services
docker-compose up -d

# Verify health
curl http://localhost:8000/actuator/health
```

---

Document created: December 2025  
Last updated: January 2026
