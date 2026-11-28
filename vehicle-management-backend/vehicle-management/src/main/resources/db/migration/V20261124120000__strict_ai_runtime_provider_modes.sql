-- Additive provider separation. Existing Google configurations/vectors stay intact.
CREATE TABLE IF NOT EXISTS ai.runtime_mode (
    singleton_id SMALLINT PRIMARY KEY CHECK (singleton_id=1),
    mode VARCHAR(20) NOT NULL CHECK (mode IN ('GOOGLE_API','QWEN_LOCAL')),
    target_mode VARCHAR(20) NOT NULL CHECK (target_mode IN ('GOOGLE_API','QWEN_LOCAL')),
    epoch BIGINT NOT NULL CHECK (epoch>0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('READY','SWITCHING','PREPARING','ERROR')),
    changed_by UUID,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO ai.runtime_mode(singleton_id,mode,target_mode,epoch,status)
VALUES(1,'GOOGLE_API','GOOGLE_API',1,'READY')
ON CONFLICT (singleton_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS ai.runtime_mode_events (
    event_id UUID PRIMARY KEY,
    from_mode VARCHAR(20) NOT NULL,
    to_mode VARCHAR(20) NOT NULL,
    epoch BIGINT NOT NULL,
    actor_id UUID,
    result VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai.provider_http_calls (
    call_id UUID PRIMARY KEY,
    epoch BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL CHECK(provider IN ('GEMINI','LM_STUDIO')),
    operation VARCHAR(20) NOT NULL CHECK(operation IN ('GENERATION','EMBEDDING','CATALOG')),
    started_at TIMESTAMPTZ NOT NULL,
    latency_ms BIGINT NOT NULL CHECK(latency_ms>=0),
    http_status INTEGER NOT NULL CHECK(http_status BETWEEN 0 AND 599),
    failure_code VARCHAR(80)
);
CREATE INDEX IF NOT EXISTS idx_provider_http_calls_epoch ON ai.provider_http_calls(epoch,provider,started_at);

ALTER TABLE ai.ai_model_configurations DROP CONSTRAINT IF EXISTS ck_ai_model_config_provider;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'ai.ai_model_configurations'::regclass
          AND conname = 'ck_ai_model_config_provider'
    ) THEN
        ALTER TABLE ai.ai_model_configurations
            ADD CONSTRAINT ck_ai_model_config_provider CHECK(provider IN ('GEMINI','LM_STUDIO'));
    END IF;
END;
$$;
ALTER TABLE ai.ai_model_catalog DROP CONSTRAINT IF EXISTS ck_ai_model_catalog_provider;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'ai.ai_model_catalog'::regclass
          AND conname = 'ck_ai_model_catalog_provider'
    ) THEN
        ALTER TABLE ai.ai_model_catalog
            ADD CONSTRAINT ck_ai_model_catalog_provider CHECK(provider IN ('GEMINI','LM_STUDIO'));
    END IF;
END;
$$;
ALTER TABLE ai.ai_runs DROP CONSTRAINT IF EXISTS ck_ai_runs_provider;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'ai.ai_runs'::regclass
          AND conname = 'ck_ai_runs_provider'
    ) THEN
        ALTER TABLE ai.ai_runs
            ADD CONSTRAINT ck_ai_runs_provider CHECK(provider IN ('GEMINI','LM_STUDIO'));
    END IF;
END;
$$;
ALTER TABLE ai.ai_model_warnings DROP CONSTRAINT IF EXISTS ck_ai_model_warnings_provider;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'ai.ai_model_warnings'::regclass
          AND conname = 'ck_ai_model_warnings_provider'
    ) THEN
        ALTER TABLE ai.ai_model_warnings
            ADD CONSTRAINT ck_ai_model_warnings_provider CHECK(provider IN ('GEMINI','LM_STUDIO'));
    END IF;
END;
$$;
ALTER TABLE ai.knowledge_index_versions DROP CONSTRAINT IF EXISTS ck_knowledge_index_versions_provider;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'ai.knowledge_index_versions'::regclass
          AND conname = 'ck_knowledge_index_versions_provider'
    ) THEN
        ALTER TABLE ai.knowledge_index_versions
            ADD CONSTRAINT ck_knowledge_index_versions_provider CHECK(provider IN ('GEMINI','LM_STUDIO'));
    END IF;
END;
$$;
ALTER TABLE ai.knowledge_staged_embeddings DROP CONSTRAINT IF EXISTS ck_knowledge_staged_provider;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'ai.knowledge_staged_embeddings'::regclass
          AND conname = 'ck_knowledge_staged_provider'
    ) THEN
        ALTER TABLE ai.knowledge_staged_embeddings
            ADD CONSTRAINT ck_knowledge_staged_provider CHECK(provider IN ('GEMINI','LM_STUDIO'));
    END IF;
END;
$$;

DROP INDEX IF EXISTS ai.uq_knowledge_index_versions_single_active;
CREATE UNIQUE INDEX IF NOT EXISTS uq_knowledge_index_versions_single_active
    ON ai.knowledge_index_versions(provider) WHERE status='ACTIVE';

ALTER TABLE ai.assistant_jobs
    ADD COLUMN IF NOT EXISTS runtime_epoch BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS runtime_mode VARCHAR(20) NOT NULL DEFAULT 'GOOGLE_API'
        CHECK(runtime_mode IN ('GOOGLE_API','QWEN_LOCAL'));

CREATE OR REPLACE FUNCTION ai.stamp_assistant_job_runtime() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    SELECT epoch, CASE WHEN status='SWITCHING' THEN target_mode ELSE mode END
      INTO NEW.runtime_epoch, NEW.runtime_mode FROM ai.runtime_mode WHERE singleton_id=1;
    RETURN NEW;
END;
$$;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgrelid = 'ai.assistant_jobs'::regclass
          AND tgname = 'stamp_assistant_job_runtime'
    ) THEN
        CREATE TRIGGER stamp_assistant_job_runtime BEFORE INSERT ON ai.assistant_jobs
            FOR EACH ROW EXECUTE FUNCTION ai.stamp_assistant_job_runtime();
    END IF;
END;
$$;
