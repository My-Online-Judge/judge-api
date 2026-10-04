-- Sub-project 2b: problems live in problem-service, and a submission names its problem by slug. V16 added
-- the column nullable so a rollback to the pre-2a image could still insert; rows such an image wrote have
-- no slug, so fill them from t_problems (kept in oj-db, untouched, until sub-project 3), then require it.
UPDATE t_submissions s
SET problem_slug = p.problem_slug
FROM t_problems p
WHERE s.problem_id = p.id
  AND s.problem_slug IS NULL;

ALTER TABLE t_submissions ALTER COLUMN problem_slug SET NOT NULL;
