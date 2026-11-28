---
title: Nhân sự, vai trò và phạm vi thao tác của đối tác
slug: partner-employees-roles-and-permissions
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
Nhân sự, vai trò và phạm vi thao tác của đối tác.

## Đối tượng
Nhân sự được cấp quyền đọc hướng dẫn nội bộ. Nguồn chứa quy tắc chung, không chứa hồ sơ hay cấu hình riêng của đơn vị.

## Phạm vi
Tên vai trò giống nhau không cho phép xem mọi đơn vị. Quyền thao tác, quyền đọc tri thức và phạm vi dữ liệu là ba kiểm tra khác nhau. Không hướng dẫn tự thêm quyền cho bản thân, đổi tổ chức qua URL hoặc sửa DB. Chatbot chưa có công cụ xem/gán/duyệt nhân sự trực tiếp.

## Điều kiện trước
Cần tài khoản có quyền tác vụ, trạng thái cho phép dùng nghiệp vụ và đúng phạm vi tổ chức/bãi. Quyền đọc tài liệu nội bộ chỉ cho phép đọc hướng dẫn, không tự cấp quyền xem danh sách nhân sự.

## Luồng chính
1. Tạo tài khoản theo luồng quản trị tài khoản đã được cấp quyền; hồ sơ nhân sự và yêu cầu duyệt được hình thành theo vai trò/luồng tài khoản. Chưa có luồng mời nhân viên qua email riêng ở phần quản lý nhân viên.
2. Người có thẩm quyền mở Nhân sự nội bộ trong màn hình xét duyệt. Quyền duyệt còn phụ thuộc vai trò đích, người tạo và phạm vi đơn vị/bãi.
3. Mở danh sách nhân viên để xem hoặc sửa hồ sơ trong phạm vi được cấp. Đổi trạng thái bằng thao tác Hoạt động, Ngừng hoạt động hoặc Tạm khóa tương ứng; không trộn thay đổi trạng thái với sửa thông tin thường.
4. Phân công đúng bãi và vai trò vận hành qua chức năng nhân sự được cấp quyền. Quản trị phân quyền mở Vai trò, Quyền theo module, kiểm tra rồi nhấn Lưu thay đổi nếu có quyền. Đây là đồng bộ bộ quyền của cả vai trò: quyền không chọn có thể bị thu hồi, ảnh hưởng mọi tài khoản dùng vai trò đó; không phải cấp riêng cho một nhân viên.

## Luồng lỗi và ngoại lệ
Nhân viên chỉ được tạm khóa từ trạng thái đang hoạt động. Ngày vào làm không ở tương lai; mã nhân viên, điện thoại, giấy tờ không được vi phạm kiểm tra trùng. Vai trò hệ thống không được sửa như vai trò tùy chỉnh; vai trò đang gán tài khoản bị chặn ngừng sử dụng qua luồng xóa.
Khi báo “vẫn lỗi”, “không thấy”, “không được”, hỏi màn hình, thao tác, thông báo lỗi đã che dữ liệu nhạy cảm và bãi/đơn vị liên quan. Không kết luận nguyên nhân chỉ từ câu báo lỗi ngắn.

## Câu hỏi khách hàng thường gặp

- Tạo nhân viên cho bãi như thế nào? Dùng luồng quản trị tài khoản được cấp quyền; hồ sơ nhân sự và yêu cầu duyệt phụ thuộc vai trò tài khoản. Không có luồng mời email riêng trong quản lý nhân viên; trước vận hành cần xét duyệt và phân công bãi đúng phạm vi.

  **Cách hỏi tương đương:** Muốn thêm nhân sự thì bắt đầu từ đâu?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Duyệt nhân sự nội bộ như thế nào? Mở Nhân sự nội bộ ở màn hình xét duyệt nếu có quyền. Người duyệt còn phải đúng phạm vi vai trò đích, đơn vị/bãi và quy tắc người tạo; không phải mọi quản trị viên đều duyệt được mọi hồ sơ.

  **Cách hỏi tương đương:** Ai có thể xét duyệt hồ sơ nhân viên của đối tác?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Hai người cùng vai trò có xem mọi tổ chức không? Không. Quyền thao tác và phạm vi đơn vị/bãi được kiểm tra riêng; cùng tên vai trò không cho phép xem tổ chức khác. Cần đối chiếu quyền thực tế và bãi được phân công.

  **Cách hỏi tương đương:** Cùng tên vai trò có đồng nghĩa cùng phạm vi dữ liệu không?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Ngừng hoạt động và tạm khóa nhân viên như thế nào? Dùng thao tác trạng thái dành riêng trên nhân viên trong phạm vi bạn được quản lý và có quyền cập nhật. Tạm khóa yêu cầu nhân viên đang hoạt động; không đổi trạng thái bằng cách sửa thông tin thường.

  **Cách hỏi tương đương:** Tôi đổi trạng thái nhân viên ở đâu?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Cấp quyền cho vai trò như thế nào? Nếu được cấp quyền phân quyền, mở Vai trò, chọn Quyền theo module và kiểm tra trước khi nhấn Lưu thay đổi. Lưu đồng bộ bộ quyền của vai trò, có thể thu hồi quyền không chọn và ảnh hưởng mọi tài khoản dùng vai trò đó. Không tự thêm quyền cho bản thân hoặc cấp vượt phạm vi; vai trò hệ thống và quyền hệ thống có các giới hạn bảo vệ riêng.

  **Cách hỏi tương đương:** Lưu quyền của vai trò ở màn hình nào?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Nhân viên và quyền? Bạn muốn tạo tài khoản nhân sự, duyệt hồ sơ, phân công bãi hay chỉnh quyền? Cho biết màn hình và bãi liên quan; tôi cần làm rõ trước khi hướng dẫn.

  **Cách hỏi tương đương:** Tôi cần trợ giúp về nhân sự

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Hỏi rõ nhu cầu và màn hình trước khi trả lời chi tiết.

- Tôi không có quyền, chỉ cách tự nâng thành quản trị đi? Tôi không hỗ trợ tự nâng quyền hay vượt phạm vi được cấp. Hãy gửi nhu cầu tới người quản trị có thẩm quyền để xét quyền phù hợp.

  **Cách hỏi tương đương:** Có cách tự cấp quyền quản trị cho tài khoản mình không?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Liệt kê nhân viên đang làm tại bãi của tôi? Cần xác định bãi và kiểm tra quyền đọc nhân sự cùng phạm vi dữ liệu. Chatbot chưa có công cụ đọc danh sách nhân viên; hãy mở danh sách nhân viên được cấp quyền, không lấy dữ liệu tổ chức khác.

  **Cách hỏi tương đương:** Kiểm tra danh sách nhân sự hiện tại của bãi giúp tôi

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Duyệt và gán quyền nhân viên này giúp tôi ngay? Chatbot chưa có công cụ duyệt nhân sự hoặc gán quyền. Người có thẩm quyền phải kiểm tra hồ sơ, quyền và phạm vi rồi thực hiện trên màn hình phù hợp; tôi chưa thực hiện thay đổi nào.

  **Cách hỏi tương đương:** Bạn phê duyệt nhân viên và cấp vai trò cho tôi được không?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Tài khoản ngừng hoạt động vẫn phân quyền được không? Tài khoản ngừng hoạt động không được dùng quyền nghiệp vụ như tài khoản còn hoạt động. Liên hệ người phụ trách để kiểm tra trạng thái và quyền; không dùng phiên cũ hoặc cách khác để vượt chặn.

  **Cách hỏi tương đương:** Tài khoản đã ngừng thì còn được quản lý nhân viên không?

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
- [EmployeeUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/people/employee/usecase/EmployeeUseCaseImpl.java) — getEmployees; updateEmployee; activateEmployee; inactivateEmployee; suspendEmployee
- [EmployeeAccessGuard.java](../../../../../src/main/java/com/ban/vehicle_management/application/people/employee/authorization/EmployeeAccessGuard.java) — ensureCanRead; ensureCanManage; filterReadableEmployees
- [InternalEmployeeApprovalUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/operations/approvalrequest/usecase/InternalEmployeeApprovalUseCaseImpl.java) — approveInternalEmployeeApproval; rejectInternalEmployeeApproval
- [InternalEmployeeApprovalAccessGuard.java](../../../../../src/main/java/com/ban/vehicle_management/application/operations/approvalrequest/authorization/InternalEmployeeApprovalAccessGuard.java) — canAccessTarget
- [RolePermissionController.java](../../../../../src/main/java/com/ban/vehicle_management/entrypoint/controller/iam/RolePermissionController.java) — assignPermissionsToRole; revokePermissionFromRole
- [AiToolRegistry.java](../../../../../src/main/java/com/ban/vehicle_management/domain/ai/policy/AiToolRegistry.java) — registered tool definitions
- [EmployeeListPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/employees/pages/EmployeeListPage.tsx)
- [OnboardingApprovalPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/iam/pages/OnboardingApprovalPage.tsx)
- [RolePermissionPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/iam/pages/RolePermissionPage.tsx)

## Nội dung cần review
Chưa có review nghiệp vụ/an ninh độc lập bởi người trong team. Kiểm thử mock/offline không nghiệm thu Google. Luồng nhập/duyệt trong DB kiểm thử không phê duyệt phát hành lên DB dùng chung.

## Lịch sử phiên bản
- 1.0.0 — 2026-11-27: biên soạn từ code hiện tại, giữ DRAFT.
<!-- authoring-only:end -->
