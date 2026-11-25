<!--
title: Thẻ gửi xe và cách xử lý mất thẻ
slug: cards-and-lost-card
documentVersion: 2.3.0
status: DRAFT
language: vi
accessScope: CUSTOMER
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Giao diện, nghiệp vụ, phân quyền và migration đã đối chiếu trong bằng chứng đợt 2
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-24
-->

# Thẻ gửi xe và cách xử lý mất thẻ

## Mục đích
Hướng dẫn xử lý thẻ không sử dụng được, báo mất thẻ và tìm lại thẻ an toàn.

## Đối tượng
Khách hàng cần trợ giúp và nhân sự được cấp quyền xử lý mất thẻ tại bãi xe thuộc phạm vi phụ trách.

## Phạm vi
Thông tin thẻ vật lý, việc báo mất và quy trình nhân viên xử lý. Phiếu báo mất thẻ là hồ sơ vận hành riêng, khác phiếu hỗ trợ để báo lỗi chức năng hệ thống.

## Thuật ngữ
Thẻ vật lý dùng để nhận diện khi ra vào bãi. Phiếu báo mất thẻ ghi nhận sự việc và xử lý theo quy trình tại bãi. Mã QR của vé và thẻ vật lý không phải cùng một dữ liệu; không gửi mã sử dụng ra vào trong chat.

## Điều kiện trước
Khách hàng liên hệ nhân viên bãi xe để được xác minh thông tin. Nhân viên phải có quyền tạo hoặc xử lý phiếu và được phép thao tác tại đúng bãi xe. Khách hàng không tự lập phiếu báo mất chính thức qua chatbot.
Không gửi số giấy tờ định danh đầy đủ, ảnh giấy tờ, mã thẻ kỹ thuật hay mã QR vào chat. Nhân viên xác minh bằng quy trình nghiệp vụ được phép, không qua yêu cầu thu giấy tờ của chatbot.

## Luồng chính
1. Nếu mất thẻ, báo nhân viên bãi xe và cho biết xe còn trong bãi hay đã ra ngoài. Không dùng thẻ khác để tự mở cổng.
2. Nhân viên mở Tạo phiếu báo mất thẻ, Tra cứu biển số, đối chiếu xe, thẻ và thông tin xác minh.
3. Nhân viên lập phiếu sau khi đủ thông tin. Hệ thống đánh dấu thẻ cũ bị mất, ghi nhận lượt gửi liên quan và tạo hóa đơn theo dữ liệu đã xác định.
4. Đối chiếu số tiền trên hóa đơn và thực hiện thanh toán theo hướng dẫn. Không coi việc tạo phiếu là đã hoàn tất xử lý.
5. Khi hệ thống đã ghi nhận thanh toán, nhân viên hoàn tất phiếu. Trường hợp cần thẻ thay thế phải chọn thẻ phù hợp còn sẵn sàng tại cùng bãi và cùng loại với thẻ cũ. Việc mở thanh chắn theo kết quả xử lý, không phải do chatbot cam kết.

## Luồng thay thế
- Khách dùng vé và đang ở trong bãi: có thể cần thay thẻ, cập nhật liên kết với vé và kết thúc lượt gửi theo quy trình.
- Khách dùng vé nhưng đã ở ngoài bãi: nhân viên kiểm tra vé và thẻ; không tạo lượt xe ra giả hoặc hứa mở cổng.
- Khách gửi lượt thông thường: không tự yêu cầu gán thẻ mới vào vé tháng không tồn tại.
- Tìm lại thẻ trước khi trả tiền: báo nhân viên để kiểm tra khả năng hủy phiếu và khôi phục theo đúng trường hợp. Không tự dùng lại thẻ đã báo mất.

## Luồng lỗi
- Không tạo được phiếu: kiểm tra thông tin xe, lượt gửi hoặc vé, bãi xe, thời gian mất và dữ liệu xác minh. Có phiếu đang mở cho thẻ hoặc lượt gửi thì không tạo lặp.
- Chưa thể hoàn tất: kiểm tra hóa đơn đã được ghi nhận thanh toán chưa và đã chọn thẻ thay thế đúng yêu cầu chưa.
- Không hủy được: phiếu đã trả tiền hoặc đã có thanh toán thành công không được hủy theo luồng này. Liên hệ nhân viên có thẩm quyền; không hứa hoàn tiền.
- Không biết phí: xem hóa đơn thực tế. Phí phụ thuộc dữ liệu và cấu hình có hiệu lực; chatbot không tự đặt một mức phí cố định.

## Trạng thái và chuyển trạng thái
Phiếu báo mất: Đang mở → Đã xử lý hoặc Đã hủy, theo điều kiện được kiểm tra.
Thẻ có thể đang sẵn sàng, được giữ, được gán, đang sử dụng, bị mất, bị khóa hoặc ngừng sử dụng. Bị mất khác với tạm khóa; tìm thấy thẻ không tự làm phiếu hoàn tất hoặc khôi phục quyền ra vào.
Với quy trình báo mất, hệ thống đánh dấu thẻ cũ bị mất. Khi giao diện mô tả “khóa thẻ cũ”, không suy ra đó là mọi loại khóa thẻ hoặc tự dùng chức năng mở khóa thay cho xử lý báo mất.

## Câu hỏi khách hàng thường gặp
- Điều kiện tạo phiếu báo mất thẻ là gì? Nhân viên cần đúng quyền và phạm vi bãi, xác định đúng thẻ hoặc lượt gửi, kiểm tra thông tin xác minh và chưa có phiếu đang mở cho cùng thẻ hoặc lượt gửi. Không tạo nhiều phiếu khi chưa biết kết quả. Phiếu hỗ trợ chỉ báo lỗi phần mềm, không thay thế phiếu báo mất thẻ. Thanh toán ghi nhận và thẻ thay thế khả dụng, cùng bãi, đúng loại được kiểm tra ở bước hoàn tất khi áp dụng.
- Mất thẻ phải làm gì? Báo nhân viên bãi xe, cho biết xe còn trong bãi hay đã ra ngoài, rồi thực hiện xác minh theo hướng dẫn.
- Có báo mất qua chat được không? Chat có thể hướng dẫn; nhân viên có quyền mới lập hồ sơ báo mất chính thức.
- Tạo phiếu hỗ trợ có thay phiếu báo mất không? Không. Phiếu hỗ trợ chỉ dùng để báo lỗi chức năng; việc báo mất xử lý tại nghiệp vụ mất thẻ.
- Phí mất thẻ bao nhiêu? Xem hóa đơn và cấu hình áp dụng; chưa có dữ liệu thì không đưa số tiền.
- Đã trả tiền mà chưa xong thì sao? Nhờ nhân viên kiểm tra tiền đã được ghi nhận và bước cấp thẻ hoặc hoàn tất còn thiếu.
- Tìm lại thẻ rồi phải làm sao? Báo nhân viên; không tự dùng lại. Khả năng hủy phiếu phụ thuộc tình trạng thanh toán và xử lý.
- Thẻ bị khóa có phải bị mất không? Không; cần kiểm tra nguyên nhân thực tế tại vé hoặc với nhân viên.
- Chat có khóa hoặc cấp lại thẻ cho tôi không? Chưa hỗ trợ, và không được khẳng định đã làm thay.
- Có phải gửi ảnh căn cước qua chat không? Không. Thực hiện xác minh qua quy trình của nhân viên bãi xe.
- Chỉ hỏi “thẻ” hoặc “mất thẻ” thì sao? Hỏi rõ vấn đề và xe đang ở trong hay ngoài bãi trước khi hướng dẫn cụ thể.
- Tạo phiếu mất thẻ thất bại cần kiểm tra gì? Nhân viên kiểm tra đúng thẻ, bãi xe, tình trạng xe trong hay ngoài bãi, quyền xử lý và thông báo lỗi. Khách hàng báo nhân viên; không tự gửi lại nhiều phiếu khi chưa biết kết quả lần trước.
- Vì sao chưa hoàn tất phiếu báo mất thẻ được? Nhờ nhân viên kiểm tra thanh toán đã được ghi nhận đầy đủ và điều kiện hoàn tất của phiếu. Nếu cần thay thẻ, thẻ thay thế phải còn sẵn sàng, đúng loại và cùng bãi. Không tự dùng lại thẻ đã báo mất hoặc trả tiền lại khi chưa đối chiếu giao dịch.
- Nhân viên tạo phiếu mất thẻ không được thì làm sao? Nhờ nhân viên kiểm tra đúng thẻ, bãi xe, tình trạng xe trong hay ngoài bãi, quyền xử lý, thông tin xác minh và có phiếu đang mở cho cùng thẻ hoặc lượt gửi hay chưa. Ghi lại thông báo lỗi; không gửi nhiều phiếu khi chưa biết kết quả lần trước. Thanh toán và thẻ thay thế là điều kiện ở bước hoàn tất, không phải điều kiện để lập phiếu lần đầu. Phiếu hỗ trợ báo lỗi phần mềm không thay thế phiếu báo mất thẻ.
- Cách kiểm tra khi tạo phiếu mất thẻ thất bại? Báo nhân viên kiểm tra đúng thẻ, bãi xe, tình trạng xe trong hay ngoài bãi, quyền xử lý, dữ liệu xác minh và phiếu đang mở có thể bị trùng. Cung cấp bước gặp lỗi và thông báo cụ thể. Không tự gửi lại nhiều phiếu; chưa yêu cầu thanh toán hoặc chọn thẻ thay thế ở bước lập phiếu lần đầu. Thanh toán và thẻ thay thế được kiểm tra ở bước hoàn tất khi trường hợp đó yêu cầu. Phiếu hỗ trợ báo lỗi phần mềm không thay thế phiếu báo mất thẻ.
- Phiếu báo mất thẻ bị lỗi cần làm gì? Báo nhân viên bãi xe, cho biết lỗi ở bước lập phiếu hay hoàn tất và thông báo cụ thể. Nhân viên kiểm tra đúng thẻ, bãi xe, tình trạng xe, quyền xử lý và có phiếu đang mở cho cùng thẻ hoặc lượt gửi hay chưa; không gửi nhiều phiếu khi chưa biết kết quả. Điều kiện thanh toán và thẻ thay thế được kiểm tra ở bước hoàn tất. Phiếu hỗ trợ báo lỗi phần mềm không thay thế phiếu báo mất thẻ.

## Điều chatbot được phép trả lời
Hướng dẫn phù hợp với khách hàng hoặc nhân sự, giải thích các bước và điều kiện. Với câu “lỗi tạo phiếu báo mất”, hỏi bước và thông báo lỗi; không tự tạo phiếu hỗ trợ để thay nghiệp vụ báo mất.

## Điều chatbot phải dùng chức năng được cấp quyền
Muốn xác nhận trạng thái thẻ, phí hoặc hồ sơ cụ thể phải có chức năng đọc dữ liệu đúng quyền. Chat hiện chưa có chức năng đọc trực tiếp trạng thái thẻ hoặc phiếu báo mất; hướng dẫn kiểm tra thông tin thẻ tại Vé tháng của tôi hoặc nhờ nhân viên. Không suy ra trạng thái thẻ chỉ từ trạng thái vé.

## Điều chatbot phải từ chối
Nhận giấy tờ định danh đầy đủ; tiết lộ mã thẻ, thông tin khách khác hoặc đường dẫn ảnh riêng tư; tự khóa, mở khóa, cấp lại, hủy hoặc hoàn tất phiếu khi chưa có chức năng và quyền hợp lệ.

## Khi nào chuyển nhân viên
Mất thẻ thực tế, xe đang ở bãi, cần xác minh, tìm lại thẻ đã báo mất, tranh chấp hóa đơn hoặc lỗi không hoàn tất được. Không đưa lời hứa về phí, thời gian cấp thẻ hoặc hoàn tiền khi chưa có chính sách và dữ liệu.

## Lưu ý khi trả lời và đối chiếu kết quả
Lập phiếu lần đầu và hoàn tất phiếu có điều kiện khác nhau. Chưa trả tiền không tự chứng minh không thể lập phiếu; đã trả tiền cũng chưa chứng minh đã hoàn tất. Nhân viên cần đối chiếu kết quả thanh toán và thẻ thay thế khi trường hợp đó yêu cầu. Không tự dùng lại thẻ đã báo mất.
