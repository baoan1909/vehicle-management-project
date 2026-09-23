<!--
title: Lịch sử gửi xe
slug: parking-history
documentVersion: 1.1.0
status: DRAFT
language: vi
accessScope: CUSTOMER
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Parking session API/access guard, FE history và database
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-09-14
-->

# Lịch sử gửi xe

## Mục đích
Hướng dẫn xem và truy vấn lịch sử gửi xe của chính khách hàng.

## Đối tượng
Customer được xác thực và approved; nhân viên có READ_ALL theo nghiệp vụ.

## Phạm vi
Thời gian vào/ra, trạng thái, biển số, phí và liên kết invoice ở mức được phép.

## Thuật ngữ
`OPEN` là đang gửi; `CLOSED` là đã ra; `LOST_CARD`/`CANCELLED` là trường hợp đặc biệt.

## Điều kiện trước
Customer dùng route `/customer/parking-history` và permission `PARKING_SESSION_READ_OWN`.

## Luồng chính
1. FE gọi endpoint `/api/parking/parking-sessions/me` với filter/page.
2. Backend resolve customer từ account hiện tại, không tin customerId client.
3. Hiển thị session summary; chi tiết nhạy cảm/ảnh chỉ khi storage access policy cho phép.

## Luồng thay thế
Nhân viên quản trị có READ_ALL xem danh sách theo filter trên màn hình parking session.

## Luồng lỗi
Chưa approved, thiếu permission, không có dữ liệu, session chưa đóng hoặc invoice/payment chưa đồng bộ.

## Trạng thái và chuyển trạng thái
OPEN→CLOSED/LOST_CARD/CANCELLED theo operational use case; chatbot không chuyển trạng thái.

## Câu hỏi khách hàng thường gặp
- Xem lịch sử ở đâu?
- Xe tôi đã ra chưa?
- Vì sao thiếu thời gian/phí?

## Điều chatbot được phép trả lời
Cách mở màn hình và ý nghĩa status.

## Điều chatbot phải dùng business tool
Danh sách/session/fee thực tế của customer hiện tại.

## Điều chatbot phải từ chối
Tra cứu theo customer/email/biển số người khác; trả ảnh/private URL khi chưa có policy; sửa lịch sử.

## Khi nào chuyển nhân viên
Discrepancy thời gian/biển số/phí, session OPEN bất thường hoặc dữ liệu không xuất hiện.

## Nguồn đối chiếu
- `ParkingSessionController` endpoint `/me`, use case/access guard.
- FE `CustomerHistoryPage.tsx`, `ParkingSessionPage.tsx`.
- `parking.parking_sessions`, `parking.parking_events`, invoices.

## Nội dung TBD
Chatbot image/masking đã chốt: không trả inline image/private URL; chỉ navigation card tới `/customer/parking-history`; biển số trong chatbot OWN chỉ giữ tối đa 3 ký tự cuối. Retention portal, export và dispute workflow cần decision riêng.

## Lịch sử phiên bản
- 1.1.0: ghi nhận TBD-022/TBD-023 APPROVED ngày 2026-09-14; implementation pending.
- 1.0.0: baseline ngày 2026-09-14.
