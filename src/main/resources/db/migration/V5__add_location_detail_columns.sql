-- Adds detail columns to locations table for location management feature
ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS address                   VARCHAR(500),
    ADD COLUMN IF NOT EXISTS zip_code                  VARCHAR(10),
    ADD COLUMN IF NOT EXISTS state                     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS municipality              VARCHAR(100),
    ADD COLUMN IF NOT EXISTS neighborhood              VARCHAR(100),
    ADD COLUMN IF NOT EXISTS city                      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS construction_type         VARCHAR(50),
    ADD COLUMN IF NOT EXISTS level                     INT,
    ADD COLUMN IF NOT EXISTS construction_year         INT,
    ADD COLUMN IF NOT EXISTS business_line_code        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS business_line_fire_key    VARCHAR(50),
    ADD COLUMN IF NOT EXISTS business_line_description VARCHAR(255),
    ADD COLUMN IF NOT EXISTS guarantees                TEXT,
    ADD COLUMN IF NOT EXISTS catastrophic_zone         VARCHAR(50),
    ADD COLUMN IF NOT EXISTS validation_status         VARCHAR(20) NOT NULL DEFAULT 'INCOMPLETE';
