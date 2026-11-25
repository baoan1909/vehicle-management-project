# Evidence rà soát bốn đợt — 24/11/2026

Xem [báo cáo tổng hợp](../../ALL_BATCHES_QWEN_REVIEW_REPORT.vi.md). Tài liệu vẫn DRAFT; không có lượt Qwen/Google mới được nghiệm thu.

- `content-integrity.json`: 16 nguồn, phiên bản/checksum, 984 câu batch và baseline riêng, nguồn code/FE/test cùng hash, trạng thái chưa nghiệm thu. So câu FAQ trùng bằng Python chỉ là chẩn đoán, không thay SQL hoặc Java matcher.
- `resource-preflight.json`: RAM thực và ngưỡng trước các gate chưa chạy. Trạng thái hoãn không phải PASS hoặc lỗi nội dung model.
- `source-hashes.json`: các file thay đổi trong lượt này; dùng để đối chiếu khi tiếp tục.
- `export-validation.log`, `export-tests.log`, `summary-tests.log`, `powershell-syntax.log`: các bước nhẹ đã thực chạy; 9 test xuất tài liệu và 4 test tổng hợp số liệu.

Evidence cũ tại `../common-knowledge-2026-11-24` và `../qwen-runtime-2026-11-24` được giữ nguyên. Không chuyển kết quả cũ thành kết quả corpus mới. Những câu đã xem hoặc dùng để sửa không được gọi là holdout mù.

Không có HTTP Google hoặc Qwen live mới từ các bước rà soát này; không tự ghi số latency bằng không cho phần chưa đo. Các lời gọi/fixture stub của lượt cũ được báo riêng trong báo cáo tương ứng.
