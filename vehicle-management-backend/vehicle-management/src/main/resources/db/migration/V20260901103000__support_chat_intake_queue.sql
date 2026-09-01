-- Phase 2 support intake. Feature availability is permission-first; role rows below are
-- only default IAM configuration and are never used as an authorization condition in code.

INSERT INTO iam.permission_actions (
    action_id, code, name, description, created_at, created_by, updated_at, updated_by
) VALUES
    ('7f4f11d7-2b52-4c20-bf11-0e4200000201', 'ACCESS_WIDGET', 'Truy cập widget', 'Cho phép truy cập widget hỗ trợ.', now(), NULL, NULL, NULL),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000202', 'CLAIM', 'Nhận xử lý', 'Cho phép nhận phiếu hỗ trợ chưa được phân công.', now(), NULL, NULL, NULL)
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (
    permission_id, permission_code, name, description, created_at, created_by, updated_at, updated_by,
    module_id, action_id, scope_id
) VALUES
    ('7f4f11d7-2b52-4c20-bf11-0e4200000201', 'SUPPORT_WIDGET_ACCESS_OWN', 'Truy cập widget hỗ trợ',
     'Cho phép tài khoản đã xác thực mở widget gửi yêu cầu hỗ trợ của chính mình.', now(), NULL, NULL, NULL,
     '00000000-0000-0000-0000-000000001029', '7f4f11d7-2b52-4c20-bf11-0e4200000201', '00000000-0000-0000-0000-000000003002'),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000202', 'SUPPORT_TICKET_CLAIM_OWN', 'Nhận phiếu chưa phân công',
     'Cho phép nhân viên đủ năng lực nhận một phiếu hỗ trợ đang mở chưa có người phụ trách.', now(), NULL, NULL, NULL,
     '00000000-0000-0000-0000-000000001029', '7f4f11d7-2b52-4c20-bf11-0e4200000202', '00000000-0000-0000-0000-000000003002')
ON CONFLICT (permission_code) DO NOTHING;

-- Default configuration only. Any future role can receive these permissions through IAM.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT seeded.id, seeded.role_id, permission.permission_id, now(), NULL, NULL, NULL, true, true
FROM (
    VALUES
        ('7f4f11d7-2b52-4c20-bf11-0e4200000211'::uuid, '00000000-0000-0000-0000-000000000003'::uuid, 'SUPPORT_WIDGET_ACCESS_OWN'),
        ('7f4f11d7-2b52-4c20-bf11-0e4200000212'::uuid, '00000000-0000-0000-0000-000000000002'::uuid, 'SUPPORT_TICKET_CLAIM_OWN')
) AS seeded(id, role_id, permission_code)
JOIN iam.permissions permission ON permission.permission_code = seeded.permission_code
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();

-- A ticket has one active intake thread. Reopen or reassignment keeps the same history.
CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_conversations_active_support_ticket
    ON operations.chat_conversations (support_ticket_id)
    WHERE support_ticket_id IS NOT NULL
      AND conversation_type = 'SUPPORT_TICKET'
      AND status = 'ACTIVE';
