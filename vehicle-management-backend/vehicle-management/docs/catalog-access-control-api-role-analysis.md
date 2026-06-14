# Phan tich nhom API catalog/access-control va phan quyen theo role

## 1. Pham vi

Tai lieu nay phan tich 4 nhom API:

- `/api/catalog/vehicle-types`
- `/api/catalog/ticket-types`
- `/api/catalog/card-types`
- `/api/access-control/cards`

Goc nhin su dung:

- PM: danh gia muc do du chuc nang de van hanh va uu tien bo sung.
- BA: mo ta nghiep vu, role nao duoc lam gi, rui ro neu thieu luong.
- Senior backend: doi chieu Clean Architecture, database/migration, security enforcement, test gap.

Nguon da doc:

- `AGENTS.md`
- `docs/backend-coding-standard.md`
- `docs/clean-architecture-guide.md`
- `docs/package-structure.md`
- `src/main/resources/db/vehicle_management.sql`
- Tat ca migration trong `src/main/resources/db/migration`
- Controller, request/response DTO, application port/use case, domain policy, persistence adapter, repository, entity va test hien co cua 4 nhom API.

## 2. Ket luan dieu hanh

4 nhom API hien tai da du cho CRUD co ban cua danh muc va quan ly kho the:

- Tao/sua/xem/xoa mem `vehicle-types`.
- Tao/sua/xem/xoa mem/kich hoat lai `ticket-types`.
- Tao/sua/xem/xoa mem `card-types`.
- Tao/sua/xem/loc/doi trang thai/retire `cards`.

Tuy nhien chua du de xem la hoan thien cho production, vi co 5 khoang ho can xu ly truoc:

1. Chua enforce permission cho 4 nhom API nay. Controller khong co `@PreAuthorize`, use case khong goi `CurrentAccountPortIn.requirePermission(...)`. Theo `SecurityConfig`, moi endpoint ngoai public deu can authenticated, nhung authenticated user nao cung co the goi 4 nhom API neu khong co check permission rieng.
2. Permission trong migration da seed rat ro (`VEHICLE_TYPE_*`, `TICKET_TYPE_*`, `CARD_TYPE_*`, `CARD_*`) nhung chua duoc noi vao code cho 4 module nay.
3. `vehicle-types` va `card-types` chua co dependency guard khi deactivate/update. Database co nhieu bang dang tham chieu `vehicle_type_id` va `card_type_id`; neu deactivate tuy y co the lam danh muc dang dung bi an khoi UI nhung nghiep vu van phu thuoc.
4. `ticket-types` co nghiep vu tot hon nhung dang dong cung code hop le trong policy: `DAILY`, `MONTHLY`, `QUARTERLY`, `YEARLY`, `FREE`. Neu BA muon cau hinh loai ve linh hoat hon, API hien tai chua du.
5. Chua co pagination, response user/public rieng, authorization tests va use case test cho `ticket-types`.

Khuyen nghi tong the:

- `PARKING_MANAGER` la role so huu 4 nhom API nay.
- `EMPLOYEE` chi nen co quyen doc cac danh muc/thong tin the can cho van hanh. Neu can doi trang thai the trong ca truc, nen tach permission status rieng thay vi cap full `CARD_UPDATE_ALL`.
- `CUSTOMER` khong nen truy cap cac API admin nay. Neu customer can xem loai xe/loai ve cong khai, tao API public/user response rieng chi tra du lieu `ACTIVE`.
- `SYSTEM_ADMIN` theo migration hien tai nen tap trung IAM/system, khong mac dinh quan tri nghiep vu bai xe. Neu san pham yeu cau "super admin" toan quyen, can seed them permission ro rang thay vi dua vao legacy permission.

## 3. Nen tang phan quyen hien tai

### 3.1 Dieu kien runtime

`SecurityConfig` cau hinh:

- `/api/public/auth/**`, actuator health/info, swagger duoc public.
- Tat ca endpoint con lai bat buoc authenticated.
- JWT converter nap permission code tu DB vao authority.
- `CurrentAccountSecurityAdapter.hasPermission(...)` chi tra true khi `CurrentAccountAccess.canUseBusinessPermissions()` true.

Dieu kien `canUseBusinessPermissions()`:

| Role | Dieu kien de dung business permission |
|---|---|
| `SYSTEM_ADMIN` | Account `ACTIVE` |
| `PARKING_MANAGER` | Account `ACTIVE` va employee record `ACTIVE` |
| `EMPLOYEE` | Account `ACTIVE` va employee record `ACTIVE` |
| `CUSTOMER` | Account `ACTIVE` |

Luu y: `CUSTOMER` co the dung permission cua minh, nhung voi cac nghiep vu `OWN` khac nhu customer vehicle, code con check them customer `ACTIVE` va `APPROVED`. 4 nhom API trong tai lieu nay hien khong co luong `OWN`.

### 3.2 Permission lien quan trong migration

Migration `V3__split_modules_actions_scopes.sql` seed cac permission sau:

| Module | Permission |
|---|---|
| `VEHICLE_TYPE` | `VEHICLE_TYPE_CREATE_ALL`, `VEHICLE_TYPE_READ_ALL`, `VEHICLE_TYPE_UPDATE_ALL`, `VEHICLE_TYPE_DELETE_ALL` |
| `TICKET_TYPE` | `TICKET_TYPE_CREATE_ALL`, `TICKET_TYPE_READ_ALL`, `TICKET_TYPE_UPDATE_ALL`, `TICKET_TYPE_DELETE_ALL` |
| `CARD_TYPE` | `CARD_TYPE_CREATE_ALL`, `CARD_TYPE_READ_ALL`, `CARD_TYPE_UPDATE_ALL`, `CARD_TYPE_DELETE_ALL` |
| `CARD` | `CARD_CREATE_ALL`, `CARD_READ_ALL`, `CARD_UPDATE_ALL`, `CARD_DELETE_ALL` |

Role assignment theo migration:

| Role | Quyen lien quan 4 nhom API |
|---|---|
| `SYSTEM_ADMIN` | Migration V3 chi seed chinh thuc nhom account/role/permission/audit/login. Khong seed full CRUD cho 4 nhom API nay. Neu DB chay tu sample SQL cu roi migrate, role ADMIN sau khi doi thanh SYSTEM_ADMIN co the con legacy permission `CARD_UPDATE_ALL` do mapping `MANAGE_CARD -> CARD_UPDATE_ALL`. Khong nen xem do la thiet ke chuan. |
| `PARKING_MANAGER` | Full CRUD cho `VEHICLE_TYPE`, `TICKET_TYPE`, `CARD_TYPE`, `CARD`. |
| `EMPLOYEE` | `VEHICLE_TYPE_READ_ALL`, `TICKET_TYPE_READ_ALL`, `CARD_READ_ALL`. Migration moi khong seed `CARD_TYPE_READ_ALL`; neu DB co legacy sample permission thi co the con them `CARD_UPDATE_ALL` do `MANAGE_CARD -> CARD_UPDATE_ALL`. Nen chuan hoa lai. |
| `CUSTOMER` | Khong co permission cho 4 nhom API admin nay. Chi co cac permission own/public o module khac. |

### 3.3 Trang thai enforce thuc te trong code

Hien tai 4 controller:

- `VehicleTypeController`
- `TicketTypeController`
- `CardTypeController`
- `CardController`

deu khong co `@PreAuthorize`.

4 use case:

- `VehicleTypeUseCaseImpl`
- `TicketTypeUsecaseImpl`
- `CardTypeUseCaseImpl`
- `CardUseCaseImpl`

deu khong inject `CurrentAccountPortIn` va khong goi `requirePermission(...)`.

Ket luan security:

- Theo DB/migration: role da duoc thiet ke phan quyen.
- Theo runtime hien tai: chi can authenticated la co the goi 4 nhom API, bat ke role.
- Day la gap can xu ly truoc khi noi frontend hoac public hoa he thong.

## 4. Phan tich `/api/catalog/vehicle-types`

### 4.1 Chuc nang hien co

| Method | Endpoint | Chuc nang hien tai |
|---|---|---|
| `POST` | `/api/catalog/vehicle-types` | Tao loai xe. Normalize `code`, `name`, `description`; default `isActive=true`; check duplicate `code`; sinh UUID. |
| `GET` | `/api/catalog/vehicle-types/{vehicleTypeId}` | Xem chi tiet loai xe theo id. |
| `GET` | `/api/catalog/vehicle-types?isActive=` | Lay danh sach, loc theo `isActive`, sap xep theo `code`. |
| `PUT` | `/api/catalog/vehicle-types/{vehicleTypeId}` | Cap nhat `code`, `name`, `description`, `isActive`; check duplicate code khac id. |
| `DELETE` | `/api/catalog/vehicle-types/{vehicleTypeId}` | Xoa mem bang cach set `isActive=false`. |

### 4.2 Doi chieu database/nghiep vu

`catalog.vehicle_types` dang duoc tham chieu boi:

- `people.customer_vehicles.vehicle_type_id` voi `ON DELETE RESTRICT`.
- `catalog.price_rules.vehicle_type_id` voi `ON DELETE RESTRICT`.
- `access_control.cards.vehicle_type_id` voi `ON DELETE SET NULL`.
- `parking.zones.vehicle_type_id` trong schema/migration cau truc bai xe.
- `parking.parking_sessions.vehicle_type_id` voi `ON DELETE RESTRICT`.

Vi vay loai xe khong chi la danh muc hien thi. No anh huong toi:

- Gia ve/price rule.
- Xe khach hang.
- The vat ly phu hop loai xe.
- Phien gui xe va van hanh bai xe.
- Cau truc zone/parking.

### 4.3 Da du chua?

Du cho CRUD co ban, chua du cho quan tri danh muc an toan.

Thieu hoac nen bo sung:

| Muc | Muc do | Ly do |
|---|---|---|
| Enforce permission | Bat buoc | DB da co `VEHICLE_TYPE_*` nhung code chua check. |
| Keyword search | Nen co | Admin/manager can tim theo code/name/description. |
| Pagination | Nen co | Danh muc co the nho, nhung nen dong bo voi cac API admin khac. |
| Activate endpoint rieng | Nen co | Hien co the set `isActive` qua PUT, nhung action restore nen ro rang: `PATCH /{id}/activate`. |
| Dependency guard truoc deactivate | Bat buoc | Khong nen deactivate loai xe dang co active price rule, active customer vehicle, active/open parking session hoac card dang dung. |
| Public/User active list | Nen co | UI customer/guest co the can danh sach loai xe active de dang ky xe/xem gia, khong nen dung AdminResponse. |

### 4.4 Phan quyen de xuat

| Role | Quyen de xuat |
|---|---|
| `SYSTEM_ADMIN` | Mac dinh khong quan ly nghiep vu loai xe. Neu san pham yeu cau super admin, cap explicit `VEHICLE_TYPE_*_ALL`. |
| `PARKING_MANAGER` | Create/read/update/delete/activate. |
| `EMPLOYEE` | Read only danh sach va chi tiet de van hanh/check-in/check-out. |
| `CUSTOMER` | Khong vao API admin. Neu can xem, tao endpoint public/user chi tra active vehicle types. |

## 5. Phan tich `/api/catalog/ticket-types`

### 5.1 Chuc nang hien co

| Method | Endpoint | Chuc nang hien tai |
|---|---|---|
| `POST` | `/api/catalog/ticket-types` | Tao loai ve. Normalize `code`, `name`, `description`; default `status=ACTIVE`; duration tinh tu `code`; check duplicate active code. |
| `GET` | `/api/catalog/ticket-types/{ticketTypeId}` | Xem chi tiet loai ve. |
| `GET` | `/api/catalog/ticket-types?status=&keyword=` | Lay danh sach, loc theo status va keyword. |
| `PUT` | `/api/catalog/ticket-types/{ticketTypeId}` | Chi update khi ticket type dang `ACTIVE`; khong cho doi code neu dang co active price rule; check duplicate active code. |
| `DELETE` | `/api/catalog/ticket-types/{ticketTypeId}` | Deactivate neu khong co active price rule va khong co subscription `PENDING`/`ACTIVE`. |
| `PATCH` | `/api/catalog/ticket-types/{ticketTypeId}/activate` | Kich hoat lai, check duplicate active code. |

### 5.2 Doi chieu migration/database

Schema goc co `is_active`, migration `V9__replace_ticket_type_is_active_with_status.sql` doi sang:

- `status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'`
- check constraint `status IN ('ACTIVE', 'INACTIVE')`
- unique partial index `uq_ticket_types_active_code` tren `code` khi `status='ACTIVE'`
- drop cot `is_active`

Dieu nay cho phep luu lich su/ban ghi inactive trung code, nhung chi mot ban ghi active cho moi code.

### 5.3 Da du chua?

Nhom nay tot nhat trong 3 danh muc catalog, nhung van can chot BA:

| Muc | Muc do | Ly do |
|---|---|---|
| Enforce permission | Bat buoc | DB da co `TICKET_TYPE_*` nhung code chua check. |
| Use case test | Bat buoc | Chua thay `TicketTypeUsecaseImplTest`; day la nhom co rule phuc tap nhat. |
| Pagination/sort | Nen co | List admin nen co paging khi data tang. |
| Lam ro fixed code hay configurable | Bat buoc ve BA | Policy chi chap nhan `DAILY`, `MONTHLY`, `QUARTERLY`, `YEARLY`, `FREE`. Neu can loai ve 7 ngay/15 ngay/custom, hien tai chua dap ung. |
| Public/User active list | Nen co | Customer/guest co the can xem loai ve active khi dang ky/xem gia. |

### 5.4 Luu y nghiep vu

`TicketTypePolicy.durationByCode(...)` dang hard-code duration:

| Code | Duration |
|---|---:|
| `DAILY` | 1 |
| `MONTHLY` | 30 |
| `QUARTERLY` | 90 |
| `YEARLY` | 365 |
| `FREE` | 180 |

Neu day la master data co dinh thi hop ly. Neu la danh muc quan tri linh hoat, can doi request de nhap `durationDays` va validation theo range thay vi hard-code.

### 5.5 Phan quyen de xuat

| Role | Quyen de xuat |
|---|---|
| `SYSTEM_ADMIN` | Mac dinh khong quan ly loai ve nghiep vu, tru khi co yeu cau super admin. |
| `PARKING_MANAGER` | Create/read/update/delete/activate. |
| `EMPLOYEE` | Read only de giai thich gia/ve cho khach va van hanh. |
| `CUSTOMER` | Khong vao API admin. Neu can xem, dung public/user active ticket types. |

## 6. Phan tich `/api/catalog/card-types`

### 6.1 Chuc nang hien co

| Method | Endpoint | Chuc nang hien tai |
|---|---|---|
| `POST` | `/api/catalog/card-types` | Tao loai the. Normalize field; default `isReturnRequired=true`, `isActive=true`; check duplicate code. |
| `GET` | `/api/catalog/card-types/{cardTypeId}` | Xem chi tiet loai the. |
| `GET` | `/api/catalog/card-types?isActive=` | Lay danh sach, loc theo `isActive`, sort theo `code`. |
| `PUT` | `/api/catalog/card-types/{cardTypeId}` | Cap nhat `code`, `name`, `description`, `isReturnRequired`, `isActive`; check duplicate code. |
| `DELETE` | `/api/catalog/card-types/{cardTypeId}` | Xoa mem bang `isActive=false`. |

### 6.2 Doi chieu database/nghiep vu

`catalog.card_types` duoc tham chieu boi `access_control.cards.card_type_id` voi `ON DELETE RESTRICT`.

Nghiep vu `is_return_required` quan trong vi quy dinh the co can thu hoi hay khong. Day khong chi la field hien thi, no anh huong quy trinh:

- The vang lai thu hoi khi checkout.
- The dang ky/thang co the gan dai han.
- The dac biet/VIP/co dinh co the co quy tac khac.

### 6.3 Da du chua?

Du CRUD co ban, chua du cho dong bo van hanh.

| Muc | Muc do | Ly do |
|---|---|---|
| Enforce permission | Bat buoc | DB co `CARD_TYPE_*`, code chua check. |
| `CARD_TYPE_READ_ALL` cho `EMPLOYEE` | Nen co | Employee co `CARD_READ_ALL` nhung khong co `CARD_TYPE_READ_ALL`, se kho hien thi dropdown/ten loai the khi van hanh. |
| Keyword search | Nen co | Tim theo code/name/description. |
| Activate endpoint rieng | Nen co | Nen dong bo voi `ticket-types`. |
| Dependency guard truoc deactivate | Bat buoc | Khong nen deactivate loai the dang co card active/assigned/in_use. |
| Admin response audit | Nen co | `CardTypeAdminResponse` hien khong co `createdAt/createdBy/updatedAt/updatedBy`, trong khi cac response admin khac co. |
| User/Public response | Tuy nhu cau | Neu frontend public can list loai the thi can response rieng, khong lo audit/internal. |

### 6.4 Phan quyen de xuat

| Role | Quyen de xuat |
|---|---|
| `SYSTEM_ADMIN` | Mac dinh khong quan ly loai the nghiep vu. |
| `PARKING_MANAGER` | Create/read/update/delete/activate. |
| `EMPLOYEE` | Read only. Nen bo sung `CARD_TYPE_READ_ALL` vao role neu employee can xem danh sach the. |
| `CUSTOMER` | Khong vao API admin. |

## 7. Phan tich `/api/access-control/cards`

### 7.1 Chuc nang hien co

| Method | Endpoint | Chuc nang hien tai |
|---|---|---|
| `POST` | `/api/access-control/cards` | Tao the moi; yeu cau `cardNumber`, `uid`, `cardTypeId`; `vehicleTypeId` optional; default `AVAILABLE`; check duplicate card number/uid; check card type va vehicle type ton tai. |
| `GET` | `/api/access-control/cards/{cardId}` | Xem chi tiet the. |
| `GET` | `/api/access-control/cards?status=&cardTypeId=&vehicleTypeId=&keyword=` | Loc danh sach theo status/cardType/vehicleType/keyword. |
| `PUT` | `/api/access-control/cards/{cardId}` | Cap nhat card number, uid, card type, vehicle type. Khong cho update khi `IN_USE`; khong cho doi cardNumber/uid neu da co operational history. |
| `PATCH` | `/api/access-control/cards/{cardId}/status` | Doi trang thai sang `BLOCKED`, `AVAILABLE`, `LOST`, `DAMAGED`, `RETIRED` theo rule domain. |
| `DELETE` | `/api/access-control/cards/{cardId}` | Retire the neu khong co active usage. |

### 7.2 Trang thai the

Theo DB check constraint va enum:

- `AVAILABLE`
- `ASSIGNED`
- `IN_USE`
- `LOST`
- `BLOCKED`
- `DAMAGED`
- `RETIRED`

Rule hien co:

- The moi mac dinh `AVAILABLE`.
- The `IN_USE` khong duoc update.
- Card number/uid khong duoc doi neu da co subscription, lost card report, parking session.
- Khong retire neu con subscription `PENDING/ACTIVE`, lost card report `OPEN`, parking session `OPEN/LOST_CARD`.
- `BLOCKED` bat buoc co `blockedAt` va `blockedReason`.
- Chi unblock ve `AVAILABLE` khi dang `BLOCKED`.

### 7.3 Da du chua?

Du cho inventory/card maintenance co ban. Chua du neu xem card module la vong doi day du cua the.

| Muc | Muc do | Ly do |
|---|---|---|
| Enforce permission | Bat buoc | DB co `CARD_*`, code chua check. |
| Tach status permission | Nen co | Neu employee can block/lost/damaged card, khong nen cap full `CARD_UPDATE_ALL` vi se cho sua cardNumber/uid/cardType. Nen co permission rieng nhu `CARD_CHANGE_STATUS_ALL` hoac endpoint-level guard rieng. |
| Include lookup display fields | Nen co | `CardAdminResponse` chi tra `cardTypeId`, `vehicleTypeId`, frontend thuong can `cardTypeCode/name`, `vehicleTypeCode/name`. |
| Pagination | Nen co | Card inventory co the lon. |
| Bulk import/create | Nen co | Nhap kho the RFID/NFC thuong theo lo/batch. |
| Scan/validate uid endpoint | Nen co | Can check nhanh UID doc tu dau doc co ton tai/chua dang dung. |
| Usage summary/history | Nen co | Manager can xem the dang gan subscription nao, phien gui xe nao, lost-card report nao. |
| State transition audit/history | Nen co | Doi status the la hanh dong nhay cam, can truy vet ai block/lost/retire va ly do. Audit chung co the du neu da bat auditing, nhung API nen expose lich su khi can. |

### 7.4 Phan quyen de xuat

| Role | Quyen de xuat |
|---|---|
| `SYSTEM_ADMIN` | Khong mac dinh quan ly kho the, tru khi co quyet dinh super admin. Khong nen dua vao legacy `CARD_UPDATE_ALL`. |
| `PARKING_MANAGER` | Full create/read/update/delete/change status. |
| `EMPLOYEE` | Read only theo migration chuan. Neu can xu ly su co tai cong, cho phep `change status` o tap trang thai gioi han (`BLOCKED`, `LOST`, `DAMAGED`) bang permission tach rieng. |
| `CUSTOMER` | Khong vao API admin. Khach hang se dung module lost card report/subscription/parking history rieng neu can. |

## 8. Ma tran API va permission de xuat

### 8.1 Vehicle types

| API | Permission nen enforce | `PARKING_MANAGER` | `EMPLOYEE` | `CUSTOMER` | `SYSTEM_ADMIN` |
|---|---|---:|---:|---:|---:|
| `POST /api/catalog/vehicle-types` | `VEHICLE_TYPE_CREATE_ALL` | Co | Khong | Khong | Tuy chinh sach |
| `GET /api/catalog/vehicle-types` | `VEHICLE_TYPE_READ_ALL` | Co | Co | Khong | Tuy chinh sach |
| `GET /api/catalog/vehicle-types/{id}` | `VEHICLE_TYPE_READ_ALL` | Co | Co | Khong | Tuy chinh sach |
| `PUT /api/catalog/vehicle-types/{id}` | `VEHICLE_TYPE_UPDATE_ALL` | Co | Khong | Khong | Tuy chinh sach |
| `DELETE /api/catalog/vehicle-types/{id}` | `VEHICLE_TYPE_DELETE_ALL` | Co | Khong | Khong | Tuy chinh sach |
| `PATCH /api/catalog/vehicle-types/{id}/activate` | `VEHICLE_TYPE_UPDATE_ALL` | Nen co | Khong | Khong | Tuy chinh sach |

### 8.2 Ticket types

| API | Permission nen enforce | `PARKING_MANAGER` | `EMPLOYEE` | `CUSTOMER` | `SYSTEM_ADMIN` |
|---|---|---:|---:|---:|---:|
| `POST /api/catalog/ticket-types` | `TICKET_TYPE_CREATE_ALL` | Co | Khong | Khong | Tuy chinh sach |
| `GET /api/catalog/ticket-types` | `TICKET_TYPE_READ_ALL` | Co | Co | Khong | Tuy chinh sach |
| `GET /api/catalog/ticket-types/{id}` | `TICKET_TYPE_READ_ALL` | Co | Co | Khong | Tuy chinh sach |
| `PUT /api/catalog/ticket-types/{id}` | `TICKET_TYPE_UPDATE_ALL` | Co | Khong | Khong | Tuy chinh sach |
| `DELETE /api/catalog/ticket-types/{id}` | `TICKET_TYPE_DELETE_ALL` | Co | Khong | Khong | Tuy chinh sach |
| `PATCH /api/catalog/ticket-types/{id}/activate` | `TICKET_TYPE_UPDATE_ALL` | Co | Khong | Khong | Tuy chinh sach |

### 8.3 Card types

| API | Permission nen enforce | `PARKING_MANAGER` | `EMPLOYEE` | `CUSTOMER` | `SYSTEM_ADMIN` |
|---|---|---:|---:|---:|---:|
| `POST /api/catalog/card-types` | `CARD_TYPE_CREATE_ALL` | Co | Khong | Khong | Tuy chinh sach |
| `GET /api/catalog/card-types` | `CARD_TYPE_READ_ALL` | Co | Nen co | Khong | Tuy chinh sach |
| `GET /api/catalog/card-types/{id}` | `CARD_TYPE_READ_ALL` | Co | Nen co | Khong | Tuy chinh sach |
| `PUT /api/catalog/card-types/{id}` | `CARD_TYPE_UPDATE_ALL` | Co | Khong | Khong | Tuy chinh sach |
| `DELETE /api/catalog/card-types/{id}` | `CARD_TYPE_DELETE_ALL` | Co | Khong | Khong | Tuy chinh sach |
| `PATCH /api/catalog/card-types/{id}/activate` | `CARD_TYPE_UPDATE_ALL` | Nen co | Khong | Khong | Tuy chinh sach |

### 8.4 Cards

| API | Permission nen enforce | `PARKING_MANAGER` | `EMPLOYEE` | `CUSTOMER` | `SYSTEM_ADMIN` |
|---|---|---:|---:|---:|---:|
| `POST /api/access-control/cards` | `CARD_CREATE_ALL` | Co | Khong | Khong | Tuy chinh sach |
| `GET /api/access-control/cards` | `CARD_READ_ALL` | Co | Co | Khong | Tuy chinh sach |
| `GET /api/access-control/cards/{id}` | `CARD_READ_ALL` | Co | Co | Khong | Tuy chinh sach |
| `PUT /api/access-control/cards/{id}` | `CARD_UPDATE_ALL` | Co | Khong | Khong | Tuy chinh sach |
| `PATCH /api/access-control/cards/{id}/status` | Hien tai: `CARD_UPDATE_ALL`; de xuat: permission status rieng | Co | Co neu duoc giao xu ly su co | Khong | Tuy chinh sach |
| `DELETE /api/access-control/cards/{id}` | `CARD_DELETE_ALL` | Co | Khong | Khong | Tuy chinh sach |

## 9. API nen bo sung

### 9.1 Public/User catalog APIs

Neu frontend customer/guest can hien thi loai xe/loai ve de dang ky hoac xem gia, khong nen dung admin API.

De xuat:

| API | Response | Chuc nang |
|---|---|---|
| `GET /api/public/catalog/vehicle-types` | `VehicleTypeUserResponse` | Lay danh sach loai xe active, chi tra id/code/name/description can thiet. |
| `GET /api/public/catalog/ticket-types` | `TicketTypeUserResponse` | Lay danh sach loai ve active, chi tra id/code/name/durationDays/description. |
| `GET /api/catalog/card-types/active` | `CardTypeAdminResponse` hoac lookup response | Cho internal UI lay card type active khi tao the. Endpoint nay van authenticated va enforce `CARD_TYPE_READ_ALL`. |

### 9.2 Activate endpoints dong bo

`ticket-types` da co activate. Nen bo sung:

| API | Chuc nang |
|---|---|
| `PATCH /api/catalog/vehicle-types/{vehicleTypeId}/activate` | Kich hoat lai loai xe. |
| `PATCH /api/catalog/card-types/{cardTypeId}/activate` | Kich hoat lai loai the. |

### 9.3 Card inventory APIs

| API | Chuc nang |
|---|---|
| `POST /api/access-control/cards/bulk-import` | Nhap kho the theo lo, validate duplicate cardNumber/uid. |
| `GET /api/access-control/cards/lookup?uid=` | Check nhanh UID doc tu dau doc, tra trang thai co ton tai/co kha dung. |
| `GET /api/access-control/cards/{cardId}/usage-summary` | Tong hop subscription, parking session, lost card report lien quan. |
| `GET /api/access-control/cards/{cardId}/status-history` | Xem lich su doi trang thai neu co audit/history. |

### 9.4 Paging/filter chuan hoa

Nen chuan hoa list admin ve:

- `page`
- `size`
- `sort`
- `keyword`
- status/isActive/cardTypeId/vehicleTypeId tuy module

Va tra `PageResponse<XxxAdminResponse>` neu project da co shared paging response.

## 10. Phuong an thuc hien ky thuat

### Phase 1 - Dong gap security

Them permission guard cho 4 nhom API theo mot trong hai cach:

1. Dung `@PreAuthorize("@permissionAuthorizer.hasPermission('...')")` tren controller, giong `CustomerController` va `CustomerVehicleController`.
2. Inject `CurrentAccountPortIn` vao use case va goi `requirePermission(...)`, giong `UserProfileUseCaseImpl`, `EmployeeUseCaseImpl`.

Khuyen nghi:

- Voi CRUD admin ro rang, dung `@PreAuthorize` o controller giup doc API de hieu.
- Voi rule phuc tap hoac endpoint co ALL/OWN, dat guard trong application/authorization service.
- Khong lap security adapter moi cho tung feature.

Permission mapping:

- VehicleType: create/read/update/delete tuong ung `VEHICLE_TYPE_*_ALL`.
- TicketType: create/read/update/delete/activate tuong ung `TICKET_TYPE_*_ALL`; activate dung `TICKET_TYPE_UPDATE_ALL`.
- CardType: create/read/update/delete/activate tuong ung `CARD_TYPE_*_ALL`; activate dung `CARD_TYPE_UPDATE_ALL`.
- Card: create/read/update/delete/status tuong ung `CARD_*_ALL`; status tam dung `CARD_UPDATE_ALL` neu chua them permission moi.

Can bo sung test:

- Controller/security test cho 403 khi thieu permission.
- Unit test verify use case guard neu chon check trong application.

### Phase 2 - Bo sung guard nghiep vu

VehicleType:

- Them port methods de check dependency:
  - active price rules by vehicle type
  - active customer vehicles by vehicle type
  - active/open parking sessions by vehicle type
  - active cards by vehicle type
  - active zones by vehicle type neu zone van gan vehicle type
- Khong cho deactivate neu dang duoc dung trong luong active.

CardType:

- Them `CardRepository.existsByCardTypeIdAndStatusIn(...)`.
- Khong cho deactivate neu con card `AVAILABLE`, `ASSIGNED`, `IN_USE`, `BLOCKED`, `LOST`, `DAMAGED` tuy chinh sach.

TicketType:

- Bo sung use case test.
- Chot lai BA: fixed-code hay configurable duration.

### Phase 3 - Hoan thien API UX

- Them keyword cho `vehicle-types`, `card-types`.
- Them pagination cho 4 list endpoints.
- Them activate endpoint cho `vehicle-types`, `card-types`.
- Them public/user active catalog endpoints neu frontend can.
- Them lookup display fields cho `CardAdminResponse`.
- Them audit fields cho `CardTypeAdminResponse` neu giu convention admin response day du.

### Phase 4 - Mo rong card inventory

- Bulk import cards.
- UID lookup/scan validation.
- Usage summary/history.
- Neu employee duoc thao tac su co the, tach permission `CARD_CHANGE_STATUS_ALL` va cap rieng cho `EMPLOYEE`.

## 11. De xuat chuan role cuoi cung

### 11.1 Chuan toi thieu theo san pham hien tai

| Role | Vehicle types | Ticket types | Card types | Cards |
|---|---|---|---|---|
| `SYSTEM_ADMIN` | Khong | Khong | Khong | Khong hoac read/update theo super-admin policy |
| `PARKING_MANAGER` | Full CRUD + activate | Full CRUD + activate | Full CRUD + activate | Full CRUD + change status |
| `EMPLOYEE` | Read | Read | Read | Read |
| `CUSTOMER` | Khong dung admin API | Khong dung admin API | Khong dung admin API | Khong dung admin API |

### 11.2 Neu can ho tro van hanh tai cong

| Role | Bo sung co the cap |
|---|---|
| `EMPLOYEE` | `CARD_CHANGE_STATUS_ALL` neu them permission moi, chi cho block/lost/damaged theo quy trinh. |
| `CUSTOMER` | Khong cap vao 4 API nay; dung lost card report/subscription/customer vehicle APIs rieng. |
| `SYSTEM_ADMIN` | Neu business muon super admin, cap explicit read/all hoac full CRUD trong role_permission, khong dua vao legacy permission cu. |

## 12. Rui ro neu giu nguyen

| Rui ro | Tac dong |
|---|---|
| Moi authenticated user goi duoc API admin | Customer/employee co the tao/sua/xoa danh muc hoac retire card neu biet endpoint. |
| Employee thieu `CARD_TYPE_READ_ALL` | UI van hanh the co the khong load du lookup card type neu security duoc enforce dung. |
| Deactivate vehicle/card type dang dung | UI va nghiep vu co the mau thuan: ban ghi active van phu thuoc vao danh muc da an. |
| `CARD_UPDATE_ALL` qua rong | Neu cap cho employee de doi status, employee cung co the sua cardNumber/uid/cardType. |
| Hard-code ticket type code | BA khong tao duoc goi ve moi neu sau nay co nhu cau 7 ngay/15 ngay/ban ngay. |
| Thieu test authorization | Gap security de tai dien khi refactor. |

## 13. Ket luan

4 nhom API da co nen CRUD va domain rule co ban, dac biet `cards` va `ticket-types` da co nhieu rule quan trong. Tuy nhien, de dat muc san sang production, viec dau tien phai lam la enforce permission theo role da seed trong DB/migration. Sau do can bo sung dependency guard cho deactivate, paging/search chuan hoa, active/public read models, va tach quyen doi trang thai the neu employee can tham gia xu ly su co.

Thu tu uu tien nen la:

1. Enforce permission va them authorization tests.
2. Chuan hoa role permission: `PARKING_MANAGER` full, `EMPLOYEE` read, `CUSTOMER` none, `SYSTEM_ADMIN` theo policy ro rang.
3. Them dependency guard cho `vehicle-types` va `card-types`.
4. Bo sung endpoint activate/search/paging va public/user lookup neu frontend can.
5. Mo rong card inventory nhu bulk import, UID lookup, usage summary/history.
