# Bàn giao chatbot và dọn repository — 25/11/2026

Code và tài liệu đang ở working tree, gồm thay đổi từ các đợt trước; chưa commit hoặc push. Team cần review diff rồi commit cả file mới liên quan. Kết quả dọn không đồng nghĩa chatbox đã đạt nghiệm thu.

## Phạm vi đã dọn

- Xóa script Python, cache Python, `target` backend, `dist` frontend, kết quả chạy tạm và log cục bộ: 10.262 file, khoảng 424 MiB.
- Gỡ các script Python, `.pyc` và `debug.log` khỏi Git index; thêm ignore để tránh đưa file sinh tự động trở lại Git.
- Giữ source Java/TypeScript, test, lockfile, migration SQL, Docker, tài nguyên UI và bằng chứng kiểm thử có chủ đích trong `docs/ai-knowledge/evidence`.
- Giữ `.env` cục bộ và các thư viện đã cài; chúng không được đưa lên Git. Team tạo cấu hình từ `.env.example` và nhận bí mật qua kênh riêng.
- Bản sao script/log đã gỡ và diff trước dọn nằm ở `.tmp/handoff-cleanup-20261125/` tại gốc repository, bị ignore. Không cần đưa bản sao này lên Git để chạy dự án.

## Môi trường và khởi động

Backend dùng JDK 21 và Maven Wrapper. Frontend dùng Node.js/npm, có `package-lock.json`; các tiện ích `.mjs` dùng thư viện chuẩn của Node. Docker cần cho hạ tầng và test DB cô lập. LM Studio chỉ cần khi dùng Qwen local.

Từ thư mục `vehicle-management-backend/vehicle-management`:

```powershell
# Chỉ sao chép khi chưa có .env; sau đó điền cấu hình riêng của máy.
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Các compose nằm ngay thư mục backend, tách PostgreSQL/pgvector/PostGIS, Redis, RabbitMQ, Keycloak, MinIO và OCR. Chọn dịch vụ theo cấu hình dự án. Ví dụ DB: `docker compose -f docker-compose.postgres-pgvector.yml up -d`. Port mặc định compose là 5434; cần đối chiếu `DB_PORT` trong `.env`. Khi chạy backend, Flyway có thể áp dụng migration lên DB cấu hình: dùng DB riêng khi review/test, không trỏ nhầm DB dùng chung.

Lệnh thông thường để team build/test, rồi chạy backend:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Từ `vehicle-management-frontend/vehicle_management_react`, tạo `.env` từ `.env.example` nếu chưa có rồi chạy:

```powershell
npm ci
npm run lint
npm run build
npm run dev
```

Các lệnh build ứng dụng ở trên **chưa chạy lại trong lượt dọn** vì giới hạn RAM. `node_modules`, `.m2-local`, model và dữ liệu Docker không được Git chuyển sang máy khác; máy mới cài/tải lại khi cần.

## Lệnh hiện tại, không cần Python

Chạy từ thư mục backend:

```powershell
node --test scripts/test-sync-ai-knowledge.mjs scripts/ai-knowledge/test-summarize-qwen-api.mjs
node scripts/sync-ai-knowledge.mjs
# Chỉ dùng khi chủ động sinh lại bản xuất và manifest từ tài liệu nguồn:
node scripts/sync-ai-knowledge.mjs --write
```

Đã chạy: **13/13 test qua**, kiểm tra 16 tài liệu, 4 đợt, 1.104 câu hỏi và 294 liên kết nội bộ không có lỗi. Chế độ kiểm tra không sửa DB, không gọi Google hoặc Qwen. Script `phase-0/authoring/evaluation/validate-phase0.ps1` cũng đã chuyển sang Node.

Các wrapper Python gọi model, kiểm key và tổng hợp báo cáo cũ đã gỡ. Không còn yêu cầu cài Python để chạy những công cụ bàn giao trên. Tên/lệnh Python trong bằng chứng cũ được giữ để giải thích lịch sử; không dùng chúng làm lệnh chạy hiện tại.

## Chạy tiếp nghiệm thu API trên máy đủ tài nguyên

Launcher hiện tại: `scripts/ai-knowledge/run-qwen-api-acceptance.ps1`, viết bằng PowerShell, chạy Java. Các pha tách compiler khỏi JVM kiểm thử; source thay đổi thì compile lại. Mỗi lần phải chọn thư mục output mới dưới `target` để giữ lỗi cũ.

```powershell
.\scripts\ai-knowledge\run-qwen-api-acceptance.ps1 -Phase Compile -OutputDirectory target/handoff-compile-01
.\scripts\ai-knowledge\run-qwen-api-acceptance.ps1 -Phase Offline -OutputDirectory target/handoff-offline-01
.\scripts\ai-knowledge\run-qwen-api-acceptance.ps1 -Phase Db -OutputDirectory target/handoff-db-01
.\scripts\ai-knowledge\run-qwen-api-acceptance.ps1 -Phase Api -Dataset batch-01 -Offset 0 -Limit 6 -OutputDirectory target/handoff-api-b1-01
node scripts/ai-knowledge/summarize-qwen-api.mjs target/handoff-api-b1-01
```

Ngưỡng RAM trống: Compile 1.024 MB, Offline 512 MB, DB 1.024 MB, API 1.536 MB. Đây chỉ là điều kiện bắt đầu. Launcher kiểm fingerprint nguồn; pha Compile giải quyết JUnit launcher đúng phiên bản từ Maven cho máy mới. **Bản PowerShell thay thế đã kiểm cú pháp, chưa được chạy compile/DB/API đầy đủ**. Cần kiểm lại trên máy team trước khi dựa vào kết quả của nó.

**Cấu hình chưa đồng nhất cần chốt trước khi chạy live:** lượt component gần nhất dùng context **512**, parallel **1**; HTTP fixture Java và guard của launcher vẫn yêu cầu context **2048**, parallel **1**, cùng hai model `qwen3.5-4b` và `text-embedding-qwen3-embedding-0.6b` đã nạp. Bản dọn này không đổi cấu hình model. Nếu tiếp tục yêu cầu context 512 thì team cần điều chỉnh và kiểm chứng giới hạn prompt của HTTP fixture trước; không bỏ guard để ghi nhận đạt giả.

Dataset `all` có kế hoạch 1.128 câu: 984 của bốn đợt, 120 câu nền và 24 câu mới. Có thể chạy `baseline`, `batch-01` đến `batch-04`, `new` bằng shard tuần tự; mỗi lượt vẫn giữ đủ 16 tài liệu. Kiểm tra phủ đủ ID, không trùng/lọt câu khi gộp kết quả. Tập 24 câu mới chưa là holdout mù do người độc lập biên soạn.

Summarizer ghi câu trả lời thực, nguồn, đường xử lý và trạng thái. HTTP 200 không phải đúng nghiệp vụ. Grade cần verdict, lý do và SHA-256 của chính câu trả lời; chưa chấm giữ `NOT_GRADED`, chưa chạy giữ `NOT_RUN`. Xem [runbook và bằng chứng API](evidence/qwen-api-acceptance-2026-11-24/README.md) cho browser và chấm nghĩa.

## Trạng thái thực tế để team tiếp tục

- Toàn bộ 16 tài liệu đang DRAFT; chưa tự động import/phát hành lên DB dev trong lượt dọn.
- Luồng FAQ dùng chung giữa Google và Qwen đã có trong code. Chọn `QWEN_LOCAL` không được gọi Google; chọn `GOOGLE_API` không được gọi Qwen. Không dùng nhà cung cấp kia làm dự phòng trái chế độ. Xem [luồng và quyền](COMMON_KNOWLEDGE_RUNTIME.vi.md).
- 8 câu thiếu ý cũ đã sửa và có kết quả hồi quy offline; lượt 60/61 rồi 61/61 được giữ trong bằng chứng. Đó là test JVM riêng, không phải kiểm thử SQL/HTTP đầy đủ trên bản cuối.
- Lượt Qwen live gần nhất: **40/40 lời gọi trực tiếp LM Studio thành công; 30 câu đủ ý, 4 thiếu ý, 6 sai; nếu tính nguồn hợp lệ chỉ 27/40 đạt**. Hai lượt kiểm lại ngữ cảnh nguồn được ghi riêng. Xem [báo cáo live 512](evidence/qwen-512-live-20261124-235027/README.md) và [từng câu/điểm/lý do](evidence/qwen-512-live-20261124-235027/answers-and-grades.csv). Đây là mẫu component với nguồn đưa sẵn, không chứng minh độ chính xác toàn chatbox.
- Còn lỗi phủ định, hiểu luồng mất thẻ, thiếu ý và trích nguồn không có thật. Một phần lỗi liên quan ngữ cảnh nguồn bị rút ngắn. Chưa có nghiệm thu API trọn luồng, retrieval SQL, phân quyền dữ liệu hoặc UI đủ các vai trò trên bản cuối. Không lấy tỷ lệ của 40 câu làm tỷ lệ cho 1.128 câu.
- Registry hiện có 7 business tool; 22 câu đòi tool chưa có phải báo khoảng trống chức năng, không giả đã xử lý hoặc bỏ khỏi mẫu số.
- Hai migration `V20261124120000__strict_ai_runtime_provider_modes.sql` và `V20261124170000__shared_reviewed_faq_audit.sql` được giữ trong source. Chưa xác nhận đã áp dụng trên DB dev; cần kiểm trạng thái Flyway trước khi chạy.
- Chưa nghiệm thu Google tương đương Qwen. Có thể dùng chung nguồn và đáp án chuẩn nhưng phải đo riêng từng nhà cung cấp; không suy chất lượng từ việc chuyển model.

Mục tiêu nghiệm thu vẫn là chấm câu trả lời thực qua API: hướng xử lý ≥98%, nguồn đúng trong top 5 ≥95%, đúng và đủ ý ≥95% từng tài liệu/nhóm ngôn ngữ, nguồn/phiên bản/quyền hợp lệ 100%, không lộ dữ liệu hay khẳng định thao tác/tài chính sai. Bảng kết quả cần phân biệt FAQ, Qwen viết, tra cứu dữ liệu và hỏi lại.

## Trước khi đưa lên Git

Review `git status`, `git diff` và `git diff --cached`; có sẵn nhiều thay đổi từ các đợt trước. Giữ `.env`, key thật, dữ liệu Docker, model, cache và output sinh tự động ngoài commit. Giữ các file mới Java/TypeScript/test/tài liệu cần thiết cùng thay đổi tương ứng. Không xóa migration hoặc bằng chứng lỗi để làm báo cáo đẹp hơn.

Kiểm tra cuối: `git diff --check` của working tree qua; Git index cũ còn 12 cảnh báo dòng trống cuối file tài liệu. Bản staged và working tree đang khác nhau từ trước, nên cần review rồi stage phiên bản cuối có chủ đích; lượt dọn không stage đè toàn bộ thay đổi cũ. Không còn Python hoặc file `.env` thật trong index. Quét 3.731 file text có thể đưa lên Git và phần thêm trong diff staged không thấy mẫu key Google/GitHub hoặc private key phổ biến; đây không phải kiểm toán toàn bộ lịch sử Git.

Lượt dọn này không commit/push, không gọi model, không khởi động dịch vụ hay sửa DB. Kết quả build/kiểm thử ứng dụng và nghiệm thu API còn lại cần thực hiện trên máy team.
