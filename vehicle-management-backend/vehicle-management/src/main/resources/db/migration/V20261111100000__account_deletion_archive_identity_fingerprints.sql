-- Privacy-safe lookup index for identities released by account tombstoning.
-- Raw identity values must never be stored in this table.
CREATE TABLE IF NOT EXISTS audit.deleted_identity_fingerprints (
    identity_fingerprint_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id uuid NOT NULL REFERENCES iam.account_deletion_requests(request_id) ON DELETE RESTRICT,
    account_id uuid NOT NULL REFERENCES iam.accounts(account_id) ON DELETE RESTRICT,
    identity_type varchar(40) NOT NULL,
    fingerprint varchar(64) NOT NULL,
    key_version varchar(30) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_deleted_identity_fingerprint_type CHECK (identity_type IN (
        'USERNAME', 'EMAIL', 'PHONE_NUMBER', 'IDENTIFY_CARD',
        'CUSTOMER_CODE', 'EMPLOYEE_CODE', 'PROVIDER_SUBJECT'
    )),
    CONSTRAINT ck_deleted_identity_fingerprint_sha256 CHECK (fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT uq_deleted_identity_fingerprint_request_value
        UNIQUE (request_id, identity_type, fingerprint, key_version)
);

CREATE INDEX IF NOT EXISTS idx_deleted_identity_fingerprint_lookup
    ON audit.deleted_identity_fingerprints(identity_type, fingerprint, key_version);
CREATE INDEX IF NOT EXISTS idx_deleted_identity_fingerprint_account
    ON audit.deleted_identity_fingerprints(account_id, created_at DESC);
