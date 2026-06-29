# Huong dan test API checkout

File nay huong dan chuan bi du lieu va test API checkout:

```http
POST /vehicle-management/api/parking/parking-sessions/check-out
```

API checkout nhan du lieu tu:

- May quet the: `cardUid`
- Lane hien tai: `laneId`
- Camera bien so: `licensePlate`
- Anh bien so checkout: `licensePlateImage`
- Anh nguoi checkout: `personImage`

Ket qua nghiep vu:

- Ve vang lai: dong phien gui xe, tinh tien, tao invoice `UNPAID`, tra `barrierAction = WAIT_PAYMENT`.
- Ve dang ky: dong phien gui xe, tong tien `0`, khong tao invoice, tra `barrierAction = OPEN`.

## 1. Chuan bi chung

### 1.1. Chay migration quyen checkout

Dam bao migration sau da duoc chay:

```text
V23__seed_parking_check_out_permission.sql
```

Permission can co:

```text
PARKING_SESSION_CHECK_OUT_ALL
```

Role nen co quyen:

```text
EMPLOYEE
PARKING_MANAGER
SYSTEM_ADMIN
```

Kiem tra nhanh trong database:

```sql
SELECT r.code AS role_code, p.permission_code
FROM iam.roles r
JOIN iam.role_permissions rp ON rp.role_id = r.role_id
JOIN iam.permissions p ON p.permission_id = rp.permission_id
WHERE p.permission_code = 'PARKING_SESSION_CHECK_OUT_ALL'
  AND rp.is_active = TRUE
ORDER BY r.code;
```

Ket qua ky vong:

```text
EMPLOYEE              PARKING_SESSION_CHECK_OUT_ALL
PARKING_MANAGER      PARKING_SESSION_CHECK_OUT_ALL
SYSTEM_ADMIN         PARKING_SESSION_CHECK_OUT_ALL
```

### 1.2. Dang nhap lay token

Dang nhap bang tai khoan `EMPLOYEE`, `PARKING_MANAGER`, hoac `SYSTEM_ADMIN`.

```http
POST http://localhost:8081/realms/vehicle-management/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded
```

Body:

```text
grant_type=password
client_id=vehicle-management-frontend
username=<email>
password=<password>
```

Luu `access_token` vao Postman variable, vi du:

```text
manager
```

Khi goi API backend:

```text
Authorization: Bearer {{manager}}
```

Neu dung bien moi nhung van bi `401 Jwt expired`, hay kiem tra Postman dang dung dung Environment va bien token co phai token moi khong.

## 2. Chuan bi du lieu nen

Can co cac du lieu sau:

- `parking_lot` status `ACTIVE`
- `zone` status `ACTIVE`
- `gate` status `ACTIVE`
- mot lane `IN` status `ACTIVE`
- mot lane `OUT` status `ACTIVE`
- `vehicle_type` active
- gia ve vang lai:
  - DAY: `06:00:00 -> 19:59:59`
  - NIGHT: `20:00:00 -> 05:59:59`
- the gui xe de test
- mot parking session `OPEN`

### 2.1. Kiem tra lane IN/OUT

```sql
SELECT
    l.lane_id,
    l.code,
    l.name,
    l.direction,
    l.status,
    g.gate_id,
    g.status AS gate_status,
    z.zone_id,
    z.status AS zone_status,
    pl.parking_lot_id,
    pl.status AS lot_status
FROM parking.lanes l
JOIN parking.gates g ON g.gate_id = l.gate_id
JOIN parking.zones z ON z.zone_id = g.zone_id
JOIN parking.parking_lots pl ON pl.parking_lot_id = z.parking_lot_id
WHERE l.status = 'ACTIVE'
ORDER BY l.code;
```

Can lay:

```text
IN laneId
OUT laneId
zoneId
vehicleTypeId
```

### 2.2. Kiem tra bang gia visitor

Gia checkout can lay duoc 2 rule:

```text
DAY:   time_from = 06:00:00, time_to = 19:59:59
NIGHT: time_from = 20:00:00, time_to = 05:59:59
```

Kiem tra:

```sql
SELECT
    pr.price_rule_id,
    pr.rule_name,
    pr.vehicle_type_id,
    pr.time_from,
    pr.time_to,
    pr.base_price,
    pr.is_active,
    pp.applies_to,
    pp.is_active AS price_plan_active,
    tt.code AS ticket_type_code
FROM catalog.price_rules pr
JOIN catalog.price_plans pp ON pp.price_plan_id = pr.price_plan_id
JOIN catalog.ticket_types tt ON tt.ticket_type_id = pr.ticket_type_id
WHERE pp.applies_to = 'VISITOR'
  AND tt.code = 'DAILY'
  AND pr.is_active = TRUE
ORDER BY pr.time_from;
```

Neu checkout ve vang lai bao:

```text
Active day parking price rule not found
Active night parking price rule not found
```

thi can tao/cap nhat lai price rule visitor.

## 3. Test checkout ve vang lai

### 3.1. Tao session OPEN bang API check-in

Dieu kien:

- Card vang lai dang `AVAILABLE`.
- Lane check-in la lane `IN`.
- Bien so dung voi bien so se checkout.

Goi API:

```http
POST http://localhost:8080/vehicle-management/api/parking/parking-sessions/check-in
Authorization: Bearer {{manager}}
Content-Type: multipart/form-data
```

Form-data:

```text
request             Text
licensePlateImage   File
personImage         File
```

Gia tri field `request`:

```json
{
  "cardUid": "RFID-VISITOR-001",
  "laneId": "IN_LANE_ID",
  "licensePlate": "59A1-12345",
  "note": "Test visitor check-in"
}
```

Ket qua ky vong:

```json
{
  "success": true,
  "message": "Parking session checked in successfully",
  "data": {
    "customerType": "VISITOR",
    "barrierAction": "OPEN",
    "parkingSession": {
      "status": "OPEN",
      "licensePlateIn": "59A1-12345"
    }
  }
}
```

Kiem tra DB:

```sql
SELECT ps.parking_session_id, ps.card_id, ps.license_plate_in, ps.status, c.uid, c.status AS card_status
FROM parking.parking_sessions ps
JOIN access_control.cards c ON c.card_id = ps.card_id
WHERE c.uid = 'RFID-VISITOR-001'
ORDER BY ps.check_in_time DESC
LIMIT 1;
```

Ky vong:

```text
parking_sessions.status = OPEN
cards.status = IN_USE
```

### 3.2. Goi API checkout ve vang lai

```http
POST http://localhost:8080/vehicle-management/api/parking/parking-sessions/check-out
Authorization: Bearer {{manager}}
Content-Type: multipart/form-data
```

Form-data:

```text
request             Text
licensePlateImage   File
personImage         File
```

Gia tri field `request`:

```json
{
  "laneId": "OUT_LANE_ID",
  "cardUid": "RFID-VISITOR-001",
  "licensePlate": "59A1-12345",
  "note": "Test visitor check-out"
}
```

Ket qua ky vong:

```json
{
  "success": true,
  "message": "Parking session checked out successfully",
  "data": {
    "customerType": "VISITOR",
    "barrierAction": "WAIT_PAYMENT",
    "parkingSession": {
      "status": "CLOSED",
      "licensePlateOut": "59A1-12345",
      "totalPrice": 5000
    },
    "parkingEvent": {
      "eventType": "CHECK_OUT",
      "licensePlateDetected": "59A1-12345",
      "licensePlateImagePath": "...",
      "personImagePath": "..."
    },
    "invoice": {
      "status": "UNPAID",
      "parkingSessionId": "...",
      "amount": 5000,
      "finalAmount": 5000
    }
  }
}
```

Gia `totalPrice` phu thuoc vao thoi gian gui xe va price rule hien tai.

Kiem tra DB:

```sql
SELECT
    ps.parking_session_id,
    ps.status,
    ps.license_plate_in,
    ps.license_plate_out,
    ps.total_price,
    c.uid,
    c.status AS card_status
FROM parking.parking_sessions ps
JOIN access_control.cards c ON c.card_id = ps.card_id
WHERE c.uid = 'RFID-VISITOR-001'
ORDER BY ps.check_out_time DESC
LIMIT 1;
```

Ky vong:

```text
parking_sessions.status = CLOSED
license_plate_out = license_plate_in
total_price > 0
cards.status = AVAILABLE
```

Kiem tra event:

```sql
SELECT event_type, license_plate_detected, license_plate_image_path, person_image_path, note
FROM parking.parking_events
WHERE parking_session_id = 'PARKING_SESSION_ID'
ORDER BY event_time DESC;
```

Ky vong co event:

```text
CHECK_OUT
```

Kiem tra invoice:

```sql
SELECT invoice_id, invoice_no, parking_session_id, amount, discount_amount, final_amount, status, paid_at
FROM billing.invoices
WHERE parking_session_id = 'PARKING_SESSION_ID';
```

Ky vong:

```text
status = UNPAID
paid_at = null
```

### 3.3. Ghi nhan thanh toan sau checkout

Checkout ve vang lai tra:

```text
barrierAction = WAIT_PAYMENT
```

Nhan vien/manager xac nhan thanh toan bang payment API:

```http
POST http://localhost:8080/vehicle-management/api/billing/invoices/{invoiceId}/payments
Authorization: Bearer {{manager}}
Content-Type: application/json
```

Body:

```json
{
  "paymentMethod": "CASH",
  "amount": 5000,
  "note": "Khach da thanh toan tien gui xe"
}
```

Ket qua ky vong:

```json
{
  "success": true,
  "message": "Payment recorded successfully",
  "data": {
    "status": "SUCCESS",
    "amount": 5000
  }
}
```

Sau payment:

```sql
SELECT status, paid_at
FROM billing.invoices
WHERE invoice_id = 'INVOICE_ID';
```

Ky vong:

```text
status = PAID
paid_at is not null
```

## 4. Test checkout ve dang ky

### 4.1. Chuan bi subscription active

Can co:

- Customer `ACTIVE`, `APPROVED`
- Customer vehicle `ACTIVE`
- Subscription `ACTIVE`
- Card cua subscription dang `ASSIGNED`

Dung cac API subscription/card hien co de tao du lieu. Sau do dung API check-in de tao session `OPEN`.

Goi API check-in:

```http
POST http://localhost:8080/vehicle-management/api/parking/parking-sessions/check-in
Authorization: Bearer {{manager}}
Content-Type: multipart/form-data
```

Field `request`:

```json
{
  "cardUid": "RFID-REGISTERED-001",
  "laneId": "IN_LANE_ID",
  "licensePlate": "59A1-67890",
  "note": "Test subscription check-in"
}
```

Ket qua ky vong:

```text
parking_sessions.status = OPEN
cards.status = IN_USE
parking_sessions.customer_id is not null
parking_sessions.customer_vehicle_id is not null
```

### 4.2. Goi API checkout ve dang ky

```http
POST http://localhost:8080/vehicle-management/api/parking/parking-sessions/check-out
Authorization: Bearer {{manager}}
Content-Type: multipart/form-data
```

Field `request`:

```json
{
  "laneId": "OUT_LANE_ID",
  "cardUid": "RFID-REGISTERED-001",
  "licensePlate": "59A1-67890",
  "note": "Test subscription check-out"
}
```

Ket qua ky vong:

```json
{
  "success": true,
  "message": "Parking session checked out successfully",
  "data": {
    "customerType": "SUBSCRIPTION",
    "barrierAction": "OPEN",
    "parkingSession": {
      "status": "CLOSED",
      "licensePlateOut": "59A1-67890",
      "totalPrice": 0
    },
    "invoice": null
  }
}
```

Kiem tra DB:

```sql
SELECT
    ps.parking_session_id,
    ps.status,
    ps.customer_id,
    ps.customer_vehicle_id,
    ps.total_price,
    c.uid,
    c.status AS card_status
FROM parking.parking_sessions ps
JOIN access_control.cards c ON c.card_id = ps.card_id
WHERE c.uid = 'RFID-REGISTERED-001'
ORDER BY ps.check_out_time DESC
LIMIT 1;
```

Ky vong:

```text
parking_sessions.status = CLOSED
total_price = 0
cards.status = ASSIGNED
```

Kiem tra khong tao invoice:

```sql
SELECT *
FROM billing.invoices
WHERE parking_session_id = 'PARKING_SESSION_ID';
```

Ky vong:

```text
0 rows
```

## 5. Test cac case loi quan trong

### 5.1. Checkout bang lane IN

Dung `laneId` cua lane `IN` trong API checkout.

Ket qua ky vong:

```json
{
  "success": false,
  "message": "Lane must be an OUT lane for check-out"
}
```

HTTP status ky vong:

```text
400 Bad Request
```

### 5.2. Checkout sai bien so

Check-in voi bien so:

```text
59A1-12345
```

Checkout voi bien so:

```text
59A1-99999
```

Ket qua ky vong:

```json
{
  "success": false,
  "message": "Detected license plate does not match check-in license plate"
}
```

HTTP status ky vong:

```text
409 Conflict
```

DB ky vong:

```text
parking_sessions van OPEN
cards van IN_USE
khong tao CHECK_OUT event
khong tao invoice moi
```

### 5.3. Card khong co session OPEN

Dung `cardUid` cua card khong o trang thai `IN_USE` hoac khong co parking session `OPEN`.

Ket qua co the gap:

```text
Card is not in use
```

hoac:

```text
Open parking session not found for card
```

HTTP status ky vong:

```text
409 Conflict
```

### 5.4. Thieu anh checkout

Khong gui `licensePlateImage` hoac `personImage`.

Ket qua ky vong:

```text
licensePlateImage must not be empty
```

hoac:

```text
personImage must not be empty
```

HTTP status ky vong:

```text
400 Bad Request
```

### 5.5. User khong co quyen checkout

Dung token customer hoac account khong co:

```text
PARKING_SESSION_CHECK_OUT_ALL
```

Ket qua ky vong:

```json
{
  "success": false,
  "message": "Access is denied"
}
```

HTTP status ky vong:

```text
403 Forbidden
```

## 6. Quy tac tinh tien can doi chieu

Gia visitor lay tu price rule:

```text
DAY   = gia tai moc 12:00
NIGHT = gia tai moc 00:00
```

Rule tinh tien:

```text
duration <= 4h:
    totalPrice = dayPrice

4h < duration < 24h:
    neu toan bo thoi gian gui nam trong ca ngay:
        totalPrice = dayPrice
    nguoc lai:
        totalPrice = nightPrice

duration >= 24h:
    fullDays = so khoang tron 24h
    remaining = phan du sau cac ngay tron
    totalPrice = fullDays * (dayPrice + nightPrice) + price(remaining)
```

Vi du voi:

```text
dayPrice = 5000
nightPrice = 10000
```

Mot so ky vong:

```text
07:00 -> 16:00 cung ngay     = 5000
17:00 -> 21:30 cung ngay     = 10000
20:00 -> 23:30 cung ngay     = 5000
06:20 25/06 -> 13:00 30/06  = 80000
```

## 7. Checklist ket luan sau khi test

Sau khi test xong, can xac nhan:

- API checkout thanh cong voi lane `OUT`.
- Sai lane `IN` bi chan.
- Sai bien so bi chan.
- Ve vang lai tao invoice `UNPAID`.
- Ve vang lai chuyen card ve `AVAILABLE`.
- Ve dang ky khong tao invoice.
- Ve dang ky chuyen card ve `ASSIGNED`.
- `parking_events` co event `CHECK_OUT` va co du 2 anh.
- User khong co permission checkout bi `403`.
