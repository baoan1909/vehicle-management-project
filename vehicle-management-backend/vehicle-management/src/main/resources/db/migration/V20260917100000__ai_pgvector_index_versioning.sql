-- Phase 2: embedding index versioning on pgvector.
-- Fail fast if the pgvector extension is not installed (no fallback to REAL[]).
CREATE EXTENSION IF NOT EXISTS vector;

DO $$
DECLARE
    installed_version TEXT;
BEGIN
    SELECT extversion INTO installed_version
    FROM pg_extension
    WHERE extname = 'vector';

    IF installed_version IS NULL THEN
        RAISE EXCEPTION 'pgvector extension is required';
    END IF;
    IF string_to_array(installed_version, '.')::INTEGER[] < ARRAY[0, 8, 0] THEN
        RAISE EXCEPTION 'pgvector 0.8.0 or newer is required; installed version is %', installed_version;
    END IF;
END $$;

-- Embedding model configurations carry an explicit output dimension.
ALTER TABLE ai.ai_model_configurations
    ADD COLUMN IF NOT EXISTS output_dimension INTEGER;

ALTER TABLE ai.ai_model_configurations
    DROP CONSTRAINT IF EXISTS ck_ai_model_config_embedding_dimension,
    DROP CONSTRAINT IF EXISTS ck_ai_model_config_output_dimension;

ALTER TABLE ai.ai_model_configurations
    ADD CONSTRAINT ck_ai_model_config_output_dimension
        CHECK (output_dimension IS NULL OR output_dimension = 768),
    ADD CONSTRAINT ck_ai_model_config_embedding_dimension
        CHECK (use_case <> 'EMBEDDING' OR output_dimension IS NOT NULL);

-- A knowledge index version snapshots exactly one embedding provider/model/dimension
-- and owns every embedding row created under that fingerprint.
CREATE TABLE ai.knowledge_index_versions (
    index_version_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_code VARCHAR(120) NOT NULL,
    model_configuration_id UUID NOT NULL REFERENCES ai.ai_model_configurations(configuration_id) ON DELETE RESTRICT,
    provider VARCHAR(30) NOT NULL,
    model_id VARCHAR(120) NOT NULL,
    dimension INTEGER NOT NULL,
    chunker_version VARCHAR(60) NOT NULL,
    embedding_prompt_version VARCHAR(60) NOT NULL,
    distance_metric VARCHAR(20) NOT NULL,
    normalization VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expected_chunk_count INTEGER NOT NULL DEFAULT 0,
    embedded_chunk_count INTEGER NOT NULL DEFAULT 0,
    failed_chunk_count INTEGER NOT NULL DEFAULT 0,
    content_checksum VARCHAR(64),
    failure_code VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    ready_at TIMESTAMPTZ,
    activated_at TIMESTAMPTZ,
    activated_by UUID,
    retired_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    updated_by UUID,
    build_lease_id UUID,
    build_lease_until TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_knowledge_index_versions_code UNIQUE (version_code),
    CONSTRAINT ck_knowledge_index_versions_provider CHECK (provider IN ('GEMINI')),
    CONSTRAINT ck_knowledge_index_versions_status CHECK (status IN ('DRAFT', 'BUILDING', 'READY', 'ACTIVE', 'RETIRED', 'FAILED')),
    CONSTRAINT ck_knowledge_index_versions_dimension CHECK (dimension = 768),
    CONSTRAINT ck_knowledge_index_versions_distance CHECK (distance_metric IN ('COSINE', 'L2', 'IP')),
    CONSTRAINT ck_knowledge_index_versions_normalization CHECK (normalization IN ('NONE', 'L2')),
    CONSTRAINT ck_knowledge_index_versions_counts CHECK (expected_chunk_count >= 0 AND embedded_chunk_count >= 0 AND failed_chunk_count >= 0)
);

CREATE UNIQUE INDEX uq_knowledge_index_versions_single_active
    ON ai.knowledge_index_versions (status) WHERE status = 'ACTIVE';

CREATE INDEX idx_knowledge_index_versions_lookup
    ON ai.knowledge_index_versions (status, created_at);

CREATE TRIGGER trg_knowledge_index_versions_set_updated_at
    BEFORE UPDATE ON ai.knowledge_index_versions
    FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- Move the embeddings storage from REAL[] to vector(768) and bind every row to an
-- index version. Destructive implicit migration is forbidden: a database containing
-- legacy rows must first run the documented explicit backfill procedure.
ALTER TABLE ai.knowledge_embeddings
    ADD COLUMN IF NOT EXISTS index_version_id UUID;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM ai.knowledge_embeddings
        WHERE embedding IS NULL OR index_version_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Legacy knowledge embeddings require explicit index-version backfill before pgvector migration';
    END IF;
END $$;

ALTER TABLE ai.knowledge_embeddings
    DROP CONSTRAINT IF EXISTS ck_knowledge_embeddings_vector_length,
    ALTER COLUMN embedding TYPE vector(768) USING array_to_vector(embedding, 768, true),
    ALTER COLUMN embedding SET NOT NULL,
    ALTER COLUMN index_version_id SET NOT NULL;

ALTER TABLE ai.knowledge_embeddings
    ADD CONSTRAINT fk_knowledge_embeddings_index_version
        FOREIGN KEY (index_version_id) REFERENCES ai.knowledge_index_versions(index_version_id) ON DELETE RESTRICT;

CREATE INDEX idx_knowledge_embeddings_index_version
    ON ai.knowledge_embeddings (index_version_id);

CREATE UNIQUE INDEX uq_knowledge_embeddings_chunk_index
    ON ai.knowledge_embeddings (chunk_id, index_version_id);

CREATE INDEX idx_knowledge_embeddings_hnsw_cosine
    ON ai.knowledge_embeddings USING hnsw (embedding vector_cosine_ops);
