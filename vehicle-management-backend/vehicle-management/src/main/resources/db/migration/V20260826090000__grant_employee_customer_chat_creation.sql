-- Employees handle customer support, so they need to initiate a support conversation
-- for a selected customer. CHAT_CONVERSATION_CREATE_OWN remains required by the use case.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT
    '4a7d0400-0000-4000-8000-000000000005',
    '00000000-0000-0000-0000-000000000002',
    '00eaa276-23bc-4fba-bdda-7aab10dd48fe',
    now(), NULL, NULL, NULL, true, true
WHERE NOT EXISTS (
    SELECT 1
    FROM iam.role_permissions
    WHERE role_id = '00000000-0000-0000-0000-000000000002'
      AND permission_id = '00eaa276-23bc-4fba-bdda-7aab10dd48fe'
);
