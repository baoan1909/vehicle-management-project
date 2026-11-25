# Đối chiếu đợt 3 — code, UI, quyền và kiểm thử

Phạm vi phân tích ngày 20/11/2026. Có code không đồng nghĩa tích hợp thanh toán đã cấu hình tại DB dev. Chỉ tạo/sửa corpus tác giả và export; không publish dữ liệu.

## pricing-and-fee-calculation

Nguồn tri thức: [pricing-and-fee-calculation](../knowledge/pricing-and-fee-calculation.md).

Backend: [PublicPricingController.java](../../../../../src/main/java/com/ban/vehicle_management/entrypoint/controller/catalog/PublicPricingController.java), [PriceRulePolicy.java](../../../../../src/main/java/com/ban/vehicle_management/domain/catalog/pricerule/policy/PriceRulePolicy.java), [PriceRulePersistenceAdapter.java](../../../../../src/main/java/com/ban/vehicle_management/infrastructure/persistence/adapter/catalog/PriceRulePersistenceAdapter.java), [ParkingCheckoutPricePolicy.java](../../../../../src/main/java/com/ban/vehicle_management/domain/parking/parkingsession/policy/ParkingCheckoutPricePolicy.java), [ParkingCheckOutUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/parking/parkingsession/usecase/ParkingCheckOutUseCaseImpl.java).

Frontend: [PricingPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/customer-portal/pages/PricingPage.tsx).

Kiểm thử: [ParkingCheckoutPricePolicyTest.java](../../../../../src/test/java/com/ban/vehicle_management/domain/parking/parkingsession/policy/ParkingCheckoutPricePolicyTest.java).

## invoice-vnpay-payment-refund

Nguồn tri thức: [invoice-vnpay-payment-refund](../knowledge/invoice-vnpay-payment-refund.md).

Backend: [VnpayPaymentController.java](../../../../../src/main/java/com/ban/vehicle_management/entrypoint/controller/billing/VnpayPaymentController.java), [VnpayPaymentUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/billing/payment/usecase/VnpayPaymentUseCaseImpl.java), [SepayPaymentUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/billing/payment/usecase/SepayPaymentUseCaseImpl.java), [SepayGatewayAdapter.java](../../../../../src/main/java/com/ban/vehicle_management/infrastructure/payment/sepay/SepayGatewayAdapter.java), [WalletInvoicePaymentUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/billing/wallet/usecase/WalletInvoicePaymentUseCaseImpl.java), [InvoiceAccessGuard.java](../../../../../src/main/java/com/ban/vehicle_management/application/billing/invoice/authorization/InvoiceAccessGuard.java), [PaymentAccessGuard.java](../../../../../src/main/java/com/ban/vehicle_management/application/billing/payment/authorization/PaymentAccessGuard.java).

Frontend: [SubscriptionPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/customer-portal/pages/SubscriptionPage.tsx), [VnpayReturnPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/billing/pages/VnpayReturnPage.tsx), [invoicePaymentsApi.ts](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/billing/api/invoicePaymentsApi.ts).

Kiểm thử: [VnpayPaymentUseCaseImplTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/billing/payment/usecase/VnpayPaymentUseCaseImplTest.java), [SepayPaymentUseCaseImplTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/billing/payment/usecase/SepayPaymentUseCaseImplTest.java), [VnpayGatewayAdapterTest.java](../../../../../src/test/java/com/ban/vehicle_management/infrastructure/payment/vnpay/VnpayGatewayAdapterTest.java).

## voucher-and-promotions

Nguồn tri thức: [voucher-and-promotions](../knowledge/voucher-and-promotions.md).

Backend: [VoucherPolicy.java](../../../../../src/main/java/com/ban/vehicle_management/domain/catalog/voucher/policy/VoucherPolicy.java), [VoucherUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/catalog/voucher/usecase/VoucherUseCaseImpl.java), [VoucherAccessGuard.java](../../../../../src/main/java/com/ban/vehicle_management/application/catalog/voucher/authorization/VoucherAccessGuard.java), [VoucherPersistenceAdapter.java](../../../../../src/main/java/com/ban/vehicle_management/infrastructure/persistence/adapter/catalog/VoucherPersistenceAdapter.java).

Frontend: [SubscriptionPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/customer-portal/pages/SubscriptionPage.tsx), [VoucherPromotionBanner.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/customer-portal/components/VoucherPromotionBanner.tsx).

Kiểm thử: [VoucherPolicyTest.java](../../../../../src/test/java/com/ban/vehicle_management/domain/catalog/voucher/policy/VoucherPolicyTest.java), [VoucherAccessGuardTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/catalog/voucher/authorization/VoucherAccessGuardTest.java).

## reservations-and-holds

Nguồn tri thức: [reservations-and-holds](../knowledge/reservations-and-holds.md).

Backend: [CustomerReservationController.java](../../../../../src/main/java/com/ban/vehicle_management/entrypoint/controller/parking/CustomerReservationController.java), [PublicCustomerReservationController.java](../../../../../src/main/java/com/ban/vehicle_management/entrypoint/controller/parking/PublicCustomerReservationController.java), [CustomerReservationUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/parking/customerreservation/usecase/CustomerReservationUseCaseImpl.java), [CustomerReservationPersistenceAdapter.java](../../../../../src/main/java/com/ban/vehicle_management/infrastructure/persistence/adapter/parking/CustomerReservationPersistenceAdapter.java), [CustomerReservationExpiryScheduler.java](../../../../../src/main/java/com/ban/vehicle_management/infrastructure/scheduler/parking/CustomerReservationExpiryScheduler.java), [ParkingCheckOutUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/parking/parkingsession/usecase/ParkingCheckOutUseCaseImpl.java).

Frontend: [CustomerReservationPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/parking/pages/CustomerReservationPage.tsx), [customerReservationApi.ts](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/parking/api/customerReservationApi.ts).

Kiểm thử: [CustomerReservationUseCaseImplTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/parking/customerreservation/usecase/CustomerReservationUseCaseImplTest.java), [GuestReservationCheckoutUseCaseTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/parking/customerreservation/usecase/GuestReservationCheckoutUseCaseTest.java).

## Ranh giới nghiệp vụ

- Giá công khai không phải dữ liệu hóa đơn cá nhân. Bộ tính lượt gửi dùng mốc bốn giờ/hai mươi bốn giờ và khung ngày/đêm; không thay chính sách đó trong đợt này.
- Thanh toán: giữ kiểm tra OWN/ALL, chữ ký, số tiền, giao dịch trùng và trạng thái. VNPay, ví, tại quầy và SePay có phạm vi khác nhau. Chưa có luồng thực thi hoàn tiền tự động được xác minh.
- Voucher: xem thử không giữ lượt; giữ lượt qua khóa voucher khi tạo hóa đơn; giới hạn tính cả lượt đang giữ và đã dùng. Không cam kết xếp chồng nhiều mã hoặc áp cho mọi dịch vụ.
- Đặt chỗ đã có cả công khai và tài khoản. Khách công khai cần mã truy cập bí mật; tài khoản cần PAYMENT_CREATE_OWN và khách đã duyệt, kiểm tra sở hữu. Không đưa token/QR vào chatbot.
- Tạo đặt chỗ khóa khu vực và kiểm tra chỗ/sức chứa, biển số, lượt gửi đang mở. Giữ thanh toán mười phút; hạn vào bãi sáu mươi phút từ trả tiền. Callback đến trễ có thể chuyển đối soát, không bảo đảm có chỗ.
- Tính phí đặt chỗ từ paidAt theo bảng giá chụp lại, trừ trả trước, phần còn lại không âm. Hủy/không đến không tự hoàn tiền.
- AiToolRegistry hiện chỉ có các tool đọc xe/vé/lịch sử/phiếu và ghi tạo phiếu hỗ trợ. Không có tool thanh toán, voucher, hoàn tiền hoặc đặt chỗ; câu hỏi cá nhân/hành động tương ứng được dẫn về module, không dùng tạo phiếu thay nghiệp vụ.

## Tình huống và tiêu chí

| Tình huống | Tài liệu | Ý bắt buộc | Tập |
|---|---|---|---|
| B3-PRI01 | pricing-and-fee-calculation | Bảng giá, bãi xe, loại xe | regression |
| B3-PRI02 | pricing-and-fee-calculation | bãi, loại xe, thời điểm | acceptance |
| B3-PRI03 | pricing-and-fee-calculation | bốn giờ, ban ngày, bảng giá | regression |
| B3-PRI04 | pricing-and-fee-calculation | bốn giờ, ban đêm, bảng giá | acceptance |
| B3-PRI05 | pricing-and-fee-calculation | hai mươi bốn giờ, ban ngày, ban đêm | regression |
| B3-PRI06 | pricing-and-fee-calculation | cùng ngày, ban ngày, hệ thống | regression |
| B3-PRI07 | pricing-and-fee-calculation | bãi, loại xe, không | acceptance |
| B3-PRI08 | pricing-and-fee-calculation | bãi, thời gian, trả trước | regression |
| B3-PRI09 | pricing-and-fee-calculation | không, ngày lễ, bảng giá | acceptance |
| B3-PRI10 | pricing-and-fee-calculation | thanh toán thành công, trả trước, phát sinh | regression |
| B3-PAY01 | invoice-vnpay-payment-refund | Vé tháng, số tiền, xác minh | regression |
| B3-PAY02 | invoice-vnpay-payment-refund | chưa, lần nữa, xác minh | acceptance |
| B3-PAY03 | invoice-vnpay-payment-refund | thất bại, xác minh, cho phép | regression |
| B3-PAY04 | invoice-vnpay-payment-refund | hệ thống, xác minh, không | acceptance |
| B3-PAY05 | invoice-vnpay-payment-refund | trùng, không, đối chiếu | regression |
| B3-PAY06 | invoice-vnpay-payment-refund | đặt chỗ, SePay, không | regression |
| B3-PAY07 | invoice-vnpay-payment-refund | Vé tháng, ví, xác nhận | acceptance |
| B3-PAY08 | invoice-vnpay-payment-refund | tại quầy, chưa, thực tế | regression |
| B3-PAY09 | invoice-vnpay-payment-refund | chưa, hoàn tiền, không cam kết | acceptance |
| B3-PAY10 | invoice-vnpay-payment-refund | hóa đơn, Vé tháng, không | regression |
| B3-VOU01 | voucher-and-promotions | Vé tháng, mã ưu đãi, số tiền | regression |
| B3-VOU02 | voucher-and-promotions | thời gian, hiệu lực, không | acceptance |
| B3-VOU03 | voucher-and-promotions | tối thiểu, bãi, không | regression |
| B3-VOU04 | voucher-and-promotions | đối tác, bãi, phạm vi | acceptance |
| B3-VOU05 | voucher-and-promotions | tối đa, giá trị vé, hệ thống | regression |
| B3-VOU06 | voucher-and-promotions | giới hạn, đang được giữ, không | regression |
| B3-VOU07 | voucher-and-promotions | chưa giữ, hóa đơn, kiểm tra lại | acceptance |
| B3-VOU08 | voucher-and-promotions | đang được giữ, giải phóng, thành công | regression |
| B3-VOU09 | voucher-and-promotions | kiểm tra lại, hết lượt, không | acceptance |
| B3-VOU10 | voucher-and-promotions | một mã, hóa đơn, chưa | regression |
| B3-RES01 | reservations-and-holds | Đặt chỗ trước, biển số, thanh toán | regression |
| B3-RES02 | reservations-and-holds | chưa đăng nhập, trình duyệt, mã truy cập | acceptance |
| B3-RES03 | reservations-and-holds | mười phút, giải phóng, đối soát | regression |
| B3-RES04 | reservations-and-holds | sáu mươi phút, thanh toán thành công, giải phóng | acceptance |
| B3-RES05 | reservations-and-holds | thanh toán thành công, ra, trả trước | regression |
| B3-RES06 | reservations-and-holds | không được hoàn tiền, chưa sử dụng, giải phóng | regression |
| B3-RES07 | reservations-and-holds | kiểm tra lại, người khác, chọn lại | acceptance |
| B3-RES08 | reservations-and-holds | biển số, chưa kết thúc, không | regression |
| B3-RES09 | reservations-and-holds | đối soát, chưa, giữ chỗ | acceptance |
| B3-RES10 | reservations-and-holds | từng ô, sức chứa, loại xe | regression |

Mỗi tình huống có sáu biến thể trong dataset; không chia các biến thể cùng nghĩa sang hai tập. ExpectedDatabaseEffect=NONE. PUBLIC: bảng giá, voucher và đặt chỗ hướng dẫn; CUSTOMER: hóa đơn/thanh toán. Kiểm thử route tích lũy và kiểm thử generation là hai lớp bằng chứng riêng.

## Giới hạn kiểm chứng

Test use case dùng mock port không chứng minh chống cạnh tranh trên DB thật. PostgreSQL của knowledge harness là DB Testcontainers riêng; actor/chat/audit và business tool ports có fixture. Live harness hiện kiểm chứng runtime trợ lý trên tài khoản tổng hợp, không phải UAT guest/nhân sự trên trình duyệt. Các trường hợp ngân hàng, callback thật, nhiều thiết bị và đối soát thực tế còn cần môi trường chuyên biệt.
