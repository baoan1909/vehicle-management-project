ALTER TABLE catalog.vouchers
    ALTER COLUMN valid_to DROP NOT NULL;

ALTER TABLE catalog.vouchers
    DROP CONSTRAINT IF EXISTS ck_vouchers_valid_period;

ALTER TABLE catalog.vouchers
    ADD CONSTRAINT ck_vouchers_valid_period
        CHECK (valid_to IS NULL OR valid_to > valid_from);

COMMENT ON COLUMN catalog.vouchers.valid_to IS
    'Optional voucher expiration time. NULL means the voucher has no expiration date.';
