ALTER TABLE opportunities
    ADD COLUMN activated_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE opportunities
    SET activated_at = created_at
    WHERE activated_at IS NULL;

ALTER TABLE opportunities
    ALTER COLUMN activated_at SET NOT NULL;
