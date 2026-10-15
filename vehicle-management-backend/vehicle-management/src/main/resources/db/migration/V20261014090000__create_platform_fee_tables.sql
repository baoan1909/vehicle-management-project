-- System Admin CRUD phí sàn toàn hệ thống
-- transaction_type = SUBSCRIPTION_PURCHASE
--
-- Forward-only:
-- - Không chỉnh sửa migration cũ.
-- - Không gắn scope theo ticketType, vehicleType, parkingLot hoặc organization.
-- - Application layer xử lý lỗi nghiệp vụ.
-- - Exclusion constraint là lớp bảo vệ cuối cùng khi kích hoạt đồng thời.

BEGIN;

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE SCHEMA IF NOT EXISTS billing;

CREATE TABLE IF NOT EXISTS billing.platform_fee_types (
                                                          platform_fee_type_id UUID
                                                          DEFAULT gen_random_uuid()
    NOT NULL,

    fee_code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),

    transaction_type VARCHAR(50) NOT NULL,
    calculation_type VARCHAR(30) NOT NULL,
    fee_basis VARCHAR(30) NOT NULL,
    charged_to VARCHAR(30) NOT NULL,

    is_stackable BOOLEAN
    DEFAULT TRUE
    NOT NULL,

    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ
    DEFAULT now()
    NOT NULL,
    created_by UUID,

    updated_at TIMESTAMPTZ,
    updated_by UUID,

    archived_at TIMESTAMPTZ,
    archived_by UUID,

    CONSTRAINT pk_platform_fee_types
    PRIMARY KEY (platform_fee_type_id),

    CONSTRAINT uq_platform_fee_types_fee_code
    UNIQUE (fee_code),

    CONSTRAINT ck_platform_fee_types_transaction_type
    CHECK (
              transaction_type = 'SUBSCRIPTION_PURCHASE'
          ),

    CONSTRAINT ck_platform_fee_types_calculation_type
    CHECK (
              calculation_type = 'PERCENTAGE'
          ),

    CONSTRAINT ck_platform_fee_types_fee_basis
    CHECK (
              fee_basis IN (
              'GROSS_AMOUNT',
              'AFTER_DISCOUNT'
                           )
    ),

    CONSTRAINT ck_platform_fee_types_charged_to
    CHECK (
              charged_to IN (
              'PARTNER',
              'CUSTOMER',
              'PLATFORM'
                            )
    ),

    CONSTRAINT ck_platform_fee_types_status
    CHECK (
              status IN (
              'DRAFT',
              'ACTIVE',
              'INACTIVE',
              'ARCHIVED'
                        )
    )
    );

CREATE TABLE IF NOT EXISTS billing.platform_fee_rule_versions (
                                                                  platform_fee_rule_version_id UUID
                                                                  DEFAULT gen_random_uuid()
    NOT NULL,

    platform_fee_type_id UUID NOT NULL,

    version INTEGER NOT NULL,
    previous_version_id UUID,

    fee_rate NUMERIC(9, 6) NOT NULL,

    effective_from TIMESTAMPTZ,
    effective_to TIMESTAMPTZ,

    status VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ
    DEFAULT now()
    NOT NULL,
    created_by UUID,

    updated_at TIMESTAMPTZ,
    updated_by UUID,

    activated_at TIMESTAMPTZ,
    activated_by UUID,

    deactivated_at TIMESTAMPTZ,
    deactivated_by UUID,

    archived_at TIMESTAMPTZ,
    archived_by UUID,

    CONSTRAINT pk_platform_fee_rule_versions
    PRIMARY KEY (platform_fee_rule_version_id),

    CONSTRAINT uq_platform_fee_rule_versions_type_version
    UNIQUE (
               platform_fee_type_id,
               version
           ),

    CONSTRAINT ck_platform_fee_rule_versions_version
    CHECK (
              version > 0
          ),

    CONSTRAINT ck_platform_fee_rule_versions_fee_rate
    CHECK (
              fee_rate >= 0
              AND fee_rate <= 1
          ),

    CONSTRAINT ck_platform_fee_rule_versions_effective_period
    CHECK (
              effective_to IS NULL
              OR effective_from IS NULL
              OR effective_to > effective_from
          ),

    CONSTRAINT ck_platform_fee_rule_versions_status
    CHECK (
              status IN (
              'DRAFT',
              'ACTIVE',
              'INACTIVE',
              'ARCHIVED'
                        )
    ),

    CONSTRAINT ex_platform_fee_rule_versions_active_period
    EXCLUDE USING gist (
                           platform_fee_type_id WITH =,
                           tstzrange(
                           effective_from,
                           effective_to,
                           '[)'
                                    ) WITH &&
                )
    WHERE (status = 'ACTIVE'),

    CONSTRAINT fk_platform_fee_rule_versions_type
    FOREIGN KEY (platform_fee_type_id)
    REFERENCES billing.platform_fee_types (
                                              platform_fee_type_id
                                          )
            ON DELETE RESTRICT,

    CONSTRAINT fk_platform_fee_rule_versions_previous
    FOREIGN KEY (previous_version_id)
    REFERENCES billing.platform_fee_rule_versions (
                                                      platform_fee_rule_version_id
                                                  )
            ON DELETE RESTRICT
    );

CREATE INDEX IF NOT EXISTS idx_platform_fee_types_status
    ON billing.platform_fee_types (
    status
    );

CREATE INDEX IF NOT EXISTS idx_platform_fee_types_transaction_status
    ON billing.platform_fee_types (
    transaction_type,
    status
    );

CREATE INDEX IF NOT EXISTS idx_platform_fee_rule_versions_type_status
    ON billing.platform_fee_rule_versions (
    platform_fee_type_id,
    status
    );

CREATE INDEX IF NOT EXISTS idx_platform_fee_rule_versions_status_effective
    ON billing.platform_fee_rule_versions (
    status,
    effective_from,
    effective_to
    );

CREATE INDEX IF NOT EXISTS idx_platform_fee_rule_versions_type_version
    ON billing.platform_fee_rule_versions (
    platform_fee_type_id,
    version
    );

COMMIT;