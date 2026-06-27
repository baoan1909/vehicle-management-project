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
    permission_value.permission_code,
    module_item.module_id,
    action_item.action_id,
    scope_item.scope_id,
    permission_value.name,
    permission_value.description
FROM (
         VALUES
             (
                 'SHIFT_ASSIGNMENT_CREATE_ALL',
                 'SHIFT_ASSIGNMENT', 'CREATE', 'ALL',
                 'Tạo quy tắc phân công',
                 'Cho phép tạo roster rule và phân công ca trực.'
             ),
             (
                 'SHIFT_ASSIGNMENT_READ_ALL',
                 'SHIFT_ASSIGNMENT', 'READ', 'ALL',
                 'Xem quy tắc phân công',
                 'Cho phép xem roster rule và phân công ca trực.'
             ),
             (
                 'SHIFT_ASSIGNMENT_UPDATE_ALL',
                 'SHIFT_ASSIGNMENT', 'UPDATE', 'ALL',
                 'Cập nhật quy tắc phân công',
                 'Cho phép cập nhật, kích hoạt và điều chỉnh phân công.'
             ),
             (
                 'SHIFT_ASSIGNMENT_DELETE_ALL',
                 'SHIFT_ASSIGNMENT', 'DELETE', 'ALL',
                 'Ngừng quy tắc phân công',
                 'Cho phép xóa mềm roster rule hoặc phân công.'
             )
     ) AS permission_value(
                           permission_code,
                           module_code,
                           action_code,
                           scope_code,
                           name,
                           description
    )
         JOIN iam.permission_modules module_item
              ON module_item.code = permission_value.module_code
         JOIN iam.permission_actions action_item
              ON action_item.code = permission_value.action_code
         JOIN iam.permission_scopes scope_item
              ON scope_item.code = permission_value.scope_code
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
                                                     'SHIFT_ASSIGNMENT_CREATE_ALL',
                                                     'SHIFT_ASSIGNMENT_READ_ALL',
                                                     'SHIFT_ASSIGNMENT_UPDATE_ALL',
                                                     'SHIFT_ASSIGNMENT_DELETE_ALL'
                  )
WHERE role_item.code IN (
                         'PARKING_MANAGER',
                         'SYSTEM_ADMIN'
    )
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET
                  is_active = TRUE,
                  is_system = TRUE,
                  updated_at = now();

COMMIT;