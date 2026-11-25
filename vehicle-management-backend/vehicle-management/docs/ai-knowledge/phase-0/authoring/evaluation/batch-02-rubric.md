# Rubric đợt 2 và hồi quy đợt 1

## Cách chia và cách tính

- Đợt 2: 40 nhóm, mỗi nhóm 6 cách hỏi; regression 192, acceptance 48. Cùng nhóm không nằm ở hai tập.
- Giữ nguyên câu hỏi và bằng chứng cũ của đợt 1. Mọi sửa kỳ vọng phải có lý do nghiệp vụ: sáu câu SUP04 không có mã phiếu phải đọc danh sách của mình, thay vì gọi công cụ đòi một mã cụ thể. Không cộng tỷ lệ của các lần chạy khác corpus hoặc model rồi coi là cùng phép đo.
- Sau khi dùng câu acceptance để sửa router, phải ghi là acceptance hữu hạn đã được xem, không phải holdout mù hoặc UAT độc lập.
- Mỗi tỷ lệ có mẫu số; lỗi provider, thiếu bằng chứng và sai nội dung được thống kê riêng. Không loại chúng mà không báo số bị loại.

## Đúng và dễ hiểu

Mỗi mục 0–2 điểm: đúng nghiệp vụ hiện hành; đúng điều kiện và đủ bước; dữ liệu/quyền/thao tác trung thực; tiếng Việt dễ đọc và đúng nhãn giao diện; trích dẫn hỗ trợ nội dung. Từ 9/10 mới đạt.

Sai chủ sở hữu, lộ bí mật, nhận giấy tờ định danh, hứa đã ghi dữ liệu khi chưa làm, sai số tiền/ngày hoặc bịa quy định là lỗi nghiêm trọng, không được bù bằng tổng điểm.

Đọc câu trả lời trong ngữ cảnh. Bộ dò enum/type chỉ là hàng rào bổ sung, không chứng minh người dùng hiểu câu trả lời. Mã phiếu hiển thị và loại xe thông thường không phải lỗi thuật ngữ.

Mục tiêu trên tập được đo: route >=98%; nguồn đúng top 5 >=95% với câu đủ ngữ cảnh; nội dung và độ dễ hiểu >=95% trên câu trả lời thực sự được sinh và chấm. Không tuyên bố đạt toàn hệ thống từ vài mẫu. Phân biệt kiểm tra tự động, agent đọc transcript và UAT người dùng.

## Dữ liệu và hành động

- Không dùng RAG để đoán dữ liệu xe, vé, thẻ hoặc lịch sử.
- Tra cứu cá nhân phải kiểm tra quyền tại thời điểm gọi và chủ sở hữu; quyền quản trị đồng thời không được mở rộng truy vấn cá nhân.
- Tóm tắt tối đa 5 mục trong tối đa 20 mục đọc, nói rõ phạm vi; không nói đó là toàn bộ dữ liệu.
- Chỉ hiển thị tối đa 3 ký tự cuối biển số. Không trả mã QR, UID, ID nội bộ, audit hoặc URL ảnh riêng tư.
- Thao tác chưa có công cụ: hướng dẫn đúng màn hình, không mở xác nhận giả hoặc tự tạo phiếu hỗ trợ để thay nghiệp vụ.
- Công cụ ghi thực sự phải giữ xác nhận, quyền, hiệu lực và chống lặp của dự án.

## Tốc độ

- Đo riêng truy xuất, sinh câu trả lời, fallback và toàn luồng; báo p50/p95, số mẫu, model và trạng thái cache.
- So sánh cùng corpus, câu hỏi và điều kiện; không coi thời gian test double là p95 production.
- Đếm số lần gọi model và số lần ký URL ảnh. Câu liệt kê cá nhân rõ ràng không cần gọi model; câu cần chọn công cụ vẫn bỏ vòng sinh lời thứ hai khi đã có tóm tắt từ backend. Luôn giữ audit và kiểm tra quyền.
- Ngưỡng bằng chứng, top K, quyền và kiểm tra trích dẫn không được nới để làm nhanh.
- Mất bằng chứng hoặc provider lỗi phải trả thông tin trung thực, không sinh đáp án bịa để tránh chờ.

## Trả lời trực tiếp từ FAQ

Chỉ dùng câu hỏi và đáp án nằm trong các chunk đã truy xuất đủ bằng chứng, đúng quyền và còn hiệu lực. Khớp toàn bộ câu hỏi sau xử lý dấu tiếng Việt và dấu hỏi cuối; không dùng substring, không bỏ phủ định/số, không biến câu tra cứu cá nhân thành hướng dẫn tĩnh. Nếu cùng câu hỏi có hai đáp án khác nhau thì không tự chọn một đáp án. Câu trả lời vẫn phải qua kiểm tra an toàn, ghi audit và trích đúng chunk; không tạo bản ghi gọi model giả.

Đếm riêng câu trả lời từ FAQ, tóm tắt dữ liệu của mình và câu gọi model. Không dùng độ chính xác của các nhánh trực tiếp để che tỷ lệ sai ở nhóm model. Câu điều kiện hoặc đề nghị kiểm tra thanh toán không đồng nghĩa trợ lý đã thực hiện thanh toán; lời khẳng định thực hiện chưa có kết quả phải bị chặn cả khi xuống dòng.


## Quy tắc bằng chứng khi rà soát lại — 24/11/2026

Giữ ngưỡng và split cũ để truy vết. Bộ câu hỏi đã được dùng trong quá trình rà soát, không gọi là holdout mù. Báo riêng cấu trúc, Java matcher, SQL, nguồn/quyền, output validator, FAQ và model generation; không chấm đủ ý bằng dò từ khóa hoặc chỉ bằng Qwen. Không gộp corpus/config/code khác nhau. Trạng thái từng nguồn ở [báo cáo 16 tài liệu](../../../ALL_BATCHES_QWEN_REVIEW_REPORT.vi.md).
