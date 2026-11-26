# Báo cáo đợt 3 và chuyển key/model — 20/11/2026

> Local inference đã gỡ ngày 27/11/2026; các lượt đo/command cũ trong báo cáo chỉ có giá trị lịch sử. Dùng [bàn giao hiện tại](../../../TEAM_HANDOFF.vi.md), không tiếp tục chạy công cụ local đã xóa.

Đã triển khai và kiểm chứng cục bộ. Tài liệu giữ **DRAFT**, chưa duyệt phát hành. Số liệu tích lũy dưới đây thuộc lượt `release-candidate` dùng output guard v2. Sau khi lượt lặp phát hiện một cách diễn đạt phủ định bị chặn nhầm, guard v3 được kiểm tra lại với 136 đầu ra Gemini đã ghi và một lượt live riêng cho tám ca quan trọng. Không trình bày 600 ca cũ như 600 lần chạy live guard v3; model, corpus và phần generation không đổi giữa hai phiên bản guard này.

## Cơ chế key/model

Giữ key đang dùng sau thành công; thử model tương thích được registry cho phép trên key đó trước khi chuyển key. Quota theo project/model/tác vụ; key cùng project chia sẻ nhóm quota. AUTH chặn credential; quota phút/ngày, quyền model, timeout/5xx và lỗi request có chính sách riêng. Có deadline, số attempt hữu hạn, pacing và cooldown qua cache hiện có cùng bản cục bộ. Không tự đưa model từ catalog vào danh sách miễn phí được duyệt.

Embedding giữ `gemini-embedding-2`, dimension 768 trong index kiểm thử; chỉ chuyển credential, không tự đổi model hoặc trộn index. Test HTTP mô phỏng kiểm tra đổi key sau model, sticky, project chung, key đánh số thưa/hơn năm key, credential không hợp lệ, quota ngày, Retry-After, timeout, đồng thời, che bí mật và serialize qua mapper giống Redis. Đây không phải bằng chứng limiter tổng trên nhiều replica.

Hỗ trợ `GEMINI_API_KEY`, `GEMINI_PROJECT_ID`, các cặp `GEMINI_API_KEY_N`/`GEMINI_PROJECT_ID_N` (N là số nguyên dương), `GEMINI_BASE_URL`, `GEMINI_MAX_ATTEMPTS`, `GEMINI_CALL_BUDGET`, `GEMINI_GENERATION_MIN_INTERVAL`, `GEMINI_EMBEDDING_MIN_INTERVAL`. Xem [.env.example](../../../../../.env.example), [runbook](../../../GEMINI_CREDENTIAL_FAILOVER.vi.md), [ADR-006](../adr/ADR-006-gemini-credential-and-evidence-failover.md).

## Bốn tài liệu và nghiệp vụ đã đối chiếu

1. [Bảng giá và cách tính phí](../knowledge/pricing-and-fee-calculation.md): bảng giá theo bãi/xe/vé; quy tắc bốn giờ, hai mươi bốn giờ, ngày/đêm; không tự thêm giá lễ/phụ thu.
2. [Hóa đơn, thanh toán, hoàn tiền](../knowledge/invoice-vnpay-payment-refund.md): VNPay, ví, tại quầy; SePay có phạm vi đặt chỗ công khai. Trang chuyển về không tự chứng minh trả tiền; tiền đã trừ cần kiểm tra trước khi trả lần nữa. Chưa xác minh có thực thi hoàn tiền tự động.
3. [Voucher](../knowledge/voucher-and-promotions.md): hiệu lực, phạm vi, giá trị tối thiểu, giới hạn; xem thử chưa giữ lượt; giữ lượt khi tạo đăng ký/hóa đơn, thanh toán thành công mới dùng lượt; không mặc định xếp chồng mã.
4. [Đặt chỗ](../knowledge/reservations-and-holds.md): công khai/tài khoản, giữ thanh toán mười phút, hạn vào sáu mươi phút từ thanh toán, tính phí từ lúc trả tiền và trừ trả trước, chỗ cuối và đối soát tiền đến trễ; hủy/không đến không tự hoàn tiền.

Code có các nghiệp vụ trên không đồng nghĩa chatbot có tool thực hiện thay. Registry hiện có đọc xe/vé/lịch sử/phiếu và tạo phiếu hỗ trợ; chưa có tool thanh toán, hoàn tiền, voucher hoặc đặt chỗ. Hướng dẫn dùng module, giữ kiểm tra quyền/sở hữu và xác nhận, không giả kết quả hoặc đổi chính sách tài chính.

Tổng 15 nguồn, 12 tài liệu thuộc ba đợt, 840 câu gồm baseline 120 + 240 mỗi đợt. Có 40 tình huống đợt 3, mỗi tình huống sáu biến thể; 16 tình huống acceptance tạo 96 câu trả lời mới. Biến thể CONTEXTUAL mới là câu có chủ đề tường minh, không phải tất cả đều là hội thoại nhiều lượt; history thật được kiểm tra riêng trong các nhóm cũ và unit controls. Xem [manifest](../knowledge-manifest.json), [dataset](batch-03-questions.jsonl), [truy vết](batch-03-traceability.md), [rubric](batch-03-rubric.md).

## Kiểm thử và kết quả tích lũy

- Bộ backend rộng trước sửa guard v3: Tests run: 3768, Failures: 0, Errors: 0, Skipped: 0. Testcontainers dùng DB riêng; sáu test script đồng bộ đạt.
- Gate AI cuối cho guard v3: Tests run: 1130, Failures: 0, Errors: 0, Skipped: 1; bao gồm replay 136 response thật và ca phủ định/dương tính đối chứng. Một ca live bị bỏ qua có chủ đích trong gate, được chạy riêng sau đó.
- Route: 720/720 câu của ba đợt đúng intent. Không gọi đây là route/runtime 840/840; baseline có lớp golden riêng và phần tương lai chưa được triển khai.
- Runtime: 600 ca, 552 truy xuất, 216 job trả lời; {'COMPLETED': 216}. Hit@5 551/552 (99.8%); đủ bằng chứng theo gate 549/552 (99.5%).
- Đã rà soát nghĩa/citation của 216 câu trả lời: 211/216 (97.7%). Bốn nhóm OWN dùng fixture, không xác nhận dữ liệu tài khoản thực tế.

| Tài liệu | Hit@5 | Đủ bằng chứng | Đúng/đủ theo nghĩa trên câu trả lời |
|---|---|---|---|
| authentication-and-profile | 54/54 (100.0%) | 54/54 (100.0%) | 10/12 (83.3%) |
| cards-and-lost-card | 36/36 (100.0%) | 36/36 (100.0%) | 10/12 (83.3%) |
| chat-conversations-and-support-boundaries | 41/42 (97.6%) | 40/42 (95.2%) | 12/12 (100.0%) |
| chatbot-privacy-and-prohibited-data | 24/24 (100.0%) | 24/24 (100.0%) | 12/12 (100.0%) |
| customer-vehicle-management | 36/36 (100.0%) | 36/36 (100.0%) | 18/18 (100.0%) |
| invoice-vnpay-payment-refund | 60/60 (100.0%) | 60/60 (100.0%) | 24/24 (100.0%) |
| parking-history | 36/36 (100.0%) | 36/36 (100.0%) | 17/18 (94.4%) |
| pricing-and-fee-calculation | 60/60 (100.0%) | 60/60 (100.0%) | 24/24 (100.0%) |
| reservations-and-holds | 60/60 (100.0%) | 60/60 (100.0%) | 24/24 (100.0%) |
| subscription-lifecycle | 36/36 (100.0%) | 36/36 (100.0%) | 18/18 (100.0%) |
| support-ticket-and-escalation | 48/48 (100.0%) | 48/48 (100.0%) | 18/18 (100.0%) |
| voucher-and-promotions | 60/60 (100.0%) | 59/60 (98.3%) | 24/24 (100.0%) |

Chi tiết từng case, kiểu diễn đạt và lý do tại [semantic-review](evidence/batch-03/semantic-review.json). Citation có ID thuộc tập bằng chứng: 193 mục, 0 vi phạm membership. Quyền PUBLIC/CUSTOMER được kiểm tra trong SQL và unit/integration; nguồn citation đúng ID không tự chứng minh mọi câu trả lời đúng ý. Đối chiếu nội dung citation được chấm cùng transcript.

Trên 36 cặp câu canonical cùng nghĩa, có dấu 36/36, không dấu 36/36, chênh 0.0 điểm phần trăm. Đây chủ yếu là câu chuẩn và FAQ/deterministic, không chứng minh mọi câu không dấu/typo đều đạt; lỗi diễn đạt khác vẫn ghi trong từng nhóm.

## Lỗi còn mở và điều kiện nghiệm thu

- `AUTH09-3`: Câu lỗi đăng nhập chung bị chuyển thành hướng dẫn Quên mật khẩu khi chưa biết phương thức/thông báo lỗi; hỏi lại sau đó không sửa được giả định ban đầu.
- `AUTH09-4`: Suy đoán cần khôi phục mật khẩu từ câu không đăng nhập được, chưa hỏi phương thức và lỗi cụ thể.
- `B2-CARD04-4`: Chỉ đưa bước mở biểu mẫu/tra cứu, thiếu điều kiện quyền/bãi và phiếu đang mở khi người dùng hỏi điều kiện tạo phiếu.
- `B2-CARD05-5`: Trả hướng dẫn báo lỗi chung, chưa giải thích kiểm tra ghi nhận thanh toán và thẻ thay thế còn sẵn sàng/cùng bãi/cùng loại ở bước hoàn tất.
- `B2-HIST04-3`: Khuyên Xóa lọc khi người dùng cần tìm theo ngày, không hướng dẫn đặt khoảng ngày; câu trả lời chưa đáp ứng đúng thao tác cần làm.

Ca truy xuất chưa đủ: CHAT06-1, CHAT06-2, B3-VOU08-5. Không hạ threshold, tăng topK hoặc bỏ câu để nâng điểm. Tỷ lệ trung bình không thay thế ngưỡng 95% từng tài liệu hoặc cho phép lỗi tài chính/quyền. Danh sách gate còn mở nằm trong [verification-status](evidence/batch-03/verification-status.json); chưa đổi DRAFT thành APPROVED.

Các ca quan trọng chạy lại hai lượt độc lập ngoài lượt tích lũy: repeat-2: 8 ca, repeat-3: 8 ca, repeat-3-fixed: 8 ca. Lượt repeat-3 ban đầu có B3-PAY04-3 bị guard v2 chặn nhầm câu “không phải bằng chứng độc lập để xác nhận”; giữ nguyên [lượt lỗi](evidence/batch-03/repeat-3.json), không ghi đè. Guard v3 mở rộng đúng ngữ pháp phủ định, vẫn chặn khẳng định mới sau dấu phẩy/nhưng. [Repeat 2](evidence/batch-03/repeat-2.json) và [xác nhận sau sửa](evidence/batch-03/repeat-3-fixed.json) cùng [chấm nghĩa từng lượt](evidence/batch-03/repeat-semantic-review.json) ghi cả PASS/FAIL, không coi COMPLETED là tự đạt. Các trường hợp đã chạy ít nhất ba lần với provider thật; không gọi mọi lần đó là cùng bản guard.

## Model, số lời gọi và độ trễ

Generation thực dùng: {'gemini-3.1-flash-lite': 5, 'gemini-3.5-flash-lite': 115}. Có 150 attempt logic, 124 HTTP generation, 5 HTTP embedding và 1 thay đổi lựa chọn đã ghi log trong lượt. Các attempt CANDIDATE_UNAVAILABLE không gửi HTTP. Token provider báo về: {'input': 164047, 'output': 7733, 'reportedAttempts': 120}; không đoán token của request lỗi hoặc hóa đơn chi phí.

| Nhóm đo | Số mẫu | p50 ms | p95 ms | max ms |
|---|---|---|---|---|
| retrievalRecordedVectors | 552 | 45 | 76 | 170 |
| faqWithoutGeneration | 48 | 63 | 89 | 94 |
| sourcedGenerationJobs | 120 | 5001 | 9462 | 40388 |
| providerAttemptsIncludingPacing | 124 | 4941 | 9413 | 12008 |

Các cache câu trả lời/retrieval của ứng dụng bị tắt để không che lỗi; harness tái dùng vector thật theo model/dimension/nội dung. Số retrieval trên không phải độ trễ embedding lạnh. FAQ không gọi generation; chưa có phép đo production warm-cache p95 <=2 giây. Generation gồm thời gian chờ pacing và retry driver khi phát sinh. Không cam kết mọi câu dưới mười giây. Vẫn giữ giới hạn context cũ; không thêm LLM classifier hoặc vòng retry không giới hạn.

Tập runtime chạy tuần tự trên máy dev và PostgreSQL riêng; p95 này không phải p95 dưới tải nhiều người dùng. Test đồng thời hiện chứng minh giới hạn probe/lỗi credential trong một instance, chưa chứng minh SLO throughput của hệ thống thực tế.

Không lấy p95 tốt của lượt tích lũy che các đợt provider chậm trong lượt lặp. Với mẫu tám ca, p95 gần bằng giá trị lớn nhất:

| Lượt lặp | Số mẫu generation | p50 ms | p95 ms | max ms | HTTP generation |
|---|---|---|---|---|---|
| repeat-2 | 8 | 5099 | 33423 | 33423 | 10 |
| repeat-3 | 8 | 4534 | 86322 | 86322 | 12 |
| repeat-3-fixed | 8 | 7024 | 39247 | 39247 | 12 |

Các đợt timeout/5xx có thể kéo dài cả job vì retry có điều tiết; mục tiêu mười giây chưa được chứng minh ổn định khi provider lỗi. Không giảm timeout mù quáng, tăng call hoặc bỏ kiểm tra bằng chứng để làm đẹp số liệu.

## Báo cáo key

[Preflight catalog](evidence/batch-03/credential-preflight.json) của cả năm biến key đều HTTP 200, không phát hiện key sai xác thực. Catalog không chứng minh quota generation/embedding còn dùng được. Riêng lượt tích lũy 600 ca:

| Biến key | Lời gọi gửi / kết quả provider thành công | Lỗi được pool ghi nhận |
|---|---|---|
| `GEMINI_API_KEY` | 129 / 125 | {'TRANSIENT': 1} |
| `GEMINI_API_KEY_1` | 0 / 0 | Không ghi nhận |
| `GEMINI_API_KEY_2` | 0 / 0 | Không ghi nhận |
| `GEMINI_API_KEY_3` | 0 / 0 | Không ghi nhận |
| `GEMINI_API_KEY_4` | 0 / 0 | Không ghi nhận |

Timeout, 5xx và quota là tình trạng theo model/tác vụ/thời điểm, không tự kết luận key hỏng vĩnh viễn. Các key không có lời gọi generation trong lượt ghi là chưa thử tác vụ đó, không gọi là lỗi. Đầu ra chỉ nêu tên biến/alias; không lưu key, project ID thật hoặc thông báo provider thô. [Metrics và sự kiện](evidence/batch-03/runtime-metrics.json) cùng transcript trước sửa giữ lịch sử vấn đề đã phát hiện.

Tổng hợp các log live trong đợt làm việc (loại trừ test lỗi giả lập): {'GEMINI_API_KEY': {'TRANSIENT': 13}, 'GEMINI_API_KEY_1': {'TRANSIENT': 1}}; lỗi AUTH ghi nhận: 0. Xem [báo cáo key toàn đợt](evidence/batch-03/key-error-summary.json). `GEMINI_API_KEY_1` đã dùng generation thành công ở lượt verified-runtime trước đó; các key `_2`–`_4` chưa cần dùng generation trong các lượt kiểm chứng này. Không kết luận cần thay key vì một timeout/5xx.

## Bằng chứng và giới hạn

[Transcript cuối](evidence/batch-03/release-candidate-runtime.json), [hash nguồn](evidence/batch-03/source-hashes.json), [manifest artifact](evidence/batch-03/artifact-manifest.json) ghi HEAD + worktree dirty, corpus, model và phiên bản chính sách. Bảo toàn transcript trước sửa screening, lexical/snippet và phủ định; không lấy kết quả cũ trình bày là code mới.

Harness chạy SQL hybrid/chunker/provider thật trên PostgreSQL Testcontainers; actor, chat, audit và business ports có fixture. Chưa UAT guest/nhân sự qua trình duyệt, xác minh callback ngân hàng/đối soát hoặc cạnh tranh nghiệp vụ tài chính trên DB thật, kiểm chứng nhiều replica, hay đo cache lạnh/nóng production. Pacing hiện theo instance; cooldown dùng cache chung, chưa có limiter tổng nguyên tử xuyên replica. Project ID do vận hành khai báo.

Không reset, purge, nạp hoặc publish corpus DB dev; không gọi thanh toán/hoàn tiền/đặt chỗ thật; không gửi thông báo, commit, push hoặc deploy. Không cần migration mới trong phạm vi này. Để dùng code trong ứng dụng đang chạy cần quy trình khởi động lại backend của môi trường đó; nghiệm thu/phát hành là gate riêng.


## Rà soát lại cùng Qwen — 24/11/2026

Đã cập nhật bốn nguồn của đợt này, bản xuất và checksum trong lượt rà soát 16 tài liệu. Giữ kết quả ở trên làm lịch sử. Kiểm tra cấu trúc mới đã qua; Java/SQL/FE/Qwen live của bản mới còn chờ RAM và chưa nghiệm thu đúng/đủ ý. Xem [báo cáo tổng hợp](../../../ALL_BATCHES_REVIEW_STATUS.vi.md) để biết thay đổi, phiên bản, trạng thái từng tài liệu và bằng chứng thực chạy. Không kế thừa điểm của corpus cũ làm điểm của bản mới; Google chưa kiểm chứng trong lượt này.
