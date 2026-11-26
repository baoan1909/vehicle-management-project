# Gỡ local AI, giữ chatbot Google — 27/11/2026

## Kết quả

- Gỡ adapter/cấu hình LM Studio, prompt rút gọn local và đăng ký model local.
- Gỡ UI chọn provider, client API, controller, scheduler, khóa/epoch chuyển chế độ và bảng runtime không dùng nữa.
- Gỡ bộ chạy/test/báo cáo dành riêng cho local inference. Giữ bằng chứng hỗn hợp có ghi rõ là lịch sử, không dùng làm nghiệm thu Google.
- Giữ Google model/key fallback, quota/circuit breaker, kiểm tra dữ liệu và phân quyền; giữ luồng FAQ, retrieval, duyệt và quản lý tài liệu.
- Giữ đủ 16 tài liệu và 1.104 câu hỏi của bốn đợt/bộ nền. 24 câu bổ sung được đổi tên thành `additional-evaluation.jsonl` cùng metadata/reference, nội dung và hash không đổi.
- Gỡ các biến `LM_STUDIO_*` khỏi `.env.example` và `.env` cục bộ; giữ giá trị Google đã có và cấu hình Google được người dùng bổ sung trong lúc làm việc. `.env` thật không đưa vào Git.

## Nâng cấp database

Migration mới: `V20261126120000__retire_local_ai_provider.sql`. Các migration cũ được giữ nguyên checksum để Flyway nâng cấp được DB đã sử dụng phiên bản trước.

Migration lưu snapshot dữ liệu local và audit runtime cũ vào `ai.retired_provider_records`, sau đó loại các cấu hình/model/vector/index local khỏi đường chạy. Không relabel dữ liệu/vector local thành Google. Run local được lưu lịch sử; tool audit cũ được giữ liên kết hiển thị, còn hành động local chưa hoàn tất bị hết hiệu lực. Tác vụ chat local và ingestion local chưa hoàn tất bị dừng, không tự gửi nội dung cũ sang Google.

Tài liệu đã duyệt, tin nhắn, hội thoại và dữ liệu Google được giữ. Nếu chưa có Google index thì quản trị viên cần xử lý/bật index phù hợp; không tự kích hoạt model hay index trong migration. Tài liệu local chưa duyệt cần chủ động xử lý lại trước khi duyệt.

Dừng backend phiên bản cũ trước khi nâng cấp. Việc còn thấy tên provider cũ trong migration, fixture nâng cấp hoặc hash/báo cáo lịch sử không có nghĩa ứng dụng còn hỗ trợ local inference. Thư mục model và phần mềm trên máy nằm ngoài phạm vi thay đổi repository.

## Kiểm tra đã thực hiện

| Kiểm tra | Kết quả |
| --- | --- |
| Build backend từ output sạch | Qua; không còn class local cũ trong output |
| Java: Google fallback/key, embedding, FAQ, ingestion, indexing, kiến trúc và migration | 226 test qua, không lỗi/skip trên kết quả cuối |
| Nâng cấp DB có dữ liệu cả hai provider | Qua trên PostgreSQL Testcontainers; Google snapshot giữ nguyên; local pending bị dừng; local data có archive |
| Spring Boot từ DB sạch | Qua; đúng một provider generation và một provider embedding, đều Google; không có route chuyển provider |
| TypeScript/Vite production build | Qua; còn cảnh báo vendor/bundle thông thường |
| Browser: tri thức, quyền thao tác và trang model Google | 9/9 qua với API giả lập |
| Công cụ đồng bộ tri thức | 9/9 qua; đủ 16 tài liệu, 1.104 câu và liên kết hợp lệ |
| Giá trị Google và migration cũ | Được giữ |

Hai lỗi fixture trong lượt đầu (tính cả tương tác constructor của spy và trạng thái mẫu quá dài so với cột) đã được sửa rồi kiểm tra lại. Log lỗi cũ, bản sao source và kết quả cuối lưu trong `.tmp/remove-local-ai-20261126/`, không đưa lên Git.

Không gọi Google API thật, không dùng key để đo quota và không nghiệm thu lại độ chính xác live. Chỉ chạy migration trên DB test cô lập, không chủ động áp dụng lên DB dev. Chưa commit/push.

Xem [hướng dẫn bàn giao](TEAM_HANDOFF.vi.md) để chạy Google API và dùng kho tri thức.
