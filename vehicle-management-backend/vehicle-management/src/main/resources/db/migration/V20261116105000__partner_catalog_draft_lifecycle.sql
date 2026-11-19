-- Preserve the publication state of every existing catalog, including internal references.
ALTER TABLE catalog.ticket_types DROP CONSTRAINT ck_ticket_types_status;
ALTER TABLE catalog.ticket_types ADD CONSTRAINT ck_ticket_types_status
    CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE'));
ALTER TABLE catalog.ticket_types ALTER COLUMN status SET DEFAULT 'DRAFT';

ALTER TABLE iam.organizations ADD COLUMN catalog_seeded_at timestamptz;
-- Existing organizations must never be reseeded or have their live catalog reset.
UPDATE iam.organizations SET catalog_seeded_at = created_at;

DO $$
DECLARE item text;
BEGIN
    FOREACH item IN ARRAY ARRAY['vehicle_types', 'price_plans', 'price_rules'] LOOP
        EXECUTE format('ALTER TABLE catalog.%I ADD COLUMN lifecycle_status varchar(20)', item);
        EXECUTE format('UPDATE catalog.%I SET lifecycle_status = CASE WHEN is_active THEN ''ACTIVE'' ELSE ''INACTIVE'' END', item);
        EXECUTE format('ALTER TABLE catalog.%I ALTER COLUMN lifecycle_status SET NOT NULL, ALTER COLUMN is_active SET DEFAULT false', item);
        EXECUTE format('ALTER TABLE catalog.%I ADD CONSTRAINT %I CHECK (lifecycle_status IN (''DRAFT'', ''ACTIVE'', ''INACTIVE'') AND is_active = (lifecycle_status = ''ACTIVE''))', item, 'ck_' || item || '_lifecycle');
    END LOOP;
END $$;

-- Keep legacy bulk deactivation paths consistent during the compatibility period.
CREATE FUNCTION catalog.synchronize_catalog_lifecycle() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        -- New application writes DRAFT explicitly. Legacy trusted writers may still
        -- supply is_active; omitted flags default to a real draft in the database.
        IF NEW.lifecycle_status IS NULL THEN
            NEW.lifecycle_status := CASE WHEN NEW.is_active THEN 'ACTIVE' ELSE 'DRAFT' END;
        END IF;
        NEW.is_active := NEW.lifecycle_status = 'ACTIVE';
    ELSE
        IF NEW.lifecycle_status IS DISTINCT FROM OLD.lifecycle_status THEN
            NEW.is_active := NEW.lifecycle_status = 'ACTIVE';
        ELSIF NEW.is_active IS DISTINCT FROM OLD.is_active THEN
            NEW.lifecycle_status := CASE WHEN NEW.is_active THEN 'ACTIVE' ELSE 'INACTIVE' END;
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_vehicle_types_lifecycle BEFORE INSERT OR UPDATE ON catalog.vehicle_types
FOR EACH ROW EXECUTE FUNCTION catalog.synchronize_catalog_lifecycle();
CREATE TRIGGER trg_price_plans_lifecycle BEFORE INSERT OR UPDATE ON catalog.price_plans
FOR EACH ROW EXECUTE FUNCTION catalog.synchronize_catalog_lifecycle();
CREATE TRIGGER trg_price_rules_lifecycle BEFORE INSERT OR UPDATE ON catalog.price_rules
FOR EACH ROW EXECUTE FUNCTION catalog.synchronize_catalog_lifecycle();

-- Source IDs preserve exact lineage even when multiple ticket versions have the same code.
ALTER TABLE catalog.vehicle_types ADD COLUMN source_catalog_id uuid REFERENCES catalog.vehicle_types(vehicle_type_id);
ALTER TABLE catalog.ticket_types ADD COLUMN source_catalog_id uuid REFERENCES catalog.ticket_types(ticket_type_id);
ALTER TABLE catalog.price_plans ADD COLUMN source_catalog_id uuid REFERENCES catalog.price_plans(price_plan_id);
ALTER TABLE catalog.price_rules ADD COLUMN source_catalog_id uuid REFERENCES catalog.price_rules(price_rule_id);
CREATE UNIQUE INDEX uq_vehicle_types_seed_source ON catalog.vehicle_types (organization_id, source_catalog_id);
CREATE UNIQUE INDEX uq_ticket_types_seed_source ON catalog.ticket_types (organization_id, source_catalog_id);
CREATE UNIQUE INDEX uq_price_plans_seed_source ON catalog.price_plans (organization_id, source_catalog_id);
CREATE UNIQUE INDEX uq_price_rules_seed_source ON catalog.price_rules (organization_id, source_catalog_id);

-- Old lots retain exclusion semantics. New lots explicitly opt in to services.
ALTER TABLE parking.parking_lots ADD COLUMN catalog_explicit_services boolean NOT NULL DEFAULT false;
ALTER TABLE parking.parking_lots ALTER COLUMN catalog_explicit_services SET DEFAULT true;
CREATE TABLE catalog.parking_lot_vehicle_type_enrollments (
    organization_id uuid NOT NULL,
    parking_lot_id uuid NOT NULL,
    vehicle_type_id uuid NOT NULL,
    PRIMARY KEY (parking_lot_id, vehicle_type_id),
    FOREIGN KEY (organization_id, parking_lot_id) REFERENCES parking.parking_lots(organization_id, parking_lot_id) ON DELETE CASCADE,
    FOREIGN KEY (organization_id, vehicle_type_id) REFERENCES catalog.vehicle_types(organization_id, vehicle_type_id) ON DELETE CASCADE
);
CREATE TABLE catalog.parking_lot_ticket_type_enrollments (
    organization_id uuid NOT NULL,
    parking_lot_id uuid NOT NULL,
    ticket_type_id uuid NOT NULL,
    PRIMARY KEY (parking_lot_id, ticket_type_id),
    FOREIGN KEY (organization_id, parking_lot_id) REFERENCES parking.parking_lots(organization_id, parking_lot_id) ON DELETE CASCADE,
    FOREIGN KEY (organization_id, ticket_type_id) REFERENCES catalog.ticket_types(organization_id, ticket_type_id) ON DELETE CASCADE
);

-- Catalog snapshots are retained in the existing audit store in the same transaction.
CREATE FUNCTION catalog.audit_catalog_change() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    before_data jsonb;
    after_data jsonb := to_jsonb(NEW);
    action_name text;
    current_state text;
BEGIN
    current_state := coalesce(after_data ->> 'lifecycle_status', after_data ->> 'status');
    IF TG_OP = 'INSERT' THEN
        action_name := 'CATALOG_CREATED';
    ELSE
        before_data := to_jsonb(OLD);
        IF before_data = after_data THEN RETURN NEW; END IF;
        action_name := CASE
            WHEN current_state = 'ACTIVE' AND current_state IS DISTINCT FROM coalesce(before_data ->> 'lifecycle_status', before_data ->> 'status') THEN 'CATALOG_PUBLISHED'
            WHEN current_state = 'INACTIVE' AND current_state IS DISTINCT FROM coalesce(before_data ->> 'lifecycle_status', before_data ->> 'status') THEN 'CATALOG_DEACTIVATED'
            ELSE 'CATALOG_UPDATED' END;
    END IF;
    INSERT INTO audit.audit_logs (audit_log_id, actor_account_id, action, target_schema, target_table,
        target_id, old_data, new_data, created_at)
    VALUES (gen_random_uuid(), coalesce((after_data ->> 'updated_by')::uuid, (after_data ->> 'created_by')::uuid),
        action_name, TG_TABLE_SCHEMA, TG_TABLE_NAME, (after_data ->> TG_ARGV[0])::uuid,
        before_data, after_data, now());
    RETURN NEW;
END $$;

CREATE TRIGGER trg_vehicle_types_catalog_audit AFTER INSERT OR UPDATE ON catalog.vehicle_types
FOR EACH ROW EXECUTE FUNCTION catalog.audit_catalog_change('vehicle_type_id');
CREATE TRIGGER trg_ticket_types_catalog_audit AFTER INSERT OR UPDATE ON catalog.ticket_types
FOR EACH ROW EXECUTE FUNCTION catalog.audit_catalog_change('ticket_type_id');
CREATE TRIGGER trg_price_plans_catalog_audit AFTER INSERT OR UPDATE ON catalog.price_plans
FOR EACH ROW EXECUTE FUNCTION catalog.audit_catalog_change('price_plan_id');
CREATE TRIGGER trg_price_rules_catalog_audit AFTER INSERT OR UPDATE ON catalog.price_rules
FOR EACH ROW EXECUTE FUNCTION catalog.audit_catalog_change('price_rule_id');
