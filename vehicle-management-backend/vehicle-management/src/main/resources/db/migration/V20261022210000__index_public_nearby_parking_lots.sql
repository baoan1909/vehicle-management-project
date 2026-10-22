CREATE INDEX IF NOT EXISTS idx_parking_lots_public_location_gist
    ON parking.parking_lots
    USING GIST (location)
    WHERE location IS NOT NULL
      AND status IN ('ACTIVE', 'SETUP', 'MAINTENANCE');
