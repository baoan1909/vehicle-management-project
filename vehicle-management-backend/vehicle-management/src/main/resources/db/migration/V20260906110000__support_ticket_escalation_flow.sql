-- Customer-requested support escalation reuses the generic approval workflow.
-- Structured request/decision payloads keep the generic table extensible while
-- preserving the original request after a reviewer records a decision.

ALTER TABLE operations.approval_requests
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100),
    ADD COLUMN IF NOT EXISTS request_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS decision_data JSONB;

ALTER TABLE operations.approval_requests
    ADD CONSTRAINT ck_approval_requests_request_data_object
        CHECK (jsonb_typeof(request_data) = 'object') NOT VALID,
    ADD CONSTRAINT ck_approval_requests_decision_data_object
        CHECK (decision_data IS NULL OR jsonb_typeof(decision_data) = 'object') NOT VALID;

ALTER TABLE operations.approval_requests
    VALIDATE CONSTRAINT ck_approval_requests_request_data_object;
ALTER TABLE operations.approval_requests
    VALIDATE CONSTRAINT ck_approval_requests_decision_data_object;

CREATE UNIQUE INDEX IF NOT EXISTS uq_approval_requests_requester_idempotency
    ON operations.approval_requests (requested_by, request_type, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- Exactly one unresolved escalation is allowed for a support ticket. This also
-- serializes duplicate browser submissions at the database boundary.
CREATE UNIQUE INDEX IF NOT EXISTS uq_approval_requests_pending_support_ticket_escalation
    ON operations.approval_requests (target_id)
    WHERE request_type = 'SUPPORT_TICKET_ESCALATION'
      AND target_schema = 'operations'
      AND target_table = 'support_tickets'
      AND status = 'PENDING';

INSERT INTO iam.permission_actions (
    action_id, code, name, description, created_at, created_by, updated_at, updated_by
) VALUES
    ('7f4f11d7-2b52-4c20-bf11-0e4200000301', 'REQUEST_REVIEW', 'Yêu cầu xem xét',
     'Cho phép chủ thể gửi yêu cầu quản lý xem xét.', now(), NULL, NULL, NULL),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000302', 'REVIEW_ESCALATION', 'Xử lý yêu cầu xem xét',
     'Cho phép xem và ra quyết định đối với yêu cầu cần quản lý xem xét.', now(), NULL, NULL, NULL)
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.permissions (
    permission_id, permission_code, name, description, created_at, created_by, updated_at, updated_by,
    module_id, action_id, scope_id
) VALUES
    ('7f4f11d7-2b52-4c20-bf11-0e4200000311', 'SUPPORT_TICKET_ESCALATION_CREATE_OWN',
     'Yêu cầu xem xét phiếu của mình',
     'Cho phép khách hàng yêu cầu quản lý xem xét hoặc đổi người hỗ trợ cho phiếu của chính mình.',
     now(), NULL, NULL, NULL, '00000000-0000-0000-0000-000000001029',
     '7f4f11d7-2b52-4c20-bf11-0e4200000301', '00000000-0000-0000-0000-000000003002'),
    ('7f4f11d7-2b52-4c20-bf11-0e4200000312', 'SUPPORT_TICKET_ESCALATION_REVIEW_ALL',
     'Xử lý yêu cầu xem xét hỗ trợ',
     'Cho phép quản lý xem và ra quyết định đối với mọi yêu cầu xem xét hỗ trợ.',
     now(), NULL, NULL, NULL, '00000000-0000-0000-0000-000000001029',
     '7f4f11d7-2b52-4c20-bf11-0e4200000302', '00000000-0000-0000-0000-000000003001')
ON CONFLICT (permission_code) DO NOTHING;

-- Default IAM configuration only. Runtime authorization remains permission-first.
INSERT INTO iam.role_permissions (
    id, role_id, permission_id, created_at, created_by, updated_at, updated_by, is_active, is_system
)
SELECT seeded.id, seeded.role_id, permission.permission_id, now(), NULL, NULL, NULL, true, true
FROM (
    VALUES
        ('7f4f11d7-2b52-4c20-bf11-0e4200000321'::uuid,
         '00000000-0000-0000-0000-000000000003'::uuid,
         'SUPPORT_TICKET_ESCALATION_CREATE_OWN'),
        ('7f4f11d7-2b52-4c20-bf11-0e4200000322'::uuid,
         '00000000-0000-0000-0000-000000000005'::uuid,
         'SUPPORT_TICKET_ESCALATION_REVIEW_ALL')
) AS seeded(id, role_id, permission_code)
JOIN iam.permissions permission ON permission.permission_code = seeded.permission_code
ON CONFLICT (role_id, permission_id) DO UPDATE
SET is_active = true,
    updated_at = now();
