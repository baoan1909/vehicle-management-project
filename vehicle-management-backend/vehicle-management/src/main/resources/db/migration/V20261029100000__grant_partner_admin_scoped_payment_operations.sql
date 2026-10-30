-- Partner Admin may record and review payments for parking lots belonging to
-- its own organization. PaymentAccessGuard continues to enforce the
-- PARKING_SCOPE_PARTNER boundary at runtime.
INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'PAYMENT_CREATE_ALL', 'PAYMENT_READ_ALL'
)
WHERE role.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true, updated_at = now();
