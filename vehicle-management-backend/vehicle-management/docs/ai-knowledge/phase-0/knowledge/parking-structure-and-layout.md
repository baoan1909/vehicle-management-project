<!--
title: Cấu trúc bãi xe và xuất bản sơ đồ chỗ đỗ
slug: parking-structure-and-layout
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

# Cấu trúc bãi xe và xuất bản sơ đồ chỗ đỗ

## Mục đích
Cấu trúc bãi xe và xuất bản sơ đồ chỗ đỗ.

## Đối tượng
Nhân sự được cấp quyền đọc hướng dẫn nội bộ. Nguồn chứa quy tắc chung, không chứa hồ sơ hay cấu hình riêng của đơn vị.

## Phạm vi
Nhãn, tường, lối đi và phần tử trang trí không tự tạo sức chứa. Ô đỗ được xuất bản có thể thay đổi dữ liệu khai thác thật. Có thao tác hỗ trợ trong trình biên tập nhưng không cam kết hệ thống tự thiết kế tối ưu hoặc tự bố trí hoàn chỉnh. Chatbot không có công cụ sửa/xuất bản sơ đồ.

## Điều kiện trước
Chọn đúng bãi thuộc phạm vi được cấp. Người cấu hình cần quyền cấu trúc và phạm vi phù hợp; quản lý bãi được phân công có thể cấu hình khi có đủ quyền, không chỉ dựa tên chức danh. Quyền xuất bản sơ đồ tách khỏi quyền lưu nháp.

## Luồng chính
1. Khai báo bãi thuộc tổ chức, sau đó tầng và khu thuộc đúng tầng/bãi. Làn vào/ra là thành phần vận hành của bãi; không suy rằng mọi làn là chỗ đỗ.
2. Trong cấu trúc bãi chọn Theo sức chứa (nhập tay) hoặc Theo từng ô (sơ đồ) phù hợp. Mở sơ đồ, chọn tầng/khu, tạo bản nháp hoặc mở sơ đồ phát hành để chỉnh sửa.
3. Tạo/sửa các ô đỗ và các phần tử hiển thị; chọn Lưu draft, kiểm tra bản xem trước và xuất bản khi có quyền. Sơ đồ hiện hành giữ nguyên cho tới khi xuất bản thành công.
4. Xuất bản kiểm tra mã ô, hình học, giới hạn khung, chồng lấn, phiên bản nháp và quan hệ tầng/khu/bãi; tạo/cập nhật chỗ đỗ thật, liên kết ô và cập nhật sức chứa theo kết quả hợp lệ.

## Luồng lỗi và ngoại lệ
Không xóa tầng khi còn khu. Không chuyển tầng sang bãi khác. Đổi/xóa ô khi đang có phiên gửi xe mở hoặc phân bổ vé có thể bị chặn; chuyển sang theo từng ô có điều kiện về phiên đang mở. Bản nháp bị người khác sửa cần tải lại, đối chiếu rồi lưu; không ép ghi đè.
Khi báo “vẫn lỗi”, “không thấy”, “không được”, hỏi màn hình, thao tác, thông báo lỗi đã che dữ liệu nhạy cảm và bãi/đơn vị liên quan. Không kết luận nguyên nhân chỉ từ câu báo lỗi ngắn.

## Câu hỏi khách hàng thường gặp

- Khai báo cấu trúc bãi xe theo thứ tự nào? Chọn bãi thuộc đúng đơn vị, tạo tầng rồi khu gắn đúng tầng/bãi, sau đó cấu hình ô đỗ và sơ đồ theo cách quản lý sức chứa. Làn vào/ra là thành phần vận hành riêng; cần quyền cấu hình và đúng phạm vi bãi.

  **Cách hỏi tương đương:** Tôi thiết lập tầng khu và sơ đồ bãi như thế nào?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Lưu bản nháp sơ đồ có đổi chỗ đỗ đang dùng không? Lưu bản nháp chưa thay sơ đồ đang vận hành. Khi xuất bản hợp lệ, ô đỗ có thể được tạo hoặc cập nhật và sức chứa được đồng bộ; cần xem trước và có quyền xuất bản riêng.

  **Cách hỏi tương đương:** Nháp sơ đồ và sơ đồ phát hành khác nhau thế nào?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Vẽ tường hay nhãn có tạo thêm chỗ đỗ không? Tường, nhãn và phần tử trang trí không tự tạo chỗ đỗ. Các ô đỗ hợp lệ khi xuất bản mới liên kết với dữ liệu chỗ đỗ thật và cập nhật sức chứa theo quy tắc; không coi toàn bộ bản vẽ chỉ là hình trang trí.

  **Cách hỏi tương đương:** Phần tử hiển thị trong sơ đồ có tăng sức chứa không?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Xóa tầng khi còn khu được không? Không xóa tầng khi còn khu. Kiểm tra các khu và dữ liệu liên quan trong đúng bãi trước khi thay đổi; không chuyển tầng sang bãi khác để né ràng buộc.

  **Cách hỏi tương đương:** Tầng đang có khu có xóa ngay được không?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Xóa ô đang có xe hoặc đang phân bổ vé được không? Ô có phiên gửi xe đang mở hoặc phân bổ vé có thể bị chặn thay đổi hay xóa. Cần kiểm tra dữ liệu thật, xem trước tác động và xử lý liên quan theo quyền; chatbot không tự xóa ô hoặc kết thúc lượt gửi.

  **Cách hỏi tương đương:** Có thể bỏ chỗ đỗ đang sử dụng khỏi sơ đồ không?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Sơ đồ? Bạn muốn xem sơ đồ, tạo nháp, sửa ô hay xuất bản? Cho biết bãi, tầng/khu và màn hình đang gặp vấn đề để tôi hướng dẫn đúng.

  **Cách hỏi tương đương:** Tôi muốn hỏi về cấu trúc bãi

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Hỏi rõ nhu cầu và màn hình trước khi trả lời chi tiết.

- Tại sao xuất bản sơ đồ báo bị người khác sửa? Tải lại bản nháp mới nhất, đối chiếu thay đổi rồi thử lại với phiên bản hiện tại. Không ép ghi đè vì người khác có thể đã sửa; xuất bản còn cần quyền và kiểm tra dữ liệu chỗ đỗ liên quan.

  **Cách hỏi tương đương:** Lưu sơ đồ gặp xung đột phiên bản thì phải làm sao?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Hệ thống tự vẽ tối ưu toàn bộ sơ đồ cho tôi được không? Chưa có cơ sở để hứa tự thiết kế tối ưu hoặc tự bố trí hoàn chỉnh chỉ từ địa chỉ. Dùng trình biên tập để khai báo tầng/khu và ô đỗ, xem trước rồi xuất bản khi đủ quyền; chatbot không tự vẽ hay phát hành.

  **Cách hỏi tương đương:** Có tự bố trí hoàn chỉnh bãi chỉ từ địa chỉ không?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Xuất bản sơ đồ mới cho bãi của tôi ngay đi? Chatbot chưa có công cụ sửa hoặc xuất bản sơ đồ. Người có quyền cần chọn đúng bãi/khu, kiểm tra nháp, phiên đang mở và phân bổ vé trước khi tự thực hiện; chưa có thay đổi nào được thực hiện trong cuộc chat.

  **Cách hỏi tương đương:** Bạn sửa và phát hành sơ đồ giúp tôi được không?

  **Ranh giới hướng dẫn/thao tác:** Chỉ hướng dẫn; không tạo, duyệt hoặc sửa dữ liệu trong chat.

  **Khi cần dữ liệu thật/hỏi lại:** Nếu hỏi về hồ sơ, nhân sự, bãi hoặc danh mục hiện tại, phải kiểm tra quyền và nguồn dữ liệu thật; chưa có công cụ thì báo rõ và chuyển người phụ trách.

- Tôi sửa sơ đồ bãi khác không được giao bằng cách đổi mã được không? Không hỗ trợ sửa sơ đồ bãi ngoài phạm vi được giao hoặc đổi mã để vượt kiểm tra. Bạn cần người quản trị xác nhận quyền cấu hình và phân công đúng bãi.

  **Cách hỏi tương đương:** Chỉ cách đổi địa chỉ truy cập để sửa sơ đồ ngoài phạm vi đi

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
