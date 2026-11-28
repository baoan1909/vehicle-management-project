-- No corpus import, publication, model activation or index activation.
ALTER TABLE ai.ai_retrieval_audits
    ADD COLUMN IF NOT EXISTS retrieval_method VARCHAR(40) NOT NULL DEFAULT 'HYBRID',
    ADD COLUMN IF NOT EXISTS source_snapshot TEXT;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'ai.ai_retrieval_audits'::regclass
          AND conname = 'ck_retrieval_method'
    ) THEN
        ALTER TABLE ai.ai_retrieval_audits
            ADD CONSTRAINT ck_retrieval_method CHECK (retrieval_method IN ('HYBRID','COMMON_FAQ','COMMON_CONFLICT'));
    END IF;
END;
$$;
-- The shared path uses the existing knowledge_chunks.search_vector_unaccent GIN index.
-- Reviewed/effective/latest-document and source/chunk permission filters remain mandatory.
