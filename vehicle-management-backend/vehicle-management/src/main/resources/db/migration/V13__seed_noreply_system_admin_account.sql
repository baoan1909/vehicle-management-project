BEGIN;

DO $$
DECLARE
    seed_account_id UUID := '20000000-0000-0000-0000-00000000a002';
    seed_profile_id UUID := '10000000-0000-0000-0000-00000000a002';
    seed_keycloak_user_id TEXT := '54fd8e18-9f9d-4d93-8f63-f4f0374da001';
    employee_account_id UUID := '20000000-0000-0000-0000-00000000a003';
    employee_profile_id UUID := '10000000-0000-0000-0000-00000000a003';
    employee_employee_id UUID := '31000000-0000-0000-0000-00000000a003';
    employee_keycloak_user_id TEXT := '54fd8e18-9f9d-4d93-8f63-f4f0374da002';
    employee_approval_request_id UUID := '65000000-0000-0000-0000-00000000a003';
    employee_status_history_id UUID := '21000000-0000-0000-0000-00000000a003';
    parking_manager_account_id UUID := '20000000-0000-0000-0000-00000000a004';
    parking_manager_profile_id UUID := '10000000-0000-0000-0000-00000000a004';
    parking_manager_employee_id UUID := '31000000-0000-0000-0000-00000000a004';
    parking_manager_keycloak_user_id TEXT := '54fd8e18-9f9d-4d93-8f63-f4f0374da003';
    parking_manager_approval_request_id UUID := '65000000-0000-0000-0000-00000000a004';
    parking_manager_status_history_id UUID := '21000000-0000-0000-0000-00000000a004';
    system_admin_role_id UUID;
    employee_role_id UUID;
    parking_manager_role_id UUID;
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

    SELECT role_id
    INTO employee_role_id
    FROM iam.roles
    WHERE code = 'EMPLOYEE'
    LIMIT 1;

    SELECT role_id
    INTO parking_manager_role_id
    FROM iam.roles
    WHERE code = 'PARKING_MANAGER'
    LIMIT 1;

    IF employee_role_id IS NULL THEN
        RAISE EXCEPTION 'Cannot create seed EMPLOYEE account because no EMPLOYEE role exists.';
    END IF;

    IF parking_manager_role_id IS NULL THEN
        RAISE EXCEPTION 'Cannot create seed PARKING_MANAGER account because no PARKING_MANAGER role exists.';
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

    SELECT account_id
    INTO seed_account_id
    FROM iam.accounts
    WHERE email = 'vehiclemanagement.noreply@gmail.com'
       OR keycloak_user_id = seed_keycloak_user_id
       OR user_profile_id = seed_profile_id
    ORDER BY
        CASE WHEN email = 'vehiclemanagement.noreply@gmail.com' THEN 1 ELSE 2 END,
        updated_at DESC NULLS LAST,
        created_at DESC NULLS LAST
    LIMIT 1;

    IF seed_account_id IS NULL THEN
        RAISE EXCEPTION 'Cannot resolve seeded SYSTEM_ADMIN account id.';
    END IF;

    DELETE FROM operations.approval_requests
    WHERE approval_request_id = employee_approval_request_id
       OR (
           request_type = 'INTERNAL_EMPLOYEE_ONBOARDING'
           AND target_schema = 'people'
           AND target_table = 'employees'
           AND target_id IN (
               SELECT e.employee_id
               FROM people.employees e
               WHERE e.employee_id = employee_employee_id
                  OR e.employee_code = 'EMP-DOB946899'
                  OR e.user_profile_id = employee_profile_id
                  OR e.user_profile_id IN (
                      SELECT a.user_profile_id
                      FROM iam.accounts a
                      WHERE a.account_id = employee_account_id
                         OR a.username = 'dob946899'
                         OR a.email = 'dob946899@gmail.com'
                         OR a.keycloak_user_id = employee_keycloak_user_id
                         OR a.user_profile_id = employee_profile_id
                  )
           )
       );

    DELETE FROM iam.account_status_history
    WHERE account_status_history_id = employee_status_history_id
       OR account_id IN (
           SELECT a.account_id
           FROM iam.accounts a
           WHERE a.account_id = employee_account_id
              OR a.username = 'dob946899'
              OR a.email = 'dob946899@gmail.com'
              OR a.keycloak_user_id = employee_keycloak_user_id
              OR a.user_profile_id = employee_profile_id
       );

    DELETE FROM operations.shift_assignments
    WHERE employee_id IN (
        SELECT e.employee_id
        FROM people.employees e
        WHERE e.employee_id = employee_employee_id
           OR e.employee_code = 'EMP-DOB946899'
           OR e.user_profile_id = employee_profile_id
           OR e.user_profile_id IN (
               SELECT a.user_profile_id
               FROM iam.accounts a
               WHERE a.account_id = employee_account_id
                  OR a.username = 'dob946899'
                  OR a.email = 'dob946899@gmail.com'
                  OR a.keycloak_user_id = employee_keycloak_user_id
                  OR a.user_profile_id = employee_profile_id
           )
    );

    IF to_regclass('operations.employee_roster_rules') IS NOT NULL THEN
        EXECUTE $reset_employee_roster_rules$
            DELETE FROM operations.employee_roster_rules
            WHERE employee_id IN (
                SELECT e.employee_id
                FROM people.employees e
                WHERE e.employee_id = $1
                   OR e.employee_code = 'EMP-DOB946899'
                   OR e.user_profile_id = $2
                   OR e.user_profile_id IN (
                       SELECT a.user_profile_id
                       FROM iam.accounts a
                       WHERE a.account_id = $3
                          OR a.username = 'dob946899'
                          OR a.email = 'dob946899@gmail.com'
                          OR a.keycloak_user_id = $4
                          OR a.user_profile_id = $2
                   )
            )
        $reset_employee_roster_rules$
        USING employee_employee_id, employee_profile_id, employee_account_id, employee_keycloak_user_id;
    END IF;

    DELETE FROM people.employees
    WHERE employee_id = employee_employee_id
       OR employee_code = 'EMP-DOB946899'
       OR user_profile_id = employee_profile_id
       OR user_profile_id IN (
           SELECT a.user_profile_id
           FROM iam.accounts a
           WHERE a.account_id = employee_account_id
              OR a.username = 'dob946899'
              OR a.email = 'dob946899@gmail.com'
              OR a.keycloak_user_id = employee_keycloak_user_id
              OR a.user_profile_id = employee_profile_id
       );

    DELETE FROM iam.accounts
    WHERE account_id = employee_account_id
       OR username = 'dob946899'
       OR email = 'dob946899@gmail.com'
       OR keycloak_user_id = employee_keycloak_user_id
       OR user_profile_id = employee_profile_id;

    IF to_regclass('people.user_profile_avatars') IS NOT NULL THEN
        EXECUTE 'DELETE FROM people.user_profile_avatars WHERE user_profile_id = $1'
        USING employee_profile_id;
    END IF;

    DELETE FROM people.user_profiles
    WHERE user_profile_id = employee_profile_id
      AND NOT EXISTS (
          SELECT 1
          FROM people.customers c
          WHERE c.user_profile_id = employee_profile_id
      );

    INSERT INTO people.user_profiles (
        user_profile_id,
        full_name,
        status,
        created_at,
        updated_at
    )
    VALUES (
        employee_profile_id,
        'Dob Employee',
        'ACTIVE',
        now(),
        now()
    )
    ON CONFLICT (user_profile_id) DO UPDATE
    SET full_name = EXCLUDED.full_name,
        status = EXCLUDED.status,
        updated_at = now();

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
        employee_account_id,
        employee_profile_id,
        'dob946899',
        'dob946899@gmail.com',
        employee_keycloak_user_id,
        employee_role_id,
        'ACTIVE',
        0,
        now(),
        now(),
        now()
    );

    INSERT INTO people.employees (
        employee_id,
        user_profile_id,
        employee_code,
        job_title,
        hired_at,
        status,
        created_at,
        updated_at
    )
    VALUES (
        employee_employee_id,
        employee_profile_id,
        'EMP-DOB946899',
        'Nhan vien van hanh',
        CURRENT_DATE,
        'ACTIVE',
        now(),
        now()
    )
    ON CONFLICT (employee_id) DO UPDATE
    SET user_profile_id = EXCLUDED.user_profile_id,
        employee_code = EXCLUDED.employee_code,
        job_title = EXCLUDED.job_title,
        hired_at = EXCLUDED.hired_at,
        status = EXCLUDED.status,
        updated_at = now();

    UPDATE operations.approval_requests
    SET status = 'APPROVED',
        approved_by = seed_account_id,
        approved_at = COALESCE(approved_at, now()),
        note = COALESCE(NULLIF(note, ''), 'Seeded approved internal employee account.'),
        updated_at = now(),
        updated_by = seed_account_id
    WHERE request_type = 'INTERNAL_EMPLOYEE_ONBOARDING'
      AND target_schema = 'people'
      AND target_table = 'employees'
      AND target_id = employee_employee_id;

    INSERT INTO operations.approval_requests (
        approval_request_id,
        request_type,
        target_schema,
        target_table,
        target_id,
        status,
        requested_by,
        approved_by,
        approved_at,
        note,
        created_at,
        created_by,
        updated_at,
        updated_by
    )
    VALUES (
        employee_approval_request_id,
        'INTERNAL_EMPLOYEE_ONBOARDING',
        'people',
        'employees',
        employee_employee_id,
        'APPROVED',
        employee_account_id,
        seed_account_id,
        now(),
        'Seeded approved internal employee account.',
        now(),
        seed_account_id,
        now(),
        seed_account_id
    )
    ON CONFLICT (approval_request_id) DO UPDATE
    SET status = 'APPROVED',
        requested_by = EXCLUDED.requested_by,
        approved_by = EXCLUDED.approved_by,
        approved_at = COALESCE(operations.approval_requests.approved_at, EXCLUDED.approved_at),
        note = EXCLUDED.note,
        updated_at = now(),
        updated_by = EXCLUDED.updated_by;

    INSERT INTO iam.account_status_history (
        account_status_history_id,
        account_id,
        old_status,
        new_status,
        reason,
        changed_at,
        changed_by
    )
    VALUES (
        employee_status_history_id,
        employee_account_id,
        NULL,
        'ACTIVE',
        'Seeded active employee account.',
        now(),
        seed_account_id
    )
    ON CONFLICT (account_status_history_id) DO UPDATE
    SET new_status = 'ACTIVE',
        reason = EXCLUDED.reason,
        changed_at = now(),
        changed_by = EXCLUDED.changed_by;

    DELETE FROM operations.approval_requests
    WHERE approval_request_id = parking_manager_approval_request_id
       OR (
           request_type = 'INTERNAL_EMPLOYEE_ONBOARDING'
           AND target_schema = 'people'
           AND target_table = 'employees'
           AND target_id IN (
               SELECT e.employee_id
               FROM people.employees e
               WHERE e.employee_id = parking_manager_employee_id
                  OR e.employee_code = 'EMP-BAOAN3236'
                  OR e.user_profile_id = parking_manager_profile_id
                  OR e.user_profile_id IN (
                      SELECT a.user_profile_id
                      FROM iam.accounts a
                      WHERE a.account_id = parking_manager_account_id
                         OR a.username = 'baoan3236'
                         OR a.email = 'baoan3236@gmail.com'
                         OR a.keycloak_user_id = parking_manager_keycloak_user_id
                         OR a.user_profile_id = parking_manager_profile_id
                  )
           )
       );

    DELETE FROM iam.account_status_history
    WHERE account_status_history_id = parking_manager_status_history_id
       OR account_id IN (
           SELECT a.account_id
           FROM iam.accounts a
           WHERE a.account_id = parking_manager_account_id
              OR a.username = 'baoan3236'
              OR a.email = 'baoan3236@gmail.com'
              OR a.keycloak_user_id = parking_manager_keycloak_user_id
              OR a.user_profile_id = parking_manager_profile_id
       );

    DELETE FROM operations.shift_assignments
    WHERE employee_id IN (
        SELECT e.employee_id
        FROM people.employees e
        WHERE e.employee_id = parking_manager_employee_id
           OR e.employee_code = 'EMP-BAOAN3236'
           OR e.user_profile_id = parking_manager_profile_id
           OR e.user_profile_id IN (
               SELECT a.user_profile_id
               FROM iam.accounts a
               WHERE a.account_id = parking_manager_account_id
                  OR a.username = 'baoan3236'
                  OR a.email = 'baoan3236@gmail.com'
                  OR a.keycloak_user_id = parking_manager_keycloak_user_id
                  OR a.user_profile_id = parking_manager_profile_id
           )
    );

    IF to_regclass('operations.employee_roster_rules') IS NOT NULL THEN
        EXECUTE $reset_parking_manager_roster_rules$
            DELETE FROM operations.employee_roster_rules
            WHERE employee_id IN (
                SELECT e.employee_id
                FROM people.employees e
                WHERE e.employee_id = $1
                   OR e.employee_code = 'EMP-BAOAN3236'
                   OR e.user_profile_id = $2
                   OR e.user_profile_id IN (
                       SELECT a.user_profile_id
                       FROM iam.accounts a
                       WHERE a.account_id = $3
                          OR a.username = 'baoan3236'
                          OR a.email = 'baoan3236@gmail.com'
                          OR a.keycloak_user_id = $4
                          OR a.user_profile_id = $2
                   )
            )
        $reset_parking_manager_roster_rules$
        USING parking_manager_employee_id, parking_manager_profile_id, parking_manager_account_id, parking_manager_keycloak_user_id;
    END IF;

    DELETE FROM people.employees
    WHERE employee_id = parking_manager_employee_id
       OR employee_code = 'EMP-BAOAN3236'
       OR user_profile_id = parking_manager_profile_id
       OR user_profile_id IN (
           SELECT a.user_profile_id
           FROM iam.accounts a
           WHERE a.account_id = parking_manager_account_id
              OR a.username = 'baoan3236'
              OR a.email = 'baoan3236@gmail.com'
              OR a.keycloak_user_id = parking_manager_keycloak_user_id
              OR a.user_profile_id = parking_manager_profile_id
       );

    DELETE FROM iam.accounts
    WHERE account_id = parking_manager_account_id
       OR username = 'baoan3236'
       OR email = 'baoan3236@gmail.com'
       OR keycloak_user_id = parking_manager_keycloak_user_id
       OR user_profile_id = parking_manager_profile_id;

    IF to_regclass('people.user_profile_avatars') IS NOT NULL THEN
        EXECUTE 'DELETE FROM people.user_profile_avatars WHERE user_profile_id = $1'
        USING parking_manager_profile_id;
    END IF;

    DELETE FROM people.user_profiles
    WHERE user_profile_id = parking_manager_profile_id
      AND NOT EXISTS (
          SELECT 1
          FROM people.customers c
          WHERE c.user_profile_id = parking_manager_profile_id
      );

    INSERT INTO people.user_profiles (
        user_profile_id,
        full_name,
        status,
        created_at,
        updated_at
    )
    VALUES (
        parking_manager_profile_id,
        'Bao An Parking Manager',
        'ACTIVE',
        now(),
        now()
    )
    ON CONFLICT (user_profile_id) DO UPDATE
    SET full_name = EXCLUDED.full_name,
        status = EXCLUDED.status,
        updated_at = now();

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
        parking_manager_account_id,
        parking_manager_profile_id,
        'baoan3236',
        'baoan3236@gmail.com',
        parking_manager_keycloak_user_id,
        parking_manager_role_id,
        'ACTIVE',
        0,
        now(),
        now(),
        now()
    );

    INSERT INTO people.employees (
        employee_id,
        user_profile_id,
        employee_code,
        job_title,
        hired_at,
        status,
        created_at,
        updated_at
    )
    VALUES (
        parking_manager_employee_id,
        parking_manager_profile_id,
        'EMP-BAOAN3236',
        'Quan ly bai xe',
        CURRENT_DATE,
        'ACTIVE',
        now(),
        now()
    )
    ON CONFLICT (employee_id) DO UPDATE
    SET user_profile_id = EXCLUDED.user_profile_id,
        employee_code = EXCLUDED.employee_code,
        job_title = EXCLUDED.job_title,
        hired_at = EXCLUDED.hired_at,
        status = EXCLUDED.status,
        updated_at = now();

    UPDATE operations.approval_requests
    SET status = 'APPROVED',
        approved_by = seed_account_id,
        approved_at = COALESCE(approved_at, now()),
        note = COALESCE(NULLIF(note, ''), 'Seeded approved parking manager account.'),
        updated_at = now(),
        updated_by = seed_account_id
    WHERE request_type = 'INTERNAL_EMPLOYEE_ONBOARDING'
      AND target_schema = 'people'
      AND target_table = 'employees'
      AND target_id = parking_manager_employee_id;

    INSERT INTO operations.approval_requests (
        approval_request_id,
        request_type,
        target_schema,
        target_table,
        target_id,
        status,
        requested_by,
        approved_by,
        approved_at,
        note,
        created_at,
        created_by,
        updated_at,
        updated_by
    )
    VALUES (
        parking_manager_approval_request_id,
        'INTERNAL_EMPLOYEE_ONBOARDING',
        'people',
        'employees',
        parking_manager_employee_id,
        'APPROVED',
        parking_manager_account_id,
        seed_account_id,
        now(),
        'Seeded approved parking manager account.',
        now(),
        seed_account_id,
        now(),
        seed_account_id
    )
    ON CONFLICT (approval_request_id) DO UPDATE
    SET status = 'APPROVED',
        requested_by = EXCLUDED.requested_by,
        approved_by = EXCLUDED.approved_by,
        approved_at = COALESCE(operations.approval_requests.approved_at, EXCLUDED.approved_at),
        note = EXCLUDED.note,
        updated_at = now(),
        updated_by = EXCLUDED.updated_by;

    INSERT INTO iam.account_status_history (
        account_status_history_id,
        account_id,
        old_status,
        new_status,
        reason,
        changed_at,
        changed_by
    )
    VALUES (
        parking_manager_status_history_id,
        parking_manager_account_id,
        NULL,
        'ACTIVE',
        'Seeded active parking manager account.',
        now(),
        seed_account_id
    )
    ON CONFLICT (account_status_history_id) DO UPDATE
    SET new_status = 'ACTIVE',
        reason = EXCLUDED.reason,
        changed_at = now(),
        changed_by = EXCLUDED.changed_by;
END $$;

COMMIT;
