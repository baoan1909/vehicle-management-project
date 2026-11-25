# Kế hoạch rà soát lại dự án và làm mới tri thức chatbox

- Ngày lập: 19/11/2026.
- Trạng thái: Bước nền và đợt 1 đã triển khai code/tài liệu; generation thật đã phục hồi 8/8 probe bằng fallback, chưa nghiệm thu toàn bộ chất lượng. Xem [báo cáo đợt 1](phase-0/authoring/evaluation/batch-01-report.md). Các đợt sau vẫn là kế hoạch; chưa thay corpus DB đang hoạt động.
- Phạm vi gốc: `docs/ai-knowledge/phase-0/authoring`, các module Backend/Frontend liên quan, PostgreSQL, ingestion/index, tool và giao diện chat.
- Đơn vị triển khai: mỗi đợt phân tích và cập nhật 4 tài liệu nghiệp vụ. Tổng tài liệu được mở rộng theo chức năng thực tế, không giữ giới hạn 12 file cũ.

## 1. Kết luận định hướng

Cần xử lý đồng thời ba phần: tính đúng của nghiệp vụ, khả năng tìm đúng bằng chứng, và cách chat hiểu/trả lời câu hỏi. Chỉ bổ sung keyword hoặc viết lại prompt không đủ để bảo đảm chất lượng. Câu hỏi mơ hồ phải được hỏi lại đúng chỗ; câu hỏi hướng dẫn không được biến thành thao tác ghi; dữ liệu hiện thời phải lấy qua API/tool có quyền.

Tài liệu mô tả code hiện tại và chính sách nghiệp vụ mục tiêu phải được phân biệt. Khi hai nguồn khác nhau, ghi rõ khoảng cách cần sửa; không mặc định code luôn đúng, cũng không đưa chính sách chưa được triển khai vào câu trả lời như một chức năng đã có.

## 2. Bằng chứng ban đầu và hạn chế

Các kết quả dưới đây là khảo sát mã nguồn/tài liệu và metadata DB local, không phải kết luận đã UAT toàn bộ chatbot:

- Bộ Phase 0 có 32 file: 12 knowledge, 10 tài liệu nền, 5 ADR và 5 file đánh giá/công cụ. 12 knowledge đều DRAFT; 11 chưa có reviewAt, một tài liệu có lịch rà soát 13/12/2026. Ngày kiểm chứng nội dung vẫn là 14/09/2026.
- Dataset có 120 case, trong đó chỉ có 21 UNACCENTED, 1 TYPO, 3 SHORT, 4 AMBIGUOUS và 4 MULTI_TURN. Tất cả case đang mang trạng thái DRAFT.
- Script `validate-phase0.ps1` kiểm tra cấu trúc artifact, giới hạn 12 knowledge và 100–200 câu hỏi; nó không gọi chatbot để chấm câu trả lời. Khi mở rộng bộ tài liệu/dataset phải sửa quy tắc kiểm tra theo manifest.
- Router hiện hạ chữ thường rồi dùng nhiều regex tiếng Việt có dấu. `VietnameseQueryNormalizer` chuẩn hóa NFC/khoảng trắng/chữ thường, chưa tạo biểu diễn không dấu dùng chung cho router.
- Nhánh nhận diện thao tác tạo phiếu được kiểm tra trước hướng dẫn. Câu có cụm “tạo phiếu” nhưng chỉ hỏi cách thực hiện có nguy cơ bị định tuyến WRITE_ACTION. Phải có kiểm thử đối chứng cho trường hợp này.
- Retrieval hybrid vector + lexical, unaccent, version index, cache, citation và tool runtime đã tồn tại. Audit Phase 0 có nhận định cũ, không thể dùng nguyên trạng làm bằng chứng hiện tại.
- Confidence hiện được tổng hợp bằng công thức từ score/độ đồng thuận/số chunk; một số tín hiệu như freshness được truyền giá trị cố định. Đây không phải xác suất câu trả lời đúng đã được hiệu chuẩn.
- Retrieval cho STATIC_KNOWLEDGE nhận câu hỏi hiện tại; phần generation có lịch sử hội thoại. Cần kiểm chứng khả năng tìm tài liệu cho câu nối tiếp như “còn trường hợp đó thì sao?”, không chỉ xem model có nhận history.
- Khảo sát DB local trước kế hoạch: 3 source; 28 bản ghi document thuộc 13 đầu tài liệu, 25 READY và 3 FAILED; index ACTIVE có 13 tài liệu/26 chunk. Có 12 tài liệu chuẩn phiên bản DB v2 và một handbook cũ. Các con số phải kiểm kê lại trước khi reset.

## 3. Bước nền: kiểm kê toàn dự án trước đợt tài liệu đầu tiên

1. Chốt snapshot code và cấu hình kiểm thử: commit/worktree, model generation/embedding, phiên bản router/retrieval/prompt/index, quyền và seed dữ liệu. Không thu thập secret vào báo cáo.
2. Lập ma trận module → người dùng → màn hình → API → use case/policy → trạng thái DB → permission → tool hiện có → tài liệu → test.
3. Bao phủ khách chưa đăng nhập, khách hàng, nhân sự/quản trị đối tác và system_admin; bao gồm tài khoản chưa duyệt, bị khóa và bị thu hồi quyền khi đang chat.
4. Kiểm kê các phần ngoài 12 tài liệu cũ: đặt chỗ, bản đồ/tìm bãi, voucher, ví, phí nền tảng, tổ chức/nhân sự, sơ đồ bãi, danh mục, thiết bị/OCR, ca làm, thông báo, đánh giá, báo cáo và quản trị AI.
5. Phân loại mỗi nghiệp vụ: đang hoạt động; có một phần; mâu thuẫn; chỉ là chính sách mục tiêu; chưa có bằng chứng. Ghi bằng chứng và bước tái hiện cho từng điểm.
6. Chạy baseline trên dữ liệu hiện tại trước khi sửa, lưu cả lỗi định tuyến, tài liệu được chọn, tool, câu trả lời, citation và thời gian từng bước. Che PII trong log.
7. Lập danh mục tài liệu cần giữ, viết lại, tách, thêm hoặc loại bỏ. Phân lô từ danh mục này; không thêm tài liệu chỉ để đủ số 4.

Đầu ra: inventory hiện trạng, ma trận truy vết, danh sách lỗi theo mức độ, manifest tài liệu dự kiến và báo cáo baseline có thể tái hiện.

## 4. Danh mục khởi đầu và các đợt 4 tài liệu

Đề xuất khởi đầu 28 tài liệu. Đây là danh mục phân tích, chưa phải cam kết mọi chức năng đều đã được hỗ trợ. Nếu một mục chứa nhiều access scope hoặc quá nhiều nghiệp vụ độc lập, tách thành nhiều tài liệu và bổ sung đợt tiếp theo.

| Đợt | Bốn tài liệu dự kiến | Trọng tâm rà soát |
|---|---|---|
| 1 | Đăng nhập và hồ sơ; Quyền riêng tư và dữ liệu cấm; Loại hội thoại và giới hạn hỗ trợ; Phiếu hỗ trợ và escalation | Tài khoản/quyền, chat khách–đối tác tách khỏi hỗ trợ hệ thống, hướng dẫn khác thực hiện hành động |
| 2 | Phương tiện khách hàng; Vé tháng/subscription; Thẻ xe và mất thẻ; Lịch sử gửi xe | Điều kiện, OWN/ALL, trạng thái, hủy/gia hạn, dữ liệu cá nhân cần tool |
| 3 | Bảng giá/tính phí; Hóa đơn/thanh toán/hoàn tiền; Voucher/khuyến mãi; Đặt chỗ và giữ chỗ | Số tiền, trạng thái thanh toán, kênh thực dùng, hết hạn, callback, retry/idempotency |
| 4 | Xe vào/ra; Tìm bãi/bản đồ; Giờ hoạt động/liên hệ/SLA; Lỗi thường gặp | Hướng dẫn người dùng, dữ liệu bãi cụ thể, lỗi có ngữ cảnh, chuyển hỗ trợ đúng lúc |
| 5 | Đăng ký đối tác/tổ chức; Nhân sự và phân quyền; Cấu trúc/sơ đồ bãi; Danh mục vận hành | Hướng dẫn nội bộ, phạm vi tổ chức/bãi, ACTIVE/INACTIVE, quyền và điều kiện trước |
| 6 | Thiết bị/QR/OCR; Ca làm và vận hành; Phí nền tảng; Ví và giao dịch ví | Phân biệt thiết bị thật/mô phỏng, lỗi nhận dạng, đối soát và ranh giới thao tác tài chính |
| 7 | Báo cáo/dashboard; Thông báo/thông tin hệ thống; Đánh giá/phản hồi; Quản trị tri thức và model AI | Nguồn số liệu, quyền xem, vòng đời nội dung, upload/duyệt/index và chẩn đoán trợ lý |

Lưu ý khi chốt phạm vi:

- Tài liệu thanh toán không mặc định chỉ có VNPAY. Đối chiếu code và cấu hình thực tế của VNPAY/SePay/ví; kênh nào chưa hoạt động phải ghi rõ.
- Ví khách hàng, ví đối tác và quản trị ví có thể phải tách. Tương tự hướng dẫn khách hàng về hóa đơn và hướng dẫn nội bộ về đối soát.
- Mỗi tài liệu có một access scope nhất quán. Không đặt hướng dẫn quản trị nhạy cảm trong tài liệu PUBLIC rồi kỳ vọng model tự che phần nội bộ.
- Dùng các scope đang có như PUBLIC, CUSTOMER, EMPLOYEE, ADMIN cùng permission thực tế. Không cấp quyền bằng tên role; không mở TENANT_PRIVATE chỉ vì phát sinh tài liệu nội bộ.
- Tên UI, đường dẫn thao tác và quyền phải được kiểm chứng. Chưa có business tool thì chỉ hướng dẫn tới module hoặc chuyển hỗ trợ, không giả lập rằng chatbot làm được.

## 5. Quy trình bắt buộc cho mỗi đợt

1. Chọn đúng 4 tài liệu từ manifest và ghi ranh giới/dependency.
2. Đọc đường đi FE → API → application/domain → DB/migration và các test liên quan; đối chiếu happy path, lỗi, trạng thái và permission.
3. Tái hiện bằng dữ liệu synthetic phù hợp cho từng nhóm người dùng. Chính sách chưa có bằng chứng đưa vào danh sách cần chốt, không đưa vào knowledge như sự thật.
4. Viết lại nội dung, thuật ngữ, FAQ, câu hỏi tương đương và điều kiện hỏi lại; kiểm tra mâu thuẫn giữa cả 4 tài liệu và các đợt trước.
5. Chuẩn bị dataset cùng đáp án/rubric và tài liệu/bằng chứng kỳ vọng trước khi tinh chỉnh router/retrieval.
6. Chạy kiểm tra tài liệu, ingestion, retrieval và trả lời thực tế trên corpus thử nghiệm của đợt. Tài liệu phụ thuộc đợt sau phải có stub kiểm thử rõ ràng hoặc trả lời hỏi lại/handoff, không viện dẫn nội dung chưa có.
7. Phân loại lỗi do tài liệu, router, retrieval, context, tool, generation hoặc UI. Chỉ đề xuất/sửa code tương ứng khi có test tái hiện và tiêu chí cải thiện.
8. Chạy hồi quy tích lũy cho toàn bộ các đợt đã đạt. Không chuyển đợt nếu còn lỗi nghiêm trọng về quyền, số tiền, trạng thái hoặc thao tác ngoài ý muốn.

Mỗi đợt bàn giao: 4 tài liệu, manifest/hash/version, ma trận truy vết cập nhật, bộ câu hỏi, transcript đã che dữ liệu, bảng điểm theo loại câu hỏi/người dùng, danh sách lỗi và quyết định tiếp theo.

## 6. Cấu trúc tài liệu phục vụ truy xuất và trả lời

Giữ một bản nguồn chuẩn trong backend tại `docs/ai-knowledge/phase-0/authoring/knowledge`; bản upload ở `docs/ai-knowledge/phase-0/knowledge` được sinh/đồng bộ từ nguồn này, có checksum và kiểm tra lệch nội dung. Không cập nhật thủ công hai bộ độc lập.

Mỗi tài liệu cần có:

- ID/slug, title, version, accessScope, owner/reviewer, ngày kiểm chứng, thời hạn rà soát, nguồn bằng chứng và phần thay thế phiên bản cũ.
- Phạm vi được hướng dẫn và phần hệ thống chưa hỗ trợ.
- Mục tiêu người dùng, điều kiện trước, vai trò nghiệp vụ và permission cần có.
- Các bước theo đúng UI hiện tại, kết quả mong đợi, trạng thái, nhánh thay thế/lỗi và cách tự kiểm tra.
- Phân biệt hướng dẫn tĩnh, dữ liệu phải đọc từ tool, thao tác cần xác nhận và trường hợp phải hỏi lại/chuyển hỗ trợ.
- FAQ ngắn với câu hỏi có dấu/không dấu, tên gọi tương đương và lỗi thường gặp. Biến thể ngôn ngữ đầy đủ đặt trong dataset/alias catalog, không nhồi lặp toàn bộ vào nội dung chunk.
- Liên kết tới tài liệu liên quan và bằng chứng Backend/Frontend/DB/test.

Chunk theo mục có nghĩa: giữ điều kiện và bước xử lý cùng nhau, không cắt rời hàng bảng trạng thái/ngoại lệ khỏi quy tắc. Metadata quản trị, frontmatter và chỉ dẫn dành cho người biên soạn không được thành nội dung trả lời. Tiền, trạng thái cá nhân, số chỗ còn lại và cấu hình thay đổi phải đọc runtime; không biến giá trị mẫu thành chính sách thật.

## 7. Hiểu tiếng Việt và câu hỏi rộng

### 7.1 Chuẩn hóa có kiểm soát

Giữ nguyên câu gốc; tạo biểu diễn chuẩn hóa Unicode/chữ thường/khoảng trắng và biểu diễn không dấu phục vụ matching/retrieval. Xử lý rõ đ/Đ. Dùng quy tắc thống nhất giữa guest và authenticated.

Alias/viết tắt phải có ngữ cảnh, ví dụ “dk”, “ko”, “k”, “dc”, “tt”. Không thay toàn cục các từ nhiều nghĩa; “gia” có thể là giá hoặc gia hạn. Không fuzzy-correct mã vé, mã hóa đơn, biển số, account ID, số tiền hay các định danh khác.

Giữ nghĩa phủ định và trạng thái: “chưa trả tiền” khác “đã trả tiền”, “không muốn hủy” khác “muốn hủy”. Việc bỏ dấu hoặc rewrite không được làm thay đổi những thông tin này. Kiểm tra an toàn trên cả câu gốc lẫn các biểu diễn chuẩn hóa.

### 7.2 Các từ hỏi không phải là ý định nghiệp vụ

“như thế nào”, “làm sao”, “như nào”, “cần làm gì”, “phải làm sao”, “lỗi” là tín hiệu cách hỏi. Cần kết hợp chủ đề, hành động, đối tượng, trạng thái và ngữ cảnh để quyết định trả lời.

| Câu hỏi/biến thể | Hành vi kỳ vọng |
|---|---|
| “đăng ký vé tháng như thế nào”, “dang ky ve thang nhu the nao” | Hướng dẫn đăng ký có điều kiện và nguồn; không tạo đăng ký |
| “vé tháng như nào”, “ve thang” | Xác định đang hỏi giá/cách đăng ký/trạng thái; hỏi lại ngắn nếu chưa đủ rõ |
| “cần làm gì khi mất thẻ”, “mat the phai lam sao” | Hướng dẫn đúng quy trình; không tự lập báo mất hoặc tạo phiếu |
| “tạo phiếu hỗ trợ như thế nào” | Hướng dẫn; không phát sinh action card chỉ vì chứa “tạo phiếu” |
| “tạo giúp tôi phiếu báo lỗi ...” | Thu thập đủ thông tin, kiểm tra quyền và yêu cầu xác nhận trước ghi |
| “đừng tạo phiếu, chỉ hướng dẫn” | Tôn trọng phủ định; không ghi dữ liệu |
| “vé của tôi duyệt chưa”, “ve cua toi duyet chua” | Đọc trạng thái qua tool OWN nếu có; không lấy câu trả lời từ RAG |
| “lỗi”, “loi”, “không được” | Dùng ngữ cảnh đã xác nhận hoặc hỏi chức năng/bước gặp lỗi; không đoán lỗi |
| “ko tt dc” | Nhận diện khả năng liên quan thanh toán, xác nhận nếu thiếu chủ đề; không đoán nguyên nhân/hoàn tiền |
| “còn bước tiếp theo?”, “vẫn lỗi”, “cái đó ở đâu” | Theo đúng chủ đề hội thoại; khi đổi chủ đề hoặc hết ngữ cảnh phải hỏi lại |

Hỏi lại tối đa 1–2 ý thiết yếu mỗi lượt, có gợi ý lựa chọn khi phù hợp. Không hỏi lại thông tin người dùng vừa cung cấp; không bắt người dùng nhắc lại toàn bộ từ đầu. Khi nhiều khả năng vẫn ngang nhau, hỏi lại tốt hơn chọn câu trả lời bằng keyword gần nhất.

## 8. Phương án cải tiến code sau phân tích

Ưu tiên sửa bằng chứng thất bại, không mặc định thay model hoặc thêm một tầng LLM.

| Mức | Thành phần cần xem xét | Điều kiện và kết quả cần đạt |
|---|---|---|
| P0 | Router/normalizer | Đồng nhất câu có dấu/không dấu; phân biệt hỏi cách làm với yêu cầu thực hiện; không tự thao tác từ câu mơ hồ/phủ định |
| P0 | Context và hỏi lại | Dùng chủ đề đã xác nhận để tạo câu truy xuất độc lập; giới hạn lịch sử; không lẫn hội thoại/người dùng/quyền |
| P0 | Knowledge access/tool authorization | Lọc quyền trước retrieval, khi đọc citation và khi thực thi/xác nhận tool; quyền bị thu hồi có hiệu lực ngay |
| P0 | Citation/answer validation | Citation phải tồn tại, được phép đọc và hỗ trợ đúng mệnh đề; không suy diễn giá, SLA, trạng thái hoặc chức năng chưa có |
| P1 | Alias, lexical + vector | Tận dụng hybrid hiện có; đo lỗi trước khi thêm typo matching hoặc reranker. Không chỉ tăng topK hoặc hạ threshold để ép có đáp án |
| P1 | Confidence | Hiệu chuẩn bằng tập gán nhãn độc lập; số chunk/freshness giả định không thay thế bằng chứng đủ để trả lời |
| P1 | Cache | Key/đọc lại phải ràng buộc scope, quyền, corpus/index/policy/model và context cần thiết; không dùng cache chung cho dữ liệu cá nhân |
| P1 | Hành vi chat | Phản hồi trạng thái nhanh, giữ câu hỏi/bản nháp, retry không trùng, tránh trạng thái “đang trả lời” vô hạn; lỗi provider khác thiếu tài liệu |
| P2 | Provider/fallback/tối ưu chi phí | Đo latency/lỗi/chất lượng của từng model; giới hạn chuỗi fallback. Chỉ đổi model sau so sánh trên cùng corpus và bộ test |

Nếu thay contract của API hoặc thêm kết quả “cần hỏi lại”, phải có kế hoạch tương thích guest/widget/trung tâm hỗ trợ, DTO, persistence nếu cần, và test tương ứng. Không coi mọi “lỗi” là một intent cố định hay từ chối mọi câu chứa “token/mật khẩu” mà bỏ qua mục đích hướng dẫn hợp lệ.

## 9. Kế hoạch kiểm thử và ngưỡng nghiệm thu

### 9.1 Bộ câu hỏi

- Mục tiêu ban đầu cho mỗi tài liệu: ít nhất 10 tình huống/nghĩa, mỗi tình huống có 6 biến thể: chuẩn có dấu, không dấu, diễn đạt khác, viết tắt/lỗi gõ, câu ngắn và hỏi theo bối cảnh.
- Tương đương khoảng 60 câu/tài liệu, 240 câu/đợt; nếu danh mục khởi đầu là 28 tài liệu thì khoảng 1.680 biến thể. Không tính các bản sao gần như giống nhau thành bằng chứng phủ thêm nghiệp vụ.
- Thêm ca nhiều lượt, chuyển chủ đề, nhiều ý định, thông tin mâu thuẫn, thiếu nguồn, tài liệu cũ, lỗi tool/provider, câu hỏi ngoài phạm vi, phủ định, prompt injection, truy cập chéo tài khoản/tổ chức và mất quyền giữa chừng.
- Giữ 120 case cũ làm regression sau khi sửa đáp án lỗi thời. Tách file biến thể theo đợt; validator đọc manifest thay vì cố định tổng 100–200.
- Tách tập dùng tinh chỉnh và tập nghiệm thu theo tình huống/ngữ nghĩa, không chia ngẫu nhiên các paraphrase của cùng một câu vào hai tập. Tập nghiệm thu có câu do người kiểm thử viết độc lập.

Mỗi case có: persona/permission, ngữ cảnh, câu hỏi, route/tool/document/section kỳ vọng, các ý bắt buộc, các điều cấm trả lời, yêu cầu hỏi lại/citation/xác nhận, trạng thái DB kỳ vọng và phiên bản corpus/model. Không bắt model trùng nguyên văn một câu đáp án.

### 9.2 Các tầng chạy

1. Artifact: metadata, manifest, link, source traceability, thiếu/mâu thuẫn/chưa duyệt, nội dung không được publish.
2. Unit: normalize, intent, alias, phủ định, hướng dẫn–hành động, hỏi lại và chính sách quyền.
3. PostgreSQL/ingestion/retrieval thật: không dấu, lexical/vector, topK, version, effective date, scope và cache invalidation. Test có DB thực, không chỉ mock repository.
4. API/tool: OWN/ALL, tài khoản chưa duyệt/khóa, idempotency, confirm/deny, quyền bị thu hồi và retry.
5. Generation thật: chạy trên corpus/model đã ghi nhận, lưu transcript/citation; chạy lặp ít nhất 3 lần cho tình huống quan trọng. Kiểm tra bằng rubric và người rà soát, không giao toàn bộ quyết định cho model chấm model.
6. Browser: guest/auth, mở/đóng widget, hỏi nối tiếp, reconnect, lỗi mạng, tải lại, mobile, đổi tài khoản; không gửi trùng hoặc lộ nội dung phiên cũ.
7. Hồi quy tích lũy và đo tải/độ trễ; phân biệt cache lạnh/nóng và tốc độ retrieval với tổng thời gian trả lời.

### 9.3 Ngưỡng mục tiêu ban đầu

Các ngưỡng chất lượng/hiệu năng sau cần chốt sau baseline, không phải cam kết đã đạt:

| Chỉ số | Mục tiêu đề xuất |
|---|---|
| Route đúng | >=98% tổng; >=95% ở từng nhóm ngôn ngữ và người dùng |
| Chênh lệch có dấu/không dấu | Không quá 2 điểm phần trăm trên tập câu có cùng nghĩa |
| Tìm đúng bằng chứng trong 5 kết quả đầu | >=95% với các câu thực sự có nguồn trong corpus |
| Đúng và đủ ý theo rubric | >=95%; báo riêng cho từng tài liệu và loại câu hỏi |
| Citation hợp lệ về ID/quyền | 100% trên tập nghiệm thu; hỗ trợ đúng nội dung được chấm riêng |
| Sai giá/SLA/trạng thái hoặc lộ dữ liệu | Không có lỗi nghiêm trọng trong tập nghiệm thu |
| Hành động ghi khi chưa xác nhận/thiếu quyền | 0 trường hợp trong bộ kiểm thử bắt buộc |
| Hỏi lại khi mơ hồ | >=95% trên bộ mơ hồ; đồng thời đo hỏi lại không cần thiết |
| Từ chối/handoff không cần thiết | Đo riêng trên câu có đủ nguồn và quyền, tránh “an toàn” bằng cách không trả lời |
| Độ trễ | Đề xuất p95 <=2 giây với cache, <=10 giây với generation; xác nhận lại theo model/hạ tầng và tải |

Không lấy tỷ lệ trung bình cao để bỏ qua lỗi tài liệu riêng lẻ. Lỗi bảo mật, thao tác ngoài ý muốn và trả sai dữ liệu tài chính/trạng thái chặn nghiệm thu đợt. Báo cả tỷ lệ lỗi và số case; 100% trên bộ test không có nghĩa bảo đảm mọi câu hỏi ngoài thực tế.

## 10. Dọn bộ tri thức cũ trên dev

### Hai lựa chọn

- Giữ/retire bộ cũ rồi chuyển index mới: dễ đối chiếu và quay lại, nhưng DB còn lịch sử.
- Purge đúng corpus dev cũ rồi nạp sạch: phù hợp nhu cầu hiện tại. Đề xuất dùng sau khi tài liệu và ingestion đã được kiểm chứng trên DB/corpus thử nghiệm; không reset sớm làm mất baseline.

### Quy trình purge đề xuất

1. Kiểm kê lại source/document/index/chunk/embedding/job/event/audit/citation, object key và dependency FK. Lập manifest ID/checksum được phép dọn; không dựa chỉ vào tên hiển thị hoặc tổng số bản ghi.
2. Export phần dữ liệu cần phục hồi và các file liên quan; thử khả năng phục hồi trên DB dev tách biệt.
3. Tạm dừng worker ingestion/index/assistant và traffic liên quan khi thay corpus; kiểm tra không còn job đang ghi dữ liệu cũ.
4. Migration/maintenance script được quản lý phiên bản, chỉ được cấu hình chạy ở dev với marker kiểm chứng môi trường/DB và điều kiện tiền kiểm. Không đặt migration xóa dữ liệu trong đường Flyway chung tự áp dụng mọi môi trường; không sửa migration đã chạy hoặc xóa flyway history.
5. Xóa theo manifest và thứ tự FK, bao gồm các bản nạp/index thất bại thuộc corpus cũ. Không dùng TRUNCATE CASCADE cả schema AI. Không đụng IAM, khách hàng, giao dịch, vé, phiếu hoặc hội thoại nghiệp vụ.
6. Xử lý citation/audit cũ có tham chiếu corpus bị xóa. Tin nhắn lịch sử phải mở được và hiển thị rõ nguồn đã cũ/không còn khả dụng; không để link lỗi hoặc âm thầm thay citation bằng tài liệu mới. Nếu muốn xóa thêm lịch sử AI dev thì đó là phạm vi dọn riêng.
7. File MinIO phải được dọn bằng thao tác theo object key đã kiểm kê, tách khỏi SQL migration. Chỉ dọn file cũ sau khi kiểm tra nạp mới và khả năng phục hồi; không xóa cả bucket.
8. Tạo manifest mới có ID/slug/version/hash, source theo scope, ingestion jobs, chunks/embedding đúng dimension/prompt/chunker và một index ACTIVE hợp lệ.
9. Chuyển cache namespace/corpus version và kiểm tra không còn câu trả lời/citation cũ. Không FLUSHALL Redis dùng chung.
10. Nạp lại/chạy lại phải không tạo bản sao ngoài ý muốn. Mô phỏng lỗi giữa chừng, rollback dữ liệu và phục hồi index, kiểm tra không có orphan hoặc sai số lượng. So khớp từng tài liệu trong manifest với dữ liệu được retrieval chọn.

Không chốt 28 hay 13 bản ghi là số cần xóa trong SQL: đó chỉ là số khảo sát tại thời điểm trước kế hoạch. Phạm vi thực tế lấy từ manifest tiền kiểm. Không coi SQL migration là đủ cho file storage, cache và lịch sử citation.

## 11. Hồ sơ cần cập nhật song song

- README và current-state-audit: tách ngày xác minh, chính sách mục tiêu, phần đã triển khai và phần còn thiếu.
- Capability/traceability: mở rộng toàn module và nội dung nội bộ.
- Intent/tool/permission matrices: ghi đúng tool thực có; câu hỏi hướng dẫn không bị gắn write intent; quyền theo effective permissions.
- Access governance: tách scope, người duyệt, vòng đời và thu hồi knowledge; không tự đổi DRAFT thành APPROVED vì validator PASS.
- Response contract: câu trả lời trực tiếp, các bước ngắn, citation kiểm chứng được, hỏi lại/handoff và lỗi hệ thống rõ ràng.
- ADR: chỉ cập nhật/thêm khi quyết định kiến trúc thay đổi, không ghi đè lịch sử quyết định.
- Evaluation: schema/manifest, phân bố câu hỏi, baseline, scorecard mỗi đợt và báo cáo cuối cùng.
- Runbook dev reset/reimport, kiểm tra rollback và quy trình làm mới khi nghiệp vụ đổi.

## 12. Thứ tự thực hiện và điều kiện kết thúc

Thứ tự: kiểm kê + baseline → đợt 1 và các cải tiến nền có bằng chứng → các đợt 2–7 kèm hồi quy tích lũy → chốt tài liệu phát sinh → rehearsal nạp mới/reset trên DB thử → dọn/nạp dev → kiểm thử toàn luồng với model thật → bàn giao scorecard và runbook.

Chỉ kết thúc khi manifest bao phủ các chức năng trong phạm vi; tài liệu không mâu thuẫn với hành vi đã xác minh; có câu trả lời thực tế đạt ngưỡng; lỗi quan trọng đã đóng; corpus/index/cache nhất quán; và các capability chưa hỗ trợ được hướng dẫn/handoff rõ ràng. Không đánh dấu hoàn thành chỉ vì file đủ số lượng, migration chạy xong hoặc unit test xanh.

## Nguồn kỹ thuật tham khảo

- [PostgreSQL unaccent](https://www.postgresql.org/docs/current/unaccent.html) — cơ sở đối chiếu matching không dấu; không tự giải quyết mơ hồ về ý nghĩa.
- [PostgreSQL pg_trgm](https://www.postgresql.org/docs/current/pgtrgm.html) — lựa chọn xem xét cho typo/similarity sau khi đo lỗi, không mặc định phải bổ sung.
- [pgvector hybrid search](https://github.com/pgvector/pgvector#hybrid-search) — đối chiếu kết hợp vector/full-text và đánh giá truy xuất hiện có.

## Cập nhật thực hiện ngày 19/11/2026

Đã soạn lại tám tài liệu trong hai đợt đầu, giữ DRAFT; có tổng 13 nguồn knowledge, 480 câu mới và baseline 120. Bản chuẩn và bằng chứng đặt trong backend `docs/ai-knowledge/phase-0/authoring/`; không thêm theo dõi thư mục tài liệu cũ ở root. Quy tắc đọc chính chủ, tốc độ, fallback và trả lời từ FAQ đã được cải tiến trong code. Kết quả và giới hạn kiểm chứng ghi tại [báo cáo đợt 2](phase-0/authoring/evaluation/batch-02-report.md).

Chưa dọn/nạp hoặc publish knowledge vào DB dev; các đợt tiếp theo, kiểm thử corpus dev thật và UAT vẫn theo kế hoạch. Số tài liệu không bị giới hạn bởi danh mục cũ; manifest lấy batch từ cấu hình thay vì hardcode số lượng chung.

Nghiệm thu live của source/code cuối còn mở: Google xác nhận hết quota embedding miễn phí 1.000 yêu cầu/ngày. Không chạy tiếp API hoặc tự đổi embedding model; đã bàn giao lệnh phát lại sau khi quota khôi phục và lưu các ca chưa đạt trong báo cáo đợt 2.

## Cập nhật thực hiện ngày 20/11/2026

Đã triển khai cấu hình Gemini nhiều key/project, duyệt model được phép trên key hiện tại trước khi chuyển key; embedding chỉ đổi credential, không đổi model/dimension/index. Chính sách vận hành, các loại lỗi, cooldown, bảo mật và giới hạn nhiều instance ghi tại [runbook](GEMINI_CREDENTIAL_FAILOVER.vi.md), cùng [ADR-006](phase-0/authoring/adr/ADR-006-gemini-credential-and-evidence-failover.md) DRAFT.

Đã phân tích và cập nhật bốn tài liệu đợt 3: giá/tính phí; hóa đơn/thanh toán/hoàn tiền; voucher; đặt chỗ/giữ chỗ. Thêm hai nguồn mới, tổng 15 nguồn và 12 tài liệu thuộc các đợt. Dataset có 840 câu, gồm baseline 120 và ba đợt, mỗi đợt 240. Có 40 tình huống mới, truy vết backend/UI/quyền và rubric riêng. Không giới hạn số tài liệu theo danh mục cũ.

Code đã cải tiến lọc câu cấm gửi bí mật, chọn đoạn dẫn chứng trong ngân sách cũ, hướng dẫn các yêu cầu chưa có business tool, giữ nghĩa phủ định khi kiểm tra kết quả. Giữ nguyên chính sách giá, giao dịch, voucher và đặt chỗ đã có. Đặt chỗ và SePay có code, nhưng chatbot chưa có tool để tự thực hiện các nghiệp vụ đó.

Kết quả unit/integration và transcript Gemini/SQL thực tế, độ trễ, tình trạng key và lỗi còn mở được chốt tại [báo cáo đợt 3](phase-0/authoring/evaluation/batch-03-report.md). Lượt live dùng DB Testcontainers riêng, các actor/chat/business ports có fixture, có tái sử dụng vector thật theo nội dung; không thay thế UAT ứng dụng đang chạy. Không lấy số liệu chạy trước sửa làm kết quả của code mới.

Chưa dọn/nạp hoặc publish corpus dev, chưa gọi thanh toán/hoàn tiền/đặt chỗ thật. Các tài liệu đợt 1–3 vẫn DRAFT; nghiệm thu nội dung, UAT guest/nhân sự, kiểm chứng callback và cạnh tranh tài chính thực tế còn theo gate riêng. Phần thiếu bằng chứng hoặc chưa đạt ngưỡng không bị xóa khỏi kế hoạch.


## Tiến độ đợt 4 — 23/11/2026

Đã đối chiếu bốn chủ đề cuối: xe vào/ra, tìm bãi/bản đồ, địa điểm/giờ/liên hệ và lỗi thường gặp. Bổ sung nguồn chuẩn tìm bãi, đưa corpus lên 16 tài liệu; dataset tích lũy 1.104 câu với 264 câu đợt 4. Không bổ sung SOP nhân viên vào PUBLIC.

Mỗi đợt phải được nghiệm thu lại khi phát hiện lỗi runtime: hiện đã sửa nối chủ đề, chuẩn hóa tìm kiếm, chọn bằng chứng theo bước và che liên hệ công khai. Giữ nguyên các lượt thất bại và hash trước sửa, không lấy HTTP 200 hay job COMPLETED làm điểm nội dung. Xem [báo cáo đợt 4](phase-0/authoring/evaluation/batch-04-report.md) để biết gate thực đạt và phần còn mở.

Nguồn/export vẫn DRAFT, chưa nạp hoặc publish vào DB dev. Không cần migration/reset dữ liệu cho đợt này: thay đổi nằm ở nguồn tri thức, runtime chọn/kiểm tra câu trả lời và giao diện liên hệ. Việc đưa corpus lên hệ thống dùng luồng quản trị đã có, sau khi người có quyền duyệt.

### Theo dõi đợt4 ngày 24/11/2026
Bốn nguồn chính và264 câu mới đã đối chiếu code/UI; corpus16 nguồn/1104 câu, toàn bộ nguồn giữ DRAFT. Bộ backend cuối4220 test không lỗi(1skip), Python12PASS, frontend lint/regression/contactbrowserPASS trên source tương ứng. Các kiểm tra này không thay thế nghiệm thu nội dung.
Live cuối dùng cùng hash v14 và snapshot9model đã được ứng dụng cho phép, chia các phần rời nhau; không gộp revision cũ. Provider timeout/5xx/quota vẫn có thể làm câu trả lời trống, tính FAIL. Xem [báo cáo cuối đợt4](phase-0/authoring/evaluation/batch-04-report.md) cho bảng theo nguồn/biến thể, tốc độ, key và các gate còn mở. Không reset/nạp/publish DB dev.
