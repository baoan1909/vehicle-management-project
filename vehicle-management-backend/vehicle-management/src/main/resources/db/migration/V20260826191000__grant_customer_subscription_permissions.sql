-- Ensure existing CUSTOMER roles can use their own monthly-ticket subscriptions.
-- This is idempotent and also reactivates permissions that may have been soft-revoked.
INSERT INTO iam.role_permissions (
    id,
    role_id,
    permission_id,
    created_at,
    updated_at,
    is_active,
    is_system
)
SELECT
    gen_random_uuid(),
    role.role_id,
    permission.permission_id,
    now(),
    now(),
    true,
    true
FROM iam.roles role
JOIN iam.permissions permission
    ON permission.permission_code IN (
        'SUBSCRIPTION_CREATE_OWN',
        'SUBSCRIPTION_READ_OWN',
        'SUBSCRIPTION_UPDATE_OWN',
        'SUBSCRIPTION_CANCEL_OWN'
    )
WHERE role.code = 'CUSTOMER'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
