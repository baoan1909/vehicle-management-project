ALTER TABLE catalog.card_types
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN;

UPDATE catalog.card_types
SET is_active = TRUE
WHERE is_active IS NULL;

ALTER TABLE catalog.card_types
    ALTER COLUMN is_active SET DEFAULT TRUE;

ALTER TABLE catalog.card_types
    ALTER COLUMN is_active SET NOT NULL;
