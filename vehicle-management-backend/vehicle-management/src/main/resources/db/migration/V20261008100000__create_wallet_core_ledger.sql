-- Giai doan 1: Wallet core + double-entry ledger + maker-checker adjustment.
-- Forward-only, idempotent (IF NOT EXISTS / ON CONFLICT).

-- =====================================================================
-- 1. Tables
-- =====================================================================

CREATE TABLE IF NOT EXISTS billing.wallets (
    wallet_id uuid NOT NULL,
    owner_type varchar(20) NOT NULL,
    customer_id uuid,
    organization_id uuid,
    wallet_purpose varchar(40) NOT NULL DEFAULT 'PERSONAL',
    currency varchar(10) NOT NULL DEFAULT 'VND',
    available_balance numeric(19,2) NOT NULL DEFAULT 0,
    pending_balance numeric(19,2) NOT NULL DEFAULT 0,
    held_balance numeric(19,2) NOT NULL DEFAULT 0,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    CONSTRAINT wallets_pkey PRIMARY KEY (wallet_id),
    CONSTRAINT ck_wallets_owner_type CHECK (owner_type IN ('CUSTOMER','ORGANIZATION','PLATFORM')),
    CONSTRAINT ck_wallets_status CHECK (status IN ('ACTIVE','DEBIT_BLOCKED','LOCKED','CLOSED')),
    CONSTRAINT ck_wallets_customer_owner CHECK (
        (owner_type = 'CUSTOMER' AND customer_id IS NOT NULL AND organization_id IS NULL)
        OR (owner_type = 'ORGANIZATION' AND organization_id IS NOT NULL AND customer_id IS NULL)
        OR (owner_type = 'PLATFORM' AND customer_id IS NULL AND organization_id IS NULL)
    ),
    CONSTRAINT ck_wallets_available_non_negative CHECK (available_balance >= 0),
    CONSTRAINT ck_wallets_pending_non_negative CHECK (pending_balance >= 0),
    CONSTRAINT ck_wallets_held_non_negative CHECK (held_balance >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_wallets_customer_currency_purpose
    ON billing.wallets (customer_id, currency, wallet_purpose)
    WHERE owner_type = 'CUSTOMER';

CREATE UNIQUE INDEX IF NOT EXISTS uq_wallets_organization_currency_purpose
    ON billing.wallets (organization_id, currency, wallet_purpose)
    WHERE owner_type = 'ORGANIZATION';

CREATE UNIQUE INDEX IF NOT EXISTS uq_wallets_platform_currency_purpose
    ON billing.wallets (currency, wallet_purpose)
    WHERE owner_type = 'PLATFORM';

CREATE INDEX IF NOT EXISTS idx_wallets_customer ON billing.wallets (customer_id) WHERE customer_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_wallets_organization ON billing.wallets (organization_id) WHERE organization_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS billing.financial_transactions (
    financial_transaction_id uuid NOT NULL,
    transaction_code varchar(64) NOT NULL,
    transaction_type varchar(40) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    idempotency_key varchar(100) NOT NULL,
    reference_type varchar(40),
    reference_id uuid,
    currency varchar(10) NOT NULL DEFAULT 'VND',
    occurred_at timestamptz NOT NULL DEFAULT now(),
    reversed_transaction_id uuid,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    CONSTRAINT financial_transactions_pkey PRIMARY KEY (financial_transaction_id),
    CONSTRAINT uq_financial_transactions_code UNIQUE (transaction_code),
    CONSTRAINT uq_financial_transactions_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_financial_transactions_status CHECK (status IN ('PENDING','POSTED','REVERSED','FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_financial_transactions_reference
    ON billing.financial_transactions (reference_type, reference_id);

CREATE TABLE IF NOT EXISTS billing.ledger_accounts (
    ledger_account_id uuid NOT NULL,
    account_code varchar(64) NOT NULL,
    account_type varchar(40) NOT NULL,
    wallet_id uuid,
    organization_id uuid,
    currency varchar(10) NOT NULL DEFAULT 'VND',
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ledger_accounts_pkey PRIMARY KEY (ledger_account_id),
    CONSTRAINT uq_ledger_accounts_code UNIQUE (account_code),
    CONSTRAINT ck_ledger_accounts_status CHECK (status IN ('ACTIVE','CLOSED'))
);

CREATE TABLE IF NOT EXISTS billing.ledger_entries (
    ledger_entry_id uuid NOT NULL,
    financial_transaction_id uuid NOT NULL,
    ledger_account_id uuid NOT NULL,
    entry_side varchar(10) NOT NULL,
    amount numeric(19,2) NOT NULL,
    balance_after numeric(19,2),
    description varchar(255),
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ledger_entries_pkey PRIMARY KEY (ledger_entry_id),
    CONSTRAINT ck_ledger_entries_side CHECK (entry_side IN ('DEBIT','CREDIT')),
    CONSTRAINT ck_ledger_entries_amount_positive CHECK (amount > 0),
    CONSTRAINT fk_ledger_entries_transaction FOREIGN KEY (financial_transaction_id)
        REFERENCES billing.financial_transactions (financial_transaction_id) ON DELETE RESTRICT,
    CONSTRAINT fk_ledger_entries_account FOREIGN KEY (ledger_account_id)
        REFERENCES billing.ledger_accounts (ledger_account_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_ledger_entries_transaction ON billing.ledger_entries (financial_transaction_id);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_account ON billing.ledger_entries (ledger_account_id);

-- Immutable posted ledger: forbid UPDATE/DELETE on ledger_entries.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_ledger_entries_immutable') THEN
        CREATE TRIGGER trg_ledger_entries_immutable
        BEFORE UPDATE OR DELETE ON billing.ledger_entries
        FOR EACH ROW EXECUTE FUNCTION audit.prevent_ledger_entry_mutation();
    END IF;
EXCEPTION WHEN undefined_function THEN
    -- Fallback: create helper then trigger when audit helper is unavailable.
    CREATE OR REPLACE FUNCTION audit.prevent_ledger_entry_mutation() RETURNS trigger AS $fn$
    BEGIN
        RAISE EXCEPTION 'Ledger entries are immutable once posted';
        RETURN NULL;
    END $fn$ LANGUAGE plpgsql;
    CREATE TRIGGER trg_ledger_entries_immutable
    BEFORE UPDATE OR DELETE ON billing.ledger_entries
    FOR EACH ROW EXECUTE FUNCTION audit.prevent_ledger_entry_mutation();
END
$$;

CREATE TABLE IF NOT EXISTS billing.wallet_adjustments (
    wallet_adjustment_id uuid NOT NULL,
    wallet_id uuid NOT NULL,
    amount numeric(19,2) NOT NULL,
    direction varchar(10) NOT NULL,
    reason varchar(500) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    requested_by uuid NOT NULL,
    requested_at timestamptz NOT NULL DEFAULT now(),
    decided_by uuid,
    decided_at timestamptz,
    financial_transaction_id uuid,
    CONSTRAINT wallet_adjustments_pkey PRIMARY KEY (wallet_adjustment_id),
    CONSTRAINT ck_wallet_adjustments_direction CHECK (direction IN ('CREDIT','DEBIT')),
    CONSTRAINT ck_wallet_adjustments_status CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT ck_wallet_adjustments_amount_positive CHECK (amount > 0),
    CONSTRAINT fk_wallet_adjustments_wallet FOREIGN KEY (wallet_id)
        REFERENCES billing.wallets (wallet_id) ON DELETE RESTRICT,
    CONSTRAINT fk_wallet_adjustments_transaction FOREIGN KEY (financial_transaction_id)
        REFERENCES billing.financial_transactions (financial_transaction_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_wallet_adjustments_wallet_status
    ON billing.wallet_adjustments (wallet_id, status);

-- =====================================================================
-- 2. Seed platform ledger accounts (internal accounting, no personal wallet)
-- =====================================================================

INSERT INTO billing.ledger_accounts (ledger_account_id, account_code, account_type, currency, status)
VALUES
    ('00000000-0000-0000-0000-000000011001', 'CUSTOMER_WALLET_LIABILITY', 'CUSTOMER_WALLET_LIABILITY', 'VND', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000011002', 'PARTNER_PAYABLE', 'PARTNER_PAYABLE', 'VND', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000011003', 'PLATFORM_CASH_CLEARING', 'PLATFORM_CASH_CLEARING', 'VND', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000011004', 'PLATFORM_REVENUE', 'PLATFORM_REVENUE', 'VND', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000011005', 'PLATFORM_VOUCHER_EXPENSE', 'PLATFORM_VOUCHER_EXPENSE', 'VND', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000011006', 'VOUCHER_ISSUER_PAYABLE', 'VOUCHER_ISSUER_PAYABLE', 'VND', 'ACTIVE'),
    ('00000000-0000-0000-0000-000000011007', 'PAYMENT_GATEWAY_FEE_EXPENSE', 'PAYMENT_GATEWAY_FEE_EXPENSE', 'VND', 'ACTIVE')
ON CONFLICT (account_code) DO NOTHING;

-- =====================================================================
-- 3. IAM: WALLET module + actions + scopes + permissions
-- =====================================================================

INSERT INTO iam.permission_modules (module_id, code, name, description, created_at)
VALUES ('00000000-0000-0000-0000-000000001042', 'WALLET', 'Vi tai chinh', 'Quan ly vi khach hang, vi doi tac, so cai va doi soat.', now())
ON CONFLICT (module_id) DO NOTHING;

-- New actions (idempotent by code).
INSERT INTO iam.permission_actions (action_id, code, name, description, created_at)
VALUES
    ('00000000-0000-0000-0000-000000002101', 'LOCK', 'Khoa', 'Cho phep khoa vi.', now()),
    ('00000000-0000-0000-0000-000000002102', 'UNLOCK', 'Mo khoa', 'Cho phep mo khoa vi.', now()),
    ('00000000-0000-0000-0000-000000002103', 'ADJUST', 'Dieu chinh', 'Cho phep tao yeu cau dieu chinh so du vi.', now()),
    ('00000000-0000-0000-0000-000000002104', 'PAYOUT', 'Chi ho', 'Cho phep tao yeu cau payout ve ngan hang.', now()),
    ('00000000-0000-0000-0000-000000002105', 'SETTLE', 'Quyet toan', 'Cho phep xu ly settlement doanh thu doi tac.', now()),
    ('00000000-0000-0000-0000-000000002106', 'RECONCILE', 'Doi soat', 'Cho phep doi soat tai chinh.', now())
ON CONFLICT (action_id) DO NOTHING;

-- New scopes PARTNER / PLATFORM (idempotent by code via conditional insert).
INSERT INTO iam.permission_scopes (scope_id, code, name, description, created_at)
SELECT '00000000-0000-0000-0000-000000003101', 'PARTNER', 'Doi tac', 'Ap dung cho du lieu thuoc doi tac cua tai khoan.', now()
WHERE NOT EXISTS (SELECT 1 FROM iam.permission_scopes WHERE code = 'PARTNER');

INSERT INTO iam.permission_scopes (scope_id, code, name, description, created_at)
SELECT '00000000-0000-0000-0000-000000003102', 'PLATFORM', 'Nen tang', 'Ap dung toan he thong nen tang.', now()
WHERE NOT EXISTS (SELECT 1 FROM iam.permission_scopes WHERE code = 'PLATFORM');

-- Permissions (23 codes from spec). module WALLET + action/scope mapping.
-- Helper: resolve ids by code so migration is portable across environments.
WITH module_cte AS (SELECT module_id FROM iam.permission_modules WHERE code = 'WALLET'),
     act(action_code, action_id) AS (
        SELECT 'READ', action_id FROM iam.permission_actions WHERE code = 'READ'
        UNION ALL SELECT 'CREATE', action_id FROM iam.permission_actions WHERE code = 'CREATE'
        UNION ALL SELECT 'UPDATE', action_id FROM iam.permission_actions WHERE code = 'UPDATE'
        UNION ALL SELECT 'LOCK', action_id FROM iam.permission_actions WHERE code = 'LOCK'
        UNION ALL SELECT 'UNLOCK', action_id FROM iam.permission_actions WHERE code = 'UNLOCK'
        UNION ALL SELECT 'ADJUST', action_id FROM iam.permission_actions WHERE code = 'ADJUST'
        UNION ALL SELECT 'APPROVE', action_id FROM iam.permission_actions WHERE code = 'APPROVE'
        UNION ALL SELECT 'REJECT', action_id FROM iam.permission_actions WHERE code = 'REJECT'
        UNION ALL SELECT 'REFUND', action_id FROM iam.permission_actions WHERE code = 'REFUND'
        UNION ALL SELECT 'PROCESS', action_id FROM iam.permission_actions WHERE code = 'PROCESS'
        UNION ALL SELECT 'PAYOUT', action_id FROM iam.permission_actions WHERE code = 'PAYOUT'
        UNION ALL SELECT 'RECONCILE', action_id FROM iam.permission_actions WHERE code = 'RECONCILE'
        UNION ALL SELECT 'SETTLE', action_id FROM iam.permission_actions WHERE code = 'SETTLE'
     ),
     scope(scope_code, scope_id) AS (
        SELECT 'OWN', scope_id FROM iam.permission_scopes WHERE code = 'OWN'
        UNION ALL SELECT 'PARTNER', scope_id FROM iam.permission_scopes WHERE code = 'PARTNER'
        UNION ALL SELECT 'PLATFORM', scope_id FROM iam.permission_scopes WHERE code = 'PLATFORM'
        UNION ALL SELECT 'ALL', scope_id FROM iam.permission_scopes WHERE code = 'ALL'
     ),
     new_perms(permission_code, action_code, scope_code) AS (
        VALUES
            ('WALLET_READ_OWN', 'READ', 'OWN'),
            ('WALLET_TRANSACTION_READ_OWN', 'READ', 'OWN'),
            ('WALLET_TOP_UP_OWN', 'CREATE', 'OWN'),
            ('WALLET_PAY_INVOICE_OWN', 'CREATE', 'OWN'),
            ('WALLET_REFUND_READ_OWN', 'READ', 'OWN'),
            ('WALLET_SCOPE_OWN', 'READ', 'OWN'),
            ('WALLET_READ_PARTNER', 'READ', 'PARTNER'),
            ('WALLET_TRANSACTION_READ_PARTNER', 'READ', 'PARTNER'),
            ('WALLET_SETTLEMENT_READ_PARTNER', 'READ', 'PARTNER'),
            ('WALLET_PAYOUT_CREATE_PARTNER', 'PAYOUT', 'PARTNER'),
            ('WALLET_BANK_ACCOUNT_MANAGE_PARTNER', 'UPDATE', 'PARTNER'),
            ('WALLET_SCOPE_PARTNER', 'READ', 'PARTNER'),
            ('WALLET_READ_ALL', 'READ', 'ALL'),
            ('WALLET_TRANSACTION_READ_ALL', 'READ', 'ALL'),
            ('WALLET_LOCK_ALL', 'LOCK', 'ALL'),
            ('WALLET_UNLOCK_ALL', 'UNLOCK', 'ALL'),
            ('WALLET_ADJUST_REQUEST_ALL', 'ADJUST', 'ALL'),
            ('WALLET_ADJUST_APPROVE_ALL', 'APPROVE', 'ALL'),
            ('WALLET_REFUND_ALL', 'REFUND', 'ALL'),
            ('WALLET_SETTLEMENT_PROCESS_ALL', 'SETTLE', 'ALL'),
            ('WALLET_PAYOUT_APPROVE_ALL', 'APPROVE', 'ALL'),
            ('WALLET_RECONCILIATION_READ_ALL', 'RECONCILE', 'ALL'),
            ('WALLET_SCOPE_PLATFORM', 'READ', 'PLATFORM')
     )
INSERT INTO iam.permissions (permission_id, permission_code, name, description, created_at, module_id, action_id, scope_id)
SELECT gen_random_uuid(), np.permission_code, np.permission_code,
    'Wallet permission ' || np.permission_code, now(),
    (SELECT module_id FROM module_cte),
    (SELECT action_id FROM act WHERE act.action_code = np.action_code),
    (SELECT scope_id FROM scope WHERE scope.scope_code = np.scope_code)
FROM new_perms np
WHERE NOT EXISTS (SELECT 1 FROM iam.permissions p WHERE p.permission_code = np.permission_code);

-- Role mapping: CUSTOMER -> OWN, PARTNER_ADMIN -> PARTNER, SYSTEM_ADMIN -> platform.
INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), r.role_id, p.permission_id, now(), true, true
FROM iam.roles r
JOIN iam.permissions p ON p.permission_code IN (
    'WALLET_READ_OWN', 'WALLET_TRANSACTION_READ_OWN', 'WALLET_TOP_UP_OWN',
    'WALLET_PAY_INVOICE_OWN', 'WALLET_REFUND_READ_OWN', 'WALLET_SCOPE_OWN'
)
WHERE r.code = 'CUSTOMER'
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, updated_at = now();

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), r.role_id, p.permission_id, now(), true, true
FROM iam.roles r
JOIN iam.permissions p ON p.permission_code IN (
    'WALLET_READ_PARTNER', 'WALLET_TRANSACTION_READ_PARTNER', 'WALLET_SETTLEMENT_READ_PARTNER',
    'WALLET_PAYOUT_CREATE_PARTNER', 'WALLET_BANK_ACCOUNT_MANAGE_PARTNER', 'WALLET_SCOPE_PARTNER'
)
WHERE r.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, updated_at = now();

INSERT INTO iam.role_permissions (id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), r.role_id, p.permission_id, now(), true, true
FROM iam.roles r
JOIN iam.permissions p ON p.permission_code IN (
    'WALLET_READ_ALL', 'WALLET_TRANSACTION_READ_ALL', 'WALLET_LOCK_ALL', 'WALLET_UNLOCK_ALL',
    'WALLET_ADJUST_REQUEST_ALL', 'WALLET_ADJUST_APPROVE_ALL', 'WALLET_REFUND_ALL',
    'WALLET_SETTLEMENT_PROCESS_ALL', 'WALLET_PAYOUT_APPROVE_ALL',
    'WALLET_RECONCILIATION_READ_ALL', 'WALLET_SCOPE_PLATFORM'
)
WHERE r.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, updated_at = now();
