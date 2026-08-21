# Flyway migration guide

## Active migration chain

Flyway scans only `classpath:db/migration`.

The initial cut-over consists of:

1. `V20260818110000__baseline_schema.sql`: consolidated schema-only baseline.
2. `V20260818120000__seed_security_reference_data.sql`: canonical roles, permission catalog, role-permission matrix, the technical `SYSTEM` audit account and the Keycloak-linked `vehiclemanagement.noreply` system account retained from legacy V13.
3. `V20260818120100__seed_catalog_reference_data.sql`: canonical vehicle, ticket, card, price-plan and price-rule data.
4. `V20260818120200__seed_support_ticket_categories.sql`: canonical support categories.

The pre-Flyway scripts were removed from the working tree after the consolidated baseline was verified. Use Git history when an old script is needed for audit or investigation; do not restore it into the active Flyway location.

## Flyway schema history

Flyway automatically creates and owns `public.flyway_schema_history` before it applies the first versioned migration. The table records the installed rank, version, description, script, checksum, installer, install time, execution time and success state for every migration, matching the table structure used by Job24.

Do not create this table in `V...sql` or in the schema baseline: Flyway needs the table before it can record that migration. The active configuration explicitly uses `public` and the table name `flyway_schema_history`.

For an empty business database, starting the backend creates the table and inserts four successful rows for the current cut-over migrations. Check it with:

```sql
SELECT installed_rank, version, description, script, success
FROM public.flyway_schema_history
ORDER BY installed_rank;
```

`docker-compose.keycloak.yml` owns the separate Keycloak database only. Query `vehicle_management_db` at the configured `DB_HOST` and `DB_PORT` to see the Flyway history; do not look for it in the Keycloak database.

## Empty database

Create an empty PostgreSQL database and start the backend with Flyway enabled. Flyway applies all four active migrations, after which Hibernate validates the final schema through `ddl-auto=validate`.

Expected reference-data counts after the cut-over migrations:

- 5 roles
- 37 permission modules
- 29 permission actions
- 5 permission scopes
- 149 permissions
- 259 role-permission rows
- 5 vehicle types (`BICYCLE`, `MOTORBIKE`, `CAR`, `LIGHT_TRUCK`, `OTHER`)
- 5 ticket types (`DAILY`, `MONTHLY`, `QUARTERLY`, `YEARLY`, `FREE`)
- 2 card types
- 2 price plans
- 30 price rules (10 visitor and 20 subscription rules)
- 10 support-ticket categories

The security seed fails immediately when a permission literal referenced by backend code is missing from the canonical permission catalog.

## Existing non-empty database

Do not drop a database that contains data that must be retained.

Before adopting Flyway:

1. Back up the database.
2. Compare its schema and reference data with the consolidated baseline.
3. Correct any drift explicitly.
4. Baseline it at version `20260818120200` so the cut-over schema and seeds are not reapplied.
5. Start the application normally and verify Flyway validation and Hibernate schema validation.

For a one-time controlled Spring Boot baseline, set both:

```text
FLYWAY_BASELINE_ON_MIGRATE=true
FLYWAY_BASELINE_VERSION=20260818120200
```

Return `FLYWAY_BASELINE_ON_MIGRATE` to `false` immediately after the history table has been created. Never leave automatic baselining enabled in shared or production environments.

## New migrations

Use `VyyyyMMddHHmmss__description_in_snake_case.sql` with a unique timestamp in Vietnam time. Never edit or rename a versioned migration after it has run in a shared environment; introduce a new forward-only migration instead.

Price changes must use a new migration. Close the previous effective period where required and insert the newly agreed price plan or rule instead of rewriting migration history.
