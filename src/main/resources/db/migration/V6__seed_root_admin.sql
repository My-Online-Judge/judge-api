-- V6: seed the root administrator (username + password login) and make the ADMIN role
-- carry every permission in the catalog. Additive + idempotent.
--
-- SECURITY: the seeded password below is a well-known default committed to the repo.
-- Change it immediately after first login (or before exposing the app).

-- 1) ADMIN role gets EVERY permission currently in the catalog (present + any seeded before now).
INSERT INTO public.t_roles_permissions (role_id, permission_id)
SELECT '626c2558-a7d5-4fa4-9161-9bec2ae74076', p.id
FROM public.t_permissions p
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 2) Root admin user. Password: 'Admin@12345' (bcrypt strength 10). CHANGE IT.
INSERT INTO public.t_users
    (id, created_at, created_by, updated_at, updated_by,
     email, enabled_mfa, password, status, username, last_login, avatar, google_id, name)
VALUES
    ('d0000000-0000-0000-0000-000000000001',
     '2026-07-17 00:00:00', 'SYS', '2026-07-17 00:00:00', 'SYS',
     'admin@myoj.local', false,
     '$2b$10$JDu3Ln2zkmeiSGBzpdw43.o.Q/bYxFYrRLMMo5nr6E0U1cXzMlUCK',
     1, 'admin', NULL, NULL, NULL, 'Root Admin')
ON CONFLICT (id) DO NOTHING;

-- 3) Assign the ADMIN role to the root admin.
INSERT INTO public.t_users_roles (user_id, role_id)
VALUES ('d0000000-0000-0000-0000-000000000001', '626c2558-a7d5-4fa4-9161-9bec2ae74076')
ON CONFLICT (user_id, role_id) DO NOTHING;
