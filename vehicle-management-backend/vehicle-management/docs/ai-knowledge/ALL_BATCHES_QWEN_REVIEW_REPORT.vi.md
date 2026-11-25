# Rà soát 16 tài liệu và luồng Qwen — 24/11/2026

Đã sửa và đồng bộ đủ 16 tài liệu của bốn đợt, tiếp tục giữ **DRAFT**. Đã đối chiếu các ranh giới nghiệp vụ với code và giao diện. **Chưa nghiệm thu chất lượng chatbot**: 50 kiểm thử offline của bản mới đã qua bằng JVM riêng; assertion PostgreSQL, full Maven/TypeScript, trình duyệt và Qwen live còn chờ tài nguyên. Google chưa được kiểm chứng trong lượt này.

## Mốc bằng chứng và điều kiện chạy

- Nguồn hiện tại: [manifest](phase-0/authoring/knowledge-manifest.json), 16 tài liệu; 984 câu của bốn batch và baseline 120 câu được báo riêng.
- Trước lượt này có 205 test offline qua; 984 câu chạy matcher trong RAM, 209 lượt khớp và 207 lượt qua bộ kiểm tra đầu ra. Đây là số liệu **corpus cũ**, không phải điểm đúng/đủ ý hoặc kết quả corpus mới.
- Đã đối chiếu 41 hash trong evidence tri thức chung với workspace, đều khớp ở thời điểm rà soát. Bằng chứng cũ vẫn giúp đánh giá phần runtime không đổi; không thay cho việc chạy lại corpus và các lớp test vừa sửa.
- Mốc SQL trước đó: 165 migration thành công trong DB riêng, sau đó fixture lỗi transaction proxy; fixture đã sửa sang port interface. Chưa có assertion SQL PASS để kế thừa.
- Các lần kiểm tra tài nguyên trước gate trong lượt này quan sát RAM trống khoảng 66–389 MB. Snapshot được lưu trong [resource-preflight](evidence/all-batches-review-2026-11-24/resource-preflight.json). Full Maven/DB/TypeScript/browser cần tối thiểu 1.024 MB; Qwen live giữ ngưỡng 1.536 MB. Các ngưỡng chỉ là điều kiện bắt đầu, không bảo đảm đủ trong suốt lượt chạy.
- Không khởi chạy test nặng dưới ngưỡng. Một phương án compile/test riêng với JVM 128 MB cũng bị chặn ở bước kiểm tra RAM 512 MB. Không coi bước kiểm tra tài nguyên là test PASS.
- Không dừng ứng dụng/container của người dùng, thay cấu hình Windows/LM Studio hoặc gọi Google. Không tự thay provider đang chọn trong DB dev.

Không kết luận máy không chạy được Qwen cùng BE/FE/Docker: lượt này đang có tải khác, và chưa chạy được bộ kiểm thử cuối.

## Kết quả thực hiện

### Đợt 1 — tài khoản, loại chat, riêng tư, phiếu hỗ trợ

- Làm rõ phương thức đăng nhập, xác minh email và trạng thái hồ sơ; không mặc định lỗi đăng nhập là quên mật khẩu.
- Dùng tên kênh dễ hiểu; quyền tạo phiếu không cho phép tạo trong chat khách hàng–đối tác. Trợ lý tự động khác người xử lý hỗ trợ hệ thống.
- Thay cách gọi mã quyền, trạng thái và thao tác kỹ thuật bằng tiếng Việt trong phần dành cho người dùng. Giữ tên code và ghi chú triển khai trong phần chỉ dành cho biên soạn.
- Làm rõ dữ liệu riêng phải kiểm tra quyền và sở hữu tại thời điểm dùng; nội dung lịch sử không cấp quyền mới.
- Phân biệt gửi biểu mẫu với xác nhận thẻ yêu cầu trong chat, phiếu đã giải quyết với phiếu đã đóng và thời hạn tự đóng 24 giờ đang có trong policy.

### Đợt 2 — xe, vé tháng, mất thẻ, lịch sử

- Bổ sung điều kiện và bước kiểm tra cho hướng dẫn thêm xe và đăng ký vé; tách thanh toán, chọn vị trí, hiệu lực và cấp thẻ.
- Làm rõ ngưng dùng xe không xóa lịch sử, tự hủy vé hoặc tự kết thúc lượt gửi.
- Thống nhất nguyên văn FAQ hoàn tất phiếu mất thẻ giữa nguồn CUSTOMER và nguồn xử lý lỗi PUBLIC. Đối chiếu `LostCardReportUseCaseImpl`: hóa đơn cần được ghi nhận thanh toán, thẻ thay thế cần sẵn sàng, cùng bãi và đúng loại khi trường hợp đó yêu cầu. Giữ cảnh báo không trả tiền lần nữa khi chưa đối soát.
- Giữ khác biệt giữa lập phiếu và hoàn tất; lịch sử theo ngày khác bản tóm tắt gần đây trong chat.

### Đợt 3 — giá, thanh toán, voucher, đặt chỗ

- Bổ sung ranh giới giữa hướng dẫn tính phí với báo số tiền thực tế; không coi thiếu bảng giá là miễn phí.
- Tách tiền bị trừ, giao dịch được hệ thống xác minh và nghiệp vụ hoàn tất. Không hứa hoàn tiền hoặc hiệu lực vé từ ảnh/trang trả về.
- Giữ khác biệt giữa xem thử voucher, giữ lượt và dùng thành công; hủy đăng ký chưa trả tiền không tự trả lại lượt đã dùng.
- Sửa FAQ hủy đặt chỗ thành điều kiện rõ ràng: “Chỉ dùng chức năng hủy khi đặt chỗ đã thanh toán, còn được giữ và chưa sử dụng.” Đối chiếu `CustomerReservationUseCaseImpl.cancelReservation`: chỉ trạng thái giữ chỗ đã trả tiền, chưa dùng mới được hủy. Không đổi policy hoặc nới output validator.

### Đợt 4 — lỗi, giờ/liên hệ, vào/ra, tìm bãi

- Bổ sung cách tiếp tục khi “đã làm rồi vẫn lỗi”, kiểm tra kết quả thao tác cũ trước khi gửi lại.
- Giữ riêng giờ tiếp nhận công ty, giờ từng bãi, mục tiêu phản hồi và thời gian giải quyết; tin chatbot không được tính là phản hồi nhân viên.
- Làm rõ xem trước, thanh toán, hoàn tất lượt gửi và mở cổng; sự cố tại cổng cần nhân viên tại bãi.
- Làm rõ định vị có sự đồng ý, tìm theo địa chỉ/điểm, bán kính và khoảng cách địa lý; tìm bãi không giữ chỗ hoặc bảo đảm nhận xe.

Mọi tài liệu đã tăng phiên bản, đồng bộ bản xuất và checksum. Các mục nguồn code, lịch sử sửa và ghi chú biên soạn được loại khỏi bản xuất. Không nhập các bản này vào DB dev.

## Sửa bộ kiểm thử Qwen

Đã phát hiện và sửa các thiếu sót của `QwenLocalAcceptanceIT`; **các lớp test mới đã biên dịch và test offline đã chạy; Qwen live còn chờ RAM**:

1. Fixture cũ chưa có dấu duyệt, block và phạm vi block của chunk, nên không kiểm tra được đường FAQ chung. Fixture mới dùng bản xuất an toàn, kiểm checksum và tạo đầy đủ provenance trong DB Testcontainers. Dấu duyệt chỉ là giả lập của fixture cô lập.
2. Bộ thử cũ luôn gọi generation sau retrieval. Bản mới trả đáp án chuẩn và kiểm tra lại nguồn khi `COMMON_FAQ`; đo số HTTP thực trong từng case và yêu cầu bằng không ở đường này. HTTP embedding xây corpus được báo riêng, không gán thành HTTP của câu FAQ.
3. Không gọi generation khi bằng chứng chưa đủ. Đầu ra sinh phải có citation hợp lệ và qua output validator; lưu vi phạm, không coi JSON hợp lệ là đúng nội dung.
4. Có lựa chọn batch, offset và số câu, mặc định sáu câu; giữ corpus đủ 16 tài liệu làm nguồn và nguồn gây nhiễu. Mỗi shard ghi rõ ID và tổng mẫu đủ điều kiện. Đây là mẫu câu tĩnh không có history, cộng mẫu guest; không đại diện cho nghiệm thu toàn bộ 984 câu, hội thoại nối tiếp hoặc luồng người đăng nhập.
5. Xuất HTTP audit ở bước kết thúc kể cả shard thất bại; tiếp tục dừng sau lỗi hạ tầng liên tiếp. Tác vụ nền catalog/worker/build được trì hoãn trong fixture để tách phép đo.
6. Thêm test cho chia shard ổn định, không trùng, điều kiện lọc, lựa chọn sai/rỗng; thêm test đọc FAQ thật cho hai lỗi nội dung cùng biến thể không dấu, kèm phản ví dụ khẳng định đã hủy phải bị chặn.
7. Bộ tổng hợp kết quả tách số câu và thời gian của FAQ/generation, giữ lỗi bị output validator chặn và chẩn đoán hạ tầng cũ. Có cảnh báo khi số mẫu ít; dữ liệu JSON sai kiểu không được tính là citation hợp lệ. Bốn test Python cho phần này đã qua, dùng dữ liệu giả lập và không gọi provider.

Đây là thay đổi bộ kiểm thử và dữ liệu hướng dẫn. Không sửa code runtime nghiệp vụ hoặc chính sách an toàn để đổi kết quả đánh giá.

## Các tầng kiểm chứng

| Tầng | Kết quả bản hiện tại | Phạm vi và giới hạn |
|---|---|---|
| Đồng bộ/manifest/liên kết | Qua: 16 nguồn, 4 batch, 1.104 câu gồm baseline; không có lỗi | Kiểm cấu trúc/checksum, không kiểm nội dung trả lời |
| Bộ test xuất tài liệu Python | 9/9 qua | Không DB/model; không phải semantic PASS |
| Bộ test tổng hợp Qwen Python | 4/4 qua | Fixture số liệu; không phải Qwen live |
| Audit nguồn và nội dung tĩnh | Qua: 16 tài liệu, 984 ID batch duy nhất; không còn xung đột câu hỏi trùng nguyên nghĩa theo phép so trực tiếp | Phép so Python chỉ là chẩn đoán câu hỏi giống nhau sau bỏ dấu; không thay CanonicalQuestionKey, ranking SQL hoặc semantic review |
| Cú pháp launcher PowerShell | Qua | Chưa chứng minh Java hoặc Qwen chạy được |
| Java/test mới và corpus mới | 50/50 qua, gồm984 câu matcher trong fixture | Compile/test JVM riêng; full Maven chưa chạy lại |
| PostgreSQL/nguồn/quyền | Chưa chạy assertion bản cuối | Migration thành công trước đó không đủ đóng gate |
| Full TypeScript/browser | Chưa chạy lại | Lượt TypeScript OOM và kiểm tra cú pháp trước vẫn giữ nguyên trong evidence cũ |
| Qwen live/generation | Chưa chạy bản này | Không có tỷ lệ đúng hoặc latency thành công mới |
| Google live | Không gọi, chưa kiểm chứng | Giữ cohort Gemini cũ đang tạm dừng |

Chi tiết source, hash, ngôn ngữ và split của từng tài liệu: [content-integrity.json](evidence/all-batches-review-2026-11-24/content-integrity.json). Điểm đúng/đủ ý của corpus mới là **chưa đo**, không phải 0% hoặc 100%.

## Trạng thái từng tài liệu

Java matcher đã chạy lại đủ 984 câu và các test mới đã qua trong lượt riêng; đây vẫn không phải điểm semantic hoặc Qwen live. “Code” là rà soát tĩnh các điều kiện đã nêu, không phải UAT chức năng.

| Đợt / tài liệu | Code | Nội dung / bản xuất | Offline | DB/nguồn/quyền | Qwen live | Đúng/đủ ý | Lỗi/gate còn mở | Google | Phê duyệt |
|---|---|---|---|---|---|---|---|---|---|
| 1 [Tài khoản](phase-0/authoring/knowledge/authentication-and-profile.md) | Có | 2.2.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 11/11 FAQ đã chấm; 49 câu chưa chấm | Hỏi tiếp, nguồn thiếu của lượt cũ cần chạy lại | Chưa | DRAFT |
| 1 [Loại chat](phase-0/authoring/knowledge/chat-conversations-and-support-boundaries.md) | Có | 2.2.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 3/3 FAQ đã chấm; 57 câu chưa chấm | Retrieval và ranh giới kênh cần kiểm chứng lại | Chưa | DRAFT |
| 1 [Riêng tư](phase-0/authoring/knowledge/chatbot-privacy-and-prohibited-data.md) | Có | 2.1.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 1/1 FAQ đã chấm; 59 câu chưa chấm | Nguồn/quyền/không lặp bí mật cần hồi quy | Chưa | DRAFT |
| 1 [Hỗ trợ](phase-0/authoring/knowledge/support-ticket-and-escalation.md) | Có | 2.1.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 8/8 FAQ đã chấm; 52 câu chưa chấm | Xác nhận/nguồn/trạng thái riêng cần hồi quy | Chưa | DRAFT |
| 2 [Xe](phase-0/authoring/knowledge/customer-vehicle-management.md) | Có | 2.1.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 5/5 FAQ đã chấm; 55 câu chưa chấm | Điều kiện thao tác/ownership cần hồi quy | Chưa | DRAFT |
| 2 [Vé tháng](phase-0/authoring/knowledge/subscription-lifecycle.md) | Có | 2.1.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 1/6 FAQ đã chấm; 54 câu chưa chấm | 5 FAQ thiếu ý bắt buộc; live/SQL còn chờ | Chưa | DRAFT |
| 2 [Thẻ/mất thẻ](phase-0/authoring/knowledge/cards-and-lost-card.md) | Có | 2.2.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 2/5 FAQ đã chấm; 55 câu chưa chấm | 3 FAQ thiếu ý bắt buộc; live/SQL còn chờ | Chưa | DRAFT |
| 2 [Lịch sử](phase-0/authoring/knowledge/parking-history.md) | Có | 2.2.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 4/4 FAQ đã chấm; 56 câu chưa chấm | Ngày/bộ lọc/giới hạn tool cần hồi quy | Chưa | DRAFT |
| 3 [Giá](phase-0/authoring/knowledge/pricing-and-fee-calculation.md) | Có | 2.1.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 20/20 FAQ đã chấm; 40 câu chưa chấm | Điều kiện công thức và dữ liệu giá cần hồi quy | Chưa | DRAFT |
| 3 [Thanh toán](phase-0/authoring/knowledge/invoice-vnpay-payment-refund.md) | Có | 2.1.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 20/20 FAQ đã chấm; 40 câu chưa chấm | Phủ định/đối soát/không hứa hoàn cần hồi quy | Chưa | DRAFT |
| 3 [Voucher](phase-0/authoring/knowledge/voucher-and-promotions.md) | Có | 2.2.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 20/20 FAQ đã chấm; 40 câu chưa chấm | Retrieval chưa đủ của lượt cũ cần chạy lại | Chưa | DRAFT |
| 3 [Đặt chỗ](phase-0/authoring/knowledge/reservations-and-holds.md) | Có | 2.1.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 21/21 FAQ đã chấm; 39 câu chưa chấm | Đã sửa câu điều kiện; đối chứng output guard chờ | Chưa | DRAFT |
| 4 [Lỗi](phase-0/authoring/knowledge/common-errors-and-troubleshooting.md) | Có | 2.1.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 18/18 FAQ đã chấm; 48 câu chưa chấm | Tiếp nối lỗi và nguồn mất thẻ cần chạy lại | Chưa | DRAFT |
| 4 [Giờ/liên hệ](phase-0/authoring/knowledge/locations-hours-sla-contact.md) | Có | 2.1.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 28/28 FAQ đã chấm; 38 câu chưa chấm | FAQ liên hệ/ngoại lệ/nguồn công bố cần hồi quy | Chưa | DRAFT |
| 4 [Xe vào/ra](phase-0/authoring/knowledge/parking-entry-exit.md) | Có | 2.1.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 19/19 FAQ đã chấm; 47 câu chưa chấm | QR/lượt trùng/cổng/thanh toán cần hồi quy | Chưa | DRAFT |
| 4 [Tìm bãi](phase-0/authoring/knowledge/parking-search-and-map.md) | Có | 2.1.0 | Java matcher qua; semantic chờ | Chờ | Chờ | 20/20 FAQ đã chấm; 46 câu chưa chấm | Địa chỉ/định vị/bán kính/không giữ chỗ cần hồi quy | Chưa | DRAFT |

## Tiếp tục kiểm thử trên bản này

Khi đủ RAM, chạy tuần tự: offline Java và 984 câu → `CommonKnowledgePersistenceTest` → full TypeScript → Playwright `qwen-runtime.behavior.spec.ts` → Qwen live từng shard. Giữ đầy đủ log lỗi và so hash trước khi kế thừa bằng chứng. Không giảm assertion, ngưỡng nguồn hoặc ngưỡng tài nguyên.

Launcher live từ thư mục backend, ví dụ:

```powershell
.\scripts\ai-knowledge\run-qwen-local-live.ps1 -Batch 1 -CaseLimit 6 -CaseOffset 0 -OutputDirectory target/qwen-review-b1-shard01
```

Chỉ tiếp tục shard kế tiếp sau khi xem lỗi hạ tầng và mẫu trả lời; không khởi chạy các shard song song. Bộ lấy mẫu này chưa bao phủ hội thoại nối tiếp/dữ liệu riêng. Muốn nghiệm thu đủ 984 câu phải bổ sung và chạy các đường đó bằng backend có quyền, sau đó chấm theo rubric, giữ đúng mẫu số và báo riêng từng nhóm. Không gộp shard khác code/corpus/config thành cùng lượt.

Ngưỡng nghiệm thu giữ nguyên: route tổng ≥98%, nhóm ≥95%; Hit@5 ≥95%; đúng/đủ ý ≥95% mỗi tài liệu/nhóm; chênh lệch có dấu/không dấu ≤2 điểm phần trăm; citation/phiên bản/quyền 100%; lộ dữ liệu, hành động ngoài ý muốn và khẳng định tài chính sai bằng 0. Hiện chưa có bằng chứng mới để đóng các ngưỡng này. Không công bố p50/p95 mới từ matcher hoặc từ một mẫu generation thành công.

## Kiểm chứng Google sau này

Giữ cùng nguồn, referenceAnswer, quyền, phiên bản và rubric; cố định cấu hình rồi chạy tập tình huống trọng yếu trước, mở rộng theo quota. Chấm riêng embedding/retrieval, tổng hợp, schema/citation, phủ định, tài chính và hỏi tiếp. So sánh trên cùng tập câu hỏi và cùng mẫu số. FAQ chuẩn đủ căn cứ không gọi model ở cả hai mode; kết quả Qwen generation không tự chứng minh Google generation đạt.

Các split cũ được giữ để truy vết, nhưng đã được sử dụng trong quá trình rà soát nên không gọi là holdout mù. Không đổi câu hỏi, referenceAnswer hoặc nhãn để nâng điểm. Bộ nghiệm thu mù mới cần được quản lý riêng trước vòng tinh chỉnh tiếp theo.

Không reset/import/publish DB dev, đổi key thật, thực hiện nghiệp vụ thật, gửi thông báo, commit, push hoặc deploy trong lượt này. [Evidence lượt hiện tại](evidence/all-batches-review-2026-11-24/README.md) giữ các kết quả đã chạy và phần còn chờ.


## Lượt chạy theo yêu cầu nghiệm thu Qwen — 24/11/2026, 22 giờ

- Biên dịch riêng các lớp test mới (gồm QwenLocalAcceptanceIT) bằng javac 128 MB: qua. Chạy JUnit trên runtime đã biên dịch và dependency từ lượt Maven trước: **50/50 qua, không bỏ qua test**. Đây không phải full Maven mới.
- Corpus 16 tài liệu hiện tại:984/984 đúng route trong fixture; 209 lượt khớp FAQ,209/209 qua bộ kiểm tra đầu ra,0 xung đột. Không gọi những số này là độ chính xác Qwen hoặc đủ ý toàn bộ984 câu.
- Docker 29.6.1 sẵn sàng. LM Studio đã nạp qwen3.5-4b và text-embedding-qwen3-embedding-0.6b, đang idle ở lần kiểm tra.
- Đã thực gọi launcher live cho batch 1/sáu câu. Guard dừng trước khi khởi chạy vì RAM 418 MB, dưới 1.536 MB. Không phát sinh HTTP inference Qwen từ lượt bị chặn này. Snapshot sau đó thay đổi theo tải; không tự bỏ guard hoặc dừng ứng dụng người dùng.
- Có chẩn đoán nội dung cần rà soát tiếp: hướng dẫn đăng ký vé tháng chưa nêu rõ khoảng ngày 0–7 trong một đáp án; một hướng dẫn tạo phiếu mất thẻ cần đánh giá đủ ý phân biệt với phiếu hỗ trợ. Đây là quan sát trên câu FAQ trong fixture, chưa phải kết quả Qwen.
- Bằng chứng mới tại [attempt01](evidence/qwen-acceptance-2026-11-24-attempt01/README.md). Các mục lịch sử ở trên được giữ để truy vết; trạng thái mới trong mục này thay phần offline còn chờ trước đó.

## Kiểm chứng ý nghĩa câu trả lời — 24/11/2026

Đã đọc và chấm 209 đầu ra FAQ thực từ fixture hiện tại, gồm 103 nhóm câu trả lời. Codex đánh giá ý nghĩa dựa trên yêu cầu nghiệp vụ, câu hỏi/ngữ cảnh, đáp án tham chiếu và nội dung nguồn; không dùng dò từ khóa và không dùng Qwen tự chấm chính nó. Đây là một lượt agent review, chưa có người duyệt độc lập và chưa phải API/UAT.

- **201/209 đúng và đủ ý trên nhóm đã chấm, tương đương 96,17%; 8 câu thiếu ý.**
- 775/984 câu chưa chấm nội dung; không tính thành PASS hoặc tự coi là FAIL. FAQ chỉ bao phủ một phần luồng, nên tỷ lệ trên không phải độ chính xác toàn chatbot.
- Chưa có câu Qwen generation mới được chấm; không gán 96,17% cho Qwen hoặc Google.
- Vé tháng: 5 câu thuộc B2-SUB01 trả “ngày bắt đầu trong khoảng cho phép” nhưng thiếu điều kiện cụ thể hôm nay đến 7 ngày tới. Trên nhóm FAQ đã chấm của tài liệu này chỉ 1/6 đáp ứng đủ ý; 54 câu khác chưa chấm.
- Mất thẻ: 3 câu thuộc B2-CARD04 chưa nêu rõ phiếu hỗ trợ không thay phiếu báo mất; một câu còn thiếu kiểm tra phiếu đang mở một cách rõ ràng. Trên nhóm đã chấm 2/5 đáp ứng đủ ý; 55 câu khác chưa chấm.
- Các đáp án dùng từ tương đương được công nhận: “mã truy cập” thay token, “giữ lịch sử” thay “không xóa lịch sử”, thông tin tại “lần tra cứu” thay “thời điểm”. Không coi chỉ thiếu từ khóa là sai. Không có lời hứa hoàn tiền là đã tuân thủ ranh giới, không bắt buộc lặp thêm một câu từ chối.

**Chưa nghiệm thu:** tỷ lệ trung bình trên 95% không đóng gate từng tài liệu, 775 câu chưa chấm, SQL/quyền và Qwen live. Giữ nguyên corpus để bảo toàn lượt đánh giá; chưa chỉnh source/reference trong khi chấm. Chi tiết từng câu, lý do và hash tại [evidence semantic](evidence/faq-semantic-2026-11-24/README.md).


## Kiểm tra model đã nạp ở tải hiện tại — 24/11/2026

Hai yêu cầu trực tiếp LM Studio đã thành công: một câu trả lời ngắn đúng theo nguồn cung cấp (~10,2 giây) và một embedding 1024 chiều (~8,3 giây). Không khởi động thêm JVM/DB; không hạ guard của harness đầy đủ. Đây là component probe với nguồn đưa sẵn, không đo retrieval/corpus, không nghiệm thu chatbox hoặc suy p95 từ một mẫu. Context nạp hiện 512 token, parallel 1; backend mặc định 2048 token. Xem [bằng chứng khả năng xử lý yêu cầu ngắn](evidence/qwen-current-capacity-2026-11-24/README.md).


## Thực hiện prompt nghiệm thu qua API — 24/11/2026

**Trạng thái: đã sửa nội dung và chuẩn bị bộ chạy; chưa nghiệm thu API/Qwen toàn luồng.** Giữ kết quả các lượt cũ ở trên, không cộng hoặc thay bằng PASS của bản mới.

- Sửa hai nguồn: vé tháng 2.2.0 bổ sung khoảng hôm nay đến 7 ngày tới; mất thẻ 2.3.0 bổ sung phiếu đang mở/trùng và ranh giới phiếu hỗ trợ. Bản xuất/checksum đồng bộ; tiếp tục DRAFT.
- Chấm lại đúng 8 đầu ra offline trước đó thiếu ý: 8/8 đủ ý sau sửa. Không đổi reference/nhãn của bộ cũ để làm xanh. Không suy thành điểm toàn bộ 209 hoặc 984 câu trên revision mới.
- Có lượt 60/61 qua, 1 lỗi được giữ; sau bổ sung rõ bước hoàn tất, 61/61 qua. Gồm test nguồn thật và truy vấn 984 câu trong fixture. Đây là compile/test JVM riêng trên production classes đã có, không phải full Maven hoặc SQL/API.
- Đóng băng 24 câu mới và reference trước inference; bộ cũ vẫn 984+120, tổng kế hoạch API 1.128 câu. Tập mới chưa dùng sửa runtime, nhưng cùng agent biên soạn nên chưa là holdout mù độc lập.
- Bổ sung bộ HTTP qua controller/security/job thật, DB/Redis riêng, dữ liệu xe/vé/phiếu chính chủ, signed JWT và external CAPTCHA fixture. Không có bypass ownership/quyền. API status/message cuối là nguồn câu trả lời; không gọi service để thay end-to-end.
- Thêm bộ chạy chia pha để compiler thoát trước runtime; kiểm source fingerprint, context 2048/parallel 1 thực nạp, guard tài nguyên và output mới. Source thay đổi thì phải compile lại.
- Bộ tổng hợp có 4 test mới; cộng 4 test summary cũ và 3 test guard:11 test Python qua. Test xuất tài liệu 9/9 có evidence riêng. Cú pháp PowerShell và test TypeScript mới đã kiểm; full TypeScript/browser chưa chạy.
- Registry thực có 7 tool. 22 câu trong tập hiện có yêu cầu tool chưa có; giữ làm khoảng trống chức năng, không giả success hoặc bỏ khỏi mẫu số.
- Đã thử full compile nhiều lần. Sau người dùng báo giải phóng thêm, preflight còn 619 MB, chưa đạt 1.024 MB; một snapshot sau đó khoảng 400 MB. Không hạ guard, dừng ứng dụng/container người dùng hoặc đổi DB dev.
- Chưa có câu API/Qwen mới được chạy hoặc chấm: 1.128 câu vẫn NOT_RUN. Không ghi tỷ lệ mới, Hit@5 hoặc p95. Chỉ những kết quả offline đã thực chạy ở trên được công nhận trong phạm vi của chúng.

Bộ HTTP/SQL fixture và frontend mới còn cần full compile, DB assertion, chạy API và chấm nội dung. Các phần UI member, lỗi model, thu hồi nguồn/mode giữa chừng, lịch sử có dữ liệu và các ranh giới tenant vẫn cần mở rộng/kiểm chứng trước nghiệm thu đầy đủ. Nhãn actualIntent trong transcript không được lấy từ expectedIntent; khi chưa có quan sát trực tiếp thì chưa đóng gate 98%.

[Evidence, bảng trạng thái từng câu và lệnh chạy tiếp](evidence/qwen-api-acceptance-2026-11-24/README.md).
