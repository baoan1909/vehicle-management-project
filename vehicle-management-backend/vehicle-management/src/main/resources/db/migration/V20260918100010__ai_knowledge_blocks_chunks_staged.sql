-- Phase 3: extracted document blocks, chunk metadata and staged embeddings.
-- Staged embeddings are written while the document is being reviewed for publication,
-- keyed by a content fingerprint so the index build can reuse them and never mixes
-- vectors across providers/models.

CREATE TABLE ai.knowledge_document_blocks (
    block_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES ai.knowledge_documents(document_id) ON DELETE CASCADE,
    doc_version INTEGER NOT NULL DEFAULT 1,
    block_index INTEGER NOT NULL,
    kind VARCHAR(32) NOT NULL,
    heading_path VARCHAR(500),
    source_page INTEGER,
    source_section VARCHAR(200),
    content TEXT NOT NULL,
    hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_knowledge_document_blocks_kind CHECK (kind IN ('HEADING', 'PARAGRAPH', 'TABLE', 'LIST', 'PAGE_MARKER')),
    CONSTRAINT uq_knowledge_document_blocks_version_index UNIQUE (document_id, doc_version, block_index)
);

ALTER TABLE ai.knowledge_chunks
    ADD COLUMN IF NOT EXISTS token_count INTEGER,
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS heading_path VARCHAR(500),
    ADD COLUMN IF NOT EXISTS start_block_index INTEGER,
    ADD COLUMN IF NOT EXISTS end_block_index INTEGER,
    ADD COLUMN IF NOT EXISTS chunker_version VARCHAR(64);

-- Chunks start as DRAFT, become READY on publish and are archived with their document.
ALTER TABLE ai.knowledge_chunks
    DROP CONSTRAINT IF EXISTS ck_knowledge_chunks_status;

ALTER TABLE ai.knowledge_chunks
    ADD CONSTRAINT ck_knowledge_chunks_status CHECK (status IN ('DRAFT', 'READY', 'ARCHIVED'));

-- Idempotent chunk upserts: one chunk per document version and index.
CREATE UNIQUE INDEX IF NOT EXISTS uq_knowledge_chunks_document_version_index
    ON ai.knowledge_chunks (document_id, document_version, chunk_index);

CREATE TABLE ai.knowledge_staged_embeddings (
    staged_embedding_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chunk_id UUID NOT NULL REFERENCES ai.knowledge_chunks(chunk_id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL,
    model_configuration_id UUID NOT NULL REFERENCES ai.ai_model_configurations(configuration_id) ON DELETE RESTRICT,
    model_id VARCHAR(120) NOT NULL,
    embedding_dimension INTEGER NOT NULL,
    embedding_prompt_version VARCHAR(60) NOT NULL,
    chunker_version VARCHAR(60) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding_fingerprint CHAR(64) NOT NULL,
    embedding vector(768) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_knowledge_staged_provider CHECK (provider IN ('GEMINI')),
    CONSTRAINT ck_knowledge_staged_dimension CHECK (embedding_dimension = 768),
    CONSTRAINT uq_knowledge_staged_fingerprint UNIQUE (chunk_id, embedding_fingerprint)
);

CREATE INDEX idx_knowledge_staged_chunk_created
    ON ai.knowledge_staged_embeddings (chunk_id, created_at DESC);