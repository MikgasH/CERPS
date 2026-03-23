#!/bin/bash
set -e

ANALYTICS_PASSWORD="${POSTGRES_ANALYTICS_PASSWORD:-analytics_readonly_pass}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create read-only role for analytics service
    DO \$\$
    BEGIN
      IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'analytics_readonly') THEN
        CREATE ROLE analytics_readonly WITH LOGIN PASSWORD '${ANALYTICS_PASSWORD}';
      ELSE
        ALTER ROLE analytics_readonly WITH PASSWORD '${ANALYTICS_PASSWORD}';
      END IF;
    END
    \$\$;

    -- Grant connect privileges
    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO analytics_readonly;

    -- Grant usage on schema
    GRANT USAGE ON SCHEMA public TO analytics_readonly;

    -- Grant select on all existing tables
    GRANT SELECT ON ALL TABLES IN SCHEMA public TO analytics_readonly;

    -- Grant usage on all sequences (for reading sequence information)
    GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO analytics_readonly;

    -- Grant select on future tables
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO analytics_readonly;

    -- Grant usage on future sequences
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO analytics_readonly;

    -- Ensure analytics role cannot modify data
    REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON ALL TABLES IN SCHEMA public FROM analytics_readonly;
EOSQL
