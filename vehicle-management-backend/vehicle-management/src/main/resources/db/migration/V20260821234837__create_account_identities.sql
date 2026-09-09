CREATE TABLE iam.account_identities (
    account_identity_id uuid DEFAULT gen_random_uuid() NOT NULL,
    account_id uuid NOT NULL,
    provider character varying(50) NOT NULL,
    provider_subject character varying(255) NOT NULL,
    provider_username character varying(255),
    provider_email public.citext,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid,
    updated_at timestamp with time zone,
    updated_by uuid,
    CONSTRAINT pk_account_identities PRIMARY KEY (account_identity_id),
    CONSTRAINT fk_account_identities_account
        FOREIGN KEY (account_id) REFERENCES iam.accounts(account_id) ON DELETE CASCADE,
    CONSTRAINT uq_account_identities_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uq_account_identities_account_provider UNIQUE (account_id, provider),
    CONSTRAINT ck_account_identities_provider CHECK (provider IN ('GOOGLE', 'FACEBOOK'))
);

COMMENT ON TABLE iam.account_identities IS
    'Immutable external identities owned by local accounts. Password-only accounts intentionally have no row.';
COMMENT ON COLUMN iam.account_identities.provider_subject IS
    'Stable subject assigned by the external provider; email must never be used as this identifier.';
