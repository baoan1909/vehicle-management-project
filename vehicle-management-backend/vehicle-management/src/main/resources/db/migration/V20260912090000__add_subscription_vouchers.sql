ALTER TABLE access_control.subscriptions
    ADD COLUMN requested_voucher_code character varying(50);

CREATE TABLE catalog.vouchers (
    voucher_id uuid DEFAULT gen_random_uuid() NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    discount_type character varying(20) NOT NULL,
    discount_value numeric(12,2) NOT NULL,
    max_discount_amount numeric(12,2),
    minimum_subscription_amount numeric(12,2) DEFAULT 0 NOT NULL,
    max_redemptions integer,
    max_redemptions_per_customer integer DEFAULT 1 NOT NULL,
    valid_from timestamp with time zone NOT NULL,
    valid_to timestamp with time zone NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT vouchers_pkey PRIMARY KEY (voucher_id),
    CONSTRAINT ck_vouchers_discount_type CHECK ((discount_type)::text = ANY ((ARRAY['PERCENTAGE'::character varying, 'FIXED_AMOUNT'::character varying])::text[])),
    CONSTRAINT ck_vouchers_status CHECK ((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'PAUSED'::character varying, 'EXPIRED'::character varying])::text[])),
    CONSTRAINT ck_vouchers_discount_value_positive CHECK (discount_value > 0),
    CONSTRAINT ck_vouchers_max_discount_non_negative CHECK (max_discount_amount IS NULL OR max_discount_amount >= 0),
    CONSTRAINT ck_vouchers_minimum_amount_non_negative CHECK (minimum_subscription_amount >= 0),
    CONSTRAINT ck_vouchers_max_redemptions_positive CHECK (max_redemptions IS NULL OR max_redemptions > 0),
    CONSTRAINT ck_vouchers_max_per_customer_positive CHECK (max_redemptions_per_customer > 0),
    CONSTRAINT ck_vouchers_valid_period CHECK (valid_to > valid_from)
);

CREATE UNIQUE INDEX uq_vouchers_code_ci ON catalog.vouchers USING btree (upper(code));
CREATE INDEX idx_vouchers_status_period ON catalog.vouchers USING btree (status, valid_from, valid_to);

CREATE TABLE billing.voucher_redemptions (
    voucher_redemption_id uuid DEFAULT gen_random_uuid() NOT NULL,
    voucher_id uuid NOT NULL,
    invoice_id uuid NOT NULL,
    subscription_id uuid NOT NULL,
    customer_id uuid NOT NULL,
    voucher_code_snapshot character varying(50) NOT NULL,
    discount_type_snapshot character varying(20) NOT NULL,
    discount_value_snapshot numeric(12,2) NOT NULL,
    discount_amount numeric(12,2) NOT NULL,
    status character varying(20) DEFAULT 'RESERVED'::character varying NOT NULL,
    reserved_at timestamp with time zone NOT NULL,
    redeemed_at timestamp with time zone,
    released_at timestamp with time zone,
    release_reason character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT voucher_redemptions_pkey PRIMARY KEY (voucher_redemption_id),
    CONSTRAINT uq_voucher_redemptions_invoice UNIQUE (invoice_id),
    CONSTRAINT uq_voucher_redemptions_subscription UNIQUE (subscription_id),
    CONSTRAINT ck_voucher_redemptions_status CHECK ((status)::text = ANY ((ARRAY['RESERVED'::character varying, 'REDEEMED'::character varying, 'RELEASED'::character varying])::text[])),
    CONSTRAINT ck_voucher_redemptions_discount_non_negative CHECK (discount_amount >= 0),
    CONSTRAINT fk_voucher_redemptions_voucher FOREIGN KEY (voucher_id) REFERENCES catalog.vouchers(voucher_id) ON DELETE RESTRICT,
    CONSTRAINT fk_voucher_redemptions_invoice FOREIGN KEY (invoice_id) REFERENCES billing.invoices(invoice_id) ON DELETE RESTRICT,
    CONSTRAINT fk_voucher_redemptions_subscription FOREIGN KEY (subscription_id) REFERENCES access_control.subscriptions(subscription_id) ON DELETE RESTRICT,
    CONSTRAINT fk_voucher_redemptions_customer FOREIGN KEY (customer_id) REFERENCES people.customers(customer_id) ON DELETE RESTRICT
);

CREATE INDEX idx_voucher_redemptions_voucher_status ON billing.voucher_redemptions USING btree (voucher_id, status);
CREATE INDEX idx_voucher_redemptions_customer_voucher ON billing.voucher_redemptions USING btree (customer_id, voucher_id, status);

CREATE TRIGGER trg_vouchers_set_updated_at BEFORE UPDATE ON catalog.vouchers FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
CREATE TRIGGER trg_voucher_redemptions_set_updated_at BEFORE UPDATE ON billing.voucher_redemptions FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
