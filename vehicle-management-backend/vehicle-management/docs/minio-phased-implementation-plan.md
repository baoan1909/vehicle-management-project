# Ke hoach trien khai MinIO theo phase

Ngay lap ke hoach: 2026-06-16
Cap nhat trang thai: 2026-06-21

## 1. Muc tieu

Ke hoach nay thay the cach nhin "lam MinIO mot lan cho xong" bang lo trinh tung phase ro rang. Moi phase co muc tieu, pham vi, thay doi database, thay doi code, API, test va dieu kien hoan thanh.

Muc tieu dai han:

- Avatar khong con la string `avatar_url` nam truc tiep trong `people.user_profiles`.
- Avatar duoc quan ly bang bang rieng `people.user_profile_avatars`.
- DB chi luu MinIO object key, khong luu public URL hay presigned URL.
- API response van co `avatarUrl` de frontend hien thi.
- File private nhu anh bien so, anh nguoi, lost card evidence se di qua private bucket va resource-level permission.

Nguyen tac chinh:

- Khong dung `external_url` trong phase hien tai.
- Khong nhan `avatarUrl` string trong write request.
- Upload/delete avatar phai di qua multipart endpoint.
- `people.user_profiles.avatar_url` da duoc drop; cac mo ta dual-write/read ben duoi duoc giu nhu lich su trien khai va truy vet migration.
- Khong tao `storage.files` qua som; chi them khi file mo rong ra nhieu module va can metadata dung chung.
- Trong qua trinh thuc hien tung phase, chi cap nhat file ke hoach nay de ghi trang thai va viec da lam/chua lam.
- Chi cap nhat file review MinIO sau khi hoan thanh toan bo `minio-phased-implementation-plan.md`, hoac khi co yeu cau review rieng ro rang.

## 2. Hien trang hien tai

Da co:

- Dependency `io.minio:minio`.
- Dependency `metadata-extractor`.
- Config `app.storage.minio`.
- `MinioClient` bean.
- `MinioStorageProperties`.
- `FileStoragePort`, `FileAccessPort`.
- `StoreFileCommand`, `StoredFile`.
- `StorageBucket`, `StorageFolder`.
- `StorageObjectKeyGenerator`.
- `ImageFileProcessor`.
- `MinioFileStorageAdapter`.
- `StorageUrlResolver`.
- API self avatar:
  - `POST /api/iam/accounts/profile/avatar`
  - `DELETE /api/iam/accounts/profile/avatar`
- Self avatar cua `iam.account` da delegate qua `UserProfileAvatarPortIn`.
- API aggregate avatar:
  - `POST /api/people/customers/{customerId}/avatar`
  - `DELETE /api/people/customers/{customerId}/avatar`
  - `POST /api/people/employees/{employeeId}/avatar`
  - `DELETE /api/people/employees/{employeeId}/avatar`
- `UserProfileAvatarPortIn` va `UserProfileAvatarUseCaseImpl`.
- `UserProfileAvatarPortIn` la use case noi bo, khong expose public API theo `userProfileId`.
- Raw `avatarUrl` string trong account/profile/customer update request da bi ignore tren write path.
- Employee avatar endpoints co controller-level `@PreAuthorize('EMPLOYEE_UPDATE_ALL')` va van giu usecase-level permission.
- Migration `V14__create_people_user_profile_avatars.sql`.
- Migration `V15__backfill_people_user_profile_avatars.sql`.
- Migration `V16__drop_people_user_profiles_avatar_url.sql`.
- Bang `people.user_profile_avatars` cho avatar metadata va lifecycle.
- `UserProfileAvatarPortOut`, `UserProfileAvatarPersistenceAdapter`, entity/repository/mapper cho bang avatar moi.
- Upload/delete avatar chi ghi `people.user_profile_avatars`; khong con ghi `people.user_profiles.avatar_url`.
- Backfill avatar MinIO object key cu tu `people.user_profiles.avatar_url` sang `people.user_profile_avatars`.
- `people.user_profiles.avatar_url` da duoc drop khoi schema snapshot va JPA entity.

Da xong them:

- Read path da doc current avatar tu `people.user_profile_avatars`; khong con doc fallback tu `people.user_profiles.avatar_url`.
- Self/customer/employee avatar deu di qua `UserProfileAvatarPortIn`.
- Account tao moi qua public register/provisioned account da co `people.user_profiles` toi thieu voi `fullName`.

Con lai:

- Chua co parking event images qua MinIO.
- Chua co cleanup retry/outbox.
- Chua co audit log cho file operations.
- Chua dong bo schema snapshot parking voi migration V5/V6/V7/V8; `vehicle_management.sql` van con drift voi entity/migration check-flow.

Luu y doc tai lieu:

- Phase 0-6 ben duoi la lich su da thuc hien cho avatar/account profile.
- Cac dong noi ve dual-write, fallback `avatar_url`, va drop column cu khong con la viec can lam tiep.
- Viec can lam tiep lien quan parking la dong bo schema snapshot voi V5/V6/V7/V8 truoc, sau do moi them private images va upload trong check-in/check-out.

## 3. Quyet dinh kien truc da chot

### 3.1 Avatar thuoc user profile

Ten bang dung:

```text
people.user_profile_avatars
```

Khong dung:

```text
people.user_avatars
```

Ly do:

- He thong tach `iam.accounts` va `people.user_profiles`.
- Avatar la thuoc tinh ho so con nguoi, khong phai thuoc tinh dang nhap.
- Customer, employee, manager, admin deu dung chung `people.user_profiles`.

### 3.2 Khong dung external_url trong phase hien tai

Khong them:

```sql
external_url VARCHAR(500)
```

Ly do:

- Avatar chi den tu backend upload vao MinIO.
- Khong muon client bypass validation bang URL ngoai.
- Khong can check constraint phuc tap `object_key OR external_url`.
- Response URL se duoc resolve tu `object_key`.

Neu sau nay can OAuth/social avatar, them migration rieng:

```sql
ALTER TABLE people.user_profile_avatars
ADD COLUMN external_url VARCHAR(500);
```

### 3.3 Response contract khong doi som

Du co bo column `people.user_profiles.avatar_url` ve sau, response van giu:

```json
{
  "avatarUrl": "https://cdn.example.com/av/..."
}
```

Nghia la `avatarUrl` la API response field, khong bat buoc trung voi ten column DB.

## 4. Phase 0 - Dong bang hien trang va hardening nho

Trang thai: Da thuc hien.

### Muc tieu

On dinh code MinIO/avatar hien co truoc khi doi database.

### Pham vi

Lam truoc tat ca phase database moi.

### Viec can lam

1. Refactor self avatar ve dung chung `UserProfileAvatarPortIn`. Da lam.

   Hien tai:

   ```text
   AccountProfileUseCaseImpl -> FileStoragePort
   ```

   Sau refactor:

   ```text
   AccountProfileUseCaseImpl
   -> load current AccountProfileState
   -> check state.userProfileId != null
   -> UserProfileAvatarPortIn.uploadAvatar/deleteAvatar
   -> reload AccountProfileState
   -> return AccountProfileStatusResult
   ```

   Ket qua:

   ```text
   AccountProfileUseCaseImpl
   -> load current AccountProfileState
   -> check state.userProfileId != null
   -> UserProfileAvatarPortIn.uploadAvatar/deleteAvatar
   -> reload AccountProfileState
   -> return AccountProfileStatusResult
   ```

2. Giu message nghiep vu khi account chua onboarding:

   ```text
   Profile is not ready. Complete onboarding first.
   ```

3. Dong bo permission style cho employee endpoints. Da lam cho avatar endpoints.

   Da them controller-level annotation:

   ```java
   @PreAuthorize("@permissionAuthorizer.hasPermission('EMPLOYEE_UPDATE_ALL')")
   ```

   cho:

   - `POST /api/people/employees/{employeeId}/avatar`
   - `DELETE /api/people/employees/{employeeId}/avatar`

4. Chot self avatar permission.

   Co 2 option:

   - Option A: authenticated + profile ready la du.
   - Option B: require `USER_PROFILE_UPDATE_OWN`.

   De xuat senior: neu DB permission seed da on dinh, dung Option B.

   Trang thai hien tai: chua doi API self avatar sang permission rieng; self avatar van dua tren authenticated current account + profile ready. Nen tach thanh change rieng neu seed permission `USER_PROFILE_UPDATE_OWN` da on dinh.

### API thay doi

Khong doi API.

### DB thay doi

Khong co.

### Test can co

- `AccountProfileAvatarUseCaseImplTest` cap nhat de verify delegate sang `UserProfileAvatarPortIn`.
- `UserProfileAvatarUseCaseImplTest` giu cleanup behavior.
- Full `mvnw test` pass.

### Dieu kien hoan thanh

- Khong con duplicate upload/delete avatar logic giua `AccountProfileUseCaseImpl` va `UserProfileAvatarUseCaseImpl`.
- API response khong doi.
- Test pass.

### Da verify

- Targeted tests: 31 pass.
- Full test suite: 528 pass.

## 5. Phase 1 - Tao bang `people.user_profile_avatars` va dual-write

Trang thai: Da thuc hien.

### Muc tieu

Them bang avatar rieng nhung chua pha code/response cu.

Trong phase nay:

- Bang moi bat dau ghi metadata avatar.
- `people.user_profiles.avatar_url` van duoc giu de backward compatibility.
- Read response van co the lay tu `avatar_url` cu neu bang moi chua day du.

### DB migration

Tao enum bang check constraint, khong can PostgreSQL enum type de de migration hon.

```sql
CREATE TABLE people.user_profile_avatars (
    avatar_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id UUID NOT NULL
        REFERENCES people.user_profiles(user_profile_id) ON DELETE RESTRICT,

    object_key VARCHAR(255) NOT NULL,

    original_filename VARCHAR(255),
    content_type VARCHAR(100),
    size_bytes BIGINT,
    checksum_sha256 VARCHAR(64),

    bucket VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,

    uploaded_by_account_id UUID
        REFERENCES iam.accounts(account_id) ON DELETE SET NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES iam.accounts(account_id) ON DELETE SET NULL,

    CONSTRAINT ck_user_profile_avatars_bucket
        CHECK (bucket IN ('PUBLIC', 'PRIVATE')),

    CONSTRAINT ck_user_profile_avatars_status
        CHECK (status IN ('ACTIVE', 'REPLACED', 'DELETED')),

    CONSTRAINT ck_user_profile_avatars_current_active
        CHECK (is_current = false OR status = 'ACTIVE'),

    CONSTRAINT ck_user_profile_avatars_size_non_negative
        CHECK (size_bytes IS NULL OR size_bytes >= 0)
);
```

Partial unique index:

```sql
CREATE UNIQUE INDEX uq_user_profile_current_avatar
ON people.user_profile_avatars(user_profile_id)
WHERE is_current = true;
```

Indexes nen co:

```sql
CREATE INDEX idx_user_profile_avatars_profile
ON people.user_profile_avatars(user_profile_id);

CREATE INDEX idx_user_profile_avatars_object_key
ON people.user_profile_avatars(object_key);

CREATE INDEX idx_user_profile_avatars_uploaded_by
ON people.user_profile_avatars(uploaded_by_account_id);
```

Trigger `updated_at`:

```sql
CREATE TRIGGER trg_user_profile_avatars_set_updated_at
BEFORE UPDATE ON people.user_profile_avatars
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
```

### Vi sao chua drop `avatar_url`

Khong drop ngay vi:

- API/profile response hien co dang doc field nay.
- Can backfill du lieu cu.
- Can deploy an toan qua it nhat 1 release.
- Neu rollback code, column cu van con.

### Code can them

Domain/model:

```text
domain.people.userprofile.model.UserProfileAvatar
```

Enum:

```text
shared.enumeration.people.UserProfileAvatarStatus
ACTIVE, REPLACED, DELETED
```

Application port out:

```text
application.people.userprofile.port.out.UserProfileAvatarPortOut
```

Methods de xuat:

```java
Optional<UserProfileAvatar> findCurrentByUserProfileId(UUID userProfileId);

UserProfileAvatar save(UserProfileAvatar avatar);

void markCurrentAsReplaced(UUID userProfileId);

void markCurrentAsDeleted(UUID userProfileId);
```

Infrastructure:

```text
UserProfileAvatarEntity
UserProfileAvatarRepository
UserProfileAvatarPersistenceMapper
UserProfileAvatarPersistenceAdapter
```

Da them cac file tren va migration:

```text
src/main/resources/db/migration/V14__create_people_user_profile_avatars.sql
```

### Upload flow phase 1

```text
1. Validate profile ton tai.
2. Upload file MinIO.
3. Trong transaction:
   - mark current avatar cu REPLACED, is_current=false
   - insert avatar moi ACTIVE, is_current=true
   - update people.user_profiles.avatar_url = object_key moi
4. Sau commit:
   - co the xoa object cu neu policy la physical cleanup
5. Neu DB fail:
   - xoa object moi vua upload
```

### Delete flow phase 1

```text
1. Load current avatar.
2. Trong transaction:
   - mark current avatar DELETED, is_current=false
   - update people.user_profiles.avatar_url = null
3. Sau commit:
   - xoa object cu neu policy la physical cleanup
```

### API thay doi

Khong doi API.

Van giu:

- `POST /api/iam/accounts/profile/avatar`
- `DELETE /api/iam/accounts/profile/avatar`
- `POST /api/people/customers/{customerId}/avatar`
- `DELETE /api/people/customers/{customerId}/avatar`
- `POST /api/people/employees/{employeeId}/avatar`
- `DELETE /api/people/employees/{employeeId}/avatar`

POST body khong doi:

```text
multipart/form-data
file=<image file>
```

Response khong doi:

- Self avatar: `AccountProfileStatusResponse`
- User profile avatar: `UserProfileAdminResponse`
- Customer avatar: `CustomerAdminProfileResponse`
- Employee avatar: `EmployeeAdminResponse`

### Test can co

- Upload avatar insert row moi `ACTIVE/is_current=true`.
- Upload avatar replace row cu thanh `REPLACED/is_current=false`.
- Unique current avatar theo user profile.
- Delete avatar mark row `DELETED/is_current=false`.
- DB fail thi xoa object moi.
- Backward compatibility: `people.user_profiles.avatar_url` van duoc update.

### Dieu kien hoan thanh

- Bang moi co du lieu cho avatar moi.
- Column cu van duoc maintain.
- API khong doi contract.
- Full test pass.

### Da verify

- Targeted tests: 24 pass.
- Full test suite: 531 pass.
- Da apply migration `V14` vao DB local de Hibernate `ddl-auto=validate` pass.

## 6. Phase 2 - Backfill avatar cu vao bang moi

Trang thai: Da thuc hien.

### Muc tieu

Chuyen du lieu avatar cu trong `people.user_profiles.avatar_url` sang `people.user_profile_avatars`.

### Migration backfill

Backfill chi voi avatar_url co gia tri.

```sql
INSERT INTO people.user_profile_avatars (
    avatar_id,
    user_profile_id,
    object_key,
    bucket,
    status,
    is_current,
    created_at
)
SELECT
    gen_random_uuid(),
    user_profile_id,
    avatar_url,
    CASE
        WHEN avatar_url LIKE 'av/%/pb-%' OR avatar_url LIKE 'av/%/%/pb-%'
            THEN 'PUBLIC'
        ELSE 'PUBLIC'
    END,
    'ACTIVE',
    true,
    now()
FROM people.user_profiles
WHERE avatar_url IS NOT NULL
  AND avatar_url <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM people.user_profile_avatars a
      WHERE a.user_profile_id = people.user_profiles.user_profile_id
        AND a.is_current = true
  );
```

Luu y:

- Neu avatar_url cu la external URL khong do MinIO quan ly, can quyet dinh:
  - bo qua va de avatarUrl cu song trong column cu den phase cleanup, hoac
  - migrate thanh object_key se khong dung vi khong phai MinIO key.
- Vi ta da chot khong dung `external_url`, chi nen backfill object key do backend quan ly.

### Code can co

- Script/migration idempotent.
- Test migration neu project co migration test.
- Query kiem tra so luong profile co avatar_url va so avatar current.

Da them migration:

```text
src/main/resources/db/migration/V15__backfill_people_user_profile_avatars.sql
```

Migration chi backfill object key dung format backend dang tao:

```text
av/yyyy/mm/dd/{userProfileId}/pb-{uuid}-avatar.{ext}
```

External URL hoac chuoi legacy khong dung format MinIO object key se khong bi day vao `object_key`; chung van nam o `people.user_profiles.avatar_url` de Phase 3 fallback xu ly.

### Dieu kien hoan thanh

- Tat ca avatar MinIO object key cu co row current trong bang moi.
- Khong vi pham unique partial index.
- Khong tao duplicate row khi migration chay lai.

### Da verify

- Da apply migration `V15` vao DB local.
- Da chay migration `V15` hai lan de verify idempotent.
- Query DB local:
  - `candidate_count=0`
  - `current_avatar_count=0`
  - `missing_current_count=0`
- Full test suite: 531 pass.

## 7. Phase 3 - Doi read path sang bang `user_profile_avatars`

Trang thai: Da thuc hien.

### Muc tieu

Bang moi tro thanh source of truth cho avatar read.

### Code thay doi

Read response avatar se lay theo thu tu:

```text
1. current avatar tu people.user_profile_avatars
2. fallback people.user_profiles.avatar_url trong giai doan chuyen tiep
```

Sau khi on dinh co the bo fallback.

Can sua:

- `AccountProfilePersistenceAdapter` hoac query layer profile state.
- `UserProfilePersistenceAdapter` hoac application enrichment.
- `CustomerAdminProfileUseCaseImpl` response build.
- `EmployeeUseCaseImpl` response build.
- Mapper/response van giu field `avatarUrl`.

Da lam:

- Them bulk read current avatar:

```java
Map<UUID, UserProfileAvatar> findCurrentByUserProfileIds(Set<UUID> userProfileIds);
```

- `UserProfileAvatarUseCaseImpl` resolve avatar theo thu tu:

```text
1. current avatar trong people.user_profile_avatars
2. fallback people.user_profiles.avatar_url
```

- `UserProfileUseCaseImpl#getUserProfiles` dung bulk resolver de tranh N+1.
- `EmployeeUseCaseImpl#getEmployees` dung bulk resolver cho user profile cua employee.
- `CustomerAdminProfileUseCaseImpl` resolve avatar trong response build.
- `AccountProfileUseCaseImpl` resolve avatar self profile qua `UserProfileAvatarPortIn`.
- API response van giu field `avatarUrl`.
- Public technical API theo `userProfileId` khong expose; avatar write di qua self/customer/employee aggregate API.

De xuat implementation:

- Khong join phuc tap trong moi mapper neu chua can.
- Tao port query current avatar:

```java
Optional<UserProfileAvatar> findCurrentByUserProfileId(UUID userProfileId);
Map<UUID, UserProfileAvatar> findCurrentByUserProfileIds(Set<UUID> userProfileIds);
```

Can co bulk method de tranh N+1 khi list user profiles/employees/customers.

### API thay doi

Khong doi.

### Test can co

- Response avatarUrl lay tu current avatar table.
- Neu table chua co row thi fallback column cu.
- List employees/user profiles khong bi N+1 nghiem trong.
- `StorageUrlResolver` van resolve public URL tu object key.

### Dieu kien hoan thanh

- Avatar response khong phu thuoc chinh vao `people.user_profiles.avatar_url`.
- Full test pass.
- Log/monitor khong co mismatch giua column cu va bang moi.

### Da verify

- Targeted tests: 37 pass.
- Full test suite: 536 pass.

## 8. Phase 4 - Stop writing `people.user_profiles.avatar_url`

Trang thai: Da thuc hien.

### Muc tieu

Dung ghi column cu, nhung chua drop column.

### Code thay doi

Upload/delete avatar chi ghi:

```text
people.user_profile_avatars
```

Khong ghi:

```text
people.user_profiles.avatar_url
```

Read path:

```text
current avatar table only
```

Fallback column cu chi giu neu can rollback ngan han.

Trang thai thuc te: fallback column cu da bo trong code khi thuc hien chung voi Phase 5.

Da lam:

- Bo `UserProfilePortOut.updateAvatar(...)`.
- Bo `UserProfilePersistenceAdapter.updateAvatar(...)`.
- `UserProfileAvatarUseCaseImpl.uploadAvatar(...)` chi ghi bang `people.user_profile_avatars`.
- `UserProfileAvatarUseCaseImpl.deleteAvatar(...)` chi mark current avatar trong bang `people.user_profile_avatars` thanh `DELETED/is_current=false`.
- Old object cleanup lay object key tu current avatar row trong `people.user_profile_avatars`, khong lay tu `people.user_profiles.avatar_url`.
- `UserProfileAvatarUseCaseImpl` khong con fallback response tu `UserProfile.avatarUrl` khi khong co current avatar row.
- `AccountProfileUseCaseImpl` self avatar tiep tuc delegate qua `UserProfileAvatarPortIn`.

### API thay doi

Khong doi.

### Test can co

- Upload khong update `people.user_profiles.avatar_url`.
- Delete khong update `people.user_profiles.avatar_url`.
- Response van co `avatarUrl`.
- API cu van pass.

### Dieu kien hoan thanh

- Khong con production code write vao `avatar_url`.
- Metric/log cho thay response avatar dung tu bang moi.
- Co rollback plan neu can.

### Da verify

- Targeted tests lien quan avatar/account/profile: 51 pass.
- Full test suite sau khi local DB da drop `people.user_profiles.avatar_url`: 536 pass.
- Production Java code khong con goi `updateAvatar(...)`.
- Production Java persistence khong con field/map `people.user_profiles.avatar_url`.

## 9. Phase 5 - Drop column `people.user_profiles.avatar_url`

Trang thai: Da thuc hien.

### Muc tieu

Loai bo column cu sau khi da on dinh qua it nhat mot release.

### Dieu kien truoc khi drop

Chi drop khi tat ca dieu kien sau dung:

- Phase 3 da deploy on dinh.
- Phase 4 da deploy on dinh.
- Khong con code read/write `avatar_url`.
- Backfill da xong.
- Frontend khong phu thuoc DB column, chi phu thuoc API `avatarUrl`.
- Co backup DB truoc migration.

### DB migration

```sql
ALTER TABLE people.user_profiles
DROP COLUMN avatar_url;
```

Da them migration:

```text
src/main/resources/db/migration/V16__drop_people_user_profiles_avatar_url.sql
```

Noi dung:

```sql
ALTER TABLE people.user_profiles
DROP COLUMN IF EXISTS avatar_url;
```

### Code cleanup

- Xoa field `avatarUrl` khoi `UserProfileEntity` neu no chi map DB.
- Domain `UserProfile` co the:
  - giu `avatarUrl` nhu view/enriched field cho response, hoac
  - tach thanh response projection.

De xuat:

- Giu `avatarUrl` trong domain/response transition neu can hien thi.
- Khong map `avatarUrl` vao persistence entity `people.user_profiles` nua.

Da lam:

- Xoa `avatar_url` khoi `src/main/resources/db/vehicle_management.sql`.
- Xoa `avatarUrl` khoi `UserProfileEntity`.
- `UserProfilePersistenceMapper#toDomain` ignore `avatarUrl`; field nay chi con la enriched response field.
- `AccountProfilePersistenceAdapter` khong con set/get `avatarUrl` tu `UserProfileEntity`.
- `AccountProfileUseCaseImpl` resolve self avatar bang `userProfileId` qua `UserProfileAvatarPortIn`, nen van tra `avatarUrl` response dung khi column cu da mat.
- `UserProfilePolicy` khong con validate/normalize `avatarUrl` nhu field write cua user profile.

### Test can co

- Hibernate validate pass khi khong co column `avatar_url`.
- All avatar responses van dung.
- Upload/delete/list/get pass.

### Dieu kien hoan thanh

- Schema sach: user profile khong con avatar column.
- Avatar lifecycle nam trong bang rieng.

### Da verify

- Targeted tests lien quan avatar/account/profile: 51 pass.
- Full test suite sau khi local DB da drop `people.user_profiles.avatar_url`: 536 pass.
- `avatar_url` chi con trong migration lich su `V15` va migration drop `V16`; khong con trong main Java persistence/schema snapshot.

## 10. Phase 6 - Tao user profile toi thieu khi tao account

### Muc tieu

Cho phep account co `user_profile_id` ngay tu luc dang ky/duoc tao noi bo de:

- self avatar API co the upload/delete avatar truoc khi hoan tat customer/employee onboarding.
- khong can gan avatar truc tiep vao `iam.accounts`.
- khong phai tao profile gia hoac cho `people.user_profiles.full_name` nullable.
- van giu dung boundary: account la auth/account, user profile la thong tin ca nhan, customer/employee la business record.

### Quyet dinh thiet ke

Khong chon huong dua avatar sang bang `iam.accounts`.

Ly do:

- Avatar trong he thong nay gan voi nguoi dung/profile, khong phai credential/account.
- `people.user_profile_avatars.user_profile_id` da la FK dung nghiep vu.
- Neu avatar FK sang account, sau nay customer/employee/admin van phai join nguoc ve profile de hien thi, lam domain lech boundary.
- Account co the bi khoa/disable, nhung avatar/profile van la du lieu people.

Khong chon huong draft profile co `completion_status` trong phase nay.

Ly do:

- Schema hien tai da co `people.user_profiles.full_name NOT NULL`.
- Product co the yeu cau full name ngay o dang ky/account provisioning, day la thong tin hop ly va on dinh.
- Onboarding completed khong nen suy ra tu viec co user profile, ma phai suy ra tu record nghiep vu va approval.

Huong chot:

```text
Tao account
-> bat buoc co fullName
-> tao people.user_profiles toi thieu
-> gan iam.accounts.user_profile_id
-> chua tao customer/employee neu chua onboarding
-> avatar self-service co the dung user_profile_id nay
```

### Rule onboarding sau Phase 6

`userProfileId != null` khong con dong nghia voi onboarding completed.

Onboarding completed phai dua vao tung role:

| Role | Co profile sau khi tao account | Onboarding con thieu gi | Khi completeMyProfile lam gi |
| --- | --- | --- | --- |
| CUSTOMER | Co | `people.customers` + approval request | Update profile, tao customer `INACTIVE/PENDING`, tao customer onboarding approval |
| EMPLOYEE | Co | `people.employees` + approval request | Update profile, tao employee `INACTIVE`, tao internal employee approval |
| PARKING_MANAGER | Co | `people.employees` + approval request | Update profile, tao employee `INACTIVE`, tao internal employee approval |
| SYSTEM_ADMIN | Co | approval request neu account `PENDING` | Update profile, tao system admin approval neu can |

Legacy account cu neu con `userProfileId = null` van duoc support:

```text
completeMyProfile
-> neu da co userProfileId thi update profile do
-> neu chua co userProfileId thi tao profile moi
```

### API thay doi

Khong them API moi.

Them field bat buoc vao request tao account:

```http
POST /api/iam/auth/register
Content-Type: application/json
```

```json
{
  "username": "customer01",
  "email": "customer01@example.com",
  "password": "Secret123!",
  "fullName": "Nguyen Van A"
}
```

```http
POST /api/iam/accounts
Content-Type: application/json
```

```json
{
  "username": "employee01",
  "email": "employee01@example.com",
  "roleCode": "EMPLOYEE",
  "fullName": "Tran Thi B"
}
```

Response khong bat buoc doi. Cac response profile van giu `avatarUrl` la enriched response field.

Self avatar API giu nguyen:

```http
POST /api/iam/accounts/profile/avatar
DELETE /api/iam/accounts/profile/avatar
```

Sau Phase 6, account moi co the upload avatar ngay sau khi account duoc tao vi da co `userProfileId`. Guard `Profile is not ready. Complete onboarding first.` chi con la bao ve cho legacy/corrupted account khong co profile.

### Code da thuc hien

- `RegisterAccountRequest` va `RegisterAccountCommand` them `fullName`.
- `CreateProvisionedAccountRequest` va `CreateProvisionedAccountCommand` them `fullName`.
- `PublicAuthPolicy` normalize/validate `fullName` bat buoc, max 150 theo `people.user_profiles.full_name`.
- `PublicAuthUseCaseImpl` tao `UserProfile` toi thieu khi public register.
- `AccountRegistrationPortOut` va `AccountRegistrationPersistenceAdapter` luu `UserProfile` truoc, sau do luu `Account` voi `userProfileId`.
- `ProvisionedAccountUseCaseImpl` tao `userProfileId` cho account noi bo va tao `UserProfile` toi thieu tu `fullName`.
- `ProvisionedAccountPortOut` va `ProvisionedAccountPersistenceAdapter` luu profile cung luc voi provisioned account.
- `KeycloakIdentityProviderSecurityAdapter` sync `fullName` vao `firstName` va attribute `full_name` khi tao Keycloak user de email/theme co du lieu hien thi.
- `AccountProfileUseCaseImpl` doi `isOnboardingRequired` de dua vao customer/employee/approval thay vi chi dua vao `userProfileId`.
- `AccountProfileUseCaseImpl#completeMyProfile` update profile san co neu account da co `userProfileId`, va chi tao profile moi cho legacy account.
- Duplicate phone/identify card check khi complete onboarding da doi sang exclude current `userProfileId` neu profile da co san.

### Test da bo sung/cap nhat

- Public register tao profile toi thieu co `fullName` va `ACTIVE`.
- Provisioned account tao account + profile cung userProfileId.
- Keycloak create-user request body co `firstName` va `attributes.full_name`.
- Customer role co profile nhung chua co customer record van `onboardingRequired = true`.
- Customer onboarding update profile san co tu registration va tao customer/approval.
- System admin pending sau khi complete profile tao approval request va het onboarding required khi approval request da ton tai.
- Targeted test lien quan account/profile/avatar/provisioning: 35 pass.

### Con can lam o version sau

- Cap nhat API docs/OpenAPI/Postman neu co.
- Frontend khi co se phai gui `fullName` trong register/provisioning form.
- Chay data audit voi account legacy co `user_profile_id IS NULL` neu production da co du lieu cu.
- Can co migration/backfill rieng neu muon enforce `iam.accounts.user_profile_id NOT NULL` cho database hien huu.

## 11. Phase 7 - Parking event images private

### Muc tieu

Dua anh bien so va anh nguoi/lai xe cua parking event vao MinIO private bucket.

### DB hien co

Da co hoac du kien co:

- `parking.parking_events.license_plate_image_path`
- `parking.parking_events.person_image_path`

### Storage rule

- Bucket: `PRIVATE`
- Folder: `PARKING_EVENT`
- DB luu object key.
- Response khong tra object key public.
- View image qua presigned URL co TTL ngan.

### API de xuat

Upload/thay image:

```http
POST /api/parking/events/{parkingEventId}/images
Content-Type: multipart/form-data
```

Parts:

```text
licensePlateImage=<file optional>
personImage=<file optional>
overviewImage=<file optional>
```

Lay URL xem anh:

```http
GET /api/parking/events/{parkingEventId}/images
```

Response:

```json
{
  "parkingEventId": "...",
  "expiresInSeconds": 900,
  "licensePlateImageUrl": "...",
  "personImageUrl": "...",
  "overviewImageUrl": "..."
}
```

Delete mot image:

```http
DELETE /api/parking/events/{parkingEventId}/images/{imageType}
```

`imageType`:

```text
LICENSE_PLATE
PERSON
OVERVIEW
```

### Permission

- Upload/delete: `PARKING_EVENT_UPDATE_ALL`.
- Read all: `PARKING_EVENT_READ_ALL`.
- Read own: `PARKING_EVENT_READ_OWN` neu event thuoc customer hien tai.

### Bat buoc security

- Khong presign theo raw object key tu request.
- Luon load parking event theo id.
- Luon check permission/ownership.
- TTL 5-15 phut.
- Khong log presigned URL.

### Test can co

- Employee/manager upload image thanh cong.
- Customer xem own image thanh cong neu business cho.
- Customer khong xem image cua nguoi khac.
- Private object tra presigned URL.
- DB fail thi cleanup object moi.

## 12. Phase 8 - Check-in/check-out tich hop camera image

### Muc tieu

Khi check-in/check-out API duoc implement, upload anh ngay trong luong nghiep vu.

### Check-in API tuong lai

```http
POST /api/parking/parking-sessions/check-in
Content-Type: multipart/form-data
```

Parts:

```text
request=<json>
licensePlateImage=<file>
personImage=<file>
```

Use case:

```text
validate lane/gate/zone/card/customer
-> create parking session OPEN
-> generate parking event id
-> upload images private with resourceId=parkingEventId
-> create parking event CHECK_IN with object keys
```

Ghi chu:

- Check-in co anh dung mot luong duy nhat la multipart.
- Khong dung JSON `imagePath` tu client cho check-in MinIO.
- Permission command cua luong nay la `PARKING_SESSION_CHECK_IN_ALL`; `CHECK_IN` event la side effect cua use case check-in.
- Phase check-in hien tai nhan ca `licensePlateImage` va `personImage`.
- `licensePlateImage` luu vao `parking_events.license_plate_image_path`.
- `personImage` luu vao `parking_events.person_image_path`.
- Cot cu `parking_events.image_path` khong con duoc dung.

### Check-out API tuong lai

```http
POST /api/parking/sessions/{parkingSessionId}/check-out
Content-Type: multipart/form-data
```

Parts:

```text
request=<json>
licensePlateImage=<file optional>
personImage=<file optional>
```

Use case:

```text
validate session OPEN
-> calculate fee
-> create parking event CHECK_OUT
-> upload images private
-> close session neu hop le
```

### Dieu kien truoc khi lam

- Parking session/check flow domain da chot.
- Parking event images phase 7 da on dinh.
- Permission parking event/session da enforce ro.

## 13. Phase 9 - Audit, cleanup, va operations

### Muc tieu

Lam file storage van hanh ben vung hon.

### Cleanup retry/outbox

Khi xoa object cu fail:

- Ghi cleanup task vao outbox/table rieng, hoac
- Scheduled job scan object orphan theo metadata/prefix.

Trang thai de xuat:

```text
PENDING
SUCCESS
FAILED_RETRYABLE
FAILED_PERMANENT
```

### Audit log

Audit nen ghi:

- Admin/manager upload/delete avatar customer.
- Admin/manager upload/delete avatar employee.
- Upload/delete parking event image.
- Tao presigned URL private image neu yeu cau trace.

Khong log:

- MinIO secret.
- Full presigned URL.
- Noi dung file.

### Observability

Nen co metrics/log:

- upload success/fail count
- delete success/fail count
- presign count
- object cleanup fail count
- average upload size

## 14. Phase 10 - Avatar review/moderation neu can

### Khi nao can

Chi lam neu PM/BA chot avatar can duyet anh.

Vi du:

- Customer upload avatar can manager approve.
- Anh vi pham bi reject va can reject reason.

### DB migration bo sung

Them columns:

```sql
ALTER TABLE people.user_profile_avatars
ADD COLUMN reviewed_by_account_id UUID REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
ADD COLUMN reviewed_at TIMESTAMPTZ,
ADD COLUMN reject_reason VARCHAR(500);
```

Mo rong status:

```text
PENDING_REVIEW
ACTIVE
REJECTED
REPLACED
DELETED
```

### Flow

Upload avatar:

```text
avatar moi PENDING_REVIEW, is_current=false
avatar cu van ACTIVE/current
```

Approve:

```text
avatar cu REPLACED/current=false
avatar moi ACTIVE/current=true
set reviewed_by/reviewed_at
```

Reject:

```text
avatar moi REJECTED/current=false
set reject_reason
xoa object moi sau commit hoac giu de audit tuy policy
```

### Khuyen nghi

Khong lam phase nay trong MVP neu chua co requirement duyet avatar.

## 15. Phase 11 - File metadata chung neu can mo rong lon

### Khi nao can

Chi lam khi file lan ra nhieu module:

- avatar
- parking event images
- lost card evidence
- support ticket attachments
- invoice/payment proof
- camera/device direct upload

### Bang co the them

```text
storage.files
```

Nhung khong nen lam truoc Phase 1-8.

Ly do:

- `people.user_profile_avatars` da du cho avatar.
- Parking event image co cot rieng trong schema.
- Tao file metadata chung qua som se lam use case nang va phai map resource polymorphic.

## 16. Thu tu uu tien de lam

Trang thai cap nhat 2026-06-21:

- Phase 0-6 da thuc hien cho avatar/account profile.
- Truoc khi lam Phase 7-8, can dong bo `vehicle_management.sql` voi parking migrations V5/V6/V7/V8 de entity, schema snapshot va check-flow khong lech nhau.

Thu tu de xuat tiep theo:

1. Dong bo schema snapshot parking voi V5/V6/V7/V8.
2. Phase 7 - Parking event images private.
3. Phase 8 - Check-in/check-out upload images.
4. Phase 9 - Audit/cleanup/operations.
5. Phase 10 - Avatar review/moderation neu can.
6. Phase 11 - File metadata chung neu can.

Phase 6 da duoc chot theo huong khong tao draft profile rieng. Account moi co profile toi thieu ngay tu dau, con onboarding completed van dua vao customer/employee/approval flow.

## 17. Bang tong hop phase

| Phase | Ten | DB change | API change | Risk | Uu tien |
| --- | --- | --- | --- | --- | --- |
| 0 | Refactor/hardening hien co | Khong | Khong | Thap | Da thuc hien |
| 1 | Tao avatar table va dual-write | Co | Khong | Trung binh | Da thuc hien |
| 2 | Backfill avatar cu | Co | Khong | Trung binh | Da thuc hien |
| 3 | Read tu avatar table | Khong/it | Khong | Trung binh | Da thuc hien |
| 4 | Stop write avatar_url | Khong | Khong | Trung binh | Da thuc hien |
| 5 | Drop avatar_url | Co | Khong | Cao neu lam som | Da thuc hien |
| 6 | Tao profile toi thieu khi tao account | Khong bat buoc | Request them `fullName` | Trung binh | Da thuc hien |
| 7 | Parking event private images | Co the co | Co | Trung binh/cao | Sau avatar |
| 8 | Check-in/out images | Co the co | Co | Cao | Sau parking image |
| 9 | Audit/cleanup/ops | Co the co | Khong | Thap/trung binh | Nen lam |
| 10 | Avatar review | Co | Co the co | Trung binh | Optional |
| 11 | storage.files chung | Co | Co the co | Cao | Tuong lai |

## 18. Definition of Done tong

MinIO/avatar duoc xem la hoan thanh vung khi:

- `people.user_profiles.avatar_url` da drop hoac khong con duoc code dung.
- `people.user_profile_avatars` la source of truth.
- Moi profile chi co toi da mot current avatar.
- Upload/delete avatar co cleanup khi DB fail.
- Delete old object co retry/outbox hoac monitoring.
- API response avatar khong doi voi frontend.
- Private images khong expose object key truc tiep.
- Permission/ownership duoc enforce trong application layer.
- Full test suite pass.

## 19. Ket luan

Huong dung la lam avatar table rieng truoc, nhung phai migration an toan:

```text
giu avatar_url
-> dual-write
-> backfill
-> read tu table moi
-> stop write column cu
-> drop column cu
```

Trang thai hien tai: chuoi migration avatar tren da di den buoc `drop column cu`. API response van giu `avatarUrl`, nhung source of truth la `people.user_profile_avatars`. Phase 6 da doi account creation de tao `people.user_profiles` toi thieu ngay tu dau bang `fullName`, nen self avatar co the hoat dong truoc khi customer/employee onboarding hoan tat ma khong can dua avatar sang `iam.accounts`. Cac phase tiep theo nen uu tien private parking images, cleanup retry/outbox, audit/observability.
