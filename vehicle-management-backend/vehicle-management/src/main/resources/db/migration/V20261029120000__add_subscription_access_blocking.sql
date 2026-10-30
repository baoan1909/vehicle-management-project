ALTER TABLE access_control.subscriptions
    ADD COLUMN IF NOT EXISTS access_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS access_blocked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS access_blocked_by UUID,
    ADD COLUMN IF NOT EXISTS access_restored_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS access_restored_by UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_subscriptions_access_blocked_by'
          AND conrelid = 'access_control.subscriptions'::regclass
    ) THEN
        ALTER TABLE access_control.subscriptions
            ADD CONSTRAINT fk_subscriptions_access_blocked_by
                FOREIGN KEY (access_blocked_by)
                REFERENCES iam.accounts(account_id)
                ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_subscriptions_access_restored_by'
          AND conrelid = 'access_control.subscriptions'::regclass
    ) THEN
        ALTER TABLE access_control.subscriptions
            ADD CONSTRAINT fk_subscriptions_access_restored_by
                FOREIGN KEY (access_restored_by)
                REFERENCES iam.accounts(account_id)
                ON DELETE SET NULL;
    END IF;
END $$;

COMMENT ON COLUMN access_control.subscriptions.access_blocked IS
    'Temporarily disables both QR and physical-card access without ending the subscription lifecycle.';
COMMENT ON COLUMN access_control.subscriptions.access_blocked_at IS
    'Time of the latest temporary access block.';
COMMENT ON COLUMN access_control.subscriptions.access_restored_at IS
    'Time of the latest access restoration.';
