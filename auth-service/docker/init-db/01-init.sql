-- Initialize database for Docker environment
-- This script runs when the PostgreSQL container starts for the first time

-- Script runs in auth database context (set by POSTGRES_DB)

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create auth schema
CREATE SCHEMA IF NOT EXISTS auth;

-- Set search path to include auth schema
ALTER DATABASE auth SET search_path TO public, auth;

-- Create initial tables for auth service
CREATE TABLE IF NOT EXISTS auth.tbl_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_name VARCHAR(20) UNIQUE NOT NULL,
    email VARCHAR(254) UNIQUE NOT NULL,
    password_hash VARCHAR(100),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone_number VARCHAR(20),
    address TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS auth.tbl_role (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS auth.tbl_permission (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    resource VARCHAR(100),
    action VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS auth.tbl_user_role (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.tbl_user(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES auth.tbl_role(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    UNIQUE(user_id, role_id)
);

CREATE TABLE IF NOT EXISTS auth.tbl_role_permission (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_id UUID NOT NULL REFERENCES auth.tbl_role(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES auth.tbl_permission(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    UNIQUE(role_id, permission_id)
);

-- Insert default roles
INSERT INTO auth.tbl_role (name, description) VALUES
('ROLE_USER', 'Default user role'),
('ROLE_ADMIN', 'Administrator role'),
('ROLE_MODERATOR', 'Moderator role')
ON CONFLICT (name) DO NOTHING;

-- Insert default permissions
INSERT INTO auth.tbl_permission (name, description, resource, action) VALUES
('USER_READ', 'Read user information', 'user', 'read'),
('USER_WRITE', 'Write user information', 'user', 'write'),
('USER_DELETE', 'Delete user', 'user', 'delete'),
('ROLE_READ', 'Read role information', 'role', 'read'),
('ROLE_WRITE', 'Write role information', 'role', 'write'),
('CACHE_MANAGE', 'Manage cache', 'cache', 'manage')
ON CONFLICT (name) DO NOTHING;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_user_email ON auth.tbl_user(email);
CREATE INDEX IF NOT EXISTS idx_user_username ON auth.tbl_user(user_name);
CREATE INDEX IF NOT EXISTS idx_user_role_user_id ON auth.tbl_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_role_id ON auth.tbl_user_role(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_role_id ON auth.tbl_role_permission(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_permission_id ON auth.tbl_role_permission(permission_id);
