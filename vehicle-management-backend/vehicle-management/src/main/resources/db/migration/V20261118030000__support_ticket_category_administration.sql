-- Dedicated administration permissions for support ticket categories.
-- Runtime authorization remains permission-based; SYSTEM_ADMIN is only the
-- initial role receiving these permissions.

INSERT INTO iam.permission_actions (
    action_id, code, name, description, created_at
) VALUES
    ('d1810000-0000-4000-8000-000000000031', 'READ_SUPPORT_CATEGORY', 'Xem cấu hình danh mục hỗ trợ', 'Xem danh sách và chi tiết cấu hình danh mục hỗ trợ.', now()),
    ('d1810000-0000-4000-8000-000000000032', 'CREATE_SUPPORT_CATEGORY', 'Tạo danh mục hỗ trợ', 'Tạo danh mục yêu cầu hỗ trợ mới.', now()),
    ('d1810000-0000-4000-8000-000000000033', 'UPDATE_SUPPORT_CATEGORY', 'Cập nhật danh mục hỗ trợ', 'Cập nhật nội dung và điều kiện sử dụng danh mục hỗ trợ.', now()),
    ('d1810000-0000-4000-8000-000000000034', 'ACTIVATE_SUPPORT_CATEGORY', 'Kích hoạt danh mục hỗ trợ', 'Kích hoạt hoặc vô hiệu hóa danh mục hỗ trợ.', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (
    permission_id, permission_code, name, description,
    module_id, action_id, scope_id, created_at
)
SELECT seed.permission_id, seed.permission_code, seed.name, seed.description,
       module.module_id, action.action_id, scope.scope_id, now()
FROM (VALUES
    ('d1810000-0000-4000-8000-000000000001'::uuid, 'SUPPORT_TICKET_CATEGORY_READ_ALL', 'Xem danh mục hỗ trợ', 'Cho phép xem toàn bộ cấu hình danh mục hỗ trợ.', 'READ_SUPPORT_CATEGORY'),
    ('d1810000-0000-4000-8000-000000000002'::uuid, 'SUPPORT_TICKET_CATEGORY_CREATE_ALL', 'Tạo danh mục hỗ trợ', 'Cho phép tạo danh mục hỗ trợ.', 'CREATE_SUPPORT_CATEGORY'),
    ('d1810000-0000-4000-8000-000000000003'::uuid, 'SUPPORT_TICKET_CATEGORY_UPDATE_ALL', 'Cập nhật danh mục hỗ trợ', 'Cho phép cập nhật danh mục hỗ trợ.', 'UPDATE_SUPPORT_CATEGORY'),
    ('d1810000-0000-4000-8000-000000000004'::uuid, 'SUPPORT_TICKET_CATEGORY_ACTIVATE_ALL', 'Kích hoạt danh mục hỗ trợ', 'Cho phép kích hoạt hoặc vô hiệu hóa danh mục hỗ trợ.', 'ACTIVATE_SUPPORT_CATEGORY')
) seed(permission_id, permission_code, name, description, action_code)
JOIN iam.permission_modules module ON module.code = 'SUPPORT_TICKET'
JOIN iam.permission_actions action ON action.code = seed.action_code
JOIN iam.permission_scopes scope ON scope.code = 'ALL'
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id,
       now(), NULL, NULL, NULL, TRUE, TRUE
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'SUPPORT_TICKET_CATEGORY_READ_ALL',
    'SUPPORT_TICKET_CATEGORY_CREATE_ALL',
    'SUPPORT_TICKET_CATEGORY_UPDATE_ALL',
    'SUPPORT_TICKET_CATEGORY_ACTIVATE_ALL'
)
WHERE role.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = TRUE, updated_at = now();

-- Legacy ALL values came from the original customer category seed. Categories
-- created for operations were already marked INTERNAL by the account-owned
-- workflow migration.
UPDATE operations.support_ticket_categories
SET audience_type = 'CUSTOMER', updated_at = now()
WHERE audience_type = 'ALL';

ALTER TABLE operations.support_ticket_categories
    DROP CONSTRAINT IF EXISTS ck_support_ticket_categories_audience;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'operations.support_ticket_categories'::regclass
          AND conname = 'ck_support_ticket_categories_audience'
    ) THEN
        ALTER TABLE operations.support_ticket_categories
            ADD CONSTRAINT ck_support_ticket_categories_audience CHECK (
                audience_type IN ('CUSTOMER', 'INTERNAL')
            );
    END IF;
END;
$$;

DROP INDEX IF EXISTS operations.uq_support_ticket_categories_active_code;

CREATE UNIQUE INDEX IF NOT EXISTS uq_support_ticket_categories_code
    ON operations.support_ticket_categories (code);
