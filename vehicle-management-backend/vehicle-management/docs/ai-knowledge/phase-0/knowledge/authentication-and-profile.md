<!--
title: Đăng ký, đăng nhập và hồ sơ
slug: authentication-and-profile
documentVersion: 1.0.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Backend IAM, Keycloak flow và Frontend auth/profile
effectiveFrom: null
reviewAt: null
lastVerifiedAt: 2026-09-14
-->

# Đăng ký, đăng nhập và hồ sơ

## Mục đích
Hướng dẫn an toàn về đăng ký, đăng nhập thường/Google, quên mật khẩu và hồ sơ.

## Đối tượng
Người chưa đăng nhập và khách hàng.

## Phạm vi
Không bao gồm duyệt nội bộ chi tiết, cấu hình Keycloak hay xử lý token.

## Thuật ngữ
- Account: tài khoản xác thực trong `iam.accounts`.
- Profile: hồ sơ dùng chung trong `people.user_profiles`.
- Onboarding: hoàn thiện hồ sơ và gửi duyệt.

## Điều kiện trước
Người dùng có email hợp lệ; đăng nhập Google yêu cầu email đã được Google xác minh.

## Luồng chính
1. Đăng ký local tại `/register`, nhập username, email, mật khẩu và họ tên hợp lệ.
2. Hệ thống tạo danh tính Keycloak, account/profile và gửi email xác thực.
3. Sau đăng nhập, người dùng hoàn thiện onboarding; customer ban đầu chờ phê duyệt.
4. Đăng nhập Google dùng OAuth authorization code + PKCE; Backend social-bootstrap ánh xạ identity đã xác thực.
5. Quên mật khẩu tại `/forgot-password`; việc đặt mật khẩu mới diễn ra qua email/Keycloak.

## Luồng thay thế
Có thể gửi lại email xác thực khi account còn PENDING và email chưa xác minh, trong giới hạn rate-limit.

## Luồng lỗi
- Username/email đã tồn tại: chọn thông tin khác.
- Google state không khớp: khởi động lại login; không gửi token cho hỗ trợ.
- Email local đã tồn tại: hệ thống hiện không tự liên kết với Google.
- Email reset không tới: kiểm tra spam rồi tạo phiếu; hệ thống không xác nhận công khai email có tồn tại.

## Trạng thái và chuyển trạng thái
Account: `PENDING`, `ACTIVE`, `LOCKED`, `DISABLED`. Customer có `approval_status` riêng: `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED` và operational status ACTIVE/INACTIVE.

## Câu hỏi khách hàng thường gặp
- Đăng ký và xác thực email thế nào?
- Tại sao Google login bị lỗi?
- Quên mật khẩu xử lý ở đâu?
- Hồ sơ của tôi đã được duyệt chưa?

## Điều chatbot được phép trả lời
Hướng dẫn UI, yêu cầu dữ liệu đầu vào, ý nghĩa trạng thái và bước khắc phục không nhạy cảm.

## Điều chatbot phải dùng business tool
Trạng thái account/onboarding/profile của người đang đăng nhập.

## Điều chatbot phải từ chối
Nhận hoặc hiển thị password, OTP, access/refresh token; tra cứu account người khác; tự approve/unlock.

## Khi nào chuyển nhân viên
Google identity conflict, account bị khóa/disabled, email không tới kéo dài hoặc người dùng tranh chấp phê duyệt.

## Nguồn đối chiếu
- `PublicAuthController`, `PublicAuthUseCaseImpl`, `PublicAuthPolicy`.
- `SocialAccountBootstrapUseCaseImpl`, `SocialAccountPolicy`.
- `AccountProfileController`, `AccountProfileUseCaseImpl`.
- `authApi.ts`, `LoginPage.tsx`, `ProfilePage.tsx`.
- Flyway baseline và `V20260821234837__create_account_identities.sql`.

## Nội dung TBD
SLA email, chính sách account lock/unlock, retention identity và quy trình support Google chính thức.

## Lịch sử phiên bản
- 1.0.0: baseline từ mã nguồn ngày 2026-09-14.

