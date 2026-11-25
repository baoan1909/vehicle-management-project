# Chuẩn bị nghiệm thu qua API Qwen — 24/11/2026

**Chưa có câu API/Qwen mới được nghiệm thu trong lượt này.** Các file offline và mã bộ chạy không được dùng thay cho kết quả HTTP thực. Nguồn vẫn DRAFT, không sửa DB dev.

## Phần đã kiểm chứng

- Hai nguồn DRAFT được sửa: vé tháng phiên bản 2.2.0, mất thẻ phiên bản 2.3.0; bản xuất và manifest đồng bộ. Bộ cũ 1.104 câu không đổi câu hỏi hoặc nhãn để tăng điểm.
- `eight-case-post-fix-review.json`: 8 câu thiếu ý cũ đã được đọc lại trên đầu ra offline sau sửa, 8/8 đủ ý. Đây là Codex review, không là người duyệt độc lập hoặc Qwen sinh.
- `offline-attempt01-60pass1fail.log`: lượt hồi quy tìm thấy một FAQ chưa diễn đạt rõ bước hoàn tất; giữ nguyên log lỗi.
- `lightweight-offline.log`, `lightweight-summary.json`: lượt 61/61 test offline qua sau sửa, có kiểm lại 984 câu trong fixture. Lượt compile này dùng dependency và production classes có sẵn; không thay gate Maven compile cuối.
- `python-summary-tests.log`: 8 kiểm thử tổng hợp số liệu qua; không biến HTTP thành công thành semantic PASS.
- `python-launcher-tests.log`: 3 kiểm thử guard qua; thiếu RAM, build cũ hoặc output không an toàn phải bị chặn trước khi chạy.
- `capability-gaps.json`: 22 câu của bộ hiện có yêu cầu tool chưa có trong registry hiện tại. Giữ nguyên yêu cầu để báo khoảng trống; không giả kết quả hoặc tự đổi gold thành PASS.
- `post-fix-offline-observations.json`, `post-fix-offline-metadata.json`: đầu ra/query/nguồn của 984 câu sau sửa; vẫn không phải API.
- `planned-case-status.jsonl`: 1.128 câu gồm 1.104 câu cũ và 24 câu mới; câu chưa chạy có answer/source=null và NOT_RUN. Không tạo transcript HTTP giả.

## Bộ chạy đã bổ sung

`QwenChatApiAcceptanceIT` mở server HTTP thật và dùng DB PostgreSQL cùng Redis Testcontainers riêng. Cả 16 nguồn được giữ trong kho; demo sources từ migration được vô hiệu hóa chỉ trong DB fixture. Dấu duyệt là giả lập của test, không phải phê duyệt phát hành.

- Guest đi qua API CAPTCHA verify rồi assistant messages; proof/rate limiter dùng Redis thật. Chỉ nhà cung cấp thử thách bên ngoài được thay bằng fixture.
- Member gửi qua API chat, chờ API status và đọc tin trả lời cuối theo inputMessageId. Identity dùng JWT RSA ký bằng khóa tạm; bộ kiểm chữ ký và permission/ownership DB vẫn chạy. Không lưu token vào evidence.
- Fixture mỗi member có xe, vé chờ thanh toán và phiếu hỗ trợ riêng, có manifest dữ liệu để chấm; fixture lịch sử hiện vẫn trống. Cần thêm/làm rõ case lịch sử có dữ liệu trước nghiệm thu đầy đủ chức năng đó.
- Có assertion HTTP cho guest chưa đăng nhập, actor khác chủ hội thoại, JWT sai chữ ký và không gọi model khi bị chặn.
- Corpus dùng embedding Qwen thật; API dùng retrieval, job, provider guard, output guard và publish của ứng dụng. Không gọi trực tiếp service để thay kết quả API.
- Nguồn/rank/version/scope/tenant, tool, model HTTP và trạng thái công bố được lưu. Nhãn actualIntent không tự lấy từ expectedIntent; chỗ chưa có quan sát route trực tiếp phải giữ chưa xác định.
- Generation và embedding Google bị tách khỏi mode local; baseURL Google trong fixture trỏ cổng loopback không phục vụ và audit yêu cầu 0 HTTP provider GEMINI. Không dùng key thật để kiểm tra Google.
- Outbound realtime được thay bằng fixture; không kiểm chứng WebSocket end-to-end bằng bộ này. Không gửi thông báo/email, thanh toán hay điều khiển thiết bị thật.

**Bộ chạy mới vẫn cần được chạy trên DB/API thật** để xác minh cấu hình Spring, SQL fixture và các assertion trên. Biên dịch thành công không phải xác nhận integration PASS.

## Tập câu mới

`phase-0/authoring/evaluation/qwen-api-new-evaluation.jsonl` có 24 câu mới và file reference riêng. Hash nằm trong metadata, được chốt trước lần inference đầu. Cùng agent biên soạn nên không gọi là holdout mù độc lập. Nếu dùng kết quả mới để sửa runtime, phải đánh dấu đã được sử dụng và giữ lượt trước sửa.

## Cách chạy tuần tự

Từ thư mục backend:

```powershell
.\scripts\ai-knowledge\run-qwen-api-acceptance.ps1 -Phase Compile -OutputDirectory target/qwen-api-compile-final
.\scripts\ai-knowledge\run-qwen-api-acceptance.ps1 -Phase Offline -OutputDirectory target/qwen-api-offline-final
.\scripts\ai-knowledge\run-qwen-api-acceptance.ps1 -Phase Db -OutputDirectory target/qwen-api-db-final
.\scripts\ai-knowledge\run-qwen-api-acceptance.ps1 -Phase Api -Dataset batch-01 -Offset 0 -Limit 6 -OutputDirectory target/qwen-api-b1-shard01
```

Compile xong compiler thoát. Các pha sau chạy một JVM riêng, không giữ Maven bên cạnh. Source fingerprint phải khớp artifact; mọi thư mục output phải mới. Guard giữ nguyên: compile/DB 1.024 MB, offline 512 MB, API 1.536 MB. Model chat thực nạp phải idle/context 2048/parallel 1, embedding phải có sẵn. Đây là ngưỡng bắt đầu, không bảo đảm đủ tải suốt lượt.

Lượt Compile đã được thử nhiều lần, kể cả sau người dùng giải phóng thêm RAM, nhưng bị guard chặn trước khởi chạy. Xem các preflight được lưu; không kết luận model không xử lý được câu ngắn từ lỗi này.

Đổi Dataset sang baseline/batch-02/batch-03/batch-04/new và tăng Offset để chạy các shard không trùng. Mỗi shard vẫn nạp cả 16 nguồn, không chỉ tài liệu đích. Chưa có cache vector xuyên các lượt trong bộ này; có thể tái sử dụng vector thật trong một cải tiến sau nếu có đủ model/input/prompt/dimension/provenance, không dùng vector giả.

## Cập nhật công cụ — 25/11/2026

Wrapper Python đã được gỡ; launcher hiện tại là PowerShell và summarizer là Node.js. Các kết quả test Python trong thư mục này là lịch sử của bản cũ. Công cụ Node thay thế có test riêng; launcher PowerShell mới chỉ kiểm cú pháp, chưa chạy compile/DB/API đầy đủ. Xem [bàn giao](../../TEAM_HANDOFF.vi.md) để tiếp tục trên máy đủ RAM và chốt khác biệt context 512/2048.

## Browser và chấm câu trả lời

Test frontend `tests/qwen-chat-api-live.behavior.spec.ts` chuyển tiếp request sang backend fixture thật, không thay đáp án. Chỉ widget CAPTCHA bên ngoài được thay. Nó kiểm guest gửi/hỏi tiếp và nguồn; chưa bao phủ UI member, lỗi model, thu hồi nguồn hoặc chuyển mode.

Mở pha Api với `-BrowserHoldSeconds 180`, rồi đặt `QWEN_API_BROWSER_READY_FILE` tới browser-ready.json của lượt đó và chạy Playwright khi đủ RAM. Browser ghi browser-complete.json để server kết thúc cửa sổ chờ. Không mở browser khi compiler đang chạy.

Sau khi có transcript:

```powershell
node scripts/ai-knowledge/summarize-qwen-api.mjs target/qwen-api-b1-shard01
```

Script xuất JSON và bảng CSV câu hỏi/câu trả lời/nguồn/trạng thái. Mặc định mọi câu đều NOT_GRADED. Grade riêng cần verdict, lý do định tính và hash đúng câu trả lời thực; thay đáp án làm grade cũ mất hiệu lực. Không tự suy đúng/đủ ý từ keyword, HTTP 200 hoặc COMPLETED. Các gate route/Hit@5/citation/quyền chưa được chấm phải giữ null/chưa nghiệm thu.

Xem [báo cáo tổng hợp](../../ALL_BATCHES_QWEN_REVIEW_REPORT.vi.md) để biết trạng thái cuối và các giới hạn còn mở.
