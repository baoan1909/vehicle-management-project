# Bộ tri thức chatbot CoParking — Phase 0

Thư mục này chứa bộ tài liệu soạn thảo và bản xuất để quản trị viên upload vào RAG qua luồng `/admin/ai-knowledge`. Đợt 1 xác minh ngày 19/11/2026; xem [báo cáo](authoring/evaluation/batch-01-report.md).

> Đợt 4 ngày 23/11/2026: 16 tài liệu và 1.104 câu tích lũy. Bốn nguồn mới/làm mới vẫn DRAFT; xem [báo cáo đợt 4](authoring/evaluation/batch-04-report.md).

## Cấu trúc

- `authoring/`: nguồn chuẩn gồm tài liệu YAML frontmatter, ma trận, dataset, rubric và báo cáo.
- `knowledge/`: 16 bản xuất upload-safe; không chỉnh nội dung độc lập tại đây.
- `authoring/knowledge-manifest.json`: scope, version và checksum cả hai bản. Chạy `node scripts/sync-ai-knowledge.mjs --write` từ backend để đồng bộ; bỏ `--write` để kiểm tra. Không truy cập DB.
- Tài liệu giữ metadata quản trị trong HTML comment để không bị đưa vào chunk, gồm `status`, `accessScope`, `documentVersion`, `sourceOfTruth` và ngày rà soát.

## Phân nhóm upload

### PUBLIC

- `authentication-and-profile.md`
- `pricing-and-fee-calculation.md`
- `parking-entry-exit.md`
- `parking-search-and-map.md`
- `chatbot-privacy-and-prohibited-data.md`
- `chat-conversations-and-support-boundaries.md`
- `common-errors-and-troubleshooting.md`
- `locations-hours-sla-contact.md`
- `voucher-and-promotions.md`
- `reservations-and-holds.md`

Upload các file này vào source RAG có access scope `PUBLIC`.

### CUSTOMER

- `customer-vehicle-management.md`
- `subscription-lifecycle.md`
- `invoice-vnpay-payment-refund.md`
- `cards-and-lost-card.md`
- `parking-history.md`
- `support-ticket-and-escalation.md`

Upload các file này vào source RAG có access scope `CUSTOMER`.

## Quy trình thay đổi

1. Sửa tài liệu trong `authoring/knowledge/` và cập nhật `documentVersion`, `lastVerifiedAt`, `reviewAt`.
2. Đối chiếu lại các class/API/migration được nêu trong `sourceOfTruth` và phần nguồn đối chiếu.
3. BA và người có permission phù hợp review các mục TBD.
4. Chỉ đổi `status: DRAFT` thành `status: APPROVED` ở nguồn chuẩn sau khi có phê duyệt; chạy script đồng bộ để sinh lại comment metadata. Cả mười hai tài liệu của ba đợt vẫn DRAFT.
5. Upload qua Admin UI để hệ thống kiểm tra tệp, lưu MinIO, extract, chunk và embed.
6. Kiểm tra nội dung/chunk ở trạng thái REVIEW rồi mới publish.
7. Sau khi toàn bộ tài liệu READY, build và activate một knowledge index version mới.

## Lưu ý về định dạng và retry

- Nguồn soạn thảo `authoring/knowledge/` dùng YAML frontmatter. Riêng bản upload sinh ra trong `knowledge/` dùng HTML comment; không upload nguồn YAML trực tiếp. Với bộ extractor Markdown hiện tại, dấu đóng `---` có thể bị CommonMark hiểu thành Setext heading và làm `source_section` vượt giới hạn 200 ký tự khi lưu block.
- Nếu một tài liệu đã upload bị lỗi vì nội dung cũ, hãy upload lại file đã sửa như một tài liệu mới với idempotency key mới. Nút retry tiếp tục đọc object cũ đang lưu trong MinIO nên không nhận thay đổi từ file local.

Không seed nội dung tài liệu trực tiếp bằng Flyway và không đặt secret, token hoặc dữ liệu cá nhân thật trong các file này.

Đợt 3 ngày 20/11/2026 có bốn tài liệu về giá, thanh toán, ưu đãi và đặt chỗ; tổng dataset tích lũy 840 câu. Xem [báo cáo và giới hạn kiểm chứng](authoring/evaluation/batch-03-report.md).

### Trạng thái rà soát ngày 24/11/2026
Corpus có 16 nguồn DRAFT, 1.104 câu trong registry (baseline120 và bốn đợt). Đợt4 có264 câu/44 tình huống; chưa có holdout mù. Kết quả có tính nghiệm thu phải lấy từ [báo cáo đợt4](authoring/evaluation/batch-04-report.md), không suy từ số test hoặc nhãn APPROVED của tài liệu quản trị cũ. Chưa nạp/publish corpus vào DB dev.
