> Đợt 5 — 27/11/2026: 21 tài liệu DRAFT, registry 1.344 câu (1.104 cũ + 240 mới); holdout 40, bổ sung cũ 24 và 4 probe giữ riêng. Xem [báo cáo đợt 5](evaluation/batch-05-report.md). Các mốc đợt 4 bên dưới là lịch sử.

# Phase 0 — Chốt nghiệp vụ và baseline trợ lý AI

> Đợt 4 — 23/11/2026: 16 tài liệu, bốn đợt và 1.104 câu. Xem [báo cáo đợt 4](evaluation/batch-04-report.md); mọi bản tri thức đang làm mới vẫn DRAFT và chưa publish.

> Lịch sử đợt 3 — 20/11/2026: nguồn hiện có 15 tài liệu, 12 tài liệu được làm mới qua ba đợt và 840 câu tích lũy. Xem [báo cáo đợt 3](evaluation/batch-03-report.md). Metadata APPROVED của các quyết định baseline bên dưới không có nghĩa corpus DRAFT đã được nghiệm thu hoặc publish. Cấu hình nhiều key/model và giới hạn vận hành được ghi ở [runbook](../../GEMINI_CREDENTIAL_FAILOVER.vi.md).

> Cập nhật đợt 1 — 19/11/2026: xem [bằng chứng/truy vết](evaluation/batch-01-traceability.md), [manifest](knowledge-manifest.json) và [báo cáo đợt 1](evaluation/batch-01-report.md). Baseline 14/09/2026 được giữ để truy vết; các nhận định runtime có delta dưới đây thay thế nhận định cũ tương ứng. Chưa tự phê duyệt/publish knowledge.


| Thuộc tính | Giá trị |
|---|---|
| Phiên bản | 1.0.0 |
| Trạng thái | APPROVED — 24 quyết định nghiệp vụ đã phê duyệt; TBD-013 DEFERRED/OUT_OF_SCOPE |
| Ngôn ngữ | Tiếng Việt (UTF-8) |
| Ngày kiểm chứng mã nguồn | 2026-09-14 |
| Ngày xác nhận quyết định | 2026-09-14 |
| Nguồn sự thật | Backend, Frontend và Flyway migrations trong repository Vehicle Management |

## Vị trí nguồn chuẩn từ 19/11/2026

Bộ này đã được chuyển vào `vehicle-management-backend/vehicle-management/docs/ai-knowledge/phase-0/authoring` theo yêu cầu. Thư mục `docs/ai-assistant` ở root là bản khảo sát cũ, tiếp tục bị Git ignore và không được dùng bởi kiểm thử. Sửa knowledge tại đây; script backend sinh bản upload ở `../knowledge/`. Không duy trì hai bộ soạn thảo độc lập.

## Mục tiêu

Bộ tài liệu này chốt baseline nghiệp vụ, định tuyến intent, quyền truy cập, hợp đồng phản hồi, kiến trúc RAG và dữ liệu đánh giá cho trợ lý hỗ trợ CoParking. Phase 0 không triển khai pgvector, embedding, ingestion worker, API quản trị tài liệu hay thay đổi runtime.

## Nguyên tắc đã chốt

1. Kiến thức tĩnh về hướng dẫn, quy trình và chính sách dùng RAG và phải có citation đã được Backend xác minh.
2. Dữ liệu cá nhân hoặc trạng thái thời gian thực dùng business tool; RAG không phải nguồn sự thật.
3. Mọi thao tác ghi hoặc đổi trạng thái dùng action card, xác nhận người dùng, kiểm tra permission/ownership và idempotency.
4. Thiếu bằng chứng, tài liệu mâu thuẫn hoặc tool thất bại thì không suy đoán; đề nghị tạo phiếu hoặc chuyển nhân viên.
5. Quyền được kiểm tra theo permission code. Không hard-code role trong Java hoặc React.
6. Access control được áp dụng trước retrieval. Multi-tenancy chưa thuộc phạm vi hiện tại; `TENANT_PRIVATE` bị vô hiệu hóa cho đến khi có quyết định và kiến trúc tenant riêng.
7. Secret và dữ liệu thanh toán nhạy cảm không được đưa vào prompt, log, RAG hay dataset.

## Phạm vi nghiệp vụ

1. Đăng ký, đăng nhập, Google OAuth, quên mật khẩu và hồ sơ.
2. Quản lý phương tiện khách hàng.
3. Vòng đời vé tháng/subscription.
4. Bảng giá và tính phí.
5. Hóa đơn, VNPAY, lỗi thanh toán và hoàn tiền.
6. Xe vào/ra.
7. Thẻ xe và báo mất thẻ.
8. Lịch sử gửi xe.
9. Phiếu hỗ trợ, hội thoại, phân công và escalation.
10. Quyền riêng tư và dữ liệu bị cấm.
11. Lỗi thường gặp.
12. Địa điểm, giờ hoạt động, SLA và kênh liên hệ.

## Danh mục artifact

- [Hiện trạng](current-state-audit.md)
- [Bản đồ năng lực](business-capability-map.md)
- [Ma trận truy vết](business-traceability-matrix.md)
- [Ma trận intent](intent-routing-matrix.md)
- [Ma trận tool và quyền](tool-permission-matrix.md)
- [Access scope và quản trị](access-scope-and-governance.md)
- [Hợp đồng phản hồi](assistant-response-contract.md)
- [Decision log](decision-log.md)
- [Câu hỏi nghiệp vụ mở](open-business-questions.md)
- [ADR](adr/)
- [Tài liệu tri thức](knowledge/)
- [Bộ câu hỏi vàng](evaluation/golden-questions.jsonl)
- [Báo cáo phân bổ](evaluation/golden-question-summary.md)
- [Báo cáo kiểm tra dataset](evaluation/dataset-validation-report.md)

## Quy ước trạng thái bằng chứng

- `CONFIRMED`: BE và DB thống nhất; FE nếu có cũng phù hợp.
- `PARTIAL`: có triển khai một phần hoặc chỉ có một nguồn kỹ thuật.
- `CONFLICT`: các nguồn không thống nhất hoặc tên gọi gây hiểu sai.
- `TBD`: chưa có bằng chứng đủ; cần Product Owner/Business Owner xác nhận.

## Gate Phase 0

Gate artifact Phase 0 là `PASS` và sổ quyết định nghiệp vụ đã được xác nhận ngày 2026-09-14: 24 mục `APPROVED`, riêng TBD-013 `DEFERRED/OUT_OF_SCOPE`. Approval nghiệp vụ không đồng nghĩa runtime hoặc knowledge ingestion đã production-ready. Knowledge document chỉ được publish sau khi đồng bộ quyết định, có approver, `reviewAt`, security validation và UAT; các capability chưa triển khai tiếp tục fail-safe/handoff.

## Phạm vi hồ sơ Phase 0 ban đầu

Danh sách dưới mô tả phạm vi khi lập hồ sơ Phase 0. Các đợt triển khai sau đã được yêu cầu bổ sung code runtime và kiểm thử; xem báo cáo từng đợt để biết phần thực hiện. Phê duyệt cũ không tự áp dụng cho tám tài liệu DRAFT được sửa ngày 19/11/2026.

- Cài extension pgvector hoặc đổi `REAL[]` sang `vector(768)`.
- Viết embedding/ingestion/reindex worker.
- Thêm API hoặc giao diện quản trị knowledge.
- Sửa orchestrator, tool runtime hoặc response DTO.
- Seed thêm permission/migration.
- Commit hoặc push.

## Phạm vi triển khai đợt 1

Đã cập nhật authentication/profile, privacy, support-ticket và thêm chat-conversations-and-support-boundaries. Có 13 knowledge source files tại thời điểm ghi nhận; danh mục 28 trong kế hoạch là dự kiến, không phải số đã triển khai. Bản chuẩn ở knowledge/; bản upload-safe trong backend được sinh bằng `node scripts/sync-ai-knowledge.mjs --write`; chạy không có --write để kiểm tra checksum, không chạm DB. Danh mục hiện tại có 16 tài liệu, xem manifest và [bàn giao cho team](../../TEAM_HANDOFF.vi.md).

Dataset baseline vẫn 120 câu, thêm batch-01 gồm 240 câu thuộc 40 scenario; không gộp paraphrase cùng scenario qua các split. Kiểm tra artifact theo manifest không chứng minh chất lượng model. Xem báo cáo để phân biệt route, SQL thật, browser mock và provider thật.

## Đợt 2 và kiểm tra tích lũy

Bổ sung phương tiện, vòng đời vé, thẻ/mất thẻ và lịch sử gửi xe; cả bốn ở phiên bản 2.0.0 DRAFT. Manifest hiện có 13 nguồn, 8 tài liệu thuộc hai đợt và 600 câu hỏi gồm baseline. Xem [báo cáo đợt 2](evaluation/batch-02-report.md), [truy vết code](evaluation/batch-02-traceability.md) và [rubric](evaluation/batch-02-rubric.md). Không publish hoặc reset DB dev tự động.

Kết thúc đợt 2: kiểm thử cục bộ đã qua; gate Gemini bản cuối chưa đóng vì hạn mức embedding ngày đã hết. Không dùng kết quả artifact/unit để đổi DRAFT thành APPROVED; xem báo cáo và verification-status trong evidence.

## Ghi chú nội bộ trong nguồn và bản xuất — đợt 4

Bọc ghi chú kỹ thuật/nguồn truy vết bằng `<!-- authoring-only:start -->` và `<!-- authoring-only:end -->`. Script sync loại khối này trước khi xuất, giữ hash nguồn đầy đủ và hash import riêng. Marker thiếu, đảo thứ tự hoặc lồng nhau làm kiểm tra thất bại; không publish khi lỗi. Nội dung điều kiện và giới hạn cần cho người dùng phải nằm ngoài khối nội bộ. Bốn tài liệu đợt 4 áp dụng quy ước này; không tự loại các hạn chế nghiệp vụ ở tài liệu cũ.

### Trạng thái rà soát ngày 24/11/2026
Mốc lịch sử trước đợt 5: corpus có 16 nguồn DRAFT, 1.104 câu trong registry (baseline120 và bốn đợt). Đợt4 có264 câu/44 tình huống; chưa có holdout mù. Kết quả có tính nghiệm thu phải lấy từ [báo cáo đợt4](evaluation/batch-04-report.md), không suy từ số test hoặc nhãn APPROVED của tài liệu quản trị cũ. Chưa nạp/publish corpus vào DB dev.
