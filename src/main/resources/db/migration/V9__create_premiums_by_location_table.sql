CREATE TABLE IF NOT EXISTS premiums_by_location (
    id                       BIGSERIAL PRIMARY KEY,
    calculation_result_id    BIGINT         NOT NULL REFERENCES calculation_results(id) ON DELETE CASCADE,
    location_index           INTEGER        NOT NULL,
    location_name            VARCHAR(255),
    net_premium              DECIMAL(15,2),
    commercial_premium       DECIMAL(15,2),
    calculable               BOOLEAN        NOT NULL DEFAULT FALSE,
    fire_buildings           DECIMAL(15,2),
    fire_contents            DECIMAL(15,2),
    coverage_extension       DECIMAL(15,2),
    cattev                   DECIMAL(15,2),
    catfhm                   DECIMAL(15,2),
    debris_removal           DECIMAL(15,2),
    extraordinary_expenses   DECIMAL(15,2),
    rental_loss              DECIMAL(15,2),
    business_interruption    DECIMAL(15,2),
    electronic_equipment     DECIMAL(15,2),
    theft                    DECIMAL(15,2),
    cash_and_values          DECIMAL(15,2),
    glass                    DECIMAL(15,2),
    luminous_signage         DECIMAL(15,2),
    CONSTRAINT uq_pbl_calc_index UNIQUE (calculation_result_id, location_index)
);

CREATE INDEX IF NOT EXISTS idx_pbl_calculation_result_id ON premiums_by_location(calculation_result_id);

CREATE TABLE IF NOT EXISTS premium_location_blocking_alerts (
    premium_by_location_id  BIGINT       NOT NULL REFERENCES premiums_by_location(id) ON DELETE CASCADE,
    alert_code              VARCHAR(50)  NOT NULL,
    alert_message           VARCHAR(255) NOT NULL
);
