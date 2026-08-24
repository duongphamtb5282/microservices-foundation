-- Initialize database for Docker environment
-- This script runs when the PostgreSQL container starts for the first time.
--
-- NOTE: schema DDL is deliberately NOT created here any more. Flyway owns the auth schema
-- (auth-service/src/main/resources/db/migration); the table DDL that used to live here drifted
-- from the JPA entities (missing version/is_deleted/deleted_at columns, different junction
-- table shapes) and made every fresh container fail migrations with "relation already exists".
-- This script now only sets up the extensions and the auth schema itself.

-- Script runs in auth database context (set by POSTGRES_DB)

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create auth schema
CREATE SCHEMA IF NOT EXISTS auth;

-- Set search path to include auth schema
ALTER DATABASE auth SET search_path TO public, auth;
