BEGIN;

INSERT INTO iam.permission_modules (code, name, description)
VALUES
    ('LOST_CARD_REPORT', 'Lost card report', 'Manage lost card report workflow.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_actions (code, name, description)
VALUES
    ('CREATE', 'Create', 'Create a resource.'),
    ('READ', 'Read', 'Read a resource.'),
    ('UPDATE', 'Update', 'Update a resource.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (permission_code, module_id, action_id, scope_id, name, description)
SELECT permission_value.permission_code,
       module_item.module_id,
       action_item.action_id,
       scope_item.scope_id,
       permission_value.name,
       permission_value.description
FROM (
    VALUES
        (
            'LOST_CARD_REPORT_CREATE_ALL',
            'LOST_CARD_REPORT',
            'CREATE',
            'ALL',
            'Create lost card report',
            'Allow creating lost card reports.'
        ),
        (
            'LOST_CARD_REPORT_READ_ALL',
            'LOST_CARD_REPORT',
            'READ',
            'ALL',
            'Read lost card report',
            'Allow reading lost card report list, detail, and preview.'
        ),
        (
            'LOST_CARD_REPORT_UPDATE_ALL',
            'LOST_CARD_REPORT',
            'UPDATE',
            'ALL',
            'Update lost card report',
            'Allow cancelling and resolving lost card reports.'
        )
) AS permission_value(permission_code, module_code, action_code, scope_code, name, description)
JOIN iam.permission_modules module_item
  ON module_item.code = permission_value.module_code
JOIN iam.permission_actions action_item
  ON action_item.code = permission_value.action_code
JOIN iam.permission_scopes scope_item
  ON scope_item.code = permission_value.scope_code
ON CONFLICT (permission_code) DO UPDATE
SET module_id = EXCLUDED.module_id,
    action_id = EXCLUDED.action_id,
    scope_id = EXCLUDED.scope_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    updated_at = now();

INSERT INTO iam.role_permissions (role_id, permission_id, is_active, is_system)
SELECT role_item.role_id,
       permission_item.permission_id,
       TRUE,
       TRUE
FROM iam.roles role_item
JOIN iam.permissions permission_item
  ON permission_item.permission_code IN (
      'LOST_CARD_REPORT_CREATE_ALL',
      'LOST_CARD_REPORT_READ_ALL',
      'LOST_CARD_REPORT_UPDATE_ALL'
  )
WHERE role_item.code IN ('EMPLOYEE', 'PARKING_MANAGER', 'SYSTEM_ADMIN')
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = TRUE,
    is_system = TRUE,
    updated_at = now();

COMMIT;
