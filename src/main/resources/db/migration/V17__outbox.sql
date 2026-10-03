-- Transactional outbox (sub-project 2a). A Kafka message is written to this table in the same
-- transaction as the state change it announces; OutboxRelay sends it after the commit and stamps
-- published_at. Delivery is therefore at-least-once: every consumer must be idempotent.
CREATE TABLE IF NOT EXISTS public.t_outbox (
    id           uuid                           NOT NULL,
    topic        character varying(255)         NOT NULL,
    message_key  character varying(255),
    payload      bytea                          NOT NULL,
    created_at   timestamp(6) without time zone NOT NULL,
    published_at timestamp(6) without time zone,
    CONSTRAINT t_outbox_pkey PRIMARY KEY (id)
);

-- The relay's query reads only unpublished rows, oldest first.
CREATE INDEX IF NOT EXISTS idx_outbox_unpublished ON public.t_outbox (created_at) WHERE published_at IS NULL;
