-- Prevents two CREATED folios for the same subscriber+agent combination.
-- Partial index: only one row per (subscriber_id, agent_code) when status is CREATED.
CREATE UNIQUE INDEX IF NOT EXISTS uq_quotes_active_folio
    ON quotes (subscriber_id, agent_code)
    WHERE quote_status = 'CREATED';
