# Quy tắc vòng đời thẻ và xử lý mất thẻ

## 1. Mục tiêu

Tài liệu này quy định cách chuyển trạng thái thẻ trong CoParking. Mọi thay đổi phải bảo toàn liên kết giữa thẻ, phiên gửi xe, vé tháng, phiếu báo mất và hóa đơn; không dùng thao tác cập nhật trạng thái chung để bỏ qua nghiệp vụ.

## 2. Ý nghĩa trạng thái thẻ

| Trạng thái | Ý nghĩa |
|---|---|
| `AVAILABLE` | Thẻ sẵn sàng cấp, giữ chỗ hoặc check-in. |
| `RESERVED` | Thẻ đăng ký được giữ cho vé đang chờ thanh toán hoặc chờ nhận thẻ. |
| `ASSIGNED` | Thẻ đã giao cho khách có vé tháng, chưa có phiên gửi xe mở. |
| `IN_USE` | Thẻ đang gắn với một phiên gửi xe mở. |
| `BLOCKED` | Thẻ bị tạm khóa do sự cố; bắt buộc lưu trạng thái trước khi khóa. |
| `LOST` | Thẻ được ghi nhận mất, tại kho hoặc qua phiếu báo mất. |
| `DAMAGED` | Thẻ hỏng, không đủ điều kiện dùng trong vận hành. |
| `RETIRED` | Thẻ đã được loại khỏi vận hành. |

## 3. Nguyên tắc chung

1. Mỗi thao tác có use case riêng: khóa, mở khóa, báo mất, hủy báo mất, thu hồi thẻ tìm thấy và ngừng sử dụng.
2. Mọi thao tác yêu cầu xác nhận trên giao diện, lý do, tài khoản thực hiện và thời điểm xử lý.
3. Không được tự đổi sang `AVAILABLE` nếu thẻ đang gắn với vé tháng, phiên gửi xe hoặc phiếu báo mất đang mở.
4. `BLOCKED` là trạng thái tạm thời. `LOST`, `DAMAGED` và `RETIRED` không được khóa tiếp.
5. `RETIRED` là trạng thái loại khỏi vận hành, không phải cách khóa tạm thời.

## 4. Khóa và mở khóa thẻ

Khi khóa, hệ thống phải lưu trạng thái trước khóa và vẫn giữ nguyên dữ liệu nghiệp vụ liên quan.

```text
status = BLOCKED
status_before_blocked = AVAILABLE | RESERVED | ASSIGNED | IN_USE
blocked_at, blocked_by, blocked_reason
```

| Trạng thái trước khi khóa | Có cho phép khóa? | Trong thời gian khóa | Khi mở khóa |
|---|---:|---|---|
| `AVAILABLE` | Có | Không cấp mới hoặc check-in bằng thẻ | Trở về `AVAILABLE` |
| `RESERVED` | Có | Dừng thao tác giao thẻ; vé chờ vẫn giữ liên kết thẻ | Trở về `RESERVED` |
| `ASSIGNED` | Có | Không cho check-in; vé tháng vẫn gắn với thẻ | Trở về `ASSIGNED` |
| `IN_USE` vãng lai | Có | Giữ phiên gửi xe `OPEN`, từ chối checkout đến khi giải quyết sự cố | Trở về `IN_USE` |
| `IN_USE` đăng ký | Có | Giữ phiên gửi xe `OPEN` và vé tháng liên kết; từ chối checkout | Trở về `IN_USE` |
| `LOST`, `DAMAGED`, `RETIRED` | Không | Không áp dụng | Không áp dụng |

Trước khi mở khóa, backend phải kiểm tra dữ liệu nền còn khớp với `status_before_blocked`:

| Trạng thái cần khôi phục | Điều kiện kiểm tra |
|---|---|
| `AVAILABLE` | Không có ràng buộc đang hoạt động. |
| `RESERVED` | Vẫn có vé `PENDING_PAYMENT` hoặc `PENDING_CARD` gắn với thẻ. |
| `ASSIGNED` | Vẫn có vé tháng `ACTIVE` gắn với thẻ. |
| `IN_USE` | Vẫn có đúng một phiên gửi xe `OPEN` gắn với thẻ. |

Nếu điều kiện không còn đúng, không tự mở khóa; nhân viên phải xử lý theo luồng sự cố và ghi lý do.

## 5. Báo mất thẻ

| Bối cảnh thẻ | Có tạo phiếu báo mất/hóa đơn? | Xử lý khi báo mất | Khi tìm lại trước thanh toán |
|---|---:|---|---|
| Thẻ vãng lai `AVAILABLE`, chưa sử dụng | Không | Ghi nhận mất kho, chuyển `LOST` | Chuyển về `AVAILABLE` |
| Thẻ vãng lai `IN_USE` | Có | Phiên thành `LOST_CARD`, tạo phiếu và hóa đơn phí gửi xe + phí mất thẻ | Hủy phiếu, thẻ về `IN_USE`, phiên về `OPEN` |
| Thẻ đăng ký `AVAILABLE`, chưa gắn vé | Không | Ghi nhận mất kho, chuyển `LOST` | Chuyển về `AVAILABLE` |
| Thẻ đăng ký `RESERVED` | Không tạo phiếu của khách | Gỡ thẻ cũ khỏi yêu cầu, đánh dấu thẻ cũ `LOST`, giữ một thẻ khác cho vé | Không áp dụng hủy phiếu |
| Thẻ đăng ký `ASSIGNED`, xe ngoài bãi | Có | Tạo phiếu mất thẻ ngoài bãi và hóa đơn phí mất thẻ | Hủy phiếu, thẻ về `ASSIGNED` |
| Thẻ đăng ký `IN_USE` | Có | Phiên thành `LOST_CARD`, tạo phiếu và hóa đơn phí mất thẻ | Hủy phiếu, thẻ về `IN_USE`, phiên về `OPEN` |

### Hủy phiếu báo mất

Chỉ cho phép khi phiếu `OPEN`, hóa đơn chưa thanh toán và chưa có thanh toán thành công. Hệ thống phải hủy hóa đơn, lưu lý do hủy, khôi phục thẻ và khôi phục phiên gửi xe theo đúng bối cảnh.

Không hủy phiếu khi hóa đơn đã thanh toán hoặc phiếu đã `RESOLVED`. Nếu cần hoàn tiền, phải tạo nghiệp vụ hoàn tiền hoặc phiếu điều chỉnh độc lập.

## 6. Thu hồi thẻ tìm thấy sau khi đã hoàn tất báo mất

Đây là nghiệp vụ khác với hủy báo mất. Dùng khi khách đã thanh toán, phiếu đã `RESOLVED`, xe đã đi hoặc thẻ thay thế đã được gán.

| Điều kiện tìm thấy thẻ | Nghiệp vụ | Phiếu báo mất và hóa đơn | Trạng thái thẻ cũ |
|---|---|---|---|
| Phiếu `OPEN`, chưa thanh toán | Hủy báo mất | Hủy phiếu và hóa đơn chưa thanh toán | Khôi phục theo bối cảnh |
| Phiếu `RESOLVED`, hóa đơn đã thanh toán, thẻ còn tốt | Thu hồi thẻ tìm thấy | Giữ nguyên phiếu, hóa đơn, thanh toán và doanh thu | `AVAILABLE` sau kiểm tra vật lý |
| Phiếu `RESOLVED`, thẻ hỏng | Thu hồi và đánh giá thẻ | Giữ nguyên dữ liệu tài chính | `DAMAGED` hoặc `RETIRED` |
| Không xác định được thẻ hoặc UID không hợp lệ | Cách ly để xác minh | Không thay đổi | Giữ `LOST` |

Khi thu hồi, tạo bản ghi tối thiểu:

```text
card_recovery_id
card_id
lost_card_report_id
recovered_at
recovered_by
condition: USABLE | DAMAGED | RETIRED
note
```

Tên thao tác trên giao diện là **Thu hồi thẻ tìm thấy**, không dùng tên **Kích hoạt lại thẻ**. Với thẻ đăng ký đã được cấp thẻ thay thế, vé tháng tiếp tục gắn với thẻ mới; thẻ cũ tìm lại được chỉ trở về kho khi đủ điều kiện.

## 7. Ngừng sử dụng thẻ

| Trạng thái trước thao tác | Có cho phép? | Kết quả |
|---|---:|---|
| `AVAILABLE` | Có | Chuyển `RETIRED`, ghi lý do và người thực hiện. |
| `DAMAGED` | Có | Chuyển `RETIRED` khi xác nhận không sửa chữa. |
| `RESERVED`, `ASSIGNED`, `IN_USE` | Không | Phải hoàn tất hoặc hủy liên kết vé/phiên trước. |
| `LOST` | Không khuyến nghị | Giữ `LOST` để bảo toàn lịch sử sự cố. |
| `BLOCKED` | Chỉ sau khi xử lý khóa và không còn ràng buộc | Có thể chuyển `RETIRED`. |

Phục hồi thẻ `RETIRED` chỉ là thao tác quản trị riêng, không phải mở khóa. Hệ thống phải kiểm tra không còn vé, phiên gửi xe hoặc phiếu báo mất hoạt động; sau đó mới có thể đưa thẻ về `AVAILABLE` và ghi lý do phục hồi.

## 8. Dữ liệu cần bổ sung

Ngoài thông tin khóa và thu hồi nêu trên, nên có bảng lịch sử trạng thái:

```text
card_status_history
- card_status_history_id
- card_id
- action
- from_status
- to_status
- reason
- related_lost_card_report_id
- performed_by
- performed_at
```

Bảng này phục vụ kiểm toán, giải thích thay đổi trạng thái và hỗ trợ khôi phục đúng dữ liệu khi xảy ra sự cố.
