<!--
title: Bảng giá và cách tính phí
slug: pricing-and-fee-calculation
documentVersion: 1.1.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Public pricing API, price policies và checkout pricing policy
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-09-14
-->

# Bảng giá và cách tính phí

## Mục đích
Giải thích cấu trúc bảng giá và nguyên tắc không bịa số tiền.

## Đối tượng
Khách công khai, customer và nhân viên hỗ trợ.

## Phạm vi
Price plan/rule, loại xe/vé, unit, khung giờ và tính phí visitor đã thấy trong code.

## Thuật ngữ
Plan là tập giá theo audience; rule gắn vehicle/ticket/time/unit; unit DB hỗ trợ `TURN`, `DAY`, `MONTH`.

## Điều kiện trước
Giá được trả phải lấy từ rule active, đúng thời gian hiệu lực và đúng audience/vehicle/ticket.

## Luồng chính
1. Trang `/pricing` gọi public API lấy plans, rules, vehicle types và ticket types.
2. FE lọc theo visitor/customer và hiển thị base price/runtime data.
3. Checkout visitor dùng Backend policy với thời gian vào/ra và rule ngày/đêm; số tiền cuối do Backend tạo invoice.

## Luồng thay thế
Khi rule không có giá, trả “chưa cấu hình” và handoff; không dùng số trong ví dụ/mock/seed làm cam kết.

## Luồng lỗi
Rule trùng/xung đột, thiếu time pair, giá âm, timeFrom=timeTo hoặc không có rule phù hợp. Holiday calendar không tham gia chọn rule trong policy hiện tại.

## Trạng thái và chuyển trạng thái
Plan/rule có kích hoạt/vô hiệu hóa và effective period. Trạng thái chi tiết cần đọc runtime thay vì cache trong tài liệu.

## Câu hỏi khách hàng thường gặp
- Giá hiện tại theo loại xe là bao nhiêu?
- Phí theo lượt/ngày/tháng khác nhau thế nào?
- Gửi qua đêm tính ra sao?

## Điều chatbot được phép trả lời
Khái niệm, cách chọn rule ở mức đã xác nhận và hướng dẫn xem bảng giá.

## Điều chatbot phải dùng business tool
Bất kỳ số tiền hiện hành, rule áp dụng cho thời điểm cụ thể hoặc phép tính hóa đơn.

## Điều chatbot phải từ chối
Tự tính tiền bằng LLM, bịa giá/ngày lễ/phí phạt, cam kết giá tương lai.

## Khi nào chuyển nhân viên
Không có rule, nhiều rule mâu thuẫn, invoice khác công thức tool hoặc khiếu nại giá.

## Nguồn đối chiếu
- `PublicPricingController`, `PricePlanPolicy`, `PriceRulePolicy`.
- `ParkingCheckoutPricePolicy`, `ParkingCheckOutUseCaseImpl`.
- FE `PricingPage.tsx`, pricing API.
- catalog price tables và holiday calendar.

## Nội dung TBD
Holiday và tie-break đã được chốt: holiday không tham gia chọn rule; sau khi lọc đúng scope, priority nhỏ hơn thắng và tie còn lại phải fail-safe, không dùng `createdAt`. Công thức rounding/tax và giá trị tiền chính thức tiếp tục lấy từ runtime hoặc cần decision riêng.

## Lịch sử phiên bản
- 1.1.0: ghi nhận TBD-005/TBD-006 APPROVED ngày 2026-09-14; implementation pending.
- 1.0.0: baseline không chứa số giá, ngày 2026-09-14.
