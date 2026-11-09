-- Customer self-service account deletion with a cancellable grace period.
-- Deleted identities are tombstoned in-place so historical foreign keys remain valid.

ALTER TABLE iam.accounts DROP CONSTRAINT IF EXISTS ck_accounts_status;
ALTER TABLE iam.accounts ADD CONSTRAINT ck_accounts_status
    CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED', 'PENDING', 'DELETED'));

ALTER TABLE iam.account_status_history DROP CONSTRAINT IF EXISTS ck_account_status_history_new_status;
ALTER TABLE iam.account_status_history ADD CONSTRAINT ck_account_status_history_new_status
    CHECK (new_status IN ('ACTIVE', 'LOCKED', 'DISABLED', 'PENDING', 'DELETED'));

ALTER TABLE people.user_profiles DROP CONSTRAINT IF EXISTS ck_user_profiles_status;
ALTER TABLE people.user_profiles ADD CONSTRAINT ck_user_profiles_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'));
ALTER TABLE people.user_profiles ALTER COLUMN phone_number TYPE varchar(100);
ALTER TABLE people.user_profiles ALTER COLUMN identify_card TYPE varchar(100);

ALTER TABLE people.customers DROP CONSTRAINT IF EXISTS ck_customers_status;
ALTER TABLE people.customers ADD CONSTRAINT ck_customers_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'));

CREATE TABLE IF NOT EXISTS iam.account_deletion_requests (
    request_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id uuid NOT NULL REFERENCES iam.accounts(account_id) ON DELETE RESTRICT,
    status varchar(30) NOT NULL,
    requested_at timestamptz NOT NULL,
    scheduled_deletion_at timestamptz NOT NULL,
    cancelled_at timestamptz,
    cancellation_reason varchar(500),
    processing_started_at timestamptz,
    processing_lease_until timestamptz,
    completed_at timestamptz,
    attempt_count integer NOT NULL DEFAULT 0,
    next_retry_at timestamptz,
    claim_token varchar(100),
    last_error_code varchar(100),
    last_error_message varchar(1000),
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    CONSTRAINT ck_account_deletion_request_status CHECK (
        status IN ('GRACE_PERIOD', 'CANCELLED', 'PROCESSING', 'RETRY', 'REVIEW_REQUIRED', 'COMPLETED')
    ),
    CONSTRAINT ck_account_deletion_request_schedule CHECK (scheduled_deletion_at > requested_at),
    CONSTRAINT ck_account_deletion_request_attempt CHECK (attempt_count >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_account_deletion_active_request
    ON iam.account_deletion_requests(account_id)
    WHERE status IN ('GRACE_PERIOD', 'PROCESSING', 'RETRY', 'REVIEW_REQUIRED');
CREATE INDEX IF NOT EXISTS idx_account_deletion_due
    ON iam.account_deletion_requests(status, scheduled_deletion_at, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_account_deletion_account
    ON iam.account_deletion_requests(account_id, requested_at DESC);

-- Runtime authorization checks permission codes; assigning to CUSTOMER only supplies defaults.
INSERT INTO iam.permission_modules(module_id, code, name, description, created_at)
VALUES ('d1100000-0000-4000-8000-000000000010', 'ACCOUNT_DELETION',
        'Xóa tài khoản', 'Vòng đời yêu cầu xóa tài khoản của chính người dùng.', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_actions(action_id, code, name, description, created_at)
VALUES ('d1100000-0000-4000-8000-000000000011', 'REQUEST',
        'Yêu cầu', 'Khởi tạo yêu cầu nghiệp vụ.', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_actions(action_id, code, name, description, created_at)
VALUES ('d1100000-0000-4000-8000-000000000012', 'CANCEL',
        'Hủy', 'Hủy yêu cầu nghiệp vụ.', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_scopes(scope_id, code, name, description, created_at)
VALUES ('d1100000-0000-4000-8000-000000000013', 'SELF',
        'Bản thân', 'Chỉ áp dụng cho tài khoản đang đăng nhập.', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions(permission_id, permission_code, name, description,
                            module_id, action_id, scope_id, created_at)
SELECT 'd1100000-0000-4000-8000-000000000001', 'ACCOUNT_DELETION_REQUEST_SELF',
       'Yêu cầu xóa tài khoản của mình', 'Tạo yêu cầu xóa tài khoản Customer đang đăng nhập.',
       module.module_id, action.action_id, scope.scope_id, now()
FROM iam.permission_modules module
JOIN iam.permission_actions action ON action.code = 'REQUEST'
JOIN iam.permission_scopes scope ON scope.code = 'SELF'
WHERE module.code = 'ACCOUNT_DELETION'
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.permissions(permission_id, permission_code, name, description,
                            module_id, action_id, scope_id, created_at)
SELECT 'd1100000-0000-4000-8000-000000000002', 'ACCOUNT_DELETION_READ_SELF',
       'Xem yêu cầu xóa tài khoản của mình', 'Xem trạng thái và hạn hủy yêu cầu xóa tài khoản.',
       module.module_id, action.action_id, scope.scope_id, now()
FROM iam.permission_modules module
JOIN iam.permission_actions action ON action.code = 'READ'
JOIN iam.permission_scopes scope ON scope.code = 'SELF'
WHERE module.code = 'ACCOUNT_DELETION'
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.permissions(permission_id, permission_code, name, description,
                            module_id, action_id, scope_id, created_at)
SELECT 'd1100000-0000-4000-8000-000000000003', 'ACCOUNT_DELETION_CANCEL_SELF',
       'Hủy yêu cầu xóa tài khoản của mình', 'Hủy yêu cầu xóa trong thời gian chờ.',
       module.module_id, action.action_id, scope.scope_id, now()
FROM iam.permission_modules module
JOIN iam.permission_actions action ON action.code = 'CANCEL'
JOIN iam.permission_scopes scope ON scope.code = 'SELF'
WHERE module.code = 'ACCOUNT_DELETION'
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.role_permissions(id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'ACCOUNT_DELETION_REQUEST_SELF', 'ACCOUNT_DELETION_READ_SELF', 'ACCOUNT_DELETION_CANCEL_SELF'
)
WHERE role.code = 'CUSTOMER'
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, is_system = true;
