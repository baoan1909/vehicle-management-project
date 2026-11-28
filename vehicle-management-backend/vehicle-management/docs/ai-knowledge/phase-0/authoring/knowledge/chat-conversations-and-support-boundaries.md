---
title: Loại hội thoại và giới hạn hỗ trợ CoParking
slug: chat-conversations-and-support-boundaries
documentVersion: 2.2.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Code FE/BE, domain policy, access guard, Flyway và test đợt 1
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-24
---

## Mục đích
Giúp người dùng chọn đúng kênh: trao đổi khách hàng–đối tác, trợ lý tự động hoặc hỗ trợ hệ thống. Không hướng dẫn gửi phiếu hỗ trợ giữa khách hàng và đối tác.

## Đối tượng
Khách hàng và nhân sự đối tác; nội dung chỉ mô tả ranh giới công khai, không cấp quyền quản trị.

## Phạm vi
Chat thường, chia sẻ đơn hàng, trợ lý tự động, tạo/xem phiếu và trao đổi với người xử lý. Không mô tả truy cập quản trị nội bộ hoặc dữ liệu của người khác.

## Thuật ngữ
- Chat khách hàng–đối tác: trao đổi trực tiếp về nghiệp vụ giữa khách hàng và nhân sự đối tác.
- Chat trợ lý CoParking: câu trả lời tự động, khác trao đổi với người thật.
- Chat hỗ trợ hệ thống: người gửi yêu cầu trao đổi với người xử lý được phân công.
- Hệ thống kiểm tra quyền tài khoản và thành viên hội thoại; tên kênh không tự cấp quyền.

## Điều kiện trước
Có quyền đọc/gửi và là thành viên hợp lệ. Chia sẻ đơn hàng cần thêm quyền đọc đơn phù hợp; người dùng không thể dùng URL/ID để mở dữ liệu ngoài phạm vi.

## Luồng chính
1. Muốn hỏi đối tác về đơn hàng: mở chat khách hàng–đối tác và gửi tin nhắn.
2. Muốn hướng dẫn dùng CoParking: mở Trợ lý hỗ trợ CoParking; đây không phải người thật.
3. Cần báo lỗi hệ thống: dùng Tạo phiếu trong luồng hỗ trợ hệ thống hoặc trang Yêu cầu của tôi.
4. Khách hàng mở Yêu cầu của tôi trong khu vực khách hàng; nhân sự đối tác mở Yêu cầu của tôi trong khu vực nhân sự nếu có quyền.
5. Khi đã có người xử lý và liên kết hội thoại, dùng Tiếp tục trao đổi/Xem trao đổi theo nút trên phiếu.
6. Tạo phiếu và lịch sử yêu cầu không xuất hiện trong chat khách hàng–đối tác. Quyền tạo phiếu không làm thay đổi giới hạn này.

## Luồng thay thế
- Guest dùng trợ lý công khai và form gửi yêu cầu với thông tin liên hệ/cơ chế bảo vệ tương ứng.
- Icon đơn hàng chỉ hiện khi đủ quyền; khách hàng cần quyền đọc đơn của mình, phía đối tác cần quyền đọc đơn trong phạm vi phụ trách theo chính sách hiện tại.
- Trợ lý CoParking có nền xanh nhạt và được đặt đầu danh sách, nhưng không thay đổi kênh người dùng đang chủ động chọn.

## Luồng lỗi
- Không có nút tạo phiếu trong chat đối tác là đúng thiết kế, không phải lỗi dropdown.
- Không có icon đơn hàng: kiểm tra quyền/ngữ cảnh, không khuyên đổi ID request.
- “lỗi/loi/không được”: trợ lý hỏi chức năng/bước khi thiếu chủ đề.
- “vẫn lỗi” sau chủ đề đăng nhập: tiếp nối đăng nhập; sau yêu cầu ghi/tra cứu riêng tư không được tự tái thực hiện hành động từ lịch sử.

## Trạng thái và chuyển trạng thái
Hội thoại có thể đang hoạt động, đã lưu trữ hoặc đã đóng; phiếu có thể mới mở, đang xử lý, đã giải quyết hoặc đã đóng. Hai vòng đời độc lập; đóng/giải quyết phiếu không đồng nghĩa xóa hội thoại.

## Câu hỏi khách hàng thường gặp
- Trợ lý chatbot khác hội thoại với nhân viên như thế nào? Trợ lý chatbot là hệ thống trả lời tự động và không phải nhân viên. Hội thoại với nhân viên là trao đổi với người thật. Khi cần người thật, dùng đúng kênh hỗ trợ hoặc yêu cầu chuyển nhân viên; việc đó không tự tạo phiếu hay gửi tin thay bạn.
- Nhân sự đối tác báo lỗi cho hệ thống như thế nào? Mở Yêu cầu của tôi hoặc kênh hỗ trợ hệ thống phù hợp, chọn tạo phiếu nếu có quyền và kiểm tra nội dung trước khi xác nhận. Không tạo phiếu trong chat trực tiếp giữa khách hàng và nhân sự đối tác; yêu cầu vận hành bãi dùng chức năng nghiệp vụ tương ứng.
- “chat khach hang doi tac co tao phieu ho tro duoc khong” → không; dùng hỗ trợ hệ thống.
- “nhan su doi tac gui ho tro he thong nhu the nao” → Yêu cầu của tôi hoặc trợ lý có quyền.
- “vi sao chat khong co icon don hang” → phụ thuộc loại chat/quyền.
- “tro ly chatbot co phai nhan vien khong” → không; yêu cầu người thật đi theo luồng hỗ trợ.
- “lam sao tao phieu” → hướng dẫn, không tự tạo.

## Điều chatbot được phép trả lời
Giải thích loại chat, vị trí UI công khai, quyền tổng quát, ranh giới và cách chuyển sang hỗ trợ đúng kênh.

## Điều chatbot phải dùng chức năng được cấp quyền
Danh sách, chi tiết phiếu của chính người hỏi và dữ liệu đơn hàng phải đọc qua chức năng đã kiểm tra quyền. Không lấy thông tin người xử lý, trạng thái riêng hoặc đơn hàng từ tài liệu.

## Điều chatbot phải từ chối
Tạo, chia sẻ hoặc gắn phiếu vào CUSTOMER_DIRECT; đưa phiếu của khách hàng này cho đối tác/khách hàng khác; thay đổi người xử lý bằng yêu cầu trong chat thường.

## Khi nào chuyển nhân viên
Người dùng cần người thật, yêu cầu hỗ trợ hệ thống hoặc sự cố chưa xử lý được. Chuyển sang người hỗ trợ không tự tạo phiếu hay tự gửi tin cho một người khác.

## Lưu ý khi trả lời và đối chiếu kết quả
Chọn đúng kênh trước khi gửi: chat với đối tác để trao đổi nghiệp vụ; trợ lý tự động để hỏi cách dùng; hỗ trợ hệ thống để báo lỗi chức năng. Có quyền tạo phiếu vẫn không cho phép tạo phiếu trong chat khách hàng–đối tác. Tin nhắn của trợ lý không được coi là nhân viên đã phản hồi.

<!-- authoring-only:start -->
## Nguồn đối chiếu
- Backend: `SupportTicketConversationService`, `SupportTicketChatMessageContextService`, `SupportTicketUseCaseImpl`, `ChatConversationUseCaseImpl`.
- FE: `chatActionAccess.ts`, `supportApi.ts`, `OperationsSupportCenterPage`, `SupportFloatingWidget`.
- Test: `SupportTicketConversationServiceTest`, `SupportTicketChatMessageContextServiceTest`, `tests/support-flows.behavior.spec.ts`.
- Migration phân quyền: `V20261117100000__account_owned_support_workflow.sql`; bảng chat/ticket/link trong operations.

## Nội dung TBD
Các kênh ngoài các loại đã kiểm chứng, hỗ trợ đa tổ chức riêng tư và SLA cụ thể không được tự suy diễn. Nếu thêm kênh mới phải rà soát lại permission, dữ liệu và test.

## Lịch sử phiên bản
- 2.2.0, 2026-11-24: rà soát lại với code hiện tại; làm rõ điều kiện và giới hạn trả lời, chuẩn hóa ngôn ngữ; tiếp tục DRAFT. Kiểm thử và giới hạn bằng chứng tại báo cáo ALL_BATCHES_REVIEW_STATUS.vi.md.
- 2.1.0, 2026-11-23: bổ sung giải thích nghiệp vụ theo lỗi truy xuất hồi quy đợt 4, giữ DRAFT.
2.0.0 — tài liệu mới đợt 1 ngày 19/11/2026; DRAFT; mô tả ranh giới hệ thống đã được xác nhận trong phiên làm việc.
<!-- authoring-only:end -->
