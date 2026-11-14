-- Customer portal guide drafts wired to the current frontend anchors.
-- These records deliberately remain DRAFT/current_version=0. A System Admin
-- must review and publish version 1 before customers can resolve them.

CREATE TEMP TABLE customer_user_guide_seed (
    guide_code VARCHAR(120) NOT NULL,
    context_key VARCHAR(120) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    deep_link VARCHAR(1000) NOT NULL,
    priority INTEGER NOT NULL,
    audience_mode VARCHAR(30) NOT NULL,
    role_codes TEXT[] NOT NULL,
    permission_codes TEXT[] NOT NULL,
    steps JSONB NOT NULL
) ON COMMIT DROP;

INSERT INTO customer_user_guide_seed VALUES
('CUSTOMER_PROFILE_ONBOARDING', '/CUSTOMER/PROFILE',
 'Hoàn thiện hồ sơ khách hàng', 'Cập nhật danh tính, liên hệ và thông tin xác minh trước khi sử dụng dịch vụ.', '/customer/profile',
 100, 'ROLE', ARRAY['CUSTOMER'], ARRAY[]::TEXT[],
 '[
   {"anchor":"CUSTOMER.PROFILE.SUMMARY","title":"Tổng quan hồ sơ","body":"Kiểm tra trạng thái tài khoản và thông tin khách hàng hiện tại.","collapseMs":150},
   {"anchor":"CUSTOMER.PROFILE.AVATAR","title":"Ảnh đại diện","body":"Tải ảnh rõ mặt, đúng định dạng và dung lượng cho phép.","collapseMs":150},
   {"anchor":"CUSTOMER.PROFILE.IDENTITY","title":"Thông tin định danh","body":"Bổ sung họ tên, ngày sinh, giới tính và CCCD chính xác.","collapseMs":150},
   {"anchor":"CUSTOMER.PROFILE.CONTACT","title":"Thông tin liên hệ","body":"Kiểm tra email và số điện thoại để nhận thông báo.","collapseMs":150},
   {"anchor":"CUSTOMER.PROFILE.ADDRESS","title":"Địa chỉ liên hệ","body":"Chọn địa chỉ theo danh mục hành chính hiện hành.","collapseMs":150},
   {"anchor":"CUSTOMER.PROFILE.VERIFICATION","title":"Tài liệu xác minh","body":"Theo dõi trạng thái duyệt và yêu cầu bổ sung hồ sơ.","collapseMs":150},
   {"anchor":"CUSTOMER.PROFILE.SAVE","title":"Lưu hồ sơ","body":"Tự kiểm tra dữ liệu trước khi lưu hoặc gửi hồ sơ.","collapseMs":150},
   {"anchor":"CUSTOMER.PROFILE.RESUBMIT","title":"Gửi duyệt lại","body":"Nếu hồ sơ bị từ chối, cập nhật theo góp ý rồi chủ động gửi lại.","collapseMs":150}
 ]'::jsonb),
('CUSTOMER_VEHICLE_CREATE', '/CUSTOMER/VEHICLES',
 'Thêm phương tiện', 'Khai báo phương tiện để đăng ký vé và theo dõi hành trình.', '/customer/vehicles',
 101, 'PERMISSION_ANY', ARRAY[]::TEXT[], ARRAY['CUSTOMER_VEHICLE_CREATE_OWN','CUSTOMER_VEHICLE_CREATE_ALL'],
 '[
   {"anchor":"CUSTOMER.VEHICLE.CREATE_OPEN","title":"Thêm xe mới","body":"Mở biểu mẫu thêm phương tiện của chính bạn.","collapseMs":150},
   {"anchor":"CUSTOMER.VEHICLE.FORM","title":"Biểu mẫu phương tiện","body":"Các bước hướng dẫn chỉ chuẩn bị giao diện, không tự lưu dữ liệu.","collapseMs":150},
   {"anchor":"CUSTOMER.VEHICLE.TYPE","title":"Loại xe","body":"Chọn loại phương tiện đúng với đăng ký xe.","collapseMs":150},
   {"anchor":"CUSTOMER.VEHICLE.PLATE","title":"Biển số","body":"Nhập biển số chính xác; hệ thống sẽ chuẩn hóa chữ hoa.","collapseMs":150},
   {"anchor":"CUSTOMER.VEHICLE.BRAND_COLOR","title":"Hãng và màu xe","body":"Bổ sung đặc điểm giúp nhận diện phương tiện.","collapseMs":150},
   {"anchor":"CUSTOMER.VEHICLE.DEFAULT","title":"Xe mặc định","body":"Đặt xe thường dùng làm mặc định nếu cần.","collapseMs":150},
   {"anchor":"CUSTOMER.VEHICLE.SAVE","title":"Lưu xe","body":"Kiểm tra lại rồi chủ động nhấn Lưu xe.","collapseMs":150},
   {"anchor":"CUSTOMER.VEHICLE.LIST","title":"Danh sách xe","body":"Theo dõi toàn bộ phương tiện đã đăng ký.","collapseMs":150},
   {"anchor":"CUSTOMER.VEHICLE.ACTIONS","title":"Thao tác phương tiện","body":"Chỉnh sửa, đặt mặc định hoặc đổi trạng thái theo quyền hiện có.","collapseMs":150}
 ]'::jsonb),
('CUSTOMER_SUBSCRIPTION_REGISTER', '/CUSTOMER/SUBSCRIPTIONS',
 'Đăng ký vé gửi xe', 'Chọn bãi, phương tiện, loại vé và kiểm tra báo giá trước khi gửi đăng ký.', '/customer/subscriptions?register=true',
 102, 'PERMISSION_ANY', ARRAY[]::TEXT[], ARRAY['SUBSCRIPTION_CREATE_OWN','SUBSCRIPTION_CREATE_ALL'],
 '[
   {"anchor":"CUSTOMER.SUBSCRIPTION.OPEN_REGISTER","title":"Mở đăng ký vé","body":"Bắt đầu đăng ký sau khi hồ sơ đã được duyệt và có phương tiện hợp lệ.","collapseMs":150},
   {"anchor":"CUSTOMER.SUBSCRIPTION.MODAL","title":"Biểu mẫu đăng ký","body":"Hoàn thiện lần lượt các trường; hướng dẫn không tự gửi đăng ký.","collapseMs":150},
   {"anchor":"CUSTOMER.SUBSCRIPTION.PARKING_LOT","title":"Chọn bãi xe","body":"Chọn bãi đang hoạt động và phù hợp với nhu cầu.","collapseMs":150},
   {"anchor":"CUSTOMER.SUBSCRIPTION.VEHICLE","title":"Chọn phương tiện","body":"Chỉ phương tiện đang hoạt động mới có thể đăng ký vé.","collapseMs":150},
   {"anchor":"CUSTOMER.SUBSCRIPTION.ADD_VEHICLE","title":"Thêm phương tiện","body":"Nếu chưa có xe, mở biểu mẫu thêm xe rồi quay lại đăng ký.","collapseMs":150},
   {"anchor":"CUSTOMER.SUBSCRIPTION.TICKET_TYPE","title":"Chọn loại vé","body":"Chọn loại vé được bãi và loại xe hỗ trợ.","collapseMs":150},
   {"anchor":"CUSTOMER.SUBSCRIPTION.PERIOD","title":"Số kỳ thanh toán","body":"Chọn số kỳ cần thanh toán trong giới hạn cho phép.","collapseMs":150},
   {"anchor":"CUSTOMER.SUBSCRIPTION.EFFECTIVE_DATE","title":"Ngày bắt đầu","body":"Ngày hiệu lực phải đáp ứng quy định của gói vé.","collapseMs":150},
   {"anchor":"CUSTOMER.SUBSCRIPTION.PHYSICAL_CARD","title":"Thẻ vật lý","body":"Chọn dùng QR hoặc nhận thẻ vật lý có khoản cọc riêng.","collapseMs":150},
   {"anchor":"CUSTOMER.SUBSCRIPTION.VOUCHER","title":"Mã ưu đãi","body":"Nhập mã và chủ động áp dụng để hệ thống kiểm tra.","collapseMs":150},
   {"anchor":"CUSTOMER.SUBSCRIPTION.QUOTE","title":"Kiểm tra báo giá","body":"Đối chiếu tiền vé, tiền cọc và ưu đãi trước khi gửi.","collapseMs":150},
   {"anchor":"CUSTOMER.SUBSCRIPTION.SUBMIT","title":"Gửi đăng ký","body":"Chỉ nhấn khi toàn bộ thông tin và số tiền đã chính xác.","collapseMs":150}
 ]'::jsonb),
('CUSTOMER_NEARBY_PARKING_DISCOVERY', '/PARKING-LOTS/NEARBY',
 'Tìm bãi xe gần tôi', 'Tìm kiếm theo vị trí, bán kính, bản đồ hoặc tra cứu biển số.', '/parking-lots/nearby',
 103, 'ROLE', ARRAY['CUSTOMER'], ARRAY[]::TEXT[],
 '[
   {"anchor":"CUSTOMER.NEARBY.LOCATION_MODE","title":"Chọn cách xác định vị trí","body":"Bạn quyết định dùng GPS, chọn trên bản đồ hoặc nhập địa chỉ; hệ thống không tự xin vị trí từ hướng dẫn.","collapseMs":150},
   {"anchor":"CUSTOMER.NEARBY.RADIUS","title":"Bán kính tìm kiếm","body":"Nhập bán kính từ 1 đến 50 km.","collapseMs":150},
   {"anchor":"CUSTOMER.NEARBY.SEARCH_ACTION","title":"Thực hiện tìm kiếm","body":"Chủ động nhấn Áp dụng sau khi chọn vị trí và bán kính.","collapseMs":150},
   {"anchor":"CUSTOMER.NEARBY.MAP","title":"Bản đồ bãi xe","body":"Xem vị trí của bạn và các bãi trong phạm vi tìm kiếm.","collapseMs":150},
   {"anchor":"CUSTOMER.NEARBY.RESULT_LIST","title":"Danh sách kết quả","body":"Kết quả được sắp theo khoảng cách tăng dần.","collapseMs":150},
   {"anchor":"CUSTOMER.NEARBY.LOT_ACTIONS","title":"Thao tác với bãi","body":"Chọn bãi để xem chỉ đường, đặt chỗ hoặc đăng ký vé.","collapseMs":150},
   {"anchor":"CUSTOMER.NEARBY.PLATE_LOOKUP","title":"Tra cứu biển số","body":"Tìm phiên gửi xe hoặc đặt chỗ còn hiệu lực theo biển số.","collapseMs":150}
 ]'::jsonb),
('CUSTOMER_PRICING_DISCOVERY', '/PRICING',
 'Tìm bảng giá theo bãi', 'Tìm bãi, sắp xếp theo vị trí và mở bảng giá chi tiết.', '/pricing',
 104, 'ROLE', ARRAY['CUSTOMER'], ARRAY[]::TEXT[],
 '[
   {"anchor":"CUSTOMER.PRICING.LOT_SEARCH","title":"Tìm bãi xe","body":"Tìm theo tên, mã hoặc địa chỉ bãi.","collapseMs":150},
   {"anchor":"CUSTOMER.PRICING.LOCATION_SORT","title":"Sắp xếp theo vị trí","body":"Khi bạn cho phép GPS, danh sách ưu tiên bãi gần hơn.","collapseMs":150},
   {"anchor":"CUSTOMER.PRICING.FAVORITE_FILTER","title":"Bãi yêu thích","body":"Lọc nhanh các bãi bạn đã đánh dấu.","collapseMs":150},
   {"anchor":"CUSTOMER.PRICING.LOT_LIST","title":"Danh sách bãi","body":"So sánh trạng thái, khoảng giá và loại xe hỗ trợ.","collapseMs":150},
   {"anchor":"CUSTOMER.PRICING.LOT_DETAIL_ACTION","title":"Mở bảng giá chi tiết","body":"Chọn một bãi để xem mức giá và gói vé hiện hành.","collapseMs":150}
 ]'::jsonb),
('CUSTOMER_PRICING_DETAIL', '/PRICING/DETAIL',
 'Xem chi tiết bảng giá', 'So sánh giá vãng lai, vé đăng ký, ưu đãi và mở đăng ký vé.', '/pricing',
 105, 'ROLE', ARRAY['CUSTOMER'], ARRAY[]::TEXT[],
 '[
   {"anchor":"CUSTOMER.PRICING.LOT_SUMMARY","title":"Thông tin bãi","body":"Kiểm tra bãi, trạng thái, địa chỉ và khoảng giá.","collapseMs":150},
   {"anchor":"CUSTOMER.PRICING.VEHICLE_TYPE","title":"Loại phương tiện","body":"Chọn loại xe để lọc đúng các quy tắc giá.","collapseMs":150},
   {"anchor":"CUSTOMER.PRICING.VISITOR_RATES","title":"Giá khách vãng lai","body":"Xem giá theo lượt, thời gian hoặc khung giờ.","collapseMs":150},
   {"anchor":"CUSTOMER.PRICING.TICKET_TYPE","title":"Loại vé đăng ký","body":"Lọc gói vé theo chu kỳ phù hợp.","collapseMs":150},
   {"anchor":"CUSTOMER.PRICING.SUBSCRIPTION_RATES","title":"Giá vé đăng ký","body":"So sánh phí và quyền lợi của từng gói.","collapseMs":150},
   {"anchor":"CUSTOMER.PRICING.VOUCHERS","title":"Ưu đãi hiện có","body":"Kiểm tra voucher được hiển thị cùng gói vé.","collapseMs":150},
   {"anchor":"CUSTOMER.PRICING.REGISTER_ACTION","title":"Đăng ký vé","body":"Mở trang đăng ký; hệ thống không tự gửi yêu cầu.","collapseMs":150}
 ]'::jsonb),
('CUSTOMER_PARKING_HISTORY_REVIEW', '/CUSTOMER/PARKING-HISTORY',
 'Xem lịch sử gửi xe', 'Lọc phiên gửi, xem sự kiện, chi phí và đánh giá bãi.', '/customer/parking-history',
 106, 'PERMISSION_ANY', ARRAY[]::TEXT[], ARRAY['PARKING_SESSION_READ_OWN','PARKING_SESSION_READ_ALL'],
 '[
   {"anchor":"CUSTOMER.HISTORY.STATS","title":"Tổng quan lịch sử","body":"Theo dõi số phiên đang gửi, đã hoàn tất và tổng phí.","collapseMs":150},
   {"anchor":"CUSTOMER.HISTORY.FILTERS","title":"Bộ lọc","body":"Lọc theo thời gian, biển số, mã phiên hoặc trạng thái.","collapseMs":150},
   {"anchor":"CUSTOMER.HISTORY.SESSION_LIST","title":"Danh sách phiên gửi","body":"Chọn một phiên để xem chi tiết.","collapseMs":150},
   {"anchor":"CUSTOMER.HISTORY.SESSION_DETAIL","title":"Chi tiết phiên","body":"Đối chiếu phương tiện, bãi, thẻ và khu vực.","collapseMs":150},
   {"anchor":"CUSTOMER.HISTORY.EVENT_TIMELINE","title":"Sự kiện vào và ra","body":"Theo dõi thời gian, làn và biển số được ghi nhận.","collapseMs":150},
   {"anchor":"CUSTOMER.HISTORY.PRICE","title":"Chi phí phiên gửi","body":"Kiểm tra tổng phí sau khi phiên đã đóng.","collapseMs":150},
   {"anchor":"CUSTOMER.HISTORY.REVIEW_ACTION","title":"Đánh giá bãi xe","body":"Phiên đã hoàn tất có thể mở hoặc cập nhật đánh giá.","collapseMs":150}
 ]'::jsonb),
('CUSTOMER_SUPPORT_CHAT', '/CUSTOMER/SUPPORT/CHAT',
 'Chat với bộ phận hỗ trợ', 'Chọn hội thoại, gửi tin nhắn, đính kèm ảnh và tạo phiếu khi đủ quyền.', '/customer/support/chat',
 107, 'PERMISSION_ALL', ARRAY[]::TEXT[], ARRAY['CHAT_CONVERSATION_READ_OWN','CHAT_MESSAGE_SEND_OWN'],
 '[
   {"anchor":"CUSTOMER.CHAT.INBOX","title":"Hộp thoại","body":"Tìm và chọn hội thoại cần tiếp tục.","collapseMs":150},
   {"anchor":"CUSTOMER.CHAT.CONVERSATION","title":"Hội thoại","body":"Kiểm tra đúng người nhận và ngữ cảnh hỗ trợ.","collapseMs":150},
   {"anchor":"CUSTOMER.CHAT.MESSAGE_HISTORY","title":"Lịch sử tin nhắn","body":"Xem các trao đổi và tệp đính kèm trước đó.","collapseMs":150},
   {"anchor":"CUSTOMER.CHAT.COMPOSER","title":"Soạn tin nhắn","body":"Nhập nội dung rõ ràng; hướng dẫn không tự gửi tin.","collapseMs":150},
   {"anchor":"CUSTOMER.CHAT.ATTACHMENT","title":"Đính kèm ảnh","body":"Nút chỉ khả dụng khi tài khoản có quyền gửi tệp.","collapseMs":150},
   {"anchor":"CUSTOMER.CHAT.SEND","title":"Gửi tin nhắn","body":"Chủ động nhấn Gửi sau khi kiểm tra nội dung.","collapseMs":150},
   {"anchor":"CUSTOMER.CHAT.MORE_ACTIONS","title":"Tác vụ khác","body":"Mở lịch sử yêu cầu hoặc tác vụ hỗ trợ bổ sung.","collapseMs":150},
   {"anchor":"CUSTOMER.CHAT.CREATE_TICKET","title":"Tạo phiếu hỗ trợ","body":"Tạo phiếu khi vấn đề cần theo dõi; nút có thể bị khóa nếu chưa đủ quyền.","collapseMs":150}
 ]'::jsonb);

INSERT INTO notification.user_guide_features (feature_id, code, name, description, enabled)
VALUES (md5('vehicle-guide-feature:CUSTOMER_PORTAL')::uuid, 'CUSTOMER_PORTAL', 'Cổng thông tin khách hàng',
        'Hướng dẫn các luồng tự phục vụ dành cho khách hàng.', true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO notification.user_guides (
    guide_id, feature_id, code, status, display_type, replay_policy, context_key,
    priority, display_order, start_at, current_version
)
SELECT md5('vehicle-guide-seed:' || seed.guide_code)::uuid,
       feature.feature_id, seed.guide_code, 'DRAFT', 'COACH_MARK', 'ONCE_PER_VERSION',
       seed.context_key, seed.priority, seed.priority, TIMESTAMPTZ '2026-01-01 00:00:00+07', 0
FROM customer_user_guide_seed seed
JOIN notification.user_guide_features feature ON feature.code = 'CUSTOMER_PORTAL'
ON CONFLICT (code) DO NOTHING;

INSERT INTO notification.user_guide_versions (
    guide_version_id, guide_id, version_number, title, summary, deep_link, audience_mode
)
SELECT md5('vehicle-guide-version:' || seed.guide_code || ':1')::uuid,
       guide.guide_id, 1, seed.title, seed.summary, seed.deep_link, seed.audience_mode
FROM customer_user_guide_seed seed
JOIN notification.user_guides guide
  ON guide.guide_id = md5('vehicle-guide-seed:' || seed.guide_code)::uuid
ON CONFLICT (guide_id, version_number) DO NOTHING;

INSERT INTO notification.user_guide_steps (
    step_id, guide_version_id, display_order, title, body, anchor_key, placement, collapse_ms
)
SELECT md5('vehicle-guide-step:' || seed.guide_code || ':' || step.ordinality)::uuid,
       version.guide_version_id, step.ordinality - 1, step.value ->> 'title', step.value ->> 'body',
       step.value ->> 'anchor', 'BOTTOM', COALESCE((step.value ->> 'collapseMs')::integer, 0)
FROM customer_user_guide_seed seed
JOIN notification.user_guide_versions version
  ON version.guide_version_id = md5('vehicle-guide-version:' || seed.guide_code || ':1')::uuid
CROSS JOIN LATERAL jsonb_array_elements(seed.steps) WITH ORDINALITY AS step(value, ordinality)
ON CONFLICT (guide_version_id, display_order) DO NOTHING;

INSERT INTO notification.user_guide_audiences (audience_id, guide_version_id, target_type, role_id)
SELECT md5('vehicle-guide-audience:' || seed.guide_code || ':ROLE:' || target.role_code)::uuid,
       version.guide_version_id, 'ROLE', role.role_id
FROM customer_user_guide_seed seed
JOIN notification.user_guide_versions version
  ON version.guide_version_id = md5('vehicle-guide-version:' || seed.guide_code || ':1')::uuid
CROSS JOIN LATERAL unnest(seed.role_codes) AS target(role_code)
JOIN iam.roles role ON role.code = target.role_code
ON CONFLICT DO NOTHING;

INSERT INTO notification.user_guide_audiences (audience_id, guide_version_id, target_type, permission_id)
SELECT md5('vehicle-guide-audience:' || seed.guide_code || ':PERMISSION:' || target.permission_code)::uuid,
       version.guide_version_id, 'PERMISSION', permission.permission_id
FROM customer_user_guide_seed seed
JOIN notification.user_guide_versions version
  ON version.guide_version_id = md5('vehicle-guide-version:' || seed.guide_code || ':1')::uuid
CROSS JOIN LATERAL unnest(seed.permission_codes) AS target(permission_code)
JOIN iam.permissions permission ON permission.permission_code = target.permission_code
ON CONFLICT DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM customer_user_guide_seed seed
        CROSS JOIN LATERAL unnest(seed.role_codes) AS required(role_code)
        LEFT JOIN iam.roles role ON role.code = required.role_code
        WHERE role.role_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Customer user guide seed references an unknown role code';
    END IF;
    IF EXISTS (
        SELECT 1 FROM customer_user_guide_seed seed
        CROSS JOIN LATERAL unnest(seed.permission_codes) AS required(permission_code)
        LEFT JOIN iam.permissions permission ON permission.permission_code = required.permission_code
        WHERE permission.permission_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Customer user guide seed references an unknown permission code';
    END IF;
END $$;
