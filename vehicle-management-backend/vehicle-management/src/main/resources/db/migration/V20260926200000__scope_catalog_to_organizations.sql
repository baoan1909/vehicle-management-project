-- Preserve the original IDs as the internal catalog so historical references remain valid.
-- Each existing Partner receives independent copies; later Partners are seeded at creation.
ALTER TABLE catalog.vehicle_types ADD COLUMN organization_id uuid;
ALTER TABLE catalog.ticket_types ADD COLUMN organization_id uuid;
ALTER TABLE catalog.price_plans ADD COLUMN organization_id uuid;
ALTER TABLE catalog.price_rules ADD COLUMN organization_id uuid;

UPDATE catalog.vehicle_types SET organization_id = '00000000-0000-0000-0000-000000009001';
UPDATE catalog.ticket_types SET organization_id = '00000000-0000-0000-0000-000000009001';
UPDATE catalog.price_plans SET organization_id = '00000000-0000-0000-0000-000000009001';
UPDATE catalog.price_rules SET organization_id = '00000000-0000-0000-0000-000000009001';

ALTER TABLE catalog.vehicle_types ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE catalog.ticket_types ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE catalog.price_plans ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE catalog.price_rules ALTER COLUMN organization_id SET NOT NULL;

ALTER TABLE catalog.vehicle_types ADD CONSTRAINT fk_vehicle_types_organization
    FOREIGN KEY (organization_id) REFERENCES iam.organizations(organization_id);
ALTER TABLE catalog.ticket_types ADD CONSTRAINT fk_ticket_types_organization
    FOREIGN KEY (organization_id) REFERENCES iam.organizations(organization_id);
ALTER TABLE catalog.price_plans ADD CONSTRAINT fk_price_plans_organization
    FOREIGN KEY (organization_id) REFERENCES iam.organizations(organization_id);
ALTER TABLE catalog.price_rules ADD CONSTRAINT fk_price_rules_organization
    FOREIGN KEY (organization_id) REFERENCES iam.organizations(organization_id);

ALTER TABLE catalog.vehicle_types DROP CONSTRAINT vehicle_types_code_key;
ALTER TABLE catalog.price_plans DROP CONSTRAINT price_plans_code_key;
DROP INDEX catalog.uq_ticket_types_active_code;

ALTER TABLE catalog.vehicle_types ADD CONSTRAINT uq_vehicle_types_org_code UNIQUE (organization_id, code);
ALTER TABLE catalog.price_plans ADD CONSTRAINT uq_price_plans_org_code UNIQUE (organization_id, code);
CREATE UNIQUE INDEX uq_ticket_types_org_active_code
    ON catalog.ticket_types (organization_id, code) WHERE status = 'ACTIVE';

-- Composite keys ensure a rule cannot combine a plan, vehicle type and ticket type
-- belonging to different Partners, even when an application caller sends foreign IDs.
ALTER TABLE catalog.vehicle_types ADD CONSTRAINT uq_vehicle_types_org_id UNIQUE (organization_id, vehicle_type_id);
ALTER TABLE catalog.ticket_types ADD CONSTRAINT uq_ticket_types_org_id UNIQUE (organization_id, ticket_type_id);
ALTER TABLE catalog.price_plans ADD CONSTRAINT uq_price_plans_org_id UNIQUE (organization_id, price_plan_id);
ALTER TABLE catalog.price_rules ADD CONSTRAINT fk_price_rules_org_plan
    FOREIGN KEY (organization_id, price_plan_id) REFERENCES catalog.price_plans(organization_id, price_plan_id);
ALTER TABLE catalog.price_rules ADD CONSTRAINT fk_price_rules_org_vehicle_type
    FOREIGN KEY (organization_id, vehicle_type_id) REFERENCES catalog.vehicle_types(organization_id, vehicle_type_id);
ALTER TABLE catalog.price_rules ADD CONSTRAINT fk_price_rules_org_ticket_type
    FOREIGN KEY (organization_id, ticket_type_id) REFERENCES catalog.ticket_types(organization_id, ticket_type_id);

CREATE INDEX idx_vehicle_types_organization ON catalog.vehicle_types (organization_id);
CREATE INDEX idx_ticket_types_organization ON catalog.ticket_types (organization_id);
CREATE INDEX idx_price_plans_organization ON catalog.price_plans (organization_id);
CREATE INDEX idx_price_rules_organization ON catalog.price_rules (organization_id);

CREATE TEMP TABLE catalog_vehicle_copy (organization_id uuid, source_id uuid, copy_id uuid) ON COMMIT DROP;
CREATE TEMP TABLE catalog_ticket_copy (organization_id uuid, source_id uuid, copy_id uuid) ON COMMIT DROP;
CREATE TEMP TABLE catalog_plan_copy (organization_id uuid, source_id uuid, copy_id uuid) ON COMMIT DROP;

INSERT INTO catalog_vehicle_copy
SELECT organization.organization_id, source.vehicle_type_id, gen_random_uuid()
FROM iam.organizations organization CROSS JOIN catalog.vehicle_types source
WHERE organization.organization_id <> '00000000-0000-0000-0000-000000009001'
  AND source.organization_id = '00000000-0000-0000-0000-000000009001';

INSERT INTO catalog_ticket_copy
SELECT organization.organization_id, source.ticket_type_id, gen_random_uuid()
FROM iam.organizations organization CROSS JOIN catalog.ticket_types source
WHERE organization.organization_id <> '00000000-0000-0000-0000-000000009001'
  AND source.organization_id = '00000000-0000-0000-0000-000000009001';

INSERT INTO catalog_plan_copy
SELECT organization.organization_id, source.price_plan_id, gen_random_uuid()
FROM iam.organizations organization CROSS JOIN catalog.price_plans source
WHERE organization.organization_id <> '00000000-0000-0000-0000-000000009001'
  AND source.organization_id = '00000000-0000-0000-0000-000000009001';

INSERT INTO catalog.vehicle_types
    (vehicle_type_id, organization_id, code, name, description, is_active, created_at)
SELECT copy.copy_id, copy.organization_id, source.code, source.name, source.description, source.is_active, now()
FROM catalog_vehicle_copy copy JOIN catalog.vehicle_types source ON source.vehicle_type_id = copy.source_id;

INSERT INTO catalog.ticket_types
    (ticket_type_id, organization_id, code, name, description, duration_days, status, created_at)
SELECT copy.copy_id, copy.organization_id, source.code, source.name, source.description,
       source.duration_days, source.status, now()
FROM catalog_ticket_copy copy JOIN catalog.ticket_types source ON source.ticket_type_id = copy.source_id;

INSERT INTO catalog.price_plans
    (price_plan_id, organization_id, code, name, description, applies_to,
     effective_from, effective_to, is_active, created_at)
SELECT copy.copy_id, copy.organization_id, source.code, source.name, source.description,
       source.applies_to, source.effective_from, source.effective_to, source.is_active, now()
FROM catalog_plan_copy copy JOIN catalog.price_plans source ON source.price_plan_id = copy.source_id;

INSERT INTO catalog.price_rules
    (price_rule_id, organization_id, price_plan_id, vehicle_type_id, ticket_type_id,
     rule_name, time_from, time_to, base_price, unit, lost_card_fee, priority, is_active, created_at)
SELECT gen_random_uuid(), plan_copy.organization_id, plan_copy.copy_id, vehicle_copy.copy_id,
       ticket_copy.copy_id, source.rule_name, source.time_from, source.time_to,
       source.base_price, source.unit, source.lost_card_fee, source.priority, source.is_active, now()
FROM catalog.price_rules source
JOIN catalog_plan_copy plan_copy ON plan_copy.source_id = source.price_plan_id
JOIN catalog_vehicle_copy vehicle_copy ON vehicle_copy.organization_id = plan_copy.organization_id
    AND vehicle_copy.source_id = source.vehicle_type_id
LEFT JOIN catalog_ticket_copy ticket_copy ON ticket_copy.organization_id = plan_copy.organization_id
    AND ticket_copy.source_id = source.ticket_type_id
WHERE source.organization_id = '00000000-0000-0000-0000-000000009001';

-- Action grants are independent of row scope; scope is resolved from membership or assignment.
INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role JOIN iam.permissions permission ON permission.permission_code IN (
    'VEHICLE_TYPE_CREATE_ALL', 'VEHICLE_TYPE_READ_ALL', 'VEHICLE_TYPE_UPDATE_ALL', 'VEHICLE_TYPE_DELETE_ALL',
    'TICKET_TYPE_CREATE_ALL', 'TICKET_TYPE_READ_ALL', 'TICKET_TYPE_UPDATE_ALL', 'TICKET_TYPE_DELETE_ALL',
    'PRICE_PLAN_CREATE_ALL', 'PRICE_PLAN_READ_ALL', 'PRICE_PLAN_UPDATE_ALL', 'PRICE_PLAN_DELETE_ALL',
    'PRICE_RULE_CREATE_ALL', 'PRICE_RULE_READ_ALL', 'PRICE_RULE_UPDATE_ALL', 'PRICE_RULE_DELETE_ALL'
)
WHERE role.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, updated_at = now();

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role JOIN iam.permissions permission ON permission.permission_code IN (
    'VEHICLE_TYPE_READ_ALL', 'TICKET_TYPE_READ_ALL', 'PRICE_PLAN_READ_ALL', 'PRICE_RULE_READ_ALL'
)
WHERE role.code IN ('SYSTEM_ADMIN', 'PARKING_MANAGER')
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, updated_at = now();

UPDATE iam.role_permissions role_permission SET is_active = false, updated_at = now()
FROM iam.roles role JOIN iam.permissions permission ON true
WHERE role_permission.role_id = role.role_id AND role_permission.permission_id = permission.permission_id
  AND role.code = 'SYSTEM_ADMIN'
  AND permission.permission_code IN (
      'VEHICLE_TYPE_CREATE_ALL', 'VEHICLE_TYPE_UPDATE_ALL', 'VEHICLE_TYPE_DELETE_ALL',
      'TICKET_TYPE_CREATE_ALL', 'TICKET_TYPE_UPDATE_ALL', 'TICKET_TYPE_DELETE_ALL',
      'PRICE_PLAN_CREATE_ALL', 'PRICE_PLAN_UPDATE_ALL', 'PRICE_PLAN_DELETE_ALL',
      'PRICE_RULE_CREATE_ALL', 'PRICE_RULE_UPDATE_ALL', 'PRICE_RULE_DELETE_ALL'
  );
