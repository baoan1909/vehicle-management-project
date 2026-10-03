\set ON_ERROR_STOP on
\pset pager off

\echo '== Required extensions =='
SELECT extname, extversion
FROM pg_extension
WHERE extname IN ('postgis', 'vector')
ORDER BY extname;

\echo '== Administrative datasets =='
SELECT dataset_key, dataset_version, source_tag, province_count, ward_count, source_sha256
FROM reference.administrative_dataset_metadata;
SELECT dataset_key, dataset_version, source_tag, province_count, district_count, ward_count, source_sha256
FROM reference.legacy_dataset_metadata;

\echo '== Reference counts and relationship quality =='
SELECT
    (SELECT count(*) FROM reference.provinces) AS current_provinces,
    (SELECT count(*) FROM reference.wards) AS current_wards,
    (SELECT count(*) FROM reference.gis_wards) AS gis_wards,
    (SELECT count(*) FROM reference.legacy_provinces) AS legacy_provinces,
    (SELECT count(*) FROM reference.legacy_districts) AS legacy_districts,
    (SELECT count(*) FROM reference.legacy_wards) AS legacy_wards;

SELECT
    (SELECT count(*) FROM reference.wards w
       LEFT JOIN reference.provinces p ON p.code = w.province_code
      WHERE p.code IS NULL) AS orphan_current_wards,
    (SELECT count(*) FROM reference.legacy_districts d
       LEFT JOIN reference.legacy_provinces p ON p.code = d.province_code
      WHERE p.code IS NULL) AS orphan_legacy_districts,
    (SELECT count(*) FROM reference.legacy_wards w
       LEFT JOIN reference.legacy_districts d ON d.code = w.district_code
      WHERE d.code IS NULL) AS orphan_legacy_wards;

\echo '== Parking location quality =='
SELECT
    count(*) AS total_parking_lots,
    count(*) FILTER (WHERE (latitude IS NULL) <> (longitude IS NULL)) AS incomplete_coordinate_pairs,
    count(*) FILTER (WHERE latitude NOT BETWEEN -90 AND 90) AS invalid_latitudes,
    count(*) FILTER (WHERE longitude NOT BETWEEN -180 AND 180) AS invalid_longitudes,
    count(*) FILTER (WHERE latitude IS NOT NULL AND location IS NULL) AS missing_locations,
    count(*) FILTER (WHERE location IS NOT NULL AND public.ST_SRID(location) <> 4326) AS invalid_srids,
    count(*) FILTER (WHERE geocoding_status = 'NEEDS_REVIEW') AS review_queue,
    count(*) FILTER (WHERE geocoding_status = 'FAILED') AS failed_geocoding,
    count(*) FILTER (
        WHERE status = 'ACTIVE'
          AND (
              nullif(btrim(address_display), '') IS NULL
              OR latitude IS NULL
              OR longitude IS NULL
              OR location IS NULL
              OR current_ward_code IS NULL
              OR geocoding_status NOT IN ('RESOLVED', 'MANUAL_CONFIRMED')
          )
    ) AS invalid_active_parking_lots
FROM parking.parking_lots;

SELECT count(*) AS active_ward_mismatches
FROM parking.parking_lots pl
WHERE pl.status = 'ACTIVE'
  AND pl.location IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM reference.current_ward_codes_for_point(pl.location) resolved
      WHERE resolved.ward_code = pl.current_ward_code
  );

\echo '== Spatial index and Flyway state =='
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'parking'
  AND indexname = 'idx_parking_lots_active_location_gist';
SELECT count(*) AS failed_flyway_migrations
FROM public.flyway_schema_history
WHERE success = false;

WITH checks AS (
    SELECT
        (SELECT count(*) FROM pg_extension WHERE extname IN ('postgis', 'vector')) = 2 AS extensions_ok,
        (SELECT count(*) FROM reference.provinces) = 34 AS current_provinces_ok,
        (SELECT count(*) FROM reference.wards) = 3321 AS current_wards_ok,
        (SELECT count(*) FROM reference.gis_wards) = 3321 AS gis_wards_ok,
        (SELECT count(*) FROM reference.legacy_provinces) = 63 AS legacy_provinces_ok,
        (SELECT count(*) FROM reference.legacy_districts) = 696 AS legacy_districts_ok,
        (SELECT count(*) FROM reference.legacy_wards) = 10035 AS legacy_wards_ok,
        NOT EXISTS (
            SELECT 1 FROM parking.parking_lots
            WHERE (latitude IS NULL) <> (longitude IS NULL)
               OR latitude NOT BETWEEN -90 AND 90
               OR longitude NOT BETWEEN -180 AND 180
               OR (latitude IS NOT NULL AND location IS NULL)
               OR (location IS NOT NULL AND public.ST_SRID(location) <> 4326)
        ) AS coordinates_ok,
        NOT EXISTS (
            SELECT 1 FROM parking.parking_lots
            WHERE status = 'ACTIVE'
              AND (
                  nullif(btrim(address_display), '') IS NULL
                  OR latitude IS NULL OR longitude IS NULL OR location IS NULL
                  OR current_ward_code IS NULL
                  OR geocoding_status NOT IN ('RESOLVED', 'MANUAL_CONFIRMED')
              )
        ) AS active_rows_ok,
        NOT EXISTS (
            SELECT 1
            FROM parking.parking_lots pl
            WHERE pl.status = 'ACTIVE'
              AND pl.location IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM reference.current_ward_codes_for_point(pl.location) resolved
                  WHERE resolved.ward_code = pl.current_ward_code
              )
        ) AS active_wards_ok,
        EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE schemaname = 'parking'
              AND indexname = 'idx_parking_lots_active_location_gist'
              AND indexdef ILIKE '%USING gist%'
        ) AS gist_index_ok,
        NOT EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE success = false) AS flyway_ok
), result AS (
    SELECT *, NOT (
        extensions_ok AND current_provinces_ok AND current_wards_ok AND gis_wards_ok
        AND legacy_provinces_ok AND legacy_districts_ok AND legacy_wards_ok
        AND coordinates_ok AND active_rows_ok AND active_wards_ok AND gist_index_ok AND flyway_ok
    ) AS gate_failed
    FROM checks
)
SELECT * FROM result;

WITH checks AS (
    SELECT
        (SELECT count(*) FROM pg_extension WHERE extname IN ('postgis', 'vector')) = 2
        AND (SELECT count(*) FROM reference.provinces) = 34
        AND (SELECT count(*) FROM reference.wards) = 3321
        AND (SELECT count(*) FROM reference.gis_wards) = 3321
        AND (SELECT count(*) FROM reference.legacy_provinces) = 63
        AND (SELECT count(*) FROM reference.legacy_districts) = 696
        AND (SELECT count(*) FROM reference.legacy_wards) = 10035
        AND NOT EXISTS (
            SELECT 1 FROM parking.parking_lots
            WHERE (latitude IS NULL) <> (longitude IS NULL)
               OR latitude NOT BETWEEN -90 AND 90
               OR longitude NOT BETWEEN -180 AND 180
               OR (latitude IS NOT NULL AND location IS NULL)
               OR (location IS NOT NULL AND public.ST_SRID(location) <> 4326)
        )
        AND NOT EXISTS (
            SELECT 1 FROM parking.parking_lots
            WHERE status = 'ACTIVE'
              AND (
                  nullif(btrim(address_display), '') IS NULL
                  OR latitude IS NULL OR longitude IS NULL OR location IS NULL
                  OR current_ward_code IS NULL
                  OR geocoding_status NOT IN ('RESOLVED', 'MANUAL_CONFIRMED')
              )
        )
        AND NOT EXISTS (
            SELECT 1
            FROM parking.parking_lots pl
            WHERE pl.status = 'ACTIVE'
              AND pl.location IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM reference.current_ward_codes_for_point(pl.location) resolved
                  WHERE resolved.ward_code = pl.current_ward_code
              )
        )
        AND EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE schemaname = 'parking'
              AND indexname = 'idx_parking_lots_active_location_gist'
              AND indexdef ILIKE '%USING gist%'
        )
        AND NOT EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE success = false)
        AS gate_passed
)
SELECT NOT gate_passed AS gate_failed FROM checks \gset
\if :gate_failed
  \echo 'PARKING LOCATION DATA GATE: FAILED'
  \quit 3
\else
  \echo 'PARKING LOCATION DATA GATE: PASSED'
\endif