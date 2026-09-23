<!--
title: Phiếu hỗ trợ, hội thoại và yêu cầu quản lý xem xét
slug: support-ticket-and-escalation
documentVersion: 1.1.0
status: DRAFT
language: vi
accessScope: CUSTOMER
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Support ticket/chat/escalation application, domain, FE và Flyway
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-09-14
-->

# Phiếu hỗ trợ, hội thoại và yêu cầu quản lý xem xét

## Mục đích
Giải thích toàn bộ vòng đời phiếu, người hỗ trợ, hội thoại cũ, phân công lại và escalation.

## Đối tượng
Customer, nhân viên được phân công và người có permission review/assign.

## Phạm vi
Tạo phiếu từ form/chat, claim/assign/reassign, trả lời, resolve/reopen/close, conversation link và yêu cầu quản lý xem xét.

## Thuật ngữ
- Assignee: account nhân viên hiện được gán.
- Claim: nhân viên tự nhận phiếu OPEN chưa gán.
- Escalation: yêu cầu quản lý đánh giá chất lượng/phân công.
- Active/Historical link: liên kết hội thoại đang dùng hoặc lịch sử.

## Điều kiện trước
Customer ACTIVE+APPROVED, category ACTIVE và permission OWN. Escalation chỉ áp dụng ticket OPEN/IN_PROGRESS đã có assignee.

## Luồng chính
1. Customer tạo phiếu; Backend dùng idempotency và tạo trạng thái OPEN, chưa gán.
2. Người có quyền assign hoặc nhân viên có quyền claim nhận phiếu.
3. Assignee hiện tại mở customer conversation và gửi phản hồi đầu tiên; ticket tự chuyển IN_PROGRESS.
4. Assignee xử lý → RESOLVED; trạng thái này chỉ đọc. Customer phải reopen trước khi tiếp tục chat; ticket RESOLVED có thể CLOSED.
5. Thẻ phiếu cũ trong chatbot chỉ là tham chiếu. Nút “Tiếp tục trao đổi” phải mở active customer conversation bằng API ownership-safe.
6. Nếu không hài lòng, customer tạo action “Yêu cầu quản lý xem xét”; reviewer độc lập chọn giữ nguyên hoặc reassign.

## Luồng thay thế
Reassign OPEN/IN_PROGRESS làm hội thoại cũ historical và người mới mở conversation active khi phản hồi. Trong lúc escalation chờ, assignee hiện tại vẫn tiếp tục hỗ trợ.

## Luồng lỗi
Category inactive, idempotency payload conflict, non-assignee reply, sai status, escalation trùng/rate limit, ticket chưa gán hoặc reviewer chính là assignee.

## Trạng thái và chuyển trạng thái
Ticket: `OPEN → IN_PROGRESS → RESOLVED → CLOSED`; `RESOLVED → OPEN` nếu chưa gán hoặc `IN_PROGRESS` nếu còn assignee khi reopen. Escalation: `PENDING → APPROVED|REJECTED`; decision approved có thể `KEEP_CURRENT` hoặc `REASSIGN`.

## Câu hỏi khách hàng thường gặp
- Ai đang xử lý phiếu của tôi?
- Làm sao tiếp tục trao đổi phiếu cũ?
- Tôi không hài lòng với nhân viên thì làm gì?
- Vì sao chưa có nút tiếp tục trao đổi?

## Điều chatbot được phép trả lời
Vòng đời, điều kiện, quyền của khách, ý nghĩa assignee và cách dùng nút điều hướng.

## Điều chatbot phải dùng business tool
Danh sách/chi tiết phiếu OWN, active conversation, escalation hiện tại và assignee đã được phép hiển thị.

## Điều chatbot phải từ chối
Tự assign/reassign/resolve/close/review escalation, đọc ticket người khác hoặc tuyên bố đã tạo phiếu trước Backend success.

## Khi nào chuyển nhân viên
User yêu cầu người thật, complaint/escalation, không có active conversation, tool fail hoặc ticket không chuyển đúng trạng thái.

## Nguồn đối chiếu
- `SupportTicketController`, `SupportTicketEscalationController`, `ChatConversationController`.
- Support use case/policy/access guard/conversation service.
- Escalation use case/policy.
- FE support widget, `SupportPage`, `SupportTicketManagementPage`, `OperationsSupportCenterPage`.
- support/chat/approval tables và migrations 20260901–20260906.

## Nội dung TBD
SLA/reopen/escalation/appeal/conversation đã chốt tại TBD-002 và TBD-018 đến TBD-021: first-human-response 30 phút/2 giờ/8 giờ/48 giờ; reopen tối đa 2 lần trong cửa sổ 7 ngày; rate limit escalation kết hợp ticket/account; một appeal trong 7 ngày; RESOLVED phải reopen trước khi chat. Transcript retention cần decision riêng.

## Lịch sử phiên bản
- 1.1.0: ghi nhận TBD-002 và TBD-018..TBD-021 APPROVED ngày 2026-09-14; runtime gaps được giữ fail-safe.
- 1.0.0: baseline ngày 2026-09-14.
