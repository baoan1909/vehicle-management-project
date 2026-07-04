# Hướng dẫn test API quản lý ca trực

Tài liệu này hướng dẫn chuẩn bị dữ liệu và kiểm thử bằng Postman cho bốn bảng:

1. `operations.shift_templates`
2. `operations.employee_roster_rules`
3. `operations.shifts`
4. `operations.shift_assignments`

Luồng kiểm thử chuẩn:

```text
Parking lot, zone, gate, employee
        ↓
Shift template
        ↓
Employee roster rule
        ↓
Generate week: shifts + shift assignments ở trạng thái DRAFT/ACTIVE
        ↓
Điều chỉnh assignment khi còn DRAFT
        ↓
Approve week: shifts chuyển sang SCHEDULED
        ↓
Replace hoặc swap assignment
        ↓
Open shift → Close shift hoặc Cancel shift
```

## 1. Biến môi trường Postman

Tạo Postman Environment và khai báo:

```text
base_url       = http://localhost:8080/vehicle-management
keycloak_url   = http://localhost:8081
realm          = <ten-realm>
client_id      = <client-id>
admin_token    =
manager_token  =
employee_token =

parkingLotId   =
carZoneId      =
motorZoneId    =
carGateId      =
motorGateId    =

empMorningCarId   =
empMorningMotorId =
empAfternoonCarId =
empAfternoonMotorId =
empNightCarId     =
empNightMotorId   =
empReliefId       =
empSpareId        =

morningTemplateId   =
afternoonTemplateId =
nightTemplateId     =

weekStartDate = 2026-06-29
weekEndDate   = 2026-07-05
shiftId       =
assignmentId  =
```

`weekStartDate` phải là thứ Hai và không nằm trong quá khứ. Nếu ngày mẫu đã qua, thay bằng thứ Hai tương lai gần nhất.

Mọi request nghiệp vụ dùng:

```text
Authorization: Bearer {{manager_token}}
Content-Type: application/json
```

## 2. Lấy token

### 2.1 Token manager

```http
POST {{keycloak_url}}/realms/{{realm}}/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded
```

Body:

```text
grant_type=password
client_id={{client_id}}
username=<manager-username>
password=<manager-password>
```

Postman Tests:

```javascript
const body = pm.response.json();
pm.environment.set("manager_token", body.access_token);
```

Manager phải có các quyền:

```text
SHIFT_CREATE_ALL
SHIFT_READ_ALL
SHIFT_UPDATE_ALL
SHIFT_DELETE_ALL
SHIFT_ASSIGNMENT_CREATE_ALL
SHIFT_ASSIGNMENT_READ_ALL
SHIFT_ASSIGNMENT_UPDATE_ALL
SHIFT_ASSIGNMENT_DELETE_ALL
```

### 2.2 Token employee

Lấy token tương tự bằng tài khoản employee được phân công. Employee cần:

```text
SHIFT_OPEN_OWN
SHIFT_ASSIGNMENT_READ_OWN
```

Lưu token vào `employee_token`.

## 3. Kiểm tra migration và permission

Ứng dụng phải chạy các migration cấu trúc/phân quyền ca trực, tối thiểu V18 đến V21.

Kiểm tra quyền bằng PostgreSQL:

```sql
SELECT r.code AS role_code, p.permission_code
FROM iam.roles r
JOIN iam.role_permissions rp ON rp.role_id = r.role_id
JOIN iam.permissions p ON p.permission_id = rp.permission_id
WHERE r.code IN ('PARKING_MANAGER', 'EMPLOYEE')
  AND rp.is_active = TRUE
  AND (p.permission_code LIKE 'SHIFT_%')
ORDER BY r.code, p.permission_code;
```

Kết quả tối thiểu:

- `PARKING_MANAGER`: toàn bộ quyền `SHIFT_*_ALL` và `SHIFT_ASSIGNMENT_*_ALL`.
- `EMPLOYEE`: `SHIFT_OPEN_OWN`, `SHIFT_ASSIGNMENT_READ_OWN`.

Sau khi thay đổi permission, đăng nhập lại để lấy access token mới.

## 4. Chuẩn bị dữ liệu nền

## 4.1 Tạo hoặc lấy parking lot ACTIVE

```http
POST {{base_url}}/api/parking/parking-lots
```

```json
{
  "code": "HCMUTE-SHIFT-TEST",
  "name": "Bãi xe kiểm thử ca trực",
  "address": "01 Võ Văn Ngân, Thủ Đức",
  "totalCapacity": 1500
}
```

Kỳ vọng:

- HTTP `201 Created`.
- `success = true`.
- `data.status = "ACTIVE"`.
- Lưu `data.parkingLotId` vào `parkingLotId`.

Postman Tests:

```javascript
const data = pm.response.json().data;
pm.environment.set("parkingLotId", data.parkingLotId);
pm.test("Parking lot ACTIVE", () => pm.expect(data.status).to.eql("ACTIVE"));
```

Nếu parking lot đã có:

```http
GET {{base_url}}/api/parking/parking-lots?keyword=HCMUTE-SHIFT-TEST
```

## 4.2 Lấy vehicle type

```http
GET {{base_url}}/api/catalog/vehicle-types?status=ACTIVE
```

Lưu ID loại ô tô và xe máy. Nếu dùng dữ liệu mẫu hiện tại, xe máy thường là:

```text
40000000-0000-0000-0000-000000000002
```

Không nên phụ thuộc ID mẫu nếu database đã thay đổi.

## 4.3 Tạo hai zone ACTIVE

Zone ô tô:

```http
POST {{base_url}}/api/parking/zones
```

```json
{
  "parkingLotId": "{{parkingLotId}}",
  "code": "CAR-ZONE-TEST",
  "name": "Khu ô tô kiểm thử",
  "vehicleTypeId": "<carVehicleTypeId>",
  "capacity": 300
}
```

Lưu `data.zoneId` vào `carZoneId`.

Zone xe máy:

```json
{
  "parkingLotId": "{{parkingLotId}}",
  "code": "MOTOR-ZONE-TEST",
  "name": "Khu xe máy kiểm thử",
  "vehicleTypeId": "<motorVehicleTypeId>",
  "capacity": 1200
}
```

Lưu `data.zoneId` vào `motorZoneId`.

Kỳ vọng cả hai response có `status = ACTIVE`.

## 4.4 Tạo hai gate ACTIVE

Gate ô tô:

```http
POST {{base_url}}/api/parking/gates
```

```json
{
  "zoneId": "{{carZoneId}}",
  "code": "CAR-GATE-TEST",
  "name": "Cổng ô tô kiểm thử"
}
```

Lưu `data.gateId` vào `carGateId`.

Gate xe máy:

```json
{
  "zoneId": "{{motorZoneId}}",
  "code": "MOTOR-GATE-TEST",
  "name": "Cổng xe máy kiểm thử"
}
```

Lưu `data.gateId` vào `motorGateId`.

## 4.5 Chuẩn bị tám employee ACTIVE

Cần:

- Sáu employee `FIXED`: hai người cho mỗi loại ca.
- Một employee `RELIEF` thay người nghỉ.
- Một employee `SPARE` để test manual assignment và replace.

Nếu chưa có employee, dùng token system admin:

```http
POST {{base_url}}/api/iam/accounts/provisioned
Authorization: Bearer {{admin_token}}
```

Ví dụ employee thứ nhất:

```json
{
  "username": "shift_emp_01",
  "email": "shift_emp_01@example.com",
  "roleCode": "EMPLOYEE",
  "fullName": "Nhân viên ca sáng ô tô"
}
```

Tạo tương tự `shift_emp_02` đến `shift_emp_08` với email khác nhau.

Sau đó lấy employee ID:

```http
GET {{base_url}}/api/people/employees?status=ACTIVE&keyword=shift_emp
```

Lưu tám `employeeId` vào các biến:

```text
empMorningCarId
empMorningMotorId
empAfternoonCarId
empAfternoonMotorId
empNightCarId
empNightMotorId
empReliefId
empSpareId
```

Lưu ý quan trọng: mỗi employee phải có account mang role `EMPLOYEE`. Chỉ có bản ghi `people.employees` nhưng account mang role manager sẽ bị lỗi:

```text
Only operational employees can be assigned
```

Để test `/shift-assignments/me`, đặt mật khẩu cho ít nhất một employee trong Keycloak và lấy `employee_token`.

# 5. Test `operations.shift_templates`

## 5.1 Tạo ba template

### Ca sáng

```http
POST {{base_url}}/api/operations/shift-templates
```

```json
{
  "parkingLotId": "{{parkingLotId}}",
  "shiftType": "MORNING",
  "name": "Ca sáng 06:00 - 14:00",
  "startLocalTime": "06:00:00",
  "endLocalTime": "14:00:00"
}
```

Lưu `data.shiftTemplateId` vào `morningTemplateId`.

### Ca chiều

```json
{
  "parkingLotId": "{{parkingLotId}}",
  "shiftType": "AFTERNOON",
  "name": "Ca chiều 14:00 - 22:00",
  "startLocalTime": "14:00:00",
  "endLocalTime": "22:00:00"
}
```

Lưu ID vào `afternoonTemplateId`.

### Ca đêm

```json
{
  "parkingLotId": "{{parkingLotId}}",
  "shiftType": "NIGHT",
  "name": "Ca đêm 22:00 - 06:00",
  "startLocalTime": "22:00:00",
  "endLocalTime": "06:00:00"
}
```

Lưu ID vào `nightTemplateId`.

Kỳ vọng mỗi request:

- HTTP `201`.
- `status = ACTIVE`.
- Thời lượng đúng tám giờ.

## 5.2 List/filter/detail

```http
GET {{base_url}}/api/operations/shift-templates?parkingLotId={{parkingLotId}}&status=ACTIVE
GET {{base_url}}/api/operations/shift-templates?parkingLotId={{parkingLotId}}&shiftType=NIGHT
GET {{base_url}}/api/operations/shift-templates/{{morningTemplateId}}
```

Kỳ vọng list đầu có đúng ba phần tử.

## 5.3 Update

```http
PUT {{base_url}}/api/operations/shift-templates/{{morningTemplateId}}
```

```json
{
  "name": "Ca sáng chính thức",
  "startLocalTime": "06:00:00",
  "endLocalTime": "14:00:00"
}
```

Kỳ vọng HTTP `200`, tên được đổi, `parkingLotId`, `shiftType`, `status` giữ nguyên.

## 5.4 Xóa mềm và activate

```http
DELETE {{base_url}}/api/operations/shift-templates/{{morningTemplateId}}
PATCH  {{base_url}}/api/operations/shift-templates/{{morningTemplateId}}/activate
```

Kỳ vọng sau DELETE là `INACTIVE`; sau activate là `ACTIVE`.

Phải activate lại trước khi sinh lịch tuần.

## 5.5 Negative test

Tạo template chỉ bảy giờ:

```json
{
  "parkingLotId": "{{parkingLotId}}",
  "shiftType": "MORNING",
  "name": "Template sai",
  "startLocalTime": "06:00:00",
  "endLocalTime": "13:00:00"
}
```

Kỳ vọng HTTP `400` với thông báo thời lượng phải đúng tám giờ.

# 6. Test `operations.employee_roster_rules`

Tất cả rule dùng `effectiveFrom = {{weekStartDate}}`, `effectiveTo = null`.

Ngày nghỉ phải khác nhau. Cấu hình chuẩn:

```text
MORNING   + CAR gate   → employee 1, nghỉ MONDAY
MORNING   + MOTOR gate → employee 2, nghỉ TUESDAY
AFTERNOON + CAR gate   → employee 3, nghỉ WEDNESDAY
AFTERNOON + MOTOR gate → employee 4, nghỉ THURSDAY
NIGHT     + CAR gate   → employee 5, nghỉ FRIDAY
NIGHT     + MOTOR gate → employee 6, nghỉ SATURDAY
RELIEF                 → employee 7, nghỉ SUNDAY
```

## 6.1 Tạo sáu rule FIXED

Ví dụ ca sáng, gate ô tô:

```http
POST {{base_url}}/api/operations/employee-roster-rules
```

```json
{
  "parkingLotId": "{{parkingLotId}}",
  "employeeId": "{{empMorningCarId}}",
  "preferredShiftType": "MORNING",
  "preferredGateId": "{{carGateId}}",
  "weeklyDayOff": "MONDAY",
  "assignmentMode": "FIXED",
  "effectiveFrom": "{{weekStartDate}}",
  "effectiveTo": null
}
```

Tạo năm request còn lại bằng ma trận trên. Kỳ vọng HTTP `201`, `status = ACTIVE`.

## 6.2 Tạo rule RELIEF

```json
{
  "parkingLotId": "{{parkingLotId}}",
  "employeeId": "{{empReliefId}}",
  "preferredShiftType": null,
  "preferredGateId": null,
  "weeklyDayOff": "SUNDAY",
  "assignmentMode": "RELIEF",
  "effectiveFrom": "{{weekStartDate}}",
  "effectiveTo": null
}
```

Kỳ vọng HTTP `201`, `assignmentMode = RELIEF` và hai field preferred là `null`.

## 6.3 List/filter/detail

```http
GET {{base_url}}/api/operations/employee-roster-rules?parkingLotId={{parkingLotId}}&status=ACTIVE
GET {{base_url}}/api/operations/employee-roster-rules?parkingLotId={{parkingLotId}}&preferredShiftType=MORNING
GET {{base_url}}/api/operations/employee-roster-rules?parkingLotId={{parkingLotId}}&assignmentMode=RELIEF
```

Kỳ vọng list đầu có đúng bảy rule: sáu FIXED và một RELIEF.

## 6.4 Update, delete mềm và activate

```http
PUT {{base_url}}/api/operations/employee-roster-rules/{rosterRuleId}
```

```json
{
  "preferredShiftType": "MORNING",
  "preferredGateId": "{{carGateId}}",
  "weeklyDayOff": "MONDAY",
  "assignmentMode": "FIXED",
  "effectiveFrom": "{{weekStartDate}}",
  "effectiveTo": null
}
```

```http
DELETE {{base_url}}/api/operations/employee-roster-rules/{rosterRuleId}
PATCH  {{base_url}}/api/operations/employee-roster-rules/{rosterRuleId}/activate
```

Kỳ vọng: DELETE chuyển `INACTIVE`; activate trả lại `ACTIVE`.

## 6.5 Negative test

- Tạo rule thứ hai có cùng `weeklyDayOff`: HTTP `409`.
- Tạo FIXED trùng `shiftType + gateId`: HTTP `409`.
- Tạo RELIEF thứ hai trong cùng thời gian hiệu lực: HTTP `409`.
- FIXED thiếu gate hoặc shift type: HTTP `400`.
- RELIEF có gate hoặc shift type: HTTP `400`.

# 7. Test sinh và quản lý `operations.shifts`

## 7.1 Sinh lịch tuần

```http
POST {{base_url}}/api/operations/work-schedules/generate-week
```

```json
{
  "parkingLotId": "{{parkingLotId}}",
  "weekStartDate": "{{weekStartDate}}"
}
```

Kỳ vọng:

- HTTP `201`.
- Message `Weekly work schedule generated successfully`.
- `data` có đúng 21 shift.
- Mỗi shift có `status = DRAFT`.
- Mỗi ngày có MORNING, AFTERNOON, NIGHT.
- Ca đêm có `endTime` thuộc ngày hôm sau.

Postman Tests:

```javascript
const body = pm.response.json();
pm.test("Generated 21 shifts", () => pm.expect(body.data).to.have.length(21));
pm.test("All shifts are DRAFT", () => {
  pm.expect(body.data.every(item => item.status === "DRAFT")).to.eql(true);
});
pm.environment.set("shiftId", body.data[0].shiftId);
```

Gọi lại cùng tuần phải trả HTTP `409`:

```text
Work schedule already exists for this week
```

Ngày không phải thứ Hai phải trả HTTP `400`.

## 7.2 List/filter/detail

```http
GET {{base_url}}/api/operations/shifts?parkingLotId={{parkingLotId}}&fromDate={{weekStartDate}}&toDate={{weekEndDate}}
GET {{base_url}}/api/operations/shifts?parkingLotId={{parkingLotId}}&shiftType=NIGHT&status=DRAFT
GET {{base_url}}/api/operations/shifts/{{shiftId}}
```

Kỳ vọng list đầu có 21 shift. `fromDate > toDate` phải trả HTTP `400`.

## 7.3 Duyệt tuần

Chỉ duyệt sau khi đã kiểm tra và khôi phục đủ hai assignment ACTIVE cho mọi shift.

```http
PATCH {{base_url}}/api/operations/work-schedules/approve-week
```

```json
{
  "parkingLotId": "{{parkingLotId}}",
  "weekStartDate": "{{weekStartDate}}"
}
```

Kỳ vọng:

- HTTP `200`.
- Đúng 21 phần tử.
- Tất cả có `status = SCHEDULED`.
- `approvedAt` khác rỗng.
- `approvedBy` bằng account manager hiện tại.

Nếu một shift thiếu assignment, kỳ vọng HTTP `409` và không shift nào được duyệt.

## 7.4 Hủy shift

Chọn một shift `DRAFT` hoặc `SCHEDULED` chưa dùng cho open:

```http
PATCH {{base_url}}/api/operations/shifts/{shiftId}/cancel
```

```json
{
  "reason": "Bãi xe tạm ngừng vận hành để bảo trì"
}
```

Kỳ vọng:

- HTTP `200`.
- `status = CANCELLED`.
- `cancellationReason` đúng nội dung đã gửi.
- `cancelledAt`, `cancelledBy` có giá trị.
- Assignment ACTIVE của shift chuyển sang REMOVED.

Hủy lại trả chính shift CANCELLED. Hủy shift OPEN hoặc CLOSED trả HTTP `409`.

## 7.5 Mở và đóng shift

API open chỉ chạy khi thời điểm hiện tại nằm trong `[startTime, endTime)`.

Để test ngay trên database local, chọn một shift SCHEDULED chưa cancel rồi chạy:

```sql
UPDATE operations.shifts
SET start_time = now() - INTERVAL '5 minutes',
    end_time = now() + INTERVAL '8 hours'
WHERE shift_id = '<shiftId>'
  AND status = 'SCHEDULED';
```

Đây chỉ là thao tác chuẩn bị local test, không dùng trong production.

Mở ca bằng manager:

```http
PATCH {{base_url}}/api/operations/shifts/{shiftId}/open
```

```json
{
  "openingCash": 500000,
  "note": "Đã nhận bàn giao tiền đầu ca"
}
```

Kỳ vọng `status = OPEN`, `openingCash = 500000`, `openedAt/openedBy` có giá trị.

Đóng ca:

```http
PATCH {{base_url}}/api/operations/shifts/{shiftId}/close
```

```json
{
  "closingCash": 3200000,
  "note": "Đã kiểm đếm và bàn giao cuối ca"
}
```

Kỳ vọng `status = CLOSED`, `closingCash = 3200000`, `closedAt/closedBy` có giá trị.

Negative test:

- `openingCash < 0`: HTTP `400`.
- Mở ca quá sớm hoặc sau endTime: HTTP `409`.
- Mở khi parking lot/gate không ACTIVE: HTTP `409`.
- Mở khi thiếu một assignment: HTTP `409`.
- Mở khi parking lot đã có shift OPEN khác: HTTP `409`.
- Đóng shift không ở trạng thái OPEN: HTTP `409`.

# 8. Test `operations.shift_assignments`

## 8.1 Kiểm tra assignment tự sinh

Sau generate:

```http
GET {{base_url}}/api/operations/shift-assignments?parkingLotId={{parkingLotId}}&fromDate={{weekStartDate}}&toDate={{weekEndDate}}&status=ACTIVE
```

Kỳ vọng đúng 42 assignment.

Theo một shift:

```http
GET {{base_url}}/api/operations/shifts/{{shiftId}}/assignments
```

Kỳ vọng đúng hai assignment ACTIVE, hai employee khác nhau, hai gate khác nhau.

Lưu một `shiftAssignmentId` vào `assignmentId`.

## 8.2 Tạo thủ công khi shift DRAFT

Vì shift tự sinh đã đủ hai assignment, trước hết xóa mềm một assignment:

```http
DELETE {{base_url}}/api/operations/shift-assignments/{{assignmentId}}
```

Kỳ vọng HTTP `200`; khi gọi detail, assignment có `status = REMOVED`.

Tạo assignment thay thế bằng employee spare và gate vừa trống:

```http
POST {{base_url}}/api/operations/shifts/{{shiftId}}/assignments
```

```json
{
  "employeeId": "{{empSpareId}}",
  "gateId": "<gateId-vua-bi-trong>"
}
```

Kỳ vọng HTTP `201`, `status = ACTIVE`.

Không được tạo nếu:

- Shift không phải DRAFT.
- Gate đã có assignment ACTIVE.
- Employee đã có assignment trong shift/ngày đó.
- Employee đủ sáu ca trong tuần.
- Employee không đủ tám giờ nghỉ.

## 8.3 Update assignment DRAFT

```http
PUT {{base_url}}/api/operations/shift-assignments/{assignmentId}
```

```json
{
  "employeeId": "{{empSpareId}}",
  "gateId": "{{carGateId}}"
}
```

Kỳ vọng HTTP `200`; `shiftId`, `status`, audit tạo ban đầu không bị client thay đổi.

Sau khi shift SCHEDULED, API update này phải trả HTTP `409`.

## 8.4 Replace employee

Chỉ thực hiện khi shift DRAFT hoặc SCHEDULED và chưa bắt đầu:

```http
PATCH {{base_url}}/api/operations/shift-assignments/{assignmentId}/replace
```

```json
{
  "replacementEmployeeId": "{{empSpareId}}",
  "reason": "Nhân viên được phân công xin nghỉ đột xuất"
}
```

Kỳ vọng:

- Assignment cũ chuyển `REMOVED`.
- Response là assignment mới `ACTIVE`.
- Shift và gate giữ nguyên.
- Employee được đổi sang `empSpareId`.
- Assignment mới có ID khác assignment cũ.

Employee mới trùng employee cũ hoặc reason rỗng trả HTTP `400`.

## 8.5 Swap hai assignment

Để test ổn định, chọn hai assignment ACTIVE trong cùng một shift SCHEDULED. Khi đó hai employee đổi gate nhưng không đổi ngày/ca nên không tạo xung đột thời gian.

```http
POST {{base_url}}/api/operations/shift-assignments/swap
```

```json
{
  "firstAssignmentId": "<assignment-A>",
  "secondAssignmentId": "<assignment-B>",
  "reason": "Hai nhân viên đã thống nhất đổi vị trí trực"
}
```

Kỳ vọng:

- HTTP `200`.
- Response có hai assignment mới ACTIVE.
- Hai assignment cũ chuyển REMOVED.
- Employee A nhận shift/gate của B và ngược lại.
- Nếu một bước lỗi, toàn bộ transaction rollback.

Dùng cùng một ID hai lần trả HTTP `400`.

## 8.6 Employee xem lịch của mình

Đổi Authorization sang:

```text
Bearer {{employee_token}}
```

```http
GET {{base_url}}/api/operations/shift-assignments/me?fromDate={{weekStartDate}}&toDate={{weekEndDate}}
```

Kỳ vọng:

- HTTP `200`.
- Chỉ có assignment của employee đăng nhập.
- Không truyền `employeeId` từ client.
- Nếu không truyền status, mặc định `ACTIVE`.

Manager không nên dùng endpoint `/me` để lấy lịch của employee khác; manager dùng filter `employeeId` ở endpoint list toàn bộ.

## 8.7 Employee mở ca được phân công

Dùng `employee_token` của một employee thuộc assignment của shift và gọi API open ở mục 7.5.

Kỳ vọng mở được nếu đúng thời gian và đủ điều kiện. Employee không thuộc assignment của ca bị từ chối.

# 9. Kiểm tra trực tiếp database

Đếm template:

```sql
SELECT shift_type, status, start_local_time, end_local_time
FROM operations.shift_templates
WHERE parking_lot_id = '<parkingLotId>'
ORDER BY start_local_time;
```

Đếm roster rule:

```sql
SELECT assignment_mode, preferred_shift_type, preferred_gate_id,
       weekly_day_off, status
FROM operations.employee_roster_rules
WHERE parking_lot_id = '<parkingLotId>'
ORDER BY assignment_mode, preferred_shift_type;
```

Đếm shift trong tuần:

```sql
SELECT status, COUNT(*)
FROM operations.shifts
WHERE parking_lot_id = '<parkingLotId>'
  AND shift_date BETWEEN '<weekStartDate>' AND '<weekEndDate>'
GROUP BY status;
```

Kiểm tra số assignment từng shift:

```sql
SELECT s.shift_code,
       COUNT(*) FILTER (WHERE sa.status = 'ACTIVE') AS active_assignments
FROM operations.shifts s
LEFT JOIN operations.shift_assignments sa ON sa.shift_id = s.shift_id
WHERE s.parking_lot_id = '<parkingLotId>'
  AND s.shift_date BETWEEN '<weekStartDate>' AND '<weekEndDate>'
GROUP BY s.shift_id, s.shift_code
ORDER BY s.shift_code;
```

Trước approve, mỗi shift phải có `active_assignments = 2`.

# 10. Thứ tự test khuyến nghị

1. Lấy token và kiểm tra permission.
2. Tạo parking lot, zone, gate và employee.
3. Tạo đúng ba shift template.
4. Test list/update/delete/activate template; cuối cùng giữ cả ba ACTIVE.
5. Tạo sáu FIXED rule và một RELIEF rule.
6. Test list/update/delete/activate roster rule; cuối cùng giữ đủ bảy ACTIVE.
7. Generate tuần tương lai.
8. Kiểm tra 21 shift và 42 assignment.
9. Test manual create/update/delete assignment khi shift còn DRAFT; khôi phục đủ hai assignment.
10. Approve tuần và kiểm tra toàn bộ shift SCHEDULED.
11. Test replace và swap assignment.
12. Test `/shift-assignments/me` bằng employee token.
13. Chuẩn bị thời gian local và test open/close.
14. Test cancel bằng một shift SCHEDULED khác.
15. Chạy các negative test và kiểm tra status code `400/404/409/403`.

# 11. Mã trạng thái kỳ vọng

```text
201 Created  : tạo template, roster rule, generate tuần, tạo assignment thủ công
200 OK       : get/list/update/activate/delete mềm/approve/open/close/cancel/replace/swap
400 Bad Request: thiếu field, enum/ngày sai, reason rỗng, tiền âm
401 Unauthorized: token thiếu, sai hoặc hết hạn
403 Forbidden: tài khoản thiếu permission
404 Not Found: không tìm thấy parking lot/template/rule/shift/assignment/employee/gate
409 Conflict : trùng dữ liệu hoặc vi phạm rule lịch làm việc/trạng thái
```

