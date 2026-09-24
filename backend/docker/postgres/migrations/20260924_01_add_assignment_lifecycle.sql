ALTER TABLE core.assignment
    ADD COLUMN IF NOT EXISTS status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ;

UPDATE core.assignment
SET status = 'PUBLISHED',
    published_at = COALESCE(published_at, created_at)
WHERE status IS NULL;

ALTER TABLE core.assignment ALTER COLUMN status SET NOT NULL;