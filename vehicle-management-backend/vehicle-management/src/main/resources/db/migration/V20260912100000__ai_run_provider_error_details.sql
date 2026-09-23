ALTER TABLE ai.ai_runs
    ADD COLUMN IF NOT EXISTS provider_status INTEGER,
    ADD COLUMN IF NOT EXISTS provider_error_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS provider_error_message_redacted VARCHAR(500),
    ADD COLUMN IF NOT EXISTS field_violations_redacted JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS failure_retryable BOOLEAN;

ALTER TABLE ai.ai_runs
    DROP CONSTRAINT IF EXISTS ck_ai_runs_provider_status,
    DROP CONSTRAINT IF EXISTS ck_ai_runs_field_violations_array;

ALTER TABLE ai.ai_runs
    ADD CONSTRAINT ck_ai_runs_provider_status CHECK (provider_status IS NULL OR provider_status BETWEEN 100 AND 599),
    ADD CONSTRAINT ck_ai_runs_field_violations_array CHECK (jsonb_typeof(field_violations_redacted) = 'array');
