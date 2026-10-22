-- Persist the global platform-fee ceiling so authorized administrators can
-- change it at runtime without restarting the application.

CREATE TABLE IF NOT EXISTS billing.platform_fee_configuration (
    configuration_key VARCHAR(30) NOT NULL,
    max_partner_total_rate NUMERIC(9, 6) NOT NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,

    CONSTRAINT pk_platform_fee_configuration PRIMARY KEY (configuration_key),
    CONSTRAINT ck_platform_fee_configuration_singleton
        CHECK (configuration_key = 'GLOBAL'),
    CONSTRAINT ck_platform_fee_configuration_max_rate
        CHECK (max_partner_total_rate >= 0 AND max_partner_total_rate <= 1),
    CONSTRAINT ck_platform_fee_configuration_row_version
        CHECK (row_version >= 0),
    CONSTRAINT fk_platform_fee_configuration_created_by
        FOREIGN KEY (created_by) REFERENCES iam.accounts(account_id) ON DELETE RESTRICT,
    CONSTRAINT fk_platform_fee_configuration_updated_by
        FOREIGN KEY (updated_by) REFERENCES iam.accounts(account_id) ON DELETE RESTRICT
);

INSERT INTO billing.platform_fee_configuration (
    configuration_key,
    max_partner_total_rate,
    row_version
)
VALUES ('GLOBAL', 0.30, 0)
ON CONFLICT (configuration_key) DO NOTHING;

INSERT INTO iam.permissions (
    permission_id,
    permission_code,
    name,
    description,
    created_at,
    module_id,
    action_id,
    scope_id
)
SELECT
    '00000000-0000-0000-0000-000000004407',
    'PLATFORM_FEE_CONFIGURE_ALL',
    'Cấu hình giới hạn phí sàn',
    'Cho phép thay đổi tổng tỷ lệ phí tối đa áp dụng cho đối tác trên toàn sàn.',
    now(),
    module.module_id,
    '10deb7f4-96fb-4a32-b780-76541581252a',
    '00000000-0000-0000-0000-000000003001'
FROM iam.permission_modules module
WHERE module.code = 'PLATFORM_FEE'
ON CONFLICT DO NOTHING;

INSERT INTO iam.role_permissions (
    id,
    role_id,
    permission_id,
    created_at,
    is_active,
    is_system
)
SELECT
    gen_random_uuid(),
    role.role_id,
    permission.permission_id,
    now(),
    true,
    true
FROM iam.roles role
JOIN iam.permissions permission
    ON permission.permission_code = 'PLATFORM_FEE_CONFIGURE_ALL'
WHERE role.code = 'SYSTEM_ADMIN'
ON CONFLICT DO NOTHING;
