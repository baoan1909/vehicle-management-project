-- A zone can serve several vehicle categories.  Keep the existing column as a
-- backward-compatible primary value while the association table is canonical.
CREATE TABLE IF NOT EXISTS parking.zone_vehicle_types (
    zone_id UUID NOT NULL,
    vehicle_type_id UUID NOT NULL,
    PRIMARY KEY (zone_id, vehicle_type_id),
    CONSTRAINT fk_zone_vehicle_types_zone
        FOREIGN KEY (zone_id) REFERENCES parking.zones(zone_id) ON DELETE CASCADE,
    CONSTRAINT fk_zone_vehicle_types_vehicle_type
        FOREIGN KEY (vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id) ON DELETE RESTRICT
);

INSERT INTO parking.zone_vehicle_types (zone_id, vehicle_type_id)
SELECT zone_id, vehicle_type_id
FROM parking.zones
WHERE vehicle_type_id IS NOT NULL
ON CONFLICT DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_zone_vehicle_types_vehicle_type
    ON parking.zone_vehicle_types(vehicle_type_id);
