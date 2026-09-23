DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS vector;
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'pgvector extension is not installed; knowledge_embeddings.embedding will use REAL[] fallback in this environment';
END $$;

ALTER TABLE operations.chat_messages
    DROP CONSTRAINT IF EXISTS ck_chat_messages_type;

ALTER TABLE operations.chat_messages
    ADD CONSTRAINT ck_chat_messages_type CHECK (
        message_type IN (
            'TEXT', 'IMAGE', 'FILE', 'SYSTEM', 'CONTEXT_CARD', 'ACTION_CARD',
            'SUPPORT_REQUEST', 'ASSISTANT_TEXT', 'TOOL_RESULT'
        )
    );

ALTER TABLE ai.assistant_jobs
    DROP CONSTRAINT IF EXISTS ck_assistant_jobs_status;

ALTER TABLE ai.assistant_jobs
    ADD CONSTRAINT ck_assistant_jobs_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'RETRYING', 'WAITING_CONFIRMATION', 'COMPLETED', 'FAILED', 'EXPIRED')
    );

ALTER TABLE ai.ai_runs
    ADD COLUMN IF NOT EXISTS configuration_id UUID REFERENCES ai.ai_model_configurations(configuration_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS rollout_version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS attempt_number INTEGER NOT NULL DEFAULT 1;

ALTER TABLE ai.ai_tool_calls
    ADD COLUMN IF NOT EXISTS conversation_id UUID REFERENCES operations.chat_conversations(conversation_id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS input_message_id UUID REFERENCES operations.chat_messages(message_id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS requested_by UUID REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS tool_type VARCHAR(20) NOT NULL DEFAULT 'READ_ONLY',
    ADD COLUMN IF NOT EXISTS argument_payload_redacted JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS executed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS failure_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;

UPDATE ai.ai_tool_calls
SET argument_payload_redacted = request_payload_redacted
WHERE argument_payload_redacted = '{}'::jsonb
  AND request_payload_redacted <> '{}'::jsonb;

UPDATE ai.ai_tool_calls
SET status = CASE status
    WHEN 'ALLOWED' THEN 'VALIDATED'
    ELSE status
END
WHERE status IN ('ALLOWED');

ALTER TABLE ai.ai_tool_calls
    DROP CONSTRAINT IF EXISTS ck_ai_tool_calls_status;

ALTER TABLE ai.ai_tool_calls
    ADD CONSTRAINT ck_ai_tool_calls_status CHECK (
        status IN ('REQUESTED', 'VALIDATED', 'AWAITING_CONFIRMATION', 'EXECUTING', 'SUCCEEDED', 'FAILED', 'DENIED', 'EXPIRED')
    );

ALTER TABLE ai.ai_tool_calls
    DROP CONSTRAINT IF EXISTS ck_ai_tool_calls_request_object,
    DROP CONSTRAINT IF EXISTS ck_ai_tool_calls_response_object;

ALTER TABLE ai.ai_tool_calls
    ADD CONSTRAINT ck_ai_tool_calls_request_object CHECK (jsonb_typeof(request_payload_redacted) = 'object'),
    ADD CONSTRAINT ck_ai_tool_calls_argument_object CHECK (jsonb_typeof(argument_payload_redacted) = 'object'),
    ADD CONSTRAINT ck_ai_tool_calls_response_object CHECK (jsonb_typeof(response_payload_redacted) = 'object'),
    ADD CONSTRAINT ck_ai_tool_calls_tool_type CHECK (tool_type IN ('READ_ONLY', 'WRITE'));

CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_tool_calls_idempotency
    ON ai.ai_tool_calls (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ai_tool_calls_pending_confirmation
    ON ai.ai_tool_calls (requested_by, status, expires_at);

CREATE TRIGGER trg_ai_tool_calls_set_updated_at
    BEFORE UPDATE ON ai.ai_tool_calls
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_messages_ai_reply_input_v2
    ON operations.chat_messages (reply_to_message_id)
    WHERE message_type = 'ASSISTANT_TEXT'
      AND related_schema = 'ai'
      AND related_table = 'assistant_jobs';

CREATE TABLE ai.knowledge_sources (
    source_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    title VARCHAR(200) NOT NULL,
    access_scope VARCHAR(40) NOT NULL DEFAULT 'PUBLIC',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT ck_knowledge_sources_access_scope CHECK (access_scope IN ('PUBLIC', 'CUSTOMER', 'EMPLOYEE', 'ADMIN', 'TENANT_PRIVATE')),
    CONSTRAINT ck_knowledge_sources_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE ai.knowledge_documents (
    document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id UUID NOT NULL REFERENCES ai.knowledge_sources(source_id) ON DELETE CASCADE,
    tenant_id UUID,
    title VARCHAR(240) NOT NULL,
    object_key VARCHAR(500),
    mime_type VARCHAR(120),
    checksum_sha256 VARCHAR(64),
    document_version INTEGER NOT NULL DEFAULT 1,
    access_scope VARCHAR(40) NOT NULL DEFAULT 'PUBLIC',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT ck_knowledge_documents_access_scope CHECK (access_scope IN ('PUBLIC', 'CUSTOMER', 'EMPLOYEE', 'ADMIN', 'TENANT_PRIVATE')),
    CONSTRAINT ck_knowledge_documents_status CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED', 'ARCHIVED'))
);

CREATE TABLE ai.knowledge_chunks (
    chunk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES ai.knowledge_documents(document_id) ON DELETE CASCADE,
    tenant_id UUID,
    title VARCHAR(240) NOT NULL,
    content TEXT NOT NULL,
    summary TEXT,
    source_page INTEGER,
    source_section VARCHAR(200),
    chunk_index INTEGER NOT NULL,
    access_scope VARCHAR(40) NOT NULL DEFAULT 'PUBLIC',
    document_version INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'READY',
    search_vector tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, '') || ' ' || coalesce(summary, ''))) STORED,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT ck_knowledge_chunks_access_scope CHECK (access_scope IN ('PUBLIC', 'CUSTOMER', 'EMPLOYEE', 'ADMIN', 'TENANT_PRIVATE')),
    CONSTRAINT ck_knowledge_chunks_status CHECK (status IN ('READY', 'ARCHIVED'))
);

CREATE TABLE ai.knowledge_embeddings (
    embedding_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chunk_id UUID NOT NULL REFERENCES ai.knowledge_chunks(chunk_id) ON DELETE CASCADE,
    embedding REAL[],
    embedding_model VARCHAR(120) NOT NULL,
    embedding_dimension INTEGER NOT NULL DEFAULT 768,
    document_version INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_knowledge_embeddings_dimension CHECK (embedding_dimension = 768),
    CONSTRAINT ck_knowledge_embeddings_vector_length CHECK (embedding IS NULL OR cardinality(embedding) = embedding_dimension)
);

CREATE TABLE ai.knowledge_ingestion_jobs (
    ingestion_job_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES ai.knowledge_documents(document_id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    error_code VARCHAR(80),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT ck_knowledge_ingestion_jobs_status CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED', 'RETRYING'))
);

CREATE TABLE ai.ai_retrieval_audits (
    retrieval_audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID REFERENCES ai.ai_runs(run_id) ON DELETE SET NULL,
    conversation_id UUID REFERENCES operations.chat_conversations(conversation_id) ON DELETE CASCADE,
    input_message_id UUID REFERENCES operations.chat_messages(message_id) ON DELETE SET NULL,
    requested_by UUID REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    query_redacted TEXT NOT NULL,
    result_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai.ai_message_citations (
    citation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES operations.chat_messages(message_id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES ai.knowledge_documents(document_id) ON DELETE CASCADE,
    chunk_id UUID NOT NULL REFERENCES ai.knowledge_chunks(chunk_id) ON DELETE CASCADE,
    title VARCHAR(240) NOT NULL,
    source_page INTEGER,
    source_section VARCHAR(200),
    retrieval_score NUMERIC(8,5),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ai.ai_model_warnings (
    warning_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(30) NOT NULL,
    model_id VARCHAR(120),
    configuration_id UUID REFERENCES ai.ai_model_configurations(configuration_id) ON DELETE CASCADE,
    warning_code VARCHAR(80) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    detail VARCHAR(500),
    detected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT ck_ai_model_warnings_provider CHECK (provider IN ('GEMINI')),
    CONSTRAINT ck_ai_model_warnings_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT ck_ai_model_warnings_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_model_warnings_open
    ON ai.ai_model_warnings (provider, coalesce(model_id, ''), coalesce(configuration_id, '00000000-0000-0000-0000-000000000000'::uuid), warning_code)
    WHERE status IN ('OPEN', 'ACKNOWLEDGED');

CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_search
    ON ai.knowledge_chunks USING GIN (search_vector);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_ready_scope
    ON ai.knowledge_chunks (status, access_scope, tenant_id, document_id);

CREATE INDEX IF NOT EXISTS idx_ai_message_citations_message
    ON ai.ai_message_citations (message_id);

INSERT INTO iam.permission_modules (
    module_id, code, name, description, created_at, created_by, updated_at, updated_by
) VALUES
    ('7f4f11d7-2b52-4c20-bf11-0e4200000501', 'AI_MODEL', 'Quan tri model AI', 'Quan tri cau hinh model AI.', now(), NULL, NULL, NULL),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000502', 'AI_KNOWLEDGE', 'Quan tri tri thuc AI', 'Quan tri kho tri thuc AI.', now(), NULL, NULL, NULL),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000503', 'AI_CATALOG', 'Dong bo catalog AI', 'Dong bo va doi soat catalog model AI.', now(), NULL, NULL, NULL)
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_actions (
    action_id, code, name, description, created_at, created_by, updated_at, updated_by
) VALUES
    ('7f4f11d7-2b52-4c20-bf11-0e4200000511', 'READ', 'Xem', 'Cho phep xem du lieu.', now(), NULL, NULL, NULL),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000512', 'MANAGE', 'Quan tri', 'Cho phep quan tri du lieu.', now(), NULL, NULL, NULL),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000513', 'SYNC', 'Dong bo', 'Cho phep dong bo du lieu.', now(), NULL, NULL, NULL)
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (
    permission_id, permission_code, name, description, created_at, created_by, updated_at, updated_by,
    module_id, action_id, scope_id
) VALUES
    ('7f4f11d7-2b52-4c20-bf11-0e4200000521', 'AI_MODEL_READ_ALL',
     'Xem cau hinh model AI', 'Cho phep xem cau hinh model AI va canh bao lien quan.',
     now(), NULL, NULL, NULL, '7f4f11d7-2b52-4c20-bf11-0e4200000501',
     (SELECT action_id FROM iam.permission_actions WHERE code = 'READ'), '00000000-0000-0000-0000-000000003001'),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000522', 'AI_MODEL_MANAGE_ALL',
     'Quan tri cau hinh model AI', 'Cho phep tao va cap nhat cau hinh model AI.',
     now(), NULL, NULL, NULL, '7f4f11d7-2b52-4c20-bf11-0e4200000501',
     (SELECT action_id FROM iam.permission_actions WHERE code = 'MANAGE'), '00000000-0000-0000-0000-000000003001'),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000523', 'AI_CATALOG_SYNC_ALL',
     'Dong bo catalog model AI', 'Cho phep goi provider de dong bo catalog model AI.',
     now(), NULL, NULL, NULL, '7f4f11d7-2b52-4c20-bf11-0e4200000503',
     (SELECT action_id FROM iam.permission_actions WHERE code = 'SYNC'), '00000000-0000-0000-0000-000000003001'),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000524', 'AI_KNOWLEDGE_MANAGE_ALL',
     'Quan tri kho tri thuc AI', 'Cho phep quan tri tai lieu va ingestion tri thuc AI.',
     now(), NULL, NULL, NULL, '7f4f11d7-2b52-4c20-bf11-0e4200000502',
     (SELECT action_id FROM iam.permission_actions WHERE code = 'MANAGE'), '00000000-0000-0000-0000-000000003001')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT seeded.id, '00000000-0000-0000-0000-000000000001'::uuid, permission.permission_id, now(), NULL, NULL, NULL, true, true
FROM (
    VALUES
        ('7f4f11d7-2b52-4c20-bf11-0e4200000531'::uuid, 'AI_MODEL_READ_ALL'),
        ('7f4f11d7-2b52-4c20-bf11-0e4200000532'::uuid, 'AI_MODEL_MANAGE_ALL'),
        ('7f4f11d7-2b52-4c20-bf11-0e4200000533'::uuid, 'AI_CATALOG_SYNC_ALL'),
        ('7f4f11d7-2b52-4c20-bf11-0e4200000534'::uuid, 'AI_KNOWLEDGE_MANAGE_ALL')
) AS seeded(id, permission_code)
JOIN iam.permissions permission ON permission.permission_code = seeded.permission_code
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
