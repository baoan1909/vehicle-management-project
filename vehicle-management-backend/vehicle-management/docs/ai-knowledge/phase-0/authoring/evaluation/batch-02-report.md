# Đợt 2 và hồi quy đợt 1 — 19/11/2026

> Bàn giao 25/11/2026: các lệnh Python trong báo cáo này là bằng chứng lịch sử; wrapper đã được gỡ. Lệnh hiện tại và các giới hạn kiểm thử được ghi trong [TEAM_HANDOFF.vi.md](../../../TEAM_HANDOFF.vi.md). Không dùng log cũ để kết luận bản code hiện tại đã nghiệm thu.

Cập nhật bốn tài liệu đợt 2, đối chiếu backend, frontend và migration; đồng thời sửa luồng chat dùng chung cho cả hai đợt. **Code và artifact đã kiểm thử cục bộ; chưa nghiệm thu chất lượng Gemini của bản cuối vì quota embedding đã hết.** Tài liệu vẫn DRAFT. Không xóa, nhập hoặc publish knowledge vào DB dev; không thay model đang cấu hình trong DB.

## Tài liệu và quy tắc đã sửa

| Tài liệu chuẩn | Nội dung đã đối chiếu |
|---|---|
| [Phương tiện](../knowledge/customer-vehicle-management.md) | Thêm/sửa xe, biển số trùng, xe mặc định, ngưng dùng giữ lịch sử, giới hạn đổi biển số |
| [Vé tháng](../knowledge/subscription-lifecycle.md) | Bắt đầu từ hôm nay đến 7 ngày tới; gia hạn vé hiện tại; điều kiện thanh toán/vị trí; thẻ vật lý riêng; giới hạn hủy |
| [Thẻ và báo mất](../knowledge/cards-and-lost-card.md) | Nhân viên có quyền lập/hoàn tất phiếu; hóa đơn và thẻ thay thế; không dùng phiếu hỗ trợ thay nghiệp vụ mất thẻ |
| [Lịch sử gửi xe](../knowledge/parking-history.md) | Phạm vi chính chủ; bộ lọc; trạng thái; ảnh xem trên giao diện được phép; chat không sửa giờ hoặc phí |

Cả bốn bản có phiên bản 2.0.0, scope CUSTOMER, xác minh ngày 19/11/2026. Xem [đối chiếu code/schema](batch-02-traceability.md) và [rubric](batch-02-rubric.md). Bản upload trong `../../knowledge/` được sinh bằng script đồng bộ; nguồn chuẩn vẫn ở `authoring/knowledge/`.

Đợt 1 được bổ sung câu hỏi thường gặp, làm rõ bước xác minh email, cách gửi biểu mẫu và xác nhận trong chat. Giữ nguyên nội dung câu hỏi baseline 120 và hai bộ 240 câu. Sáu câu SUP04 thiếu mã phiếu được sửa kỳ vọng sang đọc danh sách của mình; sáu câu AUTH08 không còn yêu cầu nhầm quyền đọc phiếu hỗ trợ, vì chat chưa có chức năng đọc hồ sơ.

## Code và tốc độ

- Dùng cùng hướng dẫn trả lời tiếng Việt cho chat công khai và chat sau đăng nhập. Trạng thái hiển thị bằng từ dễ đọc; chặn enum, tên trường, mã quyền và ID nội bộ trong câu trả lời thông thường.
- Sửa lỗi lưu câu trả lời nhiều dòng. Chỉ tin nhắn chat được dùng xuống dòng; giới hạn 4.000 ký tự, chặn HTML và ký tự điều khiển khác vẫn giữ. Tách các bước đánh số liên tiếp bằng thay đổi khoảng trắng; không sửa ngày, số tiền, số bước hoặc ý phủ định.
- Câu liệt kê xe, vé, lịch sử và phiếu của mình rõ ràng đi thẳng vào chức năng đọc có quyền: không gọi model, không cache câu trả lời riêng tư. Câu có mã, ngày, bộ lọc hoặc nhiều yêu cầu không được đổi thành truy vấn toàn danh sách.
- Chức năng đọc trả tóm tắt tiếng Việt từ backend; tối đa 5 mục trong tối đa 20 mục đọc, nói rõ phạm vi. Che biển số, bỏ QR/UID, nội dung hồ sơ, URL ảnh riêng tư. Tra cứu lịch sử không ký URL ảnh.
- `getMy...` xe/vé luôn dùng hồ sơ chính chủ dù tài khoản đồng thời có quyền đọc toàn bộ. Quyền được kiểm tra lại lúc thực hiện; bị thu hồi thì dừng và trả thông báo phù hợp, không gọi model để đoán.
- Với câu hỏi khớp toàn bộ một dòng hỏi–đáp trong nguồn đã truy xuất đủ bằng chứng, có thể trả trực tiếp kèm citation, không gọi model. Dòng Q/A phải đầy đủ; nguồn mâu thuẫn thì từ chối fast path. Không phụ thuộc riêng nhãn mục đầu chunk vì phần chồng lấn có thể mang nhãn mục trước. Áp dụng cả guest và đăng nhập; vẫn giữ scope, kiểm tra output và audit.
- Lỗi embedding/retrieval/index được phân biệt với thiếu căn cứ: guest trả service unavailable; job đăng nhập trả thông báo rõ và dừng với mã lỗi sau các lần thử giới hạn của embedding service. Không tự gọi generation để đoán câu trả lời hoặc lặp job trong lúc quota ngày đã hết.
- Các thao tác xe/vé/thẻ/lịch sử chưa có công cụ được hướng dẫn đúng màn hình. Không tạo thẻ xác nhận giả hoặc tự mở phiếu hỗ trợ thay nghiệp vụ.
- Truy xuất vẫn giữ top K và ngưỡng bằng chứng. Chỉ bỏ lời dẫn câu hỏi trong nhánh tìm từ khóa; vector và audit giữ câu gốc đã che dữ liệu nhạy cảm. Giữ số tiền, mã, ngày và phủ định.
- Tác vụ chat xử lý đồng thời có giới hạn, mặc định 2, cho phép 1–4; chỉ nhận số job còn chỗ xử lý. Chu kỳ nhận job mặc định 500 ms. Một câu chậm không chặn tuần tự toàn bộ người dùng khác.
- Prompt tag cuối là `support-assistant-v3`, retrieval policy `retrieval-policy-v4`, tách namespace cache câu trả lời cũ.
- Thời hạn sinh câu trả lời SUPPORT_CHAT mặc định 10 giây mỗi lần gọi; thời hạn embedding không bị hạ theo. Fallback chỉ trong các model được router cho phép; giữ giới hạn số lần thử và circuit breaker.
- Câu hỏi hiện tại được đưa rõ vào lượt user cuối; không để model chọn nhầm câu trong danh sách FAQ. Bỏ chỉ dẫn cũ luôn yêu cầu thẻ xác nhận khi cần hỗ trợ; biểu mẫu và thẻ trong chat là hai cách gửi khác nhau.

[Migration mới](../../../../../src/main/resources/db/migration/V20261119110000__allow_tool_audit_without_model_run.sql) cho phép `ai_tool_calls.run_id` rỗng khi tra cứu không gọi model. Mapping theo đúng schema mới; bản ghi mới để JPA khởi tạo version. Không xóa dữ liệu hoặc bỏ kiểm tra chủ sở hữu/chống lặp. Kiểm thử adapter thực gồm chèn, cập nhật và tăng version.

## Dữ liệu đánh giá

Đợt 2: 40 nhóm × 6 cách hỏi = 240 câu; regression 192, acceptance 48; mỗi tài liệu 60 câu. Các nhóm không trộn qua hai tập. Hai đợt có 480 câu mới; baseline 120 được lưu riêng, tổng manifest 600 câu.

Các câu acceptance đã được dùng để tìm lỗi và sửa nên đây là tập nghiệm thu hữu hạn đã được xem, không phải holdout mù hoặc UAT độc lập. Không coi route đúng là câu trả lời đúng; không chấm chất lượng chỉ bằng dò keyword.

## Kết quả thực chạy

| Kiểm tra | Kết quả | Phạm vi và giới hạn |
|---|---:|---|
| Backend cuối, gồm PostgreSQL integration | 2.945 qua / 2.948; 3 live opt-in bỏ qua | 0 failure/error; lệnh cuối tắt toàn bộ API thật vì quota đã hết |
| Router hai đợt | 480/480 | 240 câu mỗi đợt; không phải 480 câu trả lời model |
| Legacy được review và thực thi | 68/68 | 29 đợt 1 + 39 đợt 2; hai ca cần tách ngữ cảnh đa lượt, 50 ca còn thuộc phạm vi sau |
| Browser/API fixture | 39 qua; chạy lại 7 ca liên quan cũng qua | Không cộng thành 46 ca vì có giao nhau; không phải E2E Gemini |
| Frontend lint, static regression, build | Qua | Không chỉnh frontend thêm sau lượt kiểm tra liên quan cuối |
| Nguồn đúng trong lượt live hoàn chỉnh gần nhất | 311/312 (99,68%) | Một lỗi embedding vẫn giữ trong mẫu số; corpus 8 tài liệu, trước sửa nhận diện Q/A qua overlap |
| Đủ bằng chứng trong lượt đó | 308/312 (98,72%) | Ba INSUFFICIENT_EVIDENCE và một EMBEDDING_FAILED; không hạ threshold/top K |
| Câu trả lời trong lượt đó | 120/120 có phản hồi | 62 câu gọi model, 10 Q/A trực tiếp, 24 tra cứu cá nhân, 24 hỏi rõ/ngoài phạm vi |
| Chấm nội dung, đủ ý và căn cứ bởi agent | 113/120 (94,17%) | **Chưa đạt 95%**; xem từng ca và điểm trong semantic-last-completed.json |
| Riêng nhóm gọi model | 55/62 (88,71%) | Không dùng các nhánh trực tiếp để che tỷ lệ sai này |
| Lộ dữ liệu trái quyền / thao tác trái phép quan sát được | 0 | Trong các unit/integration/browser/live fixture đã chạy; không thay UAT |

[Bằng chứng lượt live hoàn chỉnh](evidence/batch-02/runtime-before-overlap-fix.json), [chấm từng câu](evidence/batch-02/semantic-last-completed.json), [các ca truy xuất còn lỗi](evidence/batch-02/remaining-retrieval-cases.json).

**Bản cuối chưa được đánh dấu PASS cho live semantic.** Sau lượt hoàn chỉnh nêu trên, đã sửa nhận diện dòng Q/A trong chunk có phần chồng lấn, bổ sung câu hỏi mô tả lỗi hỗ trợ và tách thông báo lỗi dịch vụ. Lượt chạy lại bản mới bị dừng khi Google xác nhận `429 RESOURCE_EXHAUSTED`, quota `EmbedContentRequestsPerDayPerProjectPerModel-FreeTier = 1000`. Các vòng kiểm thử lặp đã dùng hết hạn mức miễn phí trong ngày; đây là ảnh hưởng của đợt kiểm thử, không phải bằng chứng tài liệu không tồn tại. [Phản hồi quota đã lọc bỏ bí mật](evidence/batch-02/google-embedding-quota.json).

Không đổi embedding model, bật thanh toán hoặc chuyển corpus dev để né lỗi. Đổi model sinh câu trả lời không khắc phục quota của model embedding. Các vector truy vấn thật chỉ bắt đầu được ghi từ bản harness cuối; chưa có bộ snapshot đầy đủ để phát lại lượt bị chặn.

Ba cách hỏi bị lệch mục tiêu trong gia đình paraphrase đã được sửa và chạy lại riêng: 3/3 có nguồn và trả lời phù hợp; router đợt 2 đã chạy lại 240/240. [Lý do sửa dữ liệu](evidence/batch-02/dataset-alignment-review.json), [transcript ba ca](evidence/batch-02/scenario-alignment-runtime.json). Một biến thể dùng lỗi gõ lặp `khongg` thay cho chỉ viết tắt `ko`; route/normalizer đã kiểm tra, nhưng live trên bản cuối còn chờ quota.

## Thời gian đã đo

Số liệu sau thuộc lượt live hoàn chỉnh **trước sửa nhận diện Q/A qua overlap**, cache ứng dụng tắt; không gộp các lượt khác nhau hoặc dùng làm SLO production.

| Giai đoạn | Mẫu | p50 | p95 nearest-rank | Lớn nhất |
|---|---:|---:|---:|---:|
| Truy xuất, gồm query embedding + SQL/audit | 312 | 439 ms | 544 ms | 1.904 ms |
| Q/A từ nguồn, không gọi generation | 10 | 457 ms | 497 ms | 497 ms |
| Job có gọi generation | 62 | 1.971 ms | 3.787 ms | 23.749 ms |
| Mỗi lần gọi provider | 65 | 1.527 ms | 3.068 ms | 4.829 ms |

Có ba lần provider lỗi (một 429, hai 5xx). Một job phải đợi backoff thật 20 giây rồi xử lý lại cùng input/job nên có mẫu 23,75 giây. Giữ mẫu này trong kết quả; không chỉ báo những lần nhanh. Nhánh 24 tra cứu cá nhân không gọi model, nhưng dữ liệu và audit port trong fixture là giả lập nên không coi số ms của nó là hiệu năng DB nghiệp vụ thật.

So sánh riêng nhãn có dấu/không dấu trên lượt hoàn chỉnh: mỗi nhóm 52 truy xuất, đều đúng nguồn 52/52 và đủ bằng chứng 51/52; mỗi nhóm 20 câu trả lời đã chấm, đạt 19/20. Đây là hai nhóm theo nhãn dataset, không phải thí nghiệm độc lập; các paraphrase còn lại vẫn có lỗi. [Chi tiết](evidence/batch-02/accent-comparison.json).

## Ví dụ trước/sau có bằng chứng

Các ví dụ lấy từ những transcript thật đã lưu, khác phiên bản FAQ/prompt; chỉ minh họa hành vi, không phải A/B giữ nguyên mọi điều kiện.

| Câu hỏi | Trước | Sau trong lượt đã lưu |
|---|---|---|
| Đăng nhập vẫn lỗi | Gộp xác minh email vào “hình thức đăng nhập” | Hỏi bước đang thực hiện và thông báo lỗi; xác minh email được diễn đạt là bước kiểm tra |
| Biển số đã tồn tại | Có câu hướng tới thẻ xác nhận chưa xuất hiện | Kiểm tra biển số, danh sách xe và nhờ nhân viên xác minh |
| Ngưng dùng xe giữ lịch sử | Có câu tự đặt tên nút Ngưng dùng xe | Q/A trích nguồn ghi đúng Xóa phương tiện tại Xe của tôi; lịch sử được giữ |
| Chat có hiển thị ảnh lịch sử? | Có câu trả “Có” rồi mới dẫn sang giao diện | “Không. Chat không hiển thị ảnh lịch sử gửi xe…”; hướng dẫn mở Xem chi tiết |

Ví dụ dữ liệu riêng là fixture: vé hiển thị “Đang hoạt động; từ 01/11/2026 đến 30/11/2026; giá vé 80.000 đ”; biển số chỉ còn `***345`. UI được kiểm tra với trạng thái `TERMINATED` và trạng thái chưa biết để hiển thị tiếng Việt thay vì raw code. Đây là kiểm tra API fixture/UI, **không bịa transcript Gemini cũ có enum** khi không có bằng chứng như vậy.

## Các ca chưa đóng và hướng xử lý

- `SUP10-1`…`SUP10-6`: lượt hoàn chỉnh cuối còn thiếu chi tiết mô tả lỗi hoặc lẫn phiếu hỗ trợ với phiếu báo mất thẻ. Đã sửa Q/A nhận diện qua phần chồng lấn và bổ sung câu hỏi chuẩn; unit đã qua, **cần chạy lại live sau khi quota khôi phục**.
- `SUP09-3`: còn diễn đạt “gửi và xác nhận”, cần phân biệt gửi form với thẻ trong chat và hỏi rõ người dùng đang ở bước nào. Không đánh dấu ca này đã đạt chỉ vì có citation.
- `CHAT06-1`, `CHAT06-2`, `B2-SUB02-6`: dưới ngưỡng bằng chứng; cần tiếp tục xem query framing, nguồn và ranking, giữ nguyên gate. `SUP06-5` là lỗi embedding, báo riêng khỏi thiếu bằng chứng.
- Live của bản cuối, kiểm thử corpus dev thực và UAT chưa hoàn tất. Không publish từ kết quả unit hay từ vài mẫu nhanh.

Nghiệm thu live cuối chỉ đóng khi chạy lại đúng dataset hiện tại, chấm các câu lỗi trên theo rubric, xác nhận >=95% và không có lỗi nghiêm trọng. Nếu tiếp tục cải tiến FAQ trước embedding để vẫn phục vụ khi API hết quota, cần thiết kế gate bằng chứng riêng và kiểm thử quyền/citation; không lén bỏ gate trong đợt này.


Các lượt trước được giữ trong thư mục evidence, gồm cả kết quả sai. [Manifest bằng chứng](evidence/batch-02/evidence-manifest.json) phân biệt trạng thái source/code cuối với snapshot đã thực sự chạy live; không cộng số liệu từ nhiều corpus để coi là một lần chạy.

## Điều kiện đo và giới hạn

- Truy xuất dùng PostgreSQL/pgvector và Flyway trong Testcontainers, Markdown extractor/chunker runtime, Gemini embedding thật, 8 tài liệu của hai đợt, top K=5 và cache tắt. Không dùng MinIO hoặc corpus đang publish trong DB dev.
- Sinh lời gọi adapter Google thật với `gemini-3.1-flash-lite` và `gemini-3.5-flash-lite` trong allowlist fixture. Cấu hình fixture không thay cấu hình model thật của ứng dụng.
- Thời gian job tính từ lúc worker nhận xử lý; không gồm HTTP nhận tin, đợi hàng đợi, WebSocket, trình duyệt hoặc mạng người dùng. Đây là p50/p95 mẫu, không phải cam kết tốc độ production.
- Dữ liệu xe/vé/lịch sử/phiếu trong live fixture là giả lập có chủ đích, audit/chat port được capture. Số lần gọi model bằng 0 là kết quả kiểm chứng; số ms của nhánh này không đại diện hiệu năng truy vấn dữ liệu thật. Quyền/chính chủ, thu hồi quyền và persistence được kiểm tra riêng bằng unit/integration.
- Browser dùng API fixture, không phải E2E Google thật. Guest/support đã kiểm tra 32 ca; trang vé/phương tiện 7 ca. Frontend lint, static regression và build đã qua; bảy ca liên quan được chạy lại sau chỉnh trạng thái cuối.
- Đánh giá nội dung do agent đọc câu trả lời cùng ngữ cảnh và nguồn đã trích, không phải phê duyệt của người dùng nghiệp vụ. Lỗi nghiêm trọng không được bù bằng điểm trung bình.

## Tái lập và đưa vào ứng dụng

Chạy trong thư mục backend. Python phải là executable thực; trên máy này PATH không có alias Python dùng được:

```powershell
& 'C:/Users/baoan/AppData/Local/Programs/Python/Python314/python.exe' scripts/sync_ai_knowledge.py
& 'C:/Users/baoan/AppData/Local/Programs/Python/Python314/python.exe' scripts/test_sync_ai_knowledge.py
& docs/ai-knowledge/phase-0/authoring/evaluation/validate-phase0.ps1 -PythonExecutable 'C:/Users/baoan/AppData/Local/Programs/Python/Python314/python.exe'
.\mvnw.cmd -B -ntp '-Dmaven.repo.local=.m2-local' '-DargLine=-Xmx1024m -Dfile.encoding=UTF-8' test
```

Không chạy live tiếp trong khi quota ngày còn hết. Sau khi quota khôi phục, lượt gọi Google là opt-in `PHASE2_LIVE_EVAL=true`, thêm `PHASE2_LIVE_ACCEPTANCE_ONLY=true` để chỉ chạy 96 acceptance + 24 ca tra cứu; chọn `-Dtest=Phase2KnowledgeAcceptanceIT`. Cấp `GEMINI_API_KEY` qua môi trường an toàn; không dán key vào tài liệu hoặc lệnh công khai. Đặt hai biến live thành false khi kiểm thử thông thường. `PHASE2_QUERY_VECTOR_REPLAY=false` là chế độ đo trực tiếp và ghi vector thật của query thành công theo model/dimension/format/text hash. Sau khi có snapshot đầy đủ, đặt `PHASE2_QUERY_VECTOR_REPLAY=true` để hồi quy dùng lại vector thật; thiếu snapshot thì dừng, không tạo vector giả hoặc gọi API ngầm. Chunk mới vẫn cần embedding tương thích. Không dùng thời gian replay để tuyên bố latency API thật.

`PHASE2_LIVE_RETRIEVAL_ONLY=true` cho phép kiểm tra mọi câu STATIC mà không gọi generation; đặt `PHASE2_LIVE_ACCEPTANCE_ONLY=false` và xóa `PHASE2_LIVE_CASES` khi cần đủ 312 câu. Chế độ này vẫn cần quota embedding hoặc snapshot query thật.

`scripts/summarize_ai_live_evaluation.py <transcript> <output>` tính p50/p95 theo nearest-rank và lưu bằng chứng chunk gọn; script không tự chấm đúng nghĩa.

Khi chạy bản mới, backend cần khởi động lại và áp dụng migration bằng cơ chế Flyway hiện có. Tài liệu mới chưa ảnh hưởng câu trả lời của corpus đang chạy: cần review, nhập đúng scope, kiểm tra chunk rồi publish/build/activate index theo quy trình. Không chạy reset DB hoặc tự chuyển DRAFT thành APPROVED trong đợt này.

Các tài liệu ngoài hai đợt, dữ liệu nội bộ còn thiếu, kiểm thử trên corpus dev thật và UAT nằm ở các bước sau trong kế hoạch tổng. Không đánh dấu toàn bộ dự án hoàn tất chỉ vì hai bộ câu hỏi đã qua.

## Đường dẫn đầy đủ của bốn tài liệu chuẩn

```text
C:/DiskD/VehicleManagementProject/vehicle-management-backend/vehicle-management/docs/ai-knowledge/phase-0/authoring/knowledge/customer-vehicle-management.md
C:/DiskD/VehicleManagementProject/vehicle-management-backend/vehicle-management/docs/ai-knowledge/phase-0/authoring/knowledge/subscription-lifecycle.md
C:/DiskD/VehicleManagementProject/vehicle-management-backend/vehicle-management/docs/ai-knowledge/phase-0/authoring/knowledge/cards-and-lost-card.md
C:/DiskD/VehicleManagementProject/vehicle-management-backend/vehicle-management/docs/ai-knowledge/phase-0/authoring/knowledge/parking-history.md
```


## Rà soát lại cùng Qwen — 24/11/2026

Đã cập nhật bốn nguồn của đợt này, bản xuất và checksum trong lượt rà soát 16 tài liệu. Giữ kết quả ở trên làm lịch sử. Kiểm tra cấu trúc mới đã qua; Java/SQL/FE/Qwen live của bản mới còn chờ RAM và chưa nghiệm thu đúng/đủ ý. Xem [báo cáo tổng hợp](../../../ALL_BATCHES_QWEN_REVIEW_REPORT.vi.md) để biết thay đổi, phiên bản, trạng thái từng tài liệu và bằng chứng thực chạy. Không kế thừa điểm của corpus cũ làm điểm của bản mới; Google chưa kiểm chứng trong lượt này.
