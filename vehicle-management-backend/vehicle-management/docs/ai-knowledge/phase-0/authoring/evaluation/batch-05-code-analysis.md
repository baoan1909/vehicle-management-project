# Phân tích code đợt 5

Ngày đối chiếu: 2026-11-27. Đây là tài liệu BA/kỹ thuật ngoài nguồn runtime, không dùng như dữ liệu hiện tại của bất kỳ đơn vị nào. Các nguồn Markdown DRAFT và đáp án chuẩn phải tiếp tục qua review.

## Ranh giới dữ liệu và quyền

| Thao tác | Quyền trong code | Kiểm tra phạm vi bổ sung |
|---|---|---|
| Nộp đăng ký đối tác | Endpoint công khai, policy đầu vào | Không cấp vận hành ngay; phải xác minh, đủ hồ sơ và duyệt |
| Xem/xét duyệt đăng ký | ORGANIZATION_REGISTRATION_REVIEW_ALL | Chỉ hồ sơ đúng trạng thái, không giả đã duyệt |
| Xem tổ chức | ORGANIZATION_READ_ALL | OrganizationAccessGuard, tổ chức còn hoạt động và membership/scope |
| Tạo tổ chức | ORGANIZATION_CREATE_ALL | Mã không trùng, quản trị đối tác còn hoạt động |
| Gán quản lý vào bãi | ORGANIZATION_MEMBERSHIP_MANAGE_ALL | Đúng tổ chức, đúng bãi và người quản lý hoạt động |
| Xem nhân viên | EMPLOYEE_READ_ALL | EmployeeAccessGuard lọc tổ chức/bãi, không suy quyền từ tên vai trò |
| Tạo tài khoản nhân sự | ACCOUNT_CREATE_ALL | ProvisionedAccountController và policy vai trò có thể cấp; không phải mời email riêng |
| Duyệt quản lý/nhân viên | ONBOARDING_APPROVAL_REVIEW_PARKING_MANAGER_OWN / ONBOARDING_APPROVAL_REVIEW_EMPLOYEE_ALL | Vai trò đích, người tạo và scope qua InternalEmployeeApprovalAccessGuard |
| Sửa/trạng thái nhân viên | EMPLOYEE_UPDATE_ALL | ensureCanManage; dùng endpoint trạng thái riêng |
| Xóa nhân viên | EMPLOYEE_DELETE_ALL | ensureCanManage và ràng buộc tài khoản/nhân sự |
| Xem vai trò/quyền | ROLE_READ_ALL | Kiểm tra bảo vệ vai trò hệ thống trong use case |
| Cấp/thu hồi bộ quyền | ROLE_ASSIGN_PERMISSION_ALL / ROLE_REVOKE_PERMISSION_ALL | Đồng bộ cả vai trò, không phải cấp riêng một nhân viên |
| Đọc tầng/ô/sơ đồ | PARKING_SPACE_READ_ALL | Bãi nằm trong phạm vi truy cập |
| Cấu hình tầng/ô/nháp | PARKING_SPACE_CONFIGURE_ALL | Còn cần PARKING_TOPOLOGY_CONFIGURE_ALL và scope phù hợp theo guard |
| Đổi trạng thái ô | PARKING_SPACE_STATUS_UPDATE_ALL | Ràng buộc phiên mở và phân bổ vé |
| Xuất bản sơ đồ | PARKING_LAYOUT_PUBLISH_ALL | Quyền cấu hình, phiên bản nháp, topology và dữ liệu đang sử dụng |
| Xem cấu hình danh mục tại bãi | PARKING_LOT_READ_ALL | Bãi/đơn vị trong phạm vi và danh mục được đọc |
| Bật/tắt loại xe tại bãi | VEHICLE_TYPE_UPDATE_ALL | Guard bãi, trạng thái đã công bố, đúng đơn vị; nhiều bãi cần scope đối tác |
| Bật/tắt loại vé tại bãi | TICKET_TYPE_UPDATE_ALL | Guard danh mục/bãi, trạng thái hoạt động, đúng đơn vị |
| Đọc tri thức nội bộ | AI_KNOWLEDGE_READ_EMPLOYEE hoặc quyền quản trị tri thức cho phép | Chỉ hướng dẫn chung; không cấp quyền tác vụ hoặc dữ liệu thật |

Quyền đọc nội bộ hiện chưa được migration cấp mặc định cho PARTNER_ADMIN. Fixture API dùng vai trò test được cấp rõ quyền để đo nhánh có quyền; không mở rộng scope/public hoặc tự tăng quyền thật. Đây là gate chính sách cần review trước phát hành cho đối tác mặc định.

## Đăng ký và tổ chức

`CreatePartnerRegistrationRequest` chỉ chứa họ tên, tên đăng nhập, mật khẩu, mã/tên đơn vị, email và điện thoại. Bước hoàn thiện hồ sơ thêm ngày sinh, giới tính, giấy tờ, địa chỉ cá nhân và đơn vị; avatar được kiểm duyệt riêng, không tự coi là điều kiện kích hoạt tài khoản. Đăng ký tạo tài khoản chờ và yêu cầu duyệt, chưa tạo quyền vận hành. Duyệt kiểm tra email, hồ sơ và trùng dữ liệu trước khi kích hoạt, tạo đơn vị/membership và sao chép danh mục.

Hồ sơ bị từ chối phải gửi lại về chờ trước khi hoàn thiện tiếp; không đổi mã đơn vị qua hoàn thiện hồ sơ. Có chính sách duyệt tự động nhưng không mặc định mọi hồ sơ được tự duyệt. API cập nhật hồ sơ tổ chức còn, nhưng migration đã retire quyền của luồng đối tác; không hướng dẫn truy cập API cũ để né quyền.

Nguồn: [use case đăng ký](../../../../../src/main/java/com/ban/vehicle_management/application/iam/partnerregistration/usecase/PartnerRegistrationUseCaseImpl.java), [validator trước duyệt](../../../../../src/main/java/com/ban/vehicle_management/domain/iam/partnerregistration/policy/PartnerApprovalValidator.java), [adapter đăng ký](../../../../../src/main/java/com/ban/vehicle_management/infrastructure/persistence/adapter/iam/PartnerRegistrationPersistenceAdapter.java), [migration retire hồ sơ tổ chức](../../../../../src/main/resources/db/migration/V20261021190000__retire_partner_organization_profile_access.sql).

## Nhân sự và vai trò

EmployeeController không có create/invite độc lập; tạo tài khoản đi theo provisioning. Phê duyệt còn phụ thuộc quyền duyệt loại nhân sự, vai trò đích, người tạo, membership và bãi được phân công. Sửa thông tin không thay đổi trạng thái qua cùng endpoint. Tạm khóa chỉ từ nhân viên đang hoạt động; trạng thái tài khoản và trạng thái nhân viên là hai khái niệm riêng (tài khoản không có enum INACTIVE).

Lưu quyền là full-set synchronization; quyền không được chọn có thể bị thu hồi, tài khoản dùng vai trò được invalidation quyền. Vai trò hệ thống có bảo vệ riêng. Không coi cùng vai trò là cùng phạm vi dữ liệu.

Nguồn: [EmployeeUseCaseImpl](../../../../../src/main/java/com/ban/vehicle_management/application/people/employee/usecase/EmployeeUseCaseImpl.java), [EmployeeAccessGuard](../../../../../src/main/java/com/ban/vehicle_management/application/people/employee/authorization/EmployeeAccessGuard.java), [EmployeeRepository](../../../../../src/main/java/com/ban/vehicle_management/infrastructure/persistence/database/repository/people/EmployeeRepository.java), [guard xét duyệt](../../../../../src/main/java/com/ban/vehicle_management/application/operations/approvalrequest/authorization/InternalEmployeeApprovalAccessGuard.java), [RolePermissionController](../../../../../src/main/java/com/ban/vehicle_management/entrypoint/controller/iam/RolePermissionController.java).

## Cấu trúc và sơ đồ

Bãi → tầng → khu → ô/sơ đồ phải cùng topology; làn là thành phần vận hành riêng. Xóa tầng còn khu bị chặn. Bản nháp có revision; xung đột phải tải lại. Preview/publish kiểm mã, hình học, biên khung/chồng lấn và dữ liệu đang sử dụng. Xuất bản tạo/cập nhật ô thật và đồng bộ sức chứa; phần tử trang trí không tự tăng chỗ đỗ. Không hứa tự thiết kế tối ưu hoàn chỉnh.

Quản lý bãi được phân công có thể cấu hình khi có quyền topology và scope tương ứng. Không cấm tuyệt đối theo chức danh hoặc tự cho phép mọi bãi theo vai trò.

Nguồn: [guard tổ chức/bãi](../../../../../src/main/java/com/ban/vehicle_management/application/iam/organization/authorization/OrganizationAccessGuard.java), [ParkingLayoutUseCaseImpl](../../../../../src/main/java/com/ban/vehicle_management/application/parking/parkinglayout/usecase/ParkingLayoutUseCaseImpl.java), [ParkingLayoutPolicy](../../../../../src/main/java/com/ban/vehicle_management/domain/parking/parkinglayout/policy/ParkingLayoutPolicy.java), [migration tầng/ô/sơ đồ](../../../../../src/main/resources/db/migration/V20261008100000__parking_levels_spaces_layout_allocations.sql), [revision nháp](../../../../../src/main/resources/db/migration/V20261010000000__draft_layout_snapshot_and_revision.sql).

## Danh mục

Có loại xe, loại vé, loại thẻ, bảng giá, quy tắc giá, voucher; lịch ngày nghỉ chưa có CRUD hoàn chỉnh. Loại thẻ dùng quyền riêng, không mặc định đối tác được sửa. Mã trùng có phạm vi theo từng loại, không áp một quy tắc toàn cục. Loại xe/vé/bảng giá/quy tắc mới đi qua nháp/công bố; rule giá gắn bãi và tham chiếu đúng đơn vị. Rule đã dùng cần bản mới; loại vé đã dùng không đổi mã.

Khả dụng trong dropdown là giao của quyền, đơn vị, bãi, trạng thái và điều kiện API. Tắt phục vụ loại xe ở một bãi khác ngừng toàn đơn vị; vé còn hiệu lực/phiên mở tiếp tục theo quy tắc. Tắt cho mọi bãi, kể cả chỉ một bãi, còn ngừng các rule giá hoạt động của loại xe. Voucher có scope hệ thống/đơn vị/bãi riêng.

Nguồn: [CatalogAccessGuard](../../../../../src/main/java/com/ban/vehicle_management/application/catalog/authorization/CatalogAccessGuard.java), [khả dụng theo bãi](../../../../../src/main/java/com/ban/vehicle_management/application/catalog/availability/usecase/ParkingLotCatalogAvailabilityUseCaseImpl.java), [VehicleTypeRepository](../../../../../src/main/java/com/ban/vehicle_management/infrastructure/persistence/database/repository/catalog/VehicleTypeRepository.java), [public catalog filter](../../../../../src/main/java/com/ban/vehicle_management/application/catalog/availability/usecase/PublicParkingLotCatalogUseCaseImpl.java), [VoucherUseCaseImpl](../../../../../src/main/java/com/ban/vehicle_management/application/catalog/voucher/usecase/VoucherUseCaseImpl.java), [catalog draft lifecycle](../../../../../src/main/resources/db/migration/V20261116105000__partner_catalog_draft_lifecycle.sql), [rule gắn bãi](../../../../../src/main/resources/db/migration/V20261115100000__scope_price_rules_to_parking_lots.sql).

## Giới hạn chatbot đã xác định

AiToolRegistry chưa có công cụ đọc/ghi tổ chức, nhân sự, phân quyền, cấu trúc/sơ đồ hoặc danh mục thực tế. Policy mới báo capability gap trước khi chọn nhầm công cụ khách hàng. Hướng dẫn tĩnh dùng nguồn được phép; yêu cầu đọc/ghi cần đúng khả năng và quyền, không dùng RAG thay dữ liệu thật. Tạo phiếu hỗ trợ nói về đối tác và đọc xe/vé cá nhân nhắc loại xe/vé vẫn giữ luồng hiện có.
