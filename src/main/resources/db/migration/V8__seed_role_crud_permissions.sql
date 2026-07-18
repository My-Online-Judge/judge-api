-- V8: add role:create + role:delete permissions and grant them to ADMIN.
-- Additive + idempotent — safe on fresh DBs, DBs baselined via baseline-on-migrate, and re-runs.

INSERT INTO public.t_permissions (id, created_at, created_by, updated_at, updated_by, description, name) VALUES
    ('a1000000-0000-0000-0000-000000000008', '2026-07-18 00:00:00', 'SYS', '2026-07-18 00:00:00', 'SYS', 'Create a new role', 'role:create'),
    ('a1000000-0000-0000-0000-000000000009', '2026-07-18 00:00:00', 'SYS', '2026-07-18 00:00:00', 'SYS', 'Delete a role', 'role:delete')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.t_roles_permissions (role_id, permission_id) VALUES
    ('626c2558-a7d5-4fa4-9161-9bec2ae74076', 'a1000000-0000-0000-0000-000000000008'),
    ('626c2558-a7d5-4fa4-9161-9bec2ae74076', 'a1000000-0000-0000-0000-000000000009')
ON CONFLICT (role_id, permission_id) DO NOTHING;
