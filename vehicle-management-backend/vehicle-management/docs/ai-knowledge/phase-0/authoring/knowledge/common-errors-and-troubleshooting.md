---
title: Lỗi thường gặp khi dùng CoParking
slug: common-errors-and-troubleshooting
documentVersion: 2.1.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: AssistantOrchestrator, GuestPublicAssistantService, AssistantBusinessRequestPolicy, LostCardReportUseCaseImpl; frontend auth/history/support và xử lý lỗi
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-24
---

## Mục đích và nguyên tắc
Giúp người dùng xử lý lỗi theo đúng chức năng và bước thực hiện. Không chẩn đoán một nguyên nhân duy nhất khi chưa có bằng chứng, không mặc định lỗi nào cũng cần làm mới, đăng nhập lại hoặc tạo phiếu.

## Thông tin nên hỏi
Hỏi ngắn gọn chức năng, bước đang làm, thời điểm và thông báo trên màn hình. Khi cần đăng nhập, hỏi phương thức Google hay tài khoản/mật khẩu nếu chưa biết. Nếu người dùng đã nêu phương thức hoặc bước thì không hỏi lại điều đó. Không yêu cầu mật khẩu, OTP, cookie, token, API key hoặc ảnh giấy tờ đầy đủ.

## Đăng nhập và quyền dùng chức năng khác nhau
Đăng nhập rồi nhưng vẫn bị hệ thống từ chối quyền có thể do tài khoản không có quyền dùng chức năng hoặc không thuộc bãi đang chọn. Kiểm tra đúng tài khoản, chức năng và bãi được phân công; nếu cần quyền cho công việc, nhờ quản lý kiểm tra. Đăng nhập lại không tự cấp thêm quyền và không mượn tài khoản của người khác.

## Luồng xử lý an toàn
1. Phân biệt đang hỏi cách làm, đang báo lỗi hay cần kiểm tra dữ liệu riêng. Chỉ đọc dữ liệu chính chủ qua công cụ thực sự có quyền.
2. Với đăng nhập chung chưa rõ lỗi: hỏi phương thức và thông báo trước; không bắt đầu bằng Quên mật khẩu. Chỉ dùng khôi phục mật khẩu khi phù hợp.
3. Với lỗi kết nối: kiểm tra mạng và kết quả thao tác cũ. Thử lại xem thông tin khác với lặp thanh toán/tạo yêu cầu.
4. Với thiếu quyền: kiểm tra đúng tài khoản và phạm vi bãi, nhờ quản lý phân quyền khi cần. Làm mới hay đăng nhập lại không tự cấp quyền.
5. Với dữ liệu chưa thấy: kiểm tra bộ lọc và phạm vi của đúng màn hình. Lịch sử theo ngày phải chọn Khoảng ngày; Xóa lọc không thay thế việc chọn ngày.
6. Với mất thẻ: tách điều kiện lập phiếu và điều kiện hoàn tất. Lập phiếu cần kiểm tra quyền/bãi, đúng thẻ/lượt gửi, xác minh và không có phiếu đang mở trùng. Hoàn tất cần thanh toán ghi nhận và thẻ thay thế phù hợp nếu có.
7. Với đã trừ tiền: không trả lại ngay. Đối chiếu hóa đơn và giao dịch, nhờ bộ phận phụ trách kiểm tra. Chat chưa có công cụ xem hóa đơn hay hoàn tiền.
8. Với trợ lý đang xử lý/thử lại: chờ kết quả, tránh gửi trùng. Khi đã dừng và có Thử lại thì dùng một lần phù hợp; kiểm tra thao tác cũ trước khi lặp nghiệp vụ.

## Chuyển hỗ trợ và giới hạn
Sự cố tại cổng cần nhân viên tại bãi; thiếu quyền cần người quản lý; lỗi chức năng tái diễn dùng hỗ trợ hệ thống đúng kênh; sai lệch giao dịch cần đối soát. Không tự tạo phiếu, chuyển tiền, hoàn tiền hoặc gửi thông báo. Dữ liệu thẻ, hóa đơn và tiến trình xử lý của trợ lý không được giả vờ tra cứu khi chưa có công cụ đó. Không hướng dẫn tắt bảo mật, mượn tài khoản hoặc vượt quyền.

## Tìm lịch sử gửi xe trong một khoảng ngày
Để tìm lịch sử gửi xe trong một khoảng ngày, mở Lịch sử gửi xe, chọn Khoảng ngày, nhập ngày bắt đầu và ngày kết thúc. Ngày kết thúc không được trước ngày bắt đầu. Kiểm tra phương tiện và trạng thái đang lọc; Xóa lọc chỉ bỏ bộ lọc cũ không phù hợp, không thay việc chọn khoảng ngày. Cần đăng nhập đúng tài khoản để xem dữ liệu của mình; chat chưa lọc theo ngày thay bạn.

## Tiền đã trừ nhưng trạng thái chưa cập nhật
Nếu tiền đã trừ mà vẫn chưa cập nhật trạng thái thanh toán, không thanh toán lại ngay. Xem kết quả giao dịch và hóa đơn trong chức năng vừa dùng, giữ mã giao dịch cần thiết rồi nhờ bộ phận phụ trách đối soát. Không tự kết luận hóa đơn đã thanh toán chỉ từ trang trả về hoặc số dư bị trừ. Trợ lý chưa tra trực tiếp hóa đơn hay hoàn tiền trong chat.

## Câu hỏi khách hàng thường gặp
- Tôi không đăng nhập được thì phải làm sao? Bạn đang đăng nhập bằng Google hay bằng tài khoản và mật khẩu? Màn hình hiện thông báo gì và lỗi ở bước nào? Chỉ hướng dẫn Quên mật khẩu khi phù hợp với phương thức và lỗi; không tự cho rằng bạn quên mật khẩu.
- Đăng nhập Google chỉ lỗi ở cửa sổ thường thì làm sao? So sánh thông báo lỗi, thử tắt tiện ích liên quan hoặc xóa dữ liệu trang đăng nhập khi phù hợp, rồi đăng nhập lại. Xóa dữ liệu trang có thể làm bạn đăng xuất. Việc vào được ở cửa sổ ẩn danh chưa đủ để kết luận hệ thống CoParking gặp lỗi; không gửi cookie hoặc token để nhờ kiểm tra.
- Lỗi kết nối Failed to fetch là gì? Đây là thông báo không kết nối được, chưa đủ để xác định nguyên nhân. Kiểm tra mạng và thử lại xem thông tin khi phù hợp; nếu đang gửi thanh toán hoặc tạo yêu cầu, kiểm tra kết quả trước khi gửi lại để tránh trùng. Ghi màn hình, thời điểm và thông báo, không gửi bí mật đăng nhập.
- Báo không có quyền dùng chức năng thì làm sao? Kiểm tra bạn đang dùng đúng tài khoản, chức năng và bãi được phân công. Nếu công việc cần quyền đó, nhờ người quản lý kiểm tra phân quyền; đăng nhập lại không tự cấp thêm quyền. Không mượn tài khoản hoặc tìm cách bỏ qua kiểm tra.
- Không thấy lượt gửi xe theo ngày thì làm sao? Mở Lịch sử gửi xe, chọn Khoảng ngày với ngày bắt đầu và ngày kết thúc phù hợp, ngày kết thúc không trước ngày bắt đầu, rồi kiểm tra phương tiện và trạng thái đang lọc. Xóa lọc chỉ để bỏ bộ lọc cũ không phù hợp, không thay thế việc chọn ngày cần tìm. Nếu vẫn thiếu, nhờ nhân viên đối chiếu; chat chưa tự lọc lịch sử theo ngày.
- Điều kiện lập phiếu báo mất thẻ là gì? Nhờ nhân viên bãi kiểm tra quyền và phạm vi bãi, đúng thẻ và lượt gửi, thông tin xác minh; chỉ lập khi không có phiếu đang mở trùng cho cùng thẻ hoặc lượt gửi. Không lập nhiều phiếu khi chưa rõ kết quả. Phiếu hỗ trợ báo lỗi phần mềm không thay thế phiếu báo mất thẻ. Thanh toán và thẻ thay thế được kiểm tra ở bước hoàn tất, không phải điều kiện chung để lập phiếu lần đầu.
- Vì sao chưa hoàn tất phiếu báo mất thẻ được? Nhờ nhân viên kiểm tra thanh toán đã được ghi nhận đầy đủ và điều kiện hoàn tất của phiếu. Nếu cần thay thẻ, thẻ thay thế phải còn sẵn sàng, đúng loại và cùng bãi. Không tự dùng lại thẻ đã báo mất hoặc trả tiền lại khi chưa đối chiếu giao dịch.
- Đã trả tiền mà phiếu mất thẻ vẫn chưa xong thì làm sao? Nhờ nhân viên tại bãi đối chiếu xem thanh toán đã được ghi nhận đầy đủ chưa. Nếu cần thay thẻ, thẻ thay thế phải sẵn sàng, cùng bãi và đúng loại trước khi nhân viên hoàn tất phiếu. Không trả tiền lại khi chưa đối chiếu; trợ lý không tự hoàn tất phiếu.
- Chatbot đang thử lại thì cần làm gì? Khi trợ lý đang xử lý hoặc thử lại, hãy chờ kết quả thay vì gửi cùng yêu cầu nhiều lần. Nếu đã báo dừng hoặc thất bại và có nút Thử lại, có thể thử lại một lần; trước khi lặp thao tác nghiệp vụ, kiểm tra kết quả cũ. Trợ lý nhận tin chưa chứng minh yêu cầu đã được thực hiện; hãy kiểm tra kết quả trên chức năng tương ứng.
- Đã trừ tiền nhưng trạng thái thanh toán chưa cập nhật thì làm sao? Không thanh toán lại ngay. Xem kết quả giao dịch và hóa đơn trong chức năng đã dùng, giữ mã giao dịch cần thiết rồi liên hệ bộ phận phụ trách hoặc nhân viên bãi để đối soát. Trợ lý chưa tra trực tiếp hóa đơn hoặc hoàn tiền trong chat; trang trả về từ cổng thanh toán chưa tự chứng minh đã ghi nhận thành công.

## Lưu ý khi trả lời và đối chiếu kết quả
Thử lại việc xem thông tin khác với gửi lại thanh toán hoặc tạo yêu cầu. Với thao tác có thể ghi dữ liệu, kiểm tra kết quả lần trước trước khi lặp lại. Câu “đã làm rồi vẫn lỗi” cần tiếp tục ở bước đang mắc, không tự quay lại hướng dẫn ban đầu hay tự thực hiện một yêu cầu cũ.

<!-- authoring-only:start -->

## Nguồn đối chiếu
Xem [ma trận truy vết đợt 4](../evaluation/batch-04-traceability.md). Nguồn chính: AssistantOrchestrator, GuestPublicAssistantService, AssistantBusinessRequestPolicy, LostCardReportUseCaseImpl; frontend auth/history/support và xử lý lỗi. Quyết định nghiệp vụ không tự chứng minh chức năng runtime đã có hoặc được bật.

## Nội dung TBD
Xem khoảng trống và giới hạn bằng chứng trong báo cáo đợt 4; không dùng giá trị hoặc trạng thái chưa xác minh làm cam kết. Nội dung vẫn DRAFT, chưa được duyệt hoặc publish.

## Lịch sử phiên bản
- 2.1.0, 2026-11-24: rà soát lại với code hiện tại; làm rõ điều kiện và giới hạn trả lời, chuẩn hóa ngôn ngữ; tiếp tục DRAFT. Kiểm thử và giới hạn bằng chứng tại báo cáo ALL_BATCHES_REVIEW_STATUS.vi.md.
- 2.0.0, 2026-11-23: rà soát đợt 4 theo code, UI và sổ quyết định; tách hướng dẫn khách khỏi thao tác nội bộ, bổ sung FAQ và giới hạn dữ liệu hiện tại. Chưa publish.

<!-- authoring-only:end -->
