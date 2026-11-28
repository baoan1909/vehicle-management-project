-- Global broadcast management is exclusive to SYSTEM_ADMIN, including custom roles.
UPDATE iam.role_permissions rp
SET is_active = false, updated_at = now()
FROM iam.roles r, iam.permissions p
WHERE rp.role_id = r.role_id AND rp.permission_id = p.permission_id
  AND r.code <> 'SYSTEM_ADMIN'
  AND p.permission_code LIKE 'BROADCAST_NOTIFICATION\_%' ESCAPE '\';

-- Operational screens use scoped customer/employee endpoints, not the full profile directory.
UPDATE iam.role_permissions rp
SET is_active = false, updated_at = now()
FROM iam.roles r, iam.permissions p
WHERE rp.role_id = r.role_id AND rp.permission_id = p.permission_id
  AND r.code IN ('PARKING_MANAGER', 'EMPLOYEE', 'PARTNER_ADMIN')
  AND p.permission_code IN ('USER_PROFILE_CREATE_ALL', 'USER_PROFILE_READ_ALL', 'USER_PROFILE_UPDATE_ALL');

INSERT INTO iam.permission_modules (module_id, code, name, description, created_at)
VALUES (gen_random_uuid(), 'AVATAR_APPROVAL', 'Duyệt ảnh đại diện',
        'Duyệt ảnh đại diện trong phạm vi nhân sự được quản lý.', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (permission_id, permission_code, name, description,
                             module_id, action_id, scope_id, created_at)
SELECT gen_random_uuid(), seed.code, seed.name, seed.description,
       m.module_id, a.action_id, s.scope_id, now()
FROM (VALUES
    ('AVATAR_APPROVAL_READ_ALL', 'READ', 'Xem yêu cầu duyệt ảnh đại diện', 'Xem yêu cầu thuộc phạm vi nhân sự được quản lý.'),
    ('AVATAR_APPROVAL_REVIEW_ALL', 'UPDATE', 'Duyệt hoặc từ chối ảnh đại diện', 'Duyệt yêu cầu thuộc phạm vi nhân sự được quản lý.')
) AS seed(code, action, name, description)
JOIN iam.permission_modules m ON m.code = 'AVATAR_APPROVAL'
JOIN iam.permission_actions a ON a.code = seed.action
JOIN iam.permission_scopes s ON s.code = 'ALL'
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), r.role_id, p.permission_id, now(), true, true
FROM iam.roles r
JOIN iam.permissions p ON
    (r.code = 'SYSTEM_ADMIN' AND p.permission_code LIKE 'BROADCAST_NOTIFICATION\_%' ESCAPE '\')
    OR (r.code IN ('SYSTEM_ADMIN', 'PARTNER_ADMIN', 'PARKING_MANAGER')
        AND p.permission_code IN ('AVATAR_APPROVAL_READ_ALL', 'AVATAR_APPROVAL_REVIEW_ALL'))
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, updated_at = now();
