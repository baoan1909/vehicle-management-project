BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS notification.broadcast_announcements (
    broadcast_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_type VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    audience_type VARCHAR(30) NOT NULL,
    role_codes JSONB,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    redirect_url VARCHAR(1000),
    status VARCHAR(30) NOT NULL,
    published_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    related_schema VARCHAR(50),
    related_table VARCHAR(80),
    related_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT fk_broadcast_announcements_created_by
        FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT fk_broadcast_announcements_updated_by
        FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_broadcast_announcements_type CHECK (
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
    ),
    CONSTRAINT ck_broadcast_announcements_audience_type
        CHECK (audience_type IN ('ALL_ACTIVE_ACCOUNTS', 'ROLE_CODES')),
    CONSTRAINT ck_broadcast_announcements_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED')),
    CONSTRAINT ck_broadcast_announcements_period
        CHECK (end_at IS NULL OR end_at >= start_at),
    CONSTRAINT ck_broadcast_announcements_role_codes_json
        CHECK (role_codes IS NULL OR jsonb_typeof(role_codes) = 'array'),
    CONSTRAINT ck_broadcast_announcements_role_audience
        CHECK (
            audience_type <> 'ROLE_CODES'
            OR CASE
                WHEN jsonb_typeof(role_codes) = 'array' THEN jsonb_array_length(role_codes) > 0
                ELSE FALSE
            END
        )
);

ALTER TABLE notification.notifications
    ADD COLUMN IF NOT EXISTS broadcast_id UUID,
    ADD COLUMN IF NOT EXISTS redirect_url VARCHAR(1000);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_notifications_broadcast_announcement'
          AND conrelid = 'notification.notifications'::regclass
    ) THEN
        ALTER TABLE notification.notifications
            ADD CONSTRAINT fk_notifications_broadcast_announcement
            FOREIGN KEY (broadcast_id)
            REFERENCES notification.broadcast_announcements(broadcast_id)
            ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_broadcast_announcements_status_created
    ON notification.broadcast_announcements(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_broadcast_announcements_active_window
    ON notification.broadcast_announcements(enabled, start_at, end_at);

CREATE INDEX IF NOT EXISTS idx_broadcast_announcements_related
    ON notification.broadcast_announcements(related_schema, related_table, related_id);

CREATE INDEX IF NOT EXISTS idx_notifications_broadcast_id
    ON notification.notifications(broadcast_id);

INSERT INTO iam.permission_modules (code, name, description)
VALUES
    ('BROADCAST_NOTIFICATION', 'Broadcast notification', 'Manage system-wide announcement notifications.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_actions (code, name, description)
VALUES
    ('PUBLISH', 'Publish', 'Publish a resource.'),
    ('CANCEL', 'Cancel', 'Cancel a resource.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (permission_code, module_id, action_id, scope_id, name, description)
SELECT permission_value.permission_code,
       module_item.module_id,
       action_item.action_id,
       scope_item.scope_id,
       permission_value.name,
       permission_value.description
FROM (
    VALUES
        ('BROADCAST_NOTIFICATION_CREATE_ALL', 'BROADCAST_NOTIFICATION', 'CREATE', 'ALL', 'Create broadcast notification', 'Allow creating broadcast announcement drafts.'),
        ('BROADCAST_NOTIFICATION_READ_ALL', 'BROADCAST_NOTIFICATION', 'READ', 'ALL', 'Read broadcast notification', 'Allow reading broadcast announcement list and detail.'),
        ('BROADCAST_NOTIFICATION_UPDATE_ALL', 'BROADCAST_NOTIFICATION', 'UPDATE', 'ALL', 'Update broadcast notification', 'Allow updating broadcast announcement drafts.'),
        ('BROADCAST_NOTIFICATION_DELETE_ALL', 'BROADCAST_NOTIFICATION', 'DELETE', 'ALL', 'Delete broadcast notification', 'Allow deleting unpublished broadcast announcements.'),
        ('BROADCAST_NOTIFICATION_PUBLISH_ALL', 'BROADCAST_NOTIFICATION', 'PUBLISH', 'ALL', 'Publish broadcast notification', 'Allow publishing broadcast announcements to target accounts.'),
        ('BROADCAST_NOTIFICATION_CANCEL_ALL', 'BROADCAST_NOTIFICATION', 'CANCEL', 'ALL', 'Cancel broadcast notification', 'Allow cancelling unpublished broadcast announcements.')
) AS permission_value(permission_code, module_code, action_code, scope_code, name, description)
JOIN iam.permission_modules module_item
  ON module_item.code = permission_value.module_code
JOIN iam.permission_actions action_item
  ON action_item.code = permission_value.action_code
JOIN iam.permission_scopes scope_item
  ON scope_item.code = permission_value.scope_code
ON CONFLICT (permission_code) DO UPDATE
SET module_id = EXCLUDED.module_id,
    action_id = EXCLUDED.action_id,
    scope_id = EXCLUDED.scope_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    updated_at = now();

INSERT INTO iam.role_permissions (role_id, permission_id, is_active, is_system)
SELECT role_item.role_id,
       permission_item.permission_id,
       TRUE,
       TRUE
FROM iam.roles role_item
JOIN iam.permissions permission_item
  ON permission_item.permission_code IN (
      'BROADCAST_NOTIFICATION_CREATE_ALL',
      'BROADCAST_NOTIFICATION_READ_ALL',
      'BROADCAST_NOTIFICATION_UPDATE_ALL',
      'BROADCAST_NOTIFICATION_DELETE_ALL',
      'BROADCAST_NOTIFICATION_PUBLISH_ALL',
      'BROADCAST_NOTIFICATION_CANCEL_ALL'
  )
WHERE role_item.code IN ('PARKING_MANAGER', 'SYSTEM_ADMIN')
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = TRUE,
    is_system = TRUE,
    updated_at = now();

COMMIT;
