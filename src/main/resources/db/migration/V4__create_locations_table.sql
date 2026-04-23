-- Creates the locations table for storing per-quote location records
CREATE TABLE IF NOT EXISTS locations (
    id          BIGSERIAL PRIMARY KEY,
    quote_id    BIGINT NOT NULL REFERENCES quotes(id),
    index       INTEGER NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    location_name VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT UK_locations_quote_index UNIQUE (quote_id, index)
);

CREATE INDEX IF NOT EXISTS IDX_locations_quote_id ON locations(quote_id);
