# Truy vết đợt 4 — 23/11/2026

> Bàn giao 25/11/2026: các lệnh Python trong báo cáo này là bằng chứng lịch sử; wrapper đã được gỡ. Lệnh hiện tại và các giới hạn kiểm thử được ghi trong [TEAM_HANDOFF.vi.md](../../../TEAM_HANDOFF.vi.md). Không dùng log cũ để kết luận bản code hiện tại đã nghiệm thu.

Trạng thái DRAFT. Đối chiếu working tree, không chứng minh mọi cấu hình môi trường đã bật.

| Chủ đề | Backend / dữ liệu | Frontend / quyết định | Giới hạn |
|---|---|---|---|
| Xe vào/ra | ParkingSessionController; ParkingCheckInUseCaseImpl; ParkingCheckOutUseCaseImpl; ParkingCheckInPolicy; ParkingCheckOutPolicy; ParkingSessionAccessGuard; parking sessions/events và hóa đơn | SwipeEntryPage, SwipeListPage, ParkingSessionPage; Flyway QR 29/10 và reservation 07/11 | QR/thẻ, xem trước/xác nhận, bãi/quyền, khớp biển số, thanh toán khác hoàn tất; không có assistant tool vận hành cổng |
| Tìm bãi/bản đồ | PublicParkingLotController; PublicParkingLocationController; NearbyParkingLotUseCaseImpl; PostGisNearbyParkingLotAdapter; PublicParkingLotDirectoryUseCaseImpl; ParkingLocationFeaturePortIn | NearbyParkingLotsPage, NearbyParkingMap; bản đồ nền OSM, chỉ đường Google Maps | Tọa độ trong miền hợp lệ, bán kính nguyên 1–50km, limit 1–50; tra địa chỉ từ 3 ký tự; feature flags; khoảng cách địa lý, không giữ chỗ |
| Liên hệ/giờ | open-business-questions TBD-001..003; decision-log DEC-015 | PortalShared, ContactPage | Đồng bộ contact đã duyệt; form chưa có API nên vô hiệu hóa; chưa có giờ từng bãi và monitoring SLA đầy đủ |
| Lỗi | AssistantOrchestrator; GuestPublicAssistantService; AssistantBusinessRequestPolicy; LostCardReportUseCaseImpl; quyền và OWN | Auth, lịch sử gửi xe, support, thông báo lỗi UI | Tách điều kiện lập/hoàn tất mất thẻ; lỗi đăng nhập chưa rõ không suy ra quên mật khẩu; dùng khoảng ngày |

## Công cụ chatbot hiện tại
AiToolRegistry: search_support_knowledge; get_my_support_ticket; list_my_support_tickets; get_my_subscription_status; list_my_vehicles; list_my_parking_history; create_support_ticket. Không có công cụ xem hóa đơn, số chỗ, định vị, mở cổng, ghi nhận vào/ra hoặc hoàn tiền. Không thêm tool mới trong đợt này: dữ liệu vị trí cần sự đồng ý và giao diện bản đồ đã có luồng riêng; hướng dẫn dùng giao diện, không giả dữ liệu live.

## Kiểm chứng và dữ liệu đánh giá
40 tình huống nghiệp vụ và 4 tình huống bảo mật × 6 biến thể = 264 câu. Mọi tình huống đợt 4 thuộc acceptance; không dùng tập này làm training. Biến thể hỏi tiếp giữ điều kiện trong câu hỏi và chỉ được thừa kế public topic được phép. Bộ route tích lũy dùng registry; baseline 120 có gate riêng, không tự coi là đã chạy live.

## Quyền và phạm vi
Bốn tài liệu là PUBLIC, chỉ hướng dẫn người dùng và không chứa SOP thao tác nội bộ. Notes kỹ thuật nằm trong authoring-only block, bị loại khỏi export. Tạo phiếu và đọc dữ liệu riêng vẫn do permission và OWN runtime quyết định, không do lời chat.

## Phân biệt QR đã kiểm chứng
ParkingCheckInUseCaseImpl.checkIn và preview: chỉ subscriptionQrPayload cho nhánh không cần cardUid; reservationQrPayload không kích hoạt nhánh này. QR đặt chỗ không tự thay thẻ vãng lai. Cấm dùng đồng thời thẻ với QR của vé đăng ký, không nhầm thành cấm đối chiếu QR đặt chỗ khi dùng thẻ. Lượt live trước sửa đã gợi ý QR cho khách chưa có vé; giữ transcript và kiểm tra lại.

## Chỉ mục mã nguồn đối chiếu

- [ParkingSessionController.java](../../../../../src/main/java/com/ban/vehicle_management/entrypoint/controller/parking/ParkingSessionController.java)
- [ParkingCheckInUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/parking/parkingsession/usecase/ParkingCheckInUseCaseImpl.java)
- [ParkingCheckOutUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/parking/parkingsession/usecase/ParkingCheckOutUseCaseImpl.java)
- [ParkingCheckInPolicy.java](../../../../../src/main/java/com/ban/vehicle_management/domain/parking/parkingsession/policy/ParkingCheckInPolicy.java)
- [ParkingCheckOutPolicy.java](../../../../../src/main/java/com/ban/vehicle_management/domain/parking/parkingsession/policy/ParkingCheckOutPolicy.java)
- [ParkingSessionAccessGuard.java](../../../../../src/main/java/com/ban/vehicle_management/application/parking/parkingsession/authorization/ParkingSessionAccessGuard.java)
- [PublicParkingLotController.java](../../../../../src/main/java/com/ban/vehicle_management/entrypoint/controller/parking/PublicParkingLotController.java)
- [PublicParkingLocationController.java](../../../../../src/main/java/com/ban/vehicle_management/entrypoint/controller/parking/PublicParkingLocationController.java)
- [NearbyParkingLotUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/parking/parkinglot/usecase/NearbyParkingLotUseCaseImpl.java)
- [PublicParkingLotDirectoryUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/parking/parkinglot/usecase/PublicParkingLotDirectoryUseCaseImpl.java)
- [PublicParkingLocationUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/parking/location/usecase/PublicParkingLocationUseCaseImpl.java)
- [PostGisNearbyParkingLotAdapter.java](../../../../../src/main/java/com/ban/vehicle_management/infrastructure/persistence/adapter/parking/PostGisNearbyParkingLotAdapter.java)
- [LostCardReportUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/accesscontrol/lostcardreport/usecase/LostCardReportUseCaseImpl.java)
- [AiToolRegistry.java](../../../../../src/main/java/com/ban/vehicle_management/domain/ai/policy/AiToolRegistry.java)
- [AssistantOrchestrator.java](../../../../../src/main/java/com/ban/vehicle_management/application/ai/service/AssistantOrchestrator.java)
- [AssistantQuestionResolver.java](../../../../../src/main/java/com/ban/vehicle_management/domain/ai/model/AssistantQuestionResolver.java)
- [VietnameseQueryNormalizer.java](../../../../../src/main/java/com/ban/vehicle_management/domain/ai/model/VietnameseQueryNormalizer.java)
- [AssistantOutputValidator.java](../../../../../src/main/java/com/ban/vehicle_management/domain/ai/model/AssistantOutputValidator.java)
- [SwipeEntryPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/parking/pages/SwipeEntryPage.tsx)
- [SwipeListPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/parking/pages/SwipeListPage.tsx)
- [NearbyParkingLotsPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/parking/pages/NearbyParkingLotsPage.tsx)
- [NearbyParkingMap.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/parking/components/NearbyParkingMap.tsx)
- [ContactPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/customer-portal/pages/ContactPage.tsx)
- [PortalShared.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/customer-portal/pages/PortalShared.tsx)
- [GuestSupportConversationProvider.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/support/context/GuestSupportConversationProvider.tsx)

## Sửa runtime sau nghiệm thu nội dung

- [AssistantGuideFocus.java](../../../../../src/main/java/com/ban/vehicle_management/domain/ai/model/AssistantGuideFocus.java)
- [GroundedContextPack.java](../../../../../src/main/java/com/ban/vehicle_management/domain/ai/model/GroundedContextPack.java)
- [AssistantResponseStyle.java](../../../../../src/main/java/com/ban/vehicle_management/domain/ai/model/AssistantResponseStyle.java)
- [PiiRedactionService.java](../../../../../src/main/java/com/ban/vehicle_management/application/ai/service/PiiRedactionService.java)
- [AiAssistantProperties.java](../../../../../src/main/java/com/ban/vehicle_management/application/ai/service/AiAssistantProperties.java)
- [PublicContactRedactionTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/ai/service/PublicContactRedactionTest.java)
- [OperationalGuideFocusTest.java](../../../../../src/test/java/com/ban/vehicle_management/domain/ai/model/OperationalGuideFocusTest.java)
- [GuestPublicAssistantServiceTest.java](../../../../../src/test/java/com/ban/vehicle_management/application/operations/guestsupport/service/GuestPublicAssistantServiceTest.java)

Các test này xác minh logic và ranh giới; chỉ transcript Gemini thật mới dùng để chấm chất lượng câu trả lời. Không coi fixture business port là đã thực hiện nghiệp vụ thật.

## Kiểm thử cấu hình và FAQ liên hệ cuối
- `src/main/java/com/ban/vehicle_management/domain/ai/model/GroundedFaqLookup.java`: lấy nguyên FAQ liên hệ công ty từ bằng chứng, giới hạn ý định và từ chối nguồn mâu thuẫn.
- `src/test/java/com/ban/vehicle_management/domain/ai/model/CompanyContactFaqLookupTest.java`: kiểm tra nguồn thay đổi, câu hỏi form khác ý, tài khoản/bãi/tài chính/thay đổi/thời gian và xung đột.
- `src/test/java/com/ban/vehicle_management/ai/golden/ApprovedModelRegistryFixture.java` và `ApprovedModelRegistryFixtureTest.java`: nạp snapshot đã duyệt, giữ policy/router thật, không ép phê duyệt hoặc giới hạn ba candidate.
- `scripts/run_ai_knowledge_live.py --model-registry`: truyền snapshot không chứa key, ghi hash và cấu hình của từng run.
