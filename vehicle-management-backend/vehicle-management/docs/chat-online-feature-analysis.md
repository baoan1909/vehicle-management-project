# Phân tích tính năng chat online cho Vehicle Management

## 1. Mục tiêu tài liệu

Tài liệu này phân tích tính năng chat online cho hệ thống `vehicle-management` từ góc nhìn BA và Senior Backend.

Nguồn tham chiếu đã đọc:

- `docs/clean-architecture-guide.md`
- `docs/backend-coding-standard.md`
- `docs/package-structure.md`
- `src/main/resources/db/vehicle_management.sql`
- các module gần nhất trong vehicle: `iam`, `people`, `operations.support_tickets`, `notification`, `audit`, `storage`, `parking`, `access_control`, `billing`
- dự án `C:\DiskD\job24\job24-backend`, đặc biệt phần WebSocket, RabbitMQ, chat database và notification realtime

Mục tiêu ban đầu của chủ dự án:

- chat giữa nhân viên nội bộ
- chat giữa nhân viên nội bộ với khách hàng
- trong chat có thể gửi kèm hình ảnh
- trong chat có thể gửi yêu cầu hỗ trợ

Kết luận ngắn:

- Vehicle nên có chat, nhưng không nên xem chat chỉ là "nhắn tin text".
- Chat trong hệ thống bãi xe nên là kênh giao tiếp có ngữ cảnh nghiệp vụ: khách hàng, phương tiện, thẻ, vé đăng ký, phiên gửi xe, sự kiện camera, hóa đơn, thanh toán, báo mất thẻ, ticket hỗ trợ, ca trực, làn, thiết bị.
- Phase đầu nên làm chắc nền tảng: conversation, member, message, attachment, read state, REST history, WebSocket realtime, RabbitMQ fanout, MinIO private attachment.
- Không tạo khái niệm "support request" song song với `operations.support_tickets`. Nếu người dùng gửi yêu cầu hỗ trợ trong chat thì backend nên tạo hoặc liên kết với `operations.support_tickets`.

## 2. Hiện trạng dự án vehicle

### 2.1 Kiến trúc hiện tại

Vehicle đang đi theo Clean Architecture và schema-first package structure:

- `entrypoint.controller.<schema>`
- `entrypoint.dto.<schema>.<table>.request`
- `entrypoint.dto.<schema>.<table>.response`
- `application.<schema>.<feature>`
- `domain.<schema>.<feature>`
- `infrastructure.persistence.database.entity.<schema>`
- `infrastructure.persistence.database.repository.<schema>`
- `infrastructure.persistence.adapter.<schema>`
- `shared.enumeration.<schema>`

Các rule quan trọng khi thêm chat:

- Controller mỏng, không chứa nghiệp vụ chat.
- Use case nằm application layer.
- Quy tắc member, gửi tin, xóa tin, đóng ticket, phân quyền đọc file phải nằm domain/application.
- Persistence adapter triển khai port, không để use case gọi trực tiếp JPA repository.
- Request DTO nên là `record`.
- Dùng MapStruct cho mapping.
- Dùng `Instant` cho thời gian.
- Dùng `UUID` cho identifier.
- Text phải validate trước khi lưu, ưu tiên `TextValidationUtils`.
- File/image phải đi qua hạ tầng storage, không upload trực tiếp từ controller xuống MinIO.

### 2.2 Những module vehicle có thể tái sử dụng

Vehicle đã có sẵn nhiều nền tảng nên không cần làm lại từ đầu:

| Phần hiện có | Ý nghĩa với chat |
|---|---|
| `iam.accounts` | Người đăng nhập, sender/participant chính của chat |
| `people.user_profiles` | Thông tin hiển thị: tên, số điện thoại, avatar |
| `people.customers` | Khách hàng tham gia chat và chủ thể của support flow |
| `people.employees` | Nhân viên nội bộ tham gia chat |
| `operations.support_tickets` | Nơi lưu yêu cầu hỗ trợ, không nên tạo bảng support request song song |
| `notification.notifications` | Có thể dùng để thông báo offline hoặc badge ngoài màn chat |
| `audit.audit_logs` | Ghi vết thao tác nhạy cảm: xóa tin, mở ticket, đóng ticket, gửi file nhạy cảm |
| `application.storage` | Có `FileStoragePort`, `FileAccessPort`, `StoreFileCommand`, `StoredFile` |
| `infrastructure.storage` | Có `MinioFileStorageAdapter`, xử lý ảnh, checksum, metadata |
| `StorageBucket`, `StorageFolder` | Có bucket public/private và folder nghiệp vụ |
| `parking.parking_sessions`, `parking.parking_events` | Ngữ cảnh quan trọng nhất cho tranh chấp, khiếu nại, ảnh camera |
| `access_control.cards`, `subscriptions`, `lost_card_reports` | Ngữ cảnh mất thẻ, thẻ lỗi, vé đăng ký |
| `billing.invoices`, `payments` | Ngữ cảnh khiếu nại phí và xác nhận thanh toán |

### 2.3 Hiện trạng WebSocket/RabbitMQ trong vehicle

Tại thời điểm đọc source:

- `pom.xml` chưa có `spring-boot-starter-websocket`.
- `pom.xml` chưa có `spring-boot-starter-amqp`.
- Chưa có config WebSocket/STOMP.
- Chưa có config RabbitMQ.
- Đã có MinIO/file storage.
- Đã có notification schema nhưng chưa thấy realtime WebSocket.

Vì vậy nếu triển khai chat online, cần bổ sung cả realtime transport và async messaging.

### 2.4 Rủi ro schema support ticket cần xử lý

Có điểm lệch giữa schema snapshot và migration:

- `vehicle_management.sql` đang mô tả `operations.support_tickets` có `priority`.
- Migration `V10__support_ticket_categories_and_workflow.sql` lại thêm `operations.support_ticket_categories`, thêm `category_id`, `resolution_note`, `closed_at`, `closed_by`, `reopen_count`, rồi drop `priority`.
- Entity `SupportTicketEntity` hiện có `categoryId` bắt buộc.

Trước khi nối chat với support ticket, nên đồng bộ lại:

- `vehicle_management.sql`
- entity/domain support ticket
- migration hiện hành
- seed dữ liệu category
- policy trạng thái ticket

Nếu không, chat tạo support ticket sẽ dễ lỗi ở môi trường mới hoặc lệch contract với database.

## 3. Bài học từ job24-backend

### 3.1 Những điểm nên học

Job24 đã có một cụm realtime tương đối đầy đủ:

- WebSocket endpoint `/ws`
- STOMP application prefix `/app`
- user destination prefix `/user`
- broker destination `/topic`, `/queue`
- JWT handshake interceptor
- channel interceptor
- RabbitMQ exchange cho notification
- RabbitMQ exchange cho chat realtime
- mỗi backend instance có anonymous queue riêng
- publish realtime sau khi transaction commit
- REST API vẫn là nguồn đọc history chính
- WebSocket chỉ dùng để đẩy event realtime
- chat database gồm conversation, conversation member, message, message attachment
- có `last_message_id`, `last_message_at` để query inbox nhanh
- có `last_read_message_id` theo member để tính unread
- attachment lưu object name trong DB, file nằm MinIO

Luồng job24 đáng học:

1. Use case lưu message vào database.
2. Cập nhật last message của conversation.
3. Cập nhật read marker của người gửi.
4. Sau commit mới publish RabbitMQ.
5. Mỗi instance nhận Rabbit event.
6. Instance nào đang giữ WebSocket session của user thì push payload cho user đó.
7. Nếu WebSocket miss realtime, frontend vẫn lấy lại bằng REST history.

### 3.2 Vì sao RabbitMQ cần thiết

Nếu chỉ dùng WebSocket local:

- User A gửi tin, request đi vào backend instance A.
- User B đang mở WebSocket ở backend instance B.
- Instance A push local thì user B không nhận được.

Job24 xử lý bằng RabbitMQ:

- mọi instance bind một anonymous queue vào cùng exchange/routing key
- một event chat được gửi đến tất cả instance
- mỗi instance tự push đến các session WebSocket local của nó

Vehicle nếu có khả năng chạy nhiều instance thì nên dùng mẫu này ngay từ đầu.

### 3.3 Điểm không nên copy nguyên từ job24

Một số điểm trong job24 không nên bê nguyên sang vehicle:

| Điểm trong job24 | Nhận xét cho vehicle |
|---|---|
| Conversation dùng `Long IDENTITY` | Vehicle chuẩn hóa `UUID`, nên chat cũng dùng `UUID` |
| Chat schema/tables không thấy migration tạo đầy đủ trong `db/migration` | Vehicle schema-first, phải có migration rõ ràng và cập nhật `vehicle_management.sql` |
| Subscribe `/topic/conversations/{id}` cần kiểm soát kỹ | Vehicle phải validate participant khi subscribe, hoặc ưu tiên user-specific queue |
| Handshake lấy token qua query string | Có thể dùng nếu frontend bị giới hạn, nhưng nên ưu tiên STOMP CONNECT header hoặc cơ chế không log token |
| Attachment response có thể trả presigned URL trực tiếp | Vehicle có PII như ảnh biển số/người, nên cân nhắc API resolve URL có kiểm tra quyền |
| Conversation type của job24 gắn domain tuyển dụng | Vehicle phải thay bằng domain bãi xe |

## 4. Đề xuất nghiệp vụ chat cho vehicle

### 4.1 Các nhóm hội thoại nên có

| Nhóm hội thoại | Mục đích | Người tham gia |
|---|---|---|
| Chat nội bộ trực tiếp | Nhân viên hỏi nhanh nhau | employee, parking manager, system admin tùy quyền |
| Chat nội bộ nhóm | Trao đổi theo ca, theo bãi, theo sự cố | nhiều nhân viên, quản lý |
| Chat khách hàng - nhân viên | Khách hỏi hỗ trợ, nhân viên xử lý | customer và employee/manager được phân công |
| Chat gắn support ticket | Mọi trao đổi xoay quanh một ticket | customer, assigned employee, manager |
| Chat gắn phiên gửi xe | Xử lý tranh chấp check-in/check-out, sai biển số, mất vé | nhân viên, quản lý, có thể khách hàng |
| Chat gắn hóa đơn/thanh toán | Khiếu nại phí, xác nhận thanh toán | customer, cashier/employee, manager |
| Chat gắn báo mất thẻ | Xử lý mất thẻ, phí, xác minh | customer, employee, manager |
| Chat hệ thống | Backend gửi thông báo dạng message | system account đến user hoặc nhóm |

### 4.2 Conversation type đề xuất

Nếu đặt trong schema `operations`, enum có thể là:

- `INTERNAL_DIRECT`: nhân viên với nhân viên
- `INTERNAL_GROUP`: nhóm nội bộ
- `CUSTOMER_DIRECT`: khách hàng với nhân viên
- `SUPPORT_TICKET`: hội thoại gắn ticket hỗ trợ
- `PARKING_SESSION`: hội thoại gắn phiên gửi xe
- `BILLING`: hội thoại gắn hóa đơn/thanh toán
- `LOST_CARD`: hội thoại gắn báo mất thẻ
- `SYSTEM_DIRECT`: hệ thống gửi riêng cho người dùng

Không nên dùng một type quá chung như `USER_DIRECT` cho tất cả vì nghiệp vụ customer - employee cần rule khác nội bộ.

### 4.3 Chat có thể gửi thêm gì ngoài ảnh và yêu cầu hỗ trợ

Trong vehicle, thứ có giá trị nhất không chỉ là file, mà là "ngữ cảnh nghiệp vụ có thể click mở". Đề xuất chia thành 3 nhóm.

#### Nhóm 1: Nội dung người dùng nhập

| Loại gửi | Mô tả | Phase |
|---|---|---|
| Text | Nội dung chat thường | Phase 1 |
| Image | Ảnh hiện trường, ảnh xe, ảnh biển số, ảnh biên lai | Phase 1 |
| File | PDF, ảnh chụp giấy tờ, biên bản, tài liệu đối soát | Phase 2 |
| Voice note | Ghi âm nhanh cho nhân viên vận hành mobile | Phase 3 |
| Reply message | Trả lời một message cụ thể | Phase 2 |
| Mention | `@nhanvien` trong chat nội bộ | Phase 2 |
| Reaction | Đã xem/đồng ý/ghi nhận nhanh | Phase 3 |

#### Nhóm 2: Thẻ ngữ cảnh nghiệp vụ

Các thẻ này không nên lưu như text thuần. Nên lưu bằng `related_schema`, `related_table`, `related_id` hoặc bảng context riêng.

| Thẻ ngữ cảnh | Ví dụ sử dụng |
|---|---|
| Khách hàng | Nhân viên gửi card khách hàng để đồng nghiệp xem hồ sơ |
| Phương tiện khách hàng | Gửi xe `51A-12345` đang bị lỗi thông tin |
| Thẻ gửi xe | Gửi card thẻ đang `LOST`, `BLOCKED`, `IN_USE` |
| Vé đăng ký/subscription | Trao đổi về vé tháng đang chờ duyệt hoặc sắp hết hạn |
| Phiên gửi xe | Gửi phiên đang `OPEN`, `LOST_CARD`, tranh chấp check-out |
| Sự kiện gửi xe | Gửi event vào/ra kèm ảnh biển số, ảnh người |
| Hóa đơn | Gửi invoice chưa thanh toán hoặc bị khiếu nại |
| Thanh toán | Gửi payment failed/success/refund |
| Báo mất thẻ | Gửi lost card report để xử lý phí và khóa thẻ |
| Yêu cầu phê duyệt | Gửi approval request cần manager xử lý |
| Ca trực | Gửi shift hiện tại, người phụ trách |
| Bãi xe/khu/làn | Gửi vị trí sự cố: lot, zone, lane |
| Thiết bị | Gửi camera, kiosk, barrier, reader đang lỗi |

Ví dụ message context:

```json
{
  "type": "CONTEXT_CARD",
  "content": "Nhờ kiểm tra phiên gửi xe này giúp em.",
  "relatedSchema": "parking",
  "relatedTable": "parking_sessions",
  "relatedId": "b2a4..."
}
```

#### Nhóm 3: Action card trong chat

Action card là message có nút hành động, nhưng backend vẫn phải gọi use case thật và kiểm tra permission.

| Action card | Ý nghĩa |
|---|---|
| Tạo ticket hỗ trợ | Từ đoạn chat tạo `operations.support_tickets` |
| Gán người xử lý | Assign ticket/conversation cho nhân viên |
| Chuyển trạng thái ticket | `OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED` |
| Yêu cầu khách gửi thêm ảnh | Tạo system message yêu cầu ảnh biển số/biên lai |
| Xác nhận đã nhận phản ánh | Message hệ thống, không thay đổi nghiệp vụ lớn |
| Liên kết phiên gửi xe | Gắn conversation với `parking.parking_sessions` |
| Liên kết hóa đơn | Gắn conversation với `billing.invoices` |
| Báo mất thẻ | Tạo hoặc liên kết `access_control.lost_card_reports` |
| Khóa thẻ tạm thời | Chỉ cho role có quyền `CARD_UPDATE_ALL`, không cho customer tự kích hoạt |
| Escalate manager | Thêm manager vào conversation và tăng priority ticket |

## 5. Đề xuất database

### 5.1 Nên đặt schema nào?

Có hai hướng:

#### Hướng A: Đặt chat trong `operations`

Ưu điểm:

- Không thêm schema mới vào bản đồ kiến trúc hiện tại.
- Phù hợp với mục tiêu ban đầu: vận hành, nhân viên, hỗ trợ khách hàng.
- Dễ liên kết với `operations.support_tickets`, `operations.shifts`, `operations.approval_requests`.

Nhược điểm:

- Nếu sau này chat thành nền tảng giao tiếp đa kênh lớn, schema `operations` có thể bị nặng.

#### Hướng B: Thêm schema mới `communication` hoặc `chat`

Ưu điểm:

- Tách rõ conversation/messaging khỏi operations.
- Phù hợp nếu roadmap có omni-channel, chatbot, email, Zalo, app push, SLA, routing queue.

Nhược điểm:

- Phải cập nhật `docs/package-structure.md`, schema map, permission module, migration.
- Là một core module mới, cần quyết định kiến trúc rõ ràng.

Khuyến nghị hiện tại:

- Phase 1 nên đặt dưới `operations` với prefix `chat_`: `operations.chat_conversations`, `operations.chat_messages`.
- Nếu chat phát triển thành nền tảng giao tiếp độc lập, tạo schema `communication` ở phase sau bằng quyết định schema change chính thức.

### 5.2 Bảng `operations.chat_conversations`

Mục đích: lưu một hội thoại.

Các cột đề xuất:

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `conversation_id` | UUID PK | `gen_random_uuid()` |
| `conversation_type` | VARCHAR(30) | check enum |
| `title` | VARCHAR(200) | tiêu đề nhóm hoặc auto-generated |
| `status` | VARCHAR(20) | `ACTIVE`, `ARCHIVED`, `CLOSED` |
| `customer_id` | UUID nullable | link customer nếu có |
| `support_ticket_id` | UUID nullable | link ticket nếu có |
| `owner_account_id` | UUID nullable | người tạo/chủ conversation |
| `assigned_to` | UUID nullable | nhân viên chính đang xử lý |
| `related_schema` | VARCHAR(50) nullable | ví dụ `parking` |
| `related_table` | VARCHAR(80) nullable | ví dụ `parking_sessions` |
| `related_id` | UUID nullable | id bản ghi nghiệp vụ |
| `last_message_id` | UUID nullable | cache inbox |
| `last_message_at` | TIMESTAMPTZ nullable | cache inbox |
| `metadata` | JSONB nullable | thông tin mở rộng có kiểm soát |
| `created_at`, `created_by`, `updated_at`, `updated_by` | audit | theo shared audit abstraction |

Check constraints:

- `conversation_type IN ('INTERNAL_DIRECT', 'INTERNAL_GROUP', 'CUSTOMER_DIRECT', 'SUPPORT_TICKET', 'PARKING_SESSION', 'BILLING', 'LOST_CARD', 'SYSTEM_DIRECT')`
- `status IN ('ACTIVE', 'ARCHIVED', 'CLOSED')`

Index đề xuất:

- `(last_message_at DESC, conversation_id DESC)`
- `(customer_id)`
- `(support_ticket_id)`
- `(related_schema, related_table, related_id)`
- `(assigned_to, status)`
- partial unique cho direct conversation nếu cần chống tạo trùng.

### 5.3 Bảng `operations.chat_conversation_members`

Mục đích: người tham gia hội thoại và read marker.

Các cột đề xuất:

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `conversation_member_id` | UUID PK | |
| `conversation_id` | UUID NOT NULL | FK conversation |
| `account_id` | UUID NOT NULL | FK `iam.accounts` |
| `member_role` | VARCHAR(30) | `OWNER`, `MEMBER`, `ASSIGNEE`, `OBSERVER`, `CUSTOMER` |
| `status` | VARCHAR(20) | `ACTIVE`, `LEFT`, `REMOVED`, `BLOCKED` |
| `last_read_message_id` | UUID nullable | unread calculation |
| `muted_until` | TIMESTAMPTZ nullable | optional |
| `joined_at` | TIMESTAMPTZ NOT NULL | |
| `left_at` | TIMESTAMPTZ nullable | |
| audit columns | | |

Constraint:

- unique `(conversation_id, account_id)`
- `member_role IN ('OWNER', 'MEMBER', 'ASSIGNEE', 'OBSERVER', 'CUSTOMER')`
- `status IN ('ACTIVE', 'LEFT', 'REMOVED', 'BLOCKED')`

Index:

- `(account_id, status)`
- `(conversation_id, status)`
- `(conversation_id, account_id, status)`

### 5.4 Bảng `operations.chat_messages`

Mục đích: lưu message.

Các cột đề xuất:

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `message_id` | UUID PK | |
| `conversation_id` | UUID NOT NULL | FK conversation |
| `sender_account_id` | UUID nullable | nullable cho system message |
| `message_type` | VARCHAR(30) | check enum |
| `content` | TEXT nullable | text/caption |
| `reply_to_message_id` | UUID nullable | phase 2 |
| `related_schema` | VARCHAR(50) nullable | nếu message là context card |
| `related_table` | VARCHAR(80) nullable | nếu message là context card |
| `related_id` | UUID nullable | nếu message là context card |
| `metadata` | JSONB nullable | payload action/context đã kiểm soát |
| `deleted` | BOOLEAN NOT NULL DEFAULT false | soft delete |
| `deleted_at` | TIMESTAMPTZ nullable | |
| `edited_at` | TIMESTAMPTZ nullable | phase 2 |
| `created_at`, `created_by`, `updated_at`, `updated_by` | audit | |

Message type đề xuất:

- `TEXT`
- `IMAGE`
- `FILE`
- `SYSTEM`
- `CONTEXT_CARD`
- `ACTION_CARD`
- `SUPPORT_REQUEST`

Index:

- `(conversation_id, created_at DESC, message_id DESC)`
- `(conversation_id, message_id DESC)`
- `(sender_account_id, created_at DESC)`
- `(related_schema, related_table, related_id)`

### 5.5 Bảng `operations.chat_message_attachments`

Mục đích: metadata file trong message.

Các cột đề xuất:

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `attachment_id` | UUID PK | |
| `message_id` | UUID NOT NULL | FK message |
| `bucket` | VARCHAR(20) NOT NULL | `PRIVATE` là mặc định |
| `object_key` | VARCHAR(255) NOT NULL | object key MinIO |
| `original_filename` | VARCHAR(255) | |
| `content_type` | VARCHAR(100) | |
| `size_bytes` | BIGINT | |
| `checksum_sha256` | VARCHAR(64) | |
| `attachment_type` | VARCHAR(30) | `IMAGE`, `DOCUMENT`, `AUDIO`, `PARKING_EVIDENCE`, `PAYMENT_PROOF` |
| `width` | INT nullable | ảnh |
| `height` | INT nullable | ảnh |
| audit columns | | |

Không nên lưu presigned URL vào DB vì URL có hạn sử dụng.

### 5.6 Bảng context riêng có cần không?

Phase 1 có thể để context ngay trên `chat_messages` bằng `related_schema`, `related_table`, `related_id`.

Nếu một message cần gắn nhiều đối tượng, thêm bảng:

`operations.chat_message_contexts`

- `message_context_id`
- `message_id`
- `context_type`
- `related_schema`
- `related_table`
- `related_id`
- `display_snapshot JSONB`

Ví dụ một message về mất thẻ có thể gắn đồng thời:

- `access_control.cards`
- `access_control.lost_card_reports`
- `billing.invoices`

## 6. Thiết kế attachment và MinIO

### 6.1 Tái sử dụng storage hiện có

Vehicle đã có:

- `FileStoragePort`
- `FileAccessPort`
- `StoreFileCommand`
- `StoredFile`
- `MinioFileStorageAdapter`
- `ImageFileProcessor`
- `StorageObjectKeyGenerator`
- `StorageBucket.PUBLIC`, `StorageBucket.PRIVATE`
- `StorageFolder.SUPPORT_TICKET`

Nên thêm:

```java
CHAT_ATTACHMENT("ca", "chat-attachment")
```

vào `StorageFolder`.

Chat attachment mặc định dùng:

- `StorageBucket.PRIVATE`
- `StorageFolder.CHAT_ATTACHMENT`
- `resourceType = "chat_message"`
- `resourceId = messageId` hoặc `conversationId` tùy flow
- metadata gồm `conversation_id`, `message_id`, `sender_account_id`, `attachment_type`

### 6.2 Không gửi file qua WebSocket

Không nên upload file qua WebSocket vì:

- STOMP message không phù hợp multipart lớn.
- Khó validate content-type và size.
- Dễ làm nghẽn broker/simple broker.
- Khó retry và cleanup.

Flow đề xuất:

1. Client gọi REST multipart `POST /api/operations/chat/conversations/{conversationId}/attachments`.
2. Backend validate quyền member.
3. Backend validate file.
4. Backend upload MinIO private.
5. Backend tạo message type `IMAGE` hoặc `FILE`.
6. Backend lưu attachment metadata.
7. Backend publish realtime event sau commit.
8. Client nhận realtime event, hiển thị message.

### 6.3 Quy tắc file phase đầu

Đề xuất Phase 1:

- chỉ cho ảnh: `jpg`, `jpeg`, `png`, `webp`
- tối đa 5 ảnh/message
- tối đa 5 MB/ảnh hoặc theo config MinIO hiện có
- ảnh sẽ được resize/compress qua `ImageFileProcessor`
- private bucket

Phase 2:

- thêm PDF
- thêm `doc`, `docx` nếu nghiệp vụ cần
- tăng giới hạn theo role admin/manager
- thêm virus scan nếu production public

### 6.4 Ảnh nào nhạy cảm?

Các loại ảnh sau phải xem là private/PII:

- ảnh biển số
- ảnh người/lái xe
- ảnh giấy tờ tùy thân
- ảnh biên lai thanh toán
- ảnh xe bị hư hỏng có biển số
- ảnh camera trong bãi

Không expose public URL. Nên cấp URL đọc ngắn hạn sau khi kiểm tra user là participant hoặc có permission phù hợp.

## 7. Realtime architecture đề xuất

### 7.1 Dependency cần thêm

Trong `pom.xml`:

- `spring-boot-starter-websocket`
- `spring-boot-starter-amqp`

Nếu cần test:

- test cho WebSocket config/interceptor
- test cho Rabbit adapter/consumer

### 7.2 WebSocket endpoint

Đề xuất:

- endpoint: `/ws`
- application prefix: `/app`
- user destination prefix: `/user`
- broker destination: `/topic`, `/queue`
- heartbeat: 10 giây hoặc theo config

Contract cơ bản:

| Mục đích | Destination |
|---|---|
| User nhận update inbox | `/user/topic/chat` |
| User nhận message trong conversation | `/user/queue/chat/conversations/{conversationId}` |
| Gửi text qua WebSocket | `/app/chat/conversations/{conversationId}/messages` |

Tôi khuyến nghị dùng user-specific queue cho conversation message thay vì public topic:

- an toàn hơn vì backend push riêng cho từng participant
- không cần để client subscribe public `/topic/conversations/{id}`
- tránh rủi ro user đoán UUID conversation rồi subscribe

Nếu vẫn muốn dùng `/topic/conversations/{conversationId}`, bắt buộc `ChannelInterceptor` phải kiểm tra user là active member trước khi cho subscribe.

### 7.3 Authentication WebSocket

Vehicle đang dùng OAuth2 resource server/Keycloak và `CurrentAccountSecurityAdapter`.

Đề xuất:

- JWT handshake/channel interceptor phải resolve về `account_id`, `username`, `email`.
- Principal name nên ổn định. Tốt nhất dùng `accountId.toString()` hoặc username nội bộ đã unique.
- Nếu dùng email, phải chắc email không đổi hoặc có cơ chế đồng bộ.
- Không chỉ tin role trong token cho nghiệp vụ chat. Use case vẫn phải kiểm tra DB/member/permission.

Về truyền token:

- Nếu frontend mobile/web hỗ trợ STOMP CONNECT header, ưu tiên `Authorization: Bearer <token>`.
- Nếu browser WebSocket không set header được, có thể dùng query param `?token=...`, nhưng phải tránh log full URL.

### 7.4 RabbitMQ contract

Exchange đề xuất:

- `chat.realtime.exchange`

Routing key:

- `chat.realtime`

Queue:

- anonymous queue per backend instance

Event payload:

```json
{
  "conversationId": "uuid",
  "messageId": "uuid",
  "recipientAccountIds": ["uuid1", "uuid2"],
  "message": {
    "id": "uuid",
    "type": "TEXT",
    "content": "..."
  }
}
```

Có hai cách xác định recipient:

| Cách | Ưu điểm | Nhược điểm |
|---|---|---|
| Event chứa `recipientAccountIds` | Consumer không cần query member nhiều | Event payload lớn hơn, phải tính đúng lúc publish |
| Consumer query member theo `conversationId` | Event gọn, giống job24 | Mỗi instance query DB khi event đến |

Khuyến nghị phase 1:

- event chứa `conversationId` và message response
- consumer query active members theo `conversationId`
- sau này tối ưu bằng recipient snapshot nếu tải lớn

### 7.5 Publish sau commit

Phải publish RabbitMQ sau khi transaction lưu message commit thành công.

Nếu publish trước commit:

- client nhận message nhưng REST history chưa đọc được
- nếu transaction rollback thì client đã thấy message không tồn tại

Cần tạo helper tương tự job24 `TransactionalEvents.runAfterCommit(...)`, hoặc dùng Spring `TransactionSynchronizationManager`.

### 7.6 Offline và replay

Chat không cần cờ `realtimeDelivered` đơn giản như notification vì một message có nhiều người nhận.

Nên dùng:

- `chat_conversation_members.last_read_message_id`
- REST inbox query tính unread
- REST messages query lấy history
- WebSocket chỉ giúp realtime

Khi user reconnect:

1. Subscribe `/user/topic/chat`.
2. Backend có thể replay các conversation unread gần nhất.
3. Frontend vẫn gọi REST inbox để đồng bộ chắc chắn.

## 8. Luồng nghiệp vụ chính

### 8.1 Gửi text message

1. Client gọi REST hoặc STOMP send.
2. Backend resolve current account.
3. Use case kiểm tra account active.
4. Use case kiểm tra user là active member của conversation.
5. Validate `messageType`, `content`.
6. Lưu message.
7. Cập nhật `last_message_id`, `last_message_at`.
8. Mark read cho sender.
9. Publish RabbitMQ sau commit.
10. Mỗi instance push WebSocket đến active members.

### 8.2 Gửi ảnh

1. Client gọi REST multipart.
2. Backend kiểm tra participant.
3. Validate số lượng file, size, extension, content type.
4. Upload MinIO private.
5. Lưu message type `IMAGE`.
6. Lưu attachment metadata.
7. Publish realtime sau commit.
8. Nếu DB commit fail sau khi upload, cleanup object đã upload.

### 8.3 Tạo yêu cầu hỗ trợ từ chat

Không tạo bảng support request mới.

Flow đề xuất:

1. Customer hoặc employee gửi action `SUPPORT_REQUEST`.
2. Use case validate content/category.
3. Tạo `operations.support_tickets`.
4. Tạo hoặc liên kết conversation type `SUPPORT_TICKET`.
5. Add customer và assigned employee/queue member vào conversation.
6. Gửi system message: "Ticket #... đã được tạo".
7. Gửi notification cho nhân viên phụ trách nếu offline.

### 8.4 Liên kết chat với phiên gửi xe

Ví dụ:

- khách khiếu nại bị tính sai phí
- nhân viên cần xem ảnh check-in/check-out
- quản lý cần xác minh biển số

Flow:

1. Conversation có `related_schema = 'parking'`, `related_table = 'parking_sessions'`, `related_id = parkingSessionId`.
2. Message có thể gửi context card của `parking_events`.
3. Ảnh camera không copy sang chat nếu đã nằm trong `parking_events`; message chỉ link context.
4. Nếu khách gửi ảnh mới, ảnh đó là chat attachment riêng.

### 8.5 Nội bộ bàn giao ca

Chat nội bộ nên hỗ trợ:

- conversation theo shift
- message hệ thống khi ca mở/đóng
- mention nhân viên
- gắn lane/device đang lỗi
- gắn support ticket cần bàn giao

Không nên để chat thay thế hoàn toàn `operations.shifts`; chat chỉ là kênh trao đổi và log phụ trợ.

## 9. API REST đề xuất

Đặt dưới `entrypoint.controller.operations` nếu chọn schema `operations`.

### 9.1 Conversation

| Method | Endpoint | Mục đích |
|---|---|---|
| GET | `/api/operations/chat/conversations` | Inbox, filter theo type/status/keyword |
| GET | `/api/operations/chat/conversations/unread-count` | Đếm hội thoại chưa đọc |
| GET | `/api/operations/chat/conversations/{conversationId}` | Chi tiết conversation |
| POST | `/api/operations/chat/conversations/internal/direct` | Tạo/lấy chat nhân viên trực tiếp |
| POST | `/api/operations/chat/conversations/internal/groups` | Tạo group nội bộ |
| POST | `/api/operations/chat/conversations/customer-support` | Tạo/lấy chat hỗ trợ khách hàng |
| POST | `/api/operations/chat/conversations/{conversationId}/members` | Thêm member |
| DELETE | `/api/operations/chat/conversations/{conversationId}/members/{accountId}` | Remove member |

### 9.2 Message

| Method | Endpoint | Mục đích |
|---|---|---|
| GET | `/api/operations/chat/conversations/{conversationId}/messages` | Lấy history theo cursor |
| POST | `/api/operations/chat/conversations/{conversationId}/messages` | Gửi text/context/action |
| POST | `/api/operations/chat/conversations/{conversationId}/attachments` | Gửi ảnh/file multipart |
| DELETE | `/api/operations/chat/messages/{messageId}` | Soft delete message của mình hoặc admin |
| POST | `/api/operations/chat/conversations/{conversationId}/read` | Mark read |
| GET | `/api/operations/chat/attachments/{attachmentId}/read-url` | Lấy URL đọc file có kiểm tra quyền |

### 9.3 Support ticket integration

| Method | Endpoint | Mục đích |
|---|---|---|
| POST | `/api/operations/support-tickets/{supportTicketId}/conversation` | Tạo/lấy conversation cho ticket |
| POST | `/api/operations/chat/conversations/{conversationId}/support-ticket` | Tạo ticket từ conversation |

## 10. DTO và response

### 10.1 Request DTO

Request nên là `record`:

- `CreateInternalDirectConversationRequest`
- `CreateInternalGroupConversationRequest`
- `CreateCustomerSupportConversationRequest`
- `SendChatMessageRequest`
- `SendChatContextMessageRequest`
- `MarkConversationReadRequest`
- `CreateSupportTicketFromChatRequest`
- `ChatConversationFilterRequest`

### 10.2 Response DTO

Tách response:

- `ChatConversationUserResponse`: cho user/customer/employee xem thông tin cần thiết.
- `ChatConversationAdminResponse`: thêm audit, internal metadata nếu cần.
- `ChatMessageUserResponse`: message hiển thị trong chat.
- `ChatAttachmentUserResponse`: không expose internal object metadata quá mức.
- `ChatInboxItemUserResponse`: item trong inbox.
- `ChatUnreadCountResponse`: count.

Không expose:

- `createdBy`, `updatedBy` cho customer nếu không cần.
- storage object key nếu frontend không cần.
- internal workflow status nhạy cảm.
- dữ liệu security metadata.

## 11. Domain policy cần có

### 11.1 `ChatConversationPolicy`

Nên xử lý:

- không tự chat với chính mình trong direct chat
- internal direct chỉ giữa account nhân viên active
- customer support chat phải có customer active/approved nếu rule yêu cầu
- group nội bộ phải có ít nhất 2 member
- chỉ owner/manager được thêm hoặc xóa member
- conversation `CLOSED` không nhận message user thường
- ticket đã `CLOSED` không tự động mở lại nếu không có action reopen rõ ràng

### 11.2 `ChatMessagePolicy`

Nên xử lý:

- `TEXT` phải có content.
- `IMAGE/FILE` phải có attachment.
- `SYSTEM` chỉ backend hoặc role có quyền gửi.
- `ACTION_CARD` phải có metadata hợp lệ.
- content max length, ví dụ 4000 hoặc 8000 ký tự.
- dùng `TextValidationUtils.normalizeRequiredText`.
- reject ISO control characters, raw `<`, `>` nếu không hỗ trợ rich text.
- sender chỉ xóa message của mình, manager/admin có quyền moderation riêng.

### 11.3 `ChatAttachmentPolicy`

Nên xử lý:

- file count/message
- file size
- content type
- extension
- private bucket
- không cho upload file rỗng
- phân biệt ảnh thường, ảnh bằng chứng, biên lai, tài liệu

## 12. Phân quyền đề xuất

Thêm permission module `CHAT` hoặc `CHAT_CONVERSATION` và `CHAT_MESSAGE`.

Nếu theo convention hiện tại:

- `CHAT_CONVERSATION_READ_OWN`
- `CHAT_CONVERSATION_CREATE_OWN`
- `CHAT_CONVERSATION_CREATE_ALL`
- `CHAT_CONVERSATION_UPDATE_ALL`
- `CHAT_MESSAGE_SEND_OWN`
- `CHAT_MESSAGE_DELETE_OWN`
- `CHAT_MESSAGE_MODERATE_ALL`
- `CHAT_ATTACHMENT_READ_OWN`
- `CHAT_ATTACHMENT_CREATE_OWN`
- `CHAT_INTERNAL_CREATE_ALL`
- `CHAT_SUPPORT_ASSIGN_ALL`

Gợi ý role:

| Role | Quyền |
|---|---|
| `CUSTOMER` | đọc/gửi trong conversation mình là member, tạo support chat/ticket của mình |
| `EMPLOYEE` | đọc/gửi conversation được assign hoặc là member, tạo chat nội bộ theo scope |
| `PARKING_MANAGER` | xem/assign conversation hỗ trợ, thêm nhân viên, xử lý escalation |
| `SYSTEM_ADMIN` | cấu hình, audit, moderation khi có rule rõ |

Không nên chỉ dựa vào permission `_ALL` để đọc mọi chat khách hàng. Với dữ liệu nhạy cảm, nên có access guard theo participant, assigned employee, lot/shift scope hoặc manager role.

## 13. Bảo mật và riêng tư

### 13.1 Subscribe WebSocket phải kiểm tra quyền

Rủi ro lớn nhất của chat realtime là user subscribe nhầm hoặc cố tình subscribe conversation không thuộc mình.

Rule:

- CONNECT: token hợp lệ, account active.
- SUBSCRIBE `/user/topic/chat`: cho account hợp lệ.
- SUBSCRIBE conversation-specific queue/topic: kiểm tra active member hoặc permission quản lý.
- SEND message: vẫn kiểm tra lại trong use case.

### 13.2 Không tin dữ liệu client gửi

Client không được tự gửi:

- `senderAccountId`
- `createdAt`
- `assignedTo` nếu không có quyền
- `objectKey` file tự bịa
- `related_schema/table/id` không được validate

Backend resolve current account và validate mọi target.

### 13.3 Audit

Nên audit:

- tạo conversation hỗ trợ
- tạo support ticket từ chat
- assign/handoff ticket
- thêm/xóa member
- xóa message
- tải file nhạy cảm nếu cần truy vết
- gửi action ảnh hưởng trạng thái thẻ, vé, hóa đơn, ticket

### 13.4 PII

Chat có thể chứa:

- biển số xe
- ảnh người
- số điện thoại
- giấy tờ
- lịch sử gửi xe
- thanh toán

Cần:

- private attachment
- presigned URL ngắn hạn
- kiểm tra participant trước khi cấp URL
- tránh log full message content/file URL trong application log
- cân nhắc retention policy sau này

## 14. Package structure đề xuất

Nếu chọn schema `operations`, package nên là:

```text
entrypoint/controller/operations/ChatConversationController.java
entrypoint/controller/operations/ChatMessageController.java
entrypoint/dto/operations/chatconversation/request
entrypoint/dto/operations/chatconversation/response
entrypoint/dto/operations/chatmessage/request
entrypoint/dto/operations/chatmessage/response

application/operations/chatconversation/mapper
application/operations/chatconversation/port/in
application/operations/chatconversation/port/out
application/operations/chatconversation/usecase
application/operations/chatconversation/authorization

application/operations/chatmessage/mapper
application/operations/chatmessage/port/in
application/operations/chatmessage/port/out
application/operations/chatmessage/usecase
application/operations/chatmessage/authorization

domain/operations/chatconversation/model
domain/operations/chatconversation/policy
domain/operations/chatmessage/model
domain/operations/chatmessage/policy

infrastructure/persistence/database/entity/operations
infrastructure/persistence/database/repository/operations
infrastructure/persistence/database/specification/operations
infrastructure/persistence/adapter/operations
infrastructure/websocket/chat
infrastructure/messaging/rabbitmq

shared/enumeration/operations
```

Enum đặt dưới `shared.enumeration.operations`:

- `ChatConversationType`
- `ChatConversationStatus`
- `ChatConversationMemberRole`
- `ChatConversationMemberStatus`
- `ChatMessageType`
- `ChatAttachmentType`

## 15. Tích hợp với notification

Chat realtime và notification không giống nhau:

- Chat message nằm trong conversation và có read state theo member.
- Notification là thông báo độc lập gửi đến account.

Nên dùng notification cho:

- user offline có tin nhắn hỗ trợ mới
- conversation được assign cho nhân viên
- ticket bị escalate
- customer được phản hồi ticket

Không nên tạo một notification cho mọi message nội bộ nếu sẽ gây spam. Có thể batch hoặc chỉ thông báo khi user offline/muted state.

## 16. Tích hợp với support ticket

Nguyên tắc:

- `operations.support_tickets` vẫn là nguồn sự thật của yêu cầu hỗ trợ.
- Chat chỉ là kênh trao đổi.
- Một ticket có thể có một conversation chính.
- Một conversation hỗ trợ có thể tạo ticket nếu chưa có.

State flow đề xuất:

```text
Khách gửi support request trong chat
-> CreateSupportTicketFromChatUseCase
-> operations.support_tickets status OPEN
-> operations.chat_conversations type SUPPORT_TICKET
-> add customer + assigned employee/queue
-> system message "Ticket đã được tạo"
-> notification cho người xử lý
```

Khi ticket đổi trạng thái:

- `OPEN -> IN_PROGRESS`: system message trong chat.
- `IN_PROGRESS -> RESOLVED`: system message + resolution note.
- `RESOLVED -> CLOSED`: system message.
- reopen: system message + tăng `reopen_count` nếu schema support ticket đã đồng bộ.

## 17. Lộ trình triển khai khuyến nghị

### Phase 0: Chuẩn hóa nền

- Đồng bộ `vehicle_management.sql` với migration support ticket mới nhất.
- Quyết định schema đặt chat: khuyến nghị `operations` phase đầu.
- Thêm permission module/action/scope cho chat.
- Viết migration chat tables.
- Cập nhật docs package/schema nếu cần.

### Phase 1: Chat nền tảng

Scope:

- conversation
- member
- text message
- image attachment
- inbox
- message history
- unread count
- mark read
- WebSocket push qua RabbitMQ
- private MinIO attachment

Không làm phase 1:

- voice note
- reaction
- edit message
- bot
- SLA routing phức tạp
- full-text search nâng cao

### Phase 2: Support workflow

Scope:

- tạo support ticket từ chat
- conversation gắn support ticket
- assign/handoff
- system messages khi ticket đổi trạng thái
- notification offline
- context card cho parking session, invoice, lost card report

### Phase 3: Vận hành nâng cao

Scope:

- group theo shift/parking lot/lane
- mention
- action cards
- escalation policy
- SLA
- dashboard support inbox
- search message
- retention policy
- audit/report

### Phase 4: Omni-channel nếu cần

Scope:

- Zalo/email/app push integration
- chatbot/auto reply
- customer support routing queue
- chuyển schema `operations.chat_*` sang `communication` nếu đã đủ lớn

## 18. Test cần có

### Domain unit test

- validate direct chat không được tự chat chính mình
- validate inactive member không gửi message
- validate conversation closed không nhận message thường
- validate text content required/max length
- validate attachment count/type/size
- validate only sender can delete own message

### Application use case test

- send text message updates last message
- send image uploads storage and saves attachment
- DB fail after upload triggers cleanup
- sender is marked read after send
- non-member cannot read/send
- create support ticket from chat
- assign conversation to employee

### Infrastructure test

- persistence mapper
- repository query inbox/unread
- RabbitMQ adapter publishes after commit
- RabbitMQ consumer pushes to expected user destination
- WebSocket channel interceptor rejects unauthorized subscribe
- attachment read URL requires participant

### Controller test

- REST send message returns response DTO
- REST attachment validates multipart
- mark read endpoint
- support ticket from chat endpoint

## 19. Rủi ro và quyết định cần chốt

| Rủi ro/quyết định | Khuyến nghị |
|---|---|
| Chat đặt schema nào | Phase 1 đặt `operations.chat_*`, phase sau mới tách nếu cần |
| Public topic bị subscribe trái phép | Ưu tiên `/user/queue/chat/conversations/{id}` hoặc validate subscribe thật chặt |
| File chứa PII | Private bucket, URL ngắn hạn, kiểm tra quyền trước khi đọc |
| Support ticket schema đang lệch | Đồng bộ trước khi làm chat-ticket |
| RabbitMQ down | Message vẫn lưu DB; realtime miss nhưng REST history recover. Phase sau cân nhắc outbox retry |
| Message delivery per user | Phase 1 dùng last read marker, chưa cần delivery ledger |
| Notification spam | Chỉ notify khi offline, assigned, mention hoặc support escalation |
| Token qua query string | Chỉ dùng nếu frontend bắt buộc; tránh log URL |
| Dữ liệu ngữ cảnh bị sửa sau khi gửi | Với context card quan trọng, lưu `display_snapshot JSONB` tối thiểu |

## 20. Kết luận đề xuất

Tôi đề xuất xây chat cho vehicle theo hướng "operational conversation", không chỉ là chat text.

Phase đầu nên làm nhỏ nhưng chắc:

- `operations.chat_conversations`
- `operations.chat_conversation_members`
- `operations.chat_messages`
- `operations.chat_message_attachments`
- REST API cho inbox/history/send/read/upload

### Permission-first rule for Support Widget

- Do not check role codes in business or UI logic. Conditions such as `role == CUSTOMER`, `EMPLOYEE`, `PARKING_MANAGER`, or `SYSTEM_ADMIN` are prohibited for this feature.
- For an authenticated account, display/access is controlled only by `SUPPORT_WIDGET_ACCESS_OWN`. Default role assignment is configuration in `iam.role_permissions`, not a code condition.
- The default customer role may receive this permission; staff roles do not. A future role can use the widget by receiving the same permission without code changes.
- An unauthenticated visitor is a public session, not a customer role. Public access must use a dedicated visitor-session endpoint/configuration and must never grant or simulate `CHAT_*_OWN` permissions.
- A public widget that sends messages requires visitor-session tokens, rate limiting, CAPTCHA/anti-spam, and audit. It cannot call an API that requires `CurrentAccountAccess` directly.
- Widget access does not replace chat/ticket authorization. Reading, sending, attachments, and ticket operations continue to require their existing `CHAT_*` and `SUPPORT_TICKET_*` permissions.
- WebSocket/STOMP cho realtime
- RabbitMQ fanout cho multi-instance
- MinIO private cho ảnh
- participant-based authorization

Những thứ chat nên gửi thêm trong vehicle:

- ảnh hiện trường, ảnh xe, ảnh biển số, ảnh biên lai
- context card khách hàng, xe, thẻ, vé đăng ký
- context card phiên gửi xe, sự kiện camera
- context card hóa đơn, thanh toán
- context card báo mất thẻ
- support ticket/action card
- assignment/handoff/escalation message
- system message khi nghiệp vụ đổi trạng thái
- nội bộ theo ca, theo bãi, theo làn, theo thiết bị

Điểm quan trọng nhất: mọi message có thể realtime, nhưng nguồn sự thật vẫn là database. WebSocket chỉ là kênh đẩy nhanh, REST history và read state mới là nền ổn định để hệ thống không mất dữ liệu khi user offline, refresh trang hoặc backend chạy nhiều instance.
