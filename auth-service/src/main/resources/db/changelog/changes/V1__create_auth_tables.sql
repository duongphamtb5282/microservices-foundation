--liquibase formatted sql

--changeset auth:create-auth-schema runOnChange:false
-- Create auth schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS auth;

--changeset auth:create-user-table runOnChange:false
-- Create user table
CREATE TABLE auth.tbl_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(255),
    modified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    user_name VARCHAR(20) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone_number VARCHAR(20),
    address TEXT,
    is_active BOOLEAN DEFAULT TRUE
);

-- Create index on user_name for faster lookups
CREATE INDEX idx_user_user_name ON auth.tbl_user(user_name);
CREATE INDEX idx_user_email ON auth.tbl_user(email);
CREATE INDEX idx_user_is_deleted ON auth.tbl_user(is_deleted);

--changeset auth:create-role-table runOnChange:false
-- Create role table
CREATE TABLE auth.tbl_role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(255),
    modified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

--changeset auth:create-role-constraint runOnChange:false
-- Create unique constraint on role name
ALTER TABLE auth.tbl_role ADD CONSTRAINT uk_roles_name UNIQUE (name);

--changeset auth:create-permission-table runOnChange:false
-- Create permission table
CREATE TABLE auth.tbl_permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(255),
    modified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    resource VARCHAR(100),
    action VARCHAR(50)
);

--changeset auth:create-role-permission-table runOnChange:false
-- Create role-permission junction table
CREATE TABLE auth.tbl_role_permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR(255),
    modified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_by VARCHAR(255),
    deleted_at TIMESTAMP WITH TIME ZONE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (role_id) REFERENCES auth.tbl_role(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES auth.tbl_permission(id) ON DELETE CASCADE
);

--changeset auth:create-user-role-table runOnChange:false
-- Create user-role junction table
CREATE TABLE auth.tbl_user_role (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES auth.tbl_user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES auth.tbl_role(id) ON DELETE CASCADE
);

--changeset auth:insert-default-data runOnChange:false
-- Insert default roles
INSERT INTO auth.tbl_role (name, description, created_by) VALUES
('ADMIN', 'Administrator with full system access', 'system'),
('USER', 'Regular user with basic access', 'system'),
('MODERATOR', 'Moderator with content management access', 'system');

-- Insert default permissions
INSERT INTO auth.tbl_permission (name, description, resource, action, created_by) VALUES
('USER_READ', 'Read user information', 'user', 'read', 'system'),
('USER_WRITE', 'Create/update user information', 'user', 'write', 'system'),
('USER_DELETE', 'Delete user accounts', 'user', 'delete', 'system'),
('ROLE_READ', 'Read role information', 'role', 'read', 'system'),
('ROLE_WRITE', 'Create/update roles', 'role', 'write', 'system'),
('ROLE_DELETE', 'Delete roles', 'role', 'delete', 'system'),
('PERMISSION_READ', 'Read permission information', 'permission', 'read', 'system'),
('PERMISSION_WRITE', 'Create/update permissions', 'permission', 'write', 'system'),
('PERMISSION_DELETE', 'Delete permissions', 'permission', 'delete', 'system'),
('SYSTEM_ADMIN', 'Full system administration', 'system', 'admin', 'system');

-- Assign permissions to ADMIN role
INSERT INTO auth.tbl_role_permission (role_id, permission_id, assigned_by)
SELECT r.id, p.id, 'system'
FROM auth.tbl_role r, auth.tbl_permission p
WHERE r.name = 'ADMIN';

-- Assign basic permissions to USER role
INSERT INTO auth.tbl_role_permission (role_id, permission_id, assigned_by)
SELECT r.id, p.id, 'system'
FROM auth.tbl_role r, auth.tbl_permission p
WHERE r.name = 'USER' AND p.name IN ('USER_READ');

-- Assign moderate permissions to MODERATOR role
INSERT INTO auth.tbl_role_permission (role_id, permission_id, assigned_by)
SELECT r.id, p.id, 'system'
FROM auth.tbl_role r, auth.tbl_permission p
WHERE r.name = 'MODERATOR' AND p.name IN ('USER_READ', 'USER_WRITE', 'ROLE_READ');
