CREATE TABLE IF NOT EXISTS quotes (
    id              BIGSERIAL PRIMARY KEY,
    folio_number    VARCHAR(20)                 NOT NULL UNIQUE,
    quote_status    VARCHAR(20)                 NOT NULL,
    subscriber_id   VARCHAR(50)                 NOT NULL,
    agent_code      VARCHAR(50)                 NOT NULL,
    version         BIGINT                      NOT NULL DEFAULT 1,
    created_at      TIMESTAMP WITH TIME ZONE    NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_quotes_idempotency
    ON quotes (subscriber_id, agent_code, quote_status);
