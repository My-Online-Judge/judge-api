-- Sub-project 2a: submissions stop depending on t_problems, which moves to problem-service in 2b.
--
-- problem_slug is copied onto each submission so that "the submissions of problem X" needs neither a
-- join nor a call to another service. That is safe because a slug never changes (updates exclude it)
-- and is never reused (a deleted problem keeps its slug).
--
-- The column stays NULLABLE here on purpose: the image before 2a inserts submissions without it, so a
-- rollback of 2a keeps working. The judge-api of 2b backfills the rows such a rollback left NULL and
-- only then sets NOT NULL (expand / contract).
ALTER TABLE public.t_submissions ADD COLUMN IF NOT EXISTS problem_slug character varying(255);

UPDATE public.t_submissions s
   SET problem_slug = p.problem_slug
  FROM public.t_problems p
 WHERE p.id = s.problem_id
   AND s.problem_slug IS NULL;

CREATE INDEX IF NOT EXISTS idx_submissions_problem_slug_created_at
    ON public.t_submissions (problem_slug, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_submissions_user_problem_slug_created_at
    ON public.t_submissions (user_id, problem_slug, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_submissions_problem_id
    ON public.t_submissions (problem_id);

-- A problem created in problem-service's database never appears in this t_problems: with the foreign
-- key, its first submission would fail (SQLState 23503).
ALTER TABLE public.t_submissions DROP CONSTRAINT IF EXISTS fk6a65byiirmyaxvk5nropbc6rf;
