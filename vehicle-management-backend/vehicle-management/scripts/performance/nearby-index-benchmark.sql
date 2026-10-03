\set ON_ERROR_STOP on
\timing on

BEGIN;
SET LOCAL statement_timeout = '5min';
SET LOCAL lock_timeout = '5s';

\echo 'Phase 7 PostGIS benchmark uses a transaction and rolls every synthetic row back.'

CREATE FUNCTION pg_temp.insert_phase7_parking_lots(
    first_series integer,
    last_series integer,
    code_prefix text,
    grid_size integer,
    coordinate_step double precision
) RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO parking.parking_lots (
        parking_lot_id, organization_id, code, name, address, total_capacity, status,
        latitude, longitude, address_input_scheme, address_display, current_ward_code, geocoding_status
    )
    SELECT
        gen_random_uuid(), '00000000-0000-0000-0000-000000009001'::uuid,
        code_prefix || series, 'Performance lot ' || series, 'Synthetic benchmark only', 10, 'ACTIVE',
        ST_Y(point.location::geometry), ST_X(point.location::geometry),
        'CURRENT', 'Synthetic benchmark only', ward.ward_code, 'RESOLVED'
    FROM generate_series(first_series, last_series + 1000) AS series
    CROSS JOIN LATERAL (
        SELECT ST_SetSRID(
            ST_MakePoint(
                106.7009 + (((series / grid_size) % grid_size) - grid_size / 2) * coordinate_step,
                10.7769 + ((series % grid_size) - grid_size / 2) * coordinate_step
            ), 4326
        )::geography AS location
    ) AS point
    CROSS JOIN LATERAL (
        SELECT MIN(ward_code) AS ward_code
        FROM reference.current_ward_codes_for_point(point.location)
        HAVING COUNT(*) = 1
    ) AS ward
    ORDER BY series
    LIMIT (last_series - first_series + 1);
END;
$$;

SELECT pg_temp.insert_phase7_parking_lots(1, 1000, 'PERF-1K-', 32, 0.01);
ANALYZE parking.parking_lots;
\echo '=== 1,000 synthetic parking lots ==='
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT parking_lot_id FROM parking.parking_lots
WHERE status = 'ACTIVE' AND location IS NOT NULL
  AND ST_DWithin(location, ST_SetSRID(ST_MakePoint(106.7009, 10.7769), 4326)::geography, 5000)
ORDER BY ST_Distance(location, ST_SetSRID(ST_MakePoint(106.7009, 10.7769), 4326)::geography)
LIMIT 50;

SELECT pg_temp.insert_phase7_parking_lots(1001, 10000, 'PERF-10K-', 100, 0.003);
ANALYZE parking.parking_lots;
\echo '=== 10,000 synthetic parking lots ==='
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT parking_lot_id FROM parking.parking_lots
WHERE status = 'ACTIVE' AND location IS NOT NULL
  AND ST_DWithin(location, ST_SetSRID(ST_MakePoint(106.7009, 10.7769), 4326)::geography, 5000)
ORDER BY ST_Distance(location, ST_SetSRID(ST_MakePoint(106.7009, 10.7769), 4326)::geography)
LIMIT 50;

SELECT pg_temp.insert_phase7_parking_lots(10001, 100000, 'PERF-100K-', 317, 0.001);
ANALYZE parking.parking_lots;
\echo '=== 100,000 synthetic parking lots ==='
EXPLAIN (ANALYZE, BUFFERS, COSTS OFF)
SELECT parking_lot_id FROM parking.parking_lots
WHERE status = 'ACTIVE' AND location IS NOT NULL
  AND ST_DWithin(location, ST_SetSRID(ST_MakePoint(106.7009, 10.7769), 4326)::geography, 5000)
ORDER BY ST_Distance(location, ST_SetSRID(ST_MakePoint(106.7009, 10.7769), 4326)::geography)
LIMIT 50;

ROLLBACK;
\echo 'Synthetic benchmark rows rolled back.'
