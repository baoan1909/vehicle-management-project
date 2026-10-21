INSERT INTO iam.permission_modules (module_id, code, name, description, created_at)
VALUES (
    '00000000-0000-0000-0000-000000001047',
    'USER_GUIDE',
    'Hướng dẫn người dùng',
    'Quản trị nội dung hướng dẫn sử dụng cấp nền tảng.',
    now()
)
ON CONFLICT DO NOTHING;

INSERT INTO iam.permission_actions (action_id, code, name, description, created_at)
VALUES (
    'e7c2b3f1-9a4d-4e2b-8f1c-000000000001',
    'ARCHIVE',
    'Lưu trữ',
    'Cho phép lưu trữ đối tượng nghiệp vụ.',
    now()
)
ON CONFLICT DO NOTHING;

INSERT INTO iam.permissions (
    permission_id, permission_code, name, description, created_at, module_id, action_id, scope_id
)
VALUES
    ('00000000-0000-0000-0000-000000004501', 'USER_GUIDE_CREATE_ALL', 'Tạo hướng dẫn',
     'Cho phép tạo chức năng và hướng dẫn sử dụng cấp nền tảng.', now(),
     '00000000-0000-0000-0000-000000001047', '00000000-0000-0000-0000-000000002001', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004502', 'USER_GUIDE_READ_ALL', 'Xem hướng dẫn',
     'Cho phép xem toàn bộ cấu hình hướng dẫn sử dụng.', now(),
     '00000000-0000-0000-0000-000000001047', '00000000-0000-0000-0000-000000002002', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004503', 'USER_GUIDE_UPDATE_ALL', 'Cập nhật hướng dẫn',
     'Cho phép cập nhật hướng dẫn và bản nháp nội dung.', now(),
     '00000000-0000-0000-0000-000000001047', '00000000-0000-0000-0000-000000002003', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004504', 'USER_GUIDE_PUBLISH_ALL', 'Xuất bản hướng dẫn',
     'Cho phép xuất bản hoặc ngừng phục vụ một phiên bản hướng dẫn.', now(),
     '00000000-0000-0000-0000-000000001047', '338a1611-2d57-4f35-8399-7795ae6b2f8c', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004505', 'USER_GUIDE_ARCHIVE_ALL', 'Lưu trữ hướng dẫn',
     'Cho phép lưu trữ hướng dẫn mà không xóa dữ liệu nghiệp vụ.', now(),
     '00000000-0000-0000-0000-000000001047', 'e7c2b3f1-9a4d-4e2b-8f1c-000000000001', '00000000-0000-0000-0000-000000003001')
ON CONFLICT DO NOTHING;

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'USER_GUIDE_CREATE_ALL',
    'USER_GUIDE_READ_ALL',
    'USER_GUIDE_UPDATE_ALL',
    'USER_GUIDE_PUBLISH_ALL',
    'USER_GUIDE_ARCHIVE_ALL'
)
WHERE role.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    is_system = true,
    updated_at = now();

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM iam.roles WHERE code = 'SYSTEM_ADMIN') THEN
        RAISE EXCEPTION 'SYSTEM_ADMIN role is required for USER_GUIDE permission seed';
    END IF;

    IF (SELECT count(*) FROM iam.permissions WHERE permission_code LIKE 'USER_GUIDE\_%' ESCAPE '\') <> 5 THEN
        RAISE EXCEPTION 'Expected exactly five USER_GUIDE permissions';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM iam.role_permissions role_permission
        JOIN iam.roles role ON role.role_id = role_permission.role_id
        JOIN iam.permissions permission ON permission.permission_id = role_permission.permission_id
        WHERE permission.permission_code LIKE 'USER_GUIDE\_%' ESCAPE '\'
          AND role.code <> 'SYSTEM_ADMIN'
          AND role_permission.is_active = true
    ) THEN
        RAISE EXCEPTION 'USER_GUIDE permissions must only be granted to SYSTEM_ADMIN by default';
    END IF;
END $$;
