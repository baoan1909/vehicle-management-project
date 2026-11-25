# Decision Log — Phase 0

> Cập nhật đợt 1 — 19/11/2026: xem [bằng chứng/truy vết](evaluation/batch-01-traceability.md), [manifest](knowledge-manifest.json) và [báo cáo đợt 1](evaluation/batch-01-report.md). Baseline 14/09/2026 được giữ để truy vết; các nhận định runtime có delta dưới đây thay thế nhận định cũ tương ứng. Chưa tự phê duyệt/publish knowledge.


| ID | Quyết định/quan sát | Trạng thái | Căn cứ | Hành động sau Phase 0 |
|---|---|---|---|---|
| DEC-001 | Static knowledge dùng RAG + citation | ACCEPTED | Yêu cầu Phase 0 | Triển khai ingestion/retrieval |
| DEC-002 | Personal/runtime data chỉ business tool | ACCEPTED | Access guards hiện hữu | Bổ sung tool allowlist |
| DEC-003 | Write dùng action card + confirmation + idempotency | ACCEPTED | Tool call persistence và support idempotency đã có | Tổng quát hóa cho tool mới |
| DEC-004 | Thiếu evidence/conflict thì handoff | ACCEPTED | Ngăn hallucination | Evals và UI handoff |
| DEC-005 | Authorization theo permission, không role hard-code | ACCEPTED | IAM schema và FE routePermissions | Test negative/cross-role |
| DEC-006 | Role code hợp lệ là `SYSTEM_ADMIN` | CONFIRMED | seed security migration | Sửa typo tài liệu ngoài repo nếu có |
| DEC-007 | Tách quyền knowledge thành READ/MANAGE/APPROVE/REINDEX; giữ authorization permission-driven | APPROVED | TBD-015 đã được xác nhận | Bổ sung migration, Backend/FE guards và negative tests |
| DEC-008 | Migration knowledge hiện gán role bằng UUID là debt seed governance | CONFLICT | yêu cầu resolve code vs migration hiện hành | Migration bổ sung ở phase sau, không sửa runtime role logic |
| DEC-009 | PostgreSQL + pgvector là target RAG | ACCEPTED | schema hiện có + yêu cầu production | ADR-001 |
| DEC-010 | Hybrid = vector + lexical, không gọi full-text hiện tại là hybrid production | ACCEPTED | repository hiện chỉ tsvector | ADR-002 |
| DEC-011 | Generation model tách embedding model; index version immutable | ACCEPTED | model configurations đã có EMBEDDING use case | ADR-003 |
| DEC-012 | DTO target gồm text/citations/actionCards/confidence/handoff | ACCEPTED | yêu cầu Phase 0 | Contract validation backend |
| DEC-013 | Refund dùng quy trình Finance thủ công, Phase đầu chỉ full refund; runtime hiện chưa có workflow | APPROVED | TBD-004 đã được xác nhận; endpoint/use case chưa tồn tại | Tiếp tục handoff đến khi refund aggregate, approval và reconciliation được triển khai |
| DEC-014 | `start-progress` không phải action thủ công; first reply tự chuyển | CONFIRMED | SupportTicketUseCaseImpl | Không expose tool start-progress |
| DEC-015 | Contact chính thức là `0912345678`, `support@coparking.vn`, trụ sở `10/5 Đường Tỉnh Lộ 19, Phường An Phú Đông, Thành phố Hồ Chí Minh`; tiếp nhận 24/7 | APPROVED | TBD-001/TBD-002 đã được xác nhận | Đồng bộ FE/config và SLA monitoring trước khi publish |
| DEC-016 | Vehicle không có approval riêng; dùng customer APPROVED và vehicle ACTIVE | APPROVED | TBD-007 đã được xác nhận, phù hợp schema/policy | Sửa Guide/RAG copy mâu thuẫn |
| DEC-017 | Customer lost-card là handoff, không self-service tool | ACCEPTED_FOR_BASELINE | lost-card permissions chỉ ALL; dữ liệu định danh nhạy cảm | Thiết kế quy trình riêng nếu muốn self-service |
| DEC-018 | Multi-tenancy chưa thuộc phạm vi; `TENANT_PRIVATE` tiếp tục deny và không suy diễn tenant | DEFERRED / OUT_OF_SCOPE | TBD-013 được hoãn có chủ đích | Không sửa schema/runtime; mở ADR mới khi có yêu cầu multi-organization |
| DEC-019 | Toàn bộ disposition TBD-001..TBD-025 được ghi tại `open-business-questions.md`: 24 APPROVED, 1 DEFERRED | APPROVED | Xác nhận nghiệp vụ ngày 2026-09-14 | Theo dõi implementation gap riêng với trạng thái IMPLEMENTATION_PENDING |
| DEC-020 | Approval nghiệp vụ không tự động đánh dấu capability hoặc knowledge document là production-ready | ACCEPTED | Tách business decision khỏi implementation/publish gate | Chỉ publish sau security validation, reviewAt, approver và UAT |

## Delta kỹ thuật đợt 1 — 19/11/2026

- Dùng matching view bỏ dấu, giữ câu gốc/identifier; không cài thêm LLM classifier.
- Hỏi cách làm/phủ định không phải write command. Thêm clarification nội bộ, không migration trạng thái.
- Chỉ carry topic hướng dẫn đã biết; history không tái thực hiện action hoặc tra cứu cá nhân.
- Guest dùng JSON allowlisted citations; malformed output là lỗi khả dụng, không trả như đáp án.
- Canonical manifest kiểm tra source/import hash, không giới hạn cứng 12 file. DRAFT không tự thành APPROVED.
- Không purge/reimport DB dev trong đợt 1. Giữ các quyết định nghiệp vụ cũ, ghi rõ gap thay vì sửa policy để khớp tài liệu.

## Bổ sung 20/11/2026 — chưa thay quyết định đã duyệt

[ADR-006](adr/ADR-006-gemini-credential-and-evidence-failover.md) ghi thiết kế đã triển khai về key-first, credential/model cooldown, embedding giữ nguyên index và đoạn bằng chứng có version. ADR vẫn DRAFT. Không tự nâng quyền, publish knowledge hoặc đổi chính sách tài chính để đạt kiểm thử. Kết quả thực tế và điều kiện còn mở nằm trong [báo cáo đợt 3](evaluation/batch-03-report.md).


## Quan sát và thay đổi kỹ thuật đợt 4 — 23/11/2026 (DRAFT)

Không tạo quyết định nghiệp vụ APPROVED mới. Tái sử dụng DEC-015/TBD-001 cho liên hệ công ty và các quyết định đã chốt về mục tiêu phản hồi. Bốn nguồn đợt 4 và các delta nguồn cũ giữ DRAFT, không đổi effective/review thành ngày phát hành.

- Đối chiếu code hiện hành: vé vãng lai dùng thẻ; QR vé đăng ký là nhánh thay thế có điều kiện. QR đặt chỗ không tự kích hoạt nhánh xe vào không thẻ.
- Bãi chưa có nguồn giờ mở cửa riêng đủ để chat xác nhận. Trạng thái hoạt động và tiếp nhận hỗ trợ 24/7 không chứng minh bãi đang mở.
- Form trang Liên hệ chưa có luồng nhận yêu cầu hoàn chỉnh. Giao diện tạm khóa gửi, ghi rõ tình trạng và đưa hotline/email đã duyệt; không coi Turnstile của guest support là backend của form này.
- Sửa lỗi nối chủ đề và chọn bằng chứng nhầm bước, đồng thời giữ các giới hạn tài chính/quyền cũ. Ngoại lệ che liên hệ công khai phải có cấu hình và nguồn trích dẫn; không mở rộng sang dữ liệu riêng.
- Kiểm thử sử dụng PostgreSQL cô lập, truy xuất/embedding/generation thật khi được bật; các business port là fixture. Không nạp corpus DRAFT vào DB dev, không phát sinh tác vụ kinh doanh thật, không publish.

Xem [truy vết](evaluation/batch-04-traceability.md) và [báo cáo](evaluation/batch-04-report.md) để phân biệt kết quả cũ, lượt sửa và bản nghiệm thu hiện tại.

### Ghi nhận kỹ thuật ngày 24/11/2026 — vẫn DRAFT
Không thêm phê duyệt nghiệp vụ hoặc model. Tái sử dụng chín mục model đã được ứng dụng cho phép trong snapshot registry chỉ đọc. Giữ thứ tự model/key và embedding/index hiện hữu; không nâng các model chưa được duyệt thành miễn phí. Giữ tài liệu DRAFT dù các kiểm tra cấu trúc/test qua. Nghiệm thu nội dung và độ ổn định phụ thuộc kết quả cuối, không phụ thuộc số test.
