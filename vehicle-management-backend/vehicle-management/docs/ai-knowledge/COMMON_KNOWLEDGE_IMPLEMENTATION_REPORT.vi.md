# Báo cáo triển khai nền tảng trả lời chung

> Báo cáo lịch sử: tích hợp local và bộ chạy tương ứng đã được gỡ ngày 27/11/2026. Các số đo dưới đây chỉ áp dụng phiên bản cũ. Dùng TEAM_HANDOFF.vi.md trong docs/ai-knowledge để vận hành Google API hiện tại.


Ngày 24/11/2026. Code đã triển khai và có gate offline; **chưa nghiệm thu toàn bộ chất lượng chatbot hoặc xác nhận Google/Qwen có độ chính xác tương đương**. Chưa áp dụng migration, nhập corpus DRAFT, publish tài liệu hoặc kích hoạt index trong DB dev bằng công việc này.

## Luồng trước và sau

Trước: hướng dẫn tĩnh phải có ACTIVE index và embedding thành công, rồi mới chọn FAQ. Embedding/quota lỗi làm mất cả khả năng trả đáp án đã có.

Sau: kiểm tra an toàn/ý định/quyền/ngữ cảnh → tìm FAQ đã duyệt bằng DB → nếu khớp rõ và không xung đột, kiểm tra đầu ra và nguồn lần nữa rồi trả trực tiếp; không qua model hoặc cache model. Nếu chưa khớp, giữ đường tìm nguồn/tổng hợp của provider đang chọn. Dữ liệu cá nhân, trạng thái thực và thao tác giữ đường backend phân quyền hiện có.

Đường chung không cần Google key, LM Studio hoặc vector index tương ứng hoạt động. Các cờ trợ lý/dữ liệu/quyền vẫn giữ nguyên. PREPARING không chặn toàn bộ assistant job; SWITCHING vẫn chặn theo epoch. Mất kết nối DB vẫn làm đường chung không khả dụng, không được gọi đó là thiếu kiến thức hay tự gọi provider khác để cứu.

## Thành phần thay đổi

| Nhóm | Thay đổi và mục đích |
| --- | --- |
| Domain | CanonicalQuestionKey và GroundedFaqLookup: dùng chung chuẩn hóa, chỉ khớp bảo thủ, giữ ý hỏi tiếp/phủ định/số/kiểu câu hỏi, không nhầm lỗi liên hệ thành hỏi số điện thoại |
| Domain | CommonKnowledgeCandidate và GroundedContextPack.canonical: evidence chuẩn độc lập ngân sách model |
| Application | CommonKnowledgeService và CommonKnowledgePortOut: chỉ có phụ thuộc DB, giới hạn ứng viên, fingerprint và kiểm tra nguồn mới nhất |
| Persistence | SQL trên KnowledgeChunkRepository, adapter và MapStruct: dùng unaccent GIN, duyệt/quyền/tenant/hiệu lực/phiên bản; chỉ lấy block FAQ thực sự trong phạm vi chunk |
| Chat | KnowledgeRetrievalService tìm FAQ trước embedding/index; GuestPublicAssistantService và AssistantOrchestrator trả nguyên đáp án có citation, kiểm tra lại nguồn, không generation khi nguồn/đầu ra bị chặn |
| Audit/API | retrieval_method/source_snapshot và MapStruct/DTO; không tạo ai_run, HTTP giả hoặc index ID giả cho đường chung |
| DB | V20261124170000__shared_reviewed_faq_audit.sql thêm cột audit, không nhập/công bố/đổi dữ liệu model |
| FE | Thông báo PREPARING và trang thử tìm nguồn phân biệt FAQ chung với tìm kiếm AI; không hiển thị lexical score như phần trăm chính xác cho FAQ chung |
| Cấu hình | AI_COMMON_FAQ_ENABLED trong application.yaml/.env.example; .env thật không bị sửa |

Không tạo kho đáp án song song hoặc model thứ ba. Nội dung FAQ thừa hưởng phê duyệt tài liệu; không tự coi mọi đoạn mô tả hoặc Qwen output là đáp án đúng. Chunk cũ thiếu block nguồn/range không được tự chuyển thành FAQ đã duyệt.

## Kiểm chứng thực tế

| Nhóm | Kết quả và giới hạn |
| --- | --- |
| Backend offline | **205 test qua, 0 failure/error**, log offline-blocks-final.log; gồm corpus, canonical matcher, retrieval, guest/member, scheduler, audit mapper, mode service và hồi quy Gemini model/key |
| Corpus offline | 1 test duyệt **984 case** của bốn batch hiện có, trên 16 tài liệu DRAFT fixture, không publish hoặc dùng vector giả |
| Không gọi model ở đường chung | Unit kiểm tra không tương tác index/config/embedding/provider/router ở common hit; guest kiểm tra hai chế độ PREPARING trả cùng nội dung; member không tạo ai_run và giữ citation |
| SQL/migration | Đã tạo DB Testcontainers riêng và áp dụng **165 migration** đến V20261124170000 thành công. Lượt test sau đó dừng do fixture inject concrete adapter qua transaction proxy JDK; đã sửa sang port interface. Các assertion truy vấn chưa được xác nhận PASS trong lượt này |
| SQL chạy lại | Guard chặn trước Maven/container khi RAM chỉ còn 343 MB rồi 465 MB < ngưỡng 1024 MB; không dùng kết quả này thay cho DB test PASS |
| FE cú pháp | Esbuild kiểm tra cú pháp 3 file TS/TSX thay đổi qua; không phải full typecheck hoặc browser test |
| Full TypeScript | Lượt heap 384 MB dừng vì JavaScript heap out of memory; lượt guarded tiếp theo không bắt đầu do RAM chỉ 441 MB. Không có bằng chứng full TypeScript PASS cho bản này |
| Browser/Qwen live | Chưa chạy đợt này vì điều kiện tài nguyên và các gate còn chờ; không dùng kết quả đợt trước để công nhận bản mới |
| Google live | **Không gọi Google API thật**, kể cả catalog; không tiếp tục lượt Gemini đang tạm dừng. Log quota/auth trong unit là fixture mô phỏng, không phải kết luận key thật đang lỗi |

Context DB test được tách nhỏ, không nạp toàn BE hoặc AI HTTP clients. Không dừng ứng dụng/container dev, không đổi cấu hình Windows/LM Studio và không ép mở live dưới ngưỡng tài nguyên. Các lỗi compiler/test fixture đã được sửa và giữ log; không hạ assertion, strictness hoặc tiêu chuẩn nội dung để công nhận đạt.

## Kết quả trên bộ câu hỏi

- Route trùng nhãn mong đợi **984/984** trên fixture đã có. Đây là kết quả resolver offline, không phải 984 câu chat đầu cuối đã trả đúng.
- Trong **780 case mong đợi kiến thức tĩnh**, matcher khớp trực tiếp **209 case (26,795%)**. **207** lượt qua output validator; 2 lượt bị chặn do UNVERIFIED_ACTION_SUCCESS_CLAIM.
- Hai lượt còn bị giữ vì cùng khóa FAQ nhưng khác văn bản đáp án. Đây là xung đột cần rà soát; chưa kết luận hai nguồn trái ngược nghiệp vụ chỉ vì khác chữ.
- Một lượt có source slug khác kỳ vọng: hướng dẫn lỗi đăng nhập lấy từ tài liệu xử lý lỗi thay tài liệu tài khoản. Có khả năng nguồn chồng lặp hợp lệ; cần đối chiếu nội dung, không tự chấm đúng/sai chỉ theo slug.
- Chênh lệch mức bao phủ FAQ giữa có dấu/không dấu là **0 điểm phần trăm** trong fixture này. Không chuyển số này thành chênh lệch độ chính xác model.
- Các chẩn đoán thiếu chuỗi requiredIdeas chỉ là dấu hiệu để rà soát. Không dùng việc có đủ keyword để chấm đủ ý, hoặc việc không có đúng chữ để kết luận sai ngữ nghĩa.
- p50/p95/max thời gian domain matcher trên lượt offline cuối khoảng **10,589 / 19,517 / 81,031 ms**. Thời gian bao gồm lọc fixture trong RAM; không có truy vấn DB, embedding hoặc generation. Không coi là latency chat production hoặc benchmark Qwen/Google.

Hit@5 SQL, đúng/đủ ý từng tài liệu >=95%, dữ liệu không lộ 100%, tương đương model và tốc độ chat thực **chưa được công nhận** từ bộ đo này. Không loại các case không khớp hoặc lỗi ra khỏi mẫu số. Phần chưa khớp FAQ vẫn có thể cần nguồn/model hoặc hỏi lại, không đồng nghĩa thất bại nội dung của toàn chatbot.

## Lỗi/giới hạn còn mở

1. Chạy lại CommonKnowledgePersistenceTest trên DB cô lập sau sửa transaction proxy; cần xác nhận FTS không dấu, quyền ba tầng, tenant, phiên bản/hiệu lực, block hỗn hợp và revalidation. Migration đã áp dụng ở DB test không thay thế các assertion này.
2. Chạy full TypeScript và Playwright tuần tự khi đủ RAM. Cú pháp FE hiện đã kiểm tra riêng.
3. Rà soát hai xung đột FAQ và hai phản hồi điều kiện nghiệp vụ bị validator chặn. Giữ cơ chế chặn hiện tại; không nới bộ kiểm tra để tăng điểm. Không tự publish bản sửa tài liệu.
4. Mức bao phủ FAQ còn hạn chế, nhất là chủ đề chưa có dòng Q/A đủ rõ. Tiếp tục biên soạn từ code và quy tắc nghiệp vụ, có người duyệt, không thêm phân nhánh theo case ID/held-out.
5. Chưa có kiểm thử tải/latency SQL thực hoặc live model cho thay đổi này. Bộ guard tài nguyên phải được tôn trọng; không tăng heap, retry hoặc pool để che vấn đề chạy máy.
6. Qwen tool calling/ảnh/PDF vẫn ngoài phần đã nghiệm thu từ đợt trước. Đợt này không mở quyền mới hoặc giả lập công cụ thành công.

## Tiếp tục triển khai và nghiệm thu

Xem [runbook tri thức chung](COMMON_KNOWLEDGE_RUNTIME.vi.md). Hoàn tất các gate còn chờ trước khi triển khai phục vụ thật. Sau khi migration/code được triển khai theo quy trình dự án, chỉ tài liệu đã duyệt có block nguồn phù hợp mới dùng chung được; không cần xây lại vector chỉ để dùng FAQ.

Google sau này vẫn cần kiểm thử riêng đường embedding/index, tổng hợp, schema và các nhóm điều kiện nghiệp vụ/nguồn nhạy cảm. Có thể dùng cùng corpus, đáp án tham chiếu và rubric, kiểm tra mẫu trọng yếu trước để tiết kiệm quota. Kết quả Qwen hoặc câu FAQ dùng chung không được gắn thành điểm PASS của Google.

[Evidence và hash](evidence/common-knowledge-2026-11-24/README.md) lưu log, corpus fixture, chẩn đoán và phần chưa chạy. Không commit/push/deploy, thay key thật, thanh toán, hoàn tiền, đặt chỗ, điều khiển cổng hoặc gửi thông báo ngoài.


## Lượt rà soát 16 tài liệu tiếp theo — 24/11/2026

Đã sửa nội dung và đồng bộ nguồn DRAFT của cả bốn đợt; sửa bộ thử Qwen để fixture có dấu duyệt/block provenance và phân biệt FAQ không gọi model với generation. Báo cáo này giữ nguyên các lượt đo cũ làm lịch sử; không coi chúng là nghiệm thu corpus/harness mới. Xem [báo cáo tổng hợp](ALL_BATCHES_REVIEW_STATUS.vi.md) và [evidence mới](evidence/all-batches-review-2026-11-24/README.md). Các gate Java/SQL/full TypeScript/browser/live còn chờ tài nguyên; không gọi Google trong lượt này.
