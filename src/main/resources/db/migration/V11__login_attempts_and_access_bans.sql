-- V11: login-attempt audit log + manual access bans + ban:* permissions (Spec C).
-- Additive + idempotent — safe on fresh DBs, baselined DBs, and re-runs.

CREATE TABLE IF NOT EXISTS public.t_login_attempts (
    id          UUID PRIMARY KEY,
    username    VARCHAR(64),
    ip          VARCHAR(45),
    device_hash VARCHAR(128),
    user_agent  VARCHAR(512),
    success     BOOLEAN NOT NULL,
    error_code  VARCHAR(64),
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
);
CREATE INDEX IF NOT EXISTS idx_login_attempts_ip_created ON public.t_login_attempts (ip, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_login_attempts_username_created ON public.t_login_attempts (username, created_at DESC);

CREATE TABLE IF NOT EXISTS public.t_access_bans (
    id         UUID PRIMARY KEY,
    type       VARCHAR(10) NOT NULL,
    value      VARCHAR(128) NOT NULL,
    reason     VARCHAR(255),
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT uq_access_bans_type_value UNIQUE (type, value)
);

-- ban:* permissions. Per the V9 convention: grant to BOTH ADMIN and SYS_ROOT.
INSERT INTO public.t_permissions (id, created_at, created_by, updated_at, updated_by, description, name) VALUES
    ('a3000000-0000-0000-0000-000000000001', '2026-07-23 00:00:00', 'SYS', '2026-07-23 00:00:00', 'SYS', 'View login attempts and bans', 'ban:read'),
    ('a3000000-0000-0000-0000-000000000002', '2026-07-23 00:00:00', 'SYS', '2026-07-23 00:00:00', 'SYS', 'Ban an IP or device', 'ban:create'),
    ('a3000000-0000-0000-0000-000000000003', '2026-07-23 00:00:00', 'SYS', '2026-07-23 00:00:00', 'SYS', 'Remove a ban', 'ban:delete')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.t_roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.t_roles r, public.t_permissions p
WHERE r.name IN ('ADMIN', 'SYS_ROOT')
  AND p.id IN ('a3000000-0000-0000-0000-000000000001',
               'a3000000-0000-0000-0000-000000000002',
               'a3000000-0000-0000-0000-000000000003')
ON CONFLICT (role_id, permission_id) DO NOTHING;
