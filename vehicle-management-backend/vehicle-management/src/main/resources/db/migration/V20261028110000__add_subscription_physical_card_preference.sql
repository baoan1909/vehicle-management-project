ALTER TABLE access_control.subscriptions
    ADD COLUMN IF NOT EXISTS physical_card_requested BOOLEAN;

UPDATE access_control.subscriptions
SET physical_card_requested = TRUE
WHERE physical_card_requested IS NULL;

ALTER TABLE access_control.subscriptions
    ALTER COLUMN physical_card_requested SET DEFAULT TRUE,
    ALTER COLUMN physical_card_requested SET NOT NULL;

ALTER TABLE access_control.subscriptions
    ADD COLUMN IF NOT EXISTS physical_card_deposit NUMERIC(12, 2);

UPDATE access_control.subscriptions
SET physical_card_deposit = 0
WHERE physical_card_deposit IS NULL;

ALTER TABLE access_control.subscriptions
    ALTER COLUMN physical_card_deposit SET DEFAULT 0,
    ALTER COLUMN physical_card_deposit SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_subscriptions_physical_card_deposit_non_negative'
          AND conrelid = 'access_control.subscriptions'::regclass
    ) THEN
        ALTER TABLE access_control.subscriptions
            ADD CONSTRAINT ck_subscriptions_physical_card_deposit_non_negative
                CHECK (physical_card_deposit >= 0);
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_subscriptions_card_deposit_requires_card'
          AND conrelid = 'access_control.subscriptions'::regclass
    ) THEN
        ALTER TABLE access_control.subscriptions
            ADD CONSTRAINT ck_subscriptions_card_deposit_requires_card
                CHECK (physical_card_requested OR physical_card_deposit = 0);
    END IF;
END
$$;
