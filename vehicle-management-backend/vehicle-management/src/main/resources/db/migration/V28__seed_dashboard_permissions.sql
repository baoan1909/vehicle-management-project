BEGIN;

INSERT INTO iam.permission_modules (code, name, description)
VALUES ('DASHBOARD', 'Dashboard', 'Quản lý quyền xem dashboard thống kê.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (permission_code, module_id, action_id, scope_id, name, description)
SELECT
    'DASHBOARD_READ_ALL',
    m.module_id,
    a.action_id,
    s.scope_id,
    'Xem dashboard tổng quan',
    'Cho phép xem dashboard tổng quan hệ thống.'
FROM iam.permission_modules m
         JOIN iam.permission_actions a ON a.code = 'READ'
         JOIN iam.permission_scopes s ON s.code = 'ALL'
WHERE m.code = 'DASHBOARD'
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (id, role_id, permission_id, is_active, is_system)
SELECT gen_random_uuid(), r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code = 'DASHBOARD_READ_ALL'
WHERE r.code IN ('PARKING_MANAGER', 'SYSTEM_ADMIN')
  AND NOT EXISTS (
    SELECT 1
    FROM iam.role_permissions rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

COMMIT;