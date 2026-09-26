-- Shift and assignment use cases now validate lot ownership on every
-- management read/write. Partner Admin can cover a lot without a Manager.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, is_active, is_system
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission
    ON permission.permission_code IN (
        'SHIFT_CREATE_ALL', 'SHIFT_READ_ALL', 'SHIFT_UPDATE_ALL', 'SHIFT_DELETE_ALL',
        'SHIFT_ASSIGNMENT_CREATE_ALL', 'SHIFT_ASSIGNMENT_READ_ALL',
        'SHIFT_ASSIGNMENT_UPDATE_ALL', 'SHIFT_ASSIGNMENT_DELETE_ALL'
    )
WHERE role.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true, updated_at = now();

-- System Admin remains a cross-partner observer, not an operator.
DELETE FROM iam.role_permissions rp
USING iam.roles role, iam.permissions permission
WHERE rp.role_id = role.role_id
  AND rp.permission_id = permission.permission_id
  AND role.code = 'SYSTEM_ADMIN'
  AND permission.permission_code IN (
      'SHIFT_CREATE_ALL', 'SHIFT_UPDATE_ALL', 'SHIFT_DELETE_ALL',
      'SHIFT_ASSIGNMENT_CREATE_ALL', 'SHIFT_ASSIGNMENT_UPDATE_ALL',
      'SHIFT_ASSIGNMENT_DELETE_ALL'
  );
