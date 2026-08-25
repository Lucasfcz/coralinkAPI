ALTER TABLE opportunities
    ADD COLUMN is_active BOOLEAN;

UPDATE opportunities
    SET is_active = TRUE
    WHERE is_active IS NULL;

ALTER TABLE opportunities
    ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE opportunities
    DROP COLUMN IF EXISTS activated_at;
