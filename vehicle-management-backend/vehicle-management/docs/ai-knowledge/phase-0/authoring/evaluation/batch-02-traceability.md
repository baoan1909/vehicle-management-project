# Đối chiếu đợt 2

Ngày kiểm tra: 2026-11-19. Nguồn là checkout đang làm việc; tài liệu DRAFT, chưa nhập hoặc xuất bản vào DB dev.

## Những thay đổi so với tài liệu cũ

- Xóa xe là ngưng dùng; biển số bị chặn sửa khi có vé đang hoạt động hoặc lượt gửi còn mở.
- Đăng ký mới chọn ngày bắt đầu từ hôm nay đến 7 ngày tới.
- Gia hạn nối tiếp trên vé đang hoạt động và chưa hết hạn; vé hết hạn đăng ký mới.
- Thanh toán, chọn vị trí và cấp thẻ vật lý có điều kiện riêng. Không đồng nhất trả tiền với vào bãi ngay.
- Phiếu báo mất là nghiệp vụ vận hành; không thay bằng phiếu hỗ trợ lỗi hệ thống.
- Chat chỉ đọc dữ liệu cá nhân có quyền, giới hạn và che biển số; không đọc trực tiếp trạng thái thẻ.

## Bằng chứng trong code

| Thành phần | Điểm kiểm tra | Nguồn |
|---|---|---|
| CustomerVehicleUseCaseImpl.java | `getMyCustomerVehicles` | [dòng 140](../../../../../src/main/java/com/ban/vehicle_management/application/people/customervehicle/usecase/CustomerVehicleUseCaseImpl.java#L140) |
| CustomerVehicleUseCaseImpl.java | `deleteCustomerVehicle` | [dòng 148](../../../../../src/main/java/com/ban/vehicle_management/application/people/customervehicle/usecase/CustomerVehicleUseCaseImpl.java#L148) |
| CustomerVehicleUseCaseImpl.java | `ensureLicensePlateCanChange` | [dòng 572](../../../../../src/main/java/com/ban/vehicle_management/application/people/customervehicle/usecase/CustomerVehicleUseCaseImpl.java#L572) |
| CustomerVehicleUseCaseImpl.java | `normalizePlateForVehicleType` | [dòng 585](../../../../../src/main/java/com/ban/vehicle_management/application/people/customervehicle/usecase/CustomerVehicleUseCaseImpl.java#L585) |
| CustomerVehicleUseCaseImpl.java | `validateUniqueLicensePlate` | [dòng 227](../../../../../src/main/java/com/ban/vehicle_management/application/people/customervehicle/usecase/CustomerVehicleUseCaseImpl.java#L227) |
| CustomerVehicleAccessGuard.java | `resolveOwnCustomerId` | [dòng 132](../../../../../src/main/java/com/ban/vehicle_management/application/people/customervehicle/authorization/CustomerVehicleAccessGuard.java#L132) |
| CustomerVehicleAccessGuard.java | `resolveCurrentApprovedCustomerId` | [dòng 137](../../../../../src/main/java/com/ban/vehicle_management/application/people/customervehicle/authorization/CustomerVehicleAccessGuard.java#L137) |
| SubscriptionPolicy.java | `MIN_EFFECTIVE_LEAD_DAYS` | [dòng 19](../../../../../src/main/java/com/ban/vehicle_management/domain/accesscontrol/subscription/policy/SubscriptionPolicy.java#L19) |
| SubscriptionPolicy.java | `MAX_EFFECTIVE_LEAD_DAYS` | [dòng 20](../../../../../src/main/java/com/ban/vehicle_management/domain/accesscontrol/subscription/policy/SubscriptionPolicy.java#L20) |
| SubscriptionPolicy.java | `approve(` | [dòng 95](../../../../../src/main/java/com/ban/vehicle_management/domain/accesscontrol/subscription/policy/SubscriptionPolicy.java#L95) |
| SubscriptionPolicy.java | `markPaymentCompleted` | [dòng 146](../../../../../src/main/java/com/ban/vehicle_management/domain/accesscontrol/subscription/policy/SubscriptionPolicy.java#L146) |
| SubscriptionPolicy.java | `cancelBeforeRefundWorkflow` | [dòng 240](../../../../../src/main/java/com/ban/vehicle_management/domain/accesscontrol/subscription/policy/SubscriptionPolicy.java#L240) |
| SubscriptionUseCaseImpl.java | `getMySubscriptions` | [dòng 223](../../../../../src/main/java/com/ban/vehicle_management/application/accesscontrol/subscription/usecase/SubscriptionUseCaseImpl.java#L223) |
| SubscriptionUseCaseImpl.java | `approveSubscription` | [dòng 286](../../../../../src/main/java/com/ban/vehicle_management/application/accesscontrol/subscription/usecase/SubscriptionUseCaseImpl.java#L286) |
| SubscriptionUseCaseImpl.java | `prepareRenewal` | [dòng 641](../../../../../src/main/java/com/ban/vehicle_management/application/accesscontrol/subscription/usecase/SubscriptionUseCaseImpl.java#L641) |
| SubscriptionUseCaseImpl.java | `completeRenewalPayment` | [dòng 453](../../../../../src/main/java/com/ban/vehicle_management/application/accesscontrol/subscription/usecase/SubscriptionUseCaseImpl.java#L453) |
| SubscriptionUseCaseImpl.java | `requestMyPhysicalCard` | [dòng 505](../../../../../src/main/java/com/ban/vehicle_management/application/accesscontrol/subscription/usecase/SubscriptionUseCaseImpl.java#L505) |
| LostCardReportUseCaseImpl.java | `createReport(` | [dòng 242](../../../../../src/main/java/com/ban/vehicle_management/application/accesscontrol/lostcardreport/usecase/LostCardReportUseCaseImpl.java#L242) |
| LostCardReportUseCaseImpl.java | `resolveReport(` | [dòng 350](../../../../../src/main/java/com/ban/vehicle_management/application/accesscontrol/lostcardreport/usecase/LostCardReportUseCaseImpl.java#L350) |
| LostCardReportUseCaseImpl.java | `cancelReport(` | [dòng 445](../../../../../src/main/java/com/ban/vehicle_management/application/accesscontrol/lostcardreport/usecase/LostCardReportUseCaseImpl.java#L445) |
| ParkingSessionUseCaseImpl.java | `getOwnSessionSummaries` | [dòng 120](../../../../../src/main/java/com/ban/vehicle_management/application/parking/parkingsession/usecase/ParkingSessionUseCaseImpl.java#L120) |
| ParkingSessionUseCaseImpl.java | `findOwnSessions` | [dòng 125](../../../../../src/main/java/com/ban/vehicle_management/application/parking/parkingsession/usecase/ParkingSessionUseCaseImpl.java#L125) |
| AiToolExecutionService.java | `listMySubscriptions` | [dòng 129](../../../../../src/main/java/com/ban/vehicle_management/application/ai/service/AiToolExecutionService.java#L129) |
| AiToolExecutionService.java | `listMyParkingHistory` | [dòng 136](../../../../../src/main/java/com/ban/vehicle_management/application/ai/service/AiToolExecutionService.java#L136) |
| AssistantOrchestrator.java | `unsupportedBusinessRequest` | [dòng 411](../../../../../src/main/java/com/ban/vehicle_management/application/ai/service/AssistantOrchestrator.java#L411) |
| AssistantOrchestrator.java | `groundedRequest(` | [dòng 1011](../../../../../src/main/java/com/ban/vehicle_management/application/ai/service/AssistantOrchestrator.java#L1011) |
| AssistantOrchestrator.java | `These read tools` | [dòng 1567](../../../../../src/main/java/com/ban/vehicle_management/application/ai/service/AssistantOrchestrator.java#L1567) |
| VehiclePage.tsx | `function status` | [dòng 37](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/customer-portal/pages/VehiclePage.tsx#L37) |
| VehiclePage.tsx | `Thêm xe mới` | [dòng 196](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/customer-portal/pages/VehiclePage.tsx#L196) |
| SubscriptionPage.tsx | `function statusLabel` | [dòng 105](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/customer-portal/pages/SubscriptionPage.tsx#L105) |
| SubscriptionPage.tsx | `function canRenewSubscription` | [dòng 81](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/customer-portal/pages/SubscriptionPage.tsx#L81) |
| CustomerHistoryPage.tsx | `function statusConfig` | [dòng 82](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/customer-portal/pages/CustomerHistoryPage.tsx#L82) |
| CustomerHistoryPage.tsx | `function buildRequestFilters` | [dòng 104](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/customer-portal/pages/CustomerHistoryPage.tsx#L104) |

## Schema và migration

- [V20260818110000__baseline_schema.sql](../../../../../src/main/resources/db/migration/V20260818110000__baseline_schema.sql)
- [V20260927100000__normalize_license_plate_identity.sql](../../../../../src/main/resources/db/migration/V20260927100000__normalize_license_plate_identity.sql)
- [V20261105110000__add_subscription_space_selection.sql](../../../../../src/main/resources/db/migration/V20261105110000__add_subscription_space_selection.sql)
- [V20261105120000__decouple_physical_card_fulfillment.sql](../../../../../src/main/resources/db/migration/V20261105120000__decouple_physical_card_fulfillment.sql)
- [V20261105130000__add_subscription_physical_card_fulfillment.sql](../../../../../src/main/resources/db/migration/V20261105130000__add_subscription_physical_card_fulfillment.sql)
- [V20261106100000__add_subscription_renewals.sql](../../../../../src/main/resources/db/migration/V20261106100000__add_subscription_renewals.sql)
- [V20261111171000__terminate_subscription_for_account_deletion.sql](../../../../../src/main/resources/db/migration/V20261111171000__terminate_subscription_for_account_deletion.sql)

## Cải tiến dữ liệu và tốc độ

- Chức năng đọc vé dùng đường đọc riêng theo hồ sơ hiện tại, không lấy danh sách quản trị với customerId rỗng.
- Đường đọc xe và vé cá nhân luôn xác định chủ sở hữu, kể cả tài khoản có đồng thời quyền quản trị.
- Công cụ chỉ xuất bản tóm tắt tiếng Việt; không serialize đối tượng vé chứa khóa truy cập QR, audit và ID nội bộ.
- Ba công cụ tóm tắt phương tiện, vé và lịch sử không cần vòng sinh câu trả lời thứ hai.
- Truy vấn lịch sử cho chat bỏ bước ký URL ảnh riêng tư.
- Luồng kiến thức chỉ gửi câu hỏi đã giải quyết chủ đề và bằng chứng; không tải lại lịch sử chat không liên quan.
- Giới hạn generation có thể cấu hình riêng; không thay embedding model hoặc hạ ngưỡng bằng chứng.

## Phạm vi đánh giá

Dataset 40 nhóm × 6 cách hỏi = 240 trường hợp. Hai nhóm 04–05 của mỗi tài liệu là acceptance (48); các nhóm còn lại regression (192). Sau khi đã dùng để sửa router, đây không còn là đánh giá mù độc lập. Hướng dẫn và tra cứu cá nhân được kiểm tra riêng.

## Cải tiến kiểm tra thêm trong đợt này

- Câu hỏi liệt kê dữ liệu của mình đi qua `AssistantOwnReadRouter` và các chức năng đọc có quyền; không gọi model, không cache câu trả lời riêng tư. Câu có mã, ngày, bộ lọc hoặc nhiều yêu cầu không được đổi thành tra cứu toàn danh sách.
- `ChatMessagePolicy` cho phép xuống dòng ở tin nhắn người dùng và trợ lý; giới hạn độ dài, chặn ký tự điều khiển khác và nội dung đánh dấu vẫn còn.
- `ai_tool_calls.run_id` trước đây bắt buộc gắn với lần gọi model. [Migration mới](../../../../../src/main/resources/db/migration/V20261119110000__allow_tool_audit_without_model_run.sql) và mapping cho phép rỗng để ghi nhận tra cứu trực tiếp mà không tạo bản ghi gọi model giả. Không xóa hoặc sửa nội dung dữ liệu cũ.

## Ranh giới kiểm chứng cuối

- `GroundedFaqLookup` đọc dòng hỏi–đáp đầy đủ trong chunk hiện hành, không dùng nhãn đầu chunk làm điều kiện duy nhất vì overlap có thể giữ nhãn mục trước. Câu hỏi phải khớp toàn bộ, không phải substring; nếu hai đáp án khác nhau thì không tự chọn.
- Guest vẫn truy xuất scope PUBLIC do server đặt; câu dữ liệu cá nhân hoặc thao tác không dùng nhánh Q/A tĩnh. Cả hai loại chat giữ kiểm tra output và citation.
- `KnowledgeRetrievalService.isInfrastructureFailure` phân biệt lỗi embedding/truy xuất/cấu hình index với thiếu bằng chứng. Thông báo không nói nhầm rằng tài liệu không tồn tại.
- Cuối đợt Google trả quota embedding 1.000 request/ngày/project/model; đã dừng API thật. Bản cuối có kiểm thử cục bộ, chưa có nghiệm thu Gemini đầy đủ sau sửa overlap. Xem báo cáo để không nhầm số liệu snapshot cũ với source cuối.

## Ma trận nghiệp vụ – giao diện – code – kiểm thử

| Nghiệp vụ | Giao diện/nhãn liên quan | Code chính | Kiểm thử liên quan |
|---|---|---|---|
| Phương tiện chính chủ; ngưng dùng giữ lịch sử | Xe của tôi, Thêm xe mới, Xóa phương tiện | [CustomerVehicleUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/people/customervehicle/usecase/CustomerVehicleUseCaseImpl.java); [CustomerVehicleAccessGuard.java](../../../../../src/main/java/com/ban/vehicle_management/application/people/customervehicle/authorization/CustomerVehicleAccessGuard.java) | [CustomerVehicleOwnListLimitTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/people/customervehicle/usecase/CustomerVehicleOwnListLimitTest.java); [CustomerVehicleAccessGuardTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/people/customervehicle/authorization/CustomerVehicleAccessGuardTest.java); [Batch2AiToolPrivacyTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/ai/service/Batch2AiToolPrivacyTest.java) |
| Hiệu lực, ngày bắt đầu và gia hạn vé | Vé tháng của tôi, Ngày bắt đầu, Gia hạn vé | [SubscriptionPolicy.java](../../../../../src/main/java/com/ban/vehicle_management/domain/accesscontrol/subscription/policy/SubscriptionPolicy.java); [SubscriptionUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/accesscontrol/subscription/usecase/SubscriptionUseCaseImpl.java); [SubscriptionAccessGuard.java](../../../../../src/main/java/com/ban/vehicle_management/application/accesscontrol/subscription/authorization/SubscriptionAccessGuard.java) | [SubscriptionOwnListLimitTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/accesscontrol/subscription/usecase/SubscriptionOwnListLimitTest.java); [SubscriptionUseCaseImplTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/accesscontrol/subscription/usecase/SubscriptionUseCaseImplTest.java); [Batch2AiToolPrivacyTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/ai/service/Batch2AiToolPrivacyTest.java) |
| Báo mất và hoàn tất bởi nhân viên có quyền | Thông tin thẻ tại Vé tháng của tôi; nhân viên xử lý báo mất | [LostCardReportUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/accesscontrol/lostcardreport/usecase/LostCardReportUseCaseImpl.java); [AiToolRegistry.java](../../../../../src/main/java/com/ban/vehicle_management/domain/ai/policy/AiToolRegistry.java) | [LostCardReportUseCaseImplTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/accesscontrol/lostcardreport/usecase/LostCardReportUseCaseImplTest.java); [Phase2RoutingEvaluationTest.java](../../../../../src/test/java/com/ban/vehicle_management/domain/ai/model/Phase2RoutingEvaluationTest.java); [AssistantOrchestratorTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/ai/service/AssistantOrchestratorTest.java) |
| Lịch sử, ngày và ảnh riêng tư | Lịch sử gửi xe, Khoảng ngày, Xem chi tiết, Xóa lọc | [ParkingSessionUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/parking/parkingsession/usecase/ParkingSessionUseCaseImpl.java); [AssistantOwnDataFormatter.java](../../../../../src/main/java/com/ban/vehicle_management/application/ai/service/AssistantOwnDataFormatter.java) | [ParkingSessionOwnSummaryTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/parking/parkingsession/usecase/ParkingSessionOwnSummaryTest.java); [Batch2AiToolPrivacyTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/ai/service/Batch2AiToolPrivacyTest.java) |
| Nguồn, Q/A, câu trả lời dễ đọc và lỗi provider | Khung chat guest và sau đăng nhập; hiển thị nhiều dòng | [GroundedFaqLookup.java](../../../../../src/main/java/com/ban/vehicle_management/domain/ai/model/GroundedFaqLookup.java); [AssistantOutputValidator.java](../../../../../src/main/java/com/ban/vehicle_management/domain/ai/model/AssistantOutputValidator.java); [GuestPublicAssistantService.java](../../../../../src/main/java/com/ban/vehicle_management/application/operations/guestsupport/service/GuestPublicAssistantService.java) | [GroundedFaqLookupTest.java](../../../../../src/test/java/com/ban/vehicle_management/domain/ai/model/GroundedFaqLookupTest.java); [AssistantOutputValidatorTest.java](../../../../../src/test/java/com/ban/vehicle_management/domain/ai/model/AssistantOutputValidatorTest.java); [GuestPublicAssistantServiceTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/operations/guestsupport/service/GuestPublicAssistantServiceTest.java); [Phase2KnowledgeAcceptanceIT.java](../../../../../src/test/java/com/ban/vehicle_management/ai/golden/Phase2KnowledgeAcceptanceIT.java) |
