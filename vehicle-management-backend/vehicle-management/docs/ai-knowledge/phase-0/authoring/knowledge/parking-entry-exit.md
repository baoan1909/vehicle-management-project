---
title: Quy trình xe vào và xe ra
slug: parking-entry-exit
documentVersion: 2.1.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: ParkingCheckInUseCaseImpl, ParkingCheckOutUseCaseImpl, ParkingCheckInPolicy, ParkingCheckOutPolicy, ParkingSessionAccessGuard; frontend SwipeEntryPage/SwipeListPage; Flyway QR và parking sessions
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-24
---

## Mục đích và đối tượng
Hướng dẫn khách vãng lai và khách dùng vé đăng ký chuẩn bị khi xe vào/ra, nhận biết tình huống cần nhân viên tại bãi. Khách không vận hành cổng qua chat.

## Phạm vi và điều kiện trước
Xe cần đi đúng làn vào/ra tại bãi phù hợp. Làn, cổng, khu vực và bãi phải đáp ứng điều kiện hoạt động; chỗ phù hợp loại xe phải còn khả dụng. Thẻ hoặc QR phải hợp lệ theo loại lượt gửi. Vé đăng ký, phương tiện và chủ vé cần đáp ứng các kiểm tra hiện hành. Không mặc định mọi bãi nhận mọi loại xe.

## Gửi xe khi chưa có vé đăng ký
Khách chưa có vé đăng ký là khách vãng lai trong quy trình vào bãi này. Có thể đến làn vào và dùng thẻ vãng lai theo hướng dẫn nhân viên nếu bãi còn chỗ phù hợp. QR của vé đăng ký không dành cho khách chưa có vé hợp lệ. QR đặt chỗ dùng để đối chiếu đặt chỗ, không tự thay thẻ vãng lai khi ghi nhận xe vào. Không bắt buộc đăng ký vé tháng hoặc đặt chỗ trước chỉ để gửi xe vãng lai. Đặt chỗ trước là lựa chọn khác, chỉ giải thích khi người dùng hỏi hoặc muốn đặt trước.

## Thẻ đã có xe đang trong bãi

Nếu thẻ đã có xe đang trong bãi hoặc còn lượt gửi chưa kết thúc, không tạo thêm lượt bằng thẻ đó. Nhờ nhân viên tại bãi kiểm tra lượt trước và tình trạng xe thực tế. Không quét lại nhiều lần, tự đóng lượt qua chat hoặc chuyển sang đặt chỗ để bỏ qua kiểm tra.

## Luồng chính
1. Đến đúng làn và làm theo nhân viên. Khách vãng lai dùng thẻ vãng lai; khách có vé đăng ký dùng thẻ đã gán hoặc QR của vé hợp lệ. Không dùng đồng thời thẻ và QR của vé đăng ký cho cùng lần ghi nhận.
2. Nhân viên đối chiếu biển số, hình ảnh, loại xe, thẻ/vé và khả năng nhận xe. Chỉ khi xác nhận thành công mới có lượt gửi đang mở.
3. Khi ra, đi đúng làn ra, dùng thông tin phù hợp để tìm lượt đang mở và đối chiếu xe.
4. Với lượt cần thu phí, nhân viên kiểm tra hóa đơn và thanh toán trước khi hoàn tất. Khách có vé đăng ký cũng phải đáp ứng điều kiện lượt gửi; không suy ra luôn miễn phí.
5. Xem trước, ghi nhận thanh toán, kết thúc lượt gửi và điều khiển cổng là các bước khác nhau. Chưa có kết quả từng bước thì không nói tất cả đã hoàn tất.

## Nhận dạng sai biển số tại cổng
Nếu hệ thống nhận dạng sai biển số hoặc biển số xe ra không khớp lúc vào, dừng theo hướng dẫn và nhờ nhân viên tại bãi đối chiếu xe, hình ảnh và thẻ hoặc mã QR phù hợp. Người dùng không tự sửa biển số, xác nhận nhận dạng hoặc bỏ qua kiểm tra để ra bãi. Ảnh khó đọc cần kiểm tra thực tế, không kết luận xe đã ra chỉ từ màn hình.

## Bản đồ còn chỗ nhưng cổng từ chối xe
Bãi hiển thị còn chỗ không bảo đảm xe được nhận ngay. Khi cổng từ chối xe dù còn chỗ, nhân viên cần kiểm tra chỗ phù hợp loại xe/khu vực, thẻ hoặc vé, lượt gửi và tình trạng hoạt động. Số chỗ là thông tin tại thời điểm tra cứu, không phải chỗ đã được giữ. Báo nhân viên tại bãi để kiểm tra, không tự đặt thêm chỗ hay trả tiền để vượt kiểm tra.

## Luồng thay thế, lỗi và trạng thái
- Vé đăng ký có luồng QR và thẻ vật lý; QR hết hiệu lực hoặc đã thay đổi cần được kiểm tra tại bãi.
- Còn lượt chưa kết thúc, thẻ không phù hợp, vé/xe không hợp lệ, bãi đầy hoặc khu vực không nhận loại xe có thể làm xe không vào được.
- Biển số không khớp, ảnh không đọc được hoặc sự cố cổng cần nhân viên đối chiếu. Không hướng dẫn tự sửa dữ liệu hay bỏ qua kiểm tra.
- Lượt đang gửi khác lượt đã kết thúc. Báo mất thẻ và hủy lượt có quy trình riêng; chat không đổi các trạng thái này.
- Số chỗ từng ô và khu vực quản lý theo sức chứa có điều kiện phân bổ khác nhau. Thông tin bản đồ không thay thế kiểm tra khi xe đến.

## Giới hạn của trợ lý và chuyển hỗ trợ
Trợ lý chỉ hướng dẫn. Lịch sử chính chủ chỉ được đọc qua công cụ được cấp quyền; hiện chat không có công cụ mở cổng, ghi nhận xe vào/ra, xem hóa đơn hoặc thu tiền. Không bịa trạng thái hiện tại. Sự cố ngay tại cổng cần báo nhân viên tại bãi; lỗi phần mềm có thể được báo qua hỗ trợ hệ thống đúng hội thoại, quyền và xác nhận. Không tạo phiếu thay cho bước xử lý an toàn tại hiện trường.

## Xử lý tiền đã trừ mà chưa hoàn tất lượt gửi
Nếu đã bị trừ tiền mà chưa hoàn tất lượt gửi, không thanh toán lại ngay. Cách xử lý là báo nhân viên tại bãi, giữ thông tin giao dịch cần thiết và nhờ kiểm tra giao dịch, hóa đơn cùng bước hoàn tất lượt gửi. Không suy ra xe đã ra chỉ từ việc ngân hàng trừ tiền; cũng không yêu cầu gửi OTP hay thông tin thẻ ngân hàng.

## Câu hỏi khách hàng thường gặp
- Khách vãng lai vào bãi xe như thế nào? Khi chưa có vé đăng ký, bạn có thể gửi xe vãng lai: đưa xe vào đúng làn và dùng thẻ vãng lai theo hướng dẫn nhân viên, không bắt buộc đặt chỗ trước. QR của vé đăng ký không thay thế thẻ vãng lai. Khách vãng lai được kiểm tra biển số, loại xe và thẻ phù hợp trước khi ghi nhận lượt gửi; bãi phải còn chỗ. Trợ lý không tự ghi nhận xe vào.
- Vào bãi bằng vé đăng ký như thế nào? Dùng thẻ đã được gán hoặc mã QR còn hợp lệ của vé đăng ký, đi đúng bãi và làn phù hợp rồi để nhân viên kiểm tra. Vé, xe và chủ vé phải đáp ứng điều kiện hiện hành; biển số phải khớp. Không dùng đồng thời QR của vé đăng ký và thẻ cho cùng một lần ghi nhận.
- Vì sao thẻ đang có lượt gửi xe chưa kết thúc không vào lại được? Một thẻ hoặc vé đăng ký không được tạo thêm lượt gửi khi còn lượt chưa kết thúc. Nhờ nhân viên tại bãi kiểm tra lượt trước và tình trạng xe thực tế; không dùng chat để đóng lượt hoặc quét nhiều lần để bỏ qua kiểm tra.
- Xe vãng lai ra bãi cần làm gì? Đi đúng làn ra, đưa thẻ và để nhân viên đối chiếu biển số, lượt gửi. Nhân viên kiểm tra phí và hóa đơn; lượt gửi chỉ được hoàn tất khi đáp ứng điều kiện thanh toán và xác nhận. Màn hình báo đã trả tiền không tự chứng minh xe đã ra hoặc cổng đã mở.
- Xe dùng vé đăng ký ra bãi như thế nào? Đưa xe đến đúng làn ra và dùng thẻ hoặc mã QR hợp lệ theo vé. Nhân viên kiểm tra lượt gửi đang mở và thông tin xe trước khi hoàn tất. Không tự kết luận mọi vé đăng ký đều được ra miễn phí hoặc bỏ qua điều kiện của lượt gửi.
- Biển số xe ra không khớp lúc vào thì làm sao? Dừng theo hướng dẫn và nhờ nhân viên tại bãi đối chiếu biển số, thẻ hoặc QR và hình ảnh của lượt gửi. Không tự sửa biển số để qua cổng hoặc bỏ qua xác minh. Trợ lý không xác nhận kết quả nhận dạng thay nhân viên.
- Cổng bãi xe không mở thì cần làm gì? Giữ xe ở vị trí an toàn theo hướng dẫn và báo nhân viên tại bãi. Cho biết bạn đang vào hay ra, dùng thẻ hay QR và thông báo hiện trên màn hình. Cần kiểm tra lượt gửi, thanh toán và tình trạng cổng; trợ lý không mở cổng hoặc đoán một nguyên nhân duy nhất.
- Đã trả tiền nhưng xe vẫn chưa ra bãi được thì sao? Không thanh toán lại ngay. Báo nhân viên tại bãi để kiểm tra giao dịch đã được ghi nhận, hóa đơn và bước hoàn tất lượt gửi. Giữ thông tin giao dịch cần thiết, không gửi OTP hoặc thông tin thẻ ngân hàng; cần đối chiếu bước kết thúc lượt gửi riêng, không suy ra chỉ từ việc bị trừ tiền.
- Vì sao bãi còn chỗ trên bản đồ mà xe không vào được? Số chỗ trên bản đồ là thông tin tại lần tra cứu, không phải chỗ đã được giữ cho bạn. Khi đến, bãi còn phải kiểm tra chỗ phù hợp loại xe, khu vực, thẻ hoặc vé và tình trạng hoạt động. Nhờ nhân viên tại bãi kiểm tra; không coi bản đồ là bảo đảm được vào.

## Lưu ý khi trả lời và đối chiếu kết quả
Xem trước kết quả, ghi nhận thanh toán, hoàn tất lượt gửi và mở cổng là các bước riêng. Nếu bị kẹt tại cổng, ưu tiên vị trí an toàn và nhân viên tại bãi. Trợ lý không suy ra xe đã vào hoặc ra chỉ từ tin nhắn, ảnh giao dịch hay trạng thái thanh toán.

<!-- authoring-only:start -->

## Nguồn đối chiếu
Xem [ma trận truy vết đợt 4](../evaluation/batch-04-traceability.md). Nguồn chính: ParkingCheckInUseCaseImpl, ParkingCheckOutUseCaseImpl, ParkingCheckInPolicy, ParkingCheckOutPolicy, ParkingSessionAccessGuard; frontend SwipeEntryPage/SwipeListPage; Flyway QR và parking sessions. Quyết định nghiệp vụ không tự chứng minh chức năng runtime đã có hoặc được bật.

## Nội dung TBD
Xem khoảng trống và giới hạn bằng chứng trong báo cáo đợt 4; không dùng giá trị hoặc trạng thái chưa xác minh làm cam kết. Nội dung vẫn DRAFT, chưa được duyệt hoặc publish.

## Lịch sử phiên bản
- 2.1.0, 2026-11-24: rà soát lại với code hiện tại; làm rõ điều kiện và giới hạn trả lời, chuẩn hóa ngôn ngữ; tiếp tục DRAFT. Kiểm thử và giới hạn bằng chứng tại báo cáo ALL_BATCHES_REVIEW_STATUS.vi.md.
- 2.0.0, 2026-11-23: rà soát đợt 4 theo code, UI và sổ quyết định; tách hướng dẫn khách khỏi thao tác nội bộ, bổ sung FAQ và giới hạn dữ liệu hiện tại. Chưa publish.

<!-- authoring-only:end -->
