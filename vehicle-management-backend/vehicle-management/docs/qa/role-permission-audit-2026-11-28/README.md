# Đánh giá phân quyền và giao diện CoParking

> Đây là bằng chứng **trước sửa**. Các thay đổi và kết quả kiểm thử sau sửa nằm tại [FIXES.md](FIXES.md).

Ngày đánh giá: 28/11/2026. Đối tượng: SYSTEM_ADMIN, PARTNER_ADMIN, PARKING_MANAGER, EMPLOYEE và CUSTOMER; ưu tiên nhân sự nội bộ.

**Kết luận: chưa thể coi phân quyền đã hoàn chỉnh.** Cơ chế quyền hiệu lực, khóa sidebar trước khi được phân công và nhiều guard theo bãi đã có. Tuy nhiên, còn đường đọc hồ sơ thiếu phạm vi, thông báo toàn sàn được cấp cho Manager, kiểm tra ca trực bị bỏ qua khi kết hợp quyền thanh toán, và nhiều nút/tab chưa đồng bộ với quyền backend. Các phát hiện dưới đây còn mở; đợt này bổ sung bằng chứng và kiểm thử đánh giá, không thay đổi quyền hay dữ liệu nghiệp vụ.

## Cơ sở và giới hạn kiểm chứng

- Đã đọc tài liệu quản trị đa bãi, provision account, nhân sự, chuẩn backend; đối chiếu migration, policy quyền hiệu lực, controller, use case, repository, route guard, sidebar và các màn hình liên quan.
- [Quyền được gán](role-permissions.json) được đọc trực tiếp từ PostgreSQL local `vehicle_management_db`, container `vehicle-pgvector`, cổng 5434. Chỉ xuất cấu hình role/permission, không xuất hồ sơ hay thông tin đăng nhập.
- [Quyền hiệu lực](effective-permissions.json) do **policy Java thật** tính trên các tài khoản giả đã hoàn tất hồ sơ, được duyệt, tài khoản/role hoạt động, có tổ chức/phân công hợp lệ khi cần. Đây không phải snapshot đăng nhập của một tài khoản thật.
- Kiểm thử UI chạy Chromium với component/router thật và API fixture. Xác nhận khả năng hiển thị, điều hướng, mở form; không chứng minh request ghi thành công trên server thật.
- Kiểm thử Java tái hiện lỗi trên service/guard thật với persistence và notification mock. Không gửi thông báo thật, không thu tiền, không sửa database.
- Chưa thực hiện E2E qua Keycloak với năm tài khoản thật ở hai Partner khác nhau. Database local dừng trong lúc rà soát sâu hơn; không tự khởi động lại hay chỉnh dữ liệu. Chưa xác minh RLS của database đang chạy.
- Kiểm tra bao phủ cấu trúc phân quyền toàn dự án và đi sâu các luồng dưới đây, không phải chứng nhận an toàn cho mọi endpoint, tổ hợp trạng thái và race condition.

## Ma trận vai trò hiện tại

Số quyền không biểu thị role nào cao hơn. Quyền được gán còn chịu lọc lifecycle và phạm vi tài nguyên.

| Role | Được gán | Hiệu lực trong fixture đủ điều kiện | Trách nhiệm và phạm vi |
| --- | ---: | ---: | --- |
| SYSTEM_ADMIN | 125 | 120 | Quản trị nền tảng, Partner, khách hàng toàn sàn, role/permission, phí sàn, ví/đối soát, AI, hỗ trợ. Không mặc nhiên có quyền vận hành hoặc sửa nhân sự Partner. |
| PARTNER_ADMIN | 105 | 105 | Quản lý toàn bộ bãi **thuộc Partner của mình**, Manager/Employee của đơn vị, danh mục và giá, thiết bị, ca, vé, hóa đơn, ví đối tác. Không quản lý Partner khác hoặc quyền nền tảng. |
| PARKING_MANAGER | 124 | 124 | Vận hành và cấu hình các bãi được phân công; quản lý Employee trong phạm vi bãi; ca trực, thẻ, vé, thiết bị, giá theo quyền được cấp, voucher cấp bãi. Không quản lý Partner Admin/System Admin. |
| EMPLOYEE | 67 | 58 | Đọc dữ liệu phục vụ vận hành trong bãi được phân công; xem ca của mình, check-in/out, xử lý mất thẻ/gán thẻ và thu phí theo quyền. Nghiệp vụ theo ca phải yêu cầu ca đang mở. Không quản lý tài khoản, nhân sự, cấu trúc, role/permission. |
| CUSTOMER | 43 | 43 | Hồ sơ, xe, vé, lịch sử gửi xe, đặt chỗ, ví và hỗ trợ của chính mình. |

Quyền `_OWN` liên quan khách hàng được lọc khỏi các tài khoản nội bộ không có customer lifecycle. Vì vậy không được dùng trực tiếp danh sách quyền gán ở bảng role để quyết định UI; phải dùng `current-access.permissionCodes`.

Các file `*-navigation.json` ghi menu thực tế sau khi mở từng nhóm sidebar. Fixture EMPLOYEE có 16 liên kết, Manager 23, Partner 22, System Admin 16. CUSTOMER không dùng admin sidebar. Số menu chỉ mô tả cấu hình hiện tại, không đồng nghĩa mọi mục đều đúng nghiệp vụ.

## Ai quản lý ai

| Chủ thể | Quản lý tài khoản | Quản lý nhân sự và phân công |
| --- | --- | --- |
| SYSTEM_ADMIN | SYSTEM_ADMIN, PARTNER_ADMIN, CUSTOMER theo scope nền tảng | Đọc nhân sự nền tảng; code quản lý Employee không cho sửa nhân sự Partner. Có bất nhất list/detail Manager ở F10. |
| PARTNER_ADMIN | PARKING_MANAGER và EMPLOYEE cùng đơn vị | Tạo/phân công Manager; quản lý Employee, phân công/điều chuyển Employee giữa các bãi trong đơn vị. Duyệt onboarding Manager bị giới hạn người tạo tài khoản. |
| PARKING_MANAGER | EMPLOYEE trong scope được backend kiểm tra | Quản lý Employee đã thuộc bãi mình quản lý; quản lý ca. API cho phép gán Employee hợp lệ vào bãi mình quản lý nhưng UI chưa cung cấp đầy đủ, xem F09. |
| EMPLOYEE | Không có quyền provision account | Chỉ xem ca của mình và vận hành theo phân công. |
| CUSTOMER | Không có quyền provision account | Không quản lý nhân sự. |

Nguồn code chính: `ProvisionedAccountUseCaseImpl.managedTargetRolesForCurrentAccount`, `EmployeeAccessGuard`, `ParkingLotScopeResolver`, `ParkingLotAssignmentUseCaseImpl`, `InternalEmployeeApprovalAccessGuard`. Duyệt onboarding Employee hiện kiểm tra đơn vị chung đối với reviewer có quyền và scope hợp lệ; không nên mặc định việc duyệt hồ sơ đồng nghĩa Employee đã được gán vào bãi của reviewer.

## Khi nào sidebar được kích hoạt

Backend quyết định `accessState` theo thứ tự: trạng thái tài khoản, trạng thái role, hồ sơ/onboarding và phê duyệt, trạng thái customer/employee, scope tổ chức/bãi, cuối cùng quyền hiệu lực. Frontend chỉ hiển thị admin sidebar khi trạng thái truy cập là `ACTIVE`, rồi lọc từng mục theo quyền.

| Trạng thái | Manager và Employee |
| --- | --- |
| Chưa hoàn tất hồ sơ | Chỉ hồ sơ; không mở chức năng nghiệp vụ. |
| Đã gửi, đang chờ duyệt | Không mở sidebar nghiệp vụ. |
| Đã duyệt nhưng chưa có phân công hiệu lực | `ORGANIZATION_ACCESS_REQUIRED`; ẩn sidebar; hiện cảnh báo chờ phân công. |
| Phân công tương lai hoặc hết hạn | Chưa đủ điều kiện scope tại thời điểm kiểm tra. |
| Đủ hồ sơ, phê duyệt, trạng thái hoạt động và phân công hiệu lực | Có thể mở sidebar theo quyền hiệu lực. |
| Employee chưa mở ca | Có thể xem sidebar sau khi đủ scope; check-in/out còn phải qua kiểm tra ca đang mở. Thu phí đang có lỗi F03. |
| Bãi đình chỉ vận hành | Không đồng nghĩa mất toàn bộ sidebar; guard nghiệp vụ phải chặn thao tác vận hành tại bãi đó. |

Phân công Manager `PENDING_ACCEPTANCE` chưa được xem là phân công `ACTIVE`. API tiếp nhận bàn giao tồn tại nhưng chưa thấy UI tương ứng; các API gán Manager cũ mà UI hiện dùng chủ yếu đi theo luồng gán trực tiếp. Đây là khoảng trống cần kiểm thử riêng nếu bật yêu cầu tiếp nhận bàn giao.

## Các phát hiện ưu tiên

### F01 P1 Hồ sơ cá nhân chưa giới hạn theo tổ chức hoặc bãi

`USER_PROFILE_READ_ALL` đang được cấp và vẫn có hiệu lực với EMPLOYEE/PARKING_MANAGER. `UserProfileUseCaseImpl.getUserProfiles` chỉ kiểm tra quyền rồi gọi `findAll(status, keyword)`; `getUserProfileById` cũng chỉ kiểm tra quyền. Specification chỉ lọc trạng thái xóa, keyword/status, không có tổ chức/bãi/quan hệ sở hữu. DTO trả cả ngày sinh, điện thoại, địa chỉ và CCCD.

Đã tái hiện bằng hai unit test: actor Employee có scope bãi hợp lệ vẫn đọc được hồ sơ bất kỳ và danh sách hồ sơ giả không liên quan. Đây là lỗi phạm vi ở tầng ứng dụng; chưa gửi request tới API thật bằng tài khoản Employee.

**Cần sửa:** scope hóa hồ sơ theo quan hệ tài nguyên được phép hoặc giới hạn endpoint toàn sàn cho quyền quản trị phù hợp; giữ endpoint hồ sơ bản thân riêng. Thêm ca kiểm thử Employee/Manager đọc hồ sơ Partner khác và target ID không liên quan.

Nguồn: `UserProfileUseCaseImpl.java:79`, `UserProfilePersistenceAdapter.java:63`, `UserProfileSpecifications.java:16`, `UserProfileAdminResponse.java:20`. Bằng chứng: `RoleScopeAuditTest` gồm hai test tên bắt đầu `assignedEmployeeCan`.

### F02 P1 Manager có thể phát hành thông báo toàn sàn

Database cấp đủ quyền `BROADCAST_NOTIFICATION_*_ALL` cho PARKING_MANAGER. `BroadcastAnnouncementUseCaseImpl` đọc/sửa/phát hành theo ID và quyền chung, không kiểm tra creator, Partner hay bãi. Người nhận có thể là `ALL_ACTIVE_ACCOUNTS`; repository chọn tài khoản theo trạng thái/role mà không có tenant scope.

Unit test xác nhận Manager được phân công một bãi vẫn phát hành thông báo giả do tài khoản khác tạo, với `allActiveAccounts=true`. UI cũng cho Manager chọn “Tất cả tài khoản đang hoạt động”. Xem [ảnh tái hiện](manager-global-broadcast-audience.png). Bộ gửi thông báo được mock, không gửi tới người dùng thật.

**Cần sửa:** xác định rõ thông báo toàn sàn chỉ do quản trị nền tảng quản lý; nếu Manager cần thông báo bãi, bổ sung phạm vi người nhận và kiểm tra ownership của thông báo. Không chỉ ẩn nút frontend.

Nguồn: `BroadcastAnnouncementUseCaseImpl.java:61,112`, `AccountRepository.java:213`, `AnnouncementManagementPage.tsx:174`. Test: `assignedManagerCanPublishGlobalAnnouncementCreatedByAnotherAccount`.

### F03 P1 Kết hợp quyền của Employee bỏ qua điều kiện ca mở khi thu phí

EMPLOYEE hiện có đồng thời `PARKING_SCOPE_ASSIGNED` và `PARKING_SCOPE_SHIFT`. `PaymentAccessGuard.resolveVisibleParkingLotIds` xét scope assigned trước rồi trả về ngay; nhánh gọi `findOpenShiftParkingLotIds` không chạy. Migration `V20260926260000` quy định chỉ đọc/ghi thanh toán tại bãi có ca đang mở, nhưng test cũ chỉ truyền `PARKING_SCOPE_SHIFT`, bỏ sót tổ hợp quyền thực tế.

Đã bổ sung test tái hiện với cả hai quyền. `ensureCanRecordPayment` chấp nhận hóa đơn thuộc bãi được phân công mà không hỏi nguồn dữ liệu ca đang mở. Guard này được dùng trong thanh toán trực tiếp và SePay. Các kiểm tra hóa đơn/số tiền vẫn tồn tại; lỗi được xác nhận là thiếu điều kiện ca, không phải bỏ qua mọi kiểm tra thanh toán.

**Cần sửa:** ưu tiên điều kiện theo ca hoặc lấy giao của scope phân công và scope ca; bổ sung test ca chưa mở, hết giờ, bãi khác, cả hai quyền cùng tồn tại.

Nguồn: `PaymentAccessGuard.java:67`, `PaymentUseCaseImpl.java:66`, `SepayPaymentUseCaseImpl.java:117`. Test: `employeeWithBothAssignedAndShiftScopeBypassesOpenShiftPaymentCheck`.

### F04 P1 Hàng đợi avatar có quyền chung nhưng thiếu phạm vi

EMPLOYEE thấy menu “Duyệt ảnh đại diện” do có `USER_PROFILE_READ_ALL`, dù không có quyền duyệt. `AvatarModerationUseCaseImpl.getAvatarApprovals` đọc toàn bộ theo status, không scope. Manager có thêm `USER_PROFILE_UPDATE_ALL`; approve/reject theo ID không kiểm tra chủ hồ sơ thuộc Partner/bãi nào.

**Cần sửa:** phân biệt đọc hồ sơ và đọc hàng đợi duyệt; áp scope người duyệt cho cả list và approve/reject. Việc chỉ bỏ menu của Employee không xử lý lỗ hổng backend.

Nguồn: `AvatarModerationUseCaseImpl.java:124–160`, `AvatarModerationController.java:27–44`, `routePermissions.ts:54`, snapshot [menu Employee](EMPLOYEE-navigation.json). Phát hiện qua đọc xuyên suốt controller/use case; chưa có API E2E chéo Partner.

### F05 P1 Employee chưa thể mở ca qua giao diện hiện có

Role có `SHIFT_OPEN_OWN`; backend cung cấp API mở ca và check-in/out yêu cầu ca mở. Nhánh `ownShiftView` trong `ShiftSchedulePage` chỉ hiển thị ID ca, ID phân công và trạng thái; chỉ có nút làm mới. Không có thao tác mở ca. Trang quản lý còn thông báo thao tác mở/đóng ca chưa được gắn form đầy đủ.

Xem [giao diện Ca trực của tôi](employee-own-shift.png). **Cần sửa:** nối API mở ca và dữ liệu thời gian/bãi của ca, kiểm tra phân công đúng ca và các điều kiện backend. Đây là thiếu luồng vận hành, không nên xử lý bằng cấp thêm quyền quản trị cho Employee.

Nguồn: `ShiftSchedulePage.tsx:1530`, `ShiftUseCaseImpl.java:312`, `EmployeeParkingLotAssignmentRepository.java:63`. Đã tái hiện bằng UI fixture có phân công.

### F06 P1 Role tùy chỉnh giống Employee có thể bỏ qua guard ca trực

`ParkingSessionAccessGuard.ensureCanOperateParkingLot` chọn guard Employee bằng so sánh chuỗi role code đúng `EMPLOYEE`. Một role tùy chỉnh có cùng quyền assigned/shift/check-in đi sang guard vận hành tổ chức, không gọi guard kiểm tra ca Employee. Điều này mâu thuẫn với cách cấp quyền theo permission và hỗ trợ custom role của dự án.

Đã tái hiện trên guard thật với role giả `EMPLOYEE_CUSTOM`; test xác nhận `EmployeeParkingLotAccessGuard` không được gọi. Chưa xác minh tài khoản custom role thật đang tồn tại.

**Cần sửa:** quyết định yêu cầu ca theo capability/scope và hồ sơ nhân sự, không chỉ tên role. Nên dùng cùng quy tắc với payment sau khi sửa F03.

Nguồn: `ParkingSessionAccessGuard.java:40`. Test: `customEmployeeRoleWithShiftScopeBypassesEmployeeShiftGuard`.

### F07 P2 Sidebar ẩn đúng nhưng tab và liên kết bên trong vẫn lộ

EMPLOYEE không có quyền mở trang thiết kế sơ đồ; sidebar đã ẩn mục này. `ParkingLotWorkspaceHeader` vẫn render tất cả tab, và `ParkingLotOverviewPage` vẫn render liên kết nhanh tới thiết kế sơ đồ. Người dùng thấy đường dẫn không thể sử dụng; route guard chuyển về trang được phép.

Xem [ảnh Employee thấy liên kết thiết kế sơ đồ](employee-unauthorized-layout-link.png). **Cần sửa:** áp cùng `canAccessAdminRoute` cho tab và liên kết nhanh; khi kiểm tra phải bỏ query string khỏi path trước khi tra permission map.

Nguồn: `ParkingLotWorkspaceHeader.tsx:44,102`, `ParkingLotOverviewPage.tsx:682`, `routePermissions.ts:29`. Đã tái hiện bằng trình duyệt.

### F08 P2 Nút thao tác chưa kiểm tra đúng quyền con

| Màn hình | Hiện trạng đã xác nhận | Điều kiện nên dùng |
| --- | --- | --- |
| Quản lý thẻ | Employee vẫn thấy Cấp thẻ mới/Cấp hàng loạt bị disable | Ẩn khi không có `CARD_CREATE_ALL`; disable chỉ dành cho dữ liệu/trạng thái thao tác. |
| Ca trực | Actor chỉ có quyền đọc vẫn thấy Sinh lịch tuần và Duyệt lịch enabled | `SHIFT_CREATE_ALL` và `SHIFT_UPDATE_ALL` riêng từng nút. |
| Voucher | Actor chỉ có `VOUCHER_READ_ALL` vẫn mở form tạo và thấy Lưu voucher enabled | Tách `VOUCHER_CREATE_ALL` và `VOUCHER_UPDATE_ALL`; bảo vệ cả handler. |
| Tài khoản | Nút tạo/đổi trạng thái vẫn render rồi disable khi thiếu quyền | Ẩn theo quyền; thêm điều kiện target role thuộc phạm vi quản lý. |
| Nhân viên | Nút sửa/kích hoạt/đình chỉ render rồi disable theo quyền | Ẩn khi thiếu quyền hoặc target không thuộc phạm vi quản lý. |
| Duyệt vé | Một số nhánh hướng dẫn render nút “Không có quyền duyệt/từ chối” | Bỏ thao tác không có quyền; hướng dẫn chỉ trỏ vào thao tác hợp lệ. |

Ba dòng đầu đã tái hiện bằng Chromium: [thẻ](employee-card-disabled-actions.png), [ca chỉ đọc](read-only-shift-write-actions.png), [voucher chỉ đọc](read-only-voucher-create.png). Các dòng còn lại được xác nhận bằng JSX. Backend vẫn cần chặn request tự gửi; ẩn nút chỉ sửa trải nghiệm.

Nguồn: `CardManageHeader.tsx:23`, `ShiftSchedulePage.tsx:1578`, `VoucherManagementPage.tsx:281`, `AccountListPage.tsx:814,1038`, `EmployeeListPage.tsx:1287`, `SubscriptionApprovalPage.tsx:440`.

### F09 P2 Manager có API gán Employee nhưng UI chỉ dành cho Partner

`ParkingLotAssignmentUseCaseImpl.ensureCanAssignEmployeeAtLot` cho phép Manager đang quản lý bãi gán Employee hợp lệ cùng đơn vị. Nhưng `ShiftSchedulePage.canAssignEmployeeToParkingLot` và tab phân công bắt buộc `PARKING_SCOPE_PARTNER`, nên Manager không có thao tác tương ứng.

Không thể sửa chỉ bằng thêm `PARKING_SCOPE_ASSIGNED` vào điều kiện UI: endpoint đọc phân công theo organization hiện yêu cầu quyền truy cập organization và không phục vụ Manager; danh sách Employee cũng chỉ trả người đã thuộc scope bãi. Cần thiết kế nguồn chọn Employee/phân công theo bãi để không mở rộng đọc toàn bộ đơn vị.

Đã tái hiện UI Manager không có nút/tab phân công. **Cần thống nhất:** Partner có thể phân công và điều chuyển giữa bãi; Manager được gán Employee nào vào bãi mình cần theo quy tắc nghiệp vụ đã chốt, rồi đồng bộ API đọc, API ghi và UI.

Nguồn: `ParkingLotAssignmentUseCaseImpl.java:253,290,406`, `ShiftSchedulePage.tsx:1335–1340`, `EmployeeAccessGuard.java:153`.

### F10 P2 Platform nhìn thấy Manager trong list nhưng không mở được detail theo cùng chính sách

`readableTargetRolesForCurrentAccount` thêm PARKING_MANAGER cho platform ở API danh sách, nhưng API detail dùng `getManagedAccount` và chỉ chấp nhận nhóm target được quản lý (SYSTEM_ADMIN, PARTNER_ADMIN, CUSTOMER). Frontend lấy `ACCOUNT_UPDATE_ALL` để bật quản lý account hiện chọn, không kiểm tra target có thuộc nhóm được quản lý hay không.

**Cần sửa:** tách quyền đọc target khỏi quyền sửa target; UI phải nhận hoặc tính capability trên từng bản ghi. Không mở quyền sửa Manager cho System Admin chỉ để tránh 403 nếu nghiệp vụ chỉ cho xem.

Nguồn: `ProvisionedAccountUseCaseImpl.java:125,134,303,327,353`, `AccountListPage.tsx:711`. Xác nhận qua call chain, chưa tái hiện E2E trên bản ghi thật.

### F11 P2 Employee bị hiển thị như người phụ trách bãi

`ParkingLotOverviewPage` gán `isParkingManager` từ `PARKING_SCOPE_ASSIGNED`. Employee cũng có permission này, nên UI báo “Bạn đang được phân công phụ trách bãi xe này” và có thể tính hạng mục người phụ trách đã hoàn thành dù dữ liệu Manager rỗng.

Xem phần “Người phụ trách” trong [ảnh Employee](employee-unauthorized-layout-link.png). **Cần sửa:** dùng thông tin phân công quản lý/capability đúng để xác định trách nhiệm; không suy ra chức vụ Manager chỉ từ scope assigned.

Nguồn: `ParkingLotOverviewPage.tsx:160,270,674`.

### F12 P2 Tài liệu cấp tài khoản còn dùng phân cấp cũ

`docs/backend-account-provisioning-rules.md` vẫn ghi System Admin quản lý System Admin/Manager, Manager quản lý Employee/Customer và không có Partner Admin. Code hiện dùng mô hình phân cấp trong bảng “Ai quản lý ai”; tài liệu `saas-multi-parking-governance.md` đã mô tả Partner nhưng tài liệu cũ chưa được đánh dấu hết hiệu lực.

**Cần sửa:** cập nhật hoặc đánh dấu thay thế tài liệu cũ; chọn một ma trận quyền/phạm vi làm chuẩn. Không sửa code để quay về quy tắc cũ chỉ vì tài liệu này còn tồn tại.

### F13 P2 Mở ca lại phát thông báo ca bị hủy

Sau khi `shiftPolicy.open` và kích hoạt các phân công, `ShiftUseCaseImpl.openShift` lưu ca rồi gọi `notifyEmployeeShiftCancelled`. Hàm đó phát `SHIFT_CANCELLED`, tiêu đề “Ca trực đã hủy”. Employee có thể nhận trạng thái ngược với thao tác vừa thực hiện.

**Cần sửa:** dùng notification mở ca thích hợp và test nội dung/sự kiện khi mở, đóng, hủy ca. Phát hiện tĩnh tại `ShiftUseCaseImpl.java:372–374,1091`; chưa gửi thông báo thật.

## Những phần đã có cơ chế đúng

- `current-access` là nguồn quyền frontend; lỗi tải quyền ban đầu không được suy diễn thành quyền từ profile/cache. Bộ test lifecycle kiểm tra thu hồi quyền, đổi actor, khóa tài khoản và refresh.
- Sidebar lọc mục và nhóm theo quyền; chờ onboarding/phân công thì ẩn. Cảnh báo chờ phân công đã có cho Manager/Employee.
- Guard bãi dùng scope platform/partner/assigned; Partner không mặc nhiên nhìn thấy Partner khác.
- Quản lý Employee dùng phạm vi bãi của Manager và phân công Employee; việc cùng role không tự cho phép quản lý target.
- Duyệt Manager có kiểm tra tài khoản tạo hồ sơ; không chỉ dựa vào tổ chức chung.
- Customer có các guard ownership riêng cho xe, vé, hóa đơn/thanh toán và ví. Không có phát hiện vượt ownership mới trong các guard đã đọc và unit test đã chạy; vẫn cần E2E hai khách hàng khác nhau.
- UI thiết bị và nhiều thao tác khách hàng đã tách quyền read/create/update/delete; đây là mẫu có thể dùng để đồng bộ các màn còn thiếu.

## Kết quả kiểm thử và cách chạy lại

- `npm test`: toàn bộ chuỗi kiểm tra frontend hiện có chạy qua. Phần lớn là kiểm tra tĩnh nên không bao phủ hết quyền con.
- `tests/auth-access.behavior.spec.ts`: 31 ca Chromium chạy qua.
- Bộ audit UI riêng: 14 ca xác nhận menu/lifecycle và tái hiện hành vi hiện tại. **Test tái hiện lỗi chạy qua có nghĩa lỗi đã được tái hiện, không phải đã sửa.** Chi tiết tại [ui-results.json](ui-results.json).
- Bộ backend được chọn: 167 test regression và 6 test audit, tổng cộng **173 test / 27 lớp**, không failure/error/skipped. Bộ `RoleScopeAuditTest` gồm 5 ca tái hiện hành vi và 1 ca xuất quyền hiệu lực. Chi tiết tại [backend-tests.json](backend-tests.json).

Các kết quả trên ghi nhận thời điểm trước sửa. Bộ chạy audit UI dùng một lần đã được dọn; assertion backend đã chuyển thành kiểm thử hồi quy bảo mật. Dùng hướng dẫn trong [báo cáo sửa lỗi](FIXES.md#chạy-lại) để chạy bộ kiểm thử hiện tại.

Fixture không dùng mật khẩu/token thật. File JSON quyền và ảnh trong báo cáo này là bằng chứng lịch sử; `effective-permissions.json` được tạo từ policy Java tại thời điểm đánh giá.

## Trình tự xử lý đề xuất

1. Chặn vượt scope hồ sơ, avatar, broadcast và điều kiện ca khi thanh toán; kiểm thử âm trên hai Partner/hai bãi/hai khách hàng.
2. Hoàn thiện mở ca và phân công Employee; thống nhất quyền gán nhân sự của Manager so với Partner.
3. Lọc đồng bộ sidebar, tab, liên kết nhanh, nút, modal và các API phụ theo quyền hiệu lực; giữ disable cho điều kiện dữ liệu/trạng thái.
4. Chuẩn hóa quyền đọc/sửa từng target account và sửa thông báo mở ca.
5. Cập nhật tài liệu phân cấp, biến ma trận quyền thành kiểm thử tự động; chạy E2E với tài khoản thật sau khi sửa.

Tiêu chí nghiệm thu: thiếu quyền thì thao tác không có trong UI và không phát request phụ trái quyền; gửi trực tiếp request vẫn bị chặn; có quyền nhưng sai Partner/bãi/ca/ownership vẫn bị chặn; chờ duyệt/chờ phân công có giải thích rõ; quyền bị thu hồi cập nhật cả menu lẫn trang đang mở.
