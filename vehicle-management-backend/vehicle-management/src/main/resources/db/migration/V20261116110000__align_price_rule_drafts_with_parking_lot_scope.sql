BEGIN;

-- Price Rules are parking-lot owned.
-- Shared Partner catalog seeding must not create organization-level rules.
-- Each lot may initialize the same internal templates.
DROP INDEX IF EXISTS catalog.uq_price_rules_seed_source;

CREATE UNIQUE INDEX IF NOT EXISTS uq_price_rules_lot_seed_source
    ON catalog.price_rules (parking_lot_id, source_catalog_id)
    WHERE parking_lot_id IS NOT NULL
    AND source_catalog_id IS NOT NULL;

-- Preserve drafts from the previous seed implementation by creating
-- an independent copy for every existing lot of the same Partner.
INSERT INTO catalog.price_rules (
    price_rule_id,
    organization_id,
    parking_lot_id,
    price_plan_id,
    vehicle_type_id,
    ticket_type_id,
    rule_name,
    time_from,
    time_to,
    base_price,
    unit,
    lost_card_fee,
    priority,
    is_active,
    lifecycle_status,
    source_catalog_id,
    created_at,
    created_by,
    updated_at,
    updated_by
)
SELECT
    gen_random_uuid(),
    source.organization_id,
    lot.parking_lot_id,
    source.price_plan_id,
    source.vehicle_type_id,
    source.ticket_type_id,
    source.rule_name,
    source.time_from,
    source.time_to,
    source.base_price,
    source.unit,
    source.lost_card_fee,
    source.priority,
    false,
    'DRAFT',
    source.source_catalog_id,
    now(),
    source.created_by,
    source.updated_at,
    source.updated_by
FROM catalog.price_rules AS source
         JOIN parking.parking_lots AS lot
              ON lot.organization_id = source.organization_id
         JOIN iam.organizations AS org
              ON org.organization_id = source.organization_id
WHERE source.parking_lot_id IS NULL
  AND source.lifecycle_status = 'DRAFT'
  AND source.source_catalog_id IS NOT NULL
  AND org.code <> 'COPARKING_INTERNAL'
    ON CONFLICT (parking_lot_id, source_catalog_id)
WHERE parking_lot_id IS NOT NULL
  AND source_catalog_id IS NOT NULL
    DO NOTHING;

-- Null-lot Partner rules are legacy records only.
-- They must never be activated or returned as editable configuration.
UPDATE catalog.price_rules AS rule
SET lifecycle_status = 'INACTIVE',
    updated_at = now()
    FROM iam.organizations AS org
WHERE org.organization_id = rule.organization_id
  AND org.code <> 'COPARKING_INTERNAL'
  AND rule.parking_lot_id IS NULL
  AND rule.lifecycle_status <> 'INACTIVE';

COMMIT;