ALTER TABLE iam.account_deletion_requests
    ADD COLUMN IF NOT EXISTS active_subscription_no_refund_confirmed boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN iam.account_deletion_requests.active_subscription_no_refund_confirmed IS
    'Explicit customer consent to terminate active subscriptions without refund after the deletion grace period.';
