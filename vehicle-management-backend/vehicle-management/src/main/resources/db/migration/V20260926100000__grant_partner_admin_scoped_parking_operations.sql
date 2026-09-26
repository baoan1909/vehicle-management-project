-- Partner Admin may operate physical cards and the entrance/exit workflows in
-- every parking lot belonging to their active organization membership.
-- Both use cases check the lot scope; this does not grant shared catalog rights.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, is_active, is_system
)
SELECT
    gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission
    ON permission.permission_code IN (
        'CARD_CREATE_ALL',
        'CARD_READ_ALL',
        'CARD_UPDATE_ALL',
        'CARD_DELETE_ALL',
        'PARKING_SESSION_CHECK_IN_ALL',
        'PARKING_SESSION_CHECK_OUT_ALL'
    )
WHERE role.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
