# Khởi tạo catalog partner ở trạng thái Nháp

## Luồng trước và sau

Trước đây, cả duyệt đăng ký partner và tạo organization trực tiếp đều copy catalog nội bộ với trạng thái hoạt động của nguồn. Account mới vì vậy có thể thấy loại xe, loại vé, kế hoạch giá và quy tắc giá đang áp dụng trước khi partner xác nhận cấu hình.

Luồng mới:

1. Tạo account/organization/membership trong transaction hiện hữu.
2. Claim `organizations.catalog_seeded_at` đúng một lần; request đồng thời cùng organization chờ cùng hàng dữ liệu.
3. Copy bốn danh mục thành DRAFT, UUID độc lập, lưu `source_catalog_id`. Quy tắc nối sang bản sao bằng source UUID, kể cả nguồn có nhiều phiên bản vé cùng code.
4. Partner chỉnh nháp, bổ sung hoặc bỏ mục không dùng. Tạo thủ công cũng luôn là DRAFT, kể cả client gửi trạng thái active.
5. Công bố loại xe và vé; xác nhận ngày hiệu lực và công bố kế hoạch giá; công bố quy tắc sau khi dependency đã active. Đây là các thao tác riêng, chưa có chức năng publish cả bộ nguyên tử.
6. Chọn rõ loại xe và loại vé phục vụ tại từng bãi mới. Công bố cấp organization không tự bật dịch vụ tại bãi.
7. Bãi mới cần topology/layout hợp lệ và đủ giá có hiệu lực cho từng cặp xe/vé đã bật trước khi yêu cầu kích hoạt. Khách vãng lai cần khung giá phủ đủ ngày; không bắt bật vé tháng nếu không cung cấp dịch vụ đó.

Seed lỗi hoặc transaction đăng ký lỗi sẽ rollback cả marker và catalog. Thông báo duyệt dùng cơ chế sau commit hiện hữu. Tạo thêm account/membership trong organization hiện hữu không gọi seed/reset catalog.

## Trạng thái và API

- Vé dùng enum `DRAFT/ACTIVE/INACTIVE`.
- Xe/kế hoạch/quy tắc dùng `lifecycle_status`, vẫn giữ `is_active` tương thích. CHECK và trigger PostgreSQL đảm bảo không có DRAFT với `is_active=true`; bulk deactivation cũ được đồng bộ.
- UPCOMING/EXPIRED vẫn là trạng thái hiệu lực theo ngày của bản đã công bố.
- API quản trị trả lifecycle; danh sách xe/kế hoạch/quy tắc nhận thêm `lifecycleStatus`. `isActive=false` cũ vẫn gồm cả DRAFT và INACTIVE; truyền `lifecycleStatus` để phân biệt. API vé nhận `status=DRAFT`.
- API public luôn lấy dữ liệu active có hiệu lực trong bãi active; request trạng thái nháp không mở quyền xem nháp. Không có parkingLotId thì API giá trả danh sách trống, không fallback giá nội bộ. API loại xe không chọn bãi vẫn cung cấp bộ phân loại chuẩn dùng để gắn canonical.
- DTO public riêng không có thông tin organization/actor/audit. Lookup giá vận hành yêu cầu rule, plan, xe và vé active.
- Một organization lock được lấy sau kiểm tra quyền khi sửa/publish catalog, để validation overlap không bị vượt qua bởi hai request đồng thời.

## Permission thực tế

Điều kiện quyết định là permission thao tác, membership/lifecycle và scope. Không thêm điều kiện chặn theo tên role; gán bãi không tự cấp action permission hoặc scope partner.

| Thao tác | Điều kiện bổ sung ngoài permission thao tác |
| --- | --- |
| Đọc catalog | Scope partner và membership; hoặc scope assigned và bãi còn được phân công; scope platform chỉ đọc theo guard hiện hữu |
| Tạo/công bố/bỏ catalog chung | `PARKING_SCOPE_PARTNER` và organization hợp lệ |
| Sửa vé/kế hoạch/quy tắc | `PARKING_SCOPE_PARTNER` và organization sở hữu bản ghi |
| Sửa trường loại xe qua bãi | Giữ hành vi hiện hữu: `VEHICLE_TYPE_UPDATE_ALL`, bãi được authorize và cùng organization; không thêm role deny |
| Bật/tắt phục vụ xe tại một bãi | `VEHICLE_TYPE_UPDATE_ALL` và quyền vận hành đúng bãi được gán |
| Bật/tắt xe nhiều bãi | Thêm `PARKING_SCOPE_PARTNER`; kiểm tra từng bãi |
| Bật/tắt vé tại bãi | `TICKET_TYPE_UPDATE_ALL` và scope partner theo guard hiện hữu |

PARKING_MANAGER/EMPLOYEE có đủ các điều kiện tương ứng vẫn thao tác được. Chỉ có permission action mà thiếu scope/membership thì chưa đủ quyền. UI giá đã đồng bộ điều kiện với backend; không hiện nút ghi chung mà backend chắc chắn từ chối do thiếu scope partner.

Điểm cần lưu ý: sửa trường loại xe qua bãi vẫn ghi bản ghi dùng chung của organization, đúng hành vi đã tồn tại trước thay đổi này và được giữ theo yêu cầu bảo toàn thao tác đã được cấp quyền. Việc tách override theo bãi hoặc thay đổi ranh giới này cần một thay đổi phân quyền riêng.

Automatic approval review đã từ chối phương án mở rộng quyền ghi catalog chung từ scope partner sang scope assigned vì rủi ro thay đổi ranh giới permission lâu dài. Phương án đó không được áp dụng; các guard hiện hữu được giữ nguyên.

## Giao diện

- F5/mở trang và reset bộ lọc về Đang hoạt động/Đang áp dụng ở cả bốn danh sách.
- Có Nháp riêng và thông báo số mẫu chưa áp dụng, kèm nút Xem nháp.
- Partner có scope phù hợp xem/chỉnh catalog khi chưa có bãi.
- Lưu mới chuyển tới Nháp. Sửa kế hoạch giá thực sự gọi PUT, không chỉ cập nhật state trình duyệt.
- Form quy tắc tải cả dependency nháp để chỉnh cả bộ; publish mới yêu cầu active.
- Tạo xe thủ công chọn phân loại canonical chuẩn; không phụ thuộc duy nhất vào seed.
- Bỏ nháp là soft deactivation, cần DELETE permission. Không xóa ID hoặc phá dependency/lịch sử.
- Bãi mới chọn áp dụng rõ ràng. Xe có nghĩa vụ cũ đang ngừng dần vẫn dùng cơ chế draining hiện hữu; không coi tất cả mục chưa từng bật là mục đang cần detach khỏi zone.

## Migration và dữ liệu partner cũ

Migration mới: `V20261115100000__partner_catalog_draft_lifecycle.sql`.

Backfill dữ liệu cũ theo đúng is_active/status hiện có. Đánh dấu organization hiện hữu đã seed để retry không ghi đè dữ liệu. Bãi hiện hữu có `catalog_explicit_services=false`, giữ exclusion semantics; bãi tạo mới mặc định true. Không có UPDATE chuyển hàng loạt dữ liệu partner cũ về Nháp.

`source_catalog_id` chỉ cung cấp lineage cho bản sao mới. Giá trị NULL ở dữ liệu cũ là chưa xác minh nguồn gốc, không phải bằng chứng đủ để hạ trạng thái.

`scripts/operations/partner-catalog-draft-dry-run.sql` chạy transaction read-only, xuất membership, catalog IDs/snapshots, source lineage, subscription/renewal/session/reservation/invoice và audit. Nó không tự chọn ứng viên hay thực thi remediation. Manifest giữ nguyên trạng thái và để proposed_state NULL cho tới khi rà soát đủ bằng chứng.

Trước bất kỳ remediation riêng nào: xác minh nguồn, sửa đổi của partner, giao dịch và nghĩa vụ đang mở; lưu manifest trước/sau và bản sao có thể khôi phục; kiểm tra lại trong transaction. Không hạ trạng thái khi chưa rõ nguồn gốc hoặc còn nghĩa vụ phục vụ. Script này chưa chạy vào dữ liệu thật.

## Rollout và rollback

1. Sao lưu và thử migration trên bản sao dữ liệu đại diện; lưu số lượng/trạng thái trước triển khai.
2. Tạm dừng thao tác tạo organization và ghi catalog trong cửa sổ triển khai. Các phiên bản backend cũ vẫn có thể seed active; không để chúng tiếp tục nhận các request này trong giai đoạn chuyển đổi.
3. Áp dụng schema mở rộng, triển khai backend mới, xác nhận seed/publish/quyền/API public trên môi trường kiểm thử.
4. Triển khai frontend mới rồi mở lại các thao tác. Kiểm tra Active trống/Nháp đầy đủ ở partner mới và khả năng hoàn tất phiên hiện hữu của partner cũ.
5. Theo dõi lỗi CHECK/FK, seed thất bại và audit publication.

Rollback ưu tiên khôi phục phiên bản ứng dụng hiểu DRAFT hoặc forward-fix, giữ schema mở rộng. Không chạy down migration xóa enum/column khi còn DRAFT; backend cũ không đọc được TicketTypeStatus.DRAFT. Không rollback bằng cách kích hoạt hàng loạt nháp. Nếu bắt buộc quay về bản chưa hỗ trợ DRAFT, giữ cửa sổ bảo trì và dùng kế hoạch phục hồi đã kiểm chứng với snapshot và các giao dịch phát sinh.

## Kiểm chứng

Kết quả ngày 15/11/2026:

| Nhóm kiểm tra | Kết quả |
| --- | --- |
| Backend chọn lọc cuối cùng: catalog, permission, availability, readiness và checkout | 80 test, 0 lỗi, 0 bỏ qua |
| PartnerCatalogDraftPersistenceIT | 7 test pass trên PostgreSQL riêng |
| PartnerRegistrationTransactionIT | 5 test pass trên PostgreSQL riêng |
| Playwright catalog-draft.behavior.spec.ts | 8 test pass |
| npm test | Pass toàn bộ chuỗi kiểm tra frontend hiện hữu |
| npm run build | Pass TypeScript và Vite production build |
| git diff --check | Pass |

Ngoài lần chạy chọn lọc cuối cùng, bộ `*PolicyTest,*UseCaseImplTest,TicketTypeUsecaseImplTest,CatalogAccessGuardTest,SqlFreeAdapterDelegationTest,*PersistenceAdapterTest` đã chạy thành công. Build vẫn có cảnh báo hiện hữu về Browserslist, CSS color-adjust và kích thước chunk; không có lỗi build.

Các tệp chính để review:

- Database: migration lifecycle, bảng enrollment và audit trigger; script operations dry-run.
- Domain/API: `CatalogLifecycleStatus`, `TicketTypeStatus`, ba model/policy lifecycle, bốn response quản trị và bốn DTO public riêng; filter request và controller catalog.
- Application: `PartnerCatalogSeedPersistenceAdapter`, bốn catalog use case, `CatalogAccessGuard`, `ParkingLotCatalogAvailabilityUseCaseImpl`, `PublicParkingLotCatalogUseCaseImpl`, `ParkingLotUseCaseImpl`.
- Persistence: bốn catalog repository, `OrganizationRepository`/port/adapter và `ParkingLotRepository`/adapter.
- Frontend: `CatalogListPages.tsx`, `CatalogManageComponents.tsx`, `ParkingLotCatalogControls.tsx`, `PricingManagePages.tsx`, các API/type liên quan và `apiMessage.ts`.
- Test mới: `PartnerCatalogDraftPersistenceIT.java`, `catalog-draft.behavior.spec.ts`; cập nhật test policy/use case/registration/adapter hiện hữu.

- Unit/policy/guard/adapter và các use case vận hành trong bộ kiểm thử mở rộng.
- PostgreSQL Testcontainers: migration đầy đủ, seed hai đường, source mapping chính xác, retry/concurrency, rollback, mặc định DRAFT và legacy deactivation, explicit enrollment/readiness, truy vấn manifest chỉ đọc.
- Playwright: bốn trang F5 mặc định Active và chuyển Nháp; sửa draft plan lưu qua API và qua reload; manager vẫn có thao tác xe được cấp; employee có action và scope partner không bị chặn bởi tên role.
- TypeScript và production build; bộ kiểm tra frontend hiện hữu.

Không triển khai production trong tác vụ này. Playwright dùng API mock để kiểm tra hành vi UI; không thay thế kiểm thử tích hợp OIDC, thiết bị/camera/cổng, thanh toán và mọi tổ hợp check-in/check-out ngoài hiện trường. Các luồng checkout/subscription/reservation hiện hữu được kiểm tra hồi quy bằng các test hiện có; chưa thực hiện thử nghiệm vận hành trên bãi thật.
