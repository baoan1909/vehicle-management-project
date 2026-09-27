-- Cashiers may read and record payments only at a lot in their open shift.
INSERT INTO iam.permissions (
    permission_id, permission_code, name, description, created_at,
    module_id, action_id, scope_id
)
VALUES (
    '00000000-0000-0000-0000-000000004304', 'PARKING_SCOPE_SHIFT',
    'Phạm vi bãi xe theo ca trực', 'Truy cập thanh toán của bãi thuộc ca trực đang mở.', now(),
    '00000000-0000-0000-0000-000000001044',
    '00000000-0000-0000-0000-000000002002',
    '00000000-0000-0000-0000-000000003002'
)
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code = 'PARKING_SCOPE_SHIFT'
WHERE role.code = 'EMPLOYEE'
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, updated_at = now();
