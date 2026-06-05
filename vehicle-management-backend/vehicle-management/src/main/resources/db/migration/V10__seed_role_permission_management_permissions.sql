BEGIN;

INSERT INTO iam.permission_actions (code, name, description)
VALUES
    ('APPROVE', 'Phê duyệt', 'Cho phép phê duyệt dữ liệu hoặc yêu cầu nghiệp vụ.'),
    ('REJECT', 'Từ chối', 'Cho phép từ chối dữ liệu hoặc yêu cầu nghiệp vụ.'),
    ('SUSPEND', 'Tạm ngưng', 'Cho phép tạm ngưng đối tượng nghiệp vụ.'),
    ('ACTIVATE', 'Kích hoạt', 'Cho phép kích hoạt đối tượng nghiệp vụ.'),
    ('INACTIVATE', 'Ngưng hoạt động', 'Cho phép chuyển đối tượng sang trạng thái không hoạt động.'),
    ('ASSIGN', 'Gán', 'Cho phép gán quan hệ hoặc phân công nghiệp vụ.'),
    ('UNASSIGN', 'Thu hồi', 'Cho phép thu hồi quan hệ hoặc phân công nghiệp vụ.'),
    ('CHECK_IN', 'Ghi nhận vào', 'Cho phép ghi nhận check-in.'),
    ('CHECK_OUT', 'Ghi nhận ra', 'Cho phép ghi nhận check-out.'),
    ('CANCEL', 'Hủy', 'Cho phép hủy yêu cầu hoặc giao dịch.'),
    ('RESOLVE', 'Xử lý', 'Cho phép xử lý hoặc giải quyết vụ việc.'),
    ('CLOSE', 'Đóng', 'Cho phép đóng ticket hoặc phiên nghiệp vụ.'),
    ('STATUS_UPDATE', 'Cập nhật trạng thái', 'Cho phép cập nhật trạng thái đối tượng nghiệp vụ.'),
    ('CONFIG_UPDATE', 'Cập nhật cấu hình', 'Cho phép cập nhật cấu hình hệ thống hoặc thiết bị.'),
    ('MARK_DEFAULT', 'Đánh dấu mặc định', 'Cho phép đặt bản ghi làm mặc định.'),
    ('MARK_READ', 'Đánh dấu đã đọc', 'Cho phép đánh dấu bản ghi đã đọc.'),
    ('REFUND', 'Hoàn tiền', 'Cho phép hoàn tiền giao dịch.'),
    ('RESET_PASSWORD', 'Đặt lại mật khẩu', 'Cho phép đặt lại mật khẩu tài khoản.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_scopes (code, name, description)
VALUES
    ('ASSIGNED', 'Được phân công', 'Áp dụng cho dữ liệu được phân công cho tài khoản hiện tại.'),
    ('LOT', 'Theo bãi xe', 'Áp dụng cho dữ liệu thuộc bãi xe hoặc phạm vi vận hành được quản lý.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (permission_code, module_id, action_id, scope_id, name, description)
SELECT v.permission_code, m.module_id, a.action_id, s.scope_id, v.name, v.description
FROM (
         VALUES
             (
                 'ROLE_ASSIGN_PERMISSION_ALL',
                 'ROLE',
                 'ASSIGN',
                 'ALL',
                 'Gán quyền cho vai trò',
                 'Cho phép gán hoặc đồng bộ quyền cho một vai trò trong hệ thống.'
             ),
             (
                 'ROLE_REVOKE_PERMISSION_ALL',
                 'ROLE',
                 'UNASSIGN',
                 'ALL',
                 'Thu hồi quyền khỏi vai trò',
                 'Cho phép thu hồi một quyền khỏi vai trò trong hệ thống.'
             )
     ) AS v(permission_code, module_code, action_code, scope_code, name, description)
         JOIN iam.permission_modules m ON m.code = v.module_code
         JOIN iam.permission_actions a ON a.code = v.action_code
         JOIN iam.permission_scopes s ON s.code = v.scope_code
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (role_id, permission_id, is_active, is_system)
SELECT r.role_id, p.permission_id, TRUE, TRUE
FROM iam.roles r
         JOIN iam.permissions p ON p.permission_code IN (
             'ROLE_ASSIGN_PERMISSION_ALL',
             'ROLE_REVOKE_PERMISSION_ALL'
    )
WHERE r.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id)
    DO UPDATE SET is_active = TRUE, is_system = TRUE, updated_at = now()
WHERE iam.role_permissions.is_active = FALSE OR iam.role_permissions.is_system = FALSE;

COMMIT;
