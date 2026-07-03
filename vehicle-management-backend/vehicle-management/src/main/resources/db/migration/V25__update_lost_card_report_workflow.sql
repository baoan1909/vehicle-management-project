ALTER TABLE access_control.lost_card_reports
    ADD COLUMN IF NOT EXISTS subscription_id UUID,
    ADD COLUMN IF NOT EXISTS context VARCHAR(50),
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS cancelled_by UUID,
    ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500);

UPDATE access_control.lost_card_reports
SET context = CASE
                  WHEN parking_session_id IS NOT NULL AND customer_id IS NOT NULL THEN 'REGISTERED_IN_PARKING'
                  WHEN parking_session_id IS NOT NULL THEN 'VISITOR_IN_PARKING'
                  ELSE 'REGISTERED_OUTSIDE'
    END
WHERE context IS NULL;

ALTER TABLE access_control.lost_card_reports
    ALTER COLUMN context SET NOT NULL;

DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_lost_card_reports_subscription') THEN
            ALTER TABLE access_control.lost_card_reports
                ADD CONSTRAINT fk_lost_card_reports_subscription
                    FOREIGN KEY (subscription_id)
                        REFERENCES access_control.subscriptions(subscription_id)
                        ON DELETE SET NULL;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_lost_card_reports_cancelled_by') THEN
            ALTER TABLE access_control.lost_card_reports
                ADD CONSTRAINT fk_lost_card_reports_cancelled_by
                    FOREIGN KEY (cancelled_by)
                        REFERENCES iam.accounts(account_id)
                        ON DELETE SET NULL;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_lost_card_reports_context') THEN
            ALTER TABLE access_control.lost_card_reports
                ADD CONSTRAINT ck_lost_card_reports_context
                    CHECK (context IN ('VISITOR_IN_PARKING', 'REGISTERED_IN_PARKING', 'REGISTERED_OUTSIDE'));
        END IF;
    END $$;

CREATE INDEX IF NOT EXISTS idx_lost_card_reports_subscription
    ON access_control.lost_card_reports (subscription_id);

CREATE INDEX IF NOT EXISTS idx_lost_card_reports_context_status
    ON access_control.lost_card_reports (context, status);

CREATE INDEX IF NOT EXISTS idx_lost_card_reports_card_status
    ON access_control.lost_card_reports (card_id, status);