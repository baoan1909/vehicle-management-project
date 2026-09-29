-- PostGIS is installed by the migration role. The runtime application role only
-- needs normal DML privileges on parking.parking_lots.
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;

ALTER TABLE parking.parking_lots
    ADD COLUMN location geography(Point, 4326);

UPDATE parking.parking_lots
SET location = public.ST_SetSRID(
        public.ST_MakePoint(longitude::double precision, latitude::double precision),
        4326
    )::geography
WHERE latitude IS NOT NULL
  AND longitude IS NOT NULL;

CREATE OR REPLACE FUNCTION parking.sync_parking_lot_location()
RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, public
AS $$
BEGIN
    IF NEW.latitude IS NULL AND NEW.longitude IS NULL THEN
        NEW.location := NULL;
        RETURN NEW;
    END IF;

    IF NEW.latitude IS NULL OR NEW.longitude IS NULL THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            CONSTRAINT = 'ck_parking_lots_coordinates_pair',
            MESSAGE = 'latitude and longitude must both be present or both be null';
    END IF;

    IF NEW.latitude NOT BETWEEN -90 AND 90 THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            CONSTRAINT = 'ck_parking_lots_latitude_range',
            MESSAGE = 'latitude must be between -90 and 90';
    END IF;

    IF NEW.longitude NOT BETWEEN -180 AND 180 THEN
        RAISE EXCEPTION USING
            ERRCODE = '23514',
            CONSTRAINT = 'ck_parking_lots_longitude_range',
            MESSAGE = 'longitude must be between -180 and 180';
    END IF;

    -- location is always derived from validated numeric values. Any value supplied
    -- directly for location is deliberately ignored.
    NEW.location := public.ST_SetSRID(
            public.ST_MakePoint(NEW.longitude::double precision, NEW.latitude::double precision),
            4326
        )::geography;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_parking_lots_sync_location
    BEFORE INSERT OR UPDATE OF latitude, longitude, location
    ON parking.parking_lots
    FOR EACH ROW
    EXECUTE FUNCTION parking.sync_parking_lot_location();

ALTER TABLE parking.parking_lots
    ADD CONSTRAINT ck_parking_lots_location_presence
        CHECK ((location IS NULL) = (latitude IS NULL)),
    ADD CONSTRAINT ck_parking_lots_location_srid
        CHECK (location IS NULL OR public.ST_SRID(location) = 4326);

CREATE INDEX idx_parking_lots_active_location_gist
    ON parking.parking_lots
    USING GIST (location)
    WHERE location IS NOT NULL
      AND status = 'ACTIVE';
