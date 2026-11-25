<!--
title: Đặt chỗ trước và thời gian giữ chỗ
slug: reservations-and-holds
documentVersion: 2.1.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Code backend, giao diện nghiệp vụ và kiểm thử được ghi trong traceability đợt 3
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-24
-->

# Đặt chỗ trước và thời gian giữ chỗ

## Mục đích
Hướng dẫn đặt chỗ thực có, thời hạn thanh toán, hạn vào bãi và quy định tiền đã trả.

## Đối tượng và phạm vi
Có luồng công khai và luồng khách đăng nhập. Đây là đặt chỗ cho lượt gửi xe; không phải mua vé tháng và không bảo đảm giữ vô thời hạn hoặc đặt một khung giờ tương lai tùy ý.

## Điều kiện trước
Bãi và khu vực đang phục vụ, phù hợp loại xe, còn chỗ, biển số hợp lệ và không có yêu cầu/lượt gửi xung đột. Cần đồng ý quy định tính phí, hạn vào bãi và không hoàn tiền đang hiển thị.

## Luồng chính
1. Mở Đặt chỗ trước, chọn bãi, loại xe, khu vực và ô nếu được chọn.
2. Nhập biển số thật, đọc và đồng ý quy định.
3. Chọn Xác nhận và thanh toán; hoàn tất trong mười phút theo thời hạn trang hiển thị.
4. Chờ hệ thống xác minh thanh toán, rồi vào bãi trước hạn sáu mươi phút tính từ thanh toán thành công.
5. Khi ra bãi, hệ thống tính phí từ thời điểm trả tiền và trừ phần đã trả trước.

## Luồng thay thế và lỗi
### Chỗ cuối và thay đổi số chỗ trống
Vừa thấy còn chỗ trên màn hình không bảo đảm đặt chỗ cuối sẽ thành công. Nhiều người có thể đang chọn cùng lúc; hệ thống kiểm tra lại chỗ trống khi xác nhận giữ. Nếu người khác vừa giữ chỗ cuối hoặc chỗ không còn phù hợp, hãy chọn lại vị trí hay khu vực còn trống. Chỉ thanh toán cho đặt chỗ được tạo hợp lệ; không đổi biển số giả để tránh kiểm tra trùng.

### Nhận tiền trễ và thanh toán cần đối soát
Với đặt chỗ nhận tiền trễ, hệ thống cần kiểm tra lại giao dịch vì thời gian giữ chỗ có thể đã hết và chỗ đã được giải phóng. Khoản thanh toán cần nhân viên đối soát chưa xác nhận có chỗ cho khách. Hãy liên hệ bộ phận phụ trách để kiểm tra thời điểm, số tiền và giao dịch thực tế; không trả thêm tiền hoặc mặc định được hoàn tiền tự động. Đối soát không đồng nghĩa khôi phục lại chỗ cũ.

### Chọn phương thức thanh toán
Luồng công khai hiện chọn SePay cho khoản dưới mười nghìn đồng và VNPay từ ngưỡng đó theo giao diện/code đã đối chiếu. Phương thức chỉ dùng được khi có cấu hình hợp lệ; luồng tài khoản có thể khác, hãy theo giao diện thực tế.
Với khách chưa đăng nhập, thông tin truy cập được lưu theo trình duyệt. Mất thông tin thì nhờ nhân viên kiểm tra với biển số; không gửi mã truy cập hoặc QR vào chat công khai.

## Trạng thái và giới hạn
Chờ thanh toán chưa phải đã thanh toán giữ chỗ. Đã thanh toán còn giữ, đã vào bãi, hết hạn, đã hủy và cần đối soát là các trạng thái khác nhau. Cần đối soát không bảo đảm còn chỗ. Hủy sau thanh toán hoặc không đến không được hoàn tiền theo quy định hiện hiển thị.

### Đã trả tiền nhưng không đến bãi
Đặt chỗ đã trả tiền nhưng khách không đến trong thời hạn thì không tự nhận lại tiền. Quy định hiện hiển thị là không hoàn tiền khi hủy sau thanh toán hoặc không đến. Nếu nghi ngờ giao dịch có lỗi, liên hệ bộ phận phụ trách kiểm tra; chatbot không hứa được hoàn, không tự thanh toán lại hoặc khôi phục chỗ.

## Câu hỏi khách hàng thường gặp
- Đặt chỗ gửi xe trước như thế nào? Mở Đặt chỗ trước, chọn bãi, loại xe, khu vực và chỗ cụ thể nếu hệ thống cho chọn, nhập đúng biển số. Đọc quy định tính phí, thời hạn và không hoàn tiền, đồng ý rồi chọn Xác nhận và thanh toán. Chờ kết quả thanh toán đã được xác minh.
- Có phải đăng nhập mới được đặt chỗ không? Có luồng đặt chỗ công khai cho khách chưa đăng nhập. Hãy dùng cùng trình duyệt để mở lại thông tin được lưu và giữ bí mật mã truy cập. Khách đã đăng nhập xem các đặt chỗ của mình theo quyền; không chia sẻ mã QR hoặc mã truy cập cho người khác.
- Đặt chỗ phải thanh toán trong bao lâu? Luồng hiện tại giữ chỗ chờ thanh toán trong mười phút kể từ khi tạo đặt chỗ. Xem đồng hồ và thời hạn hiển thị trên trang. Quá hạn thì chỗ có thể được giải phóng; nếu tiền đã chuyển mà chưa xác nhận, cần đối soát trước khi đặt hoặc trả tiền lại.
- Trả tiền đặt chỗ xong phải đến bãi trong bao lâu? Bạn cần vào bãi trong sáu mươi phút tính từ thời điểm thanh toán thành công theo luồng hiện tại. Kiểm tra hạn hiển thị; quá hạn chỗ được giải phóng theo xử lý hết hạn. Hủy hoặc không đến sau khi đã trả tiền không tự được hoàn tiền.
- Tiền gửi xe đặt chỗ bắt đầu tính từ khi nào? Với đặt chỗ đã thanh toán, thời gian tính phí bắt đầu khi thanh toán thành công, không đợi đến lúc xe vào bãi. Khi ra, hệ thống tính toàn bộ khoảng thời gian đến lúc ra và trừ khoản đã trả trước.
- Hủy đặt chỗ đã thanh toán có được hoàn tiền không? Theo quy định hiện hiển thị và được yêu cầu đồng ý, hủy sau thanh toán hoặc không đến sẽ không được hoàn tiền. Chỉ dùng chức năng hủy khi đặt chỗ đã thanh toán, còn được giữ và chưa sử dụng. Khi hủy hợp lệ, chỗ được giải phóng. Nếu có giao dịch bất thường, liên hệ bộ phận phụ trách đối soát.
- Hai người cùng đặt chỗ cuối cùng thì sao? Số chỗ hiển thị có thể thay đổi trước khi bạn xác nhận. Hệ thống kiểm tra lại khi giữ chỗ; nếu chỗ cuối vừa được người khác giữ, yêu cầu có thể bị từ chối. Chọn lại chỗ hoặc khu vực khác, không thanh toán cho một đặt chỗ chưa được tạo hợp lệ.
- Đặt chỗ báo biển số đã có yêu cầu còn hiệu lực thì làm sao? Kiểm tra đặt chỗ đang còn hiệu lực của biển số trong khu vực và xem xe có lượt gửi chưa kết thúc tại bãi không. Hệ thống chặn các trường hợp trùng đang hoạt động; không đổi biển số giả để tạo thêm yêu cầu.
- Thanh toán đặt chỗ bị báo cần nhân viên đối soát thì sao? Khoản thanh toán có thể đến khi thời gian giữ chỗ đã hết hoặc không còn đủ điều kiện xác nhận. Trạng thái cần nhân viên đối soát chưa bảo đảm có chỗ đã được giữ. Liên hệ bộ phận phụ trách với thông tin giao dịch cần thiết; không tự trả thêm hoặc mặc định đã có chỗ.
- Đặt chỗ chọn vị trí cụ thể khác giữ số lượng thế nào? Khu vực quản lý từng ô cho chọn chỗ cụ thể còn phù hợp. Khu vực quản lý theo sức chứa chỉ giữ số lượng chỗ, không chọn một ô riêng. Cả hai đều phải còn chỗ và phù hợp loại xe tại thời điểm xác nhận.

## Điều chatbot được phép trả lời
Giải thích hướng dẫn, điều kiện và giới hạn đã được kiểm chứng. Trả lời bằng từ ngữ dễ hiểu; không đưa tên trạng thái kỹ thuật vào câu trả lời.

## Điều chatbot phải dùng chức năng được cấp quyền
Thông tin cá nhân, giá hiện hành, số tiền hoặc trạng thái cụ thể chỉ được xác nhận bằng nguồn dữ liệu có thẩm quyền và đúng quyền. Nếu chưa có tool phù hợp, hướng dẫn mở module nghiệp vụ hoặc liên hệ bộ phận phụ trách; không giả lập kết quả.

## Điều chatbot phải từ chối
Không tự thực hiện thanh toán, hoàn tiền, đặt/hủy chỗ, áp mã hay sửa hóa đơn. Không yêu cầu mật khẩu, mã xác minh, thông tin thẻ bí mật, mã truy cập đặt chỗ hoặc QR riêng tư. Không hứa chắc số tiền, hiệu lực hoặc thời hạn chưa có nguồn.

## Khi nào chuyển nhân viên
Giao dịch bất thường, tiền đã trừ chưa rõ kết quả, tranh chấp hoặc cần kiểm tra dữ liệu riêng ngoài khả năng tra cứu được cấp. Phân biệt liên hệ bãi xử lý nghiệp vụ với báo lỗi chức năng hệ thống; không tự tạo phiếu thay cho mọi yêu cầu tài chính.

## Lưu ý khi trả lời và đối chiếu kết quả
Xem còn chỗ, tạo đặt chỗ chờ trả tiền, trả tiền thành công và vào bãi là các bước khác nhau. Theo dõi thời hạn trên trang đang dùng. Khi giao dịch cần đối soát, chưa thể khẳng định có chỗ, tự khôi phục chỗ cũ hoặc được hoàn tiền.
