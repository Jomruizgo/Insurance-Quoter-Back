-- Add general info columns to quotes table for the general-info bounded context
ALTER TABLE quotes ADD COLUMN IF NOT EXISTS insured_name    VARCHAR(100);
ALTER TABLE quotes ADD COLUMN IF NOT EXISTS insured_rfc     VARCHAR(13);
ALTER TABLE quotes ADD COLUMN IF NOT EXISTS insured_email   VARCHAR(100);
ALTER TABLE quotes ADD COLUMN IF NOT EXISTS insured_phone   VARCHAR(20);
ALTER TABLE quotes ADD COLUMN IF NOT EXISTS risk_classification VARCHAR(20);
ALTER TABLE quotes ADD COLUMN IF NOT EXISTS business_type   VARCHAR(20);
