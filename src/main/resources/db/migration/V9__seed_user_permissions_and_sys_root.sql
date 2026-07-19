-- V9: user:* permissions + the SYS_ROOT super-user role.
-- Additive + idempotent — safe on fresh DBs, DBs baselined via baseline-on-migrate, and re-runs.
--
-- Convention from here on: every new permission-seed migration must grant the new permission
-- to BOTH the ADMIN role and the SYS_ROOT role (SYS_ROOT is not auto-updated by later migrations).

-- 1) The four user-management permissions.
INSERT INTO public.t_permissions (id, created_at, created_by, updated_at, updated_by, description, name) VALUES
    ('a1000000-0000-0000-0000-00000000000a', '2026-07-19 00:00:00', 'SYS', '2026-07-19 00:00:00', 'SYS', 'List and view users', 'user:read'),
    ('a1000000-0000-0000-0000-00000000000b', '2026-07-19 00:00:00', 'SYS', '2026-07-19 00:00:00', 'SYS', 'Create a user', 'user:create'),
    ('a1000000-0000-0000-0000-00000000000c', '2026-07-19 00:00:00', 'SYS', '2026-07-19 00:00:00', 'SYS', 'Modify a user (profile, status, roles, password)', 'user:update'),
    ('a1000000-0000-0000-0000-00000000000d', '2026-07-19 00:00:00', 'SYS', '2026-07-19 00:00:00', 'SYS', 'Delete a user', 'user:delete')
ON CONFLICT (id) DO NOTHING;

-- 2) Grant the four permissions to ADMIN.
INSERT INTO public.t_roles_permissions (role_id, permission_id) VALUES
    ('626c2558-a7d5-4fa4-9161-9bec2ae74076', 'a1000000-0000-0000-0000-00000000000a'),
    ('626c2558-a7d5-4fa4-9161-9bec2ae74076', 'a1000000-0000-0000-0000-00000000000b'),
    ('626c2558-a7d5-4fa4-9161-9bec2ae74076', 'a1000000-0000-0000-0000-00000000000c'),
    ('626c2558-a7d5-4fa4-9161-9bec2ae74076', 'a1000000-0000-0000-0000-00000000000d')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 3) The SYS_ROOT role.
INSERT INTO public.t_roles (id, created_at, created_by, updated_at, updated_by, description, name) VALUES
    ('a2000000-0000-0000-0000-000000000001', '2026-07-19 00:00:00', 'SYS', '2026-07-19 00:00:00', 'SYS', 'System super-user; immutable via the admin API', 'SYS_ROOT')
ON CONFLICT (id) DO NOTHING;

-- 4) SYS_ROOT carries EVERY permission currently in the catalog.
INSERT INTO public.t_roles_permissions (role_id, permission_id)
SELECT 'a2000000-0000-0000-0000-000000000001', p.id
FROM public.t_permissions p
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 5) Assign SYS_ROOT to the seeded root user (keeps its existing ADMIN grant).
INSERT INTO public.t_users_roles (user_id, role_id)
VALUES ('d0000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000001')
ON CONFLICT (user_id, role_id) DO NOTHING;
