# Báo cáo kiểm tra dataset và Gate Phase 0

| Thuộc tính | Giá trị |
|---|---|
| Phiên bản | 1.0.0 |
| Trạng thái | PASS — artifact baseline; business decision register đã APPROVED, knowledge document chưa publish |
| Ngày | 2026-09-14 |

## Kết quả tự động

Lệnh:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/ai-knowledge/phase-0/authoring/evaluation/validate-phase0.ps1
```

Kết quả:

```text
knowledgeDocuments: 12
goldenQuestions: 120
uniqueCaseIds: 120
intentsInMatrix: 36
domains: 12
errors: 0
warnings: 0
PHASE0_ARTIFACT_VALIDATION=PASS
```

## Checklist

| Kiểm tra | Kết quả |
|---|---|
| Đủ 12 knowledge documents | PASS |
| JSONL parse từng dòng | PASS |
| Số case trong khoảng 100–200 | PASS — 120 |
| caseId duy nhất | PASS — 120/120 |
| Intent tồn tại trong intent matrix | PASS |
| Document slug tồn tại | PASS |
| Mỗi miền có ACCENTED và UNACCENTED | PASS |
| Answer mode thuộc allowlist | PASS |
| RAG bắt buộc citation | PASS |
| ACTION_CARD bắt buộc confirmation | PASS |
| Không phát hiện mẫu API key/JWT/private key | PASS |
| Không phát hiện mojibake marker | PASS |
| Knowledge document đủ metadata và 17 mục bắt buộc | PASS |
| Runtime role hard-code mới | PASS — Phase 0 không sửa Java/React |

## Số liệu audit

- 23 controller liên quan, 135 endpoint methods được inventory.
- 18 màn hình/route FE trọng yếu được đối chiếu.
- 60 `CREATE TABLE` trong toàn bộ Flyway được inventory; 10 state machine trọng yếu được mô tả (account, customer approval, vehicle, subscription, invoice, payment, card, lost-card report, parking session, support ticket/escalation).
- 36 intent.
- 5 tool hiện có và 14 tool đề xuất sau Phase 0.
- 25 decision item đã được xử lý: 24 `APPROVED`, TBD-013 `DEFERRED/OUT_OF_SCOPE`.

## Gate

`PASS` cho độ đầy đủ và nhất quán của artifact Phase 0. Sổ quyết định nghiệp vụ đã được xác nhận ngày 2026-09-14. Knowledge document vẫn chưa được ingest/publish; capability chưa triển khai phải tiếp tục fail-safe/handoff cho đến khi nội dung được đồng bộ, security validation và UAT hoàn tất.
