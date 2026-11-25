# Chuẩn hóa danh mục hỗ trợ — 19/11/2026

## Khảo sát và quyết định trước migration

Khảo sát chỉ đọc PostgreSQL local (`vehicle-pgvector`, `localhost:5434/vehicle_management_db`) xác nhận 10 CUSTOMER ACTIVE, 8 INTERNAL ACTIVE, 3 phiếu lịch sử và phiên bản Flyway mới nhất `20261118030000`. Không có category mang created_by/updated_by khác NULL. Database local chưa được áp dụng migration mới.

Hiện có 10 seed CUSTOMER và 8 seed INTERNAL; CRUD, audience permission và ANY/ALL đã tồn tại. Schema cho phép `feature_code` nullable, VARCHAR(100), không có foreign key/enum. Giữ các feature hiện có; dùng NULL cho nhóm chung, bảo mật, dữ liệu chưa có một feature duy nhất. Feature chỉ là metadata; quyền sử dụng lấy từ effective permissions.

Tạo 23 bản ghi mới với code và UUID ổn định. Không đổi code, tên hoặc category_id của các bản ghi cũ, không UPDATE category_id trên phiếu. Những seed cũ còn nguyên metadata và không có người tạo/cập nhật được chuyển INACTIVE. Bản ghi tùy chỉnh (kể cả trùng code cũ nhưng khác metadata) được giữ nguyên. Code mới trùng bản ghi có sẵn thì dừng migration bằng lỗi rõ ràng, không ghi đè cấu hình quản trị viên.

| Code cũ | Nhóm thay thế cho phiếu mới | Xử lý lịch sử |
|---|---|---|
| CARD_NOT_WORKING | CUSTOMER_CARD_QR | Giữ bản ghi cũ, INACTIVE |
| SUBSCRIPTION_PROBLEM | CUSTOMER_SUBSCRIPTION | Giữ bản ghi cũ, INACTIVE |
| PAYMENT_PROBLEM, WRONG_FEE | CUSTOMER_PAYMENT_BILLING | Gộp ngữ nghĩa chỉ cho phiếu mới; seed cũ INACTIVE |
| PARKING_HISTORY_REQUEST | CUSTOMER_HISTORY_DATA | Phạm vi mới khác; seed cũ INACTIVE |
| PROFILE_OR_VEHICLE_UPDATE | CUSTOMER_PROFILE_VEHICLE | Chỉ lỗi hệ thống; seed cũ INACTIVE |
| OTHER | CUSTOMER_OTHER_SYSTEM | Chỉ lỗi hệ thống; seed cũ INACTIVE |
| LOST_CARD, VEHICLE_DAMAGE, STAFF_COMPLAINT | Không chuyển | INACTIVE, giữ nguyên tên/liên kết lịch sử |
| ACCOUNT_ACCESS | INTERNAL_ACCOUNT_PERMISSION | Seed cũ INACTIVE |
| PARKING_LAYOUT | INTERNAL_PARKING_CONFIGURATION | Seed cũ INACTIVE |
| PARKING_OPERATION | INTERNAL_PARKING_OPERATION | Seed cũ INACTIVE |
| DEVICE_INCIDENT | INTERNAL_DEVICE_INTEGRATION | Seed cũ INACTIVE |
| PRICING_CONFIGURATION, TICKET_CONFIGURATION, VEHICLE_CONFIGURATION | INTERNAL_CATALOG_PRICING | Gộp cho phiếu mới; seed cũ INACTIVE |
| WORKFORCE | INTERNAL_WORKFORCE_SHIFT | Seed cũ INACTIVE |

`INTERNAL_LOST_CARD_WORKFLOW` luôn là bản ghi mới. Phiếu báo mất thẻ là nghiệp vụ riêng; danh mục hỗ trợ chỉ tiếp nhận lỗi khi lập/xử lý phiếu đó. Không thay nghiệp vụ xác minh, tính phí, hóa đơn, thanh toán, khóa thẻ.

## Phân quyền

Mọi category khả dụng yêu cầu `SUPPORT_TICKET_CREATE_OWN` và permission audience tương ứng: `SUPPORT_TICKET_CATEGORY_CUSTOMER_OWN` hoặc `SUPPORT_TICKET_CATEGORY_INTERNAL_OWN`. Runtime không kiểm tra role. CUSTOMER mặc định không thêm permission nghiệp vụ. INTERNAL dùng ANY với các mã đã có trong database:

| Danh mục INTERNAL | Feature | ANY permission bổ sung |
|---|---|---|
| ACCOUNT_PERMISSION | ACCOUNT_ACCESS | Không thêm: người bị thiếu quyền vẫn báo được lỗi |
| PARKING_CONFIGURATION | PARKING_LOT | PARKING_LOT_READ_ALL |
| PARKING_OPERATION | PARKING_SESSION | PARKING_SESSION_READ_ALL, PARKING_SESSION_CHECK_IN_ALL, PARKING_SESSION_CHECK_OUT_ALL |
| DEVICE_INTEGRATION | DEVICE | DEVICE_READ_ALL |
| CATALOG_PRICING | PRICING | PRICE_PLAN_READ_ALL, PRICE_RULE_READ_ALL, TICKET_TYPE_READ_ALL, VEHICLE_TYPE_READ_ALL, VOUCHER_READ_ALL |
| SUBSCRIPTION_CARD | SUBSCRIPTION | SUBSCRIPTION_READ_ALL, CARD_READ_ALL |
| LOST_CARD_WORKFLOW | LOST_CARD_REPORT | LOST_CARD_REPORT_READ_ALL, LOST_CARD_REPORT_CREATE_ALL, LOST_CARD_REPORT_UPDATE_ALL |
| BILLING_FINANCE | BILLING | INVOICE_READ_ALL, PAYMENT_READ_ALL, WALLET_READ_PARTNER, WALLET_READ_ALL, WALLET_RECONCILIATION_READ_ALL |
| WORKFORCE_SHIFT | WORKFORCE | EMPLOYEE_READ_ALL, EMPLOYEE_READ_OWN, SHIFT_READ_ALL |
| CUSTOMER_SUPPORT | SUPPORT | CUSTOMER_READ_ALL, CHAT_CONVERSATION_READ_OWN, CHAT_CONVERSATION_READ_ALL, SUPPORT_TICKET_READ_ALL, SUPPORT_TICKET_READ_ASSIGNED |
| REPORT_DATA | NULL | REPORT_READ_ALL, PARKING_SESSION_READ_ALL |
| PERFORMANCE_SECURITY | NULL | Không thêm: mọi tài khoản INTERNAL đều báo được |
| OTHER_SYSTEM | NULL | Không thêm: mọi tài khoản INTERNAL đều báo được |

Các permission có sẵn được tái sử dụng; không tạo permission hoặc role mới. Migration kiểm tra đủ permission, không âm thầm tạo mapping rỗng nếu mã bị thiếu.

## API và giao diện

Giữ nguyên payload CRUD và `/available`. Danh mục không được phép vẫn trả 403, có `data.code=SUPPORT_CATEGORY_NOT_AVAILABLE` để client tải lại lựa chọn; danh mục được phép nhưng đã INACTIVE trả 409 với thông báo tiếng Việt. Server lấy feature và priority từ category, không tin giá trị do client truyền. API public chỉ trả ACTIVE CUSTOMER không yêu cầu permission bổ sung; guest gửi trực tiếp ID INTERNAL cũng bị từ chối. Xác thực category public dùng một truy vấn EXISTS để giữ query budget của luồng guest.

Danh sách tải category và toàn bộ mapping bằng hai truy vấn cố định. Form tải lại khi mở, có tìm kiếm, mô tả, loading/empty/error/retry; lỗi category khi gửi sẽ tải lại danh sách và yêu cầu chọn lại. Các luồng chat/chatbot tái sử dụng dialog chung. CRUD quản trị giữ ANY/ALL, audience, thứ tự, feature, permission, priority và vô hiệu hóa mềm.

## Kiểm thử và giới hạn

Không áp dụng migration lên database phát triển/sản xuất trong tác vụ này; kiểm chứng trên PostgreSQL Testcontainers riêng. Migration chỉ thêm mapping mặc định cho bản ghi vừa được insert; chạy lại không khôi phục mapping mà quản trị viên đã chỉnh sửa.

## Danh sách file thuộc tác vụ

Báo cáo này: `docs/support-category-standardization.md`. Những file hóa đơn, đơn mua vé, snapshot/chat và các thay đổi khác đã có từ trước không thuộc danh sách này.

### Backend production (relative to vehicle-management-backend/vehicle-management/)

- `src/main/java/com/ban/vehicle_management/domain/operations/supportticketcategory/policy/SupportTicketCategoryPolicy.java`
- `src/main/java/com/ban/vehicle_management/application/operations/supportticketcategory/port/in/SupportTicketCategoryPortIn.java`
- `src/main/java/com/ban/vehicle_management/application/operations/supportticketcategory/port/out/SupportTicketCategoryPortOut.java`
- `src/main/java/com/ban/vehicle_management/application/operations/supportticketcategory/usecase/SupportTicketCategoryUseCaseImpl.java`
- `src/main/java/com/ban/vehicle_management/application/operations/supportticket/usecase/SupportTicketUseCaseImpl.java`
- `src/main/java/com/ban/vehicle_management/application/operations/guestsupport/service/GuestSupportService.java`
- `src/main/java/com/ban/vehicle_management/entrypoint/controller/operations/PublicSupportController.java`
- `src/main/java/com/ban/vehicle_management/infrastructure/persistence/adapter/operations/SupportTicketCategoryPersistenceAdapter.java`
- `src/main/java/com/ban/vehicle_management/infrastructure/persistence/database/repository/operations/SupportTicketCategoryRepository.java`
- `src/main/java/com/ban/vehicle_management/infrastructure/persistence/database/projection/operations/SupportTicketCategoryPermissionProjection.java`
- `src/main/resources/db/migration/V20261119100000__standardize_support_category_catalog.sql`

### Backend tests (relative to vehicle-management-backend/vehicle-management/)

- `src/test/java/com/ban/vehicle_management/application/operations/supportticketcategory/usecase/SupportCategoryAvailabilityTest.java`
- `src/test/java/com/ban/vehicle_management/entrypoint/operations/SupportCategoryCatalogIT.java`
- `src/test/java/com/ban/vehicle_management/application/operations/guestsupport/service/GuestSupportServiceTest.java`
- `src/test/java/com/ban/vehicle_management/application/operations/guestsupport/service/GuestSupportSqlStatementCountIT.java`
- `src/test/java/com/ban/vehicle_management/domain/operations/supportticketcategory/policy/SupportTicketCategoryPolicyTest.java`
- `src/test/java/com/ban/vehicle_management/infrastructure/persistence/adapter/operations/SupportTicketCategoryPersistenceAdapterTest.java`

### Frontend (relative to vehicle-management-frontend/vehicle_management_react/)

- `package.json`
- `scripts/check-support-category-administration.mjs`
- `src/features/customer-portal/pages/SupportPage.tsx`
- `src/features/support/pages/SupportCategoryWorkflowPage.tsx`
- `src/features/support/components/CreateSupportTicketDialog.tsx`
- `src/features/support/components/GuestSupportRequests.tsx`
- `src/features/support/components/SupportCategoryPicker.tsx`
- `src/features/support/hooks/useAvailableSupportCategories.ts`
- `tests/support-categories-harness.html`
- `tests/support-categories-harness.tsx`
- `tests/support-categories.behavior.spec.ts`

## Kết quả kiểm thử thực chạy

| Lệnh | Kết quả |
|---|---|
| `mvnw.cmd -B -ntp -Dmaven.repo.local=.m2-local -DargLine=-Xmx1024m test` | PASS: 2.181 test, 0 failures, 0 errors, 0 skipped |
| `mvnw.cmd -B -ntp -Dmaven.repo.local=.m2-local -DargLine=-Xmx1024m -Dtest=SupportCategoryCatalogIT,PublicSupportSecurityIT,GuestSupportSqlStatementCountIT,PendingCustomerSupportFlowIT test` | PASS: 22 PostgreSQL integration/security tests, gồm 11 catalog/migration tests |
| Backend chọn lọc trước lượt toàn bộ | PASS: 61 test gồm category, guest query budget và PostgreSQL catalog |
| `npm run lint` | PASS (TypeScript noEmit) |
| `npm run build` | PASS (tsc + Vite production build) |
| `npm test` | PASS: toàn bộ script regression frontend hiện có |
| `npm run test:support-categories` | PASS: contract/navigation/permission guards |
| `npm run test:support-categories:e2e` | PASS: 5 Playwright tests, desktop/mobile, API search/order, loading/empty/error/retry, admin-added category, revoked permission 403 và inactive 409 |
| `npx playwright test tests/guest-support.behavior.spec.ts --timeout=60000` | PASS: 15 test guest/chatbot, tạo/tra cứu phiếu, CAPTCHA, mobile, thay đổi đăng nhập |
| `git diff --check` | PASS |

Log nằm trong `target/support-category-full-backend-final.log`, `target/support-category-verification/` và `target/support-category-integration-final-pass.log`.

## Rủi ro triển khai và phạm vi còn lại

- Migration chưa được áp dụng lên database phát triển/sản xuất. Khi backend khởi động với Flyway, migration mới sẽ được áp dụng; không có migration cũ nào bị sửa.
- Nếu dữ liệu triển khai đã có một code CUSTOMER_/INTERNAL_ mới nhưng mang UUID khác seed, migration dừng trong transaction với thông báo collision. Cần rà soát bản ghi xung đột thay vì ghi đè dữ liệu quản trị viên.
- Những category cũ đã được tùy chỉnh được giữ nguyên và có thể vẫn ACTIVE. Số 10 CUSTOMER + 13 INTERNAL là bộ chuẩn được thêm mới; các category tùy chỉnh không bị xóa để ép tổng số bản ghi.
- Việc mô tả chức năng gặp lỗi được hướng dẫn ở placeholder và mô tả category; title/content bắt buộc qua validation backend. Hệ thống không tự suy đoán ngữ nghĩa văn bản để kết luận một sự cố có thật hay không.
- Chuỗi migration đã được kiểm tra trên PostgreSQL 17 với pgvector/PostGIS trong Testcontainers, không dùng database thật của người dùng.


## Bổ sung ngày 19/11/2026: tách chat khách hàng–đối tác khỏi hỗ trợ hệ thống

### Quy tắc nghiệp vụ cuối cùng

- `CUSTOMER_DIRECT` là chat khách hàng–nhân sự đối tác: trao đổi thông thường và chia sẻ đơn hàng theo quyền. Không tạo, gắn, chia sẻ phiếu hỗ trợ hoặc mở lịch sử yêu cầu tại đây, kể cả tài khoản có quyền tạo phiếu.
- Khách hàng và nhân sự đối tác gửi yêu cầu của chính mình tới hệ thống qua `ASSISTANT_SUPPORT`, sau đó trao đổi với người xử lý system_admin qua `SUPPORT_REQUESTER_DIRECT`. Quyền xử lý hàng đợi hiện được seed cho SYSTEM_ADMIN; logic ứng dụng tiếp tục kiểm tra permission.
- Cách chuyển `CUSTOMER_DIRECT` sang API tạo phiếu cá nhân ở lượt trước đã bị loại bỏ. Quyền tạo phiếu không thay thế điều kiện đúng loại hội thoại.

### Sửa giao diện

- Không hiển thị tạo phiếu, lịch sử yêu cầu, gắn/đổi phiếu hoặc menu ba chấm rỗng trong chat khách hàng–đối tác; không tải danh sách phiếu riêng trong chat này.
- Dialog và callback kiểm tra lại quyền cùng loại hội thoại, tránh dialog còn mở khi đổi hội thoại. Helper API chỉ cho phép hai loại hội thoại hỗ trợ hệ thống; không có fallback sang API tạo phiếu thông thường.
- Đổi tên kênh CUSTOMER_DIRECT thành “Trao đổi khách hàng - đối tác”; bỏ chữ “Ticket” khỏi tab đếm hội thoại đang xử lý.
- Icon đơn hàng vẫn chỉ hiện cho thành viên có quyền gửi tin và quyền đọc subscription tương ứng: CUSTOMER cần READ_OWN, phía đối tác cần READ_ALL.
- Nút icon làm mới cùng hàng Danh mục hỗ trợ, không polling; giữ bản nháp khi làm mới. Validation báo đúng trường thiếu. Các sửa dropdown khách hàng/nội bộ từ lượt trước được giữ.

### Bảo vệ backend

- Giữ chặn loại hội thoại không phải hỗ trợ ở API tạo/chia sẻ phiếu và API lịch sử.
- Chặn gắn `contextTicketId` vào tin nhắn text/ảnh của chat thông thường trước khi ghi dữ liệu; tin nhắn không mang ngữ cảnh phiếu tiếp tục hoạt động.
- Chỉ hội thoại hỗ trợ ACTIVE nhận phiếu mới hoặc được đăng/chia sẻ lại card.
- Người xử lý không thể tạo phiếu cá nhân trong hội thoại của người yêu cầu khác; không đổi quyền sở hữu phiếu.
- Không sửa migration, không xóa lịch sử và không ghi phiếu thử vào database thực trong lượt này.

### Kiểm tra thực chạy

| Kiểm tra | Kết quả |
|---|---|
| SupportTicketConversationServiceTest, SupportTicketChatMessageContextServiceTest, SupportTicketUseCaseImplTest, ChatConversationUseCaseImplTest | PASS: 78 test, 0 failure/error/skipped |
| Playwright support-flows | PASS: 14 ca, gồm cả hai phía CUSTOMER_DIRECT, hai loại hỗ trợ hệ thống cho khách hàng/đối tác, thiếu quyền và chia sẻ đơn hàng |
| Playwright support-categories + shared-select + guest-support | PASS: 23 ca |
| Tổng Playwright | PASS: 37 ca |
| npm run lint | PASS |
| npm test | PASS |
| npm run build | PASS |

Các ca trình duyệt sử dụng API mock; các test application kiểm tra không ghi message/event/link khi loại hội thoại không hợp lệ. Log ở `target/support-category-verification/chat-boundary-backend.log`, `chat-boundary-browser-final.log`, `chat-boundary-lint.log`, `chat-boundary-regression.log`, `chat-boundary-build.log`.

### File chính của phần sửa ranh giới hội thoại

Frontend:
- `src/features/support/utils/chatActionAccess.ts`
- `src/features/support/api/supportApi.ts`
- `src/features/support/pages/OperationsSupportCenterPage.tsx`
- `tests/support-flows.behavior.spec.ts`

Backend:
- `src/main/java/com/ban/vehicle_management/application/operations/supportticket/service/SupportTicketConversationService.java`
- `src/main/java/com/ban/vehicle_management/application/operations/supportticket/service/SupportTicketChatMessageContextService.java`
- `src/main/java/com/ban/vehicle_management/application/operations/supportticket/usecase/SupportTicketUseCaseImpl.java`
- Ba test tương ứng trong `src/test/java/com/ban/vehicle_management/application/operations/supportticket/service/`.
