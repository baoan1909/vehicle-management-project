# Báo cáo tích hợp Qwen local và Google API

Ngày: 24/11/2026. Trạng thái: đã triển khai chức năng; **chưa nghiệm thu chất lượng và tốc độ Qwen live**.

## Hành vi đã triển khai

- Hai nút cho system_admin, cấu hình toàn hệ thống lưu trong DB, kiểm tra quyền ở BE.
- Qwen gồm generation và embedding local; Google giữ pool model/key. Không fallback giữa hai provider.
- Mọi HTTP AI trong Spring đi qua chốt PostgreSQL theo provider/epoch. Metadata, catalog và tác vụ nền cùng chính sách.
- Chuyển chế độ đợi HTTP cũ kết thúc; job và kết quả cũ không được xuất bản. Ghi audit mode/HTTP riêng, không lưu bí mật.
- Index ACTIVE riêng mỗi provider; giữ nguyên cấu hình/vector Google. CANDIDATE local chỉ gồm hai ID cấu hình, kích hoạt vẫn là thao tác quản trị.
- Profile Qwen gọn cho context 2048, concurrency 1, embedding CPU, MRL có điều kiện, lỗi trả an toàn.

## Những thành phần thay đổi

| Nhóm | Thành phần và mục đích |
| --- | --- |
| Runtime | AiRuntimeModeService, persistence adapter, port và state; mode/epoch bền vững, chốt giữa nhiều JVM |
| HTTP | AiProviderHttpClient; bọc cả Gemini generation, embedding và catalog để chặn provider không được chọn |
| Qwen | LmStudioProviderAdapter, WireMapper, Properties; protocol, schema, context, lỗi và vector validation |
| Chat | AssistantOrchestrator, GuestPublicAssistantService, enqueue/status; scope epoch, xuất bản và gói bằng chứng local |
| Tri thức | EmbeddingPromptFormatter, index repository/lifecycle/build, ingestion/review; chọn đúng provider và giữ checkpoint |
| Quản trị | API GET/PUT /api/ai/runtime, MapStruct DTO, đăng ký hai model local ở CANDIDATE |
| FE | AiRuntimeModePanel, API/type và thông báo quản trị phù hợp provider |
| DB | V20261124120000; mode/events/HTTP audit, job snapshot, provider constraints, unique index ACTIVE theo provider |
| Cấu hình | application.yaml và .env.example; không sửa giá trị .env thật |

## Bằng chứng chức năng

Gate cuối chạy tuần tự: **224 test, 0 failure, 0 error** (`target/qwen-work/regression-final-serial.log`). Trong đó có 7 test PostgreSQL + HTTP server có bộ đếm:

- Provider bị tắt gửi 0 request ở cả generation, embedding, catalog.
- HTTP thực còn đang chạy làm trạng thái giữ SWITCHING.
- Instance khác không vượt qua khóa dispatch; local inference giới hạn 1 giữa các instance.
- Công bố tin nhắn và ghi nhận đổi epoch được tuần tự hóa bởi transaction/row lock.
- Hai index ACTIVE tồn tại độc lập; snapshot index Google giữ nguyên khi chọn local.
- Ghi cấu hình với epoch cũ bị từ chối.

Bộ hồi quy bao gồm pool key/model, quota/cooldown/deadline Gemini, routing, index, worker, guest và orchestrator. Đây là nhóm test được chọn theo phạm vi thay đổi, không phải toàn bộ repository.

FE: TypeScript qua (`qwen-mode-lint-serial.log`), Playwright **2/2 qua** (`qwen-mode-e2e-serial.log`). Browser dùng API fixture, kiểm tra hai lựa chọn, SWITCHING/PREPARING/READY, refresh/persistence và người không có quyền; chưa gọi đây là UAT browser nối BE thật.

## Live Qwen

Lượt 03 chạy BE HTTP thật ở port cô lập 56685, FE dev server localhost5173 (tải chờ), LM Studio và Docker trên máy đang sử dụng. BE dùng DB Testcontainers riêng; không phải BE dev và chưa có tải người dùng qua FE.

- 16 tài liệu, **61 vector tạo mới bằng model Qwen thật**, không replay/vector giả; tất cả 768 chiều sau MRL và L2, sai số norm lớn nhất khoảng 1,34e-15.
- Độ trễ tạo vector tài liệu: p50 **126 ms**, p95 **441 ms**, max **2.772 ms**, 61 mẫu. Đây không phải thời gian trả lời chat.
- Dự kiến 50 câu; ghi được **11/50**, chưa chạy đến bốn câu guest cuối harness. Câu đầu tìm đúng tài liệu nhưng sinh trả lời bị `LOCAL_TIMEOUT` sau khoảng **40,2 giây**. Timeout cấu hình 30 giây không bảo đảm đúng thời gian thực khi OS bị nghẽn lịch chạy.
- 10 câu sau có lỗi retrieval `EMBEDDING_FAILED`. Trường failure trong transcript cũ ghi NO_WHOLE_PASSAGE_FITS vì pack rỗng sau lỗi retrieval; summary mới phân loại theo retrievalDiagnostic để không gán nhầm nguyên nhân cho context. Raw transcript được giữ nguyên.
- Không có câu trả lời hoàn chỉnh để chấm độ đúng/đủ ý hoặc citation. Không lấy số câu không lộ dữ liệu từ một lượt không trả lời được để tuyên bố đạt gate an toàn 100%.
- 48 mẫu tài nguyên: RAM vật lý còn thấp nhất **1,9 MB**; VRAM cao nhất **3.814/4.096 MB**. Có lỗi cạn pool JDBC và máy phản hồi chậm. Đã dừng JVM test và FE server do lượt test tạo, yêu cầu gỡ embedding test; không chạy tiếp 39 câu còn lại.
- Sau đó kiểm tra thấy Docker API không khả dụng và LM Studio không còn model được nạp. Chưa xác định riêng nguyên nhân dịch vụ dừng; không có lệnh dừng/xóa container dev trong công việc này. Không tự khởi động lại tải nặng trong tình trạng đó.

p50/p95/max tổng quan sát là 44.561/78.385/78.385 ms, gồm các lượt lỗi hạ tầng. Các số này chỉ là chẩn đoán lượt thất bại, **không phải benchmark hợp lệ cho Qwen**. Generation chỉ có một mẫu timeout; chưa có phân bố độ trễ trả lời thành công.

Kết luận thực tế: embedding local đã chạy thật, nhưng chưa chứng minh được chatbot Qwen trả lời mượt trên cấu hình/tải đo lần này. Không suy ngược rằng kết quả này phủ định lần người dùng đã chạy BE/FE/Qwen bình thường trước đó: harness thêm JVM Maven, DB Testcontainers và tác vụ đo; điều kiện tải khác nhau.

Bổ sung sau lượt thất bại: launcher `scripts/ai-knowledge/run-qwen-local-live.ps1` kiểm tra RAM và Docker trước khi chạy, đề xuất chạy cùng JVM Maven (`forkCount=0`) để giảm bộ nhớ phụ; harness dừng sau hai lỗi retrieval hạ tầng liên tiếp. Guard RAM/syntax launcher đã kiểm tra riêng; thay đổi dừng sớm trong Java harness chưa có lượt live mới do Docker/model chưa sẵn sàng. Không hạ ngưỡng nghiệm thu nội dung.

[Evidence, transcript và hash](evidence/qwen-runtime-2026-11-24/README.md) lưu cả kết quả lỗi. `http-calls.json` cuối lượt live chưa được xuất do dừng sớm; bằng chứng 0 HTTP với provider bị tắt nằm ở integration test có bộ đếm, không gán số liệu giả cho live.

## Các lượt lỗi vẫn giữ lại

- Run 01: embedding local thật thành công; fixture dừng do source_section dài hơn schema 200 ký tự. Đã giới hạn metadata hiển thị của fixture; toàn bộ nội dung giữ nguyên.
- Run 02: tạo 61 vector thật cho 16 tài liệu; dừng khi ghi Instant vào JSON bằng mapper riêng của harness. Đã sửa serialization metadata thành chuỗi thời gian.
- Kiểm tra nặng chạy song song: Node báo thiếu bộ nhớ, JVM kiểm thử bị dừng. Không công nhận lượt đó là gate qua. Chạy lại tuần tự, giới hạn heap, giữ mọi assertion; gate cuối và FE đã qua.

## Phạm vi chưa được xem là hoàn tất nghiệm thu

- Không công bố ngưỡng đúng/đủ ý 95%, route 98%, Hit@5 95% hoặc độ lệch có/không dấu khi chưa chạy đủ/chấm đủ theo từng nhóm.
- Tool calling OpenAI của Qwen chưa được kiểm chứng/ánh xạ; adapter từ chối an toàn, không giả lập thao tác và không gọi Google để cứu. Các đường backend trực tiếp được phân quyền vẫn giữ nguyên.
- Nhận ảnh/PDF trong chatbox không thuộc thay đổi này.
- Lượt Gemini cũ đang tạm dừng được giữ nguyên. Chưa chạy lại nghiệm thu Google thật, không khẳng định quota/key hiện tại đã hồi hoặc tiếp tục lỗi.
- Kiểm thử dùng DB Testcontainers riêng. Chưa apply migration, nhập corpus DRAFT, publish tài liệu hoặc kích hoạt index trong DB dev.

## Cách sử dụng và giới hạn vận hành

Xem [runbook](QWEN_GOOGLE_RUNTIME_MODES.vi.md). Sau khi triển khai migration/code theo quy trình của dự án, system_admin chọn Qwen, đồng bộ catalog và chủ động chuẩn bị/kích hoạt model/index local bằng quyền tương ứng. Giữ nguyên key và index Google để chuyển lại.

Không commit, push, deploy, thanh toán, đặt chỗ, hoàn tiền, điều khiển cổng hoặc gửi thông báo ngoài trong lượt thực hiện này.


## Lượt rà soát 16 tài liệu tiếp theo — 24/11/2026

Đã sửa nội dung và đồng bộ nguồn DRAFT của cả bốn đợt; sửa bộ thử Qwen để fixture có dấu duyệt/block provenance và phân biệt FAQ không gọi model với generation. Báo cáo này giữ nguyên các lượt đo cũ làm lịch sử; không coi chúng là nghiệm thu corpus/harness mới. Xem [báo cáo tổng hợp](ALL_BATCHES_QWEN_REVIEW_REPORT.vi.md) và [evidence mới](evidence/all-batches-review-2026-11-24/README.md). Các gate Java/SQL/full TypeScript/browser/live còn chờ tài nguyên; không gọi Google trong lượt này.
