# Kiểm chứng chuyển model dự phòng — 19/11/2026

Đã xác nhận cùng API key Google AI Studio có model hoạt động bình thường khi model khác lỗi. Lượt đánh giá ban đầu cố định `gemini-3.8-flash`; kết quả lỗi của model đó không đủ để kết luận toàn bộ provider không sử dụng được. Kiểm chứng bổ sung đã thử các FALLBACK hiện có và chạy lại generation thật với danh sách dự phòng.

## Kết quả với API key hiện tại

| Model trong cấu hình hiện hữu | Probe JSON thật | Thời gian probe |
|---|---|---:|
| gemini-3.1-flash-lite | HTTP 200, JSON hợp lệ | 1.383 ms |
| gemini-3.5-flash-lite | HTTP 200, JSON hợp lệ | 7.289 ms |
| gemini-3.5-flash | HTTP 200, JSON hợp lệ | 5.241 ms |
| gemini-3-flash-preview | HTTP 200, JSON hợp lệ | 4.410 ms |
| gemini-3.7-flash | HTTP 503, UNAVAILABLE/high demand | 1.771 ms |
| gemini-3.8-flash | HTTP 503, UNAVAILABLE/high demand | 3.640 ms |

Đây là số đo tại thời điểm chạy, không là cam kết model luôn sẵn sàng. Đã lấy danh sách model từ API và thử `generateContent` với schema thật; tên xuất hiện trong models.list không tự bảo đảm model nhận request thành công.

DB hiện có một ACTIVE `gemini-3.6-flash` và tám FALLBACK cho SUPPORT_CHAT. Hai fallback ưu tiên đầu là `gemini-3.1-flash-lite` và `gemini-3.5-flash-lite`. Không sửa DB dev hoặc tự bật model mới. Không thay model embedding: đổi generation không yêu cầu reindex; đổi embedding phải giữ dimension/prompt/index tương thích.

## Cơ chế đã kiểm tra và sửa

- Authenticated chat đã có model router, danh sách ACTIVE/FALLBACK, giới hạn theo attempt và circuit breaker. Tiếp tục sử dụng cơ chế này; không tạo router thứ hai hoặc tự nạp mọi model API liệt kê.
- Guest trước đây duyệt ứng viên nhưng thiếu giới hạn/circuit breaker và chuyển tiếp cả một số lỗi không phù hợp. Nay dùng cùng AiModelRouter, AiModelPolicyService, AiModelCircuitBreakerPortOut và cache key của SUPPORT_CHAT.
- Mỗi lượt guest tối đa `app.ai.cache.max-models-per-job-attempt` lần gọi, mặc định ba. Bỏ qua circuit đang mở; dừng ngay khi có câu trả lời hợp lệ; không gửi cùng câu hỏi đồng loạt tới mọi model.
- TIMEOUT, HTTP_408, HTTP_429, HTTP_5XX và lỗi kết nối có thể dùng ứng viên tiếp theo. Bổ sung alias `PROVIDER_ERROR` vì adapter đang trả mã này, trong khi policy cũ chỉ nêu `PROVIDER_EXCEPTION`.
- MODEL_NOT_FOUND có thể dùng fallback đã cấu hình; circuit tạm bỏ qua cấu hình lỗi. Không sửa migration cũ hoặc tự đổi model thành DISABLED trong DB.
- HTTP_400, HTTP_401/403, key thiếu và SAFETY_BLOCKED không chạy tiếp qua model khác. Candidate finishReason SAFETY/BLOCKLIST/PROHIBITED_CONTENT/SPII/RECITATION và các safety image tương ứng được map thành safety blocked; không nhầm thành response rỗng để thử model khác.
- JSON sai, citation không thuộc tập backend cung cấp, response rỗng hoặc function call trong guest không được trả như câu trả lời grounded; có thể thử fallback trong cùng giới hạn. Guest không có tool ghi/đọc riêng tư ở bất kỳ model nào.
- Model được router cho phép vẫn phải qua data-mode/freeTierApproved hiện có; không chọn model dựa trên tên role hoặc bỏ qua phê duyệt dữ liệu.
- Thời gian chờ request guest phía trình duyệt tăng từ 30 lên 90 giây để không hủy giữa cửa sổ dự phòng mặc định. Hỏi lại, chào hỏi, từ chối và thiếu topic vẫn không gọi model. 90 giây là giới hạn chờ, không phải độ trễ thường xuyên được kỳ vọng.

Theo [Google troubleshooting](https://aistudio.google.com/docs/troubleshooting), retry nên giới hạn ở lỗi tạm thời như 408/429/5xx và có giới hạn lần thử. [Rate limits](https://aistudio.google.com/docs/rate-limits) áp dụng theo project, thay đổi theo model và usage tier; chuyển model không bảo đảm vượt được hạn mức chung hoặc tình trạng toàn dịch vụ. Không đổi API key để né quota. [Models API](https://aistudio.google.com/api-reference/models) được dùng để xác định phương thức generation được hỗ trợ; [GenerateContent](https://aistudio.google.com/api-reference/generate-content) mô tả safety ở prompt/candidate.

## Generation thật sau khi có fallback

Chạy tám câu có dấu/không dấu về quên mật khẩu, OTP, chat đối tác và dữ liệu cần nhập để tạo phiếu. Retrieval dùng PostgreSQL/Flyway/pgvector tách biệt, embedding Gemini thật và bốn tài liệu đợt 1. Fixture này dùng nội dung extractor chia hai phần mỗi tài liệu, như lượt generation ban đầu; đánh giá 168 câu bằng runtime chunker là phép thử riêng đã báo cáo trước.

- Trước: 0/8 generation thành công khi cố định model lỗi.
- Sau: 8/8 generation thành công, 8/8 label citation thuộc tập bằng chứng đúng scope của fixture.
- Cả tám câu thử model chính 3.6 trước rồi phục hồi. Bảy câu thành công bằng 3.1-flash-lite; một câu phải dùng 3.5-flash-lite sau khi cả hai model trước timeout. Tổng 17 request generation, tối đa ba/câu.
- Độ trễ toàn lượt trong mẫu: nhỏ nhất 3.422 ms, median 9.117,5 ms, lớn nhất 45.667 ms. Request fallback thành công mất khoảng 1,3–5,2 giây, nhưng chờ model lỗi trước đó vẫn làm tổng lượt chậm. Circuit breaker giúp các request tiếp theo bỏ qua model lỗi liên tiếp; harness generation cố ý gọi model chính mỗi câu để đo phục hồi, không ghi trạng thái circuit vào Redis dev.
- Đánh giá định tính bởi trợ lý trên response và đúng đoạn được cite: 8/8 đạt rubric >=9/10, không có claim đã tạo phiếu, không gửi phiếu vào chat đối tác và không xin OTP. Đây không phải UAT độc lập và không chứng minh độ đúng 95% trên 240 câu hay trên mọi ngữ cảnh.

[Transcript thật](evidence/model-fallback-20261119/live-transcript.json) lưu cả từng attempt, model, lỗi, latency, response và bằng chứng. [Review rubric](evidence/model-fallback-20261119/semantic-review.json), [tổng hợp](evidence/model-fallback-20261119/summary.json), [probe sáu model](evidence/model-fallback-20261119/structured-probe-results.json).

## Kiểm thử và đường dẫn code

- 74 kiểm thử backend PASS: policy/router/circuit, orchestrator, guest service, Gemini mapper, API security và một live PostgreSQL + generation test; không skip trong lượt này.
- 17/17 browser guest PASS; lint/build PASS. Lượt browser khởi động dev server ban đầu timeout, chạy tách sau đã qua.
- Dùng mock cho trường hợp 429/5xx/timeout/401/safety và kiểm tra giới hạn lần gọi. Live test dùng adapter Gemini thật và PostgreSQL thật, có quan sát chuyển model thành công; không phải toàn bộ UI E2E với provider.
- Không tạo action hoặc ticket trong các bài kiểm chứng model; mọi hành động ghi vẫn phải qua permission, ownership, xác nhận và idempotency hiện có.

Code thay đổi:

- `src/main/java/com/ban/vehicle_management/application/operations/guestsupport/service/GuestPublicAssistantService.java`
- `src/main/java/com/ban/vehicle_management/application/ai/service/AiModelPolicyService.java`
- `src/main/java/com/ban/vehicle_management/infrastructure/ai/gemini/GeminiResponseMapper.java`
- Frontend `src/features/support/api/guestSupportApi.ts`
- Test guest/policy/mapper và `Phase1KnowledgeRetrievalIT` hỗ trợ `PHASE1_LIVE_MODELS` (tối đa ba model) để không cố định một model trong lần đánh giá sau.

Lệnh backend, API key lấy từ môi trường an toàn:

```powershell
$env:PHASE1_LIVE_EVAL='true'
$env:PHASE1_LIVE_MODELS='gemini-3.6-flash,gemini-3.1-flash-lite,gemini-3.5-flash-lite'
.\mvnw.cmd -B -ntp "-Dmaven.repo.local=.m2-local" "-DargLine=-Xmx1024m" "-Dtest=Phase1KnowledgeRetrievalIT#liveEmbeddingAndGenerationAreMeasuredOnIsolatedCorpus,GuestPublicAssistantServiceTest,AiModel*Test,GeminiResponseMapperTest,AssistantOrchestrator*Test,PublicSupportSecurityIT" test
```

Log gốc trong backend `target/chatbox-phase1/model-fallback/`: `live-and-unit-tests.log`, `frontend-lint.log`, `frontend-build.log`, `guest-browser-final.log`. [Tóm tắt log/checksum](evidence/model-fallback-20261119/test-log-summary.json). Bộ evidence không lưu API key hoặc dữ liệu tài khoản người dùng.

## Giới hạn còn lại

25/168 câu dưới ngưỡng bằng chứng của đợt 1 chưa được giải quyết chỉ bằng đổi model: retrieval gate vẫn phải quyết định đủ nguồn trước generation. Chưa chạy generation/rubric trên toàn bộ tập acceptance 48 câu và chưa nghiệm thu độ đúng toàn hệ thống. Không tự publish bốn DRAFT, không purge hoặc thay corpus dev; tài liệu tiếp tục nằm trong docs AI của backend.
