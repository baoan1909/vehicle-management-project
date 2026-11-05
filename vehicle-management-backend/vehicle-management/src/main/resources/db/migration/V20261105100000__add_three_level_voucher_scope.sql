-- Three-level voucher ownership: platform, Partner, or a single parking lot.
-- Existing vouchers remain platform campaigns. Forward-only and idempotent.

ALTER TABLE catalog.vouchers
    ADD COLUMN IF NOT EXISTS scope_type varchar(20),
    ADD COLUMN IF NOT EXISTS organization_id uuid,
    ADD COLUMN IF NOT EXISTS parking_lot_id uuid;

UPDATE catalog.vouchers
SET scope_type = 'PLATFORM',
    organization_id = NULL,
    parking_lot_id = NULL
WHERE scope_type IS NULL;

ALTER TABLE catalog.vouchers
    ALTER COLUMN scope_type SET DEFAULT 'PLATFORM',
    ALTER COLUMN scope_type SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_vouchers_scope_owner'
          AND conrelid = 'catalog.vouchers'::regclass
    ) THEN
        ALTER TABLE catalog.vouchers
            ADD CONSTRAINT ck_vouchers_scope_owner CHECK (
                (scope_type = 'PLATFORM' AND organization_id IS NULL AND parking_lot_id IS NULL)
                OR (scope_type = 'PARTNER' AND organization_id IS NOT NULL AND parking_lot_id IS NULL)
                OR (scope_type = 'PARKING_LOT' AND organization_id IS NOT NULL AND parking_lot_id IS NOT NULL)
            );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_vouchers_organization'
          AND conrelid = 'catalog.vouchers'::regclass
    ) THEN
        ALTER TABLE catalog.vouchers
            ADD CONSTRAINT fk_vouchers_organization
            FOREIGN KEY (organization_id) REFERENCES iam.organizations(organization_id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_vouchers_parking_lot'
          AND conrelid = 'catalog.vouchers'::regclass
    ) THEN
        ALTER TABLE catalog.vouchers
            ADD CONSTRAINT fk_vouchers_parking_lot
            FOREIGN KEY (parking_lot_id) REFERENCES parking.parking_lots(parking_lot_id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_vouchers_scope_type ON catalog.vouchers(scope_type);
CREATE INDEX IF NOT EXISTS idx_vouchers_organization ON catalog.vouchers(organization_id)
    WHERE organization_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_vouchers_parking_lot ON catalog.vouchers(parking_lot_id)
    WHERE parking_lot_id IS NOT NULL;

-- Reuse the existing voucher actions. Tenant/parking-lot ownership is enforced
-- by VoucherAccessGuard, not by creating duplicate functional permissions.
INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'VOUCHER_CREATE_ALL', 'VOUCHER_READ_ALL', 'VOUCHER_UPDATE_ALL'
)
WHERE role.code IN ('PARTNER_ADMIN', 'PARKING_MANAGER')
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
