# Audit hiện trạng trợ lý AI và nghiệp vụ Vehicle Management

> Cập nhật đợt 1 — 19/11/2026: xem [bằng chứng/truy vết](evaluation/batch-01-traceability.md), [manifest](knowledge-manifest.json) và [báo cáo đợt 1](evaluation/batch-01-report.md). Baseline 14/09/2026 được giữ để truy vết; các nhận định runtime có delta dưới đây thay thế nhận định cũ tương ứng. Chưa tự phê duyệt/publish knowledge.


| Thuộc tính | Giá trị |
|---|---|
| Phiên bản | 1.0.0 |
| Trạng thái | VERIFIED BASELINE — business decision register APPROVED ngày 2026-09-14 |
| Ngày kiểm chứng | 2026-09-14 |
| Phương pháp | Đọc controller/DTO/use case/policy/adapter/entity/Flyway và route/page/API/permission guard FE |

## Phạm vi rà soát

- 23 controller liên quan trực tiếp, tổng cộng 135 method endpoint được lập inventory.
- 18 màn hình/route FE trọng yếu và các API client liên quan.
- 45 bảng nghiệp vụ nền trong baseline, 2 bảng liên kết identity/hỗ trợ và 13 bảng AI/RAG/runtime (60 bảng được tạo qua toàn bộ Flyway).
- 12 miền nghiệp vụ trong yêu cầu Phase 0.

Con số trên là inventory mã nguồn, không phải tuyên bố mọi endpoint đã được UAT thực tế.

## Kiến trúc hiện tại

Backend tuân theo hướng controller → application use case/guard → domain policy → port/adapter → PostgreSQL. IAM dùng `iam.roles`, `iam.permissions`, `iam.role_permissions`; ownership được kiểm tra trong các access guard thay vì tin dữ liệu FE.

Frontend có `routes.tsx` và `routePermissions.ts`; route quản trị và khách hàng được điều khiển bằng permission code. Backend vẫn là nơi quyết định cuối cùng.

## Hiện trạng chatbot/RAG

| Hạng mục | Hiện trạng | Bằng chứng | Đánh giá |
|---|---|---|---|
| Hội thoại trợ lý | Có `ASSISTANT_SUPPORT`, assistant job, retry/status và message idempotency | migrations `V20260901120000`, `V20260907100000`, `V20260907101000`; `AssistantOrchestrator` | CONFIRMED |
| Model động | Có configuration, catalog, rollout, warning và admin FE | `AiModelAdminController`, `AiModelManagementPage.tsx` | CONFIRMED |
| Tool runtime | Registry có 5 tool; write tool tạo phiếu yêu cầu xác nhận | `AiToolRegistry`, `AiToolExecutionService`, `AiToolCallController` | CONFIRMED |
| Worker tool | Worker chỉ được chạy `search_support_knowledge`; tool cá nhân bị chặn ở worker vì thiếu request-time authorization | `executeFromAssistantWorker` | PARTIAL |
| RAG schema | Có source, document, chunk, embedding, ingestion job, retrieval audit và citation | migration `V20260908100000` | CONFIRMED |
| Retrieval | SQL chỉ dùng `tsvector`/`plainto_tsquery`; chưa dùng vector dù method tên `hybridSearch` | `KnowledgeChunkRepository` | CONFLICT |
| Embedding | Runtime đã dùng vector(768), index version và hybrid vector/lexical; xem migrations 20260917–20260919 | migration `V20260908100000` | PARTIAL |
| Ingestion admin | Chưa có controller/use case/page cho upload, chunk, embed, publish, reindex, rollback | inventory controller/FE route | TBD/NOT IMPLEMENTED |
| Citation | Có bảng persist citation, nhưng response runtime chưa có contract đầy đủ và retrieval chưa chứng minh persist end-to-end | migration và `AssistantOrchestrator` | PARTIAL |
| Response DTO | Prompt chỉ yêu cầu `{responseText}`; chưa có DTO thống nhất gồm citations/actionCards/confidence/handoff | `AssistantOrchestrator` | PARTIAL |
| PII | Có `PiiRedactionService`, payload tool lưu dạng redacted | application AI và schema `ai_tool_calls` | PARTIAL — cần test coverage mở rộng |

## Hiện trạng theo miền nghiệp vụ

### 1. Xác thực và hồ sơ

- Đăng ký local tạo user trong Keycloak, ghi account/profile, gửi email xác thực; nếu DB thất bại thì xóa user Keycloak theo best effort.
- Quên mật khẩu gọi Keycloak gửi email cập nhật mật khẩu; FE không có OTP nội bộ, các route OTP/recover chuyển về `/forgot-password`.
- Google OAuth dùng authorization code + PKCE ở FE, sau đó gọi social bootstrap; chỉ Google được self-register, email phải được Google xác minh.
- Tài khoản local cùng email không được tự động liên kết Google.
- Account có trạng thái `ACTIVE`, `LOCKED`, `DISABLED`, `PENDING`; hồ sơ khách hàng còn có operational status và approval status riêng.

Đánh giá: `CONFIRMED`, trừ SLA email và hướng dẫn xử lý lỗi Google bên ngoài hệ thống là `TBD`.

### 2. Phương tiện khách hàng

- CRUD, activate, inactivate, block, mark/unmark default đã có.
- OWN yêu cầu customer ACTIVE và APPROVED; access guard so khớp `customerId`.
- Xe default bắt buộc ACTIVE; xe BLOCKED không được activate/inactivate và chỉ ALL permission được block.

Đánh giá: `CONFIRMED`.

### 3. Subscription

- Tạo mới → `PENDING`; ngày hiệu lực yêu cầu trước 2–7 ngày.
- Duyệt trước requested date, reserve card và tạo invoice; số tiền 0 → `PENDING_CARD`, lớn hơn 0 → `PENDING_PAYMENT`.
- Thanh toán hoàn tất → `PENDING_CARD`; nhận thẻ → `ACTIVE`; sau ngày hết hạn → `EXPIRED`.
- Chỉ `PENDING`/`PENDING_PAYMENT` được cancel theo flow hiện tại; reject chỉ từ `PENDING`.
- Kiểm tra customer/vehicle/ticket type active, customer approved, overlap và sức chứa.

Đánh giá: `CONFIRMED`; business policy đã phê duyệt không auto-renew, không grace period, không chuyển xe/thẻ in-place và xem “gia hạn” là đăng ký mới. Runtime/copy FE chưa đồng bộ đầy đủ.

### 4. Giá và tính phí

- Public pricing đọc price plan/rule, vehicle type, ticket type đang áp dụng.
- Price rule có unit `TURN`, `DAY`, `MONTH`, base price, lost-card fee, time window và priority.
- Visitor checkout có policy hiện tại: ngưỡng lưu trú ngắn 4 giờ, ngày đầy đủ 24 giờ và rule ngày/đêm; con số giá phải lấy runtime từ DB.
- Holiday calendar tồn tại nhưng chưa có bằng chứng policy checkout áp dụng ngày lễ end-to-end.

Đánh giá: `PARTIAL`; không được suy diễn công thức ngoài policy.

### 5. Hóa đơn/VNPAY/hoàn tiền

- Invoice tách khỏi Payment; invoice có `UNPAID`, `PAID`, `CANCELLED`, `REFUNDED`.
- VNPAY tạo payment `PENDING`, xử lý IPN idempotent theo trạng thái và verify return.
- Payment success đánh dấu invoice paid; failure giữ invoice chưa thanh toán và phát notification.
- Enum/schema/FE có `REFUNDED`, nhưng không tìm thấy endpoint/use case thực hiện refund.

Đánh giá: payment `CONFIRMED`; refund `CONFLICT/PARTIAL` và phải handoff.

### 6. Xe vào/ra

- Check-in kiểm tra permission, làn/cổng/zone/bãi ACTIVE, hướng làn IN, trạng thái thẻ, session mở, sức chứa, loại xe và subscription context.
- Check-out kiểm tra hướng OUT, topology, thẻ `IN_USE`, biển số khớp.
- Khách vãng lai phải prepare invoice và thanh toán trước khi hoàn tất; subscription không cần prepare payment.
- OCR chỉ dành cho người có permission check-in/check-out; ảnh rỗng bị từ chối.

Đánh giá: `CONFIRMED`; xử lý OCR confidence thấp cần operator/manual review, không cho chatbot tự quyết.

### 7. Thẻ và mất thẻ

- Card có `AVAILABLE`, `RESERVED`, `ASSIGNED`, `IN_USE`, `LOST`, `BLOCKED`, `RETIRED` với policy chuyển trạng thái.
- Lost-card report có `OPEN`, `RESOLVED`, `CANCELLED`; yêu cầu thông tin xác minh và có thể liên quan invoice/phí.
- Các API lost-card hiện chỉ permission `*_ALL`, chưa có self-service OWN.

Đánh giá: card `CONFIRMED`; chatbot chỉ hướng dẫn/tạo phiếu, không nhận CCCD đầy đủ và không tự báo mất.

### 8. Lịch sử gửi xe

- Endpoint `/parking/parking-sessions/me` và route `/customer/parking-history` dùng `PARKING_SESSION_READ_OWN` hoặc ALL.
- Session có `OPEN`, `CLOSED`, `LOST_CARD`, `CANCELLED`; chứa thời gian vào/ra, biển số, card, customer vehicle và tổng phí.

Đánh giá: `CONFIRMED` cho đọc OWN; business tool chatbot chưa tồn tại nên `NOT IMPLEMENTED` ở assistant.

### 9. Phiếu hỗ trợ và escalation

- Ticket: `OPEN` → tự chuyển `IN_PROGRESS` khi assignee trả lời lần đầu → `RESOLVED` → `CLOSED`; `RESOLVED` có thể reopen.
- OPEN chưa gán có thể claim; assign/reassign cần `SUPPORT_TICKET_ASSIGN`.
- Chỉ assignee hiện tại có đủ permission assigned/process/respond mới mở customer conversation để trả lời.
- Customer có thể yêu cầu quản lý xem xét khi ticket OPEN/IN_PROGRESS và đã được gán; giới hạn một pending, tối đa theo rate-limit; reviewer phải độc lập.
- Reviewer có `ESCALATION_REVIEW_ALL`, `ASSIGN`, `READ_ALL` có thể giữ nguyên hoặc reassign.

Đánh giá: `CONFIRMED`; endpoint `start-progress` vẫn tồn tại nhưng use case luôn trả conflict vì chuyển trạng thái tự động — cần ghi rõ tránh chatbot hướng dẫn gọi endpoint này.

### 10. Privacy

- Token/API key/password/OTP/card data không thuộc nguồn RAG.
- Có redaction cơ bản nhưng chưa có bằng chứng DLP toàn diện trước mọi provider call, attachment và citation.

Đánh giá: `PARTIAL`; áp dụng deny-by-default trong contract Phase 0.

### 11. Lỗi thường gặp

- Có message lỗi Việt/Anh lẫn lộn giữa policy/use case.
- FE có mapping lỗi API, assistant có trạng thái retry/failed.
- `Failed to fetch` là lỗi transport và không đủ để kết luận nguyên nhân server.

Đánh giá: `PARTIAL`; tài liệu chỉ hướng dẫn bước an toàn và handoff.

### 12. Địa điểm, giờ, SLA, liên hệ

- Contact chính thức đã được Business Owner xác nhận: `0912345678`, `support@coparking.vn`, `10/5 Đường Tỉnh Lộ 19, Phường An Phú Đông, Thành phố Hồ Chí Minh`, tiếp nhận 24/7. FE vẫn còn dữ liệu tĩnh mâu thuẫn và chưa có nguồn config/backend.
- Contact form chỉ là UI, nút chưa có API submit rõ ràng.

Đánh giá: business policy `APPROVED`, implementation `CONFLICT/PENDING`; chỉ publish sau khi FE/config/contact API được đồng bộ và document qua knowledge governance.

## Xung đột và khoảng trống chính

1. `KnowledgeChunkRepository.hybridSearch` chỉ lexical, chưa hybrid/vector.
2. Schema cho phép `REFUNDED` nhưng runtime không có refund action.
3. FE public contact chưa khớp contact chính thức đã phê duyệt và thiếu source quản trị.
4. `GuidePage` nói phương tiện cần được phê duyệt, trong khi quyết định đã chốt không có vehicle approval riêng; copy phải được sửa.
5. Assistant registry có tool cá nhân, nhưng worker không thể chạy chúng với authorization context hiện tại.
6. Prompt/response parser mới xử lý `responseText`, chưa đáp ứng contract Phase 0.
7. `AI_KNOWLEDGE_MANAGE_ALL` đã được seed; migration gán quyền bằng UUID role hệ thống thay vì resolve role code. Runtime Java/React vẫn phải permission-driven.
8. FE route title còn nhiều chuỗi không dấu; không ảnh hưởng dataset nhưng là debt i18n.

## Rủi ro bảo mật

- Cross-account data exposure nếu business tool không tái sử dụng access guard OWN/ALL.
- Retrieval leakage nếu filter scope/tenant áp dụng sau truy vấn hoặc `tenantId = null` bị hiểu sai.
- Prompt injection trong document có thể điều khiển generation nếu không tách instruction/data.
- Citation giả nếu nhận ID từ model thay vì allowlist từ retrieval result.
- Replay write action nếu thiếu idempotency binding theo account, conversation, action và payload hash.
- Dữ liệu thật trong log/tool payload nếu redaction chỉ dựa regex.
- Gemini Free Tier có điều khoản xử lý dữ liệu cần được Privacy/Legal phê duyệt trước khi gửi dữ liệu người dùng.

## Kết luận

Hệ thống có nền tảng tốt cho assistant job, model rotation, tool persistence, support escalation và schema RAG. Sổ quyết định nghiệp vụ đã được xác nhận với 24 mục `APPROVED` và TBD-013 `DEFERRED/OUT_OF_SCOPE`. RAG production, ingestion governance, response contract và business-tool authorization context vẫn chưa hoàn chỉnh; approval nghiệp vụ không thay thế implementation, security review hoặc UAT.

## Delta kiểm chứng ngày 19/11/2026

- Router matching không dấu dùng chung, phân biệt hướng dẫn với hành động; CLARIFICATION là enum nội bộ, không tạo trạng thái DB mới.
- Guest và authenticated dùng cùng policy route. Guest chỉ PUBLIC, không đọc tool OWN hoặc ghi; contextQuestion chỉ là gợi ý chủ đề công khai, không có quyền/identifier.
- Đăng nhập: câu nối tiếp chỉ dùng tối đa 3 câu TEXT của cùng actor trong cùng hội thoại được truy xuất có giới hạn; không tái thực hiện yêu cầu ghi/cá nhân từ lịch sử.
- Guest generation yêu cầu JSON và chỉ chấp nhận nhãn citation trong bằng chứng; malformed/unknown label không trả raw output cho UI.
- Các tool đọc ticket/subscription đã có worker execution. Không có tool đọc trạng thái hồ sơ, escalation, assign/reassign/resolve/close.
- Support dựa trên requester account/permission; không mặc định approved-customer cho hỗ trợ. CUSTOMER_DIRECT không phải requester support.
- Runtime RESOLVED hỗ trợ contextual activity và deadline auto-close 24 giờ; không coi quy tắc cũ “luôn chỉ đọc/2 lần/7 ngày” là enforcement đã có.
- 12 tài liệu còn DRAFT đã thành 13 sau khi thêm một file; chưa nhập hay dọn corpus dev đang hoạt động. Số điểm/kết quả thật nằm trong báo cáo, không suy ra từ số file.

- Bổ sung kiểm chứng retrieval: scope/tenant của source/document/chunk, thời hạn và version document/chunk/embedding; cache hit và đọc citation đều xác minh lại eligibility. Lexical bỏ cụm hỏi chung trong policy-v2; không đổi threshold/topK.

## Delta đã triển khai 20/11/2026

Gemini dùng credential pool và planner chung guest/auth: giữ key hiện tại, duyệt model được phép trước key khác, cooldown phân biệt xác thực/quota/model/tác vụ. Embedding đổi key nhưng giữ model/dimension. Chính sách ba lần gọi của ghi nhận 19/11 không còn mô tả đầy đủ Gemini; provider cũ vẫn giữ cơ chế tương thích. Xem [ADR-006](adr/ADR-006-gemini-credential-and-evidence-failover.md) và [runbook](../../GEMINI_CREDENTIAL_FAILOVER.vi.md).

Đợt 3 có bốn tài liệu về giá, hóa đơn/thanh toán/hoàn tiền, voucher và đặt chỗ; tổng 15 nguồn, 12 tài liệu thuộc các đợt, 840 câu gồm baseline. Đã đối chiếu SePay và đặt chỗ công khai có trong code, chưa có tool trợ lý thực hiện nghiệp vụ tài chính/đặt chỗ. Hướng dẫn không đồng nghĩa chatbot có thể thực hiện thay.

Lọc nguồn giữ câu cảnh báo không gửi bí mật; guest có output validator chung; context pack chọn đoạn liên quan, giữ giới hạn token. Chính sách cache có version để không dùng lại đáp án theo logic cũ. Kết quả nghiệm thu và ca chưa đạt ghi riêng tại [báo cáo đợt 3](evaluation/batch-03-report.md). Không publish/reset corpus dev; không suy ra khả năng vận hành từ số lượng tài liệu.

## Rà soát đợt 4 — 23/11/2026

- Nguồn chuẩn hiện có 16 tài liệu; bốn đợt đăng ký trong manifest và 1.104 câu, gồm baseline 120. Bốn chủ đề mới: xe vào/ra, tìm bãi/bản đồ, giờ/liên hệ/phản hồi, lỗi thường gặp.
- PublicParkingLotController có directory và nearby; NearbyParkingLotUseCaseImpl kiểm tra feature flag, tọa độ, bán kính nguyên 1–50km và limit 1–50. Giao diện có xin vị trí, nhập địa chỉ, chọn điểm, danh sách và chỉ đường. AiToolRegistry chưa có tool tra bãi, số chỗ, vị trí hoặc giờ mở cửa. Không thêm tool trong đợt này.
- ParkingLot chưa có trường giờ mở/đóng. Trạng thái hoạt động không thay giờ mở cửa; 24/7 là tiếp nhận hỗ trợ cấp công ty.
- ContactPage trước sửa không có handler/API gửi; thông tin PortalShared mâu thuẫn TBD-001/DEC-015. Đã đồng bộ contact đã duyệt, bản đồ theo địa chỉ và vô hiệu hóa form có giải thích/kênh thay thế. Chưa triển khai public-contact workflow hoặc monitoring SLA. Turnstile hiện có cho guest support không chứng minh đã có public-contact queue.
- Yêu cầu mở cổng trước sửa có thể rơi vào kiến thức hoặc hướng dẫn sửa phương tiện. Đã thêm nhận diện thao tác và chính sách không điều khiển cổng dùng chung; không chặn yêu cầu tạo phiếu thực sự chỉ vì nội dung mô tả lỗi cổng.
- TOPICS/LABELS của resolver trước sửa có nhóm tài chính trùng lặp. Đã loại lặp và thêm public topic; chỉ thừa kế nhãn giới hạn, không sao chép tọa độ/ID/lệnh trước.
- Live phát hiện câu thiếu vé bị chuyển sang đặt chỗ và bộ kiểm tra chặn nhầm câu phủ định. Giữ transcript trước sửa; bổ sung phân biệt vãng lai/đặt trước, xử lý hỏi tiếp có điều kiện và kiểm tra câu phủ định có phạm vi. Kết quả cuối phải lấy từ evidence đúng version, không từ lượt trước sửa.
- Không thay schema, reset, nạp hoặc publish DB dev; tài liệu giữ DRAFT.

### Delta ngày 24/11/2026

Ngoại lệ liên hệ công khai đã có kiểm tra cấu hình và nguồn trích dẫn, áp dụng cả FAQ/guest/cache. Lượt v9 trước sửa tốc độ được dừng có chủ đích sau 35 checkpoint, vì một ca mất 86 giây và một câu hứa mở cổng sau thanh toán. Bằng chứng không bị xóa. Giữ cooldown khi timeout đầy đủ, không loại nhầm model chỉ được phần ngân sách quá ngắn; giữ lỗi gốc và giảm thời gian chờ worker riêng cho timeout; prompt nhấn mạnh kiểm tra cổng riêng. Kết quả code mới cần lấy từ lượt nghiệm thu sau sửa, không ghép v9 thành kết quả bản cuối.

### Cấu hình thực và lỗi trả lời tiếp — 24/11/2026
Đã đọc metadata model trong DB dev bằng transaction READ ONLY, không sửa registry hoặc dữ liệu nghiệp vụ. Có chín model free-tier được ứng dụng cho phép; các lượt cũ chỉ dùng fixture hai model nên không đại diện toàn registry. Harness cuối dùng snapshot metadata được lọc không chứa key, đi qua model router/policy thật.
Lỗi đăng nhập ngắn từng kéo sang hướng dẫn xin quyền; hỏi tiếp về Google ở cửa sổ thường từng chỉ nhận kết luận thiếu bước xử lý. Đã bổ sung phân biệt ý định trong resolver và chọn đoạn FAQ theo tình huống. Giữ bản trả lời lỗi và chạy lại trên code cuối.
Bổ sung nhận diện hẹp hotline/email công ty để dùng FAQ có căn cứ, giảm lời gọi sinh không cần thiết. Không dùng nhận diện này cho tài khoản cá nhân, bãi, tài chính hoặc yêu cầu sửa thông tin. Mặc định sinh 4 giây và kiểm tra hạn chung trước từng candidate; kết quả tốc độ thực phải đọc theo từng cohort, không suy ra từ giá trị timeout.
