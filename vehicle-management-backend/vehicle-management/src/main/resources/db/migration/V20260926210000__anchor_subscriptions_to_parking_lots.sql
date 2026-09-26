-- Customer vehicles remain platform-wide. Partner vehicle types copied from the
-- original catalog retain a stable link to that canonical vehicle classification.
ALTER TABLE catalog.vehicle_types ADD COLUMN canonical_vehicle_type_id uuid;

UPDATE catalog.vehicle_types
SET canonical_vehicle_type_id = vehicle_type_id
WHERE organization_id = '00000000-0000-0000-0000-000000009001';

UPDATE catalog.vehicle_types partner_type
SET canonical_vehicle_type_id = canonical_type.vehicle_type_id
FROM catalog.vehicle_types canonical_type
WHERE canonical_type.organization_id = '00000000-0000-0000-0000-000000009001'
  AND partner_type.organization_id <> canonical_type.organization_id
  AND partner_type.code = canonical_type.code;

ALTER TABLE catalog.vehicle_types ADD CONSTRAINT fk_vehicle_types_canonical
    FOREIGN KEY (canonical_vehicle_type_id) REFERENCES catalog.vehicle_types(vehicle_type_id);
CREATE INDEX idx_vehicle_types_org_canonical
    ON catalog.vehicle_types (organization_id, canonical_vehicle_type_id);

-- Historical subscriptions without a determinable card/lot relationship remain
-- unassigned. New registrations must provide a lot in the application layer.
ALTER TABLE access_control.subscriptions ADD COLUMN parking_lot_id uuid;

UPDATE access_control.subscriptions subscription
SET parking_lot_id = card.parking_lot_id
FROM access_control.cards card
WHERE subscription.card_id = card.card_id
  AND card.parking_lot_id IS NOT NULL;

ALTER TABLE access_control.subscriptions ADD CONSTRAINT fk_subscriptions_parking_lot
    FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE RESTRICT;
CREATE INDEX idx_subscriptions_parking_lot_status
    ON access_control.subscriptions (parking_lot_id, status);
