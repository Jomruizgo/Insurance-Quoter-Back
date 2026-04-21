-- Adds layout configuration columns to the quotes table
ALTER TABLE quotes
    ADD COLUMN IF NOT EXISTS number_of_locations INTEGER,
    ADD COLUMN IF NOT EXISTS location_type VARCHAR(10);
