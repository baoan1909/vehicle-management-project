-- System Admin is a platform supervisor: read every Partner/parking-lot resource,
-- manage IAM and Partner governance, but never operate a Partner parking lot.

-- Voucher is a platform campaign resource.  It must not reuse PRICE_RULE permissions,
-- because a voucher administrator must not consequently be able to change parking prices.
INSERT INTO iam.permission_modules (
    module_id,
    code,
    name,
    description,
    created_at
)
VALUES (
    '00000000-0000-0000-0000-000000001042',
    'VOUCHER',
    'Voucher',
    'Quản lý voucher và chương trình ưu đãi của nền tảng.',
    now()
)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO iam.permission_modules (
    module_id,
    code,
    name,
    description,
    created_at
)
VALUES (
    '00000000-0000-0000-0000-000000001043',
    'PARKING_TOPOLOGY',
    'Topology bãi xe',
    'Cấu hình khu vực, cổng và làn xe của bãi.',
    now()
)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description;

INSERT INTO iam.permissions (
    permission_id,
    permission_code,
    name,
    description,
    created_at,
    module_id,
    action_id,
    scope_id
)
VALUES
    ('00000000-0000-0000-0000-000000004201', 'VOUCHER_CREATE_ALL', 'Tạo voucher toàn sàn', 'Cho phép tạo voucher do nền tảng CoParking tài trợ.', now(), '00000000-0000-0000-0000-000000001042', '00000000-0000-0000-0000-000000002001', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004202', 'VOUCHER_READ_ALL', 'Xem voucher toàn sàn', 'Cho phép xem voucher do nền tảng CoParking quản lý.', now(), '00000000-0000-0000-0000-000000001042', '00000000-0000-0000-0000-000000002002', '00000000-0000-0000-0000-000000003001'),
    ('00000000-0000-0000-0000-000000004203', 'VOUCHER_UPDATE_ALL', 'Cập nhật voucher toàn sàn', 'Cho phép cập nhật và thay đổi trạng thái voucher do nền tảng CoParking quản lý.', now(), '00000000-0000-0000-0000-000000001042', '00000000-0000-0000-0000-000000002003', '00000000-0000-0000-0000-000000003001')
ON CONFLICT (permission_code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    module_id = EXCLUDED.module_id,
    action_id = EXCLUDED.action_id,
    scope_id = EXCLUDED.scope_id;

INSERT INTO iam.permissions (
    permission_id,
    permission_code,
    name,
    description,
    created_at,
    module_id,
    action_id,
    scope_id
)
VALUES (
    '00000000-0000-0000-0000-000000004204',
    'PARKING_TOPOLOGY_CONFIGURE_ALL',
    'Cấu hình topology bãi xe',
    'Cho phép cấu hình khu vực, cổng và làn xe trong bãi được phân công.',
    now(),
    '00000000-0000-0000-0000-000000001043',
    '00000000-0000-0000-0000-000000002003',
    '00000000-0000-0000-0000-000000003001'
)
ON CONFLICT (permission_code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    module_id = EXCLUDED.module_id,
    action_id = EXCLUDED.action_id,
    scope_id = EXCLUDED.scope_id;

-- Topology belongs to the Partner operation team. System Admin intentionally
-- does not receive this permission and remains read-only for all parking lots.
INSERT INTO iam.role_permissions (
    id,
    role_id,
    permission_id,
    created_at,
    is_active,
    is_system
)
SELECT
    gen_random_uuid(),
    role.role_id,
    permission.permission_id,
    now(),
    true,
    true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code = 'PARKING_TOPOLOGY_CONFIGURE_ALL'
WHERE role.code IN ('PARTNER_ADMIN', 'PARKING_MANAGER')
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();

-- Read access is global for System Admin.  Tenant filtering remains mandatory for
-- Partner Admin and Parking Manager; this role is the documented all-platform observer.
INSERT INTO iam.role_permissions (
    id,
    role_id,
    permission_id,
    created_at,
    is_active,
    is_system
)
SELECT
    gen_random_uuid(),
    system_admin.role_id,
    permission.permission_id,
    now(),
    true,
    true
FROM iam.roles system_admin
JOIN iam.permissions permission
    ON permission.permission_code IN (
        'ACCOUNT_READ_ALL',
        'AUDIT_LOG_READ_ALL',
        'CARD_READ_ALL',
        'CARD_TYPE_READ_ALL',
        'CUSTOMER_READ_ALL',
        'CUSTOMER_VEHICLE_READ_ALL',
        'DASHBOARD_READ_ALL',
        'DEVICE_READ_ALL',
        'EMPLOYEE_READ_ALL',
        'INVOICE_READ_ALL',
        'LOST_CARD_REPORT_READ_ALL',
        'ORGANIZATION_READ_ALL',
        'PARKING_EVENT_READ_ALL',
        'PARKING_LOT_READ_ALL',
        'PARKING_SESSION_READ_ALL',
        'PAYMENT_READ_ALL',
        'PRICE_PLAN_READ_ALL',
        'PRICE_RULE_READ_ALL',
        'REPORT_READ_ALL',
        'SHIFT_ASSIGNMENT_READ_ALL',
        'SHIFT_READ_ALL',
        'SUBSCRIPTION_READ_ALL',
        'TICKET_TYPE_READ_ALL',
        'USER_PROFILE_READ_ALL',
        'VEHICLE_TYPE_READ_ALL',
        'VOUCHER_READ_ALL'
    )
WHERE system_admin.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();

-- These are platform-level catalog and campaign settings.  They are not tied to an
-- individual Partner parking lot, so System Admin remains the owner.
INSERT INTO iam.role_permissions (
    id,
    role_id,
    permission_id,
    created_at,
    is_active,
    is_system
)
SELECT
    gen_random_uuid(),
    system_admin.role_id,
    permission.permission_id,
    now(),
    true,
    true
FROM iam.roles system_admin
JOIN iam.permissions permission
    ON permission.permission_code IN (
        'PRICE_PLAN_CREATE_ALL',
        'PRICE_PLAN_UPDATE_ALL',
        'PRICE_PLAN_DELETE_ALL',
        'PRICE_RULE_CREATE_ALL',
        'PRICE_RULE_UPDATE_ALL',
        'PRICE_RULE_DELETE_ALL',
        'VOUCHER_CREATE_ALL',
        'VOUCHER_UPDATE_ALL'
    )
WHERE system_admin.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();

-- Remove legacy commands that make System Admin an operator of a Partner parking lot.
-- Rows are retained for audit/history and can be re-enabled only through a new migration.
UPDATE iam.role_permissions role_permission
SET is_active = false,
    updated_at = now()
FROM iam.roles system_admin
JOIN iam.permissions permission ON true
WHERE role_permission.role_id = system_admin.role_id
  AND role_permission.permission_id = permission.permission_id
  AND system_admin.code = 'SYSTEM_ADMIN'
  AND permission.permission_code IN (
      'DEVICE_CREATE_ALL',
      'DEVICE_DELETE_ALL',
      'DEVICE_STATUS_UPDATE_ALL',
      'DEVICE_UPDATE_ALL',
      'LOST_CARD_REPORT_CREATE_ALL',
      'LOST_CARD_REPORT_UPDATE_ALL',
      'PARKING_SESSION_CHECK_IN_ALL',
      'PARKING_SESSION_CHECK_OUT_ALL',
      'PARKING_SESSION_CREATE_ALL',
      'PARKING_SESSION_DELETE_ALL',
      'PARKING_SESSION_UPDATE_ALL',
      'PARKING_LOT_CREATE_ALL',
      'PARKING_LOT_UPDATE_ALL',
      'SHIFT_ASSIGNMENT_CREATE_ALL',
      'SHIFT_ASSIGNMENT_DELETE_ALL',
      'SHIFT_ASSIGNMENT_UPDATE_ALL',
      'SHIFT_CREATE_ALL',
      'SHIFT_DELETE_ALL',
      'SHIFT_OPEN_OWN',
      'SHIFT_UPDATE_ALL',
      'SUBSCRIPTION_APPROVE_ALL',
      'SUBSCRIPTION_ASSIGN_CARD_ALL',
      'SUBSCRIPTION_CANCEL_ALL',
      'SUBSCRIPTION_CREATE_ALL',
      'SUBSCRIPTION_DELETE_ALL',
      'SUBSCRIPTION_EXPIRE_ALL',
      'SUBSCRIPTION_REJECT_ALL',
      'SUBSCRIPTION_UPDATE_ALL'
  );
