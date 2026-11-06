ALTER TABLE billing.invoices
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(40);

UPDATE billing.invoices
SET purpose = CASE
    WHEN parking_session_id IS NOT NULL THEN 'PARKING_SESSION'
    WHEN subscription_id IS NOT NULL THEN 'SUBSCRIPTION_PURCHASE'
    WHEN lost_card_report_id IS NOT NULL THEN 'LOST_CARD'
    ELSE 'MANUAL'
END
WHERE purpose IS NULL;

ALTER TABLE billing.invoices
    ALTER COLUMN purpose SET DEFAULT 'MANUAL',
    ALTER COLUMN purpose SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_invoices_purpose'
          AND conrelid = 'billing.invoices'::regclass
    ) THEN
        ALTER TABLE billing.invoices
            ADD CONSTRAINT ck_invoices_purpose CHECK (purpose IN (
                'PARKING_SESSION', 'SUBSCRIPTION_PURCHASE',
                'SUBSCRIPTION_CARD_DEPOSIT', 'LOST_CARD', 'MANUAL'
            ));
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_invoices_subscription_purpose_status
    ON billing.invoices (subscription_id, purpose, status);

ALTER TABLE access_control.subscriptions
    ADD COLUMN IF NOT EXISTS physical_card_status VARCHAR(30);

UPDATE access_control.subscriptions
SET physical_card_status = CASE
    WHEN physical_card_requested = FALSE THEN 'NOT_REQUESTED'
    WHEN card_receipt_date IS NOT NULL THEN 'ASSIGNED'
    WHEN card_id IS NOT NULL THEN 'RESERVED'
    WHEN status IN ('ACTIVE', 'PENDING_SPACE') THEN 'WAITING_INVENTORY'
    ELSE 'PAYMENT_PENDING'
END
WHERE physical_card_status IS NULL;

ALTER TABLE access_control.subscriptions
    ALTER COLUMN physical_card_status SET DEFAULT 'NOT_REQUESTED',
    ALTER COLUMN physical_card_status SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_subscriptions_physical_card_status'
          AND conrelid = 'access_control.subscriptions'::regclass
    ) THEN
        ALTER TABLE access_control.subscriptions
            ADD CONSTRAINT ck_subscriptions_physical_card_status CHECK (physical_card_status IN (
                'NOT_REQUESTED', 'PAYMENT_PENDING', 'RESERVED',
                'WAITING_INVENTORY', 'ASSIGNED'
            ));
    END IF;
END
$$;
