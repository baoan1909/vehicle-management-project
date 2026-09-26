-- Partner Admin operates devices at every lot owned by the Partner.
-- DeviceUseCaseImpl validates the lot's organization before every read/write.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, is_active, is_system
)
SELECT
    gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission
    ON permission.permission_code IN (
        'DEVICE_CREATE_ALL',
        'DEVICE_READ_ALL',
        'DEVICE_UPDATE_ALL',
        'DEVICE_STATUS_UPDATE_ALL',
        'DEVICE_DELETE_ALL'
    )
WHERE role.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
