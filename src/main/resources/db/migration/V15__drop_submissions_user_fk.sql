-- Users live in identity-service's own database since sub-project 1b. t_submissions.user_id stays a
-- plain UUID and no longer references the t_users copy left in oj-db (dropped in SP2's cleanup): a user
-- created in identity-service never appears there, so the old foreign key would reject their first submission.
ALTER TABLE public.t_submissions DROP CONSTRAINT IF EXISTS fkd08ypnjk6cmr3yrcvm9b8rdyk;
