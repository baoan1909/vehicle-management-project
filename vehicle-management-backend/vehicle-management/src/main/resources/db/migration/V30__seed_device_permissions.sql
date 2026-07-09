BEGIN;

INSERT INTO iam.permission_modules (code, name, description)
VALUES
    ('DEVICE', 'Device', 'Manage hardware devices such as cameras, kiosks, card readers and barriers.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_actions (code, name, description)
VALUES
    ('CREATE', 'Create', 'Create a resource.'),
    ('READ', 'Read', 'Read a resource.'),
    ('UPDATE', 'Update', 'Update a resource.'),
    ('DELETE', 'Delete', 'Delete or soft-delete a resource.'),
    ('STATUS_UPDATE', 'Status update', 'Update resource operational status.')
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
            'DEVICE_CREATE_ALL',
            'DEVICE',
            'CREATE',
            'ALL',
            'Create device',
            'Allow creating hardware devices.'
        ),
        (
            'DEVICE_READ_ALL',
            'DEVICE',
            'READ',
            'ALL',
            'Read device',
            'Allow reading hardware device list and detail.'
        ),
        (
            'DEVICE_UPDATE_ALL',
            'DEVICE',
            'UPDATE',
            'ALL',
            'Update device',
            'Allow updating hardware device information.'
        ),
        (
            'DEVICE_STATUS_UPDATE_ALL',
            'DEVICE',
            'STATUS_UPDATE',
            'ALL',
            'Update device status',
            'Allow activating, marking offline, and marking maintenance for hardware devices.'
        ),
        (
            'DEVICE_DELETE_ALL',
            'DEVICE',
            'DELETE',
            'ALL',
            'Delete device',
            'Allow retiring hardware devices.'
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
      'DEVICE_CREATE_ALL',
      'DEVICE_READ_ALL',
      'DEVICE_UPDATE_ALL',
      'DEVICE_STATUS_UPDATE_ALL',
      'DEVICE_DELETE_ALL'
  )
WHERE role_item.code IN ('PARKING_MANAGER', 'SYSTEM_ADMIN')
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = TRUE,
    is_system = TRUE,
    updated_at = now();

INSERT INTO iam.role_permissions (role_id, permission_id, is_active, is_system)
SELECT role_item.role_id,
       permission_item.permission_id,
       TRUE,
       TRUE
FROM iam.roles role_item
JOIN iam.permissions permission_item
  ON permission_item.permission_code = 'DEVICE_READ_ALL'
WHERE role_item.code = 'EMPLOYEE'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = TRUE,
    is_system = TRUE,
    updated_at = now();

COMMIT;
