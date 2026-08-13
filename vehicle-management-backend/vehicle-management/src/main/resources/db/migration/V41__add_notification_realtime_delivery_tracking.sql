ALTER TABLE notification.notifications
    ADD COLUMN IF NOT EXISTS realtime_delivered_at TIMESTAMPTZ;

ALTER TABLE notification.notifications
    ADD COLUMN IF NOT EXISTS notification_type VARCHAR(80) NOT NULL DEFAULT 'SYSTEM_NOTICE';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_notifications_type'
          AND conrelid = 'notification.notifications'::regclass
    ) THEN
        ALTER TABLE notification.notifications
            ADD CONSTRAINT ck_notifications_type CHECK (
                notification_type IN (
                    'SYSTEM_NOTICE',
                    'SUBSCRIPTION_REQUESTED',
                    'SUBSCRIPTION_APPROVED',
                    'SUBSCRIPTION_REJECTED',
                    'SUBSCRIPTION_EXPIRING_SOON',
                    'SUBSCRIPTION_EXPIRED',
                    'SUBSCRIPTION_CANCELLED',
                    'SUBSCRIPTION_PAYMENT_COMPLETED',
                    'INVOICE_CREATED',
                    'PAYMENT_SUCCEEDED',
                    'PAYMENT_FAILED',
                    'SUPPORT_TICKET_CREATED',
                    'SUPPORT_TICKET_ASSIGNED',
                    'SUPPORT_TICKET_IN_PROGRESS',
                    'SUPPORT_TICKET_RESPONDED',
                    'SUPPORT_TICKET_REOPENED',
                    'SUPPORT_TICKET_CLOSED',
                    'SHIFT_ASSIGNED',
                    'SHIFT_CHANGED',
                    'SHIFT_CANCELLED',
                    'DEVICE_OFFLINE',
                    'DEVICE_MAINTENANCE',
                    'LANE_MAINTENANCE',
                    'PARKING_LOT_MAINTENANCE',
                    'PRICE_PLAN_CHANGED',
                    'PRICE_RULE_CHANGED',
                    'TICKET_TYPE_CHANGED',
                    'ACCOUNT_REGISTERED',
                    'ACCOUNT_PROVISIONED',
                    'ACCOUNT_STATUS_CHANGED',
                    'ACCOUNT_PROFILE_SUBMITTED',
                    'CUSTOMER_ONBOARDING_APPROVED',
                    'CUSTOMER_ONBOARDING_REJECTED',
                    'CUSTOMER_ONBOARDING_RESUBMITTED',
                    'INTERNAL_EMPLOYEE_APPROVED',
                    'INTERNAL_EMPLOYEE_REJECTED',
                    'INTERNAL_EMPLOYEE_RESUBMITTED',
                    'SYSTEM_ADMIN_APPROVED',
                    'SYSTEM_ADMIN_REJECTED',
                    'SYSTEM_ADMIN_RESUBMITTED'
                )
            );
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_notifications_realtime_pending
    ON notification.notifications(account_id, created_at)
    WHERE channel = 'WEB' AND status = 'SENT' AND read_at IS NULL AND realtime_delivered_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_account_type_created
    ON notification.notifications(account_id, notification_type, created_at DESC);
