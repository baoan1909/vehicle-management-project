# AI Project Governance — CoParking

## 1. Mục đích

Tài liệu này quy định cách nhóm sử dụng AI trong đồ án **Phát triển website quản lý hệ thống bãi xe thông minh (CoParking)**. AI là công cụ hỗ trợ phân tích, thiết kế, lập trình, kiểm thử và viết tài liệu; mọi thành viên vẫn phải hiểu, kiểm chứng và chịu trách nhiệm với sản phẩm cuối cùng.

Mục tiêu là vừa khai thác AI hiệu quả, vừa bảo đảm tính trung thực học thuật, chất lượng kỹ thuật, khả năng bảo vệ trước hội đồng và tính truy vết của quá trình phát triển.

## 2. Phạm vi và trách nhiệm

- Áp dụng cho tất cả thành viên, mọi mã nguồn, migration, cấu hình CI/CD, tài liệu, kiểm thử và nội dung do AI hỗ trợ tạo ra.
- Thành viên sử dụng AI là người chịu trách nhiệm cuối cùng về thay đổi mình đưa vào dự án.
- Không được merge hoặc trình bày một phần việc nếu chưa hiểu luồng nghiệp vụ, tác động dữ liệu và cách kiểm tra phần việc đó.
- Người review cần kiểm tra cả tính đúng đắn của yêu cầu nghiệp vụ lẫn chất lượng kỹ thuật, không chỉ tin vào câu trả lời của AI.

## 3. Nguyên tắc bắt buộc

1. **AI hỗ trợ, con người quyết định.** Không sao chép nguyên trạng kết quả AI khi chưa đọc, chạy và đánh giá.
2. **Không bịa minh chứng.** Không tạo dữ liệu giả, kết quả test giả, ảnh chụp giả, log giả hoặc tài liệu mô tả tính năng chưa có.
3. **Bảo mật dữ liệu.** Không đưa mật khẩu, API key, token, khóa riêng, dữ liệu cá nhân thật hoặc chuỗi kết nối production vào prompt, chat, commit hay ảnh minh chứng.
4. **Tôn trọng kiến trúc.** Thay đổi phải phù hợp cấu trúc module hiện có, quy ước đặt tên, phân tầng backend/frontend và cơ chế phân quyền của dự án.
5. **Kiểm tra nguồn.** Với thư viện, API, cấu hình bảo mật hoặc kiến thức có thể thay đổi, đối chiếu tài liệu chính thức trước khi áp dụng.
6. **Ưu tiên thay đổi nhỏ, có thể kiểm chứng.** Mỗi thay đổi cần có mục tiêu, phạm vi, kiểm thử và khả năng rollback rõ ràng.

## 4. Mục tiêu chất lượng cần duy trì

| Nội dung | Yêu cầu tối thiểu |
| --- | --- |
| Nghiệp vụ | Mỗi chức năng có luồng chính, điều kiện biên, quyền thực hiện và dữ liệu đầu vào/đầu ra rõ ràng. |
| Phân quyền SaaS | Không hard-code theo người dùng. Phạm vi dữ liệu phải được kiểm soát bằng role, permission và organization_id/parking_lot_id phù hợp. |
| CSDL và migration | Migration có version duy nhất, chạy được trên cơ sở dữ liệu mới và an toàn với dữ liệu hiện hữu. Không phụ thuộc một dòng dữ liệu chỉ có ở máy cá nhân. |
| Kiểm thử | Thay đổi quan trọng phải có kiểm thử hoặc kịch bản test thủ công được ghi nhận; luôn kiểm tra quyền truy cập sai phạm vi. |
| CI/CD | Pipeline hướng đến các chặng build, phân tích tĩnh/lint, test, quét bảo mật/phụ thuộc, Docker, triển khai, health check và giám sát. |
| Minh chứng | Commit/PR, ảnh giao diện, kết quả test, log triển khai và tài liệu phải nhất quán với chức năng thực tế. |
| Hiểu biết cá nhân | Thành viên phụ trách phải giải thích được: vấn đề, thiết kế, luồng dữ liệu, permission, kiểm thử, rủi ro và lý do chọn giải pháp. |

## 5. Quy trình bắt buộc khi dùng AI

1. Xác định vấn đề và tiêu chí chấp nhận trước khi hỏi AI.
2. Cung cấp đủ ngữ cảnh kỹ thuật cần thiết nhưng loại bỏ secret và dữ liệu nhạy cảm.
3. Yêu cầu AI giải thích phương án, rủi ro, file dự kiến tác động và cách kiểm thử; không chỉ yêu cầu sinh mã.
4. Tự đọc kết quả, đối chiếu kiến trúc dự án và tài liệu chính thức khi cần.
5. Chỉnh sửa theo từng phần nhỏ; không để AI thay đổi lan sang các khu vực ngoài phạm vi yêu cầu.
6. Build, lint, test và kiểm tra thủ công các luồng liên quan.
7. Review chéo nếu thay đổi liên quan đến migration, bảo mật, permission, thanh toán, CI/CD hoặc dữ liệu nhiều tenant.
8. Ghi nhận việc dùng AI vào AI Usage Log trước khi tạo PR hoặc merge.

## 6. Quy tắc kỹ thuật của dự án

### Backend

- Tuân thủ cấu trúc package/layer hiện hữu; không tạo class hoặc package trùng vai trò.
- Authorization phải được kiểm tra ở backend. Frontend chỉ ẩn/disable nút để cải thiện trải nghiệm, không phải cơ chế bảo mật.
- System Admin có thể giám sát đa tenant theo quyền, nhưng không được can thiệp nghiệp vụ bãi xe nếu policy không cho phép.
- Partner Admin được vận hành tài nguyên thuộc organization của mình; Parking Manager và Employee chỉ trong parking lot được phân công.
- Truy vấn danh sách và thao tác chi tiết đều phải áp dụng scope, không chỉ lọc ở giao diện.
- Mọi migration mới phải có version Flyway duy nhất; ưu tiên migration idempotent khi phù hợp và không gắn với dữ liệu riêng của máy phát triển.

### Frontend

- Điều hướng, menu, button và hành động cần dựa trên permission/capability, không hard-code theo email hoặc tên role trong từng màn hình.
- Với quyền chỉ xem, hiển thị dữ liệu cần thiết nhưng disable/ẩn thao tác ghi một cách rõ ràng.
- Mọi trang có dữ liệu theo bãi/partner cần thể hiện scope đang xem và không được làm lộ dữ liệu ngoài phạm vi.

### Kiểm thử và bảo mật

- Test cả trường hợp được phép và bị từ chối quyền.
- Không log secret, token, mật khẩu hoặc dữ liệu định danh không cần thiết.
- Kiểm tra dependency/license khi thêm thư viện mới.
- Với thanh toán, ví điện tử hoặc callback bên thứ ba, bắt buộc có kiểm tra chữ ký, idempotency, đối soát và kịch bản lỗi.

## 7. AI Usage Log dùng chung

**Nguồn ghi nhận chính thức:** Google Sheet dùng chung của nhóm.

- Liên kết Google Sheet: TODO — dán liên kết có quyền truy cập của nhóm tại đây.
- Mỗi thành viên ghi nhận trong ngày, muộn nhất trước khi tạo PR hoặc merge phần việc có AI hỗ trợ.
- Không ghi prompt chứa secret, token, mật khẩu, thông tin ngân hàng hoặc dữ liệu cá nhân thật.
- Log trên Google Sheet là nguồn chi tiết; repository chỉ lưu quy định này, không sao chép dữ liệu log rời rạc vào mã nguồn.

Google Sheet nên duy trì ít nhất ba tab:

1. **AI Usage Log:** Ngày, thành viên, công cụ/model, mục tiêu, prompt tóm tắt, file/chức năng tác động, AI hỗ trợ gì, phần nhóm tự sửa/kiểm chứng, test đã chạy, commit/PR, trạng thái.
2. **AI Issues & Corrections:** Sai lệch/hallucination AI, rủi ro phát hiện được, cách sửa, người phát hiện, bằng chứng và bài học rút ra.
3. **Review Checklist:** Các mục review trước merge và người xác nhận.

## 8. Definition of Done

Một hạng mục chỉ được xem là hoàn thành khi:

- Yêu cầu nghiệp vụ và phạm vi role/tenant đã rõ.
- Mã nguồn tuân thủ kiến trúc và không phát sinh hard-code trái quy tắc.
- Migration, API và UI liên quan đã được kiểm tra tương thích.
- Build/lint/test phù hợp đã chạy thành công, hoặc lý do chưa chạy được được ghi rõ.
- Đã kiểm tra các trường hợp lỗi, dữ liệu rỗng và truy cập không đúng quyền.
- Có minh chứng liên kết tới commit/PR, ảnh hoặc kết quả test khi phù hợp.
- AI Usage Log được cập nhật nếu AI có tham gia.
- Người phụ trách giải thích được thay đổi trước nhóm hoặc hội đồng.

## 9. Checklist trước khi merge

- [ ] Có mô tả mục tiêu, phạm vi và tiêu chí chấp nhận.
- [ ] Không có secret hoặc dữ liệu nhạy cảm trong mã, config, log hay ảnh.
- [ ] Không hard-code email, account ID, organization ID hoặc parking lot ID để cấp quyền.
- [ ] Backend đã kiểm tra permission và data scope.
- [ ] Frontend phản ánh đúng quyền: không hiển thị thao tác không được phép hoặc disable có giải thích.
- [ ] Migration có version Flyway duy nhất và không gây lỗi trên môi trường mới.
- [ ] Đã chạy build/test/lint phù hợp.
- [ ] Đã kiểm tra luồng thành công, luồng lỗi và luồng không có quyền.
- [ ] AI Usage Log và minh chứng commit/PR đã được cập nhật.

## 10. Chuẩn bị bảo vệ

Mỗi thành viên cần chuẩn bị để trả lời được:

- Bài toán và người dùng nào được giải quyết.
- Luồng dữ liệu đi từ giao diện, API, service đến cơ sở dữ liệu.
- Cách phân tách dữ liệu giữa System Admin, Partner Admin, Parking Manager, Employee và Customer.
- Vì sao lựa chọn kiến trúc, thư viện và cách kiểm tra quyền hiện tại.
- Cách kiểm thử, cách xử lý lỗi, hạn chế còn tồn tại và hướng phát triển tiếp theo.
- AI đã hỗ trợ phần nào, nhóm đã kiểm chứng/sửa gì và ví dụ về vấn đề AI từng đưa ra chưa đúng.

