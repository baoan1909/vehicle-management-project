-- Operation permissions determine what may be done. These scope permissions
-- determine which parking lots are visible; membership/assignment supplies IDs.
INSERT INTO iam.permission_modules (module_id, code, name, description, created_at)
VALUES ('00000000-0000-0000-0000-000000001044', 'PARKING_DATA_SCOPE',
        'Phạm vi dữ liệu bãi xe', 'Quy định phạm vi truy cập dữ liệu vận hành.', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (
    permission_id, permission_code, name, description, created_at,
    module_id, action_id, scope_id
)
VALUES
    ('00000000-0000-0000-0000-000000004301', 'PARKING_SCOPE_PLATFORM', 'Giám sát dữ liệu toàn sàn', 'Đọc dữ liệu vận hành của mọi bãi xe.', now(), '00000000-0000-0000-0000-000000001044', '00000000-0000-0000-0000-000000002002', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004302', 'PARKING_SCOPE_PARTNER', 'Phạm vi bãi xe của đối tác', 'Truy cập các bãi thuộc Partner mà tài khoản đang là thành viên.', now(), '00000000-0000-0000-0000-000000001044', '00000000-0000-0000-0000-000000002002', '39ad7e5c-dfac-4a5e-b625-45a0500581b6'),
    ('00000000-0000-0000-0000-000000004303', 'PARKING_SCOPE_ASSIGNED', 'Phạm vi bãi xe được phân công', 'Truy cập các bãi được phân công cho tài khoản.', now(), '00000000-0000-0000-0000-000000001044', '00000000-0000-0000-0000-000000002002', '42a395e0-41dc-4952-bc48-0abef1eb0af8')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON
    (role.code = 'SYSTEM_ADMIN' AND permission.permission_code = 'PARKING_SCOPE_PLATFORM') OR
    (role.code = 'PARTNER_ADMIN' AND permission.permission_code = 'PARKING_SCOPE_PARTNER') OR
    (role.code = 'PARKING_MANAGER' AND permission.permission_code = 'PARKING_SCOPE_ASSIGNED')
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true, updated_at = now();

-- Lost-card workflows are operational. Partner Admin has the same operations
-- as a Manager, but both remain constrained by their parking-data scope.
INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'LOST_CARD_REPORT_READ_ALL',
    'LOST_CARD_REPORT_CREATE_ALL',
    'LOST_CARD_REPORT_UPDATE_ALL'
)
WHERE role.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true, updated_at = now();
