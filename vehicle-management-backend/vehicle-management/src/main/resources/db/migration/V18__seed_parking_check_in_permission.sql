BEGIN;

INSERT INTO iam.permission_actions (code, name, description)
VALUES ('CHECK_IN', 'Check in', 'Execute parking check-in command.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (permission_code, module_id, action_id, scope_id, name, description)
SELECT 'PARKING_SESSION_CHECK_IN_ALL',
       m.module_id,
       a.action_id,
       s.scope_id,
       'Check in parking session',
       'Allow executing parking session check-in workflow.'
FROM iam.permission_modules m
         JOIN iam.permission_actions a ON a.code = 'CHECK_IN'
         JOIN iam.permission_scopes s ON s.code = 'ALL'
WHERE m.code = 'PARKING_SESSION'
ON CONFLICT (permission_code)
    DO UPDATE SET module_id = EXCLUDED.module_id,
                  action_id = EXCLUDED.action_id,
                  scope_id = EXCLUDED.scope_id,
                  name = EXCLUDED.name,
                  description = EXCLUDED.description,
                  updated_at = now();

INSERT INTO iam.role_permissions (role_id, permission_id, is_active, is_system)
SELECT r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code = 'PARKING_SESSION_CHECK_IN_ALL'
WHERE r.code = 'EMPLOYEE'
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET is_active = TRUE,
                  is_system = TRUE,
                  updated_at = now()
WHERE iam.role_permissions.is_active = FALSE OR iam.role_permissions.is_system = FALSE;

COMMIT;
