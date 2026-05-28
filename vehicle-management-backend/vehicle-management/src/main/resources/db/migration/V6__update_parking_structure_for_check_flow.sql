-- 1. Them bang gates de quan ly cong xe
CREATE TABLE IF NOT EXISTS parking.gates (
                                             gate_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                             parking_lot_id UUID NOT NULL,
                                             code VARCHAR(50) NOT NULL,
                                             name VARCHAR(150) NOT NULL,
                                             status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                                             created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                             created_by UUID,
                                             updated_at TIMESTAMPTZ,
                                             updated_by UUID,

                                             CONSTRAINT fk_gates_parking_lot
                                                 FOREIGN KEY (parking_lot_id)
                                                     REFERENCES parking.parking_lots(parking_lot_id)
                                                     ON DELETE CASCADE,

                                             CONSTRAINT uq_gates_lot_code
                                                 UNIQUE (parking_lot_id, code),

                                             CONSTRAINT ck_gates_status
                                                 CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'CLOSED'))
);

-- 2. Cap nhat lanes: moi lane thuoc gate, co the gioi han loai xe,
-- va khong con luu parking_lot_id truc tiep
ALTER TABLE parking.lanes
    ADD COLUMN IF NOT EXISTS gate_id UUID,
    ADD COLUMN IF NOT EXISTS vehicle_type_id UUID;

ALTER TABLE parking.lanes
    DROP CONSTRAINT IF EXISTS fk_lanes_parking_lot;

ALTER TABLE parking.lanes
    DROP CONSTRAINT IF EXISTS uq_lanes_lot_code;

ALTER TABLE parking.lanes
    DROP COLUMN IF EXISTS parking_lot_id;

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_lanes_gate'
        ) THEN
            ALTER TABLE parking.lanes
                ADD CONSTRAINT fk_lanes_gate
                    FOREIGN KEY (gate_id)
                        REFERENCES parking.gates(gate_id)
                        ON DELETE SET NULL;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_lanes_vehicle_type'
        ) THEN
            ALTER TABLE parking.lanes
                ADD CONSTRAINT fk_lanes_vehicle_type
                    FOREIGN KEY (vehicle_type_id)
                        REFERENCES catalog.vehicle_types(vehicle_type_id)
                        ON DELETE SET NULL;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'uq_lanes_gate_code'
        ) THEN
            ALTER TABLE parking.lanes
                ADD CONSTRAINT uq_lanes_gate_code
                    UNIQUE (gate_id, code);
        END IF;
    END $$;

-- 3. Parking sessions dung zone_id thay cho parking_space_id
ALTER TABLE parking.parking_sessions
    ADD COLUMN IF NOT EXISTS zone_id UUID;

UPDATE parking.parking_sessions ps
SET zone_id = pspace.zone_id
FROM parking.parking_spaces pspace
WHERE ps.parking_space_id = pspace.parking_space_id
  AND ps.zone_id IS NULL;

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_parking_sessions_zone'
        ) THEN
            ALTER TABLE parking.parking_sessions
                ADD CONSTRAINT fk_parking_sessions_zone
                    FOREIGN KEY (zone_id)
                        REFERENCES parking.zones(zone_id)
                        ON DELETE SET NULL;
        END IF;
    END $$;

-- 4. Bo parking_spaces
ALTER TABLE parking.parking_sessions
    DROP CONSTRAINT IF EXISTS fk_parking_sessions_space;

ALTER TABLE parking.parking_sessions
    DROP COLUMN IF EXISTS parking_space_id;

DROP TABLE IF EXISTS parking.parking_spaces;

-- 5. Bo price_rule_id khoi parking_sessions
-- total_price da co san va se duoc backend tinh luc checkout
ALTER TABLE parking.parking_sessions
    DROP CONSTRAINT IF EXISTS fk_parking_sessions_price_rule;

ALTER TABLE parking.parking_sessions
    DROP COLUMN IF EXISTS price_rule_id;

-- 6. Them anh bien so va anh nguoi vao parking_events
ALTER TABLE parking.parking_events
    ADD COLUMN IF NOT EXISTS license_plate_image_path VARCHAR(255),
    ADD COLUMN IF NOT EXISTS person_image_path VARCHAR(255);

-- 7. Them rang buoc suc chua khong am
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'ck_parking_lots_total_capacity_non_negative'
        ) THEN
            ALTER TABLE parking.parking_lots
                ADD CONSTRAINT ck_parking_lots_total_capacity_non_negative
                    CHECK (total_capacity >= 0);
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'ck_zones_capacity_non_negative'
        ) THEN
            ALTER TABLE parking.zones
                ADD CONSTRAINT ck_zones_capacity_non_negative
                    CHECK (capacity >= 0);
        END IF;
    END $$;

-- 8. Index phuc vu check-in/check-out
CREATE INDEX IF NOT EXISTS idx_gates_parking_lot_status
    ON parking.gates (parking_lot_id, status);

CREATE INDEX IF NOT EXISTS idx_lanes_gate
    ON parking.lanes (gate_id);

CREATE INDEX IF NOT EXISTS idx_lanes_direction_status
    ON parking.lanes (direction, status);

CREATE INDEX IF NOT EXISTS idx_lanes_vehicle_type
    ON parking.lanes (vehicle_type_id);

CREATE INDEX IF NOT EXISTS idx_zones_lot_vehicle_type
    ON parking.zones (parking_lot_id, vehicle_type_id);

CREATE INDEX IF NOT EXISTS idx_parking_sessions_zone_status
    ON parking.parking_sessions (zone_id, status);

CREATE INDEX IF NOT EXISTS idx_parking_sessions_card_status
    ON parking.parking_sessions (card_id, status);

CREATE INDEX IF NOT EXISTS idx_parking_sessions_license_plate_in_status
    ON parking.parking_sessions (license_plate_in, status);