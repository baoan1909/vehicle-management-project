# Quản lý tri thức AI

Màn hình `/admin/ai-knowledge` mặc định mở **Tổng quan**. Mục **Cần làm tiếp** dựa vào số liệu từ API tổng quan; không dùng số lượng tài liệu để kết luận chatbot đã đạt độ chính xác.

## Thứ tự thao tác

1. **Nguồn & quyền xem:** tạo nhóm tài liệu và chọn người được xem. Có thể dùng lại nguồn đang hoạt động.
2. **Tải lên & duyệt:** tải tệp vào nguồn, đợi xử lý, mở **Xem & duyệt** để kiểm tra nội dung và quyền xem, rồi xác nhận duyệt. Nếu không tải được bản xem trước thì chưa cho duyệt từ giao diện.
3. **Bật tìm kiếm:** kiểm tra phiên bản hệ thống chuẩn bị sau khi duyệt. Đợi hoàn tất rồi **Bật sử dụng** với dịch vụ AI phù hợp. Chỉ tạo phiên bản thủ công khi cần. Câu hỏi thường gặp đã duyệt có thể được dùng trực tiếp mà không cần chỉ mục tìm kiếm.
4. **Thử tìm nguồn:** nhập câu hỏi, xem các đoạn nguồn tìm được, thử cách hỏi khác hoặc không dấu. Sau đó thử câu trả lời đầy đủ và hỏi tiếp trong chatbox.

**Theo dõi xử lý** là màn hình phụ để xem tiến độ/lỗi; nó tự cập nhật khi có lượt đang chạy. **Model & kết nối** dẫn đến màn hình cấu hình AI, chỉ hiển thị khi có quyền đọc model. Tổng quan tải khi mở và có nút làm mới; không thêm vòng gọi nền liên tục.

Các bước cho phép mở độc lập, giữ tham số URL và hỗ trợ nút Back của trình duyệt. Liên kết cũ `?tab=sources`, `documents`, `index-versions`, `jobs`, `tests` vẫn dùng được. Không đổi API hoặc phân quyền backend.

## Cách đọc trạng thái

- **Chờ duyệt:** nội dung đã xử lý, cần kiểm tra trước khi cho dùng.
- **Đã duyệt nội dung:** bản tài liệu được duyệt; không đồng nghĩa mọi phiên bản tìm kiếm đã cập nhật.
- **Đang chuẩn bị / Chờ bật sử dụng / Đang dùng:** trạng thái của phiên bản tìm kiếm, tách khỏi trạng thái duyệt tài liệu.
- **Thử tìm nguồn:** kết quả retrieval theo quyền của quản trị viên hiện tại, không phải phép thử câu trả lời sinh bởi Google/Qwen hay quyền của khách hàng.

ID, số chiều, cách chia đoạn và mã chống gửi trùng nằm trong phần mở rộng. Điểm xếp hạng tìm kiếm không hiển thị như phần trăm chính xác.

## Kiểm tra thay đổi giao diện — 25/11/2026

Đã chạy **8/8 kiểm thử trình duyệt qua**, kiểm tra ảnh desktop/mobile và `npm run build` thành công (giới hạn Node heap 768 MB). Các ca bao gồm kho trống, tài liệu chỉ còn bản lưu trữ, xem trước/duyệt, tài khoản chỉ xem, lỗi xem trước, xác nhận bật phiên bản, thử câu không dấu và lỗi tổng quan/tải lại.

```powershell
# Một worker, dùng lại Vite ở 5173 nếu có; nếu chưa có thì tự mở và đóng sau test.
node node_modules/@playwright/test/cli.js test --config=playwright.knowledge.config.ts
npm run build
```

Harness tại `tests/ai-knowledge-workflow-harness.html` dùng các component thật và API giả lập; không gọi DB, Google hoặc Qwen. Kết quả này chứng minh các hành vi UI được kiểm tra, chưa thay thế nghiệm thu API thật hoặc độ chính xác toàn chatbox. Browser output và ảnh nằm trong `test-results/`, bị Git ignore.

Build còn cảnh báo vendor CSS `color-adjust`, Browserslist và kích thước một số bundle; không có lỗi build. Không chỉnh các phần này trong thay đổi giao diện.
