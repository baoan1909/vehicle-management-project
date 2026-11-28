# Quy tắc cấp và quản lý tài khoản

Cập nhật 28/11/2026. Tài liệu này thay thế phân cấp cũ chưa có PARTNER_ADMIN. Đọc cùng [quản trị đa bãi](saas-multi-parking-governance.md).

## Permission và phạm vi

Mỗi thao tác cần permission hiệu lực tương ứng, tài khoản đủ điều kiện, target thuộc phạm vi tổ chức/bãi và trạng thái nghiệp vụ hợp lệ. Scope không tự cấp quyền thao tác. Ngoại lệ bắt buộc: quyền `BROADCAST_NOTIFICATION_*` chỉ có hiệu lực đối với SYSTEM_ADMIN; không cấp tự động nếu role này thiếu permission.

| Chủ thể mặc định | Scope | Target được tạo/quản lý | Đọc bổ sung |
| --- | --- | --- | --- |
| SYSTEM_ADMIN | PARKING_SCOPE_PLATFORM | SYSTEM_ADMIN, PARTNER_ADMIN, CUSTOMER | PARKING_MANAGER chỉ đọc |
| PARTNER_ADMIN | PARKING_SCOPE_PARTNER | PARKING_MANAGER, EMPLOYEE cùng Partner | Không đọc Partner khác |
| PARKING_MANAGER | PARKING_SCOPE_ASSIGNED | EMPLOYEE trong các bãi đang quản lý | Không đọc nhân sự ngoài phạm vi |
| EMPLOYEE | Phân công bãi và ca | Không provision account | Dữ liệu vận hành được cấp quyền |
| CUSTOMER | Bản thân | Không provision account | Dữ liệu thuộc sở hữu của mình |

Các role tùy chỉnh vẫn phải tuân theo permission và scope hiệu lực. Vai trò người dùng không thay thế kiểm tra tài nguyên ở backend.

## API tài khoản

- `POST /api/iam/accounts/provisioned`: `ACCOUNT_CREATE_ALL`, kiểm tra target role, tạo hồ sơ và membership theo quy tắc nghiệp vụ.
- `GET /api/iam/accounts/provisioned` và `GET /api/iam/accounts/provisioned/{accountId}`: `ACCOUNT_READ_ALL`, dùng cùng chính sách đọc target. System Admin có thể xem Manager mà không được sửa Manager.
- `PATCH /api/iam/accounts/provisioned/{accountId}/status`: `ACCOUNT_UPDATE_ALL`, kiểm tra target hiện tại thuộc phạm vi quản lý.
- `PATCH /api/iam/accounts/provisioned/{accountId}/role`: `ACCOUNT_UPDATE_ALL`, kiểm tra cả role hiện tại và role đích thuộc phạm vi quản lý; không chuyển qua lại giữa nhóm nội bộ và Customer.

Ví dụ: Partner tạo Manager/Employee cùng đơn vị; Manager tạo Employee thuộc đơn vị, Employee vẫn cần Partner phân công vào bãi trước khi sử dụng nghiệp vụ. Manager không tạo Customer hoặc Partner Admin. System Admin tạo Customer, nhưng không tạo Employee hoặc Manager trực tiếp qua API provision.

## Phân công và vòng đời nhân sự

- Partner Admin phân công/điều chuyển Manager và Employee giữa các bãi thuộc Partner của mình.
- Manager quản lý Employee đã được phân công vào bãi mình quản lý và xếp ca trong phạm vi đó; không tự gán Employee vào bãi hoặc điều chuyển giữa bãi.
- Hoàn tất hồ sơ, phê duyệt, trạng thái hoạt động và phân công hiệu lực là điều kiện mở sidebar nghiệp vụ. Đã duyệt nhưng chưa có phân công: `ORGANIZATION_ACCESS_REQUIRED`, hiển thị cảnh báo chờ phân công.
- Employee có thể xem lịch và mở ca bằng `SHIFT_OPEN_OWN`. Mở ca yêu cầu đúng nhân viên trong ca, phân công bãi còn hiệu lực, bãi vận hành được, ca SCHEDULED, đúng thời gian và không có ca khác đang mở.
- Check-in/out và thanh toán của actor có `PARKING_SCOPE_SHIFT` phải qua scope ca. Thanh toán lấy giao của bãi được phân công và bãi có ca đang mở hợp lệ; ca quá giờ, phân công hết hạn hoặc bãi đình chỉ không được vận hành.

## Hồ sơ và ảnh đại diện

API `/api/people/user-profiles` trả hồ sơ đầy đủ nên yêu cầu thêm `PARKING_SCOPE_PLATFORM`. Nhân sự vận hành dùng endpoint Customer/Employee đã được giới hạn phạm vi; API hồ sơ bản thân không thay đổi.

`AVATAR_APPROVAL_READ_ALL` và `AVATAR_APPROVAL_REVIEW_ALL` tách biệt với quyền hồ sơ. System Admin duyệt phạm vi nền tảng; Partner duyệt nhân sự cùng đơn vị; Manager duyệt Employee thuộc bãi quản lý. Employee không được cấp quyền duyệt mặc định. List và thao tác theo ID đều phải kiểm tra scope.

## Giao diện và kiểm thử

- Thiếu permission: ẩn sidebar/tab/liên kết/nút/form, không phát request phụ trái quyền.
- Có permission nhưng dữ liệu hoặc trạng thái chưa hợp lệ: có thể disable với giải thích phù hợp.
- Khả năng đọc/sửa tài khoản được xét trên từng target; không bật nút sửa Manager cho System Admin.
- Gọi API trực tiếp vẫn bị kiểm tra permission, scope và trạng thái.
- Regression: `ProvisionedAccountUseCaseImplTest`, `ParkingLotAssignmentUseCaseImplTest`, `RoleScopeAuditTest`, `AvatarModerationUseCaseImplTest`, `ShiftUseCaseImplTest`, `ShiftAssignmentUseCaseImplTest`, `InternalRolePermissionMigrationTest` và `tests/role-permission.behavior.spec.ts`.

Migration: `V20261128120000__harden_internal_role_permissions.sql`. Quyền broadcast ngoài SYSTEM_ADMIN bị vô hiệu hóa; quyền hồ sơ đầy đủ của nội bộ vận hành bị thu hồi; quyền avatar được cấp riêng. Backend policy tiếp tục chặn broadcast ngoài SYSTEM_ADMIN nếu dữ liệu cũ/custom grant vẫn còn.
