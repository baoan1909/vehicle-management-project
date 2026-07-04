# Review trien khai MinIO trong vehicle-management

Ngay review goc: 2026-06-16
Cap nhat trang thai: 2026-06-21

## 1. Muc dich

Tai lieu nay review phan MinIO da duoc dua vao project `vehicle-management`, tu cau hinh, dependency, kien truc port/adapter, xu ly anh, den cac luong avatar da ap dung.

Tai lieu nay khong phai roadmap chi tiet. Muc tieu la giup PM/BA/Senior review nhanh:

- MinIO dang duoc cau hinh o dau.
- Code dang di qua layer nao.
- Bang/cot nao dang luu object key.
- API nao da ap dung.
- Permission/role nao dang bao ve.
- Diem nao tot, diem nao can hardening o version tiep theo.

## 2. Ket luan nhanh

Phan MinIO hien tai di dung huong Clean Architecture:

- Controller khong goi MinIO SDK truc tiep.
- Application lam viec voi `FileStoragePort`, `FileAccessPort` va use case avatar dung chung.
- Infrastructure moi biet `MinioClient`.
- DB khong luu presigned URL; avatar object key va metadata hien luu trong `people.user_profile_avatars`.
- Response avatar co the resolve object key thanh public URL neu cau hinh `MINIO_PUBLIC_URL_BASE`.

Phan avatar da ap dung kha day du:

- Current user tu upload/delete avatar.
- Manager/admin upload/delete avatar customer theo `customerId`.
- Manager/admin upload/delete avatar employee theo `employeeId`.
- Khong expose public API upload/delete avatar theo `userProfileId`; `UserProfileAvatarPortIn` chi la use case noi bo.
- Raw `avatarUrl` string trong request profile da bi ignore/deprecate ve hanh vi write.
- Self avatar, customer avatar va employee avatar deu reuse `UserProfileAvatarPortIn`.
- `people.user_profiles.avatar_url` da drop khoi schema snapshot/JPA entity; API response van giu `avatarUrl`.

Diem can review tiep:

- Self avatar hien chi yeu cau authenticated va profile ready; neu muon chat hon, nen them/require `USER_PROFILE_UPDATE_OWN`.
- Chua co cleanup retry/outbox khi xoa object cu that bai.
- Chua ap dung parking event images, lost card evidence, support ticket attachments.

## 3. Dependency va cau hinh

### 3.1 Dependency

File: `pom.xml`

Da co:

- `io.minio:minio:8.5.17`
- `com.drewnoakes:metadata-extractor:2.19.0`

Y nghia:

- `minio` cung cap SDK upload/delete/presign/stat bucket/object.
- `metadata-extractor` doc EXIF orientation cho JPEG truoc khi resize.

### 3.2 Application config

File: `src/main/resources/application.yaml`

Config prefix:

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

Review:

- Default dev local hop ly.
- `public-url-base` de trong thi public avatar response fallback ve object key.
- Production khong duoc dung default `minioadmin/minioadmin`.
- `presigned-url-expire-seconds` hien co san cho private file, nhung chua co API private image resource-level.

### 3.3 Properties va MinioClient

Files:

- `src/main/java/com/ban/vehicle_management/infrastructure/storage/MinioStorageProperties.java`
- `src/main/java/com/ban/vehicle_management/infrastructure/storage/MinioConfig.java`

`MinioStorageProperties` bind `app.storage.minio`.

`MinioConfig` tao bean:

```text
MinioClient.builder()
  .endpoint(endpoint)
  .credentials(accessKey, secretKey)
  .build()
```

Review:

- Dung vi MinIO SDK nam o infrastructure.
- Chua co health check MinIO rieng; co the them sau neu can observability.

## 4. Kien truc hien tai

### 4.1 Application storage contracts

Files:

- `application/storage/port/out/FileStoragePort.java`
- `application/storage/port/out/FileAccessPort.java`
- `application/storage/model/StoreFileCommand.java`
- `application/storage/model/StoredFile.java`

`FileStoragePort`:

- `store(StoreFileCommand command)`
- `delete(String objectKey)`
- `exists(String objectKey)`

`FileAccessPort`:

- `createPublicUrl(String objectKey)`
- `createReadUrl(String objectKey, int expireSeconds)`
- `createReadUrls(Set<String> objectKeys, int expireSeconds)`

Review:

- Application khong phu thuoc MinIO SDK.
- Contract da du cho upload/delete/avatar va private read URL tuong lai.
- Chua co write presigned/direct upload port; chua can cho phase avatar.

### 4.2 Infrastructure adapter

File:

- `infrastructure/storage/MinioFileStorageAdapter.java`

Class nay implement ca:

- `FileStoragePort`
- `FileAccessPort`

Trach nhiem:

- Validate `StoreFileCommand`.
- Xu ly anh qua `ImageFileProcessor`.
- Generate object key qua `StorageObjectKeyGenerator`.
- Tao bucket neu chua co.
- Set public-read policy cho public bucket khi bucket moi duoc tao.
- Upload object voi metadata.
- Delete object.
- Check object exists.
- Tao public URL neu object public va co `public-url-base`.
- Tao presigned GET URL neu can read URL.

Review:

- Dung Clean Architecture: MinIO SDK chi nam trong infrastructure.
- Public/private bucket duoc chon qua `StorageBucket`.
- Bucket duoc resolve tu object key prefix `pb`/`pv`.
- Public bucket policy auto public-read la tien cho avatar, nhung can confirm policy production: chi avatar/public content moi vao bucket public.

## 5. Object key va enum storage

Files:

- `shared/enumeration/storage/StorageBucket.java`
- `shared/enumeration/storage/StorageFolder.java`
- `infrastructure/storage/StorageObjectKeyGenerator.java`

Bucket enum:

| Enum | Prefix object key | Muc dich |
| --- | --- | --- |
| `PUBLIC` | `pb` | Avatar/public content |
| `PRIVATE` | `pv` | PII/private evidence |

Folder enum:

| Enum | Path segment | File role |
| --- | --- | --- |
| `AVATAR` | `av` | `avatar` |
| `PARKING_EVENT` | `pe` | `parking-event` |
| `SUPPORT_TICKET` | `st` | `support-ticket` |
| `LOST_CARD_REPORT` | `lcr` | `lost-card-report` |

Object key format hien tai:

```text
{folder}/{yyyy}/{MM}/{dd}/{resourceId}/{bucketPrefix}-{uuid}-{fileRole}.{ext}
```

Vi du:

```text
av/2026/06/16/0f6.../pb-9cc...-avatar.jpg
```

Review:

- Key co UUID nen tranh overwrite.
- Folder segment ngan giup giu object key duoi 255 ky tu, phu hop voi avatar table va cac cot image path hien tai.
- Generator enforce key <= 255 ky tu.
- Ngay dang dung `LocalDate.now(ZoneOffset.UTC)`, chap nhan duoc cho object storage key.

## 6. Xu ly file anh

File:

- `infrastructure/storage/ImageFileProcessor.java`

Validation va processing:

- Reject file null/empty.
- Reject file vuot `image-max-size-bytes`.
- Chi chap nhan extension: `jpg`, `jpeg`, `png`, `webp`.
- Chi chap nhan content type: `image/jpeg`, `image/png`, `image/webp`.
- Extension va content type phai match.
- JPEG/PNG duoc decode bang `ImageIO` de chan file gia danh.
- JPEG duoc doc EXIF orientation va rotate/flip.
- Anh lon hon max width duoc resize theo `image-max-width-pixels`.
- JPEG duoc nen theo `image-jpeg-quality`.
- WebP hien validate RIFF/WEBP header, khong resize/decode sau.
- Tinh checksum SHA-256 sau khi prepare bytes.

Review:

- Tot hon viec chi check extension.
- JPEG/PNG co decode thuc su.
- WebP validation co ban; neu WebP la format quan trong, version sau nen dung library decode WebP hoac chuyen sang validate sau hon.
- Tat ca luong upload hien di qua processor nay vi `MinioFileStorageAdapter.store(...)` goi `imageFileProcessor.prepare(...)`.

## 7. URL resolver

File:

- `application/storage/service/StorageUrlResolver.java`

Hien co:

- `resolvePublicAvatarUrl(String avatarUrl)`
- `isManagedAvatarObjectKey(String objectKey)`

Logic:

- Chi resolve neu avatar la managed object key:
  - bat dau bang `av/`
  - chua `pb-`
- Neu `FileAccessPort.createPublicUrl(...)` tra URL thi response dung URL.
- Neu khong co public URL base thi giu object key.

Review:

- DB van luu object key, response moi resolve.
- Resolver hien thien ve avatar, chua general cho private parking event images.
- Ten package `application.storage.service` co the chap nhan vi day la technical helper dung chung; khong phai business use case.

## 8. Database va persistence

### 8.1 Bang avatar dang dung

File schema:

- `src/main/resources/db/vehicle_management.sql`

Bang source of truth:

```sql
people.user_profile_avatars
```

Gia tri luu:

- `object_key`: MinIO object key, khong luu public URL/presigned URL.
- `bucket`: `PUBLIC` hoac `PRIVATE`.
- `status`: `ACTIVE`, `REPLACED`, `DELETED`.
- `is_current`: chi ra avatar hien tai cua profile.
- metadata: original filename, content type, size, checksum, uploader va audit fields.

Vi du object key:

```text
av/2026/06/16/{userProfileId}/pb-{uuid}-avatar.jpg
```

`people.user_profiles.avatar_url` da bi drop khoi schema snapshot va `UserProfileEntity`; chi con trong migration lich su/backfill/drop.

### 8.2 Persistence methods

Files:

- `infrastructure/persistence/adapter/people/UserProfileAvatarPersistenceAdapter.java`
- `infrastructure/persistence/adapter/people/UserProfilePersistenceAdapter.java`
- `infrastructure/persistence/adapter/iam/AccountProfilePersistenceAdapter.java`

Methods/lop lien quan:

- `UserProfileAvatarPortOut.findCurrentByUserProfileId(...)`
- `UserProfileAvatarPortOut.findCurrentByUserProfileIds(...)`
- `UserProfileAvatarPortOut.markCurrentAsReplaced(...)`
- `UserProfileAvatarPortOut.markCurrentAsDeleted(...)`
- `UserProfileAvatarUseCaseImpl.uploadAvatar/deleteAvatar(...)`

Review:

- `saveAndFlush` duoc dung khi luu avatar current de response/transaction behavior ro hon.
- Metadata avatar da co bang rieng, chua can `storage.files` cho phase avatar.
- Chua co bang `storage.files`; day van la backlog khi file lan sang nhieu module.
- Parking event image path van can review length neu object key tuong lai dai hon 255.

## 9. API va luong da ap dung

### 9.1 Current user self avatar

Controller:

- `entrypoint/controller/iam/AccountProfileController.java`

API:

| Method | Endpoint | Muc dich |
| --- | --- | --- |
| `POST` | `/api/iam/accounts/profile/avatar` | Current user upload/thay avatar |
| `DELETE` | `/api/iam/accounts/profile/avatar` | Current user xoa avatar |

Use case:

- `application/iam/account/usecase/AccountProfileUseCaseImpl.java`
- `application/people/userprofile/usecase/UserProfileAvatarUseCaseImpl.java`

Behavior:

- Lay current account id.
- Load profile state theo account id.
- Neu account chua co `userProfileId` thi reject: profile not ready.
- Delegate upload/delete sang `UserProfileAvatarPortIn`.
- Upload vao `StorageBucket.PUBLIC`, `StorageFolder.AVATAR`.
- Ghi avatar current trong `people.user_profile_avatars`.
- Mark avatar cu `REPLACED` hoac `DELETED` va xoa object cu sau transaction commit.
- Neu DB update fail thi xoa object moi vua upload.
- Response `AccountProfileStatusResponse`, avatar co the da resolve public URL.

Permission hien tai:

- Endpoint nam sau auth global, nen yeu cau authenticated.
- Chua require explicit `USER_PROFILE_UPDATE_OWN`.

Review:

- Hanh vi dung cho self-service.
- Can chot co bat buoc `USER_PROFILE_UPDATE_OWN` khong.
- Logic upload/delete/cleanup da dung chung `UserProfileAvatarPortIn`.

### 9.2 User profile avatar internal use case

Public API theo `userProfileId` da duoc loai bo:

```text
POST /api/people/user-profiles/{userProfileId}/avatar
DELETE /api/people/user-profiles/{userProfileId}/avatar
```

Ly do:

- UI quan tri thao tac theo aggregate `customerId` hoac `employeeId`.
- `userProfileId` la chi tiet noi bo cua shared profile, khong nen bat frontend truyen khi dang o man customer/employee.
- Giam be mat API va tranh bypass guard theo aggregate.

Use cases noi bo:

- `UserProfileAvatarPortIn`
- `UserProfileAvatarUseCaseImpl`

Review:

- `UserProfileAvatarPortIn` van duoc giu de customer/employee/self-avatar reuse logic upload/delete/cleanup/resolve URL.
- `UserProfileUseCaseImpl` chi con phu trach CRUD/read user profile, khong expose avatar write theo `userProfileId`.

### 9.3 Customer avatar aggregate API

Controller:

- `entrypoint/controller/people/CustomerController.java`

API:

| Method | Endpoint | Muc dich |
| --- | --- | --- |
| `POST` | `/api/people/customers/{customerId}/avatar` | Upload/thay avatar customer |
| `DELETE` | `/api/people/customers/{customerId}/avatar` | Xoa avatar customer |

Use case:

- `CustomerAdminProfileUseCaseImpl`

Flow:

```text
customerId
-> CustomerPortOut.findById(customerId)
-> customer.userProfileId
-> UserProfileAvatarPortIn.uploadAvatar/deleteAvatar
-> write people.user_profile_avatars current avatar
-> return CustomerAdminProfileResponse
```

Permission:

- Controller: `@PreAuthorize(CUSTOMER_UPDATE_ALL)`
- Application: `currentAccountPortIn.requirePermission(CUSTOMER_UPDATE_ALL)`

Review:

- Day la API dung cho UI customer detail.
- Frontend khong can biet `userProfileId`.
- Security hai lop controller + application la tot.

### 9.4 Employee avatar aggregate API

Controller:

- `entrypoint/controller/people/EmployeeController.java`

API:

| Method | Endpoint | Muc dich |
| --- | --- | --- |
| `POST` | `/api/people/employees/{employeeId}/avatar` | Upload/thay avatar employee |
| `DELETE` | `/api/people/employees/{employeeId}/avatar` | Xoa avatar employee |

Use case:

- `EmployeeUseCaseImpl`

Flow:

```text
employeeId
-> EmployeePortOut.findById(employeeId)
-> EmployeeAccessGuard.ensureCanManage(employee)
-> employee.userProfileId
-> UserProfileAvatarPortIn.uploadAvatar/deleteAvatar
-> write people.user_profile_avatars current avatar
-> return EmployeeAdminResponse
```

Permission:

- Controller: `@PreAuthorize(EMPLOYEE_UPDATE_ALL)` cho upload/delete avatar.
- Application require `EMPLOYEE_UPDATE_ALL`.
- Application goi read path nen cung require `EMPLOYEE_READ_ALL` qua `getEmployeeById(...)`.
- `EmployeeAccessGuard` gioi han manager chi quan ly target phu hop.

Review:

- Hanh vi dung va style da dong bo voi `CustomerController` cho hai endpoint avatar.
- Cac endpoint employee khac van co permission application-layer trong use case.

## 10. Raw avatarUrl string trong request

Muc tieu da chot:

- Write request khong nen tiep tuc set `avatarUrl` string.
- Avatar phai di qua multipart upload/delete endpoint de validate file va cleanup MinIO.
- Response van giu `avatarUrl` de frontend hien thi.

Da ap dung:

| File | Hanh vi |
| --- | --- |
| `AccountProfileApiMapper` | Ignore `avatarUrl` khi map complete/update request |
| `AccountProfilePolicy` | Normalize command avatar ve `null`; patch-only avatar khong duoc tinh la field update |
| `AccountProfileResultMapper` | Merge profile giu avatar tu state hien co |
| `UserProfileApiMapper` | Ignore `avatarUrl` trong create/update user profile |
| `CustomerAdminProfileApiMapper` | Ignore `avatarUrl` trong update customer profile |
| `UserProfileUseCaseImpl` | Create set avatar null; update khong ghi avatar tu request |
| `CustomerAdminProfileUseCaseImpl` | Update profile khong ghi avatar tu request |

Review:

- Dung huong chong client submit URL tuy y.
- DTO request van con field `avatarUrl` de backward compatibility; version sau co the remove/deprecate trong API docs.

## 11. Transaction va cleanup

Da co:

- Upload object moi truoc.
- Update DB sau.
- Neu update DB throw exception thi xoa object moi.
- Neu update DB thanh cong thi dang ky xoa avatar cu `afterCommit`.
- Neu khong co transaction synchronization active thi xoa ngay.
- Delete old object fail thi log warn, khong rollback DB.

Review:

- Dung thuc te cho phase 1.
- Con rui ro orphan file neu xoa old object fail.
- Version sau nen co cleanup retry job/outbox neu file storage tro thanh critical.

## 12. Security va privacy

### 12.1 Public/private split

Avatar hien luu public bucket:

- Bucket: `StorageBucket.PUBLIC`
- Folder: `StorageFolder.AVATAR`

MinIO adapter se set public-read policy khi public bucket moi duoc tao.

Review:

- Chap nhan duoc cho avatar neu business xem avatar la public/semi-public.
- Khong duoc dung public bucket cho anh bien so, anh nguoi, lost card evidence, support ticket evidence.

### 12.2 Private file support

Da co nen tang:

- `StorageBucket.PRIVATE`
- `StorageFolder.PARKING_EVENT`
- `FileAccessPort.createReadUrl(...)`
- `FileAccessPort.createReadUrls(...)`

Chua co:

- API resource-level lay presigned URL cho parking event image.
- Permission/ownership guard cho private image.
- Audit log truy cap private image.

Review:

- Foundation san sang.
- Khong nen expose endpoint presign by objectKey thuan tuy.
- Private URL phai resolve theo resource id + permission, vi object key co the bi doan/log.

## 13. Test coverage

Da co tests lien quan:

- `StorageObjectKeyGeneratorTest`
- `ImageFileProcessorTest`
- `MinioFileStorageAdapterTest`
- `AccountProfileAvatarUseCaseImplTest`
- `UserProfileAvatarUseCaseImplTest`
- `CustomerAdminProfileUseCaseImplTest`
- `EmployeeUseCaseImplTest`
- `UserProfileAvatarPersistenceAdapterTest`
- Tests policy/mapper account profile lien quan raw `avatarUrl`

Lan cap nhat tai lieu 2026-06-21 khong chay lai full test suite. Trang thai verify gan nhat duoc ghi trong `minio-phased-implementation-plan.md`:

```text
Full test suite sau khi local DB da drop people.user_profiles.avatar_url: 536 pass.
```

Review:

- Unit/application tests da bao ve luong avatar va storage foundation.
- Chua co Testcontainers MinIO integration test that.
- Chua co controller multipart tests cho avatar.

## 14. Diem tot dang co

1. Dung boundary Clean Architecture.

   MinIO SDK chi nam trong infrastructure. Application goi port/use case.

2. DB luu object key, khong luu URL tam thoi.

   Day la quyet dinh dung vi presigned URL co TTL va khong nen persist.

3. Object key ngan va co UUID.

   Giam rui ro vuot `VARCHAR(255)` va tranh overwrite file.

4. Upload co metadata.

   Metadata co resource type, resource id, checksum, content type, size, original filename, owner account.

5. Co cleanup khi DB fail.

   Tranh file rac trong case upload thanh cong nhung DB update loi.

6. Xoa old avatar sau commit.

   Tranh mat avatar cu neu transaction DB rollback.

7. API aggregate cho customer/employee da dung hon UI.

   Frontend thao tac theo customer/employee, khong can biet bang shared `user_profiles`.

8. Raw `avatarUrl` string da bi chan tren write path.

   Giam rui ro client gan URL tuy y, bypass validation/upload.

## 15. Diem can review va de xuat tiep theo

### 15.1 Chot permission self avatar

Hien trang:

- Authenticated current account co profile ready la upload/delete duoc.

De xuat:

- Neu dung DB permission nghiem ngat: require `USER_PROFILE_UPDATE_OWN`.
- Neu onboarding/profile self-service duoc xem la capability mac dinh cua account authenticated: giu nhu hien tai, nhung document ro.

### 15.2 Chot length database

Hien trang:

- Avatar da dung bang `people.user_profile_avatars`.
- Object key generator enforce 255.
- Parking event image path columns hien van `VARCHAR(255)` theo migration.

De xuat:

- Khong can xu ly `avatar_url` vi column cu da drop.
- Neu mo rong folder/metadata/key format parking images: migration len `VARCHAR(500)` cho:
  - `parking.parking_events.image_path`
  - `parking.parking_events.license_plate_image_path`
  - `parking.parking_events.person_image_path`

### 15.3 Them cleanup retry/outbox

Hien trang:

- Delete old object fail thi log warn.

De xuat:

- Khi storage tro thanh quan trong, them scheduled cleanup:
  - scan orphan object theo prefix/resource metadata, hoac
  - tao outbox task khi delete fail.

### 15.4 Private image API khong duoc expose objectKey truc tiep

Khi lam parking event images:

- Upload vao `StorageBucket.PRIVATE`.
- Endpoint get URL phai nhan `parkingEventId`, khong nhan raw object key.
- Use case load event, check permission/ownership, roi moi presign URL.
- TTL ngan 5-15 phut.
- Khong log presigned URL.

### 15.5 Audit log

Nen audit:

- Admin/manager upload/delete avatar customer.
- Admin/manager upload/delete avatar employee.
- Upload/delete private parking event image.
- Get presigned URL private image neu yeu cau trace.

Khong log:

- Secret/access key.
- Full presigned URL.
- Noi dung file.

## 16. Checklist review cho PM/BA/Senior

### Product/BA

- Avatar co bat buoc trong onboarding khong? Hien tai: khong bat buoc.
- Avatar self-service co can approval khong? Hien tai: khong.
- Parking manager co duoc sua avatar customer khong? Hien tai: co qua `CUSTOMER_UPDATE_ALL`.
- Parking manager co duoc sua avatar employee nao? Hien tai: co qua `EMPLOYEE_UPDATE_ALL` va `EmployeeAccessGuard`.
- System admin co duoc sua avatar parking manager khac khong? Can chot permission/hierarchy.
- Customer/employee co duoc xem avatar nguoi khac khong? Can chot theo response UI.

### Security

- Production da doi `MINIO_ACCESS_KEY`/`MINIO_SECRET_KEY` chua?
- Public bucket chi dung avatar/public content chua?
- Co can require `USER_PROFILE_UPDATE_OWN` cho self avatar khong?
- Co audit log cho admin/manager avatar change khong?
- Private file tuong lai co resource-level permission chua?

### Backend

- Co can Testcontainers MinIO integration test khong?
- Co can controller multipart tests khong?
- Co can migration `VARCHAR(500)` cho parking event image path khong?
- Co can cleanup retry/outbox khong?
- Co can generalize `StorageUrlResolver` cho private/public resource khac khong?

## 17. Trang thai ap dung

### Da ap dung

- Dependency MinIO va metadata extractor.
- Config `app.storage.minio`.
- `MinioClient` bean.
- `FileStoragePort`, `FileAccessPort`.
- `StoreFileCommand`, `StoredFile`.
- `StorageBucket`, `StorageFolder`.
- `StorageObjectKeyGenerator`.
- `ImageFileProcessor`.
- `MinioFileStorageAdapter`.
- `StorageUrlResolver` cho public avatar.
- Bang `people.user_profile_avatars` va migration/backfill/drop `avatar_url`.
- `UserProfileAvatarPortOut`, entity/repository/mapper/persistence adapter cho avatar table.
- Upload/delete self avatar.
- Upload/delete customer avatar theo `customerId`.
- Upload/delete employee avatar theo `employeeId`.
- Self/customer/employee avatar dung chung `UserProfileAvatarPortIn`.
- Account register/provisioned account tao `people.user_profiles` toi thieu voi `fullName`.
- Ignore/deprecate raw `avatarUrl` write input.
- Tests avatar/storage lien quan.

### Chua ap dung

- Parking event images.
- Lost card report evidence.
- Support ticket attachments.
- File metadata table `storage.files`.
- Direct upload/presigned PUT.
- Cleanup retry/outbox.
- Audit log cho file operations.
- Testcontainers MinIO.
- Controller multipart tests.
- Dong bo schema snapshot parking voi migration V5/V6/V7/V8 cho check-flow/private images.

## 18. Ket luan review

MinIO foundation hien tai du de dung cho avatar production phase dau neu environment MinIO duoc cau hinh dung va public bucket policy duoc chap nhan cho avatar.

Huong thiet ke tong the tot: storage la infrastructure, application lam viec qua port, avatar write di qua multipart endpoint, DB luu object key trong `people.user_profile_avatars`. Phan nen uu tien tiep theo khong phai viet lai MinIO, ma la hardening:

1. Chot policy `USER_PROFILE_UPDATE_OWN` cho self avatar.
2. Them audit/cleanup neu avatar admin la thao tac nhay cam.
3. Khi mo private images, bat buoc resource-level authorization va presigned URL TTL ngan.
4. Dong bo schema snapshot parking voi migration check-flow truoc khi map `license_plate_image_path` va `person_image_path`.
