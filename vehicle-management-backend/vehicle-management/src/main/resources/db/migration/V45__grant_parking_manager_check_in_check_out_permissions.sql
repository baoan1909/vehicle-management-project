BEGIN;

-- Parking manager có thể thực hiện trực tiếp luồng check-in và check-out.
INSERT INTO iam.role_permissions (role_id, permission_id, is_active, is_system)
SELECT role_item.role_id, permission_item.permission_id, TRUE, TRUE
FROM iam.roles role_item
         JOIN iam.permissions permission_item
              ON permission_item.permission_code IN (
                  'PARKING_SESSION_CHECK_IN_ALL',
                  'PARKING_SESSION_CHECK_OUT_ALL'
              )
WHERE role_item.code = 'PARKING_MANAGER'
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET is_active = TRUE,
                  is_system = TRUE,
                  updated_at = now()
WHERE iam.role_permissions.is_active = FALSE
   OR iam.role_permissions.is_system = FALSE;

COMMIT;
