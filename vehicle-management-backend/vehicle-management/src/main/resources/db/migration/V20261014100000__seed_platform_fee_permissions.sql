-- Seed permission quan tri phi san toan he thong. Chi cap cho SYSTEM_ADMIN, khong hard-code role trong Java.
-- 6 permission: CREATE / READ / UPDATE / ACTIVATE / ARCHIVE / PREVIEW.

INSERT INTO iam.permission_modules (
    module_id,
    code,
    name,
    description,
    created_at
)
VALUES (
    '00000000-0000-0000-0000-000000001045',
    'PLATFORM_FEE',
    'Phi san',
    'Quan tri cac loai phi san ap dung toan he thong cho giao dich mua ve.',
    now()
)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO iam.permission_actions (action_id, code, name, description, created_at)
VALUES
    ('e7c2b3f1-9a4d-4e2b-8f1c-000000000001', 'ARCHIVE', 'Luu tru', 'Cho phep luu tru doi tuong nghiep vu.', now()),
    ('e7c2b3f1-9a4d-4e2b-8f1c-000000000002', 'PREVIEW', 'Xem truoc', 'Cho phep xem truoc ket qua tinh toan.', now())
ON CONFLICT (action_id) DO NOTHING;

INSERT INTO iam.permissions (
    permission_id,
    permission_code,
    name,
    description,
    created_at,
    module_id,
    action_id,
    scope_id
)
VALUES
    ('00000000-0000-0000-0000-000000004401', 'PLATFORM_FEE_CREATE_ALL', 'Tao phi san', 'Cho phep tao loai phi san toan he thong.', now(), '00000000-0000-0000-0000-000000001045', '00000000-0000-0000-0000-000000002001', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004402', 'PLATFORM_FEE_READ_ALL', 'Xem phi san', 'Cho phep xem danh sach, chi tiet va lich su phi san.', now(), '00000000-0000-0000-0000-000000001045', '00000000-0000-0000-0000-000000002002', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004403', 'PLATFORM_FEE_UPDATE_ALL', 'Cap nhat phi san', 'Cho phep cap nhat loai phi va phien ban o trang thai DRAFT.', now(), '00000000-0000-0000-0000-000000001045', '00000000-0000-0000-0000-000000002003', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004404', 'PLATFORM_FEE_ACTIVATE_ALL', 'Kich hoat phi san', 'Cho phep kich hoat / ngung ap dung loai phi va phien ban ty le.', now(), '00000000-0000-0000-0000-000000001045', '38f8b771-b1d7-4b5d-9b26-8c905804a3bf', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004405', 'PLATFORM_FEE_ARCHIVE_ALL', 'Luu tru phi san', 'Cho phep luu tru loai phi va phien ban, khong xoa cung du lieu tai chinh.', now(), '00000000-0000-0000-0000-000000001045', 'e7c2b3f1-9a4d-4e2b-8f1c-000000000001', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004406', 'PLATFORM_FEE_PREVIEW_ALL', 'Xem truoc phi san', 'Cho phep xem truoc tong phi ap dung tren don mua ve gia lap.', now(), '00000000-0000-0000-0000-000000001045', 'e7c2b3f1-9a4d-4e2b-8f1c-000000000002', '00000000-0000-0000-0000-000000003001')
ON CONFLICT (permission_code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    module_id = EXCLUDED.module_id,
    action_id = EXCLUDED.action_id,
    scope_id = EXCLUDED.scope_id;

INSERT INTO iam.role_permissions (
    id,
    role_id,
    permission_id,
    created_at,
    is_active,
    is_system
)
SELECT
    gen_random_uuid(),
    system_admin.role_id,
    permission.permission_id,
    now(),
    true,
    true
FROM iam.roles system_admin
JOIN iam.permissions permission ON permission.permission_code IN (
    'PLATFORM_FEE_CREATE_ALL',
    'PLATFORM_FEE_READ_ALL',
    'PLATFORM_FEE_UPDATE_ALL',
    'PLATFORM_FEE_ACTIVATE_ALL',
    'PLATFORM_FEE_ARCHIVE_ALL',
    'PLATFORM_FEE_PREVIEW_ALL'
)
WHERE system_admin.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
