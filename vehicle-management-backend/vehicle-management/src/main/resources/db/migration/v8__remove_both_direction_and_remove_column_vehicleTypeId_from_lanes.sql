ALTER TABLE parking.lanes
    DROP CONSTRAINT IF EXISTS ck_lanes_direction;

ALTER TABLE parking.lanes
    ADD CONSTRAINT ck_lanes_direction
        CHECK (direction IN ('IN', 'OUT'));

ALTER TABLE parking.lanes
    DROP CONSTRAINT IF EXISTS fk_lanes_vehicle_type;

ALTER TABLE parking.lanes
    DROP COLUMN IF EXISTS vehicle_type_id;