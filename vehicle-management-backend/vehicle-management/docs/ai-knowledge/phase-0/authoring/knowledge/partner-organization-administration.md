---
title: Quản trị tổ chức và phạm vi bãi xe
slug: partner-organization-administration
documentVersion: 1.0.0
status: DRAFT
language: vi
accessScope: EMPLOYEE
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Code backend, policy, migration và giao diện đã đối chiếu ngày 2026-11-27
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-27
---

## Mục đích
Quản trị tổ chức và phạm vi bãi xe.

## Đối tượng
Nhân sự được cấp quyền đọc hướng dẫn nội bộ. Nguồn chứa quy tắc chung, không chứa hồ sơ hay cấu hình riêng của đơn vị.

## Phạm vi
Luồng tự đề xuất và duyệt thay đổi hồ sơ tổ chức đã rút quyền mặc định của đối tác và người duyệt hệ thống; không còn là chức năng được hỗ trợ cho đối tác. Không hướng dẫn gọi đường truy cập cũ để vượt giới hạn. Chưa có chức năng ngừng hoạt động tổ chức trong phần quản trị này; không khẳng định chatbot hoặc đối tác có nút này.

## Điều kiện trước
Chỉ hướng dẫn nội bộ chung, không chứa dữ liệu đơn vị thật. Cần quyền xem/quản lý phù hợp và đơn vị hoạt động trong phạm vi được cấp; tên vai trò không thay thế kiểm tra quyền và phạm vi.

## Luồng chính
1. Người xét duyệt mở Hồ sơ đăng ký đối tác, kiểm tra thông tin đơn vị/người đăng ký và ghi chú xét duyệt trước khi phê duyệt hoặc từ chối.
2. Người có quyền quản lý thành viên chỉ phân công quản lý đang hoạt động vào bãi thuộc đúng đơn vị. Hệ thống kiểm tra liên kết thành viên và phạm vi bãi.
3. Xác minh quyền thực tế khi cần sửa hồ sơ đơn vị; chuyển người phụ trách có thẩm quyền theo quy trình hiện hành.

## Luồng lỗi và ngoại lệ
Mã đơn vị trùng bị chặn. Quản lý khác tổ chức hoặc bãi không thuộc đơn vị không được gán. Nhiều đơn vị cùng hoạt động cần làm rõ đơn vị; không tự chọn thay người dùng.
Khi báo “vẫn lỗi”, “không thấy”, “không được”, hỏi màn hình, thao tác, thông báo lỗi đã che dữ liệu nhạy cảm và bãi/đơn vị liên quan. Không kết luận nguyên nhân chỉ từ câu báo lỗi ngắn.

## Câu hỏi khách hàng thường gặp

- Đổi tên và địa chỉ tổ chức như thế nào? Luồng tự đề xuất sửa hồ sơ tổ chức đã bị rút khỏi quy trình đối tác hiện hành. Hãy liên hệ người phụ trách có thẩm quyền; không dùng API cũ hoặc coi thay đổi đã được duyệt.

  **Cách hỏi tương đương:** Đối tác có tự sửa hồ sơ đơn vị trên giao diện hiện tại không?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Cho tôi xem hồ sơ tổ chức khác dù tôi không có quyền? Tôi không thể cung cấp hồ sơ đơn vị ngoài phạm vi bạn được cấp. Hãy nhờ người quản trị kiểm tra quyền và đơn vị phụ trách qua kênh phù hợp.

  **Cách hỏi tương đương:** Có cách xem thông tin đơn vị ngoài phạm vi được giao không?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Tổ chức của tôi hiện có bao nhiêu bãi đang hoạt động? Cần đọc số liệu thật của đúng đơn vị và kiểm tra quyền; chatbot chưa có công cụ tra cứu tổ chức này. Bạn hãy xem màn hình quản trị được cấp quyền hoặc liên hệ người phụ trách, tôi không thể đoán số bãi.

  **Cách hỏi tương đương:** Kiểm tra số bãi hoạt động thực tế của đơn vị tôi giúp tôi

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Gán quản lý sang bãi của tổ chức khác giúp tôi? Không thể gán quản lý vào bãi ngoài đơn vị được phép quản lý. Phải kiểm tra tài khoản quản lý còn hoạt động và bãi thuộc đúng tổ chức; chatbot không thực hiện việc phân công này.

  **Cách hỏi tương đương:** Tôi muốn cấp quản lý vào bãi ngoài đơn vị của mình

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

## Điều chatbot được phép trả lời
Hướng dẫn theo tài liệu còn hiệu lực, đúng phạm vi và quyền đọc; dùng tên màn hình, nút bấm dễ hiểu. Không nêu mã quyền hoặc tên kỹ thuật khi người dùng chỉ hỏi cách thao tác. Tài liệu này đang là bản soạn thảo; chỉ sử dụng làm nguồn trả lời sau quy trình kiểm tra và duyệt có thẩm quyền.

## Điều chatbot phải dùng chức năng được cấp quyền
Yêu cầu xem trạng thái, danh sách, số lượng, phân công hoặc cấu hình thực tế cần công cụ đọc dữ liệu được phép và xác định đúng người/tổ chức/bãi. Bộ công cụ chatbot hiện tại chưa có công cụ cho các nghiệp vụ này; báo chưa hỗ trợ, hướng dẫn màn hình hoặc chuyển người phụ trách. Không dùng tài liệu thay dữ liệu thật.

## Điều chatbot phải từ chối
- Không tiết lộ dữ liệu ngoài quyền và phạm vi.
- Không khẳng định đã tạo, duyệt, phân công, sửa hoặc xuất bản khi chưa thực hiện.
- Không hướng dẫn tự nâng quyền hay gọi API cũ để vượt chặn.
- Không bịa dữ liệu, mức giá, thời gian duyệt hoặc tính năng chưa có.
- Không yêu cầu gửi mật khẩu, token, giấy tờ đầy đủ hoặc bí mật trong chat.

## Khi nào hỏi lại hoặc chuyển nhân viên
Hỏi lại khi chưa rõ chủ đề, bãi, đơn vị hoặc bước đang lỗi. Chuyển người phụ trách khi thiếu chức năng, quyền, dữ liệu thật hoặc hồ sơ cần xét duyệt; không tạo phiếu hỗ trợ tự động nếu chưa có yêu cầu và chức năng hợp lệ.

<!-- authoring-only:start -->
## Nguồn đối chiếu
- [OrganizationUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/iam/organization/usecase/OrganizationUseCaseImpl.java) — getAccessibleOrganizations; assignParkingManagerToParkingLot
- [OrganizationAccessGuard.java](../../../../../src/main/java/com/ban/vehicle_management/application/iam/organization/authorization/OrganizationAccessGuard.java) — ensureCanAccessOrganization; ensureCanConfigureParkingLot; ensureCanOperateParkingLot
- [OrganizationProfileUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/iam/organizationprofile/usecase/OrganizationProfileUseCaseImpl.java) — requestMyProfileUpdate; approveProfileUpdate
- [AiToolRegistry.java](../../../../../src/main/java/com/ban/vehicle_management/domain/ai/policy/AiToolRegistry.java) — registered tool definitions
- [PartnerRegistrationManagementPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/iam/pages/PartnerRegistrationManagementPage.tsx)
- [Migration rút quyền hồ sơ tổ chức](../../../../../src/main/resources/db/migration/V20261021190000__retire_partner_organization_profile_access.sql)

## Nội dung cần review
Chưa có review nghiệp vụ/an ninh độc lập bởi người trong team. Kiểm thử mock/offline không nghiệm thu Google. Luồng nhập/duyệt trong DB kiểm thử không phê duyệt phát hành lên DB dùng chung.

## Lịch sử phiên bản
- 1.0.0 — 2026-11-27: biên soạn từ code hiện tại, giữ DRAFT.
<!-- authoring-only:end -->
