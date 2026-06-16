BEGIN;

INSERT INTO iam.permission_modules (code, name, description)
VALUES
    ('SUBSCRIPTION', 'Đăng ký vé', 'Quản lý yêu cầu đăng ký vé tháng/quý/năm/free.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_actions (code, name, description)
VALUES
    ('APPROVE', 'Duyệt', 'Cho phép duyệt yêu cầu.'),
    ('REJECT', 'Từ chối', 'Cho phép từ chối yêu cầu.'),
    ('ASSIGN_CARD', 'Cấp thẻ', 'Cho phép cấp thẻ cho đăng ký.'),
    ('EXPIRE', 'Hết hạn', 'Cho phép chuyển trạng thái hết hạn.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (permission_code, module_id, action_id, scope_id, name, description)
SELECT v.permission_code, m.module_id, a.action_id, s.scope_id, v.name, v.description
FROM (
         VALUES
             ('SUBSCRIPTION_CREATE_OWN', 'SUBSCRIPTION', 'CREATE', 'OWN', 'Tạo đăng ký vé của mình', 'Cho phép khách hàng tạo yêu cầu đăng ký vé cho chính mình.'),
             ('SUBSCRIPTION_READ_OWN', 'SUBSCRIPTION', 'READ', 'OWN', 'Xem đăng ký vé của mình', 'Cho phép khách hàng xem đăng ký vé của chính mình.'),
             ('SUBSCRIPTION_UPDATE_OWN', 'SUBSCRIPTION', 'UPDATE', 'OWN', 'Cập nhật đăng ký vé của mình', 'Cho phép khách hàng cập nhật yêu cầu đăng ký khi còn chờ duyệt.'),
             ('SUBSCRIPTION_CANCEL_OWN', 'SUBSCRIPTION', 'CANCEL', 'OWN', 'Hủy đăng ký vé của mình', 'Cho phép khách hàng hủy yêu cầu đăng ký khi còn hợp lệ.'),

             ('SUBSCRIPTION_CREATE_ALL', 'SUBSCRIPTION', 'CREATE', 'ALL', 'Tạo đăng ký vé cho khách', 'Cho phép nhân viên/quản lý tạo yêu cầu đăng ký vé cho khách hàng.'),
             ('SUBSCRIPTION_READ_ALL', 'SUBSCRIPTION', 'READ', 'ALL', 'Xem tất cả đăng ký vé', 'Cho phép xem toàn bộ yêu cầu đăng ký vé.'),
             ('SUBSCRIPTION_UPDATE_ALL', 'SUBSCRIPTION', 'UPDATE', 'ALL', 'Cập nhật đăng ký vé', 'Cho phép cập nhật yêu cầu đăng ký khi còn chờ duyệt.'),
             ('SUBSCRIPTION_CANCEL_ALL', 'SUBSCRIPTION', 'CANCEL', 'ALL', 'Hủy đăng ký vé', 'Cho phép hủy yêu cầu đăng ký khi còn hợp lệ.'),
             ('SUBSCRIPTION_APPROVE_ALL', 'SUBSCRIPTION', 'APPROVE', 'ALL', 'Duyệt đăng ký vé', 'Cho phép duyệt yêu cầu đăng ký vé.'),
             ('SUBSCRIPTION_REJECT_ALL', 'SUBSCRIPTION', 'REJECT', 'ALL', 'Từ chối đăng ký vé', 'Cho phép từ chối yêu cầu đăng ký vé.'),
             ('SUBSCRIPTION_ASSIGN_CARD_ALL', 'SUBSCRIPTION', 'ASSIGN_CARD', 'ALL', 'Cấp thẻ đăng ký vé', 'Cho phép cấp thẻ cho đăng ký đã thanh toán.'),
             ('SUBSCRIPTION_EXPIRE_ALL', 'SUBSCRIPTION', 'EXPIRE', 'ALL', 'Đánh dấu hết hạn', 'Cho phép chuyển đăng ký sang hết hạn.')
     ) AS v(permission_code, module_code, action_code, scope_code, name, description)
         JOIN iam.permission_modules m ON m.code = v.module_code
         JOIN iam.permission_actions a ON a.code = v.action_code
         JOIN iam.permission_scopes s ON s.code = v.scope_code
ON CONFLICT (permission_code) DO NOTHING;

-- CUSTOMER: tự tạo, xem, sửa, hủy đăng ký của mình
INSERT INTO iam.role_permissions (id, role_id, permission_id, is_active, is_system)
SELECT gen_random_uuid(), r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code IN (
                                                         'SUBSCRIPTION_CREATE_OWN',
                                                         'SUBSCRIPTION_READ_OWN',
                                                         'SUBSCRIPTION_UPDATE_OWN',
                                                         'SUBSCRIPTION_CANCEL_OWN'
    )
WHERE r.code = 'CUSTOMER'
  AND NOT EXISTS (
    SELECT 1
    FROM iam.role_permissions rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

-- PARKING_MANAGER: quản lý toàn bộ workflow subscription
INSERT INTO iam.role_permissions (id, role_id, permission_id, is_active, is_system)
SELECT gen_random_uuid(), r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code IN (
                                                         'SUBSCRIPTION_CREATE_ALL',
                                                         'SUBSCRIPTION_READ_ALL',
                                                         'SUBSCRIPTION_UPDATE_ALL',
                                                         'SUBSCRIPTION_CANCEL_ALL',
                                                         'SUBSCRIPTION_APPROVE_ALL',
                                                         'SUBSCRIPTION_REJECT_ALL',
                                                         'SUBSCRIPTION_ASSIGN_CARD_ALL',
                                                         'SUBSCRIPTION_EXPIRE_ALL'
    )
WHERE r.code = 'PARKING_MANAGER'
  AND NOT EXISTS (
    SELECT 1
    FROM iam.role_permissions rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

-- SYSTEM_ADMIN: gán toàn quyền để test nhanh nếu cần
INSERT INTO iam.role_permissions (id, role_id, permission_id, is_active, is_system)
SELECT gen_random_uuid(), r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code LIKE 'SUBSCRIPTION_%'
WHERE r.code = 'SYSTEM_ADMIN'
  AND NOT EXISTS (
    SELECT 1
    FROM iam.role_permissions rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);

COMMIT;


-- INVOICE - PAYMENT
BEGIN;

-- 1. Đảm bảo module/action/scope cần thiết đã có
INSERT INTO iam.permission_modules (code, name, description)
VALUES
    ('PAYMENT', 'Thanh toán', 'Quản lý giao dịch thanh toán.'),
    ('INVOICE', 'Hóa đơn', 'Quản lý hóa đơn.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_actions (code, name, description)
VALUES
    ('CREATE', 'Tạo', 'Cho phép tạo dữ liệu.'),
    ('READ', 'Xem', 'Cho phép xem dữ liệu.'),
    ('CANCEL', 'Hủy', 'Cho phép hủy dữ liệu.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_scopes (code, name, description)
VALUES
    ('ALL', 'Tất cả', 'Áp dụng trên toàn bộ dữ liệu.')
ON CONFLICT (code) DO NOTHING;

-- 2. Thêm permission cho payment
INSERT INTO iam.permissions (permission_code, module_id, action_id, scope_id, name, description)
SELECT v.permission_code, m.module_id, a.action_id, s.scope_id, v.name, v.description
FROM (
         VALUES
             (
                 'PAYMENT_CREATE_ALL',
                 'PAYMENT',
                 'CREATE',
                 'ALL',
                 'Ghi nhận thanh toán',
                 'Cho phép nhân viên hoặc quản lý ghi nhận payment cho hóa đơn.'
             ),
             (
                 'PAYMENT_READ_ALL',
                 'PAYMENT',
                 'READ',
                 'ALL',
                 'Xem thanh toán',
                 'Cho phép xem danh sách giao dịch thanh toán.'
             )
     ) AS v(permission_code, module_code, action_code, scope_code, name, description)
         JOIN iam.permission_modules m ON m.code = v.module_code
         JOIN iam.permission_actions a ON a.code = v.action_code
         JOIN iam.permission_scopes s ON s.code = v.scope_code
ON CONFLICT (permission_code) DO NOTHING;

-- 3. Nếu bạn chưa thêm quyền invoice trước đó thì thêm luôn để test trọn luồng
INSERT INTO iam.permissions (permission_code, module_id, action_id, scope_id, name, description)
SELECT v.permission_code, m.module_id, a.action_id, s.scope_id, v.name, v.description
FROM (
         VALUES
             (
                 'INVOICE_CREATE_ALL',
                 'INVOICE',
                 'CREATE',
                 'ALL',
                 'Tạo hóa đơn',
                 'Cho phép tạo hóa đơn thủ công.'
             ),
             (
                 'INVOICE_READ_ALL',
                 'INVOICE',
                 'READ',
                 'ALL',
                 'Xem hóa đơn',
                 'Cho phép xem danh sách và chi tiết hóa đơn.'
             ),
             (
                 'INVOICE_CANCEL_ALL',
                 'INVOICE',
                 'CANCEL',
                 'ALL',
                 'Hủy hóa đơn',
                 'Cho phép hủy hóa đơn chưa thanh toán.'
             )
     ) AS v(permission_code, module_code, action_code, scope_code, name, description)
         JOIN iam.permission_modules m ON m.code = v.module_code
         JOIN iam.permission_actions a ON a.code = v.action_code
         JOIN iam.permission_scopes s ON s.code = v.scope_code
ON CONFLICT (permission_code) DO NOTHING;

-- 4. Gán quyền payment cho EMPLOYEE và PARKING_MANAGER
INSERT INTO iam.role_permissions (id, role_id, permission_id, is_active, is_system)
SELECT gen_random_uuid(), r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code IN (
                                                         'PAYMENT_CREATE_ALL',
                                                         'PAYMENT_READ_ALL'
    )
WHERE r.code IN ('EMPLOYEE', 'PARKING_MANAGER')
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET
                  is_active = TRUE,
                  is_system = TRUE,
                  updated_at = now();

-- 5. Gán quyền invoice cho PARKING_MANAGER để tạo/xem/cancel invoice khi test
INSERT INTO iam.role_permissions (id, role_id, permission_id, is_active, is_system)
SELECT gen_random_uuid(), r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code IN (
                                                         'INVOICE_CREATE_ALL',
                                                         'INVOICE_READ_ALL',
                                                         'INVOICE_CANCEL_ALL'
    )
WHERE r.code = 'PARKING_MANAGER'
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET
                  is_active = TRUE,
                  is_system = TRUE,
                  updated_at = now();

COMMIT;