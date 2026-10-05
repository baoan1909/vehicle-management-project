CREATE TABLE operations.onboarding_approval_policies (
    policy_id UUID PRIMARY KEY,
    policy_type VARCHAR(50) NOT NULL UNIQUE,
    auto_approve_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    effective_from TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES iam.accounts(account_id),
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES iam.accounts(account_id),
    CONSTRAINT ck_onboarding_approval_policy_type
        CHECK (policy_type IN ('CUSTOMER_ONBOARDING', 'PARTNER_REGISTRATION')),
    CONSTRAINT ck_onboarding_approval_policy_effective_from
        CHECK (auto_approve_enabled = FALSE OR effective_from IS NOT NULL)
);

INSERT INTO operations.onboarding_approval_policies (
    policy_id, policy_type, auto_approve_enabled, effective_from, version
) VALUES
    ('62000000-0000-4000-8000-000000000001', 'CUSTOMER_ONBOARDING', FALSE, NULL, 0),
    ('62000000-0000-4000-8000-000000000002', 'PARTNER_REGISTRATION', FALSE, NULL, 0);
