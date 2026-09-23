CREATE SCHEMA IF NOT EXISTS ai;

CREATE TABLE ai.ai_model_configurations (
    configuration_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(30) NOT NULL,
    use_case VARCHAR(40) NOT NULL,
    model_id VARCHAR(120) NOT NULL,
    api_version VARCHAR(30),
    temperature NUMERIC(4,2) NOT NULL DEFAULT 0.30,
    max_output_tokens INTEGER NOT NULL DEFAULT 1024,
    priority INTEGER NOT NULL DEFAULT 100,
    rollout_percentage INTEGER NOT NULL DEFAULT 100,
    status VARCHAR(20) NOT NULL,
    requires_function_calling BOOLEAN NOT NULL DEFAULT false,
    requires_structured_output BOOLEAN NOT NULL DEFAULT true,
    free_tier_approved BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    CONSTRAINT ck_ai_model_config_provider CHECK (provider IN ('GEMINI')),
    CONSTRAINT ck_ai_model_config_use_case CHECK (use_case IN ('SUPPORT_CHAT', 'INTENT_CLASSIFICATION', 'CONVERSATION_SUMMARY', 'EMBEDDING')),
    CONSTRAINT ck_ai_model_config_status CHECK (status IN ('CANDIDATE', 'ACTIVE', 'FALLBACK', 'DISABLED')),
    CONSTRAINT ck_ai_model_config_rollout CHECK (rollout_percentage BETWEEN 0 AND 100),
    CONSTRAINT ck_ai_model_config_temperature CHECK (temperature >= 0 AND temperature <= 2),
    CONSTRAINT ck_ai_model_config_max_tokens CHECK (max_output_tokens > 0)
);

CREATE INDEX idx_ai_model_config_lookup
    ON ai.ai_model_configurations (use_case, provider, status, priority, created_at);

CREATE TABLE ai.ai_model_catalog (
    provider VARCHAR(30) NOT NULL,
    model_id VARCHAR(120) NOT NULL,
    display_name VARCHAR(200),
    model_version VARCHAR(80),
    input_token_limit INTEGER,
    output_token_limit INTEGER,
    supported_actions JSONB NOT NULL DEFAULT '[]'::jsonb,
    discovered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE',
    PRIMARY KEY (provider, model_id),
    CONSTRAINT ck_ai_model_catalog_provider CHECK (provider IN ('GEMINI')),
    CONSTRAINT ck_ai_model_catalog_status CHECK (status IN ('CANDIDATE', 'ACTIVE', 'FALLBACK', 'DISABLED')),
    CONSTRAINT ck_ai_model_catalog_supported_actions_array CHECK (jsonb_typeof(supported_actions) = 'array')
);

CREATE TABLE ai.ai_runs (
    run_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES operations.chat_conversations(conversation_id) ON DELETE CASCADE,
    input_message_id UUID NOT NULL REFERENCES operations.chat_messages(message_id) ON DELETE CASCADE,
    output_message_id UUID REFERENCES operations.chat_messages(message_id) ON DELETE SET NULL,
    provider VARCHAR(30) NOT NULL,
    model_id VARCHAR(120) NOT NULL,
    prompt_version VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    latency_ms BIGINT,
    input_tokens INTEGER,
    output_tokens INTEGER,
    failure_code VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_ai_runs_provider CHECK (provider IN ('GEMINI')),
    CONSTRAINT ck_ai_runs_status CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED'))
);

CREATE UNIQUE INDEX uq_ai_runs_success_input_message
    ON ai.ai_runs (input_message_id)
    WHERE status = 'SUCCEEDED';

CREATE INDEX idx_ai_runs_conversation_created
    ON ai.ai_runs (conversation_id, created_at DESC);

CREATE TABLE ai.ai_tool_calls (
    tool_call_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES ai.ai_runs(run_id) ON DELETE CASCADE,
    tool_name VARCHAR(80) NOT NULL,
    request_payload_redacted JSONB NOT NULL DEFAULT '{}'::jsonb,
    response_payload_redacted JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(120),
    confirmed_by UUID,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_ai_tool_calls_status CHECK (status IN ('ALLOWED', 'DENIED', 'FAILED', 'SUCCEEDED')),
    CONSTRAINT ck_ai_tool_calls_request_object CHECK (jsonb_typeof(request_payload_redacted) = 'object'),
    CONSTRAINT ck_ai_tool_calls_response_object CHECK (jsonb_typeof(response_payload_redacted) = 'object')
);

CREATE INDEX idx_ai_tool_calls_run
    ON ai.ai_tool_calls (run_id, created_at);

CREATE TABLE ai.assistant_jobs (
    job_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    input_message_id UUID NOT NULL REFERENCES operations.chat_messages(message_id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES operations.chat_conversations(conversation_id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    locked_at TIMESTAMPTZ,
    error_code VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT ck_assistant_jobs_status CHECK (status IN ('PENDING', 'PROCESSING', 'RETRYING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_assistant_jobs_attempt_count CHECK (attempt_count >= 0)
);

CREATE UNIQUE INDEX uq_assistant_jobs_input_message
    ON ai.assistant_jobs (input_message_id);

CREATE INDEX idx_assistant_jobs_due
    ON ai.assistant_jobs (status, next_attempt_at, created_at);

CREATE TRIGGER trg_ai_model_configurations_set_updated_at
    BEFORE UPDATE ON ai.ai_model_configurations
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER trg_assistant_jobs_set_updated_at
    BEFORE UPDATE ON ai.assistant_jobs
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

INSERT INTO ai.ai_model_configurations (
    configuration_id, provider, use_case, model_id, api_version, temperature, max_output_tokens,
    priority, rollout_percentage, status, requires_function_calling,
    requires_structured_output, free_tier_approved
) VALUES (
    '7f4f11d7-2b52-4c20-bf11-0e4200000401',
    'GEMINI',
    'SUPPORT_CHAT',
    'gemini-3.6-flash',
    'v1beta',
    0.30,
    1024,
    10,
    100,
    'ACTIVE',
    false,
    true,
    true
) ON CONFLICT (configuration_id) DO NOTHING;
