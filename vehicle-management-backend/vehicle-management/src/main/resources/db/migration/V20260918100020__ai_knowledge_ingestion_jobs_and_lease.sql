-- Phase 3: ingestion job stages, worker lease and job event trail.
-- The worker claims jobs with FOR UPDATE SKIP LOCKED and renews a lease via heartbeat;
-- one open job per document prevents concurrent reprocessing of the same upload.

ALTER TABLE ai.knowledge_ingestion_jobs
    ADD COLUMN IF NOT EXISTS current_stage VARCHAR(32),
    ADD COLUMN IF NOT EXISTS progress_percent INTEGER,
    ADD COLUMN IF NOT EXISTS locked_by VARCHAR(80),
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS lock_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_heartbeat_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS max_attempts INTEGER NOT NULL DEFAULT 3,
    ADD COLUMN IF NOT EXISTS last_completed_stage VARCHAR(32),
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS error_message_redacted VARCHAR(500),
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS requested_by UUID REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS embedding_fingerprint VARCHAR(64);

-- Job status is the lifecycle; the extraction/embedding progress lives in current_stage.
ALTER TABLE ai.knowledge_ingestion_jobs
    DROP CONSTRAINT IF EXISTS ck_knowledge_ingestion_jobs_status;

ALTER TABLE ai.knowledge_ingestion_jobs
    ADD CONSTRAINT ck_knowledge_ingestion_jobs_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'REVIEW', 'READY', 'RETRYING', 'FAILED')
    );

ALTER TABLE ai.knowledge_ingestion_jobs
    ADD CONSTRAINT ck_knowledge_ingestion_jobs_progress CHECK (
        progress_percent IS NULL OR (progress_percent >= 0 AND progress_percent <= 100)
    );

-- Due-job queue used by the claim query.
CREATE INDEX IF NOT EXISTS idx_knowledge_ingestion_jobs_due
    ON ai.knowledge_ingestion_jobs (status, next_attempt_at, lock_expires_at, created_at);

CREATE INDEX IF NOT EXISTS idx_knowledge_ingestion_jobs_document
    ON ai.knowledge_ingestion_jobs (document_id, created_at DESC);

-- A document may have only one open job, including while it is waiting for review.
CREATE UNIQUE INDEX IF NOT EXISTS uq_knowledge_ingestion_jobs_open_document
    ON ai.knowledge_ingestion_jobs (document_id)
    WHERE status IN ('PENDING', 'PROCESSING', 'RETRYING', 'REVIEW');

-- Idempotency protection is enforced at the database level for retry-safe uploads.
CREATE UNIQUE INDEX IF NOT EXISTS uq_knowledge_ingestion_jobs_idempotency
    ON ai.knowledge_ingestion_jobs (requested_by, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE ai.knowledge_ingestion_job_events (
    event_id BIGSERIAL PRIMARY KEY,
    ingestion_job_id UUID NOT NULL REFERENCES ai.knowledge_ingestion_jobs(ingestion_job_id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    stage VARCHAR(32),
    detail VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_knowledge_ingestion_job_events_job
    ON ai.knowledge_ingestion_job_events (ingestion_job_id, created_at);