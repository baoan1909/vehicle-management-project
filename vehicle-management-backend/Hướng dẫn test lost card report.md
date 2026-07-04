# Huong Dan Test Lost Card Report

Base URL:

```text
http://localhost:8080/vehicle-management
```

Token nen dung:

- EMPLOYEE, PARKING_MANAGER hoac SYSTEM_ADMIN.
- Sau khi chay migration `V26__seed_lost_card_report_permissions.sql`, role test can co:
  - `LOST_CARD_REPORT_CREATE_ALL`
  - `LOST_CARD_REPORT_READ_ALL`
  - `LOST_CARD_REPORT_UPDATE_ALL`

## 1. Kiem Tra Quyen

Chay SQL:

```sql
SELECT r.code AS role_code, p.permission_code
FROM iam.roles r
JOIN iam.role_permissions rp ON rp.role_id = r.role_id
JOIN iam.permissions p ON p.permission_id = rp.permission_id
WHERE r.code IN ('EMPLOYEE', 'PARKING_MANAGER', 'SYSTEM_ADMIN')
  AND p.permission_code LIKE 'LOST_CARD_REPORT%'
  AND rp.is_active = TRUE
ORDER BY r.code, p.permission_code;
```

Ket qua mong doi:

```text
LOST_CARD_REPORT_CREATE_ALL
LOST_CARD_REPORT_READ_ALL
LOST_CARD_REPORT_UPDATE_ALL
```

## 2. Chuan Bi Du Lieu

Can co san:

- Parking lot, zone, gate, lane dang ACTIVE.
- Lane check-in/check-out hoat dong binh thuong.
- Price rule ve vang lai co `lostCardFee`.
- The vang lai dang `IN_USE` va co `parking_session` dang `OPEN` de test khach vang lai trong bai.
- Khach dang ky co subscription `ACTIVE` de test khach dang ky mat the ngoai bai.
- Invoice/payment API da hoat dong de xac nhan thanh toan phi mat the.

## 3. Preview Theo Bien So

API:

```http
GET /api/access-control/lost-card-reports/preview?licensePlate=60K8-2301
```

Ket qua mong doi voi xe vang lai dang trong bai:

- `context = VISITOR_IN_PARKING`
- Co `parkingSession`
- `ticketPrice > 0`
- `lostCardFee > 0`
- `totalAmount = ticketPrice + lostCardFee`

Ket qua mong doi voi xe dang ky dang trong bai:

- `context = REGISTERED_IN_PARKING`
- `ticketPrice = 0`
- Co `parkingSession`
- Co `subscription`

Ket qua mong doi voi xe dang ky mat the ngoai bai:

- Khong co `parkingSession`
- Co subscription ACTIVE theo bien so
- `context = REGISTERED_OUTSIDE`
- `ticketPrice = 0`

## 4. Tao Phieu Mat The Cho Xe Trong Bai

API:

```http
POST /api/access-control/lost-card-reports
Content-Type: application/json
```

Body:

```json
{
  "parkingSessionId": "PASTE_PARKING_SESSION_ID",
  "timeOfLost": "2026-06-29T04:00:00Z",
  "reporterName": "Nguyen Van A",
  "reporterPhone": "0901234567",
  "identifyCard": "080112345678",
  "registrationLicense": null,
  "note": "Khach bao mat the tai cong ra"
}
```

Ket qua mong doi:

- HTTP `201 Created`
- Report `status = OPEN`
- Report `context = VISITOR_IN_PARKING` hoac `REGISTERED_IN_PARKING`
- The cu chuyen sang `LOST`
- Parking session chuyen tu `OPEN` sang `LOST_CARD`
- Tao invoice `UNPAID` theo `lostCardReportId`
- `barrierAction = NONE`

## 5. Tao Phieu Mat The Cho Xe Dang Ky Ngoai Bai

API:

```http
POST /api/access-control/lost-card-reports
Content-Type: application/json
```

Body:

```json
{
  "subscriptionId": "PASTE_SUBSCRIPTION_ID",
  "timeOfLost": "2026-06-29T04:00:00Z",
  "reporterName": "Nguyen Van A",
  "reporterPhone": "0901234567",
  "identifyCard": "080112345678",
  "registrationLicense": null,
  "note": "Khach dang ky bao mat the ngoai bai"
}
```

Ket qua mong doi:

- HTTP `201 Created`
- Report `context = REGISTERED_OUTSIDE`
- Report `status = OPEN`
- The cu chuyen sang `LOST`
- Khong co parking session bi dong
- Tao invoice `UNPAID`

## 6. Huy Phieu Truoc Khi Thanh Toan

API:

```http
PATCH /api/access-control/lost-card-reports/{lostCardReportId}/cancel
Content-Type: application/json
```

Body:

```json
{
  "cancelReason": "Khach tim lai duoc the"
}
```

Ket qua mong doi:

- Report `status = CANCELLED`
- Invoice `UNPAID` chuyen sang `CANCELLED`
- Neu co parking session `LOST_CARD` thi chuyen lai `OPEN`
- Neu trong bai: the cu chuyen lai `IN_USE`
- Neu dang ky ngoai bai: the cu chuyen lai `ASSIGNED`

## 7. Thanh Toan Invoice Mat The

Lay invoice:

```http
GET /api/billing/invoices?lostCardReportId={lostCardReportId}
```

Thanh toan:

```http
POST /api/billing/invoices/{invoiceId}/payments
Content-Type: application/json
```

Body:

```json
{
  "paymentMethod": "CASH",
  "amount": 126000,
  "note": "Khach da thanh toan phi mat the"
}
```

Ket qua mong doi:

- Payment `status = SUCCESS`
- Invoice `status = PAID`

## 8. Resolve Xe Vang Lai

API:

```http
PATCH /api/access-control/lost-card-reports/{lostCardReportId}/resolve
Content-Type: application/json
```

Body co the de rong:

```json
{}
```

Ket qua mong doi:

- Chi resolve duoc khi invoice da `PAID`
- Report `status = RESOLVED`
- Parking session `LOST_CARD` chuyen sang `CLOSED`
- The cu van la `LOST`
- `barrierAction = OPEN`

## 9. Resolve Xe Dang Ky Va Cap The Moi

Can co the moi:

- `status = AVAILABLE`
- Dung loai xe hoac `vehicleTypeId = null`

API:

```http
PATCH /api/access-control/lost-card-reports/{lostCardReportId}/resolve
Content-Type: application/json
```

Body:

```json
{
  "newCardId": "PASTE_NEW_AVAILABLE_CARD_ID"
}
```

Ket qua mong doi:

- Chi resolve duoc khi invoice da `PAID`
- Report `status = RESOLVED`
- The cu van `LOST`
- The moi chuyen sang `ASSIGNED`
- Subscription cap nhat sang `newCardId`
- Neu xe dang trong bai thi parking session `LOST_CARD` chuyen sang `CLOSED`
- Neu mat the ngoai bai thi `barrierAction = NONE`

## 10. Xem Chi Tiet Va Danh Sach

Chi tiet:

```http
GET /api/access-control/lost-card-reports/{lostCardReportId}
```

Danh sach:

```http
GET /api/access-control/lost-card-reports
GET /api/access-control/lost-card-reports?status=OPEN
GET /api/access-control/lost-card-reports?context=VISITOR_IN_PARKING
GET /api/access-control/lost-card-reports?keyword=Nguyen
```

Ket qua mong doi:

- Detail tra ve report, parkingSession neu co, subscription neu co, invoice detail kem payments.
- List tra ve danh sach report theo filter.

## Loi Thuong Gap

- `403 Access Denied`: token chua co quyen trong V26 hoac dang dung token sai role.
- `Open lost card report already exists for card`: the dang co report OPEN, can cancel/resolve report cu truoc.
- `Lost card invoice must be paid before resolving report`: can goi payment truoc khi resolve.
- `New card must be AVAILABLE`: the moi de cap lai cho khach dang ky phai dang AVAILABLE.
- `Visitor lost card report must not contain newCardId`: xe vang lai khong cap lai the moi.
