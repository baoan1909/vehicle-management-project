-- Ensure existing CUSTOMER roles can manage only their own registered vehicles.
-- This is idempotent and reactivates permissions that may have been soft-revoked.
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
        'CUSTOMER_VEHICLE_CREATE_OWN',
        'CUSTOMER_VEHICLE_READ_OWN',
        'CUSTOMER_VEHICLE_UPDATE_OWN',
        'CUSTOMER_VEHICLE_DELETE_OWN'
    )
WHERE role.code = 'CUSTOMER'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
