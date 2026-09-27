-- Action permissions are separate from the lot scope enforced by InvoiceAccessGuard.
INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'INVOICE_READ_ALL', 'INVOICE_CREATE_ALL', 'INVOICE_CANCEL_ALL'
)
WHERE role.code IN ('PARTNER_ADMIN', 'PARKING_MANAGER')
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, updated_at = now();

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code = 'INVOICE_READ_ALL'
WHERE role.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, updated_at = now();

UPDATE iam.role_permissions role_permission SET is_active = false, updated_at = now()
FROM iam.roles role JOIN iam.permissions permission ON true
WHERE role_permission.role_id = role.role_id
  AND role_permission.permission_id = permission.permission_id
  AND role.code = 'SYSTEM_ADMIN'
  AND permission.permission_code IN ('INVOICE_CREATE_ALL', 'INVOICE_CANCEL_ALL');
