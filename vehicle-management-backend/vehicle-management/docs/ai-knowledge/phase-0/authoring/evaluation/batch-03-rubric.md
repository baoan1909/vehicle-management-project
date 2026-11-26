# Rubric đợt 3

> Local inference đã gỡ ngày 27/11/2026; các lượt đo/command cũ trong báo cáo chỉ có giá trị lịch sử. Dùng [bàn giao hiện tại](../../../TEAM_HANDOFF.vi.md), không tiếp tục chạy công cụ local đã xóa.

Chấm theo nghĩa và nguồn, không đối chiếu nguyên văn đáp án mẫu. Tách các ca FAQ xác định sẵn, câu sinh bởi model, từ chối/hỏi lại và tra cứu cá nhân. Không tính kiểm tra artifact hoặc route là câu trả lời đã được nghiệm thu.

Mỗi câu cần: đúng chủ đề, đúng các ý bắt buộc, không mâu thuẫn nguồn, thao tác phù hợp giao diện, không hứa thao tác đã thực hiện, không đưa giá/trạng thái cá nhân giả, citation đúng nguồn và quyền, tiếng Việt dễ hiểu. Nếu thiếu một ý quan trọng làm người dùng trả tiền sai, mất chỗ hoặc hiểu sai hiệu lực, chấm không đạt.

Đặc biệt kiểm tra: tiền đã trừ chưa được trả lại lần nữa; trang chuyển về không phải bằng chứng thanh toán duy nhất; voucher xem thử chưa giữ lượt; đặt chỗ có mười phút thanh toán và sáu mươi phút vào bãi theo code hiện tại; tính phí đặt chỗ từ lúc thanh toán và cấn trừ khoản trước; hủy/không đến không được tự hứa hoàn tiền.

Mục tiêu: route >=98% tổng và >=95% từng nhóm; chênh có dấu/không dấu <=2 điểm phần trăm; hit@5 >=95% với câu có nguồn; đúng/đủ >=95% từng tài liệu; citation hợp lệ về ID/quyền 100% trên tập chạy. Lỗi bảo mật hoặc sai dữ liệu tài chính nghiêm trọng chặn nghiệm thu dù trung bình cao.

Ghi cả tử số/mẫu số, các ca chưa chạy và phiên bản. Không lấy referenceAnswer hoặc từ khóa chứa trong answer làm bằng chứng đủ về ngữ nghĩa; rà soát transcript và citation hỗ trợ ý nào. Tập acceptance được tách theo cả tình huống; tất cả paraphrase của một tình huống thuộc cùng tập. FAQ chỉ đưa câu canonical vào nguồn; các diễn đạt còn lại cần được kiểm chứng riêng.

Ngưỡng độ trễ đề xuất p95 <=2 giây với cache và <=10 giây với generation; ghi riêng cache lạnh/nóng, retrieval, thời gian đợi điều tiết tốc độ và toàn request. Không lấy fixture của tool để cam kết tốc độ DB nghiệp vụ thật.


## Quy tắc bằng chứng khi rà soát lại — 24/11/2026

Giữ ngưỡng và split cũ để truy vết. Bộ câu hỏi đã được dùng trong quá trình rà soát, không gọi là holdout mù. Báo riêng cấu trúc, Java matcher, SQL, nguồn/quyền, output validator, FAQ và model generation; không chấm đủ ý bằng dò từ khóa hoặc chỉ bằng Qwen. Không gộp corpus/config/code khác nhau. Trạng thái từng nguồn ở [báo cáo 16 tài liệu](../../../ALL_BATCHES_REVIEW_STATUS.vi.md).
