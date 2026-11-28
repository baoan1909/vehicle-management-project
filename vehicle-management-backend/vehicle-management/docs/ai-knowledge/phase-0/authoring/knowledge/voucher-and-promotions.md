---
title: Mã ưu đãi và khuyến mãi vé tháng
slug: voucher-and-promotions
documentVersion: 2.2.0
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
Giải thích cách dùng và điều kiện mã ưu đãi trong luồng vé tháng hiện có.

## Đối tượng và phạm vi
Thông tin hướng dẫn công khai. Đăng ký, áp dụng mã và xem kết quả cá nhân vẫn cần truy cập giao diện nghiệp vụ với quyền phù hợp. Không chứa hướng dẫn tạo hoặc quản trị ưu đãi nội bộ.

## Điều kiện trước
Mã còn hiệu lực, được cho phép sử dụng, đúng bãi hoặc đối tác, đủ giá trị tối thiểu và còn lượt cho khách hàng. Không coi việc thấy banner là bảo đảm chắc chắn áp dụng thành công.

## Luồng chính
### Hiệu lực và hạn sử dụng voucher
Mã giảm giá, còn gọi là voucher hoặc mã ưu đãi, chỉ sử dụng được từ thời điểm bắt đầu và trước thời điểm kết thúc đã cấu hình. Voucher hết hạn là hết thời gian hiệu lực, khác với hết lượt sử dụng. Voucher không còn trong thời gian sử dụng sẽ bị từ chối; mã chưa đến ngày bắt đầu hoặc đang tạm dừng cũng không dùng được. Kiểm tra thời hạn và thông báo cụ thể, không gửi đăng ký nhiều lần để vượt điều kiện. Việc thấy mã trên banner hoặc đã xem thử trước đó không bảo đảm mã còn hiệu lực lúc tạo hóa đơn.

### Kiểm tra voucher và thời điểm giữ lượt
Kiểm tra hoặc xem thử voucher trước khi đăng ký chỉ cho biết điều kiện và số tiền dự kiến; bước này không tiêu thụ hay giữ lượt sử dụng mã. Khi gửi đăng ký tạo hóa đơn, hệ thống mới kiểm tra lại và giữ lượt. Vì vậy xem thử thành công chưa bảo đảm mã còn lượt lúc xác nhận đăng ký; lượt đang giữ cho hóa đơn chờ cũng được tính vào giới hạn. Không gửi đăng ký lặp để vượt số lượt.

### Điều kiện giá trị vé tối thiểu
Voucher có thể yêu cầu giá trị vé tối thiểu trước khi giảm. Nếu chưa đạt giá trị này, kiểm tra đúng bãi, loại vé và điều kiện của mã; không tự sửa giá trị hóa đơn để áp dụng. Đủ thời gian hiệu lực hoặc còn lượt không thay thế điều kiện về giá trị vé. Hãy xem thông báo kiểm tra mã trong Vé tháng để biết điều kiện chưa đạt.

### Cạnh tranh lượt voucher cuối
Hai khách hàng có thể cùng xem thử một mã còn lượt, nhưng kết quả xem thử chưa giữ lượt voucher cuối cho cả hai. Khi tạo đăng ký và hóa đơn, hệ thống kiểm tra lại tổng lượt và giới hạn mỗi khách trước khi giữ lượt. Nếu một người đã giữ lượt cuối, yêu cầu của người còn lại có thể báo hết lượt. Không gửi nhiều đăng ký lặp để tranh hoặc vượt lượt mã.

### Các bước áp dụng mã
1. Mở Vé tháng và chọn bãi, phương tiện, loại vé, ngày bắt đầu.
2. Nhập mã ưu đãi và xem kết quả kiểm tra.
3. Kiểm tra số tiền cuối cùng, rồi gửi đăng ký theo giao diện.
4. Hệ thống kiểm tra lại điều kiện và giữ lượt khi tạo đăng ký/hóa đơn; thanh toán thành công mới hoàn tất việc sử dụng.

## Luồng thay thế và lỗi
Mã không tồn tại, tạm dừng, hết hạn, sai bãi, chưa đủ giá trị hoặc hết lượt đều có thể không áp dụng được. Xem thông báo cụ thể; không nhập lặp để vượt giới hạn.

## Trạng thái và giới hạn
Mã chưa được phát hành, đang dùng và tạm dừng khác với lượt mã đang giữ, đã dùng và đã giải phóng. Chưa có căn cứ áp mã cho mọi dịch vụ hoặc cộng dồn nhiều mã trên cùng vé.

### Giải phóng lượt voucher khi hủy đăng ký chưa thanh toán
Khi hủy đăng ký chưa thanh toán theo luồng được phép, hệ thống có thể giải phóng lượt voucher còn đang giữ. Mã đã sử dụng thành công không tự được trả lượt theo cách này. Kiểm tra kết quả hủy và trạng thái đăng ký; nếu chưa rõ lượt đã được giải phóng, nhờ bộ phận phụ trách kiểm tra, không gửi đăng ký lặp hoặc tự cho rằng mã chắc chắn dùng lại được.

## Câu hỏi khách hàng thường gặp
- Giải phóng voucher của vé chờ bị hủy là gì? Nếu mã ưu đãi đang được giữ cho vé chờ hoặc hóa đơn chưa hoàn tất, luồng hủy hợp lệ sẽ giải phóng lượt đang giữ theo giao dịch. Không có nghĩa lượt đã sử dụng thành công cũng được trả lại. Trợ lý chỉ giải thích, không tự giải phóng mã hoặc xác nhận kết quả thực tế của bạn.
- Dùng mã giảm giá vé tháng như thế nào? Trong Vé tháng, chọn bãi, phương tiện, loại vé và ngày bắt đầu, nhập mã ưu đãi rồi xem kết quả áp dụng trước khi gửi đăng ký. Kiểm tra số tiền cuối cùng do hệ thống trả về; chatbot chưa tự áp mã thay bạn.
- Mã giảm giá hết hạn thì phải làm sao? Kiểm tra thời gian bắt đầu và kết thúc của mã. Mã chỉ được dùng trong thời gian có hiệu lực và khi đang được cho phép sử dụng; không thể tự gia hạn mã bằng cách nhập lại. Nếu nghi ngờ hiển thị sai, nhờ bộ phận phụ trách kiểm tra.
- Đơn vé tháng chưa đủ điều kiện mã giảm giá thì làm sao? Kiểm tra giá trị vé trước giảm và điều kiện tối thiểu của mã. Chọn đúng bãi và loại vé rồi xem lại kết quả áp dụng. Không tự sửa số tiền hoặc giả định mọi mã đều áp dụng cho mọi vé.
- Vì sao mã ưu đãi không dùng được ở bãi khác? Mã có thể áp dụng toàn hệ thống, cho một đối tác hoặc chỉ một bãi. Hãy kiểm tra phạm vi của mã và bãi đang đăng ký. Mã thuộc bãi hoặc đối tác khác có thể không được áp dụng.
- Mã giảm theo phần trăm có giới hạn tiền giảm không? Số tiền giảm phụ thuộc phần trăm, mức giảm tối đa nếu được cấu hình và giá trị vé. Tiền giảm không vượt quá giá trị vé. Hãy xem kết quả áp dụng mã từ hệ thống để biết số tiền chính xác.
- Voucher báo hết lượt sử dụng thì cần làm gì? Mã có thể giới hạn tổng lượt hoặc số lượt của mỗi khách hàng. Lượt đang được giữ cho hóa đơn chờ xử lý cũng được tính khi kiểm tra giới hạn. Xem lại các đăng ký liên quan hoặc nhờ bộ phận phụ trách kiểm tra, không gửi lặp để vượt giới hạn.
- Xem thử mã ưu đãi có giữ lượt sử dụng không? Xem kết quả áp dụng mã là bước kiểm tra, chưa giữ lượt. Lượt được giữ khi đăng ký tạo hóa đơn với mã và hệ thống kiểm tra lại điều kiện. Vì vậy kết quả xem thử không bảo đảm mã vẫn còn lượt lúc bạn gửi đăng ký.
- Hủy vé đang chờ thì lượt mã ưu đãi có được trả lại không? Với lượt mã còn đang được giữ, luồng hủy đăng ký phù hợp có thể giải phóng lượt đó. Mã đã dùng thành công không được coi là lượt đang giữ để tự hoàn lại. Kết quả phải kiểm tra trên đăng ký và giao dịch thực tế.
- Hai người cùng dùng lượt voucher cuối thì sao? Hệ thống kiểm tra lại và giữ lượt khi đăng ký được xử lý. Hai người cùng xem thử không có nghĩa cả hai đã giữ được lượt cuối; một yêu cầu có thể nhận thông báo hết lượt. Kiểm tra kết quả và không gửi đăng ký lặp.
- Có thể dùng nhiều mã giảm giá cho một vé không? Luồng đăng ký hiện nhận một mã ưu đãi cho mỗi yêu cầu và gắn lượt mã với hóa đơn. Chưa có căn cứ để cam kết cộng dồn nhiều mã trên cùng vé. Kiểm tra số tiền cuối cùng trước khi xác nhận đăng ký.

## Điều chatbot được phép trả lời
Giải thích hướng dẫn, điều kiện và giới hạn đã được kiểm chứng. Trả lời bằng từ ngữ dễ hiểu; không đưa tên trạng thái kỹ thuật vào câu trả lời.

## Điều chatbot phải dùng chức năng được cấp quyền
Thông tin cá nhân, giá hiện hành, số tiền hoặc trạng thái cụ thể chỉ được xác nhận bằng nguồn dữ liệu có thẩm quyền và đúng quyền. Nếu chưa có tool phù hợp, hướng dẫn mở module nghiệp vụ hoặc liên hệ bộ phận phụ trách; không giả lập kết quả.

## Điều chatbot phải từ chối
Không tự thực hiện thanh toán, hoàn tiền, đặt/hủy chỗ, áp mã hay sửa hóa đơn. Không yêu cầu mật khẩu, mã xác minh, thông tin thẻ bí mật, mã truy cập đặt chỗ hoặc QR riêng tư. Không hứa chắc số tiền, hiệu lực hoặc thời hạn chưa có nguồn.

## Khi nào chuyển nhân viên
Giao dịch bất thường, tiền đã trừ chưa rõ kết quả, tranh chấp hoặc cần kiểm tra dữ liệu riêng ngoài khả năng tra cứu được cấp. Phân biệt liên hệ bãi xử lý nghiệp vụ với báo lỗi chức năng hệ thống; không tự tạo phiếu thay cho mọi yêu cầu tài chính.

## Lưu ý khi trả lời và đối chiếu kết quả
Xem thử mã chỉ kiểm tra tại thời điểm xem, chưa giữ lượt. Lúc gửi đăng ký, hệ thống kiểm tra lại điều kiện và lượt còn lại. Hủy một đăng ký có thể giải phóng lượt đang giữ theo luồng hợp lệ, không tự khôi phục lượt đã dùng thành công.

<!-- authoring-only:start -->
## Nguồn đối chiếu
Đường dẫn code, quyền, API, UI và bằng chứng kiểm thử được lưu ở hồ sơ traceability đợt 3 ngoài bản tri thức xuất. Không nhúng chi tiết quản trị hoặc bí mật vào nội dung người dùng.

## Nội dung TBD
Chưa nghiệm thu/publish trên corpus DB dev. Cấu hình cổng thanh toán và số tiền hiện hành cần kiểm tra tại môi trường thực tế. Xem báo cáo đợt 3 để biết giới hạn kiểm chứng.

## Lịch sử phiên bản
- 2.2.0, 2026-11-24: rà soát lại với code hiện tại; làm rõ điều kiện và giới hạn trả lời, chuẩn hóa ngôn ngữ; tiếp tục DRAFT. Kiểm thử và giới hạn bằng chứng tại báo cáo ALL_BATCHES_REVIEW_STATUS.vi.md.
- 2.1.0, 2026-11-23: bổ sung giải thích nghiệp vụ theo lỗi truy xuất hồi quy đợt 4, giữ DRAFT.
- 2.0.0: rà soát code ngày 20/11/2026; giữ DRAFT, không thay chính sách nghiệp vụ.
<!-- authoring-only:end -->
