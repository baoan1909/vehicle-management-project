<!--
title: Thẻ xe và báo mất thẻ
slug: cards-and-lost-card
documentVersion: 1.1.0
status: DRAFT
language: vi
accessScope: CUSTOMER
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Card/lost-card policies, use cases, FE và Flyway
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-09-14
-->

# Thẻ xe và báo mất thẻ

## Mục đích
Giải thích trạng thái thẻ và hướng dẫn mất thẻ an toàn.

## Đối tượng
Customer và nhân viên hỗ trợ/vận hành.

## Phạm vi
Gán/reserve/in-use, block/unblock, lost/recover/retire và lost-card report.

## Thuật ngữ
UID là mã kỹ thuật nhạy cảm không hiển thị qua chatbot; lost-card report là hồ sơ vận hành, khác support ticket.

## Điều kiện trước
Chuyển trạng thái thẻ do Backend policy và người có permission ALL; report yêu cầu context và xác minh tại quy trình vận hành.

## Luồng chính
- Thẻ mới `AVAILABLE`; có thể `RESERVED`, `ASSIGNED`, `IN_USE`.
- Thẻ hợp lệ có thể block và lưu status trước block; unblock khôi phục status đó.
- Report mất thẻ tạo `OPEN`; operator xử lý để `RESOLVED` hoặc `CANCELLED`, có thể liên quan thẻ thay thế/invoice.

## Luồng thay thế
Thẻ LOST tìm lại có thể recover về AVAILABLE theo policy và audit người thực hiện.

## Luồng lỗi
Không block LOST/RETIRED; không retire IN_USE; thiếu metadata block; report thiếu thông tin xác minh hoặc thời gian mất ở tương lai.

## Trạng thái và chuyển trạng thái
Card: AVAILABLE, RESERVED, ASSIGNED, IN_USE, LOST, BLOCKED, RETIRED. Report: OPEN→RESOLVED|CANCELLED.

## Câu hỏi khách hàng thường gặp
- Mất thẻ phải làm gì?
- Thẻ bị khóa nghĩa là gì?
- Có cấp thẻ thay thế không?

## Điều chatbot được phép trả lời
Hướng dẫn giữ nguyên hiện trường, báo nhân viên/tạo support ticket và chỉ cung cấp dữ liệu tối thiểu.

## Điều chatbot phải dùng business tool
Status thẻ OWN qua subscription mapping; không trả UID hoặc thông tin định danh đầy đủ.

## Điều chatbot phải từ chối
Nhận CCCD/CMND đầy đủ, số thẻ/UID, tự block/unblock/recover/retire hoặc xác nhận phí thay thế không có tool.

## Khi nào chuyển nhân viên
Khách đang ở trong bãi, thẻ LOST/BLOCKED, cần xác minh/cấp thẻ hoặc có tranh chấp phí.

## Nguồn đối chiếu
- `CardController`, `CardUseCaseImpl`, `CardPolicy`.
- `LostCardReportController`, use case/policy/access guard.
- FE cards/lost-card pages và API.
- `access_control.cards`, `lost_card_reports`.

## Nội dung TBD
Self-service và masking đã chốt: customer không tự lập official lost-card report; chatbot không nhận CCCD/CMND và chỉ giữ tối đa 4 ký tự cuối parking-card. Mức phí, giấy tờ vận hành, replacement SLA và retention giấy tờ cần decision riêng.

## Lịch sử phiên bản
- 1.1.0: ghi nhận TBD-010/TBD-022 APPROVED ngày 2026-09-14; customer tool không được mở.
- 1.0.0: baseline ngày 2026-09-14.
