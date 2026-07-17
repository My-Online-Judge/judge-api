-- Backfill t_problem_tags for databases that adopted Flyway via baseline-on-migrate.
-- Such DBs (built earlier by ddl-auto) are treated as already at V1, so V1's DDL is
-- never executed. The Problem.tags element-collection table was added after those
-- DBs' last ddl-auto run, so Hibernate schema-validation reports it missing.
-- Idempotent: a no-op on any database where V1 actually ran (fresh installs).

CREATE TABLE IF NOT EXISTS public.t_problem_tags (
    problem_id uuid NOT NULL,
    tag        character varying(255)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 't_problem_tags'
          AND constraint_name = 'fka8tpg3m9rbbg4qqxfxj42rey0'
    ) THEN
        ALTER TABLE public.t_problem_tags
            ADD CONSTRAINT fka8tpg3m9rbbg4qqxfxj42rey0
            FOREIGN KEY (problem_id) REFERENCES public.t_problems(id);
    END IF;
END $$;
