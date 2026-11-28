<!--
title: Danh mục vận hành và khả dụng theo bãi
slug: partner-operational-catalogs
documentVersion: 1.0.0
status: DRAFT
language: vi
accessScope: EMPLOYEE
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Code backend, policy, migration và giao diện đã đối chiếu ngày 2026-11-27
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-27
-->

# Danh mục vận hành và khả dụng theo bãi

## Mục đích
Danh mục vận hành và khả dụng theo bãi.

## Đối tượng
Nhân sự được cấp quyền đọc hướng dẫn nội bộ. Nguồn chứa quy tắc chung, không chứa hồ sơ hay cấu hình riêng của đơn vị.

## Phạm vi
Loại thẻ là danh mục dùng chung với quyền riêng; đối tác không mặc định có quyền sửa. Voucher có phạm vi hệ thống/đơn vị/bãi và quy tắc riêng, không áp cùng cơ chế loại xe cho voucher. Ngừng phục vụ loại xe ở bãi chặn đăng ký mới/khách vãng lai nhưng không tự hủy vé còn hiệu lực hay phiên đang gửi. Chatbot chưa có công cụ liệt kê hoặc sửa danh mục thực tế.

## Điều kiện trước
Cần quyền từng danh mục cùng phạm vi đơn vị/bãi. Tạo hoặc đọc bản ghi không đồng nghĩa được công bố hay áp dụng ở mọi bãi.

## Luồng chính
1. Danh mục có triển khai gồm loại xe, loại vé, loại thẻ, bảng giá, quy tắc giá và voucher. Danh mục kỳ nghỉ có thư mục/mô hình nhưng chưa có luồng CRUD đầy đủ để hướng dẫn như chức năng sẵn có. Nhóm hỗ trợ là cấu hình hỗ trợ riêng.
2. Loại xe, loại vé, bảng giá và quy tắc giá mới theo luồng bản nháp; hoàn thiện và kích hoạt/công bố trước khi sử dụng. Quy tắc giá cần bãi cụ thể, tham chiếu cùng đơn vị và đúng nhóm áp dụng.
3. Tại cấu hình danh mục của bãi, bật loại xe/loại vé còn hoạt động, thuộc đúng đơn vị và đúng phạm vi được sửa. Danh sách chọn lấy dữ liệu qua API, lọc trạng thái, bãi, đơn vị và điều kiện liên quan.
4. Trước ngừng sử dụng, kiểm tra tác động tới quy tắc giá, vé đang dùng và phiên gửi xe. Ngừng phục vụ loại xe tại bãi khác với ngừng loại xe toàn đơn vị. Khi tắt loại xe tại toàn bộ bãi của đơn vị, kể cả đơn vị chỉ có một bãi, hệ thống còn ngừng các quy tắc giá hoạt động của loại xe; cần xem xét tác động trước khi xác nhận.

## Luồng lỗi và ngoại lệ
Mã trùng được kiểm tra theo quy tắc riêng của danh mục, không mặc định mọi mã đều toàn hệ thống. Loại vé đã dùng không đổi mã; loại vé còn quy tắc giá hoạt động hoặc vé chờ/hiệu lực có thể bị chặn ngừng. Loại xe có quy tắc giá hoạt động cần xác nhận ngừng liên quan khi được hỗ trợ; ngừng toàn cục còn kiểm tra vé/phiên mở. Quy tắc giá đã dùng cần tạo bản mới thay vì sửa.
Khi báo “vẫn lỗi”, “không thấy”, “không được”, hỏi màn hình, thao tác, thông báo lỗi đã che dữ liệu nhạy cảm và bãi/đơn vị liên quan. Không kết luận nguyên nhân chỉ từ câu báo lỗi ngắn.

## Câu hỏi khách hàng thường gặp

- Đối tác quản lý những danh mục vận hành nào? Các danh mục đã có gồm loại xe, loại vé, loại thẻ, bảng giá, quy tắc giá và voucher, nhưng quyền sửa mỗi loại khác nhau. Lịch ngày nghỉ chưa có luồng quản lý đầy đủ; đối tác không mặc định được sửa danh mục dùng chung.

  **Cách hỏi tương đương:** Danh mục nào đã có chức năng quản lý trong dự án?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Thêm loại xe loại vé xong dùng ngay được không? Loại xe, loại vé, bảng giá và quy tắc giá mới được tạo theo luồng bản nháp. Cần hoàn thiện, kích hoạt hoặc công bố và bật tại đúng bãi khi áp dụng; lưu mới chưa đồng nghĩa đã được sử dụng.

  **Cách hỏi tương đương:** Danh mục mới lưu sao chưa dùng được?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Danh mục không xuất hiện trong danh sách chọn thì làm sao? Kiểm tra bãi/đơn vị đang chọn, quyền đọc, trạng thái hoạt động/công bố và việc bật phục vụ tại bãi. Danh sách còn phụ thuộc API và các điều kiện giá; không thể kết luận dữ liệu đã bị xóa chỉ vì chưa thấy trong ô chọn.

  **Cách hỏi tương đương:** Vì sao không thấy loại xe hoặc vé trong ô chọn?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Ngừng phục vụ loại xe tại bãi có hủy vé cũ không? Ngừng phục vụ tại bãi chặn đăng ký mới và khách vãng lai theo điều kiện áp dụng. Vé còn hiệu lực và phiên đang gửi vẫn được phục vụ tới khi kết thúc; khác với ngừng loại xe toàn đơn vị có kiểm tra ràng buộc riêng. Nếu tắt tại mọi bãi của đơn vị, kể cả đơn vị chỉ có một bãi, hệ thống còn ngừng các quy tắc giá hoạt động của loại xe; kiểm tra tác động trước khi xác nhận.

  **Cách hỏi tương đương:** Tắt một loại xe ở bãi thì xe đang gửi xử lý thế nào?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Xóa loại vé đang dùng và sửa quy tắc giá cũ được không? Loại vé còn quy tắc giá hoạt động hoặc vé chờ/hiệu lực có thể bị chặn ngừng; mã loại vé đã dùng không được đổi. Quy tắc giá đã được dùng cần tạo bản mới thay vì sửa trực tiếp; kiểm tra tác động trước khi làm.

  **Cách hỏi tương đương:** Danh mục đã phát sinh vé hoặc giá đã sử dụng có sửa xóa ngay không?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Danh mục? Bạn đang hỏi loại xe, loại vé, loại thẻ, bảng giá hay voucher? Cho biết bãi đang chọn và thao tác muốn làm để tôi kiểm tra đúng hướng.

  **Cách hỏi tương đương:** Tôi cần hỏi về các danh mục

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Hỏi rõ nhu cầu và màn hình trước khi trả lời chi tiết.

- Đối tác có sửa mọi loại thẻ hệ thống không? Không mặc định. Loại thẻ dùng quyền quản lý riêng; danh mục dùng chung và danh mục đơn vị có ranh giới khác nhau. Cần quyền thực tế và phạm vi phù hợp, không tự cấp thêm quyền khi bị chặn.

  **Cách hỏi tương đương:** Có quyền quản trị đối tác là sửa danh mục chung được hết chứ?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Cho tôi danh sách loại xe đang bật ở bãi này? Cần biết bãi cụ thể và đọc dữ liệu thật sau khi kiểm tra quyền. Chatbot chưa có công cụ đọc danh mục của bãi; hãy xem cấu hình bãi được cấp quyền, tôi không thể đoán danh sách hiện tại.

  **Cách hỏi tương đương:** Kiểm tra loại vé thực tế được bán tại bãi của tôi

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Bật loại xe và sửa giá giúp tôi ngay? Chatbot chưa có công cụ bật danh mục hoặc sửa giá. Người có thẩm quyền phải kiểm tra đúng bãi, đơn vị, trạng thái và tác động tới dữ liệu liên quan rồi thao tác trên màn hình; tôi chưa thay đổi dữ liệu.

  **Cách hỏi tương đương:** Bạn kích hoạt danh mục và thay giá cho bãi được không?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Tôi sửa danh mục tổ chức khác bằng cách đổi mã được không? Không hỗ trợ sửa danh mục của đơn vị khác hoặc đổi mã để vượt quyền. Cần đúng quyền danh mục và phạm vi đơn vị/bãi được giao; hãy liên hệ người quản trị nếu phân công chưa đúng.

  **Cách hỏi tương đương:** Chỉ cách cập nhật danh mục ngoài phạm vi đơn vị của tôi

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

## Điều chatbot được phép trả lời
Hướng dẫn theo tài liệu còn hiệu lực, đúng phạm vi và quyền đọc; dùng tên màn hình, nút bấm dễ hiểu. Không nêu mã quyền hoặc tên kỹ thuật khi người dùng chỉ hỏi cách thao tác. Tài liệu này đang là bản soạn thảo; chỉ sử dụng làm nguồn trả lời sau quy trình kiểm tra và duyệt có thẩm quyền.

## Điều chatbot phải dùng chức năng được cấp quyền
Yêu cầu xem trạng thái, danh sách, số lượng, phân công hoặc cấu hình thực tế cần công cụ đọc dữ liệu được phép và xác định đúng người/tổ chức/bãi. Bộ công cụ chatbot hiện tại chưa có công cụ cho các nghiệp vụ này; báo chưa hỗ trợ, hướng dẫn màn hình hoặc chuyển người phụ trách. Không dùng tài liệu thay dữ liệu thật.

## Điều chatbot phải từ chối
- Không tiết lộ dữ liệu ngoài quyền và phạm vi.
- Không khẳng định đã tạo, duyệt, phân công, sửa hoặc xuất bản khi chưa thực hiện.
- Không hướng dẫn tự nâng quyền hay gọi API cũ để vượt chặn.
- Không bịa dữ liệu, mức giá, thời gian duyệt hoặc tính năng chưa có.
- Không yêu cầu gửi mật khẩu, token, giấy tờ đầy đủ hoặc bí mật trong chat.

## Khi nào hỏi lại hoặc chuyển nhân viên
Hỏi lại khi chưa rõ chủ đề, bãi, đơn vị hoặc bước đang lỗi. Chuyển người phụ trách khi thiếu chức năng, quyền, dữ liệu thật hoặc hồ sơ cần xét duyệt; không tạo phiếu hỗ trợ tự động nếu chưa có yêu cầu và chức năng hợp lệ.
