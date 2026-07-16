-- V3: permission-based authorization. Additive + idempotent so it is safe on a fresh DB,
-- on an existing DB adopting Flyway via baseline-on-migrate, and when re-run.
-- Column names mirror t_roles / t_users_roles so Hibernate ddl-auto:validate passes.

CREATE TABLE IF NOT EXISTS public.t_permissions (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    created_by character varying(255),
    updated_at timestamp(6) without time zone NOT NULL,
    updated_by character varying(255),
    description character varying(255),
    name character varying(255),
    CONSTRAINT t_permissions_pkey PRIMARY KEY (id),
    CONSTRAINT t_permissions_name_key UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS public.t_roles_permissions (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL,
    CONSTRAINT t_roles_permissions_pkey PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_roles_permissions_role FOREIGN KEY (role_id) REFERENCES public.t_roles(id),
    CONSTRAINT fk_roles_permissions_permission FOREIGN KEY (permission_id) REFERENCES public.t_permissions(id)
);

INSERT INTO public.t_permissions (id, created_at, created_by, updated_at, updated_by, description, name) VALUES
    ('a1000000-0000-0000-0000-000000000001', '2026-07-16 00:00:00', 'SYS', '2026-07-16 00:00:00', 'SYS', 'Create problems', 'problem:create'),
    ('a1000000-0000-0000-0000-000000000002', '2026-07-16 00:00:00', 'SYS', '2026-07-16 00:00:00', 'SYS', 'Update problems', 'problem:update'),
    ('a1000000-0000-0000-0000-000000000003', '2026-07-16 00:00:00', 'SYS', '2026-07-16 00:00:00', 'SYS', 'Delete problems', 'problem:delete'),
    ('a1000000-0000-0000-0000-000000000004', '2026-07-16 00:00:00', 'SYS', '2026-07-16 00:00:00', 'SYS', 'List judge servers', 'judgeserver:read')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.t_roles_permissions (role_id, permission_id) VALUES
    ('626c2558-a7d5-4fa4-9161-9bec2ae74076', 'a1000000-0000-0000-0000-000000000001'),
    ('626c2558-a7d5-4fa4-9161-9bec2ae74076', 'a1000000-0000-0000-0000-000000000002'),
    ('626c2558-a7d5-4fa4-9161-9bec2ae74076', 'a1000000-0000-0000-0000-000000000003'),
    ('626c2558-a7d5-4fa4-9161-9bec2ae74076', 'a1000000-0000-0000-0000-000000000004')
ON CONFLICT (role_id, permission_id) DO NOTHING;
