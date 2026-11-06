CREATE TABLE IF NOT EXISTS access_control.subscription_renewals (
    renewal_id uuid DEFAULT gen_random_uuid() NOT NULL,
    subscription_id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    ticket_type_id uuid NOT NULL,
    price_rule_id uuid NOT NULL,
    period_quantity integer NOT NULL,
    effective_from date NOT NULL,
    effective_to date NOT NULL,
    ticket_amount numeric(12,2) NOT NULL,
    physical_card_deposit numeric(12,2) DEFAULT 0 NOT NULL,
    discount_amount numeric(12,2) DEFAULT 0 NOT NULL,
    final_amount numeric(12,2) NOT NULL,
    voucher_code character varying(50),
    physical_card_requested boolean DEFAULT false NOT NULL,
    status character varying(20) DEFAULT 'PENDING_PAYMENT' NOT NULL,
    idempotency_key character varying(100),
    applied_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT subscription_renewals_pkey PRIMARY KEY (renewal_id),
    CONSTRAINT uq_subscription_renewals_invoice UNIQUE (invoice_id),
    CONSTRAINT ck_subscription_renewals_period_quantity CHECK (period_quantity BETWEEN 1 AND 12),
    CONSTRAINT ck_subscription_renewals_period CHECK (effective_to >= effective_from),
    CONSTRAINT ck_subscription_renewals_amounts CHECK (
        ticket_amount >= 0 AND physical_card_deposit >= 0 AND discount_amount >= 0
        AND final_amount = ticket_amount + physical_card_deposit - discount_amount
    ),
    CONSTRAINT ck_subscription_renewals_status CHECK (
        status IN ('PENDING_PAYMENT', 'SCHEDULED', 'APPLIED', 'CANCELLED')
    ),
    CONSTRAINT fk_subscription_renewals_subscription FOREIGN KEY (subscription_id)
        REFERENCES access_control.subscriptions(subscription_id) ON DELETE RESTRICT,
    CONSTRAINT fk_subscription_renewals_invoice FOREIGN KEY (invoice_id)
        REFERENCES billing.invoices(invoice_id) ON DELETE RESTRICT,
    CONSTRAINT fk_subscription_renewals_ticket_type FOREIGN KEY (ticket_type_id)
        REFERENCES catalog.ticket_types(ticket_type_id) ON DELETE RESTRICT,
    CONSTRAINT fk_subscription_renewals_price_rule FOREIGN KEY (price_rule_id)
        REFERENCES catalog.price_rules(price_rule_id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscription_renewals_idempotency
    ON access_control.subscription_renewals (subscription_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_subscription_renewals_live
    ON access_control.subscription_renewals (subscription_id)
    WHERE status = 'PENDING_PAYMENT';

CREATE INDEX IF NOT EXISTS idx_subscription_renewals_scheduled
    ON access_control.subscription_renewals (status, effective_from);

ALTER TABLE billing.voucher_redemptions
    DROP CONSTRAINT IF EXISTS uq_voucher_redemptions_subscription;
DROP INDEX IF EXISTS billing.uq_voucher_redemptions_subscription;

CREATE INDEX IF NOT EXISTS idx_voucher_redemptions_subscription
    ON billing.voucher_redemptions (subscription_id, status);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'trg_subscription_renewals_set_updated_at'
          AND tgrelid = 'access_control.subscription_renewals'::regclass
    ) THEN
        CREATE TRIGGER trg_subscription_renewals_set_updated_at
            BEFORE UPDATE ON access_control.subscription_renewals
            FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
    END IF;
END $$;
