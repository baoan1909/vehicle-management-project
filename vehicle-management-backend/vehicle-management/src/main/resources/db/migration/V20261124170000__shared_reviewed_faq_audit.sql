-- No corpus import, publication, model activation or index activation.
ALTER TABLE ai.ai_retrieval_audits
    ADD COLUMN retrieval_method VARCHAR(40) NOT NULL DEFAULT 'HYBRID',
    ADD COLUMN source_snapshot TEXT;
ALTER TABLE ai.ai_retrieval_audits
    ADD CONSTRAINT ck_retrieval_method CHECK (retrieval_method IN ('HYBRID','COMMON_FAQ','COMMON_CONFLICT'));
-- The shared path uses the existing knowledge_chunks.search_vector_unaccent GIN index.
-- Reviewed/effective/latest-document and source/chunk permission filters remain mandatory.
