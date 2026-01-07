-- Create read-only role for analytics service
-- Using default password 'analytics_readonly_pass'
-- Change via POSTGRES_ANALYTICS_PASSWORD environment variable in .env
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'analytics_readonly') THEN
    CREATE ROLE analytics_readonly WITH LOGIN PASSWORD 'analytics_readonly_pass';
  END IF;
END
$$;

-- Grant connect privileges
GRANT CONNECT ON DATABASE currency_db TO analytics_readonly;

-- Grant usage on schema
GRANT USAGE ON SCHEMA public TO analytics_readonly;

-- Grant select on all existing tables
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analytics_readonly;

-- Grant select on future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO analytics_readonly;

-- Ensure analytics role cannot modify data
REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON ALL TABLES IN SCHEMA public FROM analytics_readonly;
