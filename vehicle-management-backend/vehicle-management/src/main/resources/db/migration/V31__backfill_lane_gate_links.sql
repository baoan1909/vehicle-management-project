-- Backfill operational gates for legacy lanes created before the gate model.
-- Current topology is parking_lots -> zones -> gates -> lanes.

INSERT INTO parking.gates (
    gate_id,
    zone_id,
    code,
    name,
    status,
    created_at
)
SELECT
    gen_random_uuid(),
    zone_item.zone_id,
    LEFT('GATE-' || zone_item.code, 50),
    LEFT('Cong ' || zone_item.name, 150),
    'ACTIVE',
    now()
FROM parking.zones zone_item
WHERE NOT EXISTS (
    SELECT 1
    FROM parking.gates gate_item
    WHERE gate_item.zone_id = zone_item.zone_id
);

WITH lane_parking_lot AS (
    SELECT
        lane_item.lane_id,
        MIN(device_item.parking_lot_id::text)::uuid AS parking_lot_id
    FROM parking.lanes lane_item
    LEFT JOIN hardware.devices device_item
        ON device_item.lane_id = lane_item.lane_id
    WHERE lane_item.gate_id IS NULL
    GROUP BY lane_item.lane_id
),
default_zone AS (
    SELECT zone_item.zone_id
    FROM parking.zones zone_item
    ORDER BY
        CASE zone_item.status WHEN 'ACTIVE' THEN 0 ELSE 1 END,
        zone_item.code,
        zone_item.zone_id
    LIMIT 1
),
lane_zone AS (
    SELECT
        lane_parking_lot.lane_id,
        COALESCE(matched_zone.zone_id, default_zone.zone_id) AS zone_id
    FROM lane_parking_lot
    CROSS JOIN default_zone
    LEFT JOIN LATERAL (
        SELECT zone_item.zone_id
        FROM parking.zones zone_item
        WHERE lane_parking_lot.parking_lot_id IS NOT NULL
          AND zone_item.parking_lot_id = lane_parking_lot.parking_lot_id
        ORDER BY
            CASE zone_item.status WHEN 'ACTIVE' THEN 0 ELSE 1 END,
            zone_item.code,
            zone_item.zone_id
        LIMIT 1
    ) matched_zone ON TRUE
),
lane_gate AS (
    SELECT
        lane_zone.lane_id,
        gate_item.gate_id
    FROM lane_zone
    JOIN LATERAL (
        SELECT gate_item.gate_id
        FROM parking.gates gate_item
        WHERE gate_item.zone_id = lane_zone.zone_id
        ORDER BY
            CASE gate_item.status WHEN 'ACTIVE' THEN 0 ELSE 1 END,
            gate_item.code,
            gate_item.gate_id
        LIMIT 1
    ) gate_item ON TRUE
)
UPDATE parking.lanes lane_item
SET gate_id = lane_gate.gate_id
FROM lane_gate
WHERE lane_item.lane_id = lane_gate.lane_id
  AND lane_item.gate_id IS NULL;

DO $$
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM parking.lanes
            WHERE gate_id IS NULL
        ) THEN
            RAISE EXCEPTION 'Some lanes still have null gate_id. Please create at least one zone and gate before parking operations.';
        END IF;
    END $$;

ALTER TABLE parking.lanes
    ALTER COLUMN gate_id SET NOT NULL;

ALTER TABLE parking.lanes
    DROP CONSTRAINT IF EXISTS fk_lanes_gate;

ALTER TABLE parking.lanes
    ADD CONSTRAINT fk_lanes_gate
        FOREIGN KEY (gate_id)
            REFERENCES parking.gates(gate_id)
            ON DELETE RESTRICT;
