-- Phase 3: knowledge source metadata and logical document identity.
-- Adds the physical document metadata written at upload time plus the columns that
-- support the review workflow. All changes are additive; the running migrations are
-- never modified.

ALTER TABLE ai.knowledge_sources
    ADD COLUMN IF NOT EXISTS description VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by_account_id UUID;

-- Logical document identity: document_key groups document versions of the same
-- logical document; supersedes_document_id points to the row it replaced.
ALTER TABLE ai.knowledge_documents
    ADD COLUMN IF NOT EXISTS document_key UUID,
    ADD COLUMN IF NOT EXISTS supersedes_document_id UUID,
    ADD COLUMN IF NOT EXISTS original_filename VARCHAR(255),
    ADD COLUMN IF NOT EXISTS file_extension VARCHAR(32),
    ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT,
    ADD COLUMN IF NOT EXISTS failure_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS effective_from TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS effective_to TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reviewed_by UUID,
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by UUID;

-- Backfill existing rows before applying NOT NULL.
UPDATE ai.knowledge_documents
SET document_key = document_id
WHERE document_key IS NULL;

ALTER TABLE ai.knowledge_documents
    ALTER COLUMN document_key SET NOT NULL;

-- Document workflow now includes the REVIEW state (chờ duyệt).
ALTER TABLE ai.knowledge_documents
    DROP CONSTRAINT IF EXISTS ck_knowledge_documents_status;

ALTER TABLE ai.knowledge_documents
    ADD CONSTRAINT ck_knowledge_documents_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'REVIEW', 'READY', 'FAILED', 'ARCHIVED')
    );

-- One logical document carries at most one row per version.
CREATE UNIQUE INDEX IF NOT EXISTS uq_knowledge_documents_logical_version
    ON ai.knowledge_documents (document_key, document_version);

-- Idempotent upload: the same source never carries two documents with the same
-- content checksum. Conflict handlers read the existing row instead of duplicating it.
CREATE UNIQUE INDEX IF NOT EXISTS uq_knowledge_documents_source_checksum
    ON ai.knowledge_documents (source_id, checksum_sha256)
    WHERE checksum_sha256 IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_knowledge_documents_list
    ON ai.knowledge_documents (source_id, status, created_at DESC);