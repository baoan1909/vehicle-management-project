---
title: Quản lý phương tiện của tôi
slug: customer-vehicle-management
documentVersion: 2.1.0
status: DRAFT
language: vi
accessScope: CUSTOMER
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Giao diện, nghiệp vụ, phân quyền và migration đã đối chiếu trong bằng chứng đợt 2
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-24
---

## Mục đích
Hướng dẫn thêm xe, sửa thông tin, đặt xe mặc định và ngưng dùng xe trong tài khoản CoParking.

## Đối tượng
Khách hàng đã đăng nhập, hồ sơ đã được duyệt và tài khoản đang hoạt động. Nhân sự chỉ quản lý theo quyền và phạm vi được giao; chat không tra cứu xe khách khác.

## Phạm vi
Thông tin phương tiện cá nhân: loại xe, biển số, hãng, màu sắc, trạng thái và xe mặc định. Chatbot không tự thêm, sửa, khóa hoặc xóa xe.

## Thuật ngữ
Xe mặc định là xe được chọn sẵn khi sử dụng chức năng có hỗ trợ lựa chọn này. Xe phải đang hoạt động mới được đặt làm mặc định. Mỗi hồ sơ chỉ có một xe mặc định.

## Điều kiện trước
Hồ sơ khách hàng phải được duyệt, đang hoạt động và có quyền sử dụng chức năng. Chọn loại xe hợp lệ. Biển số cần đúng với phương tiện; riêng xe đạp có thể không cần biển số theo loại xe được hệ thống cấu hình.

## Luồng chính
1. Mở Xe của tôi, chọn Thêm xe mới.
2. Chọn Loại xe; nhập Biển số xe và các thông tin phù hợp. Không dùng biển số của xe khác.
3. Chọn Đặt làm xe mặc định nếu cần, rồi chọn Lưu xe.
4. Kiểm tra xe vừa lưu trong Danh sách xe đã đăng ký. Khi cần sửa, chọn xe và mở Chỉnh sửa phương tiện.

## Luồng thay thế
- Chọn Đặt xe mặc định trên xe đang hoạt động; xe mặc định cũ sẽ được bỏ lựa chọn.
- Chọn Ngưng dùng xe khi không còn sử dụng. Xe không bị xóa vĩnh viễn khỏi dữ liệu lịch sử; có thể Kích hoạt xe khi đủ điều kiện.
- Nhân sự có quyền quản trị phù hợp mới được khóa xe. Khách hàng không tự mở khóa xe bị khóa.

## Luồng lỗi
- Biển số đã tồn tại: kiểm tra lại biển số và danh sách xe, không thêm lặp. Nếu thông tin thuộc xe khác hoặc có tranh chấp, nhờ nhân viên xác minh; chat không tiết lộ chủ xe.
- Không sửa được biển số: xe có vé đang hoạt động hoặc lượt gửi chưa kết thúc có thể bị chặn thay đổi biển số. Kiểm tra vé và lịch sử trước khi nhờ nhân viên xử lý.
- Không đặt được xe mặc định: kiểm tra xe có đang hoạt động không. Xe ngưng dùng hoặc bị khóa không được đặt mặc định.
- Không tải được danh sách: kiểm tra đăng nhập và quyền truy cập; thử tải lại màn hình. Không kết luận danh sách trống khi việc tải dữ liệu thất bại.

## Trạng thái và chuyển trạng thái
- Đang hoạt động: có thể dùng cho đăng ký khi đáp ứng các điều kiện khác.
- Ngưng dùng: phương tiện tạm ngừng sử dụng; có thể kích hoạt lại theo quyền.
- Bị khóa: cần nhân sự có thẩm quyền kiểm tra; không hướng dẫn lách khóa.
Thao tác xóa phương tiện của hệ thống hiện chuyển xe sang ngưng dùng, không xóa lịch sử. Đổi trạng thái không đồng nghĩa tự hủy các vé hoặc lượt gửi liên quan.

## Câu hỏi khách hàng thường gặp
- Thêm xe mới như thế nào? Mở Xe của tôi, chọn Thêm xe mới, chọn loại xe và nhập biển số cùng thông tin phù hợp. Xe đạp có thể để trống biển số theo loại xe được cấu hình. Nếu muốn dùng xe mặc định, chọn tùy chọn đó rồi Lưu xe và kiểm tra danh sách; hồ sơ, quyền và xe cần đáp ứng điều kiện hệ thống.
- Xe đạp không có biển số thì sao? Chọn đúng loại xe đạp; hệ thống có hỗ trợ biển số để trống cho loại này.
- Xe mặc định là gì? Là xe được chọn sẵn; phải là xe đang hoạt động.
- Biển số đã có rồi phải làm sao? Kiểm tra xe đã đăng ký và biển số nhập; nếu vẫn không đúng, nhờ nhân viên xác minh.
- Vì sao không đổi được biển số? Kiểm tra vé đang dùng và lượt gửi chưa kết thúc.
- Xóa xe có xóa lịch sử không? Không. Thao tác hiện chuyển xe sang ngưng dùng.
- Có thể xem xe của tôi trong chat không? Có, khi đủ quyền; trợ lý chỉ tóm tắt các xe gần đây của chính bạn và che một phần biển số.
- Chat có thêm hoặc ngưng dùng xe hộ tôi không? Chưa hỗ trợ; thao tác trên Xe của tôi và kiểm tra trước khi lưu.
- Chỉ hỏi “xe” hoặc “biển số” thì sao? Trợ lý cần hỏi bạn muốn thêm xe, sửa thông tin hay xem danh sách.
- “Không được” sau câu hỏi thêm xe: hỏi bước gặp lỗi và thông báo cụ thể; không tự thêm xe.
- Làm sao ngưng dùng xe mà vẫn giữ lịch sử? Chọn Xóa phương tiện tại Xe của tôi và kiểm tra xác nhận. Thao tác chuyển xe sang ngưng dùng, giữ các lượt gửi trước đó.
- Chuyện gì xảy ra khi xóa xe? Xe ngưng dùng; thông tin lịch sử vẫn được giữ. Các ràng buộc vé và lượt gửi đang mở phải được hệ thống kiểm tra.
- Lỗi biển số trùng, thêm xe báo trùng biển số hoặc biển số đã tồn tại xử lý thế nào? Kiểm tra biển số nhập và Danh sách xe đã đăng ký. Không thêm lại xe đã có; nếu không phải xe của bạn, nhờ nhân viên xác minh, không đọc thông tin chủ xe khác.

## Điều chatbot được phép trả lời
Hướng dẫn màn hình, điều kiện xe mặc định, sửa biển số, ngưng dùng và ý nghĩa trạng thái. Dùng tên nút bằng tiếng Việt; không đưa mã quyền hoặc tên trạng thái nội bộ vào câu trả lời thông thường.

## Điều chatbot phải dùng chức năng được cấp quyền
Tra cứu danh sách xe của người hỏi phải dùng chức năng đọc phương tiện cá nhân đã kiểm tra quyền. Hiện đọc tối đa 20 xe và hiển thị tối đa 5 mục; hướng dẫn mở Xe của tôi để xem đầy đủ. Không suy ra loại xe, chủ sở hữu hoặc trạng thái từ biển số do người dùng nhập.

## Điều chatbot phải từ chối
Xem hoặc sửa xe người khác, bỏ qua quyền, tự mở khóa xe, thu giấy tờ định danh đầy đủ hoặc khẳng định đã thay đổi phương tiện khi chưa có kết quả thực tế.

## Khi nào chuyển nhân viên
Tranh chấp biển số hoặc chủ xe, xe bị khóa, lỗi dữ liệu tiếp diễn sau khi kiểm tra đúng màn hình. Cần thông tin tối thiểu về bước lỗi; không yêu cầu gửi giấy tờ nhạy cảm vào chat.

## Lưu ý khi trả lời và đối chiếu kết quả
Sửa biển số, ngưng dùng xe và đặt xe mặc định là các thao tác khác nhau. Ngưng dùng không xóa lịch sử, không tự hủy vé và không tự kết thúc lượt gửi. Nếu thao tác bị từ chối, đọc thông báo cụ thể và kiểm tra vé hoặc lượt gửi liên quan; không thêm lại xe để tránh ràng buộc.

<!-- authoring-only:start -->
## Nguồn đối chiếu
Đối chiếu giao diện, quy tắc biển số, phân quyền và việc ngưng dùng tại [bằng chứng đợt 2](../evaluation/batch-02-traceability.md).

## Nội dung TBD
Chatbot chưa hỗ trợ thay đổi phương tiện. Chính sách lưu trữ dữ liệu lâu dài chưa được quyết định trong đợt này. Không tự hứa thời gian xử lý tranh chấp.

## Lịch sử phiên bản
- 2.1.0, 2026-11-24: rà soát lại với code hiện tại; làm rõ điều kiện và giới hạn trả lời, chuẩn hóa ngôn ngữ; tiếp tục DRAFT. Kiểm thử và giới hạn bằng chứng tại báo cáo ALL_BATCHES_REVIEW_STATUS.vi.md.
- 2.0.0, 2026-11-19: xác minh code hiện tại; cập nhật xóa thành ngưng dùng, xe đạp, giới hạn đổi biển số và cách diễn đạt dễ hiểu.
- 1.1.0: bản tham khảo trước khi rà soát đợt 2.
<!-- authoring-only:end -->
