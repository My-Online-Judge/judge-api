-- Per-testcase judge results, stored verbatim as received from the judge.
-- NULL marks a submission judged before this feature shipped.
ALTER TABLE public.t_submissions ADD COLUMN IF NOT EXISTS details jsonb;

-- Whether a test case's input/output may be shown to the submitting user.
-- Defaults to false so every existing case stays hidden (fail-closed).
ALTER TABLE public.t_test_cases ADD COLUMN IF NOT EXISTS is_sample boolean NOT NULL DEFAULT false;

-- Authority for reading ANY user's submission (source code + per-case details).
-- Id ...00e is the next free slot; ...001 through ...00d are taken by V3/V8/V9.
INSERT INTO public.t_permissions (id, created_at, created_by, updated_at, updated_by, description, name) VALUES
    ('a1000000-0000-0000-0000-00000000000e', '2026-07-20 00:00:00', 'SYS', '2026-07-20 00:00:00', 'SYS', 'Read any user''s submission', 'submission:read_any')
ON CONFLICT (id) DO NOTHING;

-- Grant to ADMIN and SYS_ROOT explicitly. V9 warns SYS_ROOT is NOT auto-updated by later
-- migrations, so it must be listed here or the super-user silently lacks the authority.
INSERT INTO public.t_roles_permissions (role_id, permission_id) VALUES
    ('626c2558-a7d5-4fa4-9161-9bec2ae74076', 'a1000000-0000-0000-0000-00000000000e'),
    ('a2000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-00000000000e')
ON CONFLICT (role_id, permission_id) DO NOTHING;
