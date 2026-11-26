---
title: Bảng giá và cách tính phí
slug: pricing-and-fee-calculation
documentVersion: 2.1.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Code backend, giao diện nghiệp vụ và kiểm thử được ghi trong traceability đợt 3
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-24
---

## Mục đích
Hướng dẫn xem bảng giá và hiểu cách tính phí mà không đoán số tiền phải trả.

## Đối tượng và phạm vi
Khách xem thông tin công khai; người dùng hỏi về bảng giá theo bãi, loại xe, loại vé và thời điểm. Không dùng tài liệu này để xác nhận hóa đơn của một người cụ thể.

## Điều kiện trước
Chọn đúng bãi, loại xe, loại vé và thời gian áp dụng. Số tiền hiện hành phải lấy từ dữ liệu hệ thống; ví dụ trong tài liệu không phải báo giá.

## Luồng chính
### Mức phí riêng của mỗi bãi
Mức phí khác nhau vì mỗi bãi có bảng giá và thời gian hiệu lực riêng, theo loại xe và loại vé. Khi so sánh, chọn cùng loại xe, loại vé và thời điểm; không kết luận tính sai chỉ vì giá khác bãi khác. Trợ lý không dùng số tiền trong ví dụ làm giá hiện hành.

### Xem bảng giá
1. Mở Bảng giá và chọn bãi xe.
2. Xem đúng loại xe, loại vé và thời gian áp dụng.
3. Kiểm tra số tiền trên hóa đơn trước khi thanh toán; khi có đặt chỗ, kiểm tra khoản đã trả trước.

## Luồng thay thế và lỗi
### Thiếu hoặc không tìm thấy bảng giá
Khi bảng giá bị thiếu hoặc chưa hiển thị mức phí, kiểm tra lại bãi, loại xe và loại vé đã chọn. Nếu vẫn chưa có, liên hệ nhân viên bãi hoặc gửi phản ánh lỗi chức năng để được kiểm tra. Không coi giá đang thiếu là miễn phí, không tự lấy giá của bãi khác. Việc chưa có bảng giá khác với hóa đơn có số tiền cần đối chiếu.

Chưa có giá, giá không phù hợp hoặc hóa đơn khác dự kiến: kiểm tra lại lựa chọn và liên hệ bãi để đối chiếu. Không tự thêm phụ thu hoặc dùng giá của bãi khác.

## Trạng thái và giới hạn
Bảng giá có thời gian hiệu lực và có thể ngừng áp dụng. Quy tắc tính lượt gửi được giải thích bên dưới theo code ngày kiểm chứng; không áp nguyên cách này cho giá vé tháng hoặc mọi nghiệp vụ khác.

## Câu hỏi khách hàng thường gặp
- Xem bảng giá gửi xe như thế nào? Mở Bảng giá, chọn bãi xe rồi xem đúng loại xe và loại vé. Giá hiển thị phụ thuộc bãi và thời điểm áp dụng; trợ lý không lấy số tiền minh họa làm giá hiện hành.
- Vì sao phí gửi xe khác giữa các bãi? Mỗi bãi có bảng giá và thời gian áp dụng riêng. Hãy kiểm tra cùng loại xe, loại vé và thời điểm ở bãi cần gửi; không dùng giá của bãi khác để kết luận hóa đơn bị tính sai.
- Phí gửi xe không quá bốn giờ được tính như thế nào? Trong cách tính lượt gửi xe thông thường đang có, phần thời gian còn lại không quá bốn giờ được áp mức ban ngày. Số tiền phải lấy từ bảng giá phù hợp của bãi; đây không phải mức tiền cố định cho mọi bãi.
- Gửi xe qua đêm được tính phí như thế nào? Với phần thời gian chưa đủ một ngày, nếu quá bốn giờ và không nằm hoàn toàn trong khung ban ngày thì cách tính hiện tại dùng mức ban đêm. Mức tiền cụ thể phụ thuộc bảng giá của bãi; kiểm tra hóa đơn nếu cần biết số phải trả.
- Gửi xe đủ một ngày thì tính phí như thế nào? Mỗi khoảng đủ hai mươi bốn giờ được tính bằng mức ban ngày cộng mức ban đêm. Phần thời gian còn lại được tính riêng theo quy tắc lượt gửi hiện tại. Trợ lý không tự suy ra số tiền nếu thiếu bảng giá đúng bãi.
- Gửi xe đúng ranh giới khung ban ngày tính phí ra sao? Phần thời gian chưa đủ một ngày và nằm cùng ngày, từ đúng giờ bắt đầu đến đúng giờ kết thúc khung ban ngày, được coi là nằm trong khung ban ngày. Số tiền cuối cùng do hệ thống tính với bảng giá phù hợp.
- Không thấy bảng giá của bãi thì phải làm sao? Kiểm tra đã chọn đúng bãi, loại xe và loại vé. Nếu vẫn chưa có giá, liên hệ bãi hoặc gửi phản ánh lỗi chức năng; không tự dùng giá của bãi khác và không coi giá bằng không.
- Hóa đơn gửi xe khác bảng giá thì kiểm tra như thế nào? Đối chiếu bãi, loại xe, thời gian vào ra, bảng giá có hiệu lực và khoản đã trả trước nếu có đặt chỗ. Nếu vẫn khác, cung cấp thông tin giao dịch cần thiết cho nhân viên bãi kiểm tra; không gửi mã thanh toán bí mật.
- Phí gửi xe ngày lễ có tự tăng không? Không mặc định ngày lễ sẽ tăng giá. Cách tính lượt gửi xe hiện được đối chiếu chưa tự dùng lịch ngày lễ để cộng phụ thu. Hãy xem bảng giá có hiệu lực của bãi; trợ lý không tự thêm phí hoặc cam kết chính sách chưa có nguồn.
- Đặt chỗ đã trả trước thì khi ra bãi tính phí thế nào? Với lượt gửi xe gắn đặt chỗ, thời gian tính phí bắt đầu khi thanh toán thành công và kết thúc khi ra bãi. Hệ thống dùng bảng giá đã ghi nhận khi đặt, trừ khoản trả trước và chỉ thu phần phát sinh thêm; không thu lại toàn bộ lần nữa.

## Điều chatbot được phép trả lời
Giải thích hướng dẫn, điều kiện và giới hạn đã được kiểm chứng. Trả lời bằng từ ngữ dễ hiểu; không đưa tên trạng thái kỹ thuật vào câu trả lời.

## Điều chatbot phải dùng chức năng được cấp quyền
Thông tin cá nhân, giá hiện hành, số tiền hoặc trạng thái cụ thể chỉ được xác nhận bằng nguồn dữ liệu có thẩm quyền và đúng quyền. Nếu chưa có tool phù hợp, hướng dẫn mở module nghiệp vụ hoặc liên hệ bộ phận phụ trách; không giả lập kết quả.

## Điều chatbot phải từ chối
Không tự thực hiện thanh toán, hoàn tiền, đặt/hủy chỗ, áp mã hay sửa hóa đơn. Không yêu cầu mật khẩu, mã xác minh, thông tin thẻ bí mật, mã truy cập đặt chỗ hoặc QR riêng tư. Không hứa chắc số tiền, hiệu lực hoặc thời hạn chưa có nguồn.

## Khi nào chuyển nhân viên
Giao dịch bất thường, tiền đã trừ chưa rõ kết quả, tranh chấp hoặc cần kiểm tra dữ liệu riêng ngoài khả năng tra cứu được cấp. Phân biệt liên hệ bãi xử lý nghiệp vụ với báo lỗi chức năng hệ thống; không tự tạo phiếu thay cho mọi yêu cầu tài chính.

## Lưu ý khi trả lời và đối chiếu kết quả
Hướng dẫn cách tính không phải báo giá cho một lượt gửi cụ thể. Cần đúng bãi, loại xe, loại vé, thời gian và bảng giá có hiệu lực. Nếu thiếu giá hoặc dữ liệu thời gian, không đoán số tiền, không coi là miễn phí và không tự cộng phụ thu.

<!-- authoring-only:start -->
## Nguồn đối chiếu
Đường dẫn code, quyền, API, UI và bằng chứng kiểm thử được lưu ở hồ sơ traceability đợt 3 ngoài bản tri thức xuất. Không nhúng chi tiết quản trị hoặc bí mật vào nội dung người dùng.

## Nội dung TBD
Chưa nghiệm thu/publish trên corpus DB dev. Cấu hình cổng thanh toán và số tiền hiện hành cần kiểm tra tại môi trường thực tế. Xem báo cáo đợt 3 để biết giới hạn kiểm chứng.

## Lịch sử phiên bản
- 2.1.0, 2026-11-24: rà soát lại với code hiện tại; làm rõ điều kiện và giới hạn trả lời, chuẩn hóa ngôn ngữ; tiếp tục DRAFT. Kiểm thử và giới hạn bằng chứng tại báo cáo ALL_BATCHES_REVIEW_STATUS.vi.md.
- 2.0.0: rà soát code ngày 20/11/2026; giữ DRAFT, không thay chính sách nghiệp vụ.
<!-- authoring-only:end -->
