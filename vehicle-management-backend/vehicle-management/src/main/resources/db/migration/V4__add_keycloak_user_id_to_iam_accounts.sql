ALTER TABLE iam.accounts
    ADD COLUMN IF NOT EXISTS keycloak_user_id VARCHAR(255);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_accounts_keycloak_user_id'
    ) THEN
        ALTER TABLE iam.accounts
            ADD CONSTRAINT uq_accounts_keycloak_user_id UNIQUE (keycloak_user_id);
    END IF;
END $$;
