# Phase 7 nearby-search performance report

## Scope and reproducibility

- Dataset sizes: 1,000, 10,000 and 100,000 synthetic active parking lots.
- Search: geography point, radius 5 km, limit 50, ordered by distance.
- Database script: `scripts/performance/nearby-index-benchmark.sql`.
- Concurrent HTTP script: `scripts/performance/nearby-load-test.js`.
- Synthetic database rows run in one transaction and are rolled back.

## Automated evidence

- `NearbyParkingLotIntegrationTest.shouldUsePartialGistIndexInExplainAnalyze` asserts that PostgreSQL selects `idx_parking_lots_active_location_gist`.
- `MapProviderQuotaGuardTest.shouldNeverExceedLimitUnderConcurrentIncrements` verifies the quota guard cannot exceed its internal cap under 1,000 concurrent attempts.
- The provider bulkhead allows at most the configured number of outbound requests; excess callers fail fast instead of occupying the servlet thread pool through the provider timeout.

## Local Docker result

Executed on 2026-10-02 against the project PostgreSQL/PostGIS container. These figures prove index selection and provide a local regression baseline; staging capacity still needs to be measured on production-like CPU, memory and connection-pool settings.

| Dataset | Plan uses GiST | Execution time | Rows returned | Notes |
|---:|:---:|---:|---:|---|
| 1,000 | Yes | 9.904 ms | 50 of 69 matches | Bitmap Index Scan |
| 10,000 | Yes | 4.593 ms | 50 of 790 matches | Bitmap Index Scan |
| 100,000 | Yes | 211.552 ms | 50 of about 7,287 matches | Parallel Bitmap Heap/Index Scan |

| HTTP workload | P50 | P95 | P99 | Error rate |
|---|---:|---:|---:|---:|
| 20 req/s for 2 minutes | Pending staging | Pending staging | Pending staging | Pending staging |

The repository contains a repeatable k6 workload, but k6 was not installed on the development workstation. CI/staging must run it before a production capacity decision. The automated integration suite additionally sends concurrent nearby queries to detect connection-pool or thread-safety regressions.

## Acceptance gates

- Spatial filter uses `idx_parking_lots_active_location_gist`; a full-table sequential scan is a failure.
- P95 nearby latency below 250 ms and P99 below 500 ms at expected load.
- Error rate below 1%, excluding deliberate HTTP 429 tests.
- Provider timeout concurrency never exceeds `NOMINATIM_MAX_CONCURRENT_REQUESTS`.
- No test or application log contains full customer coordinates.
