ALTER TABLE people.user_profile_avatars
    DROP CONSTRAINT IF EXISTS ck_user_profile_avatars_status;

ALTER TABLE people.user_profile_avatars
    ADD CONSTRAINT ck_user_profile_avatars_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'CANCELLED', 'REPLACED', 'DELETED'));

CREATE UNIQUE INDEX uq_user_profile_pending_avatar
    ON people.user_profile_avatars (user_profile_id)
    WHERE status = 'PENDING';

CREATE TABLE operations.avatar_approval_policies (
    policy_id UUID PRIMARY KEY,
    auto_approve_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    effective_from TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES iam.accounts(account_id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES iam.accounts(account_id),
    CONSTRAINT ck_avatar_approval_policy_effective_from
        CHECK (auto_approve_enabled = FALSE OR effective_from IS NOT NULL)
);

INSERT INTO operations.avatar_approval_policies (
    policy_id, auto_approve_enabled, effective_from, version
) VALUES (
    '62000000-0000-4000-8000-000000000003', FALSE, NULL, 0
);

INSERT INTO iam.role_permissions (
    id, role_id, permission_id, is_active, is_system, created_at
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, TRUE, TRUE, now()
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code = 'USER_PROFILE_UPDATE_ALL'
WHERE role.code = 'SYSTEM_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM iam.role_permissions existing
      WHERE existing.role_id = role.role_id
        AND existing.permission_id = permission.permission_id
  );

ALTER TABLE notification.notifications DROP CONSTRAINT IF EXISTS ck_notifications_type;

ALTER TABLE notification.notifications
    ADD CONSTRAINT ck_notifications_type CHECK (notification_type IN (
        'SYSTEM_NOTICE', 'SUBSCRIPTION_REQUESTED', 'SUBSCRIPTION_APPROVED',
        'SUBSCRIPTION_REJECTED', 'SUBSCRIPTION_EXPIRING_SOON', 'SUBSCRIPTION_EXPIRED',
        'SUBSCRIPTION_CANCELLED', 'SUBSCRIPTION_PAYMENT_COMPLETED', 'INVOICE_CREATED',
        'PAYMENT_SUCCEEDED', 'PAYMENT_FAILED', 'SUPPORT_TICKET_CREATED',
        'SUPPORT_TICKET_ASSIGNED', 'SUPPORT_TICKET_IN_PROGRESS', 'SUPPORT_TICKET_RESPONDED',
        'SUPPORT_TICKET_REOPENED', 'SUPPORT_TICKET_CLOSED', 'SHIFT_ASSIGNED',
        'SHIFT_CHANGED', 'SHIFT_CANCELLED', 'DEVICE_OFFLINE', 'DEVICE_MAINTENANCE',
        'LANE_MAINTENANCE', 'PARKING_LOT_MAINTENANCE', 'PRICE_PLAN_CHANGED',
        'PRICE_RULE_CHANGED', 'TICKET_TYPE_CHANGED', 'ACCOUNT_REGISTERED',
        'ACCOUNT_PROVISIONED', 'ACCOUNT_STATUS_CHANGED', 'ACCOUNT_PROFILE_SUBMITTED',
        'AVATAR_APPROVAL_SUBMITTED', 'AVATAR_APPROVED', 'AVATAR_REJECTED',
        'CUSTOMER_ONBOARDING_APPROVED', 'CUSTOMER_ONBOARDING_REJECTED',
        'CUSTOMER_ONBOARDING_RESUBMITTED', 'INTERNAL_EMPLOYEE_APPROVED',
        'INTERNAL_EMPLOYEE_REJECTED', 'INTERNAL_EMPLOYEE_RESUBMITTED',
        'SYSTEM_ADMIN_APPROVED', 'SYSTEM_ADMIN_REJECTED', 'SYSTEM_ADMIN_RESUBMITTED'
    ));
