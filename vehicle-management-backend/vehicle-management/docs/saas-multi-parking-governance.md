# SaaS đa bãi xe: mô hình vận hành, phân quyền và dữ liệu

> Trạng thái: Đã thống nhất về nghiệp vụ, chưa triển khai toàn bộ.  
> Cập nhật: 26/09/2026.

## 1. Mục tiêu

CoParking được phát triển thành nền tảng SaaS quản lý bãi xe thông minh. Nền tảng cung cấp phần mềm, tích hợp thiết bị và các dịch vụ dùng chung; các đơn vị vận hành bãi xe (Partner) đăng ký sử dụng để quản lý một hoặc nhiều bãi xe của mình.

Mục tiêu kiến trúc là bảo đảm dữ liệu, quyền vận hành và báo cáo của Partner A không bị lẫn với Partner B, đồng thời vẫn cho phép khách hàng dùng một tài khoản CoParking tại nhiều bãi.

## 2. Cây sở hữu và phạm vi

```text
CoParking Platform
├── System Admin
├── Partner A
│   ├── Partner Admin A
│   ├── Parking Lot A1
│   │   └── Parking Manager A1 -> Employees A1
│   └── Parking Lot A2
│       └── Parking Manager A2 -> Employees A2
└── Partner B
    ├── Partner Admin B
    └── Parking Lot B1
        └── Parking Manager B1 -> Employees B1

Customer: thuộc CoParking Platform, có thể giao dịch tại A1, A2, B1 hoặc các bãi công khai khác.
```

Các cấp có ý nghĩa riêng:

- **Platform**: CoParking vận hành hệ thống SaaS.
- **Partner / Organization / Tenant**: doanh nghiệp hoặc đơn vị vận hành đăng ký sử dụng dịch vụ.
- **Parking Lot**: một bãi xe vật lý thuộc đúng một Partner; một Partner có thể có nhiều bãi.
- **Topology**: cấu trúc nội bộ của một bãi, giữ theo mô hình hiện tại `ParkingLot -> Zone -> Gate -> Lane`.

Không đưa lại `ParkingSpace` vào core topology chỉ để hỗ trợ SaaS. Tính năng 3D là mô-đun tùy chọn; nếu được bật, dữ liệu sơ đồ/slot phục vụ điều phối sẽ gắn với một `ParkingLot` cụ thể.

## 3. Nguyên tắc phân quyền

Một quyền luôn được quyết định bởi hai thành phần:

1. **Role** trả lời: tài khoản được làm gì?
2. **Scope** trả lời: tài khoản được làm ở Partner hoặc bãi nào?

Frontend có thể cho người dùng chọn Partner/bãi đang làm việc, nhưng backend phải tự xác thực scope. Không tin `parkingLotId` chỉ vì nó được gửi từ client.

Mọi API quản trị theo bãi phải kiểm tra theo thứ tự:

1. Tài khoản có hoạt động không?
2. Tài khoản có quyền thao tác không?
3. Tài khoản có membership trong Partner của bãi không?
4. Tài khoản có scope đến bãi đó không?
5. Tài nguyên con có thực sự thuộc bãi đó không?

Nếu bất kỳ bước nào thất bại, trả về `403 Forbidden` và ghi audit log khi phù hợp.

## 4. Vai trò và trách nhiệm

| Vai trò | Scope | Trách nhiệm chính | Giới hạn bắt buộc |
|---|---|---|---|
| `SYSTEM_ADMIN` | Toàn Platform | Quản lý Partner Admin, duyệt đối tác, gói/loại phí SaaS, chiến dịch và voucher toàn sàn, khách hàng toàn sàn, ví, giao dịch ngân hàng, đối soát, dashboard toàn sàn | Không vận hành thường xuyên thay cho bãi; mọi truy cập hỗ trợ vào dữ liệu nhạy cảm phải có audit |
| `PARTNER_ADMIN` | Một Partner, toàn bộ bãi thuộc Partner | Đăng ký/quản lý bãi, vị trí bãi, giá vé theo từng bãi, tạo và phân công Manager, tra cứu nhân viên/khách có phát sinh tại Partner, báo cáo toàn Partner hoặc từng bãi, tham gia chiến dịch sàn, voucher cấp Partner, theo dõi đối soát doanh thu | Không xem Partner khác; không quản lý System Admin, role/permission nền tảng, gói SaaS hoặc số dư ví khách hàng |
| `PARKING_MANAGER` | Một hoặc nhiều bãi được Partner Admin gán | Quản lý zone/gate/lane, thiết bị, nhân viên, ca trực, voucher cấp bãi, khách đăng ký/giao dịch tại bãi, check-in/out, dashboard và báo cáo bãi; sử dụng 3D nếu bãi được bật tính năng | Không tạo Partner Admin, không quản lý hợp đồng/giá SaaS, không truy cập bãi ngoài scope, không tự thay đổi bảng giá nếu Partner không ủy quyền |
| `EMPLOYEE` | Bãi, gate/lane và ca trực được gán | Check-in/out, thu phí theo quyền, hỗ trợ khách, xử lý nghiệp vụ vận hành tại ca | Không cấu hình topology, giá, voucher, nhân sự hoặc báo cáo nhạy cảm |
| `CUSTOMER` | Hồ sơ, xe, ví và giao dịch của chính mình | Tìm bãi, xem bản đồ/khoảng cách/giá/tình trạng lấp đầy, đặt chỗ, mua vé, thanh toán, sử dụng ví | Không dùng admin portal |
| `GUEST` | Dữ liệu công khai | Xem bãi công khai, đăng ký và đăng nhập | Không có quyền nghiệp vụ |

System Admin tạo Partner Admin đầu tiên khi duyệt Partner. Partner Admin chỉ được tạo/gán Manager và Employee trong chính Partner của họ; không được tự nâng quyền thành System Admin.

## 5. Quy trình Partner gia nhập

```text
Đơn vị gửi đăng ký đối tác
        -> PENDING_REVIEW
System Admin kiểm tra và duyệt
        -> tạo Partner + mời Partner Admin kích hoạt tài khoản
Partner Admin hoàn thiện thông tin và tạo bãi
        -> ONBOARDING
Thiết lập topology, giá, thiết bị, Manager và nhân viên
        -> ACTIVE
```

Chi tiết:

1. Đại diện đơn vị gửi thông tin pháp lý, liên hệ, bãi ban đầu và nhu cầu sử dụng.
2. System Admin duyệt hoặc từ chối yêu cầu; quyết định được lưu lịch sử/audit.
3. Khi duyệt, hệ thống tạo Partner, cấp quyền `PARTNER_ADMIN` thông qua lời mời kích hoạt tài khoản, không gửi hoặc lưu mật khẩu thủ công.
4. Partner Admin tạo một hay nhiều `ParkingLot`, khai báo địa chỉ và tọa độ để khách có thể tìm trên bản đồ.
5. Partner Admin phân công `PARKING_MANAGER` cho từng bãi.
6. Manager cấu hình topology, thiết bị, nhân viên và ca trực cho bãi được gán.
7. Bãi chỉ được công khai/hoạt động khi đáp ứng cấu hình tối thiểu theo chính sách hệ thống.

## 6. Phân tách dữ liệu

### 6.1. Dữ liệu cấp Platform

- Account, Customer, Customer Vehicle, hồ sơ người dùng.
- Ví khách hàng, sổ cái ví, giao dịch ngân hàng, đối soát toàn sàn.
- Campaign và voucher toàn sàn.
- Gói SaaS, phí nền tảng, Partner và lịch sử duyệt Partner.

Customer là khách hàng của CoParking, không phải dữ liệu sở hữu riêng của một bãi. Partner/Manager chỉ được nhìn thấy khách có phát sinh quan hệ với bãi thuộc scope của họ.

### 6.2. Dữ liệu cấp Partner

- Thông tin pháp lý, người đại diện và cấu hình dùng chung của Partner.
- Các bãi thuộc Partner.
- Danh mục loại xe, loại vé và bảng giá/quy tắc giá của chính Partner. Partner Admin được cấu hình và vận hành toàn bộ bãi của Partner ngay cả khi chưa phân công Parking Manager. Giá gửi xe không phải phí dịch vụ SaaS của nền tảng.
- Voucher/chương trình do Partner tài trợ, có thể áp dụng một hoặc nhiều bãi được chọn.
- Báo cáo tổng hợp, doanh thu, phí nền tảng và đối soát thanh toán của Partner.

Quyết định chuyển đổi dữ liệu ngày 26/09/2026: sao chép bộ loại xe, loại vé, bảng giá và quy tắc giá hiện có thành dữ liệu khởi tạo **riêng cho từng Partner**. Partner mới cũng nhận bản sao khi được duyệt. Mỗi Partner sửa bản sao của mình; sửa giá ở Partner A không được đổi giá ở Partner B. Migration phải dùng quan hệ khóa ngoại/mã ổn định và chịu được số Partner khác nhau giữa các máy, không dựa vào UUID của một cơ sở dữ liệu local cụ thể. Bản ghi cũ đã phát sinh giao dịch phải giữ nguyên tham chiếu lịch sử.

Hồ sơ xe khách hàng vẫn thuộc Platform và có thể dùng ở nhiều Partner. Vì vậy loại xe riêng của Partner phải ánh xạ tới phân loại xe chuẩn dùng cho hồ sơ khách hàng; không được đổi trực tiếp `customer_vehicles.vehicle_type_id` sang bản sao của một Partner. Mọi API danh mục phải kiểm tra đồng thời quyền và `organization_id` trên backend. System Admin được xem xuyên Partner nhưng không sửa danh mục/giá của Partner khác.

### 6.3. Dữ liệu cấp Parking Lot

- Zone, gate, lane, thiết bị, nhân viên, ca trực và cấu hình vận hành.
- Bãi sử dụng danh mục và bảng giá thuộc Partner của mình; nếu cần giá khác nhau giữa các bãi cùng Partner thì bổ sung cấu hình/ghi đè theo `parking_lot_id`, không tạo giá toàn sàn.
- Voucher cấp bãi do Manager quản lý trong phạm vi bãi.
- Parking session, đặt chỗ, đăng ký vé, hóa đơn, thanh toán, sự kiện bãi và báo cáo bãi.

Các bản ghi lịch sử như session, reservation, subscription, invoice và payment cần lưu trực tiếp `parking_lot_id`. Không chỉ suy ra qua zone, vì topology có thể thay đổi nhưng lịch sử phải giữ đúng nơi phát sinh giao dịch.

## 7. Chiến dịch và voucher

| Cấp | Người quản lý | Phạm vi |
|---|---|---|
| Chiến dịch toàn sàn | System Admin | Các Partner/bãi đủ điều kiện hoặc tự đăng ký tham gia |
| Voucher cấp Partner | Partner Admin | Một hoặc nhiều bãi thuộc Partner |
| Voucher cấp bãi | Parking Manager | Một bãi được gán |

Mỗi chiến dịch/voucher phải thể hiện rõ:

- nguồn tài trợ: `PLATFORM` hoặc `PARTNER`;
- phạm vi áp dụng: toàn sàn, Partner hay danh sách bãi;
- điều kiện vé/đặt chỗ, thời gian hiệu lực, số lượt dùng;
- quy tắc cộng dồn và độ ưu tiên.

Mặc định không cộng dồn voucher. Partner có thể đăng ký hoặc rút khỏi chiến dịch toàn sàn, nhưng không được sửa nội dung, điều kiện hay ngân sách của chiến dịch do System Admin tạo.

## 8. Ví điện tử và đối soát

Ví khách hàng là ví CoParking cấp Platform. Partner không được trực tiếp sửa số dư ví khách hàng.

```text
Ví khách hàng CoParking
    -> thanh toán hóa đơn/đặt chỗ tại Parking Lot
    -> ghi nhận doanh thu và nghĩa vụ thanh toán cho Partner
    -> đối soát giao dịch ngân hàng
    -> settlement cho Partner
```

Trách nhiệm:

- **System Admin**: quản lý ví, quy tắc nạp/rút/hoàn tiền, tra cứu giao dịch, đối soát ngân hàng, xử lý chênh lệch và settlement Partner.
- **Partner Admin**: xem doanh thu, phí, khoản phải nhận và lịch sử đối soát của Partner; không được cộng/trừ ví khách hàng.
- **Manager**: xem giao dịch của bãi; có thể tạo yêu cầu hoàn tiền theo chính sách nhưng không tự thay đổi số dư.
- **Customer**: nạp tiền, thanh toán, xem số dư/lịch sử và gửi yêu cầu hoàn tiền.

Thiết kế dữ liệu cần dùng sổ cái bất biến, ví dụ `wallet_ledger_entries`, thay vì chỉ duy trì một cột `balance`. Mỗi thay đổi số dư có mã giao dịch duy nhất và giao dịch ngân hàng phải có mã tham chiếu duy nhất để tránh cộng/trừ hai lần khi webhook được gửi lại.

## 9. Tính năng bãi xe 3D

3D là feature option, không bắt buộc cho mọi Partner/bãi:

```text
System Admin cấp feature 3D trong gói SaaS
    + Partner Admin bật 3D cho từng bãi
    + Manager được quyền vận hành tại bãi đó
    = Bãi dùng mô-đun 3D
```

Khi không bật, bãi vẫn hoạt động với quy trình check-in/check-out hiện tại. Khi bật, Manager có thể theo dõi sơ đồ, tình trạng slot và chỉ dẫn xe theo chính sách nhân sự của bãi.

## 10. Dashboard và báo cáo

- **System Admin**: xem dashboard toàn sàn và drill-down `Partner -> Parking Lot`; theo dõi Partner active, bãi active, doanh thu, lượt xe, ví, giao dịch lỗi/chờ đối soát, chiến dịch và thiết bị.
- **Partner Admin**: xem dashboard toàn Partner hoặc từng bãi thuộc Partner.
- **Parking Manager**: chỉ xem dashboard/báo cáo của bãi được gán.
- **Employee**: chỉ xem thông tin phục vụ ca và bãi được giao.

System Admin có quyền xem thống kê từng Partner để vận hành SaaS, nhưng truy cập dữ liệu chi tiết nhạy cảm cần được audit.

## 11. Hướng triển khai kỹ thuật

1. Thêm `organizations`, `partner_applications`, `organization_memberships`, `member_parking_lot_scopes`.
2. Thêm `organization_id` vào `parking.parking_lots`; backfill dữ liệu cũ vào Partner mặc định.
3. Bổ sung `PARTNER_ADMIN`; mở rộng security context để trả về membership và danh sách bãi được phép dùng.
4. Tạo guard dùng chung cho tenant/bãi; áp dụng cho mọi API read/write nghiệp vụ.
5. Scope lần lượt các module topology, thiết bị, ca trực, session, vé, hóa đơn, thanh toán, voucher và báo cáo.
6. Xây dựng UI System Admin, UI Partner Admin và bộ chọn bãi làm việc cho Manager.
7. Thêm test bảo mật: Manager của bãi A truy cập tài nguyên bãi B phải luôn nhận `403 Forbidden`.

### 11.1. Chuyển danh mục và giá từ toàn sàn sang từng Partner

Trạng thái 26/09/2026: **chưa hoàn tất trong code**. Các bảng `catalog.vehicle_types`, `catalog.ticket_types`, `catalog.price_plans` và `catalog.price_rules` hiện chưa có `organization_id`; API danh mục vẫn đọc dữ liệu toàn cục. Đặc biệt `access_control.subscriptions` chưa lưu `parking_lot_id`, nên chưa thể xác định bảng giá của Partner khi khách đăng ký vé. Không được giải quyết bằng cách chỉ cấp thêm permission `*_CREATE_ALL`/`*_UPDATE_ALL` cho Partner Admin.

Thứ tự chuyển đổi an toàn:

1. Thêm quan hệ sở hữu Partner cho danh mục, kế hoạch và quy tắc giá; giữ khóa tham chiếu cũ để hóa đơn/giao dịch lịch sử không đổi. Loại xe Partner phải có ánh xạ tới loại xe chuẩn của hồ sơ khách hàng toàn sàn.
2. Sao chép dữ liệu hiện có cho từng Partner bằng migration forward-only có kiểm tra idempotency; tạo cùng bộ khởi tạo lúc duyệt Partner mới.
3. Bổ sung bãi vào đăng ký vé và các luồng báo giá/check-in/check-out; mọi truy vấn giá phải nhận bãi/Partner rõ ràng và chỉ tìm trong phạm vi đó.
4. Áp dụng kiểm tra role **và** scope cho từng thao tác đọc/ghi danh mục; System Admin chỉ đọc, Partner Admin toàn quyền trong Partner của mình, Manager theo quyền được giao trong bãi.
5. Chỉ sau khi API và luồng tính giá đã dùng scope đúng mới mở nút chỉnh sửa trên UI và cấp quyền ghi cho Partner Admin.
6. Kiểm thử ít nhất hai Partner với cùng mã loại xe/vé/bảng giá nhưng mức giá khác nhau; tài khoản Partner A không xem/sửa được dữ liệu Partner B; khách dùng cùng xe ở cả hai bãi; dữ liệu cũ vẫn đọc được.

### 11.2. Partner Admin kế thừa nghiệp vụ của Parking Manager

Partner Admin phải thao tác được trên **mọi bãi thuộc Partner của mình**, kể cả khi chưa gán Parking Manager. Không sao chép mù toàn bộ `role_permissions` của Parking Manager: một số API cũ vẫn dùng dữ liệu toàn cục hoặc chưa kiểm tra `organization_id`/`parking_lot_id`. Cấp quyền trước khi chốt scope sẽ cho phép sửa dữ liệu Partner khác.

- Đã có chốt phạm vi: topology, thẻ vật lý và check-in/check-out; thiết bị được bổ sung kiểm tra Partner tại use case.
- Đã chốt phạm vi Partner cho hồ sơ nhân sự, phạm vi bãi cho ca trực và phân công; cần tiếp tục chốt phạm vi bãi cho nhân sự, sự kiện bãi, đăng ký vé, hóa đơn/thanh toán, báo cáo.
- Danh mục loại xe, loại vé và giá tuân theo mục 11.1; không cấp CRUD toàn cục cho Partner Admin khi chưa chuyển sở hữu dữ liệu.
- Mỗi module cần test Partner A thao tác bãi A1/A2 thành công, bãi B1 nhận `403`, còn System Admin không có quyền ghi.

### 11.3. Trạng thái triển khai phạm vi bãi (26/09/2026)

- Đã bổ sung khóa `parking_lot_id` trực tiếp cho `parking.parking_sessions`; chỉ backfill từ zone xác định được. Lượt cũ không còn zone được để `NULL`, không gán đoán từ thẻ. Lượt mới ghi bãi lúc check-in. Danh sách lượt xe của Partner/Manager được lọc ngay trong truy vấn theo bãi được phép.
- Đã chặn theo bãi ở mẫu ca, quy tắc xếp lịch, ca trực và phân công; Partner Admin được cấp quyền vận hành nhóm này trong các bãi thuộc Partner, System Admin chỉ xem. Nhân viên check-in/check-out chỉ ở bãi có ca đang mở được phân công.
- Nhân viên mới được gắn membership Partner khi cấp tài khoản. Migration lịch sử chỉ tự gắn khi người tạo tài khoản và các ca cũ suy ra duy nhất cùng một Partner. Nhân viên không xác định được Partner cần đối soát và gán thủ công trước khi xếp lịch.
- Hồ sơ nhân viên đã có chốt quyền theo membership Partner ở backend: Partner Admin chỉ quản lý nhân sự thuộc Partner của mình; System Admin chỉ đọc. Manager cần có bãi được phân công và chỉ truy cập nhân sự cùng Partner. Đây **chưa phải** phạm vi nhân sự theo từng bãi; Manager của hai bãi cùng Partner vẫn có thể thấy nhân sự của nhau. Cần thêm quan hệ phân công nhân viên vào bãi ổn định (không suy đoán từ ca trực) trước khi tuyên bố hoàn tất phần này.
- System Admin vẫn được tạo Partner theo luồng quản trị nền tảng, nhưng không còn được tạo/sửa bãi của Partner hoặc phân công Manager vào bãi; các nút ghi tương ứng phải dựa vào permission thực tế.
- **Chưa hoàn tất tách toàn hệ thống:** `people.employees` chưa có phạm vi bãi quản lý ổn định; `access_control.subscriptions` và `billing.invoices` chưa có `parking_lot_id`; danh mục loại xe/vé/giá vẫn toàn cục. Không cấp thêm quyền ghi cho Partner ở các module này cho đến khi khóa sở hữu, backfill và luồng tạo mới được hoàn thiện. Bộ chọn phạm vi trên frontend không thay thế kiểm tra scope tại backend.

Đối soát sau migration (không tự sửa dữ liệu): đếm `parking.parking_sessions` có `parking_lot_id IS NULL` và các tài khoản `EMPLOYEE` chưa có `iam.organization_memberships` trạng thái `ACTIVE`. Mỗi dòng tồn đọng cần xác nhận thủ công theo hồ sơ nghiệp vụ trước khi bật truy cập theo bãi.

## 12. Các quyết định cần chốt trước khi triển khai

- Một Manager có được quản lý nhiều bãi hay chỉ đúng một bãi?
- Partner Admin có được thêm Partner Admin thứ hai hay chỉ System Admin được cấp?
- Voucher cấp bãi của Manager có cần Partner Admin phê duyệt theo ngưỡng ngân sách không?
- Khách có được dùng một voucher toàn sàn cùng voucher Partner/bãi không, hay chỉ chọn một voucher?
- Quy tắc hoàn tiền qua ví, qua cổng thanh toán và tại quầy được ưu tiên theo thứ tự nào?
- Tiêu chí tối thiểu để một bãi chuyển từ `ONBOARDING` sang `ACTIVE` là gì?
