-- Platform oversight is read-only for Partner parking facilities and staffing.
-- Organization creation/Partner onboarding permissions are intentionally kept.
DELETE FROM iam.role_permissions rp
USING iam.roles role, iam.permissions permission
WHERE rp.role_id = role.role_id
  AND rp.permission_id = permission.permission_id
  AND role.code = 'SYSTEM_ADMIN'
  AND permission.permission_code IN (
      'PARKING_LOT_CREATE_ALL',
      'PARKING_LOT_UPDATE_ALL',
      'PARKING_LOT_DELETE_ALL',
      'ORGANIZATION_MEMBERSHIP_MANAGE_ALL'
  );
