BEGIN;

ALTER TABLE billing.payments
    ALTER COLUMN status SET DEFAULT 'PENDING',
    ALTER COLUMN paid_at DROP NOT NULL,
    ALTER COLUMN paid_at DROP DEFAULT;

ALTER TABLE billing.payments
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS provider_transaction_no VARCHAR(100),
    ADD COLUMN IF NOT EXISTS provider_response_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS provider_transaction_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS bank_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS card_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_transaction_ref
    ON billing.payments (transaction_ref)
    WHERE transaction_ref IS NOT NULL;

COMMIT;
