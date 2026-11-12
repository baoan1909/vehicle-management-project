ALTER TABLE access_control.subscriptions
    ADD COLUMN IF NOT EXISTS terminated_at timestamptz,
    ADD COLUMN IF NOT EXISTS termination_reason varchar(100),
    ADD COLUMN IF NOT EXISTS account_deletion_request_id uuid;

ALTER TABLE access_control.subscriptions DROP CONSTRAINT IF EXISTS ck_subscriptions_status;
ALTER TABLE access_control.subscriptions DROP CONSTRAINT IF EXISTS subscriptions_status_check;
ALTER TABLE access_control.subscriptions ADD CONSTRAINT ck_subscriptions_status CHECK (
    status IN ('PENDING','PENDING_PAYMENT','PENDING_CARD','PENDING_SPACE','ACTIVE',
               'EXPIRED','CANCELLED','REJECTED','TERMINATED')
);

ALTER TABLE access_control.subscriptions DROP CONSTRAINT IF EXISTS fk_subscriptions_account_deletion_request;
ALTER TABLE access_control.subscriptions ADD CONSTRAINT fk_subscriptions_account_deletion_request
    FOREIGN KEY (account_deletion_request_id)
    REFERENCES iam.account_deletion_requests(request_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_subscriptions_account_deletion_request
    ON access_control.subscriptions(account_deletion_request_id)
    WHERE account_deletion_request_id IS NOT NULL;
