-- V10: auth telemetry columns (IP / device / user-agent) for later rate-limiting.
-- Additive + idempotent, all nullable — safe on fresh DBs, baselined DBs, and re-runs.

ALTER TABLE public.t_tokens ADD COLUMN IF NOT EXISTS ip VARCHAR(45);
ALTER TABLE public.t_tokens ADD COLUMN IF NOT EXISTS device_hash VARCHAR(128);
ALTER TABLE public.t_tokens ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512);
ALTER TABLE public.t_users  ADD COLUMN IF NOT EXISTS last_login_ip VARCHAR(45);
ALTER TABLE public.t_users  ADD COLUMN IF NOT EXISTS last_login_device_hash VARCHAR(128);
