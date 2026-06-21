BEGIN;

INSERT INTO iam.role_permissions (role_id, permission_id, is_active, is_system)
SELECT r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code IN (
             'ACCOUNT_CREATE_ALL',
             'ACCOUNT_READ_ALL',
             'ACCOUNT_UPDATE_ALL'
         )
WHERE r.code = 'PARKING_MANAGER'
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET is_active = TRUE, is_system = TRUE, updated_at = now()
WHERE iam.role_permissions.is_active = FALSE OR iam.role_permissions.is_system = FALSE;

COMMIT;
