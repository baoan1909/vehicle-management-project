-- Phase 2: permissions for per-space management, manual status and layout publish.
-- Topology remains Partner-operated: PARTNER_ADMIN and PARKING_MANAGER receive
-- configure/status/publish rights; SYSTEM_ADMIN stays a read-only observer.

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
    ('00000000-0000-0000-0000-000000004311', 'PARKING_SPACE_READ_ALL', 'Xem ô đỗ xe', 'Cho phép xem từng ô đỗ, trạng thái và sơ đồ trong phạm vi được phân công.', now(), '00000000-0000-0000-0000-000000001020', '00000000-0000-0000-0000-000000002002', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004312', 'PARKING_SPACE_CONFIGURE_ALL', 'Cấu hình ô đỗ xe', 'Cho phép tạo, sửa và lưu trữ từng ô đỗ trong bãi được phân công.', now(), '00000000-0000-0000-0000-000000001020', '00000000-0000-0000-0000-000000002003', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004313', 'PARKING_SPACE_STATUS_UPDATE_ALL', 'Cập nhật trạng thái ô đỗ', 'Cho phép cập nhật trạng thái thủ công của từng ô đỗ trong bãi được phân công.', now(), '00000000-0000-0000-0000-000000001020', '11dae923-4b69-4f31-be17-42fd7ab824dc', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004314', 'PARKING_SPACE_ASSIGN_ALL', 'Gán ô đỗ cho đăng ký', 'Cho phép giữ và gán ô đỗ cho vé đăng ký trong bãi được phân công.', now(), '00000000-0000-0000-0000-000000001020', '67921d5f-7815-4aa6-bf84-3c3c3aa15558', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004315', 'PARKING_LAYOUT_PUBLISH_ALL', 'Xuất bản sơ đồ bãi xe', 'Cho phép xuất bản sơ đồ ô đỗ trong bãi được phân công.', now(), '00000000-0000-0000-0000-000000001020', '338a1611-2d57-4f35-8399-7795ae6b2f8c', '00000000-0000-0000-0000-000000003001')
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
    role.role_id,
    permission.permission_id,
    now(),
    true,
    true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'PARKING_SPACE_READ_ALL',
    'PARKING_SPACE_CONFIGURE_ALL',
    'PARKING_SPACE_STATUS_UPDATE_ALL',
    'PARKING_SPACE_ASSIGN_ALL',
    'PARKING_LAYOUT_PUBLISH_ALL'
)
WHERE role.code IN ('PARTNER_ADMIN', 'PARKING_MANAGER')
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();

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
JOIN iam.permissions permission ON permission.permission_code = 'PARKING_SPACE_READ_ALL'
WHERE system_admin.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
