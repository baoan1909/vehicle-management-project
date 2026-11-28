# Cấu hình nhiều key và model dự phòng Gemini

Backend giữ key đang hoạt động cho từng nhóm tác vụ. Với chat, hệ thống thử các model đủ điều kiện trên key hiện tại trước khi chuyển sang key kế tiếp. Không chia tải vòng qua tất cả key. Với embedding, hệ thống giữ nguyên model và dimension của index, chỉ thay credential còn dùng được.

## Cấu hình

Giữ `GEMINI_API_KEY`, `GEMINI_BASE_URL` và bổ sung `GEMINI_PROJECT_ID`. Key dự phòng dùng các cặp `GEMINI_API_KEY_1` / `GEMINI_PROJECT_ID_1`, tiếp tục `_2`, `_3`… Không giới hạn ở năm key và chấp nhận chỉ số không liên tục. Xem `.env.example` của backend; không lưu key thật trong file mẫu.

Ứng dụng đọc Spring Environment. File `.env` cần được launcher/IDE nạp vào môi trường; code nghiệp vụ không đọc lại file trên từng request. Script kiểm thử live cung cấp cách nạp `.env` vào tiến trình con mà không đưa bí mật lên dòng lệnh.

Project ID phải là project thực sở hữu key. Các key cùng project dùng chung trạng thái quota tương ứng. Thiếu Project ID hoặc còn giá trị ví dụ `project-a`… được xếp vào cùng nhóm chưa xác định để tránh giả định chúng có quota độc lập. Project ID này là metadata cấu hình, không phải kết quả xác minh quyền của Google.

Key rỗng, placeholder và trùng nhau bị loại khỏi danh sách gọi. Báo cáo chỉ ghi tên biến. Giữ cấu hình cũ một key vẫn sử dụng được; thay đổi credential cần khởi động lại ứng dụng để nạp lại cấu hình.

## Model và chi phí

Danh sách generation lấy từ registry hiện có, sau chính sách ACTIVE/FALLBACK, rollout, quyền dùng và phê duyệt free tier. Model xuất hiện trong API danh sách chưa có nghĩa được phê duyệt hoặc miễn phí trong mọi project. Không tự thêm model chưa duyệt vào đường gọi; quản trị cấu hình model phải thực hiện bằng quy trình hiện có. Khi dùng chế độ không trả phí, giữ kiểm tra `freeTierApproved`.

Không bật billing, không tạo project, không thay key ngoài danh sách được cung cấp. Không chuyển embedding model tự động trên index đang dùng. Nếu cần Embedding 1, phải chuẩn bị và nghiệm thu index riêng; bản thay đổi này chỉ chuyển key cho cùng model embedding để không làm sai kết quả tìm kiếm.

## Thứ tự và giới hạn

- Generation lập kế hoạch theo key trước, model sau; lựa chọn thành công được giữ trong bộ nhớ theo use case.
- Embedding chọn credential riêng, cùng embedding model/dimension đang cấu hình.
- Key lỗi xác thực bị tạm ngừng trên mọi tác vụ của chính key đó.
- Quota ngày bị tạm ngừng theo project/model/tác vụ. Không thử key cùng project để gọi lại quota đã hết.
- Quota phút và lỗi tạm thời dùng cooldown, xét Retry-After/RetryInfo; không retry dồn tại HTTP adapter. Worker vẫn giữ lịch retry hữu hạn sẵn có.
- Lỗi request chung và safety block không quét sang key/model khác để tìm cách vượt lỗi/chặn.
- Lỗi không có quyền riêng một model không vô hiệu hóa mọi model của key.

Mặc định `GEMINI_MAX_ATTEMPTS=20`, `GEMINI_CALL_BUDGET=12s`. Generation còn chịu timeout hiện có; mỗi lần gửi chỉ được dùng phần thời gian còn lại. Hết ngân sách request không có nghĩa mọi key đã lỗi. Các ứng viên đã cooldown được bỏ qua khi lập kế hoạch sau để tránh kẹt ở đầu danh sách.

Khi một model chỉ còn được cấp thời gian ngắn vì model trước đã dùng gần hết deadline, timeout đó được ghi là hết ngân sách request, không tự làm xấu sức khỏe model mới. Lỗi tạo request ngay tại ứng dụng dừng trước HTTP và không chuyển qua mọi key. Bộ đếm chỉ tăng khi thực sự bắt đầu HTTP send.

`GEMINI_GENERATION_MIN_INTERVAL=5s` và `GEMINI_EMBEDDING_MIN_INTERVAL=650ms` điều tiết theo project/model/tác vụ trong một instance. Có semaphore theo nhóm, không giữ khóa toàn cục khi chờ HTTP. Đây không phải giới hạn tốc độ tổng cho mọi ứng dụng cùng Google project.

## Trạng thái và phục hồi

Availability được lưu qua distributed-cache port hiện có, kèm bộ nhớ cục bộ khi Redis không dùng được. Quota ngày tính mốc phục hồi theo America/Los_Angeles; lỗi giới hạn không xác định không được tự gán thành quota ngày. Lỗi xác thực được giữ tối đa một ngày; lỗi quyền/model năm phút; lỗi ngắn hạn ít nhất ba mươi giây và tôn trọng retry hint trong giới hạn một ngày.

Sau restart, cooldown còn trong Redis vẫn được đọc; lựa chọn sticky và nhịp gửi cục bộ được thiết lập lại. Khi Redis không hoạt động, trạng thái và điều tiết chỉ bảo vệ instance hiện tại. Nhiều instance có thể cùng probe khi cooldown hết; chưa triển khai bộ điều tiết tốc độ tập trung xuyên instance. Không coi đây là quota chính xác của Google.

## Kiểm tra và bàn giao — 25/11/2026

Các wrapper Python kiểm tra key, smoke, retrieval và acceptance cũ đã được gỡ khi dọn repository. Những lệnh Python trong báo cáo trước ngày này là lịch sử, không còn là hướng dẫn chạy. Việc gỡ wrapper không thay đổi cấu hình key hoặc cơ chế đổi model/key trong Java.

Không có kết quả kiểm tra quota Google mới trong lượt dọn này. Các kết quả cũ chỉ có giá trị trong phạm vi lượt chạy đã ghi; việc đọc được danh sách model không chứng minh còn quota sinh câu trả lời hoặc embedding. Chatbot hiện chỉ có Google API; không còn chế độ local.

Công cụ đồng bộ tài liệu hiện dùng `node scripts/sync-ai-knowledge.mjs`; kiểm thử Google dùng các test Java hiện có và phải được bật riêng khi cần live. Xem [hướng dẫn bàn giao](TEAM_HANDOFF.vi.md) để biết lệnh hiện tại, yêu cầu tài nguyên và phần nghiệm thu còn thiếu. Các bằng chứng lịch sử được giữ nguyên; không suy kết quả kiểm tra hiện tại từ log cũ.

## Bảo vệ bí mật

Request gắn credential vào header riêng; không thay key trên HTTP client dùng chung. Response được loại các key đã cấu hình trước khi mapper hoặc audit xử lý. Exception từ catalog không đính kèm nguyên nhân có thể chứa bí mật. Không đưa key vào DTO, log, `.env.example`, frontend hoặc evidence.

Bộ lọc nguồn phân biệt lời cấm gửi bí mật với chỉ dẫn gửi bí mật; vẫn kiểm tra riêng các chỉ dẫn dương tính xuất hiện sau lời cấm. Chính sách safety-v2 tham gia fingerprint cache trả lời để không dùng lại đáp án theo bộ lọc cũ. Lọc nguồn chỉ là một lớp bảo vệ; vẫn giữ kiểm tra quyền, cô lập context, chính sách tool và kiểm tra đầu ra.

Guard đầu ra `output-validation-v3` phân biệt “không phải bằng chứng để xác nhận đã thanh toán” với khẳng định đã thực hiện thanh toán. Phiên bản guard tham gia fingerprint cache; không cần xóa toàn bộ Redis. Bộ test phát lại 136 câu trả lời provider đã ghi để kiểm tra nhánh khẳng định hành động, kèm câu dương tính đối chứng và lượt live sau sửa. Replay không tạo thêm lời gọi API và không được tính là lượt provider mới.

Nguồn chính thức: [quota](https://ai.google.dev/gemini-api/docs/rate-limits), [model metadata](https://ai.google.dev/api/models), [embedding migration](https://ai.google.dev/gemini-api/docs/embeddings#migration).


## Đối chiếu registry khi nghiệm thu — 24/11/2026

Harness cũ dùng hai model fixture và giới hạn ba model, nên không phản ánh đầy đủ registry của ứng dụng. Các số đo đó chỉ có giá trị cho cấu hình fixture đã ghi. Không dùng chúng để kết luận mọi model/key đang có đều không dùng được.

Script live nay nhận `--model-registry <snapshot.json>` từ bản xuất chỉ đọc các model SUPPORT_CHAT đã được duyệt. Chỉ nhận ACTIVE/FALLBACK có `freeTierApproved` dạng boolean true; giữ API version, temperature, token limit, priority và rollout. Không tự duyệt model trong catalog, không đổi dữ liệu dev. Harness dùng AiModelRouter/AiModelPolicyService thật để áp chính sách và thứ tự trên dữ liệu snapshot, không cắt danh sách ở ba model. Bản snapshot đã lọc được lưu cùng mỗi lượt chạy và có checksum; embedding vẫn giữ model/dimension/index cũ.

Lượt đối chiếu registry dev ngày 24/11 cho thấy một ACTIVE và tám FALLBACK đã có cờ phê duyệt. Đây là trạng thái cấu hình của ứng dụng, không phải kiểm chứng mới về giá/quota của Google hay bằng chứng tất cả model đều gọi thành công. Snapshot và kết quả từng alias/model phải được ghi trong báo cáo nghiệm thu.

## Ngân sách Google sau smoke đợt 5 — 27/11/2026

Smoke thực tế ghi nhận Google generation trả HTTP 200 sau khoảng 16 giây, trong khi deadline cũ 4 giây đã kết thúc ở caller. Mặc định mới: generation tối đa 20 giây (vẫn bị cap bởi request-timeout), ngân sách provider plan 60 giây. Cấu hình môi trường rõ ràng vẫn được tôn trọng. Riêng .env cục bộ đang ghi đè 4s đã được đổi đúng dòng timeout sang 20s, không thay key hoặc cấu hình DB; .env vẫn không đưa vào Git. Retry/cooldown, project sharing và số attempt vẫn hữu hạn. Đây là đánh đổi độ trễ để cho model đang được duyệt đủ thời gian trả lời; không khẳng định mọi request cần 20/60 giây.

Proxy quan sát trong harness dùng HTTP handlers đồng thời để không tạo head-of-line blocking khi một lượt trước đã timeout; bộ chạy case/worker vẫn tuần tự. Case/HTTP/cohort lưu riêng; kết quả 4/12 giây cũ không được gộp với cohort mới.
