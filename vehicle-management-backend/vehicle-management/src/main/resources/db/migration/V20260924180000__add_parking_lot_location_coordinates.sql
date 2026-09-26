ALTER TABLE parking.parking_lots
    ADD COLUMN IF NOT EXISTS latitude numeric(9,6),
    ADD COLUMN IF NOT EXISTS longitude numeric(9,6);

ALTER TABLE parking.parking_lots
    ADD CONSTRAINT ck_parking_lots_latitude_range
        CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90);

ALTER TABLE parking.parking_lots
    ADD CONSTRAINT ck_parking_lots_longitude_range
        CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180);

ALTER TABLE parking.parking_lots
    ADD CONSTRAINT ck_parking_lots_coordinates_pair
        CHECK ((latitude IS NULL) = (longitude IS NULL));
