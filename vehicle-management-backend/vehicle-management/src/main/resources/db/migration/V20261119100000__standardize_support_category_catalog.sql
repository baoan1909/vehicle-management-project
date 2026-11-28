-- See docs/support-category-standardization.md for the pre-migration mapping.
-- New identities preserve the names and foreign keys of historical tickets.
DROP TABLE IF EXISTS pg_temp.support_category_catalog_seed;
CREATE TEMP TABLE support_category_catalog_seed (
    category_id uuid, code text, name text, description text, priority text,
    audience_type text, feature_code text, display_order integer
) ON COMMIT DROP;
INSERT INTO support_category_catalog_seed VALUES
    ('c1910000-0000-4000-8000-000000000001'::uuid, 'CUSTOMER_ACCOUNT_ACCESS', 'Tài khoản và đăng nhập', 'Không đăng nhập được, tài khoản bị khóa sai, phiên đăng nhập lỗi, không nhận được quyền truy cập hợp lệ.', 'HIGH', 'CUSTOMER', NULL, 10),
    ('c1910000-0000-4000-8000-000000000002'::uuid, 'CUSTOMER_PROFILE_VEHICLE', 'Hồ sơ và phương tiện', 'Không cập nhật được hồ sơ hoặc xe, biển số hiển thị sai, dữ liệu xe không đồng bộ.', 'NORMAL', 'CUSTOMER', NULL, 20),
    ('c1910000-0000-4000-8000-000000000003'::uuid, 'CUSTOMER_PARKING_ACCESS', 'Vào/ra bãi xe', 'Không check-in/check-out được, barrier không mở, biển số không nhận diện, trạng thái lượt gửi xe sai.', 'HIGH', 'CUSTOMER', NULL, 30),
    ('c1910000-0000-4000-8000-000000000004'::uuid, 'CUSTOMER_CARD_QR', 'Thẻ và mã QR', 'Thẻ hoặc QR hợp lệ nhưng không sử dụng được, QR không hiển thị, hệ thống báo thẻ không đúng trạng thái.', 'HIGH', 'CUSTOMER', NULL, 40),
    ('c1910000-0000-4000-8000-000000000005'::uuid, 'CUSTOMER_SUBSCRIPTION', 'Vé đăng ký và gia hạn', 'Không mua, gia hạn hoặc kích hoạt được vé, thời hạn sai, vé đã thanh toán nhưng chưa có hiệu lực.', 'NORMAL', 'CUSTOMER', NULL, 50),
    ('c1910000-0000-4000-8000-000000000006'::uuid, 'CUSTOMER_RESERVATION_SPACE', 'Đặt chỗ và vị trí đỗ', 'Không đặt hoặc chọn được vị trí, giữ chỗ sai, vị trí hiển thị trống nhưng không thể chọn.', 'NORMAL', 'CUSTOMER', NULL, 60),
    ('c1910000-0000-4000-8000-000000000007'::uuid, 'CUSTOMER_PAYMENT_BILLING', 'Thanh toán, hóa đơn và ví', 'Thanh toán lỗi, trừ tiền sai, hóa đơn sai, ví không cập nhật, hoàn tiền chưa ghi nhận.', 'HIGH', 'CUSTOMER', NULL, 70),
    ('c1910000-0000-4000-8000-000000000008'::uuid, 'CUSTOMER_HISTORY_DATA', 'Lịch sử và dữ liệu giao dịch', 'Thiếu hoặc sai lịch sử gửi xe, vé, hóa đơn, giao dịch hay trạng thái xử lý.', 'NORMAL', 'CUSTOMER', NULL, 80),
    ('c1910000-0000-4000-8000-000000000009'::uuid, 'CUSTOMER_NOTIFICATION_SUPPORT', 'Thông báo và hỗ trợ trực tuyến', 'Không nhận thông báo, thông báo sai, chatbot hoặc chat hỗ trợ không hoạt động, không gửi được phiếu.', 'NORMAL', 'CUSTOMER', NULL, 90),
    ('c1910000-0000-4000-8000-000000000010'::uuid, 'CUSTOMER_OTHER_SYSTEM', 'Lỗi chức năng khác', 'Lỗi hệ thống chưa thuộc các nhóm trên; người yêu cầu bắt buộc mô tả chức năng gặp lỗi.', 'NORMAL', 'CUSTOMER', NULL, 100),
    ('c1910000-0000-4000-8000-000000000011'::uuid, 'INTERNAL_ACCOUNT_PERMISSION', 'Tài khoản, quyền và phạm vi dữ liệu', 'Không đăng nhập được, thiếu quyền hợp lệ, nhìn sai bãi xe, quyền đã cấp nhưng chưa có hiệu lực.', 'HIGH', 'INTERNAL', 'ACCOUNT_ACCESS', 200),
    ('c1910000-0000-4000-8000-000000000012'::uuid, 'INTERNAL_PARKING_CONFIGURATION', 'Cấu hình bãi xe và sơ đồ', 'Lỗi tạo hoặc cập nhật bãi, tầng, khu vực, vị trí đỗ, sơ đồ hoặc sức chứa.', 'NORMAL', 'INTERNAL', 'PARKING_LOT', 210),
    ('c1910000-0000-4000-8000-000000000013'::uuid, 'INTERNAL_PARKING_OPERATION', 'Vận hành xe vào/ra', 'Lỗi check-in, check-out, barrier, biển số, phân bổ chỗ hoặc đóng lượt gửi xe.', 'HIGH', 'INTERNAL', 'PARKING_SESSION', 220),
    ('c1910000-0000-4000-8000-000000000014'::uuid, 'INTERNAL_DEVICE_INTEGRATION', 'Thiết bị và kết nối', 'Camera, đầu đọc, barrier, máy quét hoặc thiết bị không đồng bộ hay không gửi dữ liệu.', 'HIGH', 'INTERNAL', 'DEVICE', 230),
    ('c1910000-0000-4000-8000-000000000015'::uuid, 'INTERNAL_CATALOG_PRICING', 'Danh mục vé, phương tiện và cấu hình giá', 'Không cấu hình được loại vé, loại xe, kế hoạch giá, quy tắc giá, voucher hoặc hiệu lực áp dụng.', 'NORMAL', 'INTERNAL', 'PRICING', 240),
    ('c1910000-0000-4000-8000-000000000016'::uuid, 'INTERNAL_SUBSCRIPTION_CARD', 'Vé đăng ký, thẻ và QR', 'Không duyệt, kích hoạt hoặc gia hạn vé, cấp thẻ lỗi, QR sai hoặc trạng thái vé không đồng bộ.', 'NORMAL', 'INTERNAL', 'SUBSCRIPTION', 250),
    ('c1910000-0000-4000-8000-000000000017'::uuid, 'INTERNAL_LOST_CARD_WORKFLOW', 'Lỗi xử lý báo mất thẻ', 'Không lập được phiếu báo mất thẻ, tính sai phí, không sinh hóa đơn, thanh toán không cập nhật, không khóa được thẻ cũ hoặc không hoàn tất/hủy được phiếu.', 'HIGH', 'INTERNAL', 'LOST_CARD_REPORT', 260),
    ('c1910000-0000-4000-8000-000000000018'::uuid, 'INTERNAL_BILLING_FINANCE', 'Hóa đơn, thanh toán, ví và đối soát', 'Hóa đơn sai, thanh toán không ghi nhận, ví đối tác sai, phí nền tảng hoặc đối soát bất thường.', 'HIGH', 'INTERNAL', 'BILLING', 270),
    ('c1910000-0000-4000-8000-000000000019'::uuid, 'INTERNAL_WORKFORCE_SHIFT', 'Nhân sự, phân công và ca trực', 'Không tạo hoặc phân công được nhân sự, sai phạm vi bãi, ca trực không cập nhật hoặc không thể mở/đóng ca.', 'NORMAL', 'INTERNAL', 'WORKFORCE', 280),
    ('c1910000-0000-4000-8000-000000000020'::uuid, 'INTERNAL_CUSTOMER_SUPPORT', 'Khách hàng, chat và phiếu hỗ trợ', 'Không mở hội thoại, không gửi/nhận tin, phiếu không cập nhật, thông báo hỗ trợ không đến.', 'NORMAL', 'INTERNAL', 'SUPPORT', 290),
    ('c1910000-0000-4000-8000-000000000021'::uuid, 'INTERNAL_REPORT_DATA', 'Báo cáo, lịch sử và xuất dữ liệu', 'Báo cáo sai, thiếu bản ghi, bộ lọc sai, không xuất được dữ liệu hoặc số liệu không đồng nhất.', 'NORMAL', 'INTERNAL', NULL, 300),
    ('c1910000-0000-4000-8000-000000000022'::uuid, 'INTERNAL_PERFORMANCE_SECURITY', 'Hiệu năng và an toàn hệ thống', 'Trang quá chậm, lỗi hàng loạt, truy cập bất thường, dữ liệu lộ sai phạm vi hoặc hành vi đáng ngờ.', 'URGENT', 'INTERNAL', NULL, 310),
    ('c1910000-0000-4000-8000-000000000023'::uuid, 'INTERNAL_OTHER_SYSTEM', 'Lỗi chức năng nội bộ khác', 'Lỗi hệ thống chưa thuộc nhóm trên; người yêu cầu bắt buộc chỉ rõ chức năng bị ảnh hưởng.', 'NORMAL', 'INTERNAL', NULL, 320);

-- A custom record using a reserved new code must be reviewed, never overwritten.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM operations.support_ticket_categories category
        JOIN support_category_catalog_seed seed ON seed.code = category.code
        WHERE category.category_id <> seed.category_id
    ) THEN
        RAISE EXCEPTION 'Support category catalog code collision: preserve custom data and review reserved CUSTOMER_/INTERNAL_ codes';
    END IF;
END $$;

DROP TABLE IF EXISTS pg_temp.support_category_inserted_ids;
CREATE TEMP TABLE support_category_inserted_ids (category_id uuid PRIMARY KEY) ON COMMIT DROP;
WITH inserted AS (
INSERT INTO operations.support_ticket_categories (
    category_id, code, name, description, priority, status, audience_type,
    feature_code, permission_match_mode, display_order
)
SELECT category_id, code, name, description, priority, 'ACTIVE', audience_type,
       feature_code, 'ANY', display_order
FROM support_category_catalog_seed
ON CONFLICT (code) DO NOTHING
RETURNING category_id
)
INSERT INTO support_category_inserted_ids SELECT category_id FROM inserted;

DROP TABLE IF EXISTS pg_temp.support_category_permission_seed;
CREATE TEMP TABLE support_category_permission_seed (code text, permission_code text) ON COMMIT DROP;
INSERT INTO support_category_permission_seed VALUES
    ('INTERNAL_PARKING_CONFIGURATION', 'PARKING_LOT_READ_ALL'),
    ('INTERNAL_PARKING_OPERATION', 'PARKING_SESSION_READ_ALL'),
    ('INTERNAL_PARKING_OPERATION', 'PARKING_SESSION_CHECK_IN_ALL'),
    ('INTERNAL_PARKING_OPERATION', 'PARKING_SESSION_CHECK_OUT_ALL'),
    ('INTERNAL_DEVICE_INTEGRATION', 'DEVICE_READ_ALL'),
    ('INTERNAL_CATALOG_PRICING', 'PRICE_PLAN_READ_ALL'),
    ('INTERNAL_CATALOG_PRICING', 'PRICE_RULE_READ_ALL'),
    ('INTERNAL_CATALOG_PRICING', 'TICKET_TYPE_READ_ALL'),
    ('INTERNAL_CATALOG_PRICING', 'VEHICLE_TYPE_READ_ALL'),
    ('INTERNAL_CATALOG_PRICING', 'VOUCHER_READ_ALL'),
    ('INTERNAL_SUBSCRIPTION_CARD', 'SUBSCRIPTION_READ_ALL'),
    ('INTERNAL_SUBSCRIPTION_CARD', 'CARD_READ_ALL'),
    ('INTERNAL_LOST_CARD_WORKFLOW', 'LOST_CARD_REPORT_READ_ALL'),
    ('INTERNAL_LOST_CARD_WORKFLOW', 'LOST_CARD_REPORT_CREATE_ALL'),
    ('INTERNAL_LOST_CARD_WORKFLOW', 'LOST_CARD_REPORT_UPDATE_ALL'),
    ('INTERNAL_BILLING_FINANCE', 'INVOICE_READ_ALL'),
    ('INTERNAL_BILLING_FINANCE', 'PAYMENT_READ_ALL'),
    ('INTERNAL_BILLING_FINANCE', 'WALLET_READ_PARTNER'),
    ('INTERNAL_BILLING_FINANCE', 'WALLET_READ_ALL'),
    ('INTERNAL_BILLING_FINANCE', 'WALLET_RECONCILIATION_READ_ALL'),
    ('INTERNAL_WORKFORCE_SHIFT', 'EMPLOYEE_READ_ALL'),
    ('INTERNAL_WORKFORCE_SHIFT', 'EMPLOYEE_READ_OWN'),
    ('INTERNAL_WORKFORCE_SHIFT', 'SHIFT_READ_ALL'),
    ('INTERNAL_CUSTOMER_SUPPORT', 'CUSTOMER_READ_ALL'),
    ('INTERNAL_CUSTOMER_SUPPORT', 'CHAT_CONVERSATION_READ_OWN'),
    ('INTERNAL_CUSTOMER_SUPPORT', 'CHAT_CONVERSATION_READ_ALL'),
    ('INTERNAL_CUSTOMER_SUPPORT', 'SUPPORT_TICKET_READ_ALL'),
    ('INTERNAL_CUSTOMER_SUPPORT', 'SUPPORT_TICKET_READ_ASSIGNED'),
    ('INTERNAL_REPORT_DATA', 'REPORT_READ_ALL'),
    ('INTERNAL_REPORT_DATA', 'PARKING_SESSION_READ_ALL');

-- Fail closed if a prerequisite permission is absent; ANY with no mapping must
-- never accidentally expose a restricted category to all internal accounts.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM support_category_permission_seed seed
        LEFT JOIN iam.permissions permission ON permission.permission_code = seed.permission_code
        WHERE permission.permission_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Support category catalog requires existing feature permissions; apply prerequisite IAM migrations';
    END IF;
END $$;

INSERT INTO operations.support_ticket_category_permissions (category_id, permission_id)
SELECT category.category_id, permission.permission_id
FROM support_category_permission_seed seed
JOIN support_category_catalog_seed catalog ON catalog.code = seed.code
JOIN operations.support_ticket_categories category
    ON category.code = catalog.code AND category.category_id = catalog.category_id
JOIN support_category_inserted_ids inserted ON inserted.category_id = category.category_id
JOIN iam.permissions permission ON permission.permission_code = seed.permission_code
ON CONFLICT (category_id, permission_id) DO NOTHING;

-- Retire only untouched shipped seeds. Never rename/reassign/delete history.
-- Audit provenance plus full metadata guards protect administrator customizations.
UPDATE operations.support_ticket_categories category
SET status = 'INACTIVE', updated_at = now()
FROM (VALUES
    ('LOST_CARD', 'Mất thẻ xe', 'Khách hàng mất thẻ xe hoặc không xuất trình được thẻ khi ra bãi.', 'HIGH', 'CUSTOMER', NULL, 10),
    ('WRONG_FEE', 'Khiếu nại phí gửi xe', 'Khách hàng phản ánh bị tính sai phí gửi xe.', 'HIGH', 'CUSTOMER', NULL, 20),
    ('VEHICLE_DAMAGE', 'Xe hư hỏng/trầy xước', 'Khách hàng phản ánh xe bị trầy xước, hư hỏng hoặc mất tài sản.', 'URGENT', 'CUSTOMER', NULL, 30),
    ('CARD_NOT_WORKING', 'Thẻ không hoạt động', 'Thẻ gửi xe không quét được hoặc không sử dụng được.', 'NORMAL', 'CUSTOMER', NULL, 40),
    ('SUBSCRIPTION_PROBLEM', 'Vấn đề vé đăng ký', 'Vé tháng, quý, năm hoặc miễn phí không hoạt động hoặc cần hỗ trợ.', 'NORMAL', 'CUSTOMER', NULL, 50),
    ('PAYMENT_PROBLEM', 'Vấn đề thanh toán', 'Thanh toán lỗi, đã thanh toán nhưng chưa ghi nhận hoặc cần kiểm tra giao dịch.', 'HIGH', 'CUSTOMER', NULL, 60),
    ('PARKING_HISTORY_REQUEST', 'Yêu cầu tra cứu lịch sử gửi xe', 'Khách hàng cần tra cứu lịch sử gửi xe.', 'LOW', 'CUSTOMER', NULL, 70),
    ('PROFILE_OR_VEHICLE_UPDATE', 'Cập nhật hồ sơ/xe', 'Khách hàng cần hỗ trợ cập nhật thông tin cá nhân hoặc phương tiện.', 'LOW', 'CUSTOMER', NULL, 80),
    ('STAFF_COMPLAINT', 'Khiếu nại nhân viên', 'Khách hàng phản ánh thái độ hoặc cách xử lý của nhân viên.', 'HIGH', 'CUSTOMER', NULL, 90),
    ('OTHER', 'Vấn đề khác', 'Yêu cầu hỗ trợ khác chưa thuộc nhóm cố định.', 'NORMAL', 'CUSTOMER', NULL, 100),
    ('PARKING_OPERATION', 'Vận hành xe vào/ra', 'Sự cố check-in, check-out, thẻ, biển số hoặc làn xe.', 'HIGH', 'INTERNAL', 'PARKING_SESSION', 200),
    ('DEVICE_INCIDENT', 'Sự cố thiết bị', 'Camera, đầu đọc thẻ, barrier hoặc thiết bị vận hành gặp sự cố.', 'HIGH', 'INTERNAL', 'DEVICE', 210),
    ('PRICING_CONFIGURATION', 'Cấu hình giá', 'Hỗ trợ kế hoạch giá, quy tắc giá và hiệu lực áp dụng.', 'NORMAL', 'INTERNAL', 'PRICING', 220),
    ('TICKET_CONFIGURATION', 'Cấu hình loại vé', 'Hỗ trợ cấu hình và sử dụng loại vé.', 'NORMAL', 'INTERNAL', 'TICKET_TYPE', 230),
    ('VEHICLE_CONFIGURATION', 'Cấu hình loại phương tiện', 'Hỗ trợ danh mục loại phương tiện.', 'NORMAL', 'INTERNAL', 'VEHICLE_TYPE', 240),
    ('PARKING_LAYOUT', 'Bãi xe và sơ đồ', 'Hỗ trợ bãi xe, khu vực, vị trí và sơ đồ vận hành.', 'NORMAL', 'INTERNAL', 'PARKING_LOT', 250),
    ('WORKFORCE', 'Nhân sự và ca trực', 'Hỗ trợ nhân sự, phân công và ca trực.', 'NORMAL', 'INTERNAL', 'WORKFORCE', 260),
    ('ACCOUNT_ACCESS', 'Tài khoản và quyền truy cập', 'Hỗ trợ đăng nhập, phạm vi và quyền truy cập của tài khoản nội bộ.', 'HIGH', 'INTERNAL', 'ACCOUNT_ACCESS', 270)
) legacy(code, name, description, priority, audience_type, feature_code, display_order)
WHERE category.code = legacy.code
  AND category.name = legacy.name
  AND category.description = legacy.description
  AND category.priority = legacy.priority
  AND category.audience_type = legacy.audience_type
  AND category.feature_code IS NOT DISTINCT FROM legacy.feature_code
  AND category.display_order = legacy.display_order
  AND category.permission_match_mode = 'ANY'
  AND category.created_by IS NULL AND category.updated_by IS NULL
  AND category.status = 'ACTIVE';
