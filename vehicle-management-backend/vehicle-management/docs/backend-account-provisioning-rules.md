# Backend account management rules

Tài liệu này mô tả rule backend cho nhóm API quản lý provisioned account qua module IAM.

Rule chính:

- `SYSTEM_ADMIN` chỉ quản lý được tài khoản có role `SYSTEM_ADMIN`, `PARKING_MANAGER`.
- `PARKING_MANAGER` chỉ quản lý được tài khoản có role `EMPLOYEE`, `CUSTOMER`.
- `EMPLOYEE`, `CUSTOMER` không được quản lý provisioned account.

Trong tài liệu này, "quản lý tài khoản" bao gồm: tạo account, xem danh sách/chi tiết account, cập nhật trạng thái account, và đổi role account.

## 1. API áp dụng

### 1.1 Tạo provisioned account

```http
POST /api/iam/accounts/provisioned
```

Body:

```json
{
  "username": "employee.01",
  "email": "employee.01@example.com",
  "roleCode": "EMPLOYEE",
  "fullName": "Nguyen Van Employee"
}
```

### 1.2 Xem danh sách provisioned account

```http
GET /api/iam/accounts/provisioned
```

Backend phải chỉ trả về account nằm trong phạm vi role mà caller được quản lý.

### 1.3 Xem chi tiết provisioned account

```http
GET /api/iam/accounts/provisioned/{accountId}
```

Nếu target account nằm ngoài phạm vi role được quản lý, backend trả `403 Forbidden`.

### 1.4 Cập nhật trạng thái provisioned account

```http
PATCH /api/iam/accounts/provisioned/{accountId}/status
```

Body:

```json
{
  "status": "LOCKED",
  "reason": "Policy violation"
}
```

Nếu target account nằm ngoài phạm vi role được quản lý, backend trả `403 Forbidden`.

### 1.5 Đổi role provisioned account

```http
PATCH /api/iam/accounts/provisioned/{accountId}/role
```

Body:

```json
{
  "roleCode": "PARKING_MANAGER"
}
```

## 2. Rule phân quyền quản lý account theo role hiện tại

Các permission như `ACCOUNT_CREATE_ALL`, `ACCOUNT_READ_ALL`, `ACCOUNT_UPDATE_ALL` chỉ là quyền kỹ thuật để được gọi API. Ngoài permission, backend vẫn phải kiểm tra role hiện tại được phép quản lý target role nào.

| Current role | Được quản lý target role |
| --- | --- |
| `SYSTEM_ADMIN` | `SYSTEM_ADMIN`, `PARKING_MANAGER` |
| `PARKING_MANAGER` | `EMPLOYEE`, `CUSTOMER` |
| `EMPLOYEE` | Không được provision account |
| `CUSTOMER` | Không được provision account |

Nếu caller có permission kỹ thuật nhưng target role không nằm trong danh sách được phép quản lý, backend trả `403 Forbidden`.

Migration quyền cần đồng bộ với rule này:

- `SYSTEM_ADMIN` đã có `ACCOUNT_CREATE_ALL`, `ACCOUNT_READ_ALL`, `ACCOUNT_UPDATE_ALL`.
- `PARKING_MANAGER` được cấp `ACCOUNT_CREATE_ALL`, `ACCOUNT_READ_ALL`, `ACCOUNT_UPDATE_ALL` để gọi nhóm API quản lý provisioned account cho `EMPLOYEE` và `CUSTOMER`.
- Backend vẫn bắt buộc filter/check target role theo bảng trên, không được cho manager xem hoặc cập nhật `SYSTEM_ADMIN`, `PARKING_MANAGER`.

## 3. Rule đổi role provisioned account

API đổi role phải áp dụng hai lớp kiểm tra:

1. Caller phải được phép quản lý target account hiện tại.
2. Caller phải được phép quản lý target role mới.

Mục đích là tránh bypass bằng cách:

1. Tạo account hoặc lấy một account trong phạm vi được phép.
2. Gọi API đổi role sang role nằm ngoài phạm vi quản lý.

Ngoài rule target role, backend vẫn giữ rule hiện có:

- Không đổi role qua lại giữa nhóm internal và customer.
- Internal roles gồm `SYSTEM_ADMIN`, `PARKING_MANAGER`, `EMPLOYEE`.
- `CUSTOMER` là nhóm customer.

Ví dụ:

- `SYSTEM_ADMIN` không được đổi một account sang `EMPLOYEE`.
- `SYSTEM_ADMIN` không được xem, khóa, mở khóa, đổi role account `EMPLOYEE`, `CUSTOMER`.
- `PARKING_MANAGER` không được đổi một account sang `SYSTEM_ADMIN`.
- `PARKING_MANAGER` không được xem, khóa, mở khóa, đổi role account `SYSTEM_ADMIN`, `PARKING_MANAGER`.
- Đổi từ internal role sang `CUSTOMER` vẫn bị chặn bởi rule không đổi giữa nhóm internal và customer.

## 4. Ý nghĩa nghiệp vụ

`SYSTEM_ADMIN` quản trị tầng hệ thống/IAM cấp cao, nên chỉ quản lý nhóm tài khoản cấp hệ thống:

- `SYSTEM_ADMIN`: admin hệ thống, cần approval system admin onboarding.
- `PARKING_MANAGER`: quản lý bãi xe, sau đó đi qua internal employee onboarding approval.

`PARKING_MANAGER` quản lý vận hành bãi xe, nên chỉ quản lý nhóm tài khoản vận hành và khách hàng:

- `EMPLOYEE`: nhân viên vận hành, sau đó đi qua internal employee onboarding approval.
- `CUSTOMER`: tài khoản khách hàng được tạo bởi quản lý hoặc khách hàng tự đăng ký, sau đó đi qua customer onboarding approval khi cần.

Rule này phải nằm ở application use case, không chỉ ở controller annotation, vì controller chỉ kiểm tra permission chung còn application use case mới nắm được target role hoặc target account.

## 5. Test checklist

### 5.1 SYSTEM_ADMIN tạo PARKING_MANAGER

Kỳ vọng: thành công.

```json
{
  "username": "manager.01",
  "email": "manager01@example.com",
  "roleCode": "PARKING_MANAGER",
  "fullName": "Tran Manager"
}
```

### 5.2 SYSTEM_ADMIN tạo EMPLOYEE

Kỳ vọng: `403 Forbidden`.

```json
{
  "username": "employee.01",
  "email": "employee01@example.com",
  "roleCode": "EMPLOYEE",
  "fullName": "Nguyen Employee"
}
```

### 5.3 SYSTEM_ADMIN tạo CUSTOMER

Kỳ vọng: `403 Forbidden`.

```json
{
  "username": "customer.01",
  "email": "customer01@example.com",
  "roleCode": "CUSTOMER",
  "fullName": "Nguyen Customer"
}
```

### 5.4 PARKING_MANAGER tạo EMPLOYEE

Kỳ vọng: thành công.

```json
{
  "username": "employee.02",
  "email": "employee02@example.com",
  "roleCode": "EMPLOYEE",
  "fullName": "Le Employee"
}
```

### 5.5 PARKING_MANAGER tạo CUSTOMER

Kỳ vọng: thành công.

```json
{
  "username": "customer.02",
  "email": "customer02@example.com",
  "roleCode": "CUSTOMER",
  "fullName": "Pham Customer"
}
```

### 5.6 PARKING_MANAGER tạo SYSTEM_ADMIN hoặc PARKING_MANAGER

Kỳ vọng: `403 Forbidden`.

### 5.7 SYSTEM_ADMIN xem hoặc cập nhật EMPLOYEE/CUSTOMER

Kỳ vọng: `403 Forbidden`.

### 5.8 PARKING_MANAGER xem hoặc cập nhật SYSTEM_ADMIN/PARKING_MANAGER

Kỳ vọng: `403 Forbidden`.

## 6. Code location

Rule hiện được enforce ở:

```text
src/main/java/com/ban/vehicle_management/application/iam/account/usecase/ProvisionedAccountUseCaseImpl.java
```

Test chính:

```text
src/test/java/com/ban/vehicle_management/application/iam/account/usecase/ProvisionedAccountUseCaseImplTest.java
```

## 7. Phân tích BA và senior engineer

### 7.1 Phân tích nghiệp vụ

Rule quản lý account phải tách rõ hai khái niệm:

- Permission kỹ thuật: caller có được gọi API hay không, ví dụ `ACCOUNT_CREATE_ALL`, `ACCOUNT_READ_ALL`, `ACCOUNT_UPDATE_ALL`.
- Phạm vi nghiệp vụ: caller được quản lý account thuộc role nào.

Nếu chỉ dựa vào permission kỹ thuật thì `SYSTEM_ADMIN` có `ACCOUNT_UPDATE_ALL` sẽ vô tình quản lý được `EMPLOYEE`, `CUSTOMER`. Điều này sai với rule nghiệp vụ mới vì:

- `SYSTEM_ADMIN` là tầng quản trị hệ thống/IAM cấp cao, chỉ nên quản lý `SYSTEM_ADMIN`, `PARKING_MANAGER`.
- `PARKING_MANAGER` là tầng vận hành, chỉ nên quản lý `EMPLOYEE`, `CUSTOMER`.
- Việc để một role quản lý chéo tầng làm mờ trách nhiệm phê duyệt, audit và vận hành.

Vì vậy, rule target role phải áp dụng cho tất cả API quản lý account, không chỉ API tạo account.

### 7.2 Phân tích API theo hành vi mong muốn

| API | Hành vi đúng |
| --- | --- |
| `POST /api/iam/accounts/provisioned` | Check permission tạo account, sau đó check target role trong body có thuộc phạm vi caller được quản lý không. |
| `GET /api/iam/accounts/provisioned` | Trả danh sách đã được filter theo phạm vi role caller được quản lý. Không trả account ngoài phạm vi. |
| `GET /api/iam/accounts/provisioned/{accountId}` | Load target account, nếu role của target account ngoài phạm vi quản lý thì trả `403 Forbidden`. |
| `PATCH /api/iam/accounts/provisioned/{accountId}/status` | Load target account, check caller được quản lý role hiện tại của account đó, sau đó mới cập nhật status. |
| `PATCH /api/iam/accounts/provisioned/{accountId}/role` | Check cả role hiện tại của target account và role mới trong request. Caller phải được quản lý cả hai. |

Với API danh sách, nên filter dữ liệu thay vì trả `403` toàn bộ request. Với API thao tác trên một account cụ thể, nếu account tồn tại nhưng ngoài phạm vi quản lý thì trả `403`.

### 7.3 Trạng thái triển khai code

Rule quản lý account đã được enforce trong `ProvisionedAccountUseCaseImpl`:

- `createProvisionedAccount(...)`: check target role trong body qua `ensureCanManageTargetRole(...)`.
- `getProvisionedAccounts(...)`: truyền `managedRoleCodes` xuống filter query để chỉ trả account thuộc phạm vi caller được quản lý.
- `getProvisionedAccountById(...)`: load target account rồi check role hiện tại của target account.
- `updateProvisionedAccountStatus(...)`: load target account rồi check role hiện tại trước khi cập nhật status.
- `updateProvisionedAccountRole(...)`: check cả role hiện tại của target account và role mới trong request.

Filter danh sách được thực hiện ở `ProvisionedAccountSpecifications` bằng `managedRoleCodes`. Nếu command không có role scope hợp lệ, specification trả predicate rỗng theo hướng fail-closed thay vì trả toàn bộ provisioned accounts.

Các rủi ro bypass đã được chặn:

- `SYSTEM_ADMIN` không thể xem hoặc cập nhật account `EMPLOYEE`, `CUSTOMER`.
- `SYSTEM_ADMIN` không thể đổi một account `EMPLOYEE` sang `PARKING_MANAGER` chỉ vì role mới nằm trong phạm vi của system admin.
- Nếu sau này cấp `ACCOUNT_UPDATE_ALL` cho `PARKING_MANAGER`, manager vẫn không thể thao tác lên account ngoài phạm vi vì use case kiểm tra role hiện tại của target account.

### 7.4 Thiết kế đã triển khai

Không rải logic role matrix ở nhiều method. Rule đã được gom vào domain policy:

```text
src/main/java/com/ban/vehicle_management/domain/iam/account/policy/ProvisionedAccountPolicy.java
```

Policy này chứa các rule chính:

- `managedTargetRoles(currentRole)`: xác định target roles mà caller được quản lý.
- `canManageTargetRole(currentRole, targetRole)`: kiểm tra role matrix.
- `validateRoleTransition(currentRole, targetRole)`: chặn đổi role giữa internal/customer.
- `validateStatusTransition(currentStatus, targetStatus)`: chặn transition trạng thái không hợp lệ.
- Mapping `AccountStatus` sang `UserProfileStatus`, `EmployeeStatus`, `CustomerStatus`.

Use case dùng lại policy cho:

- Create: check target role trong request.
- List: truyền `managedRoleCodes` xuống filter query.
- Detail: load account rồi check role hiện tại của target account.
- Status update: load account rồi check role hiện tại của target account.
- Role update: check cả role hiện tại và role mới.

Riêng onboarding rule đã được tách vào:

```text
src/main/java/com/ban/vehicle_management/domain/iam/account/policy/AccountOnboardingPolicy.java
```

Policy onboarding chứa các rule như role nào cần employee/customer record, khi nào system admin cần approval request, và default job title theo role.

### 7.5 Trạng thái triển khai kỹ thuật

Các bước đã hoàn thành:

1. Tạo domain policy xác định role được quản lý theo current role.
2. Sửa list API để chỉ trả account thuộc allowed roles.
3. Sửa detail/status/role update để check role hiện tại của target account.
4. Sửa role update để check cả role hiện tại và role mới.
5. Tách onboarding rule khỏi `AccountProfileUseCaseImpl`.
6. Giữ unique check, port call, transaction và external integration ở application use case.

Code đã có filter/check target role. `PARKING_MANAGER` được cấp permission kỹ thuật để gọi API create/read/update provisioned account, nhưng dữ liệu và thao tác vẫn bị giới hạn trong phạm vi `EMPLOYEE`, `CUSTOMER`.

### 7.6 Test cần có

| Case | Kỳ vọng |
| --- | --- |
| `SYSTEM_ADMIN` list account | Chỉ thấy `SYSTEM_ADMIN`, `PARKING_MANAGER`. |
| `SYSTEM_ADMIN` detail `EMPLOYEE` | `403 Forbidden`. |
| `SYSTEM_ADMIN` update status `CUSTOMER` | `403 Forbidden`. |
| `SYSTEM_ADMIN` update role `EMPLOYEE -> PARKING_MANAGER` | `403 Forbidden` vì role hiện tại của target account ngoài phạm vi quản lý. |
| `PARKING_MANAGER` list account | Chỉ thấy `EMPLOYEE`, `CUSTOMER`. |
| `PARKING_MANAGER` detail `SYSTEM_ADMIN` | `403 Forbidden`. |
| `PARKING_MANAGER` update status `PARKING_MANAGER` | `403 Forbidden`. |
| `PARKING_MANAGER` update role `EMPLOYEE -> SYSTEM_ADMIN` | `403 Forbidden` vì role mới ngoài phạm vi quản lý. |
