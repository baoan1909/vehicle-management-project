# Báo cáo bước nền và đợt 1 — 19/11/2026

> Bàn giao 25/11/2026: các lệnh Python trong báo cáo này là bằng chứng lịch sử; wrapper đã được gỡ. Lệnh hiện tại và các giới hạn kiểm thử được ghi trong [TEAM_HANDOFF.vi.md](../../../TEAM_HANDOFF.vi.md). Không dùng log cũ để kết luận bản code hiện tại đã nghiệm thu.

Trạng thái: đã triển khai nền và đúng bốn tài liệu nghiệp vụ. Kiểm chứng bổ sung đã phục hồi generation 8/8 câu bằng model dự phòng; xem [báo cáo fallback](model-fallback-verification.vi.md). Chưa nghiệm thu độ đúng toàn hệ thống; vẫn còn 25 câu dưới ngưỡng bằng chứng. Không publish knowledge, purge DB, commit, push hoặc deploy.

## Vị trí bàn giao

Theo yêu cầu cập nhật trong phiên, toàn bộ tài liệu mới được đặt trong `vehicle-management-backend/vehicle-management/docs/ai-knowledge`:

- `phase-0/authoring/`: nguồn chuẩn, ma trận, ADR lịch sử, dataset, rubric và báo cáo.
- `phase-0/knowledge/`: bản upload-safe được sinh từ nguồn chuẩn; metadata nằm trong HTML comment và extractor bỏ qua.
- `phase-0/authoring/knowledge-manifest.json`: version, scope, SHA-256 hai bản và số lượng dataset.
- `REANALYSIS_AND_KNOWLEDGE_REFRESH_PLAN.vi.md`: kế hoạch gốc, có trạng thái triển khai mới ở đầu.

Thư mục `docs/ai-assistant/phase-0` ở root tiếp tục bị Git ignore, không là dependency của kiểm thử hoặc script đồng bộ. Không thêm quy tắc theo dõi Git cho thư mục đó. Chạy các lệnh dưới đây từ thư mục backend, trừ lệnh frontend.

## Baseline và lỗi đã tái hiện

Bộ probe cố định 28 câu được chạy bằng router thực trước khi sửa: 9/28 đúng (32,14%). Đây là 28 probe mới theo yêu cầu, không phải tỷ lệ chất lượng của toàn bộ khách hàng. 22 kiểm thử cũ ở baseline đều pass; chúng chưa bao phủ các lỗi mới. Giữ nguyên log baseline.

| Nhóm probe | Trước | Sau |
|---|---:|---:|
| Hỏi cách tạo phiếu | 0/4 | 4/4 |
| Phủ định thao tác ghi | 0/2 | 2/2 |
| Yêu cầu tạo rõ ràng | 0/2 | 2/2 |
| Đăng nhập, viết tắt | 1/3 | 3/3 |
| Hướng dẫn mật khẩu hợp lệ | 1/2 | 2/2 |
| Prompt injection/secret | 2/2 | 2/2 |
| Dữ liệu của chính mình | 1/2 | 2/2 |
| Câu ngắn thiếu chủ đề | 0/3 | 3/3 |
| Câu nối tiếp thiếu lịch sử | 0/2 | 2/2 |
| Chào hỏi | 1/2 | 2/2 |
| Gặp nhân viên | 1/2 | 2/2 |
| Ngoài phạm vi | 2/2 | 2/2 |
| Tổng | 9/28 | 28/28 |

Nguyên nhân đã xác minh:

1. Router cũ chủ yếu khớp regex có dấu; các câu không dấu cùng nghĩa thường rơi OUT_OF_SCOPE.
2. Nhánh tạo phiếu đứng trước nhận diện hướng dẫn/phủ định. Hỏi cách làm có thể vào WRITE_ACTION; câu “tạo giúp tôi phiếu” lại chưa khớp.
3. Rà soát bổ sung tái hiện ba câu “loi tao phieu”, “tao phieu bi loi”, “toi bi loi khi tao phieu” bị coi là WRITE_ACTION. Nhánh failed-operation mới giữ chúng ở hướng dẫn, vẫn giữ yêu cầu “tạo giúp tôi phiếu báo lỗi đăng nhập” ở write có xác nhận; thêm chín kiểm thử regression.
4. Thiếu route hỏi lại và truy xuất nối tiếp có giới hạn. Không đủ thông tin dễ chuyển handoff.
5. Kiểm thử PostgreSQL bổ sung đã bắt lỗi chunk version cũ vẫn được chọn khi version document đổi. SQL trước đó chưa ràng buộc đủ version document/chunk/embedding; cache retrieval cũng chưa xác minh lại eligibility tại DB.
6. Guest trước đó lấy nội dung model trực tiếp; đợt này yêu cầu JSON và nhãn citation thuộc tập bằng chứng backend cung cấp.

Baseline chưa có câu trả lời generation thật hợp lệ để chấm trước/sau. Không suy ra tỷ lệ đúng câu trả lời từ tỷ lệ route.

## Code và giới hạn

- `VietnameseQueryNormalizer`: giữ câu gốc; NFC/lowercase/khoảng trắng như trước, thêm folded view NFD và đ→d chỉ phục vụ matching. Không dùng folded view để tra mã vé, hóa đơn hoặc ID.
- `AssistantIntentRouter`: thứ tự safety → chào/handoff/ngoài phạm vi → thiếu chủ đề → phân biệt hướng dẫn/ghi → dữ liệu cá nhân → knowledge. Alias giới hạn dk/ko/khongg/dc theo ranh giới từ; không sửa mờ biển số hoặc UUID. Không diễn giải k/tt khi thiếu ngữ cảnh.
- `AssistantQuestionResolver`: chỉ thêm tên chủ đề trong allowlist; tối đa ba câu trước. Không sao chép ID hoặc chỉ dẫn trong lịch sử. Khi chủ đề mới không phù hợp, không lùi qua ranh giới write/private/security để phục hồi một chủ đề cũ.
- `AssistantOrchestrator`: lấy tối đa tám tin trước thời điểm input, lọc TEXT/chưa xóa/cùng actor, dùng tối đa ba câu. Hỏi lại không gọi model/retrieval/tool. Quyền ở tool vẫn kiểm tra tại thời điểm thực thi.
- Guest: thêm `contextQuestion` tùy chọn; client chỉ giữ một câu trong bộ nhớ, xóa khi đăng nhập hoặc reload. Backend chỉ lấy tên chủ đề, không tin scope/permission từ client. Guest luôn PUBLIC, không có business tool hoặc tự tạo phiếu.
- Guest generation: schema `responseText` + `citations`; nhãn lạ, JSON sai, response rỗng hoặc function call đều không được trả như câu trả lời grounded. Provider lỗi trả lỗi dịch vụ; thiếu knowledge trả chẩn đoán retrieval khác biệt.
- Lexical query bỏ các cụm hỏi chung và mở rộng alias giới hạn; vector/query audit vẫn giữ toàn bộ câu. Phủ định, trạng thái, số tiền và định danh được giữ. Policy cache/audit chuyển retrieval-policy-v2, không đổi threshold hoặc topK.
- Retrieval SQL: kiểm tra scope/tenant ở source, document và chunk; thời hạn; version document/chunk/embedding; source ACTIVE và document/chunk READY. Cache hit xác minh lại IDs bằng truy vấn eligibility hiện thời; sai hoặc thu hồi thì bỏ hit và retrieval lại. Không đổi threshold/topK để làm xanh kiểm thử.
- Đọc citation yêu cầu membership và eligibility DB theo permission hiện thời; endpoint chưa có tenant binding hiện thời nên chỉ trả citation global, không dùng tenant do client cung cấp.
- `AiToolCallUseCaseImpl` giữ nguyên nghiệp vụ; thêm kiểm thử confirm, deny, expiry, ownership, permission revocation, frozen arguments và retry cùng idempotency key. Chưa có tool hồ sơ, continue chat hoặc escalation chỉ vì dataset cũ từng nêu.

CLARIFICATION là enum nội bộ, không có migration DB enum. API guest cũ bốn tham số service vẫn hoạt động; field request mới tùy chọn. Không thêm LLM rewrite hoặc cơ chế polling danh mục liên tục.

## Bốn tài liệu nghiệp vụ

| Tài liệu | Thay đổi | Scope | Trạng thái |
|---|---|---|---|
| authentication-and-profile.md | Đối chiếu Keycloak/UI/hồ sơ, phân biệt hướng dẫn và tool chưa có | PUBLIC | DRAFT 2.0.0 |
| chatbot-privacy-and-prohibited-data.md | Phân biệt câu giáo dục với tiết lộ secret, guest PUBLIC, OWN và dữ liệu cấm | PUBLIC | DRAFT 2.0.0 |
| chat-conversations-and-support-boundaries.md | Mới; tách chat thường, trợ lý và hỗ trợ hệ thống; quyền menu/icon | PUBLIC | DRAFT 2.0.0 |
| support-ticket-and-escalation.md | Trạng thái, assignment, auto-close, trao đổi tiếp và khoảng cách escalation | CUSTOMER | DRAFT 2.0.0 |

Chín tài liệu nghiệp vụ còn lại giữ nội dung và bản export. Tất cả bốn tài liệu mới/chỉnh đều lastVerifiedAt=2026-11-19, không tự APPROVED. Không hạ scope CUSTOMER để nhân sự đối tác tìm thấy tài liệu. Hướng dẫn PUBLIC về loại chat nằm ở tài liệu mới; knowledge riêng cho nhân sự nội bộ thuộc backlog.

`CUSTOMER_DIRECT` vẫn là chat khách–đối tác, không tạo/gắn/chia sẻ ticket vào chat này. Requester gửi hỗ trợ của chính mình tới hệ thống qua ASSISTANT_SUPPORT hoặc SUPPORT_REQUESTER_DIRECT theo quyền. Nhãn system_admin không thay permission, assignment và ownership.

## Dataset, rubric và truy vết

- [Ma trận thực tế](batch-01-traceability.md): capability → actor/quyền → UI → API → rule → DB → tool → tài liệu → test.
- [Dataset](batch-01-questions.jsonl): 240 câu, 40 tình huống, sáu biến thể mỗi tình huống; 60 câu cho mỗi tài liệu.
- [Tình huống nguồn](batch-01-scenarios.json), [rubric 0–10](batch-01-rubric.md).
- Regression 192 và acceptance 48 tách theo scenarioId, không đưa paraphrase cùng tình huống sang hai tập. Các tập đã được chạy trong quá trình sửa; đây không phải đánh giá blind hoặc chứng minh khả năng tổng quát trên câu chưa từng thấy.
- [Rà soát legacy120](legacy-120-review.json): giữ nguyên 120 dòng gốc; 29 câu thuộc đợt có kỳ vọng route thô kiểm thử được; một câu AUTH-009 cần tách context thay vì gộp hai lượt trong một question; 90 ca ngoài đợt được giữ và đánh dấu chưa cập nhật.
- Các kỳ vọng lỗi thời được ghi riêng: câu hỏi riêng tư mang tính giáo dục không phải REFUSE mặc định; thông tin người xử lý phiếu của tôi cần dữ liệu hiện thời; hồ sơ/continue/escalation chưa có tool thực tế.

## Kết quả thực chạy

<!-- VERIFIED_RESULTS -->
| Phép đo | Kết quả | Diễn giải |
|---|---:|---|
| Probe trước/sau | 9/28 → 28/28 | Router thực, cùng 28 input |
| Route đợt 1 | 240/240 (100%) | Mỗi tài liệu 60/60; regression 192/192, acceptance 48/48 |
| Có dấu/không dấu theo nhãn dataset | 40/40 và 40/40 | Chênh lệch 0 điểm phần trăm trên cặp tập này |
| Legacy đã review trong đợt | 29/29 | Một ca đa lượt cần cấu trúc lại; 90 ca ngoài đợt chưa cập nhật |
| Live retrieval runtime chunker/embedding/SQL | 168/168 top 5 | Authentication 54/54, privacy 24/24, chat 42/42, support 48/48; acceptance 24/24 |
| Đủ bằng chứng trước/sau lexical | 111/168 → 143/168 | 66,07% → 85,12%, không đổi threshold/topK; acceptance sau: 18/24 |
| Còn dưới ngưỡng bằng chứng | 25/168 | Auth 11, privacy 2, chat 1, support 11; [danh sách cần review](evidence/batch-01/remaining-low-evidence.json) |
| Latency live retrieval, cache tắt | median 445 ms; p95 mẫu 499 ms; max 711 ms | 168 mẫu gồm query embedding + SQL/audit, nhịp chờ 900 ms ngoài thời gian đo; không phải SLO production |
| Generation thật | Trước 0/8; sau fallback 8/8 | Lượt bổ sung dùng 3.1-flash-lite và 3.5-flash-lite; [transcript/review](model-fallback-verification.vi.md). Mẫu tám câu không đại diện toàn bộ tập |
| Backend hồi quy cuối | 2.539 pass; 2 opt-in live skip | Tổng 2.541, 0 failure/error. Hai live test đã được chạy riêng và báo kết quả riêng, không bị tính vào pass mặc định |
| Browser | 40/40 | Guest, signed-in guard, mobile, hỏi tiếp, dropdown và chat boundary; API fixture |
| Frontend lint, static regression, build | PASS | Log gốc kèm bên dưới |
| Artifact, metadata, checksum, local links | PASS | 13 knowledge, 4 trong batch, 360 dòng câu hỏi; source/import đồng bộ |
<!-- /VERIFIED_RESULTS -->

Phân loại bằng chứng:

- Unit/router/orchestrator/tool: dùng mock cho I/O và provider để kiểm tra guard, hợp đồng và lỗi. 240 câu route không phải 240 câu trả lời model.
- PostgreSQL/Flyway/pgvector: Testcontainers riêng `vehicle_management_test`; không rơi về DB dev. Kiểm tra accent/unaccent, scope, expiry, inactive source, stale version/embedding và cache reauthorization. Ca thông thường dùng vector tổng hợp có chủ đích.
- Live retrieval đầy đủ: dùng Markdown extractor + runtime chunker, Gemini embedding thật và SQL hybrid thật trên đúng bốn tài liệu trong corpus tách biệt; cache tắt, topK=5. Không upload/publish qua MinIO. Không đồng nhất kết quả corpus nhỏ này với độ chính xác trên corpus production lớn.
- Cache cold/hot quan sát được 27 ms / 18 ms: cache port là test double trong bộ nhớ, eligibility/ranking/audit dùng PostgreSQL thật, embedding tổng hợp. Một mẫu mỗi trạng thái chỉ chứng minh nhánh thực thi, không đủ kết luận p95/SLO hoặc hiệu năng Redis thật.
- Browser: API/provider được route bằng fixture; kiểm tra giao diện guest, signed-in guard, mobile, hỏi tiếp, reset context, dropdown và menu support. Không phải E2E với Gemini.
- Live generation: tám probe dùng nội dung extractor chia hai phần mỗi tài liệu để tách thử provider khỏi runtime chunker; đánh giá 168 câu riêng dùng runtime chunker thật. Đã gọi adapter Gemini thật tám câu; retrieval của cả tám có nguồn và evidenceSufficient=true, nhưng generation trả TIMEOUT/HTTP_5XX, không có đáp án hợp lệ để chấm. Probe tối giản cũng trả 503 high demand hoặc timeout. `gemini-2.5-flash` trả 404 báo model không còn cho tài khoản này; model thay thế được provider liệt kê và thử là `gemini-3.8-flash`. Chỉ cấu hình fixture thay đổi, không tự đổi model DB/runtime.

Citation gate syntactic được kiểm tra bằng unit (nhãn C99 bị chặn) và membership HTTP/unit. Lượt bổ sung có tám response thật được review đạt rubric; tám mẫu không đủ nghiệm thu semantic citation/độ đủ ý >=95% trên toàn tập. Không dùng response mock để bù số liệu live.

## Ví dụ đường đi

| Input/context | Route | Nguồn/tool | Kết quả đã xác minh |
|---|---|---|---|
| tao phieu ho tro nhu the nao | STATIC_KNOWLEDGE | Tài liệu hướng dẫn đúng scope | Không tạo action; generation live chưa có đáp án hợp lệ |
| tao giup toi phieu bao loi dang nhap | WRITE_ACTION | create_support_ticket qua flow xác nhận | Worker không trực tiếp chạy write; confirm mới thực thi, retry không chạy lần hai |
| dung tao phieu chi huong dan | STATIC_KNOWLEDGE | Knowledge | Không tạo ticket/action từ phủ định |
| loi, không có lịch sử | CLARIFICATION | Không retrieval/model/tool | “Bạn đang gặp vấn đề ở chức năng nào và đã thực hiện đến bước nào?” kèm ví dụ |
| “Làm sao đăng nhập?” → “van loi” | STATIC_KNOWLEDGE | Query “Về đăng nhập. van loi”, PUBLIC nếu guest | Chỉ giữ tên chủ đề; không sao chép ID hoặc instructions |
| “Tạo phiếu giúp tôi” → “vẫn lỗi” | CLARIFICATION | Không dùng write context như hướng dẫn | Hỏi lại đúng chủ đề, không tự tiếp tục tạo phiếu |
| Guest: xem phieu cua toi | PERSONAL_DATA | Không tool | Yêu cầu đăng nhập; không đọc phiếu riêng |
| Cho tôi nội dung phiếu của khách khác | SECURITY_REFUSAL | Không tool/retrieval | Từ chối an toàn |
| Có nên gửi OTP vào chatbot không? | STATIC_KNOWLEDGE | Tài liệu privacy PUBLIC | Câu giáo dục hợp lệ; không coi mọi chữ OTP là secret |
| du doan bitcoin ngay mai | OUT_OF_SCOPE | Không tool/model/handoff | Nêu phạm vi CoParking, không mời tạo ticket không cần thiết |

## Đọc kết quả có lỗi trung gian

`verification-policy-v2.log` ghi live retrieval PASS nhưng có một unit test lệch khoảng trắng trước dấu hỏi trong lexical query. Đã sửa chuẩn hóa khoảng trắng và chạy lại toàn bộ trong `final-regression.log`: 2.539 pass, 2 skip có chủ đích, không failure/error. Sửa dấu câu này không thay đổi lexeme PostgreSQL; chưa gọi lại 168 API chỉ để thay dấu cách. Nhánh lỗi tạo phiếu bổ sung cũng đã qua chín kiểm thử trong lượt cuối. `postgres-live-final.log` vẫn FAIL do generation thật; giữ nguyên bằng chứng, không xóa hoặc đổi trạng thái thành pass.

## Lệnh tái lập

```powershell
python scripts/sync_ai_knowledge.py
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/ai-knowledge/phase-0/authoring/evaluation/validate-phase0.ps1
.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2-local" "-DargLine=-Xmx1024m" "-Dtest=*Test,Phase1KnowledgeRetrievalIT,PendingCustomerSupportFlowIT,PublicSupportSecurityIT,SupportCategoryCatalogIT" test
# Chỉ bật khi có API key qua môi trường an toàn; không ghi key vào lệnh/báo cáo.
$env:PHASE1_LIVE_RETRIEVAL='true'
.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2-local" "-Dtest=Phase1KnowledgeRetrievalIT#liveRetrievalEvaluatesEveryStaticCaseUsingRuntimeChunker" test
$env:PHASE1_LIVE_EVAL='true'
.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2-local" "-Dtest=Phase1KnowledgeRetrievalIT#liveEmbeddingAndGenerationAreMeasuredOnIsolatedCorpus" test
```

Frontend tại `vehicle-management-frontend/vehicle_management_react`:

```powershell
npm run lint
npm test
npm run build
npx playwright test tests/guest-support.behavior.spec.ts tests/support-flows.behavior.spec.ts tests/support-categories.behavior.spec.ts tests/shared-select.behavior.spec.ts --timeout=60000
```

Log gốc nằm ở backend `target/chatbox-phase1/` (ignored): `baseline-tests.log`, `backend-regression-final.log`, `acceptance-verified.log`, `retrieval-paced.log`, `verification-policy-v2.log`, `final-regression.log`, `browser-final.log`, `frontend-lint.log`, `frontend-regression.log`, `frontend-build.log`, `postgres-live-final.log`, `artifact-validation-final.log`. [Tóm tắt log và checksum](evidence/batch-01/test-log-summary.json), [route](evidence/batch-01/routing-summary.json), [retrieval](evidence/batch-01/retrieval-summary.json) và [transcript lỗi provider thật](evidence/batch-01/live-transcript.json) được lưu cùng báo cáo; không phụ thuộc log ignored để đọc các chỉ số chính.

## DB dev và phần chưa làm

Inventory chỉ đọc của DB `vehicle-pgvector` ghi nhận 3 source, 28 document, 24 index version, 49 chunk, 124 embedding, 0 citation và 9 retrieval audit tại thời điểm chụp. Đây là số row DB, không phải 28 tài liệu đã phê duyệt. File inventory chỉ gồm IDs/scope/status/checksum/object key và dependency FK, không chép nội dung chat, secret hoặc PII.

[Runbook reset dev](batch-01-dev-reset-runbook.md) nêu backup/restore, dependency citation/audit, dừng worker, allowlist ID, targeted cache eviction và MinIO. Đợt này không có SQL purge/migration phá hủy, xóa bucket, FLUSHALL, reset Flyway hoặc xóa chat/ticket. Corpus đang chạy chưa được thay bằng DRAFT mới.

25 ca dưới ngưỡng chưa được gọi là “handoff sai” chỉ từ hit document: phải đối chiếu đúng đoạn và đủ ý trước. Không giảm ngưỡng hoặc ép model trả lời để đạt số liệu. Một số ca là followup thiếu bước, phủ định thao tác ghi, trạng thái RESOLVED/CLOSED và hướng dẫn profile; yêu cầu mức trả lời/hỏi lại đúng chi tiết cần được kiểm bằng provider thật sau khi lỗi dịch vụ hết. Chưa đạt nghiệm thu toàn bộ mục đúng/đủ ý >=95% và semantic citation; đây là giới hạn bàn giao, không phải PASS ngầm.

Backlog:

1. Tám probe đã chạy lại thành công bằng fallback. Tiếp tục chạy toàn bộ acceptance, chấm đúng/đủ ý và semantic citation theo rubric; bổ sung câu hỏi UAT độc lập với bộ đã xem.
2. Đo latency theo bước trên provider ổn định, cache lạnh/nóng Redis thật và mẫu đủ lớn; hiện không có p95 generation thành công.
3. Đợt 2 bổ sung knowledge cho quyền requester nhân sự đối tác và nghiệp vụ nội bộ theo scope; không tái sử dụng scope CUSTOMER sai actor.
4. Cần quyết định/triển khai riêng nếu muốn tool hồ sơ, tiếp tục chat hoặc escalation. Không sửa chính sách phê duyệt cũ chỉ để khớp code.
5. Chỉ review/publish/reimport sau khi người có quyền duyệt bốn DRAFT; dùng manifest và runbook, không sửa lịch sử migration đã chạy.



## Kiểm chứng bổ sung về model

Các nhận định provider lỗi ở trên ghi lại lượt test cố định một model ban đầu. [Báo cáo fallback 19/11](model-fallback-verification.vi.md) xác nhận bốn model có JSON response thật, 8/8 câu hỏi có nguồn trả lời được sau chuyển model và 74 kiểm thử backend/17 browser guest qua. Log lỗi ban đầu được giữ nguyên để truy vết.

## Kết quả tích lũy sau đợt 2

Các số ở trên là mốc kiểm chứng lịch sử trên bốn tài liệu của đợt 1. Đợt 2 đã bổ sung bốn tài liệu, sửa FAQ, câu hỏi tiếp nối, dữ liệu chính chủ và tốc độ. Xem [báo cáo tích lũy](batch-02-report.md) để biết kết quả trên corpus tám tài liệu; không coi hai phép đo khác corpus là một thí nghiệm A/B giữ nguyên điều kiện.


## Rà soát lại cùng Qwen — 24/11/2026

Đã cập nhật bốn nguồn của đợt này, bản xuất và checksum trong lượt rà soát 16 tài liệu. Giữ kết quả ở trên làm lịch sử. Kiểm tra cấu trúc mới đã qua; Java/SQL/FE/Qwen live của bản mới còn chờ RAM và chưa nghiệm thu đúng/đủ ý. Xem [báo cáo tổng hợp](../../../ALL_BATCHES_QWEN_REVIEW_REPORT.vi.md) để biết thay đổi, phiên bản, trạng thái từng tài liệu và bằng chứng thực chạy. Không kế thừa điểm của corpus cũ làm điểm của bản mới; Google chưa kiểm chứng trong lượt này.
