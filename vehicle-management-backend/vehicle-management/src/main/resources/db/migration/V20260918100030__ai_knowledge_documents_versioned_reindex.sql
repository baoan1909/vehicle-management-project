-- Phase 3/4: safe reindex with immutable document version history.
-- Reindexing must NOT mutate the row and chunks already published into the ACTIVE
-- corpus. It creates a new document row (same logical document_key, version + 1,
-- supersedes_document_id -> old row) so the old READY chunks stay untouched until the
-- new version is approved. To allow a second row with the same content checksum, the
-- uniqueness scope of an uploaded file is widened to (source_id, checksum, version)
-- instead of (source_id, checksum) alone.

DROP INDEX IF EXISTS ai.uq_knowledge_documents_source_checksum;

CREATE UNIQUE INDEX IF NOT EXISTS uq_knowledge_documents_source_checksum_version
    ON ai.knowledge_documents (source_id, checksum_sha256, document_version)
    WHERE checksum_sha256 IS NOT NULL;