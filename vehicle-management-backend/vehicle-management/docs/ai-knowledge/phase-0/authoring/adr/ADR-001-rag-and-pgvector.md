# ADR-001: PostgreSQL + pgvector cho RAG

- Trạng thái: PROPOSED
- Ngày: 2026-09-14

## Context
Schema đã có knowledge source/document/chunk/embedding nhưng embedding là `REAL[]` và retrieval chỉ full-text.

## Problem
Lexical search không đủ cho tiếng Việt có/không dấu, diễn đạt tương đương và câu hỏi tự nhiên; `REAL[]` không có vector index production phù hợp.

## Decision
Dùng PostgreSQL hiện hữu với extension pgvector; mỗi active index dùng `vector(768)` theo cấu hình/version đã chốt. Giữ full-text để hybrid, không thay thế bằng vector-only.

## Alternatives
- Chỉ full-text: đơn giản nhưng recall ngữ nghĩa thấp.
- Vector database riêng: mạnh nhưng tăng vận hành, consistency và authorization surface.
- `REAL[]` scan: phù hợp fallback/dev nhỏ, không production scale.

## Consequences
Cần extension, migration an toàn, backfill/reindex, vector index và quan sát latency/recall. Schema/version phải cho phép đổi dimension qua index mới thay vì mutate active index.

## Security impact
Filter permission/scope/tenant trước retrieval; embedding không được coi là vô hại và phải theo retention/data classification.

## Data migration impact
Tạo cột/index version mới, backfill kiểm soát, shadow compare, cutover; không cast mù dữ liệu sai dimension.

## Rollback strategy
Giữ lexical path và active index cũ; feature flag quay lại full-text khi vector path lỗi.

## Open questions
Extension availability, corpus size, HNSW/IVFFlat, target recall/latency và tenant model.

