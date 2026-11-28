BEGIN;

ALTER TABLE catalog.price_rules
    ADD COLUMN IF NOT EXISTS parking_lot_id uuid;

-- Preserve original rule IDs for historical subscription/renewal references.
-- Copy each active Partner-level rule to every existing parking lot.
INSERT INTO catalog.price_rules (
    price_rule_id, organization_id, parking_lot_id, price_plan_id,
    vehicle_type_id, ticket_type_id, rule_name, time_from, time_to,
    base_price, unit, lost_card_fee, priority, is_active,
    created_at, created_by, updated_at, updated_by
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
    source.is_active,
    now(),
    source.created_by,
    source.updated_at,
    source.updated_by
FROM catalog.price_rules AS source
         JOIN parking.parking_lots AS lot
              ON lot.organization_id = source.organization_id
WHERE source.parking_lot_id IS NULL
  AND source.is_active = true;

UPDATE catalog.price_rules
SET is_active = false,
    updated_at = now()
WHERE parking_lot_id IS NULL
  AND is_active = true;

-- PostgreSQL requires an explicit existence check for named constraints.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'catalog.price_rules'::regclass
          AND conname = 'fk_price_rules_org_parking_lot'
    ) THEN
ALTER TABLE catalog.price_rules
    ADD CONSTRAINT fk_price_rules_org_parking_lot
        FOREIGN KEY (organization_id, parking_lot_id)
            REFERENCES parking.parking_lots (
                                             organization_id, parking_lot_id
                );
END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'catalog.price_rules'::regclass
          AND conname = 'ck_active_price_rules_require_parking_lot'
    ) THEN
ALTER TABLE catalog.price_rules
    ADD CONSTRAINT ck_active_price_rules_require_parking_lot
        CHECK (is_active = false OR parking_lot_id IS NOT NULL);
END IF;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_price_rules_parking_lot
    ON catalog.price_rules (parking_lot_id);

CREATE INDEX IF NOT EXISTS idx_price_rules_lot_active_lookup
    ON catalog.price_rules (
    parking_lot_id, vehicle_type_id, ticket_type_id, is_active
    );

-- Managers may maintain rules within their assigned parking-lot scope.
-- Price plans remain read-only for Managers.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, is_active, is_system
)
SELECT
    gen_random_uuid(),
    r.role_id,
    p.permission_id,
    now(),
    true,
    true
FROM iam.roles AS r
         CROSS JOIN iam.permissions AS p
WHERE r.code = 'PARKING_MANAGER'
  AND p.permission_code IN (
                            'PRICE_RULE_CREATE_ALL',
                            'PRICE_RULE_READ_ALL',
                            'PRICE_RULE_UPDATE_ALL',
                            'PRICE_RULE_DELETE_ALL'
    )
    ON CONFLICT (role_id, permission_id)
DO UPDATE SET
    is_active = true,
           updated_at = now();

-- Keep System Admin read-only for price plans and price rules.
UPDATE iam.role_permissions AS rp
SET is_active = false,
    updated_at = now()
    FROM iam.roles AS r
CROSS JOIN iam.permissions AS p
WHERE rp.role_id = r.role_id
  AND rp.permission_id = p.permission_id
  AND r.code = 'SYSTEM_ADMIN'
  AND p.permission_code IN (
    'PRICE_PLAN_CREATE_ALL',
    'PRICE_PLAN_UPDATE_ALL',
    'PRICE_PLAN_DELETE_ALL',
    'PRICE_RULE_CREATE_ALL',
    'PRICE_RULE_UPDATE_ALL',
    'PRICE_RULE_DELETE_ALL'
    );

-- Price plans remain Partner-owned.
-- Remove any existing Manager write permissions for price plans.
UPDATE iam.role_permissions AS rp
SET is_active = false,
    updated_at = now()
    FROM iam.roles AS r
CROSS JOIN iam.permissions AS p
WHERE rp.role_id = r.role_id
  AND rp.permission_id = p.permission_id
  AND r.code = 'PARKING_MANAGER'
  AND p.permission_code IN (
    'PRICE_PLAN_CREATE_ALL',
    'PRICE_PLAN_UPDATE_ALL',
    'PRICE_PLAN_DELETE_ALL'
    );

COMMIT;