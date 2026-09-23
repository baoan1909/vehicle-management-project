<!--
title: Quy trình xe vào và xe ra
slug: parking-entry-exit
documentVersion: 1.0.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Parking check-in/out use cases, policies, FE vận hành và Flyway
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-09-14
-->

# Quy trình xe vào và xe ra

## Mục đích
Giải thích quy trình gửi xe mà không cho customer chatbot điều khiển thiết bị.

## Đối tượng
Khách vãng lai, customer và nhân viên vận hành.

## Phạm vi
Làn/cổng/zone/bãi, OCR, thẻ, biển số, session, invoice visitor và lỗi vận hành.

## Thuật ngữ
Session là phiên gửi xe; event là dấu vết check-in/check-out/manual review; topology là lane→gate→zone→lot.

## Điều kiện trước
Topology ACTIVE, đúng hướng làn, còn sức chứa, loại xe được zone chấp nhận, thẻ và ảnh đầu vào hợp lệ.

## Luồng chính
1. Xe vào làn IN; operator quét thẻ và OCR ảnh.
2. Visitor dùng thẻ AVAILABLE; subscription card phải ASSIGNED và subscription/customer/vehicle hợp lệ.
3. Backend tạo session OPEN/event CHECK_IN và chuyển card IN_USE.
4. Xe ra làn OUT; card IN_USE và biển số ra khớp biển số vào.
5. Subscription hoàn tất trực tiếp; visitor prepare checkout, tạo invoice, thanh toán rồi đóng session.

## Luồng thay thế
OCR confidence thấp hoặc mismatch phải manual review bởi operator. Payment visitor có thể hoàn tất sau callback và completion idempotent.

## Luồng lỗi
Sai hướng, lane/gate/zone/lot maintenance/closed, bãi đầy, thẻ sai trạng thái, card có session mở, plate mismatch, ảnh rỗng, visitor chưa paid.

## Trạng thái và chuyển trạng thái
Session `OPEN → CLOSED`; `LOST_CARD` và `CANCELLED` có policy riêng. Card `AVAILABLE/ASSIGNED → IN_USE → AVAILABLE/ASSIGNED`. Event: CHECK_IN, CHECK_OUT_PENDING, CHECK_OUT, MANUAL_REVIEW, BARRIER_OPEN.

## Câu hỏi khách hàng thường gặp
- Vào/ra bãi theo bước nào?
- Vì sao không mở cổng?
- OCR sai biển số thì làm gì?

## Điều chatbot được phép trả lời
Quy trình chung, yêu cầu gặp nhân viên khi lỗi và trạng thái phiên qua tool OWN.

## Điều chatbot phải dùng business tool
Phiên đang mở/lịch sử của người dùng; số tiền phải từ invoice/pricing tool.

## Điều chatbot phải từ chối
Mở barrier, check-in/out, bỏ qua plate/card, tự chấp nhận OCR hoặc sửa event.

## Khi nào chuyển nhân viên
Mọi sự cố tại cổng, mismatch, lost card, thiết bị/topology lỗi, payment chưa đồng bộ.

## Nguồn đối chiếu
- `ParkingSessionController`, `ParkingOcrController`.
- Check-in/out use cases và policies.
- FE `SwipeEntryPage`, `SwipeListPage`, `ParkingSessionPage`.
- parking topology/session/event, cards, invoices/payments.

## Nội dung TBD
Ngưỡng OCR nghiệp vụ, SOP manual review, sự cố mất điện/network, barrier command và SLA tại cổng.

## Lịch sử phiên bản
- 1.0.0: baseline ngày 2026-09-14.

