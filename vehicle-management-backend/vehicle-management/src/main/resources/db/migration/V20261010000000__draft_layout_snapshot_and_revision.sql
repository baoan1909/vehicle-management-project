-- Phase 2 remainder: draft/layout isolation, optimistic locking revision.
-- Draft items carry their own snapshot (code, vehicle type, geometry) so saving
-- a draft never touches operational parking_spaces. parking_space_id stays NULL
-- until publish materializes the space.

ALTER TABLE parking.parking_space_layout_items
    ADD COLUMN code VARCHAR(50),
    ADD COLUMN vehicle_type_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_layout_items_vehicle_type') THEN
        ALTER TABLE parking.parking_space_layout_items
            ADD CONSTRAINT fk_layout_items_vehicle_type
            FOREIGN KEY (vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE SET NULL;
    END IF;
END
$$;

ALTER TABLE parking.parking_space_layout_items
    ALTER COLUMN parking_space_id DROP NOT NULL;

UPDATE parking.parking_space_layout_items item
SET code = space.code,
    vehicle_type_id = space.vehicle_type_id
FROM parking.parking_spaces space
WHERE space.parking_space_id = item.parking_space_id
  AND item.code IS NULL;

ALTER TABLE parking.parking_space_layout_items
    ALTER COLUMN code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_layout_items_version_code
    ON parking.parking_space_layout_items (layout_version_id, code);

ALTER TABLE parking.parking_layout_versions
    ADD COLUMN revision BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN last_saved_at TIMESTAMPTZ;
