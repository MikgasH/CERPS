# Criterion 3: Database

## Overview

| Aspect | Details |
|--------|---------|
| **Criterion** | Relational Database with Migrations |
| **Technology** | PostgreSQL 15, Liquibase |
| **Databases** | 2 (users_db, currency_db) |
| **Tables** | 6 total |
| **Migrations** | 9 versioned changesets |

## Technology Choice

### Why PostgreSQL (vs MongoDB)

| Factor | PostgreSQL | MongoDB |
|--------|------------|---------|
| Data Structure | Fixed schema (users, rates) | Flexible documents |
| ACID Compliance | Full support | Limited (single document) |
| Financial Data | Industry standard | Not recommended |
| Relationships | Native JOINs | Manual references |
| Spring Integration | Mature JPA support | Requires separate starter |
| Indexing | B-tree, partial, composite | B-tree, compound |

**Decision:** PostgreSQL chosen for ACID compliance critical for financial rate data and user authentication.

### Why Liquibase (vs Flyway)

| Factor | Liquibase | Flyway |
|--------|-----------|--------|
| Format | YAML, XML, SQL | SQL only |
| Rollback | Automatic generation | Manual scripts |
| Diff | Database comparison tool | Not available |
| Learning Curve | Moderate | Simple |

**Decision:** Liquibase chosen for YAML format (readable, no SQL expertise required) and automatic rollback generation.

## Database Architecture

### Database-per-Service Pattern

```
┌─────────────────┐     ┌─────────────────┐
│  User Service   │     │Currency Service │
│     :8081       │     │     :8080       │
└────────┬────────┘     └────────┬────────┘
         │                       │
         v                       v
┌─────────────────┐     ┌─────────────────┐
│    users_db     │     │  currency_db    │
│   PostgreSQL    │     │   PostgreSQL    │
│     :5433       │     │     :5434       │
└─────────────────┘     └─────────────────┘
                               ^
                               │ (read-only)
                        ┌──────┴──────┐
                        │Analytics Svc│
                        │    :8082    │
                        └─────────────┘
```

**Benefits:**
- Service autonomy (independent schema changes)
- Failure isolation (one DB down doesn't affect others)
- Optimized for service workload
- Clear data ownership boundaries

## Schema Design

### users_db (User Service)

```sql
-- Table: roles
CREATE TABLE roles (
    id BIGINT PRIMARY KEY DEFAULT nextval('roles_seq'),
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Table: users
CREATE TABLE users (
    id BIGINT PRIMARY KEY DEFAULT nextval('users_seq'),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,  -- BCrypt hash
    enabled BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Table: user_roles (Many-to-Many)
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Default Roles
INSERT INTO roles (name) VALUES 
    ('ROLE_USER'), 
    ('ROLE_PREMIUM_USER'), 
    ('ROLE_ADMIN');
```

### currency_db (Currency Service)

```sql
-- Table: supported_currencies
CREATE TABLE supported_currencies (
    id BIGINT PRIMARY KEY DEFAULT nextval('supported_currencies_seq'),
    currency_code VARCHAR(3) NOT NULL UNIQUE
);

-- Table: exchange_rates
CREATE TABLE exchange_rates (
    id UUID PRIMARY KEY,
    base_currency VARCHAR(3) NOT NULL,
    target_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(20, 10) NOT NULL,
    source VARCHAR(20) DEFAULT 'AGGREGATED',
    timestamp TIMESTAMP NOT NULL
);

-- Table: api_provider_keys (Encrypted)
CREATE TABLE api_provider_keys (
    id BIGINT PRIMARY KEY DEFAULT nextval('api_provider_keys_seq'),
    provider_name VARCHAR(50) NOT NULL UNIQUE,
    encrypted_api_key TEXT NOT NULL,  -- AES-256-GCM encrypted
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Default Currencies
INSERT INTO supported_currencies (currency_code) VALUES 
    ('USD'), ('EUR'), ('GBP'), ('JPY'), ('CHF'), 
    ('CAD'), ('AUD'), ('CNY'), ('SEK'), ('NZD');
```

## ER Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                          users_db                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐         ┌──────────────┐         ┌──────────┐ │
│  │    users     │         │  user_roles  │         │  roles   │ │
│  ├──────────────┤         ├──────────────┤         ├──────────┤ │
│  │ PK id        │<───────┐│ PK,FK user_id│         │ PK id    │ │
│  │    email     │        └┤ PK,FK role_id├────────>│    name  │ │
│  │    password  │         └──────────────┘         │created_at│ │
│  │    enabled   │                                  └──────────┘ │
│  │ created_at   │                                               │
│  │ updated_at   │                                               │
│  └──────────────┘                                               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        currency_db                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────┐  ┌────────────────┐  ┌────────────────┐ │
│  │ supported_currencies│  │ exchange_rates │  │api_provider_keys│ │
│  ├────────────────────┤  ├────────────────┤  ├────────────────┤ │
│  │ PK id              │  │ PK id (UUID)   │  │ PK id          │ │
│  │    currency_code   │  │    base_currency│  │    provider_name│ │
│  └────────────────────┘  │    target_currency│ │encrypted_api_key│ │
│                          │    rate         │  │    active      │ │
│                          │    source       │  │    created_at  │ │
│                          │    timestamp    │  │    updated_at  │ │
│                          └────────────────┘  └────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Liquibase Migrations

### Migration Structure

```
src/main/resources/db/changelog/
├── db.changelog-master.yaml          # Master changelog
└── migrations/
    ├── v1.0-create-supported-currencies-table.yaml
    ├── v1.1-create-exchange-rates-table.yaml
    ├── v1.2-add-exchange-rates-indexes.yaml
    ├── v1.3-insert-default-currencies.yaml
    └── v1.4-create-api-provider-keys-table.yaml
```

### Migration Example

```yaml
# v1.1-create-exchange-rates-table.yaml
databaseChangeLog:
  - changeSet:
      id: create-exchange-rates-table
      author: hashkin
      changes:
        - createTable:
            tableName: exchange_rates
            columns:
              - column:
                  name: id
                  type: UUID
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: rate
                  type: DECIMAL(20, 10)
                  constraints:
                    nullable: false
      rollback:
        - dropTable:
            tableName: exchange_rates
```

### Migration Versioning

| Version | Migration | Description |
|---------|-----------|-------------|
| 1.0 | create-supported-currencies-table | Currency codes table |
| 1.1 | create-exchange-rates-table | Rate storage with UUID |
| 1.2 | add-exchange-rates-indexes | Performance indexes |
| 1.3 | insert-default-currencies | 10 default currencies |
| 1.4 | create-api-provider-keys-table | Encrypted key storage |

## Index Optimization

### exchange_rates Indexes

```yaml
# idx_latest_rate - For latest rate queries
- createIndex:
    indexName: idx_latest_rate
    tableName: exchange_rates
    columns:
      - column: base_currency
      - column: target_currency
      - column: timestamp (DESC)

# idx_trends - For trend analysis queries
- createIndex:
    indexName: idx_trends
    tableName: exchange_rates
    columns:
      - column: base_currency
      - column: target_currency
      - column: timestamp

# idx_timestamp - For cleanup operations
- createIndex:
    indexName: idx_timestamp
    tableName: exchange_rates
    columns:
      - column: timestamp
```

### Query Optimization

| Query Pattern | Index Used | Estimated Performance |
|---------------|------------|----------------------|
| Latest rate for pair | idx_latest_rate | O(log n) |
| Trend analysis range | idx_trends | O(log n) + O(k) |
| Old records cleanup | idx_timestamp | O(log n) |

## Data Retention

**Exchange Rates:**
- Retention period: 395 days
- Cleanup: Automated scheduler
- Query: `DELETE FROM exchange_rates WHERE timestamp < NOW() - INTERVAL '395 days'`

**Reason:** Supports 1-year trend analysis with 30-day buffer.

## Security Considerations

### Password Storage
- Algorithm: BCrypt
- Cost factor: 12
- Never stored in plain text

### API Key Storage
- Algorithm: AES-256-GCM
- Key derivation: Master key from environment
- IV: Random 96-bit per encryption

### Database Users

| User | Database | Permissions |
|------|----------|-------------|
| postgres | users_db | ALL |
| postgres | currency_db | ALL |
| analytics_readonly | currency_db | SELECT only |

## Connection Configuration

```yaml
# User Service
spring:
  datasource:
    url: jdbc:postgresql://postgres-users:5432/users_db
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5

# Currency Service  
spring:
  datasource:
    url: jdbc:postgresql://postgres-currency:5432/currency_db
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5

# Analytics Service (read-only)
spring:
  datasource:
    url: jdbc:postgresql://postgres-currency:5432/currency_db
    username: analytics_readonly
  jpa:
    hibernate:
      ddl-auto: validate  # Never modify schema
```

## Testing Strategy

- **Unit Tests:** Repository layer with H2 in PostgreSQL mode
- **Integration Tests:** Full database operations
- **Test Isolation:** Transactions rolled back after each test

```yaml
# Test configuration
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL
  jpa:
    hibernate:
      ddl-auto: create-drop
```
