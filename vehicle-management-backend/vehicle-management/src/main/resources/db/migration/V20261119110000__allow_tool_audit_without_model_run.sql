-- Deterministic own-data reads still produce tool audits but do not create a model run.
-- Existing run references, ownership columns and idempotency constraints stay intact.
ALTER TABLE ai.ai_tool_calls
    ALTER COLUMN run_id DROP NOT NULL;
