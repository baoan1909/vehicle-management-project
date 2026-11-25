# Evidence tri thức chung — 24/11/2026

- `backend-205-tests.log`: gate offline cuối qua 205 test; một trong đó duyệt 984 case fixture. Không tính 984 thành 984 lượt model live.
- `sql-migrations-context-failure.log`: DB Testcontainers riêng áp dụng 165 migration thành công; test sau đó lỗi wiring transaction proxy. Không gọi đây là DB assertion PASS.
- `pending-gates.json`: lỗi fixture đã sửa, những lần guard chặn và các gate còn chờ.
- `frontend-syntax.log`: cú pháp 3 file TS/TSX qua; không thay full typecheck/browser.
- `typescript-memory-failure.log`: lỗi heap của toolchain, giữ nguyên thay vì bỏ khỏi báo cáo.
- `observations.json`, `metadata.json`, `summary.json`: dữ liệu offline DRAFT fixture, block FAQ, route/matcher/validator. Không có SQL ranking hoặc model inference trong bộ đo này.
- `source-hashes.json`: hash các file thay đổi hiện tại. Fixture SQL đã sửa sau lỗi context và chưa có lượt test DB thành công; không dùng hash này để gán test đã chạy cho code chưa chạy.

Đường dùng chung có bằng chứng unit không tương tác provider/index/embedding và không tạo run giả; không gán số HTTP production hoặc điểm model từ các stub này. Log Gemini auth/quota là kịch bản unit, không phải sự cố key thật.

Đọc cùng [báo cáo triển khai](../../COMMON_KNOWLEDGE_IMPLEMENTATION_REPORT.vi.md). Không có thao tác publish corpus DRAFT hoặc migration trên DB dev trong lượt thực hiện này.
