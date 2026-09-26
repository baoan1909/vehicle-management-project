-- A newly registered partner parking lot is configured in stages.
-- Existing active lots remain active; only newly created lots default to SETUP.
ALTER TABLE parking.parking_lots
    DROP CONSTRAINT IF EXISTS ck_parking_lots_status;

ALTER TABLE parking.parking_lots
    ADD CONSTRAINT ck_parking_lots_status
        CHECK (status IN ('SETUP', 'ACTIVE', 'MAINTENANCE', 'CLOSED'));

ALTER TABLE parking.parking_lots
    ALTER COLUMN status SET DEFAULT 'SETUP';
