-- Canonical support categories. Support-ticket transactions are not seeded here.

INSERT INTO operations.support_ticket_categories (
    code, name, description, priority, status
)
VALUES
    ('LOST_CARD', 'Mất thẻ xe', 'Khách hàng mất thẻ xe hoặc không xuất trình được thẻ khi ra bãi.', 'HIGH', 'ACTIVE'),
    ('WRONG_FEE', 'Khiếu nại phí gửi xe', 'Khách hàng phản ánh bị tính sai phí gửi xe.', 'HIGH', 'ACTIVE'),
    ('VEHICLE_DAMAGE', 'Xe hư hỏng/trầy xước', 'Khách hàng phản ánh xe bị trầy xước, hư hỏng hoặc mất tài sản.', 'URGENT', 'ACTIVE'),
    ('CARD_NOT_WORKING', 'Thẻ không hoạt động', 'Thẻ gửi xe không quét được hoặc không sử dụng được.', 'NORMAL', 'ACTIVE'),
    ('SUBSCRIPTION_PROBLEM', 'Vấn đề vé đăng ký', 'Vé tháng, quý, năm hoặc miễn phí không hoạt động hoặc cần hỗ trợ.', 'NORMAL', 'ACTIVE'),
    ('PAYMENT_PROBLEM', 'Vấn đề thanh toán', 'Thanh toán lỗi, đã thanh toán nhưng chưa ghi nhận hoặc cần kiểm tra giao dịch.', 'HIGH', 'ACTIVE'),
    ('PARKING_HISTORY_REQUEST', 'Yêu cầu tra cứu lịch sử gửi xe', 'Khách hàng cần tra cứu lịch sử gửi xe.', 'LOW', 'ACTIVE'),
    ('PROFILE_OR_VEHICLE_UPDATE', 'Cập nhật hồ sơ/xe', 'Khách hàng cần hỗ trợ cập nhật thông tin cá nhân hoặc phương tiện.', 'LOW', 'ACTIVE'),
    ('STAFF_COMPLAINT', 'Khiếu nại nhân viên', 'Khách hàng phản ánh thái độ hoặc cách xử lý của nhân viên.', 'HIGH', 'ACTIVE'),
    ('OTHER', 'Vấn đề khác', 'Yêu cầu hỗ trợ khác chưa thuộc nhóm cố định.', 'NORMAL', 'ACTIVE')
ON CONFLICT (code) WHERE status = 'ACTIVE' DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    priority = EXCLUDED.priority,
    updated_at = now();
