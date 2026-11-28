---
title: Đăng ký, đăng nhập và hồ sơ
slug: authentication-and-profile
documentVersion: 2.2.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Code FE/BE, domain policy, access guard, Flyway và test đợt 1
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-11-24
---

## Mục đích
Hướng dẫn đăng ký, đăng nhập, xác minh email, quên mật khẩu và hoàn thiện hồ sơ bằng chức năng hiện có; không tra cứu trạng thái cá nhân bằng tài liệu tĩnh.

## Đối tượng
Người chưa đăng nhập, khách hàng và người dùng cần hướng dẫn tài khoản. Phê duyệt tài khoản nội bộ không thuộc hướng dẫn công khai này.

## Phạm vi
Trang Đăng ký, Đăng nhập, Quên mật khẩu và Hồ sơ cá nhân của khách hàng hoặc nhân sự. Không cung cấp bí mật đăng nhập, cấu hình xác thực hay thao tác quản trị tài khoản.

## Thuật ngữ
Tài khoản dùng để đăng nhập; hồ sơ lưu thông tin cá nhân. Trạng thái tài khoản, tình trạng hoạt động khách hàng và kết quả duyệt hồ sơ là các thông tin riêng. Hoàn thiện hồ sơ là cập nhật và gửi thông tin theo giao diện.

## Điều kiện trước
Có email hợp lệ và truy cập được hộp thư. Chỉ nhập mật khẩu vào trang xác thực chính thức. Đăng nhập bằng Google cần email được Google xác minh; hệ thống không tự nối với tài khoản dùng mật khẩu có cùng email.

## Luồng chính
1. Mở Đăng ký, nhập username, email, mật khẩu và họ tên hợp lệ trên form; không gửi mật khẩu vào chat.
2. Làm theo email xác minh. Nếu chưa nhận, kiểm tra spam và dùng chức năng gửi lại nếu được phép.
3. Mở Đăng nhập để đăng nhập. Đăng nhập bằng Google thực hiện xác thực với Google, sau đó hệ thống kiểm tra và liên kết danh tính theo quy trình được phép.
4. Sau đăng nhập, hoàn thiện hồ sơ theo trang hiện tại. Chờ duyệt không có nghĩa tài khoản đã bị khóa.
5. Quên mật khẩu: mở Quên mật khẩu, nhập email trên form và làm theo email và trang xác thực chính thức; hệ thống không gửi mật khẩu cũ cho người dùng.
6. Đọc thông báo cụ thể trên giao diện trước khi thử lại; không lặp gửi email liên tục.

## Luồng thay thế
- Route OTP/khôi phục cũ chuyển về `/forgot-password`; không hướng dẫn nhập OTP trong chat.
- Tài khoản chờ duyệt có thể dùng các chức năng hỗ trợ được hệ thống cho phép; không mặc định mọi hỗ trợ đều yêu cầu hồ sơ được duyệt.
- Câu “làm sao lấy lại mật khẩu” là hướng dẫn hợp lệ, không phải yêu cầu tiết lộ mật khẩu.

## Luồng lỗi
- Đăng nhập không được: hỏi đang đăng nhập bằng tài khoản/mật khẩu hay Google, đang ở bước nào và thấy thông báo lỗi gì. Xác minh email và hoàn thiện hồ sơ là các bước kiểm tra tài khoản, không phải cách đăng nhập. Không tự chuyển sang hướng dẫn quên mật khẩu khi chưa biết người dùng có quên mật khẩu hay không.
- Trùng email/username: dùng chức năng phù hợp hoặc hỗ trợ; không tự liên kết tài khoản Google trùng email.
- Không nhận email: kiểm tra địa chỉ đã nhập, spam và gửi lại theo giới hạn; không hứa thời gian email chưa được kiểm chứng.
- Tài khoản bị khóa hoặc ngừng hoạt động: không khuyên vượt quyền hoặc tạo tài khoản để né hạn chế; liên hệ hỗ trợ hệ thống.
- Câu “vẫn lỗi” sau hướng dẫn đăng nhập dùng đúng chủ đề gần nhất; nếu chưa có chủ đề thì hỏi chức năng và bước gặp lỗi.

## Trạng thái và chuyển trạng thái
Tài khoản có thể đang chờ, đang hoạt động, bị khóa hoặc ngừng hoạt động. Khách hàng còn có approval/operational status riêng; trạng thái thực của người dùng chỉ xác định qua nguồn đã được phân quyền. Tài liệu này không tự thay đổi trạng thái.

## Câu hỏi khách hàng thường gặp
- Không đăng nhập được thì kiểm tra gì trước? Hỏi người dùng đang đăng nhập bằng Google hay tài khoản và mật khẩu, màn hình hiện thông báo gì và lỗi ở bước nào. Không mặc định cần Quên mật khẩu; chỉ hướng dẫn khôi phục khi phương thức và lỗi phù hợp. Nếu người dùng đã nêu phương thức thì không hỏi lại.
- “dang ky tai khoan nhu the nao” → các bước đăng ký và xác minh email.
- “quen mat khau phai lam sao” → dùng Quên mật khẩu và email chính thức.
- “dang nhap ko dc” → hướng dẫn theo lỗi đăng nhập, hỏi bước còn thiếu.
- “hồ sơ của tôi duyệt chưa?” → dữ liệu cá nhân; không trả trạng thái bằng tài liệu hướng dẫn.
- “lỗi” → hỏi chức năng/bước, không tự suy ra đăng nhập.
- Làm sao đăng ký CoParking? Mở Đăng ký, điền tài khoản, email, mật khẩu và họ tên; làm theo email xác minh rồi đăng nhập.
- Đặt lại mật khẩu thế nào? Dùng Quên mật khẩu, nhập email tại trang chính thức và làm theo thư nhận được. Không chia sẻ mật khẩu, mã xác thực hoặc đường dẫn khôi phục.
- Đăng nhập bằng Google không thành công cần làm gì? Kiểm tra thông báo và email Google đã xác minh; nếu vẫn lỗi, báo bước gặp lỗi cho nhân viên, không tự liên kết tài khoản trùng email.
- Xem trạng thái hồ sơ trên giao diện ở đâu? Mở hồ sơ cá nhân, đọc thông báo duyệt; chat chưa có chức năng tra cứu hồ sơ thực tế của bạn.
- Hồ sơ cần sửa thì làm sao? Đọc lý do yêu cầu bổ sung, cập nhật thông tin trên hồ sơ cá nhân và gửi lại theo nút đang được phép.
- Sau hướng dẫn quên mật khẩu, bước tiếp theo là gì, cái đó ở đâu? Mở Quên mật khẩu trên trang đăng nhập. Nếu đã làm rồi vẫn chưa được, hỏi người dùng lỗi ở bước nhập email, nhận thư hay đặt mật khẩu mới; không tự đoán đã hoàn tất.

## Điều chatbot được phép trả lời
Các bước UI, ý nghĩa trạng thái tổng quát, phân biệt lỗi và cách tìm hỗ trợ. Trả lời ngắn, ưu tiên bước người dùng đang mắc; chỉ viện dẫn nguồn thật.

## Điều chatbot phải dùng chức năng được cấp quyền
Tình trạng tài khoản và hồ sơ thực tế phải kiểm tra bằng chức năng có quyền. Chat hiện chưa có chức năng đọc trạng thái hồ sơ; không giả vờ đã kiểm tra. Hướng dẫn người dùng mở Hồ sơ hoặc gửi hỗ trợ nếu cần người xử lý.

## Điều chatbot phải từ chối
Tiết lộ mật khẩu/OTP/token, đọc hồ sơ người khác, tự approve/unlock, bỏ qua xác thực hoặc khẳng định email người khác tồn tại.

## Khi nào chuyển nhân viên
Người dùng yêu cầu người thật, lỗi xung đột danh tính đăng nhập, tài khoản khóa, email không đến sau các bước hợp lệ hoặc trạng thái hồ sơ bất thường; dùng kênh hỗ trợ hệ thống, không chat đối tác để gửi phiếu.

## Lưu ý khi trả lời và đối chiếu kết quả
Hỏi cách đăng nhập khác với hỏi tài khoản của bạn đang bị gì. Nếu đã nêu phương thức đăng nhập và thông báo lỗi, không hỏi lại các thông tin đó. Câu “vẫn lỗi” chỉ tiếp nối chủ đề đã rõ; khi chưa rõ, cần hỏi bước đang mắc. Không kết luận tài khoản đã bị khóa hoặc hồ sơ đã được duyệt từ tài liệu hướng dẫn.

<!-- authoring-only:start -->
## Nguồn đối chiếu
- Backend: `PublicAuthUseCaseImpl`, `PublicAuthPolicy`, `SocialAccountBootstrapUseCaseImpl`, `AccountProfileUseCaseImpl`.
- FE: `src/app/routes.tsx`, `LoginPage`, `ProfilePage`, `InternalProfilePage`.
- Bằng chứng kiểm thử và truy vết: `../evaluation/batch-01-traceability.md`, `../evaluation/batch-01-questions.jsonl`.
- Nguồn chuẩn là file này; bản backend `docs/ai-knowledge/phase-0/knowledge/` được sinh bằng script sync và kiểm checksum.

## Nội dung TBD
SLA email, xử lý tranh chấp identity và mở khóa do người có thẩm quyền quyết định. Không biến quyết định mục tiêu chưa triển khai thành lời hứa. DRAFT chưa phải phê duyệt publish.

## Lịch sử phiên bản
- 2.2.0, 2026-11-24: rà soát lại với code hiện tại; làm rõ điều kiện và giới hạn trả lời, chuẩn hóa ngôn ngữ; tiếp tục DRAFT. Kiểm thử và giới hạn bằng chứng tại báo cáo ALL_BATCHES_REVIEW_STATUS.vi.md.
- 2.1.0, 2026-11-23: bổ sung phân biệt điều kiện và hướng xử lý theo lỗi hồi quy đợt 4; giữ DRAFT.
2.0.0 — rà soát đợt 1 ngày 19/11/2026; tách hướng dẫn/cá nhân, cập nhật Keycloak và hỗ trợ trước duyệt. Giữ trạng thái DRAFT.
<!-- authoring-only:end -->
