# Sửa phân quyền nội bộ ngày 28/11/2026

Đã triển khai các thay đổi cho 13 phát hiện trong báo cáo đánh giá. Chính sách đã thống nhất: thông báo toàn sàn chỉ dành cho SYSTEM_ADMIN; Partner Admin phân công/điều chuyển nhân sự giữa bãi; Manager quản lý Employee và ca trong bãi được giao.

## Thay đổi theo phát hiện

| Phát hiện | Thay đổi đã triển khai |
| --- | --- |
| F01 Hồ sơ cá nhân | API thư mục hồ sơ đầy đủ yêu cầu thêm `PARKING_SCOPE_PLATFORM` ở application use case. Thu hồi quyền hồ sơ đầy đủ mặc định của nhân sự vận hành; các endpoint hồ sơ bản thân và Customer/Employee có scope tiếp tục phục vụ nghiệp vụ. |
| F02 Thông báo toàn sàn | Policy quyền hiệu lực loại `BROADCAST_NOTIFICATION_*` khỏi mọi role ngoài SYSTEM_ADMIN, kể cả custom/stale grants. Migration vô hiệu hóa các grant đó; frontend lọc cùng điều kiện và bổ sung menu Thông báo toàn sàn cho System Admin. Người nhận vẫn xem thông báo được phát hành cho mình. |
| F03 Thanh toán theo ca | Nhánh scope ca được xét trước; quyền thanh toán lấy giao giữa bãi được phân công và bãi có ca đang mở hợp lệ. Adapter dùng truy vấn vận hành hiện có, kiểm tra thời gian, trạng thái nhân sự/membership/tổ chức/phân công và bãi đình chỉ. |
| F04 Avatar | Thêm `AVATAR_APPROVAL_READ_ALL`, `AVATAR_APPROVAL_REVIEW_ALL`; list và review kiểm tra phạm vi qua EmployeeAccessGuard. System Admin có scope nền tảng; Partner/Manager chỉ xử lý nhân sự thuộc phạm vi. Employee không có menu mặc định. |
| F05 Mở ca | API ca của tôi trả cả phân công chưa mở và DTO riêng gồm bãi, mã ca, thời gian, trạng thái; không trả audit fields của admin. UI có form tiền đầu ca/ghi chú. Backend luôn kiểm tra phân công bãi, ca của nhân viên, trạng thái và thời gian khi mở. |
| F06 Role tùy chỉnh | Guard check-in/out chọn điều kiện ca theo `PARKING_SCOPE_SHIFT`, không so tên role EMPLOYEE. Quyền scope ca yêu cầu lifecycle nhân sự hoạt động. |
| F07 Tab/liên kết | Tab workspace và liên kết nhanh dùng chung route permission; không hiện thiết kế sơ đồ khi thiếu quyền. Tổng quan bãi và lịch ca bỏ các request phụ khi thiếu quyền tương ứng. |
| F08 Quyền con | Ẩn thao tác thiếu quyền ở thẻ, lịch ca/phân công ca, mẫu ca/quy tắc, voucher, tài khoản, nhân viên và duyệt vé. Form/handler có kiểm tra quyền; mẫu ca/quy tắc tách quyền xóa và kích hoạt lại. Disable giữ cho điều kiện dữ liệu/trạng thái. |
| F09 Phân công Employee | API gán Employee vào bãi yêu cầu Partner có quyền quản lý tổ chức. Manager không tự gán/điều chuyển giữa bãi; UI hiện hành đã dùng chính sách này. |
| F10 Đọc/sửa account | API detail dùng tập target được đọc giống API list. System Admin đọc được Manager nhưng cập nhật vẫn bị chặn; UI không hiện nút đổi trạng thái/role cho target này. |
| F11 Người phụ trách | Không suy ra người phụ trách hoặc hoàn tất cấu hình từ `PARKING_SCOPE_ASSIGNED`. Dùng dữ liệu phân công đã tải; không hiện Employee như Manager. Yêu cầu kích hoạt bãi còn cần quyền cấu hình topology, Employee chỉ đọc không được gửi yêu cầu này. |
| F12 Tài liệu | Cập nhật tài liệu cấp tài khoản và quản trị đa bãi, thay thế phân cấp chưa có Partner. |
| F13 Thông báo mở ca | Mở ca phát `SHIFT_CHANGED`, nội dung “Ca trực đã mở”, thay vì gửi thông báo hủy ca. |

Cảnh báo chờ phân công trên trang hồ sơ của Manager/Employee được giữ lại. Chưa có phân công hiệu lực vẫn không mở sidebar nghiệp vụ; chưa mở ca không ngăn xem lịch và thao tác mở ca hợp lệ.

## Kiểm chứng

- **255 test backend / 36 lớp**, không lỗi, không bỏ qua. [Kết quả theo lớp](after-fix/backend-tests.json).
- Trong đó có 12 test tích hợp PostgreSQL: migration (1), lifecycle onboarding nội bộ (3), quyền hiệu lực Partner (8). Migration chạy trên database/container tạm, áp dụng toàn bộ 169 migrations và kiểm tra chạy lại migration mới không trùng quyền.
- **50 test Chromium**, gồm 31 ca lifecycle truy cập và 19 ca hồi quy phân quyền. [Kết quả UI](after-fix/ui-results.json). Có cả thao tác mở ca hợp lệ, ca hết giờ, quyền chỉ đọc, target Manager chỉ đọc và grant broadcast cũ.
- `npm test`, `npm run lint` và `npm run build` chạy qua. Build còn cảnh báo bundle lớn và CSS/tài nguyên hiện có; không có lỗi biên dịch.
- `git diff --check` chạy qua.

Unit tests dùng service/guard thật với port mock; UI dùng router/component thật và API fixture. Chưa chạy E2E qua Keycloak với các tài khoản người dùng thật hoặc thử giao dịch thật. Không gửi thông báo hoặc thu tiền thật.

Ảnh sau sửa: [ca của Employee](after-fix/employee-own-shift.png), [thẻ thiếu quyền tạo](after-fix/employee-card-disabled-actions.png), [ca chỉ đọc](after-fix/read-only-shift-write-actions.png), [voucher chỉ đọc](after-fix/read-only-voucher-create.png), [tab bãi của Employee](after-fix/employee-unauthorized-layout-link.png).

## Migration và triển khai

File: `src/main/resources/db/migration/V20261128120000__harden_internal_role_permissions.sql`.

Migration đã được kiểm tra trên PostgreSQL tạm; chưa chạy vào database ứng dụng đang dùng. Khi triển khai/khởi động backend có Flyway, cần áp dụng migration này cùng bản frontend mới. Backend chặn broadcast ngoài SYSTEM_ADMIN và hồ sơ ngoài scope ngay cả khi grant cũ còn tồn tại; quyền avatar mới chỉ khả dụng sau migration. Không cấp bù quyền quản trị cho Employee để mở ca.

Snapshot `role-permissions.json` và `effective-permissions.json` là bằng chứng trước sửa. `effective-permissions-after-fix.json` do policy Java tính cho fixture sau khi mô phỏng thay đổi grant của migration; không phải dữ liệu đăng nhập thật.

## Chạy lại

Từ thư mục backend:

```powershell
./mvnw.cmd '-Drole.scope.audit=true' '-Dtest=RoleScopeAuditTest,*AccessGuardTest,*ScopeResolverTest,*AccessPolicyTest,PermissionAccessPrerequisitePolicyTest,CurrentAccessStatePolicyTest,ProvisionedAccountUseCaseImplTest,ParkingLotAssignmentUseCaseImplTest,PermissionFirstArchitectureTest,AvatarModerationUseCaseImplTest,UserProfileUseCaseImplTest,ShiftUseCaseImplTest,ShiftAssignmentUseCaseImplTest,EmployeeManagerReadUseCaseImplTest,AccountAuthorizationPersistenceAdapterTest,InternalOnboardingEffectiveAccessPersistenceTest,PartnerAdminEffectiveAccessPersistenceIT,ParkingLotUseCaseImplTest,EmployeePaymentScopePersistenceAdapterTest,InternalRolePermissionMigrationTest' test
```

Từ thư mục frontend:

```powershell
npm test
npm run lint
npm run build
npx playwright test tests/auth-access.behavior.spec.ts tests/role-permission.behavior.spec.ts
```

`RoleScopeAuditTest` giờ chứa assertion bảo mật chạy mặc định; chỉ ca xuất snapshot cần `-Drole.scope.audit=true`. Bộ chạy UI audit trước sửa và các log/bản sao tạm đã được dọn. Kết quả lịch sử được giữ trong báo cáo; dùng `tests/role-permission.behavior.spec.ts` để kiểm tra bản đã sửa.


## Bổ sung: quyền đọc vận hành và dữ liệu bãi mới

Các sửa đổi ở F07/F08 trước đó chưa đủ và đã gây hồi quy tổng quan bãi. Đợt bổ sung này xử lý riêng:

- Tổng quan dùng đúng `PARKING_SPACE_READ_ALL` để tải tầng/khu/cổng/làn. Không tính dữ liệu chưa tải hoặc bị lỗi thành 0/chưa cấu hình; không cho kích hoạt khi số liệu chưa sẵn sàng.
- Route, tab và liên kết Cấu trúc yêu cầu `PARKING_SPACE_READ_ALL`. Quyền xem/tạo bãi riêng lẻ không mở trang cấu trúc.
- Thêm các API chỉ đọc `/api/parking/operations`: tầng, khu, làn hoạt động, tổng hợp bãi, bản đồ tầng, ô đỗ, sơ đồ đã xuất bản và ID ô đang được phân bổ. Chấp nhận quyền đọc sơ đồ hoặc check-in/check-out, sau đó kiểm tra phạm vi tài nguyên bằng các use case hiện có. DTO vận hành không trả trường audit; trạng thái phân bổ chỉ trả ID ô, không trả hồ sơ khách hay vé.
- Check-in và sơ đồ trực tiếp sử dụng API vận hành cho cả Employee/Manager/Partner. Employee không được mở quyền cấu hình, xuất bản, đổi trạng thái ô hoặc phân bổ chỗ. Check-in/out thực tế vẫn giữ điều kiện ca mở đúng bãi.
- Tách trạng thái lỗi với dữ liệu rỗng; xóa dữ liệu cũ khi chuyển bãi và bỏ qua phản hồi đến muộn. Bãi mới chưa có tầng được hiển thị là rỗng sau khi API thành công. Không dùng bãi mẫu giả khi danh sách bãi chưa tải được.
- Bản đồ địa lý có thông báo lỗi, nút thử lại, cập nhật kích thước và cấu hình nguồn ảnh/dự phòng có attribution qua `.env.example`. Sơ đồ ô đỗ trực tiếp không phụ thuộc nguồn ảnh địa lý này.

Kiểm thử mới dùng UUID ngẫu nhiên, không phụ thuộc bãi HCMUTE. Bộ tích hợp PostgreSQL tạo tổ chức/bãi/tầng/khu/cổng/làn/ô mới, chạy qua controller có method security, kiểm tra ba role đọc được phạm vi hợp lệ, chặn ngoài tổ chức và không mở quyền cấu hình. Test chạy trên container tạm; không thay dữ liệu ứng dụng.

Kết quả bổ sung: **75 test backend** (có 8 test tích hợp PostgreSQL) và **66 ca trình duyệt khác nhau** (16 ca mới + 50 ca hồi quy cũ) đều đạt, không bỏ qua. `npm test`, TypeScript và production build chạy qua. [Tổng hợp gọn kết quả](after-fix/parking-followup-tests.json).

Chạy lại từ backend:

```powershell
./mvnw.cmd '-Dtest=ParkingOperationReadUseCaseImplTest,ParkingOperationReadPostgresIT,ParkingLevelUseCaseScopeTest,SpaceSummaryUseCaseTest,ParkingSpaceUseCaseScopeTest,ParkingLayoutUseCaseTest,OrganizationAccessGuardTest,ParkingLotScopeResolverTest,ParkingSessionAccessGuardTest,ParkingRealtimeSubscriptionGuardTest,PermissionFirstArchitectureTest' test
```

Chạy lại từ frontend:

```powershell
npm test
npm run lint
npm run build
npx playwright test tests/parking-operation-access.behavior.spec.ts tests/role-permission.behavior.spec.ts tests/auth-access.behavior.spec.ts
```

Không cần migration mới cho phần bổ sung. Cần khởi động lại backend với mã mới trước khi dùng frontend vì có endpoint mới.

### Hạn chế môi trường bản đồ địa lý

Tại thời điểm kiểm tra code, DNS mặc định của máy trả lỗi cho `tile.openstreetmap.org` sau khi xóa cache; truy vấn riêng tới DNS công cộng có kết quả. Sau yêu cầu sửa môi trường của người dùng, đã xác minh Wi-Fi vẫn nhận DNS qua DHCP (`123.23.23.23`, `123.26.26.26`), nhưng cả hai máy chủ trả NXDOMAIN cho tên miền ảnh. Chưa có bằng chứng cấu hình DNS từng bị xóa hoặc biết chính xác lúc dịch vụ DNS bắt đầu trả kết quả sai.

Đã thêm quy tắc Windows NRPT riêng cho `tile.openstreetmap.org` và `.tile.openstreetmap.org`, dùng `1.1.1.1` / `8.8.8.8`; giữ nguyên DNS Wi-Fi và file hosts. Quy tắc: `{1721B429-957E-40F3-BC40-1A52DEB32B6C}`. Kiểm tra sau sửa: phân giải qua hệ điều hành thành công; nguồn ảnh thật trả HTTP 200 `image/png`; Chromium tải và giải mã ảnh 256×256 trên tên miền chính và cả a/b/c. Đây là sửa cấu hình của máy hiện tại, không tự áp dụng cho máy khác. Bản sao cấu hình và script hoàn tác được giữ trong `.tmp/map-dns-repair-20261128/` ở gốc workspace.

Ứng dụng dùng URL chuẩn theo [OSM Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/). Nguồn dự phòng trong ứng dụng vẫn cần cấu hình URL và attribution hợp lệ nếu dùng; không cần thay nguồn ảnh để bản đồ hoạt động trên máy sau lần sửa DNS này.

## Bổ sung: bộ lọc làn trên trang Thiết bị

Nguyên nhân 403: trang Thiết bị gọi chuỗi API quản trị khu vực → cổng → làn, yêu cầu `PARKING_SPACE_READ_ALL`, trong khi nhân viên có `DEVICE_READ_ALL` để xem thiết bị. Dữ liệu phụ của bộ lọc đã phụ thuộc sai quyền quản trị cấu trúc.

- Thêm `GET /api/hardware/devices/lane-options?parkingLotId=...` (tham số bãi không bắt buộc), yêu cầu `DEVICE_READ_ALL` tại controller và use case. Danh sách chỉ thuộc các bãi được phép; bãi được chỉ định ngoài phạm vi trả 403. Phạm vi rỗng trả danh sách rỗng. DTO chỉ gồm thông tin làn/cổng/khu cần cho vị trí thiết bị, không có trường audit hay cấu hình.
- Truy vấn trực tiếp làn theo phạm vi bãi, không phụ thuộc bãi HCMUTE hoặc việc làn đã có thiết bị. Giữ làn bảo trì/đóng trong danh mục để lọc được thiết bị tại vị trí đó. Không thêm quyền cấu hình hoặc quyền check-in cho người chỉ đọc thiết bị.
- Trang Thiết bị dùng API mới, bỏ chuỗi API khu/cổng/làn quản trị. Bộ lọc đổi theo bãi và đặt lại lựa chọn làn khi chuyển bãi. Lỗi tải làn có cảnh báo rõ ràng, vẫn giữ danh sách thiết bị và cho tải lại; không hiển thị nhầm thành “Không liên kết làn”.

Kiểm chứng đợt này: **46 ca backend đạt** (`DeviceUseCaseImplTest`: 30, `ParkingOperationReadPostgresIT`: 16, bao gồm cả ca vận hành trước đó); **7 ca Chromium đạt**, không bỏ qua; `npm test` và `npm run build` (gồm TypeScript) thành công. Kiểm tra ba role EMPLOYEE/PARKING_MANAGER/PARTNER_ADMIN, quyền chỉ đọc thiết bị không có quyền cấu trúc/check-in, phạm vi bãi, làn bảo trì, bãi mới rỗng và tải lại sau lỗi. Backend tích hợp chạy PostgreSQL tạm với actor kiểm thử; trình duyệt dùng API fixtures, chưa phải kiểm thử đăng nhập Keycloak thực tế.

```powershell
# Backend
./mvnw.cmd '-Dtest=DeviceUseCaseImplTest,ParkingOperationReadPostgresIT' test
# Frontend
npx playwright test tests/device-lane-access.behavior.spec.ts
```

Không cần migration hoặc bổ sung permission vào role. Khởi động lại backend với mã mới và tải lại trang Thiết bị để dùng endpoint mới. Log và kết quả chạy tạm được dọn; giữ test hồi quy và báo cáo này.

## Bổ sung: nhân sự theo bãi và hai nhóm phân công

Bộ lọc Nhân viên trước sửa đọc ca trực hoặc `iam.member_parking_lot_scopes`, trong khi phân công mới đã chuyển sang `iam.parking_lot_management_assignments` và `people.employee_parking_lot_assignments`. Vì thế người đã được phân công nhưng chưa có ca trực có thể biến mất khỏi bộ lọc.

- Truy vấn mới đọc đúng nguồn theo role: Manager có phân công ACTIVE; Employee có phân công ACTIVE hoặc SCHEDULED đã đến hạn. Khoảng hiệu lực là `[effective_from, effective_to)`. Phân công chưa đến hạn, hết hạn, chờ tiếp nhận/xét duyệt, tạm dừng, đã hủy hoặc đã kết thúc không được coi là công tác hiện tại. Không dùng scope cũ hoặc ca trực lịch sử để đưa người đã hết phân công trở lại kết quả.
- Bắt buộc thành viên tổ chức còn ACTIVE trong đúng tổ chức sở hữu bãi; tiếp tục loại hồ sơ đã xóa/tài khoản chờ xóa và giữ nguyên kiểm tra quyền đọc cùng phạm vi bãi ở application layer. Không cấp thêm permission, không sửa dữ liệu phân công hoặc tạo migration.
- Tab Phân công bãi xe có hai nhóm: **Quản lý bãi** và **Nhân viên công tác**. Dùng chung tìm kiếm, lọc bãi và trạng thái. Nhóm Manager đọc API management-history hiện có, tính trạng thái theo thời gian và đưa thao tác Quản lý phân công về đúng bãi; Employee tiếp tục dùng luồng chuyển công tác riêng. Số liệu công tác Employee theo bãi đang chọn.
- Nhóm Manager chỉ tải khi có `ORGANIZATION_READ_ALL`; liên kết thao tác yêu cầu `ORGANIZATION_MEMBERSHIP_MANAGE_ALL` và quyền đọc tài khoản cần cho hộp thoại hiện có. Lỗi tải Manager có cảnh báo/thử lại riêng, không xóa nhóm Employee hoặc báo giả rằng chưa có Manager. Bỏ qua phản hồi của bãi cũ khi đổi bộ lọc.

Kết quả: **29 ca backend đạt** (12 PostgreSQL integration mới, 7 EmployeeAccessGuard, 10 EmployeeUseCase); **10 ca Chromium đạt**, không bỏ qua; `npm test`, TypeScript và production build thành công. Integration dùng database container tạm và actor kiểm thử; trình duyệt dùng API fixtures. Truy vấn mới cũng được chạy ở chế độ READ ONLY trên database đang dùng: bãi HCMUTE trả đúng 1 PARKING_MANAGER và 1 EMPLOYEE. Chưa thay phiên backend đang chạy hoặc thực hiện đăng nhập Keycloak E2E.

```powershell
# Backend
./mvnw.cmd '-Dtest=EmployeeLotFilterPostgresIT,EmployeeUseCaseImplTest,EmployeeAccessGuardTest' test
# Frontend
npx playwright test tests/workforce-lot-assignment.behavior.spec.ts
```

Khởi động lại backend để nhận truy vấn mới, tải lại frontend để nhận hai nhóm phân công. Không cần phân công lại nhân sự. Log/build report tạm của đợt kiểm thử được dọn, giữ test hồi quy và báo cáo này.

## Bổ sung: người phụ trách và đơn vị quản lý trong Tổng quan bãi

Nguyên nhân: frontend chỉ tải danh sách tài khoản Manager, tổ chức và phân công cho phạm vi Partner/Platform. Manager bị bỏ qua các API này, nên giao diện diễn giải dữ liệu chưa tải thành chưa có người phụ trách và bỏ mục này khỏi mẫu số hoàn tất.

- Thêm `GET /api/parking/parking-lots/{parkingLotId}/responsibility`, kiểm tra `PARKING_LOT_READ_ALL` và phạm vi bãi bằng use case hiện có trước khi truy vấn. DTO chỉ có tên tổ chức và danh sách accountId/tên hiển thị Manager; không trả hồ sơ, email, quyền hoặc dữ liệu audit.
- Chỉ lấy phân công ACTIVE đã đến hạn, chưa hết hạn, đúng tổ chức; Manager có tài khoản ACTIVE, role PARKING_MANAGER và membership ACTIVE trong tổ chức của bãi. Dùng MapStruct cho projection/domain/response.
- Tổng quan dùng API này cho mọi người được xem bãi, bỏ phụ thuộc API quản trị danh sách tài khoản/tổ chức. Không cấp thêm quyền gán/gỡ Manager. Trường hợp Partner trực tiếp phụ trách dựa trên trạng thái PARTNER_SUPERVISED; lỗi tải được hiển thị rõ và có nút Thử lại. Người phụ trách luôn được tính trong checklist khi checklist hiển thị, tránh 100% giả khi dữ liệu lỗi.
- Kiểm thử hoàn tất: **40 backend đạt** (17 PostgreSQL, 19 ParkingLotUseCase, 4 VoucherAccessGuard); **21 ca Chromium đạt** (5 ca người phụ trách + 16 ca vận hành hồi quy); `npm test`, TypeScript và production build đạt. PostgreSQL chạy container tạm; UI dùng API fixtures. Chưa xác minh E2E bằng đăng nhập thật. Lần kiểm tra bổ sung trên database đang dùng bị gián đoạn bởi hạn mức xét duyệt, không thay đổi dữ liệu ứng dụng.

```powershell
./mvnw.cmd '-Dtest=EmployeeLotFilterPostgresIT,ParkingLotUseCaseImplTest,VoucherAccessGuardTest' test
npx playwright test tests/parking-responsibility.behavior.spec.ts tests/parking-operation-access.behavior.spec.ts
```

Không cần migration hoặc phân công lại nhân sự. Khởi động lại backend với mã mới và tải lại frontend để sử dụng endpoint mới. Giữ test hồi quy; đã dọn log và kết quả trình duyệt tạm.
