# ADR-004: Bảo mật tool, xác nhận và idempotency

- Trạng thái: PROPOSED
- Ngày: 2026-09-14

## Context
Tool call đã persist và create-support-ticket yêu cầu xác nhận, nhưng worker chỉ chạy search knowledge.

## Problem
Model output không đáng tin để authorize hoặc thực hiện write; retry/replay có thể tạo dữ liệu trùng; tool cá nhân cần request-time security context.

## Decision
Registry allowlist có schema/version/type/permission. Backend bind tool call với actor/conversation/canonical payload; read tool kiểm tra permission+ownership khi execute; write tool persist `AWAITING_CONFIRMATION`, expire, confirm một lần và dùng idempotency. Response field allowlist và redaction bắt buộc.

## Alternatives
Cho model gọi REST trực tiếp; tin FE confirmation; authorize lúc declaration. Đều bị loại vì bypass và TOCTOU.

## Consequences
Cần execution context an toàn cho async worker, transactional state machine, audit và negative tests.

## Security impact
Ngăn confused deputy, cross-account, prompt-injection tool abuse, replay và over-sharing.

## Data migration impact
Có thể cần payload hash, actor/resource binding và tool version; migration phase sau.

## Rollback strategy
Kill switch theo tool; deny write; giữ chatbot ở RAG/handoff.

## Open questions
TTL theo tool, step-up auth cho payment, retry semantics và approval UX.

