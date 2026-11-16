# Rà soát độ phủ User Guide và Coach mark — 15/11/2026

## Kết quả trước khi bổ sung

Hệ thống hiện có 16 workflow guide trong V20261113120000 (seed DRAFT) và ba guide tổng quan đã phát hành trong V20261115110000. Nội dung tổng quan đã mô tả hành trình nhưng chưa đủ hướng dẫn tại từng workspace. Đã kiểm tra lại các anchor mà hai seed này tham chiếu: đều có khai báo trong frontend, gồm cả anchor truyền động qua props; không kết luận thiếu anchor chỉ từ việc không thấy lời gọi userGuideAnchorProps trực tiếp.

Những thiếu hụt chính:

| Phần luồng | Hiện trạng | Bổ sung |
| --- | --- | --- |
| Tạo bãi và kích hoạt | Có walkthrough bản nháp và tổng quan, chưa có nội dung manual hoàn chỉnh để bàn giao theo readiness | WORKFLOW_ACTIVATION; giải thích kích hoạt trực tiếp, gửi yêu cầu, SETUP/ACTIVE và địa giới |
| Cấu trúc và layout | Có coach mark draft theo node/form, thiếu tuyến manual theo thứ tự dependency | WORKFLOW_STRUCTURE_SETUP và PARKING_LAYOUT_SETUP; xe công bố → khu → cổng → IN/OUT; SPACE cần publish, CAPACITY bỏ qua |
| Bốn danh mục mẫu | Nội dung cũ thiên về tạo/sửa, chưa giải thích đủ DRAFT và áp dụng dịch vụ riêng tại bãi | WORKFLOW_VEHICLE_DRAFT, WORKFLOW_TICKET_DRAFT, WORKFLOW_PLAN_DRAFT, WORKFLOW_RULE_DRAFT |
| Kho thẻ | Chưa có coach mark workspace | WORKFLOW_CARD_STOCK; tạo stock khác gán thẻ, AVAILABLE/đúng bãi/đúng loại |
| Thiết bị và bằng chứng | Chưa có coach mark workspace | WORKFLOW_DEVICE_SETUP; cấu hình ACTIVE khác ảnh/live view thực tế |
| Nhân sự và phân công | Tổng quan chưa chỉ rõ luồng bàn giao | WORKFLOW_STAFF_ASSIGNMENT; scope, hiệu lực, quyền cấu hình khác quyền vận hành |
| Lịch ca quản trị và ca của tôi | Chưa có coach mark tương ứng | WORKFLOW_SHIFT_PLANNING và WORKFLOW_OWN_SHIFT; tách quyền đọc, lập lịch, mở và đóng ca |
| Tra cứu phiên | Chưa có coach mark riêng | WORKFLOW_SESSION_LOOKUP; tìm phiên khác quyền checkout/payment |
| Vãng lai vào/ra | Có bản nháp theo form; cần tuyến manual theo toàn bộ giao dịch | WORKFLOW_VISITOR_ENTRY và WORKFLOW_CHECKOUT; preview, prepare, payment và complete riêng |
| Subscription/QR | Có draft duyệt/payment/cấp thẻ, chưa có tuyến manual liên kết nghĩa vụ vé với vào/ra | WORKFLOW_SUBSCRIPTION_FULFILLMENT và WORKFLOW_SUBSCRIPTION_QR |
| Reservation/QR | Chưa có tuyến coach mark vận hành riêng | WORKFLOW_RESERVATION_QR; HELD/đã trả/chưa hết hạn, chỗ giữ và đối trừ khi ra |
| Ngoại lệ và retry | Nội dung phân tán | WORKFLOW_OPERATION_RECOVERY; thẻ/QR, thiếu ảnh, OCR, scope/ca, sức chứa, payment pending/thất bại và retry |

Tổng cộng bổ sung **19 guide / 79 bước**. Nội dung mất thẻ chi tiết (lập phiếu, thu phí, thẻ thay thế) đã có trong seed cũ; giữ nguyên quy trình đó, chỉ bổ sung điểm chuyển giao từ hướng dẫn vận hành.

## Implementation

- Migration mới: V20261115120000__seed_workflow_coach_mark_coverage.sql. Chỉ thêm dữ liệu notification User Guide vào các bảng feature/guide/version/step/audience hiện hữu.
- Các guide mới PUBLISHED, COACH_MARK, MANUAL_ONLY, phiên bản 1; ngày hiệu lực 15/11/2026. Đây là nội dung hướng dẫn đọc thủ công, không tạo bãi, công bố catalog, tạo thẻ, mở ca, thu tiền hay xác nhận xe vào/ra.
- Dùng code và UUID riêng cho phần bổ sung. Không sửa migration cũ, không tự publish 16 draft cũ, không ghi đè code trùng hoặc nội dung/version mà quản trị đã chỉnh. Không thay đổi user progress, role_permissions hay quyền nghiệp vụ.
- Audience dựa vào permission hiện có; guide cấu trúc cần cả SPACE_CONFIGURE và TOPOLOGY_CONFIGURE. Guide xem kho/thiết bị/ca/danh mục dùng quyền đọc phù hợp; nội dung nói rõ action và scope riêng khi ghi. Xem nội dung không chứng minh account được thao tác mọi phần của bài hướng dẫn.
- Bổ sung anchor tại thẻ, thiết bị, nhân sự, ca quản trị, ca của tôi, danh sách phiên, loại vé và danh sách kế hoạch/quy tắc giá. Tái sử dụng anchor topology/layout/swipe/subscription hiện có.
- Menu Hướng dẫn trên header chỉ mở hành trình tổng quan sau onboarding theo effective permission. Drawer Trợ giúp chỉ hiển thị readiness và các bước nghiệp vụ; đã bỏ khối mở coach mark và danh sách chọn nhánh trong drawer.
- Runtime getPublishedGuide vẫn kiểm tra audience và schedule server-side. Không thêm tutorial host, schema, registry authorization hoặc mở rộng quyền route.
- F5 không tự chạy guide mới vì MANUAL_ONLY. Bốn danh mục vẫn mặc định Active/Đang áp dụng; nội dung chỉ cách chọn Nháp/Xem nháp.
- Hướng dẫn từng workspace mở tại liên kết của workspace. Liên kết Hướng dẫn & Trợ giúp cạnh tiêu đề Quản lý thẻ mở WORKFLOW_CARD_STOCK, kiểm tra CARD_READ_ALL và audience server-side. Các walkthrough form cũ giữ code và điều kiện cũ; draft cũ vẫn cần xuất bản qua quy trình quản trị.

## Trình tự phụ thuộc được giải thích

Account/onboarding → organization hoặc phân công hiệu lực → bãi SETUP + vị trí
→ xe/vé công bố → plan → rule/coverage → dịch vụ áp dụng tại bãi
→ khu/sức chứa → cổng → làn IN/OUT → layout SPACE nếu có
→ readiness/kích hoạt → ca OPEN nếu session guard yêu cầu
→ thẻ/QR + ảnh + preview → check-in → phiên mở
→ tìm phiên/đối chiếu/prepare → thanh toán khi cần → complete → CLOSED/thẻ/chỗ.

Kho thẻ, thiết bị và phân công/lịch ca là các nhánh chuẩn bị theo dịch vụ; không bắt mở ca trước khi bãi ACTIVE. Không bắt người quản trị giao manager để tự cấu hình tiếp. Không ép cấu hình subscription/reservation cho bãi không cung cấp. Vé/đặt chỗ đã mua và phiên đang mở được giải thích là nghĩa vụ riêng với điều kiện bán mới.

## Kiểm thử

- PostgreSQL Testcontainers/Flyway: 3 WorkflowCoachMarkSeedIT đạt; kiểm tra 19 guide phát hành thủ công, 79 bước theo thứ tự, audience có permission thật, không dùng role-only, chạy lại không ghi đè tiêu đề/trạng thái quản trị và không publish draft cũ.
- Hồi quy: 12 UserGuideRuntimeUseCaseImplTest và 4 PostOnboardingReadinessUseCaseImplTest đạt.
- Playwright: 4 ca đạt — đối chiếu seed/selector/anchor; EMPLOYEE được cấp cấu hình khác EMPLOYEE thiếu quyền; checkout-only không thấy guide check-in; liên kết tại Quản lý thẻ mở đúng guide, header chỉ mở tổng quan, drawer không còn khối coach mark, F5 không autoplay và không có business write (API được mock).
- TypeScript, npm test và production build đạt; git diff --check sạch.

Giới hạn: kiểm tra khai báo anchor cho toàn bộ 79 bước và chạy UI thực tế của tuyến thẻ, không xác minh mọi node/form trên mọi bộ dữ liệu. Một số anchor chỉ xuất hiện sau khi chọn bãi/khu/phiên/mode; nội dung yêu cầu chọn đúng ngữ cảnh và host giữ fallback hiện hữu. Không tự mở form hay gọi mutation để làm anchor xuất hiện. Ca của tôi hiện là màn hình xem phân công: guide nói rõ cần luồng mở ca được hỗ trợ hoặc người có quyền, không thêm nút nghiệp vụ giả. Không kiểm thử camera/đầu đọc, OIDC tenant thật, thanh toán thật hoặc production trong lần bổ sung này.

## Áp dụng

Chạy Flyway bằng quy trình triển khai backend hiện hữu, sau đó cập nhật frontend để có route selector và anchor mới. Chưa chạy migration vào DB nghiệp vụ/môi trường dùng chung. Không cần sửa hay chuyển trạng thái catalog, bãi, assignment hoặc session để xem các guide.

