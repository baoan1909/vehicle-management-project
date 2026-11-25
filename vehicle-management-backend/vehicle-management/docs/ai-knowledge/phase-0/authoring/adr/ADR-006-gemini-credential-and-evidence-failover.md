# ADR-006 — Gemini credentials và bằng chứng trả lời

Ngày: 20/11/2026. Trạng thái: DRAFT; đã có triển khai cục bộ, chưa duyệt phát hành hoặc publish knowledge. Không thay thế các quyết định đã duyệt trước đó.

## Bối cảnh

Một project có thể hết quota theo model/tác vụ hoặc gặp lỗi tạm thời. Nhiều API key thuộc cùng project không tạo quota riêng. Tài liệu tìm đúng vẫn có thể trả lời sai nếu đoạn cần thiết bị cắt, hoặc bộ lọc loại nhầm câu cảnh báo không gửi bí mật.

## Quyết định đã triển khai

- Giữ credential đang dùng sau khi thành công. Thử các model tương thích đã được registry cho phép trên credential đó trước khi chuyển credential; không tự cho model lấy từ catalog vào danh sách sử dụng. Catalog không chứng minh model miễn phí.
- Nhóm giới hạn theo project/model/tác vụ; lỗi xác thực theo credential. Key không có project hợp lệ dùng nhóm bảo thủ chung. Có deadline, giới hạn attempt, cooldown và lịch sử lỗi đã che bí mật.
- Generation có thể đổi model trong danh sách cho phép. Embedding chỉ đổi credential, giữ model/dimension/index; đổi model embedding cần quy trình lập index riêng.
- Tái sử dụng cổng cache hiện có cho cooldown và giữ bản cục bộ khi cache không sẵn sàng. Dữ liệu thời gian serialize bằng chuỗi ISO; không phụ thuộc cấu hình JavaTimeModule của Redis mapper.
- Dùng planner chung cho chat công khai/tài khoản. Adapter vẫn tương thích đường gọi cũ. Các provider khác giữ chính sách cũ nếu không quản lý availability riêng.
- Chọn nguyên dòng/đoạn bằng chứng liên quan trong giới hạn token cũ. Dùng cả từ đơn và cặp từ liên tiếp để phân biệt khái niệm gần nhau; không viết lại số tiền hoặc phủ định của nguồn. Đây là heuristic, không thay thế chấm nghĩa và citation.
- Kiểm tra đầu ra và lọc nguồn dùng chung cho guest/auth. Phân biệt phủ định một kết quả chưa được xác minh với tuyên bố đã hoàn tất thao tác. Các thay đổi chính sách/đoạn dẫn chứng có version trong fingerprint cache.

## Đánh đổi và giới hạn

Key-first không phải chiến lược tìm provider có độ trễ thấp nhất trên mọi request. Nó giữ đúng thứ tự người vận hành yêu cầu và tránh phân tán quota. Điều tiết nhịp và semaphore hiện theo instance; cooldown được chia sẻ qua cache nhưng chưa phải bộ giới hạn tổng/khóa probe nguyên tử giữa nhiều replica. Project ID do người vận hành khai báo, chưa xác minh quyền sở hữu bằng Google Cloud IAM.

Không thử hết các key chỉ để tăng số liệu kiểm chứng. Mã lỗi quota, timeout và 5xx không đồng nghĩa key hỏng vĩnh viễn. Không có key phù hợp thì trả lỗi có kiểm soát, không đổi embedding sang model khác hoặc tự dùng model trả phí.

## Bằng chứng và khôi phục

Xem [runbook](../../../GEMINI_CREDENTIAL_FAILOVER.vi.md), [báo cáo đợt 3](../evaluation/batch-03-report.md) và [truy vết](../evaluation/batch-03-traceability.md). Test HTTP server kiểm tra thứ tự/key/model, lỗi, bảo mật, concurrent request và serialization; live harness dùng PostgreSQL riêng và Gemini thật, business ports có fixture. Không diễn giải các test này thành UAT thanh toán hoặc kiểm chứng nhiều replica.

Khi cần dừng dùng backup, bỏ các biến key dự phòng và khởi động lại theo quy trình triển khai. Không xóa Redis dùng chung để xử lý cooldown. Thu hồi/đổi key qua kênh quản lý bí mật, không đưa giá trị key vào issue hoặc evidence. Knowledge vẫn DRAFT cho đến khi đạt tiêu chí nội dung và vận hành.
