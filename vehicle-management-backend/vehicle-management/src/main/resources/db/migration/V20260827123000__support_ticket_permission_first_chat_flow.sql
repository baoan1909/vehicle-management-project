-- Permission-first support workflow. Roles remain seed configurations only.
-- The application evaluates these permissions and the ticket/chat relationship; it does not
-- use a role code to authorize operational behaviour.

-- Each permission needs a distinct action. iam.permissions enforces one permission per
-- (module, action, scope), so reusing CREATE/READ/PROCESS would violate that constraint.
INSERT INTO iam.permission_actions (
    action_id, code, name, description, created_at, created_by, updated_at, updated_by
) VALUES
    ('7f4f11d7-2b52-4c20-bf11-0e4200000101', 'RESPOND', 'Phản hồi', 'Cho phép phản hồi yêu cầu hỗ trợ.', now(), NULL, NULL, NULL),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000102', 'CREATE_FROM_CHAT', 'Tạo từ hội thoại', 'Cho phép tạo phiếu hỗ trợ từ hội thoại riêng.', now(), NULL, NULL, NULL),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000103', 'CREATE_CUSTOMER_DIRECT', 'Mở hội thoại khách hàng', 'Cho phép tạo hội thoại riêng với khách hàng.', now(), NULL, NULL, NULL),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000104', 'OBSERVE', 'Giám sát', 'Cho phép giám sát dữ liệu nghiệp vụ.', now(), NULL, NULL, NULL)
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (
    permission_id, permission_code, name, description, created_at, created_by, updated_at, updated_by,
    module_id, action_id, scope_id
) VALUES
    ('7f4f11d7-2b52-4c20-bf11-0e4200000001', 'SUPPORT_TICKET_RESPOND_ASSIGNED', 'Phản hồi phiếu được phân công',
     'Cho phép mở hội thoại riêng với khách hàng cho phiếu được phân công.', now(), NULL, NULL, NULL,
     '00000000-0000-0000-0000-000000001029', '7f4f11d7-2b52-4c20-bf11-0e4200000101', '42a395e0-41dc-4952-bc48-0abef1eb0af8'),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000002', 'SUPPORT_TICKET_CREATE_FROM_CHAT_OWN', 'Tạo phiếu từ hội thoại của mình',
     'Cho phép khách hàng đã được duyệt tạo phiếu và giao trực tiếp cho đối tác trong hội thoại riêng.', now(), NULL, NULL, NULL,
     '00000000-0000-0000-0000-000000001029', '7f4f11d7-2b52-4c20-bf11-0e4200000102', '00000000-0000-0000-0000-000000003002'),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000003', 'CHAT_CONVERSATION_CREATE_CUSTOMER_DIRECT', 'Mở hội thoại riêng với khách hàng',
     'Cho phép chủ động tạo hoặc dùng lại hội thoại riêng với một khách hàng.', now(), NULL, NULL, NULL,
     '1ad15487-d9a1-4b0c-821a-a646337c1f8e', '7f4f11d7-2b52-4c20-bf11-0e4200000103', '00000000-0000-0000-0000-000000003002'),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000004', 'CHAT_CONVERSATION_OBSERVE_ALL', 'Giám sát mọi hội thoại',
     'Cho phép giám sát hội thoại theo nhu cầu kiểm soát; không được tự động cấp cho quản trị hệ thống.', now(), NULL, NULL, NULL,
     '1ad15487-d9a1-4b0c-821a-a646337c1f8e', '7f4f11d7-2b52-4c20-bf11-0e4200000104', '00000000-0000-0000-0000-000000003001')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT seeded.id, seeded.role_id, permission.permission_id, now(), NULL, NULL, NULL, true, true
FROM (
    VALUES
        ('7f4f11d7-2b52-4c20-bf11-0e4200000011'::uuid, '00000000-0000-0000-0000-000000000003'::uuid, 'SUPPORT_TICKET_CREATE_FROM_CHAT_OWN'),
        ('7f4f11d7-2b52-4c20-bf11-0e4200000012'::uuid, '00000000-0000-0000-0000-000000000002'::uuid, 'SUPPORT_TICKET_RESPOND_ASSIGNED'),
        ('7f4f11d7-2b52-4c20-bf11-0e4200000013'::uuid, '00000000-0000-0000-0000-000000000002'::uuid, 'CHAT_CONVERSATION_CREATE_CUSTOMER_DIRECT'),
        ('7f4f11d7-2b52-4c20-bf11-0e4200000014'::uuid, '00000000-0000-0000-0000-000000000005'::uuid, 'CHAT_CONVERSATION_CREATE_CUSTOMER_DIRECT')
) AS seeded(id, role_id, permission_code)
JOIN iam.permissions permission ON permission.permission_code = seeded.permission_code
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();

-- PARKING_MANAGER remains the dispatcher by default: read, assign and manage workflow,
-- but no longer resolves a ticket merely by virtue of its role configuration.
UPDATE iam.role_permissions
SET is_active = false,
    updated_at = now()
WHERE role_id = '00000000-0000-0000-0000-000000000005'
  AND permission_id = (
      SELECT permission_id
      FROM iam.permissions
      WHERE permission_code = 'SUPPORT_TICKET_PROCESS_ALL'
  );
