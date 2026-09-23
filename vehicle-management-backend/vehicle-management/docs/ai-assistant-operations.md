# CoParking AI Assistant Operations

## Runtime switches

- `AI_ASSISTANT_ENABLED=true` enables assistant job processing.
- `GEMINI_API_KEY` is required for Gemini calls and model catalog sync.
- `GEMINI_BASE_URL` defaults to `https://generativelanguage.googleapis.com`.
- `AI_GEMINI_DATA_MODE=UNPAID|PAID` controls provider data mode policy.
- `AI_ALLOW_UNPAID_SUPPORT_DATA=true` is required before UNPAID Gemini configs can use support data.
- `AI_ASSISTANT_RECENT_MESSAGE_LIMIT`, `AI_ASSISTANT_MAX_ATTEMPTS`,
  `AI_ASSISTANT_RETRY_INITIAL_DELAY`, and `AI_ASSISTANT_REQUEST_TIMEOUT` tune the worker.
- `AI_MODEL_CATALOG_SYNC_FIXED_DELAY_MS` and `AI_MODEL_CATALOG_SYNC_INITIAL_DELAY_MS`
  tune scheduled catalog sync.

## Database changes

`V20260908100000__ai_tool_call_rag_admin_runtime.sql` adds the runtime tables and columns for:

- `ai.ai_tool_calls` lifecycle persistence, idempotency, ownership, expiry, optimistic locking, and redacted payloads.
- RAG storage: `ai.knowledge_sources`, `ai.knowledge_documents`, `ai.knowledge_chunks`,
  `ai.knowledge_embeddings`, `ai.knowledge_ingestion_jobs`, retrieval audits, and citations.
- Admin warnings: `ai.ai_model_warnings`.
- `ai.ai_runs.configuration_id`, `rollout_version`, and `attempt_number`.
- New message/job statuses: `ASSISTANT_TEXT`, `TOOL_RESULT`, `WAITING_CONFIRMATION`, and `EXPIRED`.

`V20260908101000__seed_default_support_knowledge.sql` seeds a small public support knowledge base.

The migration attempts `CREATE EXTENSION vector`; if the local database does not provide pgvector,
it falls back to `REAL[]` embeddings so tests and basic keyword retrieval keep working. Production
vector similarity should install pgvector and add a follow-up migration to move `REAL[]` data to
the native `vector(768)` type.

## Tool calling flow

The orchestrator sends Gemini function declarations from `AiToolRegistry`.

- Read-only tools execute in the worker and can feed a function response back to Gemini.
- Write tools persist an `AWAITING_CONFIRMATION` row in `ai.ai_tool_calls` and publish an
  `ACTION_CARD` chat message.
- User confirmation calls `POST /api/ai/tool-calls/{toolCallId}/confirm`.
- User denial calls `POST /api/ai/tool-calls/{toolCallId}/deny`.
- Both paths publish a `TOOL_RESULT` message and keep the frozen redacted arguments for audit.

Implemented request-time tools include support knowledge search, ticket lookup/listing,
subscription status lookup, and customer support ticket creation. Additional write operations such
as escalation, reassignment, or handoff should be added to `AiToolRegistry` only after their
dedicated business use cases are wired to `AiToolExecutionService`.

## Admin API

Admin routes are guarded by the new permissions seeded for the system administrator role.

- `GET /api/ai/models/configurations`
- `PATCH /api/ai/models/configurations/{configurationId}/status`
- `PATCH /api/ai/models/configurations/{configurationId}/rollout`
- `GET /api/ai/models/catalog`
- `POST /api/ai/models/catalog/sync`
- `GET /api/ai/models/warnings`

Rollout selection is deterministic by conversation/account/use-case/version and requires ACTIVE
configurations for the use case to sum to 100 percent. Fallback models are still appended after
the selected active model.

## Verification

Run backend:

```powershell
.\mvnw.cmd test
```

Run frontend:

```powershell
cmd /c npm run build
```
