-- The application now supports Google only. Existing databases need this forward upgrade.
-- Preserve retired inference/audit data before removing it from runtime tables.
CREATE TABLE IF NOT EXISTS ai.retired_provider_records (
    record_table TEXT NOT NULL,
    record_key TEXT NOT NULL,
    payload JSONB NOT NULL,
    retired_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (record_table, record_key)
);
COMMENT ON TABLE ai.retired_provider_records IS
    'Read-only historical snapshots of removed local AI data; never used for inference';

DROP TABLE IF EXISTS pg_temp.retired_local_ingestion_documents;
CREATE TEMP TABLE retired_local_ingestion_documents (
    document_id UUID PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO retired_local_ingestion_documents (document_id)
SELECT DISTINCT job.document_id
FROM ai.knowledge_ingestion_jobs job
WHERE job.status IN ('PENDING', 'PROCESSING', 'RETRYING', 'REVIEW')
    AND EXISTS (
        SELECT 1 FROM ai.knowledge_staged_embeddings staged
        JOIN ai.knowledge_chunks chunk ON chunk.chunk_id = staged.chunk_id
        WHERE staged.provider = 'LM_STUDIO' AND chunk.document_id = job.document_id)
ON CONFLICT (document_id) DO NOTHING;

-- A previous execution may already have removed the local runtime tables.
DO $$
BEGIN
    IF to_regclass('ai.runtime_mode') IS NOT NULL THEN
        INSERT INTO retired_local_ingestion_documents (document_id)
        SELECT job.document_id
        FROM ai.knowledge_ingestion_jobs job
        WHERE job.status IN ('PENDING', 'PROCESSING', 'RETRYING', 'REVIEW')
          AND EXISTS (SELECT 1 FROM ai.runtime_mode WHERE mode = 'QWEN_LOCAL')
        ON CONFLICT (document_id) DO NOTHING;
    END IF;
END;
$$;

INSERT INTO ai.retired_provider_records(record_table, record_key, payload)
SELECT 'ai_model_configurations', configuration_id::text, to_jsonb(t)
FROM ai.ai_model_configurations t WHERE provider = 'LM_STUDIO'
UNION ALL
SELECT 'ai_model_catalog', provider || ':' || model_id, to_jsonb(t)
FROM ai.ai_model_catalog t WHERE provider = 'LM_STUDIO'
UNION ALL
SELECT 'ai_runs', run_id::text, to_jsonb(t)
FROM ai.ai_runs t WHERE provider = 'LM_STUDIO'
UNION ALL
SELECT 'ai_tool_calls', tool_call_id::text, to_jsonb(t)
FROM ai.ai_tool_calls t WHERE run_id IN (SELECT run_id FROM ai.ai_runs WHERE provider = 'LM_STUDIO')
UNION ALL
SELECT 'ai_retrieval_audits', retrieval_audit_id::text, to_jsonb(t)
FROM ai.ai_retrieval_audits t WHERE run_id IN (SELECT run_id FROM ai.ai_runs WHERE provider = 'LM_STUDIO')
UNION ALL
SELECT 'ai_model_warnings', warning_id::text, to_jsonb(t)
FROM ai.ai_model_warnings t WHERE provider = 'LM_STUDIO'
UNION ALL
SELECT 'knowledge_index_versions', index_version_id::text, to_jsonb(t)
FROM ai.knowledge_index_versions t WHERE provider = 'LM_STUDIO'
UNION ALL
SELECT 'knowledge_embeddings', embedding_id::text, to_jsonb(t)
FROM ai.knowledge_embeddings t WHERE index_version_id IN (
    SELECT index_version_id FROM ai.knowledge_index_versions WHERE provider = 'LM_STUDIO')
UNION ALL
SELECT 'knowledge_staged_embeddings', staged_embedding_id::text, to_jsonb(t)
FROM ai.knowledge_staged_embeddings t WHERE provider = 'LM_STUDIO'
UNION ALL
SELECT 'assistant_jobs', job_id::text, to_jsonb(t)
FROM ai.assistant_jobs t WHERE to_jsonb(t) ->> 'runtime_mode' = 'QWEN_LOCAL'
UNION ALL
SELECT 'knowledge_ingestion_jobs', ingestion_job_id::text, to_jsonb(t)
FROM ai.knowledge_ingestion_jobs t WHERE document_id IN (SELECT document_id FROM retired_local_ingestion_documents)
UNION ALL
SELECT 'knowledge_documents', document_id::text, to_jsonb(t)
FROM ai.knowledge_documents t WHERE document_id IN (SELECT document_id FROM retired_local_ingestion_documents)
ON CONFLICT (record_table, record_key) DO NOTHING;

-- Keep the first snapshot and skip optional runtime tables already retired.
DO $$
DECLARE
    item record;
BEGIN
    FOR item IN
        SELECT * FROM (VALUES
            ('runtime_mode', 'singleton_id'),
            ('runtime_mode_events', 'event_id'),
            ('provider_http_calls', 'call_id')
        ) AS retired(table_name, key_column)
    LOOP
        IF to_regclass(format('ai.%I', item.table_name)) IS NOT NULL THEN
            EXECUTE format(
                'INSERT INTO ai.retired_provider_records (record_table, record_key, payload)
                 SELECT %L, %I::text, to_jsonb(t) FROM ai.%I t
                 ON CONFLICT (record_table, record_key) DO NOTHING',
                item.table_name, item.key_column, item.table_name
            );
        END IF;
    END LOOP;
END;
$$;

-- Never re-run pending local requests through Google without a new user request.
UPDATE ai.assistant_jobs job SET status = 'FAILED', error_code = 'AI_PROVIDER_REMOVED',
    locked_at = NULL, locked_by = NULL, lock_expires_at = NULL, updated_at = now()
WHERE to_jsonb(job) ->> 'runtime_mode' = 'QWEN_LOCAL'
    AND status IN ('PENDING', 'PROCESSING', 'RETRYING');

-- Explicitly restart these documents through the admin UI when Google processing
-- is intended; removing a local provider must not silently upload pending content.
UPDATE ai.knowledge_ingestion_jobs SET status = 'FAILED', error_code = 'AI_PROVIDER_REMOVED',
    error_message_redacted = 'Local processing retired; explicitly retry with Google',
    locked_at = NULL, locked_by = NULL, lock_expires_at = NULL, completed_at = now(), updated_at = now()
WHERE document_id IN (SELECT document_id FROM retired_local_ingestion_documents)
    AND status IN ('PENDING', 'PROCESSING', 'RETRYING', 'REVIEW');
UPDATE ai.knowledge_documents SET status = 'FAILED', failure_code = 'AI_PROVIDER_REMOVED'
WHERE document_id IN (SELECT document_id FROM retired_local_ingestion_documents)
    AND status IN ('PENDING', 'PROCESSING', 'REVIEW');

DELETE FROM ai.knowledge_embeddings WHERE index_version_id IN (
    SELECT index_version_id FROM ai.knowledge_index_versions WHERE provider = 'LM_STUDIO');
DELETE FROM ai.knowledge_staged_embeddings WHERE provider = 'LM_STUDIO';
DELETE FROM ai.knowledge_index_versions WHERE provider = 'LM_STUDIO';
-- Preserve tool audit links rendered by existing messages, but expire local
-- actions which must not remain confirmable after the provider is removed.
UPDATE ai.ai_tool_calls SET
    status = CASE WHEN status IN ('REQUESTED', 'VALIDATED', 'AWAITING_CONFIRMATION', 'EXECUTING')
        THEN 'EXPIRED' ELSE status END,
    failure_code = CASE WHEN status IN ('REQUESTED', 'VALIDATED', 'AWAITING_CONFIRMATION', 'EXECUTING')
        THEN 'AI_PROVIDER_REMOVED' ELSE failure_code END,
    run_id = NULL, updated_at = now()
WHERE run_id IN (SELECT run_id FROM ai.ai_runs WHERE provider = 'LM_STUDIO');
DELETE FROM ai.ai_runs WHERE provider = 'LM_STUDIO';
DELETE FROM ai.ai_model_warnings WHERE provider = 'LM_STUDIO';
DELETE FROM ai.ai_model_catalog WHERE provider = 'LM_STUDIO';
DELETE FROM ai.ai_model_configurations WHERE provider = 'LM_STUDIO';

-- Restore Google-only provider constraints; no local vector is relabeled Google.
ALTER TABLE ai.ai_model_configurations DROP CONSTRAINT IF EXISTS ck_ai_model_config_provider,
    ADD CONSTRAINT ck_ai_model_config_provider CHECK (provider = 'GEMINI');
ALTER TABLE ai.ai_model_catalog DROP CONSTRAINT IF EXISTS ck_ai_model_catalog_provider,
    ADD CONSTRAINT ck_ai_model_catalog_provider CHECK (provider = 'GEMINI');
ALTER TABLE ai.ai_runs DROP CONSTRAINT IF EXISTS ck_ai_runs_provider,
    ADD CONSTRAINT ck_ai_runs_provider CHECK (provider = 'GEMINI');
ALTER TABLE ai.ai_model_warnings DROP CONSTRAINT IF EXISTS ck_ai_model_warnings_provider,
    ADD CONSTRAINT ck_ai_model_warnings_provider CHECK (provider = 'GEMINI');
ALTER TABLE ai.knowledge_index_versions DROP CONSTRAINT IF EXISTS ck_knowledge_index_versions_provider,
    ADD CONSTRAINT ck_knowledge_index_versions_provider CHECK (provider = 'GEMINI');
ALTER TABLE ai.knowledge_staged_embeddings DROP CONSTRAINT IF EXISTS ck_knowledge_staged_provider,
    ADD CONSTRAINT ck_knowledge_staged_provider CHECK (provider = 'GEMINI');

DROP TRIGGER IF EXISTS stamp_assistant_job_runtime ON ai.assistant_jobs;
DROP FUNCTION IF EXISTS ai.stamp_assistant_job_runtime();
ALTER TABLE ai.assistant_jobs DROP COLUMN IF EXISTS runtime_mode, DROP COLUMN IF EXISTS runtime_epoch;
DROP TABLE IF EXISTS ai.runtime_mode_events;
DROP TABLE IF EXISTS ai.provider_http_calls;
DROP TABLE IF EXISTS ai.runtime_mode;
