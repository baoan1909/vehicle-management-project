-- Giai doan 2: Wallet top-up orders via VNPAY (idempotent, reconcilable).
-- Forward-only, idempotent.

CREATE TABLE IF NOT EXISTS billing.wallet_topup_orders (
    topup_order_id uuid NOT NULL,
    wallet_id uuid NOT NULL,
    amount numeric(19,2) NOT NULL,
    currency varchar(10) NOT NULL DEFAULT 'VND',
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    transaction_ref varchar(64) NOT NULL,
    provider_transaction_no varchar(100),
    provider_response_code varchar(20),
    provider_transaction_status varchar(20),
    payment_url text,
    expires_at timestamptz NOT NULL,
    completed_at timestamptz,
    failure_reason varchar(500),
    idempotency_key varchar(100) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    CONSTRAINT wallet_topup_orders_pkey PRIMARY KEY (topup_order_id),
    CONSTRAINT uq_wallet_topup_orders_ref UNIQUE (transaction_ref),
    CONSTRAINT uq_wallet_topup_orders_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_wallet_topup_orders_status CHECK (status IN ('PENDING','COMPLETED','FAILED','CANCELLED','EXPIRED')),
    CONSTRAINT ck_wallet_topup_orders_amount_positive CHECK (amount > 0),
    CONSTRAINT fk_wallet_topup_orders_wallet FOREIGN KEY (wallet_id)
        REFERENCES billing.wallets (wallet_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_wallet_topup_orders_wallet_status
    ON billing.wallet_topup_orders (wallet_id, status);
CREATE INDEX IF NOT EXISTS idx_wallet_topup_orders_status_expires
    ON billing.wallet_topup_orders (status, expires_at);
CREATE INDEX IF NOT EXISTS idx_wallet_topup_orders_provider_no
    ON billing.wallet_topup_orders (provider_transaction_no)
    WHERE provider_transaction_no IS NOT NULL;
