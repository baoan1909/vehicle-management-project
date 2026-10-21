# Hồ sơ đối tác và quy trình xét duyệt cập nhật

## Mục tiêu nghiệp vụ

Sau khi được duyệt trở thành Partner Admin, người dùng không còn nhìn thấy nghiệp vụ **Đăng ký đối tác**. Thay vào đó họ quản lý **Hồ sơ đối tác** của tổ chức mà mình là thành viên.

Partner Admin có thể đề xuất đổi tên hoặc địa chỉ. Đề xuất này **không sửa ngay** bảng `iam.organizations`. Chỉ khi System Admin duyệt, dữ liệu đang hoạt động mới được thay đổi. Nhờ vậy System Admin vẫn kiểm soát các thông tin pháp lý và vận hành của đối tác.

## Quyền và phạm vi

| Permission | Được cấp cho | Ý nghĩa |
| --- | --- | --- |
| `ORGANIZATION_PROFILE_READ_OWN` | `PARTNER_ADMIN` | Xem hồ sơ của tổ chức mà tài khoản có membership đang hoạt động. |
| `ORGANIZATION_PROFILE_UPDATE_REQUEST_OWN` | `PARTNER_ADMIN` | Gửi một yêu cầu cập nhật hồ sơ của chính tổ chức đó. |
| `ORGANIZATION_PROFILE_REVIEW_ALL` | `SYSTEM_ADMIN` | Xem, duyệt hoặc từ chối mọi yêu cầu cập nhật hồ sơ đối tác. |
| `ORGANIZATION_REGISTRATION_REVIEW_ALL` | `SYSTEM_ADMIN` | Xem, duyệt hoặc từ chối đơn đăng ký đối tác ban đầu. |

Không kiểm tra trực tiếp bằng tên vai trò trong Java. Migration cấp permission cho role; controller và use case chỉ kiểm tra permission. Phạm vi “của mình” được xác định qua `iam.organization_memberships`, không nhận `organizationId` tùy ý từ giao diện.

## Luồng xử lý

```text
Partner Admin
  → GET /api/iam/organizations/my-profile
  → POST /api/iam/organizations/my-profile/update-requests
       lưu bản chụp thông tin đề xuất vào operations.approval_requests (PENDING)
       không sửa iam.organizations

System Admin
  → GET /api/iam/organization-profile-update-requests?status=PENDING
  → POST .../{id}/approve
       kiểm tra lại dữ liệu → cập nhật iam.organizations → đánh dấu APPROVED
  → POST .../{id}/reject
       bắt buộc ghi lý do → đánh dấu REJECTED
```

Mỗi tổ chức chỉ có một yêu cầu `PENDING` tại một thời điểm. Điều này tránh việc hai bản cập nhật cạnh tranh nhau. Khi duyệt, bản ghi yêu cầu được khóa ở tầng persistence trước khi xử lý để tránh hai System Admin cùng duyệt một yêu cầu.

## Thông tin hiển thị và căn cứ xét duyệt

Trang **Hồ sơ đối tác** của Partner Admin hiển thị hai nhóm dữ liệu rõ ràng:

- hồ sơ đã có hiệu lực: mã, tên, địa chỉ, trạng thái, tài khoản đại diện, email, ngày tạo và lần cập nhật cuối;
- tóm tắt vận hành chỉ đọc: tổng số bãi xe, số bãi đang hoạt động, đang thiết lập và đang bảo trì.

System Admin không duyệt chỉ dựa trên thông tin do Partner gửi. Trong hộp xét duyệt, mỗi trường thay đổi hiển thị theo cặp **Hiện hành** và **Đề xuất**, kèm thời điểm gửi. Điều này giúp reviewer biết chính xác thay đổi nào sẽ được áp dụng.

Nếu System Admin không thấy menu **Duyệt cập nhật hồ sơ đối tác** hoặc API trả `403`, hãy đăng xuất rồi đăng nhập lại để tải permission mới và kiểm tra endpoint `GET /api/iam/accounts/current-access` có `ORGANIZATION_PROFILE_REVIEW_ALL`. Nếu API duyệt trả `200` nhưng danh sách rỗng, kiểm tra bản ghi `ORGANIZATION_PROFILE_UPDATE` trong `operations.approval_requests`.

## Tổ chức mã nguồn

### Backend

- `application/iam/organizationprofile/port/in/OrganizationProfilePortIn.java`: hợp đồng use case.
- `application/iam/organizationprofile/usecase/OrganizationProfileUseCaseImpl.java`: kiểm tra permission, lấy organization qua membership và điều phối workflow.
- `application/iam/organizationprofile/port/out/OrganizationProfileApprovalPortOut.java`: cổng lưu/đọc yêu cầu duyệt.
- `infrastructure/persistence/adapter/iam/OrganizationProfileApprovalPersistenceAdapter.java`: hiện thực cổng trên bằng bảng approval chung.
- `entrypoint/controller/iam/OrganizationProfileController.java`: bốn API cho Partner Admin và System Admin.
- `entrypoint/dto/iam/organization/*`: dữ liệu đi qua HTTP; entity domain không được trả trực tiếp.
- `V20261015100000__add_partner_organization_profile_approval_permissions.sql`: khai báo permission và gán theo **mã role**, không phụ thuộc ID role cố định.

### Frontend

- `features/iam/pages/OrganizationProfilePage.tsx`: màn hình Partner Admin xem/gửi thay đổi hồ sơ.
- `features/iam/pages/OrganizationProfileApprovalPage.tsx`: hàng đợi System Admin duyệt/từ chối.
- `features/iam/api/organizationProfileApi.ts`: các lời gọi HTTP và kiểu dữ liệu.
- `app/routePermissions.ts`: ẩn route khỏi người không có permission.
- `config/navigation.ts`: hiển thị “Hồ sơ đối tác” cho Partner Admin, còn System Admin thấy các mục duyệt.

## Kiểm thử cần thực hiện

1. Đăng nhập Partner Admin: menu chỉ có **Hồ sơ đối tác**, không có **Đăng ký đối tác**.
2. Gửi đổi tên/địa chỉ; kiểm tra trang hồ sơ vẫn hiện dữ liệu cũ và nút gửi bị khóa khi đang chờ.
3. Đăng nhập System Admin, mở **Duyệt cập nhật hồ sơ đối tác**, xác nhận màn hình so sánh đúng giá trị **Hiện hành** và **Đề xuất**, rồi duyệt yêu cầu.
4. Đăng nhập lại Partner Admin hoặc làm mới trang: thông tin mới mới xuất hiện.
5. Gửi yêu cầu khác, từ chối không nhập lý do: hệ thống phải báo lỗi; từ chối có lý do thì Partner Admin thấy phản hồi.
6. Thử gọi API bằng tài khoản không có permission: backend phải trả `403`, dù người dùng tự nhập URL trực tiếp.

Kiểm thử đơn vị `OrganizationProfileUseCaseImplTest` bảo vệ bốn quy tắc: gửi yêu cầu không được cập nhật organization ngay, chỉ duyệt mới cập nhật, từ chối bắt buộc có lý do, và System Admin nhận được giá trị hiện hành để so sánh.
