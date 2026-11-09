-- Generalize self-service deletion for organization and workforce accounts.
-- Runtime authorization remains permission based; role grants below only provide defaults.

ALTER TABLE iam.accounts DROP CONSTRAINT IF EXISTS ck_accounts_status;
ALTER TABLE iam.accounts ADD CONSTRAINT ck_accounts_status
    CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED', 'PENDING', 'PENDING_DELETION', 'DELETED'));

ALTER TABLE iam.account_status_history DROP CONSTRAINT IF EXISTS ck_account_status_history_new_status;
ALTER TABLE iam.account_status_history ADD CONSTRAINT ck_account_status_history_new_status
    CHECK (new_status IN ('ACTIVE', 'LOCKED', 'DISABLED', 'PENDING', 'PENDING_DELETION', 'DELETED'));

ALTER TABLE people.employees DROP CONSTRAINT IF EXISTS ck_employees_status;
ALTER TABLE people.employees ADD CONSTRAINT ck_employees_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'));
ALTER TABLE people.employees ALTER COLUMN employee_code TYPE varchar(100);

ALTER TABLE iam.account_deletion_requests
    ADD COLUMN IF NOT EXISTS previous_account_status varchar(30);

UPDATE iam.account_deletion_requests request
SET previous_account_status = 'ACTIVE'
WHERE previous_account_status IS NULL;

ALTER TABLE iam.account_deletion_requests
    ALTER COLUMN previous_account_status SET NOT NULL;

ALTER TABLE iam.account_deletion_requests
    ADD CONSTRAINT ck_account_deletion_previous_status
    CHECK (previous_account_status IN ('ACTIVE', 'LOCKED', 'DISABLED', 'PENDING'));

UPDATE iam.permissions
SET description = 'Tạo yêu cầu xóa tài khoản đang đăng nhập khi không còn nghĩa vụ nghiệp vụ.'
WHERE permission_code = 'ACCOUNT_DELETION_REQUEST_SELF';

INSERT INTO iam.role_permissions(id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'ACCOUNT_DELETION_REQUEST_SELF', 'ACCOUNT_DELETION_READ_SELF', 'ACCOUNT_DELETION_CANCEL_SELF'
)
WHERE role.code IN ('PARTNER_ADMIN', 'PARKING_MANAGER', 'EMPLOYEE')
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, is_system = true;

