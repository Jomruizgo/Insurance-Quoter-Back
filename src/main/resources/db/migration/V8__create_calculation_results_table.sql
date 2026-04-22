CREATE TABLE IF NOT EXISTS calculation_results (
    id                 BIGSERIAL PRIMARY KEY,
    quote_id           BIGINT         NOT NULL UNIQUE REFERENCES quotes(id) ON DELETE CASCADE,
    net_premium        DECIMAL(15,2)  NOT NULL,
    commercial_premium DECIMAL(15,2)  NOT NULL,
    calculated_at      TIMESTAMPTZ    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_calculation_results_quote_id ON calculation_results(quote_id);
