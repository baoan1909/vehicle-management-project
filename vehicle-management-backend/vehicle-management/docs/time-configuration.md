# Time configuration

`src/main/resources/time.properties` is the physical catalog for global time-related configuration. Property keys remain owned by their modules; it is not one global `TimeProperties` object.

## Timezone contract

- `APP_TIME_ZONE` / `app.time-zone` is the single application timezone. The default is `Asia/Ho_Chi_Minh`.
- Restart all backend instances after changing it. Every instance in one environment must use the same value.
- Domain and persistence timestamps remain `Instant`; Hibernate binds them in UTC.
- Each Hikari PostgreSQL connection runs `SET TIME ZONE '<app.time-zone>'`.
- Jackson renders every `Instant` as ISO-8601 with the offset active in that IANA zone. For example, `2026-09-26T08:30:00Z` is returned as `2026-09-26T15:30:00+07:00` in `Asia/Ho_Chi_Minh`.
- PostgreSQL `timestamptz` preserves the absolute instant. It does not retain the original offset or zone name. DBeaver/pgAdmin use separate sessions and must set their timezone separately.
- Requests must carry `Z` or an explicit offset. Offset-free timestamps are rejected.
- No data migration or manual `+7/-7 hours` conversion is required.

The frontend loads `GET /api/public/application-time` before rendering and uses that IANA zone for instant display, calendar boundaries, and offset-free date-time form values.

## Mandatory API time contract

- Every absolute point in time in domain models, application results, API request DTOs, API response DTOs, realtime messages, and cache envelopes must use `Instant`. Examples include `createdAt`, `updatedAt`, `startAt`, `endAt`, `publishedAt`, `cancelledAt`, `approvedAt`, and `expiresAt`.
- Backend mappers must map `Instant` values directly. They must not convert an instant to a display `String` such as `HH:mm dd-MM-yyyy`.
- Jackson is the single API-boundary formatter. With `app.time-zone=Asia/Ho_Chi_Minh`, an instant is rendered as `2026-08-25T16:01:00+07:00`.
- A missing optional instant is JSON `null`, not an empty string.
- `LocalDate` is allowed only for a calendar date with no time. `LocalTime` is allowed only for a wall-clock/business time with no date. `Duration` is used for elapsed time and configuration. None of these types may be substituted for an absolute event timestamp.
- Human-readable formats belong in the frontend presentation layer. They are not API contracts and must not be produced by backend mappers.

Legacy response DTOs that expose absolute timestamps as formatted `String` values violate this contract and must be migrated together with their frontend consumers.

## PostgreSQL database default

Migration `V20260926180000__set_database_default_timezone.sql` sets the database-level default timezone to `Asia/Ho_Chi_Minh` for new sessions and sets the current Flyway session immediately. PostgreSQL may report the equivalent canonical alias `Asia/Saigon`; both resolve to `+07:00` for the tested instant.

This migration does not rewrite any `TIMESTAMPTZ` value. The database-level default affects presentation and parsing for new sessions only. Hikari's `connection-init-sql` remains authoritative for application connections and follows `app.time-zone`; changing `app.time-zone` later does not rewrite the already-applied database default migration.

## Override order

Configuration is loaded in this order: classpath `time.properties`, optional `.env`, then optional `./config/time-override.properties`. A later source overrides an earlier one. Environment variables referenced by a property also override its default.

New typed `Duration` keys take precedence over compatibility keys. Existing environment variables such as `VNPAY_TIMEOUT_MINUTES`, `OCR_READ_TIMEOUT_MS`, and `MINIO_PRESIGNED_EXPIRE_SECONDS` remain inputs to the new typed keys when their new equivalent is absent.

Spring duration values accept forms such as `20s`, `15m`, `48h`, or ISO-8601 (`PT20S`, `PT15M`, `PT48H`). Startup validation rejects invalid zones, non-positive timeout/TTL values, invalid retry ordering, and invalid circuit-breaker ordering.

## Property ownership

| Area | Typed property | Purpose / default |
|---|---|---|
| Application | `app.time-zone` | IANA zone, `Asia/Ho_Chi_Minh` |
| Subscription | `app.subscription.payment-timeout` | Pending payment lifetime, `48h` |
| Scheduler | `app.scheduler.subscription-lifecycle.fixed-delay` | Lifecycle interval, `5m` |
| Scheduler | `app.scheduler.subscription-lifecycle.initial-delay` | Startup delay, `1m` |
| Notification | `app.notification.pending-replay-window` | Realtime replay window, `6h` |
| Verification | `app.iam.verification-email.resend-cooldown` | Resend cooldown, `1m` |
| Verification | `app.iam.verification-email.rate-window` | Rate window, `1h` |
| Verification | `app.iam.verification-email.max-requests` | Requests per window, `5` |
| Shift | `app.operations.shift.required-duration` | Required shift length, `8h` |
| Shift | `app.operations.shift.minimum-rest` | Rest between shifts, `8h` |
| Support | `app.operations.support-ticket.escalation-rate-window` | Duplicate escalation window, `24h` |
| Storage | `app.storage.minio.presigned-url-expiry` | Default signed URL lifetime, `15m` |
| Storage | `app.storage.access.parking-image-read-url-expiry` | Parking image URL lifetime, `15m` |
| Storage | `app.storage.access.chat-attachment-read-url-expiry` | Chat attachment URL lifetime, `15m` |
| Security | `app.security.oauth2.jwk.connect-timeout` | JWK connect timeout, `3s` |
| Security | `app.security.oauth2.jwk.read-timeout` | JWK read timeout, `15s` |
| OCR | `app.ocr.connect-timeout` / `read-timeout` | OCR client timeouts, `1500ms` / `10s` |
| Payment | `app.payment.vnpay.timeout` | VNPay transaction window, `15m` |
| Redis | `app.redis.connect-timeout` / `command-timeout` | Redis timeouts, `1s` / `1s` |
| AI | `app.ai.*`, `app.ai.cache.*`, `app.ai.embedding.*`, `app.ai.ingestion.*`, `app.ai.circuit-breaker.*`, `app.ai.quality.*` | Existing module-owned timeout, retry, TTL, lock, lease, retention, and stale policies |

Values that vary by tenant, parking lot, price plan, or effective date belong in the database. Calendar invariants and derived boundaries stay in code.

## Operations checks

After deployment:

1. Confirm the startup log contains the resolved application timezone.
2. Call `/api/public/application-time` and verify the returned zone.
3. Call an endpoint returning an `Instant` and verify its offset.
4. Through the backend datasource run `SHOW TIME ZONE`; it must match `app.time-zone`.
5. Compare `EXTRACT(EPOCH FROM timestamp_column)` before and after a timezone change; the epoch must not change.

For an external DB client, run `SET TIME ZONE 'Asia/Ho_Chi_Minh';` (or the configured zone) on that client's own connection.
