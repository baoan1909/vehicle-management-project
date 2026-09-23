# Bộ tri thức chatbot CoParking — Phase 0

Thư mục này là nguồn tài liệu nghiệp vụ có kiểm soát phiên bản để quản trị viên upload vào RAG qua luồng `/admin/ai-knowledge`.

## Cấu trúc

- `knowledge/`: 12 tài liệu nghiệp vụ chuẩn, giữ nguyên slug để liên kết với golden dataset.
- Tài liệu giữ metadata quản trị trong HTML comment để không bị đưa vào chunk, gồm `status`, `accessScope`, `documentVersion`, `sourceOfTruth` và ngày rà soát.

## Phân nhóm upload

### PUBLIC

- `authentication-and-profile.md`
- `pricing-and-fee-calculation.md`
- `parking-entry-exit.md`
- `chatbot-privacy-and-prohibited-data.md`
- `common-errors-and-troubleshooting.md`
- `locations-hours-sla-contact.md`

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

1. Sửa tài liệu trên nhánh riêng và cập nhật `documentVersion`, `lastVerifiedAt`, `reviewAt`.
2. Đối chiếu lại các class/API/migration được nêu trong `sourceOfTruth` và phần nguồn đối chiếu.
3. BA và người có permission phù hợp review các mục TBD.
4. Chỉ đổi `status: DRAFT` thành `status: APPROVED` trong khối metadata comment sau khi có phê duyệt.
5. Upload qua Admin UI để hệ thống kiểm tra tệp, lưu MinIO, extract, chunk và embed.
6. Kiểm tra nội dung/chunk ở trạng thái REVIEW rồi mới publish.
7. Sau khi toàn bộ tài liệu READY, build và activate một knowledge index version mới.

## Lưu ý về định dạng và retry

- Không đổi khối metadata đầu file về YAML frontmatter dùng cặp dấu `---`. Với bộ extractor Markdown hiện tại, dấu đóng `---` có thể bị CommonMark hiểu thành Setext heading và làm `source_section` vượt giới hạn 200 ký tự khi lưu block.
- Nếu một tài liệu đã upload bị lỗi vì nội dung cũ, hãy upload lại file đã sửa như một tài liệu mới với idempotency key mới. Nút retry tiếp tục đọc object cũ đang lưu trong MinIO nên không nhận thay đổi từ file local.

Không seed nội dung tài liệu trực tiếp bằng Flyway và không đặt secret, token hoặc dữ liệu cá nhân thật trong các file này.
