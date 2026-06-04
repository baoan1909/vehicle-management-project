-- V9: Replace catalog.ticket_types.is_active with status
-- Rule: only one ACTIVE ticket type per code, but many INACTIVE rows can share the same code.

ALTER TABLE catalog.ticket_types
    ADD COLUMN IF NOT EXISTS status VARCHAR(20);

UPDATE catalog.ticket_types
SET status = CASE
                 WHEN is_active IS TRUE THEN 'ACTIVE'
                 ELSE 'INACTIVE'
    END
WHERE status IS NULL;

ALTER TABLE catalog.ticket_types
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE catalog.ticket_types
    ALTER COLUMN status SET NOT NULL;

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'ck_ticket_types_status'
        ) THEN
            ALTER TABLE catalog.ticket_types
                ADD CONSTRAINT ck_ticket_types_status
                    CHECK (status IN ('ACTIVE', 'INACTIVE'));
        END IF;
    END $$;

-- Drop old unique constraint on code, because code is no longer globally unique.
DO $$
    DECLARE
        unique_constraint RECORD;
    BEGIN
        FOR unique_constraint IN
            SELECT con.conname
            FROM pg_constraint con
                     JOIN pg_class rel ON rel.oid = con.conrelid
                     JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
            WHERE nsp.nspname = 'catalog'
              AND rel.relname = 'ticket_types'
              AND con.contype = 'u'
              AND (
                      SELECT array_agg(att.attname ORDER BY att.attname)
                      FROM unnest(con.conkey) AS key(attnum)
                               JOIN pg_attribute att
                                    ON att.attrelid = rel.oid
                                        AND att.attnum = key.attnum
                  ) = ARRAY['code']::name[]
            LOOP
                EXECUTE format(
                        'ALTER TABLE catalog.ticket_types DROP CONSTRAINT IF EXISTS %I',
                        unique_constraint.conname
                        );
            END LOOP;
    END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ticket_types_active_code
    ON catalog.ticket_types (code)
    WHERE status = 'ACTIVE';

ALTER TABLE catalog.ticket_types
    DROP COLUMN IF EXISTS is_active;