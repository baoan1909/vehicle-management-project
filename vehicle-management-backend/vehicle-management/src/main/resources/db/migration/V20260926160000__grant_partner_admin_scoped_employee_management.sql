-- Employee profile operations now require an active Partner membership on
-- both the acting account and the target employee's account.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, is_active, is_system
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission
    ON permission.permission_code IN (
        'EMPLOYEE_READ_ALL', 'EMPLOYEE_UPDATE_ALL', 'EMPLOYEE_DELETE_ALL'
    )
WHERE role.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true, updated_at = now();

-- System Admin observes Partner personnel but cannot mutate personnel records.
DELETE FROM iam.role_permissions rp
USING iam.roles role, iam.permissions permission
WHERE rp.role_id = role.role_id
  AND rp.permission_id = permission.permission_id
  AND role.code = 'SYSTEM_ADMIN'
  AND permission.permission_code IN ('EMPLOYEE_UPDATE_ALL', 'EMPLOYEE_DELETE_ALL');
