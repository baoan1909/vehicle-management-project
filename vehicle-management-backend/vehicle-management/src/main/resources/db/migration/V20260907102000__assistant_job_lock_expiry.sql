ALTER TABLE ai.assistant_jobs
    ADD COLUMN locked_by VARCHAR(80),
    ADD COLUMN lock_expires_at TIMESTAMPTZ;

CREATE INDEX idx_assistant_jobs_expired_processing
    ON ai.assistant_jobs (lock_expires_at, created_at)
    WHERE status = 'PROCESSING';
