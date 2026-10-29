ALTER TABLE access_control.subscriptions
    ADD COLUMN IF NOT EXISTS period_quantity INTEGER;

UPDATE access_control.subscriptions
SET period_quantity = 1
WHERE period_quantity IS NULL;

ALTER TABLE access_control.subscriptions
    ALTER COLUMN period_quantity SET DEFAULT 1,
    ALTER COLUMN period_quantity SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_subscriptions_period_quantity'
          AND conrelid = 'access_control.subscriptions'::regclass
    ) THEN
        ALTER TABLE access_control.subscriptions
            ADD CONSTRAINT chk_subscriptions_period_quantity
                CHECK (period_quantity BETWEEN 1 AND 12);
    END IF;
END
$$;
