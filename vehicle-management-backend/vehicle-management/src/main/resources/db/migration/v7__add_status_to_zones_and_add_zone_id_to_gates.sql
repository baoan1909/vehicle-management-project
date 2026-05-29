-- V7: update gates to zone relation and add status to zones
-- New model: parking_lots -> zones -> gates -> lanes

-- 1. Add status to zones
ALTER TABLE parking.zones
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'ck_zones_status'
        ) THEN
            ALTER TABLE parking.zones
                ADD CONSTRAINT ck_zones_status
                    CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'CLOSED'));
        END IF;
    END $$;

CREATE INDEX IF NOT EXISTS idx_zones_status
    ON parking.zones (status);

-- 2. Add zone_id to gates
ALTER TABLE parking.gates
    ADD COLUMN IF NOT EXISTS zone_id UUID;

-- 3. Try to auto-fill zone_id only when a parking lot has exactly one zone
UPDATE parking.gates gate_item
SET zone_id = zone_item.zone_id
FROM parking.zones zone_item
WHERE gate_item.zone_id IS NULL
  AND gate_item.parking_lot_id = zone_item.parking_lot_id
  AND (
          SELECT COUNT(*)
          FROM parking.zones count_zone
          WHERE count_zone.parking_lot_id = gate_item.parking_lot_id
      ) = 1;

-- 4. Remove old parking_lot relation from gates
ALTER TABLE parking.gates
    DROP CONSTRAINT IF EXISTS fk_gates_parking_lot;

ALTER TABLE parking.gates
    DROP CONSTRAINT IF EXISTS uq_gates_lot_code;

-- 5. Add new gate -> zone constraints
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_gates_zone'
        ) THEN
            ALTER TABLE parking.gates
                ADD CONSTRAINT fk_gates_zone
                    FOREIGN KEY (zone_id)
                        REFERENCES parking.zones(zone_id)
                        ON DELETE CASCADE;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'uq_gates_zone_code'
        ) THEN
            ALTER TABLE parking.gates
                ADD CONSTRAINT uq_gates_zone_code
                    UNIQUE (zone_id, code);
        END IF;
    END $$;

-- 6. Stop migration if any gate still has no zone_id
DO $$
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM parking.gates
            WHERE zone_id IS NULL
        ) THEN
            RAISE EXCEPTION 'Some gates still have null zone_id. Please update parking.gates.zone_id before setting NOT NULL.';
        END IF;
    END $$;

-- 7. Make zone_id required and remove old parking_lot_id
ALTER TABLE parking.gates
    ALTER COLUMN zone_id SET NOT NULL;

ALTER TABLE parking.gates
    DROP COLUMN IF EXISTS parking_lot_id;

-- 8. Replace old gate index
DROP INDEX IF EXISTS parking.idx_gates_parking_lot_status;

CREATE INDEX IF NOT EXISTS idx_gates_zone_status
    ON parking.gates (zone_id, status);