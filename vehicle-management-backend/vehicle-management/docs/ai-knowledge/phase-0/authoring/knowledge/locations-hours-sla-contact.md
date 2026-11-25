---
title: Giờ hoạt động, liên hệ và thời gian phản hồi
slug: locations-hours-sla-contact
documentVersion: 2.1.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: open-business-questions TBD-001..003, decision-log DEC-015; frontend PortalShared/ContactPage; kiểm tra không có public-contact use case
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-24
---

## Mục đích và nguồn quyết định
Hướng dẫn liên hệ CoParking và giải thích giờ tiếp nhận, giờ từng bãi và mục tiêu phản hồi. Thông tin cấp công ty và mục tiêu được phê duyệt tại sổ quyết định nghiệp vụ; không đồng nghĩa mọi chức năng giám sát hoặc gửi yêu cầu đã triển khai.

## Thông tin cấp công ty
- Hotline: 0912345678.
- Email: support@coparking.vn.
- Trụ sở: 10/5 Đường Tỉnh Lộ 19, Phường An Phú Đông, Thành phố Hồ Chí Minh.
- Tiếp nhận hỗ trợ 24/7. Đây không phải giờ mở cửa của tất cả bãi và không phải cam kết xử lý ngay.

## Giờ nhận yêu cầu và ý nghĩa phản hồi
Tiếp nhận hỗ trợ cả ban ngày và ban đêm, gồm cuối tuần và ngày lễ. Giờ tiếp nhận hỗ trợ 24/7 không phải giờ mở cửa của mọi bãi. Phản hồi tự động của chatbot khác phản hồi của người thật: tin tự động không được tính là phản hồi đầu tiên của nhân viên, cũng không xác nhận yêu cầu đã giải quyết.

## Biểu mẫu và nút gửi liên hệ
Nút “Chưa nhận yêu cầu qua biểu mẫu” hiện bị vô hiệu hóa, chưa dùng để gửi được. Nếu nút chưa dùng được, hãy gửi email support@coparking.vn hoặc gọi hotline 0912345678. Không coi việc điền biểu mẫu là đã gửi yêu cầu.

## Mục tiêu phản hồi đầu tiên
Khẩn cấp: 30 phút; cao: 2 giờ; bình thường: 8 giờ; thấp: 48 giờ. Tính liên tục, gồm cuối tuần và ngày lễ theo giờ Việt Nam. Tin tự động/chatbot không tính là phản hồi người thật. Đây là mục tiêu nghiệp vụ đã phê duyệt, không phải thời gian giải quyết xong, cũng không phải xác nhận hệ thống đang theo dõi đủ mọi mốc. Chat chưa tra được thời hạn riêng của yêu cầu hiện tại.

## Luồng liên hệ
1. Nếu đang bị kẹt tại cổng hoặc có vấn đề vào/ra, báo nhân viên tại bãi.
2. Với câu hỏi cấp công ty hoặc lỗi chức năng, gọi hotline hoặc email. Nêu bước thực hiện, thời điểm, thông báo và mã yêu cầu cần thiết; không gửi bí mật hoặc giấy tờ đầy đủ.
3. Phiếu hỗ trợ hệ thống tuân theo kênh, quyền và xác nhận riêng. Chat với nhân sự đối tác không tự trở thành hội thoại hỗ trợ hệ thống.
4. Biểu mẫu tại trang Liên hệ hiện chưa nhận yêu cầu. Chưa có luồng gửi đầy đủ; điền form không có nghĩa đã gửi. Giao diện cần thể hiện rõ chưa gửi được và nêu kênh thay thế.

## Giờ từng bãi và xử lý mâu thuẫn
Chưa có nguồn công bố giờ mở cửa riêng từng bãi được kiểm chứng trong phạm vi này. Hỏi bãi nào và xác nhận bằng thông tin chính thức của bãi hoặc nhân viên. Trạng thái hoạt động, dữ liệu demo hoặc giờ hỗ trợ công ty không thay thế giờ bãi. Với nguồn thiếu, cũ hoặc mâu thuẫn, nói chưa thể xác nhận thay vì chọn một giá trị. Không lấy địa chỉ trụ sở làm địa chỉ mọi bãi.

## Phần đã làm và phần chưa triển khai
Thông tin liên hệ FE được đồng bộ với quyết định hiện hành trong đợt 4; form không gửi được được thể hiện rõ. Chức năng tiếp nhận liên hệ qua biểu mẫu với xác minh chống gửi tự động, giới hạn số lần gửi và theo dõi vẫn cần triển khai riêng. Không công bố đã có hàng đợi hoặc email tự động chỉ từ thiết kế.

## Câu hỏi khách hàng thường gặp
- Hotline hỗ trợ CoParking là gì? Hotline chính thức của CoParking là 0912345678, tiếp nhận hỗ trợ 24/7. Đây là kênh hỗ trợ cấp công ty; nếu đang gặp lỗi tại cổng, hãy báo nhân viên tại bãi để được kiểm tra trực tiếp.
- Email hỗ trợ CoParking là gì? Bạn có thể gửi email đến support@coparking.vn. Nêu chức năng đang gặp vấn đề, bước thực hiện, thời điểm và thông báo lỗi; không gửi mật khẩu, mã xác minh, thông tin truy cập bí mật hoặc giấy tờ cá nhân đầy đủ.
- Trụ sở CoParking ở đâu? Trụ sở CoParking ở 10/5 Đường Tỉnh Lộ 19, Phường An Phú Đông, Thành phố Hồ Chí Minh. Đây là địa chỉ cấp công ty, không phải địa chỉ của mọi bãi xe. Muốn tìm bãi cụ thể, dùng Tìm bãi xe và xem đúng bãi.
- Hỗ trợ CoParking nhận yêu cầu vào giờ nào? CoParking tiếp nhận hỗ trợ 24/7 theo quyết định nghiệp vụ hiện hành. Tiếp nhận liên tục không có nghĩa nhân viên phản hồi hoặc giải quyết ngay. Giờ mở cửa từng bãi cần được kiểm tra riêng.
- Bãi xe mở cửa lúc mấy giờ? Cần biết bạn hỏi bãi nào và kiểm tra thông tin đã xác minh của bãi đó. Trợ lý chưa có nguồn giờ mở cửa riêng từng bãi để xác nhận trong chat; không suy ra bãi mở 24/7 từ giờ tiếp nhận hỗ trợ. Hãy xem thông tin bãi hoặc liên hệ nhân viên bãi để xác nhận.
- Bao lâu nhân viên phản hồi yêu cầu hỗ trợ? Mục tiêu phản hồi đầu tiên của nhân viên theo mức ưu tiên là: khẩn cấp 30 phút, cao 2 giờ, bình thường 8 giờ và thấp 48 giờ, tính liên tục theo giờ Việt Nam. Đây là mục tiêu nghiệp vụ, không phải thời hạn giải quyết hay xác nhận đang được hệ thống giám sát đầy đủ.
- Chatbot trả lời có tính là nhân viên đã phản hồi không? Không. Tin nhắn tự động hoặc câu trả lời của chatbot không được tính là phản hồi đầu tiên của nhân viên. Mục tiêu phản hồi người thật cũng không đồng nghĩa sự cố đã được giải quyết.
- Gửi biểu mẫu liên hệ CoParking như thế nào? Biểu mẫu tại trang Liên hệ hiện chưa nhận yêu cầu vì luồng gửi chưa được triển khai đầy đủ. Bạn có thể gọi hotline 0912345678 hoặc gửi email support@coparking.vn. Không coi việc điền biểu mẫu là yêu cầu đã được gửi.
- Bãi còn hoạt động có nghĩa đang mở cửa không? Không. Trạng thái hoạt động của bãi không tự xác nhận giờ mở cửa hoặc khả năng nhận xe ngay lúc này. Còn phải kiểm tra thông tin giờ của bãi, chỗ phù hợp loại xe và điều kiện vận hành; liên hệ nhân viên bãi khi chưa rõ.
- Thông tin liên hệ hoặc giờ mở cửa mâu thuẫn thì làm sao? Không tự chọn một giá trị để khẳng định. Cho biết bãi và thông tin đang mâu thuẫn, rồi xác nhận với kênh chính thức hoặc nhân viên bãi. Không lấy địa chỉ mẫu, bản cũ hoặc nội dung đã hết hiệu lực làm cam kết.

## Lưu ý khi trả lời và đối chiếu kết quả
Giờ tiếp nhận của công ty, giờ mở cửa từng bãi, mục tiêu nhân viên phản hồi và thời gian giải quyết là bốn thông tin khác nhau. Chỉ thông tin đã có nguồn mới được xác nhận. Không dùng tin trả lời của chatbot để kết luận nhân viên đã nhận xử lý hoặc yêu cầu đã được giải quyết.

<!-- authoring-only:start -->

## Nguồn đối chiếu
Xem [ma trận truy vết đợt 4](../evaluation/batch-04-traceability.md). Nguồn chính: open-business-questions TBD-001..003, decision-log DEC-015; frontend PortalShared/ContactPage; kiểm tra không có public-contact use case. Quyết định nghiệp vụ không tự chứng minh chức năng runtime đã có hoặc được bật.

## Nội dung TBD
Xem khoảng trống và giới hạn bằng chứng trong báo cáo đợt 4; không dùng giá trị hoặc trạng thái chưa xác minh làm cam kết. Nội dung vẫn DRAFT, chưa được duyệt hoặc publish.

## Lịch sử phiên bản
- 2.1.0, 2026-11-24: rà soát lại với code hiện tại; làm rõ điều kiện và giới hạn trả lời, chuẩn hóa ngôn ngữ; tiếp tục DRAFT. Kiểm thử và giới hạn bằng chứng tại báo cáo ALL_BATCHES_QWEN_REVIEW_REPORT.vi.md.
- 2.0.0, 2026-11-23: rà soát đợt 4 theo code, UI và sổ quyết định; tách hướng dẫn khách khỏi thao tác nội bộ, bổ sung FAQ và giới hạn dữ liệu hiện tại. Chưa publish.

<!-- authoring-only:end -->
