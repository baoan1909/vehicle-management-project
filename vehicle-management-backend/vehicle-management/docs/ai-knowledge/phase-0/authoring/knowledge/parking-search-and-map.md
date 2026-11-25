---
title: Tìm bãi, bản đồ và chỉ đường
slug: parking-search-and-map
documentVersion: 2.1.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: PublicParkingLotController, NearbyParkingLotUseCaseImpl, PublicParkingLotDirectoryUseCaseImpl, PostGisNearbyParkingLotAdapter, PublicParkingLocationUseCaseImpl; frontend NearbyParkingLotsPage/NearbyParkingMap
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-24
---

## Mục đích và đối tượng
Hướng dẫn mọi người dùng tìm bãi và đường đi bằng giao diện công khai. Dữ liệu vị trí, chỗ trống hoặc giá hiện tại không được suy ra từ tài liệu này.

## Điều kiện và nguồn dữ liệu
Chức năng phụ thuộc cấu hình công bố bản đồ, tìm gần đây và tra địa chỉ. Dữ liệu công khai có bộ lọc và trạng thái riêng. Tìm gần đây kiểm tra tọa độ, bán kính nguyên từ 1 đến 50 km và giới hạn số kết quả. Không tự suy ra tính năng đang bật từ việc code tồn tại.

## Quyền vị trí và định vị
Trình duyệt dùng quyền vị trí để lấy vị trí tìm bãi khi bạn đồng ý. Nếu bị từ chối quyền vị trí, bạn vẫn có thể dùng Tìm địa chỉ hoặc chọn điểm trên bản đồ, không bắt buộc bật lại định vị. Chỉ cho phép vị trí khi muốn dùng Tìm bãi gần tôi. Không gửi tọa độ chính xác qua chat; quyền vị trí của trình duyệt khác quyền tài khoản CoParking.

## Cách gọi thường dùng trên giao diện
Định vị còn được gọi là GPS: dùng vị trí hiện tại khi bạn đồng ý. Không dùng GPS thì chọn khu vực tìm bãi bằng Tìm địa chỉ hoặc chọn điểm thủ công trên bản đồ. Bán kính là phạm vi tìm quanh vị trí đã chọn; muốn mở rộng phạm vi tìm bãi gần đây thì tăng bán kính trong giới hạn cho phép và bấm Áp dụng. Khi tìm quanh vị trí nhưng không có bãi nào, kiểm tra điểm tìm, tăng bán kính hoặc chọn khu vực khác. Xem số chỗ trống hiện tại khác với giữ chỗ; chat chưa tra trực tiếp số chỗ còn lại.

## Luồng chính
1. Mở Tìm bãi xe. Chọn Tìm bãi gần tôi và cho phép vị trí, hoặc dùng Tìm địa chỉ/chọn điểm thủ công nếu được bật.
2. Nhập ít nhất 3 ký tự khi tra địa chỉ và chọn đúng kết quả. Kiểm tra lại khu vực trước khi tìm.
3. Chọn Bán kính tìm kiếm, bấm Áp dụng và xem danh sách quanh điểm đã chọn.
4. Chọn đúng bãi, loại xe và thông tin đang hiển thị; dùng nút chỉ đường để mở Google Maps khi cần.
5. Nếu muốn đặt trước, dùng luồng Đặt chỗ trước riêng. Xem bản đồ không phát sinh đặt chỗ hoặc thanh toán.

## Luồng thay thế và lỗi
- Không muốn bật định vị: dùng địa chỉ hoặc chọn điểm trên bản đồ. Không bắt buộc gửi tọa độ vào chat.
- Vị trí chỉ được lấy sau thao tác đồng ý, không tự thu thập khi mở trang. Vị trí chính xác là thông tin nhạy cảm; tránh chép vào log hoặc tài liệu đánh giá.
- Định vị có thể bị từ chối, hết thời gian, không hỗ trợ hoặc bị hạn chế bởi kết nối không an toàn. Chưa đủ thông tin để kết luận nguyên nhân thuộc hệ thống CoParking.
- Không có bãi: kiểm tra điểm, tăng bán kính hoặc đổi khu vực; không có kết quả khác với xác nhận mọi bãi đều đầy.
- Nền bản đồ không tải: danh sách và chỉ đường vẫn có thể dùng nếu dữ liệu đã tải. Tra địa chỉ và tìm gần đây có thể bị tắt độc lập.

## Ý nghĩa kết quả và giới hạn
Khoảng cách gần đây được tính theo vị trí địa lý, không phải thời gian lái xe. Số chỗ và giá phải lấy từ nguồn nghiệp vụ hiện tại, phụ thuộc bãi, loại xe và thời điểm; không bảo đảm còn chỗ khi đến. Không suy ra giờ mở cửa từ trạng thái hoạt động. Chat hiện chưa có công cụ tra bãi, vị trí hoặc số chỗ trực tiếp, dù ứng dụng đã có chức năng tìm kiếm và bản đồ. Không trả bãi cụ thể hay số chỗ giả; hướng dẫn người dùng tra trên giao diện.

## Xem còn chỗ khác với được nhận xe
Xem bãi còn chỗ không có nghĩa được nhận xe khi đến nơi. Thông tin này không bảo đảm bãi còn chỗ phù hợp lúc bạn đến và không tạo giữ chỗ. Nhân viên còn phải kiểm tra loại xe, thẻ hoặc vé và điều kiện hoạt động; muốn đặt trước phải dùng đúng chức năng và xem điều kiện trước khi xác nhận.

## Câu hỏi khách hàng thường gặp
- Tìm bãi xe gần tôi như thế nào? Mở Tìm bãi xe và chọn Tìm bãi gần tôi, sau đó cho phép trình duyệt dùng vị trí cho lần tìm kiếm. Nếu không muốn chia sẻ vị trí, dùng Tìm địa chỉ hoặc chọn điểm trên bản đồ. Trợ lý chưa tự biết vị trí của bạn.
- Không cho phép định vị thì tìm bãi xe được không? Bạn có thể dùng Tìm địa chỉ hoặc chọn điểm trên bản đồ để tìm bãi mà không bật định vị. Chỉ bật lại quyền vị trí trong trình duyệt nếu bạn muốn dùng Tìm bãi gần tôi; không cần gửi tọa độ chính xác qua chat.
- Định vị tìm bãi xe hết thời gian thì làm sao? Kiểm tra quyền vị trí, tín hiệu định vị và kết nối rồi thử lại khi phù hợp. Có thể dùng Tìm địa chỉ hoặc chọn điểm trên bản đồ thay thế. Trình duyệt không hỗ trợ, kết nối không an toàn hoặc tín hiệu yếu đều có thể ảnh hưởng; chưa đủ thông tin để khẳng định một nguyên nhân.
- Tìm bãi xe theo địa chỉ như thế nào? Chọn Tìm địa chỉ, nhập địa chỉ có ít nhất 3 ký tự và chọn kết quả phù hợp, rồi tìm bãi quanh điểm đó. Nếu không ra kết quả, bổ sung số nhà, đường hoặc khu vực; khi tra địa chỉ tạm dừng, dùng định vị hoặc chọn điểm trên bản đồ nếu tính năng đó đang bật.
- Đổi bán kính tìm bãi xe như thế nào? Ở Bán kính tìm kiếm, chọn hoặc nhập số nguyên từ 1 đến 50 km rồi bấm Áp dụng. Có thể chọn lại điểm tìm kiếm nếu cần; kết quả phụ thuộc vị trí và dữ liệu bãi hiện có.
- Không tìm thấy bãi xe gần đây thì làm sao? Kiểm tra điểm tìm kiếm và thử tăng bán kính hoặc chọn khu vực khác. Không có kết quả không có nghĩa mọi bãi đều đầy: còn phụ thuộc phạm vi, tọa độ và dữ liệu bãi đang được cung cấp. Nếu tính năng đang tạm dừng, thử lại sau; không bịa một bãi thay thế.
- Bản đồ bãi xe không tải được thì làm sao? Bạn vẫn có thể xem danh sách kết quả và dùng nút chỉ đường nếu dữ liệu bãi đã tải. Kiểm tra kết nối rồi thử lại bản đồ; bản đồ lỗi không tự có nghĩa bãi đã đóng hoặc yêu cầu đặt chỗ đã mất.
- Chỉ đường đến bãi xe như thế nào? Chọn bãi trong kết quả và dùng nút chỉ đường để mở Google Maps. Khoảng cách tìm bãi là khoảng cách địa lý quanh điểm đã chọn, không phải cam kết quãng đường hoặc thời gian lái xe. Xem tuyến đường thực tế trên ứng dụng bản đồ.
- Bãi xe nào gần tôi hiện còn chỗ? Trợ lý chưa tra cứu trực tiếp vị trí và số chỗ hiện tại trong chat. Mở Tìm bãi xe, chọn vị trí và xem kết quả của bãi phù hợp loại xe. Số chỗ có thể thay đổi; xem trên bản đồ chưa phải đặt hoặc giữ chỗ.
- Tìm bãi trên bản đồ có tự giữ chỗ không? Không. Tìm bãi và chỉ đường chỉ giúp xem thông tin, không tạo đặt chỗ hoặc giữ chỗ. Muốn đặt trước, mở Đặt chỗ trước và kiểm tra điều kiện, thời hạn, số tiền rồi xác nhận theo giao diện; thông tin còn chỗ có thể thay đổi.

## Lưu ý khi trả lời và đối chiếu kết quả
Không bật vị trí vẫn có thể chọn địa chỉ hoặc điểm tìm nếu chức năng tương ứng đang bật. Không có kết quả chưa chứng minh mọi bãi đều đầy. Khoảng cách địa lý không phải thời gian lái xe; xem bãi trên bản đồ không tạo đặt chỗ và không bảo đảm được nhận xe khi đến.

<!-- authoring-only:start -->

## Nguồn đối chiếu
Xem [ma trận truy vết đợt 4](../evaluation/batch-04-traceability.md). Nguồn chính: PublicParkingLotController, NearbyParkingLotUseCaseImpl, PublicParkingLotDirectoryUseCaseImpl, PostGisNearbyParkingLotAdapter, PublicParkingLocationUseCaseImpl; frontend NearbyParkingLotsPage/NearbyParkingMap. Quyết định nghiệp vụ không tự chứng minh chức năng runtime đã có hoặc được bật.

## Nội dung TBD
Xem khoảng trống và giới hạn bằng chứng trong báo cáo đợt 4; không dùng giá trị hoặc trạng thái chưa xác minh làm cam kết. Nội dung vẫn DRAFT, chưa được duyệt hoặc publish.

## Lịch sử phiên bản
- 2.1.0, 2026-11-24: rà soát lại với code hiện tại; làm rõ điều kiện và giới hạn trả lời, chuẩn hóa ngôn ngữ; tiếp tục DRAFT. Kiểm thử và giới hạn bằng chứng tại báo cáo ALL_BATCHES_QWEN_REVIEW_REPORT.vi.md.
- 2.0.0, 2026-11-23: rà soát đợt 4 theo code, UI và sổ quyết định; tách hướng dẫn khách khỏi thao tác nội bộ, bổ sung FAQ và giới hạn dữ liệu hiện tại. Chưa publish.

<!-- authoring-only:end -->
