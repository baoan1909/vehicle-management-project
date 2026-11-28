# Hợp đồng phản hồi trợ lý

> Cập nhật đợt 1 — 19/11/2026: xem [bằng chứng/truy vết](evaluation/batch-01-traceability.md), [manifest](knowledge-manifest.json) và [báo cáo đợt 1](evaluation/batch-01-report.md). Baseline 14/09/2026 được giữ để truy vết; các nhận định runtime có delta dưới đây thay thế nhận định cũ tương ứng. Chưa tự phê duyệt/publish knowledge.


## Envelope chuẩn

```json
{
  "responseText": "Nội dung trả lời bằng tiếng Việt.",
  "citations": [],
  "actionCards": [],
  "confidence": 0.0,
  "handoffRecommended": false
}
```

Backend tạo và xác minh envelope cuối; không deserialize rồi tin toàn bộ dữ liệu do model tự khai báo.

## JSON Schema rút gọn

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["responseText", "citations", "actionCards", "confidence", "handoffRecommended"],
  "properties": {
    "responseText": { "type": "string", "minLength": 1, "maxLength": 4000 },
    "citations": { "type": "array", "maxItems": 8 },
    "actionCards": { "type": "array", "maxItems": 5 },
    "confidence": { "type": "number", "minimum": 0, "maximum": 1 },
    "handoffRecommended": { "type": "boolean" }
  }
}
```

## `responseText`

- Tiếng Việt có dấu, plain text/Markdown subset an toàn; không chứa HTML thô/script.
- Tối đa 4.000 ký tự sau sanitize; không echo secret, token, OTP, số thẻ đầy đủ hay PII không cần thiết.
- Không tuyên bố write thành công trước tool result `SUCCEEDED`.
- Khi không có evidence: nói rõ chưa đủ thông tin, không tự bịa.

## `citations`

```json
{
  "documentId": "uuid",
  "chunkId": "uuid",
  "title": "Tiêu đề nguồn",
  "sourcePage": 1,
  "sourceSection": "Mục",
  "retrievalScore": 0.82
}
```

- Tối đa 8 citation, không trùng `chunkId`.
- `documentId/chunkId/title/page/section/score` lấy từ retrieval result allowlist và DB, không lấy ID do model sinh.
- Citation chỉ hợp lệ khi source/document/chunk còn active/READY và actor được scope/tenant tại thời điểm trả lời.
- `retrievalScore` chuẩn hóa 0–1 bởi Backend; null page/section được phép nếu nguồn không có.
- RAG answer không có citation hợp lệ phải bị hạ xuống handoff.

## `actionCards`

```json
{
  "toolCallId": "uuid",
  "actionType": "CREATE_SUPPORT_TICKET",
  "title": "Tạo phiếu hỗ trợ",
  "description": "Kiểm tra nội dung trước khi xác nhận.",
  "confirmationRequired": true,
  "status": "AWAITING_CONFIRMATION",
  "expiresAt": "2026-09-14T12:00:00Z"
}
```

- `toolCallId` do Backend persist trước khi gửi FE.
- `actionType` nằm trong allowlist map tới một tool/version cụ thể.
- WRITE luôn `confirmationRequired=true`; trạng thái: `AWAITING_CONFIRMATION`, `CONFIRMED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `DENIED`, `EXPIRED`.
- FE không tự đổi trạng thái và không gửi arbitrary tool name/payload khi confirm.
- `expiresAt` ISO-8601 UTC; Backend kiểm tra expiry/replay.

## `confidence`

Không dùng confidence do LLM tự khai báo. Backend tính score đã hiệu chỉnh:

- RAG: chất lượng retrieval, coverage citation, freshness, contradiction check.
- Tool: authorization/validation pass và độ đầy đủ result.
- Validation: schema/output safety pass.
- Có conflict/tool fail/không citation cho RAG thì cap thấp và handoff.

TBD-017 đã phê duyệt cách dùng ngưỡng: baseline ban đầu `>=0.80` trả lời; `0.55–0.79` trả lời có cảnh báo/handoff option; `<0.55` không trả lời nghiệp vụ, handoff. Threshold phải được hiệu chỉnh/version hóa bằng golden set trước production. Contradiction, stale source hoặc RAG không có citation luôn handoff. Release gate: citation correctness ≥98%, grounded precision ≥95%, PII/cross-account leakage = 0 và high-risk unsupported answer = 0. Đây không phải SLA hay độ chính xác đã cam kết trước khi eval đạt gate.

## `handoffRecommended`

`true` khi: không có tài liệu đủ; tài liệu mâu thuẫn/stale; tool fail/timeout/forbidden; user yêu cầu người thật; refund/khiếu nại/rủi ro an toàn; ngoài phạm vi; nghi ngờ lộ secret; confidence dưới ngưỡng.

## HTTP và lỗi

- 200 khi envelope đã tạo, kể cả phản hồi handoff an toàn.
- 202 cho job async/action đang chờ.
- 400 input/schema; 401 chưa auth; 403 thiếu quyền; 404 resource OWN không thấy; 409 state/idempotency conflict; 429 rate limit; 500 lỗi bất ngờ.
- Client chỉ nhận mã lỗi công khai và message Việt hóa; provider payload/stack trace lưu audit đã redact.

## Tương thích hiện trạng

Hiện `AssistantOrchestrator` yêu cầu/parse chủ yếu `{responseText}` và tool API dùng DTO riêng. Vì vậy contract này là target contract cho Phase 1+, chưa được đánh dấu runtime-compatible trong Phase 0.

## Tương thích đợt 1

Không thêm trạng thái persisted hoặc bắt client cũ đọc field mới. Authenticated clarification là message ASSISTANT_TEXT như trước. Guest giữ answer/citations/handoffRecommended/diagnosticCode; bổ sung request contextQuestion tùy chọn, tối đa 500 ký tự. Server chỉ trích public topic, không nhận scope/ownership. CLARIFICATION_REQUIRED không có citation/action/handoff đề nghị tạo phiếu tự động. JSON generation guest chỉ được trả sau khi responseText không rỗng và mọi nhãn citation nằm trong allowlist bằng chứng.


### Citation hiện thời — đợt 1

Đọc lại citation không chỉ kiểm tra thành viên hội thoại: backend kiểm tra permission hiện thời, source/document/chunk và version. Nguồn đã thu hồi hoặc version không còn hợp lệ bị lọc. Endpoint hiện chưa có tenant binding hiện thời nên chỉ trả citation global. Cache retrieval cũng xác minh lại tập chunk tại DB trước khi dùng hit; lỗi mapping/eligibility rơi về retrieval bình thường, không trả null.


### Model fallback — kiểm chứng bổ sung 19/11

Guest sử dụng danh sách SUPPORT_CHAT có sẵn, policy lỗi và circuit breaker giống hạ tầng authenticated, tối đa ba lần gọi mặc định. 408/429/5xx/timeout/network có thể dùng model kế tiếp; auth/key/input/safety dừng. Mọi response vẫn phải qua schema/citation guard. UI guest chờ tối đa 90 giây cho cửa sổ mặc định. Xem [báo cáo kiểm chứng](evaluation/model-fallback-verification.vi.md); không tự chuyển sang model embedding khác hoặc model chưa được phép.

### Gemini và dẫn chứng — thay đổi 20/11

Đối với Gemini, planner/pool mới thay thế mô tả ba lần gọi ở trên bằng deadline và số attempt có cấu hình. Lỗi xác thực chặn credential lỗi và có thể dùng credential tiếp theo; lỗi request/safety không quét các key. Duyệt model được registry cho phép trên key hiện tại trước khi chuyển key. [Runbook](../../GEMINI_CREDENTIAL_FAILOVER.vi.md) mô tả quota, pacing và các giới hạn kiểm chứng.

Guest và tài khoản cùng kiểm tra output, nguồn và citation. Dẫn chứng được chọn theo câu hỏi trong ngân sách cũ, giữ nguyên tiền, thời điểm và phủ định. Không bỏ giới hạn quan trọng như chưa giữ lượt, chưa bảo đảm chỗ hoặc chưa có chức năng hoàn tiền. “Chưa bảo đảm có chỗ đã được giữ” là cảnh báo, không phải tuyên bố đã giữ chỗ; một tuyên bố mới sau dấu phẩy vẫn phải qua guard. Hỏi nối tiếp chưa rõ bước cần hỏi bước hiện tại/thông báo lỗi; phân biệt biểu mẫu với thẻ xác nhận trong chat.


## Delta kỹ thuật đợt 4 — 23/11/2026 (DRAFT)

- Giữ nguyên envelope, kiểm tra scope/citation, giới hạn bằng chứng 3.000 token ước lượng, tối đa năm chunk và 1.500 ký tự/chunk. Không giảm confidence threshold hoặc tăng ngân sách để lấy điểm.
- `vi-lexical-v5` chỉ thay đổi chuỗi dùng tìm kiếm; giữ câu gốc cho model/audit, số tiền, ngày, mã định danh, phủ định và từ đồng âm như hiển thị, va chạm.
- `query-snippet-v5` chọn câu hỏi/đáp án trọn ý, phân biệt xe vào/ra và lập/hoàn tất phiếu mất thẻ, kể cả chunk ngắn. Không sửa số, nối câu khác luồng hoặc dùng bảng đáp án theo mã test.
- `plain-vietnamese-v5`, `contextual-response-v8` giữ điều kiện bước đang hỏi và hướng dẫn nơi mở tính năng khi người dùng hỏi “cái đó ở đâu”. `output-validation-v4` phân biệt lời khẳng định thành công với câu phủ định bằng chứng; các ca khẳng định tài chính sai vẫn bị chặn.
- Lịch sử dùng để nối chủ đề phải cùng người hỏi, cùng chat, trước tin hiện tại, không bị xóa. Chỉ giữ nhãn chủ đề công khai hữu hạn, không sao chép mã riêng, vị trí hoặc lệnh viết của lượt trước.
- Đầu vào và kết quả dữ liệu riêng vẫn che dữ liệu cá nhân. Riêng đầu ra grounded có thể giữ liên hệ công khai thuộc `app.ai.public-contact-values` **và** xuất hiện nguyên giá trị trong nguồn đã trích dẫn. Không có cấu hình/nguồn thì che. Token/mật khẩu và dữ liệu thẻ thanh toán được che trước ngoại lệ; không cho phép khớp một phần email hoặc chuỗi số dài. FAQ, generation, guest và cache đều áp dụng kiểm tra tương ứng.
- Hướng dẫn mất thẻ công khai chỉ giải thích điều kiện và nhờ nhân viên bãi xử lý; không cấp quyền nhân viên, tự đóng lượt gửi, ghi nhận xe vào/ra hay điều khiển cổng.

Kết quả chạy và phạm vi nghiệm thu nằm trong [báo cáo đợt 4](evaluation/batch-04-report.md), không suy từ việc code compile sang chất lượng câu trả lời.

### Timeout và thời gian thử lại — 24/11/2026 (DRAFT)

Lượt v9 phát hiện một job mất 86.032ms, gồm 20s + 40s đợi worker thử lại. Timeout có ít nhất 90% thời gian cấu hình áp cooldown tạm thời; sai lệch rất nhỏ khi lấy permit/dựng request không làm mất phân loại timeout. Timeout chỉ do phần ngân sách còn lại quá ngắn không tự đánh dấu model lỗi, để tránh loại nhầm model còn khỏe. Giữ nguyên key trước model sau theo registry đã duyệt, deadline, pacing và Retry-After/quota.

Giữ mã timeout thực thay vì ghi đè bởi candidate bị bỏ qua sau deadline. Worker chỉ rút phần chờ bổ sung xuống tối đa 1s đối với TIMEOUT/GEMINI_REQUEST_BUDGET_EXHAUSTED; HTTP429 và lỗi khác giữ backoff hiện hành. Không tăng call budget, số lần thử hoặc chuyển sang model trả phí. Timeout vẫn có thể tạo đuôi độ trễ; không cam kết mọi câu dưới 10 giây khi nhà cung cấp lỗi.

### Giữ chế độ tìm bãi trong câu hỏi nối tiếp — 24/11/2026

Resolver giữ các nhãn công khai riêng cho tìm theo địa chỉ và đổi bán kính trước nhãn tìm bãi chung. Câu “Nếu muốn nhập địa chỉ thì làm sao?” sau hướng dẫn tìm theo địa chỉ không được tự chuyển sang yêu cầu bật định vị. Chỉ giữ nhãn chế độ, không sao chép địa chỉ riêng từ lượt trước. Đợt v10 trước sửa được lưu riêng; v11 phải được đánh giá trên cùng code/corpus/config bằng các shard không trùng ID.

### Ngữ cảnh lỗi đăng nhập — 24/11/2026

Đối với lỗi đăng nhập chưa rõ nguyên nhân, nguồn được chọn phải ưu tiên câu hỏi về phương thức/thông báo/bước thực hiện, không ghép checklist bãi được phân công của lỗi thiếu quyền. Câu hỏi đã nêu thiếu quyền vẫn dùng nguồn phân quyền tương ứng.

Hỏi tiếp sau lỗi Google ở cửa sổ thường giữ chế độ so sánh cửa sổ thường/ẩn danh bằng nhãn công khai, không sao chép cookie hoặc dữ liệu riêng từ lịch sử. Hướng xử lý phải có bước kiểm tra phù hợp trong bằng chứng; không chỉ trả lời rằng ẩn danh vào được chưa chứng minh hệ thống hỏng. Đề xuất xóa dữ liệu trang cần nhắc khả năng đăng xuất. Context pack chọn FAQ trọn ý cả ở chunk ngắn; không tăng ngân sách hoặc sửa dữ liệu tài chính.

### Cấu hình nghiệm thu và FAQ liên hệ — 24/11/2026
- Bản cuối đợt 4 đặt mặc định thời gian chờ một lần sinh câu trả lời là 4 giây; ngân sách gọi provider mỗi lượt xử lý vẫn 12 giây, tối đa ba lượt worker. Đây không phải cam kết trả lời trong 4/12 giây: pacing, lần thử lại, truy xuất và chuyển key/model được đo riêng. Chỉ timeout/hết ngân sách có khoảng chờ worker tối đa 1 giây; không hạ thời gian chờ của lỗi quota khác.
- Không tạo AiRun hoặc gọi adapter cho candidate sau khi ngân sách chung đã hết. Timeout gần đủ thời gian cấu hình được ghi là timeout; model chỉ nhận phần ngân sách quá ngắn không bị đánh dấu hỏng như một timeout đầy đủ.
- Các cách hỏi riêng về hotline hoặc email **công ty CoParking** có thể dùng nguyên câu trả lời FAQ trong bằng chứng hiện hành. Không chèn số/email cố định vào bộ nhận diện; không áp dụng cho liên hệ riêng, bãi cụ thể, thanh toán, thay đổi thông tin, câu hỏi hỗn hợp hoặc nguồn mâu thuẫn. Đầu ra vẫn qua kiểm tra citation, quyền và ngoại lệ che liên hệ công khai có cấu hình.
- Harness có thể nhận snapshot registry chỉ đọc đã được phê duyệt, dùng AiModelRouter/policy thật để chọn thứ tự. Bản nghiệm thu cuối dùng snapshot chín model với maxOutputTokens=1024, không dùng fixture hai model/4096 token của các lượt cũ. Trạng thái được phê duyệt trong ứng dụng không chứng minh quota hoặc tính sẵn có tại Google.
- Chi tiết hash, phạm vi fixture/live, tỷ lệ và giới hạn nằm trong [báo cáo đợt 4](evaluation/batch-04-report.md). Không gộp các revision thành một cohort.


### FAQ dùng chung độc lập provider — 24/11/2026

- Hướng dẫn đã duyệt có thể trả bằng DB trước index/embedding; xem [hợp đồng tri thức chung](../../COMMON_KNOWLEDGE_RUNTIME.vi.md).
- Dùng nguyên đáp án trong mục FAQ đã duyệt; giữ điều kiện, phủ định, nguồn, phiên bản và quyền. Không tự coi mọi đoạn mô tả hoặc nội dung model sinh là đáp án chuẩn.
- Đáp án chuẩn phụ thuộc tri thức và quyền; phần model Google tổng hợp vẫn cần nghiệm thu riêng.
- Câu hỏi tiếp về lỗi không được biến thành hướng dẫn ban đầu. Mâu thuẫn nguồn, nguồn bị thu hồi hoặc đầu ra bị chặn không được chữa bằng việc tự gọi model khác.
- FAQ không đọc trạng thái tài khoản/giao dịch thực tế và không xác nhận thao tác đã thành công. Những việc đó giữ đường backend được cấp quyền.
- Bằng chứng nghiệm thu phải tách mức bao phủ FAQ, tìm đúng nguồn, đủ ý, output validator, SQL và phản hồi model thật. Một lần trùng keyword hoặc đúng format không chứng minh đúng nghiệp vụ.

### Rà soát bốn đợt và nguồn dùng chung — 24/11/2026

- FAQ có cùng câu hỏi, cùng điều kiện và cùng phạm vi nghĩa trong các nguồn được phép cần thống nhất đáp án. Không thay bằng việc chọn nguồn có điểm cao hơn khi văn bản còn khác nhau; giữ chốt xung đột và kiểm lại nguồn trước công bố.
- Dùng câu điều kiện rõ ràng khi mô tả thao tác chỉ được phép trong một trạng thái. Hướng dẫn hủy không xác nhận đã hủy; câu khẳng định đã thao tác vẫn phải bị chặn nếu chưa có kết quả backend. Không nới validator để làm câu trong dataset qua.
- Nội dung code, mã quyền, lịch sử sửa và ghi chú biên soạn đặt trong vùng `authoring-only`, được script xuất loại bỏ. Bản xuất và manifest/checksum phải được kiểm tra trước khi xây fixture hoặc nhập theo quy trình được duyệt.
- Fixture trong DB cô lập có thể đặt dấu duyệt để kiểm thử FAQ; không có nghĩa tài liệu DRAFT được duyệt phát hành. Không nhận kết quả từ harness luôn gọi model cho cả FAQ là bằng chứng của đường trả lời trực tiếp.
- Bộ câu hỏi cũ và split được giữ để truy vết; không gọi các câu đã dùng trong rà soát là holdout mù. Chấm đúng/đủ ý bằng rubric và nguồn, không bằng dò từ khóa.
- Trạng thái hiện tại theo [báo cáo 16 tài liệu](../../ALL_BATCHES_REVIEW_STATUS.vi.md); gate Java/SQL/FE/live còn chờ phải hiển thị riêng, không gộp thành PASS.
