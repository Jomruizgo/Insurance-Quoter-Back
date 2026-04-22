-- Creates table for storing blocking alerts per location
CREATE TABLE IF NOT EXISTS location_blocking_alerts (
    location_id   BIGINT       NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    alert_code    VARCHAR(50)  NOT NULL,
    alert_message VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_location_alerts_location_id ON location_blocking_alerts (location_id);
