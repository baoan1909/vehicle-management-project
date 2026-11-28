ALTER TABLE billing.invoices DROP CONSTRAINT IF EXISTS ck_invoices_purpose;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'billing.invoices'::regclass
          AND conname = 'ck_invoices_purpose'
    ) THEN
        ALTER TABLE billing.invoices
            ADD CONSTRAINT ck_invoices_purpose CHECK (purpose IN (
            'PARKING_SESSION', 'SUBSCRIPTION_PURCHASE', 'SUBSCRIPTION_RENEWAL',
            'SUBSCRIPTION_CARD_DEPOSIT', 'LOST_CARD', 'MANUAL', 'RESERVATION_PREPAYMENT',
            'PARKING_SESSION_PREPAYMENT'
        ));
    END IF;
END;
$$;

CREATE TABLE IF NOT EXISTS parking.parking_session_prepayments (
    prepayment_id uuid PRIMARY KEY,
    parking_session_id uuid NOT NULL REFERENCES parking.parking_sessions(parking_session_id),
    invoice_id uuid NOT NULL UNIQUE REFERENCES billing.invoices(invoice_id),
    estimated_check_out_at timestamptz NOT NULL,
    projected_gross_amount numeric(12,2) NOT NULL,
    paid_before_amount numeric(12,2) NOT NULL,
    amount numeric(12,2) NOT NULL,
    status varchar(24) NOT NULL,
    expires_at timestamptz NOT NULL,
    paid_at timestamptz,
    policy_version varchar(20) NOT NULL DEFAULT '1',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz,
    CONSTRAINT ck_parking_session_prepayments_amounts CHECK (
        projected_gross_amount >= 0 AND paid_before_amount >= 0 AND amount > 0
    ),
    CONSTRAINT ck_parking_session_prepayments_status CHECK (
        status IN ('PENDING_PAYMENT', 'PAID', 'EXPIRED')
    ),
    CONSTRAINT ck_parking_session_prepayments_paid_at CHECK (
        (status = 'PAID' AND paid_at IS NOT NULL)
        OR (status <> 'PAID' AND paid_at IS NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_parking_session_prepayments_pending
    ON parking.parking_session_prepayments(parking_session_id)
    WHERE status = 'PENDING_PAYMENT';

CREATE INDEX IF NOT EXISTS idx_parking_session_prepayments_paid
    ON parking.parking_session_prepayments(parking_session_id, status, paid_at);
