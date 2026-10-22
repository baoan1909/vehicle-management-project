-- A Parking Manager onboarding request belongs to the Partner Admin who
-- provisioned that Manager account. It must not be visible to System Admins
-- or to another Partner Admin in the same/different organization.
INSERT INTO iam.permissions (
    permission_id, permission_code, name, description,
    created_at, created_by, updated_at, updated_by,
    module_id, action_id, scope_id
) VALUES (
    '4a7d0300-0000-4000-8000-000000000005',
    'ONBOARDING_APPROVAL_REVIEW_PARKING_MANAGER_OWN',
    'Duyệt onboarding Parking Manager do mình tạo',
    'Cho phép Partner Admin xem và duyệt hồ sơ Parking Manager do chính tài khoản đó tạo.',
    now(), NULL, NULL, NULL,
    '4a7d0100-0000-4000-8000-000000000001',
    '4a7d0200-0000-4000-8000-000000000002',
    '00000000-0000-0000-0000-000000003002'
)
ON CONFLICT DO NOTHING;

INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT
    gen_random_uuid(), role.role_id, permission.permission_id, now(), NULL, NULL, NULL, true, true
FROM iam.roles role
JOIN iam.permissions permission
    ON permission.permission_code = 'ONBOARDING_APPROVAL_REVIEW_PARKING_MANAGER_OWN'
WHERE role.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();

-- The previous global permission let System Admin review Parking Manager
-- onboarding. Retire that grant; the controller now requires the scoped
-- permission above and checks iam.accounts.created_by as a second boundary.
UPDATE iam.role_permissions role_permission
SET is_active = false,
    updated_at = now()
FROM iam.roles role
JOIN iam.permissions permission
    ON permission.permission_code = 'ONBOARDING_APPROVAL_REVIEW_PARKING_MANAGER_ALL'
WHERE role_permission.role_id = role.role_id
  AND role_permission.permission_id = permission.permission_id
  AND role.code = 'SYSTEM_ADMIN';
