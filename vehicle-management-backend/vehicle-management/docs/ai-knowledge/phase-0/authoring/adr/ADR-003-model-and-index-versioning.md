# ADR-003: Tách model generation/embedding và version hóa index

- Trạng thái: PROPOSED
- Ngày: 2026-09-14

## Context
AI model configuration đã có use case `SUPPORT_CHAT`, `INTENT_CLASSIFICATION`, `CONVERSATION_SUMMARY`, `EMBEDDING`, rollout và catalog.

## Problem
Đổi generation model không nên reindex; đổi embedding model/dimension bắt buộc tạo lại vector. Hard-code model khiến release phụ thuộc provider catalog.

## Decision
Route generation và embedding độc lập theo configuration/catalog. Mỗi index lưu provider/model/dimension/normalization/chunker/version/checksum. Active index immutable; đổi embedding model tạo index version mới và reindex trước cutover.

## Alternatives
Một model cho mọi use case; sửa model name trong code; mutate vectors tại chỗ. Đều tăng coupling hoặc tạo mixed-index invalid.

## Consequences
Cần model compatibility checks, catalog warnings, dual-read/shadow evaluation và storage tăng trong reindex.

## Security impact
Chỉ provider/model allowlist; không cho admin nhập base URL/key qua UI; secret ở secret manager/env.

## Data migration impact
Backfill index mới song song; dimension cấu hình được nhưng cố định trong một active index.

## Rollback strategy
Đổi pointer active index/model config về version trước; không xóa ngay index cũ.

## Open questions
Retention index cũ, rollout metric, embedding model mặc định và thời gian catalog sync.

