# Parking location Phase 8 readiness assessment

Assessment date: 2026-10-03. Scope: Phases 2–8 for administrative addresses, PostGIS parking locations, public nearby search, customer map, quota/security/observability, and rollout safety.

## Executive decision

The implementation is safe to deploy **with all four feature flags disabled**. It is **not yet approved for general production enablement** because the production-like k6 run, browser E2E/manual device matrix, and backup/restore drill require a staging environment and operator evidence.

## Automated evidence completed

- Backend full regression: 1,367 tests, 0 failures, 0 errors, 0 skipped.
- Fresh Testcontainers database: project PostgreSQL 17 image started successfully; all 79 Flyway migrations applied from an empty database.
- Fresh database assertions: vector/PostGIS extensions, reference counts, FK rejection, trigger-derived SRID 4326 location, `ST_DWithin`, and partial GiST index selection all passed.
- Local Docker data gate: passed. Current counts are 34 provinces, 3,321 wards, 3,321 GIS wards; legacy counts are 63 provinces, 696 districts, and 10,035 wards. There were no orphan references or failed Flyway migrations.
- API security tests: public feature status, unauthenticated admin 401, unauthorized admin 403, numeric injection payload 400, and rate-limit 429 passed.
- Unit security/business tests: coordinate/radius/limit validation, current/legacy address rules, permission checks, cross-organization update rejection, quota concurrency, SSRF allowlist, feature-disabled 503 structure, and public DTO non-disclosure passed.
- Frontend: TypeScript/Vite production build passed; static contract suite for explicit geolocation permission, privacy, fallback, accessibility labels, feature gating, error/empty/loading states, and Google Maps direction parameters passed.
- Local spatial benchmark from Phase 7 selected GiST at 1k/10k/100k rows; measured 100k execution time was 211.552 ms.

## Requirement matrix

| Area | Status | Evidence / remaining work |
|---|---|---|
| Coordinate, radius, limit, address and permission unit tests | Pass | Automated unit suite |
| Daily quota calculation/concurrency | Pass | Atomic quota guard tests; API 429 test |
| Google Maps URL builder | Pass with limitation | Static frontend contract verifies origin, destination, `api=1`, and driving mode; browser navigation E2E remains |
| Flyway from empty PostgreSQL + PostGIS + pgvector | Pass | `SpatialFreshDatabaseContainerTest` |
| Seed counts, metadata and FK integrity | Pass | Fresh container plus operational data gate |
| `ST_DWithin`, trigger, SRID and GiST | Pass | Fresh container and existing PostGIS integration tests |
| Administrative public APIs | Pass | Existing integration suite |
| Create/update/geocode/manual confirmation | Pass | Unit/integration coverage; real-provider staging smoke test remains |
| Nearby boundaries/order/inactive/null/location/public DTO | Pass | Existing integration suite |
| 401/403, IDOR, injection, abuse limits | Pass | Unit and MockMvc integration tests |
| SSRF/provider response bound/API-key isolation | Pass with limitation | HTTPS hostname allowlist, redirects off, bounded stream, backend-only config; oversized live response is code-reviewed but not exercised against a fake HTTP server |
| Map still usable as a list when tiles fail | Pass with limitation | UI error boundary and static contract; browser E2E remains |
| GPS allow/deny/timeout/unsupported/low accuracy | Pass with limitation | UI branches and telemetry contract; real browser/mobile matrix remains |
| Log privacy | Pass with operational dependency | Application emits outcome/coarse metrics, not exact coordinates; staging log capture must be inspected under load |
| Production backup and restore | Pending | Must be executed by deployment operator in isolated staging |
| Production-like HTTP capacity | Pending | k6 workload exists; run against staging and attach P95/P99/error evidence |
| Progressive rollout and rollback drill | Pending | Runbook and runtime flags exist; staging exercise still required |

## Cross-phase findings

### Phase 2 — database infrastructure

Ready. The combined image supports pgvector and PostGIS without removing AI vector functionality. Extension ownership remains a migration concern; the runtime account does not need installation privileges.

### Phase 3 — administrative data

Ready. The source tag `v2.4.1` contains **10,035** legacy wards, not 10,040. Migration metadata, validation and automated tests intentionally use the source-truth value 10,035. Changing the assertion to 10,040 would make a correct import fail.

### Phase 4 — parking address and coordinates

Code-ready. Activation requires verified address/location/current ward and GIS containment. Address/coordinate changes are audited and untrusted client geocoding state is reset. Production data still needs a bounded geocoding/review campaign; do not activate uncertain records.

### Phase 5 — nearby API

Ready behind `PUBLIC_NEARBY_SEARCH_ENABLED`. It is parameterized, radius-limited, result-limited, active-only, independent of external providers, and index-backed.

### Phase 6 — customer map

Functionally implemented behind `CUSTOMER_PARKING_MAP_ENABLED`. The frontend loads public feature status and does not request GPS when disabled. Full browser automation infrastructure (Vitest/Testing Library or Playwright) is not currently installed, so mobile/browser interaction evidence is still a release gate.

### Phase 7 — quota, security, performance and monitoring

Implementation-ready. Redis-backed quota/rate controls, provider bulkhead/timeouts/retry/response bounds, privacy-safe metrics, alerts/dashboard, database benchmark and k6 script exist. The k6 staging result is pending.

### Phase 8 — rollout safety

Code and local QA are ready. Flags default to false, test profile enables them, disabled features return structured 503 responses, and the frontend fails closed. The data-check script returns exit code 3 on a failed gate. Production enablement remains conditional on external staging/operations evidence.

## Required go-live evidence

1. Verified backup restore with checksum and duration.
2. Data gate passes on the restored copy, pre-deploy production, and post-deploy production.
3. k6 thresholds pass on production-like staging.
4. Browser matrix passes on Android Chrome, iOS Safari, and one desktop browser for GPS allow/deny/timeout, manual address/point, tile failure and Google Maps handoff.
5. Real Nominatim smoke test confirms Vietnamese headers, cache hit behavior, quota stop and no secret/coordinate leakage in logs.
6. Monitoring owner and rollback operator execute a feature-off drill.
7. Only then progress through internal admin, pilot lots, customer beta, and general availability.