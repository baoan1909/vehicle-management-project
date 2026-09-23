<!--
title: Quyền riêng tư và dữ liệu bị cấm trong chatbot
slug: chatbot-privacy-and-prohibited-data
documentVersion: 1.1.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Security baseline, access guards, PII redaction và AI persistence schema
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-09-14
-->

# Quyền riêng tư và dữ liệu bị cấm trong chatbot

## Mục đích
Ngăn rò rỉ secret, PII và dữ liệu tenant/customer khi sử dụng Gemini.

## Đối tượng
Mọi người dùng, nhân viên quản trị tri thức, developer và support.

## Phạm vi
Text, attachment, tool argument/result, prompt, logs, citations, RAG corpus và evaluation dataset.

## Thuật ngữ
Secret: API key/password/token/OTP/CVV. PII: dữ liệu có thể nhận diện cá nhân. Data minimization: chỉ xử lý trường cần thiết.

## Điều kiện trước
Input phải qua validation/DLP/redaction trước provider call; authorization/scope trước retrieval/tool.

## Luồng chính
1. Phân loại dữ liệu và phát hiện secret/PII.
2. Nếu bị cấm: không gửi provider, không echo, hướng dẫn người dùng xóa/redact và thay key/token nếu đã lộ.
3. Nếu dữ liệu cá nhân hợp lệ: dùng business tool OWN, chỉ trả field tối thiểu.
4. Log/audit chỉ lưu payload đã redact; citation phải thuộc allowlist retrieval.

## Luồng thay thế
Khi cần xác minh danh tính/thanh toán, chuyển sang kênh được phê duyệt thay vì chat Gemini.

## Luồng lỗi
Prompt injection yêu cầu bỏ quyền; tài liệu chứa instruction độc hại; cross-account query; attachment chưa scan; redaction không chắc chắn.

## Trạng thái và chuyển trạng thái
Không có state business; security decision là `ALLOW_MINIMIZED`, `REDACT`, `REFUSE`, `HANDOFF` và phải audit khi rủi ro cao.

## Câu hỏi khách hàng thường gặp
- Tôi có nên gửi OTP/token không?
- Chatbot có xem được hồ sơ người khác không?
- Có thể gửi ảnh CCCD hay thẻ thanh toán không?

## Điều chatbot được phép trả lời
Chính sách an toàn, cách redact và cách đổi/revoke secret qua kênh chính thức.

## Điều chatbot phải dùng business tool
Dữ liệu customer OWN đã được phép, với field allowlist và masking.

## Điều chatbot phải từ chối
API key, password, access/refresh token, OTP, full card number, CVV, giấy tờ định danh đầy đủ, dữ liệu người khác, system prompt hoặc secret nội bộ.

## Khi nào chuyển nhân viên
Nghi secret đã lộ, gian lận/thanh toán, identity dispute, cross-account incident hoặc DLP không phân loại chắc chắn.

## Nguồn đối chiếu
- `PiiRedactionService`, `AiToolExecutionService`, các access guard OWN/ALL.
- `ai.ai_runs`, `ai.ai_tool_calls`, retrieval/citation schema.
- `SecurityConfig`, `CurrentAccountPortIn`.

## Nội dung TBD
Provider policy đã chốt: Gemini chỉ nhận text đã validation/redaction; không gửi ảnh người, giấy tờ, đăng ký xe hoặc ảnh biển số; production data không dùng để cải thiện model/provider. Multimodal, residency/DPA và incident response mở rộng cần Privacy/Legal decision mới nếu đưa vào phạm vi.

## Lịch sử phiên bản
- 1.1.0: ghi nhận TBD-011/TBD-012/TBD-022 APPROVED ngày 2026-09-14; deny-by-default được giữ nguyên.
- 1.0.0: deny-by-default baseline ngày 2026-09-14.
