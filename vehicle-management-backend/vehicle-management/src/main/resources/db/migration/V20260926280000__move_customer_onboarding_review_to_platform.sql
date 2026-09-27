-- Customer accounts are shared across partners. Their onboarding approval must
-- be handled by the platform rather than an operator of a single parking lot.
INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission
  ON permission.permission_code = 'ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL'
WHERE role.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, updated_at = now();

UPDATE iam.role_permissions role_permission SET is_active = false, updated_at = now()
FROM iam.roles role JOIN iam.permissions permission ON true
WHERE role_permission.role_id = role.role_id
  AND role_permission.permission_id = permission.permission_id
  AND role.code IN ('PARTNER_ADMIN', 'PARKING_MANAGER')
  AND permission.permission_code = 'ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL';
