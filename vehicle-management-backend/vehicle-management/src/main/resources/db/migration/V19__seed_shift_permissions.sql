BEGIN;

INSERT INTO iam.permissions (
    permission_code,
    module_id,
    action_id,
    scope_id,
    name,
    description
)
SELECT
    value_item.permission_code,
    module_item.module_id,
    action_item.action_id,
    scope_item.scope_id,
    value_item.name,
    value_item.description
FROM (
         VALUES
             (
                 'SHIFT_CREATE_ALL',
                 'SHIFT', 'CREATE', 'ALL',
                 'Tạo cấu hình ca trực',
                 'Cho phép tạo shift template và ca trực.'
             ),
             (
                 'SHIFT_READ_ALL',
                 'SHIFT', 'READ', 'ALL',
                 'Xem cấu hình ca trực',
                 'Cho phép xem shift template và ca trực.'
             ),
             (
                 'SHIFT_UPDATE_ALL',
                 'SHIFT', 'UPDATE', 'ALL',
                 'Cập nhật cấu hình ca trực',
                 'Cho phép cập nhật và kích hoạt shift template.'
             ),
             (
                 'SHIFT_DELETE_ALL',
                 'SHIFT', 'DELETE', 'ALL',
                 'Ngừng cấu hình ca trực',
                 'Cho phép xóa mềm shift template.'
             )
     ) AS value_item(
                     permission_code,
                     module_code,
                     action_code,
                     scope_code,
                     name,
                     description
    )
         JOIN iam.permission_modules module_item
              ON module_item.code = value_item.module_code
         JOIN iam.permission_actions action_item
              ON action_item.code = value_item.action_code
         JOIN iam.permission_scopes scope_item
              ON scope_item.code = value_item.scope_code
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (
    role_id,
    permission_id,
    is_active,
    is_system
)
SELECT
    role_item.role_id,
    permission_item.permission_id,
    TRUE,
    TRUE
FROM iam.roles role_item
         JOIN iam.permissions permission_item
              ON permission_item.permission_code IN (
                                                     'SHIFT_CREATE_ALL',
                                                     'SHIFT_READ_ALL',
                                                     'SHIFT_UPDATE_ALL',
                                                     'SHIFT_DELETE_ALL'
                  )
WHERE role_item.code IN ('PARKING_MANAGER', 'SYSTEM_ADMIN')
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET
                  is_active = TRUE,
                  is_system = TRUE,
                  updated_at = now();

COMMIT;