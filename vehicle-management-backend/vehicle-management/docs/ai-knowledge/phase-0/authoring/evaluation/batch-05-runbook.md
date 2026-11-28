# Chạy đợt 5 trên DB kiểm thử sạch

Chạy từ thư mục backend tìm theo repository, không dùng đường dẫn máy người khác. Cần JDK 21, Maven Wrapper, Node và Docker. Testcontainers dùng image `vehicle-management/postgres-pgvector-postgis:17`, tạo DB `vehicle_management_test` trên cổng ngẫu nhiên và dọn container khi kết thúc. Không trỏ datasource về DB dev/production, không bật billing và không tự thêm model vào registry.

## Offline và đóng băng

```powershell
node scripts/ai-knowledge/validate-batch05.mjs
node --test scripts/test-sync-ai-knowledge.mjs scripts/ai-knowledge/validate-batch05.test.mjs
.\mvnw.cmd -q -DskipTests test-compile
```

Không chạy trình sinh asset để sửa holdout. `build-batch05-assets.mjs` chỉ tái sinh 5 nguồn DRAFT, 240 câu chính và scenario; không gọi DB/provider hoặc đọc holdout. Sau sửa nguồn cần review rồi chạy `node scripts/sync-ai-knowledge.mjs --write`, validation và tạo cohort mới. Giữ nguyên dữ liệu cũ cùng hash kiểm kê.

Trước live chạy các test HTTP giả lập: `GeminiCredentialPoolTest`, `GeminiCredentialAvailabilityTest`, `GeminiGenerationDeadlineTest`, `AiModelRouterTest`, `AiModelPolicyServiceTest`, `AiModelFallbackPolicyTest`. Chúng kiểm thứ tự key/model, quota theo project, cooldown, auth isolation, embedding và deadline; không chứng minh độ chính xác Google.

## Smoke và kiểm thử chính

```powershell
.\scripts\ai-knowledge\run-batch05-google-live.ps1 -Smoke -CaseLimit 8
.\scripts\ai-knowledge\run-batch05-google-live.ps1
.\scripts\ai-knowledge\run-batch05-google-live.ps1 -FullRegression
node scripts/ai-knowledge/summarize-batch05-evidence.mjs
```

Wrapper đọc đúng các biến Google đã cấu hình từ `.env` vào child process, không in giá trị key. Chỉ chạy một lệnh Maven tại một thời điểm. Mỗi lần chạy tạo thư mục cohort riêng dưới `target/batch05-google-live`. Model lấy từ registry migration/policy được duyệt trong DB sạch; catalog hiện hành chỉ kiểm khả năng hỗ trợ, không tự cấp phê duyệt free tier hoặc khẳng định quota.

Mặc định chạy 240 câu chính + 40 holdout. FullRegression thêm 1.104 câu cũ và 24 bổ sung, giữ cohort/tập riêng trong kết quả; 4 probe tổng hợp generation luôn báo riêng, không thay mẫu số. Smoke chọn ít câu ở cả bốn chủ đề. FAQ phù hợp được phép trả trực tiếp; probe không ép bỏ FAQ. Nếu một stage lỗi hoặc provider không tiếp tục được, dừng và đọc checkpoint trước khi chạy lại; không tự lặp để cạn quota.

## Luồng nhập tài liệu

Harness thực hiện API tạo/chọn nguồn đúng scope → upload multipart có Idempotency-Key → worker extract/chunk/embedding → preview có documentVersion → duyệt → chọn candidate index đã được chuẩn bị hoặc tạo khi chưa có → build → activate. Không đặt READY bằng SQL. Nạp đủ 21 nguồn, gồm 16 nguồn cũ, để kiểm cạnh tranh nguồn. Duyệt trên DB kiểm thử không phải phê duyệt phát hành các file DRAFT lên DB dùng chung.

HTTP chatbot đi qua context path cấu hình của ứng dụng. Định danh JWT là fixture test, nhưng account/role/permission, quyền đọc nguồn, chat storage, queue, orchestration, retrieval và Google adapter dùng code thật. Kho tệp riêng thay MinIO để cô lập test; không tuyên bố kiểm MinIO hoặc Keycloak đầu cuối. PUBLIC trong harness là tài khoản đã xác thực có scope công khai; luồng khách ẩn danh cần nghiệm thu riêng.

## Checkpoint và tiếp tục

Lưu checkpoint, transcript, HTTP attempts, registry/catalog snapshot và hash sau mỗi case/stage. Kiểm tra source/corpus/prompt/model và holdout freeze trước tiếp tục. Có thể chọn các case ID bằng `-CaseIdsFile <file-mỗi-dòng-một-ID>` hoặc tiếp tục từ `-ResumeFrom <thư-mục-cohort>` khi manifest, holdout, runtime router và registry vẫn cùng hash; case đã hoàn tất được bỏ qua. Thay đổi provenance buộc chạy cohort mới. Harness dùng DB tạm mới cho mỗi lần chạy, nạp lại corpus và không tự phục hồi DB cũ; không coi checkpoint ở target là DB snapshot hoặc cache vector. Nếu chạy lại, giữ cohort cũ và báo riêng số lần gọi bổ sung. Không gộp điểm qua các cohort khác corpus/prompt/model.

Khi quota/cooldown chặn toàn bộ ứng viên hợp lệ, giữ alias, model, loại lỗi, Retry-After nếu có rồi dừng live. Deadline chỉ là hết ngân sách request, không chứng minh mọi key hết quota. Không đổi embedding model/dimension/prompt trên index; muốn đổi cần index riêng và nghiệm thu riêng.

## Chấm và bàn giao

Chấm từng case theo [rubric](batch-05-rubric.md) và nguồn code. Người review độc lập xử lý các case nhạy cảm, mơ hồ và holdout có nghi vấn gold trước khi tính điểm. Bảng NOT_RUN/NOT_GRADED không được chuyển PASS theo HTTP hoặc keyword. Soát source/version/hiệu lực/quyền, thao tác ngoài ý muốn và phân tách FAQ/generation/tool/handoff.

Chỉ đưa JSON/Markdown đã loại bí mật vào evidence. Không Git `.env`, token, database dump, vector lớn, private upload objects hoặc run.log thô. Không commit/push/deploy, không phê duyệt nguồn trên DB dev/production.

## Trạng thái tại lần bàn giao

Lượt cuối dừng tại B5-GEN-01 do service 503/timeout trong ngân sách, không phải bằng chứng mọi credential hết quota. Mới có 8/240 câu chính hoàn thành, chưa có whole-case PASS; holdout và hồi quy live chưa chạy. Collector citation API/drain HTTP được bổ sung sau cohort 20261126T192655Z và mới kiểm tra offline. Vì fingerprint harness/provenance đổi, chạy cohort mới với bản hiện tại khi Google khả dụng; không ép ResumeFrom của cohort cũ hoặc kế thừa điểm còn thiếu nguồn.

Mặc định generation hiện là20s và provider plan60s; .env cục bộ đã đổi riêng generation4s sang20s. Có thể cấu hình ngắn hơn khi phù hợp, nhưng phải lưu cùng cohort và không coi deadline là hết quota.
