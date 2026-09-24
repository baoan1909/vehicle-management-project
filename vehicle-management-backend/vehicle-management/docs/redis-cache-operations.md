# Redis Cache Operations

Redis is strictly an optimization and resilience layer for the assistant RAG pipeline.
PostgreSQL + pgvector remains the source of truth; Gemini remains the generator.

## Run Redis alone (no impact on PostgreSQL)

```bash
docker compose -f docker-compose.redis.yml up -d
docker compose -f docker-compose.redis.yml ps
docker exec vehicle-redis-cache redis-cli ping  # PONG
```

Backend connects via `REDIS_HOST` / `REDIS_PORT` (defaults `localhost:6379`).
No password is stored in compose; set `REDIS_PASSWORD` in the runtime environment when ACL is enabled.

## Enable / disable

```bash
REDIS_ENABLED=true   # full cache + distributed lock + Redis circuit state
REDIS_ENABLED=false  # no cache read/write, no lock acquire, local circuit fallback only
AI_CACHE_ENABLED=false  # same fail-open effect for AI caches
```

Disable path: chatbot keeps using PostgreSQL hybrid retrieval and Gemini directly.
No database rollback is needed; cached data simply expires.

## Rollback

1. Set `REDIS_ENABLED=false` and restart backend.
2. Stop Redis if needed: `docker compose -f docker-compose.redis.yml down`.
3. Messages, citations, retrieval audits and AI runs stay in PostgreSQL untouched.

## Health

`GET /actuator/health` includes contributor `redisCache`:

- `enabled: false` + `mode: disabled` + `degraded: true` when Redis is off.
- `reachable: true/false`, `latencyMs`, `mode: full|degraded`, `degraded: true/false` otherwise.
- No host, password, key or value is ever exposed.

## Key design (server-generated, HMAC)

Prefix: `vm:{env}:{service}:v1:{namespace}:...`

- `embedding-query:{provider}:{model}:{dim}:{promptVer}:{normVer}:{queryHmac}`
- `knowledge-retrieval:{indexId}:{checksum}:{policy}:{threshold}:{scopeFp}:{tenantFp}:{topK}:{queryHmac}`
- `grounded-answer:{indexId}:{checksum}:{promptVer}:{genFp}:{scopeFp}:{tenantFp}:{lang}:{queryHmac}`
- `ai-circuit-breaker:{provider}:{useCase}:{configurationId}`
- `distributed-lock:grounded-answer:{sha256(groundedAnswerKey)}`

Raw queries never appear in keys. `AI_CACHE_KEY_HMAC_SECRET` must be set outside
source code; without it all caches fail open (miss) and production should warn.

## TTL (fixed + +-10% jitter, no sliding)

- Query embedding: 24h (safe queries only).
- Retrieval: PUBLIC 15m / CUSTOMER 10m / TENANT_PRIVATE 5m / negative 30s.
- Grounded answer: PUBLIC 15m / CUSTOMER 5m / TENANT_PRIVATE 5m.
- Single-flight lock: 45s; contender wait/re-read up to 2s.

## Safety

- No PII, write actions, business tool results, action cards or secrets are cached.
- Every cache hit still persists fresh PostgreSQL message/citation/audit rows with new IDs.
- Invalid payloads (schema, size, dimension, checksum, scope/tenant, citations) are deleted and treated as miss.
- Never run `FLUSHALL`/`FLUSHDB` from an API; future eviction must be per namespace/version with its own permission.
