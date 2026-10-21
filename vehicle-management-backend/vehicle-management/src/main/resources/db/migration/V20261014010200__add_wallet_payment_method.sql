-- Giai doan 3: WALLET payment method + idempotency for wallet invoice payments.
-- Forward-only, idempotent.

DO $$
BEGIN
    ALTER TABLE billing.payments DROP CONSTRAINT IF EXISTS ck_payments_method;
EXCEPTION WHEN undefined_table THEN
    NULL;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_payments_method'
    ) THEN
        ALTER TABLE billing.payments
            ADD CONSTRAINT ck_payments_method
            CHECK (payment_method IN ('CASH', 'QR', 'BANK_TRANSFER', 'MOMO', 'VNPAY', 'WALLET'));
    END IF;
END
$$;

ALTER TABLE billing.payments ADD COLUMN IF NOT EXISTS idempotency_key varchar(100);
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'uq_payments_idempotency_key') THEN
        CREATE UNIQUE INDEX uq_payments_idempotency_key
            ON billing.payments (idempotency_key) WHERE idempotency_key IS NOT NULL;
    END IF;
END
$$;

ALTER TABLE billing.payments ADD COLUMN IF NOT EXISTS wallet_id uuid;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_payments_wallet' AND table_name = 'payments'
    ) THEN
        ALTER TABLE billing.payments
            ADD CONSTRAINT fk_payments_wallet FOREIGN KEY (wallet_id)
            REFERENCES billing.wallets (wallet_id) ON DELETE RESTRICT;
    END IF;
END
$$;

ALTER TABLE billing.payments ADD COLUMN IF NOT EXISTS reversed_payment_id uuid;
ALTER TABLE billing.payments ADD COLUMN IF NOT EXISTS refunded_amount numeric(19,2) NOT NULL DEFAULT 0;
