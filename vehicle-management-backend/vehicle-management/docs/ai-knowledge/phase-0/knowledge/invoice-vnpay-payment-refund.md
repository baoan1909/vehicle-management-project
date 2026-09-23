<!--
title: Hóa đơn, VNPAY, lỗi thanh toán và hoàn tiền
slug: invoice-vnpay-payment-refund
documentVersion: 1.1.0
status: DRAFT
language: vi
accessScope: CUSTOMER
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Billing controllers, use cases, policies, FE payment và Flyway
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-09-14
-->

# Hóa đơn, VNPAY, lỗi thanh toán và hoàn tiền

## Mục đích
Mô tả an toàn vòng đời invoice/payment và giới hạn hỗ trợ refund.

## Đối tượng
Customer có invoice của mình, nhân viên thu ngân/quản trị có permission.

## Phạm vi
Invoice, manual payment, VNPAY create/IPN/return, failure/timeout/retry và refund hiện trạng.

## Thuật ngữ
Invoice là nghĩa vụ thanh toán; Payment là giao dịch riêng. Return URL là UX; IPN có chữ ký là nguồn cập nhật chính.

## Điều kiện trước
Invoice `UNPAID`, finalAmount dương và thuộc customer; không có payment SUCCESS trước đó.

## Luồng chính
1. Hệ thống tạo invoice từ subscription hoặc parking visitor.
2. Customer khởi tạo VNPAY → payment `PENDING` có transactionRef/expiry và redirect URL.
3. Backend kiểm tra chữ ký/IPN, số tiền và trạng thái; success → payment SUCCESS, invoice PAID.
4. Return page xác minh kết quả hiện có và hiển thị trạng thái, không tự tin dữ liệu query string.

## Luồng thay thế
Manual payment dành cho operator và không đi qua endpoint VNPAY. Payment PENDING cũ có thể được đánh dấu failed/expired theo flow kỹ thuật trước khi tạo lại.

## Luồng lỗi
Sai chữ ký/số tiền, transaction ref trùng, callback lặp, timeout, invoice không còn payable hoặc provider trả failure. User có thể thử lại khi Backend cho phép hoặc tạo phiếu.

## Trạng thái và chuyển trạng thái
Invoice: `UNPAID → PAID|CANCELLED`; `REFUNDED` tồn tại trong schema nhưng transition runtime chưa thấy. Payment: `PENDING → SUCCESS|FAILED`; `REFUNDED` tồn tại nhưng chưa có action.

## Câu hỏi khách hàng thường gặp
- Thanh toán VNPAY như thế nào?
- Vì sao tiền đã trừ nhưng hóa đơn còn chờ?
- Tôi có thể thử lại không?
- Hoàn tiền thế nào?

## Điều chatbot được phép trả lời
Giải thích trạng thái, hướng dẫn kiểm tra và cảnh báo không đóng trang/không chia sẻ thông tin nhạy cảm.

## Điều chatbot phải dùng business tool
Invoice/payment status và tạo payment URL cho invoice OWN sau action confirmation.

## Điều chatbot phải từ chối
Nhận toàn bộ số thẻ/CVV/OTP/token; tự mark paid; tự refund; hứa thời hạn hoàn tiền.

## Khi nào chuyển nhân viên
Tiền đã trừ nhưng status chưa đúng, callback timeout kéo dài, amount conflict, suspected duplicate hoặc mọi yêu cầu refund.

## Nguồn đối chiếu
- Billing controllers, `InvoiceUseCaseImpl`, `PaymentUseCaseImpl`, `VnpayPaymentUseCaseImpl`.
- `InvoicePolicy`, `PaymentPolicy`, access guards.
- FE `SubscriptionPage`, `VnpayReturnPage`, `InvoiceManagementPage`.
- `billing.invoices`, `billing.payments`.

## Nội dung TBD
Refund policy đã chốt: quy trình Finance thủ công, Phase đầu chỉ full refund theo các điều kiện tại TBD-004; partial refund disabled. Refund aggregate, VNPAY/provider execution và reconciliation vẫn chưa được triển khai nên chatbot tiếp tục handoff.

## Lịch sử phiên bản
- 1.1.0: ghi nhận TBD-004 APPROVED ngày 2026-09-14; runtime refund vẫn chưa triển khai.
- 1.0.0: đánh dấu rõ refund chưa triển khai, 2026-09-14.
