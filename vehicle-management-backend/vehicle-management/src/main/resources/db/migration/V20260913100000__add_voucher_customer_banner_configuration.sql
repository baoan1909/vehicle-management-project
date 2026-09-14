ALTER TABLE catalog.vouchers
    ADD COLUMN show_on_dashboard boolean DEFAULT false NOT NULL,
    ADD COLUMN show_on_subscription_page boolean DEFAULT false NOT NULL,
    ADD COLUMN banner_title character varying(150),
    ADD COLUMN banner_description character varying(500),
    ADD COLUMN banner_priority integer DEFAULT 0 NOT NULL;

ALTER TABLE catalog.vouchers
    ADD CONSTRAINT ck_vouchers_banner_priority_non_negative CHECK (banner_priority >= 0);
