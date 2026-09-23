<!--
title: Quản lý phương tiện khách hàng
slug: customer-vehicle-management
documentVersion: 1.1.0
status: DRAFT
language: vi
accessScope: CUSTOMER
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: CustomerVehicle controller, use case, access guard, policy và database
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-09-14
-->

# Quản lý phương tiện khách hàng

## Mục đích
Giải thích cách khách hàng quản lý phương tiện và giới hạn ownership.

## Đối tượng
Customer ACTIVE, APPROVED; nhân viên có permission ALL khi quản trị.

## Phạm vi
CRUD, trạng thái, xe mặc định, biển số và loại xe; không mô tả vehicle-type admin.

## Thuật ngữ
- Customer vehicle: xe thuộc hồ sơ customer.
- Default vehicle: xe mặc định, bắt buộc ACTIVE.

## Điều kiện trước
Customer hiện tại phải ACTIVE và APPROVED cho thao tác OWN; loại xe tham chiếu phải hợp lệ.

## Luồng chính
1. Mở `/customer/vehicles`.
2. Thêm biển số và loại xe; Backend lấy customerId từ account hiện tại.
3. Có thể xem, sửa, active/inactive và đặt/bỏ mặc định theo permission.

## Luồng thay thế
Người có permission ALL có thể quản lý xe theo customer và block khi cần.

## Luồng lỗi
Không được truy cập xe người khác; xe BLOCKED không được tự active/inactive; xe không ACTIVE không được đặt default; duplicate biển số/constraint trả conflict nếu có.

## Trạng thái và chuyển trạng thái
`ACTIVE ↔ INACTIVE`; `ACTIVE/INACTIVE → BLOCKED` bởi ALL. Quy tắc unblock chưa có endpoint customer. Xóa dùng endpoint delete theo policy hiện tại; semantics xóa vật lý/mềm cần kiểm chứng adapter.

## Câu hỏi khách hàng thường gặp
- Thêm xe ở đâu?
- Vì sao không đặt xe mặc định?
- Vì sao xe bị khóa?

## Điều chatbot được phép trả lời
Cách sử dụng màn hình, ý nghĩa trạng thái, điều kiện xe default.

## Điều chatbot phải dùng business tool
Danh sách/chi tiết/status xe của chính người dùng.

## Điều chatbot phải từ chối
Đọc/sửa xe người khác, tự unblock xe hoặc nhận giấy tờ xe đầy đủ qua chat.

## Khi nào chuyển nhân viên
Xe BLOCKED, ownership/biển số tranh chấp, lỗi constraint hoặc dữ liệu không đồng bộ.

## Nguồn đối chiếu
- `CustomerVehicleController`, `CustomerVehicleUseCaseImpl`.
- `CustomerVehicleAccessGuard`, `CustomerVehiclePolicy`.
- FE `VehiclePage.tsx`, `customerPortalApi.ts`.
- `people.customer_vehicles`, constraint status trong baseline Flyway.

## Nội dung TBD
Vehicle approval đã được chốt: không có approval riêng; customer phải APPROVED và vehicle phải ACTIVE. Chính sách xóa/retention và quy tắc unique biển số toàn hệ thống vẫn cần decision riêng.

## Lịch sử phiên bản
- 1.1.0: ghi nhận TBD-007 APPROVED ngày 2026-09-14; implementation/copy sync pending.
- 1.0.0: baseline từ mã nguồn ngày 2026-09-14.
