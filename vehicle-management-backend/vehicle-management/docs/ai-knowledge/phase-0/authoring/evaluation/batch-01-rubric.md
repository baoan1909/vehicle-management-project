# Rubric đợt 1

> Local inference đã gỡ ngày 27/11/2026; các lượt đo/command cũ trong báo cáo chỉ có giá trị lịch sử. Dùng [bàn giao hiện tại](../../../TEAM_HANDOFF.vi.md), không tiếp tục chạy công cụ local đã xóa.

- Route: so expectedIntent sau khi resolve context theo case. Báo tổng, từng doc, có/không dấu và scenario split.
- Retrieval: expectedDocument phải có trong top 5, version hiệu lực và scope đúng. Không gọi kết quả synthetic-vector là đánh giá semantic embedding thật.
- Nội dung (0–2 mỗi mục): đúng nghiệp vụ; đủ bước/điều kiện; không khẳng định dữ liệu riêng/thao tác chưa làm; ngắn và tiếp nối đúng câu hỏi; citation hỗ trợ mệnh đề. >=9/10 mới đạt; bất kỳ lỗi nghiêm trọng về quyền/ghi/số tiền/trạng thái là fail dù tổng cao.
- Citation syntax/ID/quyền chấm tự động; hỗ trợ ngữ nghĩa chấm riêng bằng transcript, không coi label tồn tại là đủ đúng.
- Hỏi lại: thiếu topic thì hỏi chức năng/bước, không tự tạo phiếu; có topic đủ thì không bắt người dùng nhắc lại toàn bộ.
- Hành động: command cần đủ dữ liệu + confirm + permission; câu hỏi hướng dẫn/phủ định không có DB effect. Tool dự kiến ở dataset không có nghĩa call khi thiếu ticket ID/dữ liệu/permission.
- Latency: đo p50/p95 trên mẫu, ghi model/corpus/cached state. Mẫu 8 câu không đủ cam kết p95 hoặc độ đúng 95% toàn hệ thống.
- Holdout: scenario AUTH09–10/PRIV09–10/CHAT09–10/SUP09–10 được giữ cùng split; báo hữu hạn 48 biến thể, không tuyên bố generalization từ paraphrase.


## Quy tắc bằng chứng khi rà soát lại — 24/11/2026

Giữ ngưỡng và split cũ để truy vết. Bộ câu hỏi đã được dùng trong quá trình rà soát, không gọi là holdout mù. Báo riêng cấu trúc, Java matcher, SQL, nguồn/quyền, output validator, FAQ và model generation; không chấm đủ ý bằng dò từ khóa hoặc chỉ bằng Qwen. Không gộp corpus/config/code khác nhau. Trạng thái từng nguồn ở [báo cáo 16 tài liệu](../../../ALL_BATCHES_REVIEW_STATUS.vi.md).
