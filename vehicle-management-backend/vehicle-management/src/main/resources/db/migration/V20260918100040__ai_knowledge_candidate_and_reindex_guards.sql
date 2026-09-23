-- Phase 3/4 hardening: only one non-terminal knowledge index candidate may exist.
-- Application advisory locks serialize candidate creation; this partial unique index
-- is the final database invariant for concurrent nodes and operational scripts.
-- Preserve the newest pending candidate when upgrading a database that already has
-- duplicates; older candidates can no longer be built safely against a moving corpus.
WITH ranked_pending AS (
    SELECT index_version_id,
           ROW_NUMBER() OVER (ORDER BY COALESCE(updated_at, created_at) DESC, index_version_id DESC) AS row_number
    FROM ai.knowledge_index_versions
    WHERE status IN ('DRAFT', 'BUILDING')
)
UPDATE ai.knowledge_index_versions AS index_version
SET status = 'FAILED',
    failure_code = 'DUPLICATE_PENDING_CANDIDATE_MIGRATION',
    build_lease_id = NULL,
    build_lease_until = NULL,
    updated_at = now()
FROM ranked_pending
WHERE index_version.index_version_id = ranked_pending.index_version_id
  AND ranked_pending.row_number > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_knowledge_index_versions_single_pending
    ON ai.knowledge_index_versions ((1))
    WHERE status IN ('DRAFT', 'BUILDING');
