-- Partner Admin may read its approved organization profile and submit a change request.
-- System Admin alone reviews partner registration and profile-change workflows.
-- Review permissions use their own module/actions because iam.permissions requires
-- every (module_id, action_id, scope_id) combination to be unique.
INSERT INTO iam.permission_modules (module_id, code, name, description, created_at)
VALUES (
    gen_random_uuid(), 'ORGANIZATION_APPROVAL', 'Duyệt hồ sơ đối tác',
    'Quản lý quyền xét duyệt đăng ký và cập nhật hồ sơ đối tác.', now()
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_actions (action_id, code, name, description, created_at)
VALUES
    (gen_random_uuid(), 'REVIEW_ORGANIZATION_PROFILE', 'Duyệt cập nhật hồ sơ đối tác',
     'Cho phép xem, duyệt hoặc từ chối yêu cầu cập nhật hồ sơ đối tác.', now()),
    (gen_random_uuid(), 'REVIEW_PARTNER_REGISTRATION', 'Duyệt đăng ký đối tác',
     'Cho phép xem, duyệt hoặc từ chối hồ sơ đăng ký đối tác.', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (
    permission_id, permission_code, name, description, created_at, module_id, action_id, scope_id
) SELECT
    gen_random_uuid(), seed.permission_code, seed.name, seed.description, now(),
    module.module_id, action.action_id, scope.scope_id
FROM (
    VALUES
        ('ORGANIZATION_PROFILE_READ_OWN', 'Xem hồ sơ đối tác của mình',
         'Cho phép xem hồ sơ đối tác mà tài khoản đang là thành viên hoạt động.',
         'ORGANIZATION', 'READ', 'OWN'),
        ('ORGANIZATION_PROFILE_UPDATE_REQUEST_OWN', 'Gửi yêu cầu cập nhật hồ sơ đối tác',
         'Cho phép gửi thay đổi hồ sơ đối tác của mình để System Admin xét duyệt.',
         'ORGANIZATION', 'UPDATE', 'OWN'),
        ('ORGANIZATION_PROFILE_REVIEW_ALL', 'Duyệt cập nhật hồ sơ đối tác',
         'Cho phép duyệt hoặc từ chối yêu cầu cập nhật hồ sơ của mọi đối tác.',
         'ORGANIZATION_APPROVAL', 'REVIEW_ORGANIZATION_PROFILE', 'ALL'),
        ('ORGANIZATION_REGISTRATION_REVIEW_ALL', 'Duyệt đăng ký đối tác',
         'Cho phép xem, duyệt hoặc từ chối hồ sơ đăng ký đối tác trên toàn nền tảng.',
         'ORGANIZATION_APPROVAL', 'REVIEW_PARTNER_REGISTRATION', 'ALL')
) AS seed(permission_code, name, description, module_code, action_code, scope_code)
JOIN iam.permission_modules module ON module.code = seed.module_code
JOIN iam.permission_actions action ON action.code = seed.action_code
JOIN iam.permission_scopes scope ON scope.code = seed.scope_code
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM (
    VALUES
        ('PARTNER_ADMIN', 'ORGANIZATION_PROFILE_READ_OWN'),
        ('PARTNER_ADMIN', 'ORGANIZATION_PROFILE_UPDATE_REQUEST_OWN'),
        ('SYSTEM_ADMIN', 'ORGANIZATION_PROFILE_REVIEW_ALL'),
        ('SYSTEM_ADMIN', 'ORGANIZATION_REGISTRATION_REVIEW_ALL')
) AS mapping(role_code, permission_code)
JOIN iam.roles role ON role.code = mapping.role_code
JOIN iam.permissions permission ON permission.permission_code = mapping.permission_code
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
