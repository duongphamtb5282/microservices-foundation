-- V1: auth schema, core tables and seed data (Flyway).
--
-- Every statement is idempotent (IF NOT EXISTS / ON CONFLICT DO NOTHING): the schema may already
-- be populated from before Flyway took over migrations (the dev database was created by hand and
-- via docker/init-db). "already exists" is the expected state here, not an error — the changeset
-- must record as applied without re-creating or duplicating anything.

CREATE SCHEMA IF NOT EXISTS auth;

-- ==================== TABLES ====================

CREATE TABLE IF NOT EXISTS auth.tbl_user (
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

CREATE TABLE IF NOT EXISTS auth.tbl_role (
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
-- No separate ALTER for uk_roles_name: name is UNIQUE inline above. On pre-existing schemas the
-- unique index already exists under its own name (e.g. tbl_role_name_key) — adding a duplicate
-- constraint would fail or be pure overhead.

CREATE TABLE IF NOT EXISTS auth.tbl_permission (
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

CREATE TABLE IF NOT EXISTS auth.tbl_role_permission (
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
    FOREIGN KEY (permission_id) REFERENCES auth.tbl_permission(id) ON DELETE CASCADE,
    UNIQUE (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS auth.tbl_user_role (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES auth.tbl_user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES auth.tbl_role(id) ON DELETE CASCADE
);

-- ==================== INDEXES ====================

CREATE INDEX IF NOT EXISTS idx_user_user_name ON auth.tbl_user(user_name);
CREATE INDEX IF NOT EXISTS idx_user_email ON auth.tbl_user(email);
CREATE INDEX IF NOT EXISTS idx_user_is_deleted ON auth.tbl_user(is_deleted);

-- ==================== SEED DATA ====================

-- Default roles
INSERT INTO auth.tbl_role (name, description, created_by) VALUES
('ADMIN', 'Administrator with full system access', 'system'),
('USER', 'Regular user with basic access', 'system'),
('MODERATOR', 'Moderator with content management access', 'system')
ON CONFLICT (name) DO NOTHING;

-- Default permissions
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
('SYSTEM_ADMIN', 'Full system administration', 'system', 'admin', 'system')
ON CONFLICT (name) DO NOTHING;

-- Assign permissions to roles. Idempotency here must NOT rely on ON CONFLICT: tbl_role_permission
-- has no UNIQUE constraint on (role_id, permission_id) in the canonical schema, so there is no
-- conflict target — a NOT EXISTS guard makes the insert a no-op when the link already exists,
-- on both fresh and pre-populated databases.
INSERT INTO auth.tbl_role_permission (role_id, permission_id, assigned_by)
SELECT r.id, p.id, 'system'
FROM auth.tbl_role r, auth.tbl_permission p
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM auth.tbl_role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- Assign basic permissions to USER role
INSERT INTO auth.tbl_role_permission (role_id, permission_id, assigned_by)
SELECT r.id, p.id, 'system'
FROM auth.tbl_role r, auth.tbl_permission p
WHERE r.name = 'USER' AND p.name IN ('USER_READ')
  AND NOT EXISTS (SELECT 1 FROM auth.tbl_role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- Assign moderate permissions to MODERATOR role
INSERT INTO auth.tbl_role_permission (role_id, permission_id, assigned_by)
SELECT r.id, p.id, 'system'
FROM auth.tbl_role r, auth.tbl_permission p
WHERE r.name = 'MODERATOR' AND p.name IN ('USER_READ', 'USER_WRITE', 'ROLE_READ')
  AND NOT EXISTS (SELECT 1 FROM auth.tbl_role_permission rp
                  WHERE rp.role_id = r.id AND rp.permission_id = p.id);
