<!--
title: Hóa đơn, thanh toán và yêu cầu hoàn tiền
slug: invoice-vnpay-payment-refund
documentVersion: 2.1.0
status: DRAFT
language: vi
accessScope: CUSTOMER
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Code backend, giao diện nghiệp vụ và kiểm thử được ghi trong traceability đợt 3
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-24
-->

# Hóa đơn, thanh toán và yêu cầu hoàn tiền

## Mục đích
Hướng dẫn kiểm tra và thanh toán hóa đơn an toàn; phân biệt đã trả tiền với vé hoặc đặt chỗ đã có hiệu lực.

## Đối tượng và phạm vi
Khách đã đăng nhập với hóa đơn của mình và quyền phù hợp. VNPay, ví và thanh toán tại quầy xuất hiện trong luồng vé tháng. SePay hiện được đối chiếu ở luồng đặt chỗ công khai; có code không đồng nghĩa môi trường đang bật tích hợp.

## Điều kiện trước
Hóa đơn thuộc phạm vi được phép xem, còn được thanh toán và chưa có giao dịch thành công. Kiểm tra số tiền và phương thức thực sự hiển thị. Cấu hình cổng thanh toán phải hợp lệ.

## Luồng chính
### Ý nghĩa của trang thanh toán thành công
Trang thanh toán thành công hoặc trang quay về từ cổng thanh toán là nơi hiển thị kết quả, không phải bằng chứng độc lập rằng hóa đơn đã được trả. Trạng thái thanh toán cần được hệ thống xác minh với giao dịch hợp lệ, đúng chữ ký và số tiền. Nếu tiền đã trừ nhưng kết quả còn chờ, kiểm tra giao dịch và nhờ đối soát trước khi trả thêm lần nữa.

### Các bước thanh toán
1. Mở Vé tháng và chọn hóa đơn cần trả.
2. Kiểm tra số tiền; chọn phương thức đang được hỗ trợ.
3. Thực hiện và xác nhận trong giao diện nghiệp vụ.
4. Xem kết quả do hệ thống xác minh; không gửi thêm một lần khi kết quả còn chưa rõ.

## Luồng thay thế và lỗi
### Lỗi tiền đã trừ nhưng hóa đơn chưa cập nhật
Nếu tiền đã trừ mà hóa đơn chưa cập nhật, kiểm tra trạng thái và lịch sử giao dịch. Không trả thêm lần nữa khi giao dịch trước chưa được xác minh. Liên hệ bộ phận phụ trách với thời điểm, số tiền và mã giao dịch cần thiết để đối soát; không gửi mã xác minh hoặc thông tin thẻ bí mật.

### Giao dịch thất bại và thử lại
Sau khi giao dịch báo thất bại, kiểm tra kết quả thực tế và xem tiền có bị trừ không trước khi thanh toán lại. Chỉ thử lại bằng lựa chọn đang có khi giao dịch trước đã rõ kết quả; nếu tiền bị trừ hoặc chưa chắc chắn, nhờ đối soát trước để tránh trả trùng. Không tự sửa số tiền hay giả kết quả thành công.

### Khi giao diện đặt chỗ cho thanh toán bằng SePay
SePay hiện có ở luồng đặt chỗ công khai: giao diện chọn SePay cho khoản dưới mười nghìn đồng và VNPay từ ngưỡng đó theo code đã đối chiếu. Thanh toán đặt chỗ bằng SePay chỉ dùng được khi cổng có cấu hình hợp lệ và giao diện cho phép. Không suy ra mọi hóa đơn hoặc mọi tài khoản đều dùng SePay. Xem phương thức đang hiển thị và chờ hệ thống xác minh kết quả; không chuyển tiền vào tài khoản do trợ lý tự nghĩ ra.

Nếu phương thức chưa cấu hình hoặc tạm không dùng được, làm theo lựa chọn thực tế trên giao diện. Không chuyển tiền vào tài khoản hay đường dẫn do chatbot tự nghĩ ra. Tiền đã trừ, sai số tiền hoặc trùng giao dịch cần được đối soát.

## Trạng thái và giới hạn
Hóa đơn chờ, đã thanh toán và đã hủy khác với giao dịch đang chờ, thành công hoặc thất bại. Hiệu lực vé còn phụ thuộc các điều kiện của vé. Chưa xác minh có luồng thực thi hoàn tiền tự động; không hứa hoàn tiền hoặc thời hạn.

### Tiếp nhận yêu cầu hoàn tiền
Để yêu cầu hoàn tiền cho hóa đơn, liên hệ bộ phận phụ trách giao dịch để kiểm tra điều kiện và đối soát. Chatbot chưa có chức năng thực hiện hoàn tiền, chưa có quy trình tự động được xác minh và không cam kết chắc chắn được hoàn hoặc ngày nhận tiền. Chỉ cung cấp thông tin giao dịch cần thiết qua kênh được phép; không gửi mật khẩu, mã xác minh hay thông tin thẻ bí mật. Với đặt chỗ đã trả tiền, cần đọc quy định hủy hoặc không đến thì không hoàn tiền đang hiển thị trước khi xác nhận.

## Câu hỏi khách hàng thường gặp
- Thanh toán vé tháng qua VNPay như thế nào? Mở Vé tháng, chọn hóa đơn đang chờ thanh toán, kiểm tra số tiền rồi chọn phương thức VNPay đang khả dụng. Hoàn tất tại cổng thanh toán và quay lại xem kết quả đã được hệ thống xác minh; trợ lý không tự thanh toán thay bạn.
- Tiền đã trừ nhưng hóa đơn còn chờ thì phải làm sao? Kiểm tra lại trạng thái hóa đơn và lịch sử giao dịch. Chưa thanh toán lần nữa khi giao dịch trước chưa được xác minh. Nếu vẫn chưa cập nhật, liên hệ bộ phận phụ trách kèm thời điểm, số tiền và mã giao dịch cần thiết; không gửi mã xác minh hoặc thông tin thẻ bí mật.
- Thanh toán thất bại thì có được thử lại không? Kiểm tra giao dịch trước có thực sự thất bại và hóa đơn còn được phép thanh toán. Nếu đã bị trừ tiền hoặc kết quả chưa rõ, cần xác minh trước. Chỉ tạo lần thanh toán mới khi hệ thống cho phép, tránh trả trùng.
- Quay lại trang thành công có nghĩa là đã thanh toán chưa? Trang chuyển về chỉ hiển thị kết quả. Cần kiểm tra trạng thái đã được hệ thống xác minh từ thông báo hợp lệ của cổng thanh toán. Không dựa riêng vào địa chỉ trang hoặc ảnh chụp để kết luận hóa đơn đã được trả.
- Thông báo thanh toán gửi lặp có bị thu tiền hai lần không? Hệ thống kiểm tra giao dịch và trạng thái đã xử lý để hạn chế cập nhật trùng. Thông báo lặp không phải yêu cầu bạn trả lại tiền. Nếu có hai khoản tiền bị trừ thực tế, cần nhờ bộ phận phụ trách đối chiếu từng giao dịch.
- Thanh toán qua SePay dùng trong trường hợp nào? Luồng đặt chỗ công khai hiện có SePay cho khoản dưới ngưỡng VNPay của giao diện, hiện là mười nghìn đồng. Chỉ sử dụng khi hệ thống hiển thị phương thức này và tích hợp đã được cấu hình; không coi SePay là phương thức dùng cho mọi hóa đơn.
- Thanh toán vé tháng bằng ví như thế nào? Trong Vé tháng, mở hóa đơn chờ thanh toán, chọn thanh toán bằng ví nếu giao diện cho phép, kiểm tra số tiền và xác nhận. Hệ thống kiểm tra quyền, số dư và trạng thái hóa đơn; không gửi mật khẩu hoặc yêu cầu trợ lý trừ ví trực tiếp.
- Không dùng VNPay thì thanh toán vé tháng tại quầy thế nào? Chọn phương thức thanh toán tại quầy khi giao diện Vé tháng cho phép, rồi làm theo hướng dẫn của bãi. Việc chọn tại quầy chưa có nghĩa hóa đơn đã được trả; nhân sự có quyền cần ghi nhận khoản thanh toán thực tế.
- Muốn hoàn tiền hóa đơn thì cần làm gì? Liên hệ bộ phận phụ trách giao dịch để kiểm tra điều kiện và đối soát. Chatbot hiện chưa có chức năng hoàn tiền và không cam kết thời hạn hay chắc chắn được hoàn. Với đặt chỗ đã thanh toán, giao diện hiện yêu cầu chấp nhận quy định hủy hoặc không đến thì không hoàn tiền.
- Vì sao hóa đơn đã thanh toán mà vé chưa có hiệu lực? Trạng thái hóa đơn và hiệu lực vé là hai việc khác nhau. Kiểm tra kết quả thanh toán cùng ngày bắt đầu, tình trạng xử lý vé và điều kiện liên quan trên Vé tháng. Nếu chưa rõ, nhờ bộ phận phụ trách kiểm tra; không trả tiền thêm chỉ để kích hoạt lại.

## Điều chatbot được phép trả lời
Giải thích hướng dẫn, điều kiện và giới hạn đã được kiểm chứng. Trả lời bằng từ ngữ dễ hiểu; không đưa tên trạng thái kỹ thuật vào câu trả lời.

## Điều chatbot phải dùng chức năng được cấp quyền
Thông tin cá nhân, giá hiện hành, số tiền hoặc trạng thái cụ thể chỉ được xác nhận bằng nguồn dữ liệu có thẩm quyền và đúng quyền. Nếu chưa có tool phù hợp, hướng dẫn mở module nghiệp vụ hoặc liên hệ bộ phận phụ trách; không giả lập kết quả.

## Điều chatbot phải từ chối
Không tự thực hiện thanh toán, hoàn tiền, đặt/hủy chỗ, áp mã hay sửa hóa đơn. Không yêu cầu mật khẩu, mã xác minh, thông tin thẻ bí mật, mã truy cập đặt chỗ hoặc QR riêng tư. Không hứa chắc số tiền, hiệu lực hoặc thời hạn chưa có nguồn.

## Khi nào chuyển nhân viên
Giao dịch bất thường, tiền đã trừ chưa rõ kết quả, tranh chấp hoặc cần kiểm tra dữ liệu riêng ngoài khả năng tra cứu được cấp. Phân biệt liên hệ bãi xử lý nghiệp vụ với báo lỗi chức năng hệ thống; không tự tạo phiếu thay cho mọi yêu cầu tài chính.

## Lưu ý khi trả lời và đối chiếu kết quả
Phân biệt ba việc: ngân hàng trừ tiền, hệ thống ghi nhận giao dịch và nghiệp vụ vé hoặc đặt chỗ hoàn tất. Một việc xảy ra chưa chứng minh hai việc còn lại. Kết quả chưa rõ cần đối soát trước khi trả thêm; trợ lý không xác nhận hoàn tiền hoặc ngày nhận tiền từ lời kể.
