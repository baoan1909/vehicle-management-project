-- Partner admins can contact customers who reviewed a parking lot in their scoped organization.
-- The application still enforces conversation membership for read/send/attachment operations.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT gen_random_uuid(), role.role_id, permission.permission_id,
       now(), NULL, NULL, NULL, true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'CHAT_CONVERSATION_CREATE_CUSTOMER_DIRECT',
    'CHAT_CONVERSATION_READ_OWN',
    'CHAT_MESSAGE_SEND_OWN',
    'CHAT_ATTACHMENT_CREATE_OWN',
    'CHAT_ATTACHMENT_READ_OWN'
)
WHERE role.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    is_system = true,
    updated_at = now();
