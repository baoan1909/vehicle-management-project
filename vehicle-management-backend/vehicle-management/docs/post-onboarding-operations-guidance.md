# Lộ trình hướng dẫn sau onboarding đến check-in/check-out

## 1. Nguyên tắc quyết định quyền

Role chỉ dùng để mô tả nhóm người dùng. Launcher và backend User Guide quyết định eligibility bằng effective permissions của current-access. Scope organization, bãi được giao, trạng thái account/employee/onboarding và các guard nghiệp vụ vẫn được backend kiểm tra tại từng API ghi.

| Thao tác | Effective permission chính | Scope/guard | Điều kiện và dependency | Nhóm thường làm | Guide |
|---|---|---|---|---|---|
| Tạo bãi | `PARKING_LOT_CREATE_ALL` | `OrganizationAccessGuard` | Membership organization hoạt động; địa chỉ/vị trí hợp lệ | Partner Admin | `PARKING_LOT_CREATE` |
| Đọc/cập nhật/kích hoạt bãi | `PARKING_LOT_READ_ALL`, `PARKING_LOT_UPDATE_ALL` | Organization và parking-lot scope | Zone, gate, IN/OUT lane, published layout cho SPACE, published pricing và vị trí hợp lệ | Partner Admin; người được cấp quyền | `PARKING_LOT_ACTIVATION` |
| Yêu cầu kích hoạt | `PARKING_LOT_READ_ALL` | Parking Manager được giao bãi | Bãi ở `SETUP`, readiness và vị trí đạt | Parking Manager | `PARKING_LOT_ACTIVATION` |
| Cấu hình tầng/zone/cổng/làn | `PARKING_TOPOLOGY_CONFIGURE_ALL` hoặc capability cấu trúc tương ứng | `ParkingLotScopeResolver`, organization/assignment guard | Bãi thuộc scope; dependency cha tồn tại | Partner Admin/Parking Manager/người được ủy quyền | `PARKING_STRUCTURE_SETUP` |
| Cấu hình/publish layout | `PARKING_SPACE_CONFIGURE_ALL`, `PARKING_LAYOUT_PUBLISH_ALL` | Bãi và zone thuộc scope | Zone SPACE; space hợp lệ trước publish | Người có capability | `PARKING_LAYOUT_PUBLISH` |
| Loại phương tiện | `VEHICLE_TYPE_*_ALL` | `CatalogAccessGuard` theo organization | Catalog draft/active; canonical mapping hợp lệ | Partner Admin/người được ủy quyền | `CATALOG_VEHICLE_TYPE_SETUP` |
| Loại vé | `TICKET_TYPE_*_ALL` | `CatalogAccessGuard` theo organization | Catalog hợp lệ trước price rule | Partner Admin/người được ủy quyền | `CATALOG_TICKET_TYPE_SETUP` |
| Kế hoạch giá | `PRICE_PLAN_*_ALL` | `CatalogAccessGuard` theo organization | VISITOR/CUSTOMER, ngày hiệu lực không xung đột | Partner Admin/người được ủy quyền | `PRICE_PLAN_SETUP` |
| Quy tắc giá | `PRICE_RULE_*_ALL` | `CatalogAccessGuard` theo organization | Plan + vehicle type + ticket type; đơn vị/khung giờ/giá hợp lệ | Partner Admin/người được ủy quyền | `PRICE_RULE_SETUP` |
| Phân công nhân sự/ca | `EMPLOYEE_*_ALL`, `SHIFT_*`, `SHIFT_ASSIGNMENT_*` | `EmployeeParkingLotAccessGuard` | Account/employee active; phân công đúng bãi và hiệu lực | Partner Admin/Parking Manager/người được cấp quyền | Hành trình sau onboarding |
| Check-in | `PARKING_SESSION_CHECK_IN_ALL` | `ParkingSessionAccessGuard`, bãi được giao | Bãi/làn IN hoạt động, thẻ/QR, loại xe, bằng chứng, sức chứa/vị trí | Người vận hành có capability | `PARKING_CHECK_IN_FIRST_RUN` |
| Check-out | `PARKING_SESSION_CHECK_OUT_ALL` | `ParkingSessionAccessGuard`, bãi được giao | Phiên mở, làn OUT, đối chiếu biển số, phí và payment hợp lệ | Người vận hành có capability | `PARKING_CHECK_OUT_FIRST_RUN` |
| Thu tiền | `PAYMENT_CREATE_ALL` | Scope hóa đơn/phiên | Invoice hợp lệ; phương thức và trạng thái giao dịch hợp lệ | Người thu ngân được cấp quyền | Guide checkout/payment tương ứng |
| Mất thẻ/override | Permission lost-card/plate review tương ứng | Scope bãi/phiên và guard nghiệp vụ | Không được suy ra từ quyền check-in/check-out | Người có capability ngoại lệ | Bộ guide lost-card |

`_ALL` là tên permission hiện hữu, không phải quyền vượt tenant. Organization và parking-lot guard vẫn là nguồn quyết định cuối cùng.

## 2. Dependency graph chung

```text
Account + onboarding hợp lệ
  -> Organization/membership hoặc phân công bãi hợp lệ
  -> Bãi SETUP có địa chỉ và vị trí xác nhận
  -> Loại phương tiện + loại vé phù hợp
  -> Kế hoạch giá -> quy tắc giá được công bố cho dịch vụ
  -> Tầng/zone -> cổng -> làn IN/OUT -> layout SPACE nếu áp dụng
  -> Thẻ/QR + thiết bị/bằng chứng + phương thức thanh toán
  -> Nhân sự + phân công + ca theo policy
  -> Readiness theo từng capability
  -> Check-in -> phiên OPEN
  -> Tìm phiên -> prepare checkout/tính phí
  -> Thanh toán nếu cần -> complete checkout
```

Readiness phải hiểu theo từng kết quả: có thể check-in, tìm phiên, chuẩn bị checkout, thu tiền, hoàn tất checkout và xử lý ngoại lệ; không dùng account `ACTIVE` như cờ “bãi sẵn sàng”.

## 3. Lộ trình theo capability

### Partner Admin thường gặp

1. Xác nhận organization và membership.
2. Tạo bãi ở `SETUP`, hoàn thiện địa chỉ/vị trí và quản lý.
3. Rà soát loại phương tiện, loại vé.
4. Tạo/công bố kế hoạch giá và quy tắc giá.
5. Hoàn thiện hoặc giao cấu trúc, layout, thiết bị.
6. Chuẩn bị thẻ, phương thức thanh toán, nhân sự và ca.
7. Kiểm tra readiness, kích hoạt bãi.
8. Chọn nhánh vãng lai, subscription hoặc reservation thực sự cung cấp.
9. Chạy luồng check-in rồi check-out; xử lý ngoại lệ bằng capability riêng.

### Parking Manager thường gặp

1. Kiểm tra bãi được giao và thời điểm hiệu lực.
2. Đọc trạng thái bàn giao, không tạo lại phần Partner đã hoàn thành.
3. Hoàn thiện topology/layout/thiết bị trong capability được cấp.
4. Kiểm tra dịch vụ/catalog/giá; chuyển blocker ngoài quyền cho người có quyền.
5. Chuẩn bị nhân sự, ca, thẻ và điều kiện thu tiền.
6. Kích hoạt nếu có quyền hoặc gửi activation request nếu đúng policy.
7. Vận hành và giám sát phiên; bàn giao ngoại lệ ngoài quyền.

### Employee/người vận hành

1. Kiểm tra account/employee/onboarding, bãi được giao và ca.
2. Phân loại phần thiếu: tự làm được, cần dependency trước, hoặc chờ người có quyền.
3. Kiểm tra bãi/làn/camera/thẻ/QR/sức chứa.
4. Check-in theo vãng lai/subscription/reservation được hỗ trợ.
5. Check-out: phiên mở, bằng chứng, đối chiếu, phí, payment và complete.
6. Không bypass mất thẻ, sai QR, lệch biển số, thiếu ảnh hoặc payment pending.

## 4. Employee: tự làm hay phải chờ

| Trạng thái | Cách hiển thị |
|---|---|
| Có effective permission, đúng bãi, assignment hiệu lực và backend cho phép | Có thể thực hiện ngay; mở guide màn hình tương ứng |
| Có permission nhưng thiếu dependency mà employee cũng được làm | Đưa dependency lên trước; hoàn thành rồi kiểm tra lại |
| Có permission nhưng sai scope/chưa đến thời điểm phân công/chưa mở ca | Bị chặn bởi trạng thái; giải thích điều kiện cụ thể |
| Không có permission | Chờ người có capability; không hiện nút giả và không bypass guard |
| Dữ liệu thật đã đạt | Bỏ qua yêu cầu làm lại; guide vẫn có thể xem lại thủ công |
| Dịch vụ không áp dụng | Không yêu cầu cấu hình subscription/reservation cho bãi chỉ phục vụ vãng lai |

## 5. Quy tắc hiển thị và tiến độ

- Header ưu tiên guide được trang hiện tại đăng ký; nếu không có, chọn hành trình sau onboarding bằng effective permissions.
- Thứ tự fallback: capability thiết lập Partner -> capability bàn giao Manager -> capability vận hành Employee. Không đọc `user.role`.
- Ba hành trình header là `MANUAL_ONLY`; F5 không tự phát lại.
- “Đã xem/hoàn thành guide” chỉ là telemetry User Guide, không thay đổi trạng thái nghiệp vụ.
- Đổi account làm thay đổi current-access và lựa chọn guide; không dùng tiến độ account cũ.
- Backend `getPublishedGuide` kiểm tra lại audience permission khi mở thủ công.
- Guide hiện hành giữ cơ chế spotlight, tự scroll, placement an toàn, focus trap, Escape và khôi phục focus/scroll.
- “Hỗ trợ” trên menu header bị disabled và không có link giả.

## 6. Luồng dịch vụ và blocker

- **Vãng lai:** thẻ AVAILABLE đúng bãi -> IN lane/biển số/ảnh/sức chứa -> OPEN session -> OUT lane/đối chiếu -> tính phí -> payment -> đóng phiên, giải phóng thẻ/chỗ.
- **Subscription:** khách hàng + xe + yêu cầu -> duyệt -> payment -> thẻ/QR -> check-in/out; hết hạn/gia hạn/thu hồi theo trạng thái thật.
- **Reservation:** chỉ áp dụng khi dịch vụ hỗ trợ; reservation/payment/thời hạn/QR/chỗ giữ -> check-in đúng cửa sổ -> checkout đối trừ trả trước và phát sinh.
- **Blocker bắt buộc:** chưa có bãi/phân công/ca; bãi/làn/catalog/giá chưa active; hết chỗ; thẻ khóa/mất/sai bãi; QR sai; thiếu ảnh; OCR/biển số lệch; không có phiên; payment pending/failed. Guide không cung cấp đường bypass.

## 7. Implementation

- `AdminHelpMenu` cung cấp dấu hỏi, keyboard/Escape/click-outside/focus và hai item Hướng dẫn/Hỗ trợ.
- `AdminLayout` đặt toàn bộ header, sidebar và nội dung trong một `UserGuideBackendHost` để launcher dùng đúng provider.
- `postOnboardingJourney.ts` lựa chọn journey bằng effective permission và trạng thái access/onboarding.
- `GET /api/notifications/user-guides/post-onboarding-readiness` tổng hợp scope hiện hành, readiness thật của từng bãi và capability riêng cho check-in, tìm phiên, checkout, payment, ngoại lệ.
- `PostOnboardingReadinessDrawer` phân loại từng bước thành có thể làm ngay, cần dependency, chờ người có quyền, đã hoàn thành hoặc đang bị chặn; đổi bãi hay bấm kiểm tra lại đều gọi lại backend.
- Migration `V20261115110000` seed ba guide đã phát hành, `MANUAL_ONLY`, audience theo permission.
- Sidebar có anchor cho Bãi xe, Catalog, Bảng giá, Nhân sự và Vào/Ra; popup dùng placement `RIGHT` để không che spotlight.

## 8. Giới hạn kiểm chứng

Hành trình tổng quan không gọi API ghi và không suy diễn 403 thành “chưa có dữ liệu”. Endpoint tổng hợp tái sử dụng `OrganizationAccessGuard`, `ParkingSessionAccessGuard`, effective permissions và chính truy vấn activation readiness đang dùng khi kích hoạt bãi. Quyết định cuối cùng vẫn được API nghiệp vụ kiểm tra lại tại thời điểm ghi để tránh dùng capability cũ sau khi đổi quyền, scope, phân công hoặc ca.

Readiness catalog/giá hiện được trả về như một dependency kết hợp vì policy kích hoạt hiện hành kiểm tra toàn bộ ma trận dịch vụ/loại xe/loại vé/price rule trong một truy vấn. Drawer không khẳng định riêng mục nào thiếu khi backend chưa cung cấp chi tiết nhỏ hơn; người dùng được đưa tới màn hình catalog/giá để xử lý và kiểm tra lại.
