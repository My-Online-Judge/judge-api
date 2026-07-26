-- V11: t_submissions.error_message was varchar(255) in the V1 baseline (captured from a
-- pg_dump of the pre-Flyway schema), but the entity declares @Column(columnDefinition = "TEXT").
-- ddl-auto=validate does not catch the drift (Hibernate checks Java-type compatibility, not
-- length), so any verdict whose message exceeded 255 chars — a real gcc dump easily does —
-- failed to persist (SQLState 22001), rolled back the verdict transaction, and poison-pilled
-- the submission.judged consumer until the DLQ recoverer tripped; the user then saw
-- SYSTEM_ERROR instead of their actual verdict. Widen to match the entity.
-- varchar -> text is a metadata-only change in Postgres: no table rewrite, no data loss.
ALTER TABLE public.t_submissions
    ALTER COLUMN error_message TYPE text;
