CREATE TABLE IF NOT EXISTS coverage_options (
    id                     BIGSERIAL PRIMARY KEY,
    quote_id               BIGINT         NOT NULL
        REFERENCES quotes(id) ON DELETE CASCADE,
    code                   VARCHAR(50)    NOT NULL,
    description            VARCHAR(255),
    selected               BOOLEAN        NOT NULL DEFAULT FALSE,
    deductible_percentage  DECIMAL(5,2)
        CONSTRAINT chk_deductible_pct CHECK (deductible_percentage >= 0 AND deductible_percentage <= 100),
    coinsurance_percentage DECIMAL(5,2)
        CONSTRAINT chk_coinsurance_pct CHECK (coinsurance_percentage >= 0 AND coinsurance_percentage <= 100),
    CONSTRAINT uk_coverage_options_quote_code UNIQUE (quote_id, code)
);

CREATE INDEX IF NOT EXISTS idx_coverage_options_quote_id ON coverage_options (quote_id);
