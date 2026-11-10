ALTER TABLE parking.parking_events
    ADD COLUMN IF NOT EXISTS vehicle_image_path VARCHAR(255);