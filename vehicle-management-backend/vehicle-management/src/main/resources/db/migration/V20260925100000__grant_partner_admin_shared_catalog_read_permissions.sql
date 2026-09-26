-- Vehicle, ticket and card types are platform-managed reference catalogs.
-- A Partner Admin may consult them while configuring and reviewing its own parking lots,
-- but must not create, update, activate, or delete the shared definitions.
INSERT INTO iam.role_permissions (
    id,
    role_id,
    permission_id,
    created_at,
    is_active,
    is_system
)
SELECT
    gen_random_uuid(),
    partner_admin.role_id,
    permission.permission_id,
    now(),
    true,
    true
FROM iam.roles partner_admin
JOIN iam.permissions permission
    ON permission.permission_code IN (
        'VEHICLE_TYPE_READ_ALL',
        'TICKET_TYPE_READ_ALL',
        'CARD_TYPE_READ_ALL'
    )
WHERE partner_admin.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
