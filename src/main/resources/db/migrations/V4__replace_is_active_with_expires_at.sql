ALTER TABLE opportunities
    ADD COLUMN expires_at DATE;

UPDATE opportunities
    SET expires_at = COALESCE(
        registration_deadline + INTERVAL '3 days',
        end_date,
        start_date,
        CAST(created_at AS DATE) + INTERVAL '30 days',
        CURRENT_DATE + INTERVAL '30 days'
    )
    WHERE expires_at IS NULL;

ALTER TABLE opportunities
    ALTER COLUMN expires_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_opportunities_expires_at ON opportunities (expires_at);

ALTER TABLE opportunities
    DROP COLUMN IF EXISTS is_active;
