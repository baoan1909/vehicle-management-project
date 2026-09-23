<!--
title: Lỗi thường gặp và hướng xử lý
slug: common-errors-and-troubleshooting
documentVersion: 1.0.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Error handling, use cases, FE flows và assistant job status
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-09-14
-->

# Lỗi thường gặp và hướng xử lý

## Mục đích
Cung cấp checklist an toàn, không suy đoán nguyên nhân khi thiếu status/log.

## Đối tượng
Người dùng public/customer và nhân viên hỗ trợ tuyến đầu.

## Phạm vi
Google login, token/session, network, payment, subscription, history, chat, assistant retry/failed và permission.

## Thuật ngữ
`Failed to fetch` là lỗi transport tổng quát; `RETRYING` là job sẽ thử lại; `FAILED terminal=true` là đã dừng tự retry.

## Điều kiện trước
Người dùng chỉ chia sẻ request ID/public error code; không chia sẻ token, cookie hoặc full payload chứa PII.

## Luồng chính
1. Ghi nhận thao tác, thời điểm, màn hình và public error code.
2. Refresh một lần, kiểm tra mạng/session và đăng nhập lại khi phù hợp.
3. Với Google lỗi ở cửa sổ thường nhưng ẩn danh được: thử xóa cookie/site data Google/Keycloak hoặc tắt extension liên quan; không đổi backend khi chưa có bằng chứng.
4. Payment: không thanh toán lặp ngay khi tiền đã trừ; dùng tool kiểm tra invoice/payment rồi tạo phiếu.
5. Assistant: chờ khi RETRYING; khi FAILED terminal, dùng “Thử lại” một lần hoặc tạo phiếu.

## Luồng thay thế
403 kiểm tra permission/current-access; 404 OWN có thể là không thuộc tài khoản; 409 là conflict trạng thái; 429 chờ theo retry guidance nếu có.

## Luồng lỗi
Không có HTTP status/log, lỗi tái diễn, data discrepancy, suspected security incident hoặc provider outage.

## Trạng thái và chuyển trạng thái
Assistant job có queued/processing/retrying/waiting confirmation/completed/failed theo enum/schema; chỉ public status được hiển thị.

## Câu hỏi khách hàng thường gặp
- `Failed to fetch` là gì?
- Vì sao Google chỉ lỗi ở cửa sổ thường?
- Vì sao không thấy vé/lịch sử?
- Trợ lý retry rồi failed phải làm gì?

## Điều chatbot được phép trả lời
Checklist, ý nghĩa public status và đường dẫn self-service.

## Điều chatbot phải dùng business tool
Trạng thái assistant message, subscription, invoice, session hoặc ticket thuộc người dùng.

## Điều chatbot phải từ chối
Yêu cầu token/cookie/API key, khẳng định root cause khi thiếu evidence, hướng dẫn vô hiệu hóa bảo mật hệ thống.

## Khi nào chuyển nhân viên
Terminal failure, payment discrepancy, account locked, 403 hợp lệ nhưng cần quyền, lỗi lặp lại hoặc nghi sự cố bảo mật.

## Nguồn đối chiếu
- `AssistantController`, `AssistantStatusService`, `AssistantOrchestrator`.
- FE auth/support/payment pages và API error mapping.
- Domain/use-case exception messages của 12 miền.

## Nội dung TBD
Status/error-code catalog công khai, retry delay, support runbook và incident status page.

## Lịch sử phiên bản
- 1.0.0: baseline ngày 2026-09-14.

