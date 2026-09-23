<!--
title: Địa điểm, giờ hoạt động, SLA và kênh liên hệ
slug: locations-hours-sla-contact
documentVersion: 1.1.0
status: DRAFT
language: vi
accessScope: PUBLIC
ownerPermission: AI_KNOWLEDGE_MANAGE_ALL
sourceOfTruth: Sổ quyết định nghiệp vụ Phase 0, TBD-001 đến TBD-003
effectiveFrom: 2026-09-14
reviewAt: 2026-12-13
lastVerifiedAt: 2026-09-14
-->

# Địa điểm, giờ hoạt động, SLA và kênh liên hệ

## Mục đích
Định nghĩa contact, first-human-response target và public contact workflow đã được phê duyệt; runtime/FE phải được đồng bộ trước khi publish vào RAG production.

## Đối tượng
Khách hàng và người quản trị nội dung.

## Phạm vi
Địa điểm bãi, giờ hoạt động, hotline/email/kênh hỗ trợ và SLA.

## Thuật ngữ
SLA là cam kết mức dịch vụ đã được owner phê duyệt, không phải thời gian ước đoán của model.

## Điều kiện trước
Mỗi giá trị phải có nguồn chính thức, owner, effectiveFrom, reviewAt và version.

## Luồng chính
1. Contact cấp công ty: hotline `0912345678`, email `support@coparking.vn`, trụ sở `10/5 Đường Tỉnh Lộ 19, Phường An Phú Đông, Thành phố Hồ Chí Minh`; tiếp nhận 24/7.
2. First-human-response target chạy 24/7 theo `Asia/Ho_Chi_Minh`: URGENT 30 phút, HIGH 2 giờ, NORMAL 8 giờ, LOW 48 giờ. Auto-ack/chatbot không tính là phản hồi người thật.
3. Public contact dùng API riêng, queue `PUBLIC_CONTACT`, thông báo tới email support, CAPTCHA được xác minh server-side, consent bắt buộc, không attachment và rate limit theo decision register.
4. Knowledge Steward đồng bộ nguồn chính thức; approver đối chiếu và publish.
5. Chatbot chỉ retrieval phiên bản active, chưa quá reviewAt.

## Luồng thay thế
Nếu chưa có nguồn hoặc tài liệu stale, chatbot nói chưa thể xác nhận và đề nghị tạo phiếu/kênh đã xác minh.

## Luồng lỗi
FE static khác backend/config, hotline không hoạt động, địa chỉ cũ hoặc SLA chưa được phê duyệt.

## Trạng thái và chuyển trạng thái
DRAFT → PENDING_APPROVAL → PUBLISHED → SUPERSEDED/ARCHIVED theo governance target.

## Câu hỏi khách hàng thường gặp
- Bãi xe ở đâu và mở cửa lúc nào?
- Hotline/email hỗ trợ là gì?
- Bao lâu nhân viên phản hồi phiếu?

## Điều chatbot được phép trả lời
Chỉ giá trị từ document PUBLISHED còn hiệu lực kèm citation.

## Điều chatbot phải dùng business tool
Tình trạng bãi theo thời gian thực nếu tương lai có tool; không dùng tài liệu tĩnh cho trạng thái mở/đóng tức thời.

## Điều chatbot phải từ chối
Bịa địa chỉ/giờ/hotline/email/SLA; dùng dữ liệu mock hoặc static chưa phê duyệt làm cam kết.

## Khi nào chuyển nhân viên
Tài liệu thiếu/stale/mâu thuẫn hoặc người dùng cần hỗ trợ khẩn cấp mà chưa có kênh chính thức.

## Nguồn đối chiếu
- `open-business-questions.md`, TBD-001 đến TBD-003 là nguồn quyết định nghiệp vụ.
- FE `ContactPage.tsx`, `PortalShared.tsx` chỉ là implementation cần đồng bộ, không phải source chính thức.
- Parking lot table có địa chỉ vận hành nhưng chưa có public publication governance theo từng bãi.

## Nội dung TBD
Không còn TBD thuộc TBD-001 đến TBD-003. Contact theo từng parking lot và retention của public contact request cần decision riêng nếu được đưa vào phạm vi.

## Lịch sử phiên bản
- 1.1.0: ghi nhận contact, SLA và contact-form policy đã APPROVED ngày 2026-09-14; implementation pending.
- 1.0.0: placeholder governance; không chứa giá trị liên hệ chưa xác nhận, 2026-09-14.
