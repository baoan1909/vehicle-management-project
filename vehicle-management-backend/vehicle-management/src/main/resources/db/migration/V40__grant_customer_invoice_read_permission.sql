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
    'INVOICE_READ_OWN',
    m.module_id,
    a.action_id,
    s.scope_id,
    'Xem hoa don cua minh',
    'Cho phep khach hang xem hoa don gan voi ho so cua chinh minh.'
FROM iam.permission_modules m
JOIN iam.permission_actions a ON a.code = 'READ'
JOIN iam.permission_scopes s ON s.code = 'OWN'
WHERE m.code = 'INVOICE'
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (
    id,
    role_id,
    permission_id,
    is_active,
    is_system
)
SELECT
    gen_random_uuid(),
    r.role_id,
    p.permission_id,
    TRUE,
    TRUE
FROM iam.roles r
JOIN iam.permissions p
    ON p.permission_code = 'INVOICE_READ_OWN'
WHERE r.code = 'CUSTOMER'
ON CONFLICT (role_id, permission_id)
DO UPDATE SET
    is_active = TRUE,
    is_system = TRUE,
    updated_at = now();

COMMIT;
