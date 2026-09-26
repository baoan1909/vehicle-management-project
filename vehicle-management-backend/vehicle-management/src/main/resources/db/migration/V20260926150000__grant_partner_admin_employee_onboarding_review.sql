-- Employee accounts are assigned to the creator's Partner at provisioning.
-- Review access still checks that the target account shares that Partner.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, is_active, is_system
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission
  ON permission.permission_code = 'ONBOARDING_APPROVAL_REVIEW_EMPLOYEE_ALL'
WHERE role.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true, updated_at = now();
