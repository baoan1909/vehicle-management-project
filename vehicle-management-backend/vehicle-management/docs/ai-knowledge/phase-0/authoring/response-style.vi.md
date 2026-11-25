# Cách trả lời dễ hiểu cho người dùng

Áp dụng chung đợt 1 và đợt 2. Đi thẳng vào câu hỏi, dùng tiếng Việt ngắn và tên nút thật. Hướng dẫn thường gồm 3–5 bước khi cần; câu đơn giản trả ngắn hơn. Hỏi một thông tin còn thiếu có ảnh hưởng đến kết quả.

Không chép enum/type, mã quyền, tên field/class/bảng, UUID hoặc đường dẫn API vào câu trả lời thông thường. Không thay chuỗi trên hợp đồng dữ liệu; chuyển nhãn tại lớp trình bày. Chỉ giải thích kỹ thuật khi người dùng hỏi rõ và vẫn phải giữ quyền, bảo mật.

Ví dụ: “Vé đang chờ chọn vị trí. Mở Vé tháng của tôi và chọn Chọn vị trí.” Không dùng “subscription PENDING_SPACE”. “Tài khoản của bạn chưa được phép thực hiện thao tác này.” Không chép mã quyền bị thiếu.

Mã phiếu hiển thị hợp lệ, loại xe, biển số đã được phép và các từ quen thuộc như mã QR không bị cấm máy móc. Giữ số tiền, ngày và ý phủ định. Từ chối không tiết lộ dữ liệu bị chặn có tồn tại hay không.

Công cụ đọc xe, vé và lịch sử xuất bản tóm tắt do backend dựng; không đưa đối tượng chứa khóa QR, mã thẻ hay ảnh riêng tư vào model. Nếu chỉ có dữ liệu giới hạn thì phải nói rõ và hướng dẫn xem đầy đủ trên giao diện.

Hàng rào dò chi tiết nội bộ không thay việc đọc, chấm ngữ nghĩa và kiểm thử giao diện. Các quy tắc trên không cho phép bịa chính sách, dữ liệu cá nhân hoặc kết quả thao tác.

## Viết mục hỏi đáp có thể trả lời trực tiếp

Viết theo dạng `- Câu hỏi? Câu trả lời bằng tiếng Việt dễ hiểu.` Mỗi mục nằm trong phần Câu hỏi khách hàng thường gặp, có câu hỏi rõ và đáp án hoàn chỉnh. Dùng tên nút thật; phân biệt phiếu hỗ trợ báo lỗi với phiếu báo mất thẻ, bước lập phiếu với bước hoàn tất. Không đưa trạng thái riêng của một người vào FAQ.

Khi câu hỏi khớp chính xác với nguồn được phép và đủ bằng chứng, ứng dụng có thể trả đáp án này cùng trích dẫn mà không gọi model. Cách viết không được thay quy trình review, publish hoặc kiểm tra quyền. Nếu có hai đáp án mâu thuẫn, nhánh trả lời trực tiếp dừng.
