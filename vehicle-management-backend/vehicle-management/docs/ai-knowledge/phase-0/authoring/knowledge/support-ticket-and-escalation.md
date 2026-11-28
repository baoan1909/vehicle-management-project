---
title: Phiếu hỗ trợ, hội thoại và yêu cầu quản lý xem xét
slug: support-ticket-and-escalation
documentVersion: 2.1.0
status: DRAFT
language: vi
accessScope: CUSTOMER
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Code FE/BE, domain policy, access guard, Flyway và test đợt 1
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-24
---

## Mục đích
Hướng dẫn người gửi tạo, theo dõi, tiếp tục trao đổi và yêu cầu xem xét phiếu của chính mình; phân biệt dữ liệu thực tế với hướng dẫn tĩnh.

## Đối tượng
Khách hàng có quyền sử dụng chức năng hỗ trợ. Nhân sự đối tác gửi hỗ trợ theo luồng và quyền riêng; không tự được đọc thông tin riêng của khách hàng. Hướng dẫn công khai về loại hội thoại áp dụng cho cả hai nhóm; quy trình nội bộ chi tiết chưa nằm trong tài liệu này.

## Phạm vi
Phiếu hỗ trợ lỗi chức năng hệ thống; không thay thế báo mất thẻ, thanh toán, đăng ký vé hoặc các module xử lý nghiệp vụ.

## Thuật ngữ
Người gửi là tài khoản sở hữu phiếu. Người xử lý là nhân sự có quyền và được phân công trong hàng đợi hỗ trợ hệ thống. Tự nhận phiếu là nhận xử lý một phiếu chưa gán; yêu cầu quản lý xem xét là đề nghị người có quyền kiểm tra việc hỗ trợ hoặc phân công.

## Điều kiện trước
Cần quyền tạo hoặc đọc phiếu của chính mình tương ứng, tài khoản được phép sử dụng và danh mục còn khả dụng. Không mặc định phải được duyệt hồ sơ mới dùng được mọi chức năng hỗ trợ: hệ thống kiểm tra quyền tại thời điểm thao tác. Tạo từ chat cần đúng loại hội thoại, là thành viên hợp lệ và là chủ hội thoại gửi yêu cầu. Danh mục bị ẩn có thể do trạng thái hoặc quyền.

## Luồng chính
1. Mở trang Yêu cầu của tôi hoặc trợ lý có quyền; chọn Tạo phiếu hỗ trợ.
2. Chọn danh mục, nhập tiêu đề và nội dung chi tiết: chức năng, bước thực hiện, lỗi thực tế; không gửi thông tin bí mật.
3. Nút làm mới danh mục nằm cùng hàng với nhãn; dùng khi cần tải lại, không phải polling liên tục.
4. Gửi biểu mẫu hoặc kiểm tra và xác nhận thẻ yêu cầu nếu nhờ trợ lý tạo. Chỉ thông báo thành công khi hệ thống trả kết quả.
5. Theo dõi phiếu mới mở, đang xử lý và người xử lý qua màn hình được phép xem.
6. Khi có liên kết hội thoại, dùng Tiếp tục trao đổi hoặc Xem trao đổi. Không đưa phiếu vào chat khách hàng–đối tác.
7. Không hài lòng: dùng Yêu cầu quản lý xem xét khi đủ quyền/điều kiện; không yêu cầu chatbot tự phân công lại.

## Luồng thay thế
- Thiếu nội dung/danh mục/tiêu đề: hiển thị đúng trường thiếu, giữ bản nháp.
- Quyền hoặc danh mục thay đổi trong lúc nhập: tải lại danh sách khả dụng, không giữ lựa chọn trái phép.
- Thử gửi lại phải tránh tạo trùng; kiểm tra kết quả lần trước, không tự tạo phiếu mới khi chưa rõ đã gửi thành công hay chưa.
- Câu “tạo phiếu như thế nào” chỉ nhận hướng dẫn; “đừng tạo phiếu” không phát sinh action ghi.

## Luồng lỗi
Danh mục ngừng hoạt động, thiếu quyền, sai loại hội thoại, thiếu dữ liệu, yêu cầu trùng hoặc trạng thái xung đột đều có thông báo tương ứng từ hệ thống. Không khuyên né quyền hoặc bỏ kiểm tra. Thiếu tài liệu hướng dẫn khác với dịch vụ trả lời hay tra cứu đang tạm lỗi; khi cần hướng dẫn người dùng xem Yêu cầu của tôi trước khi gửi lại.

## Trạng thái và chuyển trạng thái
Vòng đời cơ bản: mới mở → đang xử lý → đã giải quyết → đã đóng. Tự nhận phiếu gán người xử lý; phản hồi đầu tiên bắt đầu xử lý theo quy trình. Mở lại phiếu đã giải quyết sẽ trở về mới mở nếu chưa có người xử lý, hoặc đang xử lý nếu đã có người được phân công. Phiếu đã đóng không nhận nội dung trao đổi mới gắn với phiếu.
Hệ thống hiện tự đóng sau 24 giờ không hoạt động kể từ mốc giải quyết hoặc trao đổi hợp lệ. Trao đổi gắn với phiếu đã giải quyết có thể gia hạn mốc này mà không tự mở lại phiếu. Giao diện có thể hiển thị Xem trao đổi thay vì Tiếp tục trao đổi. Không mặc định phiếu đã giải quyết luôn chỉ được đọc, có tối đa hai lần mở lại hoặc có cửa sổ bảy ngày khi chức năng đang chạy chưa áp dụng những giới hạn đó.

## Câu hỏi khách hàng thường gặp
- “tao phieu ho tro nhu the nao” → chọn danh mục, tiêu đề, nội dung và gửi/xác nhận.
- “tao giup toi phieu bao loi dang nhap” → yêu cầu ghi; cần đủ dữ liệu và xác nhận.
- “xem phieu cua toi” → cần quyền và tool, không suy đoán trạng thái.
- “phieu da giai quyet co chat duoc khong” → xem trạng thái/nút thực tế; hội thoại và phiếu không đồng nhất.
- “ho tro khong tot thi can lam gi” → dùng yêu cầu xem xét theo quyền/điều kiện.
- Không tạo phiếu nhé, tôi chỉ muốn biết cách làm: chỉ hướng dẫn chọn danh mục, nhập tiêu đề và nội dung, kiểm tra rồi gửi. Chưa gửi hoặc xác nhận thì chưa có phiếu mới.
- Phiếu đã giải quyết nghĩa là gì? Là đã giải quyết; kiểm tra nút trao đổi hoặc mở lại trên màn hình. Không mặc định phiếu đã đóng.
- Phiếu đóng rồi cần làm gì? Xem lại kết quả xử lý; nếu vẫn còn lỗi và không có nút mở lại, liên hệ hỗ trợ để được hướng dẫn. Không hứa tự mở lại phiếu đã đóng.
- Vì sao danh mục hỗ trợ bị ẩn? Có thể danh mục ngừng hoạt động hoặc tài khoản không được phép chọn. Dùng nút tải lại danh mục, đọc thông báo; không chọn danh mục ngoài quyền.
- Xác nhận thẻ yêu cầu tạo phiếu thế nào? Kiểm tra danh mục, tiêu đề và nội dung trên thẻ yêu cầu trong khung chat; chỉ chọn xác nhận nếu đúng. Hủy nếu không muốn gửi; chỉ báo tạo thành công sau khi hệ thống ghi nhận.
- Đã làm rồi vẫn lỗi sau hướng dẫn tạo phiếu thì sao? Hỏi lỗi ở bước chọn danh mục, nhập nội dung, gửi hay xác nhận; không mặc định đã có phiếu để tra cứu.
- Tạo phiếu cần nhập nội dung gì? Tiêu đề nêu ngắn gọn lỗi; nội dung chi tiết cần có chức năng đang dùng, các bước đã làm, kết quả mong muốn và lỗi thực tế hoặc thông báo đang thấy. Không ghi mật khẩu, mã xác minh hay thông tin thanh toán nhạy cảm.
- Gửi biểu mẫu có phải xác nhận thêm thẻ trong chat không? Không gộp hai cách gửi: với biểu mẫu, điền đủ rồi chọn Gửi; với yêu cầu tạo qua trợ lý, kiểm tra thẻ yêu cầu và chọn Xác nhận. Chờ kết quả hệ thống ở cách đang dùng, không gửi hai lần.
- Làm sao gửi nội dung lỗi đầy đủ? Viết tiêu đề ngắn gọn, tên chức năng đang dùng, các bước đã thực hiện, kết quả mong muốn và lỗi thực tế hoặc thông báo đang thấy. Kiểm tra nội dung trước khi gửi; không ghi mật khẩu, mã xác minh hay thông tin thanh toán nhạy cảm.
- Phiếu báo lỗi cần làm gì trước? Với phiếu hỗ trợ lỗi hệ thống, chọn danh mục phù hợp, nhập tiêu đề và nội dung nêu chức năng, bước đã làm và lỗi thực tế, rồi kiểm tra trước khi gửi. Phiếu hỗ trợ này khác phiếu báo mất thẻ tại bãi xe.
- Hướng dẫn mô tả lỗi khi tạo phiếu? Với phiếu hỗ trợ lỗi hệ thống, tiêu đề nêu ngắn gọn lỗi; nội dung cần có tên chức năng, các bước đã làm, kết quả mong muốn và lỗi thực tế hoặc thông báo đang thấy. Không gửi mật khẩu, mã xác minh hay thông tin thanh toán nhạy cảm. Đây không phải bước lập phiếu báo mất thẻ.

## Điều chatbot được phép trả lời
Hướng dẫn UI, điều kiện, ý nghĩa trạng thái và giới hạn. Trả lời bước tiếp theo phù hợp thay vì liệt kê toàn bộ vòng đời không liên quan.

## Điều chatbot phải dùng chức năng được cấp quyền
Chat có thể đọc danh sách hoặc chi tiết phiếu của chính bạn khi đủ quyền. Tra cứu vé là chức năng khác, không thay cho tra cứu phiếu. Nhờ trợ lý tạo phiếu cần kiểm tra và xác nhận trước. Trợ lý chưa thực hiện yêu cầu quản lý xem xét, phân công, đổi người xử lý, giải quyết hoặc đóng phiếu; hãy dùng chức năng được phép trên giao diện.

## Điều chatbot phải từ chối
Đọc phiếu người khác, vượt quyền, tự phân công/đóng/duyệt escalation, tạo trước xác nhận, tạo/gắn/chia sẻ phiếu trong chat khách hàng–đối tác hoặc dùng dữ liệu tĩnh làm trạng thái riêng.

## Khi nào chuyển nhân viên
Yêu cầu người thật, khiếu nại, chức năng tra cứu hoặc dịch vụ trả lời bị lỗi, không có hội thoại hợp lệ hoặc trạng thái bất thường. Người xử lý hàng đợi hệ thống được xác định bằng quyền và phân công thực tế, không bằng tên vai trò được gõ trong chat.

## Lưu ý khi trả lời và đối chiếu kết quả
Nếu đã bấm Gửi mà chưa rõ kết quả, xem Yêu cầu của tôi trước khi gửi lại. Nếu chỉ hỏi cách tạo, trợ lý chỉ hướng dẫn; nếu nhờ tạo qua chat, phải kiểm tra và xác nhận yêu cầu trước. Phiếu hỗ trợ không thay phiếu báo mất thẻ hoặc yêu cầu thanh toán.

<!-- authoring-only:start -->
## Nguồn đối chiếu
- Backend: `SupportRequesterContextResolver`, `SupportTicketAccessGuard`, `SupportTicketPolicy`, `SupportTicketUseCaseImpl`, `SupportTicketConversationService`, `SupportTicketChatMessageContextService`, escalation policy/use case.
- FE: `SupportPage`, `CreateSupportTicketDialog`, `SupportTicketRequesterActions`, `SupportFloatingWidget`, `OperationsSupportCenterPage`.
- Test: các SupportTicket*Test, `PublicSupportSecurityIT`, `PendingCustomerSupportFlowIT`, `tests/support-flows.behavior.spec.ts`.
- Ma trận: `../evaluation/batch-01-traceability.md`; dataset `batch-01-questions.jsonl`.

## Nội dung TBD
Các SLA/giới hạn reopen/appeal đã nêu trong decision register cũ là chính sách mục tiêu nếu chưa có enforcement. Không triển khai chính sách mới trong đợt này chỉ để khớp tài liệu. Transcript retention và quy trình tranh chấp cần quyết định riêng. DRAFT chưa được publish tự động.

## Lịch sử phiên bản
- 2.1.0, 2026-11-24: rà soát lại với code hiện tại; làm rõ điều kiện và giới hạn trả lời, chuẩn hóa ngôn ngữ; tiếp tục DRAFT. Kiểm thử và giới hạn bằng chứng tại báo cáo ALL_BATCHES_REVIEW_STATUS.vi.md.
2.0.0 — 19/11/2026: cập nhật requester account/quyền, category, ranh giới chat, trạng thái RESOLVED và tool thực có. Không tự thay đổi policy nghiệp vụ.
<!-- authoring-only:end -->
