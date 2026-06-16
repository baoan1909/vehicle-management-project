# Hướng Dẫn Test Subscription API Bằng Postman

Tài liệu này hướng dẫn test API Subscription theo hướng thực tế bằng Postman, tận dụng các API nền đã có trong backend.

Base URL:

```text
http://localhost:8080/vehicle-management
```

Base Subscription API:

```text
/api/access-control/subscriptions
```

Quy ước token trong Postman:

```text
{{customer_token}} = access token của CUSTOMER
{{manager_token}}  = access token của PARKING_MANAGER
```

Các ID cần lưu lại trong Postman environment:

```text
{{customer_id}}
{{customer_vehicle_id}}
{{vehicle_type_id}}
{{ticket_type_id}}
{{price_plan_id}}
{{price_rule_id}}
{{parking_lot_id}}
{{zone_id}}
{{card_type_id}}
{{card_id}}
{{subscription_id}}
{{invoice_id}}
```

## 1. Luồng Test Cho Customer

Mục tiêu của luồng này:

```text
Customer đăng ký tài khoản -> cập nhật profile -> được manager approve -> tạo xe -> tạo subscription của chính mình.
```

### 1.1. Customer Đăng Ký Tài Khoản

Request:

```http
POST {{base_url}}/api/public/auth/register
```

Body:

```json
{
  "username": "customer_subscription_test",
  "email": "customer.subscription.test@gmail.com",
  "password": "123456"
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Please verify your email address.",
  "data": {
    "accountId": "...",
    "username": "customer_subscription_test",
    "email": "customer.subscription.test@gmail.com"
  }
}
```

Ghi lại:

```text
accountId nếu response có trả về.
```

Lưu ý:

```text
Nếu hệ thống đang bật xác thực email, cần verify email hoặc dùng tài khoản customer đã verify sẵn.
Sau khi đăng ký, đăng nhập qua Keycloak/frontend để lấy {{customer_token}}.
```

### 1.2. Customer Kiểm Tra Trạng Thái Onboarding

Request:

```http
GET {{base_url}}/api/iam/accounts/onboarding
```

Authorization:

```text
Bearer {{customer_token}}
```

Kết quả mong muốn khi chưa có profile:

```text
profile chưa hoàn tất hoặc customer chưa approved.
```

### 1.3. Customer Cập Nhật Profile

Request:

```http
POST {{base_url}}/api/iam/accounts/onboarding
```

Authorization:

```text
Bearer {{customer_token}}
```

Body:

```json
{
  "fullName": "Nguyen Van Customer",
  "phoneNumber": "+84901230001",
  "dateOfBirth": "2003-09-19",
  "gender": "MALE",
  "address": "Thanh pho Ho Chi Minh",
  "identifyCard": "079203000001",
  "avatarUrl": "https://cdn.example.com/avatar/customer-test.jpg"
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Onboarding completed successfully",
  "data": {
    "profileCompleted": true,
    "customerStatus": "ACTIVE",
    "customerApprovalStatus": "PENDING"
  }
}
```

Nếu response có `customerId`, lưu vào:

```text
{{customer_id}}
```

Nếu chưa có `customerId` trong response, lát nữa manager lấy qua API approval/customer.

### 1.4. Manager Xem Yêu Cầu Onboarding Customer

Request:

```http
GET {{base_url}}/api/operations/approval-requests/customer-onboarding
```

Authorization:

```text
Bearer {{manager_token}}
```

Có thể filter nếu API đang hỗ trợ:

```http
GET {{base_url}}/api/operations/approval-requests/customer-onboarding?keyword=customer_subscription_test
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Fetched customer onboarding approval requests successfully",
  "data": [
    {
      "approvalRequestId": "...",
      "status": "PENDING",
      "customerId": "..."
    }
  ]
}
```

Ghi lại:

```text
{{approval_request_id}}
{{customer_id}}
```

### 1.5. Manager Approve Customer

Request:

```http
PATCH {{base_url}}/api/operations/approval-requests/customer-onboarding/{{approval_request_id}}/approve
```

Authorization:

```text
Bearer {{manager_token}}
```

Body:

```json
{
  "note": "Approve customer để test subscription"
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Customer onboarding approval request approved successfully",
  "data": {
    "status": "APPROVED",
    "customerId": "{{customer_id}}"
  }
}
```

Sau bước này customer phải đạt điều kiện:

```text
customer.status = ACTIVE
customer.approvalStatus = APPROVED
```

### 1.6. Customer Tạo Xe Của Mình

Trước bước này cần có `vehicleTypeId`. Nếu chưa có, manager tạo ở phần 2.2.

Request:

```http
POST {{base_url}}/api/people/customer-vehicles
```

Authorization:

```text
Bearer {{customer_token}}
```

Body:

```json
{
  "customerId": null,
  "vehicleTypeId": "{{vehicle_type_id}}",
  "licensePlate": "SUB-CUS-001",
  "brand": "Honda",
  "color": "Black",
  "isDefault": true
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Customer vehicle created successfully",
  "data": {
    "customerVehicleId": "...",
    "customerId": "{{customer_id}}",
    "vehicleTypeId": "{{vehicle_type_id}}",
    "licensePlate": "SUB-CUS-001",
    "status": "ACTIVE"
  }
}
```

Ghi lại:

```text
{{customer_vehicle_id}}
```

Lưu ý:

```text
Với customer token, customerId trong body nên để null.
Backend tự lấy customerId từ token.
```

### 1.7. Customer Tạo Subscription Của Mình

Điều kiện trước khi gọi:

```text
Customer ACTIVE + APPROVED
Customer vehicle ACTIVE
Ticket type ACTIVE
Price plan CUSTOMER active
Price rule active đúng vehicleTypeId + ticketTypeId
Zone ACTIVE còn capacity
Card AVAILABLE đúng vehicleTypeId
```

Request:

```http
POST {{base_url}}/api/access-control/subscriptions/me
```

Authorization:

```text
Bearer {{customer_token}}
```

Body:

```json
{
  "customerVehicleId": "{{customer_vehicle_id}}",
  "ticketTypeId": "{{ticket_type_id}}",
  "requestedEffectiveFrom": "2026-06-20"
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Subscription created successfully",
  "data": {
    "subscriptionId": "...",
    "customerId": "{{customer_id}}",
    "customerVehicleId": "{{customer_vehicle_id}}",
    "ticketTypeId": "{{ticket_type_id}}",
    "status": "PENDING",
    "cardId": null,
    "requestedEffectiveFrom": "2026-06-20"
  }
}
```

Ghi lại:

```text
{{subscription_id}}
```

Lưu ý ngày:

```text
requestedEffectiveFrom phải nằm trong khoảng hôm nay + 2 đến hôm nay + 7.
Ví dụ hôm nay là 2026-06-16 thì ngày hợp lệ là 2026-06-18 đến 2026-06-23.
```

### 1.8. Customer Xem Danh Sách Subscription Của Mình

Request:

```http
GET {{base_url}}/api/access-control/subscriptions
```

Authorization:

```text
Bearer {{customer_token}}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Fetched subscriptions successfully",
  "data": [
    {
      "subscriptionId": "{{subscription_id}}",
      "customerId": "{{customer_id}}",
      "status": "PENDING"
    }
  ]
}
```

Quan trọng:

```text
Customer chỉ thấy subscription của chính mình.
```

### 1.9. Customer Xem Chi Tiết Subscription

Request:

```http
GET {{base_url}}/api/access-control/subscriptions/{{subscription_id}}
```

Authorization:

```text
Bearer {{customer_token}}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Fetched subscription successfully",
  "data": {
    "subscriptionId": "{{subscription_id}}",
    "customerId": "{{customer_id}}",
    "status": "PENDING"
  }
}
```

### 1.10. Customer Cập Nhật Subscription Khi Còn PENDING

Request:

```http
PUT {{base_url}}/api/access-control/subscriptions/{{subscription_id}}
```

Authorization:

```text
Bearer {{customer_token}}
```

Body:

```json
{
  "customerVehicleId": "{{customer_vehicle_id}}",
  "ticketTypeId": "{{ticket_type_id}}",
  "requestedEffectiveFrom": "2026-06-21"
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Subscription updated successfully",
  "data": {
    "subscriptionId": "{{subscription_id}}",
    "status": "PENDING",
    "requestedEffectiveFrom": "2026-06-21"
  }
}
```

### 1.11. Customer Hủy Subscription Khi Còn PENDING

Request:

```http
PATCH {{base_url}}/api/access-control/subscriptions/{{subscription_id}}/cancel
```

Authorization:

```text
Bearer {{customer_token}}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Subscription cancelled successfully",
  "data": {
    "subscriptionId": "{{subscription_id}}",
    "status": "CANCELLED"
  }
}
```

Có thể test `DELETE` tương đương:

```http
DELETE {{base_url}}/api/access-control/subscriptions/{{subscription_id}}
```

## 2. Luồng Chuẩn Bị Dữ Liệu Nền Bằng Manager

Mục tiêu:

```text
Manager chuẩn bị vehicle type, ticket type, price plan, price rule, parking lot, zone, card type, card.
```

Tất cả request trong phần này dùng:

```text
Authorization: Bearer {{manager_token}}
```

### 2.1. Tạo Vehicle Type

Nếu đã có vehicle type xe máy/ô tô thì có thể dùng lại.

Request:

```http
POST {{base_url}}/api/catalog/vehicle-types
```

Body:

```json
{
  "code": "MOTORBIKE",
  "name": "Xe máy",
  "description": "Loại xe máy dùng để test subscription",
  "isActive": true
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Vehicle type created successfully",
  "data": {
    "vehicleTypeId": "...",
    "code": "MOTORBIKE",
    "isActive": true
  }
}
```

Ghi lại:

```text
{{vehicle_type_id}}
```

Nếu bị trùng code, dùng:

```http
GET {{base_url}}/api/catalog/vehicle-types?keyword=MOTORBIKE
```

để lấy `vehicleTypeId` có sẵn.

### 2.2. Tạo Ticket Type

Subscription chỉ dùng:

```text
MONTHLY
QUARTERLY
YEARLY
FREE
```

Request:

```http
POST {{base_url}}/api/catalog/ticket-types
```

Body:

```json
{
  "code": "MONTHLY",
  "name": "Vé tháng",
  "description": "Vé tháng dùng để test subscription"
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Ticket type created successfully",
  "data": {
    "ticketTypeId": "...",
    "code": "MONTHLY",
    "durationDays": 30,
    "status": "ACTIVE"
  }
}
```

Ghi lại:

```text
{{ticket_type_id}}
```

Nếu code đã tồn tại, dùng:

```http
GET {{base_url}}/api/catalog/ticket-types?status=ACTIVE&keyword=MONTHLY
```

### 2.3. Tạo Price Plan Cho Customer

Request:

```http
POST {{base_url}}/api/catalog/price-plans
```

Body:

```json
{
  "code": "CUSTOMER-SUB-2026",
  "name": "Bảng giá khách đăng ký 2026",
  "description": "Bảng giá dùng cho subscription",
  "appliesTo": "CUSTOMER",
  "effectiveFrom": "2026-01-01",
  "effectiveTo": "2026-12-31"
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Price plan created successfully",
  "data": {
    "pricePlanId": "...",
    "appliesTo": "CUSTOMER",
    "isActive": true
  }
}
```

Ghi lại:

```text
{{price_plan_id}}
```

### 2.4. Tạo Price Rule Cho Subscription

Request:

```http
POST {{base_url}}/api/catalog/price-rules
```

Body:

```json
{
  "pricePlanId": "{{price_plan_id}}",
  "vehicleTypeId": "{{vehicle_type_id}}",
  "ticketTypeId": "{{ticket_type_id}}",
  "ruleName": "Giá vé tháng xe máy",
  "timeFrom": null,
  "timeTo": null,
  "basePrice": 140000,
  "unit": "MONTH",
  "lostCardFee": 50000,
  "priority": 1
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Price rule created successfully",
  "data": {
    "priceRuleId": "...",
    "vehicleTypeId": "{{vehicle_type_id}}",
    "ticketTypeId": "{{ticket_type_id}}",
    "basePrice": 140000,
    "isActive": true
  }
}
```

Ghi lại:

```text
{{price_rule_id}}
```

### 2.5. Tạo Parking Lot

Nếu đã có bãi HCMUTE thì dùng lại.

Request:

```http
POST {{base_url}}/api/parking/parking-lots
```

Body:

```json
{
  "code": "HCMUTE",
  "name": "Bãi xe HCMUTE",
  "address": "01 Vo Van Ngan, Thu Duc",
  "totalCapacity": 2000
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Parking lot created successfully",
  "data": {
    "parkingLotId": "...",
    "code": "HCMUTE",
    "status": "ACTIVE"
  }
}
```

Ghi lại:

```text
{{parking_lot_id}}
```

### 2.6. Tạo Zone Có Capacity Cho Loại Xe

Request:

```http
POST {{base_url}}/api/parking/zones
```

Body:

```json
{
  "parkingLotId": "{{parking_lot_id}}",
  "code": "MOTO-SUB-ZONE",
  "name": "Khu xe máy test subscription",
  "vehicleTypeId": "{{vehicle_type_id}}",
  "capacity": 100
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Zone created successfully",
  "data": {
    "zoneId": "...",
    "vehicleTypeId": "{{vehicle_type_id}}",
    "capacity": 100,
    "status": "ACTIVE"
  }
}
```

Ghi lại:

```text
{{zone_id}}
```

### 2.7. Tạo Card Type

Nếu đã có loại thẻ thì dùng lại.

Request:

```http
POST {{base_url}}/api/catalog/card-types
```

Body:

```json
{
  "code": "MONTHLY_CARD",
  "name": "Thẻ vé tháng",
  "description": "Thẻ dùng cho khách đăng ký",
  "isReturnRequired": true,
  "isActive": true
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Card type created successfully",
  "data": {
    "cardTypeId": "...",
    "code": "MONTHLY_CARD",
    "isActive": true
  }
}
```

Ghi lại:

```text
{{card_type_id}}
```

### 2.8. Tạo Card AVAILABLE Cho Vehicle Type

Request:

```http
POST {{base_url}}/api/access-control/cards
```

Body:

```json
{
  "cardNumber": "SUB-CARD-001",
  "uid": "SUB-UID-001",
  "cardTypeId": "{{card_type_id}}",
  "vehicleTypeId": "{{vehicle_type_id}}"
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Card created successfully",
  "data": {
    "cardId": "...",
    "cardNumber": "SUB-CARD-001",
    "vehicleTypeId": "{{vehicle_type_id}}",
    "status": "AVAILABLE"
  }
}
```

Ghi lại:

```text
{{card_id}}
```

## 3. Luồng Test Manager Cho Subscription

Mục tiêu:

```text
Manager xem/tạo/duyệt/từ chối/cấp thẻ/hủy subscription.
```

### 3.1. Manager Tạo Subscription Giùm Customer

Request:

```http
POST {{base_url}}/api/access-control/subscriptions
```

Authorization:

```text
Bearer {{manager_token}}
```

Body:

```json
{
  "customerId": "{{customer_id}}",
  "customerVehicleId": "{{customer_vehicle_id}}",
  "ticketTypeId": "{{ticket_type_id}}",
  "requestedEffectiveFrom": "2026-06-20"
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Subscription created successfully",
  "data": {
    "subscriptionId": "...",
    "customerId": "{{customer_id}}",
    "status": "PENDING"
  }
}
```

Ghi lại:

```text
{{subscription_id}}
```

### 3.2. Manager Xem Tất Cả Subscription

Request:

```http
GET {{base_url}}/api/access-control/subscriptions
```

Authorization:

```text
Bearer {{manager_token}}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Fetched subscriptions successfully",
  "data": [
    {
      "subscriptionId": "{{subscription_id}}",
      "customerId": "{{customer_id}}",
      "status": "PENDING"
    }
  ]
}
```

Filter thường dùng:

```http
GET {{base_url}}/api/access-control/subscriptions?status=PENDING
GET {{base_url}}/api/access-control/subscriptions?keyword=SUB-CUS
GET {{base_url}}/api/access-control/subscriptions?customerId={{customer_id}}
```

### 3.3. Manager Duyệt Subscription

Request:

```http
PATCH {{base_url}}/api/access-control/subscriptions/{{subscription_id}}/approve
```

Authorization:

```text
Bearer {{manager_token}}
```

Kết quả mong muốn với vé có phí:

```json
{
  "success": true,
  "message": "Subscription approved successfully",
  "data": {
    "subscriptionId": "{{subscription_id}}",
    "status": "PENDING_PAYMENT",
    "cardId": "...",
    "approvedBy": "..."
  }
}
```

Kỳ vọng sau approve:

```text
Subscription chuyển PENDING -> PENDING_PAYMENT.
Card được chọn chuyển AVAILABLE -> RESERVED.
Invoice được tạo với status UNPAID.
```

Kiểm tra invoice bằng API:

```http
GET {{base_url}}/api/billing/invoices?subscriptionId={{subscription_id}}
```

Authorization:

```text
Bearer {{manager_token}}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Fetched invoices successfully",
  "data": [
    {
      "invoiceId": "...",
      "subscriptionId": "{{subscription_id}}",
      "amount": 140000,
      "discountAmount": 0,
      "finalAmount": 140000,
      "status": "UNPAID"
    }
  ]
}
```

Ghi lại:

```text
{{invoice_id}}
```

### 3.4. Manager Từ Chối Subscription

Chỉ dùng cho subscription đang `PENDING`.

Request:

```http
PATCH {{base_url}}/api/access-control/subscriptions/{{subscription_id}}/reject
```

Authorization:

```text
Bearer {{manager_token}}
```

Body:

```json
{
  "reason": "Khách hàng không đủ điều kiện đăng ký vé."
}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Subscription rejected successfully",
  "data": {
    "subscriptionId": "{{subscription_id}}",
    "status": "REJECTED",
    "rejectionReason": "Khách hàng không đủ điều kiện đăng ký vé."
  }
}
```

### 3.5. Thanh Toán Invoice

Hiện trong controller đã rà thấy có `InvoiceController`, nhưng chưa thấy `PaymentController` trong nhánh hiện tại.

Vì vậy có 2 trường hợp:

#### Trường hợp A: Payment API đã được bạn thêm ở nhánh hiện tại

Request dự kiến:

```http
POST {{base_url}}/api/billing/invoices/{{invoice_id}}/payments
```

Authorization:

```text
Bearer {{manager_token}}
```

Body:

```json
{
  "paymentMethod": "CASH",
  "amount": 140000,
  "transactionRef": null,
  "note": "Khách thanh toán tiền mặt"
}
```

Kết quả mong muốn:

```text
Invoice chuyển UNPAID -> PAID.
Subscription chuyển PENDING_PAYMENT -> PENDING_CARD.
```

#### Trường hợp B: Payment API chưa có

Luồng manager sẽ dừng ở:

```text
Subscription status = PENDING_PAYMENT
Invoice status = UNPAID
```

Khi đó chưa gọi được:

```http
PATCH /api/access-control/subscriptions/{subscriptionId}/assign-card
```

vì API assign-card yêu cầu invoice đã `PAID`.

### 3.6. Manager Cấp Thẻ Cho Subscription

Chỉ gọi khi:

```text
subscription.status = PENDING_CARD
invoice.status = PAID
card.status = RESERVED
```

Request:

```http
PATCH {{base_url}}/api/access-control/subscriptions/{{subscription_id}}/assign-card
```

Authorization:

```text
Bearer {{manager_token}}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Subscription card assigned successfully",
  "data": {
    "subscriptionId": "{{subscription_id}}",
    "status": "ACTIVE",
    "cardReceiptDate": "2026-06-16"
  }
}
```

Kỳ vọng:

```text
Card chuyển RESERVED -> ASSIGNED.
Subscription chuyển PENDING_CARD -> ACTIVE.
effectiveFrom = max(requestedEffectiveFrom, cardReceiptDate).
```

### 3.7. Manager Hủy Subscription

Request:

```http
PATCH {{base_url}}/api/access-control/subscriptions/{{subscription_id}}/cancel
```

Authorization:

```text
Bearer {{manager_token}}
```

Kết quả mong muốn khi subscription đang `PENDING`:

```json
{
  "success": true,
  "message": "Subscription cancelled successfully",
  "data": {
    "status": "CANCELLED"
  }
}
```

Kết quả mong muốn khi subscription đang `PENDING_PAYMENT`:

```text
Subscription -> CANCELLED.
Invoice UNPAID -> CANCELLED.
Card RESERVED -> AVAILABLE.
```

### 3.8. Manager Đánh Dấu Hết Hạn

Chỉ gọi khi subscription đang `ACTIVE` và hiện tại đã qua `effectiveTo`.

Request:

```http
PATCH {{base_url}}/api/access-control/subscriptions/{{subscription_id}}/expire
```

Authorization:

```text
Bearer {{manager_token}}
```

Kết quả mong muốn:

```json
{
  "success": true,
  "message": "Subscription expired successfully",
  "data": {
    "status": "EXPIRED"
  }
}
```

Nếu chưa hết hạn, kết quả mong muốn:

```text
409 Conflict
Subscription has not expired yet
```

## 4. Test Các Lỗi Quan Trọng

### 4.1. Customer Không Có Quyền Tạo Subscription

Request:

```http
POST {{base_url}}/api/access-control/subscriptions/me
```

Token:

```text
Customer token thiếu SUBSCRIPTION_CREATE_OWN.
```

Kết quả mong muốn:

```json
{
  "success": false,
  "message": "Access is denied",
  "data": {
    "status": 403,
    "error": "Forbidden"
  }
}
```

### 4.2. Customer Chưa Approved

Điều kiện:

```text
Customer profile đã có nhưng approvalStatus chưa APPROVED.
```

Kết quả mong muốn:

```text
403 Forbidden
```

Lý do:

```text
SubscriptionAccessGuard chỉ cho customer ACTIVE + APPROVED tạo subscription.
```

### 4.3. Xe Không Thuộc Customer

Body:

```json
{
  "customerVehicleId": "ID_XE_CUA_CUSTOMER_KHAC",
  "ticketTypeId": "{{ticket_type_id}}",
  "requestedEffectiveFrom": "2026-06-20"
}
```

Kết quả mong muốn:

```text
400 Bad Request
Customer vehicle does not belong to customer
```

### 4.4. Ngày Hiệu Lực Không Hợp Lệ

Body:

```json
{
  "customerVehicleId": "{{customer_vehicle_id}}",
  "ticketTypeId": "{{ticket_type_id}}",
  "requestedEffectiveFrom": "2026-12-31"
}
```

Kết quả mong muốn:

```text
400 Bad Request
requestedEffectiveFrom must be between ...
```

### 4.5. Thiếu Price Rule

Điều kiện:

```text
Không có price rule active cho vehicleTypeId + ticketTypeId.
```

Kết quả mong muốn:

```text
404 Not Found
Active subscription price rule not found
```

### 4.6. Hết Card Available

Điều kiện:

```text
Không còn card AVAILABLE cho vehicleTypeId.
```

Kết quả mong muốn khi approve:

```text
409 Conflict
No available card for vehicle type
```

### 4.7. Hết Capacity

Điều kiện:

```text
Số subscription giữ chỗ/active >= tổng capacity các zone ACTIVE của vehicleTypeId.
```

Kết quả mong muốn khi approve:

```text
409 Conflict
No available subscription capacity for vehicle type
```

### 4.8. Assign Card Khi Invoice Chưa Paid

Request:

```http
PATCH {{base_url}}/api/access-control/subscriptions/{{subscription_id}}/assign-card
```

Điều kiện:

```text
subscription.status = PENDING_PAYMENT
invoice.status = UNPAID
```

Kết quả mong muốn:

```text
409 Conflict
Paid invoice not found for subscription
```

## 5. SQL Chỉ Dùng Để Debug Khi Postman Bị Lỗi

Kiểm tra customer token có resolve đúng customer không:

```sql
SELECT
    a.account_id,
    a.username,
    a.status AS account_status,
    r.code AS role_code,
    c.customer_id,
    c.status AS customer_status,
    c.approval_status
FROM iam.accounts a
JOIN iam.roles r ON r.role_id = a.role_id
LEFT JOIN people.customers c ON c.user_profile_id = a.user_profile_id
WHERE a.username = 'customer_subscription_test';
```

Kiểm tra quyền subscription của CUSTOMER:

```sql
SELECT r.code AS role_code, p.permission_code
FROM iam.roles r
JOIN iam.role_permissions rp ON rp.role_id = r.role_id
JOIN iam.permissions p ON p.permission_id = rp.permission_id
WHERE r.code = 'CUSTOMER'
  AND p.permission_code LIKE 'SUBSCRIPTION_%'
  AND rp.is_active = TRUE
ORDER BY p.permission_code;
```

Kiểm tra quyền subscription của PARKING_MANAGER:

```sql
SELECT r.code AS role_code, p.permission_code
FROM iam.roles r
JOIN iam.role_permissions rp ON rp.role_id = r.role_id
JOIN iam.permissions p ON p.permission_id = rp.permission_id
WHERE r.code = 'PARKING_MANAGER'
  AND p.permission_code LIKE 'SUBSCRIPTION_%'
  AND rp.is_active = TRUE
ORDER BY p.permission_code;
```

Kiểm tra subscription/invoice/card sau approve:

```sql
SELECT subscription_id, customer_id, customer_vehicle_id, card_id, status
FROM access_control.subscriptions
WHERE subscription_id = 'SUBSCRIPTION_ID';

SELECT invoice_id, subscription_id, amount, final_amount, status
FROM billing.invoices
WHERE subscription_id = 'SUBSCRIPTION_ID';

SELECT card_id, card_number, vehicle_type_id, status
FROM access_control.cards
WHERE card_id = 'CARD_ID';
```
