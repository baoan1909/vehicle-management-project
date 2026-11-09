ALTER TABLE access_control.subscriptions
    ADD COLUMN IF NOT EXISTS space_selection_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS space_selection_confirmed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS space_selection_expires_at TIMESTAMPTZ;

ALTER TABLE access_control.subscriptions
    DROP CONSTRAINT IF EXISTS ck_subscriptions_status;

ALTER TABLE access_control.subscriptions
    DROP CONSTRAINT IF EXISTS subscriptions_status_check;

ALTER TABLE access_control.subscriptions
    ADD CONSTRAINT ck_subscriptions_status CHECK (
        status IN (
            'PENDING', 'PENDING_PAYMENT', 'PENDING_CARD', 'PENDING_SPACE',
            'ACTIVE', 'EXPIRED', 'CANCELLED', 'REJECTED'
        )
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_parking_space_allocation_live_subscription
    ON parking.parking_space_allocations (subscription_id)
    WHERE allocation_status IN ('HELD', 'ACTIVE');

CREATE INDEX IF NOT EXISTS idx_subscriptions_space_selection_expiry
    ON access_control.subscriptions (space_selection_expires_at)
    WHERE space_selection_required = TRUE
      AND space_selection_confirmed_at IS NULL;
