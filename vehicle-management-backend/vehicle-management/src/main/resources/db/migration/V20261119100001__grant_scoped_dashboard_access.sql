-- Dashboard metrics are still constrained at runtime by PARKING_SCOPE_PARTNER
-- or PARKING_SCOPE_ASSIGNED and the current organization/parking-lot assignments.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, is_active, is_system
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission
  ON permission.permission_code = 'DASHBOARD_READ_ALL'
WHERE role.code IN ('PARTNER_ADMIN', 'PARKING_MANAGER')
ON CONFLICT (role_id, permission_id)
DO UPDATE SET is_active = true, updated_at = now();
