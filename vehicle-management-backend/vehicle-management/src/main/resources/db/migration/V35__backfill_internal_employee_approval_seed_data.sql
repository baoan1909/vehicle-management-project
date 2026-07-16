DO $$
DECLARE
    missing_approver_count INTEGER;
BEGIN
    WITH internal_employee_accounts AS (
        SELECT
            e.employee_id,
            a.account_id,
            r.code AS role_code
        FROM people.employees e
                 JOIN people.user_profiles up ON up.user_profile_id = e.user_profile_id
                 JOIN iam.accounts a ON a.user_profile_id = up.user_profile_id
                 JOIN iam.roles r ON r.role_id = a.role_id
        WHERE r.code IN ('EMPLOYEE', 'PARKING_MANAGER')
    ),
    latest_approval AS (
        SELECT DISTINCT ON (ar.target_id)
            ar.target_id,
            ar.status
        FROM operations.approval_requests ar
        WHERE ar.request_type = 'INTERNAL_EMPLOYEE_ONBOARDING'
          AND ar.target_schema = 'people'
          AND ar.target_table = 'employees'
        ORDER BY ar.target_id, ar.created_at DESC NULLS LAST, ar.approval_request_id DESC
    ),
    employees_requiring_backfill AS (
        SELECT iea.*
        FROM internal_employee_accounts iea
                 LEFT JOIN latest_approval la ON la.target_id = iea.employee_id
        WHERE COALESCE(la.status, '') <> 'APPROVED'
    )
    SELECT COUNT(*)
    INTO missing_approver_count
    FROM employees_requiring_backfill employee
             LEFT JOIN LATERAL (
        SELECT approver.account_id
        FROM iam.accounts approver
                 JOIN iam.roles approver_role ON approver_role.role_id = approver.role_id
        WHERE approver.status = 'ACTIVE'
          AND (
            (employee.role_code = 'EMPLOYEE' AND approver_role.code = 'PARKING_MANAGER')
                OR (employee.role_code = 'PARKING_MANAGER' AND approver_role.code = 'SYSTEM_ADMIN')
                OR approver_role.code = 'SYSTEM_ADMIN'
                OR approver.username IN ('admin', 'vehiclemanagement.noreply', 'SYSTEM')
            )
        ORDER BY
            CASE
                WHEN employee.role_code = 'EMPLOYEE' AND approver_role.code = 'PARKING_MANAGER' THEN 1
                WHEN employee.role_code = 'PARKING_MANAGER' AND approver_role.code = 'SYSTEM_ADMIN' THEN 1
                WHEN approver.username = 'admin' THEN 2
                WHEN approver.username = 'vehiclemanagement.noreply' THEN 3
                WHEN approver.username = 'SYSTEM' THEN 4
                WHEN approver_role.code = 'SYSTEM_ADMIN' THEN 5
                ELSE 6
                END
        LIMIT 1
    ) approver ON TRUE
    WHERE approver.account_id IS NULL;

    IF missing_approver_count > 0 THEN
        RAISE EXCEPTION 'Cannot backfill internal employee approvals because no active admin/manager approver account was found';
    END IF;

    WITH internal_employee_accounts AS (
        SELECT
            e.employee_id,
            a.account_id,
            r.code AS role_code
        FROM people.employees e
                 JOIN people.user_profiles up ON up.user_profile_id = e.user_profile_id
                 JOIN iam.accounts a ON a.user_profile_id = up.user_profile_id
                 JOIN iam.roles r ON r.role_id = a.role_id
        WHERE r.code IN ('EMPLOYEE', 'PARKING_MANAGER')
    ),
    latest_approval AS (
        SELECT DISTINCT ON (ar.target_id)
            ar.target_id,
            ar.status
        FROM operations.approval_requests ar
        WHERE ar.request_type = 'INTERNAL_EMPLOYEE_ONBOARDING'
          AND ar.target_schema = 'people'
          AND ar.target_table = 'employees'
        ORDER BY ar.target_id, ar.created_at DESC NULLS LAST, ar.approval_request_id DESC
    ),
    employees_requiring_backfill AS (
        SELECT iea.*
        FROM internal_employee_accounts iea
                 LEFT JOIN latest_approval la ON la.target_id = iea.employee_id
        WHERE COALESCE(la.status, '') <> 'APPROVED'
    )
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
    SELECT
        gen_random_uuid(),
        'INTERNAL_EMPLOYEE_ONBOARDING',
        'people',
        'employees',
        employee.employee_id,
        'APPROVED',
        employee.account_id,
        approver.account_id,
        now(),
        'Backfilled approved internal employee onboarding seed data.',
        now(),
        approver.account_id,
        now(),
        approver.account_id
    FROM employees_requiring_backfill employee
             JOIN LATERAL (
        SELECT approver.account_id
        FROM iam.accounts approver
                 JOIN iam.roles approver_role ON approver_role.role_id = approver.role_id
        WHERE approver.status = 'ACTIVE'
          AND (
            (employee.role_code = 'EMPLOYEE' AND approver_role.code = 'PARKING_MANAGER')
                OR (employee.role_code = 'PARKING_MANAGER' AND approver_role.code = 'SYSTEM_ADMIN')
                OR approver_role.code = 'SYSTEM_ADMIN'
                OR approver.username IN ('admin', 'vehiclemanagement.noreply', 'SYSTEM')
            )
        ORDER BY
            CASE
                WHEN employee.role_code = 'EMPLOYEE' AND approver_role.code = 'PARKING_MANAGER' THEN 1
                WHEN employee.role_code = 'PARKING_MANAGER' AND approver_role.code = 'SYSTEM_ADMIN' THEN 1
                WHEN approver.username = 'admin' THEN 2
                WHEN approver.username = 'vehiclemanagement.noreply' THEN 3
                WHEN approver.username = 'SYSTEM' THEN 4
                WHEN approver_role.code = 'SYSTEM_ADMIN' THEN 5
                ELSE 6
                END
        LIMIT 1
    ) approver ON TRUE;
END $$;
