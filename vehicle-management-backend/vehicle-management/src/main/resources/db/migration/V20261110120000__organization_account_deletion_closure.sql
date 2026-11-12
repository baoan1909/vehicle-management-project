-- Permission-first organization closure as part of the account-deletion lifecycle.
-- Organizations and historical records are retained; only live visibility/access is removed.

ALTER TABLE iam.organizations DROP CONSTRAINT IF EXISTS ck_organizations_status;
ALTER TABLE iam.organizations ADD CONSTRAINT ck_organizations_status
    CHECK (status IN ('ACTIVE', 'CLOSING', 'SUSPENDED', 'CLOSED'));

ALTER TABLE iam.organizations
    ADD COLUMN IF NOT EXISTS closed_at timestamptz,
    ADD COLUMN IF NOT EXISTS closed_by uuid,
    ADD COLUMN IF NOT EXISTS closure_reason varchar(500),
    ADD COLUMN IF NOT EXISTS closure_request_id uuid;

ALTER TABLE iam.organizations DROP CONSTRAINT IF EXISTS fk_organizations_closed_by;
ALTER TABLE iam.organizations ADD CONSTRAINT fk_organizations_closed_by
    FOREIGN KEY (closed_by) REFERENCES iam.accounts(account_id) ON DELETE SET NULL;

ALTER TABLE iam.account_deletion_requests
    ADD COLUMN IF NOT EXISTS identity_provider_subject_snapshot varchar(255);

ALTER TABLE iam.account_deletion_requests DROP CONSTRAINT IF EXISTS ck_account_deletion_request_status;
ALTER TABLE iam.account_deletion_requests ADD CONSTRAINT ck_account_deletion_request_status CHECK (
    status IN ('GRACE_PERIOD', 'CANCELLED', 'PROCESSING', 'RETRY', 'BLOCKED', 'REVIEW_REQUIRED', 'COMPLETED')
);

DROP INDEX IF EXISTS iam.uq_account_deletion_active_request;
CREATE UNIQUE INDEX uq_account_deletion_active_request
    ON iam.account_deletion_requests(account_id)
    WHERE status IN ('GRACE_PERIOD', 'PROCESSING', 'RETRY', 'REVIEW_REQUIRED');

CREATE TABLE IF NOT EXISTS iam.account_deletion_organization_scopes (
    scope_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id uuid NOT NULL REFERENCES iam.account_deletion_requests(request_id) ON DELETE CASCADE,
    organization_id uuid NOT NULL REFERENCES iam.organizations(organization_id) ON DELETE RESTRICT,
    previous_organization_status varchar(30) NOT NULL,
    closure_status varchar(30) NOT NULL DEFAULT 'GRACE_PERIOD',
    captured_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    CONSTRAINT uq_account_deletion_organization_scope UNIQUE (request_id, organization_id),
    CONSTRAINT ck_account_deletion_scope_previous_status
        CHECK (previous_organization_status IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT ck_account_deletion_scope_status
        CHECK (closure_status IN ('GRACE_PERIOD', 'PROCESSING', 'CANCELLED', 'BLOCKED', 'COMPLETED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_account_deletion_active_organization_scope
    ON iam.account_deletion_organization_scopes(organization_id)
    WHERE closure_status IN ('GRACE_PERIOD', 'PROCESSING');
CREATE INDEX IF NOT EXISTS idx_account_deletion_scope_request
    ON iam.account_deletion_organization_scopes(request_id, closure_status);

ALTER TABLE iam.organizations DROP CONSTRAINT IF EXISTS fk_organizations_closure_request;
ALTER TABLE iam.organizations ADD CONSTRAINT fk_organizations_closure_request
    FOREIGN KEY (closure_request_id) REFERENCES iam.account_deletion_requests(request_id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS iam.account_deletion_cleanup_items (
    cleanup_item_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id uuid NOT NULL REFERENCES iam.account_deletion_requests(request_id) ON DELETE CASCADE,
    organization_id uuid REFERENCES iam.organizations(organization_id) ON DELETE RESTRICT,
    resource_type varchar(50) NOT NULL,
    external_identifier varchar(1000) NOT NULL,
    scheduled_at timestamptz NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'SCHEDULED',
    attempt_count integer NOT NULL DEFAULT 0,
    next_retry_at timestamptz,
    processing_lease_until timestamptz,
    claim_token varchar(100),
    last_error varchar(1000),
    completed_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid,
    updated_at timestamptz,
    updated_by uuid,
    CONSTRAINT uq_account_deletion_cleanup_resource
        UNIQUE (request_id, resource_type, external_identifier),
    CONSTRAINT ck_account_deletion_cleanup_status
        CHECK (status IN ('SCHEDULED', 'PROCESSING', 'RETRY', 'COMPLETED', 'REVIEW_REQUIRED')),
    CONSTRAINT ck_account_deletion_cleanup_attempt CHECK (attempt_count >= 0)
);
CREATE INDEX IF NOT EXISTS idx_account_deletion_cleanup_due
    ON iam.account_deletion_cleanup_items(status, scheduled_at, next_retry_at);

ALTER TABLE operations.chat_conversations
    ADD COLUMN IF NOT EXISTS organization_id uuid;
ALTER TABLE operations.chat_conversations DROP CONSTRAINT IF EXISTS fk_chat_conversations_organization;
ALTER TABLE operations.chat_conversations ADD CONSTRAINT fk_chat_conversations_organization
    FOREIGN KEY (organization_id) REFERENCES iam.organizations(organization_id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_chat_conversations_organization_status
    ON operations.chat_conversations(organization_id, status);

-- Only deterministic operational links are backfilled. Ambiguous conversations remain unscoped.
UPDATE operations.chat_conversations conversation
SET organization_id = lot.organization_id
FROM parking.parking_sessions session
JOIN parking.parking_lots lot ON lot.parking_lot_id = session.parking_lot_id
WHERE conversation.organization_id IS NULL
  AND conversation.related_schema = 'parking'
  AND conversation.related_table = 'parking_sessions'
  AND conversation.related_id = session.parking_session_id;

INSERT INTO iam.permission_actions(action_id, code, name, description, created_at)
VALUES ('d1200000-0000-4000-8000-000000000001', 'CLOSE', 'Đóng', 'Đóng tài nguyên trong phạm vi được cấp.', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_actions(action_id, code, name, description, created_at)
VALUES ('d1200000-0000-4000-8000-000000000002', 'LOOKUP', 'Tra cứu', 'Tra cứu dữ liệu lưu trữ có kiểm soát.', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_actions(action_id, code, name, description, created_at)
VALUES
    ('d1200000-0000-4000-8000-000000000004', 'READ_DELETED', 'Xem dữ liệu đã xóa', 'Xem dữ liệu account đã xóa có kiểm soát.', now()),
    ('d1200000-0000-4000-8000-000000000005', 'READ_CLOSED', 'Xem dữ liệu đã đóng', 'Xem dữ liệu tổ chức đã đóng có kiểm soát.', now()),
    ('d1200000-0000-4000-8000-000000000006', 'READ_AUDIT', 'Xem audit xóa', 'Xem audit và kết quả cleanup xóa account.', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permission_scopes(scope_id, code, name, description, created_at)
VALUES ('d1200000-0000-4000-8000-000000000003', 'OWN', 'Sở hữu', 'Tài nguyên thuộc tổ chức mà tài khoản quản trị.', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions(permission_id, permission_code, name, description,
                            module_id, action_id, scope_id, created_at)
SELECT 'd1200000-0000-4000-8000-000000000010', 'ORGANIZATION_CLOSE_OWN',
       'Đóng tổ chức đang quản trị', 'Cho phép account chịu trách nhiệm cuối cùng đóng mềm tổ chức của mình.',
       module.module_id, action.action_id, scope.scope_id, now()
FROM iam.permission_modules module
JOIN iam.permission_actions action ON action.code = 'CLOSE'
JOIN iam.permission_scopes scope ON scope.code = 'OWN'
WHERE module.code = 'ORGANIZATION'
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO iam.permissions(permission_id, permission_code, name, description,
                            module_id, action_id, scope_id, created_at)
SELECT permission_seed.permission_id, permission_seed.permission_code,
       permission_seed.name, permission_seed.description,
       module.module_id, action.action_id, scope.scope_id, now()
FROM (VALUES
    ('d1200000-0000-4000-8000-000000000011'::uuid, 'DELETED_ACCOUNT_READ_ALL', 'Xem account đã xóa', 'Xem bản ghi account đã xóa qua API lưu trữ.'),
    ('d1200000-0000-4000-8000-000000000012'::uuid, 'CLOSED_ORGANIZATION_READ_ALL', 'Xem tổ chức đã đóng', 'Xem tổ chức đã đóng qua API lưu trữ.'),
    ('d1200000-0000-4000-8000-000000000013'::uuid, 'ACCOUNT_DELETION_AUDIT_READ_ALL', 'Xem audit xóa account', 'Xem timeline và kết quả cleanup của yêu cầu xóa.'),
    ('d1200000-0000-4000-8000-000000000014'::uuid, 'DELETED_IDENTITY_LOOKUP_ALL', 'Tra cứu định danh đã xóa', 'Tra cứu fingerprint định danh đã xóa có kiểm soát.')
) permission_seed(permission_id, permission_code, name, description)
JOIN iam.permission_modules module ON module.code = 'ACCOUNT_DELETION'
JOIN iam.permission_actions action ON action.code = CASE
    WHEN permission_seed.permission_code = 'DELETED_ACCOUNT_READ_ALL' THEN 'READ_DELETED'
    WHEN permission_seed.permission_code = 'CLOSED_ORGANIZATION_READ_ALL' THEN 'READ_CLOSED'
    WHEN permission_seed.permission_code = 'ACCOUNT_DELETION_AUDIT_READ_ALL' THEN 'READ_AUDIT'
    ELSE 'LOOKUP' END
JOIN iam.permission_scopes scope ON scope.code = 'ALL'
ON CONFLICT (permission_code) DO NOTHING;

-- Role codes are used only to seed defaults. Runtime policies resolve effective permissions.
INSERT INTO iam.role_permissions(id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code = 'ORGANIZATION_CLOSE_OWN'
WHERE role.code = 'PARTNER_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, is_system = true;

INSERT INTO iam.role_permissions(id, role_id, permission_id, created_at, is_active, is_system)
SELECT gen_random_uuid(), role.role_id, permission.permission_id, now(), true, true
FROM iam.roles role
JOIN iam.permissions permission ON permission.permission_code IN (
    'DELETED_ACCOUNT_READ_ALL', 'CLOSED_ORGANIZATION_READ_ALL',
    'ACCOUNT_DELETION_AUDIT_READ_ALL', 'DELETED_IDENTITY_LOOKUP_ALL'
)
WHERE role.code = 'SYSTEM_ADMIN'
ON CONFLICT (role_id, permission_id) DO UPDATE SET is_active = true, is_system = true;
