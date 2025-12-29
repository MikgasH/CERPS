# Criterion 5: Containerization

## Overview

| Aspect | Details |
|--------|---------|
| **Criterion** | Docker Containerization |
| **Technology** | Docker + Docker Compose |
| **Total Containers** | 9 |
| **Network** | Bridge (cerps-network) |
| **Orchestration** | Docker Compose V2 |

## Container Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      cerps-network (bridge)                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────┐ ┌─────────────┐                                │
│  │postgres-users│ │postgres-curr│  Databases                    │
│  │   :5433     │ │   :5434     │                                │
│  └─────────────┘ └─────────────┘                                │
│         │               │                                        │
│         v               v                                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                │
│  │user-service │ │currency-svc │ │analytics-svc│  App Services  │
│  │   :8081     │ │   :8080     │ │   :8082     │                │
│  └─────────────┘ └─────────────┘ └─────────────┘                │
│                        │                                         │
│         ┌──────────────┼──────────────┐                         │
│         v              v              v                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐                │
│  │mock-service-1│ │mock-service-2│ │ api-gateway │  Gateway     │
│  │   :8083     │ │   :8084     │ │   :8000     │                │
│  └─────────────┘ └─────────────┘ └─────────────┘                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Container Inventory

| Container | Image | Port | Size | Purpose |
|-----------|-------|------|------|---------|
| postgres-users | postgres:15-alpine | 5433:5432 | ~80 MB | User database |
| postgres-currency | postgres:15-alpine | 5434:5432 | ~80 MB | Currency database |
| user-service | exchange-user-service | 8081:8081 | 419 MB | Authentication |
| currency-service | exchange-currency-service | 8080:8080 | 421 MB | Exchange rates |
| analytics-service | exchange-analytics-service | 8082:8082 | 408 MB | Trend analysis |
| mock-service-1 | exchange-mock-service-1 | 8083:8083 | 328 MB | Fallback provider |
| mock-service-2 | exchange-mock-service-2 | 8084:8084 | 328 MB | Fallback provider |
| api-gateway | exchange-api-gateway | 8000:8000 | 370 MB | API entry point |

**Total: 9 containers (~2.5 GB total)**

## Dockerfile Structure

### Multi-stage Build (Services)

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Benefits of Multi-stage

| Aspect | Single Stage | Multi-stage |
|--------|--------------|-------------|
| Image Size | ~500MB (with JDK) | ~200MB (JRE only) |
| Build Tools | Included | Excluded |
| Security | Larger attack surface | Minimal runtime |
| Build Cache | Limited | Optimized |

## Docker Compose Configuration

### Service Definition Example

```yaml
user-service:
  build:
    context: .
    dockerfile: user-service/Dockerfile
  container_name: user-service
  ports:
    - "8081:8081"
  environment:
    SPRING_PROFILES_ACTIVE: docker
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-users:5432/users_db
    JWT_SECRET: ${JWT_SECRET}
  depends_on:
    postgres-users:
      condition: service_healthy
  networks:
    - cerps-network
  restart: unless-stopped
  healthcheck:
    test: ["CMD", "wget", "--spider", "http://localhost:8081/actuator/health"]
    interval: 30s
    timeout: 10s
    retries: 3
    start_period: 40s
  deploy:
    resources:
      limits:
        cpus: '1.0'
        memory: 512M
      reservations:
        cpus: '0.5'
        memory: 256M
```

## Health Checks

### Database Health Checks

```yaml
postgres-users:
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U postgres -d users_db"]
    interval: 10s
    timeout: 5s
    retries: 5
```

### Application Health Checks

```yaml
user-service:
  healthcheck:
    test: ["CMD", "wget", "--spider", "http://localhost:8081/actuator/health"]
    interval: 30s
    timeout: 10s
    retries: 3
    start_period: 40s
```

### Health Check Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    Health Check Sequence                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Container starts                                             │
│          │                                                       │
│          v                                                       │
│  2. Wait start_period (40s for apps, 0 for DBs)                 │
│          │                                                       │
│          v                                                       │
│  3. Execute health check command                                 │
│          │                                                       │
│     ┌────┴────┐                                                  │
│     v         v                                                  │
│  Success   Failure                                               │
│     │         │                                                  │
│     v         v                                                  │
│  healthy   Retry (up to 3 times)                                │
│     │         │                                                  │
│     │    ┌────┴────┐                                            │
│     │    v         v                                            │
│     │  Success   All failed                                      │
│     │    │         │                                            │
│     │    v         v                                            │
│     │  healthy   unhealthy                                       │
│     │                                                            │
│     v                                                            │
│  4. Dependent services can start                                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Resource Management

### Resource Limits

| Container | CPU Limit | Memory Limit | CPU Reserved | Memory Reserved |
|-----------|-----------|--------------|--------------|-----------------|
| PostgreSQL | 0.5 | 256MB | 0.25 | 128MB |
| App Services | 0.75-1.0 | 384-512MB | 0.25-0.5 | 256MB |
| Mock Services | 0.5 | 256MB | 0.25 | 128MB |
| API Gateway | 0.75 | 384MB | 0.25 | 256MB |

### Total Resource Requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| CPU | 4 cores | 6 cores |
| RAM | 3 GB | 4 GB |
| Disk | 5 GB | 10 GB |

## Networking

### Bridge Network

```yaml
networks:
  cerps-network:
    driver: bridge
```

### Service Discovery

Docker Compose provides built-in DNS:
- Container name = DNS hostname
- `user-service` -> resolves to container IP
- No external service registry needed

### Port Mapping

| Service | Container Port | Host Port | Access |
|---------|---------------|-----------|--------|
| API Gateway | 8000 | 8000 | External (clients) |
| User Service | 8081 | 8081 | Debug only |
| Currency Service | 8080 | 8080 | Debug only |
| Analytics Service | 8082 | 8082 | Debug only |
| PostgreSQL Users | 5432 | 5433 | Debug only |
| PostgreSQL Currency | 5432 | 5434 | Debug only |

**Production:** Only API Gateway port exposed externally.

## Volume Management

### Persistent Volumes

```yaml
volumes:
  postgres_currency_data:
    driver: local
  postgres_users_data:
    driver: local
```

### Volume Mapping

```yaml
postgres-currency:
  volumes:
    - postgres_currency_data:/var/lib/postgresql/data
    - ./init-scripts/currency-init.sql:/docker-entrypoint-initdb.d/init.sql:ro
```

### Data Persistence

| Volume | Purpose | Lifecycle |
|--------|---------|-----------|
| postgres_users_data | User database | Persists across restarts |
| postgres_currency_data | Currency database | Persists across restarts |

## Startup Order

### Dependency Chain

```yaml
api-gateway:
  depends_on:
    user-service:
      condition: service_healthy
    currency-service:
      condition: service_healthy
    analytics-service:
      condition: service_healthy
```

### Startup Sequence

```
1. postgres-users, postgres-currency (parallel)
       │
       v (wait for healthy)
2. mock-service-1, mock-service-2 (parallel)
       │
       v
3. user-service
       │
       v (wait for healthy)
4. currency-service, analytics-service (parallel)
       │
       v (wait for healthy)
5. api-gateway
```

## Environment Configuration

### Environment Variables

```yaml
environment:
  # Profile selection
  SPRING_PROFILES_ACTIVE: docker
  
  # Database connection
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-users:5432/users_db
  SPRING_DATASOURCE_USERNAME: postgres
  SPRING_DATASOURCE_PASSWORD: postgres
  
  # Security (from .env file)
  JWT_SECRET: ${JWT_SECRET}
  ENCRYPTION_MASTER_KEY: ${ENCRYPTION_MASTER_KEY}
  
  # Service URLs
  USER_SERVICE_URL: http://user-service:8081
  MOCK_SERVICE_1_URL: http://mock-service-1:8083
```

### .env File

```env
JWT_SECRET=your-base64-256bit-key
ENCRYPTION_MASTER_KEY=your-base64-256bit-key
FIXER_API_KEY=optional-real-api-key
```

## Commands

### Start All Services

```bash
docker-compose up -d
```

### View Logs

```bash
docker-compose logs -f [service-name]
```

### Stop Services

```bash
docker-compose down
```

### Clean Rebuild

```bash
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

### Check Health Status

```bash
docker-compose ps
# or
docker inspect --format='{{.State.Health.Status}}' container-name
```

## Security Considerations

| Measure | Implementation |
|---------|----------------|
| Non-root user | Spring user in containers |
| Minimal images | Alpine-based images |
| No secrets in images | Environment variables |
| Read-only volumes | Init scripts mounted :ro |
| Network isolation | Single bridge network |
