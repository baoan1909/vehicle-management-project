BEGIN;

-- Allow DRAFT while preserving all existing publication states.
ALTER TABLE catalog.ticket_types
DROP CONSTRAINT IF EXISTS ck_ticket_types_status;

ALTER TABLE catalog.ticket_types
    ADD CONSTRAINT ck_ticket_types_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE'));

ALTER TABLE catalog.ticket_types
    ALTER COLUMN status SET DEFAULT 'DRAFT';

-- Mark existing organizations as seeded only when introducing this column.
-- On reruns, preserve NULL for organizations still awaiting initial seeding.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_attribute
        WHERE attrelid = 'iam.organizations'::regclass
          AND attname = 'catalog_seeded_at'
          AND NOT attisdropped
    ) THEN
ALTER TABLE iam.organizations
    ADD COLUMN catalog_seeded_at timestamptz;

UPDATE iam.organizations
SET catalog_seeded_at = created_at
WHERE catalog_seeded_at IS NULL;
END IF;
END;
$$;

-- Introduce lifecycle status without overwriting existing lifecycle values.
DO $$
DECLARE
item text;
    constraint_name text;
BEGIN
    FOREACH item IN ARRAY ARRAY[
        'vehicle_types',
        'price_plans',
        'price_rules'
    ] LOOP
        EXECUTE format(
            'ALTER TABLE catalog.%I
             ADD COLUMN IF NOT EXISTS lifecycle_status varchar(20)',
            item
        );

EXECUTE format(
        'UPDATE catalog.%I
         SET lifecycle_status =
             CASE WHEN is_active THEN ''ACTIVE'' ELSE ''INACTIVE'' END
         WHERE lifecycle_status IS NULL',
        item
        );

EXECUTE format(
        'ALTER TABLE catalog.%I
         ALTER COLUMN lifecycle_status SET NOT NULL,
         ALTER COLUMN is_active SET DEFAULT false',
        item
        );

constraint_name := 'ck_' || item || '_lifecycle';

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conrelid = format('catalog.%I', item)::regclass
              AND conname = constraint_name
        ) THEN
            EXECUTE format(
                'ALTER TABLE catalog.%I
                 ADD CONSTRAINT %I
                 CHECK (
                     lifecycle_status IN (''DRAFT'', ''ACTIVE'', ''INACTIVE'')
                     AND is_active = (lifecycle_status = ''ACTIVE'')
                 )',
                item,
                constraint_name
            );
END IF;
END LOOP;
END;
$$;

-- Keep legacy writes consistent with lifecycle status.
CREATE OR REPLACE FUNCTION catalog.synchronize_catalog_lifecycle()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        -- New application writes DRAFT explicitly.
        -- Legacy writers may supply is_active instead.
        IF NEW.lifecycle_status IS NULL THEN
            NEW.lifecycle_status :=
                CASE WHEN NEW.is_active THEN 'ACTIVE' ELSE 'DRAFT' END;
END IF;

        NEW.is_active := NEW.lifecycle_status = 'ACTIVE';
ELSE
        IF NEW.lifecycle_status IS DISTINCT FROM OLD.lifecycle_status THEN
            NEW.is_active := NEW.lifecycle_status = 'ACTIVE';
        ELSIF NEW.is_active IS DISTINCT FROM OLD.is_active THEN
            NEW.lifecycle_status :=
                CASE WHEN NEW.is_active THEN 'ACTIVE' ELSE 'INACTIVE' END;
END IF;
END IF;

RETURN NEW;
END;
$$;

DO $$
DECLARE
item text;
    trigger_name text;
BEGIN
    FOREACH item IN ARRAY ARRAY[
        'vehicle_types',
        'price_plans',
        'price_rules'
    ] LOOP
        trigger_name := 'trg_' || item || '_lifecycle';

        IF NOT EXISTS (
            SELECT 1
            FROM pg_trigger
            WHERE tgrelid = format('catalog.%I', item)::regclass
              AND tgname = trigger_name
        ) THEN
            EXECUTE format(
                'CREATE TRIGGER %I
                 BEFORE INSERT OR UPDATE ON catalog.%I
                 FOR EACH ROW
                 EXECUTE FUNCTION catalog.synchronize_catalog_lifecycle()',
                trigger_name,
                item
            );
END IF;
END LOOP;
END;
$$;

-- Preserve exact source lineage.
-- Check foreign keys separately so an existing column can still receive its FK.
DO $$
DECLARE
item record;
    constraint_name text;
BEGIN
FOR item IN
SELECT *
FROM (
         VALUES
             ('vehicle_types', 'vehicle_type_id'),
             ('ticket_types', 'ticket_type_id'),
             ('price_plans', 'price_plan_id'),
             ('price_rules', 'price_rule_id')
     ) AS mapping(table_name, id_column)
    LOOP
        EXECUTE format(
            'ALTER TABLE catalog.%I
             ADD COLUMN IF NOT EXISTS source_catalog_id uuid',
            item.table_name
        );

constraint_name :=
            item.table_name || '_source_catalog_id_fkey';

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conrelid =
                format('catalog.%I', item.table_name)::regclass
              AND conname = constraint_name
        ) THEN
            EXECUTE format(
                'ALTER TABLE catalog.%I
                 ADD CONSTRAINT %I
                 FOREIGN KEY (source_catalog_id)
                 REFERENCES catalog.%I (%I)',
                item.table_name,
                constraint_name,
                item.table_name,
                item.id_column
            );
END IF;

EXECUTE format(
        'CREATE UNIQUE INDEX IF NOT EXISTS %I
         ON catalog.%I (organization_id, source_catalog_id)',
        'uq_' || item.table_name || '_seed_source',
        item.table_name
        );
END LOOP;
END;
$$;

-- Existing lots retain exclusion semantics.
-- Newly inserted lots explicitly opt in to services.
ALTER TABLE parking.parking_lots
    ADD COLUMN IF NOT EXISTS catalog_explicit_services
    boolean NOT NULL DEFAULT false;

ALTER TABLE parking.parking_lots
    ALTER COLUMN catalog_explicit_services SET DEFAULT true;

CREATE TABLE IF NOT EXISTS catalog.parking_lot_vehicle_type_enrollments (
                                                                            organization_id uuid NOT NULL,
                                                                            parking_lot_id uuid NOT NULL,
                                                                            vehicle_type_id uuid NOT NULL,

                                                                            PRIMARY KEY (parking_lot_id, vehicle_type_id),

    FOREIGN KEY (organization_id, parking_lot_id)
    REFERENCES parking.parking_lots (
                                        organization_id, parking_lot_id
                                    )
    ON DELETE CASCADE,

    FOREIGN KEY (organization_id, vehicle_type_id)
    REFERENCES catalog.vehicle_types (
                                         organization_id, vehicle_type_id
                                     )
    ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS catalog.parking_lot_ticket_type_enrollments (
                                                                           organization_id uuid NOT NULL,
                                                                           parking_lot_id uuid NOT NULL,
                                                                           ticket_type_id uuid NOT NULL,

                                                                           PRIMARY KEY (parking_lot_id, ticket_type_id),

    FOREIGN KEY (organization_id, parking_lot_id)
    REFERENCES parking.parking_lots (
                                        organization_id, parking_lot_id
                                    )
    ON DELETE CASCADE,

    FOREIGN KEY (organization_id, ticket_type_id)
    REFERENCES catalog.ticket_types (
                                        organization_id, ticket_type_id
                                    )
    ON DELETE CASCADE
    );

-- Retain catalog snapshots in the audit store in the same transaction.
CREATE OR REPLACE FUNCTION catalog.audit_catalog_change()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
before_data jsonb;
    after_data jsonb := to_jsonb(NEW);
    action_name text;
    current_state text;
BEGIN
    current_state := coalesce(
        after_data ->> 'lifecycle_status',
        after_data ->> 'status'
    );

    IF TG_OP = 'INSERT' THEN
        action_name := 'CATALOG_CREATED';
ELSE
        before_data := to_jsonb(OLD);

        IF before_data = after_data THEN
            RETURN NEW;
END IF;

        action_name := CASE
            WHEN current_state = 'ACTIVE'
                AND current_state IS DISTINCT FROM coalesce(
                    before_data ->> 'lifecycle_status',
                    before_data ->> 'status'
                )
                THEN 'CATALOG_PUBLISHED'

            WHEN current_state = 'INACTIVE'
                AND current_state IS DISTINCT FROM coalesce(
                    before_data ->> 'lifecycle_status',
                    before_data ->> 'status'
                )
                THEN 'CATALOG_DEACTIVATED'

            ELSE 'CATALOG_UPDATED'
END;
END IF;

INSERT INTO audit.audit_logs (
    audit_log_id,
    actor_account_id,
    action,
    target_schema,
    target_table,
    target_id,
    old_data,
    new_data,
    created_at
)
VALUES (
           gen_random_uuid(),
           coalesce(
                   (after_data ->> 'updated_by')::uuid,
                   (after_data ->> 'created_by')::uuid
           ),
           action_name,
           TG_TABLE_SCHEMA,
           TG_TABLE_NAME,
           (after_data ->> TG_ARGV[0])::uuid,
           before_data,
           after_data,
           now()
       );

RETURN NEW;
END;
$$;

DO $$
DECLARE
item record;
    trigger_name text;
BEGIN
FOR item IN
SELECT *
FROM (
         VALUES
             ('vehicle_types', 'vehicle_type_id'),
             ('ticket_types', 'ticket_type_id'),
             ('price_plans', 'price_plan_id'),
             ('price_rules', 'price_rule_id')
     ) AS mapping(table_name, id_column)
    LOOP
        trigger_name := 'trg_' || item.table_name || '_catalog_audit';

IF NOT EXISTS (
            SELECT 1
            FROM pg_trigger
            WHERE tgrelid =
                format('catalog.%I', item.table_name)::regclass
              AND tgname = trigger_name
        ) THEN
            EXECUTE format(
                'CREATE TRIGGER %I
                 AFTER INSERT OR UPDATE ON catalog.%I
                 FOR EACH ROW
                 EXECUTE FUNCTION catalog.audit_catalog_change(%L)',
                trigger_name,
                item.table_name,
                item.id_column
            );
END IF;
END LOOP;
END;
$$;

COMMIT;