-- Drop unused columns from parking.parking_sessions.
-- parking_space_id was replaced by zone_id.
-- price_rule_id was replaced by total_price; checkout pricing is calculated by backend.

ALTER TABLE parking.parking_sessions
    DROP CONSTRAINT IF EXISTS fk_parking_sessions_space;

ALTER TABLE parking.parking_sessions
    DROP CONSTRAINT IF EXISTS fk_parking_sessions_price_rule;

ALTER TABLE parking.parking_sessions
    DROP COLUMN IF EXISTS parking_space_id;

ALTER TABLE parking.parking_sessions
    DROP COLUMN IF EXISTS price_rule_id;