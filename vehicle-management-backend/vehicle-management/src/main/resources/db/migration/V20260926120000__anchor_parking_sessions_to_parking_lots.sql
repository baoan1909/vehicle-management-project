-- Zone is an operational location, not a durable ownership key: it may be
-- removed after checkout. Keep the lot on the session itself.
ALTER TABLE parking.parking_sessions
    ADD COLUMN parking_lot_id uuid;

-- A zone provides the only reliable historical ownership. Do not guess from
-- a card: physical cards may have been reassigned between parking lots.
UPDATE parking.parking_sessions session
SET parking_lot_id = zone.parking_lot_id
FROM parking.zones zone
WHERE session.zone_id = zone.zone_id;

ALTER TABLE parking.parking_sessions
    ADD CONSTRAINT fk_parking_sessions_parking_lot
    FOREIGN KEY (parking_lot_id)
    REFERENCES parking.parking_lots(parking_lot_id)
    ON DELETE RESTRICT;

CREATE INDEX idx_parking_sessions_lot_check_in
    ON parking.parking_sessions(parking_lot_id, check_in_time DESC);
