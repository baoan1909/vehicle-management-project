# Kết quả Qwen live context 512

Đã gọi Qwen thật 40/40 câu, thêm 2 câu đối chứng nguồn; không gọi Google, không mở backend/DB và không chạy kiểm thử khác.

- Cấu hình: Qwen3.5-4B, context 512, parallel 1, temperature 0, đầu ra tối đa 140 token.
- HTTP thành công 40/40, không câu bị cắt vì hết token.
- Chấm nội dung trên 40 gói nguồn của lượt này: 30 đúng/đủ ý, 4 thiếu ý, 6 sai. Tỷ lệ đúng/đủ ý có điều kiện theo gói nguồn là 75.0%; không phải độ chính xác toàn chatbox.
- Có 4 câu dẫn [C1] khi không được cung cấp nguồn xác minh. Sau yêu cầu citation, chỉ 27/40 câu đồng thời đạt nội dung và không có lỗi dẫn nguồn rỗng.
- Thời gian direct model p50=3017ms, p95=5870ms, max=12771ms. Tải RAM/caches không cố định; không trình bày thành latency API production.
- RAM trống trước các lần gọi: 274–939MB. Sau gọi thấp nhất 219MB; tạm dừng và tiếp tục từ checkpoint khi RAM hồi. Không hạ guard bộ nghiệm thu đầy đủ.

## Lỗi chính

1. Xóa xe: đầu ra có dấu nói lịch sử bị xóa; không dấu bịa “khởi động lại danh sách”. Gói nguồn thử ban đầu chỉ giữ câu đáp “Không. Thao tác hiện chuyển xe sang ngưng dùng.” nên thiếu câu hỏi xác định đối tượng phủ định. Hai đối chứng đưa nguyên câu hỏi FAQ đều trả đúng. Giữ kết quả sai ban đầu; không thay bằng PASS hoặc gộp hai điều kiện đo.
2. Tin chatbot: biến thể có dấu làm mất phủ định ở “cũng đồng nghĩa với việc sự cố đã được giải quyết”, đảo nghĩa nguồn.
3. Cổng không mở: biến thể không dấu tự giả định “thanh toán không thành”, dù chưa có dữ liệu chứng minh.
4. Mất thẻ: có câu nói lập phiếu và thanh toán “độc lập”, câu khác nói tạo phiếu ở bước hoàn tất; sai ranh giới nghiệp vụ.
5. Phiếu hỗ trợ và ngày đăng ký: có câu thiếu ý hoặc lưỡng lự dù nguồn đã nêu điều kiện. Câu về lịch sử người khác chỉ báo thiếu nguồn, chưa nêu đúng ranh giới quyền.
6. Bốn câu không nguồn vẫn gắn[C1]. Không bịa số chỗ/giờ ra hoặc khẳng định đã hoàn tiền, nhưng citation vẫn không đạt.

**Chưa đạt nghiệm thu.** Nguồn được chọn sẵn và dùng prompt gọn để chạy trên model 512 đã nạp; chưa đo khả năng backend hiểu câu hỏi/chọn nguồn, quyền dữ liệu, API hay UI. Không dùng kết quả này thay nghiệm thu 16 tài liệu/984 câu hoặc so sánh Google. Nguồn ngắn thiếu ngữ cảnh là hạn chế của bộ thử, không được quy toàn bộ lỗi đó cho model.

## Evidence

- observations.jsonl: 40 HTTP inference thật, câu trả lời/nguồn/usage/thời gian/RAM nguyên bản.
- frozen-cases.json: câu hỏi và nguồn chốt trước lượt chạy; 16 tài liệu với cặp có dấu/không dấu, 4 tình huống hồi quy và 4 ranh giới.
- graded-answers.json: điểm và lý do từng câu do Codex đọc/chấm; không Qwen tự chấm, chưa có người duyệt độc lập.
- answers-and-grades.csv: bảng câu hỏi/nguồn/câu trả lời/điểm/lý do.
- source-context-recheck.json: 2 đối chứng nguồn đầy đủ, tách khỏi tỷ lệ 40 câu.
- summary.json: số liệu và phạm vi thực tế; qualityAccepted=false.

Không sửa code/tài liệu nghiệp vụ, thay key, đổi model hoặc kích hoạt dữ liệu DB trong lượt chạy chỉ Qwen này.
