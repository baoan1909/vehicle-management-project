CREATE TABLE billing.revenue_allocation_reversals (
    revenue_allocation_reversal_id UUID PRIMARY KEY,
    revenue_allocation_id UUID NOT NULL,
    refund_payment_id UUID NOT NULL,
    refund_amount NUMERIC(19,2) NOT NULL,
    reversal_ratio NUMERIC(19,10) NOT NULL,
    platform_fee_amount NUMERIC(19,2) NOT NULL,
    voucher_commission_amount NUMERIC(19,2) NOT NULL,
    partner_payable_amount NUMERIC(19,2) NOT NULL,
    gateway_fee_amount NUMERIC(19,2) NOT NULL,
    discount_amount NUMERIC(19,2) NOT NULL,
    financial_transaction_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_revenue_allocation_reversal_refund UNIQUE (refund_payment_id),
    CONSTRAINT fk_revenue_allocation_reversal_allocation FOREIGN KEY (revenue_allocation_id)
        REFERENCES billing.revenue_allocations(revenue_allocation_id) ON DELETE RESTRICT,
    CONSTRAINT fk_revenue_allocation_reversal_refund FOREIGN KEY (refund_payment_id)
        REFERENCES billing.payments(payment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_revenue_allocation_reversal_transaction FOREIGN KEY (financial_transaction_id)
        REFERENCES billing.financial_transactions(financial_transaction_id) ON DELETE RESTRICT,
    CONSTRAINT ck_revenue_allocation_reversal_amounts CHECK (
        refund_amount > 0
        AND reversal_ratio > 0
        AND reversal_ratio <= 1
        AND platform_fee_amount >= 0
        AND voucher_commission_amount >= 0
        AND partner_payable_amount >= 0
        AND gateway_fee_amount >= 0
        AND discount_amount >= 0
    )
);

CREATE INDEX idx_revenue_allocation_reversals_allocation
    ON billing.revenue_allocation_reversals (revenue_allocation_id, created_at);
