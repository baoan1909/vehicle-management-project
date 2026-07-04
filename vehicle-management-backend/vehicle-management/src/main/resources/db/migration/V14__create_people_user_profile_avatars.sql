CREATE TABLE IF NOT EXISTS people.user_profile_avatars (
    avatar_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id UUID NOT NULL
        REFERENCES people.user_profiles(user_profile_id) ON DELETE RESTRICT,
    object_key VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(100),
    size_bytes BIGINT,
    checksum_sha256 VARCHAR(64),
    bucket VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT false,
    uploaded_by_account_id UUID
        REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ,
    updated_by UUID REFERENCES iam.accounts(account_id) ON DELETE SET NULL,
    CONSTRAINT ck_user_profile_avatars_bucket
        CHECK (bucket IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT ck_user_profile_avatars_status
        CHECK (status IN ('ACTIVE', 'REPLACED', 'DELETED')),
    CONSTRAINT ck_user_profile_avatars_current_active
        CHECK (is_current = false OR status = 'ACTIVE'),
    CONSTRAINT ck_user_profile_avatars_size_non_negative
        CHECK (size_bytes IS NULL OR size_bytes >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_profile_current_avatar
    ON people.user_profile_avatars(user_profile_id)
    WHERE is_current = true;

CREATE INDEX IF NOT EXISTS idx_user_profile_avatars_profile
    ON people.user_profile_avatars(user_profile_id);

CREATE INDEX IF NOT EXISTS idx_user_profile_avatars_object_key
    ON people.user_profile_avatars(object_key);

CREATE INDEX IF NOT EXISTS idx_user_profile_avatars_uploaded_by
    ON people.user_profile_avatars(uploaded_by_account_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'trg_user_profile_avatars_set_updated_at'
    ) THEN
        CREATE TRIGGER trg_user_profile_avatars_set_updated_at
        BEFORE UPDATE ON people.user_profile_avatars
        FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
    END IF;
END $$;
