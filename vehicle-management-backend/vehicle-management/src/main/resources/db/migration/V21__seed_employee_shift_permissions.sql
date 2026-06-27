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
                 'SHIFT_OPEN_OWN',
                 'SHIFT',
                 'UPDATE',
                 'OWN',
                 'Open assigned shift',
                 'Allows an employee to open a shift assigned to them.'
             ),
             (
                 'SHIFT_ASSIGNMENT_READ_OWN',
                 'SHIFT_ASSIGNMENT',
                 'READ',
                 'OWN',
                 'Read own shift assignments',
                 'Allows an employee to view their own shift assignments.'
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
                                                     'SHIFT_OPEN_OWN',
                                                     'SHIFT_ASSIGNMENT_READ_OWN'
                  )
WHERE role_item.code = 'EMPLOYEE'
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET
                  is_active = TRUE,
                  is_system = TRUE,
                  updated_at = now();

COMMIT;