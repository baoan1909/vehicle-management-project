BEGIN;

DO $$
DECLARE
    seed_account_id UUID := '20000000-0000-0000-0000-00000000a002';
    seed_profile_id UUID := '10000000-0000-0000-0000-00000000a002';
    seed_keycloak_user_id TEXT := '54fd8e18-9f9d-4d93-8f63-f4f0374da001';
    system_admin_role_id UUID;
BEGIN
    SELECT role_id
    INTO system_admin_role_id
    FROM iam.roles
    WHERE code = 'SYSTEM_ADMIN'
    LIMIT 1;

    IF system_admin_role_id IS NULL THEN
        SELECT role_id
        INTO system_admin_role_id
        FROM iam.roles
        WHERE code = 'ADMIN'
        LIMIT 1;
    END IF;

    IF system_admin_role_id IS NULL THEN
        RAISE EXCEPTION 'Cannot create noreply SYSTEM_ADMIN account because no SYSTEM_ADMIN/ADMIN role exists.';
    END IF;

    INSERT INTO people.user_profiles (
        user_profile_id,
        full_name,
        status,
        created_at,
        updated_at
    )
    VALUES (
        seed_profile_id,
        'Vehicle Management System Admin',
        'ACTIVE',
        now(),
        now()
    )
    ON CONFLICT (user_profile_id) DO UPDATE
    SET full_name = EXCLUDED.full_name,
        status = EXCLUDED.status,
        updated_at = now();

    IF EXISTS (
        SELECT 1
        FROM iam.accounts
        WHERE account_id = seed_account_id
           OR email = 'vehiclemanagement.noreply@gmail.com'
    ) THEN
        UPDATE iam.accounts
        SET user_profile_id = seed_profile_id,
            username = 'vehiclemanagement.noreply',
            email = 'vehiclemanagement.noreply@gmail.com',
            keycloak_user_id = seed_keycloak_user_id,
            role_id = system_admin_role_id,
            status = 'ACTIVE',
            updated_at = now()
        WHERE account_id = seed_account_id
           OR email = 'vehiclemanagement.noreply@gmail.com';
    ELSE
        INSERT INTO iam.accounts (
            account_id,
            user_profile_id,
            username,
            email,
            keycloak_user_id,
            role_id,
            status,
            failed_login_count,
            password_changed_at,
            created_at,
            updated_at
        )
        VALUES (
            seed_account_id,
            seed_profile_id,
            'vehiclemanagement.noreply',
            'vehiclemanagement.noreply@gmail.com',
            seed_keycloak_user_id,
            system_admin_role_id,
            'ACTIVE',
            0,
            now(),
            now(),
            now()
        );
    END IF;
END $$;

COMMIT;
