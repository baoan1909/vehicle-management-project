-- Phase 5: permission-driven retrieval scopes. Role codes are used only for
-- initial grants; runtime authorization resolves effective permissions.

-- Citation labels are part of the model-visible allowlist and must survive
-- persistence so the API never reconstructs or invents labels later.
ALTER TABLE ai.ai_message_citations
    ADD COLUMN IF NOT EXISTS label VARCHAR(20);

WITH ranked AS (
    SELECT citation_id,
           ROW_NUMBER() OVER (PARTITION BY message_id ORDER BY citation_order, citation_id) AS label_number
    FROM ai.ai_message_citations
    WHERE label IS NULL
)
UPDATE ai.ai_message_citations citation
SET label = 'C' || ranked.label_number::text
FROM ranked
WHERE citation.citation_id = ranked.citation_id;

ALTER TABLE ai.ai_message_citations
    ALTER COLUMN label SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_message_citations_message_label
    ON ai.ai_message_citations (message_id, label);

INSERT INTO iam.permission_scopes (
    scope_id, code, name, description, created_at, created_by, updated_at, updated_by
) VALUES
    ('7f4f11d7-2b52-4c20-bf11-0e42000005a1', 'CUSTOMER', 'Khách hàng',
     'Áp dụng cho nội dung được phép hiển thị cho khách hàng đã đăng nhập.', now(), NULL, NULL, NULL),
    ('7f4f11d7-2b52-4c20-bf11-0e42000005a2', 'EMPLOYEE', 'Nhân viên',
     'Áp dụng cho nội dung nội bộ dành cho nhân viên.', now(), NULL, NULL, NULL),
    ('7f4f11d7-2b52-4c20-bf11-0e42000005a3', 'TENANT_PRIVATE', 'Riêng theo đơn vị',
     'Áp dụng cho nội dung riêng của đúng đơn vị đã được backend xác định.', now(), NULL, NULL, NULL)
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (
    permission_id, permission_code, name, description, created_at, created_by, updated_at, updated_by,
    module_id, action_id, scope_id
) VALUES
    (
        '7f4f11d7-2b52-4c20-bf11-0e4200000581', 'AI_KNOWLEDGE_READ_CUSTOMER',
        'Đọc tri thức dành cho khách hàng', 'Cho phép chatbot truy xuất tài liệu dành cho khách hàng đã đăng nhập.',
        now(), NULL, NULL, NULL, '7f4f11d7-2b52-4c20-bf11-0e4200000502',
        (SELECT action_id FROM iam.permission_actions WHERE code = 'READ'),
        (SELECT scope_id FROM iam.permission_scopes WHERE code = 'CUSTOMER')
    ),
    (
        '7f4f11d7-2b52-4c20-bf11-0e4200000582', 'AI_KNOWLEDGE_READ_EMPLOYEE',
        'Đọc tri thức dành cho nhân viên', 'Cho phép chatbot truy xuất tài liệu nội bộ dành cho nhân viên.',
        now(), NULL, NULL, NULL, '7f4f11d7-2b52-4c20-bf11-0e4200000502',
        (SELECT action_id FROM iam.permission_actions WHERE code = 'READ'),
        (SELECT scope_id FROM iam.permission_scopes WHERE code = 'EMPLOYEE')
    ),
    (
        '7f4f11d7-2b52-4c20-bf11-0e4200000583', 'AI_KNOWLEDGE_READ_TENANT_PRIVATE',
        'Đọc tri thức riêng của đơn vị', 'Cho phép truy xuất tài liệu riêng khi backend đã ràng buộc đúng tenant.',
        now(), NULL, NULL, NULL, '7f4f11d7-2b52-4c20-bf11-0e4200000502',
        (SELECT action_id FROM iam.permission_actions WHERE code = 'READ'),
        (SELECT scope_id FROM iam.permission_scopes WHERE code = 'TENANT_PRIVATE')
    )
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT seed.id, role.role_id, permission.permission_id, now(), NULL, NULL, NULL, true, true
FROM (
    VALUES
        ('7f4f11d7-2b52-4c20-bf11-0e4200000591'::uuid, 'CUSTOMER', 'AI_KNOWLEDGE_READ_CUSTOMER'),
        ('7f4f11d7-2b52-4c20-bf11-0e4200000592'::uuid, 'EMPLOYEE', 'AI_KNOWLEDGE_READ_EMPLOYEE'),
        ('7f4f11d7-2b52-4c20-bf11-0e4200000593'::uuid, 'PARKING_MANAGER', 'AI_KNOWLEDGE_READ_EMPLOYEE')
) AS seed(id, role_code, permission_code)
JOIN iam.roles role ON role.code = seed.role_code
JOIN iam.permissions permission ON permission.permission_code = seed.permission_code
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
