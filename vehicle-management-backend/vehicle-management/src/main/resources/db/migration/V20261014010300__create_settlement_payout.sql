-- Giai doan 4: voucher revenue sharing, partner settlement, payout.
-- Forward-only, idempotent.

CREATE TABLE IF NOT EXISTS catalog.voucher_financial_terms (
    voucher_financial_term_id uuid NOT NULL,
    voucher_id uuid NOT NULL,
    funding_source varchar(20) NOT NULL DEFAULT 'PLATFORM',
    funding_organization_id uuid,
    commission_beneficiary_type varchar(40),
    commission_beneficiary_id uuid,
    commission_rate numeric(9,6) NOT NULL DEFAULT 0,
    commission_basis varchar(20) NOT NULL DEFAULT 'PLATFORM_FEE',
    max_commission_amount numeric(19,2),
    settlement_delay_days integer NOT NULL DEFAULT 7,
    valid_from timestamptz,
    valid_to timestamptz,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    CONSTRAINT voucher_financial_terms_pkey PRIMARY KEY (voucher_financial_term_id),
    CONSTRAINT ck_voucher_terms_funding CHECK (funding_source IN ('PLATFORM','PARTNER','EXTERNAL_SPONSOR')),
    CONSTRAINT ck_voucher_terms_basis CHECK (commission_basis IN ('GROSS','FINAL_AMOUNT','DISCOUNT_AMOUNT','PLATFORM_FEE')),
    CONSTRAINT ck_voucher_terms_status CHECK (status IN ('ACTIVE','INACTIVE'))
);
CREATE INDEX IF NOT EXISTS idx_voucher_terms_voucher ON catalog.voucher_financial_terms (voucher_id, status);

CREATE TABLE IF NOT EXISTS billing.revenue_allocations (
    revenue_allocation_id uuid NOT NULL,
    payment_id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    parking_lot_id uuid,
    organization_id uuid,
    voucher_id uuid,
    gross_amount numeric(19,2) NOT NULL,
    discount_amount numeric(19,2) NOT NULL DEFAULT 0,
    customer_paid_amount numeric(19,2) NOT NULL,
    sponsored_discount_amount numeric(19,2) NOT NULL DEFAULT 0,
    platform_fee_amount numeric(19,2) NOT NULL DEFAULT 0,
    voucher_commission_amount numeric(19,2) NOT NULL DEFAULT 0,
    partner_payable_amount numeric(19,2) NOT NULL,
    gateway_fee_amount numeric(19,2) NOT NULL DEFAULT 0,
    funding_source_snapshot varchar(20),
    commission_rate_snapshot numeric(9,6),
    commission_basis_snapshot varchar(20),
    status varchar(20) NOT NULL DEFAULT 'POSTED',
    available_at timestamptz,
    settled_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT revenue_allocations_pkey PRIMARY KEY (revenue_allocation_id),
    CONSTRAINT uq_revenue_allocations_payment UNIQUE (payment_id),
    CONSTRAINT ck_revenue_allocations_status CHECK (status IN ('POSTED','REVERSED'))
);
CREATE INDEX IF NOT EXISTS idx_revenue_allocations_org ON billing.revenue_allocations (organization_id, status);
CREATE INDEX IF NOT EXISTS idx_revenue_allocations_available ON billing.revenue_allocations (status, available_at)
    WHERE settled_at IS NULL;

CREATE TABLE IF NOT EXISTS billing.partner_bank_accounts (
    bank_account_id uuid NOT NULL,
    organization_id uuid NOT NULL,
    bank_code varchar(20) NOT NULL,
    account_number varchar(50) NOT NULL,
    account_name varchar(150) NOT NULL,
    verified boolean NOT NULL DEFAULT false,
    verified_at timestamptz,
    verified_by uuid,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    CONSTRAINT partner_bank_accounts_pkey PRIMARY KEY (bank_account_id),
    CONSTRAINT uq_partner_bank_account UNIQUE (organization_id, bank_code, account_number),
    CONSTRAINT ck_partner_bank_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE IF NOT EXISTS billing.payout_requests (
    payout_request_id uuid NOT NULL,
    wallet_id uuid NOT NULL,
    organization_id uuid NOT NULL,
    bank_account_id uuid NOT NULL,
    amount numeric(19,2) NOT NULL,
    currency varchar(10) NOT NULL DEFAULT 'VND',
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    idempotency_key varchar(100) NOT NULL,
    requested_by uuid NOT NULL,
    requested_at timestamptz NOT NULL DEFAULT now(),
    decided_by uuid,
    decided_at timestamptz,
    failure_reason varchar(500),
    financial_transaction_id uuid,
    CONSTRAINT payout_requests_pkey PRIMARY KEY (payout_request_id),
    CONSTRAINT uq_payout_requests_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_payout_requests_status CHECK (status IN ('PENDING','APPROVED','REJECTED','COMPLETED','FAILED')),
    CONSTRAINT ck_payout_requests_amount CHECK (amount > 0),
    CONSTRAINT fk_payout_wallet FOREIGN KEY (wallet_id) REFERENCES billing.wallets (wallet_id) ON DELETE RESTRICT,
    CONSTRAINT fk_payout_bank FOREIGN KEY (bank_account_id)
        REFERENCES billing.partner_bank_accounts (bank_account_id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_payout_requests_org_status
    ON billing.payout_requests (organization_id, status);

-- Extra platform accounts for sponsor receivable (external sponsor flow).
INSERT INTO billing.ledger_accounts (ledger_account_id, account_code, account_type, currency, status)
VALUES
    ('00000000-0000-0000-0000-000000011008', 'SPONSOR_RECEIVABLE', 'SPONSOR_RECEIVABLE', 'VND', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000011009', 'PARTNER_RECEIVABLE', 'PARTNER_RECEIVABLE', 'VND', 'ACTIVE')
ON CONFLICT (account_code) DO NOTHING;
