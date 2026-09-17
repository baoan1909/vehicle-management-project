-- Phase 2: default embedding model configuration and knowledge admin permissions.

-- Default embedding configuration (GEMINI, gemini-embedding-2, fixed dimension 768).
INSERT INTO ai.ai_model_configurations (
    configuration_id, provider, use_case, model_id, api_version, temperature, max_output_tokens,
    priority, rollout_percentage, status, requires_function_calling,
    requires_structured_output, free_tier_approved, output_dimension
) SELECT
    '7f4f11d7-2b52-4c20-bf11-0e4200000402',
    'GEMINI', 'EMBEDDING', 'gemini-embedding-2', 'v1beta', 0.30, 1024,
    10, 100, 'ACTIVE', false, false, true, 768
WHERE NOT EXISTS (
    SELECT 1 FROM ai.ai_model_configurations WHERE use_case = 'EMBEDDING'
)
ON CONFLICT (configuration_id) DO NOTHING;

-- New action for reindexing the knowledge base. APPROVE already exists globally and
-- is reused by AI_KNOWLEDGE_APPROVE_ALL.
INSERT INTO iam.permission_actions (
    action_id, code, name, description, created_at, created_by, updated_at, updated_by
) VALUES (
    '7f4f11d7-2b52-4c20-bf11-0e4200000514', 'REINDEX', 'Lập chỉ mục',
    'Cho phép lập chỉ mục lại kho tri thức AI.', now(), NULL, NULL, NULL
)
ON CONFLICT (code) DO NOTHING;

-- Read/approve/reindex permissions on the AI_KNOWLEDGE module.
INSERT INTO iam.permissions (
    permission_id, permission_code, name, description, created_at, created_by, updated_at, updated_by,
    module_id, action_id, scope_id
) VALUES
    (
        '7f4f11d7-2b52-4c20-bf11-0e4200000551', 'AI_KNOWLEDGE_READ_ALL',
        'Xem kho tri thức AI', 'Cho phép xem cấu hình và trạng thái chỉ mục kho tri thức AI.',
        now(), NULL, NULL, NULL, '7f4f11d7-2b52-4c20-bf11-0e4200000502',
        (SELECT action_id FROM iam.permission_actions WHERE code = 'READ'),
        '00000000-0000-0000-0000-000000003001'
    ),
    (
        '7f4f11d7-2b52-4c20-bf11-0e4200000553', 'AI_KNOWLEDGE_APPROVE_ALL',
        'Phê duyệt chỉ mục tri thức AI', 'Cho phép kích hoạt hoặc quay lại chỉ mục kho tri thức AI.',
        now(), NULL, NULL, NULL, '7f4f11d7-2b52-4c20-bf11-0e4200000502',
        (SELECT action_id FROM iam.permission_actions WHERE code = 'APPROVE'),
        '00000000-0000-0000-0000-000000003001'
    ),
    (
        '7f4f11d7-2b52-4c20-bf11-0e4200000554', 'AI_KNOWLEDGE_REINDEX_ALL',
        'Lập chỉ mục kho tri thức AI', 'Cho phép bắt đầu quá trình lập chỉ mục lại kho tri thức AI.',
        now(), NULL, NULL, NULL, '7f4f11d7-2b52-4c20-bf11-0e4200000502',
        (SELECT action_id FROM iam.permission_actions WHERE code = 'REINDEX'),
        '00000000-0000-0000-0000-000000003001'
    )
ON CONFLICT (permission_code) DO NOTHING;

-- Grant all AI_KNOWLEDGE permissions to SYSTEM_ADMIN resolved by role code (no hard-coded role id).
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT seeded.id, role.role_id, permission.permission_id, now(), NULL, NULL, NULL, true, true
FROM (
    VALUES
        ('7f4f11d7-2b52-4c20-bf11-0e4200000571'::uuid, 'AI_KNOWLEDGE_READ_ALL'),
        ('7f4f11d7-2b52-4c20-bf11-0e4200000572'::uuid, 'AI_KNOWLEDGE_MANAGE_ALL'),
        ('7f4f11d7-2b52-4c20-bf11-0e4200000573'::uuid, 'AI_KNOWLEDGE_APPROVE_ALL'),
        ('7f4f11d7-2b52-4c20-bf11-0e4200000574'::uuid, 'AI_KNOWLEDGE_REINDEX_ALL')
) AS seeded(id, permission_code)
JOIN iam.roles role ON role.code = 'SYSTEM_ADMIN'
JOIN iam.permissions permission ON permission.permission_code = seeded.permission_code
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
