ALTER TABLE parking.parking_lots
    ADD COLUMN IF NOT EXISTS activation_requested_at timestamptz,
    ADD COLUMN IF NOT EXISTS activation_requested_by uuid;
