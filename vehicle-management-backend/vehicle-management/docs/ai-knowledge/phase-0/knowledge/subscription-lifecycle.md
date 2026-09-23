<!--
title: Vòng đời vé tháng và subscription
slug: subscription-lifecycle
documentVersion: 1.1.0
status: DRAFT
language: vi
accessScope: CUSTOMER
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Subscription use case, policy, access guard và Flyway
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-09-14
-->

# Vòng đời vé tháng và subscription

## Mục đích
Mô tả vòng đời đăng ký vé và các điều kiện chuyển trạng thái.

## Đối tượng
Khách hàng, người duyệt vé và nhân viên gán thẻ có permission.

## Phạm vi
Tạo, sửa khi chờ, duyệt/từ chối, thanh toán, gán thẻ, kích hoạt, hủy, hết hạn.

## Thuật ngữ
Subscription là vé đăng ký trong `access_control.subscriptions`; không tạo khái niệm vé tháng song song.

## Điều kiện trước
Customer ACTIVE+APPROVED, xe ACTIVE thuộc customer, ticket type ACTIVE thuộc nhóm được hỗ trợ, price rule hợp lệ, không overlap và còn capacity.

## Luồng chính
1. Customer gửi đăng ký với ngày bắt đầu dự kiến từ 2 đến 7 ngày sau ngày hiện tại → `PENDING`.
2. Người có quyền duyệt trước requested date; hệ thống reserve card và tạo invoice.
3. Invoice 0 → `PENDING_CARD`; invoice lớn hơn 0 → `PENDING_PAYMENT`.
4. Payment hoàn tất → `PENDING_CARD`.
5. Giao/gán reserved card → `ACTIVE`; effective date là ngày muộn hơn giữa requested date và receipt date.
6. Sau effectiveTo → `EXPIRED` qua nghiệp vụ expire.

## Luồng thay thế
`PENDING` có thể sửa hoặc reject. `PENDING`/`PENDING_PAYMENT` có thể cancel; hệ thống xử lý invoice chưa trả và release card đã reserve.

## Luồng lỗi
Quá hạn duyệt, trùng thời gian vé, hết capacity, xe/type/customer không hoạt động, invoice active trùng, chưa paid hoặc card không reserved.

## Trạng thái và chuyển trạng thái
`PENDING → PENDING_PAYMENT|PENDING_CARD|REJECTED|CANCELLED`; `PENDING_PAYMENT → PENDING_CARD|CANCELLED`; `PENDING_CARD → ACTIVE`; `ACTIVE → EXPIRED`.

## Câu hỏi khách hàng thường gặp
- Trạng thái chờ duyệt/chờ thanh toán/chờ gán thẻ nghĩa là gì?
- Vé còn hạn không?
- Có thể hủy đăng ký không?

## Điều chatbot được phép trả lời
Giải thích vòng đời và điều kiện đã xác nhận, không dự đoán thời điểm duyệt.

## Điều chatbot phải dùng business tool
Danh sách/trạng thái/ngày hiệu lực/invoice của subscription thuộc người dùng.

## Điều chatbot phải từ chối
Tự approve/reject/assign card/expire; đọc vé người khác; cam kết capacity/giá mà không gọi tool.

## Khi nào chuyển nhân viên
Tranh chấp duyệt/giá, ACTIVE muốn hủy, cần hoàn tiền, overlap/capacity bất thường.

## Nguồn đối chiếu
- `SubscriptionController`, `SubscriptionUseCaseImpl`.
- `SubscriptionPolicy`, `SubscriptionAccessGuard`.
- FE `SubscriptionPage.tsx`, `SubscriptionApprovalPage.tsx`.
- `access_control.subscriptions`, cards, invoices, payments.

## Nội dung TBD
Không còn TBD trong phạm vi TBD-008/TBD-009: không auto-renew, không grace period, không chuyển xe/thẻ in-place và không self-service cancel ACTIVE; “gia hạn” là đăng ký mới. Refund nếu đủ điều kiện tuân theo quy trình thủ công tại TBD-004.

## Lịch sử phiên bản
- 1.1.0: ghi nhận TBD-008/TBD-009 APPROVED ngày 2026-09-14; implementation/copy sync pending.
- 1.0.0: baseline từ mã nguồn ngày 2026-09-14.
