<!--
title: Đăng ký, sử dụng và gia hạn vé tháng
slug: subscription-lifecycle
documentVersion: 2.2.0
status: DRAFT
language: vi
accessScope: CUSTOMER
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Giao diện, nghiệp vụ, phân quyền và migration đã đối chiếu trong bằng chứng đợt 2
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-24
-->

# Đăng ký, sử dụng và gia hạn vé tháng

## Mục đích
Hướng dẫn đăng ký vé tháng, theo dõi thanh toán, chọn vị trí và gia hạn theo chức năng hiện tại.

## Đối tượng
Khách hàng đã đăng nhập, được duyệt và có quyền đăng ký hoặc xem vé. Nhân viên duyệt và xử lý vé theo bãi xe, đơn vị và quyền được giao.

## Phạm vi
Đăng ký mới, duyệt, thanh toán, vị trí đỗ, hiệu lực vé, thẻ vật lý, gia hạn và hủy trước khi sử dụng. Chatbot không tự duyệt, thu tiền, gia hạn hoặc hủy vé.

## Thuật ngữ
Vé tháng là đăng ký sử dụng dịch vụ theo loại vé và số kỳ đã chọn. Một kỳ không mặc định luôn là 30 ngày: thời hạn phụ thuộc loại vé. Thẻ vật lý là phương tiện ra vào có quy trình cấp riêng; không đồng nhất tình trạng thẻ với hiệu lực vé.

## Điều kiện trước
Hồ sơ được duyệt và đang hoạt động; phương tiện thuộc tài khoản và đang hoạt động. Bãi xe, loại vé, giá và sức chứa phải đáp ứng kiểm tra của hệ thống. Không được có vé chồng thời gian bị quy tắc hiện hành chặn.
Ngày bắt đầu đăng ký mới hiện được chọn từ hôm nay đến 7 ngày tới, tính theo ngày tại Việt Nam. Có thể chọn từ 1 đến 12 kỳ theo điều kiện loại vé. Không áp dụng lại hướng dẫn cũ bắt buộc trước 2 ngày.

## Luồng chính
1. Mở Vé tháng của tôi, chọn Đăng ký vé mới.
2. Chọn bãi xe, phương tiện, loại vé, số kỳ và ngày bắt đầu; kiểm tra giá và lựa chọn thẻ vật lý nếu có.
3. Gửi đăng ký và theo dõi Chờ duyệt. Nhân viên cần duyệt trong hạn mà hệ thống cho phép; chat không cam kết giờ duyệt.
4. Nếu có số tiền phải trả, hoàn tất thanh toán trên chức năng được cung cấp. Chỉ tin kết quả hệ thống đã ghi nhận, không coi việc mở trang thanh toán là đã trả tiền.
5. Nếu vé hiển thị Chờ chọn vị trí, chọn Chọn vị trí và hoàn tất yêu cầu. Kiểm tra trạng thái, ngày hiệu lực và tình trạng tạm khóa trước khi dùng vé hoặc mã QR.

## Luồng thay thế
- Đăng ký không phát sinh tiền phải trả có thể tiếp tục sang bước tiếp theo sau khi được duyệt; bãi quản lý theo vị trí vẫn có thể yêu cầu chọn chỗ.
- Thẻ vật lý có các bước yêu cầu thẻ, thanh toán cọc, chờ có thẻ và gán thẻ riêng. Không khẳng định mọi vé phải nhận thẻ mới được hoạt động.
- Gia hạn: mở vé đang hoạt động, còn hạn, chọn Gia hạn vé; chọn điều kiện kỳ tiếp theo, xem chi phí rồi xác nhận và thanh toán nếu cần. Thời gian mới nối tiếp sau ngày hết hạn hiện tại. Hệ thống giữ vé, mã QR và thẻ hiện có; điều kiện gói kỳ mới được áp dụng theo thời điểm nghiệp vụ.
- Vé đã hết hạn không dùng chức năng gia hạn này; cần đăng ký mới theo điều kiện hiện tại.
- Không có tự gia hạn vô hạn hoặc mặc định gia hạn chỉ vì đã mở hộp thoại.

## Luồng lỗi
- Không gửi được đăng ký: kiểm tra hồ sơ, xe, ngày bắt đầu, số kỳ và loại vé của bãi.
- Vé chồng thời gian hoặc bãi không còn khả năng tiếp nhận: kiểm tra lại lựa chọn, không thử gửi lặp để vượt giới hạn.
- Đã thanh toán nhưng chưa dùng được: kiểm tra hệ thống đã ghi nhận tiền chưa, ngày hiệu lực đã đến chưa, có đang chờ chọn vị trí hoặc tạm khóa không. Không yêu cầu trả tiền lần nữa khi chưa đối chiếu hóa đơn.
- Không gia hạn được: kiểm tra vé đang hoạt động và ngày hết hạn chưa qua; xem thông báo điều kiện loại vé và thanh toán.
- Không có nút thao tác: kiểm tra trạng thái và quyền; không hướng dẫn gọi API để vượt giao diện.

## Trạng thái và chuyển trạng thái
- Chờ duyệt: đăng ký đang đợi người có thẩm quyền xem xét.
- Chờ thanh toán: còn khoản thanh toán cần hoàn tất.
- Chờ chọn vị trí: cần hoàn tất lựa chọn vị trí theo yêu cầu của bãi.
- Đang hoạt động: vẫn cần đối chiếu ngày hiệu lực và tình trạng tạm khóa khi sử dụng.
- Nếu trạng thái chưa xác định, kiểm tra thông tin chi tiết hoặc nhờ nhân viên; không suy ra tình trạng thẻ hay quyền ra vào. Luồng hiện tại quản lý việc cấp thẻ vật lý riêng với vé.
- Hết hạn, Đã hủy, Từ chối và Đã chấm dứt có ý nghĩa khác nhau; không gọi chung là “hết hạn”.
- Tạm khóa: việc sử dụng vé, mã QR và thẻ liên quan đang bị tạm ngừng, dù vé có thể vẫn còn thời hạn.
Thẻ vật lý hiển thị riêng: Chưa yêu cầu thẻ; Chờ thanh toán cọc thẻ; Đã giữ thẻ · chờ gán; Đã thanh toán · chờ có thẻ; Đã gán thẻ vật lý.

## Câu hỏi khách hàng thường gặp
- Đăng ký vé tháng như thế nào? Mở Vé tháng của tôi, chọn Đăng ký vé mới rồi chọn bãi, phương tiện, loại vé, số kỳ và ngày bắt đầu từ hôm nay đến 7 ngày tới theo ngày tại Việt Nam. Kiểm tra giá, gửi đăng ký và theo dõi duyệt. Thanh toán, chọn vị trí hoặc nhận thẻ thực hiện theo yêu cầu đang hiển thị; thanh toán xong chưa tự chứng minh vé đã dùng được.
- Hôm nay đăng ký bắt đầu hôm nay được không? Ngày bắt đầu được phép từ hôm nay đến 7 ngày tới; còn phải đáp ứng các điều kiện khác và được xử lý theo quy trình.
- Thanh toán rồi có vào bãi ngay không? Cần kiểm tra ngày hiệu lực, trạng thái, chọn vị trí và tạm khóa; không chỉ dựa vào thanh toán.
- Gia hạn có phải tạo vé mới không? Vé đang hoạt động, chưa hết hạn có chức năng Gia hạn vé nối tiếp vé hiện tại. Vé đã hết hạn cần đăng ký mới.
- Hủy vé đang dùng để hoàn tiền ngay được không? Quy trình hủy hiện không cho tự hủy vé đang hoạt động để hoàn tiền ngay. Liên hệ nhân viên để được xem xét theo chính sách, không hứa hoàn tiền.
- Trạng thái nào có thể hủy theo quy trình hiện tại? Chờ duyệt hoặc Chờ thanh toán, khi đủ quyền và đáp ứng điều kiện. Chat chưa hủy thay bạn.
- Không có thẻ vật lý thì vé có dùng được không? Kiểm tra trạng thái và quyền sử dụng mã QR; thẻ vật lý được xử lý riêng.
- Vé của tôi còn hạn không? Trợ lý phải đọc dữ liệu được phép và nêu ngày hiệu lực, không suy đoán từ lịch sử chat.
- Chỉ hỏi “vé tháng” thì sao? Cần làm rõ bạn muốn đăng ký, gia hạn, thanh toán hay xem vé.
- Ngày bắt đầu vé tháng trong khoảng nào? Khi đăng ký mới, chọn từ hôm nay đến 7 ngày tới theo ngày tại Việt Nam. Không mặc định phải đợi 2 ngày; vé còn cần được duyệt, thanh toán và đáp ứng điều kiện sử dụng.

## Điều chatbot được phép trả lời
Các bước sử dụng, ý nghĩa trạng thái và điều kiện đã xác minh. Câu hỏi “không gia hạn được” là yêu cầu tìm hiểu lỗi, không phải lệnh gia hạn.

## Điều chatbot phải dùng chức năng được cấp quyền
Thông tin cá nhân phải lấy từ chức năng đọc vé của chính tài khoản. Hiện tóm tắt các vé gần đây, đọc tối đa 20 vé và hiển thị tối đa 5 mục. Bộ lọc trạng thái trong chat chỉ áp dụng trong phần dữ liệu giới hạn này; xem đầy đủ trên Vé tháng của tôi. Không đưa mã QR, mã nội bộ hoặc thông tin người duyệt vào câu trả lời.

## Điều chatbot phải từ chối
Đọc vé người khác; duyệt, thay xe, đổi thẻ, gia hạn, hủy hoặc hoàn tiền trái quyền; xác nhận thanh toán chỉ từ lời kể hoặc ảnh do người dùng cung cấp.

## Khi nào chuyển nhân viên
Tranh chấp số tiền, đề nghị hoàn tiền, vé bị khóa, thanh toán đã được ghi nhận nhưng trạng thái tiếp tục sai sau khi kiểm tra các điều kiện. Không tự đặt thời hạn xử lý.

## Lưu ý khi trả lời và đối chiếu kết quả
Thanh toán hóa đơn, chọn vị trí, hiệu lực vé và cấp thẻ là các bước riêng. Khi chưa dùng được vé, đối chiếu từng bước trên Vé tháng của tôi; không trả tiền thêm để thử kích hoạt. Chat chỉ xác nhận tình trạng riêng khi chức năng tra cứu có quyền trả dữ liệu hiện tại.
