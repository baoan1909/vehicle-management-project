# Review nội dung độc lập đợt 5

Ngày 2026-11-27. Reviewer là agent AI khác với tác giả chính; chưa thay thế review/an ninh hoặc phê duyệt của người trong team. Reviewer không dùng kết quả holdout để yêu cầu chỉnh runtime.

Các phát hiện đã đưa vào nguồn và đáp án chính:

| Phát hiện | Xử lý | Code |
|---|---|---|
| Ngừng phục vụ loại xe tại mọi bãi còn ngừng quy tắc giá hoạt động, kể cả đơn vị chỉ có một bãi | Thêm cảnh báo tác động trước xác nhận | `ParkingLotCatalogAvailabilityUseCaseImpl.setVehicleTypeEnabled`, `coversEveryPartnerLot` |
| Lưu quyền đồng bộ cả vai trò, có thể thu hồi quyền không chọn và ảnh hưởng mọi tài khoản dùng vai trò | Thêm ranh giới với cấp quyền riêng một nhân viên | `AssignPermissionsToRoleUseCaseImpl.assignPermissionsToRole` |
| Đăng ký chặn trùng điện thoại | Bổ sung điều kiện đầu vào | `PartnerRegistrationUseCaseImpl.ensureRegistrationHasNoConflict` |
| Hồ sơ đã từ chối không sửa trực tiếp qua hoàn thiện hồ sơ | Nêu phải gửi lại tạo trạng thái chờ trước rồi bổ sung theo luồng được phép | `resubmitMyRegistration`, `completeMyProfile` |
| API sửa hồ sơ tổ chức còn nhưng quyền đã retire | Giữ nguồn nội bộ riêng, không hướng dẫn gọi API cũ | `V20261021190000__retire_partner_organization_profile_access.sql` |
| Cấu hình topology có thể được cấp cho quản lý bãi được phân công | Dùng quyền + phạm vi thay tên vai trò tuyệt đối | `OrganizationAccessGuard.ensureCanConfigureParkingLot` |
| Xuất bản sơ đồ tạo/cập nhật dữ liệu ô đỗ thật | Phân biệt ô đỗ với phần tử trang trí | `ParkingLayoutUseCaseImpl.publishLayout` |

Metadata các ca hướng dẫn chưa đủ chứng minh kiểm quyền khi actor ngừng hoạt động hoặc dữ liệu thật ngoài phạm vi. Đã thêm trường actor/target và ghi rõ giới hạn; harness bổ sung kiểm tra HTTP quyền ở phần setup, nhưng cần xem kết quả thực chạy và không công nhận gate toàn bộ từ vài fixture.

Tác giả holdout báo có thể có đáp án dựa vào API hồ sơ tổ chức trước khi kiểm migration retire. Giữ nguyên hash holdout và đánh dấu `GOLD_REVIEW_REQUIRED` trong file trạng thái riêng. Cần người độc lập adjudicate theo code trước khi chấm; không sửa gold để làm đẹp kết quả và không dùng holdout tuning runtime.

## Review code và harness

Agent reviewer đã kiểm tra read-only phần định tuyến, policy capability gap, tạo index và harness. Đã sửa các regression: tạo phiếu hỗ trợ với chủ đề sơ đồ/danh mục; đọc dữ liệu xe/vé cá nhân có nhắc loại xe/vé; nhận diện tạo nhân sự, xóa sơ đồ, quản trị bảng giá/voucher; câu phủ định và yêu cầu thao tác lịch sự. Có test riêng cho các ranh giới này.

Tạo index khi có candidate DRAFT/BUILDING dùng cùng advisory lock với điều phối tự động và trả Conflict; READY không được coi là candidate đang chờ. Harness chọn đúng trạng thái, không tạo index trùng; bắt cả AssertionError khi lưu checkpoint; không dừng vì một failure event lịch sử đã phục hồi; resume so sánh model/policy thay vì ID DB sinh ngẫu nhiên.

Đã ghi nhận giới hạn quyền: migration mặc định chỉ cấp AI_KNOWLEDGE_READ_EMPLOYEE cho EMPLOYEE/PARKING_MANAGER, không cấp cho PARTNER_ADMIN. Không tự mở scope hoặc tăng quyền của đối tác. Fixture dùng quyền test riêng phải được phân biệt với quyền mặc định khi đánh giá phát hành.
