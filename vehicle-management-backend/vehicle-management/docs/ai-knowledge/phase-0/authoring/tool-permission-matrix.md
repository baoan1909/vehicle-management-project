# Ma trận business tool, permission và ownership

> Cập nhật đợt 1 — 19/11/2026: xem [bằng chứng/truy vết](evaluation/batch-01-traceability.md), [manifest](knowledge-manifest.json) và [báo cáo đợt 1](evaluation/batch-01-report.md). Baseline 14/09/2026 được giữ để truy vết; các nhận định runtime có delta dưới đây thay thế nhận định cũ tương ứng. Chưa tự phê duyệt/publish knowledge.


## Tool hiện có

| Tool | Type | Permission | Confirmation | Ownership/scope | Idempotency | Runtime status | Evidence |
|---|---|---|---|---|---|---|---|
| `search_support_knowledge` | READ_ONLY | `SUPPORT_WIDGET_ACCESS_OWN` | Không | PUBLIC + CUSTOMER; tenant hiện truyền null | Không cần | Worker thực thi được | `AiToolRegistry`, `AiToolExecutionService`, `KnowledgeChunkRepository` |
| `get_my_support_ticket` | READ_ONLY | `SUPPORT_TICKET_READ_OWN` | Không | Access guard phải xác nhận ticket thuộc customer hiện tại | Không cần | Worker thực thi được trong actor scope; quyền/ownership được kiểm tra lại | cùng nguồn + `SupportTicketAccessGuard` |
| `list_my_support_tickets` | READ_ONLY | `SUPPORT_TICKET_READ_OWN` | Không | requester accountId resolve từ current account | Không cần | Worker thực thi được trong actor scope; quyền/ownership được kiểm tra lại | `AiToolExecutionService` |
| `get_my_subscription_status` | READ_ONLY | `SUBSCRIPTION_READ_OWN` | Không | current approved customer; không nhận customerId từ model | Không cần | Worker thực thi được trong actor scope; kiểm tra OWN và approved-customer theo policy subscription hiện tại | `AiToolRegistry`, `SubscriptionAccessGuard` |
| `create_support_ticket` | WRITE | `SUPPORT_TICKET_CREATE_OWN` | Có | current customer + assistant conversation | Có, bind tool call | Tool call confirm/deny đã có | `AiToolCallController`, `AiToolCallUseCaseImpl`, support use case |

## Tool đề xuất sau Phase 0

| Tool | Mục đích | Type | Permission | Ownership | Confirmation | Idempotency | Dữ liệu trả về tối thiểu |
|---|---|---|---|---|---|---|---|
| `get_my_profile_status` | Trạng thái account/onboarding | READ_ONLY | `USER_PROFILE_READ_OWN` | current account | Không | — | status, onboardingRequired; không trả identifyCard |
| `list_my_vehicles` | Danh sách xe | READ_ONLY | `CUSTOMER_VEHICLE_READ_OWN` | current approved customer | Không | — | id, masked plate nếu cần, type, status, isDefault |
| `update_my_vehicle` | Add/update/status/default | WRITE | permission cụ thể theo action | owner vehicle | Có | account+action+vehicle/payload hash | kết quả use case |
| `manage_my_subscription` | Create/update pending/cancel | WRITE | CREATE/UPDATE/CANCEL_OWN | owner customer/vehicle/subscription | Có | bắt buộc | status sau Backend xác nhận |
| `get_current_pricing` | Giá đang hiệu lực | READ_ONLY | Public permission hoặc anonymous policy riêng | chỉ active PUBLIC | Không | — | rule/version/effective period |
| `get_my_invoice` | Invoice/payment của tôi | READ_ONLY | `INVOICE_READ_OWN` | invoice.customerId | Không | — | amount/status, không raw transaction secrets |
| `create_vnpay_payment` | Tạo payment URL | WRITE | `PAYMENT_CREATE_OWN` | invoice owner và UNPAID | Có | bắt buộc | short-lived redirect metadata |
| `get_my_active_parking_session` | Phiên đang mở | READ_ONLY | `PARKING_SESSION_READ_OWN` | customer/vehicle owner | Không | — | lot, time, status; media URL theo access policy |
| `list_my_parking_history` | Lịch sử gửi xe | READ_ONLY | `PARKING_SESSION_READ_OWN` | current customer | Không | — | page, session summary |
| `get_my_card_status` | Trạng thái thẻ qua subscription | READ_ONLY | `SUBSCRIPTION_READ_OWN` | subscription owner | Không | — | masked card number/status; không UID |
| `open_ticket_conversation` | Điều hướng đến chat nhân viên | READ_ONLY | `SUPPORT_TICKET_READ_OWN` | ticket owner + active link | Không | — | conversationId/navigation action |
| `prepare_reopen_ticket` | Mở lại ticket RESOLVED | WRITE | `SUPPORT_TICKET_REOPEN_OWN` | ticket owner | Có | bắt buộc | ticket status |
| `request_manager_review` | Escalation/khiếu nại assignee | WRITE | `SUPPORT_TICKET_ESCALATION_CREATE_OWN` | owner; assigned; OPEN/IN_PROGRESS | Có | bắt buộc | escalation PENDING |
| `get_assistant_message_status` | Trạng thái job của message | READ_ONLY | `SUPPORT_WIDGET_ACCESS_OWN` | conversation/message owner | Không | — | status, terminal, public error code |

## Tool bị cấm cho customer chatbot

- Check-in/check-out, điều khiển cổng/barrier hoặc chấp nhận OCR.
- Approve/reject subscription, assign/reclassify/unblock/retire card.
- Mark invoice/payment paid, refund, chỉnh giá.
- Assign/reassign/resolve ticket hoặc review escalation.
- Đọc dữ liệu ALL, dữ liệu employee/admin hay dữ liệu customer khác.
- Đồng bộ model, upload/publish/reindex knowledge.

## Quy tắc thực thi

1. Tool declaration không phải authorization. Mọi lần thực thi phải gọi use case/access guard hiện hữu.
2. Model không được truyền `customerId`, `accountId` hoặc tenant tự do cho tool OWN.
3. Runtime WRITE: `AWAITING_CONFIRMATION` → `EXECUTING` → `SUCCEEDED/FAILED`; xác nhận ghi `confirmedAt/confirmedBy`, không có enum `CONFIRMED`. Deny/expiry đi trực tiếp sang `DENIED/EXPIRED`, không execute.
4. Runtime key là `ai-tool:<inputMessageId>:<toolName>`; owner/conversation và frozen arguments nằm trên AiToolCall. Confirm dùng row lock; SUCCEEDED/EXECUTING retry trả lại trạng thái. Canonical-payload binding xuyên các message khác nhau là yêu cầu mục tiêu cần đánh giá riêng, không tuyên bố đã được bảo đảm bởi chính chuỗi key này.
5. Response được allowlist field; lỗi provider/repository không lộ stack trace hoặc internal identifier.
6. Audit lưu tool name, actor, permission decision, resource, status, timestamp và payload đã redact.

## Permission knowledge management

Migration `V20260917101000__seed_ai_embedding_config_and_knowledge_permissions.sql` đã bổ sung các permission tách biệt. Resolver hiện phân scope từ permission có hiệu lực; trạng thái IMPLEMENTATION_PENDING trong bảng lịch sử 14/09 không mô tả runtime 19/11. Không thêm permission trùng:

- `AI_KNOWLEDGE_READ_ALL`: metadata/preview.
- `AI_KNOWLEDGE_MANAGE_ALL`: upload/edit/archive draft.
- `AI_KNOWLEDGE_APPROVE_ALL`: publish/rollback approval.
- `AI_KNOWLEDGE_REINDEX_ALL`: reindex/rollback index.

Không thêm permission trùng. Role code thực tế là `SYSTEM_ADMIN`; role nhận quyền qua `iam.role_permissions`, không dùng `hasRole` hoặc nhánh logic theo role.

## Ranh giới bổ sung đợt 1

CLARIFICATION, GREETING, SECURITY_REFUSAL, OUT_OF_SCOPE và hướng dẫn STATIC_KNOWLEDGE không khai báo write tool. WRITE_ACTION chỉ cho create_support_ticket; nội dung thiếu không được ghi và xác nhận có server authorization/idempotency. PERSONAL_DATA chỉ được phép tool đang có; hồ sơ cá nhân chưa có tool, không được dùng model text để giả trạng thái. Guest không có tool declaration ở mọi nhánh.
