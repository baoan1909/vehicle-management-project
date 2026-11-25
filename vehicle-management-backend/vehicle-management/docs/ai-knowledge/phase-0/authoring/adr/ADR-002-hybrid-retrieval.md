# ADR-002: Hybrid retrieval vector + lexical

- Trạng thái: PROPOSED
- Ngày: 2026-09-14

## Context
Repository hiện đặt tên `hybridSearch` nhưng chỉ dùng `ts_rank` với `plainto_tsquery('simple', query)`.

## Problem
Vector cải thiện semantic recall nhưng có thể bỏ sót mã, biển số dạng mask, status/code; lexical tốt cho thuật ngữ chính xác nhưng yếu với paraphrase.

## Decision
Truy vấn lexical và vector trong cùng authorization filter, hợp nhất bằng Reciprocal Rank Fusion hoặc score calibration đã đánh giá; rerank tùy chọn. Chỉ trả top-k có evidence đủ.

## Alternatives
Vector-only, lexical-only, provider File Search. Các phương án này bị loại khỏi baseline vì quyền/tenant, độ chính xác từ khóa hoặc lock-in.

## Consequences
Cần normalize tiếng Việt, dedupe chunk, metrics theo retrieval branch, threshold theo domain và golden eval.

## Security impact
Không query rộng rồi filter sau. Prompt injection scan và instruction/data separation bắt buộc.

## Data migration impact
Không đổi nội dung gốc; bổ sung lexical config/version và embedding index version.

## Rollback strategy
Tắt vector/reranker theo feature flag, giữ lexical known-good.

## Open questions
Normalization dấu, stemming, fusion weights, top-k và reranker provider.

