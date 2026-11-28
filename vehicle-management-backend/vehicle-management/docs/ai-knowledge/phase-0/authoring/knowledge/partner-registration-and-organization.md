---
title: Đăng ký đối tác và hồ sơ đơn vị ban đầu
slug: partner-registration-and-organization
documentVersion: 1.0.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Code backend, policy, migration và giao diện đã đối chiếu ngày 2026-11-27
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-27
---

## Mục đích
Đăng ký đối tác và hồ sơ đơn vị ban đầu.

## Đối tượng
Người tìm hiểu đăng ký đối tác; không cần quyền đọc tài liệu nội bộ.

## Phạm vi
Tài khoản đăng nhập, hồ sơ cá nhân, yêu cầu đối tác, đơn vị và quyền vận hành là các phần khác nhau. Không có cam kết thời gian duyệt cố định. Hướng dẫn quản trị đơn vị nội bộ nằm trong nguồn riêng, không mở công khai cùng tài liệu này.

## Điều kiện trước
Người đăng ký dùng email chính chủ, cung cấp thông tin hợp lệ, không trùng tên đăng nhập, email, số điện thoại hoặc mã đơn vị đã tồn tại hay đang chờ duyệt. Trang đăng ký công khai không cấp quyền vận hành ngay.

## Luồng chính
1. Mở Đăng ký tài khoản đối tác, nhập Họ và tên, Tên đăng nhập, Email, Mật khẩu, Xác nhận mật khẩu, Số điện thoại, Mã đơn vị, Tên đơn vị; chọn Đăng ký trở thành đối tác.
2. Xác minh email theo thư nhận được. Đăng nhập để theo dõi hồ sơ và hoàn thiện hồ sơ cá nhân cùng địa chỉ đơn vị ở phần hồ sơ được phép.
3. Trước xét duyệt, hoàn thiện họ tên, điện thoại, ngày sinh, giới tính, giấy tờ định danh, địa chỉ cá nhân và địa chỉ đơn vị qua màn hình hồ sơ bảo mật. Không gửi các dữ liệu này vào chat.
4. Chờ xét duyệt. Duyệt thủ công yêu cầu người có thẩm quyền; duyệt tự động chỉ khi có chính sách đang áp dụng và đủ điều kiện. Khi duyệt thành công mới tạo đơn vị hoạt động, liên kết người đăng ký và sao chép danh mục dùng chung.

## Luồng lỗi và ngoại lệ
Hồ sơ chờ duyệt có thể bổ sung qua luồng hoàn thiện hồ sơ; không đổi mã đơn vị đã đăng ký. Hồ sơ bị từ chối cần đọc ghi chú; chỉ gửi lại khi đã xác minh email, tài khoản còn đủ điều kiện và không có hồ sơ đang chờ hoặc đã duyệt. Gửi lại tạo yêu cầu chờ duyệt mới, sau đó mới bổ sung qua luồng hoàn thiện hồ sơ; hồ sơ đã bị từ chối không sửa trực tiếp qua luồng này. Gửi lại không phải tự được duyệt.
Khi báo “vẫn lỗi”, “không thấy”, “không được”, hỏi màn hình, thao tác, thông báo lỗi đã che dữ liệu nhạy cảm và bãi/đơn vị liên quan. Không kết luận nguyên nhân chỉ từ câu báo lỗi ngắn.

## Câu hỏi khách hàng thường gặp

- Đăng ký đối tác như thế nào? Mở Đăng ký tài khoản đối tác, điền thông tin cá nhân, tài khoản, điện thoại, mã và tên đơn vị rồi chọn Đăng ký trở thành đối tác. Xác minh email, hoàn thiện hồ sơ và chờ xét duyệt trước khi vận hành bãi.

  **Cách hỏi tương đương:** Muốn trở thành đối tác thì cần làm gì?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Đăng ký đối tác cần những thông tin nào? Lúc tạo tài khoản cần họ tên, tên đăng nhập, email, mật khẩu, số điện thoại, mã và tên đơn vị. Trước duyệt còn phải hoàn thiện ngày sinh, giới tính, giấy tờ và địa chỉ cá nhân/đơn vị trên màn hình hồ sơ bảo mật; không gửi giấy tờ hay mật khẩu trong chat.

  **Cách hỏi tương đương:** Tôi cần chuẩn bị gì để nộp đăng ký đối tác?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Tôi xác minh email rồi có vận hành bãi ngay không? Xác minh email chưa đủ để vận hành. Bạn còn cần hoàn thiện hồ sơ và được xét duyệt; đăng nhập theo dõi trạng thái không đồng nghĩa đã có quyền quản trị bãi.

  **Cách hỏi tương đương:** Đã xác thực email nhưng chưa duyệt thì dùng quản trị bãi được chưa?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Hồ sơ đối tác bị từ chối gửi lại như thế nào? Đọc ghi chú từ chối và gửi lại khi đã xác minh email, tài khoản còn đủ điều kiện. Gửi lại tạo hồ sơ chờ duyệt mới, sau đó mới bổ sung theo luồng hoàn thiện hồ sơ; không sửa trực tiếp hồ sơ đã từ chối. Không gửi lại khi đang chờ hoặc đã được duyệt; vẫn phải qua xét duyệt.

  **Cách hỏi tương đương:** Bị từ chối đăng ký đối tác thì phải làm sao?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Mã đơn vị đăng ký bị trùng thì sửa như thế nào? Mã đơn vị đã tồn tại hoặc đang có hồ sơ chờ duyệt sẽ bị chặn. Không đổi mã đơn vị đã đăng ký qua phần hoàn thiện hồ sơ; hãy đối chiếu lỗi và liên hệ người phụ trách khi cần xử lý.

  **Cách hỏi tương đương:** Có được đổi mã đơn vị sau khi đã nộp hồ sơ không?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Đối tác? Bạn muốn tìm hiểu đăng ký mới, hồ sơ chờ duyệt hay quản trị đơn vị? Cho biết màn hình và việc bạn muốn làm; không gửi mật khẩu hay giấy tờ trong chat.

  **Cách hỏi tương đương:** Tôi muốn hỏi về đối tác

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Hỏi rõ nhu cầu và màn hình trước khi trả lời chi tiết.

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
- [PartnerRegistrationUseCaseImpl.java](../../../../../src/main/java/com/ban/vehicle_management/application/iam/partnerregistration/usecase/PartnerRegistrationUseCaseImpl.java) — submitRegistration; completeMyProfile; approveRegistration; resubmitMyRegistration
- [PartnerRegistrationPolicy.java](../../../../../src/main/java/com/ban/vehicle_management/domain/iam/partnerregistration/policy/PartnerRegistrationPolicy.java) — normalize
- [PartnerApprovalValidator.java](../../../../../src/main/java/com/ban/vehicle_management/domain/iam/partnerregistration/policy/PartnerApprovalValidator.java) — validateBeforeApproval
- [AiToolRegistry.java](../../../../../src/main/java/com/ban/vehicle_management/domain/ai/policy/AiToolRegistry.java) — registered tool definitions
- [PartnerRegistrationPage.tsx](../../../../../../../vehicle-management-frontend/vehicle_management_react/src/features/customer-portal/pages/PartnerRegistrationPage.tsx)

## Nội dung cần review
Chưa có review nghiệp vụ/an ninh độc lập bởi người trong team. Kiểm thử mock/offline không nghiệm thu Google. Luồng nhập/duyệt trong DB kiểm thử không phê duyệt phát hành lên DB dùng chung.

## Lịch sử phiên bản
- 1.0.0 — 2026-11-27: biên soạn từ code hiện tại, giữ DRAFT.
<!-- authoring-only:end -->
