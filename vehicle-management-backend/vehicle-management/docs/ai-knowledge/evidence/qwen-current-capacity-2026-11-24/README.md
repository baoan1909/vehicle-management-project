# Qwen ở tải máy hiện tại — 24/11/2026

Đã gọi trực tiếp hai model có sẵn trong LM Studio, tuần tự, không mở thêm backend/DB kiểm thử:

- Một câu hỏi ngắn với nguồn được cung cấp: Qwen trả lời đúng rằng xem bản đồ không tự giữ chỗ, khoảng 10,2 giây; 67 token vào và 20 token ra.
- Một embedding ngắn: thành công, vector 1024 chiều, chuẩn L2 xấp xỉ 1; khoảng 8,3 giây. Chưa kiểm truy vấn SQL hoặc phép chiếu 768 của backend.

Đây là kiểm tra khả năng xử lý yêu cầu ngắn của model đã nạp, không phải điểm đúng của toàn chatbox hay nghiệm thu 984 câu. Không sửa/hạ guard 1536 MB của bộ nghiệm thu đầy đủ. Một mẫu không đủ suy p95 hoặc tốc độ ổn định.

LM Studio đang nạp context 512, parallel 1; backend có mặc định context 2048. Cần đối chiếu lại context trước lượt RAG đầy đủ. Không đổi cấu hình này trong phép thử.

Xem summary.json để biết snapshot RAM/VRAM, tiny-generation-probe.json để biết prompt/đáp án thực và tiny-embedding-probe.json để biết kích thước vector/thời gian. Không gọi Google.
