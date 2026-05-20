BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE iam.roles
SET code = 'SYSTEM_ADMIN',
    name = 'Quản trị hệ thống',
    description = 'Quản lý tài khoản quản trị, vai trò, quyền, nhật ký hệ thống và bảo mật.'
WHERE code = 'ADMIN';

INSERT INTO iam.roles (code, name, description, is_system)
VALUES
    ('PARKING_MANAGER', 'Quản lý bãi xe', 'Quản lý nghiệp vụ vận hành bãi xe, nhân viên, ca trực, bảng giá và báo cáo.', TRUE)
ON CONFLICT (code) DO NOTHING;

ALTER TABLE iam.role_permissions
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS is_system BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS iam.permission_modules (
                                                      module_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                      code VARCHAR(50) NOT NULL UNIQUE,
                                                      name VARCHAR(150) NOT NULL,
                                                      description TEXT,
                                                      created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                      created_by UUID,
                                                      updated_at TIMESTAMPTZ,
                                                      updated_by UUID,
                                                      CONSTRAINT ck_permission_modules_code_upper CHECK (code = upper(code))
);

CREATE TABLE IF NOT EXISTS iam.permission_actions (
                                                      action_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                      code VARCHAR(50) NOT NULL UNIQUE,
                                                      name VARCHAR(150) NOT NULL,
                                                      description TEXT,
                                                      created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                      created_by UUID,
                                                      updated_at TIMESTAMPTZ,
                                                      updated_by UUID,
                                                      CONSTRAINT ck_permission_actions_code_upper CHECK (code = upper(code))
);

CREATE TABLE IF NOT EXISTS iam.permission_scopes (
                                                     scope_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                     code VARCHAR(50) NOT NULL UNIQUE,
                                                     name VARCHAR(150) NOT NULL,
                                                     description TEXT,
                                                     created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                     created_by UUID,
                                                     updated_at TIMESTAMPTZ,
                                                     updated_by UUID,
                                                     CONSTRAINT ck_permission_scopes_code_upper CHECK (code = upper(code))
);

INSERT INTO iam.permission_actions (action_id, code, name, description)
VALUES
    ('00000000-0000-0000-0000-000000002001', 'CREATE', 'Tạo mới', 'Cho phép tạo bản ghi mới trong module được cấp quyền.'),
    ('00000000-0000-0000-0000-000000002002', 'READ', 'Xem', 'Cho phép xem hoặc tra cứu dữ liệu trong module được cấp quyền.'),
    ('00000000-0000-0000-0000-000000002003', 'UPDATE', 'Cập nhật', 'Cho phép chỉnh sửa dữ liệu đã tồn tại trong module được cấp quyền.'),
    ('00000000-0000-0000-0000-000000002004', 'DELETE', 'Xóa', 'Cho phép xóa dữ liệu trong module được cấp quyền.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_scopes (scope_id, code, name, description)
VALUES
    ('00000000-0000-0000-0000-000000003001', 'ALL', 'Toàn bộ', 'Áp dụng cho toàn bộ dữ liệu trong module được cấp quyền.'),
    ('00000000-0000-0000-0000-000000003002', 'OWN', 'Cá nhân', 'Chỉ áp dụng cho dữ liệu thuộc về người dùng hiện tại.'),
    ('00000000-0000-0000-0000-000000003003', 'PUBLIC', 'Công khai', 'Áp dụng cho dữ liệu công khai hoặc chức năng không yêu cầu đăng nhập.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_modules (module_id, code, name, description)
VALUES
    ('00000000-0000-0000-0000-000000001001', 'ACCOUNT', 'Tài khoản', 'Quản lý tài khoản đăng nhập của người dùng trong hệ thống.'),
    ('00000000-0000-0000-0000-000000001002', 'ROLE', 'Vai trò', 'Quản lý các vai trò dùng để phân quyền cho tài khoản.'),
    ('00000000-0000-0000-0000-000000001003', 'PERMISSION', 'Quyền', 'Quản lý danh mục quyền và cấu hình phân quyền theo vai trò.'),
    ('00000000-0000-0000-0000-000000001004', 'AUDIT_LOG', 'Nhật ký hệ thống', 'Lưu và tra cứu lịch sử thao tác quan trọng trong hệ thống.'),
    ('00000000-0000-0000-0000-000000001005', 'LOGIN_ATTEMPT', 'Lịch sử đăng nhập', 'Theo dõi các lần đăng nhập thành công hoặc thất bại.'),
    ('00000000-0000-0000-0000-000000001006', 'USER_PROFILE', 'Hồ sơ người dùng', 'Quản lý thông tin cá nhân dùng chung cho tài khoản, khách hàng và nhân viên.'),
    ('00000000-0000-0000-0000-000000001007', 'CUSTOMER', 'Khách hàng', 'Quản lý thông tin khách hàng sử dụng dịch vụ bãi xe.'),
    ('00000000-0000-0000-0000-000000001008', 'EMPLOYEE', 'Nhân viên', 'Quản lý thông tin nhân viên vận hành bãi xe.'),
    ('00000000-0000-0000-0000-000000001009', 'CUSTOMER_VEHICLE', 'Xe khách hàng', 'Quản lý danh sách phương tiện thuộc về khách hàng.'),
    ('00000000-0000-0000-0000-000000001010', 'VEHICLE_TYPE', 'Loại xe', 'Quản lý danh mục loại xe như xe máy, ô tô, xe đạp.'),
    ('00000000-0000-0000-0000-000000001011', 'TICKET_TYPE', 'Loại vé', 'Quản lý danh mục loại vé như vé lượt, vé tháng hoặc vé VIP.'),
    ('00000000-0000-0000-0000-000000001012', 'CARD_TYPE', 'Loại thẻ', 'Quản lý danh mục loại thẻ vật lý dùng trong bãi xe.'),
    ('00000000-0000-0000-0000-000000001013', 'PRICE_PLAN', 'Bảng giá', 'Quản lý các bảng giá áp dụng cho khách vãng lai và khách đăng ký.'),
    ('00000000-0000-0000-0000-000000001014', 'PRICE_RULE', 'Quy tắc giá', 'Quản lý quy tắc tính giá theo loại xe, loại vé và thời gian.'),
    ('00000000-0000-0000-0000-000000001015', 'CARD', 'Thẻ gửi xe', 'Quản lý thẻ RFID/NFC dùng để nhận diện lượt gửi xe.'),
    ('00000000-0000-0000-0000-000000001016', 'SUBSCRIPTION', 'Vé tháng', 'Quản lý đăng ký, gia hạn và duyệt vé tháng của khách hàng.'),
    ('00000000-0000-0000-0000-000000001017', 'LOST_CARD_REPORT', 'Báo mất thẻ', 'Ghi nhận và xử lý các trường hợp khách hàng bị mất thẻ.'),
    ('00000000-0000-0000-0000-000000001018', 'PARKING_LOT', 'Bãi xe', 'Quản lý thông tin bãi xe, sức chứa và trạng thái hoạt động.'),
    ('00000000-0000-0000-0000-000000001019', 'ZONE', 'Khu vực', 'Quản lý các khu vực bên trong bãi xe.'),
    ('00000000-0000-0000-0000-000000001020', 'PARKING_SPACE', 'Ô đỗ', 'Quản lý từng ô hoặc vị trí đỗ xe trong bãi.'),
    ('00000000-0000-0000-0000-000000001021', 'LANE', 'Làn xe', 'Quản lý làn xe vào, làn xe ra và trạng thái của từng làn.'),
    ('00000000-0000-0000-0000-000000001022', 'PARKING_SESSION', 'Phiên gửi xe', 'Quản lý phiên gửi xe từ lúc check-in đến lúc check-out.'),
    ('00000000-0000-0000-0000-000000001023', 'PARKING_EVENT', 'Sự kiện gửi xe', 'Lưu các sự kiện vào ra, quét thẻ, nhận diện biển số và mở barrier.'),
    ('00000000-0000-0000-0000-000000001024', 'INVOICE', 'Hóa đơn', 'Quản lý hóa đơn phát sinh từ vé tháng, vé lượt và phí mất thẻ.'),
    ('00000000-0000-0000-0000-000000001025', 'PAYMENT', 'Thanh toán', 'Quản lý giao dịch thanh toán bằng tiền mặt hoặc kênh thanh toán điện tử.'),
    ('00000000-0000-0000-0000-000000001026', 'REPORT', 'Báo cáo', 'Cung cấp báo cáo doanh thu, lưu lượng xe và tình trạng vận hành.'),
    ('00000000-0000-0000-0000-000000001027', 'SHIFT', 'Ca trực', 'Quản lý ca trực làm việc tại bãi xe.'),
    ('00000000-0000-0000-0000-000000001028', 'SHIFT_ASSIGNMENT', 'Phân công ca trực', 'Quản lý việc phân công nhân viên vào từng ca trực.'),
    ('00000000-0000-0000-0000-000000001029', 'SUPPORT_TICKET', 'Ticket hỗ trợ', 'Quản lý yêu cầu hỗ trợ do khách hàng gửi lên hệ thống.'),
    ('00000000-0000-0000-0000-000000001030', 'DEVICE', 'Thiết bị', 'Quản lý camera, kiosk, đầu đọc thẻ và barrier trong bãi xe.'),
    ('00000000-0000-0000-0000-000000001031', 'NOTIFICATION', 'Thông báo', 'Quản lý thông báo gửi đến tài khoản người dùng.'),
    ('00000000-0000-0000-0000-000000001032', 'PUBLIC_INFO', 'Thông tin công khai', 'Cung cấp thông tin công khai như bảng giá, nội quy và tình trạng còn chỗ.')
ON CONFLICT (code) DO NOTHING;

ALTER TABLE iam.permissions
    ADD COLUMN IF NOT EXISTS module_id UUID,
    ADD COLUMN IF NOT EXISTS action_id UUID,
    ADD COLUMN IF NOT EXISTS scope_id UUID;

UPDATE iam.permissions p
SET module_id = m.module_id,
    action_id = a.action_id,
    scope_id = s.scope_id,
    permission_code =
        CASE p.permission_code
            WHEN 'VIEW_DASHBOARD' THEN 'REPORT_READ_ALL'
            WHEN 'MANAGE_ACCOUNT' THEN 'ACCOUNT_UPDATE_ALL'
            WHEN 'MANAGE_ROLE' THEN 'ROLE_UPDATE_ALL'
            WHEN 'MANAGE_PRICE' THEN 'PRICE_PLAN_UPDATE_ALL'
            WHEN 'MANAGE_CARD' THEN 'CARD_UPDATE_ALL'
            WHEN 'OPERATE_PARKING_GATE' THEN 'PARKING_SESSION_UPDATE_ALL'
            WHEN 'PROCESS_PAYMENT' THEN 'PAYMENT_UPDATE_ALL'
            WHEN 'APPROVE_SUBSCRIPTION' THEN 'SUBSCRIPTION_UPDATE_ALL'
            WHEN 'VIEW_OWN_PROFILE' THEN 'USER_PROFILE_READ_OWN'
            WHEN 'VIEW_OWN_PARKING_HISTORY' THEN 'PARKING_SESSION_READ_OWN'
            WHEN 'SEND_SUPPORT_TICKET' THEN 'SUPPORT_TICKET_CREATE_OWN'
            WHEN 'VIEW_PUBLIC_PRICE' THEN 'PUBLIC_INFO_READ_PUBLIC'
            ELSE p.permission_code
            END,
    name =
        CASE p.permission_code
            WHEN 'VIEW_DASHBOARD' THEN 'Xem báo cáo'
            WHEN 'MANAGE_ACCOUNT' THEN 'Cập nhật tài khoản'
            WHEN 'MANAGE_ROLE' THEN 'Cập nhật vai trò'
            WHEN 'MANAGE_PRICE' THEN 'Cập nhật bảng giá'
            WHEN 'MANAGE_CARD' THEN 'Cập nhật thẻ gửi xe'
            WHEN 'OPERATE_PARKING_GATE' THEN 'Cập nhật phiên gửi xe'
            WHEN 'PROCESS_PAYMENT' THEN 'Cập nhật thanh toán'
            WHEN 'APPROVE_SUBSCRIPTION' THEN 'Cập nhật vé tháng'
            WHEN 'VIEW_OWN_PROFILE' THEN 'Xem hồ sơ cá nhân'
            WHEN 'VIEW_OWN_PARKING_HISTORY' THEN 'Xem lịch sử gửi xe'
            WHEN 'SEND_SUPPORT_TICKET' THEN 'Gửi ticket hỗ trợ'
            WHEN 'VIEW_PUBLIC_PRICE' THEN 'Xem thông tin công khai'
            ELSE p.name
            END
FROM iam.permission_modules m,
     iam.permission_actions a,
     iam.permission_scopes s
WHERE m.code =
      CASE p.permission_code
          WHEN 'VIEW_DASHBOARD' THEN 'REPORT'
          WHEN 'MANAGE_ACCOUNT' THEN 'ACCOUNT'
          WHEN 'MANAGE_ROLE' THEN 'ROLE'
          WHEN 'MANAGE_PRICE' THEN 'PRICE_PLAN'
          WHEN 'MANAGE_CARD' THEN 'CARD'
          WHEN 'OPERATE_PARKING_GATE' THEN 'PARKING_SESSION'
          WHEN 'PROCESS_PAYMENT' THEN 'PAYMENT'
          WHEN 'APPROVE_SUBSCRIPTION' THEN 'SUBSCRIPTION'
          WHEN 'VIEW_OWN_PROFILE' THEN 'USER_PROFILE'
          WHEN 'VIEW_OWN_PARKING_HISTORY' THEN 'PARKING_SESSION'
          WHEN 'SEND_SUPPORT_TICKET' THEN 'SUPPORT_TICKET'
          WHEN 'VIEW_PUBLIC_PRICE' THEN 'PUBLIC_INFO'
          ELSE upper(p.module)
          END
  AND a.code =
      CASE
          WHEN p.permission_code IN ('VIEW_DASHBOARD', 'VIEW_OWN_PROFILE', 'VIEW_OWN_PARKING_HISTORY', 'VIEW_PUBLIC_PRICE') THEN 'READ'
          WHEN p.permission_code = 'SEND_SUPPORT_TICKET' THEN 'CREATE'
          ELSE 'UPDATE'
          END
  AND s.code =
      CASE
          WHEN p.permission_code IN ('VIEW_OWN_PROFILE', 'VIEW_OWN_PARKING_HISTORY', 'SEND_SUPPORT_TICKET') THEN 'OWN'
          WHEN p.permission_code = 'VIEW_PUBLIC_PRICE' THEN 'PUBLIC'
          ELSE 'ALL'
          END;

ALTER TABLE iam.permissions
    ALTER COLUMN module_id SET NOT NULL,
    ALTER COLUMN action_id SET NOT NULL,
    ALTER COLUMN scope_id SET NOT NULL;

DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_permissions_module') THEN
            ALTER TABLE iam.permissions
                ADD CONSTRAINT fk_permissions_module
                    FOREIGN KEY (module_id) REFERENCES iam.permission_modules(module_id)
                        ON DELETE RESTRICT;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_permissions_action') THEN
            ALTER TABLE iam.permissions
                ADD CONSTRAINT fk_permissions_action
                    FOREIGN KEY (action_id) REFERENCES iam.permission_actions(action_id)
                        ON DELETE RESTRICT;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_permissions_scope') THEN
            ALTER TABLE iam.permissions
                ADD CONSTRAINT fk_permissions_scope
                    FOREIGN KEY (scope_id) REFERENCES iam.permission_scopes(scope_id)
                        ON DELETE RESTRICT;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_permissions_module_action_scope') THEN
            ALTER TABLE iam.permissions
                ADD CONSTRAINT uq_permissions_module_action_scope
                    UNIQUE (module_id, action_id, scope_id);
        END IF;
    END $$;

ALTER TABLE iam.permissions
    DROP COLUMN IF EXISTS module,
    DROP COLUMN IF EXISTS action;

INSERT INTO iam.permissions (permission_code, module_id, action_id, scope_id, name, description)
SELECT v.permission_code, m.module_id, a.action_id, s.scope_id, v.name, v.description
FROM (
         VALUES
             ('ACCOUNT_CREATE_ALL', 'ACCOUNT', 'CREATE', 'ALL', 'Tạo tài khoản', 'Cho phép tạo tài khoản người dùng trong hệ thống.'),
             ('ACCOUNT_READ_ALL', 'ACCOUNT', 'READ', 'ALL', 'Xem tài khoản', 'Cho phép xem danh sách và chi tiết tài khoản.'),
             ('ACCOUNT_UPDATE_ALL', 'ACCOUNT', 'UPDATE', 'ALL', 'Cập nhật tài khoản', 'Cho phép cập nhật, khóa hoặc mở khóa tài khoản.'),
             ('ACCOUNT_DELETE_ALL', 'ACCOUNT', 'DELETE', 'ALL', 'Xóa tài khoản', 'Cho phép xóa tài khoản khỏi hệ thống.'),

             ('ROLE_CREATE_ALL', 'ROLE', 'CREATE', 'ALL', 'Tạo vai trò', 'Cho phép tạo vai trò mới.'),
             ('ROLE_READ_ALL', 'ROLE', 'READ', 'ALL', 'Xem vai trò', 'Cho phép xem danh sách và chi tiết vai trò.'),
             ('ROLE_UPDATE_ALL', 'ROLE', 'UPDATE', 'ALL', 'Cập nhật vai trò', 'Cho phép cập nhật thông tin vai trò.'),
             ('ROLE_DELETE_ALL', 'ROLE', 'DELETE', 'ALL', 'Xóa vai trò', 'Cho phép xóa vai trò khỏi hệ thống.'),

             ('PERMISSION_CREATE_ALL', 'PERMISSION', 'CREATE', 'ALL', 'Tạo quyền', 'Cho phép tạo quyền mới.'),
             ('PERMISSION_READ_ALL', 'PERMISSION', 'READ', 'ALL', 'Xem quyền', 'Cho phép xem danh sách và chi tiết quyền.'),
             ('PERMISSION_UPDATE_ALL', 'PERMISSION', 'UPDATE', 'ALL', 'Cập nhật quyền', 'Cho phép cập nhật quyền và cấu hình phân quyền.'),
             ('PERMISSION_DELETE_ALL', 'PERMISSION', 'DELETE', 'ALL', 'Xóa quyền', 'Cho phép xóa quyền khỏi hệ thống.'),

             ('AUDIT_LOG_READ_ALL', 'AUDIT_LOG', 'READ', 'ALL', 'Xem nhật ký hệ thống', 'Cho phép xem lịch sử thao tác quan trọng trong hệ thống.'),
             ('LOGIN_ATTEMPT_READ_ALL', 'LOGIN_ATTEMPT', 'READ', 'ALL', 'Xem lịch sử đăng nhập', 'Cho phép xem lịch sử đăng nhập thành công hoặc thất bại.'),

             ('EMPLOYEE_CREATE_ALL', 'EMPLOYEE', 'CREATE', 'ALL', 'Tạo nhân viên', 'Cho phép tạo hồ sơ nhân viên.'),
             ('EMPLOYEE_READ_ALL', 'EMPLOYEE', 'READ', 'ALL', 'Xem nhân viên', 'Cho phép xem danh sách và chi tiết nhân viên.'),
             ('EMPLOYEE_UPDATE_ALL', 'EMPLOYEE', 'UPDATE', 'ALL', 'Cập nhật nhân viên', 'Cho phép cập nhật hồ sơ nhân viên.'),
             ('EMPLOYEE_DELETE_ALL', 'EMPLOYEE', 'DELETE', 'ALL', 'Xóa nhân viên', 'Cho phép xóa hồ sơ nhân viên.'),
             ('EMPLOYEE_READ_OWN', 'EMPLOYEE', 'READ', 'OWN', 'Xem hồ sơ nhân viên của mình', 'Cho phép nhân viên xem hồ sơ của chính mình.'),

             ('CUSTOMER_CREATE_ALL', 'CUSTOMER', 'CREATE', 'ALL', 'Tạo khách hàng', 'Cho phép tạo hồ sơ khách hàng.'),
             ('CUSTOMER_READ_ALL', 'CUSTOMER', 'READ', 'ALL', 'Xem khách hàng', 'Cho phép xem danh sách và chi tiết khách hàng.'),
             ('CUSTOMER_UPDATE_ALL', 'CUSTOMER', 'UPDATE', 'ALL', 'Cập nhật khách hàng', 'Cho phép cập nhật hồ sơ khách hàng.'),
             ('CUSTOMER_DELETE_ALL', 'CUSTOMER', 'DELETE', 'ALL', 'Xóa khách hàng', 'Cho phép xóa hồ sơ khách hàng.'),

             ('USER_PROFILE_CREATE_ALL', 'USER_PROFILE', 'CREATE', 'ALL', 'Tạo hồ sơ người dùng', 'Cho phép tạo hồ sơ người dùng.'),
             ('USER_PROFILE_READ_ALL', 'USER_PROFILE', 'READ', 'ALL', 'Xem hồ sơ người dùng', 'Cho phép xem hồ sơ người dùng.'),
             ('USER_PROFILE_UPDATE_ALL', 'USER_PROFILE', 'UPDATE', 'ALL', 'Cập nhật hồ sơ người dùng', 'Cho phép cập nhật hồ sơ người dùng.'),
             ('USER_PROFILE_DELETE_ALL', 'USER_PROFILE', 'DELETE', 'ALL', 'Xóa hồ sơ người dùng', 'Cho phép xóa hồ sơ người dùng.'),
             ('USER_PROFILE_READ_OWN', 'USER_PROFILE', 'READ', 'OWN', 'Xem hồ sơ cá nhân', 'Cho phép xem hồ sơ cá nhân của chính mình.'),
             ('USER_PROFILE_UPDATE_OWN', 'USER_PROFILE', 'UPDATE', 'OWN', 'Cập nhật hồ sơ cá nhân', 'Cho phép cập nhật hồ sơ cá nhân của chính mình.'),

             ('CUSTOMER_VEHICLE_CREATE_ALL', 'CUSTOMER_VEHICLE', 'CREATE', 'ALL', 'Tạo xe khách hàng', 'Cho phép tạo phương tiện cho khách hàng.'),
             ('CUSTOMER_VEHICLE_READ_ALL', 'CUSTOMER_VEHICLE', 'READ', 'ALL', 'Xem xe khách hàng', 'Cho phép xem phương tiện của khách hàng.'),
             ('CUSTOMER_VEHICLE_UPDATE_ALL', 'CUSTOMER_VEHICLE', 'UPDATE', 'ALL', 'Cập nhật xe khách hàng', 'Cho phép cập nhật phương tiện của khách hàng.'),
             ('CUSTOMER_VEHICLE_DELETE_ALL', 'CUSTOMER_VEHICLE', 'DELETE', 'ALL', 'Xóa xe khách hàng', 'Cho phép xóa phương tiện của khách hàng.'),
             ('CUSTOMER_VEHICLE_CREATE_OWN', 'CUSTOMER_VEHICLE', 'CREATE', 'OWN', 'Đăng ký xe cá nhân', 'Cho phép khách hàng đăng ký xe của chính mình.'),
             ('CUSTOMER_VEHICLE_READ_OWN', 'CUSTOMER_VEHICLE', 'READ', 'OWN', 'Xem xe cá nhân', 'Cho phép khách hàng xem xe của chính mình.'),
             ('CUSTOMER_VEHICLE_UPDATE_OWN', 'CUSTOMER_VEHICLE', 'UPDATE', 'OWN', 'Cập nhật xe cá nhân', 'Cho phép khách hàng cập nhật xe của chính mình.'),
             ('CUSTOMER_VEHICLE_DELETE_OWN', 'CUSTOMER_VEHICLE', 'DELETE', 'OWN', 'Xóa xe cá nhân', 'Cho phép khách hàng xóa xe của chính mình.'),

             ('PRICE_PLAN_CREATE_ALL', 'PRICE_PLAN', 'CREATE', 'ALL', 'Tạo bảng giá', 'Cho phép tạo bảng giá.'),
             ('PRICE_PLAN_READ_ALL', 'PRICE_PLAN', 'READ', 'ALL', 'Xem bảng giá', 'Cho phép xem bảng giá.'),
             ('PRICE_PLAN_UPDATE_ALL', 'PRICE_PLAN', 'UPDATE', 'ALL', 'Cập nhật bảng giá', 'Cho phép cập nhật bảng giá.'),
             ('PRICE_PLAN_DELETE_ALL', 'PRICE_PLAN', 'DELETE', 'ALL', 'Xóa bảng giá', 'Cho phép xóa bảng giá.'),

             ('PRICE_RULE_CREATE_ALL', 'PRICE_RULE', 'CREATE', 'ALL', 'Tạo quy tắc giá', 'Cho phép tạo quy tắc giá.'),
             ('PRICE_RULE_READ_ALL', 'PRICE_RULE', 'READ', 'ALL', 'Xem quy tắc giá', 'Cho phép xem quy tắc giá.'),
             ('PRICE_RULE_UPDATE_ALL', 'PRICE_RULE', 'UPDATE', 'ALL', 'Cập nhật quy tắc giá', 'Cho phép cập nhật quy tắc giá.'),
             ('PRICE_RULE_DELETE_ALL', 'PRICE_RULE', 'DELETE', 'ALL', 'Xóa quy tắc giá', 'Cho phép xóa quy tắc giá.'),

             ('VEHICLE_TYPE_CREATE_ALL', 'VEHICLE_TYPE', 'CREATE', 'ALL', 'Tạo loại xe', 'Cho phép tạo loại xe.'),
             ('VEHICLE_TYPE_READ_ALL', 'VEHICLE_TYPE', 'READ', 'ALL', 'Xem loại xe', 'Cho phép xem loại xe.'),
             ('VEHICLE_TYPE_UPDATE_ALL', 'VEHICLE_TYPE', 'UPDATE', 'ALL', 'Cập nhật loại xe', 'Cho phép cập nhật loại xe.'),
             ('VEHICLE_TYPE_DELETE_ALL', 'VEHICLE_TYPE', 'DELETE', 'ALL', 'Xóa loại xe', 'Cho phép xóa loại xe.'),

             ('TICKET_TYPE_CREATE_ALL', 'TICKET_TYPE', 'CREATE', 'ALL', 'Tạo loại vé', 'Cho phép tạo loại vé.'),
             ('TICKET_TYPE_READ_ALL', 'TICKET_TYPE', 'READ', 'ALL', 'Xem loại vé', 'Cho phép xem loại vé.'),
             ('TICKET_TYPE_UPDATE_ALL', 'TICKET_TYPE', 'UPDATE', 'ALL', 'Cập nhật loại vé', 'Cho phép cập nhật loại vé.'),
             ('TICKET_TYPE_DELETE_ALL', 'TICKET_TYPE', 'DELETE', 'ALL', 'Xóa loại vé', 'Cho phép xóa loại vé.'),

             ('CARD_TYPE_CREATE_ALL', 'CARD_TYPE', 'CREATE', 'ALL', 'Tạo loại thẻ', 'Cho phép tạo loại thẻ.'),
             ('CARD_TYPE_READ_ALL', 'CARD_TYPE', 'READ', 'ALL', 'Xem loại thẻ', 'Cho phép xem loại thẻ.'),
             ('CARD_TYPE_UPDATE_ALL', 'CARD_TYPE', 'UPDATE', 'ALL', 'Cập nhật loại thẻ', 'Cho phép cập nhật loại thẻ.'),
             ('CARD_TYPE_DELETE_ALL', 'CARD_TYPE', 'DELETE', 'ALL', 'Xóa loại thẻ', 'Cho phép xóa loại thẻ.'),

             ('CARD_CREATE_ALL', 'CARD', 'CREATE', 'ALL', 'Tạo thẻ', 'Cho phép tạo thẻ gửi xe.'),
             ('CARD_READ_ALL', 'CARD', 'READ', 'ALL', 'Xem thẻ', 'Cho phép xem thẻ gửi xe.'),
             ('CARD_UPDATE_ALL', 'CARD', 'UPDATE', 'ALL', 'Cập nhật thẻ', 'Cho phép cập nhật thẻ gửi xe.'),
             ('CARD_DELETE_ALL', 'CARD', 'DELETE', 'ALL', 'Xóa thẻ', 'Cho phép xóa thẻ gửi xe.'),

             ('SUBSCRIPTION_CREATE_ALL', 'SUBSCRIPTION', 'CREATE', 'ALL', 'Tạo vé tháng', 'Cho phép tạo vé tháng.'),
             ('SUBSCRIPTION_READ_ALL', 'SUBSCRIPTION', 'READ', 'ALL', 'Xem vé tháng', 'Cho phép xem vé tháng.'),
             ('SUBSCRIPTION_UPDATE_ALL', 'SUBSCRIPTION', 'UPDATE', 'ALL', 'Cập nhật vé tháng', 'Cho phép cập nhật, duyệt hoặc gia hạn vé tháng.'),
             ('SUBSCRIPTION_DELETE_ALL', 'SUBSCRIPTION', 'DELETE', 'ALL', 'Xóa vé tháng', 'Cho phép xóa vé tháng.'),
             ('SUBSCRIPTION_CREATE_OWN', 'SUBSCRIPTION', 'CREATE', 'OWN', 'Đăng ký vé tháng', 'Cho phép khách hàng đăng ký vé tháng.'),
             ('SUBSCRIPTION_READ_OWN', 'SUBSCRIPTION', 'READ', 'OWN', 'Xem vé tháng cá nhân', 'Cho phép khách hàng xem vé tháng của chính mình.'),
             ('SUBSCRIPTION_UPDATE_OWN', 'SUBSCRIPTION', 'UPDATE', 'OWN', 'Cập nhật vé tháng cá nhân', 'Cho phép khách hàng cập nhật hoặc gia hạn vé tháng của chính mình.'),

             ('REPORT_READ_ALL', 'REPORT', 'READ', 'ALL', 'Xem báo cáo', 'Cho phép xem báo cáo doanh thu và lưu lượng xe.'),

             ('LOST_CARD_REPORT_CREATE_ALL', 'LOST_CARD_REPORT', 'CREATE', 'ALL', 'Tạo báo mất thẻ', 'Cho phép tạo báo cáo mất thẻ.'),
             ('LOST_CARD_REPORT_READ_ALL', 'LOST_CARD_REPORT', 'READ', 'ALL', 'Xem báo mất thẻ', 'Cho phép xem báo cáo mất thẻ.'),
             ('LOST_CARD_REPORT_UPDATE_ALL', 'LOST_CARD_REPORT', 'UPDATE', 'ALL', 'Cập nhật báo mất thẻ', 'Cho phép cập nhật xử lý báo mất thẻ.'),
             ('LOST_CARD_REPORT_DELETE_ALL', 'LOST_CARD_REPORT', 'DELETE', 'ALL', 'Xóa báo mất thẻ', 'Cho phép xóa báo cáo mất thẻ.'),

             ('PARKING_SESSION_CREATE_ALL', 'PARKING_SESSION', 'CREATE', 'ALL', 'Tạo phiên gửi xe', 'Cho phép tạo phiên gửi xe khi check-in.'),
             ('PARKING_SESSION_READ_ALL', 'PARKING_SESSION', 'READ', 'ALL', 'Xem phiên gửi xe', 'Cho phép xem phiên gửi xe.'),
             ('PARKING_SESSION_UPDATE_ALL', 'PARKING_SESSION', 'UPDATE', 'ALL', 'Cập nhật phiên gửi xe', 'Cho phép cập nhật phiên gửi xe khi check-out hoặc xử lý nghiệp vụ.'),
             ('PARKING_SESSION_DELETE_ALL', 'PARKING_SESSION', 'DELETE', 'ALL', 'Xóa phiên gửi xe', 'Cho phép xóa phiên gửi xe.'),
             ('PARKING_SESSION_READ_OWN', 'PARKING_SESSION', 'READ', 'OWN', 'Xem lịch sử gửi xe cá nhân', 'Cho phép khách hàng xem lịch sử gửi xe của chính mình.'),

             ('PARKING_EVENT_CREATE_ALL', 'PARKING_EVENT', 'CREATE', 'ALL', 'Tạo sự kiện gửi xe', 'Cho phép tạo sự kiện vào ra.'),
             ('PARKING_EVENT_READ_ALL', 'PARKING_EVENT', 'READ', 'ALL', 'Xem sự kiện gửi xe', 'Cho phép xem sự kiện gửi xe.'),
             ('PARKING_EVENT_UPDATE_ALL', 'PARKING_EVENT', 'UPDATE', 'ALL', 'Cập nhật sự kiện gửi xe', 'Cho phép cập nhật sự kiện gửi xe.'),
             ('PARKING_EVENT_DELETE_ALL', 'PARKING_EVENT', 'DELETE', 'ALL', 'Xóa sự kiện gửi xe', 'Cho phép xóa sự kiện gửi xe.'),
             ('PARKING_EVENT_READ_OWN', 'PARKING_EVENT', 'READ', 'OWN', 'Xem sự kiện gửi xe cá nhân', 'Cho phép khách hàng xem sự kiện gửi xe của chính mình.'),

             ('PUBLIC_INFO_READ_PUBLIC', 'PUBLIC_INFO', 'READ', 'PUBLIC', 'Xem thông tin công khai', 'Cho phép xem bảng giá, nội quy và thông tin công khai.')
     ) AS v(permission_code, module_code, action_code, scope_code, name, description)
         JOIN iam.permission_modules m ON m.code = v.module_code
         JOIN iam.permission_actions a ON a.code = v.action_code
         JOIN iam.permission_scopes s ON s.code = v.scope_code
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (role_id, permission_id, is_active, is_system)
SELECT r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code IN (
                                                         'ACCOUNT_CREATE_ALL', 'ACCOUNT_READ_ALL', 'ACCOUNT_UPDATE_ALL', 'ACCOUNT_DELETE_ALL',
                                                         'ROLE_CREATE_ALL', 'ROLE_READ_ALL', 'ROLE_UPDATE_ALL', 'ROLE_DELETE_ALL',
                                                         'PERMISSION_CREATE_ALL', 'PERMISSION_READ_ALL', 'PERMISSION_UPDATE_ALL', 'PERMISSION_DELETE_ALL',
                                                         'AUDIT_LOG_READ_ALL', 'LOGIN_ATTEMPT_READ_ALL'
    )
WHERE r.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET is_active = TRUE, is_system = TRUE, updated_at = now()
WHERE iam.role_permissions.is_active = FALSE OR iam.role_permissions.is_system = FALSE;

INSERT INTO iam.role_permissions (role_id, permission_id, is_active, is_system)
SELECT r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code IN (
                                                         'EMPLOYEE_CREATE_ALL', 'EMPLOYEE_READ_ALL', 'EMPLOYEE_UPDATE_ALL', 'EMPLOYEE_DELETE_ALL',
                                                         'CUSTOMER_CREATE_ALL', 'CUSTOMER_READ_ALL', 'CUSTOMER_UPDATE_ALL', 'CUSTOMER_DELETE_ALL',
                                                         'USER_PROFILE_CREATE_ALL', 'USER_PROFILE_READ_ALL', 'USER_PROFILE_UPDATE_ALL', 'USER_PROFILE_DELETE_ALL',
                                                         'CUSTOMER_VEHICLE_CREATE_ALL', 'CUSTOMER_VEHICLE_READ_ALL', 'CUSTOMER_VEHICLE_UPDATE_ALL', 'CUSTOMER_VEHICLE_DELETE_ALL',
                                                         'PRICE_PLAN_CREATE_ALL', 'PRICE_PLAN_READ_ALL', 'PRICE_PLAN_UPDATE_ALL', 'PRICE_PLAN_DELETE_ALL',
                                                         'PRICE_RULE_CREATE_ALL', 'PRICE_RULE_READ_ALL', 'PRICE_RULE_UPDATE_ALL', 'PRICE_RULE_DELETE_ALL',
                                                         'VEHICLE_TYPE_CREATE_ALL', 'VEHICLE_TYPE_READ_ALL', 'VEHICLE_TYPE_UPDATE_ALL', 'VEHICLE_TYPE_DELETE_ALL',
                                                         'TICKET_TYPE_CREATE_ALL', 'TICKET_TYPE_READ_ALL', 'TICKET_TYPE_UPDATE_ALL', 'TICKET_TYPE_DELETE_ALL',
                                                         'CARD_TYPE_CREATE_ALL', 'CARD_TYPE_READ_ALL', 'CARD_TYPE_UPDATE_ALL', 'CARD_TYPE_DELETE_ALL',
                                                         'CARD_CREATE_ALL', 'CARD_READ_ALL', 'CARD_UPDATE_ALL', 'CARD_DELETE_ALL',
                                                         'SUBSCRIPTION_CREATE_ALL', 'SUBSCRIPTION_READ_ALL', 'SUBSCRIPTION_UPDATE_ALL', 'SUBSCRIPTION_DELETE_ALL',
                                                         'REPORT_READ_ALL',
                                                         'LOST_CARD_REPORT_CREATE_ALL', 'LOST_CARD_REPORT_READ_ALL', 'LOST_CARD_REPORT_UPDATE_ALL', 'LOST_CARD_REPORT_DELETE_ALL',
                                                         'PARKING_SESSION_CREATE_ALL', 'PARKING_SESSION_READ_ALL', 'PARKING_SESSION_UPDATE_ALL', 'PARKING_SESSION_DELETE_ALL',
                                                         'PARKING_EVENT_CREATE_ALL', 'PARKING_EVENT_READ_ALL', 'PARKING_EVENT_UPDATE_ALL', 'PARKING_EVENT_DELETE_ALL'
    )
WHERE r.code = 'PARKING_MANAGER'
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET is_active = TRUE, is_system = TRUE, updated_at = now()
WHERE iam.role_permissions.is_active = FALSE OR iam.role_permissions.is_system = FALSE;

INSERT INTO iam.role_permissions (role_id, permission_id, is_active, is_system)
SELECT r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code IN (
                                                         'PARKING_SESSION_CREATE_ALL', 'PARKING_SESSION_READ_ALL', 'PARKING_SESSION_UPDATE_ALL',
                                                         'PARKING_EVENT_CREATE_ALL', 'PARKING_EVENT_READ_ALL', 'PARKING_EVENT_UPDATE_ALL',
                                                         'LOST_CARD_REPORT_CREATE_ALL', 'LOST_CARD_REPORT_READ_ALL', 'LOST_CARD_REPORT_UPDATE_ALL',
                                                         'TICKET_TYPE_READ_ALL', 'CARD_READ_ALL', 'CUSTOMER_READ_ALL', 'USER_PROFILE_READ_ALL',
                                                         'CUSTOMER_VEHICLE_READ_ALL', 'VEHICLE_TYPE_READ_ALL',
                                                         'EMPLOYEE_READ_OWN',
                                                         'USER_PROFILE_READ_OWN', 'USER_PROFILE_UPDATE_OWN',
                                                         'CUSTOMER_VEHICLE_CREATE_OWN', 'CUSTOMER_VEHICLE_READ_OWN', 'CUSTOMER_VEHICLE_UPDATE_OWN', 'CUSTOMER_VEHICLE_DELETE_OWN',
                                                         'SUBSCRIPTION_CREATE_OWN', 'SUBSCRIPTION_READ_OWN', 'SUBSCRIPTION_UPDATE_OWN',
                                                         'PARKING_SESSION_READ_OWN', 'PARKING_EVENT_READ_OWN',
                                                         'PUBLIC_INFO_READ_PUBLIC'
    )
WHERE r.code = 'EMPLOYEE'
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET is_active = TRUE, is_system = TRUE, updated_at = now()
WHERE iam.role_permissions.is_active = FALSE OR iam.role_permissions.is_system = FALSE;

INSERT INTO iam.role_permissions (role_id, permission_id, is_active, is_system)
SELECT r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code IN (
                                                         'USER_PROFILE_READ_OWN', 'USER_PROFILE_UPDATE_OWN',
                                                         'CUSTOMER_VEHICLE_CREATE_OWN', 'CUSTOMER_VEHICLE_READ_OWN', 'CUSTOMER_VEHICLE_UPDATE_OWN', 'CUSTOMER_VEHICLE_DELETE_OWN',
                                                         'SUBSCRIPTION_CREATE_OWN', 'SUBSCRIPTION_READ_OWN', 'SUBSCRIPTION_UPDATE_OWN',
                                                         'PARKING_SESSION_READ_OWN', 'PARKING_EVENT_READ_OWN',
                                                         'PUBLIC_INFO_READ_PUBLIC'
    )
WHERE r.code = 'CUSTOMER'
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET is_active = TRUE, is_system = TRUE, updated_at = now()
WHERE iam.role_permissions.is_active = FALSE OR iam.role_permissions.is_system = FALSE;

INSERT INTO iam.role_permissions (role_id, permission_id, is_active, is_system)
SELECT r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code = 'PUBLIC_INFO_READ_PUBLIC'
WHERE r.code = 'GUEST'
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET is_active = TRUE, is_system = TRUE, updated_at = now()
WHERE iam.role_permissions.is_active = FALSE OR iam.role_permissions.is_system = FALSE;

COMMIT;