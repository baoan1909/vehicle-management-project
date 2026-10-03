# Parking location production rollout and rollback

## Safety model

Deploy database-compatible code while all four flags are `false`. Flyway migrations are additive; rollback never drops the new tables, columns, triggers, indexes, or imported reference data.

Flags and dependency order:

1. `ADMIN_ADDRESS_V2_ENABLED`
2. `GEOCODING_PROVIDER_ENABLED`
3. `PUBLIC_NEARBY_SEARCH_ENABLED`
4. `CUSTOMER_PARKING_MAP_ENABLED`

`CUSTOMER_PARKING_MAP_ENABLED=true` is rejected at application startup unless public nearby search is also enabled. The frontend reads `/api/public/parking-map/features` and fails closed before requesting browser location.

## Pre-deploy evidence

- Build the exact database image used by the release and run `SpatialFreshDatabaseContainerTest`.
- Run all backend and frontend tests.
- Run the k6 workload from `scripts/performance/nearby-load-test.js` on production-like staging. Required: P95 < 250 ms, P99 < 500 ms, error rate < 1%, excluding deliberate 429 responses.
- Confirm alert rules and the Grafana dashboard from the Phase 7 operations runbook.
- Record release SHA, image digests, Flyway current version, operator, start time, and rollback owner in the change ticket.

## Backup and restore drill

Create an encrypted database backup using the platform backup service or `pg_dump --format=custom`. Do not place a database password in the command line, shell history, source control, or CI output.

Verify the backup before migration:

```powershell
pg_restore --list <backup-file> | Select-Object -First 20
```

Restore into an isolated staging database with a new name, run Flyway `validate`, then execute `scripts/operations/parking-location-data-check.sql`. The change ticket must contain backup identifier, checksum, restore duration, and a successful data-gate result. A backup that has not been restored is not considered verified.

## Deployment sequence

1. Put all four flags at `false`.
2. Confirm the database image exposes both `vector` and `postgis`; use a migration owner that can install extensions. The runtime account must not have extension-install privileges.
3. Deploy the migration release. Flyway creates reference tables, imports current/legacy datasets, adds location columns, backfills geography, installs triggers and creates the GiST index.
4. Run the read-only data gate:

```powershell
Get-Content -Raw scripts/operations/parking-location-data-check.sql |
  docker compose -f docker-compose.postgres-pgvector.yml exec -T pgvector `
    psql -v ON_ERROR_STOP=1 -U postgres -d vehicle_management_db -f /dev/stdin
```

5. Keep records with `NEEDS_REVIEW` or `FAILED` out of `ACTIVE`. Existing records without coordinates may remain draft/setup.
6. Geocode missing records in approved batches. Each provider attempt consumes the daily quota; stop the batch before the internal cap, persist progress, and continue after the Vietnam-midnight reset. Never bypass Redis quota counters.
7. Resolve `current_ward_code` from PostGIS and manually review zero/multiple boundary matches or address/point mismatches.
8. Re-run the data gate. The exit code must be zero before enabling any customer flag.

The repository does not contain an unattended bulk-geocoding job. Until one is reviewed, the safe production procedure is explicit admin geocoding in bounded batches; do not script around authorization, quota, audit, or review status.

## Progressive enablement

| Stage | Audience | Flags | Minimum observation |
|---|---|---|---|
| Staging | QA | all true | full regression + k6 |
| Internal admin | trusted administrators | admin + provider | one business day |
| Pilot | selected parking lots | add nearby | one business day and data-quality alerts green |
| Beta | limited customers | add customer map | 24–48 hours |
| General availability | all customers | all true | formal go/no-go |

At every stage watch 5xx/429 rate, provider quota and errors, nearby P95/P99, empty-result rate, active-location data quality, geolocation outcomes, and support reports. Exact customer coordinates, raw IP addresses, and address queries must not be logged.

## Rollback

Rollback is feature-first:

1. Set `CUSTOMER_PARKING_MAP_ENABLED=false`.
2. If nearby search is unhealthy, set `PUBLIC_NEARBY_SEARCH_ENABLED=false`.
3. If provider/quota is unhealthy, set `GEOCODING_PROVIDER_ENABLED=false`; GPS/manual point nearby search can remain available if healthy.
4. If admin address workflows are unsafe, set `ADMIN_ADDRESS_V2_ENABLED=false`.
5. Roll back the application image only to a version compatible with the additive schema.
6. Re-run the data gate and compare counts/checksums with the pre-deploy record.

Do not run `flyway clean`, drop PostGIS/vector, remove reference data, delete new parking columns, or reverse the location backfill during an incident. Preserve review/audit data for diagnosis.

## Go/no-go decision

Production enablement is **NO-GO** if any of these is true: backup restore was not proven, data gate exits non-zero, fresh-container migration fails, k6 thresholds fail, `NEEDS_REVIEW` contains a lot planned for activation, security tests fail, secrets appear in frontend/logs, or monitoring/rollback ownership is missing.