# Ke hoach dua MinIO vao vehicle-management

## 1. Muc tieu tai lieu

Tai lieu nay tong hop hien trang `vehicle-management`, cac migration/database lien quan den phan quyen va file anh, dong thoi phan tich luong MinIO dang co trong `job24-backend` de de xuat cach dua MinIO vao `vehicle-management` theo huong Clean Architecture.

Pham vi uu tien giai doan dau:

- Anh ho so nguoi dung: `people.user_profile_avatars`.
- Anh su kien gui xe: `parking.parking_events.license_plate_image_path` va `parking.parking_events.person_image_path`; cot cu `image_path` khong con duoc dung.
- Khong thay doi cac nghiep vu da chot, chi them lop storage va API/luong cap nhat anh mot cach co kiem soat.

Tai lieu nay khong de xuat luu file truc tiep tren local disk. MinIO duoc xem la storage provider nam o tang infrastructure, application chi lam viec voi port.

## 2. Nguon da doc

- `vehicle-management/AGENTS.md`
- `vehicle-management/docs/backend-coding-standard.md`
- `vehicle-management/docs/clean-architecture-guide.md`
- `vehicle-management/docs/package-structure.md`
- `vehicle-management/src/main/resources/db/vehicle_management.sql`
- Tat ca migration trong `vehicle-management/src/main/resources/db/migration`
- Security, authorization, controller, use case va entity hien co trong `vehicle-management`
- MinIO flow trong `job24-backend`:
  - `config/MinioConfig.java`
  - `infrastructure/storage/MinioService.java`
  - `infrastructure/storage/MinioServiceImpl.java`
  - `infrastructure/storage/FileAccessService*.java`
  - `domain/enumStorage/BucketType.java`
  - `domain/enumStorage/FolderType.java`
  - `infrastructure/storage/ImageCompressionUtils.java`
  - cac adapter/use case upload anh/tai lieu nhu shift, chat, job image, marketing banner

## 3. Hien trang vehicle-management

### 3.1 Kien truc hien co

Project dang theo schema-first Clean Architecture:

- Controller: `entrypoint.controller.<schema>`
- DTO: `entrypoint.dto.<schema>.<table>.request|response`
- Application: use case, port in/out, mapper
- Domain: model, policy, service
- Infrastructure: JPA entity/repository/specification, persistence adapter, security adapter, external integration
- Shared: enum, exception, utility, API response

Quy tac can giu khi them MinIO:

- Controller khong duoc goi MinIO client truc tiep.
- Application khong phu thuoc MinIO SDK.
- Business rule va authorization nam o application/domain.
- Infrastructure chua MinIO adapter.
- Dung MapStruct khi mapping domain/entity/response.
- Dung `Instant` cho thoi gian.
- File/image handling la shared concern, khong tao moi adapter lap lai cho tung feature neu logic giong nhau.

### 3.2 Hien trang database cho anh/file

Trang thai cap nhat ngay 2026-06-21:

- Avatar khong con luu trong `people.user_profiles.avatar_url`.
- Schema snapshot `vehicle_management.sql` da co bang `people.user_profile_avatars` voi metadata avatar, `object_key`, bucket, status va `is_current`.
- Migration `V14__create_people_user_profile_avatars.sql`, `V15__backfill_people_user_profile_avatars.sql`, `V16__drop_people_user_profiles_avatar_url.sql` da the hien chuoi chuyen doi sang bang avatar rieng.
- `people.user_profiles.avatar_url` da bi drop khoi schema snapshot va JPA entity; chi con xuat hien trong migration lich su/backfill/drop.
- `parking.parking_events.license_plate_image_path` va `person_image_path` la hai cot anh nghiep vu cua check flow.
- Migration `V5__update_parking_check_images_and_price.sql` va `V6__update_parking_structure_for_check_flow.sql` them `license_plate_image_path` va `person_image_path`; migration sau V18 drop cot cu `image_path`.
- Schema snapshot parking van con drift voi migration check-flow: snapshot chua co `parking.gates`, `parking.lanes` van theo `parking_lot_id`, va `parking_sessions` van con `parking_space_id`/`price_rule_id` trong khi entity/migration da di theo `zone_id`.

Trong code hien tai:

- Da co dependency `io.minio:minio` va `metadata-extractor`.
- Da co config `app.storage.minio` trong `application.yaml`, gom endpoint, access key, secret key, public/private bucket, TTL, public URL base, image max size, max width va JPEG quality.
- Da co `MinioClient` bean, `MinioStorageProperties`, `MinioFileStorageAdapter`, `ImageFileProcessor`, `StorageObjectKeyGenerator`.
- Da co `FileStoragePort`, `FileAccessPort`, `StoreFileCommand`, `StoredFile`, `StorageUrlResolver`.
- Da co `StorageBucket` va `StorageFolder`; object key dang dung folder ngan nhu `av`, `pe`, `st`, `lcr` de giu duoi gioi han `VARCHAR(255)`.
- Da co API self avatar:
  - `POST /api/iam/accounts/profile/avatar`
  - `DELETE /api/iam/accounts/profile/avatar`
- Da co API avatar theo aggregate:
  - `POST /api/people/customers/{customerId}/avatar`
  - `DELETE /api/people/customers/{customerId}/avatar`
  - `POST /api/people/employees/{employeeId}/avatar`
  - `DELETE /api/people/employees/{employeeId}/avatar`
- Khong expose API avatar theo `userProfileId`; avatar admin/manager di qua aggregate customer/employee.
- `UserProfile`, `AccountProfileState` va response profile van con `avatarUrl` nhu enriched API field de frontend hien thi.
- `CompleteAccountProfileRequest`, `UpdateAccountProfileRequest`, `CreateUserProfileRequest`, `UpdateUserProfileRequest` van con field `avatarUrl` de backward compatibility, nhung mapper/use case da ignore write input nay.
- Self/customer/employee avatar deu delegate qua `UserProfileAvatarPortIn`; upload/delete chi ghi `people.user_profile_avatars`.
- `ParkingEventEntity`, `ParkingEvent` va response DTO da map `licensePlateImagePath` va `personImagePath`.

Ket luan:

- Avatar foundation va avatar aggregate da hoan thanh hon phase dau: source of truth hien la `people.user_profile_avatars`, khong phai column cu.
- API self avatar la dung huong va nen giu.
- API avatar theo `userProfileId` khong nen giu trong public contract vi UI quan tri thao tac theo customer/employee.
- API avatar theo aggregate nghiep vu da co va la contract chinh: `customers/{customerId}/avatar` va `employees/{employeeId}/avatar`.
- Anh su kien parking dung hai cot ro rang: `license_plate_image_path` va `person_image_path`.
- Cot `image_path` la legacy va khong con nam trong contract moi.

## 4. Hien trang phan quyen role

### 4.1 Co che authorization hien tai

`SecurityConfig` cho phep public:

- `/api/public/auth/**`
- `/actuator/health/**`
- `/actuator/info`
- Swagger/OpenAPI

Moi endpoint con lai can authenticated JWT.

`JwtAuthenticationConverter`:

- Doc `account_id` trong JWT hoac subject Keycloak.
- Load account noi bo tu DB qua `AccountAuthorizationPortOut`.
- Lay active permission codes theo role tu `iam.role_permissions`.
- Add tung permission code thanh `SimpleGrantedAuthority`.

`CurrentAccountAccess.canUseBusinessPermissions()`:

- Account phai `ACTIVE`.
- Neu role la `PARKING_MANAGER` hoac `EMPLOYEE`, employee record phai `ACTIVE`.
- `CUSTOMER` va `SYSTEM_ADMIN` khong bat buoc employee record.

`PermissionAuthorizer`:

- Delegate ve `CurrentAccountPortIn.hasPermission(...)`.
- Duoc controller dung qua `@PreAuthorize("@permissionAuthorizer.hasPermission('...')")`.

Dieu nay co nghia: quyen thuc thi API khong chi den tu role claim Keycloak, ma den tu role-permission trong database.

### 4.2 Permission model sau migration

Migration `V3__split_modules_actions_scopes.sql` chuan hoa permission thanh dang:

```text
<MODULE>_<ACTION>_<SCOPE>
```

Vi du:

- `ACCOUNT_CREATE_ALL`
- `CUSTOMER_VEHICLE_READ_OWN`
- `PARKING_EVENT_UPDATE_ALL`
- `PUBLIC_INFO_READ_PUBLIC`

Action hien co:

- CRUD co ban: `CREATE`, `READ`, `UPDATE`, `DELETE`
- Bo sung tu migration V10: `APPROVE`, `REJECT`, `SUSPEND`, `ACTIVATE`, `INACTIVATE`, `ASSIGN`, `UNASSIGN`, `CHECK_IN`, `CHECK_OUT`, `CANCEL`, `RESOLVE`, `CLOSE`, `STATUS_UPDATE`, `CONFIG_UPDATE`, `MARK_DEFAULT`, `MARK_READ`, `REFUND`, `RESET_PASSWORD`

Scope hien co:

- `ALL`: toan bo du lieu trong module
- `OWN`: du lieu cua chinh user/current account
- `ASSIGNED`: du lieu duoc phan cong
- `LOT`: du lieu trong pham vi bai xe
- `PUBLIC`: cong khai

### 4.3 Dieu kien hieu luc chung theo role

| Role | Dieu kien de permission co hieu luc |
| --- | --- |
| `SYSTEM_ADMIN` | Account `ACTIVE` |
| `PARKING_MANAGER` | Account `ACTIVE` va employee record `ACTIVE` |
| `EMPLOYEE` | Account `ACTIVE` va employee record `ACTIVE` |
| `CUSTOMER` | Account `ACTIVE`; rieng cac thao tac `OWN` tren customer vehicle yeu cau customer `ACTIVE` va `APPROVED` |

### 4.4 SYSTEM_ADMIN duoc lam gi

Theo migration V3 va V10, `SYSTEM_ADMIN` duoc seed quyen chinh:

- Quan ly account noi bo:
  - `ACCOUNT_CREATE_ALL`
  - `ACCOUNT_READ_ALL`
  - `ACCOUNT_UPDATE_ALL`
  - `ACCOUNT_DELETE_ALL`
- Quan ly role:
  - `ROLE_CREATE_ALL`
  - `ROLE_READ_ALL`
  - `ROLE_UPDATE_ALL`
  - `ROLE_DELETE_ALL`
  - `ROLE_ASSIGN_PERMISSION_ALL`
  - `ROLE_REVOKE_PERMISSION_ALL`
- Xem/quan ly permission catalog:
  - `PERMISSION_CREATE_ALL`
  - `PERMISSION_READ_ALL`
  - `PERMISSION_UPDATE_ALL`
  - `PERMISSION_DELETE_ALL`
- Xem audit/security:
  - `AUDIT_LOG_READ_ALL`
  - `LOGIN_ATTEMPT_READ_ALL`

Neu database duoc tao tu `vehicle_management.sql` roi chay migration, role `ADMIN` cu duoc doi thanh `SYSTEM_ADMIN` va cac role_permissions seed cu van ton tai sau khi doi ma permission. Khi do `SYSTEM_ADMIN` con co cac quyen ke thua tu seed goc:

- `REPORT_READ_ALL`
- `PRICE_PLAN_UPDATE_ALL`
- `CARD_UPDATE_ALL`
- `PARKING_SESSION_UPDATE_ALL`
- `PAYMENT_UPDATE_ALL`
- `SUBSCRIPTION_UPDATE_ALL`
- `USER_PROFILE_READ_OWN`
- `PARKING_SESSION_READ_OWN`
- `SUPPORT_TICKET_CREATE_OWN`
- `PUBLIC_INFO_READ_PUBLIC`

Luu y nghiep vu:

- Flow duyet system admin onboarding bat buoc caller la role `SYSTEM_ADMIN`.
- Khong duoc tu duyet chinh minh (`ensureNotSelfReview`).
- `SYSTEM_ADMIN` la role quan tri he thong/IAM, khong nen mac dinh xem anh camera/anh mat nguoi trong bai xe neu khong co permission parking tu DB.

### 4.5 PARKING_MANAGER duoc lam gi

Theo migration V3, `PARKING_MANAGER` duoc seed quyen van hanh bai xe rong:

- Quan ly nhan vien:
  - `EMPLOYEE_CREATE_ALL`
  - `EMPLOYEE_READ_ALL`
  - `EMPLOYEE_UPDATE_ALL`
  - `EMPLOYEE_DELETE_ALL`
- Quan ly khach hang:
  - `CUSTOMER_CREATE_ALL`
  - `CUSTOMER_READ_ALL`
  - `CUSTOMER_UPDATE_ALL`
  - `CUSTOMER_DELETE_ALL`
- Quan ly ho so nguoi dung:
  - `USER_PROFILE_CREATE_ALL`
  - `USER_PROFILE_READ_ALL`
  - `USER_PROFILE_UPDATE_ALL`
  - `USER_PROFILE_DELETE_ALL`
- Quan ly xe khach hang:
  - `CUSTOMER_VEHICLE_CREATE_ALL`
  - `CUSTOMER_VEHICLE_READ_ALL`
  - `CUSTOMER_VEHICLE_UPDATE_ALL`
  - `CUSTOMER_VEHICLE_DELETE_ALL`
- Quan ly catalog tinh gia:
  - `PRICE_PLAN_*_ALL`
  - `PRICE_RULE_*_ALL`
  - `VEHICLE_TYPE_*_ALL`
  - `TICKET_TYPE_*_ALL`
  - `CARD_TYPE_*_ALL`
- Quan ly the:
  - `CARD_CREATE_ALL`
  - `CARD_READ_ALL`
  - `CARD_UPDATE_ALL`
  - `CARD_DELETE_ALL`
- Quan ly ve thang/subscription:
  - `SUBSCRIPTION_CREATE_ALL`
  - `SUBSCRIPTION_READ_ALL`
  - `SUBSCRIPTION_UPDATE_ALL`
  - `SUBSCRIPTION_DELETE_ALL`
- Bao cao:
  - `REPORT_READ_ALL`
- Bao mat the:
  - `LOST_CARD_REPORT_CREATE_ALL`
  - `LOST_CARD_REPORT_READ_ALL`
  - `LOST_CARD_REPORT_UPDATE_ALL`
  - `LOST_CARD_REPORT_DELETE_ALL`
- Phien gui xe:
  - `PARKING_SESSION_CREATE_ALL`
  - `PARKING_SESSION_READ_ALL`
  - `PARKING_SESSION_UPDATE_ALL`
  - `PARKING_SESSION_DELETE_ALL`
- Su kien gui xe:
  - `PARKING_EVENT_CREATE_ALL`
  - `PARKING_EVENT_READ_ALL`
  - `PARKING_EVENT_UPDATE_ALL`
  - `PARKING_EVENT_DELETE_ALL`

Rang buoc code hien tai:

- `PARKING_MANAGER` chi duyet onboarding customer.
- `PARKING_MANAGER` duyet internal employee onboarding chi cho target role `EMPLOYEE`.
- `EmployeeAccessGuard` gioi han parking manager chi doc/quan ly employee target role `EMPLOYEE`, khong quan ly `PARKING_MANAGER` khac.

### 4.6 EMPLOYEE duoc lam gi

Theo migration V3, `EMPLOYEE` duoc seed quyen:

- Van hanh phien gui xe:
  - `PARKING_SESSION_CREATE_ALL`
  - `PARKING_SESSION_READ_ALL`
  - `PARKING_SESSION_UPDATE_ALL`
- Ghi nhan/xem/cap nhat su kien gui xe:
  - `PARKING_EVENT_CREATE_ALL`
  - `PARKING_EVENT_READ_ALL`
  - `PARKING_EVENT_UPDATE_ALL`
- Xu ly bao mat the:
  - `LOST_CARD_REPORT_CREATE_ALL`
  - `LOST_CARD_REPORT_READ_ALL`
  - `LOST_CARD_REPORT_UPDATE_ALL`
- Doc cac du lieu can cho van hanh:
  - `TICKET_TYPE_READ_ALL`
  - `CARD_READ_ALL`
  - `CUSTOMER_READ_ALL`
  - `USER_PROFILE_READ_ALL`
  - `CUSTOMER_VEHICLE_READ_ALL`
  - `VEHICLE_TYPE_READ_ALL`
- Xem ho so nhan vien cua minh:
  - `EMPLOYEE_READ_OWN`
- Tu xem/cap nhat ho so ca nhan:
  - `USER_PROFILE_READ_OWN`
  - `USER_PROFILE_UPDATE_OWN`
- Quyen owner-scope giong customer:
  - `CUSTOMER_VEHICLE_CREATE_OWN`
  - `CUSTOMER_VEHICLE_READ_OWN`
  - `CUSTOMER_VEHICLE_UPDATE_OWN`
  - `CUSTOMER_VEHICLE_DELETE_OWN`
  - `SUBSCRIPTION_CREATE_OWN`
  - `SUBSCRIPTION_READ_OWN`
  - `SUBSCRIPTION_UPDATE_OWN`
  - `PARKING_SESSION_READ_OWN`
  - `PARKING_EVENT_READ_OWN`
  - `PUBLIC_INFO_READ_PUBLIC`

Neu database duoc tao tu sample dump roi chay migration, `EMPLOYEE` con ke thua tu seed goc:

- `CARD_UPDATE_ALL` tu `MANAGE_CARD`
- `PAYMENT_UPDATE_ALL` tu `PROCESS_PAYMENT`
- `PARKING_SESSION_UPDATE_ALL` tu `OPERATE_PARKING_GATE`

Luu y nghiep vu:

- `EMPLOYEE` la role van hanh cong/lane, nen co the upload anh check-in/check-out cho event.
- Viec cho `EMPLOYEE` xem private image can gioi han theo pham vi ca truc/lane/lot trong tuong lai. Scope `ASSIGNED` va `LOT` da co san trong DB nhung chua thay duoc dung rong trong code.

### 4.7 CUSTOMER duoc lam gi

Theo migration V3, `CUSTOMER` duoc seed quyen:

- Ho so ca nhan:
  - `USER_PROFILE_READ_OWN`
  - `USER_PROFILE_UPDATE_OWN`
- Xe cua minh:
  - `CUSTOMER_VEHICLE_CREATE_OWN`
  - `CUSTOMER_VEHICLE_READ_OWN`
  - `CUSTOMER_VEHICLE_UPDATE_OWN`
  - `CUSTOMER_VEHICLE_DELETE_OWN`
- Ve thang/subscription cua minh:
  - `SUBSCRIPTION_CREATE_OWN`
  - `SUBSCRIPTION_READ_OWN`
  - `SUBSCRIPTION_UPDATE_OWN`
- Lich su gui xe va su kien cua minh:
  - `PARKING_SESSION_READ_OWN`
  - `PARKING_EVENT_READ_OWN`
- Thong tin cong khai:
  - `PUBLIC_INFO_READ_PUBLIC`

Neu database duoc tao tu sample dump roi chay migration, `CUSTOMER` con ke thua:

- `SUPPORT_TICKET_CREATE_OWN` tu `SEND_SUPPORT_TICKET`

Rang buoc code hien tai:

- `CustomerVehicleAccessGuard` yeu cau current customer phai ton tai, `CustomerStatus.ACTIVE`, `CustomerApprovalStatus.APPROVED`.
- Customer chi thao tac xe thuoc chinh customer id cua minh.

### 4.8 Role-policy rieng cho avatar va file

Avatar hien duoc quan ly bang `people.user_profile_avatars`, nhung API khong nen bat frontend quan ly truc tiep bang avatar/profile internals trong moi man hinh. Contract nen theo actor va aggregate dang thao tac.

| Actor | API nen dung | Permission/guard | Ghi chu |
| --- | --- | --- | --- |
| Moi current user authenticated | `POST/DELETE /api/iam/accounts/profile/avatar` | Authenticated current account; version sau co the them `USER_PROFILE_UPDATE_OWN` neu role seed duoc can bang lai | Dung de user tu doi avatar cua minh sau khi profile da ton tai. Avatar khong la dieu kien onboarding. |
| `PARKING_MANAGER` quan ly customer | `POST/DELETE /api/people/customers/{customerId}/avatar` | `CUSTOMER_UPDATE_ALL` | UI customer detail khong can biet `userProfileId`; backend resolve `customerId -> userProfileId`. |
| `PARKING_MANAGER` quan ly employee | `POST/DELETE /api/people/employees/{employeeId}/avatar` | `EMPLOYEE_UPDATE_ALL` + `EmployeeAccessGuard` | Parking manager chi nen thao tac employee target role `EMPLOYEE`, khong sua parking manager khac. |
| `SYSTEM_ADMIN` quan ly internal account | Can quyet dinh version sau: cap `EMPLOYEE_UPDATE_ALL` cho system admin, hoac cho phep qua `ACCOUNT_UPDATE_ALL` voi target hierarchy | Mac dinh seed hien tai cua `SYSTEM_ADMIN` nghieng ve IAM, khong co `USER_PROFILE_UPDATE_ALL`/`EMPLOYEE_UPDATE_ALL` trong migration V3 | Khong nen ngam cho system admin sua moi ho so neu DB permission khong cap. |
| `EMPLOYEE` | Chi self avatar qua `/api/iam/accounts/profile/avatar` | Authenticated/self; khong co `EMPLOYEE_UPDATE_ALL` | Employee co `USER_PROFILE_UPDATE_OWN`, khong duoc sua avatar customer/employee khac. |
| `CUSTOMER` | Chi self avatar qua `/api/iam/accounts/profile/avatar` | Authenticated/self; khong co `CUSTOMER_UPDATE_ALL` | Customer co `USER_PROFILE_UPDATE_OWN`, khong duoc sua avatar cua customer khac. |

Nguyen tac:

- Public API cho UI nen theo aggregate nghiep vu: account self, customer, employee.
- Endpoint theo `userProfileId` khong expose public API, vi `userProfileId` la chi tiet persistence dung chung. Logic avatar theo `userProfileId` chi nam trong `UserProfileAvatarPortIn` noi bo.
- Khi manager dang o man customer/employee, backend tu resolve `customerId`/`employeeId` sang `userProfileId`, sau do ghi/doc avatar current trong `people.user_profile_avatars`.
- Khong tiep tuc mo rong write request nhan `avatarUrl` string. Avatar nen di qua multipart endpoint rieng de validate file, resize, metadata va cleanup object cu.

## 5. API va permission hien co dang duoc enforce

### 5.1 API dang enforce permission tot

| Nhom API | Endpoint | Permission/guard |
| --- | --- | --- |
| Public auth | `POST /api/public/auth/register` | Public |
| Public auth | `POST /api/public/auth/resend-verification-email` | Public |
| Public auth | `POST /api/public/auth/forgot-password` | Public |
| Current account profile | `GET /api/iam/accounts/onboarding` | Authenticated current account |
| Current account profile | `POST /api/iam/accounts/onboarding` | Authenticated current account |
| Current account profile | `PATCH /api/iam/accounts/profile` | Authenticated current account |
| Current account avatar | `POST /api/iam/accounts/profile/avatar` | Authenticated current account, profile phai ton tai |
| Current account avatar | `DELETE /api/iam/accounts/profile/avatar` | Authenticated current account, profile phai ton tai |
| Account provisioning | `/api/iam/accounts/provisioned/**` | `ACCOUNT_CREATE_ALL`, `ACCOUNT_READ_ALL`, `ACCOUNT_UPDATE_ALL` |
| Role | `/api/iam/roles/**` | `ROLE_CREATE_ALL`, `ROLE_READ_ALL`, `ROLE_UPDATE_ALL`, `ROLE_DELETE_ALL` |
| Role permission | `/api/roles/{roleId}/permissions/**` | `ROLE_READ_ALL`, `ROLE_ASSIGN_PERMISSION_ALL`, `ROLE_REVOKE_PERMISSION_ALL` |
| Permission list | `GET /api/permissions` | `PERMISSION_READ_ALL` |
| User profiles | `GET /api/people/user-profiles/**` | Application requires `USER_PROFILE_READ_ALL` |
| Employees | `GET/PUT/DELETE/PATCH /api/people/employees/**` | Application requires `EMPLOYEE_READ_ALL`, `EMPLOYEE_UPDATE_ALL`, `EMPLOYEE_DELETE_ALL`; manager guard restricts target role |
| Customers | `GET/PUT/PATCH /api/people/customers/**` | `CUSTOMER_READ_ALL`, `CUSTOMER_UPDATE_ALL` |
| Customer avatar | `POST/DELETE /api/people/customers/{customerId}/avatar` | `CUSTOMER_UPDATE_ALL` in controller va use case |
| Employee avatar | `POST/DELETE /api/people/employees/{employeeId}/avatar` | `EMPLOYEE_UPDATE_ALL` in controller va use case, plus `EmployeeAccessGuard` |
| Customer vehicles | `/api/people/customer-vehicles/**` | `CUSTOMER_VEHICLE_*_ALL` or `CUSTOMER_VEHICLE_*_OWN` plus ownership guard |
| Catalog vehicle types | `/api/catalog/vehicle-types/**` | `VEHICLE_TYPE_*_ALL` in controller va use case |
| Catalog ticket types | `/api/catalog/ticket-types/**` | `TICKET_TYPE_*_ALL` in controller va use case |
| Catalog card types | `/api/catalog/card-types/**` | `CARD_TYPE_*_ALL` in controller va use case |
| Access control cards | `/api/access-control/cards/**` | `CARD_*_ALL` in controller va use case |
| Approval system admin | `/api/operations/approval-requests/system-admin-onboarding/**` | `ACCOUNT_READ_ALL`, `ACCOUNT_UPDATE_ALL`, plus role `SYSTEM_ADMIN`, no self-review |
| Approval internal employee | `/api/operations/approval-requests/internal-employee-onboarding/**` | `ACCOUNT_*` or `EMPLOYEE_*`, plus target hierarchy |
| Approval customer | `/api/operations/approval-requests/customer-onboarding/**` | `CUSTOMER_READ_ALL`, `CUSTOMER_UPDATE_ALL`, plus role `PARKING_MANAGER` |

### 5.2 API co permission seed trong DB nhung chua thay enforce ro trong code

Nhung controller/use case sau dang co CRUD nghiep vu, nhung chua thay `@PreAuthorize` hoac `currentAccountPortIn.requirePermission(...)` tuong ung:

- Catalog:
  - `/api/catalog/price-plans`
  - `/api/catalog/price-rules`
- Parking topology:
  - `/api/parking/parking-lots`
  - `/api/parking/zones`
  - `/api/parking/gates`
  - `/api/parking/lanes`
- Support ticket categories:
  - `/api/operations/support-ticket-categories`

De xuat truoc khi mo file/image API:

- Bo sung permission enforcement cho cac module con thieu.
- Dung application-layer guard cho file private; avatar aggregate hien da co controller annotation va use-case permission check.
- Them security tests cho moi API quan trong.

## 6. Phan tich luong MinIO trong job24-backend

### 6.1 Thanh phan chinh

`MinioConfig`:

- Tao `MinioClient` tu:
  - `minio.url`
  - `minio.access-key`
  - `minio.secret-key`

`BucketType`:

- `PUBLIC("public", "pb")`
- `PRIVATE("private", "pv")`
- Suy ra bucket tu prefix file name `pb-` hoac `pv-`.

`FolderType`:

- Cac folder nghiep vu: `AVATAR`, `IDENTITY`, `BUSINESS_LICENSE`, `CV`, `SHIFT`, `JOB`, `JOB_CATEGORY`, `STAFF_PROFILE`, `CHAT_ATTACHMENT`, `MARKETING_BANNER`.
- Build object name dang:

```text
<folder>/<bucket-prefix>-<safe-file-name>
```

`MinioService`:

- `uploadFile(MultipartFile file, BucketType bucketType, FolderType folderType)`
- `deleteFile(String objectName)`
- `getPresignedUrl(String objectName, int expireSeconds)`
- `validateFile(MultipartFile file, List<String> allowedExtensions, long maxSizeBytes)`

`FileAccessService`:

- Lay presigned URL mot file hoac nhieu file.

### 6.2 Luong upload trong Job24

1. Feature/use case nhan `MultipartFile`.
2. Validate file bang `minioService.validateFile(...)` hoac validate bo sung o feature.
3. Goi `minioService.uploadFile(file, bucketType, folderType)`.
4. `MinioServiceImpl`:
   - Lay bucket name.
   - Build object key.
   - Lay current username tu security.
   - Chan `.ico`.
   - Neu content type la image thi nen anh ve max width 1280, quality 0.8.
   - Upload `putObject` len MinIO.
   - Gan metadata `created_by`.
   - Tra ve `objectName`.
5. Feature luu `objectName` vao DB, vi du `imageUrl`, `filePath`, `objectName`.

### 6.3 Luong presigned URL trong Job24

1. Client goi API lay URL bang object name.
2. `FileAccessService` validate filename khong rong.
3. `MinioServiceImpl.getPresignedUrl(...)`:
   - Suy ra bucket tu prefix `pb`/`pv`.
   - `statObject` de dam bao object ton tai.
   - Neu public: tao presigned URL.
   - Neu private: lay current user, role, metadata `created_by`, co logic owner check nhung mot phan dang comment.
   - Tao presigned GET URL.
   - Tra ve path + query, khong tra full MinIO host.

### 6.4 Luong delete trong Job24

1. Feature xoa metadata DB hoac thay the anh.
2. Goi `minioService.deleteFile(objectName)`.
3. Service suy ra bucket, stat object, remove object.
4. Exception xoa file dang duoc log va phan lon bi swallow/comment.

### 6.5 Diem tot nen hoc

- Tach MinIO client o infrastructure.
- Co bucket public/private.
- Luu object key vao DB, khong luu binary.
- Upload anh co nen/resize va sua EXIF orientation.
- Co metadata `created_by`.
- Dung presigned URL cho truy cap file.
- Cac flow moi hon dung application port va infrastructure adapter, vi du `ShiftSlotStorageAdapter`, `ChatAttachmentStorageAdapter`, `JobPostingReportStorageAdapter`.
- Tests mock `MinioClient` va mock `MinioService` o use case/controller.

### 6.6 Diem can cai tien khi dua sang vehicle-management

- Object key Job24 chua co UUID, neu cung folder va cung ten file co the overwrite. Vehicle-management nen them UUID/ULID.
- `validateFile` trong Job24 dang comment extension/content type check o service chung. Vehicle-management nen enforce ro.
- Private authorization trong `getPresignedUrl` dang bi comment mot phan. Vehicle-management can enforce resource-level permission/ownership.
- `deleteFile` khong nen im lang hoan toan. Nen log co correlation id va co retry/outbox neu can.
- Khong nen cho client truyen filename bat ky de lay URL neu file la PII. Nen lay URL theo resource context.
- Suy bucket tu prefix filename la tien loi, nhung ve dai han nen co file metadata hoac object key convention bat buoc.
- Symlink check cua `FileAccessService` khong co nhieu y nghia voi object storage key. Vehicle-management nen validate object key format thay vi dung `Files.isSymbolicLink`.

## 7. Chuc nang de xuat cho vehicle-management

### 7.1 Ten chuc nang

Quan ly anh va file qua MinIO cho vehicle-management.

### 7.2 Mo ta chuc nang

He thong cho phep upload, luu tru, thay the, xoa va truy cap tam thoi cac anh/file nghiep vu qua MinIO. Backend chi luu object key va metadata can thiet trong PostgreSQL. Client khong truy cap MinIO bang credential truc tiep. Moi request upload/download/delete phai di qua API cua backend de kiem tra authentication, permission, ownership va business state.

Giai doan dau:

- Avatar profile: anh public hoac semi-public, gan voi `people.user_profile_avatars`.
- Parking event images: anh private, gan voi `parking.parking_events.license_plate_image_path`, `person_image_path`.

Giai doan sau:

- Anh/tai lieu support ticket.
- Anh lost card report.
- Anh/device snapshot tu camera hardware.
- Tai lieu hoa don/thanh toan neu co.

### 7.3 Nguyen tac thiet ke

- Khong expose JPA entity.
- Khong de controller goi MinIO.
- Application dung `FileStoragePort` va `FileAccessPort`.
- Infrastructure implement bang `MinioFileStorageAdapter`.
- Presigned URL khong duoc persist vao DB, chi tra ve trong response.
- DB luu object key on dinh.
- Private image phai di qua permission/ownership.
- Avatar co the public, nhung van nen upload qua backend de validate va audit.
- Parking event image la PII, phai private.

## 8. Kien truc de xuat

### 8.1 Package de xuat

```text
com.ban.vehicle_management
+-- application
|   +-- storage
|   |   +-- model
|   |   +-- port/in
|   |   +-- port/out
|   |   `-- usecase
|   +-- iam/account/usecase
|   `-- parking/parkingevent/usecase
+-- domain
|   `-- storage/model
+-- infrastructure
|   `-- storage
|       +-- MinioConfig.java
|       +-- MinioStorageProperties.java
|       +-- MinioFileStorageAdapter.java
|       +-- ImageProcessingService.java
|       `-- ObjectKeyGenerator.java
+-- entrypoint
|   `-- controller/storage
`-- shared
    `-- enumeration/storage
```

Neu muon toi gian hon, `application.storage` co the chi gom port out va model. Cac use case resource-specific nhu avatar/event image nam trong module chu quan (`iam.account`, `parking.parkingevent`).

### 8.2 Port/model de xuat

`FileStoragePort`:

```java
StoredFile store(StoreFileCommand command);
void delete(String objectKey);
boolean exists(String objectKey);
```

`FileAccessPort`:

```java
String createReadUrl(String objectKey, int expireSeconds);
Map<String, String> createReadUrls(Set<String> objectKeys, int expireSeconds);
```

`StoreFileCommand`:

- `MultipartFile file`
- `StorageBucket bucket`
- `StorageFolder folder`
- `String resourceType`
- `UUID resourceId`
- `UUID ownerAccountId`
- `Map<String, String> metadata`

`StoredFile`:

- `String objectKey`
- `String originalFilename`
- `String contentType`
- `long sizeBytes`
- `String checksumSha256` neu co the tinh

### 8.3 Enum de xuat

`StorageBucket`:

- `PUBLIC("vehicle-public", "pb")`
- `PRIVATE("vehicle-private", "pv")`

`StorageFolder`:

- `AVATAR("av", "avatar")`
- `PARKING_EVENT("pe", "parking-event")`
- `SUPPORT_TICKET("st", "support-ticket")`
- `LOST_CARD_REPORT("lcr", "lost-card-report")`

Ghi chu:

- Code hien tai dung folder segment ngan de object key khong vuot `VARCHAR(255)`.
- Display/business name cua file role nam rieng trong enum de response/log de doc hon neu can.

### 8.4 Object key convention

De tranh overwrite va giu key ngan hon 255 ky tu:

```text
<folder>/<yyyy>/<MM>/<dd>/<resource-id>/<bucket-prefix>-<uuid>-<kind>.<ext>
```

Vi du:

```text
avatars/2026/06/11/8b7.../pb-0dd...-avatar.jpg
parking-events/2026/06/11/91a.../pv-151...-license-plate.jpg
parking-events/2026/06/11/91a.../pv-7aa...-person.jpg
```

Neu can tuong thich voi cot `VARCHAR(255)`, key nen duoc tinh ngan:

```text
pe/2026/06/11/<event-id>/pv-<uuid>-plate.jpg
```

De xuat:

- Phase 1: giu object key duoi 255 ky tu de khong bat buoc migration length.
- Phase 2: tang cac cot image path/avatar len `VARCHAR(500)` hoac them bang `storage.files`.

### 8.5 Bucket policy

- `vehicle-public`: co the public read qua gateway/CDN cho avatar neu business chap nhan.
- `vehicle-private`: khong public. Tat ca read phai qua presigned URL.
- Khong dung chung bucket public/private cua Job24.
- Khong hardcode credential trong `application.yaml`; dung env vars.

Config de xuat:

```yaml
app:
  storage:
    minio:
      endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
      access-key: ${MINIO_ACCESS_KEY:minioadmin}
      secret-key: ${MINIO_SECRET_KEY:minioadmin}
      public-bucket: ${MINIO_PUBLIC_BUCKET:vehicle-public}
      private-bucket: ${MINIO_PRIVATE_BUCKET:vehicle-private}
      presigned-url-expire-seconds: ${MINIO_PRESIGNED_EXPIRE_SECONDS:900}
      public-url-base: ${MINIO_PUBLIC_URL_BASE:}
      image-max-size-bytes: ${MINIO_IMAGE_MAX_SIZE_BYTES:5242880}
      image-max-width-pixels: ${MINIO_IMAGE_MAX_WIDTH_PIXELS:1280}
      image-jpeg-quality: ${MINIO_IMAGE_JPEG_QUALITY:0.85}
```

## 9. Luong nghiep vu de xuat

### 9.1 Upload avatar current user

1. User authenticated goi API upload avatar.
2. Application lay current account.
3. Kiem tra profile da ton tai. Neu chua onboarding thi tra conflict hoac yeu cau complete onboarding truoc.
4. Validate file:
   - not empty
   - extension: `jpg`, `jpeg`, `png`, `webp`
   - content type image
   - decode duoc image
   - max size, vi du 5MB
5. Upload len `PUBLIC/AVATAR`.
6. Mark avatar current cu trong `people.user_profile_avatars` thanh `REPLACED` neu co.
7. Insert avatar moi `ACTIVE/is_current=true` vao `people.user_profile_avatars`.
8. Sau khi transaction commit, xoa object key cu neu co.
9. Response tra ve profile status va `avatarUrl` da resolve tu object key current.

Permission:

- Authenticated current user.
- Nen gan voi `USER_PROFILE_UPDATE_OWN` khi hoan thien RBAC cho own-profile.

### 9.2 Nguyen tac admin/manager cap nhat avatar

Manager/admin khong nen thao tac avatar qua `userProfileId` trong UI chinh. `people.user_profiles` la bang shared profile, con man hinh nghiep vu la customer hoac employee. Do do, API chinh nen resolve avatar qua aggregate:

- Customer detail: `customerId -> customer.userProfileId -> people.user_profile_avatars current`
- Employee detail: `employeeId -> employee.userProfileId -> people.user_profile_avatars current`

Endpoint `POST/DELETE /api/people/user-profiles/{userProfileId}/avatar` khong giu trong public contract. `UserProfileAvatarPortIn` chi la use case noi bo de cac aggregate customer/employee/self-avatar tai su dung logic upload/delete/cleanup.

### 9.3 Manager cap nhat avatar customer

1. Parking manager goi API upload/delete avatar trong man customer detail.
2. Application require `CUSTOMER_UPDATE_ALL`.
3. Load customer theo `customerId`; neu khong ton tai thi `404`.
4. Lay `userProfileId` tu customer; khong nhan `userProfileId` tu client.
5. Validate file va upload len `PUBLIC/AVATAR`.
6. Ghi avatar current vao `people.user_profile_avatars`.
7. Xoa object cu sau commit.
8. Response tra `CustomerAdminProfileResponse` hoac response avatar ngan gon co `customerId`, `userProfileId`, `avatarUrl`.

Permission:

- `PARKING_MANAGER` co `CUSTOMER_UPDATE_ALL` theo seed V3.
- `CUSTOMER` khong co quyen nay; customer tu sua avatar qua API self.
- `SYSTEM_ADMIN` chi nen duoc dung neu DB cap `CUSTOMER_UPDATE_ALL`, khong bypass role-permission.

### 9.4 Manager/admin cap nhat avatar employee

1. Caller goi API upload/delete avatar trong man employee detail.
2. Application require `EMPLOYEE_UPDATE_ALL` hoac policy dac biet duoc chot cho `SYSTEM_ADMIN`.
3. Load employee theo `employeeId`; neu khong ton tai thi `404`.
4. Apply target hierarchy:
   - `PARKING_MANAGER` chi update employee target role `EMPLOYEE`.
   - `SYSTEM_ADMIN` neu duoc cap quyen version sau thi chi update internal account/employee dung policy da chot.
5. Lay `userProfileId` tu employee; khong nhan `userProfileId` tu client.
6. Validate file va upload len `PUBLIC/AVATAR`.
7. Ghi avatar current vao `people.user_profile_avatars`.
8. Xoa object cu sau commit.
9. Response tra `EmployeeAdminResponse` hoac response avatar ngan gon co `employeeId`, `userProfileId`, `avatarUrl`.

Permission:

- `PARKING_MANAGER` co `EMPLOYEE_UPDATE_ALL` theo seed V3 va code hien co da co `EmployeeAccessGuard`.
- `EMPLOYEE` khong co `EMPLOYEE_UPDATE_ALL`; employee tu sua avatar qua API self.
- Can chot version sau: neu `SYSTEM_ADMIN` can sua avatar parking manager, nen them migration cap `EMPLOYEE_UPDATE_ALL` cho `SYSTEM_ADMIN` hoac tao guard rieng cho `ACCOUNT_UPDATE_ALL` + target role internal.

### 9.5 Upload anh parking event

Ap dung khi `parking_event` da ton tai hoac trong luong check-in/check-out tao event.

1. Caller la employee/manager goi API upload image cho event.
2. Application require:
   - `PARKING_EVENT_UPDATE_ALL` neu event da ton tai.
   - Hoac permission command cua luong nghiep vu neu image duoc upload ngay trong check-in/check-out.
   - Voi check-in hien tai: `PARKING_SESSION_CHECK_IN_ALL`; event `CHECK_IN` la side effect cua use case check-in, khong bat caller co them `PARKING_EVENT_CREATE_ALL`.
3. Load event va parking session lien quan.
4. Kiem tra business state:
   - Event ton tai.
   - Event type phu hop `CHECK_IN`, `CHECK_OUT`, `MANUAL_REVIEW` neu can.
   - Neu ve sau dung scope `LOT`/`ASSIGNED`, kiem tra employee duoc phan cong lot/lane/shift.
5. Validate file anh.
6. Upload len `PRIVATE/PARKING_EVENT`.
7. Cap nhat:
   - `license_plate_image_path` cho anh bien so.
   - `person_image_path` cho anh nguoi/lai xe.
   - Khong ghi `image_path` trong contract moi.
8. Xoa object cu sau commit.
9. Response tra object keys va presigned URL ngan han neu client can hien thi ngay.

Permission doc:

- `PARKING_MANAGER`: read/update/delete all event image.
- `EMPLOYEE`: create/read/update all event image theo seed hien tai.
- `CUSTOMER`: chi read own event image neu event thuoc parking session cua minh va co `PARKING_EVENT_READ_OWN`.
- `SYSTEM_ADMIN`: chi read/update neu co permission parking tu DB.

### 9.6 Lay URL xem anh parking event

1. Client yeu cau URL theo `parkingEventId`, khong gui object key tuy y.
2. Application load event/session.
3. Kiem tra permission:
   - `PARKING_EVENT_READ_ALL`, hoac
   - `PARKING_EVENT_READ_OWN` va session/customer vehicle thuoc current customer.
4. Tao presigned URL cho tung object key.
5. Tra response co TTL.

Khong de xuat API public `GET /api/files?filename=...` cho private object.

### 9.7 Xoa/thay the anh

1. Require permission update/delete cua resource.
2. Neu thay the:
   - Upload object moi truoc.
   - Update DB trong transaction.
   - Sau commit xoa object cu.
   - Neu DB fail thi xoa object moi de rollback storage.
3. Neu xoa:
   - Set cot path = null trong DB.
   - Sau commit xoa object trong MinIO.
   - Neu xoa MinIO fail thi log va dua vao retry job/outbox.

## 10. API de xuat

### 10.1 Avatar current user

#### `POST /api/iam/accounts/profile/avatar`

Upload hoac thay the avatar cua current account.

Request:

- `multipart/form-data`
- field `file`

Permission:

- Authenticated current account.
- Nen require `USER_PROFILE_UPDATE_OWN` khi RBAC own-profile duoc enforce.

Response:

- `AccountProfileStatusResponse`
- Them `avatarUrl` la object key hoac URL public theo convention response hien tai.

Nhiem vu:

- Validate anh.
- Upload MinIO public bucket.
- Ghi avatar current vao `people.user_profile_avatars`.
- Xoa avatar cu sau commit.

#### `DELETE /api/iam/accounts/profile/avatar`

Xoa avatar current account.

Permission:

- Authenticated current account.
- Nen require `USER_PROFILE_UPDATE_OWN`.

Nhiem vu:

- Mark avatar current thanh `DELETED/is_current=false`.
- Xoa object cu sau commit.

### 10.2 Avatar customer admin

#### `POST /api/people/customers/{customerId}/avatar`

Upload/thay avatar cua customer trong man quan ly customer.

Request:

- `multipart/form-data`
- field `file`

Permission:

- `CUSTOMER_UPDATE_ALL`

Muc dich:

- Dung cho `PARKING_MANAGER` cap nhat avatar customer khi xu ly ho so khach hang.
- Khong expose `userProfileId` cho frontend.
- Backend resolve `customerId -> userProfileId`.
- Upload MinIO public bucket va ghi avatar current vao `people.user_profile_avatars`.
- Xoa avatar cu sau commit.

Response de xuat:

- `CustomerAdminProfileResponse` neu man detail can refresh full profile.
- Hoac `CustomerAvatarResponse` ngan gon:

```json
{
  "customerId": "...",
  "userProfileId": "...",
  "avatarUrl": "https://..."
}
```

#### `DELETE /api/people/customers/{customerId}/avatar`

Xoa avatar cua customer trong man quan ly customer.

Permission:

- `CUSTOMER_UPDATE_ALL`

Muc dich:

- Mark avatar current thanh `DELETED/is_current=false`.
- Xoa object cu sau commit.
- Response tuong tu upload.

### 10.3 Avatar employee admin

#### `POST /api/people/employees/{employeeId}/avatar`

Upload/thay avatar cua employee trong man quan ly employee.

Request:

- `multipart/form-data`
- field `file`

Permission:

- `EMPLOYEE_UPDATE_ALL`
- Apply `EmployeeAccessGuard` de parking manager chi sua target role `EMPLOYEE`.

Muc dich:

- Dung cho `PARKING_MANAGER` cap nhat avatar employee.
- Khong expose `userProfileId` cho frontend.
- Backend resolve `employeeId -> userProfileId`.
- Upload MinIO public bucket va ghi avatar current vao `people.user_profile_avatars`.
- Xoa avatar cu sau commit.

Response de xuat:

- `EmployeeAdminResponse` neu man detail can refresh full employee.
- Hoac `EmployeeAvatarResponse` ngan gon:

```json
{
  "employeeId": "...",
  "userProfileId": "...",
  "avatarUrl": "https://..."
}
```

#### `DELETE /api/people/employees/{employeeId}/avatar`

Xoa avatar cua employee trong man quan ly employee.

Permission:

- `EMPLOYEE_UPDATE_ALL`
- Apply target hierarchy nhu upload.

### 10.4 Avatar user-profile internal use case

Khong expose public API theo `userProfileId`:

```text
POST /api/people/user-profiles/{userProfileId}/avatar
DELETE /api/people/user-profiles/{userProfileId}/avatar
```

Ly do:

- UI chinh thao tac theo `customerId` hoac `employeeId`.
- `userProfileId` la chi tiet noi bo cua shared profile.
- Neu expose API theo `userProfileId`, frontend co the bypass guard theo aggregate.

Use case noi bo van giu:

```text
UserProfileAvatarPortIn
UserProfileAvatarUseCaseImpl
```

Use case nay chi duoc goi tu cac luong:

- self avatar cua current account
- customer avatar aggregate
- employee avatar aggregate

### 10.5 Parking event images

#### `POST /api/parking/events/{parkingEventId}/images`

Upload/thay anh cho parking event.

Request:

- `multipart/form-data`
- Optional field `licensePlateImage`
- Optional field `personImage`
- Optional field `overviewImage`

Permission:

- `PARKING_EVENT_UPDATE_ALL`

Nhiem vu:

- Upload anh vao private bucket.
- Cap nhat `license_plate_image_path`, `person_image_path`.
- Tra response event image admin/user tuy permission.

#### `GET /api/parking/events/{parkingEventId}/images/presigned-urls`

Lay URL tam thoi de xem anh event.

Permission:

- `PARKING_EVENT_READ_ALL`, hoac
- `PARKING_EVENT_READ_OWN` neu event thuoc current customer.

Response de xuat:

```json
{
  "parkingEventId": "...",
  "expiresInSeconds": 900,
  "licensePlateImageUrl": "...",
  "personImageUrl": "...",
  "overviewImageUrl": "..."
}
```

#### `DELETE /api/parking/events/{parkingEventId}/images/{imageType}`

Xoa mot loai anh cua event.

`imageType`:

- `LICENSE_PLATE`
- `PERSON`
- `OVERVIEW`

Permission:

- `PARKING_EVENT_UPDATE_ALL`

Nhiem vu:

- Set cot tuong ung ve null.
- Xoa object sau commit.

### 10.6 Bulk presigned URL theo resource

#### `POST /api/parking/events/images/presigned-urls`

Lay presigned URL cho nhieu event, phuc vu list/detail admin.

Request:

```json
{
  "parkingEventIds": ["..."],
  "imageTypes": ["LICENSE_PLATE", "PERSON", "OVERVIEW"]
}
```

Permission:

- `PARKING_EVENT_READ_ALL`

Nhiem vu:

- Load event theo id.
- Kiem tra permission mot lan va filter resource ton tai.
- Tra map theo `parkingEventId`.

Khong de xuat bulk by filename cho private image.

### 10.7 API cho check-in/check-out trong tuong lai

Khi module `parking_session`/`parking_event` co endpoint nghiep vu:

#### `POST /api/parking/parking-sessions/check-in`

Request la multipart:

- JSON part `request`: cardUid, laneId, licensePlate, note.
- File part:
  - `licensePlateImage` bat buoc trong luong MinIO check-in hien tai.
  - `personImage` bat buoc trong luong MinIO check-in hien tai.

Permission:

- `PARKING_SESSION_CHECK_IN_ALL`

Nhiem vu:

- Validate lane/gate/zone active.
- Tao session `OPEN`.
- Generate `parkingEventId`.
- Upload anh private bucket voi `resourceId=parkingEventId`.
- Tao event `CHECK_IN` va luu object key vao event.

Ghi chu:

- Khong nhan JSON `imagePath` tu client trong check-in MinIO.
- `licensePlateImage` luu vao `parking_events.license_plate_image_path`.
- `personImage` luu vao `parking_events.person_image_path`.
- Cot cu `parking_events.image_path` khong con duoc dung.

#### `POST /api/parking/sessions/{parkingSessionId}/check-out`

Permission:

- `PARKING_SESSION_UPDATE_ALL`
- `PARKING_EVENT_CREATE_ALL`

Nhiem vu:

- Validate session open.
- Tinh gia.
- Tao event `CHECK_OUT`.
- Upload anh.
- Dong session neu hop le.

## 11. Response DTO de xuat

### 11.1 ParkingEventImageAdminResponse

Admin/full response:

- `parkingEventId`
- `licensePlateImagePath`
- `personImagePath`
- `licensePlateImageUrl`
- `personImageUrl`
- `expiresInSeconds`
- `createdAt`
- `createdBy`
- `updatedAt`
- `updatedBy`

### 11.2 ParkingEventImageUserResponse

User response:

- `parkingEventId`
- `licensePlateImageUrl`
- `personImageUrl` neu business cho phep customer xem
- `expiresInSeconds`

Khong expose:

- `createdBy`
- `updatedBy`
- internal object metadata
- bucket name

### 11.3 Avatar response

Current response dang co `avatarUrl`. Co hai cach:

1. Giu `avatarUrl` la object key va frontend goi API lay URL.
2. Tra `avatarUrl` la URL public/presigned da resolve.

De xuat:

- Voi public avatar: response co the tra URL public neu bucket public/read gateway da cau hinh.
- Trong DB van luu object key, khong luu URL signed.
- Write request khong nen tiep tuc nhan `avatarUrl` string; avatar nen duoc upload/delete qua multipart endpoint rieng.
- Neu can backward compatibility ngan han, chi chap nhan `avatarUrl` la object key do backend quan ly hoac ignore field nay trong request update profile.

## 12. Phuong an database

### 12.1 Avatar: bang rieng da la source of truth

Trang thai hien tai:

- `people.user_profile_avatars` da thay the `people.user_profiles.avatar_url`.
- DB luu MinIO object key trong `object_key`, khong luu public URL hoac presigned URL.
- `is_current=true` va `status=ACTIVE` xac dinh avatar hien tai cua moi profile.
- `avatarUrl` van la field response API, duoc resolve tu current avatar object key.
- `people.user_profiles.avatar_url` chi con trong migration lich su/backfill/drop, khong con trong schema snapshot/JPA entity.

Uu diem so voi cot cu:

- Co metadata `original_filename`, `content_type`, `size_bytes`, `checksum_sha256`.
- Co lifecycle `ACTIVE`, `REPLACED`, `DELETED`.
- Co `uploaded_by_account_id` va audit fields.
- De quan ly current avatar va cleanup object cu ro rang hon.

### 12.2 Parking event images: tan dung cot hien co

Khi lam parking event images, tiep tuc tan dung cac cot:

- `parking.parking_events.license_plate_image_path`
- `parking.parking_events.person_image_path`

Gia tri luu:

- MinIO object key private bucket, khong luu presigned URL.

Luu y:

- Schema snapshot `vehicle_management.sql` da dong bo `license_plate_image_path` va `person_image_path`.
- `ParkingEventEntity` va domain `ParkingEvent` da map hai cot anh moi.
- Neu key parking event co nguy co vuot 255 ky tu, can chot migration tang length rieng cho cac cot image path cua parking event.

### 12.3 Phase sau: Bang file metadata dung chung

Khi file lan ra nhieu module, nen them schema/table rieng:

```text
storage.files
- file_id UUID PK
- bucket VARCHAR(100)
- object_key VARCHAR(500) UNIQUE
- original_filename VARCHAR(255)
- content_type VARCHAR(100)
- size_bytes BIGINT
- checksum_sha256 VARCHAR(64)
- visibility VARCHAR(20) PUBLIC|PRIVATE
- resource_schema VARCHAR(50)
- resource_table VARCHAR(80)
- resource_id UUID
- owner_account_id UUID
- status VARCHAR(20) ACTIVE|DELETED
- created_at TIMESTAMPTZ
- created_by UUID
- updated_at TIMESTAMPTZ
- updated_by UUID
```

Luu y:

- Phase 2 moi nen tao schema `storage`, tranh over-engineer phase dau.
- Neu tao entity co audit fields thi dung audit abstraction cua project.

## 13. Security va privacy

### 13.1 Phan loai visibility

| Loai file | Bucket | Ly do |
| --- | --- | --- |
| Avatar profile | Public hoac private tuy chinh sach | It nhay cam hon, can hien thi UI |
| Anh bien so | Private | PII, lien quan lich su di chuyen |
| Anh nguoi/lai xe | Private | Du lieu sinh trac/nhan dang nhay cam |
| Anh lost card/support evidence | Private | Co the chua PII |
| Anh marketing/public content | Public | Noi dung cong khai |

### 13.2 Quyen truy cap anh parking event

- `PARKING_EVENT_READ_ALL`: xem toan bo event images.
- `PARKING_EVENT_READ_OWN`: chi xem event thuoc parking session cua customer hien tai.
- `PARKING_EVENT_UPDATE_ALL`: upload/thay/xoa anh event.
- `PARKING_EVENT_CREATE_ALL`: upload anh trong luong tao event.

Khong nen:

- Cho `CUSTOMER` lay object key tuy y.
- Cho `SYSTEM_ADMIN` xem anh private neu khong co permission parking.
- Dung bucket public cho anh bien so/anh nguoi.

### 13.3 TTL presigned URL

De xuat:

- Avatar public: URL public hoac presigned 1 gio den 24 gio.
- Private event image: 5 den 15 phut.
- Bulk admin list: 5 phut.
- Download bang chung: toi da 15 phut, co audit log.

### 13.4 Audit

Nen ghi audit cho:

- Upload avatar admin.
- Upload/thay/xoa event image.
- Tao presigned URL cho private parking event image neu can trace.

Khong log:

- Full presigned URL.
- MinIO secret/access key.
- Noi dung file.

## 14. Loi ich va gia tri nghiep vu

Cho van hanh bai xe:

- Luu anh check-in/check-out lam bang chung doi soat.
- Ho tro xu ly mat the, khieu nai phi, tranh chap bien so.
- Giam phu thuoc local disk cua backend.
- De scale multi-instance backend.

Cho customer:

- Avatar/profile thong nhat.
- Ve sau co the xem lich su gui xe kem bang chung neu policy cho phep.

Cho BA/PM:

- Giai doan dau it xam lan, khong doi luong chinh.
- Co duong nang cap sang file metadata va direct upload khi dung camera/device that.

## 15. Rủi ro va de xuat xu ly

| Rủi ro | Tac dong | De xuat |
| --- | --- | --- |
| Mot so API van chua enforce permission dong bo, dac biet `price-plans`, `price-rules`, parking topology va support ticket categories | Mo file/private image API co the tao lo hong bao mat neu copy pattern cu | Bo sung guard/security tests truoc hoac cung phase; catalog vehicle/ticket/card type va cards da co hardening tot hon |
| Cot path chi `VARCHAR(255)` | Object key dai co the loi DB | Convention key ngan hoac migration len `VARCHAR(500)` |
| Upload thanh cong nhung DB save fail | File rac tren MinIO | Xoa object moi trong catch hoac transaction synchronization |
| DB save thanh cong nhung delete old file fail | File orphan | Outbox/retry cleanup job |
| Client lay URL bang filename tuy y | Lộ file neu doan duoc key | Presign theo resource id va permission |
| Public bucket dung sai cho PII | Lo anh bien so/nguoi | Private mac dinh, public chi avatar/public content |
| Validate file chi dua vao extension | Upload file doc hai doi duoi | Check content type, decode image, magic bytes neu co the |
| Ten file trung overwrite | Mat file cu | Object key bat buoc co UUID |
| Presigned URL bi log/copy | Truy cap trai phep trong TTL | TTL ngan, khong log URL, HTTPS |

## 16. Trang thai thuc hien va roadmap

### Da lam trong phase hien tai

MinIO foundation:

- Da add dependency `io.minio:minio`.
- Da add dependency `metadata-extractor` de ho tro EXIF orientation.
- Da add config `app.storage.minio` trong `application.yaml`.
- Da tao `MinioClient` bean.
- Da tao `MinioStorageProperties`.
- Da tao `StorageBucket`, `StorageFolder`.
- Da tao `FileStoragePort`, `FileAccessPort`, `StoredFile`, `StoreFileCommand`.
- Da implement `StorageObjectKeyGenerator` voi object key co UUID va folder segment ngan.
- Da implement `MinioFileStorageAdapter`.
- Da implement public bucket policy khi bucket moi duoc tao.
- Da support `public-url-base`; neu cau hinh thi response co the tra URL public, neu khong thi fallback presigned/read URL.
- Da upload metadata co resource/owner context.

Image processing:

- Da validate file rong, kich thuoc, content type/extension hop le.
- Da support `jpg`, `jpeg`, `png`, `webp`.
- Da decode image de tranh file gia danh.
- Da resize anh theo max width.
- Da nen JPEG theo quality cau hinh.
- Da xu ly EXIF orientation cho JPEG.
- Da validate WebP RIFF.

Avatar self-service:

- Da co `POST /api/iam/accounts/profile/avatar`.
- Da co `DELETE /api/iam/accounts/profile/avatar`.
- Da yeu cau current account co profile truoc khi upload/delete avatar.
- Da upload avatar vao public bucket/folder `AVATAR`.
- Da ghi avatar vao `people.user_profile_avatars`, khong con ghi `people.user_profiles.avatar_url`.
- Da delete object cu sau transaction commit.
- Da cleanup object moi neu DB update fail.
- Da resolve public avatar URL trong response neu object key do backend quan ly.
- Account moi tao tu public register/provisioned account da co `userProfileId` toi thieu tu `fullName`, nen self avatar co the dung truoc khi customer/employee onboarding hoan tat.

Avatar aggregate:

- Da co `POST /api/people/customers/{customerId}/avatar`.
- Da co `DELETE /api/people/customers/{customerId}/avatar`.
- Da co `POST /api/people/employees/{employeeId}/avatar`.
- Da co `DELETE /api/people/employees/{employeeId}/avatar`.
- Customer avatar require `CUSTOMER_UPDATE_ALL` o controller va use case.
- Employee avatar require `EMPLOYEE_UPDATE_ALL` o controller va use case, dong thoi reuse `EmployeeAccessGuard`.

Avatar user-profile internal use case:

- Khong expose `POST /api/people/user-profiles/{userProfileId}/avatar`.
- Khong expose `DELETE /api/people/user-profiles/{userProfileId}/avatar`.
- Van giu `UserProfileAvatarPortIn` va `UserProfileAvatarUseCaseImpl` de cac aggregate customer/employee/self-avatar tai su dung logic upload/delete/cleanup.

Tests da co:

- `StorageObjectKeyGeneratorTest`.
- `ImageFileProcessorTest`.
- `MinioFileStorageAdapterTest`.
- `StorageUrlResolverTest`.
- `AccountProfileAvatarUseCaseImplTest`.
- `UserProfileAvatarUseCaseImplTest`.
- `CustomerAdminProfileUseCaseImplTest`.
- `EmployeeUseCaseImplTest`.
- `UserProfileAvatarPersistenceAdapterTest`.

Ghi chu test tong:

- Cac test lien quan MinIO/avatar da duoc bo sung.
- Tai lieu nay khong chay lai full test suite tai lan cap nhat 2026-06-21; `minio-phased-implementation-plan.md` ghi nhan lan verify gan nhat cua chuoi avatar la 536 pass sau khi drop `avatar_url`.

### Chua lam - uu tien version tiep theo

Avatar policy/API cleanup:

- Chot policy `SYSTEM_ADMIN` co duoc sua avatar parking manager hay khong:
  - Neu co, them migration cap `EMPLOYEE_UPDATE_ALL` cho `SYSTEM_ADMIN`, hoac
  - Tao guard rieng cho `ACCOUNT_UPDATE_ALL` + target role internal.
- Co the remove/deprecate field `avatarUrl` khoi request DTO khi frontend/backward compatibility cho phep.
- Response van giu `avatarUrl` de frontend hien thi.
- Chot co require `USER_PROFILE_UPDATE_OWN` cho self avatar hay giu authenticated + profile ready.

Hardening permission:

- Bo sung permission enforcement cho `price-plans`.
- Bo sung permission enforcement cho `price-rules`.
- Bo sung permission enforcement cho parking topology: `parking-lots`, `zones`, `gates`, `lanes`.
- Bo sung permission enforcement cho `support-ticket-categories`.
- Them controller/use case security tests cho cac API file va module con thieu.
- Catalog vehicle/ticket/card type va access-control cards da co permission guard o controller va use case; khong con nam trong nhom con thieu.

Storage/database:

- Khong can migration tang length `avatar_url` vi column nay da drop.
- Can chot co migration tang length cac image path cua parking event len `VARCHAR(500)` hay tiep tuc giu key ngan duoi 255.
- Can dong bo `vehicle_management.sql` voi migration parking V5/V6/V7/V8 truoc khi lam private parking image/check-flow.
- Can co cleanup/retry job hoac outbox cho object cu xoa fail.
- Can audit log cho admin/manager upload/delete avatar neu yeu cau trace.

### Phase 3: Parking event images

- Update `ParkingEventEntity`, domain, mapper them:
  - `licensePlateImagePath`
  - `personImagePath`
- Tao response DTO admin/user cho event images.
- Tao port/use case upload/get/delete event images.
- Tao controller `ParkingEventImageController` hoac nam trong future `ParkingEventController`.
- Tests:
  - employee upload event image
  - manager get all image URL
  - customer get own image URL
  - customer cannot get others image
  - private object always presigned
  - DB fail then uploaded object cleanup

### Phase 4: Check-in/check-out integration

- Khi check-in/check-out API duoc implement:
  - Nhan multipart image trong request.
  - Upload image trong use case.
  - Persist object key vao event.
  - Gan actor/current account.
  - Gan event time bang `Instant`.

### Phase 5: Mo rong

- Support ticket attachments.
- Lost card report evidence.
- Camera/device upload direct-to-MinIO bang presigned PUT.
- File metadata table va cleanup scheduler.

## 17. Test plan

### Unit tests

- `ObjectKeyGeneratorTest`
  - key co UUID
  - key dung folder/bucket prefix
  - key khong vuot do dai quy dinh
  - sanitize filename
- `ImageValidationServiceTest`
  - reject empty file
  - reject too large
  - reject invalid content type
  - reject unreadable image
  - accept jpg/png/webp
- `MinioFileStorageAdapterTest`
  - create bucket if missing
  - upload sets content type and metadata
  - delete object
  - presigned URL
  - MinIO exception mapped to domain/shared exception

### Application tests

- Avatar:
  - current user upload success
  - replace old avatar
  - profile not ready
  - validation fail
- Parking event image:
  - manager/employee update success
  - customer read own success
  - customer read other forbidden
  - missing event not found
  - DB failure cleanup uploaded object

### Controller tests

- Multipart binding.
- HTTP status:
  - 201 for new upload resource if tao moi metadata
  - 200 for replace/update
  - 400 invalid file
  - 401 unauthenticated
  - 403 forbidden
  - 404 missing event/profile

### Integration tests

- Neu co Testcontainers MinIO: upload -> stat -> presign -> delete.
- Neu CI chua san sang: mock `FileStoragePort` o controller/application tests, de MinIO adapter unit test mock MinioClient.

## 18. Ket luan de xuat

Nen dua MinIO vao `vehicle-management` theo huong tung buoc:

1. Khong copy local `FileStorageService` cu tu Job24.
2. Copy y tuong MinIO service/adapter, nhung sua cac diem yeu: UUID object key, validate that, private authorization, khong presign theo filename tuy y.
3. Avatar da lam xong theo huong bang rieng `people.user_profile_avatars`; DB luu object key, response van giu `avatarUrl`.
4. API avatar public cho frontend chi theo actor/aggregate: current account self, customer, employee; khong expose endpoint `user-profiles/{userProfileId}/avatar`.
5. Lam parking event images ngay sau do vi database da co cot va nghiep vu bai xe can bang chung.
6. Truoc khi mo private image cho nhieu role, can dong bo permission enforcement cho cac API con thieu guard nhu price plan/rule, parking topology va support-ticket-category, dong thoi dong bo schema snapshot parking voi migration check-flow.

Thiet ke nay giu dung Clean Architecture: controller mong, application quyet dinh use case/permission/ownership, domain giu rule, infrastructure moi biet MinIO.
