-- System Admin CRUD phi san toan he thong (transactionType = SUBSCRIPTION_PURCHASE).
-- Forward-only: khong sua migration cu. Khong gan scope theo ticketType/vehicleType/parkingLot/organization.
-- Application layer tra loi loi nghiep vu; exclusion constraint la lop bao ve cuoi cung cho kich hoat dong thoi.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE billing.platform_fee_types (
    platform_fee_type_id uuid DEFAULT gen_random_uuid() NOT NULL,
    fee_code character varying(100) NOT NULL,
    name character varying(200) NOT NULL,
    description character varying(1000),
    transaction_type character varying(50) NOT NULL,
    calculation_type character varying(30) NOT NULL,
    fee_basis character varying(30) NOT NULL,
    charged_to character varying(30) NOT NULL,
    is_stackable boolean DEFAULT true NOT NULL,
    status character varying(20) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    archived_at timestamp with time zone,
    archived_by uuid,
    CONSTRAINT pk_platform_fee_types PRIMARY KEY (platform_fee_type_id),
    CONSTRAINT uq_platform_fee_types_fee_code UNIQUE (fee_code),
    CONSTRAINT ck_platform_fee_types_transaction_type CHECK ((transaction_type = 'SUBSCRIPTION_PURCHASE')),
    CONSTRAINT ck_platform_fee_types_calculation_type CHECK ((calculation_type = 'PERCENTAGE')),
    CONSTRAINT ck_platform_fee_types_fee_basis CHECK ((fee_basis IN ('GROSS_AMOUNT', 'AFTER_DISCOUNT'))),
    CONSTRAINT ck_platform_fee_types_charged_to CHECK ((charged_to IN ('PARTNER', 'CUSTOMER', 'PLATFORM'))),
    CONSTRAINT ck_platform_fee_types_status CHECK ((status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED')))
);

CREATE TABLE billing.platform_fee_rule_versions (
    platform_fee_rule_version_id uuid DEFAULT gen_random_uuid() NOT NULL,
    platform_fee_type_id uuid NOT NULL,
    version integer NOT NULL,
    previous_version_id uuid,
    fee_rate numeric(9,6) NOT NULL,
    effective_from timestamp with time zone,
    effective_to timestamp with time zone,
    status character varying(20) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    activated_at timestamp with time zone,
    activated_by uuid,
    deactivated_at timestamp with time zone,
    deactivated_by uuid,
    archived_at timestamp with time zone,
    archived_by uuid,
    CONSTRAINT pk_platform_fee_rule_versions PRIMARY KEY (platform_fee_rule_version_id),
    CONSTRAINT uq_platform_fee_rule_versions_type_version UNIQUE (platform_fee_type_id, version),
    CONSTRAINT ck_platform_fee_rule_versions_version CHECK ((version > 0)),
    CONSTRAINT ck_platform_fee_rule_versions_fee_rate CHECK ((fee_rate >= 0 AND fee_rate <= 1)),
    CONSTRAINT ck_platform_fee_rule_versions_effective_period CHECK ((effective_to IS NULL OR effective_from IS NULL OR effective_to > effective_from)),
    CONSTRAINT ck_platform_fee_rule_versions_status CHECK ((status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED'))),
    CONSTRAINT ex_platform_fee_rule_versions_active_period
        EXCLUDE USING gist (
            platform_fee_type_id WITH =,
            tstzrange(effective_from, effective_to, '[)') WITH &&
        ) WHERE (status = 'ACTIVE'),
    CONSTRAINT fk_platform_fee_rule_versions_type FOREIGN KEY (platform_fee_type_id)
        REFERENCES billing.platform_fee_types (platform_fee_type_id) ON DELETE RESTRICT,
    CONSTRAINT fk_platform_fee_rule_versions_previous FOREIGN KEY (previous_version_id)
        REFERENCES billing.platform_fee_rule_versions (platform_fee_rule_version_id) ON DELETE RESTRICT
);

CREATE INDEX idx_platform_fee_types_status ON billing.platform_fee_types (status);
CREATE INDEX idx_platform_fee_types_transaction_status ON billing.platform_fee_types (transaction_type, status);
CREATE INDEX idx_platform_fee_rule_versions_type_status ON billing.platform_fee_rule_versions (platform_fee_type_id, status);
CREATE INDEX idx_platform_fee_rule_versions_status_effective
    ON billing.platform_fee_rule_versions (status, effective_from, effective_to);
CREATE INDEX idx_platform_fee_rule_versions_type_version
    ON billing.platform_fee_rule_versions (platform_fee_type_id, version);
