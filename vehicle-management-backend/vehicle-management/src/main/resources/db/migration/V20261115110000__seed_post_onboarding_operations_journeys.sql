-- Manual-only post-onboarding journeys. Selection is driven by effective
-- permissions in the client and revalidated by the user-guide runtime.
-- These guides explain dependency order only; viewing them never mutates
-- parking, catalog, pricing, staffing, shift, payment, or session state.

CREATE TEMP TABLE post_onboarding_guide_seed (
    guide_code VARCHAR(120) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    audience_mode VARCHAR(30) NOT NULL,
    permission_codes TEXT[] NOT NULL,
    priority INTEGER NOT NULL,
    steps JSONB NOT NULL
) ON COMMIT DROP;

INSERT INTO post_onboarding_guide_seed VALUES
('POST_ONBOARDING_PARTNER_SETUP',
 'Lộ trình thiết lập và đưa bãi xe vào vận hành',
 'Đi từ organization, catalog, giá, cấu trúc bãi và nhân sự đến check-in/check-out.',
 'PERMISSION_ALL', ARRAY['PARKING_LOT_CREATE_ALL','PRICE_PLAN_CREATE_ALL'], 1,
 '[
   {"anchor":"ADMIN.HELP.LAUNCHER","placement":"BOTTOM","title":"Mở lại lộ trình bất cứ lúc nào","body":"Dấu hỏi trên header mở lại hướng dẫn của màn hình hiện tại; tại trang tổng quan, nó mở lộ trình sau onboarding này. Xem hướng dẫn không tự đánh dấu nghiệp vụ đã hoàn thành."},
   {"anchor":"ADMIN.NAV.PARKING","placement":"RIGHT","title":"1. Xác nhận phạm vi và tạo bãi","body":"Kiểm tra organization đang hoạt động, sau đó tạo bãi ở trạng thái thiết lập. Hoàn thiện địa chỉ hành chính, vị trí bản đồ và người quản lý. Không kích hoạt khi bãi chưa sẵn sàng."},
   {"anchor":"ADMIN.NAV.CATALOG","placement":"RIGHT","title":"2. Chuẩn bị loại phương tiện và loại vé","body":"Rà soát catalog mẫu của organization, giữ các loại thực sự cung cấp và kích hoạt theo quyền. Loại phương tiện và loại vé là dependency của quy tắc giá."},
   {"anchor":"ADMIN.NAV.PRICING","placement":"RIGHT","title":"3. Công bố kế hoạch và quy tắc giá","body":"Tạo kế hoạch đúng dịch vụ VISITOR hoặc CUSTOMER, khoảng hiệu lực không xung đột; sau đó tạo quy tắc theo loại xe, loại vé, đơn vị và khung giờ. Bãi chỉ sẵn sàng cho dịch vụ có giá hợp lệ."},
   {"anchor":"ADMIN.NAV.PARKING","placement":"RIGHT","title":"4. Hoàn thiện cấu trúc bãi","body":"Tạo tầng hoặc mặt bằng, zone, cổng, ít nhất một làn IN và một làn OUT đang hoạt động. Với zone SPACE, thiết kế và xuất bản layout trước khi kích hoạt."},
   {"anchor":"ADMIN.NAV.WORKFORCE","placement":"RIGHT","title":"5. Phân công người vận hành","body":"Phân công Parking Manager, nhân viên và ca trực đúng bãi, đúng thời điểm hiệu lực. Người có effective permission có thể tiếp tục phần còn thiếu; hướng dẫn không mở rộng quyền."},
   {"anchor":"ADMIN.NAV.PARKING","placement":"RIGHT","title":"6. Chuẩn bị thẻ, thiết bị và thanh toán","body":"Chuẩn bị thẻ đúng bãi, camera hoặc bằng chứng hình ảnh, thiết bị làn và phương thức thanh toán được hỗ trợ. Vé đăng ký hoặc đặt chỗ chỉ cần cấu hình khi bãi cung cấp dịch vụ đó."},
   {"anchor":"ADMIN.NAV.PARKING","placement":"RIGHT","title":"7. Kiểm tra readiness và kích hoạt","body":"Đánh giá riêng khả năng check-in, tìm phiên, chuẩn bị checkout, thu tiền và hoàn tất checkout. Nếu thiếu dependency, hoàn thành hoặc giao đúng người có quyền rồi kiểm tra lại."},
   {"anchor":"ADMIN.NAV.OPERATIONS","placement":"RIGHT","title":"8. Thực hiện xe vào","body":"Chọn đúng bãi và làn IN; quét thẻ hoặc QR; chọn loại xe; chụp bằng chứng; kiểm tra OCR, sức chứa hoặc vị trí; chỉ xác nhận khi preview hợp lệ."},
   {"anchor":"ADMIN.NAV.OPERATIONS","placement":"RIGHT","title":"9. Thực hiện xe ra trong mọi nhánh","body":"Tìm phiên đang mở, chụp ảnh xe ra, đối chiếu biển số, tính phí và xử lý thanh toán nếu cần rồi mới hoàn tất checkout. Mất thẻ, lệch biển số, payment pending hoặc QR sai phải đi đúng luồng ngoại lệ, không bypass."}
 ]'::jsonb),
('POST_ONBOARDING_PARKING_MANAGER',
 'Lộ trình bàn giao và vận hành bãi xe',
 'Hoàn thiện phần được phân quyền tại bãi được giao trước khi vận hành xe vào/ra.',
 'PERMISSION_ALL', ARRAY['PARKING_LOT_READ_ALL','PARKING_SPACE_CONFIGURE_ALL'], 2,
 '[
   {"anchor":"ADMIN.HELP.LAUNCHER","placement":"BOTTOM","title":"Mở lại hướng dẫn","body":"Dùng dấu hỏi để mở lại hướng dẫn màn hình hoặc lộ trình bàn giao. Tiến độ xem guide tách biệt với trạng thái cấu hình thật của bãi."},
   {"anchor":"ADMIN.NAV.PARKING","placement":"RIGHT","title":"1. Kiểm tra phân công và bàn giao","body":"Chỉ làm việc trên bãi được phân công và trong thời gian hiệu lực. Đọc phần Partner đã hoàn thành, phần còn thiếu và phần phải chờ người có quyền."},
   {"anchor":"ADMIN.NAV.PARKING","placement":"RIGHT","title":"2. Hoàn thiện cấu trúc được phép","body":"Kiểm tra tầng, zone, cổng, làn IN/OUT, sức chứa, layout và thiết bị. Nếu catalog hoặc giá chưa được công bố và bạn không có quyền, chuyển blocker cho người có quyền thay vì sửa vượt scope."},
   {"anchor":"ADMIN.NAV.CATALOG","placement":"RIGHT","title":"3. Kiểm tra dịch vụ tại bãi","body":"Đối chiếu loại xe, loại vé và dịch vụ được bật tại bãi. Quyền xem catalog không đồng nghĩa quyền sửa hoặc kích hoạt catalog chung của organization."},
   {"anchor":"ADMIN.NAV.WORKFORCE","placement":"RIGHT","title":"4. Chuẩn bị nhân sự và ca","body":"Gán nhân viên hoặc ca khi có quyền; xác nhận người vận hành có phân công hiệu lực, đúng bãi và đủ quyền check-in, check-out, thanh toán riêng biệt."},
   {"anchor":"ADMIN.NAV.PARKING","placement":"RIGHT","title":"5. Gửi yêu cầu hoặc kích hoạt bãi","body":"Nếu có quyền cập nhật, kích hoạt sau khi readiness đạt. Nếu chỉ được yêu cầu kích hoạt, gửi yêu cầu và chờ người có quyền; không tạo thao tác kích hoạt giả."},
   {"anchor":"ADMIN.NAV.OPERATIONS","placement":"RIGHT","title":"6. Vận hành và xử lý ngoại lệ","body":"Xe vào cần làn IN, thẻ hoặc QR, biển số, ảnh và sức chứa. Xe ra cần phiên mở, đối chiếu, phí và payment hợp lệ. Chuyển mất thẻ, override biển số hoặc ngoại lệ thanh toán cho đúng capability."}
 ]'::jsonb),
('POST_ONBOARDING_EMPLOYEE_OPERATIONS',
 'Lộ trình chuẩn bị ca và vận hành xe vào/ra',
 'Kiểm tra phân công, ca, bãi và capability riêng cho từng thao tác vận hành.',
 'PERMISSION_ANY', ARRAY['PARKING_SESSION_CHECK_IN_ALL','PARKING_SESSION_CHECK_OUT_ALL'], 3,
 '[
   {"anchor":"ADMIN.HELP.LAUNCHER","placement":"BOTTOM","title":"Mở lại hướng dẫn","body":"Dùng dấu hỏi để xem lại hướng dẫn. Hệ thống chọn theo effective permission; tên role không tự cấp khả năng thao tác."},
   {"anchor":"ADMIN.NAV.WORKFORCE","placement":"RIGHT","title":"1. Kiểm tra phân công và ca","body":"Account, employee và onboarding phải hợp lệ; bãi được phân công phải đúng scope và còn hiệu lực. Mở ca nếu policy của thao tác yêu cầu."},
   {"anchor":"ADMIN.NAV.PARKING","placement":"RIGHT","title":"2. Kiểm tra điều kiện bãi","body":"Xác nhận bãi, làn, camera, thẻ hoặc QR, sức chứa và dịch vụ đang hoạt động. Việc bạn có permission thì có thể bổ sung; việc ngoài quyền phải chờ người có quyền."},
   {"anchor":"ADMIN.NAV.OPERATIONS","placement":"RIGHT","title":"3. Check-in đúng nhánh","body":"Vãng lai dùng thẻ khả dụng đúng bãi; vé đăng ký hoặc đặt chỗ dùng thẻ/QR hợp lệ. Chọn làn IN, loại xe, chụp ảnh, đối chiếu OCR và chỉ xác nhận khi preview đạt."},
   {"anchor":"ADMIN.NAV.OPERATIONS","placement":"RIGHT","title":"4. Check-out và thanh toán","body":"Tìm đúng phiên mở, chọn làn OUT, chụp ảnh, đối chiếu biển số và phí. Có quyền check-out không mặc nhiên có quyền thu tiền; payment pending hoặc thất bại phải được xử lý trước khi hoàn tất."},
   {"anchor":"ADMIN.NAV.OPERATIONS","placement":"RIGHT","title":"5. Xử lý trường hợp ngoại lệ","body":"Không bypass khi mất thẻ, QR sai, biển số lệch, thiếu ảnh, không tìm thấy phiên hoặc subscription/reservation thay đổi. Chỉ dùng review/override khi effective permission và scope thực sự cho phép."}
 ]'::jsonb);

INSERT INTO notification.user_guide_features (feature_id, code, name, description, enabled)
VALUES (
    md5('vehicle-guide-feature:OPERATIONS_ONBOARDING')::uuid,
    'OPERATIONS_ONBOARDING',
    'Lộ trình sau onboarding',
    'Hướng dẫn theo capability từ thiết lập bãi đến check-in/check-out.',
    true
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO notification.user_guides (
    guide_id, feature_id, code, status, display_type, replay_policy, context_key,
    priority, display_order, start_at, current_version
)
SELECT md5('vehicle-guide-seed:' || seed.guide_code)::uuid,
       feature.feature_id,
       seed.guide_code,
       'PUBLISHED',
       'COACH_MARK',
       'MANUAL_ONLY',
       '/ADMIN/HEADER',
       seed.priority,
       seed.priority,
       TIMESTAMPTZ '2026-01-01 00:00:00+07',
       1
FROM post_onboarding_guide_seed seed
JOIN notification.user_guide_features feature ON feature.code = 'OPERATIONS_ONBOARDING'
ON CONFLICT (code) DO NOTHING;

INSERT INTO notification.user_guide_versions (
    guide_version_id, guide_id, version_number, title, summary, deep_link,
    audience_mode, published_at
)
SELECT md5('vehicle-guide-version:' || seed.guide_code || ':1')::uuid,
       guide.guide_id,
       1,
       seed.title,
       seed.summary,
       '/admin/dashboard',
       seed.audience_mode,
       TIMESTAMPTZ '2026-11-15 00:00:00+07'
FROM post_onboarding_guide_seed seed
JOIN notification.user_guides guide ON guide.code = seed.guide_code
ON CONFLICT (guide_id, version_number) DO NOTHING;

INSERT INTO notification.user_guide_steps (
    step_id, guide_version_id, display_order, title, body, anchor_key, placement, collapse_ms
)
SELECT md5('vehicle-guide-step:' || seed.guide_code || ':' || step.ordinality)::uuid,
       version.guide_version_id,
       step.ordinality - 1,
       step.value ->> 'title',
       step.value ->> 'body',
       step.value ->> 'anchor',
       COALESCE(step.value ->> 'placement', 'RIGHT'),
       0
FROM post_onboarding_guide_seed seed
JOIN notification.user_guides guide ON guide.code = seed.guide_code
JOIN notification.user_guide_versions version
  ON version.guide_id = guide.guide_id AND version.version_number = 1
CROSS JOIN LATERAL jsonb_array_elements(seed.steps) WITH ORDINALITY AS step(value, ordinality)
ON CONFLICT (guide_version_id, display_order) DO NOTHING;

INSERT INTO notification.user_guide_audiences (
    audience_id, guide_version_id, target_type, permission_id
)
SELECT md5('vehicle-guide-audience:' || seed.guide_code || ':' || required.permission_code)::uuid,
       version.guide_version_id,
       'PERMISSION',
       permission.permission_id
FROM post_onboarding_guide_seed seed
JOIN notification.user_guides guide ON guide.code = seed.guide_code
JOIN notification.user_guide_versions version
  ON version.guide_id = guide.guide_id AND version.version_number = 1
CROSS JOIN LATERAL unnest(seed.permission_codes) AS required(permission_code)
JOIN iam.permissions permission ON permission.permission_code = required.permission_code
ON CONFLICT DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM post_onboarding_guide_seed seed
        CROSS JOIN LATERAL unnest(seed.permission_codes) AS required(permission_code)
        LEFT JOIN iam.permissions permission ON permission.permission_code = required.permission_code
        WHERE permission.permission_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Post-onboarding guide references an unknown permission code';
    END IF;
END $$;
