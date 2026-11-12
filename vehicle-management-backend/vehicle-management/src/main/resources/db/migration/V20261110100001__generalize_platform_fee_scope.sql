-- Generalize platform fees from subscription-only rules to GLOBAL or transaction-specific rules.
-- Existing fee types keep their original semantics after the backfill.

ALTER TABLE billing.platform_fee_types
    ADD COLUMN IF NOT EXISTS scope_type VARCHAR(30);

UPDATE billing.platform_fee_types
SET scope_type = 'TRANSACTION_TYPE'
WHERE scope_type IS NULL;

ALTER TABLE billing.platform_fee_types
    ALTER COLUMN scope_type SET NOT NULL,
    ALTER COLUMN transaction_type DROP NOT NULL;

ALTER TABLE billing.platform_fee_types
    DROP CONSTRAINT IF EXISTS ck_platform_fee_types_transaction_type;

ALTER TABLE billing.platform_fee_types
    ADD CONSTRAINT ck_platform_fee_types_transaction_type
        CHECK (transaction_type IS NULL OR transaction_type IN (
            'PARKING_SESSION',
            'SUBSCRIPTION_PURCHASE',
            'SUBSCRIPTION_RENEWAL',
            'SUBSCRIPTION_CARD_DEPOSIT',
            'LOST_CARD',
            'MANUAL',
            'RESERVATION_PREPAYMENT'
        )),
    ADD CONSTRAINT ck_platform_fee_types_scope_type
        CHECK (scope_type IN ('GLOBAL', 'TRANSACTION_TYPE')),
    ADD CONSTRAINT ck_platform_fee_types_scope_target
        CHECK (
            (scope_type = 'GLOBAL' AND transaction_type IS NULL)
            OR
            (scope_type = 'TRANSACTION_TYPE' AND transaction_type IS NOT NULL)
        );

CREATE INDEX IF NOT EXISTS idx_platform_fee_types_scope_transaction_status
    ON billing.platform_fee_types (scope_type, transaction_type, status);

ALTER TABLE billing.revenue_allocations
    ADD COLUMN IF NOT EXISTS fund_custody VARCHAR(20) NOT NULL DEFAULT 'PLATFORM';

ALTER TABLE billing.revenue_allocations
    ADD CONSTRAINT ck_revenue_allocations_fund_custody
        CHECK (fund_custody IN ('PLATFORM', 'PARTNER'));

ALTER TABLE billing.payments
    ADD COLUMN IF NOT EXISTS fund_custody VARCHAR(20);

UPDATE billing.payments
SET fund_custody = CASE
    WHEN payment_method IN ('CASH', 'BANK_TRANSFER') THEN 'PARTNER'
    ELSE 'PLATFORM'
END
WHERE fund_custody IS NULL;

ALTER TABLE billing.payments
    ALTER COLUMN fund_custody SET NOT NULL,
    ADD CONSTRAINT ck_payments_fund_custody CHECK (fund_custody IN ('PLATFORM', 'PARTNER'));

CREATE TABLE billing.platform_fee_assessments (
    platform_fee_assessment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    organization_id UUID,
    requested_transaction_type VARCHAR(50) NOT NULL,
    resolved_scope VARCHAR(30) NOT NULL,
    fallback_applied BOOLEAN NOT NULL DEFAULT FALSE,
    basis_amount NUMERIC(19,2) NOT NULL,
    total_fee_amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    fund_custody VARCHAR(20) NOT NULL,
    collector VARCHAR(20) NOT NULL,
    amount_fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    quoted_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    applied_at TIMESTAMPTZ,
    reversed_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_platform_fee_assessments_payment UNIQUE (payment_id),
    CONSTRAINT fk_platform_fee_assessments_invoice FOREIGN KEY (invoice_id)
        REFERENCES billing.invoices(invoice_id) ON DELETE RESTRICT,
    CONSTRAINT fk_platform_fee_assessments_payment FOREIGN KEY (payment_id)
        REFERENCES billing.payments(payment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_platform_fee_assessments_organization FOREIGN KEY (organization_id)
        REFERENCES iam.organizations(organization_id) ON DELETE RESTRICT,
    CONSTRAINT ck_platform_fee_assessments_amounts CHECK (basis_amount >= 0 AND total_fee_amount >= 0),
    CONSTRAINT ck_platform_fee_assessments_currency CHECK (currency = 'VND'),
    CONSTRAINT ck_platform_fee_assessments_custody CHECK (fund_custody IN ('PLATFORM', 'PARTNER')),
    CONSTRAINT ck_platform_fee_assessments_collector CHECK (collector IN ('PLATFORM', 'PARTNER')),
    CONSTRAINT ck_platform_fee_assessments_status CHECK (status IN (
        'QUOTED', 'APPLIED', 'EXPIRED', 'CANCELLED', 'REVERSED',
        'PARTIALLY_REVERSED', 'RECONCILIATION_REQUIRED'
    ))
);

CREATE INDEX idx_platform_fee_assessments_invoice
    ON billing.platform_fee_assessments (invoice_id, created_at DESC);
CREATE INDEX idx_platform_fee_assessments_organization
    ON billing.platform_fee_assessments (organization_id, created_at DESC)
    WHERE organization_id IS NOT NULL;

CREATE TABLE billing.platform_fee_assessment_lines (
    platform_fee_assessment_line_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    platform_fee_assessment_id UUID NOT NULL,
    fee_type_id UUID NOT NULL,
    fee_rule_version_id UUID NOT NULL,
    fee_code VARCHAR(100) NOT NULL,
    fee_name VARCHAR(200) NOT NULL,
    scope_type VARCHAR(30) NOT NULL,
    transaction_type VARCHAR(50),
    calculation_type VARCHAR(30) NOT NULL,
    fee_basis VARCHAR(30) NOT NULL,
    charged_to VARCHAR(30) NOT NULL,
    fee_rate NUMERIC(9,6) NOT NULL,
    basis_amount NUMERIC(19,2) NOT NULL,
    calculated_fee_amount NUMERIC(19,2) NOT NULL,
    final_fee_amount NUMERIC(19,2) NOT NULL,
    is_stackable BOOLEAN NOT NULL,
    rule_version INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_platform_fee_assessment_lines_assessment FOREIGN KEY (platform_fee_assessment_id)
        REFERENCES billing.platform_fee_assessments(platform_fee_assessment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_platform_fee_assessment_lines_type FOREIGN KEY (fee_type_id)
        REFERENCES billing.platform_fee_types(platform_fee_type_id) ON DELETE RESTRICT,
    CONSTRAINT fk_platform_fee_assessment_lines_version FOREIGN KEY (fee_rule_version_id)
        REFERENCES billing.platform_fee_rule_versions(platform_fee_rule_version_id) ON DELETE RESTRICT,
    CONSTRAINT ck_platform_fee_assessment_lines_scope CHECK (scope_type IN ('GLOBAL', 'TRANSACTION_TYPE')),
    CONSTRAINT ck_platform_fee_assessment_lines_target CHECK (
        (scope_type = 'GLOBAL' AND transaction_type IS NULL)
        OR (scope_type = 'TRANSACTION_TYPE' AND transaction_type IS NOT NULL)
    ),
    CONSTRAINT ck_platform_fee_assessment_lines_amounts CHECK (
        basis_amount >= 0 AND calculated_fee_amount >= 0 AND final_fee_amount >= 0
    )
);

CREATE INDEX idx_platform_fee_assessment_lines_assessment
    ON billing.platform_fee_assessment_lines (platform_fee_assessment_id);
