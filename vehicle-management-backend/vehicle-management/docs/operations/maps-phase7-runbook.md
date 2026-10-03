# Maps, quota and nearby-search runbook

## Runtime controls

- Provider quota keys use `maps:quota:{provider}:{operation}:{yyyy-MM-dd}` and reset at the next midnight in `Asia/Ho_Chi_Minh`.
- `NOMINATIM_DAILY_REQUEST_LIMIT` is the provider ceiling. `NOMINATIM_QUOTA_USAGE_PERCENT` defaults to `90`, so the application stops before the provider ceiling.
- Cache hits do not increment quota. Each real provider attempt increments quota atomically before the outbound request.
- When Redis is enabled but unavailable, provider calls fail closed with HTTP 503. This protects the paid/free provider limit across multiple application instances.
- Nearby PostGIS searches never consume provider quota.

## Quota exhaustion

1. Confirm `MAP_DAILY_QUOTA_EXHAUSTED`, the provider and operation on the dashboard.
2. Do not delete or decrement the Redis quota key merely to restore traffic; verify the provider console first.
3. Keep nearby search available. Customers can still use GPS or choose a point on the map.
4. If an emergency quota extension is approved, update the secret/environment configuration and restart through the normal deployment workflow.
5. The counter resets automatically at Vietnam midnight. Never log or copy customer coordinates while investigating.

## Provider and secret rotation

1. Create a new backend key in the provider console and restrict it to only required APIs and source networks.
2. Store it in the deployment secret manager; never place it in `.env.example`, Git, frontend bundles or CI output.
3. Deploy one instance with the new secret, verify provider/cache/quota metrics, then roll the remaining instances.
4. Revoke the old key only after the rollout is healthy.
5. Frontend keys, if introduced later, must be restricted by production domain/referrer. Backend keys must never be delivered to the browser.
6. Provider URL changes require an HTTPS hostname in the server-side allowlist. Redirect following remains disabled.

## Privacy-safe investigation

Allowed log/metric dimensions are radius, result count, duration, fixed provider/operation/outcome tags, or a coarse grid. Never log full latitude/longitude, raw IP addresses, geocoding query strings, provider keys or Redis cache values. Rate-limit IP identities are SHA-256 hashed before storage.

## Monitoring

- Prometheus scrape endpoint: `/actuator/prometheus` (authenticated by default).
- Import `observability/grafana/dashboards/maps-phase7.json`.
- Load `observability/prometheus/maps-alerts.yml` into Prometheus.
- The dashboard covers nearby count/latency/results, geocoding cache ratio, quota, provider errors, PostGIS/data quality and browser geolocation outcomes.

## Incorrect parking coordinates

1. Move the parking lot out of `ACTIVE` if customers may be misdirected.
2. Review address audit history and `geocoding_status`.
3. Re-geocode only through the explicit admin action, then confirm the marker manually.
4. Verify `current_ward_code`, `location` SRID 4326 and the GIS ward containment result before reactivation.

## Benchmark

Database benchmark (all synthetic rows are rolled back):

```powershell
Get-Content -Raw scripts/performance/nearby-index-benchmark.sql |
  docker compose -f docker-compose.postgres-pgvector.yml exec -T pgvector `
    psql -U postgres -d vehicle_management_db -f /dev/stdin
```

Concurrent API test:

```powershell
k6 run -e BASE_URL=http://127.0.0.1:8080/vehicle-management scripts/performance/nearby-load-test.js
```

Acceptance requires a GiST index scan (or bitmap index scan using `idx_parking_lots_active_location_gist`), no full-table sequential scan for the spatial filter, P95 below 250 ms and errors below 1% at the expected request rate.
