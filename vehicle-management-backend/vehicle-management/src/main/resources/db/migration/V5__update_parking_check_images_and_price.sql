ALTER TABLE parking.parking_events
    ADD COLUMN IF NOT EXISTS license_plate_image_path VARCHAR(255),
    ADD COLUMN IF NOT EXISTS person_image_path VARCHAR(255);

ALTER TABLE parking.parking_sessions
    DROP CONSTRAINT IF EXISTS fk_parking_sessions_price_rule;

ALTER TABLE parking.parking_sessions
    DROP COLUMN IF EXISTS price_rule_id;