-- Only infer a historical subscription's lot when its catalog owner has
-- exactly one lot. Do not guess for Partners with multiple possible lots.
WITH single_lot_organizations AS (
    SELECT organization_id, (array_agg(parking_lot_id))[1] AS parking_lot_id
    FROM parking.parking_lots
    GROUP BY organization_id
    HAVING count(*) = 1
)
UPDATE access_control.subscriptions subscription
SET parking_lot_id = lot.parking_lot_id
FROM catalog.ticket_types ticket_type
JOIN single_lot_organizations lot ON lot.organization_id = ticket_type.organization_id
WHERE subscription.ticket_type_id = ticket_type.ticket_type_id
  AND subscription.parking_lot_id IS NULL;
