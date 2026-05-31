BEGIN;

DO $$
DECLARE
    system_role_id UUID;
BEGIN
    SELECT role_id
    INTO system_role_id
    FROM iam.roles
    WHERE code = 'SYSTEM_ADMIN'
    LIMIT 1;

    IF system_role_id IS NULL THEN
        SELECT role_id
        INTO system_role_id
        FROM iam.roles
        WHERE code = 'ADMIN'
        LIMIT 1;
    END IF;

    IF system_role_id IS NULL THEN
        RAISE EXCEPTION 'Cannot create SYSTEM account for auditing because no ADMIN/SYSTEM_ADMIN role exists.';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM iam.accounts WHERE username = 'SYSTEM') THEN
        INSERT INTO iam.accounts (
            account_id,
            user_profile_id,
            username,
            email,
            role_id,
            status,
            failed_login_count,
            password_changed_at,
            created_at,
            updated_at
        )
        VALUES (
            '20000000-0000-0000-0000-00000000a001',
            NULL,
            'SYSTEM',
            'system+' || replace(gen_random_uuid()::text, '-', '') || '@vehicle-management.local',
            system_role_id,
            'ACTIVE',
            0,
            now(),
            now(),
            now()
        );
    END IF;
END $$;

COMMIT;
