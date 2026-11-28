# Báo cáo đợt 5 — 2026-11-27

Đã tạo 5 nguồn DRAFT cho 4 chủ đề: đăng ký công khai và quản trị tổ chức nội bộ tách scope, cộng nhân sự/phân quyền, cấu trúc/sơ đồ và danh mục. Tổng kho 21 tài liệu, gồm nguyên 16 nguồn cũ. Bản xuất và manifest đã đồng bộ. Chưa nghiệm thu chất lượng Google chatbox.

## Bộ dữ liệu và mẫu số

| Tập | Dự kiến | Có transcript | Đã chấm |
|---|---:|---:|---:|
| LEGACY_1104 | 1104 | 0 | 0 |
| BATCH5_MAIN_240 | 240 | 8 | 0 |
| ADDITIONAL_24 | 24 | 0 | 0 |
| BATCH5_HOLDOUT_40 | 40 | 0 | 0 |
| GENERATION_PROBES_4 | 4 | 1 | 0 |

Bốn đợt cũ 984 + nền 120 = 1.104 giữ nguyên hash. Tập chính 240 = 60/chủ đề; holdout 40 = 10/chủ đề; 24 bổ sung cũ và 4 probe generation giữ riêng. Không có tỷ lệ đúng toàn chatbot khi chưa chấm.

## Kiểm tra offline và fallback

Các lượt test Java phù hợp ghi nhận 244 test, 0 failure, 0 error; không phải toàn suite repository. 12 test Node đã qua. Xem [JUnit](evidence/batch-05/java-tests.json).

Validation cấu trúc: PASS, lịch sử giữ nguyên: true, holdout đúng hash freeze: true. Đây không phải điểm ngữ nghĩa. Timeout Google mặc định đã tăng generation từ 4 lên 20 giây, provider plan từ 12 lên 60 giây sau bằng chứng đáp ứng khoảng 16 giây; .env cục bộ đổi đúng dòng generation timeout. Proxy observer mới không chặn nối đuôi và đóng băng count sau drain; vẫn chỉ một case/worker. Bộ HTTP giả lập kiểm tra thứ tự key/model có báo cáo riêng tại [fallback-report.md](evidence/batch-05/fallback-report.md).

## Google live qua API

| Cohort | Proxy observations Google | Catalog / embedding / generation | Upload / duyệt | Kết thúc |
|---|---:|---|---|---|
| cohort-20261126T183340Z | 0 | 0 / 0 / 0 | 0 / 0 | STOPPED |
| cohort-20261126T183628Z | 1 | 1 / 0 / 0 | 0 / 0 | STOPPED |
| cohort-20261126T184009Z | 1 | 1 / 0 / 0 | 0 / 0 | STOPPED |
| cohort-20261126T184255Z | 6 | 1 / 5 / 0 | 1 / 0 | STOPPED |
| cohort-20261126T184526Z | 79 | 1 / 78 / 0 | 21 / 21 | STOPPED |
| cohort-20261126T185016Z | 0 | 0 / 0 / 0 | 0 / 0 | STOPPED |
| cohort-20261126T185736Z | 80 | 1 / 79 / 0 | 21 / 21 | STOPPED |
| cohort-20261126T191654Z | 97 | 1 / 91 / 5 | 21 / 21 | STOPPED |
| cohort-20261126T192655Z | 99 | 1 / 90 / 8 | 21 / 21 | STOPPED |

Generation ở cohort cuối: 8 quan sát, HTTP/status {"503":3,"NO_RESPONSE":5}, 0 phản hồi generation được giao theo run thành công. Aliases quan sát: GEMINI_API_KEY, GEMINI_API_KEY_1, GEMINI_API_KEY_2, GEMINI_API_KEY_3, GEMINI_API_KEY_4, GEMINI_API_KEY_5; chưa có quan sát generation: GEMINI_API_KEY_6, GEMINI_API_KEY_7. Không suy hết quota hoặc đã thử hết mọi credential từ lỗi/deadline. Bộ đếm và budget đọc riêng từng cohort.

Điểm dừng cuối: IllegalStateException: CASE_NOT_COMPLETED_CHECKPOINT:PROCESSING:not_a_claim_that_all_keys_are_exhausted. Không suy quota còn lại. Số trong bảng là dòng quan sát proxy; đối chiếu riêng checkpointHttpCount/httpCountConsistent và in-flight observations. Cohort cũ có thể có proxy hoàn thành muộn sau checkpoint, không coi đó là tổng gửi cuối cùng. Lỗi harness, provider, cooldown và model được lưu theo từng cohort; không gộp các corpus/prompt/model khác nhau.

Harness dùng HTTP socket thật, DB Testcontainers riêng, luồng upload/worker/preview/publish/index và chatbox thật. Chỉ thay bộ xác minh JWT bằng định danh test và kho MinIO bằng filesystem riêng; không mock router, retrieval, provider hay business ports. PUBLIC ở harness là tài khoản xác thực chỉ có quyền hỗ trợ và scope công khai; chưa chứng minh luồng khách ẩn danh.

## Bảng từng case và bằng chứng

[Case results](evidence/batch-05/case-results.jsonl), [tổng hợp cohort](evidence/batch-05/cohort-summary.json), [theo chủ đề](evidence/batch-05/coverage-by-topic.json), [theo tài liệu](evidence/batch-05/coverage-by-document.json), [theo ngôn ngữ](evidence/batch-05/coverage-by-language.json), [theo model](evidence/batch-05/coverage-by-model.json), [kiểm kê gốc](evidence/batch-05/baseline-inventory.json), [validation](evidence/batch-05/offline-validation.json). Mỗi thư mục cohort có transcript, checkpoint, HTTP attempts và artifact hash. Log thô riêng nằm dưới target, chỉ giữ hash và đường dẫn, không đưa lên Git.

## Gate còn thiếu

- Hướng xử lý ≥98%, nguồn top 5 ≥95%, đủ ý ≥95% từng nguồn/nhóm ngôn ngữ: PENDING khi chưa đủ live và review.
- Nguồn/phiên bản/quyền 100%, lộ dữ liệu/vượt quyền/thao tác ngoài ý muốn bằng 0: chưa công nhận chỉ từ câu hỏi an toàn. Bốn fixture API đã qua: đơn vị của mình 200, đơn vị khác 403, tài khoản công khai không quản trị tri thức 403, tài khoản DISABLED không mở hỗ trợ 403; tạo index khi có candidate trả 409. Chưa có ma trận đầy đủ theo actor/bãi và 240/40 case để công nhận toàn bộ gate; tám câu hoàn thành không làm đổi các bảng nghiệp vụ được lấy hash.
- Migration hiện chỉ cấp AI_KNOWLEDGE_READ_EMPLOYEE mặc định cho EMPLOYEE/PARKING_MANAGER; không cấp mặc định cho PARTNER_ADMIN. Fixture live cấp quyền đọc này cho vai trò kiểm thử riêng để đo đường có quyền; không chứng minh đối tác mặc định có quyền đọc bốn nguồn nội bộ. Không tự mở scope hay cấp thêm quyền trên DB dùng chung; cần review chính sách cấp quyền trước phát hành.
- Holdout do AI khác biên soạn và đóng băng; human review PENDING. Có [ghi nhận cần adjudicate gold](batch-05-gold-review-status.json) từ tác giả; không sửa holdout, không dùng kết quả để tuning runtime.
- Chất lượng từng model dự phòng và độ phủ generation đủ 4 chủ đề: chưa công nhận khi không có transcript đã chấm phù hợp.
- Lịch sử hội thoại được dựng bằng tin nhắn fixture lưu thật trong DB ở harness mới; chỉ công nhận nhóm CONTEXTUAL sau khi có transcript đúng phiên bản và được chấm.

## Thay đổi và chạy lại

Thêm nguồn, dataset, scenario, rubric, traceability, holdout, validator, trình tạo tài liệu và harness API. Bổ sung từ vựng miền quản trị đối tác và nhận diện câu thiếu thông tin trong AssistantIntentRouter; không cấp quyền hoặc thêm công cụ nghiệp vụ. Yêu cầu đọc/ghi dữ liệu quản trị chưa có tool được chặn trước khi chọn nhầm chức năng dành cho khách hàng. Tạo index mới khi đã có candidate dùng khóa đồng bộ hiện có và trả Conflict thay vì lỗi ràng buộc DB/HTTP 500. Corpus offline test dùng resolver scope thật theo quyền khai báo thay vì chỉ giả lập PUBLIC/CUSTOMER. Không khôi phục provider local, không sửa migration cũ hay dữ liệu dev, không commit/push.

Collector citation API và freeze/drain proxy được bổ sung sau cohort 20261126T192655Z; xem artifact của cohort tương ứng trước khi công nhận collector live. Ở lần bàn giao này collector mới chỉ được biên dịch/kiểm tra offline. Cần cohort mới để kiểm source/version đầy đủ, không kế thừa PASS từ 8 FAQ còn thiếu bằng chứng citation.

Xem [phân tích code](batch-05-code-analysis.md), [runbook DB sạch](batch-05-runbook.md) và [review nội dung độc lập](batch-05-independent-review.md).
