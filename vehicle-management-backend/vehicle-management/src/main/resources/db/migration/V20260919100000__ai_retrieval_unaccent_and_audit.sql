-- Phase 5: Vietnamese unaccent lexical search, retrieval audit and citation hardening.
--
-- ADR (why this shape):
-- 1. PostgreSQL generated columns require IMMUTABLE expressions, but the stock
--    public.unaccent() is only STABLE, so the existing search_vector column cannot
--    be rebuilt on top of unaccent(). Instead we add a trigger-maintained
--    search_vector_unaccent column fed by ai.immutable_unaccent(). The old column
--    stays untouched for backward compatibility.
-- 2. ai.immutable_unaccent() prefers public.unaccent() when the extension is
--    installed and falls back to an explicit Vietnamese translate() mapping when
--    it is not. Migration-time CREATE EXTENSION is best-effort so fresh/test
--    databases keep migrating; production fail-fast is enforced at application
--    startup by RetrievalProperties.unaccent-required (AI_RETRIEVAL_UNACCENT_REQUIRED=true).
-- 3. effective_to is EXCLUSIVE (effective_to IS NULL OR effective_to > now()):
--    a document stops being retrievable at the exact instant its validity ends.
-- 4. Audit/citation changes are purely additive; existing columns are never altered.

-- Best-effort extension install (superuser may be unavailable in test environments).
DO $$
BEGIN
    BEGIN
        CREATE EXTENSION IF NOT EXISTS unaccent;
    EXCEPTION
        WHEN OTHERS THEN
            RAISE NOTICE 'unaccent extension unavailable; ai.immutable_unaccent() will use the Vietnamese translate() fallback';
    END;
END $$;

-- Immutable wrapper: unaccent when available, explicit Vietnamese mapping otherwise.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'unaccent') THEN
        EXECUTE $func$
            CREATE OR REPLACE FUNCTION ai.immutable_unaccent(input_text TEXT)
            RETURNS TEXT
            LANGUAGE sql
            IMMUTABLE
            PARALLEL SAFE
            AS $body$
                SELECT public.unaccent('unaccent', COALESCE(input_text, ''))
            $body$;
        $func$;
    ELSE
        EXECUTE $func$
            CREATE OR REPLACE FUNCTION ai.immutable_unaccent(input_text TEXT)
            RETURNS TEXT
            LANGUAGE sql
            IMMUTABLE
            PARALLEL SAFE
            AS $body$
                SELECT translate(COALESCE(input_text, ''),
                    'àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ',
                    'aaaaaaaaaaaaaaaaaeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyydAAAAAAAAAAAAAAAAAEEEEEEEEEEEIIIIIOOOOOOOOOOOOOOOUUUUUUUUUUUYYYYYD')
            $body$;
        $func$;
    END IF;
END $$;

-- Trigger-maintained unaccent search vector (old generated column is left untouched).
ALTER TABLE ai.knowledge_chunks
    ADD COLUMN IF NOT EXISTS search_vector_unaccent TSVECTOR;

CREATE OR REPLACE FUNCTION ai.trg_knowledge_chunks_search_vector_unaccent()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $trigger$
BEGIN
    NEW.search_vector_unaccent := to_tsvector('simple', ai.immutable_unaccent(
        COALESCE(NEW.title, '') || ' ' || COALESCE(NEW.content, '') || ' ' || COALESCE(NEW.summary, '')));
    RETURN NEW;
END;
$trigger$;

DROP TRIGGER IF EXISTS trg_knowledge_chunks_search_vector_unaccent ON ai.knowledge_chunks;

CREATE TRIGGER trg_knowledge_chunks_search_vector_unaccent
    BEFORE INSERT OR UPDATE OF title, content, summary ON ai.knowledge_chunks
    FOR EACH ROW EXECUTE FUNCTION ai.trg_knowledge_chunks_search_vector_unaccent();

-- Backfill existing rows (batched by primary key order is unnecessary for admin-scale corpora).
UPDATE ai.knowledge_chunks
SET search_vector_unaccent = to_tsvector('simple', ai.immutable_unaccent(
    COALESCE(title, '') || ' ' || COALESCE(content, '') || ' ' || COALESCE(summary, '')))
WHERE search_vector_unaccent IS NULL;

CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_search_unaccent
    ON ai.knowledge_chunks USING GIN (search_vector_unaccent);

-- Retrieval audit: additive columns only.
ALTER TABLE ai.ai_retrieval_audits
    ADD COLUMN IF NOT EXISTS output_message_id UUID REFERENCES operations.chat_messages(message_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS tenant_id UUID,
    ADD COLUMN IF NOT EXISTS normalized_query_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS active_index_version_id UUID,
    ADD COLUMN IF NOT EXISTS retrieval_policy_version VARCHAR(60),
    ADD COLUMN IF NOT EXISTS threshold_version VARCHAR(60),
    ADD COLUMN IF NOT EXISTS access_scopes TEXT,
    ADD COLUMN IF NOT EXISTS vector_candidate_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS lexical_candidate_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fused_result_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS final_result_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS top_k INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS max_chunks_per_document INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS grounded_confidence NUMERIC(4, 3),
    ADD COLUMN IF NOT EXISTS diagnostic_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS latency_ms BIGINT;

-- Per-result audit trace.
CREATE TABLE IF NOT EXISTS ai.ai_retrieval_audit_results (
    audit_result_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    retrieval_audit_id UUID NOT NULL REFERENCES ai.ai_retrieval_audits(retrieval_audit_id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES ai.knowledge_documents(document_id) ON DELETE CASCADE,
    chunk_id UUID NOT NULL REFERENCES ai.knowledge_chunks(chunk_id) ON DELETE CASCADE,
    vector_rank INTEGER,
    lexical_rank INTEGER,
    vector_score NUMERIC(8, 5),
    lexical_score NUMERIC(8, 5),
    fused_score NUMERIC(12, 8),
    final_rank INTEGER,
    selected_for_context BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_ai_retrieval_audit_results_audit_chunk UNIQUE (retrieval_audit_id, chunk_id)
);

CREATE INDEX IF NOT EXISTS idx_ai_retrieval_audit_results_audit
    ON ai.ai_retrieval_audit_results (retrieval_audit_id);

-- Citations: bind to retrieval audit + index version, order, uniqueness.
ALTER TABLE ai.ai_message_citations
    ADD COLUMN IF NOT EXISTS retrieval_audit_id UUID REFERENCES ai.ai_retrieval_audits(retrieval_audit_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS index_version_id UUID,
    ADD COLUMN IF NOT EXISTS citation_order INTEGER NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_message_citations_message_chunk
    ON ai.ai_message_citations (message_id, chunk_id);

CREATE INDEX IF NOT EXISTS idx_ai_message_citations_retrieval_audit
    ON ai.ai_message_citations (retrieval_audit_id);
