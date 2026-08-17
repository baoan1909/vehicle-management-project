BEGIN;

ALTER TABLE access_control.cards
    ADD COLUMN IF NOT EXISTS status_before_blocked VARCHAR(20),
    ADD COLUMN IF NOT EXISTS blocked_by UUID,
    ADD COLUMN IF NOT EXISTS retired_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS retired_by UUID,
    ADD COLUMN IF NOT EXISTS retired_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS recovered_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS recovered_by UUID,
    ADD COLUMN IF NOT EXISTS recovery_note VARCHAR(500);

UPDATE access_control.cards card
SET status_before_blocked = CASE
    WHEN EXISTS (
        SELECT 1
        FROM parking.parking_sessions session
        WHERE session.card_id = card.card_id
          AND session.status = 'OPEN'
    ) THEN 'IN_USE'
    WHEN EXISTS (
        SELECT 1
        FROM access_control.subscriptions subscription
        WHERE subscription.card_id = card.card_id
          AND subscription.status IN ('PENDING_PAYMENT', 'PENDING_CARD')
    ) THEN 'RESERVED'
    WHEN EXISTS (
        SELECT 1
        FROM access_control.subscriptions subscription
        WHERE subscription.card_id = card.card_id
          AND subscription.status = 'ACTIVE'
    ) THEN 'ASSIGNED'
    ELSE 'AVAILABLE'
END,
    blocked_by = COALESCE(card.blocked_by, card.updated_by, card.created_by)
WHERE card.status = 'BLOCKED'
  AND card.status_before_blocked IS NULL;

UPDATE access_control.cards
SET status = 'RETIRED',
    retired_at = COALESCE(retired_at, updated_at, created_at, now()),
    retired_by = COALESCE(retired_by, updated_by, created_by),
    retired_reason = COALESCE(retired_reason, 'Hỏng vật lý (migrated)')
WHERE status = 'DAMAGED';

UPDATE access_control.cards
SET retired_at = COALESCE(retired_at, updated_at, created_at, now()),
    retired_by = COALESCE(retired_by, updated_by, created_by),
    retired_reason = COALESCE(retired_reason, 'Ngừng sử dụng (migrated)')
WHERE status = 'RETIRED';

ALTER TABLE access_control.cards
    DROP CONSTRAINT IF EXISTS ck_cards_status;

ALTER TABLE access_control.cards
    ADD CONSTRAINT ck_cards_status
        CHECK (status IN (
            'AVAILABLE',
            'RESERVED',
            'ASSIGNED',
            'IN_USE',
            'LOST',
            'BLOCKED',
            'RETIRED'
        ));

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cards_blocked_by') THEN
        ALTER TABLE access_control.cards
            ADD CONSTRAINT fk_cards_blocked_by
                FOREIGN KEY (blocked_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cards_retired_by') THEN
        ALTER TABLE access_control.cards
            ADD CONSTRAINT fk_cards_retired_by
                FOREIGN KEY (retired_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cards_recovered_by') THEN
        ALTER TABLE access_control.cards
            ADD CONSTRAINT fk_cards_recovered_by
                FOREIGN KEY (recovered_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_cards_status_before_blocked
    ON access_control.cards(status, status_before_blocked);

COMMIT;
