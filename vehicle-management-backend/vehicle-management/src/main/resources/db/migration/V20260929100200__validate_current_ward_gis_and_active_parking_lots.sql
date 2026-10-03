CREATE TABLE reference.gis_dataset_metadata (
    dataset_key varchar(80) PRIMARY KEY,
    dataset_version varchar(30) NOT NULL,
    source_url text NOT NULL,
    license varchar(30) NOT NULL,
    expected_ward_count integer NOT NULL,
    imported_ward_count integer NOT NULL,
    imported_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO reference.gis_dataset_metadata(
    dataset_key, dataset_version, source_url, license,
    expected_ward_count, imported_ward_count
)
VALUES (
    'vietnam-current-ward-boundaries',
    'v5.2.0',
    'https://github.com/thanglequoc/vietnamese-provinces-database',
    'MIT',
    3321,
    (SELECT count(*) FROM reference.gis_wards)
);

DO $$
DECLARE
    imported_count integer;
    invalid_count integer;
BEGIN
    SELECT count(*) INTO imported_count FROM reference.gis_wards;
    IF imported_count <> 3321 THEN
        RAISE EXCEPTION 'Invalid GIS ward count: expected 3321, got %', imported_count;
    END IF;

    SELECT count(*) INTO invalid_count
    FROM reference.gis_wards
    WHERE public.ST_SRID(geom) <> 4326
       OR public.ST_IsEmpty(geom)
       OR NOT public.ST_IsValid(geom);
    IF invalid_count <> 0 THEN
        RAISE EXCEPTION 'Invalid GIS ward geometries: %', invalid_count;
    END IF;
END
$$;

CREATE OR REPLACE FUNCTION reference.current_ward_codes_for_point(
    point_location geography(Point, 4326)
)
RETURNS TABLE(ward_code varchar)
LANGUAGE sql
STABLE
SET search_path = pg_catalog, public, reference
AS $$
    SELECT gw.ward_code
    FROM reference.gis_wards gw
    WHERE gw.bbox && point_location::geometry
      AND public.ST_Covers(gw.geom, point_location::geometry)
    ORDER BY gw.ward_code
$$;

CREATE OR REPLACE FUNCTION parking.validate_active_parking_lot_location()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, public, parking, reference
AS $$
DECLARE
    matched_codes varchar[];
BEGIN
    IF NEW.status <> 'ACTIVE' THEN
        RETURN NEW;
    END IF;

    IF NULLIF(btrim(NEW.address_display), '') IS NULL
       OR NEW.latitude IS NULL
       OR NEW.longitude IS NULL
       OR NEW.location IS NULL
       OR NEW.current_ward_code IS NULL
       OR NEW.geocoding_status NOT IN ('RESOLVED', 'MANUAL_CONFIRMED') THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            CONSTRAINT = 'ck_parking_lots_active_address_ready',
            MESSAGE = 'active parking lot requires a verified address and location';
    END IF;

    SELECT array_agg(m.ward_code) INTO matched_codes
    FROM reference.current_ward_codes_for_point(NEW.location) m;

    IF coalesce(array_length(matched_codes, 1), 0) <> 1
       OR matched_codes[1] <> NEW.current_ward_code THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            CONSTRAINT = 'ck_parking_lots_active_location_matches_ward',
            MESSAGE = 'active parking lot location must match exactly one current ward';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_parking_lots_validate_active_location
    BEFORE INSERT OR UPDATE OF status, address_display, latitude, longitude,
        location, current_ward_code, geocoding_status
    ON parking.parking_lots
    FOR EACH ROW
    EXECUTE FUNCTION parking.validate_active_parking_lot_location();
