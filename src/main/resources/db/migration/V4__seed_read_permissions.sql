-- V4: read-catalog permissions for the admin-UI groundwork. Additive + idempotent.

INSERT INTO public.t_permissions (id, created_at, created_by, updated_at, updated_by, description, name) VALUES
    ('a1000000-0000-0000-0000-000000000005', '2026-07-16 00:00:00', 'SYS', '2026-07-16 00:00:00', 'SYS', 'List roles', 'role:read'),
    ('a1000000-0000-0000-0000-000000000006', '2026-07-16 00:00:00', 'SYS', '2026-07-16 00:00:00', 'SYS', 'List permissions', 'permission:read')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.t_roles_permissions (role_id, permission_id) VALUES
    ('626c2558-a7d5-4fa4-9161-9bec2ae74076', 'a1000000-0000-0000-0000-000000000005'),
    ('626c2558-a7d5-4fa4-9161-9bec2ae74076', 'a1000000-0000-0000-0000-000000000006')
ON CONFLICT (role_id, permission_id) DO NOTHING;
