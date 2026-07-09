# Huong Dan Test Hardware Devices

Base URL:

```text
http://localhost:8080/vehicle-management
```

Token nen dung:

- PARKING_MANAGER hoac SYSTEM_ADMIN de test day du CRUD/status/delete.
- EMPLOYEE chi test duoc API xem danh sach va chi tiet.

Sau khi chay migration `V30__seed_device_permissions.sql`, role test can co:

- `DEVICE_CREATE_ALL`
- `DEVICE_READ_ALL`
- `DEVICE_UPDATE_ALL`
- `DEVICE_STATUS_UPDATE_ALL`
- `DEVICE_DELETE_ALL`

## 1. Kiem Tra Quyen

Chay SQL:

```sql
SELECT r.code AS role_code, p.permission_code
FROM iam.roles r
JOIN iam.role_permissions rp ON rp.role_id = r.role_id
JOIN iam.permissions p ON p.permission_id = rp.permission_id
WHERE r.code IN ('EMPLOYEE', 'PARKING_MANAGER', 'SYSTEM_ADMIN')
  AND p.permission_code LIKE 'DEVICE%'
  AND rp.is_active = TRUE
ORDER BY r.code, p.permission_code;
```

Ket qua mong doi:

```text
EMPLOYEE:
DEVICE_READ_ALL

PARKING_MANAGER/SYSTEM_ADMIN:
DEVICE_CREATE_ALL
DEVICE_DELETE_ALL
DEVICE_READ_ALL
DEVICE_STATUS_UPDATE_ALL
DEVICE_UPDATE_ALL
```

## 2. Chuan Bi Du Lieu

Can co:

- Mot parking lot khong `CLOSED`.
- Mot lane thuoc parking lot do, neu muon test device gan lane.
- Token manager/admin hop le.

Co the lay parking lot:

```http
GET /api/parking/parking-lots?status=ACTIVE
```

Co the lay lane:

```http
GET /api/parking/lanes?status=ACTIVE
```

Ghi lai:

```text
parkingLotId = PASTE_PARKING_LOT_ID
laneId = PASTE_LANE_ID
```

## 3. Tao Thiet Bi Gan Lane

API:

```http
POST /api/hardware/devices
Content-Type: application/json
```

Body:

```json
{
  "parkingLotId": "PASTE_PARKING_LOT_ID",
  "laneId": "PASTE_LANE_ID",
  "deviceCode": "cam-moto-in-01",
  "deviceType": "CAMERA",
  "name": "Camera cong xe may - lan vao",
  "ipAddress": "192.168.10.11",
  "config": {
    "position": "MOTO_IN",
    "resolution": "1080p",
    "rtspUrl": "rtsp://192.168.10.11:554/stream1"
  }
}
```

Ket qua mong doi:

- HTTP `201 Created`
- `deviceCode = CAM-MOTO-IN-01`
- `status = ACTIVE`
- Co `deviceId`
- Khong co `lastHeartbeatAt`

## 4. Tao Thiet Bi Khong Gan Lane

Dung cho may tinh/kiosk hoac camera toan canh.

API:

```http
POST /api/hardware/devices
Content-Type: application/json
```

Body:

```json
{
  "parkingLotId": "PASTE_PARKING_LOT_ID",
  "laneId": null,
  "deviceCode": "KIOSK-MOTO-GATE-01",
  "deviceType": "KIOSK",
  "name": "May tinh van hanh cong xe may",
  "ipAddress": "192.168.20.11",
  "config": {
    "os": "Windows",
    "location": "MOTO_GATE"
  }
}
```

Ket qua mong doi:

- HTTP `201 Created`
- `laneId = null`
- `status = ACTIVE`

## 5. Xem Chi Tiet

API:

```http
GET /api/hardware/devices/{deviceId}
```

Ket qua mong doi:

- HTTP `200 OK`
- Tra dung thiet bi vua tao.
- Co audit fields `createdAt`, `createdBy`, `updatedAt`, `updatedBy`.

## 6. List Va Filter

Lay tat ca:

```http
GET /api/hardware/devices
```

Loc theo status:

```http
GET /api/hardware/devices?status=ACTIVE
```

Loc theo loai thiet bi:

```http
GET /api/hardware/devices?deviceType=CAMERA
```

Loc theo parking lot:

```http
GET /api/hardware/devices?parkingLotId=PASTE_PARKING_LOT_ID
```

Tim keyword:

```http
GET /api/hardware/devices?keyword=CAM
GET /api/hardware/devices?keyword=192.168
```

Ket qua mong doi:

- HTTP `200 OK`
- Danh sach tra ve dung filter.
- Neu khong truyen filter thi tra ca `RETIRED`.

## 7. Update Thiet Bi

API:

```http
PUT /api/hardware/devices/{deviceId}
Content-Type: application/json
```

Body:

```json
{
  "parkingLotId": "PASTE_PARKING_LOT_ID",
  "laneId": "PASTE_LANE_ID",
  "deviceCode": "CAM-MOTO-IN-01",
  "deviceType": "CAMERA",
  "name": "Camera cong xe may - lan vao da cap nhat",
  "ipAddress": "192.168.10.111",
  "config": {
    "position": "MOTO_IN",
    "resolution": "2K"
  }
}
```

Ket qua mong doi:

- HTTP `200 OK`
- `name`, `ipAddress`, `config` duoc cap nhat.
- `status` khong doi.

## 8. Chuyen Offline

API:

```http
PATCH /api/hardware/devices/{deviceId}/offline
```

Ket qua mong doi:

- HTTP `200 OK`
- `status = OFFLINE`
- Goi lai lan nua van thanh cong va giu `OFFLINE`.

## 9. Chuyen Maintenance

API:

```http
PATCH /api/hardware/devices/{deviceId}/maintenance
```

Ket qua mong doi:

- HTTP `200 OK`
- `status = MAINTENANCE`
- Goi lai lan nua van thanh cong va giu `MAINTENANCE`.

## 10. Activate Lai

API:

```http
PATCH /api/hardware/devices/{deviceId}/activate
```

Ket qua mong doi:

- HTTP `200 OK`
- `status = ACTIVE`
- Neu device dang `RETIRED`, API nay van duoc phep phuc hoi neu parking lot khong `CLOSED` va lane neu co khong `CLOSED`.

## 11. Delete Mem

API:

```http
DELETE /api/hardware/devices/{deviceId}
```

Ket qua mong doi:

- HTTP `200 OK`
- Message `Device retired successfully`
- Goi detail lai thay `status = RETIRED`
- Goi delete lan nua van thanh cong, khong hard delete.

## 12. Test Loi Trung Device Code

Tao lai device voi `deviceCode` da ton tai:

```json
{
  "parkingLotId": "PASTE_PARKING_LOT_ID",
  "laneId": null,
  "deviceCode": "CAM-MOTO-IN-01",
  "deviceType": "CAMERA",
  "name": "Camera trung code",
  "ipAddress": "192.168.10.99",
  "config": {}
}
```

Ket qua mong doi:

- HTTP `409 Conflict`
- Message gan dung: `Device code already exists`

## 13. Test Loi Lane Khong Thuoc Parking Lot

Dung `parkingLotId` cua bai A va `laneId` cua bai B.

Ket qua mong doi:

- HTTP `409 Conflict`
- Message gan dung: `Lane does not belong to parking lot`

## 14. Test Retired Khong Duoc Offline/Maintenance

Sau khi delete mem device:

```http
PATCH /api/hardware/devices/{deviceId}/offline
PATCH /api/hardware/devices/{deviceId}/maintenance
```

Ket qua mong doi:

- HTTP `409 Conflict`
- Message gan dung: `Retired device cannot change to this status`

## 15. Test Dashboard

Sau khi tao mot so device:

```http
GET /api/dashboard/overview
```

Ket qua mong doi:

- `deviceStatus` co thong tin `CAMERA`, `KIOSK`, `CARD_READER`, `BARRIER`.
- `RETIRED` khong duoc tinh vao dashboard.

## 16. Loi Thuong Gap

Neu bi `403 Forbidden`:

- Token khong phai manager/admin.
- Chua chay migration `V30__seed_device_permissions.sql`.
- Role chua co permission `DEVICE_*`.

Neu bi `500` lien quan `last_heartbeat_at`:

- Code/schema chua dong bo rule bo heartbeat.
- Can tao/chay migration bo cot `last_heartbeat_at`.
- Can xoa field `lastHeartbeatAt` trong `DeviceEntity` va `Device`.

Neu bi `404 Parking lot not found`:

- Sai `parkingLotId`.
- Parking lot da bi xoa/khong ton tai.

Neu bi `409 Cannot use device for a closed parking lot`:

- Parking lot dang `CLOSED`, khong duoc tao/cap nhat/activate device cho bai do.
