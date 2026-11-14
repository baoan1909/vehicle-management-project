-- Reference drafts for the business workflow guides wired into the frontend.
-- They intentionally remain DRAFT/current_version=0. A System Admin must review
-- and publish version 1 before the runtime resolver can serve them to users.
-- Deterministic IDs ensure that rerunning the seed cannot overwrite a guide
-- already created by a System Admin with the same business code.

CREATE TEMP TABLE business_user_guide_seed (
    feature_code VARCHAR(80) NOT NULL,
    feature_name VARCHAR(150) NOT NULL,
    guide_code VARCHAR(120) NOT NULL,
    context_key VARCHAR(120) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    deep_link VARCHAR(1000) NOT NULL,
    display_type VARCHAR(40) NOT NULL,
    replay_policy VARCHAR(40) NOT NULL,
    priority INTEGER NOT NULL,
    audience_mode VARCHAR(30) NOT NULL,
    permission_codes TEXT[] NOT NULL,
    steps JSONB NOT NULL
) ON COMMIT DROP;

INSERT INTO business_user_guide_seed VALUES
('PARKING_SESSION', 'Vận hành xe vào và xe ra', 'PARKING_CHECK_IN_FIRST_RUN', '/ADMIN/SWIPE',
 'Hướng dẫn tiếp nhận xe vào', 'Quét thẻ, chụp bằng chứng và xác nhận xe vào đúng làn.', '/admin/swipe?mode=check-in',
 'COACH_MARK', 'ONCE_PER_VERSION', 10, 'PERMISSION_ANY', ARRAY['PARKING_SESSION_CHECK_IN_ALL'],
 '[
   {"anchor":"PARKING_OPERATION.MODE","title":"Chọn chế độ xe vào","body":"Kiểm tra màn hình đang ở chế độ Xe vào."},
   {"anchor":"PARKING_OPERATION.LOT","title":"Chọn bãi xe","body":"Chọn đúng bãi đang vận hành."},
   {"anchor":"PARKING_OPERATION.LANE","title":"Chọn làn vào","body":"Chọn làn vào đang hoạt động."},
   {"anchor":"PARKING_OPERATION.CAMERA","title":"Chọn camera","body":"Chọn camera của làn để lấy bằng chứng."},
   {"anchor":"PARKING_OPERATION.CARD_OR_QR","title":"Quét thẻ hoặc QR","body":"Quét thẻ khách vãng lai hoặc QR vé đăng ký."},
   {"anchor":"PARKING_OPERATION.VEHICLE_TYPE","title":"Loại phương tiện","body":"Đối chiếu và chọn đúng loại phương tiện."},
   {"anchor":"PARKING_OPERATION.CAPTURE","title":"Chụp bằng chứng","body":"Chụp đủ biển số, phương tiện và người điều khiển."},
   {"anchor":"PARKING_OPERATION.OCR_RESULT","title":"Kiểm tra OCR","body":"Đối chiếu biển số nhận dạng và sửa nếu cần."},
   {"anchor":"PARKING_OPERATION.LOCATION_PREVIEW","title":"Vị trí dự kiến","body":"Kiểm tra vị trí hệ thống đang preview hoặc giữ chỗ."},
   {"anchor":"PARKING_OPERATION.CONFIRM","title":"Xác nhận xe vào","body":"Tự kiểm tra lại dữ liệu trước khi xác nhận xe vào."}
 ]'::jsonb),
('PARKING_SESSION', 'Vận hành xe vào và xe ra', 'PARKING_CHECK_OUT_FIRST_RUN', '/ADMIN/SWIPE',
 'Hướng dẫn xử lý xe ra', 'Tìm phiên gửi xe, đối chiếu biển số và hoàn tất thanh toán.', '/admin/swipe?mode=check-out',
 'COACH_MARK', 'ONCE_PER_VERSION', 11, 'PERMISSION_ANY', ARRAY['PARKING_SESSION_CHECK_OUT_ALL'],
 '[
   {"anchor":"PARKING_OPERATION.MODE","title":"Chọn chế độ xe ra","body":"Chuyển sang chế độ Xe ra."},
   {"anchor":"PARKING_OPERATION.LOT","title":"Chọn bãi xe","body":"Chọn đúng bãi đang xử lý xe ra."},
   {"anchor":"PARKING_OPERATION.LANE","title":"Chọn làn ra","body":"Chọn làn ra đang hoạt động."},
   {"anchor":"PARKING_OPERATION.CARD_OR_QR","title":"Tìm phiên gửi xe","body":"Quét thẻ hoặc QR để tải phiên đang mở."},
   {"anchor":"PARKING_OPERATION.CHECKOUT_SUMMARY","title":"Thông tin phiên gửi","body":"Kiểm tra xe, thời gian gửi và trạng thái phiên."},
   {"anchor":"PARKING_OPERATION.CAPTURE","title":"Chụp ảnh xe ra","body":"Chụp bằng chứng xe ra và nhận dạng biển số."},
   {"anchor":"PARKING_OPERATION.PLATE_COMPARISON","title":"Đối chiếu biển số","body":"Xử lý cảnh báo nếu biển số vào và ra không khớp."},
   {"anchor":"PARKING_OPERATION.PRICE","title":"Kiểm tra phí","body":"Kiểm tra thời gian gửi, loại vé và số tiền cần thu."},
   {"anchor":"PARKING_OPERATION.PAYMENT_METHOD","title":"Chọn thanh toán","body":"Khách vãng lai cần chọn tiền mặt hoặc VNPay."},
   {"anchor":"PARKING_OPERATION.PAYMENT_ACTION","title":"Hoàn tất thanh toán","body":"Chỉ ghi nhận sau khi giao dịch đã được xác nhận."},
   {"anchor":"PARKING_OPERATION.CHECKOUT_CONFIRM","title":"Xác nhận xe ra","body":"Tự kiểm tra trạng thái thanh toán trước khi xác nhận xe ra."}
 ]'::jsonb),
('CARD_MANAGEMENT', 'Quản lý thẻ và báo mất thẻ', 'LOST_CARD_REPORT_CREATE', '/ADMIN/LOST/FORM',
 'Tạo phiếu báo mất thẻ', 'Xác minh xe và chủ xe trước khi khóa thẻ cũ.', '/admin/lost/form',
 'COACH_MARK', 'ONCE_PER_VERSION', 20, 'PERMISSION_ANY', ARRAY['LOST_CARD_REPORT_CREATE_ALL'],
 '[
   {"anchor":"LOST_CARD.CREATE.PLATE_LOOKUP","title":"Tìm theo biển số","body":"Nhập biển số để tìm thẻ, phiên gửi và chủ xe."},
   {"anchor":"LOST_CARD.CREATE.PREVIEW","title":"Kiểm tra kết quả","body":"Đối chiếu dữ liệu hệ thống tìm thấy."},
   {"anchor":"LOST_CARD.CREATE.LOST_TIME","title":"Thời điểm mất","body":"Ghi nhận thời điểm người dùng phát hiện mất thẻ."},
   {"anchor":"LOST_CARD.CREATE.REPORTER","title":"Người báo mất","body":"Nhập người báo mất và thông tin liên hệ."},
   {"anchor":"LOST_CARD.CREATE.OWNER_EVIDENCE","title":"Giấy tờ xác minh","body":"Nhập giấy tờ cần thiết để xác minh quyền sở hữu."},
   {"anchor":"LOST_CARD.CREATE.REVIEW","title":"Kiểm tra phiếu","body":"Rà soát lại toàn bộ dữ liệu trước khi gửi."},
   {"anchor":"LOST_CARD.CREATE.SUBMIT","title":"Tạo phiếu và khóa thẻ","body":"Thẻ cũ sẽ bị khóa. Chỉ xác nhận khi đã hoàn tất xác minh."}
 ]'::jsonb),
('CARD_MANAGEMENT', 'Quản lý thẻ và báo mất thẻ', 'LOST_CARD_PAYMENT', '/ADMIN/LOST/DETAIL',
 'Thanh toán phí mất thẻ', 'Kiểm tra hóa đơn và ghi nhận phương thức thanh toán.', '/admin/lost/detail',
 'COACH_MARK', 'ONCE_PER_VERSION', 21, 'PERMISSION_ALL', ARRAY['LOST_CARD_REPORT_UPDATE_ALL','PAYMENT_CREATE_ALL'],
 '[
   {"anchor":"LOST_CARD.DETAIL.INVOICE","title":"Hóa đơn mất thẻ","body":"Kiểm tra số tiền và trạng thái hóa đơn."},
   {"anchor":"LOST_CARD.DETAIL.PAYMENT_METHOD","title":"Phương thức thanh toán","body":"Chọn phương thức khách hàng thực tế sử dụng."},
   {"anchor":"LOST_CARD.DETAIL.PAY_CASH","title":"Xác nhận tiền mặt","body":"Chỉ ghi nhận sau khi đã nhận đủ tiền."},
   {"anchor":"LOST_CARD.DETAIL.PAY_VNPAY","title":"Thanh toán VNPay","body":"Chờ VNPay trả về trạng thái thành công trước khi tiếp tục."}
 ]'::jsonb),
('CARD_MANAGEMENT', 'Quản lý thẻ và báo mất thẻ', 'LOST_CARD_REPLACEMENT', '/ADMIN/LOST/DETAIL',
 'Cấp thẻ thay thế', 'Chọn thẻ khả dụng đúng bãi và hoàn tất phiếu.', '/admin/lost/detail',
 'COACH_MARK', 'ONCE_PER_VERSION', 22, 'PERMISSION_ANY', ARRAY['LOST_CARD_REPORT_UPDATE_ALL'],
 '[
   {"anchor":"LOST_CARD.DETAIL.REPLACEMENT_CARD","title":"Chọn thẻ thay thế","body":"Chỉ chọn thẻ AVAILABLE, đúng bãi và đúng loại."},
   {"anchor":"LOST_CARD.DETAIL.RESOLVE","title":"Hoàn tất xử lý","body":"Tự xác nhận sau khi hóa đơn đã thanh toán và thẻ thay thế hợp lệ."},
   {"anchor":"LOST_CARD.DETAIL.CANCEL","title":"Hủy phiếu","body":"Chỉ hủy khi lý do nghiệp vụ đã được xác minh."}
 ]'::jsonb),
('SUBSCRIPTION_APPROVAL', 'Duyệt vé đăng ký', 'SUBSCRIPTION_REVIEW_PENDING', '/ADMIN/SUBSCRIPTION-APPROVALS',
 'Duyệt yêu cầu vé đăng ký', 'Kiểm tra hồ sơ trước khi duyệt hoặc từ chối.', '/admin/subscription-approvals',
 'COACH_MARK', 'ONCE_PER_VERSION', 30, 'PERMISSION_ANY', ARRAY['SUBSCRIPTION_APPROVE_ALL','SUBSCRIPTION_REJECT_ALL'],
 '[
   {"anchor":"SUBSCRIPTION.APPROVAL.STATUS_PIPELINE","title":"Trạng thái xử lý","body":"Chọn yêu cầu đang ở trạng thái chờ duyệt."},
   {"anchor":"SUBSCRIPTION.APPROVAL.DETAIL_DRAWER","title":"Kiểm tra hồ sơ","body":"Đối chiếu khách hàng, xe, bãi và gói đăng ký."},
   {"anchor":"SUBSCRIPTION.APPROVAL.REQUEST_CARD","title":"Yêu cầu thẻ vật lý","body":"Kiểm tra yêu cầu nhận thẻ và điều kiện cấp thẻ."},
   {"anchor":"SUBSCRIPTION.APPROVAL.REJECT_REASON","title":"Lý do từ chối","body":"Nêu lý do rõ ràng khi hồ sơ không hợp lệ."},
   {"anchor":"SUBSCRIPTION.APPROVAL.REJECT","title":"Từ chối","body":"Tự xác nhận từ chối sau khi kiểm tra lý do."},
   {"anchor":"SUBSCRIPTION.APPROVAL.APPROVE","title":"Duyệt yêu cầu","body":"Tự xác nhận duyệt; trạng thái tiếp theo phụ thuộc yêu cầu thanh toán."}
 ]'::jsonb),
('SUBSCRIPTION_APPROVAL', 'Duyệt vé đăng ký', 'SUBSCRIPTION_PAYMENT_COUNTER', '/ADMIN/SUBSCRIPTION-APPROVALS',
 'Ghi nhận thanh toán tại quầy', 'Thanh toán yêu cầu đang chờ và theo dõi trạng thái tiếp theo.', '/admin/subscription-approvals',
 'COACH_MARK', 'ONCE_PER_VERSION', 31, 'PERMISSION_ANY', ARRAY['PAYMENT_CREATE_ALL'],
 '[
   {"anchor":"SUBSCRIPTION.APPROVAL.STATUS_PIPELINE","title":"Yêu cầu chờ thanh toán","body":"Chỉ xử lý yêu cầu đang ở PENDING_PAYMENT."},
   {"anchor":"SUBSCRIPTION.APPROVAL.DETAIL_DRAWER","title":"Kiểm tra hóa đơn","body":"Đối chiếu yêu cầu và số tiền cần thu."},
   {"anchor":"SUBSCRIPTION.APPROVAL.CASH_PAYMENT","title":"Ghi nhận tiền mặt","body":"Chỉ xác nhận khi đã nhận đủ tiền tại quầy."}
 ]'::jsonb),
('SUBSCRIPTION_APPROVAL', 'Duyệt vé đăng ký', 'SUBSCRIPTION_CARD_FULFILLMENT', '/ADMIN/SUBSCRIPTION-APPROVALS',
 'Gán thẻ vật lý cho vé đăng ký', 'Hoàn tất cấp thẻ đã được hệ thống giữ trước.', '/admin/subscription-approvals',
 'COACH_MARK', 'ONCE_PER_VERSION', 32, 'PERMISSION_ANY', ARRAY['SUBSCRIPTION_ASSIGN_CARD_ALL'],
 '[
   {"anchor":"SUBSCRIPTION.APPROVAL.DETAIL_DRAWER","title":"Kiểm tra vé đã kích hoạt","body":"Chỉ cấp thẻ khi yêu cầu đã đủ điều kiện."},
   {"anchor":"SUBSCRIPTION.APPROVAL.ASSIGN_CARD","title":"Gán thẻ đã giữ","body":"Đối chiếu thẻ hệ thống đã reserve trước khi xác nhận."},
   {"anchor":"SUBSCRIPTION.APPROVAL.ACCESS_TOGGLE","title":"Quyền ra vào","body":"Kiểm tra trạng thái quyền truy cập sau khi cấp thẻ."}
 ]'::jsonb),
('PARKING_LOT', 'Thiết lập bãi xe', 'PARKING_LOT_CREATE', '/ADMIN/PARKING-STRUCTURE',
 'Tạo bãi xe ban đầu', 'Khai báo thông tin, mô hình tầng và người quản lý.', '/admin/parking-structure?action=create',
 'COACH_MARK', 'ONCE_PER_VERSION', 40, 'PERMISSION_ANY', ARRAY['PARKING_LOT_CREATE_ALL'],
 '[
   {"anchor":"PARKING_SETUP.CREATE.OPEN","title":"Mở trình tạo bãi","body":"Bắt đầu wizard tạo bãi xe."},
   {"anchor":"PARKING_SETUP.CREATE.INFO","title":"Thông tin bãi","body":"Nhập tên, mã và thông tin vận hành."},
   {"anchor":"PARKING_SETUP.CREATE.ADDRESS","title":"Địa chỉ","body":"Chọn địa chỉ hành chính chính xác."},
   {"anchor":"PARKING_SETUP.CREATE.LOCATION","title":"Vị trí bản đồ","body":"Kiểm tra tọa độ của bãi trên bản đồ."},
   {"anchor":"PARKING_SETUP.CREATE.LEVELS","title":"Mô hình tầng","body":"Chọn bãi mặt đất hoặc tạo các tầng ban đầu."},
   {"anchor":"PARKING_SETUP.CREATE.MANAGER","title":"Phân công quản lý","body":"Chọn Parking Manager nếu tài khoản có quyền phân công."}
 ]'::jsonb),
('PARKING_LOT', 'Thiết lập bãi xe', 'PARKING_STRUCTURE_SETUP', '/ADMIN/PARKING-STRUCTURE',
 'Thiết lập cấu trúc bãi', 'Tạo tầng, khu vực, cổng và các làn vận hành.', '/admin/parking-structure',
 'COACH_MARK', 'ONCE_PER_VERSION', 41, 'PERMISSION_ANY', ARRAY['PARKING_SPACE_CONFIGURE_ALL'],
 '[
   {"anchor":"PARKING_SETUP.STRUCTURE.LEVEL","title":"Tạo tầng","body":"Tạo tầng hoặc mặt bằng trước khi chia khu vực."},
   {"anchor":"PARKING_SETUP.STRUCTURE.ZONE","title":"Tạo khu vực","body":"Tạo zone thuộc đúng tầng."},
   {"anchor":"PARKING_SETUP.STRUCTURE.GATE","title":"Tạo cổng","body":"Khai báo cổng vận hành của bãi."},
   {"anchor":"PARKING_SETUP.STRUCTURE.LANE_IN","title":"Làn vào","body":"Tạo và kích hoạt ít nhất một làn vào."},
   {"anchor":"PARKING_SETUP.STRUCTURE.LANE_OUT","title":"Làn ra","body":"Tạo và kích hoạt ít nhất một làn ra."}
 ]'::jsonb),
('PARKING_LOT', 'Thiết lập bãi xe', 'PARKING_LAYOUT_PUBLISH', '/ADMIN/PARKING-LAYOUTS',
 'Thiết kế và xuất bản sơ đồ', 'Lưu nháp, xem trước và xuất bản layout ô đỗ.', '/admin/parking-layouts',
 'COACH_MARK', 'ONCE_PER_VERSION', 42, 'PERMISSION_ANY', ARRAY['PARKING_LAYOUT_PUBLISH_ALL'],
 '[
   {"anchor":"PARKING_SETUP.LAYOUT.ZONE_SELECTOR","title":"Chọn khu vực","body":"Chọn SPACE zone cần thiết kế."},
   {"anchor":"PARKING_SETUP.LAYOUT.CANVAS","title":"Thiết kế layout","body":"Bố trí các ô đỗ và kiểm tra mã vị trí."},
   {"anchor":"PARKING_SETUP.LAYOUT.SAVE_DRAFT","title":"Lưu bản nháp","body":"Lưu thay đổi để tiếp tục chỉnh sửa sau."},
   {"anchor":"PARKING_SETUP.LAYOUT.PREVIEW","title":"Xem trước","body":"Kiểm tra layout trước khi xuất bản."},
   {"anchor":"PARKING_SETUP.LAYOUT.PUBLISH","title":"Xuất bản","body":"Chỉ người có quyền mới tự xác nhận xuất bản layout."}
 ]'::jsonb),
('PARKING_LOT', 'Thiết lập bãi xe', 'PARKING_LOT_ACTIVATION', '/ADMIN/PARKING-LOTS',
 'Kiểm tra sẵn sàng và kích hoạt bãi', 'Đối chiếu cấu trúc, layout và người phụ trách.', '/admin/parking-lots',
 'COACH_MARK', 'ONCE_PER_VERSION', 43, 'PERMISSION_ANY', ARRAY['PARKING_LOT_UPDATE_ALL'],
 '[
   {"anchor":"PARKING_SETUP.READINESS","title":"Kiểm tra sẵn sàng","body":"Đối chiếu tầng, zone, cổng, làn và layout đã publish."},
   {"anchor":"PARKING_SETUP.MANAGER_ASSIGNMENT","title":"Người phụ trách","body":"Bãi cần có người chịu trách nhiệm vận hành."},
   {"anchor":"PARKING_SETUP.ACTIVATION_MISSING","title":"Hạng mục còn thiếu","body":"Hoàn tất các điều kiện còn thiếu trước khi kích hoạt."},
   {"anchor":"PARKING_SETUP.ACTIVATE","title":"Kích hoạt bãi","body":"Tự xác nhận kích hoạt khi toàn bộ điều kiện đã đạt."},
   {"anchor":"PARKING_SETUP.REQUEST_ACTIVATION","title":"Gửi yêu cầu kích hoạt","body":"Gửi yêu cầu nếu vai trò hiện tại không được kích hoạt trực tiếp."}
 ]'::jsonb),
('PRICING', 'Cấu hình giá', 'PRICE_PLAN_SETUP', '/ADMIN/PRICE-PLANS',
 'Thiết lập kế hoạch giá', 'Khai báo đối tượng và khoảng hiệu lực của kế hoạch.', '/admin/price-plans',
 'COACH_MARK', 'ONCE_PER_VERSION', 50, 'PERMISSION_ANY', ARRAY['PRICE_PLAN_CREATE_ALL','PRICE_PLAN_UPDATE_ALL','PRICE_PLAN_DELETE_ALL'],
 '[
   {"anchor":"PRICING.PLAN.APPLIES_TO","title":"Đối tượng áp dụng","body":"VISITOR dành cho khách vãng lai; CUSTOMER dành cho vé đăng ký."},
   {"anchor":"PRICING.PLAN.CODE","title":"Mã kế hoạch","body":"Mã kế hoạch phải duy nhất."},
   {"anchor":"PRICING.PLAN.NAME","title":"Tên kế hoạch","body":"Đặt tên giúp nhận biết phạm vi áp dụng."},
   {"anchor":"PRICING.PLAN.EFFECTIVE_FROM","title":"Ngày bắt đầu","body":"Chọn ngày kế hoạch bắt đầu có hiệu lực."},
   {"anchor":"PRICING.PLAN.EFFECTIVE_TO","title":"Ngày kết thúc","body":"Ngày kết thúc không được trước ngày bắt đầu."},
   {"anchor":"PRICING.PLAN.PREVIEW","title":"Kiểm tra hiệu lực","body":"Các kế hoạch active cùng loại không được chồng khoảng hiệu lực."},
   {"anchor":"PRICING.PLAN.SUBMIT","title":"Lưu kế hoạch","body":"Tự kiểm tra và xác nhận tạo hoặc cập nhật."},
   {"anchor":"PRICING.PLAN.STATUS","title":"Thay đổi trạng thái","body":"Đánh giá ảnh hưởng trước khi kích hoạt hoặc ngừng kế hoạch."}
 ]'::jsonb),
('PRICING', 'Cấu hình giá', 'PRICE_RULE_SETUP', '/ADMIN/PRICE-RULES',
 'Thiết lập quy tắc giá', 'Cấu hình giá theo kế hoạch, loại xe và loại vé.', '/admin/price-rules',
 'COACH_MARK', 'ONCE_PER_VERSION', 51, 'PERMISSION_ANY', ARRAY['PRICE_RULE_CREATE_ALL','PRICE_RULE_UPDATE_ALL','PRICE_RULE_DELETE_ALL'],
 '[
   {"anchor":"PRICING.RULE.PLAN","title":"Chọn kế hoạch","body":"Chọn kế hoạch đúng nhóm VISITOR hoặc CUSTOMER."},
   {"anchor":"PRICING.RULE.VEHICLE_TYPE","title":"Loại xe","body":"Chọn loại xe được áp dụng mức giá."},
   {"anchor":"PRICING.RULE.TICKET_TYPE","title":"Loại vé","body":"VISITOR chỉ dùng DAILY; CUSTOMER dùng vé kỳ hạn hoặc FREE."},
   {"anchor":"PRICING.RULE.BASE_PRICE","title":"Giá cơ bản","body":"Giá phải là số không âm."},
   {"anchor":"PRICING.RULE.LOST_CARD_FEE","title":"Phí mất thẻ","body":"Phí mất thẻ phải là số không âm."},
   {"anchor":"PRICING.RULE.UNIT","title":"Đơn vị tính","body":"VISITOR dùng TURN hoặc DAY; CUSTOMER dùng MONTH."},
   {"anchor":"PRICING.RULE.TIME_RANGE","title":"Khung giờ","body":"Quy tắc VISITOR cần khung giờ active không chồng lấn."},
   {"anchor":"PRICING.RULE.PRIORITY","title":"Độ ưu tiên","body":"Priority phải không âm và dùng để phân giải quy tắc."},
   {"anchor":"PRICING.RULE.VALIDATION","title":"Kiểm tra tổ hợp","body":"CUSTOMER không được trùng kế hoạch, loại xe và loại vé."},
   {"anchor":"PRICING.RULE.SUBMIT","title":"Lưu quy tắc","body":"Tự xác nhận sau khi kiểm tra toàn bộ điều kiện."}
 ]'::jsonb),
('PRICING', 'Cấu hình giá', 'CATALOG_VEHICLE_TYPE_SETUP', '/ADMIN/VEHICLE',
 'Cấu hình loại phương tiện', 'Tạo loại xe và kiểm tra phạm vi áp dụng tại bãi.', '/admin/vehicle',
 'COACH_MARK', 'ONCE_PER_VERSION', 52, 'PERMISSION_ANY', ARRAY['VEHICLE_TYPE_CREATE_ALL','VEHICLE_TYPE_UPDATE_ALL'],
 '[
   {"anchor":"CATALOG_VEHICLE.HEADER","title":"Tạo loại xe","body":"Nút tạo chỉ hiển thị khi có quyền và đã chọn bãi."},
   {"anchor":"CATALOG_VEHICLE.LOT_SCOPE","title":"Chọn bãi áp dụng","body":"Kiểm tra Partner và bãi đang cấu hình."},
   {"anchor":"CATALOG_VEHICLE.LIST","title":"Danh sách loại xe","body":"Theo dõi trạng thái phục vụ tại bãi."},
   {"anchor":"CATALOG_VEHICLE.DETAIL","title":"Kiểm tra ảnh hưởng","body":"Xem các price rule liên quan trước khi ngừng phục vụ."}
 ]'::jsonb),
('PRICING', 'Cấu hình giá', 'CATALOG_TICKET_TYPE_SETUP', '/ADMIN/TICKET',
 'Cấu hình loại vé', 'Tạo loại vé và kiểm tra quy tắc giá liên quan.', '/admin/ticket',
 'COACH_MARK', 'ONCE_PER_VERSION', 53, 'PERMISSION_ANY', ARRAY['TICKET_TYPE_CREATE_ALL','TICKET_TYPE_UPDATE_ALL'],
 '[
   {"anchor":"CATALOG_TICKET.HEADER","title":"Tạo loại vé","body":"Nút tạo chỉ hiển thị khi có quyền và đã chọn bãi."},
   {"anchor":"CATALOG_TICKET.LOT_SCOPE","title":"Chọn bãi áp dụng","body":"Kiểm tra bãi đang cấu hình loại vé."},
   {"anchor":"CATALOG_TICKET.LIST","title":"Danh sách loại vé","body":"Theo dõi loại vé đang hoạt động."},
   {"anchor":"CATALOG_TICKET.DETAIL","title":"Kiểm tra price rule","body":"Xem ảnh hưởng đến quy tắc giá trước khi ngừng dùng."}
 ]'::jsonb);

INSERT INTO notification.user_guide_features (feature_id, code, name, description, enabled)
SELECT md5('vehicle-guide-feature:' || seed.feature_code)::uuid,
       seed.feature_code,
       min(seed.feature_name),
       'Nhóm hướng dẫn nghiệp vụ được tích hợp với giao diện Vehicle Management.',
       true
FROM business_user_guide_seed seed
GROUP BY seed.feature_code
ON CONFLICT (code) DO NOTHING;

INSERT INTO notification.user_guides (
    guide_id, feature_id, code, status, display_type, replay_policy, context_key,
    priority, display_order, start_at, current_version
)
SELECT md5('vehicle-guide-seed:' || seed.guide_code)::uuid,
       feature.feature_id,
       seed.guide_code,
       'DRAFT',
       seed.display_type,
       seed.replay_policy,
       seed.context_key,
       seed.priority,
       seed.priority,
       TIMESTAMPTZ '2026-01-01 00:00:00+07',
       0
FROM business_user_guide_seed seed
JOIN notification.user_guide_features feature ON feature.code = seed.feature_code
ON CONFLICT (code) DO NOTHING;

INSERT INTO notification.user_guide_versions (
    guide_version_id, guide_id, version_number, title, summary, deep_link, audience_mode
)
SELECT md5('vehicle-guide-version:' || seed.guide_code || ':1')::uuid,
       guide.guide_id,
       1,
       seed.title,
       seed.summary,
       seed.deep_link,
       seed.audience_mode
FROM business_user_guide_seed seed
JOIN notification.user_guides guide
  ON guide.guide_id = md5('vehicle-guide-seed:' || seed.guide_code)::uuid
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
       'BOTTOM',
       COALESCE((step.value ->> 'collapseMs')::integer, 0)
FROM business_user_guide_seed seed
JOIN notification.user_guide_versions version
  ON version.guide_version_id = md5('vehicle-guide-version:' || seed.guide_code || ':1')::uuid
CROSS JOIN LATERAL jsonb_array_elements(seed.steps) WITH ORDINALITY AS step(value, ordinality)
ON CONFLICT (guide_version_id, display_order) DO NOTHING;

INSERT INTO notification.user_guide_audiences (
    audience_id, guide_version_id, target_type, permission_id
)
SELECT md5('vehicle-guide-audience:' || seed.guide_code || ':' || permission_codes.permission_code)::uuid,
       version.guide_version_id,
       'PERMISSION',
       permission.permission_id
FROM business_user_guide_seed seed
JOIN notification.user_guide_versions version
  ON version.guide_version_id = md5('vehicle-guide-version:' || seed.guide_code || ':1')::uuid
CROSS JOIN LATERAL unnest(seed.permission_codes) AS permission_codes(permission_code)
JOIN iam.permissions permission ON permission.permission_code = permission_codes.permission_code
ON CONFLICT DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM business_user_guide_seed seed
        CROSS JOIN LATERAL unnest(seed.permission_codes) AS required_permissions(permission_code)
        LEFT JOIN iam.permissions permission ON permission.permission_code = required_permissions.permission_code
        WHERE permission.permission_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Business user guide seed references an unknown permission code';
    END IF;
END $$;
