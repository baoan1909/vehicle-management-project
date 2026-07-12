DO $$
DECLARE
approver_id UUID;
BEGIN
SELECT a.account_id
INTO approver_id
FROM iam.accounts a
         JOIN iam.roles r ON r.role_id = a.role_id
WHERE a.status = 'ACTIVE'
  AND (
    a.username IN ('admin', 'vehiclemanagement.noreply', 'SYSTEM')
        OR r.code IN ('SYSTEM_ADMIN', 'PARKING_MANAGER')
    )
ORDER BY
    CASE a.username
        WHEN 'admin' THEN 1
        WHEN 'vehiclemanagement.noreply' THEN 2
        WHEN 'SYSTEM' THEN 3
        ELSE 4
        END
    LIMIT 1;

IF approver_id IS NULL THEN
        RAISE EXCEPTION 'No active admin/manager account found for customer approved_by seed';
END IF;

UPDATE people.customers
SET approved_by = COALESCE(approved_by, approver_id),
    approved_at = COALESCE(approved_at, created_at, now()),
    updated_at = now()
WHERE approval_status = 'APPROVED'
  AND (approved_by IS NULL OR approved_at IS NULL);
END $$;