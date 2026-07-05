# Huong dan test API Support Ticket

Base URL:

```text
http://localhost:8080/vehicle-management
```

Can chuan bi 3 token:

- `{{customer}}`: account role `CUSTOMER`, customer phai `ACTIVE` va `APPROVED`.
- `{{employee}}`: account role `EMPLOYEE`, status `ACTIVE`.
- `{{manager}}`: account role `PARKING_MANAGER`, status `ACTIVE`.

Neu vua them migration `V27__seed_support_ticket_permissions.sql`, hay restart backend de Flyway chay migration.

## 1. Kiem tra category ACTIVE

Dung token manager hoac customer:

```http
GET {{baseUrl}}/api/operations/support-ticket-categories?status=ACTIVE
Authorization: Bearer {{manager}}
```

Ket qua mong doi:

- `success = true`
- `data` co it nhat 1 category `ACTIVE`
- Luu lai `categoryId`, vi du category `LOST_CARD`

Neu chua co category, tao bang API category hoac dung category da seed tu V10.

## 2. Customer tao ticket

Dung token customer:

```http
POST {{baseUrl}}/api/operations/support-tickets
Authorization: Bearer {{customer}}
Content-Type: application/json
```

Body:

```json
{
  "categoryId": "{{categoryId}}",
  "title": "Toi bi mat the xe",
  "content": "Toi gui xe luc 7h30 o khu xe may A1, luc ra thi khong tim thay the."
}
```

Ket qua mong doi:

- HTTP `201 Created`
- `status = OPEN`
- `assignedTo = null`
- `reopenCount = 0`
- Response co `categoryId`, `categoryCode`, `categoryName`, `priority`
- Luu lai `supportTicketId`

## 3. Customer xem danh sach ticket cua minh

```http
GET {{baseUrl}}/api/operations/support-tickets
Authorization: Bearer {{customer}}
```

Ket qua mong doi:

- Chi thay ticket cua customer dang dang nhap.
- Ticket vua tao xuat hien trong danh sach.

Thu filter:

```http
GET {{baseUrl}}/api/operations/support-tickets?status=OPEN
GET {{baseUrl}}/api/operations/support-tickets?keyword=mat the
GET {{baseUrl}}/api/operations/support-tickets?priority=HIGH
```

## 4. Customer cap nhat ticket khi OPEN

```http
PUT {{baseUrl}}/api/operations/support-tickets/{{supportTicketId}}
Authorization: Bearer {{customer}}
Content-Type: application/json
```

Body:

```json
{
  "categoryId": "{{categoryId}}",
  "title": "Toi bi mat the xe may",
  "content": "Toi gui xe luc khoang 7h30, khi ra khoi bai thi phat hien mat the."
}
```

Ket qua mong doi:

- HTTP `200 OK`
- Title/content duoc cap nhat.
- Status van la `OPEN`.

## 5. Manager xem tat ca ticket

```http
GET {{baseUrl}}/api/operations/support-tickets
Authorization: Bearer {{manager}}
```

Ket qua mong doi:

- Manager xem duoc tat ca ticket.
- Ticket vua tao xuat hien.

Thu filter:

```http
GET {{baseUrl}}/api/operations/support-tickets?customerId={{customerId}}
GET {{baseUrl}}/api/operations/support-tickets?categoryId={{categoryId}}
GET {{baseUrl}}/api/operations/support-tickets?status=OPEN
GET {{baseUrl}}/api/operations/support-tickets?priority=HIGH
```

## 6. Manager assign ticket cho employee

Dung token manager:

```http
PATCH {{baseUrl}}/api/operations/support-tickets/{{supportTicketId}}/assign
Authorization: Bearer {{manager}}
Content-Type: application/json
```

Body:

```json
{
  "assignedTo": "{{employeeAccountId}}"
}
```

Ket qua mong doi:

- HTTP `200 OK`
- `assignedTo = employeeAccountId`
- `status` van la `OPEN`

Neu sai employee:

- Account khong ton tai hoac khong phai `EMPLOYEE/PARKING_MANAGER` ACTIVE se tra loi loi.

## 7. Employee xem ticket duoc giao

```http
GET {{baseUrl}}/api/operations/support-tickets
Authorization: Bearer {{employee}}
```

Ket qua mong doi:

- Employee chi thay ticket co `assignedTo` la account cua minh.

Co the filter:

```http
GET {{baseUrl}}/api/operations/support-tickets?assignedTo={{employeeAccountId}}
```

## 8. Employee start progress

```http
PATCH {{baseUrl}}/api/operations/support-tickets/{{supportTicketId}}/start-progress
Authorization: Bearer {{employee}}
```

Ket qua mong doi:

- HTTP `200 OK`
- `status = IN_PROGRESS`

Neu employee khac goi:

- Ky vong `403 Forbidden`

## 9. Employee resolve ticket

```http
PATCH {{baseUrl}}/api/operations/support-tickets/{{supportTicketId}}/resolve
Authorization: Bearer {{employee}}
Content-Type: application/json
```

Body:

```json
{
  "resolutionNote": "Da xac minh thong tin va huong dan khach xu ly mat the."
}
```

Ket qua mong doi:

- HTTP `200 OK`
- `status = RESOLVED`
- `resolvedAt` co gia tri
- `resolutionNote` co noi dung vua nhap

## 10. Customer reopen neu chua hai long

```http
PATCH {{baseUrl}}/api/operations/support-tickets/{{supportTicketId}}/reopen
Authorization: Bearer {{customer}}
```

Ket qua mong doi:

- HTTP `200 OK`
- Neu ticket co `assignedTo`: `status = IN_PROGRESS`
- Neu ticket khong co `assignedTo`: `status = OPEN`
- `resolvedAt = ""`
- `resolutionNote = null`
- `reopenCount` tang them 1
- `lastReopenedAt` co gia tri

## 11. Manager resolve lai ticket

Dung token manager:

```http
PATCH {{baseUrl}}/api/operations/support-tickets/{{supportTicketId}}/resolve
Authorization: Bearer {{manager}}
Content-Type: application/json
```

Body:

```json
{
  "resolutionNote": "Manager da kiem tra lai va xac nhan da xu ly xong."
}
```

Ket qua mong doi:

- HTTP `200 OK`
- `status = RESOLVED`

## 12. Customer hoac manager close ticket

Customer chinh chu close:

```http
PATCH {{baseUrl}}/api/operations/support-tickets/{{supportTicketId}}/close
Authorization: Bearer {{customer}}
```

Hoac manager close:

```http
PATCH {{baseUrl}}/api/operations/support-tickets/{{supportTicketId}}/close
Authorization: Bearer {{manager}}
```

Ket qua mong doi:

- HTTP `200 OK`
- `status = CLOSED`
- `closedAt` co gia tri
- `closedBy` la account dang dang nhap

Sau khi `CLOSED`, goi reopen:

```http
PATCH {{baseUrl}}/api/operations/support-tickets/{{supportTicketId}}/reopen
Authorization: Bearer {{customer}}
```

Ket qua mong doi:

- Loi validate/conflict vi ticket CLOSED khong duoc reopen.

## 13. Cac case can test loi

### Customer tao voi category INACTIVE hoac khong ton tai

Ky vong:

- `404 Not Found`
- Message: active category not found hoac tuong duong.

### Customer update ticket khong phai cua minh

Ky vong:

- `403 Forbidden`

### Customer update khi ticket IN_PROGRESS/RESOLVED/CLOSED

Ky vong:

- `409 Conflict`
- Chi ticket `OPEN` moi duoc update noi dung.

### Employee assign ticket

Dung token employee goi:

```http
PATCH {{baseUrl}}/api/operations/support-tickets/{{supportTicketId}}/assign
```

Ky vong:

- `403 Forbidden`

### Employee close ticket

Dung token employee goi:

```http
PATCH {{baseUrl}}/api/operations/support-tickets/{{supportTicketId}}/close
```

Ky vong:

- `403 Forbidden`

## 14. SQL kiem tra quyen da seed

Kiem tra role customer:

```sql
SELECT r.code AS role_code, p.permission_code
FROM iam.roles r
JOIN iam.role_permissions rp ON rp.role_id = r.role_id
JOIN iam.permissions p ON p.permission_id = rp.permission_id
WHERE r.code = 'CUSTOMER'
  AND p.permission_code LIKE 'SUPPORT_TICKET%'
  AND rp.is_active = TRUE
ORDER BY p.permission_code;
```

Kiem tra role employee:

```sql
SELECT r.code AS role_code, p.permission_code
FROM iam.roles r
JOIN iam.role_permissions rp ON rp.role_id = r.role_id
JOIN iam.permissions p ON p.permission_id = rp.permission_id
WHERE r.code = 'EMPLOYEE'
  AND p.permission_code LIKE 'SUPPORT_TICKET%'
  AND rp.is_active = TRUE
ORDER BY p.permission_code;
```

Kiem tra role manager:

```sql
SELECT r.code AS role_code, p.permission_code
FROM iam.roles r
JOIN iam.role_permissions rp ON rp.role_id = r.role_id
JOIN iam.permissions p ON p.permission_id = rp.permission_id
WHERE r.code = 'PARKING_MANAGER'
  AND p.permission_code LIKE 'SUPPORT_TICKET%'
  AND rp.is_active = TRUE
ORDER BY p.permission_code;
```
