-- Reuse the existing VEHICLE_TYPE_UPDATE_ALL action permission. The effective
-- parking-lot boundary is enforced separately by PARKING_SCOPE_PARTNER or
-- PARKING_SCOPE_ASSIGNED and OrganizationAccessGuard.

INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by,
    updated_at, updated_by, is_active, is_system
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), NULL,
       NULL, NULL, true, true
FROM iam.roles role
JOIN iam.permissions permission
  ON permission.permission_code = 'VEHICLE_TYPE_UPDATE_ALL'
WHERE role.code IN ('PARTNER_ADMIN', 'PARKING_MANAGER')
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
