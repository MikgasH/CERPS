# Deployment Guide

## Prerequisites

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| Docker | 20.x | 24.x |
| Docker Compose | 2.x | 2.x |
| RAM | 3 GB | 4 GB |
| CPU | 4 cores | 6 cores |
| Disk | 5 GB | 10 GB |

## Quick Start

```bash
# 1. Clone repository
git clone https://github.com/MikgasH/exchange.git
cd cerps-hashkin

# 2. Create environment file
cp .env.example .env

# 3. Generate secure keys
openssl rand -base64 32  # Use for JWT_SECRET
openssl rand -base64 32  # Use for ENCRYPTION_MASTER_KEY

# 4. Edit .env with generated keys
nano .env

# 5. Start all services
docker-compose up -d

# 6. Check status
docker-compose ps

# 7. Verify health
curl http://localhost:8000/actuator/health
```

## Environment Configuration

### Required Variables

```env
# JWT Secret (256 bits, Base64)
JWT_SECRET=your-base64-encoded-32-byte-key

# Encryption Master Key (256 bits, Base64)
ENCRYPTION_MASTER_KEY=your-base64-encoded-32-byte-key
```

### Optional Variables

```env
# PostgreSQL (defaults work for development)
POSTGRES_DB=exchange_rates_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# External API Keys (mock services work without these)
FIXER_API_KEY=your-fixer-key
EXCHANGERATES_API_KEY=your-exchangerates-key
CURRENCYAPI_KEY=your-currencyapi-key
```

## Service Endpoints

| Service | URL | Purpose |
|---------|-----|---------|
| API Gateway | http://localhost:8000 | Main entry point |
| Swagger UI | http://localhost:8000/swagger-ui.html | API explorer |
| User Service | http://localhost:8081 | Authentication (internal) |
| Currency Service | http://localhost:8080 | Exchange rates (internal) |
| Analytics Service | http://localhost:8082 | Trends (internal) |

## Commands Reference

### Lifecycle

```bash
# Start
docker-compose up -d

# Stop (keep data)
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop and remove data
docker-compose down -v
```

### Monitoring

```bash
# Status
docker-compose ps

# Logs (all)
docker-compose logs -f

# Logs (specific service)
docker-compose logs -f currency-service

# Health check
curl http://localhost:8000/actuator/health
```

### Maintenance

```bash
# Restart service
docker-compose restart currency-service

# Rebuild and restart
docker-compose up -d --build currency-service

# Full rebuild
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

## Troubleshooting

### Services Not Starting

```bash
# Check logs
docker-compose logs

# Common issues:
# - Port already in use: Stop conflicting services
# - Out of memory: Close other applications
# - Invalid .env: Check key format (Base64, 32 bytes)
```

### Database Connection Failed

```bash
# Check database status
docker-compose logs postgres-users
docker-compose logs postgres-currency

# Restart databases
docker-compose restart postgres-users postgres-currency
```

### Health Check Failing

```bash
# Wait longer (services need ~2 min)
sleep 120 && docker-compose ps

# Check specific service
docker-compose logs api-gateway
```

## Production Considerations

 **This is a diploma project. For production deployment:**

1. Use HTTPS (TLS certificates)
2. External PostgreSQL (managed database)
3. Redis for distributed caching
4. Kubernetes for orchestration
5. Centralized logging (ELK)
6. Monitoring (Prometheus/Grafana)
7. Secrets management (Vault)
8. Load balancer
9. Rate limiting
10. Regular security audits
