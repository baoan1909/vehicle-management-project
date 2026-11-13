DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM billing.payments
        WHERE payment_method IN ('PAYOS', 'STRIPE')
    ) THEN
        RAISE EXCEPTION
            'Cannot retire PAYOS/STRIPE payment methods while historical payment rows still exist';
    END IF;
END $$;

ALTER TABLE billing.payments DROP CONSTRAINT IF EXISTS ck_payments_method;
ALTER TABLE billing.payments ADD CONSTRAINT ck_payments_method CHECK (payment_method IN (
    'CASH', 'QR', 'BANK_TRANSFER', 'MOMO', 'VNPAY', 'SEPAY', 'WALLET'
));

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_sepay_provider_transaction
    ON billing.payments(provider_transaction_no)
    WHERE payment_method = 'SEPAY' AND provider_transaction_no IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payments_sepay_monthly_usage
    ON billing.payments(status, paid_at, expires_at, created_at)
    WHERE payment_method = 'SEPAY';
