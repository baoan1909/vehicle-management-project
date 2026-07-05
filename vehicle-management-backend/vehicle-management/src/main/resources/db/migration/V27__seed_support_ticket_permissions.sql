BEGIN;

INSERT INTO iam.permission_actions (code, name, description)
VALUES
    ('CREATE', 'Create', 'Create a resource.'),
    ('READ', 'Read', 'Read a resource.'),
    ('UPDATE', 'Update', 'Update a resource.'),
    ('ASSIGN', 'Assign', 'Assign a resource to an account.'),
    ('PROCESS', 'Process', 'Process an assigned workflow item.'),
    ('REOPEN', 'Reopen', 'Reopen a resolved workflow item.'),
    ('CLOSE', 'Close', 'Close a resolved workflow item.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_scopes (code, name, description)
VALUES
    ('ALL', 'All', 'Apply to all resources in the module.'),
    ('OWN', 'Own', 'Apply only to resources owned by the current user.'),
    ('ASSIGNED', 'Assigned', 'Apply only to resources assigned to the current account.')
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
            'SUPPORT_TICKET_CREATE_OWN',
            'SUPPORT_TICKET',
            'CREATE',
            'OWN',
            'Create own support ticket',
            'Allow customers to create their own support tickets.'
        ),
        (
            'SUPPORT_TICKET_READ_OWN',
            'SUPPORT_TICKET',
            'READ',
            'OWN',
            'Read own support ticket',
            'Allow customers to read their own support tickets.'
        ),
        (
            'SUPPORT_TICKET_READ_ASSIGNED',
            'SUPPORT_TICKET',
            'READ',
            'ASSIGNED',
            'Read assigned support ticket',
            'Allow employees to read support tickets assigned to them.'
        ),
        (
            'SUPPORT_TICKET_READ_ALL',
            'SUPPORT_TICKET',
            'READ',
            'ALL',
            'Read all support tickets',
            'Allow managers to read all support tickets.'
        ),
        (
            'SUPPORT_TICKET_UPDATE_OWN',
            'SUPPORT_TICKET',
            'UPDATE',
            'OWN',
            'Update own support ticket',
            'Allow customers to update their own open support tickets.'
        ),
        (
            'SUPPORT_TICKET_UPDATE_ALL',
            'SUPPORT_TICKET',
            'UPDATE',
            'ALL',
            'Update all support tickets',
            'Allow managers to update open support tickets.'
        ),
        (
            'SUPPORT_TICKET_ASSIGN',
            'SUPPORT_TICKET',
            'ASSIGN',
            'ALL',
            'Assign support ticket',
            'Allow managers to assign support tickets to employees or managers.'
        ),
        (
            'SUPPORT_TICKET_PROCESS_ASSIGNED',
            'SUPPORT_TICKET',
            'PROCESS',
            'ASSIGNED',
            'Process assigned support ticket',
            'Allow employees to start progress and resolve support tickets assigned to them.'
        ),
        (
            'SUPPORT_TICKET_PROCESS_ALL',
            'SUPPORT_TICKET',
            'PROCESS',
            'ALL',
            'Process all support tickets',
            'Allow managers to start progress and resolve any support ticket.'
        ),
        (
            'SUPPORT_TICKET_REOPEN_OWN',
            'SUPPORT_TICKET',
            'REOPEN',
            'OWN',
            'Reopen own support ticket',
            'Allow customers to reopen their own resolved support tickets.'
        ),
        (
            'SUPPORT_TICKET_REOPEN_ALL',
            'SUPPORT_TICKET',
            'REOPEN',
            'ALL',
            'Reopen all support tickets',
            'Allow managers to reopen resolved support tickets.'
        ),
        (
            'SUPPORT_TICKET_CLOSE_OWN',
            'SUPPORT_TICKET',
            'CLOSE',
            'OWN',
            'Close own support ticket',
            'Allow customers to close their own resolved support tickets.'
        ),
        (
            'SUPPORT_TICKET_CLOSE_ALL',
            'SUPPORT_TICKET',
            'CLOSE',
            'ALL',
            'Close all support tickets',
            'Allow managers to close resolved support tickets.'
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
      'SUPPORT_TICKET_CREATE_OWN',
      'SUPPORT_TICKET_READ_OWN',
      'SUPPORT_TICKET_UPDATE_OWN',
      'SUPPORT_TICKET_REOPEN_OWN',
      'SUPPORT_TICKET_CLOSE_OWN'
  )
WHERE role_item.code = 'CUSTOMER'
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
  ON permission_item.permission_code IN (
      'SUPPORT_TICKET_READ_ASSIGNED',
      'SUPPORT_TICKET_PROCESS_ASSIGNED'
  )
WHERE role_item.code = 'EMPLOYEE'
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
  ON permission_item.permission_code IN (
      'SUPPORT_TICKET_READ_ALL',
      'SUPPORT_TICKET_UPDATE_ALL',
      'SUPPORT_TICKET_ASSIGN',
      'SUPPORT_TICKET_PROCESS_ALL',
      'SUPPORT_TICKET_REOPEN_ALL',
      'SUPPORT_TICKET_CLOSE_ALL'
  )
WHERE role_item.code IN ('PARKING_MANAGER', 'SYSTEM_ADMIN')
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = TRUE,
    is_system = TRUE,
    updated_at = now();

COMMIT;
