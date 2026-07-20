ALTER TABLE opportunities ADD COLUMN image_url VARCHAR(255);
ALTER TABLE opportunities ADD COLUMN is_free BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE raw_opportunities RENAME COLUMN source_url TO news_url;