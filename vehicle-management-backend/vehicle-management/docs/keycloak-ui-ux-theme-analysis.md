# Keycloak UI/UX theme analysis

## 1. Muc tieu

Tai lieu nay phan tich cach Job24 dang dung Keycloak cho email/action flow, hien trang Keycloak theme cua `vehicle-management`, va de xuat cach dong bo cac giao dien auth de nguoi dung khong bi roi vao trai nghiem "luc la giao dien san pham, luc la giao dien mac dinh cua Keycloak".

Muc tieu san pham:

- Login, dang ky, quen mat khau, xac minh email va doi mat khau sau khi bam link email phai co cung nhan dien `CoParking`/`Vehicle Management`.
- Cac man hinh bat buoc di qua Keycloak khong duoc de mac dinh logo, mau sac, copywriting va layout cua Keycloak.
- Backend van giu dung vai tro: tao user, trigger email/action, enforce business rule. UI/UX cua Keycloak nam trong Keycloak theme, khong tron vao controller/use case.

## 2. Ket qua kiem tra Job24

Thu muc da kiem tra:

```text
C:\DiskD\VehicleManagement\VehicleManagementProject\vehicle-management-backend\job24-backend
```

### 2.1 Job24 khong dat Keycloak theme trong backend

Trong `job24-backend` khong tim thay:

- `login.ftl`
- `register.ftl`
- `login-reset-password.ftl`
- `email-*.ftl`
- `theme.properties` cho Keycloak theme
- docker compose mount `/opt/keycloak/themes`

Backend Job24 chi co `Dockerfile` cho Spring Boot app, khong co container Keycloak/theme di kem.

Ket luan: neu Job24 co custom giao dien Keycloak trong moi truong chay that, phan do dang nam ngoai `job24-backend` hoac cau hinh truc tiep trong Keycloak server, khong version-control trong backend repo nay.

### 2.2 Job24 dang tranh dung trang dang ky cua Keycloak

Job24 dang tu lam API dang ky rieng trong app:

- `POST /api/auth/register/staff`
- `POST /api/auth/register/recruiter`
- `POST /api/auth/register/company`

Nam tai:

```text
job24-backend/src/main/java/com/ricesoft/job24/infrastructure/web/controller/user/AuthController.java
job24-backend/src/main/java/com/ricesoft/job24/application/user/service/RegisterUserService.java
```

Dang ky tao user tren DB app va Keycloak qua `KeycloakService`, sau do app co the login bang password de lay token. Vi vay, man hinh dang ky cua Job24 kha nang cao nam o frontend cua Job24, khong nam trong Keycloak.

Day la diem nen hoc: nhung flow dang ky co nhieu field nghiep vu, role, ho so, upload/approval thi nen o frontend product, khong nen dua tat ca vao Keycloak registration page.

### 2.3 Job24 van dung Keycloak cho email action

Job24 co API resend:

```text
POST /api/auth/resend-mail
```

Controller goi:

```text
KeycloakService.resendPasswordSetupEmail(email)
```

Service lay user theo email, doc client role, sau do goi:

```java
executeActionsEmail(keycloakClientId, redirect, List.of("UPDATE_PASSWORD"))
```

Neu role la staff thi redirect ve frontend staff, nguoc lai redirect ve frontend recruiter.

Y nghia UX: email setup/reset password va trang update password sau khi bam link van la Keycloak action flow. Neu khong co Keycloak theme rieng, user se thay giao dien Keycloak mac dinh o buoc quan trong nhat cua auth journey.

### 2.4 Email app-side cua Job24 khac voi email Keycloak

Job24 co mot so HTML template trong:

```text
job24-backend/src/main/resources/templates
```

Vi du:

- `avatar-rejection.html`
- `identity-rejection.html`
- `business-license-rejection.html`

Day la email do app tu render/gui, khong phai email Keycloak. Khong nen nham lan nhom nay voi email `VERIFY_EMAIL`, `UPDATE_PASSWORD`, `RESET_PASSWORD` cua Keycloak.

## 3. Hien trang vehicle-management

Sau khi tach ownership, Keycloak UI/import assets nam trong frontend repo:

```text
C:\DiskD\VehicleManagementProject\vehicle-management-frontend\vehicle_management_react\keycloak
```

Backend van giu `docker-compose.keycloak.yml` de chay local infrastructure,
nhung compose mount thu muc Keycloak tu frontend.

### 3.1 Project da co email theme cho Keycloak

Vehicle-management hien co:

```text
vehicle-management-frontend/vehicle_management_react/keycloak/themes/vehicle-management/email/theme.properties
vehicle-management-frontend/vehicle_management_react/keycloak/themes/vehicle-management/email/messages/messages_en.properties
```

`docker-compose.keycloak.yml` da mount:

```yaml
- ../../vehicle-management-frontend/vehicle_management_react/keycloak/themes:/opt/keycloak/themes
- ../../vehicle-management-frontend/vehicle_management_react/keycloak/import:/opt/keycloak/data/import
```

Va dev command da tat cache:

```yaml
--spi-theme-cache-themes=false
--spi-theme-cache-templates=false
```

Realm import dang cau hinh:

```json
"emailTheme": "vehicle-management"
```

Day la nen tot: email Keycloak da duoc version-control, khong phu thuoc thao tac tay trong Admin Console.

### 3.2 Cau hinh con thieu

Realm import dang co:

```json
"loginTheme": "",
"accountTheme": "",
"adminTheme": "",
"emailTheme": "vehicle-management"
```

Nghia la email theme da duoc gan. Login theme files da co trong frontend-owned
`keycloak/themes/vehicle-management/login`, nhung realm import van chua tro toi
login theme nay.

Neu frontend co dung Keycloak authorization-code/browser login, user se thay login page mac dinh. Neu frontend chi login bang API rieng, user van co the thay Keycloak UI khi:

- bam link verify email
- bam link forgot password/update password
- gap required action `UPDATE_PASSWORD`
- gap action error/expired link
- gap info page sau khi action thanh cong

### 3.3 Realm dang tat self-service registration/reset password cua Keycloak

Realm import dang co:

```json
"registrationAllowed": false,
"resetPasswordAllowed": false,
"verifyEmail": false
```

Dieu nay phu hop voi cach backend hien tai:

- Dang ky qua `POST /api/public/auth/register`.
- Gui lai email verify qua `POST /api/public/auth/resend-verification-email`.
- Quen mat khau qua `POST /api/public/auth/forgot-password`.
- Backend trigger Keycloak `send-verify-email` va `execute-actions-email`.

Luu y PM/BA: tat `resetPasswordAllowed` khong co nghia la khong co quen mat khau. Nghia la khong dung link "Forgot password?" mac dinh tren login page cua Keycloak. App dang tu cung cap forgot-password API va trigger email action.

## 4. Ranh gioi san pham nen chot

### 4.1 Nen de frontend product xu ly

Cac flow sau nen tiep tuc nam o frontend/app, vi co logic nghiep vu rieng:

- Dang ky tai khoan public.
- Onboarding profile sau dang ky.
- Resend verification email.
- Forgot password form nhap email.
- Thong bao "neu email ton tai, chung toi se gui email".

Ly do:

- Can copywriting theo product va bao mat enumeration.
- Co rule DB noi bo `iam.accounts`, `people.user_profiles`, role/onboarding.
- De test API va rate-limit trong backend.
- Khong ep Keycloak registration form gan business schema cua app.

### 4.2 Bat buoc custom bang Keycloak login theme

Cac man sau van thuoc Keycloak va can theme:

- Login page neu dung browser login/authorization-code.
- Update password page khi user bam link email.
- Verify email / execute required action page.
- Error page: link het han, action khong hop le, session expired.
- Info page: action thanh cong, quay ve frontend.
- Login reset password page neu sau nay bat `resetPasswordAllowed=true`.

PM view: day la cac diem cham co rui ro cao. User dang xu ly bao mat tai khoan, nen neu layout nhay sang Keycloak mac dinh se lam giam niem tin va tang ty le drop-off.

## 5. De xuat theme structure

Them cac thu muc/file sau:

```text
keycloak/
└── themes/vehicle-management/
├── login/
│   ├── theme.properties
│   ├── template.ftl
│   ├── login.ftl
│   ├── login-reset-password.ftl
│   ├── login-update-password.ftl
│   ├── login-verify-email.ftl
│   ├── info.ftl
│   ├── error.ftl
│   ├── messages/
│   │   ├── messages_en.properties
│   │   └── messages_vi.properties
│   └── resources/
│       ├── css/coparking-auth.css
│       └── img/logo.svg
└── email/
    ├── theme.properties
    ├── html/
    │   ├── email-verification.ftl
    │   ├── executeActions.ftl
    │   └── password-reset.ftl
    ├── text/
    │   ├── email-verification.ftl
    │   ├── executeActions.ftl
    │   └── password-reset.ftl
    └── messages/
        ├── messages_en.properties
        └── messages_vi.properties
```

Trong phase dau co the khong override tat ca `.ftl`, nhung toi thieu nen co:

- `login/theme.properties`
- `login/template.ftl`
- `login/login.ftl`
- `login/login-update-password.ftl`
- `login/info.ftl`
- `login/error.ftl`
- `login/resources/css/coparking-auth.css`
- `email/html/executeActions.ftl`
- `email/html/email-verification.ftl`

## 6. De xuat UI/UX guideline

### 6.1 Brand

Thong nhat ten hien thi:

- Chon mot trong hai: `CoParking` hoac `Vehicle Management`.
- Hien tai email subject dang dung `CoParking`, body lai noi `Vehicle-management`. Nen chot ten public-facing la `CoParking`, ten ky thuat/repo la `vehicle-management`.

Copy de xuat:

- Product name: `CoParking`
- Short descriptor: `Nền tảng quản lý bãi xe`
- Email sender display: `CoParking Security`

### 6.2 Layout auth page

Nen dung layout gon, tin cay, khong marketing qua da:

- Nen trang sang, vung form trung tam.
- Logo/ten san pham o dau form.
- Tieu de ro theo task: `Dang nhap`, `Dat lai mat khau`, `Xac minh email`.
- Mo ta ngan, tranh ngon ngu Keycloak noi bo nhu "required action".
- CTA chinh noi bat.
- Link phu quay ve frontend login.
- Thong bao loi dung tieng Viet/Anh theo locale.

Khong nen:

- De logo Keycloak.
- De chu "Welcome to Keycloak".
- De mau/default PatternFly lech voi frontend.
- De thong bao ky thuat lo ra client/realm/action.

### 6.3 Email UX

Email Keycloak can co:

- Header brand.
- Mot CTA button duy nhat.
- Fallback plain link.
- Han su dung link.
- Canh bao neu nguoi dung khong yeu cau thao tac.
- Footer support/contact.
- Text version tuong duong HTML.

Copy nen tach theo action:

- Verify email: "Xác minh địa chỉ email"
- Update password for provisioned account: "Thiết lập mật khẩu"
- Forgot password: "Đặt lại mật khẩu"

Khong nen gom tat ca vao cau "Quan tri vien vua yeu cau..." vi public self-registration/forgot password khong phai luc nao cung do admin tao.

## 7. Cau hinh realm can chinh

Trong `vehicle-management-frontend/vehicle_management_react/keycloak/import/vehicle-management-realm.json`, nen chinh:

```json
"loginTheme": "vehicle-management",
"emailTheme": "vehicle-management",
"internationalizationEnabled": true,
"supportedLocales": ["vi", "en"],
"defaultLocale": "vi"
```

Tiep tuc giu neu frontend/app tu quan ly flow:

```json
"registrationAllowed": false,
"resetPasswordAllowed": false
```

Chi bat `resetPasswordAllowed=true` neu quyet dinh cho Keycloak login page hien link forgot password va xu ly reset credentials truc tiep.

## 8. Mapping cac journey hien tai

| Journey | UI hien nen dung | Backend/Keycloak action | Theme can co |
| --- | --- | --- | --- |
| Public register | Frontend product | `POST /api/public/auth/register`, Keycloak `send-verify-email` | Email verify, action/info/error |
| Resend verify | Frontend product | `POST /api/public/auth/resend-verification-email` | Email verify, action/info/error |
| Forgot password form | Frontend product | `POST /api/public/auth/forgot-password`, Keycloak `UPDATE_PASSWORD` | Email executeActions, update password, info/error |
| Provisioned account setup | Frontend/admin creates account | Keycloak `UPDATE_PASSWORD` | Email executeActions, update password, info/error |
| Browser login, neu dung | Keycloak | OIDC authorization-code | Login theme |
| API/direct login, neu dung | Frontend product | Token endpoint/backend auth | Khong can login page, van can action pages |

## 9. Backlog thuc hien de xuat

### Phase 1: Dong bo action/email dang bat buoc

- Tao `login` theme cho `vehicle-management`.
- Style `login-update-password.ftl`, `info.ftl`, `error.ftl`.
- Viet lai email HTML/text cho verify email va update password.
- Them message `vi` va `en`.
- Set `loginTheme=vehicle-management` trong realm import.
- Test verify email, forgot password, expired link.

### Phase 2: Dong bo login page neu frontend dung browser flow

- Style `login.ftl`.
- An/disable register/forgot link theo quyet dinh product.
- Neu login bang email duoc phep, label nen la `Email hoặc tên đăng nhập`.
- Dam bao redirect sau login/logout ve frontend dung URL.

### Phase 3: Hardening production

- Doi redirect URI/web origins tu wildcard sang domain cu the.
- Bat HTTPS/base frontend URL theo env.
- Bat event logging cho email/action quan trong neu can audit.
- Bat theme cache trong production.
- Dong goi theme bang image Keycloak rieng hoac mount volume co version.

## 10. Acceptance criteria

Mot release theme duoc xem la dat khi:

- Khong con thay logo/chung tu "Keycloak" tren cac trang user-facing.
- Verify email tu register/resend co HTML brand, CTA ro, plain text fallback.
- Forgot password/update password email co noi dung dung ngu canh.
- Bam link email mo trang Keycloak da theme dung mau/typography/logo cua frontend.
- Link het han/action loi hien trang error co brand va huong dan quay ve frontend.
- Realm import co `loginTheme` va `emailTheme` dung `vehicle-management`.
- Chay lai Keycloak local bang `docker-compose.keycloak.yml` khong can thao tac tay trong Admin Console.

## 11. Ket luan

Job24 cho thay mot pattern dung duoc: form nghiep vu phuc tap nen nam o frontend/app, con Keycloak chi giu identity action nhu setup/reset password. Diem Job24 chua tot la theme Keycloak khong nam trong backend repo nen kho kiem soat UI/UX bang source control.

Vehicle-management nen di theo huong tot hon: tiep tuc giu registration/forgot-password form trong frontend product, nhung phai version-control day du Keycloak `login` va `email` theme trong repo. Nhu vay moi tranh trai nghiem bi dut doan giua UI san pham va UI mac dinh cua Keycloak.
