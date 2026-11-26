---
title: Xem lịch sử gửi xe của tôi
slug: parking-history
documentVersion: 2.2.0
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
Hướng dẫn xem lượt xe vào, xe ra, thời gian và phí đã ghi nhận; xử lý khi lịch sử thiếu hoặc không khớp.

## Đối tượng
Khách hàng đã đăng nhập, được duyệt và có quyền xem lịch sử của mình. Nhân sự chỉ xem dữ liệu tại bãi và đơn vị thuộc quyền phụ trách.

## Phạm vi
Lịch sử liên kết với các phương tiện trong tài khoản khách hàng hiện tại, bộ lọc, chi tiết lượt gửi và thông tin được phép hiển thị. Chatbot không sửa hoặc xóa lịch sử.

## Thuật ngữ
Một lượt gửi xe có thông tin xe vào và có thể có xe ra. Chưa có thời gian ra không tự chứng minh xe chắc chắn còn trong bãi: có thể cần đối chiếu sự kiện và dữ liệu vận hành.

## Điều kiện trước
Đăng nhập đúng tài khoản có hồ sơ được duyệt, đang hoạt động và quyền xem lịch sử. Lịch sử trong tài khoản phụ thuộc phương tiện được liên kết; không tra cứu lịch sử người khác bằng biển số hoặc thông tin họ cung cấp qua chat.

## Luồng chính
1. Mở Lịch sử gửi xe.
2. Chọn Khoảng ngày, Tìm kiếm hoặc Trạng thái theo thông tin cần xem. Ngày kết thúc không được trước ngày bắt đầu.
3. Chọn Xem chi tiết ở lượt gửi để đối chiếu thời gian, trạng thái, tổng phí và Sự kiện vào / ra.
4. Nếu cần xem lại toàn bộ, chọn Xóa lọc rồi kiểm tra danh sách. Ngày và giờ được hiển thị theo múi giờ Việt Nam.

## Luồng thay thế
- Có thể hỏi chat về các lượt gửi gần đây của chính mình khi đủ quyền. Chat chỉ trả bản tóm tắt giới hạn và che phần lớn biển số.
- Muốn lọc ngày, tìm một phương tiện cụ thể hoặc xem ảnh, dùng màn hình Lịch sử gửi xe với quyền hiện có. Công cụ chat đợt này chỉ hỗ trợ các lượt gần đây và bộ lọc trạng thái, chưa nhận bộ lọc ngày hoặc biển số.
- Nhân sự sử dụng màn hình quản lý lượt gửi xe theo phân công, không yêu cầu chatbot truy cập dữ liệu toàn đơn vị bằng chức năng tra cứu cá nhân.

## Luồng lỗi
- Không thấy lịch sử: kiểm tra đúng tài khoản, phương tiện, khoảng ngày và trạng thái; chọn Xóa lọc. Danh sách trống khác với lỗi tải dữ liệu.
- Thiếu giờ ra: xem trạng thái và sự kiện; nếu xe đã ra thực tế nhưng chưa ghi nhận, báo nhân viên bãi để đối chiếu. Không tự điền giờ ra.
- Phí không khớp: đối chiếu lượt gửi, thời gian và hóa đơn được phép xem. Chat không tự tính lại hoặc hứa điều chỉnh, hoàn tiền.
- Không xem được ảnh: kiểm tra trên màn hình có quyền. Chat không cấp đường dẫn ảnh riêng tư hoặc lấy ảnh camera thay người dùng.
- Lỗi vẫn tiếp diễn: ghi lại bước thao tác, thời điểm và thông báo lỗi; tránh gửi thông tin cá nhân không cần thiết.

## Trạng thái và chuyển trạng thái
- Đang gửi: lượt gửi chưa hoàn tất theo dữ liệu hiện tại.
- Đã hoàn tất: lượt gửi đã kết thúc.
- Mất thẻ: lượt gửi liên quan quy trình báo mất, không gộp với đã ra thông thường.
- Đã hủy: lượt gửi đã bị hủy theo nghiệp vụ, không đồng nghĩa đã hoàn tất.
Chat không chuyển trạng thái hoặc tự sửa giờ vào, giờ ra. Có giờ ra và trạng thái hợp lệ mới là căn cứ cho câu trả lời cụ thể, không suy đoán chỉ từ tin nhắn cũ.

## Câu hỏi khách hàng thường gặp
- Tìm lịch sử gửi xe theo ngày như thế nào? Mở Lịch sử gửi xe, chọn Khoảng ngày, nhập ngày bắt đầu và ngày kết thúc rồi kiểm tra bộ lọc phương tiện và trạng thái. Ngày kết thúc không được trước ngày bắt đầu. Xóa lọc chỉ bỏ bộ lọc cũ không phù hợp; không thay thế việc chọn ngày cần tìm. Nếu vẫn thiếu lượt, nhờ nhân viên đối chiếu.
- Xem lịch sử ở đâu? Mở Lịch sử gửi xe, chọn khoảng ngày và Xem chi tiết.
- Không dấu “xem lich su gui xe o dau” có hiểu không? Có cùng ý nghĩa hướng dẫn mở màn hình; không tự đọc dữ liệu khi chỉ hỏi cách xem.
- Xe tôi đã ra chưa? Cần dữ liệu được phép và thông tin đủ để xác định lượt; không khẳng định dựa vào tài liệu hướng dẫn.
- Không thấy lượt gửi hôm qua thì sao? Kiểm tra khoảng ngày, phương tiện, trạng thái và Xóa lọc; nếu vẫn thiếu, nhờ nhân viên đối chiếu.
- Thiếu giờ ra phải làm gì? Kiểm tra sự kiện và báo nhân viên nếu xe đã ra thực tế.
- Xem năm lượt gửi gần nhất của tôi được không? Có thể tóm tắt khi đủ quyền, tối đa 5 mục; xem đầy đủ tại màn hình lịch sử.
- Có xem ảnh xe trong chat không? Không. Xem trên màn hình lịch sử với quyền phù hợp, không chia sẻ đường dẫn riêng tư.
- Phí trong lịch sử sai thì sửa ở đâu? Đối chiếu hóa đơn và liên hệ nhân viên; chatbot không sửa phí.
- Chỉ hỏi “lịch sử” thì sao? Hỏi rõ bạn muốn được hướng dẫn hay xem các lượt gần đây của mình.
- “Không được” sau câu hỏi xem lịch sử: tiếp tục hỏi bộ lọc hoặc bước gặp lỗi, không chuyển sang thao tác sửa dữ liệu.
- Vì sao tổng phí trong lịch sử không đúng? Đối chiếu các lượt gửi, khoảng ngày, trạng thái và hóa đơn trên màn hình. Nếu vẫn lệch, nhờ nhân viên kiểm tra; chat không tự sửa phí hoặc khẳng định hệ thống tính sai khi chưa có dữ liệu.
- Có nút Tìm kiếm để áp dụng bộ lọc không? Tìm kiếm là ô nhập từ khóa, Trạng thái là danh sách chọn; thay đổi bộ lọc sẽ cập nhật danh sách. Không có nút Tìm kiếm riêng để bấm. Dùng Xóa lọc để đưa các bộ lọc về mặc định.
- Xem ảnh xe vào và ra ở đâu? Mở Lịch sử gửi xe, tìm lượt cần xem, chọn Xem chi tiết rồi xem phần Sự kiện vào / ra nếu có ảnh và tài khoản được phép xem. Chat không gửi đường dẫn ảnh riêng tư.
- Xem ảnh lượt gửi xe như thế nào? Mở Lịch sử gửi xe, tìm lượt cần xem, chọn Xem chi tiết rồi xem phần Sự kiện vào / ra nếu có ảnh và tài khoản được phép xem. Chat không gửi đường dẫn ảnh riêng tư.
- Chat có hiển thị ảnh trong lịch sử gửi xe không? Không. Chat không hiển thị ảnh lịch sử gửi xe hoặc gửi đường dẫn ảnh riêng tư. Bạn mở Lịch sử gửi xe, chọn lượt cần xem, mở Xem chi tiết rồi xem Sự kiện vào / ra nếu có ảnh và tài khoản được phép xem.

## Điều chatbot được phép trả lời
Hướng dẫn sử dụng bộ lọc, ý nghĩa trạng thái và các bước đối chiếu khi thiếu dữ liệu. Câu trả lời dùng ngày giờ dễ đọc và tên nút thật trên giao diện.

## Điều chatbot phải dùng chức năng được cấp quyền
Lượt gửi, thời gian và phí thực tế phải đọc từ chức năng tra cứu lịch sử của chính tài khoản. Hiện đọc tối đa 20 lượt gần đây, hiển thị tối đa 5 mục; không suy rộng kết quả này thành toàn bộ lịch sử. Che biển số, chỉ giữ tối đa 3 ký tự cuối. Không gửi ảnh, mã thẻ, thông tin chủ xe hoặc dữ liệu sự kiện nhạy cảm cho model.

## Điều chatbot phải từ chối
Lịch sử người khác, toàn bộ ảnh camera, đường dẫn ảnh riêng tư, thay đổi hoặc xóa lịch sử; bỏ qua đăng nhập, quyền hoặc xác minh chủ sở hữu.

## Khi nào chuyển nhân viên
Thiếu lượt sau khi kiểm tra bộ lọc, thời gian hoặc phí khác thực tế, xe đã ra nhưng dữ liệu chưa ghi nhận. Phân biệt lỗi chức năng cần hỗ trợ hệ thống với việc đối chiếu lượt gửi tại bãi.

## Lưu ý khi trả lời và đối chiếu kết quả
Danh sách trống sau khi lọc khác với lỗi không tải được dữ liệu. Khi tìm theo ngày, phải chọn đúng khoảng ngày; bỏ bộ lọc không thay thế thao tác này. Bản tóm tắt gần đây trong chat không đại diện cho toàn bộ lịch sử và không chứng minh xe đã ra nếu dữ liệu chưa đủ.

<!-- authoring-only:start -->
## Nguồn đối chiếu
Đối chiếu cách xác định chủ sở hữu, giới hạn danh sách, múi giờ, trạng thái và ảnh tại [bằng chứng đợt 2](../evaluation/batch-02-traceability.md).

## Nội dung TBD
Quy trình xuất dữ liệu, lưu trữ lâu dài và giải quyết tranh chấp chưa được mở rộng. Chat chưa lọc theo ngày hoặc biển số; hướng dẫn dùng giao diện thay vì giả vờ đã áp dụng bộ lọc.

## Lịch sử phiên bản
- 2.2.0, 2026-11-24: rà soát lại với code hiện tại; làm rõ điều kiện và giới hạn trả lời, chuẩn hóa ngôn ngữ; tiếp tục DRAFT. Kiểm thử và giới hạn bằng chứng tại báo cáo ALL_BATCHES_REVIEW_STATUS.vi.md.
- 2.1.0, 2026-11-23: bổ sung phân biệt điều kiện và hướng xử lý theo lỗi hồi quy đợt 4; giữ DRAFT.
- 2.0.0, 2026-11-19: xác minh dữ liệu cá nhân, bộ lọc giao diện, tra cứu gần đây, che biển số và cấm ảnh riêng tư trong chat.
- 1.1.0: bản tham khảo trước khi rà soát đợt 2.
<!-- authoring-only:end -->
